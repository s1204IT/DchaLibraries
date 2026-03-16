package com.android.providers.calendar;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.TimeZone;

public class CalendarCache {
    private static final String[] sProjection = {"key", "value"};
    private final SQLiteOpenHelper mOpenHelper;

    public static class CacheException extends Exception {
        public CacheException() {
        }

        public CacheException(String detailMessage) {
            super(detailMessage);
        }
    }

    public CalendarCache(SQLiteOpenHelper openHelper) {
        this.mOpenHelper = openHelper;
    }

    public void writeTimezoneDatabaseVersion(String timezoneDatabaseVersion) throws CacheException {
        writeData("timezoneDatabaseVersion", timezoneDatabaseVersion);
    }

    public String readTimezoneDatabaseVersion() {
        try {
            return readData("timezoneDatabaseVersion");
        } catch (CacheException e) {
            Log.e("CalendarCache", "Could not read timezone database version from CalendarCache");
            return null;
        }
    }

    public void writeTimezoneType(String timezoneType) throws CacheException {
        writeData("timezoneType", timezoneType);
    }

    public String readTimezoneType() {
        try {
            return readData("timezoneType");
        } catch (CacheException e) {
            Log.e("CalendarCache", "Cannot read timezone type from CalendarCache - using AUTO as default", e);
            return "auto";
        }
    }

    public void writeTimezoneInstances(String timezone) {
        try {
            writeData("timezoneInstances", timezone);
        } catch (CacheException e) {
            Log.e("CalendarCache", "Cannot write instances timezone to CalendarCache");
        }
    }

    public String readTimezoneInstances() {
        try {
            return readData("timezoneInstances");
        } catch (CacheException e) {
            String localTimezone = TimeZone.getDefault().getID();
            Log.e("CalendarCache", "Cannot read instances timezone from CalendarCache - using device one: " + localTimezone, e);
            return localTimezone;
        }
    }

    public void writeTimezoneInstancesPrevious(String timezone) {
        try {
            writeData("timezoneInstancesPrevious", timezone);
        } catch (CacheException e) {
            Log.e("CalendarCache", "Cannot write previous instance timezone to CalendarCache");
        }
    }

    public String readTimezoneInstancesPrevious() {
        try {
            return readData("timezoneInstancesPrevious");
        } catch (CacheException e) {
            Log.e("CalendarCache", "Cannot read previous instances timezone from CalendarCache", e);
            return null;
        }
    }

    public void writeData(String key, String value) throws CacheException {
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            writeDataLocked(db, key, value);
            db.setTransactionSuccessful();
            if (Log.isLoggable("CalendarCache", 2)) {
                Log.i("CalendarCache", "Wrote (key, value) = [ " + key + ", " + value + "] ");
            }
        } finally {
            db.endTransaction();
        }
    }

    protected void writeDataLocked(SQLiteDatabase db, String key, String value) throws CacheException {
        if (db == null) {
            throw new CacheException("Database cannot be null");
        }
        if (key == null) {
            throw new CacheException("Cannot use null key for write");
        }
        ContentValues values = new ContentValues();
        values.put("_id", Integer.valueOf(key.hashCode()));
        values.put("key", key);
        values.put("value", value);
        db.replace("CalendarCache", null, values);
    }

    public String readData(String key) throws CacheException {
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        return readDataLocked(db, key);
    }

    protected String readDataLocked(SQLiteDatabase db, String key) throws CacheException {
        if (db == null) {
            throw new CacheException("Database cannot be null");
        }
        if (key == null) {
            throw new CacheException("Cannot use null key for read");
        }
        String rowValue = null;
        Cursor cursor = db.query("CalendarCache", sProjection, "key=?", new String[]{key}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                rowValue = cursor.getString(1);
            } else if (Log.isLoggable("CalendarCache", 2)) {
                Log.i("CalendarCache", "Could not find key = [ " + key + " ]");
            }
            return rowValue;
        } finally {
            cursor.close();
        }
    }
}
