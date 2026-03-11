package com.android.browser.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.FileUtils;
import android.text.TextUtils;
import java.io.File;

public class SnapshotProvider extends ContentProvider {
    static final String[] DELETE_PROJECTION;
    SnapshotDatabaseHelper mOpenHelper;
    public static final Uri AUTHORITY_URI = Uri.parse("content://com.android.browser.snapshots");
    static final UriMatcher URI_MATCHER = new UriMatcher(-1);
    static final byte[] NULL_BLOB_HACK = new byte[0];

    static {
        URI_MATCHER.addURI("com.android.browser.snapshots", "snapshots", 10);
        URI_MATCHER.addURI("com.android.browser.snapshots", "snapshots/#", 11);
        DELETE_PROJECTION = new String[]{"viewstate_path"};
    }

    static final class SnapshotDatabaseHelper extends SQLiteOpenHelper {
        public SnapshotDatabaseHelper(Context context) {
            super(context, "snapshots.db", (SQLiteDatabase.CursorFactory) null, 3);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE snapshots(_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,url TEXT NOT NULL,date_created INTEGER,favicon BLOB,thumbnail BLOB,background INTEGER,view_state BLOB NOT NULL,viewstate_path TEXT,viewstate_size INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("DROP TABLE snapshots");
                onCreate(db);
            }
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE snapshots ADD COLUMN viewstate_path TEXT");
                db.execSQL("ALTER TABLE snapshots ADD COLUMN viewstate_size INTEGER");
                db.execSQL("UPDATE snapshots SET viewstate_size = length(view_state)");
            }
        }
    }

    static File getOldDatabasePath(Context context) {
        File dir = context.getExternalFilesDir(null);
        return new File(dir, "snapshots.db");
    }

    private void migrateToDataFolder() {
        File dbPath = getContext().getDatabasePath("snapshots.db");
        if (!dbPath.exists()) {
            File oldPath = getOldDatabasePath(getContext());
            if (oldPath.exists()) {
                if (!oldPath.renameTo(dbPath)) {
                    FileUtils.copyFile(oldPath, dbPath);
                }
                oldPath.delete();
            }
        }
    }

    @Override
    public boolean onCreate() {
        migrateToDataFolder();
        this.mOpenHelper = new SnapshotDatabaseHelper(getContext());
        return true;
    }

    SQLiteDatabase getWritableDatabase() {
        return this.mOpenHelper.getWritableDatabase();
    }

    SQLiteDatabase getReadableDatabase() {
        return this.mOpenHelper.getReadableDatabase();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = getReadableDatabase();
        if (db == null) {
            return null;
        }
        int match = URI_MATCHER.match(uri);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String limit = uri.getQueryParameter("limit");
        switch (match) {
            case 10:
                break;
            case 11:
                selection = DatabaseUtils.concatenateWhere(selection, "_id=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
                break;
            default:
                throw new UnsupportedOperationException("Unknown URL " + uri.toString());
        }
        qb.setTables("snapshots");
        Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);
        cursor.setNotificationUri(getContext().getContentResolver(), AUTHORITY_URI);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        if (db == null) {
            return null;
        }
        int match = URI_MATCHER.match(uri);
        switch (match) {
            case 10:
                if (!values.containsKey("view_state")) {
                    values.put("view_state", NULL_BLOB_HACK);
                }
                long id = db.insert("snapshots", "title", values);
                if (id < 0) {
                    return null;
                }
                Uri inserted = ContentUris.withAppendedId(uri, id);
                getContext().getContentResolver().notifyChange(inserted, (ContentObserver) null, false);
                return inserted;
            default:
                throw new UnsupportedOperationException("Unknown insert URI " + uri);
        }
    }

    private void deleteDataFiles(SQLiteDatabase db, String selection, String[] selectionArgs) {
        Cursor c = db.query("snapshots", DELETE_PROJECTION, selection, selectionArgs, null, null, null);
        Context context = getContext();
        while (c.moveToNext()) {
            String filename = c.getString(0);
            if (!TextUtils.isEmpty(filename)) {
                File f = context.getFileStreamPath(filename);
                if (f.exists() && !f.delete()) {
                    f.deleteOnExit();
                }
            }
        }
        c.close();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = getWritableDatabase();
        if (db == null) {
            return 0;
        }
        int match = URI_MATCHER.match(uri);
        switch (match) {
            case 10:
                break;
            case 11:
                selection = DatabaseUtils.concatenateWhere(selection, "snapshots._id=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
                break;
            default:
                throw new UnsupportedOperationException("Unknown delete URI " + uri);
        }
        deleteDataFiles(db, selection, selectionArgs);
        int deleted = db.delete("snapshots", selection, selectionArgs);
        if (deleted > 0) {
            getContext().getContentResolver().notifyChange(uri, (ContentObserver) null, false);
            return deleted;
        }
        return deleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("not implemented");
    }
}
