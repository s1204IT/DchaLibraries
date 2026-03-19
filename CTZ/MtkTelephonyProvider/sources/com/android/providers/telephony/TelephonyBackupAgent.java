package com.android.providers.telephony;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackupDataOutput;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

@TargetApi(23)
public class TelephonyBackupAgent extends BackupAgent {

    @VisibleForTesting
    static final Uri ALL_THREADS_URI;

    @VisibleForTesting
    static final String[] PROJECTION_ID;

    @VisibleForTesting
    static final Uri SINGLE_CANONICAL_ADDRESS_URI;
    private static int THREAD_ARCHIVED_IDX = 0;
    private static String[] THREAD_ARCHIVED_PROJECTION = null;

    @VisibleForTesting
    static final Uri THREAD_ID_CONTENT_URI;

    @VisibleForTesting
    static final String UNKNOWN_SENDER = "ʼUNKNOWN_SENDER!ʼ";
    private static volatile boolean sIsRestoring = false;

    @VisibleForTesting
    static final String sSmilTextOnly = "<smil><head><layout><root-layout/><region id=\"Text\" top=\"0\" left=\"0\" height=\"100%%\" width=\"100%%\"/></layout></head><body>%s</body></smil>";

    @VisibleForTesting
    static final String sSmilTextPart = "<par dur=\"5000ms\"><text src=\"%s\" region=\"Text\" /></par>";
    private long mBytesOverQuota;
    private ContentResolver mContentResolver;
    private long mUnknownSenderThreadId;
    private static String ATTACHMENT_DATA_PATH = "/app_parts/";

    @VisibleForTesting
    static final String[] SMS_PROJECTION = {"_id", "sub_id", "address", "body", "subject", "date", "date_sent", "status", "type", "thread_id", "read"};
    private static final String[] SMS_RECIPIENTS_PROJECTION = {"_id", "recipient_ids"};

    @VisibleForTesting
    static final String[] MMS_PROJECTION = {"_id", "sub_id", "sub", "sub_cs", "date", "date_sent", "m_type", "v", "msg_box", "ct_l", "thread_id", "tr_id", "read"};

    @VisibleForTesting
    static final String[] MMS_ADDR_PROJECTION = {"type", "address", "charset"};

    @VisibleForTesting
    static final String[] MMS_TEXT_PROJECTION = {"text", "chset"};
    private static ContentValues sDefaultValuesSms = new ContentValues(5);
    private static ContentValues sDefaultValuesMms = new ContentValues(6);
    private static final ContentValues sDefaultValuesAddr = new ContentValues(2);
    private static final ContentValues sDefaultValuesAttachments = new ContentValues(2);

    @VisibleForTesting
    int mMaxMsgPerFile = 1000;
    private SparseArray<String> mSubId2phone = new SparseArray<>();
    private Map<String, Integer> mPhone2subId = new ArrayMap();
    private Map<Long, Boolean> mThreadArchived = new HashMap();

    @VisibleForTesting
    Map<Long, List<String>> mCacheRecipientsByThread = null;

    @VisibleForTesting
    Map<Set<String>, Long> mCacheGetOrCreateThreadId = null;

    static {
        sDefaultValuesSms.put("read", (Integer) 1);
        sDefaultValuesSms.put("seen", (Integer) 1);
        sDefaultValuesSms.put("address", UNKNOWN_SENDER);
        sDefaultValuesSms.put("sub_id", (Integer) (-1));
        sDefaultValuesMms.put("read", (Integer) 1);
        sDefaultValuesMms.put("seen", (Integer) 1);
        sDefaultValuesMms.put("sub_id", (Integer) (-1));
        sDefaultValuesMms.put("msg_box", (Integer) 0);
        sDefaultValuesMms.put("text_only", (Integer) 1);
        sDefaultValuesAddr.put("type", (Integer) 0);
        sDefaultValuesAddr.put("charset", (Integer) 106);
        PROJECTION_ID = new String[]{"_id"};
        THREAD_ARCHIVED_PROJECTION = new String[]{"archived"};
        THREAD_ARCHIVED_IDX = 0;
        THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID");
        ALL_THREADS_URI = Telephony.Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();
        SINGLE_CANONICAL_ADDRESS_URI = Uri.parse("content://mms-sms/canonical-address");
    }

