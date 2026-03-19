package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.android.providers.telephony.MmsSmsProvider;
import mediatek.telephony.MtkTelephony;

public class WapPushProvider extends ContentProvider {
    private static boolean mUseStrictPhoneNumberComparation;
    private SQLiteOpenHelper mWapPushOpenHelper;
    private static final Uri NOTIFICATION_URI = Uri.parse("content://wappush");
    private static final boolean MTK_WAPPUSH_SUPPORT = SystemProperties.get("ro.vendor.mtk_wappush_support").equals("1");
    private static final UriMatcher URI_MATCHER = new UriMatcher(-1);

    static {
        URI_MATCHER.addURI("wappush", null, 0);
        URI_MATCHER.addURI("wappush", "#", 1);
        URI_MATCHER.addURI("wappush", "thread_id/#", 4);
        URI_MATCHER.addURI("wappush", "si", 2);
        URI_MATCHER.addURI("wappush", "sl", 3);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int iDelete;
        Log.d("@M_Mms/Provider/WapPush", "delete begin, uri = " + uri + ", selection = " + str);
        if (!MTK_WAPPUSH_SUPPORT) {
            return 0;
        }
        SQLiteDatabase writableDatabase = this.mWapPushOpenHelper.getWritableDatabase();
        switch (URI_MATCHER.match(uri)) {
            case 0:
                iDelete = writableDatabase.delete("wappush", str, strArr);
                if (iDelete != 0) {
                    updatePushAllThread(writableDatabase, null, null);
                }
                break;
            case 1:
                try {
                    iDelete = deleteOnePushMsg(writableDatabase, Integer.parseInt(uri.getPathSegments().get(0)));
                } catch (Exception e) {
                    Log.e("@M_Mms/Provider/WapPush", "Delete: Bad Message ID");
                    return 0;
                }
                break;
            case 2:
                StringBuilder sb = new StringBuilder();
                sb.append("type=0");
                sb.append(TextUtils.isEmpty(str) ? "" : " AND (" + str + ')');
                iDelete = writableDatabase.delete("wappush", sb.toString(), strArr);
                if (iDelete != 0) {
                    updatePushAllThread(writableDatabase, "type=0", null);
                }
                break;
            case 3:
                StringBuilder sb2 = new StringBuilder();
                sb2.append("type=1");
                sb2.append(TextUtils.isEmpty(str) ? "" : " AND (" + str + ')');
                iDelete = writableDatabase.delete("wappush", sb2.toString(), strArr);
                if (iDelete != 0) {
                    updatePushAllThread(writableDatabase, "type=1", null);
                }
                break;
            case 4:
                try {
                    int i = Integer.parseInt(uri.getPathSegments().get(1));
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("thread_id=");
                    sb3.append(i);
                    sb3.append(TextUtils.isEmpty(str) ? "" : " AND (" + str + ')');
                    iDelete = writableDatabase.delete("wappush", sb3.toString(), strArr);
                    updatePushThread(writableDatabase, i);
                } catch (Exception e2) {
                    Log.e("@M_Mms/Provider/WapPush", "Delete: Bad conversation ID");
                    return 0;
                }
                break;
            default:
                Log.e("@M_Mms/Provider/WapPush", "Unknown URI " + uri);
                return 0;
        }
        if (iDelete > 0) {
            notifyChange(uri);
        }
        Log.d("@M_Mms/Provider/WapPush", "delete end, affectedRows = " + iDelete);
        return iDelete;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case 0:
            case 2:
            case 3:
            case 4:
                return "vnd.android.cursor.dir/wappush";
            case 1:
                return "vnd.android.cursor.item/wappush";
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Log.d("@M_Mms/Provider/WapPush", "insert begin, uri = " + uri + ", values = " + contentValues);
        if (!MTK_WAPPUSH_SUPPORT || contentValues == null) {
            return null;
        }
        int iMatch = URI_MATCHER.match(uri);
        if (iMatch != 0) {
            switch (iMatch) {
                case 2:
                    contentValues.put("type", (Integer) 0);
                    break;
                case 3:
                    contentValues.put("type", (Integer) 1);
                    break;
                default:
                    Log.e("@M_TAG", "Unknown URI " + uri);
                    return null;
            }
        }
        SQLiteDatabase writableDatabase = this.mWapPushOpenHelper.getWritableDatabase();
        String asString = contentValues.getAsString("address");
        long orCreatePushThreadId = -1;
        if (asString != null) {
            orCreatePushThreadId = getOrCreatePushThreadId(writableDatabase, asString);
            contentValues.put("thread_id", Long.valueOf(orCreatePushThreadId));
        }
        if (contentValues.getAsLong("date") == null) {
            contentValues.put("date", Long.valueOf(System.currentTimeMillis()));
        }
        long jInsert = writableDatabase.insert("wappush", null, contentValues);
        if (jInsert > 0) {
            String asString2 = "";
            if (contentValues.getAsString("text") != null && contentValues.getAsString("url") != null) {
                asString2 = contentValues.getAsString("text") + " " + contentValues.getAsString("url");
            } else if (contentValues.getAsString("url") != null && contentValues.getAsString("text") == null) {
                asString2 = contentValues.getAsString("url");
            } else if (contentValues.getAsString("text") != null && contentValues.getAsString("url") == null) {
                asString2 = contentValues.getAsString("text");
            }
            Log.d("@M_Mms/Provider/WapPush", "insert TABLE_WORDS begin");
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("_id", Long.valueOf(4 + jInsert));
            contentValues2.put("index_text", asString2);
            contentValues2.put("source_id", Long.valueOf(jInsert));
            contentValues2.put("table_to_use", (Integer) 3);
            writableDatabase.insert("words", "index_text", contentValues2);
            Log.d("@M_Mms/Provider/WapPush", "insert TABLE_WORDS end");
            Uri uriWithAppendedId = ContentUris.withAppendedId(MtkTelephony.WapPush.CONTENT_URI, jInsert);
            if (orCreatePushThreadId > 0) {
                updatePushThread(writableDatabase, orCreatePushThreadId);
            }
            notifyChange(uri);
            Log.d("@M_Mms/Provider/WapPush", "insert succeed, uri = " + uriWithAppendedId);
            return uriWithAppendedId;
        }
        Log.d("@M_Mms/Provider/WapPush", "Failed to insert! " + contentValues.toString());
        return null;
    }

