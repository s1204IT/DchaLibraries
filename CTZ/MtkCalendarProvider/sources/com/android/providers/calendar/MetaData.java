package com.android.providers.calendar;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MetaData {
    private static final String[] sCalendarMetaDataProjection = {"localTimezone", "minInstance", "maxInstance"};
    private Fields mFields = new Fields();
    private boolean mInitialized;
    private final SQLiteOpenHelper mOpenHelper;

    public class Fields {
        public long maxInstance;
        public long minInstance;
        public String timezone;

        public Fields() {
        }
    }

    public MetaData(SQLiteOpenHelper sQLiteOpenHelper) {
        this.mOpenHelper = sQLiteOpenHelper;
    }

    public Fields getFieldsLocked() {
        Fields fields = new Fields();
        if (!this.mInitialized) {
            readLocked(this.mOpenHelper.getReadableDatabase());
        }
        fields.timezone = this.mFields.timezone;
        fields.minInstance = this.mFields.minInstance;
        fields.maxInstance = this.mFields.maxInstance;
        return fields;
    }

    private void readLocked(SQLiteDatabase sQLiteDatabase) {
        String string;
        long j;
        Cursor cursorQuery = sQLiteDatabase.query("CalendarMetaData", sCalendarMetaDataProjection, null, null, null, null, null);
        try {
            long j2 = 0;
            if (cursorQuery.moveToNext()) {
                string = cursorQuery.getString(0);
                j2 = cursorQuery.getLong(1);
                j = cursorQuery.getLong(2);
            } else {
                string = null;
                j = 0;
            }
            this.mFields.timezone = string;
            this.mFields.minInstance = j2;
            this.mFields.maxInstance = j;
            this.mInitialized = true;
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    public void writeLocked(String str, long j, long j2) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("_id", (Integer) 1);
        contentValues.put("localTimezone", str);
        contentValues.put("minInstance", Long.valueOf(j));
        contentValues.put("maxInstance", Long.valueOf(j2));
        try {
            this.mOpenHelper.getWritableDatabase().replace("CalendarMetaData", null, contentValues);
            this.mFields.timezone = str;
            this.mFields.minInstance = j;
            this.mFields.maxInstance = j2;
        } catch (RuntimeException e) {
            this.mFields.timezone = null;
            Fields fields = this.mFields;
            this.mFields.maxInstance = 0L;
            fields.minInstance = 0L;
            throw e;
        }
    }

    public void clearInstanceRange() {
        SQLiteDatabase readableDatabase = this.mOpenHelper.getReadableDatabase();
        readableDatabase.beginTransaction();
        try {
            if (!this.mInitialized) {
                readLocked(readableDatabase);
            }
            writeLocked(this.mFields.timezone, 0L, 0L);
            readableDatabase.setTransactionSuccessful();
        } finally {
            readableDatabase.endTransaction();
        }
    }
}
