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

        public CacheException(String str) {
            super(str);
        }
    }

    public CalendarCache(SQLiteOpenHelper sQLiteOpenHelper) {
        this.mOpenHelper = sQLiteOpenHelper;
    }

    public void writeTimezoneDatabaseVersion(String str) throws CacheException {
        writeData("timezoneDatabaseVersion", str);
    }

    public String readTimezoneDatabaseVersion() {
        try {
            return readData("timezoneDatabaseVersion");
        } catch (CacheException e) {
            Log.e("CalendarCache", "Could not read timezone database version from CalendarCache");
            return null;
        }
    }

    public void writeTimezoneType(String str) throws CacheException {
        writeData("timezoneType", str);
    }

    public String readTimezoneType() {
        try {
            return readData("timezoneType");
        } catch (CacheException e) {
            Log.e("CalendarCache", "Cannot read timezone type from CalendarCache - using AUTO as default", e);
            return "auto";
        }
    }

    public void writeTimezoneInstances(String str) {
        try {
            writeData("timezoneInstances", str);
        } catch (CacheException e) {
            Log.e("CalendarCache", "Cannot write instances timezone to CalendarCache");
        }
    }

    public String readTimezoneInstances() {
        try {
            return readData("timezoneInstances");
        } catch (CacheException e) {
            String id = TimeZone.getDefault().getID();
            Log.e("CalendarCache", "Cannot read instances timezone from CalendarCache - using device one: " + id, e);
            return id;
        }
    }

    public void writeTimezoneInstancesPrevious(String str) {
        try {
            writeData("timezoneInstancesPrevious", str);
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

    public void writeData(String str, String str2) throws CacheException {
        SQLiteDatabase readableDatabase = this.mOpenHelper.getReadableDatabase();
        readableDatabase.beginTransaction();
        try {
            writeDataLocked(readableDatabase, str, str2);
            readableDatabase.setTransactionSuccessful();
            if (Log.isLoggable("CalendarCache", 2)) {
                Log.i("CalendarCache", "Wrote (key, value) = [ " + str + ", " + str2 + "] ");
            }
        } finally {
            readableDatabase.endTransaction();
        }
    }

    protected void writeDataLocked(SQLiteDatabase sQLiteDatabase, String str, String str2) throws CacheException {
        if (sQLiteDatabase == null) {
            throw new CacheException("Database cannot be null");
        }
        if (str == null) {
            throw new CacheException("Cannot use null key for write");
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("_id", Integer.valueOf(str.hashCode()));
        contentValues.put("key", str);
        contentValues.put("value", str2);
        sQLiteDatabase.replace("CalendarCache", null, contentValues);
    }

    public String readData(String str) throws CacheException {
        return readDataLocked(this.mOpenHelper.getReadableDatabase(), str);
    }

    protected String readDataLocked(SQLiteDatabase sQLiteDatabase, String str) throws CacheException {
        if (sQLiteDatabase == null) {
            throw new CacheException("Database cannot be null");
        }
        if (str == null) {
            throw new CacheException("Cannot use null key for read");
        }
        String string = null;
        Cursor cursorQuery = sQLiteDatabase.query("CalendarCache", sProjection, "key=?", new String[]{str}, null, null, null);
        try {
            if (cursorQuery.moveToNext()) {
                string = cursorQuery.getString(1);
            } else if (Log.isLoggable("CalendarCache", 2)) {
                Log.i("CalendarCache", "Could not find key = [ " + str + " ]");
            }
            return string;
        } finally {
            cursorQuery.close();
        }
    }
}
