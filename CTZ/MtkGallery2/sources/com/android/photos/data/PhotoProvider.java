package com.android.photos.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.BaseColumns;
import com.android.gallery3d.common.ApiHelper;
import com.mediatek.gallery3d.video.BookmarkEnhance;

public class PhotoProvider extends SQLiteContentProvider {
    protected ChangeNotification mNotifier = null;
    private static final String TAG = PhotoProvider.class.getSimpleName();
    static final Uri BASE_CONTENT_URI = new Uri.Builder().scheme("content").authority("com.android.gallery3d.photoprovider").build();
    protected static final String[] PROJECTION_COUNT = {"COUNT(*)"};
    private static final String[] PROJECTION_MIME_TYPE = {BookmarkEnhance.COLUMN_MEDIA_TYPE};
    protected static final String[] BASE_COLUMNS_ID = {BookmarkEnhance.COLUMN_ID};
    protected static final UriMatcher sUriMatcher = new UriMatcher(-1);

    public interface Albums extends BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(PhotoProvider.BASE_CONTENT_URI, "albums");
    }

    public interface ChangeNotification {
        void notifyChange(Uri uri, boolean z);
    }

    public interface Metadata extends BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(PhotoProvider.BASE_CONTENT_URI, "metadata");
    }

    public interface Photos extends BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(PhotoProvider.BASE_CONTENT_URI, "photos");
    }

    static {
        sUriMatcher.addURI("com.android.gallery3d.photoprovider", "photos", 1);
        sUriMatcher.addURI("com.android.gallery3d.photoprovider", "photos/#", 2);
        sUriMatcher.addURI("com.android.gallery3d.photoprovider", "albums", 3);
        sUriMatcher.addURI("com.android.gallery3d.photoprovider", "albums/#", 4);
        sUriMatcher.addURI("com.android.gallery3d.photoprovider", "metadata", 5);
        sUriMatcher.addURI("com.android.gallery3d.photoprovider", "metadata/#", 6);
        sUriMatcher.addURI("com.android.gallery3d.photoprovider", "accounts", 7);
        sUriMatcher.addURI("com.android.gallery3d.photoprovider", "accounts/#", 8);
    }

    @Override
    public int deleteInTransaction(Uri uri, String str, String[] strArr, boolean z) {
        int iMatchUri = matchUri(uri);
        return deleteCascade(uri, iMatchUri, addIdToSelection(iMatchUri, str), addIdToSelectionArgs(iMatchUri, uri, strArr));
    }

    @Override
    public String getType(Uri uri) {
        String string;
        Cursor cursorQuery = query(uri, PROJECTION_MIME_TYPE, null, null, null);
        if (cursorQuery.moveToNext()) {
            string = cursorQuery.getString(0);
        } else {
            string = null;
        }
        cursorQuery.close();
        return string;
    }

    @Override
    public Uri insertInTransaction(Uri uri, ContentValues contentValues, boolean z) {
        int iMatchUri = matchUri(uri);
        validateMatchTable(iMatchUri);
        long jInsert = getDatabaseHelper().getWritableDatabase().insert(getTableFromMatch(iMatchUri, uri), null, contentValues);
        if (jInsert == -1) {
            return null;
        }
        Uri uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert);
        postNotifyUri(uriWithAppendedId);
        return uriWithAppendedId;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return query(uri, strArr, str, strArr2, str2, (CancellationSignal) null);
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) {
        String[] strArrReplaceCount = replaceCount(strArr);
        int iMatchUri = matchUri(uri);
        Cursor cursorQuery = query(getTableFromMatch(iMatchUri, uri), strArrReplaceCount, addIdToSelection(iMatchUri, str), addIdToSelectionArgs(iMatchUri, uri, strArr2), str2, cancellationSignal);
        if (cursorQuery != null) {
            cursorQuery.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursorQuery;
    }

    @Override
    public int updateInTransaction(Uri uri, ContentValues contentValues, String str, String[] strArr, boolean z) {
        int iUpdate;
        int iMatchUri = matchUri(uri);
        SQLiteDatabase writableDatabase = getDatabaseHelper().getWritableDatabase();
        if (iMatchUri == 5) {
            iUpdate = modifyMetadata(writableDatabase, contentValues);
        } else {
            iUpdate = writableDatabase.update(getTableFromMatch(iMatchUri, uri), contentValues, addIdToSelection(iMatchUri, str), addIdToSelectionArgs(iMatchUri, uri, strArr));
        }
        postNotifyUri(uri);
        return iUpdate;
    }

    protected static String addIdToSelection(int i, String str) {
        if (i != 2 && i != 4 && i != 6) {
            return str;
        }
        return DatabaseUtils.concatenateWhere(str, "_id = ?");
    }

    protected static String[] addIdToSelectionArgs(int i, Uri uri, String[] strArr) {
        if (i != 2 && i != 4 && i != 6) {
            return strArr;
        }
        return DatabaseUtils.appendSelectionArgs(strArr, new String[]{uri.getPathSegments().get(1)});
    }

    protected static String getTableFromMatch(int i, Uri uri) {
        switch (i) {
            case 1:
            case 2:
                return "photos";
            case 3:
            case 4:
                return "albums";
            case 5:
            case 6:
                return "metadata";
            case 7:
            case 8:
                return "accounts";
            default:
                throw unknownUri(uri);
        }
    }

    @Override
    public SQLiteOpenHelper getDatabaseHelper(Context context) {
        return new PhotoDatabase(context, "photo.db");
    }

    private int modifyMetadata(SQLiteDatabase sQLiteDatabase, ContentValues contentValues) {
        if (contentValues.get("value") == null) {
            return sQLiteDatabase.delete("metadata", "photo_id = ? AND key = ?", new String[]{contentValues.getAsString("photo_id"), contentValues.getAsString("key")});
        }
        return sQLiteDatabase.replace("metadata", null, contentValues) == -1 ? 0 : 1;
    }

    private int matchUri(Uri uri) {
        int iMatch = sUriMatcher.match(uri);
        if (iMatch == -1) {
            throw unknownUri(uri);
        }
        return iMatch;
    }

    @Override
    protected void notifyChange(ContentResolver contentResolver, Uri uri, boolean z) {
        if (this.mNotifier != null) {
            this.mNotifier.notifyChange(uri, z);
        } else {
            super.notifyChange(contentResolver, uri, z);
        }
    }

    protected static IllegalArgumentException unknownUri(Uri uri) {
        return new IllegalArgumentException("Unknown Uri format: " + uri);
    }

    protected static String nestWhere(String str, String str2, String str3) {
        return str + " IN (" + SQLiteQueryBuilder.buildQueryString(false, str2, BASE_COLUMNS_ID, str3, null, null, null, null) + ")";
    }

    protected static String metadataSelectionFromPhotos(String str) {
        return nestWhere("photo_id", "photos", str);
    }

    protected static String photoSelectionFromAlbums(String str) {
        return nestWhere("album_id", "albums", str);
    }

    protected static String photoSelectionFromAccounts(String str) {
        return nestWhere("account_id", "accounts", str);
    }

    protected static String albumSelectionFromAccounts(String str) {
        return nestWhere("account_id", "accounts", str);
    }

    protected int deleteCascade(Uri uri, int i, String str, String[] strArr) {
        switch (i) {
            case 1:
            case 2:
                deleteCascade(Metadata.CONTENT_URI, 5, metadataSelectionFromPhotos(str), strArr);
                break;
            case 3:
            case 4:
                deleteCascade(Photos.CONTENT_URI, 1, photoSelectionFromAlbums(str), strArr);
                break;
            case 7:
            case 8:
                deleteCascade(Photos.CONTENT_URI, 1, photoSelectionFromAccounts(str), strArr);
                deleteCascade(Albums.CONTENT_URI, 3, albumSelectionFromAccounts(str), strArr);
                break;
        }
        int iDelete = getDatabaseHelper().getWritableDatabase().delete(getTableFromMatch(i, uri), str, strArr);
        if (iDelete > 0) {
            postNotifyUri(uri);
        }
        return iDelete;
    }

    private static void validateMatchTable(int i) {
        if (i != 1 && i != 3 && i != 5 && i != 7) {
            throw new IllegalArgumentException("Operation not allowed on an existing row.");
        }
    }

    protected Cursor query(String str, String[] strArr, String str2, String[] strArr2, String str3, CancellationSignal cancellationSignal) {
        SQLiteDatabase readableDatabase = getDatabaseHelper().getReadableDatabase();
        if (ApiHelper.HAS_CANCELLATION_SIGNAL) {
            return readableDatabase.query(false, str, strArr, str2, strArr2, null, null, str3, null, cancellationSignal);
        }
        return readableDatabase.query(str, strArr, str2, strArr2, null, null, str3);
    }

    protected static String[] replaceCount(String[] strArr) {
        if (strArr != null && strArr.length == 1 && "_count".equals(strArr[0])) {
            return PROJECTION_COUNT;
        }
        return strArr;
    }
}
