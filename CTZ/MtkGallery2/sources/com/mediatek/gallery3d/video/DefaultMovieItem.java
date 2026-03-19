package com.mediatek.gallery3d.video;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.MediaStore;
import com.mediatek.gallery3d.util.Log;

public class DefaultMovieItem implements IMovieItem {
    private static final int INVALID = -1;
    private static final int SCHEME_CONTENT = 2;
    private static final int SCHEME_FILE = 1;
    private static final int SCHEME_NONE = 0;
    private static final String TAG = "VP_DefaultMovieItem";
    private long mBuckedId;
    private Context mContext;
    private int mCurId;
    private String mDisplayName;
    private boolean mIsDrm;
    private String mMimeType;
    private String mTitle;
    private Uri mUri;
    private int mUriScheme;
    private String mVideoPath;
    private int mVideoType;

    public DefaultMovieItem(Context context, Uri uri, String str, String str2) throws Throwable {
        this.mUriScheme = 0;
        Log.d(TAG, "DefaultMovieItem() construct");
        this.mContext = context;
        initValueByUri(uri);
        this.mMimeType = str;
        judgeVideoType(this.mUri, this.mMimeType);
        this.mTitle = str2;
    }

    public DefaultMovieItem(Context context, String str, String str2, String str3) {
        this(context, Uri.parse(str), str2, str3);
    }

    @Override
    public Uri getUri() {
        Log.d(TAG, "getUri()");
        return this.mUri;
    }

    @Override
    public String getMimeType() {
        Log.d(TAG, "getMimeType() mMimeType= " + this.mMimeType);
        return this.mMimeType;
    }

    @Override
    public String getTitle() {
        Log.d(TAG, "getTitle() mTitle= " + this.mTitle);
        return this.mTitle;
    }

    @Override
    public void setTitle(String str) {
        Log.d(TAG, "setTitle() title= " + str);
        this.mTitle = str;
    }

    @Override
    public void setUri(Uri uri) throws Throwable {
        Log.d(TAG, "setUri() uri= " + uri);
        initValueByUri(uri);
        judgeVideoType(this.mUri, this.mMimeType);
    }

    @Override
    public void setMimeType(String str) {
        Log.d(TAG, "setMimeType() mimeType= " + str);
        this.mMimeType = str;
        judgeVideoType(this.mUri, this.mMimeType);
    }

    public String toString() {
        return "MovieItem(uri=" + this.mUri + ", mCurId=" + this.mCurId + ", mBuckedId=" + this.mBuckedId + ", mime=" + this.mMimeType + ", title=" + this.mTitle + ", isDrm=" + this.mIsDrm + ", VideoPath=" + this.mVideoPath + ", mDisplayName=" + this.mDisplayName + ")";
    }

    @Override
    public boolean isDrm() {
        Log.d(TAG, "isDrm() mIsDrm= " + this.mIsDrm);
        return this.mIsDrm;
    }

    @Override
    public boolean canBeRetrieved() {
        return this.mCurId != -1;
    }

    @Override
    public String getVideoPath() {
        Log.d(TAG, "getVideoPath() mVideoPath= " + this.mVideoPath);
        return this.mVideoPath;
    }

    @Override
    public boolean canShare() {
        return this.mUri.getScheme() == null || !this.mUri.getScheme().equals("file");
    }

    @Override
    public int getCurId() {
        Log.d(TAG, "getCurId() mCurId= " + this.mCurId);
        return this.mCurId;
    }

    @Override
    public long getBuckedId() {
        Log.d(TAG, "getBuckedId() mBuckedId= " + this.mBuckedId);
        return this.mBuckedId;
    }

    @Override
    public String getDisplayName() {
        Log.d(TAG, "getDisplayName() mDisplayName= " + this.mDisplayName);
        return this.mDisplayName;
    }

    @Override
    public int getVideoType() {
        Log.d(TAG, "getVideoType() mVideoType= " + this.mVideoType);
        return this.mVideoType;
    }

    @Override
    public void setVideoType(int i) {
        Log.d(TAG, "getVideoType() videoType= " + i);
        this.mVideoType = i;
    }

    private void judgeVideoType(Uri uri, String str) {
        this.mVideoType = MovieUtils.judgeVideoType(uri, str);
        Log.d(TAG, "judgeVideoType() mVideoType= " + this.mVideoType);
    }

    private void setDefaultValue(Uri uri) {
        Log.d(TAG, "setDefaultValue()");
        this.mUri = uri;
        this.mVideoPath = null;
        this.mIsDrm = false;
        this.mCurId = -1;
        this.mBuckedId = -1L;
        this.mDisplayName = null;
    }

    private void initValueByUri(Uri uri) throws Throwable {
        Log.d(TAG, "initValueByUri() uri= " + uri);
        setDefaultValue(uri);
        getInfoFromDB(uri);
    }

    private int getCurId(Cursor cursor) {
        int columnIndex = cursor.getColumnIndex(BookmarkEnhance.COLUMN_ID);
        Log.d(TAG, "getCurId() index= " + columnIndex);
        if (columnIndex != -1) {
            this.mCurId = cursor.getInt(columnIndex);
        }
        return this.mCurId;
    }

