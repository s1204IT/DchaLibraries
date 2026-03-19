package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SmsHeader;
import com.android.providers.telephony.MmsSmsProvider;
import com.android.providers.telephony.TelephonyBackupAgent;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import mediatek.telephony.MtkSmsManager;
import mediatek.telephony.MtkSmsMessage;

public class SmsProvider extends ContentProvider {

    @VisibleForTesting
    public SQLiteOpenHelper mCeOpenHelper;

    @VisibleForTesting
    public SQLiteOpenHelper mDeOpenHelper;
    private boolean mIsInternationalCardNotActivate = false;
    private static final Uri NOTIFICATION_URI = Uri.parse("content://sms");
    private static final Uri ICC_URI = Uri.parse("content://sms/icc");
    private static final Integer ONE = 1;
    private static final String[] CANONICAL_ADDRESSES_COLUMNS_2 = {"_id", "address"};
    private static final String[] ICC_COLUMNS = {"service_center_address", "address", "message_class", "body", "date", "status", "index_on_icc", "is_status_report", "transport_type", "type", "locked", "error_code", "_id", "sub_id"};
    private static final String[] sIDProjection = {"_id"};
    private static final UriMatcher sURLMatcher = new UriMatcher(-1);

    static {
        sURLMatcher.addURI("sms", null, 0);
        sURLMatcher.addURI("sms", "#", 1);
        sURLMatcher.addURI("sms", "inbox", 2);
        sURLMatcher.addURI("sms", "inbox/#", 3);
        sURLMatcher.addURI("sms", "sent", 4);
        sURLMatcher.addURI("sms", "sent/#", 5);
        sURLMatcher.addURI("sms", "draft", 6);
        sURLMatcher.addURI("sms", "draft/#", 7);
        sURLMatcher.addURI("sms", "outbox", 8);
        sURLMatcher.addURI("sms", "outbox/#", 9);
        sURLMatcher.addURI("sms", "undelivered", 27);
        sURLMatcher.addURI("sms", "failed", 24);
        sURLMatcher.addURI("sms", "failed/#", 25);
        sURLMatcher.addURI("sms", "queued", 26);
        sURLMatcher.addURI("sms", "conversations", 10);
        sURLMatcher.addURI("sms", "conversations/*", 11);
        sURLMatcher.addURI("sms", "raw", 15);
        sURLMatcher.addURI("sms", "raw/permanentDelete", 28);
        sURLMatcher.addURI("raw", "#", 29);
        sURLMatcher.addURI("sms", "attachments", 16);
        sURLMatcher.addURI("sms", "attachments/#", 17);
        sURLMatcher.addURI("sms", "threadID", 18);
        sURLMatcher.addURI("sms", "threadID/*", 19);
        sURLMatcher.addURI("sms", "status/#", 20);
        sURLMatcher.addURI("sms", "sr_pending", 21);
        sURLMatcher.addURI("sms", "icc", 22);
        sURLMatcher.addURI("sms", "icc/#", 23);
        sURLMatcher.addURI("sms", "sim", 22);
        sURLMatcher.addURI("sms", "sim/#", 23);
        sURLMatcher.addURI("sms", "all_threadid", 34);
        sURLMatcher.addURI("sms", "auto_delete/#", 35);
        sURLMatcher.addURI("sms", "thread_id", 40);
    }

    @Override
    public boolean onCreate() {
        setAppOps(14, 15);
        this.mDeOpenHelper = MmsSmsDatabaseHelper.getInstanceForDe(getContext());
        this.mCeOpenHelper = MmsSmsDatabaseHelper.getInstanceForCe(getContext());
        TelephonyBackupAgent.DeferredSmsMmsRestoreService.startIfFilesExist(getContext());
        return true;
    }

