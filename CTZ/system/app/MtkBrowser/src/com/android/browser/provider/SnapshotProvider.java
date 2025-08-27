package com.android.browser.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.FileUtils;
import android.text.TextUtils;
import java.io.File;

/* loaded from: classes.dex */
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

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onCreate(SQLiteDatabase sQLiteDatabase) throws SQLException {
            sQLiteDatabase.execSQL("CREATE TABLE snapshots(_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,url TEXT NOT NULL,date_created INTEGER,favicon BLOB,thumbnail BLOB,background INTEGER,view_state BLOB NOT NULL,viewstate_path TEXT,viewstate_size INTEGER,job_id INTEGER,progress INTEGER,is_done INTEGER);");
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws SQLException {
            if (i < 2) {
                sQLiteDatabase.execSQL("DROP TABLE snapshots");
                onCreate(sQLiteDatabase);
            }
            if (i < 3) {
                sQLiteDatabase.execSQL("ALTER TABLE snapshots ADD COLUMN viewstate_path TEXT");
                sQLiteDatabase.execSQL("ALTER TABLE snapshots ADD COLUMN viewstate_size INTEGER");
                sQLiteDatabase.execSQL("UPDATE snapshots SET viewstate_size = length(view_state)");
            }
            if (i < 4) {
                sQLiteDatabase.execSQL("ALTER TABLE snapshots ADD COLUMN job_id INTEGER");
                sQLiteDatabase.execSQL("ALTER TABLE snapshots ADD COLUMN progress INTEGER");
                sQLiteDatabase.execSQL("ALTER TABLE snapshots ADD COLUMN is_done INTEGER");
                sQLiteDatabase.execSQL("UPDATE snapshots SET job_id = -1 ");
                sQLiteDatabase.execSQL("UPDATE snapshots SET progress = 100 ");
                sQLiteDatabase.execSQL("UPDATE snapshots SET is_done = 1 ");
            }
        }
    }

    static File getOldDatabasePath(Context context) {
        return new File(context.getExternalFilesDir(null), "snapshots.db");
    }

    private void migrateToDataFolder() {
        File databasePath = getContext().getDatabasePath("snapshots.db");
        if (databasePath.exists()) {
            return;
        }
        File oldDatabasePath = getOldDatabasePath(getContext());
        if (oldDatabasePath.exists()) {
            if (!oldDatabasePath.renameTo(databasePath)) {
                FileUtils.copyFile(oldDatabasePath, databasePath);
            }
            oldDatabasePath.delete();
        }
    }

    @Override // android.content.ContentProvider
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

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        SQLiteDatabase readableDatabase = getReadableDatabase();
        if (readableDatabase == null) {
            return null;
        }
        readableDatabase.beginTransaction();
        try {
            int iMatch = URI_MATCHER.match(uri);
            SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
            String queryParameter = uri.getQueryParameter("limit");
            switch (iMatch) {
                case 10:
                    break;
                case 11:
                    str = DatabaseUtils.concatenateWhere(str, "_id=?");
                    strArr2 = DatabaseUtils.appendSelectionArgs(strArr2, new String[]{Long.toString(ContentUris.parseId(uri))});
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown URL " + uri.toString());
            }
            sQLiteQueryBuilder.setTables("snapshots");
            Cursor cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, queryParameter);
            cursorQuery.setNotificationUri(getContext().getContentResolver(), AUTHORITY_URI);
            readableDatabase.setTransactionSuccessful();
            return cursorQuery;
        } finally {
            readableDatabase.endTransaction();
        }
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return null;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (writableDatabase == null) {
            return null;
        }
        writableDatabase.beginTransaction();
        try {
            if (URI_MATCHER.match(uri) == 10) {
                if (!contentValues.containsKey("view_state")) {
                    contentValues.put("view_state", NULL_BLOB_HACK);
                }
                long jInsert = writableDatabase.insert("snapshots", "title", contentValues);
                if (jInsert < 0) {
                    return null;
                }
                Uri uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert);
                getContext().getContentResolver().notifyChange(uriWithAppendedId, (ContentObserver) null, false);
                writableDatabase.setTransactionSuccessful();
                return uriWithAppendedId;
            }
            throw new UnsupportedOperationException("Unknown insert URI " + uri);
        } finally {
            writableDatabase.endTransaction();
        }
    }

    private void deleteDataFiles(SQLiteDatabase sQLiteDatabase, String str, String[] strArr) {
        Cursor cursorQuery = sQLiteDatabase.query("snapshots", DELETE_PROJECTION, str, strArr, null, null, null);
        Context context = getContext();
        while (cursorQuery.moveToNext()) {
            String string = cursorQuery.getString(0);
            if (!TextUtils.isEmpty(string)) {
                if (cursorQuery.getInt(1) == -1) {
                    File fileStreamPath = context.getFileStreamPath(string);
                    if (fileStreamPath.exists() && !fileStreamPath.delete()) {
                        fileStreamPath.deleteOnExit();
                    }
                } else {
                    int iLastIndexOf = string.lastIndexOf(File.separator);
                    if (iLastIndexOf != -1) {
                        deleteSavePageDir(new File(string.substring(0, iLastIndexOf)));
                    }
                }
            }
        }
        cursorQuery.close();
    }

    private void deleteSavePageDir(File file) {
        if (file.isFile()) {
            if (!file.delete()) {
                file.deleteOnExit();
                return;
            }
            return;
        }
        if (file.isDirectory()) {
            File[] fileArrListFiles = file.listFiles();
            if (fileArrListFiles == null || fileArrListFiles.length == 0) {
                if (!file.delete()) {
                    file.deleteOnExit();
                    return;
                }
                return;
            }
            for (File file2 : fileArrListFiles) {
                deleteSavePageDir(file2);
            }
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (writableDatabase == null) {
            return 0;
        }
        writableDatabase.beginTransaction();
        try {
            switch (URI_MATCHER.match(uri)) {
                case 10:
                    break;
                case 11:
                    str = DatabaseUtils.concatenateWhere(str, "snapshots._id=?");
                    strArr = DatabaseUtils.appendSelectionArgs(strArr, new String[]{Long.toString(ContentUris.parseId(uri))});
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown delete URI " + uri);
            }
            deleteDataFiles(writableDatabase, str, strArr);
            int iDelete = writableDatabase.delete("snapshots", str, strArr);
            if (iDelete > 0) {
                getContext().getContentResolver().notifyChange(uri, (ContentObserver) null, false);
            }
            writableDatabase.setTransactionSuccessful();
            return iDelete;
        } finally {
            writableDatabase.endTransaction();
        }
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (writableDatabase == null) {
            return 0;
        }
        writableDatabase.beginTransaction();
        try {
            int iUpdate = writableDatabase.update("snapshots", contentValues, str, strArr);
            getContext().getContentResolver().notifyChange(uri, (ContentObserver) null, false);
            writableDatabase.setTransactionSuccessful();
            return iUpdate;
        } finally {
            writableDatabase.endTransaction();
        }
    }
}