    @Override
    public boolean onCreate() {
        this.mWapPushOpenHelper = MmsSmsDatabaseHelper.getInstanceForCe(getContext());
        mUseStrictPhoneNumberComparation = getContext().getResources().getBoolean(android.R.^attr-private.pointerIconWait);
        if (!MTK_WAPPUSH_SUPPORT) {
            return false;
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        Log.d("@M_Mms/Provider/WapPush", "query begin, uri = " + uri + ", selection = " + str);
        if (!MTK_WAPPUSH_SUPPORT) {
            return null;
        }
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables("wappush");
        switch (URI_MATCHER.match(uri)) {
            case 0:
                break;
            case 1:
                sQLiteQueryBuilder.appendWhere("_id=" + uri.getPathSegments().get(0));
                break;
            case 2:
                sQLiteQueryBuilder.appendWhere("type=0");
                break;
            case 3:
                sQLiteQueryBuilder.appendWhere("type=1");
                break;
            case 4:
                sQLiteQueryBuilder.appendWhere("thread_id=" + uri.getPathSegments().get(1));
                break;
            default:
                Log.e("@M_TAG", "Unknown URI " + uri);
                return null;
        }
        if (TextUtils.isEmpty(str2)) {
            str2 = "date ASC";
        }
        Cursor cursorQuery = sQLiteQueryBuilder.query(this.mWapPushOpenHelper.getReadableDatabase(), strArr, str, strArr2, null, null, str2);
        if (cursorQuery != null) {
            cursorQuery.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
        }
        Log.d("@M_Mms/Provider/WapPush", "query end");
        return cursorQuery;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int iUpdate;
        Log.d("@M_Mms/Provider/WapPush", "update begin, uri = " + uri + ", values = " + contentValues + ", selection = " + str);
        if (!MTK_WAPPUSH_SUPPORT) {
            return 0;
        }
        SQLiteDatabase writableDatabase = this.mWapPushOpenHelper.getWritableDatabase();
        switch (URI_MATCHER.match(uri)) {
            case 0:
                iUpdate = writableDatabase.update("wappush", contentValues, str, strArr);
                if (iUpdate > 0) {
                    updatePushAllThread(writableDatabase, str, strArr);
                }
                break;
            case 1:
                StringBuilder sb = new StringBuilder();
                sb.append("_id=");
                sb.append(uri.getPathSegments().get(0));
                sb.append(TextUtils.isEmpty(str) ? "" : " AND (" + str + ')');
                String string = sb.toString();
                iUpdate = writableDatabase.update("wappush", contentValues, string, strArr);
                if (iUpdate > 0) {
                    updatePushAllThread(writableDatabase, string, strArr);
                }
                break;
            case 2:
                StringBuilder sb2 = new StringBuilder();
                sb2.append("type=0");
                sb2.append(TextUtils.isEmpty(str) ? "" : " AND (" + str + ')');
                String string2 = sb2.toString();
                iUpdate = writableDatabase.update("wappush", contentValues, string2, strArr);
                if (iUpdate > 0) {
                    updatePushAllThread(writableDatabase, string2, strArr);
                }
                break;
            case 3:
                StringBuilder sb3 = new StringBuilder();
                sb3.append("type=1");
                sb3.append(TextUtils.isEmpty(str) ? "" : " AND (" + str + ')');
                String string3 = sb3.toString();
                iUpdate = writableDatabase.update("wappush", contentValues, string3, strArr);
                if (iUpdate > 0) {
                    updatePushAllThread(writableDatabase, string3, strArr);
                }
                break;
            case 4:
                try {
                    int i = Integer.parseInt(uri.getPathSegments().get(1));
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append("thread_id=");
                    sb4.append(i);
                    sb4.append(TextUtils.isEmpty(str) ? "" : " AND (" + str + ')');
                    iUpdate = writableDatabase.update("wappush", contentValues, sb4.toString(), strArr);
                    if (iUpdate > 0) {
                        updatePushThread(writableDatabase, i);
                    }
                } catch (Exception e) {
                    Log.e("@M_Mms/Provider/WapPush", "Update: Bad conversation ID");
                    return 0;
                }
                break;
            default:
                Log.e("@M_TAG", "Unknown URI " + uri);
                return 0;
        }
        if (iUpdate > 0) {
            notifyChange(uri);
        }
        Log.d("@M_Mms/Provider/WapPush", "update end, affectedRows = " + iUpdate);
        return iUpdate;
    }

    private long getOrCreatePushThreadId(SQLiteDatabase sQLiteDatabase, String str) {
        long j;
        MmsSmsProvider.MmsProviderLog.ipi("@M_Mms/Provider/WapPush", "getOrCreatePushThreadId address:" + str);
        Uri.Builder builderBuildUpon = Uri.parse("content://mms-sms/threadID").buildUpon();
        builderBuildUpon.appendQueryParameter("recipient", str);
        builderBuildUpon.appendQueryParameter("wappush", str);
        Uri uriBuild = builderBuildUpon.build();
        MmsSmsProvider.MmsProviderLog.ipi("@M_Mms/Provider/WapPush", "getOrCreatePushThreadId uri: " + uriBuild);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Cursor cursorQuery = getContext().getContentResolver().query(uriBuild, new String[]{"_id"}, null, null, null);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if (cursorQuery != null) {
                Log.v("@M_Mms/Provider/WapPush", "getOrCreateThreadId cursor cnt: " + cursorQuery.getCount());
                try {
                    if (cursorQuery.moveToFirst()) {
                        j = cursorQuery.getLong(0);
                    } else {
                        j = -1;
                    }
                } finally {
                    cursorQuery.close();
                }
            } else {
                j = -1;
            }
            if (j < 0) {
                Log.w("@M_Mms/Provider/WapPush", "getOrCreatePushThreadId: Failed to create an ThreadId");
                return -1L;
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("type", (Integer) 2);
            sQLiteDatabase.update("threads", contentValues, "_id=" + j, null);
            return j;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private void updatePushThread(SQLiteDatabase sQLiteDatabase, long j) {
        long j2 = 0;
        if (j < 0) {
            updatePushAllThread(sQLiteDatabase, null, null);
        }
        Log.i("@M_Mms/Provider/WapPush", "updatePushThread thread: " + j);
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("select * from threads where type <>2 AND _id=" + j, null);
        if (cursorRawQuery != null) {
            try {
                if (cursorRawQuery.getCount() != 0) {
                    return;
                }
            } finally {
            }
        }
        int count = 0;
        if (sQLiteDatabase.delete("threads", "_id = ? AND _id NOT IN (SELECT thread_id FROM wappush)", new String[]{String.valueOf(j)}) > 0) {
            return;
        }
        String str = "UPDATE threads SET message_count = (SELECT COUNT(_id) FROM wappush WHERE thread_id=" + j + "), readcount = (SELECT COUNT(_id) FROM wappush WHERE thread_id=" + j + " AND (read=1) )  WHERE threads._id = " + j + ";";
        Log.i("@M_Mms/Provider/WapPush", "update threads table for wappush: " + str);
        sQLiteDatabase.execSQL(str);
        Cursor cursorQuery = sQLiteDatabase.query("wappush", new String[]{"date", "text", "url"}, "thread_id = " + j, null, null, null, "date DESC LIMIT 1");
        String str2 = "";
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    long j3 = cursorQuery.getLong(0);
                    String string = cursorQuery.getString(1);
                    if (string == null || string.equals("")) {
                        string = cursorQuery.getString(2);
                    }
                    j2 = j3;
                    str2 = string;
                }
            } finally {
            }
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("date", Long.valueOf(j2));
        contentValues.put("snippet", str2);
        cursorQuery = sQLiteDatabase.query("wappush", new String[]{"read"}, "thread_id = " + j + " AND read = 0", null, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    contentValues.put("read", (Integer) 0);
                } else {
                    contentValues.put("read", (Integer) 1);
                }
            } finally {
            }
        }
        cursorRawQuery = sQLiteDatabase.rawQuery("SELECT thread_id FROM wappush WHERE error=1 AND thread_id = " + j + " LIMIT 1", null);
        if (cursorRawQuery != null) {
            try {
                count = cursorRawQuery.getCount();
            } finally {
            }
        }
        contentValues.put("error", Integer.valueOf(count));
        sQLiteDatabase.update("threads", contentValues, "_id = " + j, null);
    }

    private void updatePushAllThread(SQLiteDatabase sQLiteDatabase, String str, String[] strArr) {
        String str2;
        Log.i("@M_Mms/Provider/WapPush", "updatePushAllThread");
        if (str == null) {
            str2 = "";
        } else {
            str2 = "WHERE (" + str + ")";
        }
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id FROM threads WHERE type = 2  AND _id IN (SELECT DISTINCT thread_id FROM wappush " + str2 + ")", strArr);
        if (cursorRawQuery != null) {
            while (cursorRawQuery.moveToNext()) {
                try {
                    updatePushThread(sQLiteDatabase, cursorRawQuery.getInt(0));
                } finally {
                    cursorRawQuery.close();
                }
            }
        }
        sQLiteDatabase.delete("threads", "_id NOT IN (SELECT DISTINCT thread_id FROM wappush) AND type = 2", null);
    }

    private int deleteOnePushMsg(SQLiteDatabase sQLiteDatabase, int i) {
        Log.i("@M_Mms/Provider/WapPush", "deleteOnePushMsg messageId:" + i);
        Cursor cursorQuery = sQLiteDatabase.query("wappush", new String[]{"thread_id"}, "_id=" + i, null, null, null, null);
        int i2 = -1;
        if (cursorQuery != null) {
            if (cursorQuery.moveToFirst()) {
                i2 = cursorQuery.getInt(0);
            }
            cursorQuery.close();
        }
        int iDelete = sQLiteDatabase.delete("wappush", "_id=" + i, null);
        if (i2 > 0) {
            updatePushThread(sQLiteDatabase, i2);
        }
        return iDelete;
    }

    private void notifyChange(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null);
        Log.i("@M_Mms/Provider/WapPush", "notifyChange, uri = " + uri);
    }
}
