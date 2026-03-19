package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.provider.Telephony;
import android.util.Log;
import com.google.android.mms.util.SqliteWrapper;
import java.util.List;
import mediatek.telephony.MtkTelephony;

public class CbProvider extends ContentProvider {
    private SQLiteOpenHelper mMmsSmsOpenHelper;
    private SQLiteOpenHelper mOpenHelper;
    private static final Boolean DEBUG = true;
    private static final Uri THREAD_ID_URI = Uri.parse("content://cb/thread_id");
    private static String[] ID_PROJECTION = {"_id"};
    private static final UriMatcher URI_MATCHER = new UriMatcher(-1);

    static {
        URI_MATCHER.addURI("cb", "channel", 1);
        URI_MATCHER.addURI("cb", "messages", 2);
        URI_MATCHER.addURI("cb", "threads", 3);
        URI_MATCHER.addURI("cb", "addresses", 4);
        URI_MATCHER.addURI("cb", "thread_id", 5);
        URI_MATCHER.addURI("cb", "messages/#", 6);
        URI_MATCHER.addURI("cb", "channel/#", 7);
        URI_MATCHER.addURI("cb", "addresses/#", 8);
        URI_MATCHER.addURI("cb", "threads/#", 9);
        URI_MATCHER.addURI("cb", "cbraw", 10);
    }

    @Override
    public boolean onCreate() {
        this.mOpenHelper = new CbDatabaseHelper(getContext());
        this.mMmsSmsOpenHelper = MmsSmsDatabaseHelper.getInstanceForCe(getContext());
        return true;
    }

