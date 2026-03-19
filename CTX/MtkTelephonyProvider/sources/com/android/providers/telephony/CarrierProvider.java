package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class CarrierProvider extends ContentProvider {
    static final Uri CONTENT_URI = Uri.parse("content://carrier_information/carrier");
    private SQLiteDatabase mDatabase;
    private CarrierDatabaseHelper mDbHelper;

    @Override
    public boolean onCreate() {
        Log.d("CarrierProvider", "onCreate");
        this.mDbHelper = new CarrierDatabaseHelper(getContext());
        return this.mDatabase != null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables("carrier_key");
        return sQLiteQueryBuilder.query(getReadableDatabase(), strArr, str, strArr2, null, null, str2);
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        contentValues.put("last_modified", Long.valueOf(System.currentTimeMillis()));
        long jInsertOrThrow = getWritableDatabase().insertOrThrow("carrier_key", null, contentValues);
        if (jInsertOrThrow <= 0) {
            return null;
        }
        Uri uriWithAppendedId = ContentUris.withAppendedId(CONTENT_URI, jInsertOrThrow);
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return uriWithAppendedId;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int iDelete = getWritableDatabase().delete("carrier_key", str, strArr);
        Log.d("CarrierProvider", "  delete.count=" + iDelete);
        return iDelete;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        contentValues.put("last_modified", Long.valueOf(System.currentTimeMillis()));
        int iUpdate = getWritableDatabase().update("carrier_key", contentValues, str, strArr);
        if (iUpdate > 0) {
            getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        }
        Log.d("CarrierProvider", "  update.count=" + iUpdate);
        return iUpdate;
    }

    SQLiteDatabase getReadableDatabase() {
        return this.mDbHelper.getReadableDatabase();
    }

    SQLiteDatabase getWritableDatabase() {
        return this.mDbHelper.getWritableDatabase();
    }
}
