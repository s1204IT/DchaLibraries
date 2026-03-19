package com.mediatek.gallery3d.video;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.android.gallery3d.R;
import com.mediatek.galleryportable.Log;

public class BookmarkEnhance {
    public static final String COLUMN_MEDIA_TYPE = "mime_type";
    private static final String COLUMN_POSITION = "position";
    public static final int INDEX_ADD_DATE = 3;
    public static final int INDEX_DATA = 1;
    public static final int INDEX_ID = 0;
    private static final int INDEX_MEDIA_TYPE = 6;
    public static final int INDEX_MIME_TYPE = 4;
    private static final int INDEX_POSITION = 5;
    public static final int INDEX_TITLE = 2;
    private static final String NULL_HOCK = "position";
    public static final String ORDER_COLUMN = "date_added ASC ";
    private static final String TAG = "VP_BookmarkEnhance";
    private static final String VIDEO_STREAMING_MEDIA_TYPE = "streaming";
    private final Context mContext;
    private final ContentResolver mCr;
    private static final Uri BOOKMARK_URI = Uri.parse("content://media/internal/bookmark");
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATA = "_data";
    public static final String COLUMN_TITLE = "_display_name";
    public static final String COLUMN_ADD_DATE = "date_added";
    private static final String COLUMN_MIME_TYPE = "media_type";
    public static final String[] PROJECTION = {COLUMN_ID, COLUMN_DATA, COLUMN_TITLE, COLUMN_ADD_DATE, COLUMN_MIME_TYPE};

    public BookmarkEnhance(Context context) {
        this.mContext = context;
        this.mCr = context.getContentResolver();
    }

    public Uri insert(String str, String str2, String str3, long j) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, str == null ? this.mContext.getString(R.string.default_title) : str);
        contentValues.put(COLUMN_DATA, str2);
        contentValues.put("position", Long.valueOf(j));
        contentValues.put(COLUMN_ADD_DATE, Long.valueOf(System.currentTimeMillis()));
        contentValues.put(COLUMN_MEDIA_TYPE, VIDEO_STREAMING_MEDIA_TYPE);
        contentValues.put(COLUMN_MIME_TYPE, str3);
        Uri uriInsert = this.mCr.insert(BOOKMARK_URI, contentValues);
        Log.v(TAG, "insert(" + str + "," + str2 + ", " + j + ") return " + uriInsert);
        return uriInsert;
    }

    public int delete(long j) {
        int iDelete = this.mCr.delete(ContentUris.withAppendedId(BOOKMARK_URI, j), null, null);
        Log.v(TAG, "delete(" + j + ") return " + iDelete);
        return iDelete;
    }

    public int deleteAll() {
        int iDelete = this.mCr.delete(BOOKMARK_URI, "mime_type=? ", new String[]{VIDEO_STREAMING_MEDIA_TYPE});
        Log.v(TAG, "deleteAll() return " + iDelete);
        return iDelete;
    }

    public boolean exists(String str) {
        boolean zMoveToFirst = false;
        Cursor cursorQuery = this.mCr.query(BOOKMARK_URI, PROJECTION, "_data=? and mime_type=? ", new String[]{str, VIDEO_STREAMING_MEDIA_TYPE}, null);
        if (cursorQuery != null) {
            zMoveToFirst = cursorQuery.moveToFirst();
            cursorQuery.close();
        }
        Log.v(TAG, "exists(" + str + ") return " + zMoveToFirst);
        return zMoveToFirst;
    }

    public Cursor query() {
        Cursor cursorQuery = this.mCr.query(BOOKMARK_URI, PROJECTION, "mime_type='streaming' ", null, ORDER_COLUMN);
        StringBuilder sb = new StringBuilder();
        sb.append("query() return cursor=");
        sb.append(cursorQuery == null ? -1 : cursorQuery.getCount());
        Log.v(TAG, sb.toString());
        return cursorQuery;
    }

    public int update(long j, String str, String str2, int i) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, str);
        contentValues.put(COLUMN_DATA, str2);
        contentValues.put("position", Integer.valueOf(i));
        int iUpdate = this.mCr.update(ContentUris.withAppendedId(BOOKMARK_URI, j), contentValues, null, null);
        Log.v(TAG, "update(" + j + ", " + str + ", " + str2 + ", " + i + ") return " + iUpdate);
        return iUpdate;
    }
}
