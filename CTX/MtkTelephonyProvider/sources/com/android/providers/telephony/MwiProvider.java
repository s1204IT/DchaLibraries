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
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import mediatek.telephony.MtkTelephony;

public class MwiProvider extends ContentProvider {
    public static final boolean MTK_MWI_SUPPORT;
    private static final UriMatcher URI_MATCHER;
    private SQLiteOpenHelper mMwiOpenHelper;
    private static final Uri NOTIFICATION_URI = Uri.parse("content://mwimsg");
    public static final boolean MTK_IMS_SUPPORT = SystemProperties.get("persist.vendor.ims_support").equals("1");
    public static final boolean MTK_VOLTE_SUPPORT = SystemProperties.get("persist.vendor.volte_support").equals("1");

    static {
        MTK_MWI_SUPPORT = MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT;
        URI_MATCHER = new UriMatcher(-1);
        URI_MATCHER.addURI("mwimsg", null, 0);
        URI_MATCHER.addURI("mwimsg", "#", 1);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int iDelete;
        Log.d("@M_Mms/Provider/Mwi", "delete begin, uri = " + uri + ", selection = " + str);
        if (!MTK_MWI_SUPPORT) {
            return 0;
        }
        SQLiteDatabase writableDatabase = this.mMwiOpenHelper.getWritableDatabase();
        switch (URI_MATCHER.match(uri)) {
            case 0:
                iDelete = writableDatabase.delete("mwi", str, strArr);
                break;
            case 1:
                String str2 = "_id=" + uri.getPathSegments().get(0);
                if (str != null && !str.equals("")) {
                    str2 = str + " and " + str2;
                }
                iDelete = writableDatabase.delete("mwi", str2, strArr);
                break;
            default:
                Log.e("Mms/Provider/Mwi", "Unknown URI " + uri);
                return 0;
        }
        if (iDelete > 0) {
            notifyChange(uri);
        }
        Log.d("Mms/Provider/Mwi", "delete end, affectedRows = " + iDelete);
        return iDelete;
    }

    @Override
    public String getType(Uri uri) {
        if (URI_MATCHER.match(uri) == 0) {
            return "vnd.android.cursor.dir/mwimsg";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Log.d("@M_Mms/Provider/Mwi", "insert begin, uri = " + uri + ", values = " + contentValues);
        if (!MTK_MWI_SUPPORT || contentValues == null) {
            return null;
        }
        long jInsert = this.mMwiOpenHelper.getWritableDatabase().insert("mwi", null, contentValues);
        Uri uriWithAppendedId = ContentUris.withAppendedId(MtkTelephony.MtkMwi.CONTENT_URI, jInsert);
        if (jInsert > 0) {
            notifyChange(uri);
            Log.d("Mms/Provider/Mwi", "insert succeed, uri = " + uriWithAppendedId);
            return uriWithAppendedId;
        }
        Log.d("Mms/Provider/Mwi", "Failed to insert! " + contentValues.toString());
        return null;
    }

    @Override
    public boolean onCreate() {
        this.mMwiOpenHelper = MmsSmsDatabaseHelper.getInstanceForCe(getContext());
        if (!MTK_MWI_SUPPORT) {
            return false;
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        Log.d("@M_Mms/Provider/Mwi", "query begin, uri = " + uri + ", selection = " + str);
        if (!MTK_MWI_SUPPORT) {
            return null;
        }
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables("mwi");
        switch (URI_MATCHER.match(uri)) {
            case 0:
                break;
            case 1:
                sQLiteQueryBuilder.appendWhere("_id=" + uri.getPathSegments().get(0));
                break;
            default:
                Log.e("@M_TAG", "Unknown URI " + uri);
                return null;
        }
        if (TextUtils.isEmpty(str2)) {
            str2 = "msg_date ASC";
        }
        Cursor cursorQuery = sQLiteQueryBuilder.query(this.mMwiOpenHelper.getReadableDatabase(), strArr, str, strArr2, null, null, str2);
        if (cursorQuery != null) {
            cursorQuery.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
        }
        Log.d("@M_Mms/Provider/Mwi", "query end");
        return cursorQuery;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        String str2;
        Log.d("@M_Mms/Provider/Mwi", "update begin, uri = " + uri + ", values = " + contentValues + ", selection = " + str);
        SQLiteDatabase writableDatabase = this.mMwiOpenHelper.getWritableDatabase();
        int iUpdate = 0;
        if (URI_MATCHER.match(uri) == 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("_id=");
            sb.append(uri.getPathSegments().get(0));
            if (TextUtils.isEmpty(str)) {
                str2 = "";
            } else {
                str2 = " AND (" + str + ')';
            }
            sb.append(str2);
            iUpdate = writableDatabase.update("mwi", contentValues, sb.toString(), strArr);
        }
        if (iUpdate > 0) {
            notifyChange(uri);
        }
        Log.d("@M_Mms/Provider/Mwi", "update end, affectedRows = " + iUpdate);
        return iUpdate;
    }

    private void notifyChange(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null);
        Log.i("@M_Mms/Provider/Mwi", "notifyChange, uri = " + uri);
    }
}
