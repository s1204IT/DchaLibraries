package com.mediatek.server.cta.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionRecords;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.StrictMode;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.mediatek.cta.CtaUtils;
import com.mediatek.internal.R;
import com.mediatek.server.cta.CtaPermsController;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PermRecordsController {
    static final boolean DEBUG_WRITEFILE = false;
    private static final String KEY_PERM_NAME = "PERM_NAME";
    private static final String KEY_REQUEST_TIME = "REQUEST_TIME";
    private static final String KEY_UID = "UID";
    private static final int MSG_REPORT_PERM_RECORDS = 1;
    static int RECORDS_LIMIT = 0;
    static final String TAG = "PermRecordsController";
    static final long WRITE_DELAY = 1800000;
    Context mContext;
    PermRecordsHandler mHandler;
    private BroadcastReceiver mIntentReceiver;
    boolean mWriteScheduled;
    private final SparseArray<UserData> mUserDatas = new SparseArray<>();
    final Runnable mWriteRunner = new Runnable() {
        @Override
        public void run() {
            synchronized (PermRecordsController.this) {
                PermRecordsController.this.mWriteScheduled = false;
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voidArr) {
                        PermRecordsController.this.writeState();
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
            }
        }
    };
    final AtomicFile mFile = new AtomicFile(new File(new File(Environment.getDataDirectory(), "system"), "permission_records.xml"));

    class PermRecordsHandler extends Handler {
        PermRecordsHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    PermRecordsController.this.handlePermRequestUsage(message.getData());
                    break;
            }
        }
    }

    private static final class UserData {
        public ArrayMap<String, PackageData> mPackageDatas = new ArrayMap<>();
        public final int userId;

        public UserData(int i) {
            this.userId = i;
        }

        public void addPackageData(PackageData packageData) {
            this.mPackageDatas.put(packageData.packageName, packageData);
        }
    }

    private static final class PackageData {
        public ArrayMap<String, PermissionRecord> mRecords = new ArrayMap<>();
        public final String packageName;

        public PackageData(String str) {
            this.packageName = str;
        }

        public void addPermissionRecord(PermissionRecord permissionRecord) {
            this.mRecords.put(permissionRecord.permission, permissionRecord);
        }
    }

    private static final class PermissionRecord {
        public final String permission;
        public List<Long> requestTimes = new ArrayList();

        public PermissionRecord(String str) {
            this.permission = str;
        }

        public void addUsageTime(long j) {
            this.requestTimes.add(Long.valueOf(j));
            if (this.requestTimes.size() > PermRecordsController.RECORDS_LIMIT) {
                this.requestTimes.remove(0);
            }
        }
    }

    public PermRecordsController(Context context) {
        this.mContext = context;
        RECORDS_LIMIT = this.mContext.getResources().getInteger(R.integer.permission_records_limit);
        setupHandler();
        readState();
    }

    public void systemReady() {
        if (CtaPermsController.DEBUG) {
            Slog.d(TAG, "systemReady()");
        }
        setupReceiver();
    }

    private void setupHandler() {
        HandlerThread handlerThread = new HandlerThread(TAG, -2) {
            @Override
            public void run() {
                Process.setCanSelfBackground(false);
                if (StrictMode.conditionallyEnableDebugLogging()) {
                    Slog.i(PermRecordsController.TAG, "Enabled StrictMode logging for " + getName() + " looper.");
                }
                super.run();
            }
        };
        handlerThread.start();
        this.mHandler = new PermRecordsHandler(handlerThread.getLooper());
    }

    private void setupReceiver() {
        if (CtaPermsController.DEBUG) {
            Slog.d(TAG, "setupReceiver()");
        }
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                int intExtra2 = intent.getIntExtra("android.intent.extra.UID", -1);
                Uri data = intent.getData();
                String schemeSpecificPart = data != null ? data.getSchemeSpecificPart() : null;
                boolean booleanExtra = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                if (CtaPermsController.DEBUG) {
                    Slog.d(PermRecordsController.TAG, "Receive intent action = " + action + ", user = " + intExtra + ", appUid = " + intExtra2 + ", appName = " + schemeSpecificPart + ", replacing = " + booleanExtra);
                }
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    PermRecordsController.this.onUserRemoved(intExtra);
                } else if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !booleanExtra) {
                    PermRecordsController.this.onAppRemoved(intExtra, intExtra2, schemeSpecificPart);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter2, null, null);
    }

    private void onUserRemoved(int i) {
        if (CtaPermsController.DEBUG) {
            Slog.d(TAG, "onUserRemoved() user = " + i);
        }
        synchronized (this.mUserDatas) {
            if (this.mUserDatas.get(i) != null) {
                this.mUserDatas.remove(i);
            }
        }
    }

    private void onAppRemoved(int i, int i2, String str) {
        if (CtaPermsController.DEBUG) {
            Slog.d(TAG, "onAppRemoved() user = " + i + ", appUid = " + i2 + ", pkgName = " + str);
        }
        synchronized (this.mUserDatas) {
            UserData userData = this.mUserDatas.get(i);
            if (userData != null) {
                if (userData.mPackageDatas.get(str) != null) {
                    userData.mPackageDatas.remove(str);
                }
            }
        }
    }

    public void reportPermRequestUsage(String str, int i) {
        if (!CtaUtils.isCtaSupported()) {
            return;
        }
        if (TextUtils.isEmpty(str)) {
            Slog.w(TAG, "reportPermRequestUsage() permName is null; ignoring");
            return;
        }
        if (!CtaUtils.isCtaMonitoredPerms(str)) {
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (CtaPermsController.DEBUG) {
            Slog.d(TAG, "reportPermRequestUsage() permName = " + str + ", uid = " + i + ", requestTime = " + jCurrentTimeMillis);
        }
        Message messageObtain = Message.obtain();
        messageObtain.what = 1;
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PERM_NAME, str);
        bundle.putInt(KEY_UID, i);
        bundle.putLong(KEY_REQUEST_TIME, jCurrentTimeMillis);
        messageObtain.setData(bundle);
        this.mHandler.sendMessage(messageObtain);
    }

    private void handlePermRequestUsage(Bundle bundle) {
        String string = bundle.getString(KEY_PERM_NAME);
        int i = bundle.getInt(KEY_UID);
        long j = bundle.getLong(KEY_REQUEST_TIME);
        String[] packagesForUid = this.mContext.getPackageManager().getPackagesForUid(i);
        if (packagesForUid == null || packagesForUid.length == 0) {
            Slog.w(TAG, "handlePermRequestUsage() packages for uid = " + i + " is null; ignoring");
            return;
        }
        int userId = UserHandle.getUserId(i);
        for (String str : packagesForUid) {
            if (isPackageRequestingPermission(str, string, userId)) {
                addPermissionRecords(userId, string, str, j, true);
            }
        }
    }

    private boolean isPackageRequestingPermission(String str, String str2, int i) {
        try {
            PackageInfo packageInfoAsUser = this.mContext.getPackageManager().getPackageInfoAsUser(str, 4096, i);
            int length = packageInfoAsUser.requestedPermissions == null ? 0 : packageInfoAsUser.requestedPermissions.length;
            for (int i2 = 0; i2 < length; i2++) {
                if (str2.equals(packageInfoAsUser.requestedPermissions[i2])) {
                    return true;
                }
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Couldn't retrieve permissions for package:" + str);
            return false;
        }
    }

    public void addPermissionRecords(int i, String str, String str2, long j, boolean z) {
        UserData userData;
        PackageData packageData;
        if (CtaPermsController.DEBUG) {
            Slog.d(TAG, "addPermissionRecords userId = " + i + ", pkgName = " + str2 + ", permName = " + str + ", time = " + j + ", doWrite = " + z);
        }
        synchronized (this.mUserDatas) {
            UserData userData2 = this.mUserDatas.get(i);
            if (userData2 != null) {
                userData = userData2;
            } else {
                UserData userData3 = new UserData(i);
                this.mUserDatas.put(i, userData3);
                userData = userData3;
            }
            PackageData packageData2 = userData.mPackageDatas.get(str2);
            if (packageData2 != null) {
                packageData = packageData2;
            } else {
                PackageData packageData3 = new PackageData(str2);
                userData.addPackageData(packageData3);
                packageData = packageData3;
            }
            PermissionRecord permissionRecord = packageData.mRecords.get(str);
            if (permissionRecord == null) {
                permissionRecord = new PermissionRecord(str);
                packageData.addPermissionRecord(permissionRecord);
            }
            permissionRecord.addUsageTime(j);
        }
        if (z) {
            scheduleWriteLocked();
        }
    }

    public List<String> getPermRecordPkgs() {
        int callUserId = getCallUserId();
        synchronized (this.mUserDatas) {
            UserData userData = this.mUserDatas.get(callUserId);
            if (userData == null) {
                Slog.w(TAG, "getPermRecordPkgs(), no permission records for userId = " + callUserId);
                return null;
            }
            return new ArrayList(userData.mPackageDatas.keySet());
        }
    }

    public List<String> getPermRecordPerms(String str) {
        int callUserId = getCallUserId();
        synchronized (this.mUserDatas) {
            UserData userData = this.mUserDatas.get(callUserId);
            if (userData == null) {
                Slog.w(TAG, "getPermRecordPerms(), no permission records for userId = " + callUserId);
                return null;
            }
            PackageData packageData = userData.mPackageDatas.get(str);
            if (packageData == null) {
                Slog.w(TAG, "getPermRecordPerms(), no permission records for userId = " + callUserId + ", packageName = " + str);
                return null;
            }
            return new ArrayList(packageData.mRecords.keySet());
        }
    }

    public PermissionRecords getPermRecords(String str, String str2) {
        int callUserId = getCallUserId();
        synchronized (this.mUserDatas) {
            UserData userData = this.mUserDatas.get(callUserId);
            if (userData == null) {
                Slog.w(TAG, "getPermRecords(), no permission records for userId = " + callUserId);
                return null;
            }
            PackageData packageData = userData.mPackageDatas.get(str);
            if (packageData == null) {
                Slog.w(TAG, "getPermRecords(), no permission records for userId = " + callUserId + ", packageName = " + str);
                return null;
            }
            PermissionRecord permissionRecord = packageData.mRecords.get(str2);
            if (permissionRecord == null) {
                Slog.w(TAG, "getPermRecords(), no permission records for userId = " + callUserId + ", packageName = " + str + ", permName = " + str2);
                return null;
            }
            return new PermissionRecords(str, str2, new ArrayList(permissionRecord.requestTimes));
        }
    }

    private int getCallUserId() {
        return UserHandle.getUserId(Binder.getCallingUid());
    }

    private void scheduleWriteLocked() {
        if (!this.mWriteScheduled) {
            this.mWriteScheduled = true;
            this.mHandler.postDelayed(this.mWriteRunner, WRITE_DELAY);
        }
    }

    public void shutdown() {
        boolean z = false;
        Slog.d(TAG, "Writing data to file before shutdown...");
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages(null);
        }
        synchronized (this) {
            if (this.mWriteScheduled) {
                this.mWriteScheduled = false;
                z = true;
            }
        }
        if (z) {
            writeState();
        }
    }

    void readState() {
        XmlPullParser xmlPullParserNewPullParser;
        int next;
        Slog.d(TAG, "readState() BEGIN");
        synchronized (this.mFile) {
            synchronized (this) {
                try {
                    FileInputStream fileInputStreamOpenRead = this.mFile.openRead();
                    try {
                        this.mUserDatas.clear();
                        try {
                            try {
                                try {
                                    try {
                                        try {
                                            xmlPullParserNewPullParser = Xml.newPullParser();
                                            xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                                            do {
                                                next = xmlPullParserNewPullParser.next();
                                                if (next == 2) {
                                                    break;
                                                }
                                            } while (next != 1);
                                        } catch (NullPointerException e) {
                                            Slog.w(TAG, "Failed parsing " + e);
                                            Slog.w(TAG, "readState() fails");
                                            this.mUserDatas.clear();
                                            if (fileInputStreamOpenRead != null) {
                                                try {
                                                    fileInputStreamOpenRead.close();
                                                } catch (IOException e2) {
                                                }
                                            }
                                        }
                                    } catch (IllegalStateException e3) {
                                        Slog.w(TAG, "Failed parsing " + e3);
                                        Slog.w(TAG, "readState() fails");
                                        this.mUserDatas.clear();
                                        if (fileInputStreamOpenRead != null) {
                                            try {
                                                fileInputStreamOpenRead.close();
                                            } catch (IOException e4) {
                                            }
                                        }
                                    }
                                } catch (XmlPullParserException e5) {
                                    Slog.w(TAG, "Failed parsing " + e5);
                                    Slog.w(TAG, "readState() fails");
                                    this.mUserDatas.clear();
                                    if (fileInputStreamOpenRead != null) {
                                        try {
                                            fileInputStreamOpenRead.close();
                                        } catch (IOException e6) {
                                        }
                                    }
                                }
                            } catch (IndexOutOfBoundsException e7) {
                                Slog.w(TAG, "Failed parsing " + e7);
                                Slog.w(TAG, "readState() fails");
                                this.mUserDatas.clear();
                                if (fileInputStreamOpenRead != null) {
                                    try {
                                        fileInputStreamOpenRead.close();
                                    } catch (IOException e8) {
                                    }
                                }
                            }
                        } catch (IOException e9) {
                            Slog.w(TAG, "Failed parsing " + e9);
                            Slog.w(TAG, "readState() fails");
                            this.mUserDatas.clear();
                            if (fileInputStreamOpenRead != null) {
                                try {
                                    fileInputStreamOpenRead.close();
                                } catch (IOException e10) {
                                }
                            }
                        } catch (NumberFormatException e11) {
                            Slog.w(TAG, "Failed parsing " + e11);
                            Slog.w(TAG, "readState() fails");
                            this.mUserDatas.clear();
                            if (fileInputStreamOpenRead != null) {
                                try {
                                    fileInputStreamOpenRead.close();
                                } catch (IOException e12) {
                                }
                            }
                        }
                        if (next != 2) {
                            throw new IllegalStateException("no start tag found");
                        }
                        int depth = xmlPullParserNewPullParser.getDepth();
                        while (true) {
                            int next2 = xmlPullParserNewPullParser.next();
                            if (next2 == 1 || (next2 == 3 && xmlPullParserNewPullParser.getDepth() <= depth)) {
                                break;
                            }
                            if (next2 != 3 && next2 != 4) {
                                if (xmlPullParserNewPullParser.getName().equals("userId")) {
                                    readUserId(xmlPullParserNewPullParser);
                                } else {
                                    Slog.w(TAG, "Unknown element under <perm-records>: " + xmlPullParserNewPullParser.getName());
                                    XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                }
                            }
                        }
                        if (fileInputStreamOpenRead != null) {
                            try {
                                fileInputStreamOpenRead.close();
                            } catch (IOException e13) {
                            }
                        }
                    } catch (Throwable th) {
                        Slog.w(TAG, "readState() fails");
                        this.mUserDatas.clear();
                        if (fileInputStreamOpenRead != null) {
                            try {
                                fileInputStreamOpenRead.close();
                            } catch (IOException e14) {
                            }
                        }
                        throw th;
                    }
                } catch (FileNotFoundException e15) {
                    Slog.i(TAG, "No existing permission records " + this.mFile.getBaseFile() + "; starting empty");
                    return;
                }
            }
        }
        Slog.d(TAG, "readState() END");
    }

    void readUserId(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException, NumberFormatException {
        int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "n"));
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4 && xmlPullParser.getName().equals("pkg")) {
                        readPackage(xmlPullParser, i);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readPackage(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException, NumberFormatException {
        String attributeValue = xmlPullParser.getAttributeValue(null, "n");
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4 && xmlPullParser.getName().equals("perm")) {
                        readPermission(xmlPullParser, i, attributeValue);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readPermission(XmlPullParser xmlPullParser, int i, String str) throws XmlPullParserException, IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, "n");
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4 && xmlPullParser.getName().equals("time")) {
                        addPermissionRecords(i, attributeValue, str, Long.parseLong(xmlPullParser.getAttributeValue(null, "n")), false);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void writeState() {
        Slog.d(TAG, "writeState() BEGIN");
        SparseArray<UserData> sparseArray = new SparseArray<>();
        copyUserDatas(sparseArray);
        synchronized (this.mFile) {
            synchronized (this) {
                try {
                    FileOutputStream fileOutputStreamStartWrite = this.mFile.startWrite();
                    try {
                        FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                        fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                        fastXmlSerializer.startDocument(null, true);
                        fastXmlSerializer.startTag(null, "perm-records");
                        int size = sparseArray.size();
                        for (int i = 0; i < size; i++) {
                            UserData userDataValueAt = sparseArray.valueAt(i);
                            fastXmlSerializer.startTag(null, "userId");
                            fastXmlSerializer.attribute(null, "n", String.valueOf(userDataValueAt.userId));
                            for (int i2 = 0; i2 < userDataValueAt.mPackageDatas.size(); i2++) {
                                PackageData packageDataValueAt = userDataValueAt.mPackageDatas.valueAt(i2);
                                fastXmlSerializer.startTag(null, "pkg");
                                fastXmlSerializer.attribute(null, "n", packageDataValueAt.packageName);
                                for (int i3 = 0; i3 < packageDataValueAt.mRecords.size(); i3++) {
                                    PermissionRecord permissionRecordValueAt = packageDataValueAt.mRecords.valueAt(i3);
                                    fastXmlSerializer.startTag(null, "perm");
                                    fastXmlSerializer.attribute(null, "n", permissionRecordValueAt.permission);
                                    for (int i4 = 0; i4 < permissionRecordValueAt.requestTimes.size(); i4++) {
                                        fastXmlSerializer.startTag(null, "time");
                                        fastXmlSerializer.attribute(null, "n", String.valueOf(permissionRecordValueAt.requestTimes.get(i4)));
                                        fastXmlSerializer.endTag(null, "time");
                                    }
                                    fastXmlSerializer.endTag(null, "perm");
                                }
                                fastXmlSerializer.endTag(null, "pkg");
                            }
                            fastXmlSerializer.endTag(null, "userId");
                        }
                        fastXmlSerializer.endTag(null, "perm-records");
                        fastXmlSerializer.endDocument();
                        this.mFile.finishWrite(fileOutputStreamStartWrite);
                    } catch (IOException e) {
                        Slog.w(TAG, "Failed to write state, restoring backup.", e);
                        this.mFile.failWrite(fileOutputStreamStartWrite);
                    }
                } catch (IOException e2) {
                    Slog.w(TAG, "Failed to write state: " + e2);
                    return;
                }
            }
        }
        Slog.d(TAG, "writeState() END");
    }

    private void copyUserDatas(SparseArray<UserData> sparseArray) {
        UserData userData;
        PackageData packageData;
        PermissionRecord permissionRecord;
        Slog.d(TAG, "copyUserDatas() BEGIN");
        synchronized (this.mUserDatas) {
            for (int i = 0; i < this.mUserDatas.size(); i++) {
                UserData userDataValueAt = this.mUserDatas.valueAt(i);
                for (int i2 = 0; i2 < userDataValueAt.mPackageDatas.size(); i2++) {
                    PackageData packageDataValueAt = userDataValueAt.mPackageDatas.valueAt(i2);
                    for (int i3 = 0; i3 < packageDataValueAt.mRecords.size(); i3++) {
                        PermissionRecord permissionRecordValueAt = packageDataValueAt.mRecords.valueAt(i3);
                        for (int i4 = 0; i4 < permissionRecordValueAt.requestTimes.size(); i4++) {
                            UserData userData2 = sparseArray.get(userDataValueAt.userId);
                            if (userData2 != null) {
                                userData = userData2;
                            } else {
                                UserData userData3 = new UserData(userDataValueAt.userId);
                                sparseArray.put(userDataValueAt.userId, userData3);
                                userData = userData3;
                            }
                            PackageData packageData2 = userData.mPackageDatas.get(packageDataValueAt.packageName);
                            if (packageData2 != null) {
                                packageData = packageData2;
                            } else {
                                PackageData packageData3 = new PackageData(packageDataValueAt.packageName);
                                userData.addPackageData(packageData3);
                                packageData = packageData3;
                            }
                            PermissionRecord permissionRecord2 = packageData.mRecords.get(permissionRecordValueAt.permission);
                            if (permissionRecord2 != null) {
                                permissionRecord = permissionRecord2;
                            } else {
                                PermissionRecord permissionRecord3 = new PermissionRecord(permissionRecordValueAt.permission);
                                packageData.addPermissionRecord(permissionRecord3);
                                permissionRecord = permissionRecord3;
                            }
                            permissionRecord.addUsageTime(permissionRecordValueAt.requestTimes.get(i4).longValue());
                        }
                    }
                }
            }
        }
        Slog.d(TAG, "copyUserDatas() BEGIN");
    }
}
