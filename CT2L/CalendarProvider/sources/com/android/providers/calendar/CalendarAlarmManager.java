package com.android.providers.calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

public class CalendarAlarmManager {
    static final Uri SCHEDULE_ALARM_REMOVE_URI = Uri.withAppendedPath(CalendarContract.CONTENT_URI, "schedule_alarms_remove");
    static final Uri SCHEDULE_ALARM_URI = Uri.withAppendedPath(CalendarContract.CONTENT_URI, "schedule_alarms");
    protected Object mAlarmLock;
    private AlarmManager mAlarmManager;
    protected Context mContext;
    protected AtomicBoolean mNextAlarmCheckScheduled;
    private final PowerManager.WakeLock mScheduleNextAlarmWakeLock;

    public CalendarAlarmManager(Context context) {
        initializeWithContext(context);
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mScheduleNextAlarmWakeLock = powerManager.newWakeLock(1, "ScheduleNextAlarmWakeLock");
        this.mScheduleNextAlarmWakeLock.setReferenceCounted(true);
    }

    protected void initializeWithContext(Context context) {
        this.mContext = context;
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mNextAlarmCheckScheduled = new AtomicBoolean(false);
        this.mAlarmLock = new Object();
    }

    void scheduleNextAlarm(boolean removeAlarms) {
        if (!this.mNextAlarmCheckScheduled.getAndSet(true) || removeAlarms) {
            if (Log.isLoggable("CalendarProvider2", 3)) {
                Log.d("CalendarProvider2", "Scheduling check of next Alarm");
            }
            Intent intent = new Intent("com.android.providers.calendar.intent.CalendarProvider2");
            intent.putExtra("removeAlarms", removeAlarms);
            PendingIntent pending = PendingIntent.getBroadcast(this.mContext, 0, intent, 536870912);
            if (pending != null) {
                cancel(pending);
            }
            PendingIntent pending2 = PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456);
            long triggerAtTime = SystemClock.elapsedRealtime() + 5000;
            set(2, triggerAtTime, pending2);
        }
    }

    PowerManager.WakeLock getScheduleNextAlarmWakeLock() {
        return this.mScheduleNextAlarmWakeLock;
    }

    void acquireScheduleNextAlarmWakeLock() {
        getScheduleNextAlarmWakeLock().acquire();
    }

    void releaseScheduleNextAlarmWakeLock() {
        try {
            getScheduleNextAlarmWakeLock().release();
        } catch (RuntimeException e) {
            if (!e.getMessage().startsWith("WakeLock under-locked ")) {
                throw e;
            }
            Log.w("CalendarAlarmManager", "WakeLock under-locked ignored.");
        }
    }

    void rescheduleMissedAlarms() {
        rescheduleMissedAlarms(this.mContext.getContentResolver());
    }

    void runScheduleNextAlarm(boolean removeAlarms, CalendarProvider2 cp2) {
        SQLiteDatabase db = cp2.mDb;
        if (db != null) {
            this.mNextAlarmCheckScheduled.set(false);
            db.beginTransaction();
            if (removeAlarms) {
                try {
                    removeScheduledAlarmsLocked(db);
                } finally {
                    db.endTransaction();
                }
            }
            scheduleNextAlarmLocked(db, cp2);
            db.setTransactionSuccessful();
        }
    }

    void scheduleNextAlarmCheck(long triggerTime) {
        Intent intent = new Intent("com.android.providers.calendar.SCHEDULE_ALARM");
        intent.setClass(this.mContext, CalendarReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(this.mContext, 0, intent, 536870912);
        if (pending != null) {
            cancel(pending);
        }
        PendingIntent pending2 = PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456);
        if (Log.isLoggable("CalendarProvider2", 3)) {
            Time time = new Time();
            time.set(triggerTime);
            String timeStr = time.format(" %a, %b %d, %Y %I:%M%P");
            Log.d("CalendarProvider2", "scheduleNextAlarmCheck at: " + triggerTime + timeStr);
        }
        set(0, triggerTime, pending2);
    }

    private void scheduleNextAlarmLocked(SQLiteDatabase db, CalendarProvider2 cp2) {
        Time time = new Time();
        long currentMillis = System.currentTimeMillis();
        long start = currentMillis - 7200000;
        long end = start + 86400000;
        if (Log.isLoggable("CalendarProvider2", 3)) {
            time.set(start);
            String startTimeStr = time.format(" %a, %b %d, %Y %I:%M%P");
            Log.d("CalendarProvider2", "runScheduleNextAlarm() start search: " + startTimeStr);
        }
        String[] selectArg = {Long.toString(currentMillis - 612000000)};
        int rowsDeleted = db.delete("CalendarAlerts", "_id IN (SELECT ca._id FROM CalendarAlerts AS ca LEFT OUTER JOIN Instances USING (event_id,begin,end) LEFT OUTER JOIN Reminders AS r ON (ca.event_id=r.event_id AND ca.minutes=r.minutes) LEFT OUTER JOIN view_events AS e ON (ca.event_id=e._id) WHERE Instances.begin ISNULL   OR ca.alarmTime<?   OR (r.minutes ISNULL       AND ca.minutes<>0)   OR e.visible=0)", selectArg);
        long nextAlarmTime = end;
        ContentResolver resolver = this.mContext.getContentResolver();
        long tmpAlarmTime = CalendarContract.CalendarAlerts.findNextAlarmTime(resolver, currentMillis);
        if (tmpAlarmTime != -1 && tmpAlarmTime < nextAlarmTime) {
            nextAlarmTime = tmpAlarmTime;
        }
        time.setToNow();
        time.normalize(false);
        long localOffset = time.gmtoff * 1000;
        String allDayOffset = " -(" + localOffset + ") ";
        String allDayQuery = "SELECT begin" + allDayOffset + " -(minutes*60000) AS myAlarmTime,Instances.event_id AS eventId,begin,end,title,allDay,method,minutes FROM Instances INNER JOIN view_events ON (view_events._id=Instances.event_id) INNER JOIN Reminders ON (Instances.event_id=Reminders.event_id) WHERE visible=1 AND myAlarmTime>=CAST(? AS INT) AND myAlarmTime<=CAST(? AS INT) AND end>=? AND method=1 AND allDay=1";
        String nonAllDayQuery = "SELECT begin -(minutes*60000) AS myAlarmTime,Instances.event_id AS eventId,begin,end,title,allDay,method,minutes FROM Instances INNER JOIN view_events ON (view_events._id=Instances.event_id) INNER JOIN Reminders ON (Instances.event_id=Reminders.event_id) WHERE visible=1 AND myAlarmTime>=CAST(? AS INT) AND myAlarmTime<=CAST(? AS INT) AND end>=? AND method=1 AND allDay=0";
        String query = "SELECT * FROM (" + allDayQuery + " UNION ALL " + nonAllDayQuery + ") WHERE 0=(SELECT count(*) FROM CalendarAlerts CA WHERE CA.event_id=eventId AND CA.begin=begin AND CA.alarmTime=myAlarmTime) ORDER BY myAlarmTime,begin,title";
        String[] queryParams = {String.valueOf(start), String.valueOf(nextAlarmTime), String.valueOf(currentMillis), String.valueOf(start), String.valueOf(nextAlarmTime), String.valueOf(currentMillis)};
        String instancesTimezone = cp2.mCalendarCache.readTimezoneInstances();
        String timezoneType = cp2.mCalendarCache.readTimezoneType();
        boolean isHomeTimezone = "home".equals(timezoneType);
        cp2.acquireInstanceRangeLocked(start - 86400000, end + 86400000, false, false, instancesTimezone, isHomeTimezone);
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, queryParams);
            int beginIndex = cursor.getColumnIndex("begin");
            int endIndex = cursor.getColumnIndex("end");
            int eventIdIndex = cursor.getColumnIndex("eventId");
            int alarmTimeIndex = cursor.getColumnIndex("myAlarmTime");
            int minutesIndex = cursor.getColumnIndex("minutes");
            if (Log.isLoggable("CalendarProvider2", 3)) {
                time.set(nextAlarmTime);
                String alarmTimeStr = time.format(" %a, %b %d, %Y %I:%M%P");
                Log.d("CalendarProvider2", "cursor results: " + cursor.getCount() + " nextAlarmTime: " + alarmTimeStr);
            }
            while (true) {
                if (!cursor.moveToNext()) {
                    break;
                }
                long alarmTime = cursor.getLong(alarmTimeIndex);
                long eventId = cursor.getLong(eventIdIndex);
                int minutes = cursor.getInt(minutesIndex);
                long startTime = cursor.getLong(beginIndex);
                long endTime = cursor.getLong(endIndex);
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    time.set(alarmTime);
                    String schedTime = time.format(" %a, %b %d, %Y %I:%M%P");
                    time.set(startTime);
                    String startTimeStr2 = time.format(" %a, %b %d, %Y %I:%M%P");
                    Log.d("CalendarProvider2", "  looking at id: " + eventId + " " + startTime + startTimeStr2 + " alarm: " + alarmTime + schedTime);
                }
                if (alarmTime >= nextAlarmTime) {
                    if (alarmTime > 60000 + nextAlarmTime) {
                        break;
                    }
                } else {
                    nextAlarmTime = alarmTime;
                }
                if (CalendarContract.CalendarAlerts.alarmExists(resolver, eventId, startTime, alarmTime)) {
                    if (Log.isLoggable("CalendarProvider2", 3)) {
                        int titleIndex = cursor.getColumnIndex("title");
                        String title = cursor.getString(titleIndex);
                        Log.d("CalendarProvider2", "  alarm exists for id: " + eventId + " " + title);
                    }
                } else {
                    Uri uri = CalendarContract.CalendarAlerts.insert(resolver, eventId, startTime, endTime, alarmTime, minutes);
                    if (uri == null) {
                        if (Log.isLoggable("CalendarProvider2", 6)) {
                            Log.e("CalendarProvider2", "runScheduleNextAlarm() insert into CalendarAlerts table failed");
                        }
                    } else {
                        scheduleAlarm(alarmTime);
                    }
                }
            }
            if (rowsDeleted > 0) {
                scheduleAlarm(currentMillis);
            }
            if (nextAlarmTime != Long.MAX_VALUE) {
                scheduleNextAlarmCheck(60000 + nextAlarmTime);
            } else {
                scheduleNextAlarmCheck(86400000 + currentMillis);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static void removeScheduledAlarmsLocked(SQLiteDatabase db) {
        if (Log.isLoggable("CalendarProvider2", 3)) {
            Log.d("CalendarProvider2", "removing scheduled alarms");
        }
        db.delete("CalendarAlerts", "state=0", null);
    }

    public void set(int type, long triggerAtTime, PendingIntent operation) {
        this.mAlarmManager.setExact(type, triggerAtTime, operation);
    }

    public void cancel(PendingIntent operation) {
        this.mAlarmManager.cancel(operation);
    }

    public void scheduleAlarm(long alarmTime) {
        CalendarContract.CalendarAlerts.scheduleAlarm(this.mContext, this.mAlarmManager, alarmTime);
    }

    public void rescheduleMissedAlarms(ContentResolver cr) {
        CalendarContract.CalendarAlerts.rescheduleMissedAlarms(cr, this.mContext, this.mAlarmManager);
    }
}
