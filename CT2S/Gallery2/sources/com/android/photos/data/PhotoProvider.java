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
import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.NotificationCompat;
import com.android.gallery3d.common.ApiHelper;

public class PhotoProvider extends SQLiteContentProvider {
    protected ChangeNotification mNotifier = null;
    private static final String TAG = PhotoProvider.class.getSimpleName();
    static final Uri BASE_CONTENT_URI = new Uri.Builder().scheme("content").authority("com.android.gallery3d.photoprovider").build();
    protected static final String[] PROJECTION_COUNT = {"COUNT(*)"};
    private static final String[] PROJECTION_MIME_TYPE = {"mime_type"};
    protected static final String[] BASE_COLUMNS_ID = {"_id"};
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
    public int deleteInTransaction(Uri uri, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        int match = matchUri(uri);
        return deleteCascade(uri, match, addIdToSelection(match, selection), addIdToSelectionArgs(match, uri, selectionArgs));
    }

    @Override
    public String getType(Uri uri) {
        Cursor cursor = query(uri, PROJECTION_MIME_TYPE, null, null, null);
        String mimeType = null;
        if (cursor.moveToNext()) {
            mimeType = cursor.getString(0);
        }
        cursor.close();
        return mimeType;
    }

    @Override
    public Uri insertInTransaction(Uri uri, ContentValues values, boolean callerIsSyncAdapter) {
        int match = matchUri(uri);
        validateMatchTable(match);
        String table = getTableFromMatch(match, uri);
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        long id = db.insert(table, null, values);
        if (id == -1) {
            return null;
        }
        Uri insertedUri = ContentUris.withAppendedId(uri, id);
        postNotifyUri(insertedUri);
        return insertedUri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return query(uri, projection, selection, selectionArgs, sortOrder, (CancellationSignal) null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        String[] projection2 = replaceCount(projection);
        int match = matchUri(uri);
        String selection2 = addIdToSelection(match, selection);
        String[] selectionArgs2 = addIdToSelectionArgs(match, uri, selectionArgs);
        String table = getTableFromMatch(match, uri);
        Cursor c = query(table, projection2, selection2, selectionArgs2, sortOrder, cancellationSignal);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public int updateInTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        int rowsUpdated;
        int match = matchUri(uri);
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        if (match == 5) {
            rowsUpdated = modifyMetadata(db, values);
        } else {
            String selection2 = addIdToSelection(match, selection);
            String[] selectionArgs2 = addIdToSelectionArgs(match, uri, selectionArgs);
            String table = getTableFromMatch(match, uri);
            rowsUpdated = db.update(table, values, selection2, selectionArgs2);
        }
        postNotifyUri(uri);
        return rowsUpdated;
    }

    protected static String addIdToSelection(int match, String selection) {
        switch (match) {
            case 2:
            case 4:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                return DatabaseUtils.concatenateWhere(selection, "_id = ?");
            case 3:
            case 5:
            default:
                return selection;
        }
    }

    protected static String[] addIdToSelectionArgs(int match, Uri uri, String[] selectionArgs) {
        switch (match) {
            case 2:
            case 4:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                String[] whereArgs = {uri.getPathSegments().get(1)};
                return DatabaseUtils.appendSelectionArgs(selectionArgs, whereArgs);
            case 3:
            case 5:
            default:
                return selectionArgs;
        }
    }

    protected static String getTableFromMatch(int match, Uri uri) {
        switch (match) {
            case 1:
            case 2:
                return "photos";
            case 3:
            case 4:
                return "albums";
            case 5:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                return "metadata";
            case 7:
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                return "accounts";
            default:
                throw unknownUri(uri);
        }
    }

    @Override
    public SQLiteOpenHelper getDatabaseHelper(Context context) {
        return new PhotoDatabase(context, "photo.db");
    }

    private int modifyMetadata(SQLiteDatabase db, ContentValues values) {
        if (values.get("value") == null) {
            String[] selectionArgs = {values.getAsString("photo_id"), values.getAsString("key")};
            int rowCount = db.delete("metadata", "photo_id = ? AND key = ?", selectionArgs);
            return rowCount;
        }
        long rowId = db.replace("metadata", null, values);
        return rowId == -1 ? 0 : 1;
    }

    private int matchUri(Uri uri) {
        int match = sUriMatcher.match(uri);
        if (match == -1) {
            throw unknownUri(uri);
        }
        return match;
    }

    @Override
    protected void notifyChange(ContentResolver resolver, Uri uri, boolean syncToNetwork) {
        if (this.mNotifier != null) {
            this.mNotifier.notifyChange(uri, syncToNetwork);
        } else {
            super.notifyChange(resolver, uri, syncToNetwork);
        }
    }

    protected static IllegalArgumentException unknownUri(Uri uri) {
        return new IllegalArgumentException("Unknown Uri format: " + uri);
    }

    protected static String nestWhere(String matchColumn, String table, String nestedWhere) {
        String query = SQLiteQueryBuilder.buildQueryString(false, table, BASE_COLUMNS_ID, nestedWhere, null, null, null, null);
        return matchColumn + " IN (" + query + ")";
    }

    protected static String metadataSelectionFromPhotos(String where) {
        return nestWhere("photo_id", "photos", where);
    }

    protected static String photoSelectionFromAlbums(String where) {
        return nestWhere("album_id", "albums", where);
    }

    protected static String photoSelectionFromAccounts(String where) {
        return nestWhere("account_id", "accounts", where);
    }

    protected static String albumSelectionFromAccounts(String where) {
        return nestWhere("account_id", "accounts", where);
    }

    protected int deleteCascade(Uri uri, int match, String selection, String[] selectionArgs) {
        switch (match) {
            case 1:
            case 2:
                deleteCascade(Metadata.CONTENT_URI, 5, metadataSelectionFromPhotos(selection), selectionArgs);
                break;
            case 3:
            case 4:
                deleteCascade(Photos.CONTENT_URI, 1, photoSelectionFromAlbums(selection), selectionArgs);
                break;
            case 7:
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                deleteCascade(Photos.CONTENT_URI, 1, photoSelectionFromAccounts(selection), selectionArgs);
                deleteCascade(Albums.CONTENT_URI, 3, albumSelectionFromAccounts(selection), selectionArgs);
                break;
        }
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        String table = getTableFromMatch(match, uri);
        int deleted = db.delete(table, selection, selectionArgs);
        if (deleted > 0) {
            postNotifyUri(uri);
        }
        return deleted;
    }

    private static void validateMatchTable(int match) {
        switch (match) {
            case 1:
            case 3:
            case 5:
            case 7:
                return;
            case 2:
            case 4:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
            default:
                throw new IllegalArgumentException("Operation not allowed on an existing row.");
        }
    }

    protected Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String orderBy, CancellationSignal cancellationSignal) {
        SQLiteDatabase db = getDatabaseHelper().getReadableDatabase();
        return ApiHelper.HAS_CANCELLATION_SIGNAL ? db.query(false, table, columns, selection, selectionArgs, null, null, orderBy, null, cancellationSignal) : db.query(table, columns, selection, selectionArgs, null, null, orderBy);
    }

    protected static String[] replaceCount(String[] projection) {
        if (projection != null && projection.length == 1 && "_count".equals(projection[0])) {
            return PROJECTION_COUNT;
        }
        return projection;
    }
}