    public static String getSmsTable(boolean z) {
        return z ? "sms_restricted" : "sms";
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String[] strArr3;
        String str3;
        String smsTable = getSmsTable(ProviderUtil.isAccessRestricted(getContext(), getCallingPackage(), Binder.getCallingUid()));
        MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "query begin, url = " + uri + ", selection = " + str);
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        int iMatch = sURLMatcher.match(uri);
        SQLiteDatabase readableDatabase = getReadableDatabase(iMatch);
        if (iMatch == 34) {
            return getAllSmsThreadIds(str, strArr2);
        }
        if (iMatch != 40) {
            switch (iMatch) {
                case 0:
                    constructQueryForBox(sQLiteQueryBuilder, 0, smsTable);
                    strArr3 = strArr;
                    if (!TextUtils.isEmpty(str2)) {
                        str3 = sQLiteQueryBuilder.getTables().equals(smsTable) ? "date DESC" : null;
                    } else {
                        str3 = str2;
                    }
                    Cursor cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                    cursorQuery.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                    return cursorQuery;
                case 1:
                    sQLiteQueryBuilder.setTables(smsTable);
                    sQLiteQueryBuilder.appendWhere("(_id = " + uri.getPathSegments().get(0) + ")");
                    strArr3 = strArr;
                    if (!TextUtils.isEmpty(str2)) {
                    }
                    Cursor cursorQuery2 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                    cursorQuery2.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                    return cursorQuery2;
                case 2:
                    constructQueryForBox(sQLiteQueryBuilder, 1, smsTable);
                    strArr3 = strArr;
                    if (!TextUtils.isEmpty(str2)) {
                    }
                    Cursor cursorQuery22 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                    cursorQuery22.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                    return cursorQuery22;
                case 3:
                case 5:
                case 7:
                case 9:
                    sQLiteQueryBuilder.setTables(smsTable);
                    sQLiteQueryBuilder.appendWhere("(_id = " + uri.getPathSegments().get(1) + ")");
                    strArr3 = strArr;
                    if (!TextUtils.isEmpty(str2)) {
                    }
                    Cursor cursorQuery222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                    cursorQuery222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                    return cursorQuery222;
                case 4:
                    constructQueryForBox(sQLiteQueryBuilder, 2, smsTable);
                    strArr3 = strArr;
                    if (!TextUtils.isEmpty(str2)) {
                    }
                    Cursor cursorQuery2222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                    cursorQuery2222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                    return cursorQuery2222;
                case 6:
                    constructQueryForBox(sQLiteQueryBuilder, 3, smsTable);
                    strArr3 = strArr;
                    if (!TextUtils.isEmpty(str2)) {
                    }
                    Cursor cursorQuery22222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                    cursorQuery22222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                    return cursorQuery22222;
                case 8:
                    constructQueryForBox(sQLiteQueryBuilder, 4, smsTable);
                    strArr3 = strArr;
                    if (!TextUtils.isEmpty(str2)) {
                    }
                    Cursor cursorQuery222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                    cursorQuery222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                    return cursorQuery222222;
                case 10:
                    sQLiteQueryBuilder.setTables(smsTable + ", (SELECT thread_id AS group_thread_id, MAX(date) AS group_date, COUNT(*) AS msg_count FROM " + smsTable + " GROUP BY thread_id) AS groups");
                    sQLiteQueryBuilder.appendWhere(smsTable + ".thread_id=groups.group_thread_id AND " + smsTable + ".date=groups.group_date");
                    HashMap map = new HashMap();
                    StringBuilder sb = new StringBuilder();
                    sb.append(smsTable);
                    sb.append(".body AS snippet");
                    map.put("snippet", sb.toString());
                    map.put("thread_id", smsTable + ".thread_id AS thread_id");
                    map.put("msg_count", "groups.msg_count AS msg_count");
                    map.put("delta", null);
                    sQLiteQueryBuilder.setProjectionMap(map);
                    strArr3 = strArr;
                    if (!TextUtils.isEmpty(str2)) {
                    }
                    Cursor cursorQuery2222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                    cursorQuery2222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                    return cursorQuery2222222;
                case 11:
                    try {
                        int i = Integer.parseInt(uri.getPathSegments().get(1));
                        if (Log.isLoggable("Mms/Provider/Sms", 2)) {
                            Log.d("Mms/Provider/Sms", "query conversations: threadID=" + i);
                            break;
                        }
                        sQLiteQueryBuilder.setTables(smsTable);
                        sQLiteQueryBuilder.appendWhere("thread_id = " + i);
                        strArr3 = strArr;
                        if (!TextUtils.isEmpty(str2)) {
                        }
                        Cursor cursorQuery22222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                        cursorQuery22222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                        return cursorQuery22222222;
                    } catch (Exception e) {
                        Log.e("Mms/Provider/Sms", "Bad conversation thread id: " + uri.getPathSegments().get(1));
                        return null;
                    }
                default:
                    switch (iMatch) {
                        case 15:
                            purgeDeletedMessagesInRawTable(readableDatabase);
                            sQLiteQueryBuilder.setTables("raw");
                            strArr3 = strArr;
                            if (!TextUtils.isEmpty(str2)) {
                            }
                            Cursor cursorQuery222222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                            cursorQuery222222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                            return cursorQuery222222222;
                        case 16:
                            sQLiteQueryBuilder.setTables("attachments");
                            strArr3 = strArr;
                            if (!TextUtils.isEmpty(str2)) {
                            }
                            Cursor cursorQuery2222222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                            cursorQuery2222222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                            return cursorQuery2222222222;
                        case 17:
                            sQLiteQueryBuilder.setTables("attachments");
                            sQLiteQueryBuilder.appendWhere("(sms_id = " + uri.getPathSegments().get(1) + ")");
                            strArr3 = strArr;
                            if (!TextUtils.isEmpty(str2)) {
                            }
                            Cursor cursorQuery22222222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                            cursorQuery22222222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                            return cursorQuery22222222222;
                        default:
                            switch (iMatch) {
                                case 19:
                                    sQLiteQueryBuilder.setTables("canonical_addresses");
                                    if (strArr == null) {
                                        strArr3 = sIDProjection;
                                    } else {
                                        strArr3 = strArr;
                                    }
                                    if (!TextUtils.isEmpty(str2)) {
                                    }
                                    Cursor cursorQuery222222222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                                    cursorQuery222222222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                                    return cursorQuery222222222222;
                                case 20:
                                    sQLiteQueryBuilder.setTables(smsTable);
                                    sQLiteQueryBuilder.appendWhere("(_id = " + uri.getPathSegments().get(1) + ")");
                                    strArr3 = strArr;
                                    if (!TextUtils.isEmpty(str2)) {
                                    }
                                    Cursor cursorQuery2222222222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                                    cursorQuery2222222222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                                    return cursorQuery2222222222222;
                                case 21:
                                    sQLiteQueryBuilder.setTables("sr_pending");
                                    strArr3 = strArr;
                                    if (!TextUtils.isEmpty(str2)) {
                                    }
                                    Cursor cursorQuery22222222222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                                    cursorQuery22222222222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                                    return cursorQuery22222222222222;
                                case 22:
                                    return getAllMessagesFromIcc(uri, getSubIdFromUri(uri));
                                case 23:
                                    return getSingleMessageFromIcc(uri.getPathSegments().get(1), getSubIdFromUri(uri));
                                case 24:
                                    constructQueryForBox(sQLiteQueryBuilder, 5, smsTable);
                                    strArr3 = strArr;
                                    if (!TextUtils.isEmpty(str2)) {
                                    }
                                    Cursor cursorQuery222222222222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                                    cursorQuery222222222222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                                    return cursorQuery222222222222222;
                                case 25:
                                    break;
                                case 26:
                                    constructQueryForBox(sQLiteQueryBuilder, 6, smsTable);
                                    strArr3 = strArr;
                                    if (!TextUtils.isEmpty(str2)) {
                                    }
                                    Cursor cursorQuery2222222222222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                                    cursorQuery2222222222222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                                    return cursorQuery2222222222222222;
                                case 27:
                                    constructQueryForUndelivered(sQLiteQueryBuilder, smsTable);
                                    strArr3 = strArr;
                                    if (!TextUtils.isEmpty(str2)) {
                                    }
                                    Cursor cursorQuery22222222222222222 = sQLiteQueryBuilder.query(readableDatabase, strArr3, str, strArr2, null, null, str3);
                                    cursorQuery22222222222222222.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                                    return cursorQuery22222222222222222;
                                default:
                                    Log.e("Mms/Provider/Sms", "Invalid request: " + uri);
                                    return null;
                            }
                            break;
                    }
                    break;
            }
        } else {
            return getThreadIdWithoutInsert(uri.getQueryParameter("recipient"), readableDatabase);
        }
    }

    private void purgeDeletedMessagesInRawTable(SQLiteDatabase sQLiteDatabase) {
        long jCurrentTimeMillis = System.currentTimeMillis() - 3600000;
        int iDelete = sQLiteDatabase.delete("raw", "deleted = 1 AND date < " + jCurrentTimeMillis, null);
        if (Log.isLoggable("Mms/Provider/Sms", 2)) {
            Log.d("Mms/Provider/Sms", "purgeDeletedMessagesInRawTable: num rows older than " + jCurrentTimeMillis + " purged: " + iDelete);
        }
    }

    private SQLiteOpenHelper getDBOpenHelper(int i) {
        if (i == 15 || i == 28) {
            return this.mDeOpenHelper;
        }
        return this.mCeOpenHelper;
    }

    private Object[] convertIccToSms(MtkSmsMessage mtkSmsMessage, ArrayList<String> arrayList, int i, int i2) {
        String displayMessageBody;
        Object[] objArr = new Object[14];
        objArr[0] = mtkSmsMessage.getServiceCenterAddress();
        if (mtkSmsMessage.getStatusOnIcc() == 1 || mtkSmsMessage.getStatusOnIcc() == 3) {
            objArr[1] = mtkSmsMessage.getDisplayOriginatingAddress();
        } else {
            objArr[1] = mtkSmsMessage.getDestinationAddress();
        }
        String strValueOf = null;
        if (arrayList != null) {
            strValueOf = arrayList.get(0);
            displayMessageBody = arrayList.get(1);
        } else {
            displayMessageBody = null;
        }
        objArr[2] = String.valueOf(mtkSmsMessage.getMessageClass());
        if (displayMessageBody == null) {
            displayMessageBody = mtkSmsMessage.getDisplayMessageBody();
        }
        objArr[3] = displayMessageBody;
        objArr[4] = Long.valueOf(mtkSmsMessage.getTimestampMillis());
        objArr[5] = Integer.valueOf(mtkSmsMessage.getStatusOnIcc());
        if (this.mIsInternationalCardNotActivate) {
            if (strValueOf == null) {
                try {
                    objArr[6] = String.valueOf(mtkSmsMessage.getIndexOnIcc() ^ 1024);
                } catch (NumberFormatException e) {
                    Log.e("Mms/Provider/Sms", "concatSmsIndex bad number");
                }
            } else {
                objArr[6] = strValueOf;
            }
        } else {
            if (strValueOf == null) {
                strValueOf = Integer.valueOf(mtkSmsMessage.getIndexOnIcc());
            }
            objArr[6] = strValueOf;
        }
        Log.d("Mms/Provider/Sms", "convertIccToSms; contactSmsIndex:" + objArr[6]);
        objArr[7] = Boolean.valueOf(mtkSmsMessage.isStatusReportMessage());
        objArr[8] = "sms";
        objArr[9] = 0;
        objArr[10] = 0;
        objArr[11] = 0;
        objArr[12] = Integer.valueOf(i);
        objArr[13] = Integer.valueOf(i2);
        return objArr;
    }

    private Cursor getSingleMessageFromIcc(String str, int i) {
        try {
            int i2 = Integer.parseInt(str);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ArrayList allMessagesFromIcc = MtkSmsManager.getSmsManagerForSubscriptionId(i).getAllMessagesFromIcc();
                if (allMessagesFromIcc != null && !allMessagesFromIcc.isEmpty()) {
                    MtkSmsMessage mtkSmsMessage = (MtkSmsMessage) allMessagesFromIcc.get(i2);
                    if (mtkSmsMessage == null) {
                        throw new IllegalArgumentException("Message not retrieved. ID: " + str);
                    }
                    MatrixCursor matrixCursor = new MatrixCursor(ICC_COLUMNS, 1);
                    matrixCursor.addRow(convertIccToSms(mtkSmsMessage, 0, i));
                    return withIccNotificationUri(matrixCursor);
                }
                Log.e("Mms/Provider/Sms", "getSingleMessageFromIcc messages is null");
                return null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad SMS ICC ID: " + str);
        }
    }

    private Cursor getAllMessagesFromIcc(Uri uri, int i) {
        ArrayList<String> concatSmsIndexAndBody;
        SmsHeader userDataHeader;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            ArrayList<MtkSmsMessage> allMessagesFromIcc = MtkSmsManager.getSmsManagerForSubscriptionId(i).getAllMessagesFromIcc();
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if (allMessagesFromIcc == null || allMessagesFromIcc.isEmpty()) {
                Log.e("Mms/Provider/Sms", "getAllMessagesFromIcc messages is null");
                return null;
            }
            int size = allMessagesFromIcc.size();
            MatrixCursor matrixCursor = new MatrixCursor(ICC_COLUMNS, size);
            boolean zEquals = "1".equals(uri.getQueryParameter("showInOne"));
            for (int i2 = 0; i2 < size; i2++) {
                MtkSmsMessage mtkSmsMessage = allMessagesFromIcc.get(i2);
                if (mtkSmsMessage != null && !mtkSmsMessage.isStatusReportMessage()) {
                    if (zEquals && (userDataHeader = mtkSmsMessage.getUserDataHeader()) != null && userDataHeader.concatRef != null) {
                        concatSmsIndexAndBody = getConcatSmsIndexAndBody(allMessagesFromIcc, i2);
                    } else {
                        concatSmsIndexAndBody = null;
                    }
                    matrixCursor.addRow(convertIccToSms(mtkSmsMessage, concatSmsIndexAndBody, i2, i));
                }
            }
            return withIccNotificationUri(matrixCursor);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private Cursor withIccNotificationUri(Cursor cursor) {
        cursor.setNotificationUri(getContext().getContentResolver(), ICC_URI);
        return cursor;
    }

    private void constructQueryForBox(SQLiteQueryBuilder sQLiteQueryBuilder, int i, String str) {
        sQLiteQueryBuilder.setTables(str);
        if (i != 0) {
            sQLiteQueryBuilder.appendWhere("type=" + i);
        }
    }

    private void constructQueryForUndelivered(SQLiteQueryBuilder sQLiteQueryBuilder, String str) {
        sQLiteQueryBuilder.setTables(str);
        sQLiteQueryBuilder.appendWhere("(type=4 OR type=5 OR type=6)");
    }

    @Override
    public String getType(Uri uri) {
        switch (uri.getPathSegments().size()) {
            case 0:
                return "vnd.android.cursor.dir/sms";
            case 1:
                try {
                    Integer.parseInt(uri.getPathSegments().get(0));
                    return "vnd.android.cursor.item/sms";
                } catch (NumberFormatException e) {
                    return "vnd.android.cursor.dir/sms";
                }
            case 2:
                if (uri.getPathSegments().get(0).equals("conversations")) {
                    return "vnd.android.cursor.item/sms-chat";
                }
                return "vnd.android.cursor.item/sms";
            default:
                return null;
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        int callingUid = Binder.getCallingUid();
        String callingPackage = getCallingPackage();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int i = 0;
            for (ContentValues contentValues : contentValuesArr) {
                if (insertInner(uri, contentValues, callingUid, callingPackage) != null) {
                    i++;
                }
            }
            notifyChange(sURLMatcher.match(uri) != 15, uri, callingPackage, false);
            return i;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int callingUid = Binder.getCallingUid();
        String callingPackage = getCallingPackage();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Uri uriInsertInner = insertInner(uri, contentValues, callingUid, callingPackage);
            notifyChange(sURLMatcher.match(uri) != 15, uriInsertInner, callingPackage, false);
            return uriInsertInner;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Uri insertInner(Uri uri, ContentValues contentValues, int i, String str) {
        ContentValues contentValues2;
        boolean z;
        boolean z2;
        boolean z3;
        Long asLong;
        String asString;
        ContentValues contentValues3 = contentValues;
        int iMatch = sURLMatcher.match(uri);
        String str2 = "sms";
        int iIntValue = 6;
        if (iMatch == 0) {
            Integer asInteger = contentValues3.getAsInteger("type");
            if (asInteger != null) {
                iIntValue = asInteger.intValue();
            }
        } else if (iMatch == 2) {
            iIntValue = 1;
        } else if (iMatch == 4) {
            iIntValue = 2;
        } else if (iMatch == 6) {
            iIntValue = 3;
        } else if (iMatch != 8) {
            if (iMatch == 18) {
                str2 = "canonical_addresses";
            } else if (iMatch == 21) {
                str2 = "sr_pending";
            } else if (iMatch == 24) {
                iIntValue = 5;
            } else if (iMatch != 26) {
                switch (iMatch) {
                    case 15:
                        str2 = "raw";
                        break;
                    case 16:
                        str2 = "attachments";
                        break;
                    default:
                        Log.e("Mms/Provider/Sms", "Invalid request: " + uri);
                        return null;
                }
            }
            iIntValue = 0;
        } else {
            iIntValue = 4;
        }
        SQLiteDatabase writableDatabase = getWritableDatabase(iMatch);
        writableDatabase.beginTransaction();
        try {
            if (str2.equals("sms")) {
                if (contentValues3 == null) {
                    contentValues2 = new ContentValues(1);
                    z = true;
                    z2 = true;
                } else {
                    contentValues2 = new ContentValues(contentValues3);
                    z = !contentValues3.containsKey("date");
                    z2 = !contentValues3.containsKey("type");
                    if (contentValues3.containsKey("import_sms")) {
                        contentValues2.remove("import_sms");
                        z3 = true;
                    }
                    if (z) {
                        Long asLong2 = contentValues2.getAsLong("date");
                        contentValues2.put("date", asLong2);
                        Log.d("Mms/Provider/Sms", "insert sms date " + asLong2);
                    } else {
                        contentValues2.put("date", new Long(System.currentTimeMillis()));
                    }
                    if (z2 && iIntValue != 0) {
                        contentValues2.put("type", Integer.valueOf(iIntValue));
                    }
                    asLong = contentValues2.getAsLong("thread_id");
                    asString = contentValues2.getAsString("address");
                    if ((asLong != null || asLong.longValue() == 0) && !TextUtils.isEmpty(asString)) {
                        long threadIdInternal = z3 ? getThreadIdInternal(asString, writableDatabase, true) : getThreadIdInternal(asString, writableDatabase, false);
                        contentValues2.put("thread_id", Long.valueOf(threadIdInternal));
                        Log.d("Mms/Provider/Sms", "insert getContentResolver getOrCreateThreadId end id = " + threadIdInternal);
                    }
                    if (contentValues2.getAsInteger("type").intValue() == 3) {
                        writableDatabase.delete("sms", "thread_id=? AND type=?", new String[]{contentValues2.getAsString("thread_id"), Integer.toString(3)});
                    }
                    if (iIntValue != 1) {
                        contentValues2.put("read", ONE);
                    }
                    if (!contentValues2.containsKey("person")) {
                        contentValues2.put("person", (Integer) 0);
                    }
                    if (ProviderUtil.shouldSetCreator(contentValues2, i)) {
                        contentValues2.put("creator", str);
                    }
                    contentValues3 = contentValues2;
                }
                z3 = false;
                if (z) {
                }
                if (z2) {
                    contentValues2.put("type", Integer.valueOf(iIntValue));
                }
                asLong = contentValues2.getAsLong("thread_id");
                asString = contentValues2.getAsString("address");
                if (asLong != null) {
                    if (z3) {
                    }
                    contentValues2.put("thread_id", Long.valueOf(threadIdInternal));
                    Log.d("Mms/Provider/Sms", "insert getContentResolver getOrCreateThreadId end id = " + threadIdInternal);
                    if (contentValues2.getAsInteger("type").intValue() == 3) {
                    }
                    if (iIntValue != 1) {
                    }
                    if (!contentValues2.containsKey("person")) {
                    }
                    if (ProviderUtil.shouldSetCreator(contentValues2, i)) {
                    }
                    contentValues3 = contentValues2;
                } else {
                    if (z3) {
                    }
                    contentValues2.put("thread_id", Long.valueOf(threadIdInternal));
                    Log.d("Mms/Provider/Sms", "insert getContentResolver getOrCreateThreadId end id = " + threadIdInternal);
                    if (contentValues2.getAsInteger("type").intValue() == 3) {
                    }
                    if (iIntValue != 1) {
                    }
                    if (!contentValues2.containsKey("person")) {
                    }
                    if (ProviderUtil.shouldSetCreator(contentValues2, i)) {
                    }
                    contentValues3 = contentValues2;
                }
            } else if (contentValues3 == null) {
                contentValues3 = new ContentValues(1);
            }
            long jInsert = writableDatabase.insert(str2, "body", contentValues3);
            if (Log.isLoggable("Mms/Provider/Sms", 2)) {
                Log.v("Mms/Provider/Sms", "insert table body end");
            }
            if (str2 == "sms") {
                ContentValues contentValues4 = new ContentValues();
                contentValues4.put("_id", Long.valueOf(jInsert));
                contentValues4.put("index_text", contentValues3.getAsString("body"));
                contentValues4.put("source_id", Long.valueOf(jInsert));
                contentValues4.put("table_to_use", (Integer) 1);
                writableDatabase.insert("words", "index_text", contentValues4);
                if (Log.isLoggable("Mms/Provider/Sms", 2)) {
                    Log.v("Mms/Provider/Sms", "insert TABLE_WORDS end");
                }
            }
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
            if (jInsert <= 0) {
                Log.e("Mms/Provider/Sms", "insert: failed!");
                return null;
            }
            Uri uriWithAppendedPath = Uri.withAppendedPath(uri, String.valueOf(jInsert));
            if (Log.isLoggable("Mms/Provider/Sms", 2)) {
                Log.d("Mms/Provider/Sms", "insertInner " + uriWithAppendedPath + " succeeded");
            }
            notifyChange(false, uriWithAppendedPath, null, true);
            Log.d("Mms/Provider/Sms", "insertInner succeed, uri = " + uriWithAppendedPath);
            return uriWithAppendedPath;
        } catch (Throwable th) {
            writableDatabase.endTransaction();
            throw th;
        }
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int iDelete;
        Log.d("Mms/Provider/Sms", "delete begin, uri = " + uri + ", selection = " + str);
        int iMatch = sURLMatcher.match(uri);
        SQLiteDatabase writableDatabase = getWritableDatabase(iMatch);
        boolean z = true;
        if (iMatch == 11) {
            try {
                int i = Integer.parseInt(uri.getPathSegments().get(1));
                iDelete = writableDatabase.delete("sms", DatabaseUtils.concatenateWhere("thread_id=" + i, str), strArr);
                MmsSmsDatabaseHelper.updateThread(writableDatabase, (long) i);
            } catch (Exception e) {
                throw new IllegalArgumentException("Bad conversation thread id: " + uri.getPathSegments().get(1));
            }
        } else {
            if (iMatch == 15) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("deleted", (Integer) 1);
                iDelete = writableDatabase.update("raw", contentValues, str, strArr);
                if (Log.isLoggable("Mms/Provider/Sms", 2)) {
                    Log.d("Mms/Provider/Sms", "delete: num rows marked deleted in raw table: " + iDelete);
                }
            } else if (iMatch == 28) {
                iDelete = writableDatabase.delete("raw", str, strArr);
                if (Log.isLoggable("Mms/Provider/Sms", 2)) {
                    Log.d("Mms/Provider/Sms", "delete: num rows permanently deleted in raw table: " + iDelete);
                }
            } else if (iMatch != 35) {
                switch (iMatch) {
                    case 0:
                        if (str != null && str.equals("ForMultiDelete")) {
                            Log.d("Mms/Provider/Sms", "delete FOR_MULTIDELETE");
                            String smsIdsFromArgs = getSmsIdsFromArgs(strArr);
                            Cursor cursorRawQuery = writableDatabase.rawQuery(String.format("SELECT DISTINCT thread_id FROM sms WHERE _id IN %s", smsIdsFromArgs), null);
                            try {
                                long[] jArr = new long[cursorRawQuery.getCount()];
                                int i2 = 0;
                                while (cursorRawQuery.moveToNext()) {
                                    int i3 = i2 + 1;
                                    jArr[i2] = cursorRawQuery.getLong(0);
                                    i2 = i3;
                                    break;
                                }
                                cursorRawQuery.close();
                                iDelete = deleteMessages(writableDatabase, String.format(" _id IN %s", smsIdsFromArgs), null);
                                if (iDelete != 0) {
                                    MmsSmsDatabaseHelper.updateMultiThreads(writableDatabase, jArr);
                                }
                            } catch (Throwable th) {
                                cursorRawQuery.close();
                                throw th;
                            }
                        } else {
                            Log.d("Mms/Provider/Sms", "SMS_ALL: where = " + str);
                            Cursor cursorQuery = writableDatabase.query("sms", new String[]{"distinct thread_id"}, str, strArr, null, null, null);
                            try {
                                long[] jArr2 = new long[cursorQuery.getCount()];
                                int i4 = 0;
                                while (cursorQuery.moveToNext()) {
                                    int i5 = i4 + 1;
                                    jArr2[i4] = cursorQuery.getLong(0);
                                    i4 = i5;
                                    break;
                                }
                                cursorQuery.close();
                                int iDelete2 = writableDatabase.delete("sms", str, strArr);
                                if (iDelete2 != 0) {
                                    MmsSmsDatabaseHelper.updateMultiThreads(writableDatabase, jArr2);
                                }
                                iDelete = iDelete2;
                            } catch (Throwable th2) {
                                cursorQuery.close();
                                throw th2;
                            }
                        }
                        break;
                    case 1:
                        try {
                            iDelete = MmsSmsDatabaseHelper.deleteOneSms(writableDatabase, Integer.parseInt(uri.getPathSegments().get(0)));
                        } catch (Exception e2) {
                            throw new IllegalArgumentException("Bad message id: " + uri.getPathSegments().get(0));
                        }
                        break;
                    default:
                        switch (iMatch) {
                            case 21:
                                iDelete = writableDatabase.delete("sr_pending", str, strArr);
                                break;
                            case 22:
                                int subIdFromUri = getSubIdFromUri(uri);
                                Log.i("Mms/Provider/Sms", "Delete messages in subId = " + subIdFromUri);
                                if (str != null && str.equals("ForMultiDelete")) {
                                    Log.d("Mms/Provider/Sms", "delete FOR_MULTIDELETE");
                                    iDelete = 0;
                                    for (int i6 = 0; i6 < strArr.length; i6++) {
                                        if (strArr[i6] != null) {
                                            String str2 = strArr[i6];
                                            Log.i("Mms/Provider/Sms", "Delete Sub" + (subIdFromUri + 1) + " SMS id: " + str2);
                                            iDelete += deleteMessageFromIcc(str2, subIdFromUri);
                                        }
                                    }
                                } else {
                                    iDelete = deleteMessageFromIcc("-1", subIdFromUri);
                                }
                                break;
                            case 23:
                                return deleteMessageFromIcc(uri.getPathSegments().get(1), getSubIdFromUri(uri));
                            default:
                                throw new IllegalArgumentException("Unknown URL");
                        }
                        break;
                }
            } else {
                try {
                    int i7 = Integer.parseInt(uri.getPathSegments().get(1));
                    String strConcatenateWhere = DatabaseUtils.concatenateWhere("thread_id=" + i7, str);
                    if (strArr != null) {
                        writableDatabase.execSQL("delete from words where table_to_use=1 and source_id in " + getSmsIdsFromArgs(strArr));
                        Log.d("Mms/Provider/Sms", "delete words end");
                        iDelete = 0;
                        for (int i8 = 0; i8 < strArr.length; i8++) {
                            if (i8 % 100 == 0) {
                                Log.d("Mms/Provider/Sms", "delete sms1 beginTransaction i = " + i8);
                            }
                            iDelete += writableDatabase.delete("sms", "_id=" + strArr[i8], null);
                        }
                    } else if (strConcatenateWhere != null) {
                        String[] strArrSplit = strConcatenateWhere.split("_id<");
                        if (strArrSplit.length > 1) {
                            String strReplace = strArrSplit[1].replace(")", "");
                            Log.d("Mms/Provider/Sms", "SMS_CONVERSATIONS_ID args[1] = " + strArrSplit[1]);
                            int i9 = Integer.parseInt(strReplace);
                            Log.d("Mms/Provider/Sms", "SMS_CONVERSATIONS_ID id = " + i9);
                            iDelete = 0;
                            for (int i10 = 1; i10 < i9; i10++) {
                                if (i10 % 30 == 0 || i10 == i9 - 1) {
                                    Log.d("Mms/Provider/Sms", "delete sms2 beginTransaction i = " + i10);
                                    iDelete += writableDatabase.delete("sms", DatabaseUtils.concatenateWhere("thread_id=" + i7, "locked=0 AND type<>3 AND ipmsg_id<=0 AND _id>" + (i10 - 30) + " AND _id<=" + i10), null);
                                    Log.d("Mms/Provider/Sms", "delete sms2 endTransaction i = " + i10 + " count=" + iDelete);
                                }
                            }
                        } else {
                            iDelete = 0;
                        }
                    }
                    MmsSmsDatabaseHelper.updateThread(writableDatabase, i7);
                } catch (Exception e3) {
                    throw new IllegalArgumentException("Bad conversation thread id: " + uri.getPathSegments().get(1));
                }
            }
            z = false;
        }
        if (iDelete > 0) {
            notifyChange(z, uri, getCallingPackage(), false);
        }
        Log.d("Mms/Provider/Sms", "delete end, count = " + iDelete);
        return iDelete;
    }

    protected static String getSmsIdsFromArgs(String[] strArr) {
        StringBuffer stringBuffer = new StringBuffer("(");
        if (strArr == null || strArr.length < 1) {
            return "()";
        }
        for (int i = 0; i < strArr.length - 1 && strArr[i] != null; i++) {
            stringBuffer.append(strArr[i]);
            stringBuffer.append(",");
        }
        if (strArr[strArr.length - 1] != null) {
            stringBuffer.append(strArr[strArr.length - 1]);
        }
        String string = stringBuffer.toString();
        if (string.endsWith(",")) {
            string = string.substring(0, string.lastIndexOf(","));
        }
        return string + ")";
    }

    private int deleteMessageFromIcc(String str, int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                return SmsManager.getSmsManagerForSubscriptionId(i).deleteMessageFromIcc(Integer.parseInt(str)) ? 1 : 0;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bad SMS ICC ID: " + str);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            getContext().getContentResolver().notifyChange(ICC_URI, null);
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int callingUid = Binder.getCallingUid();
        String callingPackage = getCallingPackage();
        Log.d("Mms/Provider/Sms", "update begin, uri = " + uri + ", values = " + contentValues + ", selection = " + str);
        String str2 = "sms";
        int iMatch = sURLMatcher.match(uri);
        SQLiteDatabase writableDatabase = getWritableDatabase(iMatch);
        boolean z = true;
        String str3 = null;
        if (iMatch == 15) {
            str2 = "raw";
            z = false;
        } else {
            switch (iMatch) {
                case 0:
                case 2:
                case 4:
                case 6:
                case 8:
                case 10:
                    break;
                case 1:
                    str3 = "_id=" + uri.getPathSegments().get(0);
                    break;
                case 3:
                case 5:
                case 7:
                case 9:
                    str3 = "_id=" + uri.getPathSegments().get(1);
                    break;
                case 11:
                    String str4 = uri.getPathSegments().get(1);
                    try {
                        Integer.parseInt(str4);
                        str3 = "thread_id=" + str4;
                    } catch (Exception e) {
                        Log.e("Mms/Provider/Sms", "Bad conversation thread id: " + str4);
                    }
                    break;
                default:
                    switch (iMatch) {
                        case 20:
                            str3 = "_id=" + uri.getPathSegments().get(1);
                            break;
                        case 21:
                            str2 = "sr_pending";
                            break;
                        default:
                            switch (iMatch) {
                                case 24:
                                case 26:
                                    break;
                                case 25:
                                    break;
                                default:
                                    throw new UnsupportedOperationException("URI " + uri + " not supported");
                            }
                            break;
                    }
                    break;
            }
        }
        if (str2.equals("sms") && ProviderUtil.shouldRemoveCreator(contentValues, callingUid)) {
            Log.w("Mms/Provider/Sms", callingPackage + " tries to update CREATOR");
            contentValues.remove("creator");
        }
        int iUpdate = writableDatabase.update(str2, contentValues, DatabaseUtils.concatenateWhere(str, str3), strArr);
        if (iUpdate > 0) {
            if (Log.isLoggable("Mms/Provider/Sms", 2)) {
                MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "update " + uri + " succeeded");
            }
            notifyChange(z, uri, callingPackage, Boolean.valueOf(contentValues.containsKey("read")).booleanValue());
        }
        Log.d("Mms/Provider/Sms", "update end, affectedRows = " + iUpdate);
        return iUpdate;
    }

    private void notifyChange(boolean z, Uri uri, String str, boolean z2) {
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.notifyChange(uri, null, true, -1);
        contentResolver.notifyChange(Telephony.MmsSms.CONTENT_URI, null, true, -1);
        contentResolver.notifyChange(Uri.parse("content://mms-sms/conversations/"), null, true, -1);
        int iMatch = sURLMatcher.match(uri);
        Log.d("Mms/Provider/Sms", "URLMatcher matches type of message" + iMatch);
        if (z2 && iMatch != 29 && iMatch != 15 && iMatch != -1) {
            Log.d("Mms/Provider/Sms", "notifyChange, notify unread change");
            MmsSmsProvider.notifyUnreadMessageNumberChanged(getContext());
        }
        if (z) {
            ProviderUtil.notifyIfNotDefaultSmsApp(uri, str, context);
        }
    }

    private Object[] convertIccToSms(MtkSmsMessage mtkSmsMessage, int i, int i2) {
        return convertIccToSms(mtkSmsMessage, null, i, i2);
    }

    private ArrayList<String> getConcatSmsIndexAndBody(ArrayList<MtkSmsMessage> arrayList, int i) {
        int i2;
        int i3;
        SmsHeader userDataHeader;
        SmsHeader userDataHeader2;
        int size = arrayList.size();
        ArrayList<String> arrayList2 = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        MtkSmsMessage mtkSmsMessage = arrayList.get(i);
        if (mtkSmsMessage != null && (userDataHeader2 = mtkSmsMessage.getUserDataHeader()) != null && userDataHeader2.concatRef != null) {
            i3 = userDataHeader2.concatRef.msgCount;
            i2 = userDataHeader2.concatRef.refNumber;
        } else {
            i2 = 0;
            i3 = 0;
        }
        ArrayList arrayList3 = new ArrayList();
        arrayList3.add(mtkSmsMessage);
        while (true) {
            i++;
            if (i >= size) {
                break;
            }
            MtkSmsMessage mtkSmsMessage2 = arrayList.get(i);
            if (mtkSmsMessage2 != null && (userDataHeader = mtkSmsMessage2.getUserDataHeader()) != null && userDataHeader.concatRef != null && i2 == userDataHeader.concatRef.refNumber) {
                arrayList3.add(mtkSmsMessage2);
                arrayList.set(i, null);
                if (i3 == arrayList3.size()) {
                    break;
                }
            }
        }
        int size2 = arrayList3.size();
        for (int i4 = 0; i4 < i3; i4++) {
            int i5 = 0;
            while (true) {
                if (i5 < size2) {
                    MtkSmsMessage mtkSmsMessage3 = (MtkSmsMessage) arrayList3.get(i5);
                    if (i4 != mtkSmsMessage3.getUserDataHeader().concatRef.seqNumber - 1) {
                        i5++;
                    } else {
                        if (this.mIsInternationalCardNotActivate) {
                            try {
                                sb.append((mtkSmsMessage.getIndexOnIcc() ^ 1024) + "");
                            } catch (NumberFormatException e) {
                                Log.e("Mms/Provider/Sms", "concatSmsIndex bad number");
                            }
                        } else {
                            sb.append(mtkSmsMessage3.getIndexOnIcc());
                        }
                        sb.append(";");
                        sb2.append(mtkSmsMessage3.getDisplayMessageBody());
                    }
                }
            }
        }
        MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "concatenation sms index:" + sb.toString() + "concatenation sms body:" + sb2.toString());
        arrayList2.add(sb.toString());
        arrayList2.add(sb2.toString());
        return arrayList2;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws OperationApplicationException {
        SQLiteDatabase writableDatabase = getDBOpenHelper(0).getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            int size = arrayList.size();
            ContentProviderResult[] contentProviderResultArr = new ContentProviderResult[size];
            int i = 0;
            for (int i2 = 0; i2 < size; i2++) {
                i++;
                if (i > 50) {
                    throw new OperationApplicationException("Too many content provider operations between yield points. The maximum number of operations per yield point is 50", 0);
                }
                contentProviderResultArr[i2] = arrayList.get(i2).apply(this, contentProviderResultArr, i2);
            }
            writableDatabase.setTransactionSuccessful();
            return contentProviderResultArr;
        } finally {
            writableDatabase.endTransaction();
        }
    }

    private Cursor getAllSmsThreadIds(String str, String[] strArr) {
        return getDBOpenHelper(0).getReadableDatabase().query("sms", new String[]{"distinct thread_id"}, str, strArr, null, null, null);
    }

    private long getThreadIdInternal(String str, SQLiteDatabase sQLiteDatabase, boolean z) {
        String str2 = SystemProperties.get("ro.vendor.mtk_wappush_support").equals("1") ? "SELECT _id FROM threads WHERE type<>2 AND type<>3 AND recipient_ids=? AND status=0" : "SELECT _id FROM threads WHERE type<>3 AND recipient_ids=? AND status=0";
        long jFastGetRecipientId = z ? fastGetRecipientId(str, sQLiteDatabase) : getRecipientId(str, sQLiteDatabase);
        MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "sms insert, getThreadIdInternal, recipientId = " + jFastGetRecipientId);
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery(str2, new String[]{String.valueOf(jFastGetRecipientId)});
        if (cursorRawQuery != null) {
            try {
                if (cursorRawQuery.getCount() == 0) {
                    MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "getThreadId: create new thread_id for recipients " + str);
                    long jInsertThread = insertThread(jFastGetRecipientId, sQLiteDatabase);
                    if (cursorRawQuery != null) {
                        cursorRawQuery.close();
                    }
                    return jInsertThread;
                }
            } finally {
                if (cursorRawQuery != null) {
                    cursorRawQuery.close();
                }
            }
        }
        if (cursorRawQuery.getCount() != 1) {
            Log.w("Mms/Provider/Sms", "getThreadId: why is cursorCount=" + cursorRawQuery.getCount());
        } else if (cursorRawQuery.moveToFirst()) {
            return cursorRawQuery.getLong(0);
        }
        if (cursorRawQuery == null) {
            return 0L;
        }
        cursorRawQuery.close();
        return 0L;
    }

    private Cursor getThreadIdWithoutInsert(String str, SQLiteDatabase sQLiteDatabase) throws Throwable {
        String str2;
        if (SystemProperties.get("ro.vendor.mtk_wappush_support").equals("1")) {
            str2 = "SELECT _id FROM threads WHERE type<>2 AND type<>3 AND recipient_ids=?";
        } else {
            str2 = "SELECT _id FROM threads WHERE type<>3 AND recipient_ids=?";
        }
        long singleAddressId = getSingleAddressId(str, sQLiteDatabase, false);
        MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "getThreadIdWithoutInsert, recipientId = " + singleAddressId);
        if (singleAddressId != -1) {
            return sQLiteDatabase.rawQuery(str2, new String[]{String.valueOf(singleAddressId)});
        }
        return null;
    }

    private long insertThread(long j, SQLiteDatabase sQLiteDatabase) {
        ContentValues contentValues = new ContentValues(4);
        long jCurrentTimeMillis = System.currentTimeMillis();
        contentValues.put("date", Long.valueOf(jCurrentTimeMillis - (jCurrentTimeMillis % 1000)));
        contentValues.put("recipient_ids", Long.valueOf(j));
        contentValues.put("message_count", (Integer) 0);
        return sQLiteDatabase.insert("threads", null, contentValues);
    }

    private long getRecipientId(String str, SQLiteDatabase sQLiteDatabase) throws Throwable {
        if (!str.equals("insert-address-token")) {
            long singleAddressId = getSingleAddressId(str, sQLiteDatabase, true);
            if (singleAddressId != -1) {
                return singleAddressId;
            }
            MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "getAddressIds: address ID not found for " + str);
            return 0L;
        }
        return 0L;
    }

    private long fastGetRecipientId(String str, SQLiteDatabase sQLiteDatabase) {
        long j;
        if (!str.equals("insert-address-token")) {
            String strSqlEscapeString = DatabaseUtils.sqlEscapeString(str);
            boolean z = getContext().getResources().getBoolean(android.R.^attr-private.pointerIconWait);
            StringBuilder sb = new StringBuilder();
            sb.append("(address=");
            sb.append(strSqlEscapeString);
            sb.append(" OR PHONE_NUMBERS_EQUAL(address, ");
            sb.append(strSqlEscapeString);
            sb.append(z ? ", 1))" : ", 0))");
            Cursor cursorQuery = sQLiteDatabase.query("canonical_addresses", CANONICAL_ADDRESSES_COLUMNS_2, sb.toString(), null, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        j = cursorQuery.getLong(cursorQuery.getColumnIndex("_id"));
                        MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "fastGetRecipientId, id=" + j);
                    } else {
                        j = -1;
                    }
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
            if (j != -1) {
                return j;
            }
            MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "fastGetRecipientId: address ID not found for " + str);
            return insertCanonicalAddresses(sQLiteDatabase, str);
        }
        return 0L;
    }

    private long getSingleAddressId(String str, SQLiteDatabase sQLiteDatabase, boolean z) throws Throwable {
        Cursor cursorQuery;
        long jInsertCanonicalAddresses;
        HashMap map = new HashMap();
        HashMap map2 = new HashMap();
        try {
            cursorQuery = sQLiteDatabase.query("canonical_addresses", CANONICAL_ADDRESSES_COLUMNS_2, null, null, null, null, null);
            if (cursorQuery != null) {
                while (cursorQuery.moveToNext()) {
                    try {
                        long j = cursorQuery.getLong(cursorQuery.getColumnIndexOrThrow("_id"));
                        String string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("address"));
                        String strKey = MmsSmsProvider.key(string, CharBuffer.allocate(7));
                        ArrayList arrayList = (ArrayList) map2.get(strKey);
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                            map2.put(strKey, arrayList);
                        }
                        arrayList.add(string);
                        map.put(string, Long.valueOf(j));
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
            }
            boolean zIsEmailAddress = Telephony.Mms.isEmailAddress(str);
            boolean zIsPhoneNumber = Telephony.Mms.isPhoneNumber(str);
            if (zIsEmailAddress) {
                str = str.toLowerCase();
            }
            ArrayList arrayList2 = (ArrayList) map2.get(MmsSmsProvider.key(str, CharBuffer.allocate(7)));
            String str2 = "";
            if (arrayList2 != null) {
                for (int i = 0; i < arrayList2.size(); i++) {
                    str2 = (String) arrayList2.get(i);
                    if (str2.equals(str)) {
                        jInsertCanonicalAddresses = ((Long) map.get(str2)).longValue();
                        break;
                    }
                    if (zIsPhoneNumber && str != null && str.length() <= 15 && str2 != null && str2.length() <= 15 && PhoneNumberUtils.compare(str, str2, getContext().getResources().getBoolean(android.R.^attr-private.pointerIconWait))) {
                        jInsertCanonicalAddresses = ((Long) map.get(str2)).longValue();
                        break;
                    }
                }
                jInsertCanonicalAddresses = -1;
            } else {
                jInsertCanonicalAddresses = -1;
            }
            if (!z) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return jInsertCanonicalAddresses;
            }
            if (jInsertCanonicalAddresses == -1) {
                jInsertCanonicalAddresses = insertCanonicalAddresses(sQLiteDatabase, str);
                MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "getSingleAddressId: insert new canonical_address for xxxxxx, addressess = " + str.toString());
            } else {
                MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "getSingleAddressId: get exist id=" + jInsertCanonicalAddresses + ", refinedAddress=" + str + ", currentNumber=" + str2);
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return jInsertCanonicalAddresses;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private long insertCanonicalAddresses(SQLiteDatabase sQLiteDatabase, String str) {
        MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Sms", "sms insert insertCanonicalAddresses for address = " + str);
        ContentValues contentValues = new ContentValues(1);
        contentValues.put("address", str);
        return sQLiteDatabase.insert("canonical_addresses", "address", contentValues);
    }

    static int deleteMessages(SQLiteDatabase sQLiteDatabase, String str, String[] strArr) {
        String str2;
        Log.d("Mms/Provider/Sms", "deleteMessages, start");
        int iDelete = 100;
        if (TextUtils.isEmpty(str)) {
            str2 = "_id in (select _id from sms limit 100)";
        } else {
            str2 = "_id in (select _id from sms where " + str + " limit 100)";
        }
        int i = 0;
        while (iDelete > 0) {
            iDelete = sQLiteDatabase.delete("sms", str2, strArr);
            i += iDelete;
            Log.d("Mms/Provider/Sms", "deleteMessages, delete " + iDelete + " sms");
        }
        Log.d("Mms/Provider/Sms", "deleteMessages, delete sms end");
        return i;
    }

    public static int getSubIdFromUri(Uri uri) {
        String queryParameter = uri.getQueryParameter("subscription");
        int defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
        try {
            return Integer.valueOf(queryParameter).intValue();
        } catch (NumberFormatException e) {
            Log.d("Mms/Provider/Sms", "getSubIdFromUri : " + e);
            return defaultSubscriptionId;
        }
    }

    SQLiteDatabase getReadableDatabase(int i) {
        return getDBOpenHelper(i).getReadableDatabase();
    }

    SQLiteDatabase getWritableDatabase(int i) {
        return getDBOpenHelper(i).getWritableDatabase();
    }
}
