package com.android.deskclock.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import com.android.deskclock.LogUtils;
import java.util.Calendar;

class ClockDatabaseHelper extends SQLiteOpenHelper {
    private Context mContext;

    private static void createAlarmsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE alarm_templates (_id INTEGER PRIMARY KEY,hour INTEGER NOT NULL, minutes INTEGER NOT NULL, daysofweek INTEGER NOT NULL, enabled INTEGER NOT NULL, vibrate INTEGER NOT NULL, label TEXT NOT NULL, ringtone TEXT, delete_after_use INTEGER NOT NULL DEFAULT 0);");
        LogUtils.i("Alarms Table created", new Object[0]);
    }

    private static void createInstanceTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE alarm_instances (_id INTEGER PRIMARY KEY,year INTEGER NOT NULL, month INTEGER NOT NULL, day INTEGER NOT NULL, hour INTEGER NOT NULL, minutes INTEGER NOT NULL, vibrate INTEGER NOT NULL, label TEXT NOT NULL, ringtone TEXT, alarm_state INTEGER NOT NULL, alarm_id INTEGER REFERENCES alarm_templates(_id) ON UPDATE CASCADE ON DELETE CASCADE);");
        LogUtils.i("Instance table created", new Object[0]);
    }

    private static void createCitiesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE selected_cities (city_id TEXT PRIMARY KEY,city_name TEXT NOT NULL, timezone_name TEXT NOT NULL, timezone_offset INTEGER NOT NULL);");
        LogUtils.i("Cities table created", new Object[0]);
    }

    public ClockDatabaseHelper(Context context) {
        super(context, "alarms.db", (SQLiteDatabase.CursorFactory) null, 7);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAlarmsTable(db);
        createInstanceTable(db);
        createCitiesTable(db);
        LogUtils.i("Inserting default alarms", new Object[0]);
        String insertMe = "INSERT INTO alarm_templates (hour, minutes, daysofweek, enabled, vibrate, label, ringtone, delete_after_use) VALUES ";
        db.execSQL(insertMe + "(8, 30, 31, 0, 0, '', NULL, 0);");
        db.execSQL(insertMe + "(9, 00, 96, 0, 0, '', NULL, 0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        LogUtils.v("Upgrading alarms database from version " + oldVersion + " to " + currentVersion, new Object[0]);
        if (oldVersion <= 6) {
            db.execSQL("DROP TABLE IF EXISTS alarm_instances;");
            db.execSQL("DROP TABLE IF EXISTS selected_cities;");
            createAlarmsTable(db);
            createInstanceTable(db);
            createCitiesTable(db);
            LogUtils.i("Copying old alarms to new table", new Object[0]);
            String[] OLD_TABLE_COLUMNS = {"_id", "hour", "minutes", "daysofweek", "enabled", "vibrate", "message", "alert"};
            Cursor cursor = db.query("alarms", OLD_TABLE_COLUMNS, null, null, null, null, null);
            Calendar currentTime = Calendar.getInstance();
            while (cursor.moveToNext()) {
                Alarm alarm = new Alarm();
                alarm.id = cursor.getLong(0);
                alarm.hour = cursor.getInt(1);
                alarm.minutes = cursor.getInt(2);
                alarm.daysOfWeek = new DaysOfWeek(cursor.getInt(3));
                alarm.enabled = cursor.getInt(4) == 1;
                alarm.vibrate = cursor.getInt(5) == 1;
                alarm.label = cursor.getString(6);
                String alertString = cursor.getString(7);
                if ("silent".equals(alertString)) {
                    alarm.alert = Alarm.NO_RINGTONE_URI;
                } else {
                    alarm.alert = TextUtils.isEmpty(alertString) ? null : Uri.parse(alertString);
                }
                db.insert("alarm_templates", null, Alarm.createContentValues(alarm));
                if (alarm.enabled) {
                    AlarmInstance newInstance = alarm.createInstanceAfter(currentTime);
                    db.insert("alarm_instances", null, AlarmInstance.createContentValues(newInstance));
                }
            }
            cursor.close();
            LogUtils.i("Dropping old alarm table", new Object[0]);
            db.execSQL("DROP TABLE IF EXISTS alarms;");
        }
    }

    long fixAlarmInsert(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Object value = values.get("_id");
            if (value != null) {
                long id = ((Long) value).longValue();
                if (id > -1) {
                    Cursor cursor = db.query("alarm_templates", new String[]{"_id"}, "_id = ?", new String[]{id + ""}, null, null, null);
                    if (cursor.moveToFirst()) {
                        values.putNull("_id");
                    }
                }
            }
            long rowId = db.insert("alarm_templates", "ringtone", values);
            db.setTransactionSuccessful();
            db.endTransaction();
            if (rowId < 0) {
                throw new SQLException("Failed to insert row");
            }
            LogUtils.v("Added alarm rowId = " + rowId, new Object[0]);
            return rowId;
        } catch (Throwable th) {
            db.endTransaction();
            throw th;
        }
    }
}
