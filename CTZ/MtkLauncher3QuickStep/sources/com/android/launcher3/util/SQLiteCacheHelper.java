package com.android.launcher3.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.util.Log;

public abstract class SQLiteCacheHelper {
    private static final boolean NO_ICON_CACHE = false;
    private static final String TAG = "SQLiteCacheHelper";
    private boolean mIgnoreWrites;
    private final MySQLiteOpenHelper mOpenHelper;
    private final String mTableName;

    protected abstract void onCreateTable(SQLiteDatabase sQLiteDatabase);

    public SQLiteCacheHelper(Context context, String str, int i, String str2) {
        str = NO_ICON_CACHE ? null : str;
        this.mTableName = str2;
        this.mOpenHelper = new MySQLiteOpenHelper(context, str, i);
        this.mIgnoreWrites = false;
    }

    public void delete(String str, String[] strArr) {
        if (this.mIgnoreWrites) {
            return;
        }
        try {
            this.mOpenHelper.getWritableDatabase().delete(this.mTableName, str, strArr);
        } catch (SQLiteFullException e) {
            onDiskFull(e);
        } catch (SQLiteException e2) {
            Log.d(TAG, "Ignoring sqlite exception", e2);
        }
    }

    public void insertOrReplace(ContentValues contentValues) {
        if (this.mIgnoreWrites) {
            return;
        }
        try {
            this.mOpenHelper.getWritableDatabase().insertWithOnConflict(this.mTableName, null, contentValues, 5);
        } catch (SQLiteFullException e) {
            onDiskFull(e);
        } catch (SQLiteException e2) {
            Log.d(TAG, "Ignoring sqlite exception", e2);
        }
    }

    private void onDiskFull(SQLiteFullException sQLiteFullException) {
        Log.e(TAG, "Disk full, all write operations will be ignored", sQLiteFullException);
        this.mIgnoreWrites = true;
    }

    public Cursor query(String[] strArr, String str, String[] strArr2) {
        return this.mOpenHelper.getReadableDatabase().query(this.mTableName, strArr, str, strArr2, null, null, null);
    }

    public void clear() {
        this.mOpenHelper.clearDB(this.mOpenHelper.getWritableDatabase());
    }

    private class MySQLiteOpenHelper extends NoLocaleSQLiteHelper {
        public MySQLiteOpenHelper(Context context, String str, int i) {
            super(context, str, i);
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            SQLiteCacheHelper.this.onCreateTable(sQLiteDatabase);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            if (i != i2) {
                clearDB(sQLiteDatabase);
            }
        }

        @Override
        public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            if (i != i2) {
                clearDB(sQLiteDatabase);
            }
        }

        private void clearDB(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SQLiteCacheHelper.this.mTableName);
            onCreate(sQLiteDatabase);
        }
    }
}
