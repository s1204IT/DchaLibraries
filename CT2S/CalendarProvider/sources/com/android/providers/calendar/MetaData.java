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

    public MetaData(SQLiteOpenHelper openHelper) {
        this.mOpenHelper = openHelper;
    }

    public Fields getFieldsLocked() {
        Fields fields = new Fields();
        if (!this.mInitialized) {
            SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
            readLocked(db);
        }
        fields.timezone = this.mFields.timezone;
        fields.minInstance = this.mFields.minInstance;
        fields.maxInstance = this.mFields.maxInstance;
        return fields;
    }

    private void readLocked(SQLiteDatabase db) {
        String timezone = null;
        long minInstance = 0;
        long maxInstance = 0;
        Cursor cursor = db.query("CalendarMetaData", sCalendarMetaDataProjection, null, null, null, null, null);
        try {
            if (cursor.moveToNext()) {
                timezone = cursor.getString(0);
                minInstance = cursor.getLong(1);
                maxInstance = cursor.getLong(2);
            }
            this.mFields.timezone = timezone;
            this.mFields.minInstance = minInstance;
            this.mFields.maxInstance = maxInstance;
            this.mInitialized = true;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void writeLocked(String timezone, long begin, long end) {
        ContentValues values = new ContentValues();
        values.put("_id", (Integer) 1);
        values.put("localTimezone", timezone);
        values.put("minInstance", Long.valueOf(begin));
        values.put("maxInstance", Long.valueOf(end));
        try {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            db.replace("CalendarMetaData", null, values);
            this.mFields.timezone = timezone;
            this.mFields.minInstance = begin;
            this.mFields.maxInstance = end;
        } catch (RuntimeException e) {
            this.mFields.timezone = null;
            Fields fields = this.mFields;
            this.mFields.maxInstance = 0L;
            fields.minInstance = 0L;
            throw e;
        }
    }

    public void clearInstanceRange() {
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            if (!this.mInitialized) {
                readLocked(db);
            }
            writeLocked(this.mFields.timezone, 0L, 0L);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
