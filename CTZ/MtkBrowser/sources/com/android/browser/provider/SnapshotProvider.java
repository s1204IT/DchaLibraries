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

    static final class SnapshotDatabaseHelper extends SQLiteOpenHelper {
        public SnapshotDatabaseHelper(Context context) {
            super(context, "snapshots.db", (SQLiteDatabase.CursorFactory) null, 4);
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE snapshots(_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,url TEXT NOT NULL,date_created INTEGER,favicon BLOB,thumbnail BLOB,background INTEGER,view_state BLOB NOT NULL,viewstate_path TEXT,viewstate_size INTEGER,job_id INTEGER,progress INTEGER,is_done INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
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

    public interface Snapshots {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(SnapshotProvider.AUTHORITY_URI, "snapshots");
    }

    static {
        URI_MATCHER.addURI("com.android.browser.snapshots", "snapshots", 10);
        URI_MATCHER.addURI("com.android.browser.snapshots", "snapshots/#", 11);
        DELETE_PROJECTION = new String[]{"viewstate_path", "job_id"};
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
            if (file.delete()) {
                return;
            }
            file.deleteOnExit();
            return;
        }
        if (file.isDirectory()) {
            File[] fileArrListFiles = file.listFiles();
            if (fileArrListFiles == null || fileArrListFiles.length == 0) {
                if (file.delete()) {
                    return;
                }
                file.deleteOnExit();
                return;
            }
            for (File file2 : fileArrListFiles) {
                deleteSavePageDir(file2);
            }
            if (file.delete()) {
                return;
            }
            file.deleteOnExit();
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

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int iDelete = 0;
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (writableDatabase != null) {
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
                iDelete = writableDatabase.delete("snapshots", str, strArr);
                if (iDelete > 0) {
                    getContext().getContentResolver().notifyChange(uri, (ContentObserver) null, false);
                }
                writableDatabase.setTransactionSuccessful();
            } finally {
                writableDatabase.endTransaction();
            }
        }
        return iDelete;
    }

    SQLiteDatabase getReadableDatabase() {
        return this.mOpenHelper.getReadableDatabase();
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    SQLiteDatabase getWritableDatabase() {
        return this.mOpenHelper.getWritableDatabase();
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Uri uriWithAppendedId = null;
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (writableDatabase != null) {
            writableDatabase.beginTransaction();
            try {
                if (URI_MATCHER.match(uri) != 10) {
                    throw new UnsupportedOperationException("Unknown insert URI " + uri);
                }
                if (!contentValues.containsKey("view_state")) {
                    contentValues.put("view_state", NULL_BLOB_HACK);
                }
                long jInsert = writableDatabase.insert("snapshots", "title", contentValues);
                if (jInsert >= 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert);
                    getContext().getContentResolver().notifyChange(uriWithAppendedId, (ContentObserver) null, false);
                    writableDatabase.setTransactionSuccessful();
                }
            } finally {
                writableDatabase.endTransaction();
            }
        }
        return uriWithAppendedId;
    }

    @Override
    public boolean onCreate() {
        migrateToDataFolder();
        this.mOpenHelper = new SnapshotDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String[] strArrAppendSelectionArgs;
        String strConcatenateWhere;
        Cursor cursorQuery = null;
        SQLiteDatabase readableDatabase = getReadableDatabase();
        if (readableDatabase != null) {
            readableDatabase.beginTransaction();
            try {
                int iMatch = URI_MATCHER.match(uri);
                SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
                String queryParameter = uri.getQueryParameter("limit");
                switch (iMatch) {
                    case 10:
                        strArrAppendSelectionArgs = strArr2;
                        strConcatenateWhere = str;
                        break;
                    case 11:
                        strConcatenateWhere = DatabaseUtils.concatenateWhere(str, "_id=?");
                        strArrAppendSelectionArgs = DatabaseUtils.appendSelectionArgs(strArr2, new String[]{Long.toString(ContentUris.parseId(uri))});
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown URL " + uri.toString());
                }
                sQLiteQueryBuilder.setTables("snapshots");
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, strConcatenateWhere, strArrAppendSelectionArgs, null, null, str2, queryParameter);
                cursorQuery.setNotificationUri(getContext().getContentResolver(), AUTHORITY_URI);
                readableDatabase.setTransactionSuccessful();
            } finally {
                readableDatabase.endTransaction();
            }
        }
        return cursorQuery;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int iUpdate = 0;
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (writableDatabase != null) {
            writableDatabase.beginTransaction();
            try {
                iUpdate = writableDatabase.update("snapshots", contentValues, str, strArr);
                getContext().getContentResolver().notifyChange(uri, (ContentObserver) null, false);
                writableDatabase.setTransactionSuccessful();
            } finally {
                writableDatabase.endTransaction();
            }
        }
        return iUpdate;
    }
}
