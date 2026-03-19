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
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.provider.ClockContract;
import java.util.Calendar;

class ClockDatabaseHelper extends SQLiteOpenHelper {
    static final String ALARMS_TABLE_NAME = "alarm_templates";
    static final String DATABASE_NAME = "alarms.db";
    private static final String DEFAULT_ALARM_1 = "(8, 30, 31, 0, 1, '', NULL, 0);";
    private static final String DEFAULT_ALARM_2 = "(9, 00, 96, 0, 1, '', NULL, 0);";
    static final String INSTANCES_TABLE_NAME = "alarm_instances";
    static final String OLD_ALARMS_TABLE_NAME = "alarms";
    private static final String SELECTED_CITIES_TABLE_NAME = "selected_cities";
    private static final int VERSION_5 = 5;
    private static final int VERSION_6 = 6;
    private static final int VERSION_7 = 7;
    private static final int VERSION_8 = 8;

    private static void createAlarmsTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE alarm_templates (_id INTEGER PRIMARY KEY,hour INTEGER NOT NULL, minutes INTEGER NOT NULL, daysofweek INTEGER NOT NULL, enabled INTEGER NOT NULL, vibrate INTEGER NOT NULL, label TEXT NOT NULL, ringtone TEXT, delete_after_use INTEGER NOT NULL DEFAULT 0);");
        LogUtils.i("Alarms Table created", new Object[0]);
    }

    private static void createInstanceTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE alarm_instances (_id INTEGER PRIMARY KEY,year INTEGER NOT NULL, month INTEGER NOT NULL, day INTEGER NOT NULL, hour INTEGER NOT NULL, minutes INTEGER NOT NULL, vibrate INTEGER NOT NULL, label TEXT NOT NULL, ringtone TEXT, alarm_state INTEGER NOT NULL, alarm_id INTEGER REFERENCES alarm_templates(_id) ON UPDATE CASCADE ON DELETE CASCADE);");
        LogUtils.i("Instance table created", new Object[0]);
    }

    public ClockDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 8);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        createAlarmsTable(sQLiteDatabase);
        createInstanceTable(sQLiteDatabase);
        LogUtils.i("Inserting default alarms", new Object[0]);
        String str = "INSERT INTO alarm_templates (hour, minutes, " + ClockContract.AlarmsColumns.DAYS_OF_WEEK + ", " + ClockContract.AlarmsColumns.ENABLED + ", " + ClockContract.AlarmSettingColumns.VIBRATE + ", " + ClockContract.AlarmSettingColumns.LABEL + ", " + ClockContract.AlarmSettingColumns.RINGTONE + ", " + ClockContract.AlarmsColumns.DELETE_AFTER_USE + ") VALUES ";
        sQLiteDatabase.execSQL(str + DEFAULT_ALARM_1);
        sQLiteDatabase.execSQL(str + DEFAULT_ALARM_2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws Exception {
        LogUtils.v("Upgrading alarms database from version %d to %d", Integer.valueOf(i), Integer.valueOf(i2));
        if (i <= 7) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS selected_cities;");
        }
        if (i <= 6) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS alarm_instances;");
            createAlarmsTable(sQLiteDatabase);
            createInstanceTable(sQLiteDatabase);
            LogUtils.i("Copying old alarms to new table", new Object[0]);
            Cursor cursorQuery = sQLiteDatabase.query(OLD_ALARMS_TABLE_NAME, new String[]{"_id", "hour", "minutes", ClockContract.AlarmsColumns.DAYS_OF_WEEK, ClockContract.AlarmsColumns.ENABLED, ClockContract.AlarmSettingColumns.VIBRATE, "message", "alert"}, null, null, null, null, null);
            try {
                Calendar calendar = Calendar.getInstance();
                while (cursorQuery != null) {
                    if (!cursorQuery.moveToNext()) {
                        break;
                    }
                    Alarm alarm = new Alarm();
                    alarm.id = cursorQuery.getLong(0);
                    alarm.hour = cursorQuery.getInt(1);
                    alarm.minutes = cursorQuery.getInt(2);
                    alarm.daysOfWeek = Weekdays.fromBits(cursorQuery.getInt(3));
                    alarm.enabled = cursorQuery.getInt(4) == 1;
                    alarm.vibrate = cursorQuery.getInt(5) == 1;
                    alarm.label = cursorQuery.getString(6);
                    String string = cursorQuery.getString(7);
                    if ("silent".equals(string)) {
                        alarm.alert = Alarm.NO_RINGTONE_URI;
                    } else {
                        alarm.alert = TextUtils.isEmpty(string) ? null : Uri.parse(string);
                    }
                    sQLiteDatabase.insert(ALARMS_TABLE_NAME, null, Alarm.createContentValues(alarm));
                    if (alarm.enabled) {
                        sQLiteDatabase.insert(INSTANCES_TABLE_NAME, null, AlarmInstance.createContentValues(alarm.createInstanceAfter(calendar)));
                    }
                }
                LogUtils.i("Dropping old alarm table", new Object[0]);
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS alarms;");
            } finally {
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    long fixAlarmInsert(ContentValues contentValues) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            Object obj = contentValues.get("_id");
            if (obj != null) {
                long jLongValue = ((Long) obj).longValue();
                if (jLongValue > -1) {
                    Cursor cursorQuery = writableDatabase.query(ALARMS_TABLE_NAME, new String[]{"_id"}, "_id = ?", new String[]{String.valueOf(jLongValue)}, null, null, null);
                    Throwable th = null;
                    try {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                contentValues.putNull("_id");
                            }
                        } finally {
                        }
                    } finally {
                        if (cursorQuery != null) {
                            $closeResource(th, cursorQuery);
                        }
                    }
                }
            }
            long jInsert = writableDatabase.insert(ALARMS_TABLE_NAME, ClockContract.AlarmSettingColumns.RINGTONE, contentValues);
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
            if (jInsert < 0) {
                throw new SQLException("Failed to insert row");
            }
            LogUtils.v("Added alarm rowId = " + jInsert, new Object[0]);
            return jInsert;
        } catch (Throwable th2) {
            writableDatabase.endTransaction();
            throw th2;
        }
    }
}
