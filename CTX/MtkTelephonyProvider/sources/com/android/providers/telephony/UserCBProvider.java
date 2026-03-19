package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;

public class UserCBProvider extends ContentProvider {
    private SQLiteOpenHelper mOpenHelper;
    private static final Uri NOTIFICATION_URI = Uri.parse("content://usercb");
    private static final HashMap<String, String> sConversationProjectionMap = new HashMap<>();
    private static final String[] sIDProjection = {"_id"};
    private static final UriMatcher URI_MATCHER = new UriMatcher(-1);

    static {
        URI_MATCHER.addURI("usercb", null, 0);
    }

    @Override
    public boolean onCreate() {
        this.mOpenHelper = MmsSmsDatabaseHelper.getInstanceForCe(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String str3;
        Log.d("@M_UserCBProvider", "query begin uri = " + uri);
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        if (URI_MATCHER.match(uri) == 0) {
            sQLiteQueryBuilder.setTables("usercb");
        }
        if (TextUtils.isEmpty(str2)) {
            if (sQLiteQueryBuilder.getTables().equals("usercb")) {
                str2 = "_id ASC";
                str3 = str2;
            } else {
                str3 = null;
            }
        } else {
            str3 = str2;
        }
        Log.d("@M_UserCBProvider", "query getReadbleDatabase");
        SQLiteDatabase readableDatabase = this.mOpenHelper.getReadableDatabase();
        Log.d("@M_UserCBProvider", "query getReadbleDatabase qb.query begin");
        Cursor cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str3);
        Log.d("@M_UserCBProvider", "query getReadbleDatabase qb.query end");
        if (cursorQuery != null) {
            cursorQuery.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
        }
        return cursorQuery;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Log.d("@M_UserCBProvider", "insert begin");
        if (URI_MATCHER.match(uri) != 0) {
        }
        Log.d("@M_UserCBProvider", "insert match url end");
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        Log.d("@M_UserCBProvider", "insert mOpenHelper.getWritableDatabase end");
        if (contentValues == null) {
            return null;
        }
        ContentValues contentValues2 = new ContentValues(contentValues);
        long jInsert = writableDatabase.insert("usercb", "usercb-pdus", contentValues2);
        Log.d("@M_UserCBProvider", "insert table body end");
        if (jInsert > 0) {
            Uri uri2 = Uri.parse("content://usercb/" + jInsert);
            if (Log.isLoggable("UserCBProvider", 2)) {
                Log.d("UserCBProvider", "insert " + uri2 + " succeeded");
            }
            return uri2;
        }
        Log.d("UserCBProvider", "insert: failed! " + contentValues2.toString());
        Log.d("@M_UserCBProvider", "insert end");
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        Log.d("@M_UserCBProvider", "update begin");
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        if (URI_MATCHER.match(uri) != 0) {
            throw new UnsupportedOperationException("URI " + uri + " not supported");
        }
        int iUpdate = writableDatabase.update("usercb", contentValues, DatabaseUtils.concatenateWhere(str, null), strArr);
        if (iUpdate > 0) {
            Log.d("UserCBProvider", "update " + uri + " succeeded");
        }
        Log.d("@M_UserCBProvider", "update end");
        return iUpdate;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        this.mOpenHelper.getWritableDatabase();
        deleteOnce(uri, str, strArr);
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    private int deleteOnce(Uri uri, String str, String[] strArr) {
        int iMatch = URI_MATCHER.match(uri);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        Log.d("UserCBProvider", "Delete deleteOnce: " + iMatch);
        if (iMatch == 0) {
            return writableDatabase.delete("usercb", str, strArr);
        }
        throw new IllegalArgumentException("Unknown URL");
    }
}
