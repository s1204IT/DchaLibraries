package com.android.common.content;

import android.accounts.Account;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

public class SyncStateContentProviderHelper {
    public static final String PATH = "syncstate";
    private static final String QUERY_COUNT_SYNC_STATE_ROWS = "SELECT count(*) FROM _sync_state WHERE _id=?";
    private static final String SELECT_BY_ACCOUNT = "account_name=? AND account_type=?";
    private static final String SYNC_STATE_META_TABLE = "_sync_state_metadata";
    private static final String SYNC_STATE_META_VERSION_COLUMN = "version";
    private static final String SYNC_STATE_TABLE = "_sync_state";
    private static long DB_VERSION = 1;
    private static final String[] ACCOUNT_PROJECTION = {"account_name", "account_type"};

    public void createDatabase(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS _sync_state");
        sQLiteDatabase.execSQL("CREATE TABLE _sync_state (_id INTEGER PRIMARY KEY,account_name TEXT NOT NULL,account_type TEXT NOT NULL,data TEXT,UNIQUE(account_name, account_type));");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS _sync_state_metadata");
        sQLiteDatabase.execSQL("CREATE TABLE _sync_state_metadata (version INTEGER);");
        ContentValues contentValues = new ContentValues();
        contentValues.put(SYNC_STATE_META_VERSION_COLUMN, Long.valueOf(DB_VERSION));
        sQLiteDatabase.insert(SYNC_STATE_META_TABLE, SYNC_STATE_META_VERSION_COLUMN, contentValues);
    }

    public void onDatabaseOpened(SQLiteDatabase sQLiteDatabase) {
        if (DatabaseUtils.longForQuery(sQLiteDatabase, "SELECT version FROM _sync_state_metadata", null) != DB_VERSION) {
            createDatabase(sQLiteDatabase);
        }
    }

    public Cursor query(SQLiteDatabase sQLiteDatabase, String[] strArr, String str, String[] strArr2, String str2) {
        return sQLiteDatabase.query(SYNC_STATE_TABLE, strArr, str, strArr2, null, null, str2);
    }

    public long insert(SQLiteDatabase sQLiteDatabase, ContentValues contentValues) {
        return sQLiteDatabase.replace(SYNC_STATE_TABLE, "account_name", contentValues);
    }

    public int delete(SQLiteDatabase sQLiteDatabase, String str, String[] strArr) {
        return sQLiteDatabase.delete(SYNC_STATE_TABLE, str, strArr);
    }

    public int update(SQLiteDatabase sQLiteDatabase, ContentValues contentValues, String str, String[] strArr) {
        return sQLiteDatabase.update(SYNC_STATE_TABLE, contentValues, str, strArr);
    }

    public int update(SQLiteDatabase sQLiteDatabase, long j, Object obj) {
        if (DatabaseUtils.longForQuery(sQLiteDatabase, QUERY_COUNT_SYNC_STATE_ROWS, new String[]{Long.toString(j)}) < 1) {
            return 0;
        }
        sQLiteDatabase.execSQL("UPDATE _sync_state SET data=? WHERE _id=" + j, new Object[]{obj});
        return 1;
    }

    public void onAccountsChanged(SQLiteDatabase sQLiteDatabase, Account[] accountArr) {
        Cursor cursorQuery = sQLiteDatabase.query(SYNC_STATE_TABLE, ACCOUNT_PROJECTION, null, null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                String string = cursorQuery.getString(0);
                String string2 = cursorQuery.getString(1);
                if (!contains(accountArr, new Account(string, string2))) {
                    sQLiteDatabase.delete(SYNC_STATE_TABLE, SELECT_BY_ACCOUNT, new String[]{string, string2});
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private static <T> boolean contains(T[] tArr, T t) {
        for (T t2 : tArr) {
            if (t2 == null) {
                if (t == null) {
                    return true;
                }
            } else if (t != null && t2.equals(t)) {
                return true;
            }
        }
        return false;
    }
}
