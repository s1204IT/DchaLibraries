package com.android.providers.userdictionary;

import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.UserDictionary;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;

public class UserDictionaryProvider extends ContentProvider {
    private static HashMap<String, String> sDictProjectionMap;
    private BackupManager mBackupManager;
    private DatabaseHelper mOpenHelper;
    private static final Uri CONTENT_URI = UserDictionary.CONTENT_URI;
    private static final UriMatcher sUriMatcher = new UriMatcher(-1);

    static {
        sUriMatcher.addURI("user_dictionary", "words", 1);
        sUriMatcher.addURI("user_dictionary", "words/#", 2);
        sDictProjectionMap = new HashMap<>();
        sDictProjectionMap.put("_id", "_id");
        sDictProjectionMap.put("word", "word");
        sDictProjectionMap.put("frequency", "frequency");
        sDictProjectionMap.put("locale", "locale");
        sDictProjectionMap.put("appid", "appid");
        sDictProjectionMap.put("shortcut", "shortcut");
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, "user_dict.db", (SQLiteDatabase.CursorFactory) null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE words (_id INTEGER PRIMARY KEY,word TEXT,frequency INTEGER,locale TEXT,appid INTEGER,shortcut TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 1 && newVersion == 2) {
                Log.i("UserDictionaryProvider", "Upgrading database from version " + oldVersion + " to version 2: adding shortcut column");
                db.execSQL("ALTER TABLE words ADD shortcut TEXT;");
            } else {
                Log.w("UserDictionaryProvider", "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
                db.execSQL("DROP TABLE IF EXISTS words");
                onCreate(db);
            }
        }
    }

    @Override
    public boolean onCreate() {
        this.mOpenHelper = new DatabaseHelper(getContext());
        this.mBackupManager = new BackupManager(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String orderBy;
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case 1:
                qb.setTables("words");
                qb.setProjectionMap(sDictProjectionMap);
                break;
            case 2:
                qb.setTables("words");
                qb.setProjectionMap(sDictProjectionMap);
                qb.appendWhere("_id=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = "frequency DESC";
        } else {
            orderBy = sortOrder;
        }
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case 1:
                return "vnd.android.cursor.dir/vnd.google.userword";
            case 2:
                return "vnd.android.cursor.item/vnd.google.userword";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        ContentValues values;
        if (sUriMatcher.match(uri) != 1) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        if (!values.containsKey("word")) {
            throw new SQLException("Word must be specified");
        }
        if (!values.containsKey("frequency")) {
            values.put("frequency", "1");
        }
        if (!values.containsKey("locale")) {
            values.put("locale", (String) null);
        }
        if (!values.containsKey("shortcut")) {
            values.put("shortcut", (String) null);
        }
        values.put("appid", (Integer) 0);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        long rowId = db.insert("words", "word", values);
        if (rowId > 0) {
            Uri wordUri = ContentUris.withAppendedId(UserDictionary.Words.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(wordUri, null);
            this.mBackupManager.dataChanged();
            return wordUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        int count;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case 1:
                count = db.delete("words", where, whereArgs);
                break;
            case 2:
                String wordId = uri.getPathSegments().get(1);
                count = db.delete("words", "_id=" + wordId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        this.mBackupManager.dataChanged();
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case 1:
                count = db.update("words", values, where, whereArgs);
                break;
            case 2:
                String wordId = uri.getPathSegments().get(1);
                count = db.update("words", values, "_id=" + wordId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        this.mBackupManager.dataChanged();
        return count;
    }
}