    @Override
    public void onCreate() {
        List<SubscriptionInfo> activeSubscriptionInfoList;
        super.onCreate();
        SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(this);
        if (subscriptionManagerFrom != null && (activeSubscriptionInfoList = subscriptionManagerFrom.getActiveSubscriptionInfoList()) != null) {
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                String normalizedNumber = getNormalizedNumber(subscriptionInfo);
                this.mSubId2phone.append(subscriptionInfo.getSubscriptionId(), normalizedNumber);
                this.mPhone2subId.put(normalizedNumber, Integer.valueOf(subscriptionInfo.getSubscriptionId()));
            }
        }
        this.mContentResolver = getContentResolver();
        initUnknownSender();
    }

    @VisibleForTesting
    void setContentResolver(ContentResolver contentResolver) {
        this.mContentResolver = contentResolver;
    }

    @VisibleForTesting
    void setSubId(SparseArray<String> sparseArray, Map<String, Integer> map) {
        this.mSubId2phone = sparseArray;
        this.mPhone2subId = map;
    }

    @VisibleForTesting
    void initUnknownSender() {
        this.mUnknownSenderThreadId = getOrCreateThreadId(null);
        sDefaultValuesSms.put("thread_id", Long.valueOf(this.mUnknownSenderThreadId));
        sDefaultValuesMms.put("thread_id", Long.valueOf(this.mUnknownSenderThreadId));
    }

    @Override
    public void onFullBackup(FullBackupDataOutput fullBackupDataOutput) throws Exception {
        Throwable th;
        Throwable th2;
        int i;
        SharedPreferences sharedPreferences = getSharedPreferences("backup_shared_prefs", 0);
        if (sharedPreferences.getLong("reset_quota_time", Long.MAX_VALUE) < System.currentTimeMillis()) {
            clearSharedPreferences();
        }
        this.mBytesOverQuota = sharedPreferences.getLong("backup_data_bytes", 0L) - sharedPreferences.getLong("backup_quota_bytes", Long.MAX_VALUE);
        if (this.mBytesOverQuota > 0) {
            this.mBytesOverQuota = (long) (this.mBytesOverQuota * 1.1d);
        }
        Cursor cursorQuery = this.mContentResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, null, null, "date ASC");
        try {
            Cursor cursorQuery2 = this.mContentResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION, null, null, "date ASC");
            if (cursorQuery != null) {
                try {
                    cursorQuery.moveToFirst();
                } catch (Throwable th3) {
                    try {
                        throw th3;
                    } catch (Throwable th4) {
                        th = th3;
                        th2 = th4;
                        if (cursorQuery2 != null) {
                            throw th2;
                        }
                        $closeResource(th, cursorQuery2);
                        throw th2;
                    }
                }
            }
            if (cursorQuery2 != null) {
                cursorQuery2.moveToFirst();
            }
            int i2 = 0;
            while (cursorQuery != null && !cursorQuery.isAfterLast() && cursorQuery2 != null && !cursorQuery2.isAfterLast()) {
                if (TimeUnit.MILLISECONDS.toSeconds(getMessageDate(cursorQuery)) < getMessageDate(cursorQuery2)) {
                    i = i2 + 1;
                    backupAll(fullBackupDataOutput, cursorQuery, String.format(Locale.US, "%06d_sms_backup", Integer.valueOf(i2)));
                } else {
                    i = i2 + 1;
                    backupAll(fullBackupDataOutput, cursorQuery2, String.format(Locale.US, "%06d_mms_backup", Integer.valueOf(i2)));
                }
                i2 = i;
            }
            while (cursorQuery != null && !cursorQuery.isAfterLast()) {
                backupAll(fullBackupDataOutput, cursorQuery, String.format(Locale.US, "%06d_sms_backup", Integer.valueOf(i2)));
                i2++;
            }
            while (cursorQuery2 != null) {
                if (cursorQuery2.isAfterLast()) {
                    break;
                }
                int i3 = i2 + 1;
                backupAll(fullBackupDataOutput, cursorQuery2, String.format(Locale.US, "%06d_mms_backup", Integer.valueOf(i2)));
                i2 = i3;
            }
            if (cursorQuery2 != null) {
                $closeResource(null, cursorQuery2);
            }
            this.mThreadArchived = new HashMap();
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @VisibleForTesting
    void clearSharedPreferences() {
        getSharedPreferences("backup_shared_prefs", 0).edit().remove("backup_data_bytes").remove("backup_quota_bytes").remove("reset_quota_time").apply();
    }

    private static long getMessageDate(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex("date"));
    }

    @Override
    public void onQuotaExceeded(long j, long j2) {
        SharedPreferences sharedPreferences = getSharedPreferences("backup_shared_prefs", 0);
        if (sharedPreferences.contains("backup_data_bytes") && sharedPreferences.contains("backup_quota_bytes")) {
            j = (long) (j + ((sharedPreferences.getLong("backup_data_bytes", 0L) - sharedPreferences.getLong("backup_quota_bytes", 0L)) * 1.1d));
        }
        sharedPreferences.edit().putLong("backup_data_bytes", j).putLong("backup_quota_bytes", j2).putLong("reset_quota_time", System.currentTimeMillis() + 2592000000L).apply();
    }

    private void backupAll(FullBackupDataOutput fullBackupDataOutput, Cursor cursor, String str) throws Exception {
        int iPutMmsMessagesToJson;
        if (cursor == null || cursor.isAfterLast()) {
            return;
        }
        JsonWriter jsonWriter = getJsonWriter(str);
        try {
            if (str.endsWith("_sms_backup")) {
                iPutMmsMessagesToJson = putSmsMessagesToJson(cursor, jsonWriter);
            } else {
                iPutMmsMessagesToJson = putMmsMessagesToJson(cursor, jsonWriter);
            }
            backupFile(iPutMmsMessagesToJson, str, fullBackupDataOutput);
        } finally {
            if (jsonWriter != null) {
                $closeResource(null, jsonWriter);
            }
        }
    }

    @VisibleForTesting
    int putMmsMessagesToJson(Cursor cursor, JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginArray();
        int iWriteMmsToWriter = 0;
        while (iWriteMmsToWriter < this.mMaxMsgPerFile && !cursor.isAfterLast()) {
            iWriteMmsToWriter += writeMmsToWriter(jsonWriter, cursor);
            cursor.moveToNext();
        }
        jsonWriter.endArray();
        return iWriteMmsToWriter;
    }

    @VisibleForTesting
    int putSmsMessagesToJson(Cursor cursor, JsonWriter jsonWriter) throws Exception {
        jsonWriter.beginArray();
        int i = 0;
        while (i < this.mMaxMsgPerFile && !cursor.isAfterLast()) {
            writeSmsToWriter(jsonWriter, cursor);
            i++;
            cursor.moveToNext();
        }
        jsonWriter.endArray();
        return i;
    }

    private void backupFile(int i, String str, FullBackupDataOutput fullBackupDataOutput) throws IOException {
        File file = new File(getFilesDir().getPath() + "/" + str);
        if (i > 0) {
            try {
                if (this.mBytesOverQuota > 0) {
                    this.mBytesOverQuota -= file.length();
                    return;
                }
                super.fullBackupFile(file, fullBackupDataOutput);
            } finally {
                file.delete();
            }
        }
    }

    public static class DeferredSmsMmsRestoreService extends IntentService {
        private final Comparator<File> mFileComparator;
        private TelephonyBackupAgent mTelephonyBackupAgent;
        private PowerManager.WakeLock mWakeLock;

        public DeferredSmsMmsRestoreService() {
            super("DeferredSmsMmsRestoreService");
            this.mFileComparator = new Comparator<File>() {
                @Override
                public int compare(File file, File file2) {
                    return file2.getName().compareTo(file.getName());
                }
            };
            setIntentRedelivery(true);
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            try {
                this.mWakeLock.acquire();
                boolean unused = TelephonyBackupAgent.sIsRestoring = true;
                File[] filesToRestore = getFilesToRestore(this);
                if (filesToRestore != null && filesToRestore.length != 0) {
                    Arrays.sort(filesToRestore, this.mFileComparator);
                    int length = filesToRestore.length;
                    int i = 0;
                    boolean z = false;
                    while (true) {
                        Throwable th = null;
                        if (i >= length) {
                            if (z) {
                                ProviderUtil.notifyIfNotDefaultSmsApp(null, null, this);
                            }
                            return;
                        }
                        File file = filesToRestore[i];
                        String name = file.getName();
                        try {
                            try {
                                FileInputStream fileInputStream = new FileInputStream(file);
                                try {
                                    this.mTelephonyBackupAgent.doRestoreFile(name, fileInputStream.getFD());
                                    try {
                                        fileInputStream.close();
                                        z = true;
                                    } catch (Exception e) {
                                        e = e;
                                        z = true;
                                        Log.e("DeferredSmsMmsRestoreService", "onHandleIntent", e);
                                    }
                                } catch (Throwable th2) {
                                    if (th != null) {
                                        try {
                                            fileInputStream.close();
                                        } catch (Throwable th3) {
                                            th.addSuppressed(th3);
                                        }
                                    } else {
                                        fileInputStream.close();
                                    }
                                    throw th2;
                                }
                            } finally {
                                file.delete();
                            }
                        } catch (Exception e2) {
                            e = e2;
                        }
                        i++;
                    }
                }
            } finally {
                boolean unused2 = TelephonyBackupAgent.sIsRestoring = false;
                this.mWakeLock.release();
            }
        }

        @Override
        public void onCreate() {
            super.onCreate();
            this.mTelephonyBackupAgent = new TelephonyBackupAgent();
            this.mTelephonyBackupAgent.attach(this);
            this.mTelephonyBackupAgent.onCreate();
            this.mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(1, "DeferredSmsMmsRestoreService");
        }

        @Override
        public void onDestroy() {
            if (this.mTelephonyBackupAgent != null) {
                this.mTelephonyBackupAgent.onDestroy();
                this.mTelephonyBackupAgent = null;
            }
            super.onDestroy();
        }

        static void startIfFilesExist(Context context) {
            File[] filesToRestore = getFilesToRestore(context);
            if (filesToRestore == null || filesToRestore.length == 0) {
                return;
            }
            context.startService(new Intent(context, (Class<?>) DeferredSmsMmsRestoreService.class));
        }

        private static File[] getFilesToRestore(Context context) {
            return context.getFilesDir().listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith("_sms_backup") || file.getName().endsWith("_mms_backup");
                }
            });
        }
    }

    @Override
    public void onRestoreFinished() {
        super.onRestoreFinished();
        DeferredSmsMmsRestoreService.startIfFilesExist(this);
    }

    private void doRestoreFile(String str, FileDescriptor fileDescriptor) throws Exception {
        JsonReader jsonReader = getJsonReader(fileDescriptor);
        try {
            if (str.endsWith("_sms_backup")) {
                putSmsMessagesToProvider(jsonReader);
            } else if (str.endsWith("_mms_backup")) {
                putMmsMessagesToProvider(jsonReader);
            }
        } finally {
            if (jsonReader != null) {
                $closeResource(null, jsonReader);
            }
        }
    }

    @VisibleForTesting
    void putSmsMessagesToProvider(JsonReader jsonReader) throws IOException {
        jsonReader.beginArray();
        int i = this.mMaxMsgPerFile;
        ContentValues[] contentValuesArr = new ContentValues[i];
        int i2 = 0;
        while (jsonReader.hasNext()) {
            ContentValues smsValuesFromReader = readSmsValuesFromReader(jsonReader);
            if (!doesSmsExist(smsValuesFromReader)) {
                int i3 = i2 + 1;
                contentValuesArr[i2 % i] = smsValuesFromReader;
                if (i3 % i == 0) {
                    this.mContentResolver.bulkInsert(Telephony.Sms.CONTENT_URI, contentValuesArr);
                }
                i2 = i3;
            }
        }
        int i4 = i2 % i;
        if (i4 > 0) {
            this.mContentResolver.bulkInsert(Telephony.Sms.CONTENT_URI, (ContentValues[]) Arrays.copyOf(contentValuesArr, i4));
        }
        jsonReader.endArray();
    }

    @VisibleForTesting
    void putMmsMessagesToProvider(JsonReader jsonReader) throws IOException {
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            Mms mmsFromReader = readMmsFromReader(jsonReader);
            if (!doesMmsExist(mmsFromReader)) {
                addMmsMessage(mmsFromReader);
            }
        }
    }

    private boolean doesSmsExist(ContentValues contentValues) throws Exception {
        boolean z = false;
        Cursor cursorQuery = this.mContentResolver.query(Telephony.Sms.CONTENT_URI, PROJECTION_ID, String.format(Locale.US, "%s = %d and %s = %s", "date", contentValues.getAsLong("date"), "body", DatabaseUtils.sqlEscapeString(contentValues.getAsString("body"))), null, null);
        try {
            if (cursorQuery != null) {
                if (cursorQuery.getCount() > 0) {
                    z = true;
                }
            }
            return z;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private boolean doesMmsExist(Mms mms) throws Exception {
        Cursor cursorQuery = this.mContentResolver.query(Telephony.Mms.CONTENT_URI, PROJECTION_ID, String.format(Locale.US, "%s = %d", "date", mms.values.getAsLong("date")), null, null);
        try {
            if (cursorQuery != null) {
                if (cursorQuery.moveToFirst()) {
                    do {
                        MmsBody mmsBody = getMmsBody(cursorQuery.getInt(0));
                        if (mmsBody != null && mmsBody.equals(mms.body)) {
                            return true;
                        }
                    } while (cursorQuery.moveToNext());
                }
            }
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return false;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private static String getNormalizedNumber(SubscriptionInfo subscriptionInfo) {
        if (subscriptionInfo == null) {
            return null;
        }
        return PhoneNumberUtils.formatNumberToE164(subscriptionInfo.getNumber(), subscriptionInfo.getCountryIso().toUpperCase(Locale.US));
    }

    private void writeSmsToWriter(JsonWriter jsonWriter, Cursor cursor) throws Exception {
        jsonWriter.beginObject();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String columnName = cursor.getColumnName(i);
            String string = cursor.getString(i);
            if (string != null) {
                byte b = -1;
                int iHashCode = columnName.hashCode();
                if (iHashCode != -1562235024) {
                    if (iHashCode != -891548806) {
                        if (iHashCode == 94650 && columnName.equals("_id")) {
                            b = 2;
                        }
                    } else if (columnName.equals("sub_id")) {
                        b = 0;
                    }
                } else if (columnName.equals("thread_id")) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                        String str = this.mSubId2phone.get(cursor.getInt(i));
                        if (str != null) {
                            jsonWriter.name("self_phone").value(str);
                        }
                        break;
                    case 1:
                        handleThreadId(jsonWriter, cursor.getLong(i));
                        break;
                    case 2:
                        break;
                    default:
                        jsonWriter.name(columnName).value(string);
                        break;
                }
            }
        }
        jsonWriter.endObject();
    }

    private void handleThreadId(JsonWriter jsonWriter, long j) throws Exception {
        List<String> recipientsByThread = getRecipientsByThread(j);
        if (recipientsByThread == null || recipientsByThread.isEmpty()) {
            return;
        }
        writeRecipientsToWriter(jsonWriter.name("recipients"), recipientsByThread);
        if (!this.mThreadArchived.containsKey(Long.valueOf(j))) {
            boolean zIsThreadArchived = isThreadArchived(j);
            if (zIsThreadArchived) {
                jsonWriter.name("archived").value(true);
            }
            this.mThreadArchived.put(Long.valueOf(j), Boolean.valueOf(zIsThreadArchived));
        }
    }

    private boolean isThreadArchived(long j) throws Exception {
        Uri.Builder builderBuildUpon = Telephony.Threads.CONTENT_URI.buildUpon();
        builderBuildUpon.appendPath(String.valueOf(j)).appendPath("recipients");
        Cursor cursorQuery = getContentResolver().query(builderBuildUpon.build(), THREAD_ARCHIVED_PROJECTION, null, null, null);
        try {
            if (cursorQuery != null) {
                if (cursorQuery.moveToFirst()) {
                    return cursorQuery.getInt(THREAD_ARCHIVED_IDX) == 1;
                }
            }
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return false;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private static void writeRecipientsToWriter(JsonWriter jsonWriter, List<String> list) throws IOException {
        jsonWriter.beginArray();
        if (list != null) {
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                jsonWriter.value(it.next());
            }
        }
        jsonWriter.endArray();
    }

    private ContentValues readSmsValuesFromReader(JsonReader jsonReader) throws IOException {
        String strNextName;
        ContentValues contentValues = new ContentValues(sDefaultValuesSms.size() + 6);
        contentValues.putAll(sDefaultValuesSms);
        jsonReader.beginObject();
        long orCreateThreadId = -1;
        boolean zNextBoolean = false;
        while (jsonReader.hasNext()) {
            strNextName = jsonReader.nextName();
            switch (strNextName) {
                case "body":
                case "date":
                case "date_sent":
                case "status":
                case "type":
                case "subject":
                case "address":
                case "read":
                    contentValues.put(strNextName, jsonReader.nextString());
                    break;
                case "recipients":
                    orCreateThreadId = getOrCreateThreadId(getRecipients(jsonReader));
                    contentValues.put("thread_id", Long.valueOf(orCreateThreadId));
                    break;
                case "archived":
                    zNextBoolean = jsonReader.nextBoolean();
                    break;
                case "self_phone":
                    String strNextString = jsonReader.nextString();
                    if (!this.mPhone2subId.containsKey(strNextString)) {
                        break;
                    } else {
                        contentValues.put("sub_id", this.mPhone2subId.get(strNextString));
                        break;
                    }
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();
        archiveThread(orCreateThreadId, zNextBoolean);
        return contentValues;
    }

    private static Set<String> getRecipients(JsonReader jsonReader) throws IOException {
        ArraySet arraySet = new ArraySet();
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            arraySet.add(jsonReader.nextString());
        }
        jsonReader.endArray();
        return arraySet;
    }

    private int writeMmsToWriter(JsonWriter jsonWriter, Cursor cursor) throws Exception {
        int i = cursor.getInt(0);
        MmsBody mmsBody = getMmsBody(i);
        if (mmsBody == null || mmsBody.text == null) {
            return 0;
        }
        jsonWriter.beginObject();
        boolean z = true;
        for (int i2 = 0; i2 < cursor.getColumnCount(); i2++) {
            String columnName = cursor.getColumnName(i2);
            String string = cursor.getString(i2);
            if (string != null) {
                switch (columnName) {
                    case "sub_id":
                        String str = this.mSubId2phone.get(cursor.getInt(i2));
                        if (str != null) {
                            jsonWriter.name("self_phone").value(str);
                            break;
                        } else {
                            continue;
                            break;
                        }
                        break;
                    case "thread_id":
                        handleThreadId(jsonWriter, cursor.getLong(i2));
                        continue;
                        break;
                    case "sub":
                        z = false;
                        break;
                }
                jsonWriter.name(columnName).value(string);
            }
        }
        writeMmsAddresses(jsonWriter.name("mms_addresses"), i);
        jsonWriter.name("mms_body").value(mmsBody.text);
        jsonWriter.name("mms_charset").value(mmsBody.charSet);
        if (!z) {
            writeStringToWriter(jsonWriter, cursor, "sub_cs");
        }
        jsonWriter.endObject();
        return 1;
    }

    private Mms readMmsFromReader(JsonReader jsonReader) throws IOException {
        String strNextName;
        String strNextString = null;
        Mms mms = new Mms();
        mms.values = new ContentValues(sDefaultValuesMms.size() + 5);
        mms.values.putAll(sDefaultValuesMms);
        jsonReader.beginObject();
        int i = 0;
        long orCreateThreadId = -1;
        int iNextInt = 106;
        boolean zNextBoolean = false;
        while (true) {
            if (jsonReader.hasNext()) {
                strNextName = jsonReader.nextName();
                switch (strNextName) {
                    case "self_phone":
                        String strNextString2 = jsonReader.nextString();
                        if (!this.mPhone2subId.containsKey(strNextString2)) {
                            break;
                        } else {
                            mms.values.put("sub_id", this.mPhone2subId.get(strNextString2));
                            break;
                        }
                        break;
                    case "mms_addresses":
                        getMmsAddressesFromReader(jsonReader, mms);
                        break;
                    case "attachments":
                        getMmsAttachmentsFromReader(jsonReader, mms);
                        break;
                    case "smil":
                        mms.smil = jsonReader.nextString();
                        break;
                    case "mms_body":
                        strNextString = jsonReader.nextString();
                        break;
                    case "mms_charset":
                        iNextInt = jsonReader.nextInt();
                        break;
                    case "recipients":
                        orCreateThreadId = getOrCreateThreadId(getRecipients(jsonReader));
                        mms.values.put("thread_id", Long.valueOf(orCreateThreadId));
                        break;
                    case "archived":
                        zNextBoolean = jsonReader.nextBoolean();
                        break;
                    case "sub":
                    case "sub_cs":
                    case "date":
                    case "date_sent":
                    case "m_type":
                    case "v":
                    case "msg_box":
                    case "ct_l":
                    case "tr_id":
                    case "read":
                        mms.values.put(strNextName, jsonReader.nextString());
                        break;
                    default:
                        jsonReader.skipValue();
                        break;
                }
            } else {
                jsonReader.endObject();
                if (strNextString != null) {
                    mms.body = new MmsBody(strNextString, iNextInt);
                }
                ContentValues contentValues = mms.values;
                if ((mms.attachments == null || mms.attachments.size() == 0) && strNextString != null) {
                    i = 1;
                }
                contentValues.put("text_only", Integer.valueOf(i));
                if (mms.values.get("sub") != null && mms.values.get("sub_cs") == null) {
                    mms.values.put("sub_cs", (Integer) 106);
                }
                archiveThread(orCreateThreadId, zNextBoolean);
                return mms;
            }
        }
    }

    private void archiveThread(long j, boolean z) {
        if (j < 0 || !z) {
            return;
        }
        ContentValues contentValues = new ContentValues(1);
        contentValues.put("archived", (Integer) 1);
        this.mContentResolver.update(Telephony.Threads.CONTENT_URI, contentValues, "_id=?", new String[]{Long.toString(j)});
    }

    private MmsBody getMmsBody(int i) throws Exception {
        String str;
        Cursor cursorQuery = this.mContentResolver.query(Telephony.Mms.CONTENT_URI.buildUpon().appendPath(String.valueOf(i)).appendPath("part").build(), MMS_TEXT_PROJECTION, "ct=?", new String[]{"text/plain"}, "_id ASC");
        int i2 = 0;
        try {
            if (cursorQuery != null) {
                if (cursorQuery.moveToFirst()) {
                    int i3 = 0;
                    str = null;
                    do {
                        String string = cursorQuery.getString(0);
                        if (string != null) {
                            if (str != null) {
                                string = str.concat(string);
                            }
                            i3 = cursorQuery.getInt(1);
                            str = string;
                        }
                    } while (cursorQuery.moveToNext());
                    i2 = i3;
                } else {
                    str = null;
                }
            }
            if (str == null) {
                return null;
            }
            return new MmsBody(str, i2);
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private void writeMmsAddresses(JsonWriter jsonWriter, int i) throws Exception {
        Uri.Builder builderBuildUpon = Telephony.Mms.CONTENT_URI.buildUpon();
        builderBuildUpon.appendPath(String.valueOf(i)).appendPath("addr");
        Uri uriBuild = builderBuildUpon.build();
        jsonWriter.beginArray();
        Cursor cursorQuery = this.mContentResolver.query(uriBuild, MMS_ADDR_PROJECTION, null, null, "_id ASC");
        try {
            if (cursorQuery != null) {
                if (cursorQuery.moveToFirst()) {
                    do {
                        if (cursorQuery.getString(cursorQuery.getColumnIndex("address")) != null) {
                            jsonWriter.beginObject();
                            writeIntToWriter(jsonWriter, cursorQuery, "type");
                            writeStringToWriter(jsonWriter, cursorQuery, "address");
                            writeIntToWriter(jsonWriter, cursorQuery, "charset");
                            jsonWriter.endObject();
                        }
                    } while (cursorQuery.moveToNext());
                }
            }
            jsonWriter.endArray();
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private static void getMmsAddressesFromReader(JsonReader jsonReader, Mms mms) throws IOException {
        mms.addresses = new ArrayList();
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            jsonReader.beginObject();
            ContentValues contentValues = new ContentValues(sDefaultValuesAddr);
            while (jsonReader.hasNext()) {
                String strNextName = jsonReader.nextName();
                byte b = -1;
                int iHashCode = strNextName.hashCode();
                if (iHashCode != -1147692044) {
                    if (iHashCode != 3575610) {
                        if (iHashCode == 739074380 && strNextName.equals("charset")) {
                            b = 1;
                        }
                    } else if (strNextName.equals("type")) {
                        b = 0;
                    }
                } else if (strNextName.equals("address")) {
                    b = 2;
                }
                switch (b) {
                    case 0:
                    case 1:
                        contentValues.put(strNextName, Integer.valueOf(jsonReader.nextInt()));
                        break;
                    case 2:
                        contentValues.put(strNextName, jsonReader.nextString());
                        break;
                    default:
                        jsonReader.skipValue();
                        break;
                }
            }
            jsonReader.endObject();
            if (contentValues.containsKey("address")) {
                mms.addresses.add(contentValues);
            }
        }
        jsonReader.endArray();
    }

    private static void getMmsAttachmentsFromReader(JsonReader jsonReader, Mms mms) throws IOException {
        mms.attachments = new ArrayList();
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            jsonReader.beginObject();
            ContentValues contentValues = new ContentValues(sDefaultValuesAttachments);
            while (jsonReader.hasNext()) {
                String strNextName = jsonReader.nextName();
                byte b = -1;
                int iHashCode = strNextName.hashCode();
                if (iHashCode != -734768633) {
                    if (iHashCode == -196041627 && strNextName.equals("mime_type")) {
                        b = 0;
                    }
                } else if (strNextName.equals("filename")) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                    case 1:
                        contentValues.put(strNextName, jsonReader.nextString());
                        break;
                    default:
                        jsonReader.skipValue();
                        break;
                }
            }
            jsonReader.endObject();
            if (contentValues.containsKey("filename")) {
                mms.attachments.add(contentValues);
            }
        }
        jsonReader.endArray();
    }

    private void addMmsMessage(Mms mms) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        Uri uriBuild = Telephony.Mms.CONTENT_URI.buildUpon().appendPath(String.valueOf(jCurrentTimeMillis)).appendPath("part").build();
        String str = String.format(Locale.US, "text.%06d.txt", 0);
        String str2 = TextUtils.isEmpty(mms.smil) ? String.format(sSmilTextOnly, String.format(sSmilTextPart, str)) : mms.smil;
        ContentValues contentValues = new ContentValues(7);
        contentValues.put("mid", Long.valueOf(jCurrentTimeMillis));
        contentValues.put("seq", (Integer) (-1));
        contentValues.put("ct", "application/smil");
        contentValues.put("name", "smil.xml");
        contentValues.put("cid", "<smil>");
        contentValues.put("cl", "smil.xml");
        contentValues.put("text", str2);
        if (this.mContentResolver.insert(uriBuild, contentValues) == null) {
            return;
        }
        ContentValues contentValues2 = new ContentValues(8);
        contentValues2.put("mid", Long.valueOf(jCurrentTimeMillis));
        contentValues2.put("seq", (Integer) 0);
        contentValues2.put("ct", "text/plain");
        contentValues2.put("name", str);
        contentValues2.put("cid", "<" + str + ">");
        contentValues2.put("cl", str);
        contentValues2.put("chset", Integer.valueOf(mms.body.charSet));
        contentValues2.put("text", mms.body.text);
        if (this.mContentResolver.insert(uriBuild, contentValues2) == null) {
            return;
        }
        if (mms.attachments != null) {
            for (ContentValues contentValues3 : mms.attachments) {
                ContentValues contentValues4 = new ContentValues(6);
                contentValues4.put("mid", Long.valueOf(jCurrentTimeMillis));
                contentValues4.put("seq", (Integer) 0);
                contentValues4.put("ct", contentValues3.getAsString("mime_type"));
                String asString = contentValues3.getAsString("filename");
                contentValues4.put("cid", "<" + asString + ">");
                contentValues4.put("cl", asString);
                contentValues4.put("_data", getDataDir() + ATTACHMENT_DATA_PATH + asString);
                if (this.mContentResolver.insert(uriBuild, contentValues4) == null) {
                    return;
                }
            }
        }
        Uri uriInsert = this.mContentResolver.insert(Telephony.Mms.CONTENT_URI, mms.values);
        if (uriInsert == null) {
            return;
        }
        long id = ContentUris.parseId(uriInsert);
        ContentValues contentValues5 = new ContentValues(1);
        contentValues5.put("mid", Long.valueOf(id));
        this.mContentResolver.update(uriBuild, contentValues5, null, null);
        Uri uriWithAppendedPath = Uri.withAppendedPath(uriInsert, "addr");
        Iterator<ContentValues> it = mms.addresses.iterator();
        while (it.hasNext()) {
            ContentValues contentValues6 = new ContentValues(it.next());
            contentValues6.put("msg_id", Long.valueOf(id));
            this.mContentResolver.insert(uriWithAppendedPath, contentValues6);
        }
    }

    private static final class MmsBody {
        public int charSet;
        public String text;

        public MmsBody(String str, int i) {
            this.text = str;
            this.charSet = i;
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof MmsBody)) {
                return false;
            }
            MmsBody mmsBody = (MmsBody) obj;
            return this.text.equals(mmsBody.text) && this.charSet == mmsBody.charSet;
        }

        public String toString() {
            return "Text:" + this.text + " charSet:" + this.charSet;
        }
    }

    private static final class Mms {
        public List<ContentValues> addresses;
        public List<ContentValues> attachments;
        public MmsBody body;
        public String smil;
        public ContentValues values;

        private Mms() {
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Values:");
            sb.append(this.values.toString());
            sb.append("\nRecipients:");
            sb.append(this.addresses.toString());
            sb.append("\nAttachments:");
            sb.append(this.attachments == null ? "none" : this.attachments.toString());
            sb.append("\nBody:");
            sb.append(this.body);
            return sb.toString();
        }
    }

    private JsonWriter getJsonWriter(String str) throws IOException {
        return new JsonWriter(new BufferedWriter(new OutputStreamWriter(new DeflaterOutputStream(openFileOutput(str, 0)), "UTF-8"), 32768));
    }

    private static JsonReader getJsonReader(FileDescriptor fileDescriptor) throws IOException {
        return new JsonReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(fileDescriptor)), "UTF-8"));
    }

    private static void writeStringToWriter(JsonWriter jsonWriter, Cursor cursor, String str) throws IOException {
        String string = cursor.getString(cursor.getColumnIndex(str));
        if (string != null) {
            jsonWriter.name(str).value(string);
        }
    }

    private static void writeIntToWriter(JsonWriter jsonWriter, Cursor cursor, String str) throws IOException {
        int i = cursor.getInt(cursor.getColumnIndex(str));
        if (i != 0) {
            jsonWriter.name(str).value(i);
        }
    }

    private long getOrCreateThreadId(Set<String> set) {
        if (set == null) {
            set = new ArraySet<>();
        }
        if (set.isEmpty()) {
            set.add(UNKNOWN_SENDER);
        }
        if (this.mCacheGetOrCreateThreadId == null) {
            this.mCacheGetOrCreateThreadId = new HashMap();
        }
        if (!this.mCacheGetOrCreateThreadId.containsKey(set)) {
            long orCreateThreadId = this.mUnknownSenderThreadId;
            try {
                orCreateThreadId = Telephony.Threads.getOrCreateThreadId(this, set);
            } catch (RuntimeException e) {
            }
            this.mCacheGetOrCreateThreadId.put(set, Long.valueOf(orCreateThreadId));
            return orCreateThreadId;
        }
        return this.mCacheGetOrCreateThreadId.get(set).longValue();
    }

    private List<String> getRecipientsByThread(long j) {
        if (this.mCacheRecipientsByThread == null) {
            this.mCacheRecipientsByThread = new HashMap();
        }
        if (!this.mCacheRecipientsByThread.containsKey(Long.valueOf(j))) {
            String rawRecipientIdsForThread = getRawRecipientIdsForThread(j);
            if (!TextUtils.isEmpty(rawRecipientIdsForThread)) {
                this.mCacheRecipientsByThread.put(Long.valueOf(j), getAddresses(rawRecipientIdsForThread));
            } else {
                this.mCacheRecipientsByThread.put(Long.valueOf(j), new ArrayList());
            }
        }
        return this.mCacheRecipientsByThread.get(Long.valueOf(j));
    }

    private String getRawRecipientIdsForThread(long j) {
        Cursor cursorQuery;
        if (j > 0 && (cursorQuery = this.mContentResolver.query(ALL_THREADS_URI, SMS_RECIPIENTS_PROJECTION, "_id=?", new String[]{String.valueOf(j)}, null)) != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    return cursorQuery.getString(1);
                }
            } finally {
                cursorQuery.close();
            }
        }
        return null;
    }

    private List<String> getAddresses(String str) {
        Cursor cursorQuery;
        ArrayList arrayList = new ArrayList();
        for (String str2 : str.split(" ")) {
            try {
                long j = Long.parseLong(str2);
                if (j < 0) {
                    continue;
                } else {
                    try {
                        cursorQuery = this.mContentResolver.query(ContentUris.withAppendedId(SINGLE_CANONICAL_ADDRESS_URI, j), null, null, null, null);
                    } catch (Exception e) {
                        cursorQuery = null;
                    }
                    if (cursorQuery != null) {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                String string = cursorQuery.getString(0);
                                if (!TextUtils.isEmpty(string)) {
                                    arrayList.add(string);
                                }
                            }
                        } finally {
                            cursorQuery.close();
                        }
                    } else {
                        continue;
                    }
                }
            } catch (NumberFormatException e2) {
            }
        }
        arrayList.isEmpty();
        return arrayList;
    }

    @Override
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException {
    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
    }

    public static boolean getIsRestoring() {
        return sIsRestoring;
    }
}