    private void notifyChange() {
        Log.i("Mms/Provider/Cb", "Notify change");
        ContentResolver contentResolver = getContext().getContentResolver();
        contentResolver.notifyChange(MtkTelephony.SmsCb.CONTENT_URI, null);
        contentResolver.notifyChange(MtkTelephony.SmsCb.Conversations.CONTENT_URI, null);
        contentResolver.notifyChange(Telephony.MmsSms.CONTENT_URI, null);
        MmsSmsProvider.notifyUnreadMessageNumberChanged(getContext());
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        SQLiteDatabase readableDatabase = this.mMmsSmsOpenHelper.getReadableDatabase();
        int iMatch = URI_MATCHER.match(uri);
        Log.d("@M_Mms/Provider/Cb", " query match " + iMatch);
        switch (iMatch) {
            case 1:
                sQLiteQueryBuilder.setTables("channel");
                sQLiteQueryBuilder.appendWhere("(sub_id = " + SmsProvider.getSubIdFromUri(uri) + ")");
                break;
            case 2:
                sQLiteQueryBuilder.setTables("cellbroadcast");
                Cursor cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2);
                if (cursorQuery != null) {
                    cursorQuery.setNotificationUri(getContext().getContentResolver(), MtkTelephony.SmsCb.CONTENT_URI);
                }
                return cursorQuery;
            case 3:
                sQLiteQueryBuilder.setTables("threads");
                Cursor cursorQuery2 = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2);
                Log.i("Mms/Provider/Cb", "query conversation from threads");
                if (cursorQuery2 != null) {
                    cursorQuery2.setNotificationUri(getContext().getContentResolver(), MtkTelephony.SmsCb.CONTENT_URI);
                }
                return cursorQuery2;
            case 4:
                sQLiteQueryBuilder.setTables("address");
                break;
            case 5:
                List<String> queryParameters = uri.getQueryParameters("recipient");
                if (queryParameters == null || queryParameters.size() == 0) {
                    return null;
                }
                return getThreadId(queryParameters);
            case 6:
                sQLiteQueryBuilder.setTables("cellbroadcast");
                sQLiteQueryBuilder.appendWhere("(thread_id = " + uri.getPathSegments().get(1) + ")");
                Cursor cursorQuery3 = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, buildConversationOrder(str2));
                if (cursorQuery3 != null) {
                    cursorQuery3.setNotificationUri(getContext().getContentResolver(), MtkTelephony.SmsCb.CONTENT_URI);
                }
                return cursorQuery3;
            case 7:
                sQLiteQueryBuilder.setTables("channel");
                sQLiteQueryBuilder.appendWhere("(_id = " + uri.getPathSegments().get(1) + ")");
                break;
            case 8:
                sQLiteQueryBuilder.setTables("address");
                sQLiteQueryBuilder.appendWhere("(_id = " + uri.getPathSegments().get(1) + ")");
                break;
            case 9:
                sQLiteQueryBuilder.setTables("channel");
                sQLiteQueryBuilder.appendWhere("(_id = " + uri.getPathSegments().get(1) + ")");
                break;
            case 10:
                sQLiteQueryBuilder.setTables("cbraw");
                break;
            default:
                return null;
        }
        Cursor cursorQuery4 = sQLiteQueryBuilder.query(this.mOpenHelper.getReadableDatabase(), strArr, str, strArr2, null, null, str2);
        if (cursorQuery4 != null) {
            cursorQuery4.setNotificationUri(getContext().getContentResolver(), MtkTelephony.SmsCb.CONTENT_URI);
        }
        return cursorQuery4;
    }

    private String buildConversationOrder(String str) {
        if (str == null) {
            return "date ASC";
        }
        return str;
    }

    private Cursor getThreadId(List<String> list) throws Throwable {
        long addressId = getAddressId(list.get(0));
        String strValueOf = String.valueOf(addressId);
        Log.i("Mms/Provider/Cb", "getThreadId THREAD_QUERY: SELECT _id FROM threads WHERE type = 3 AND recipient_ids = ?, address_id=" + addressId);
        if (DEBUG.booleanValue()) {
            Log.i("Mms/Provider/Cb", "getThreadId THREAD_QUERY: SELECT _id FROM threads WHERE type = 3 AND recipient_ids = ?, address_id=" + addressId);
        }
        Cursor cursorRawQuery = this.mMmsSmsOpenHelper.getReadableDatabase().rawQuery("SELECT _id FROM threads WHERE type = 3 AND recipient_ids = ?", new String[]{strValueOf});
        if (cursorRawQuery.getCount() == 0) {
            cursorRawQuery.close();
            if (DEBUG.booleanValue()) {
                Log.i("Mms/Provider/Cb", "getThreadId cursor zero, creating new threadid");
            }
            insertThread(strValueOf, 1);
            cursorRawQuery = this.mMmsSmsOpenHelper.getReadableDatabase().rawQuery("SELECT _id FROM threads WHERE type = 3 AND recipient_ids = ?", new String[]{strValueOf});
        }
        if (DEBUG.booleanValue()) {
            Log.i("Mms/Provider/Cb", "getThreadId cursor count: " + cursorRawQuery.getCount());
        }
        return cursorRawQuery;
    }

    private void insertThread(String str, int i) {
        ContentValues contentValues = new ContentValues(4);
        long jCurrentTimeMillis = System.currentTimeMillis();
        contentValues.put("date", Long.valueOf(jCurrentTimeMillis - (jCurrentTimeMillis % 1000)));
        contentValues.put("recipient_ids", str);
        contentValues.put("msg_count", (Integer) 0);
        contentValues.put("type", (Integer) 3);
        if (DEBUG.booleanValue()) {
            Log.i("Mms/Provider/Cb", "insertThread");
        }
        this.mMmsSmsOpenHelper.getWritableDatabase().insert("threads", null, contentValues);
        Log.d("@M_Mms/Provider/Cb", "Notify change insertThread");
        notifyChange();
    }

    private long getAddressId(String str) throws Throwable {
        Cursor cursorQuery;
        try {
            cursorQuery = this.mOpenHelper.getReadableDatabase().query("address", ID_PROJECTION, "address=?", new String[]{str}, null, null, null);
            try {
                if (cursorQuery.getCount() == 0) {
                    ContentValues contentValues = new ContentValues(1);
                    contentValues.put("address", str);
                    long jInsert = this.mOpenHelper.getWritableDatabase().insert("address", "address", contentValues);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return jInsert;
                }
                if (cursorQuery.moveToFirst()) {
                    long j = cursorQuery.getLong(cursorQuery.getColumnIndexOrThrow("_id"));
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return j;
                }
                if (cursorQuery == null) {
                    return -1L;
                }
                cursorQuery.close();
                return -1L;
            } catch (Throwable th) {
                th = th;
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private String getOrCreateThreadId(String str) {
        Uri.Builder builderBuildUpon = Uri.parse("content://mms-sms/threadID").buildUpon();
        builderBuildUpon.appendQueryParameter("recipient", str);
        builderBuildUpon.appendQueryParameter("cellbroadcast", str);
        Uri uriBuild = builderBuildUpon.build();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Cursor cursorQuery = SqliteWrapper.query(getContext(), getContext().getContentResolver(), uriBuild, ID_PROJECTION, (String) null, (String[]) null, (String) null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        return String.valueOf(cursorQuery.getLong(0));
                    }
                    Log.e("Mms/Provider/Cb", "getOrCreateThreadId returned no rows!");
                } finally {
                    cursorQuery.close();
                }
            }
            Log.d("Mms/Provider/Cb", "getOrCreateThreadId failed with address " + str);
            throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private ContentValues internalInsertMessages(ContentValues contentValues) {
        ContentValues contentValues2;
        if (contentValues != null) {
            contentValues2 = new ContentValues(contentValues);
        } else {
            contentValues2 = new ContentValues();
        }
        Long asLong = contentValues2.getAsLong("thread_id");
        String asString = contentValues2.getAsString("channel_id");
        if ((asLong == null || asLong.longValue() == 0) && asString != null) {
            contentValues2.put("thread_id", getOrCreateThreadId(asString));
        }
        if (!contentValues2.containsKey("sub_id")) {
            contentValues2.put("sub_id", (Integer) (-1));
        }
        if (!contentValues2.containsKey("body")) {
            contentValues2.put("body", "");
        }
        if (!contentValues2.containsKey("channel_id")) {
            contentValues2.put("channel_id", (Integer) (-1));
        }
        if (!contentValues2.containsKey("read")) {
            contentValues2.put("read", (Integer) 0);
        }
        if (!contentValues2.containsKey("date")) {
            contentValues2.put("date", (Integer) 0);
        }
        if (!contentValues2.containsKey("thread_id")) {
            contentValues2.put("thread_id", (Integer) 0);
        }
        return contentValues2;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        ContentValues contentValues2;
        ContentValues contentValues3;
        ContentValues contentValues4;
        ContentValues contentValues5;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        SQLiteDatabase writableDatabase2 = this.mMmsSmsOpenHelper.getWritableDatabase();
        int iMatch = URI_MATCHER.match(uri);
        Log.d("Mms/Provider/Cb", " insert match = " + iMatch);
        if (iMatch != 10) {
            switch (iMatch) {
                case 1:
                    if (contentValues != null) {
                        contentValues3 = new ContentValues(contentValues);
                    } else {
                        contentValues3 = new ContentValues();
                    }
                    if (!contentValues3.containsKey("name")) {
                        contentValues3.put("name", "");
                    }
                    if (!contentValues3.containsKey("number")) {
                        contentValues3.put("number", "");
                    }
                    if (!contentValues3.containsKey("enable")) {
                        contentValues3.put("enable", (Boolean) false);
                    }
                    if (!contentValues3.containsKey("sub_id")) {
                        contentValues3.put("sub_id", (Integer) (-1));
                    }
                    long jInsert = writableDatabase.insert("channel", null, contentValues3);
                    if (jInsert > 0) {
                        Uri uri2 = Uri.parse("content://channel/" + jInsert);
                        notifyChange();
                        return uri2;
                    }
                    break;
                case 2:
                    ContentValues contentValuesInternalInsertMessages = internalInsertMessages(contentValues);
                    long jInsert2 = writableDatabase2.insert("cellbroadcast", null, contentValuesInternalInsertMessages);
                    Log.d("Mms/Provider/Cb", "insert to cellbroadcast " + contentValuesInternalInsertMessages);
                    if (jInsert2 > 0) {
                        Uri uri3 = Uri.parse("content://messages/" + jInsert2);
                        notifyChange();
                        return uri3;
                    }
                    break;
                case 3:
                    if (contentValues != null) {
                        contentValues4 = new ContentValues(contentValues);
                    } else {
                        contentValues4 = new ContentValues();
                    }
                    if (!contentValues4.containsKey("snippet")) {
                        contentValues4.put("snippet", "");
                    }
                    if (!contentValues4.containsKey("channel_id")) {
                        contentValues4.put("channel_id", (Integer) (-1));
                    }
                    if (!contentValues4.containsKey("date")) {
                        contentValues4.put("date", (Integer) 0);
                    }
                    if (!contentValues4.containsKey("address_id")) {
                        contentValues4.put("address_id", (Integer) (-1));
                    }
                    if (!contentValues4.containsKey("msg_count")) {
                        contentValues4.put("msg_count", (Integer) 0);
                    }
                    contentValues4.put("type", (Integer) 3);
                    long jInsert3 = writableDatabase2.insert("threads", null, contentValues4);
                    Log.d("Mms/Provider/Cb", "insert conversation to threads");
                    if (jInsert3 > 0) {
                        Uri uri4 = Uri.parse("content://threads/" + jInsert3);
                        notifyChange();
                        return uri4;
                    }
                    break;
                case 4:
                    if (contentValues != null) {
                        contentValues5 = new ContentValues(contentValues);
                    } else {
                        contentValues5 = new ContentValues();
                    }
                    if (!contentValues5.containsKey("address")) {
                        contentValues5.put("address", (Integer) (-1));
                    }
                    long jInsert4 = writableDatabase.insert("address", null, contentValues5);
                    if (jInsert4 > 0) {
                        Uri uri5 = Uri.parse("content://addresses/" + jInsert4);
                        notifyChange();
                        return uri5;
                    }
                    break;
                default:
                    return null;
            }
        } else {
            if (contentValues != null) {
                contentValues2 = new ContentValues(contentValues);
            } else {
                contentValues2 = new ContentValues();
            }
            long jInsert5 = writableDatabase.insert("cbraw", null, contentValues2);
            if (jInsert5 > 0) {
                Uri uri6 = Uri.parse("content://cbraw/" + jInsert5);
                if (Log.isLoggable("Mms/Provider/Cb", 2)) {
                    Log.d("Mms/Provider/Cb", "insert " + uri6 + " succeeded");
                }
                return uri6;
            }
            Log.d("Mms/Provider/Cb", "insert: failed! " + contentValues2.toString());
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int iUpdate;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        SQLiteDatabase writableDatabase2 = this.mMmsSmsOpenHelper.getWritableDatabase();
        int iMatch = URI_MATCHER.match(uri);
        if (iMatch != 9) {
            switch (iMatch) {
                case 1:
                    iUpdate = writableDatabase.update("channel", contentValues, str, strArr);
                    break;
                case 2:
                    iUpdate = writableDatabase2.update("cellbroadcast", contentValues, str, strArr);
                    break;
                case 3:
                    iUpdate = writableDatabase2.update("threads", contentValues, str, strArr);
                    break;
                case 4:
                    iUpdate = writableDatabase.update("address", contentValues, str, strArr);
                    break;
                default:
                    throw new UnsupportedOperationException("Cannot update that URL: " + uri);
            }
        } else {
            String str2 = uri.getPathSegments().get(1);
            try {
                Integer.parseInt(str2);
                iUpdate = writableDatabase2.update("cellbroadcast", contentValues, DatabaseUtils.concatenateWhere(str, "thread_id=" + str2), strArr);
            } catch (Exception e) {
                Log.e("Mms/Provider/Cb", "Bad conversation thread id: " + str2);
                iUpdate = 0;
            }
        }
        Log.d("@M_Mms/Provider/Cb", "Notify change update");
        notifyChange();
        return iUpdate;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int iDeleteOnce;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        if (str != null && str.equals("ForMultiDelete")) {
            writableDatabase.beginTransaction();
            iDeleteOnce = 0;
            for (int i = 0; i < strArr.length; i++) {
                if (strArr[i] != null) {
                    int i2 = Integer.parseInt(strArr[i]);
                    Uri uriWithAppendedId = ContentUris.withAppendedId(uri, i2);
                    Log.i("Mms/Provider/Cb", "message_id is " + i2);
                    iDeleteOnce += deleteOnce(uriWithAppendedId, null, null);
                }
            }
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
        } else {
            iDeleteOnce = deleteOnce(uri, str, strArr);
        }
        Log.d("@M_Mms/Provider/Cb", "Notify change delete");
        notifyChange();
        return iDeleteOnce;
    }

    public int deleteOnce(Uri uri, String str, String[] strArr) {
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        SQLiteDatabase writableDatabase2 = this.mMmsSmsOpenHelper.getWritableDatabase();
        switch (URI_MATCHER.match(uri)) {
            case 1:
                return writableDatabase.delete("channel", str, strArr);
            case 2:
                int iDelete = writableDatabase2.delete("cellbroadcast", str, strArr);
                if (iDelete > 0) {
                    MmsSmsDatabaseHelper.updateThread(writableDatabase2, -1L);
                    return iDelete;
                }
                return iDelete;
            case 3:
                return writableDatabase2.delete("cellbroadcast", str, strArr);
            case 4:
                return writableDatabase.delete("address", str, strArr);
            case 5:
                return 0;
            case 6:
                try {
                    return writableDatabase2.delete("cellbroadcast", DatabaseUtils.concatenateWhere("_id=" + Integer.parseInt(uri.getPathSegments().get(1)), str), strArr);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Bad conversation thread id: " + uri.getPathSegments().get(1));
                }
            case 7:
            case 8:
            default:
                throw new UnsupportedOperationException("Cannot delete that URL: " + uri);
            case 9:
                try {
                    int i = Integer.parseInt(uri.getPathSegments().get(1));
                    int iDelete2 = writableDatabase2.delete("cellbroadcast", DatabaseUtils.concatenateWhere("thread_id=" + i, str), strArr);
                    MmsSmsDatabaseHelper.updateThread(writableDatabase, (long) i);
                    return iDelete2;
                } catch (Exception e2) {
                    throw new IllegalArgumentException("Bad conversation thread id: " + uri.getPathSegments().get(1));
                }
            case 10:
                return writableDatabase.delete("cbraw", str, strArr);
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case 1:
                return "vnd.android.cursor.item/cb-channel";
            case 2:
                return "vnd.android.cursor.dir/cb-messages";
            case 3:
                return "vnd.android.cursor.dir/cb-conversation";
            case 4:
                return "vnd.android.cursor.item/cb-address";
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }
}