    private Long getBucketId(Cursor cursor) {
        int columnIndex = cursor.getColumnIndex("bucket_id");
        Log.d(TAG, "getBucketId() index= " + columnIndex);
        if (columnIndex != -1) {
            this.mBuckedId = cursor.getLong(columnIndex);
        }
        return Long.valueOf(this.mBuckedId);
    }

    private String getVideoPath(Cursor cursor) {
        int columnIndex = cursor.getColumnIndex(BookmarkEnhance.COLUMN_DATA);
        Log.d(TAG, "getVideoPath() index= " + columnIndex);
        if (columnIndex != -1) {
            this.mVideoPath = cursor.getString(columnIndex);
        }
        return this.mVideoPath;
    }

    private boolean isDrm(Cursor cursor) {
        int columnIndex = cursor.getColumnIndex("is_drm");
        Log.d(TAG, "isDrm() index= " + columnIndex);
        if (columnIndex != -1) {
            this.mIsDrm = 1 == cursor.getInt(columnIndex);
        }
        return this.mIsDrm;
    }

    private String getDisplayName(Cursor cursor) {
        int columnIndex = cursor.getColumnIndex(BookmarkEnhance.COLUMN_TITLE);
        Log.d(TAG, "getDisplayName() index= " + columnIndex);
        if (columnIndex != -1) {
            this.mDisplayName = cursor.getString(columnIndex);
        }
        return this.mDisplayName;
    }

    private boolean retrieveInfoFromDb(Uri uri, String[] strArr, String str) throws Throwable {
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = this.mContext.getContentResolver().query(uri, strArr, str, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            this.mCurId = getCurId(cursorQuery);
                            this.mBuckedId = getBucketId(cursorQuery).longValue();
                            this.mVideoPath = getVideoPath(cursorQuery);
                            this.mDisplayName = getDisplayName(cursorQuery);
                            this.mIsDrm = isDrm(cursorQuery);
                        }
                    } catch (SQLiteException e) {
                        e = e;
                        cursor = cursorQuery;
                        Log.e(TAG, "retrieveInfoFromDb encountered SQLiteException, " + e);
                        if (cursor != null) {
                            cursor.close();
                        }
                        return false;
                    } catch (IllegalArgumentException e2) {
                        e = e2;
                        cursor = cursorQuery;
                        Log.e(TAG, "retrieveInfoFromDb encountered IllegalArgumentException, " + e);
                        if (cursor != null) {
                            cursor.close();
                        }
                        return false;
                    } catch (Throwable th) {
                        th = th;
                        cursor = cursorQuery;
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery == null) {
                    return true;
                }
                cursorQuery.close();
                return true;
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (SQLiteException e3) {
            e = e3;
        } catch (IllegalArgumentException e4) {
            e = e4;
        }
    }

    private void doRetrieve(Uri uri, String str) throws Throwable {
        Log.d(TAG, "doRetrieve() where= " + str);
        if (!retrieveInfoFromDb(uri, new String[]{BookmarkEnhance.COLUMN_ID, "bucket_id", BookmarkEnhance.COLUMN_DATA, BookmarkEnhance.COLUMN_TITLE, "is_drm"}, str)) {
            retrieveWithoutDrm(uri, str);
        }
        createUri(uri);
        Log.d(TAG, toString());
    }

    private void retrieveWithoutDrm(Uri uri, String str) throws Throwable {
        retrieveInfoFromDb(uri, new String[]{BookmarkEnhance.COLUMN_ID, "bucket_id", BookmarkEnhance.COLUMN_DATA, BookmarkEnhance.COLUMN_TITLE}, str);
    }

    private void createUri(Uri uri) {
        if (this.mCurId != -1 && this.mUriScheme == 1) {
            this.mUri = ContentUris.withAppendedId(uri, this.mCurId);
        }
    }

    private void retrieveByContentUri(Uri uri) throws Throwable {
        doRetrieve(uri, null);
    }

    private void retrieveByFileUri(Uri uri) throws Throwable {
        String path = uri.getPath();
        Log.d(TAG, "retrieveFileUri, videoPath " + path);
        if (path == null) {
            return;
        }
        doRetrieve(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "_data = '" + path.replaceAll("'", "''") + "'");
    }

    private int getUriScheme(Uri uri) {
        if (uri.getScheme() == null) {
            this.mUriScheme = 0;
        } else if (uri.getScheme().equals("file")) {
            this.mUriScheme = 1;
        } else if (uri.getScheme().equals("content")) {
            this.mUriScheme = 2;
        }
        Log.d(TAG, "getUriScheme() mUriScheme= " + this.mUriScheme);
        return this.mUriScheme;
    }

    private void getInfoFromDB(Uri uri) throws Throwable {
        switch (getUriScheme(uri)) {
            case 1:
                retrieveByFileUri(uri);
                break;
            case 2:
                retrieveByContentUri(uri);
                break;
        }
    }
}
