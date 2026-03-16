package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.provider.Contacts;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;

public class SmsProvider extends ContentProvider {
    private SQLiteOpenHelper mOpenHelper;
    private static final Uri NOTIFICATION_URI = Uri.parse("content://sms");
    private static final Uri ICC_URI = Uri.parse("content://sms/icc");
    private static final Uri ICC_SUBID_URI = Uri.parse("content://sms/icc/subId");
    private static final Integer ONE = 1;
    private static final String[] CONTACT_QUERY_PROJECTION = {"person"};
    private static final String[] ICC_COLUMNS = {"service_center_address", "address", "message_class", "body", "date", "status", "index_on_icc", "is_status_report", "transport_type", "type", "locked", "error_code", "_id"};
    private static final HashMap<String, String> sConversationProjectionMap = new HashMap<>();
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
        sURLMatcher.addURI("sms", "attachments", 16);
        sURLMatcher.addURI("sms", "attachments/#", 17);
        sURLMatcher.addURI("sms", "threadID", 18);
        sURLMatcher.addURI("sms", "threadID/*", 19);
        sURLMatcher.addURI("sms", "status/#", 20);
        sURLMatcher.addURI("sms", "sr_pending", 21);
        sURLMatcher.addURI("sms", "icc", 22);
        sURLMatcher.addURI("sms", "icc/#", 23);
        sURLMatcher.addURI("sms", "icc/subId/*", 28);
        sURLMatcher.addURI("sms", "icc/subId/*/#", 29);
        sURLMatcher.addURI("sms", "sim", 22);
        sURLMatcher.addURI("sms", "sim/#", 23);
        sConversationProjectionMap.put("snippet", "sms.body AS snippet");
        sConversationProjectionMap.put("thread_id", "sms.thread_id AS thread_id");
        sConversationProjectionMap.put("msg_count", "groups.msg_count AS msg_count");
        sConversationProjectionMap.put("delta", null);
    }

    @Override
    public boolean onCreate() {
        setAppOps(14, 15);
        this.mOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        int subId = SubscriptionManager.getDefaultSubId();
        int match = sURLMatcher.match(url);
        switch (match) {
            case 0:
                constructQueryForBox(qb, 0);
                break;
            case 1:
                qb.setTables("sms");
                qb.appendWhere("(_id = " + url.getPathSegments().get(0) + ")");
                break;
            case 2:
                constructQueryForBox(qb, 1);
                break;
            case 3:
            case 5:
            case 7:
            case 9:
            case 25:
                qb.setTables("sms");
                qb.appendWhere("(_id = " + url.getPathSegments().get(1) + ")");
                break;
            case 4:
                constructQueryForBox(qb, 2);
                break;
            case 6:
                constructQueryForBox(qb, 3);
                break;
            case 8:
                constructQueryForBox(qb, 4);
                break;
            case 10:
                qb.setTables("sms, (SELECT thread_id AS group_thread_id, MAX(date)AS group_date,COUNT(*) AS msg_count FROM sms GROUP BY thread_id) AS groups");
                qb.appendWhere("sms.thread_id = groups.group_thread_id AND sms.date =groups.group_date");
                qb.setProjectionMap(sConversationProjectionMap);
                break;
            case 11:
                try {
                    int threadID = Integer.parseInt(url.getPathSegments().get(1));
                    if (Log.isLoggable("SmsProvider", 2)) {
                        Log.d("SmsProvider", "query conversations: threadID=" + threadID);
                    }
                    qb.setTables("sms");
                    qb.appendWhere("thread_id = " + threadID);
                } catch (Exception e) {
                    Log.e("SmsProvider", "Bad conversation thread id: " + url.getPathSegments().get(1));
                    return null;
                }
                break;
            case 12:
            case 13:
            case 14:
            case 18:
            default:
                Log.e("SmsProvider", "Invalid request: " + url);
                return null;
            case 15:
                qb.setTables("raw");
                break;
            case 16:
                qb.setTables("attachments");
                break;
            case 17:
                qb.setTables("attachments");
                qb.appendWhere("(sms_id = " + url.getPathSegments().get(1) + ")");
                break;
            case 19:
                qb.setTables("canonical_addresses");
                if (projectionIn == null) {
                    projectionIn = sIDProjection;
                }
                break;
            case 20:
                qb.setTables("sms");
                qb.appendWhere("(_id = " + url.getPathSegments().get(1) + ")");
                break;
            case 21:
                qb.setTables("sr_pending");
                break;
            case 22:
                return getAllMessagesFromIcc(subId);
            case 23:
                String messageIndexString = url.getPathSegments().get(1);
                return getSingleMessageFromIcc(subId, messageIndexString);
            case 24:
                constructQueryForBox(qb, 5);
                break;
            case 26:
                constructQueryForBox(qb, 6);
                break;
            case 27:
                constructQueryForUndelivered(qb);
                break;
            case 28:
                String subIdString = url.getPathSegments().get(2);
                try {
                    return getAllMessagesFromIcc(Integer.parseInt(subIdString));
                } catch (NumberFormatException e2) {
                    throw new IllegalArgumentException("Bad SUB ID: " + subIdString);
                }
            case 29:
                String subIdString2 = url.getPathSegments().get(2);
                try {
                    int subId2 = Integer.parseInt(subIdString2);
                    String messageIndexString2 = url.getPathSegments().get(3);
                    return getSingleMessageFromIcc(subId2, messageIndexString2);
                } catch (NumberFormatException e3) {
                    throw new IllegalArgumentException("Bad SUB ID: " + subIdString2);
                }
        }
        String orderBy = null;
        if (!TextUtils.isEmpty(sort)) {
            orderBy = sort;
        } else if (qb.getTables().equals("sms")) {
            orderBy = "date DESC";
        }
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs, null, null, orderBy);
        ret.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
        return ret;
    }

    private Object[] convertIccToSms(SmsMessage message, int id) {
        Object[] row = new Object[13];
        row[0] = message.getServiceCenterAddress();
        int statusOnIcc = message.getStatusOnIcc();
        if (statusOnIcc == 1 || statusOnIcc == 3) {
            row[1] = message.getDisplayOriginatingAddress();
        } else {
            row[1] = message.mWrappedSmsMessage.getRecipientAddress();
        }
        row[2] = String.valueOf(message.getMessageClass());
        row[3] = message.getDisplayMessageBody();
        row[4] = Long.valueOf(message.getTimestampMillis());
        row[5] = Integer.valueOf(statusOnIcc);
        row[6] = Integer.valueOf(message.getIndexOnIcc());
        row[7] = Boolean.valueOf(message.isStatusReportMessage());
        row[8] = "sms";
        row[9] = 0;
        row[10] = 0;
        row[11] = 0;
        row[12] = Integer.valueOf(id);
        return row;
    }

    private Cursor getSingleMessageFromIcc(int subId, String messageIndexString) {
        try {
            Integer.parseInt(messageIndexString);
            SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
            long token = Binder.clearCallingIdentity();
            try {
                ArrayList<SmsMessage> messages = smsManager.getAllMessagesFromIcc();
                if (messages == null) {
                    throw new IllegalArgumentException("ICC message not retrieved");
                }
                SmsMessage message = messages.get(-1);
                if (message == null) {
                    throw new IllegalArgumentException("Message not retrieved. ID: " + messageIndexString);
                }
                MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, 1);
                cursor.addRow(convertIccToSms(message, 0));
                return withIccNotificationUri(cursor);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad SMS ICC ID: " + messageIndexString);
        }
    }

    private Cursor getAllMessagesFromIcc(int subId) {
        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
        long token = Binder.clearCallingIdentity();
        try {
            ArrayList<SmsMessage> messages = smsManager.getAllMessagesFromIcc();
            Binder.restoreCallingIdentity(token);
            int count = messages.size();
            MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, count);
            for (int i = 0; i < count; i++) {
                SmsMessage message = messages.get(i);
                if (message != null) {
                    cursor.addRow(convertIccToSms(message, i));
                }
            }
            return withIccNotificationUri(cursor);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private Cursor withIccNotificationUri(Cursor cursor) {
        cursor.setNotificationUri(getContext().getContentResolver(), ICC_URI);
        return cursor;
    }

    private void constructQueryForBox(SQLiteQueryBuilder qb, int type) {
        qb.setTables("sms");
        if (type != 0) {
            qb.appendWhere("type=" + type);
        }
    }

    private void constructQueryForUndelivered(SQLiteQueryBuilder qb) {
        qb.setTables("sms");
        qb.appendWhere("(type=4 OR type=5 OR type=6)");
    }

    @Override
    public String getType(Uri url) {
        switch (url.getPathSegments().size()) {
            case 0:
                return "vnd.android.cursor.dir/sms";
            case 1:
                try {
                    Integer.parseInt(url.getPathSegments().get(0));
                    return "vnd.android.cursor.item/sms";
                } catch (NumberFormatException e) {
                    return "vnd.android.cursor.dir/sms";
                }
            case 2:
                if (url.getPathSegments().get(0).equals("conversations")) {
                    return "vnd.android.cursor.item/sms-chat";
                }
                return "vnd.android.cursor.item/sms";
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        int callerUid = Binder.getCallingUid();
        long token = Binder.clearCallingIdentity();
        try {
            return insertInner(url, initialValues, callerUid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private Uri insertInner(Uri url, ContentValues initialValues, int callerUid) {
        ContentValues values;
        int type = 0;
        int match = sURLMatcher.match(url);
        String table = "sms";
        switch (match) {
            case 0:
                Integer typeObj = initialValues.getAsInteger("type");
                type = typeObj != null ? typeObj.intValue() : 1;
                break;
            case 2:
                type = 1;
                break;
            case 4:
                type = 2;
                break;
            case 6:
                type = 3;
                break;
            case 8:
                type = 4;
                break;
            case 15:
                table = "raw";
                break;
            case 16:
                table = "attachments";
                break;
            case 18:
                table = "canonical_addresses";
                break;
            case 21:
                table = "sr_pending";
                break;
            case 24:
                type = 5;
                break;
            case 26:
                type = 6;
                break;
            default:
                Log.e("SmsProvider", "Invalid request: " + url);
                return null;
        }
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        if (table.equals("sms")) {
            boolean addDate = false;
            boolean addType = false;
            if (initialValues == null) {
                values = new ContentValues(1);
                addDate = true;
                addType = true;
            } else {
                values = new ContentValues(initialValues);
                if (!initialValues.containsKey("date")) {
                    addDate = true;
                }
                if (!initialValues.containsKey("type")) {
                    addType = true;
                }
            }
            if (addDate) {
                values.put("date", new Long(System.currentTimeMillis()));
            }
            if (addType && type != 0) {
                values.put("type", Integer.valueOf(type));
            }
            Long threadId = values.getAsLong("thread_id");
            String address = values.getAsString("address");
            if ((threadId == null || threadId.longValue() == 0) && !TextUtils.isEmpty(address)) {
                values.put("thread_id", Long.valueOf(Telephony.Threads.getOrCreateThreadId(getContext(), address)));
            }
            if (values.getAsInteger("type").intValue() == 3) {
                db.delete("sms", "thread_id=? AND type=?", new String[]{values.getAsString("thread_id"), Integer.toString(3)});
            }
            if (type == 1) {
                if (values.getAsLong("person") == null && !TextUtils.isEmpty(address)) {
                    Cursor cursor = null;
                    Uri uri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(address));
                    try {
                        try {
                            cursor = getContext().getContentResolver().query(uri, CONTACT_QUERY_PROJECTION, null, null, null);
                            if (cursor.moveToFirst()) {
                                Long id = Long.valueOf(cursor.getLong(0));
                                values.put("person", id);
                            }
                        } catch (Exception ex) {
                            Log.e("SmsProvider", "insert: query contact uri " + uri + " caught ", ex);
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            } else {
                values.put("read", ONE);
            }
            if (ProviderUtil.shouldSetCreator(values, callerUid)) {
                values.put("creator", ProviderUtil.getPackageNamesByUid(getContext(), callerUid));
            }
        } else if (initialValues == null) {
            values = new ContentValues(1);
        } else {
            values = initialValues;
        }
        long rowID = db.insert(table, "body", values);
        if (table == "sms") {
            ContentValues cv = new ContentValues();
            cv.put("_id", Long.valueOf(rowID));
            cv.put("index_text", values.getAsString("body"));
            cv.put("source_id", Long.valueOf(rowID));
            cv.put("table_to_use", (Integer) 1);
            db.insert("words", "index_text", cv);
        }
        if (rowID > 0) {
            Uri uri2 = Uri.parse("content://" + table + "/" + rowID);
            if (Log.isLoggable("SmsProvider", 2)) {
                Log.d("SmsProvider", "insert " + uri2 + " succeeded");
            }
            notifyChange(uri2);
            return uri2;
        }
        Log.e("SmsProvider", "insert: failed!");
        return null;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        int count;
        int match = sURLMatcher.match(url);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int subId = SubscriptionManager.getDefaultSubId();
        switch (match) {
            case 0:
                count = db.delete("sms", where, whereArgs);
                if (count != 0) {
                    MmsSmsDatabaseHelper.updateAllThreads(db, where, whereArgs);
                }
                break;
            case 1:
                try {
                    int message_id = Integer.parseInt(url.getPathSegments().get(0));
                    count = MmsSmsDatabaseHelper.deleteOneSms(db, message_id);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Bad message id: " + url.getPathSegments().get(0));
                }
                break;
            case 11:
                try {
                    int threadID = Integer.parseInt(url.getPathSegments().get(1));
                    count = db.delete("sms", DatabaseUtils.concatenateWhere("thread_id=" + threadID, where), whereArgs);
                    MmsSmsDatabaseHelper.updateThread(db, threadID);
                } catch (Exception e2) {
                    throw new IllegalArgumentException("Bad conversation thread id: " + url.getPathSegments().get(1));
                }
                break;
            case 15:
                count = db.delete("raw", where, whereArgs);
                break;
            case 21:
                count = db.delete("sr_pending", where, whereArgs);
                break;
            case 23:
                String messageIndexString = url.getPathSegments().get(1);
                int count2 = deleteMessageFromIcc(subId, messageIndexString);
                return count2;
            case 29:
                String subIdString = url.getPathSegments().get(2);
                String messageIndexString2 = url.getPathSegments().get(3);
                try {
                    int subId2 = Integer.parseInt(subIdString);
                    int count3 = deleteMessageFromIcc(subId2, messageIndexString2);
                    return count3;
                } catch (NumberFormatException e3) {
                    throw new IllegalArgumentException("Bad SUB ID: " + subIdString);
                }
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
        if (count > 0) {
            notifyChange(url);
            return count;
        }
        return count;
    }

    private int deleteMessageFromIcc(int subId, String messageIndexString) {
        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
        long token = Binder.clearCallingIdentity();
        try {
            try {
                return smsManager.deleteMessageFromIcc(Integer.parseInt(messageIndexString)) ? 1 : 0;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bad SMS ICC ID: " + messageIndexString);
            }
        } finally {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(ICC_URI, null, true, -1);
            Uri iccUriWithSubId = Uri.withAppendedPath(ICC_SUBID_URI, String.valueOf(subId));
            cr.notifyChange(iccUriWithSubId, null, true, -1);
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        int callerUid = Binder.getCallingUid();
        String table = "sms";
        String extraWhere = null;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        switch (sURLMatcher.match(url)) {
            case 0:
            case 2:
            case 4:
            case 6:
            case 8:
            case 10:
            case 24:
            case 26:
                break;
            case 1:
                extraWhere = "_id=" + url.getPathSegments().get(0);
                break;
            case 3:
            case 5:
            case 7:
            case 9:
            case 25:
                extraWhere = "_id=" + url.getPathSegments().get(1);
                break;
            case 11:
                String threadId = url.getPathSegments().get(1);
                try {
                    Integer.parseInt(threadId);
                    extraWhere = "thread_id=" + threadId;
                } catch (Exception e) {
                    Log.e("SmsProvider", "Bad conversation thread id: " + threadId);
                }
                break;
            case 12:
            case 13:
            case 14:
            case 16:
            case 17:
            case 18:
            case 19:
            case 22:
            case 23:
            default:
                throw new UnsupportedOperationException("URI " + url + " not supported");
            case 15:
                table = "raw";
                break;
            case 20:
                extraWhere = "_id=" + url.getPathSegments().get(1);
                break;
            case 21:
                table = "sr_pending";
                break;
        }
        if (table.equals("sms") && ProviderUtil.shouldRemoveCreator(values, callerUid)) {
            Log.w("SmsProvider", ProviderUtil.getPackageNamesByUid(getContext(), callerUid) + " tries to update CREATOR");
            values.remove("creator");
        }
        int count = db.update(table, values, DatabaseUtils.concatenateWhere(where, extraWhere), whereArgs);
        if (count > 0) {
            if (Log.isLoggable("SmsProvider", 2)) {
                Log.d("SmsProvider", "update " + url + " succeeded");
            }
            notifyChange(url);
        }
        return count;
    }

    private void notifyChange(Uri uri) {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(uri, null, true, -1);
        cr.notifyChange(Telephony.MmsSms.CONTENT_URI, null, true, -1);
        cr.notifyChange(Uri.parse("content://mms-sms/conversations/"), null, true, -1);
    }
}
