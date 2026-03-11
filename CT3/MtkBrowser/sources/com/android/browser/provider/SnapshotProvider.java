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

    public interface Snapshots {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(SnapshotProvider.AUTHORITY_URI, "snapshots");
    }

    static {
        URI_MATCHER.addURI("com.android.browser.snapshots", "snapshots", 10);
        URI_MATCHER.addURI("com.android.browser.snapshots", "snapshots/#", 11);
        DELETE_PROJECTION = new String[]{"viewstate_path", "job_id"};
    }

    static final class SnapshotDatabaseHelper extends SQLiteOpenHelper {
        public SnapshotDatabaseHelper(Context context) {
            super(context, "snapshots.db", (SQLiteDatabase.CursorFactory) null, 4);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE snapshots(_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,url TEXT NOT NULL,date_created INTEGER,favicon BLOB,thumbnail BLOB,background INTEGER,view_state BLOB NOT NULL,viewstate_path TEXT,viewstate_size INTEGER,job_id INTEGER,progress INTEGER,is_done INTEGER);");
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
            if (oldVersion >= 4) {
                return;
            }
            db.execSQL("ALTER TABLE snapshots ADD COLUMN job_id INTEGER");
            db.execSQL("ALTER TABLE snapshots ADD COLUMN progress INTEGER");
            db.execSQL("ALTER TABLE snapshots ADD COLUMN is_done INTEGER");
            db.execSQL("UPDATE snapshots SET job_id = -1 ");
            db.execSQL("UPDATE snapshots SET progress = 100 ");
            db.execSQL("UPDATE snapshots SET is_done = 1 ");
        }
    }

    static File getOldDatabasePath(Context context) {
        File dir = context.getExternalFilesDir(null);
        return new File(dir, "snapshots.db");
    }

    private void migrateToDataFolder() {
        File dbPath = getContext().getDatabasePath("snapshots.db");
        if (dbPath.exists()) {
            return;
        }
        File oldPath = getOldDatabasePath(getContext());
        if (!oldPath.exists()) {
            return;
        }
        if (!oldPath.renameTo(dbPath)) {
            FileUtils.copyFile(oldPath, dbPath);
        }
        oldPath.delete();
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
        db.beginTransaction();
        try {
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
            db.setTransactionSuccessful();
            return cursor;
        } finally {
            db.endTransaction();
        }
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
        db.beginTransaction();
        try {
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
                    db.setTransactionSuccessful();
                    return inserted;
                default:
                    throw new UnsupportedOperationException("Unknown insert URI " + uri);
            }
        } finally {
            db.endTransaction();
        }
    }

    private void deleteDataFiles(SQLiteDatabase db, String selection, String[] selectionArgs) {
        Cursor c = db.query("snapshots", DELETE_PROJECTION, selection, selectionArgs, null, null, null);
        Context context = getContext();
        while (c.moveToNext()) {
            String filename = c.getString(0);
            if (!TextUtils.isEmpty(filename)) {
                int id = c.getInt(1);
                if (id == -1) {
                    File f = context.getFileStreamPath(filename);
                    if (f.exists() && !f.delete()) {
                        f.deleteOnExit();
                    }
                } else {
                    int position = filename.lastIndexOf(File.separator);
                    if (position != -1) {
                        String folder = filename.substring(0, position);
                        deleteSavePageDir(new File(folder));
                    }
                }
            }
        }
        c.close();
    }

    private void deleteSavePageDir(File file) {
        if (file.isFile()) {
            if (!file.delete()) {
                file.deleteOnExit();
                return;
            }
            return;
        }
        if (!file.isDirectory()) {
            return;
        }
        File[] childFiles = file.listFiles();
        if (childFiles == null || childFiles.length == 0) {
            if (!file.delete()) {
                file.deleteOnExit();
                return;
            }
            return;
        }
        for (File file2 : childFiles) {
            deleteSavePageDir(file2);
        }
        if (file.delete()) {
            return;
        }
        file.deleteOnExit();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = getWritableDatabase();
        if (db == null) {
            return 0;
        }
        db.beginTransaction();
        try {
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
            }
            db.setTransactionSuccessful();
            return deleted;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = getWritableDatabase();
        if (db == null) {
            return 0;
        }
        db.beginTransaction();
        try {
            int count = db.update("snapshots", values, selection, selectionArgs);
            getContext().getContentResolver().notifyChange(uri, (ContentObserver) null, false);
            db.setTransactionSuccessful();
            return count;
        } finally {
            db.endTransaction();
        }
    }
}
