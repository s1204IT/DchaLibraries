package com.android.common.content;

import android.accounts.Account;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

public class SyncStateContentProviderHelper {
    private static long DB_VERSION = 1;
    private static final String[] ACCOUNT_PROJECTION = {"account_name", "account_type"};

    public void createDatabase(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS _sync_state");
        db.execSQL("CREATE TABLE _sync_state (_id INTEGER PRIMARY KEY,account_name TEXT NOT NULL,account_type TEXT NOT NULL,data TEXT,UNIQUE(account_name, account_type));");
        db.execSQL("DROP TABLE IF EXISTS _sync_state_metadata");
        db.execSQL("CREATE TABLE _sync_state_metadata (version INTEGER);");
        ContentValues values = new ContentValues();
        values.put("version", Long.valueOf(DB_VERSION));
        db.insert("_sync_state_metadata", "version", values);
    }

    public void onDatabaseOpened(SQLiteDatabase db) {
        long version = DatabaseUtils.longForQuery(db, "SELECT version FROM _sync_state_metadata", null);
        if (version != DB_VERSION) {
            createDatabase(db);
        }
    }

    public Cursor query(SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return db.query("_sync_state", projection, selection, selectionArgs, null, null, sortOrder);
    }

    public long insert(SQLiteDatabase db, ContentValues values) {
        return db.replace("_sync_state", "account_name", values);
    }

    public int delete(SQLiteDatabase db, String userWhere, String[] whereArgs) {
        return db.delete("_sync_state", userWhere, whereArgs);
    }

    public int update(SQLiteDatabase db, ContentValues values, String selection, String[] selectionArgs) {
        return db.update("_sync_state", values, selection, selectionArgs);
    }

    public int update(SQLiteDatabase db, long rowId, Object data) {
        if (DatabaseUtils.longForQuery(db, "SELECT count(*) FROM _sync_state WHERE _id=?", new String[]{Long.toString(rowId)}) < 1) {
            return 0;
        }
        db.execSQL("UPDATE _sync_state SET data=? WHERE _id=" + rowId, new Object[]{data});
        return 1;
    }

    public void onAccountsChanged(SQLiteDatabase db, Account[] accounts) {
        Cursor c = db.query("_sync_state", ACCOUNT_PROJECTION, null, null, null, null, null);
        while (c.moveToNext()) {
            try {
                String accountName = c.getString(0);
                String accountType = c.getString(1);
                Account account = new Account(accountName, accountType);
                if (!contains(accounts, account)) {
                    db.delete("_sync_state", "account_name=? AND account_type=?", new String[]{accountName, accountType});
                }
            } finally {
                c.close();
            }
        }
    }

    private static <T> boolean contains(T[] array, T value) {
        for (T element : array) {
            if (element == null) {
                if (value == null) {
                    return true;
                }
            } else if (value != null && element.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
