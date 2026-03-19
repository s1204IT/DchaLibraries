package com.android.providers.calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
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

    public CalendarAlarmManager(Context context) {
        initializeWithContext(context);
    }

    protected void initializeWithContext(Context context) {
        this.mContext = context;
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mNextAlarmCheckScheduled = new AtomicBoolean(false);
        this.mAlarmLock = new Object();
    }

    void scheduleNextAlarm(boolean z) {
        if (!this.mNextAlarmCheckScheduled.getAndSet(true) || z) {
            if (Log.isLoggable("CalendarProvider2", 3)) {
                Log.d("CalendarProvider2", "Scheduling check of next Alarm");
            }
            Intent intent = new Intent("com.android.providers.calendar.intent.CalendarProvider2");
            intent.putExtra("removeAlarms", z);
            intent.setClass(this.mContext, CalendarProviderBroadcastReceiver.class);
            PendingIntent broadcast = PendingIntent.getBroadcast(this.mContext, 0, intent, 536870912);
            if (broadcast != null) {
                cancel(broadcast);
            }
            PendingIntent broadcast2 = PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456);
            set(2, SystemClock.elapsedRealtime() + 3000, broadcast2);
            Log.i("CalendarProvider2", "scheduleNextAlarm after 3000ms, pending=" + broadcast2);
        }
    }

    void rescheduleMissedAlarms() {
        rescheduleMissedAlarms(this.mContext.getContentResolver());
    }

    void runScheduleNextAlarm(boolean z, CalendarProvider2 calendarProvider2) {
        SQLiteDatabase writableDatabase = calendarProvider2.getWritableDatabase();
        if (writableDatabase == null) {
            Log.wtf("CalendarProvider2", "Unable to get the database.");
            return;
        }
        this.mNextAlarmCheckScheduled.set(false);
        writableDatabase.beginTransaction();
        if (z) {
            try {
                removeScheduledAlarmsLocked(writableDatabase);
            } catch (Throwable th) {
                writableDatabase.endTransaction();
                throw th;
            }
        }
        scheduleNextAlarmLocked(writableDatabase, calendarProvider2);
        writableDatabase.setTransactionSuccessful();
        writableDatabase.endTransaction();
    }

    void scheduleNextAlarmCheck(long j) {
        Intent intent = new Intent("com.android.providers.calendar.SCHEDULE_ALARM");
        intent.setClass(this.mContext, CalendarReceiver.class);
        PendingIntent broadcast = PendingIntent.getBroadcast(this.mContext, 0, intent, 536870912);
        if (broadcast != null) {
            cancel(broadcast);
        }
        PendingIntent broadcast2 = PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456);
        if (Log.isLoggable("CalendarProvider2", 3)) {
            Time time = new Time();
            time.set(j);
            Log.d("CalendarProvider2", "scheduleNextAlarmCheck at: " + j + time.format(" %a, %b %d, %Y %I:%M%P"));
        }
        set(0, j, broadcast2);
    }

    private void scheduleNextAlarmLocked(SQLiteDatabase sQLiteDatabase, CalendarProvider2 calendarProvider2) throws Throwable {
        ContentResolver contentResolver;
        long j;
        Cursor cursorRawQuery;
        Time time;
        long j2;
        long j3;
        int i;
        int i2;
        char c;
        Time time2 = new Time();
        long jCurrentTimeMillis = System.currentTimeMillis();
        long j4 = jCurrentTimeMillis - 7200000;
        long j5 = j4 + 86400000;
        if (Log.isLoggable("CalendarProvider2", 3)) {
            time2.set(j4);
            Log.d("CalendarProvider2", "runScheduleNextAlarm() start search: " + time2.format(" %a, %b %d, %Y %I:%M%P"));
        }
        int iDelete = sQLiteDatabase.delete("CalendarAlerts", "_id IN (SELECT ca._id FROM CalendarAlerts AS ca LEFT OUTER JOIN Instances USING (event_id,begin,end) LEFT OUTER JOIN Reminders AS r ON (ca.event_id=r.event_id AND ca.minutes=r.minutes) LEFT OUTER JOIN view_events AS e ON (ca.event_id=e._id) WHERE Instances.begin ISNULL   OR ca.alarmTime<?   OR (r.minutes ISNULL       AND ca.minutes<>0)   OR e.visible=0)", new String[]{Long.toString(jCurrentTimeMillis - 612000000)});
        ContentResolver contentResolver2 = this.mContext.getContentResolver();
        long jFindNextAlarmTime = CalendarContract.CalendarAlerts.findNextAlarmTime(contentResolver2, jCurrentTimeMillis);
        if (jFindNextAlarmTime == -1 || jFindNextAlarmTime >= j5) {
            contentResolver = contentResolver2;
            j = j5;
        } else {
            contentResolver = contentResolver2;
            j = jFindNextAlarmTime;
        }
        time2.setToNow();
        time2.normalize(false);
        long j6 = j;
        String str = "SELECT * FROM (" + ("SELECT begin" + (" -(" + (time2.gmtoff * 1000) + ") ") + " -(minutes*60000) AS myAlarmTime,Instances.event_id AS eventId,begin,end,title,allDay,method,minutes FROM Instances INNER JOIN view_events ON (view_events._id=Instances.event_id) INNER JOIN Reminders ON (Instances.event_id=Reminders.event_id) WHERE visible=1 AND myAlarmTime>=CAST(? AS INT) AND myAlarmTime<=CAST(? AS INT) AND end>=? AND method=1 AND allDay=1") + " UNION ALL " + ("SELECT begin -(minutes*60000) AS myAlarmTime,Instances.event_id AS eventId,begin,end,title,allDay,method,minutes FROM Instances INNER JOIN view_events ON (view_events._id=Instances.event_id) INNER JOIN Reminders ON (Instances.event_id=Reminders.event_id) WHERE visible=1 AND myAlarmTime>=CAST(? AS INT) AND myAlarmTime<=CAST(? AS INT) AND end>=? AND method=1 AND allDay=0") + ") WHERE 0=(SELECT count(*) FROM CalendarAlerts CA WHERE CA.event_id=eventId AND CA.begin=begin AND CA.alarmTime=myAlarmTime) ORDER BY myAlarmTime,begin,title";
        String[] strArr = {String.valueOf(j4), String.valueOf(j6), String.valueOf(jCurrentTimeMillis), String.valueOf(j4), String.valueOf(j6), String.valueOf(jCurrentTimeMillis)};
        long j7 = j6;
        calendarProvider2.acquireInstanceRangeLocked(j4 - 86400000, j5 + 86400000, false, false, calendarProvider2.mCalendarCache.readTimezoneInstances(), "home".equals(calendarProvider2.mCalendarCache.readTimezoneType()));
        try {
            cursorRawQuery = sQLiteDatabase.rawQuery(str, strArr);
            try {
                int columnIndex = cursorRawQuery.getColumnIndex("begin");
                int columnIndex2 = cursorRawQuery.getColumnIndex("end");
                int columnIndex3 = cursorRawQuery.getColumnIndex("eventId");
                int columnIndex4 = cursorRawQuery.getColumnIndex("myAlarmTime");
                int columnIndex5 = cursorRawQuery.getColumnIndex("minutes");
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    time = time2;
                    time.set(j7);
                    Log.d("CalendarProvider2", "cursor results: " + cursorRawQuery.getCount() + " nextAlarmTime: " + time.format(" %a, %b %d, %Y %I:%M%P"));
                } else {
                    time = time2;
                }
                while (true) {
                    if (!cursorRawQuery.moveToNext()) {
                        j2 = j7;
                        break;
                    }
                    long j8 = cursorRawQuery.getLong(columnIndex4);
                    long j9 = cursorRawQuery.getLong(columnIndex3);
                    int i3 = cursorRawQuery.getInt(columnIndex5);
                    j2 = j7;
                    long j10 = cursorRawQuery.getLong(columnIndex);
                    long j11 = cursorRawQuery.getLong(columnIndex2);
                    if (Log.isLoggable("CalendarProvider2", 3)) {
                        time.set(j8);
                        String str2 = time.format(" %a, %b %d, %Y %I:%M%P");
                        time.set(j10);
                        String str3 = time.format(" %a, %b %d, %Y %I:%M%P");
                        i = columnIndex;
                        StringBuilder sb = new StringBuilder();
                        i2 = columnIndex2;
                        sb.append("  looking at id: ");
                        sb.append(j9);
                        sb.append(" ");
                        sb.append(j10);
                        sb.append(str3);
                        sb.append(" alarm: ");
                        sb.append(j8);
                        sb.append(str2);
                        Log.d("CalendarProvider2", sb.toString());
                    } else {
                        i = columnIndex;
                        i2 = columnIndex2;
                    }
                    if (j8 >= j2) {
                        if (j8 > j2 + 60000) {
                            break;
                        }
                    } else {
                        j2 = j8;
                    }
                    if (CalendarContract.CalendarAlerts.alarmExists(contentResolver, j9, j10, j8)) {
                        c = 3;
                        if (Log.isLoggable("CalendarProvider2", 3)) {
                            Log.d("CalendarProvider2", "  alarm exists for id: " + j9 + " " + cursorRawQuery.getString(cursorRawQuery.getColumnIndex("title")));
                        }
                    } else {
                        c = 3;
                        if (CalendarContract.CalendarAlerts.insert(contentResolver, j9, j10, j11, j8, i3) != null) {
                            scheduleAlarm(j8);
                        } else if (Log.isLoggable("CalendarProvider2", 6)) {
                            Log.e("CalendarProvider2", "runScheduleNextAlarm() insert into CalendarAlerts table failed");
                        }
                    }
                    j7 = j2;
                    columnIndex = i;
                    columnIndex2 = i2;
                }
                if (cursorRawQuery != null) {
                    cursorRawQuery.close();
                }
                if (iDelete > 0) {
                    j3 = jCurrentTimeMillis;
                    scheduleAlarm(j3);
                } else {
                    j3 = jCurrentTimeMillis;
                }
                if (j2 != Long.MAX_VALUE) {
                    scheduleNextAlarmCheck(j2 + 60000);
                } else {
                    scheduleNextAlarmCheck(j3 + 86400000);
                }
            } catch (Throwable th) {
                th = th;
                if (cursorRawQuery != null) {
                    cursorRawQuery.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorRawQuery = null;
        }
    }

    private static void removeScheduledAlarmsLocked(SQLiteDatabase sQLiteDatabase) {
        if (Log.isLoggable("CalendarProvider2", 3)) {
            Log.d("CalendarProvider2", "removing scheduled alarms");
        }
        sQLiteDatabase.delete("CalendarAlerts", "state=0", null);
    }

    public void set(int i, long j, PendingIntent pendingIntent) {
        this.mAlarmManager.setExact(i, j, pendingIntent);
    }

    public void seto(int i, long j, PendingIntent pendingIntent) {
        this.mAlarmManager.set(i, j, pendingIntent);
    }

    public void cancel(PendingIntent pendingIntent) {
        this.mAlarmManager.cancel(pendingIntent);
    }

    public void scheduleAlarm(long j) {
        Log.d("CalendarAlarmManager", "schedule reminder alarm fired at " + j);
        CalendarContract.CalendarAlerts.scheduleAlarm(this.mContext, this.mAlarmManager, j);
    }

    public void rescheduleMissedAlarms(ContentResolver contentResolver) {
        CalendarContract.CalendarAlerts.rescheduleMissedAlarms(contentResolver, this.mContext, this.mAlarmManager);
    }
}
