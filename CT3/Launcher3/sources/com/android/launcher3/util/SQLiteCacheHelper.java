package com.android.launcher3.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public abstract class SQLiteCacheHelper {
    private boolean mIgnoreWrites = false;
    private final MySQLiteOpenHelper mOpenHelper;
    private final String mTableName;

    protected abstract void onCreateTable(SQLiteDatabase sQLiteDatabase);

    public SQLiteCacheHelper(Context context, String name, int version, String tableName) {
        this.mTableName = tableName;
        this.mOpenHelper = new MySQLiteOpenHelper(context, name, version);
    }

    public void update(ContentValues values, String whereClause, String[] whereArgs) {
        if (this.mIgnoreWrites) {
            return;
        }
        try {
            this.mOpenHelper.getWritableDatabase().update(this.mTableName, values, whereClause, whereArgs);
        } catch (SQLiteFullException e) {
            onDiskFull(e);
        } catch (SQLiteException e2) {
            Log.d("SQLiteCacheHelper", "Ignoring sqlite exception", e2);
        }
    }

    public void delete(String whereClause, String[] whereArgs) {
        if (this.mIgnoreWrites) {
            return;
        }
        try {
            this.mOpenHelper.getWritableDatabase().delete(this.mTableName, whereClause, whereArgs);
        } catch (SQLiteFullException e) {
            onDiskFull(e);
        } catch (SQLiteException e2) {
            Log.d("SQLiteCacheHelper", "Ignoring sqlite exception", e2);
        }
    }

    public void insertOrReplace(ContentValues values) {
        if (this.mIgnoreWrites) {
            return;
        }
        try {
            this.mOpenHelper.getWritableDatabase().insertWithOnConflict(this.mTableName, null, values, 5);
        } catch (SQLiteFullException e) {
            onDiskFull(e);
        } catch (SQLiteException e2) {
            Log.d("SQLiteCacheHelper", "Ignoring sqlite exception", e2);
        }
    }

    private void onDiskFull(SQLiteFullException e) {
        Log.e("SQLiteCacheHelper", "Disk full, all write operations will be ignored", e);
        this.mIgnoreWrites = true;
    }

    public Cursor query(String[] columns, String selection, String[] selectionArgs) {
        return this.mOpenHelper.getReadableDatabase().query(this.mTableName, columns, selection, selectionArgs, null, null, null);
    }

    private class MySQLiteOpenHelper extends SQLiteOpenHelper {
        public MySQLiteOpenHelper(Context context, String name, int version) {
            super(context, name, (SQLiteDatabase.CursorFactory) null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            SQLiteCacheHelper.this.onCreateTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == newVersion) {
                return;
            }
            clearDB(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == newVersion) {
                return;
            }
            clearDB(db);
        }

        private void clearDB(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + SQLiteCacheHelper.this.mTableName);
            onCreate(db);
        }
    }
}
