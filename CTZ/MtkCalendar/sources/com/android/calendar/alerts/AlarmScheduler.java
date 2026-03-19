package com.android.calendar.alerts;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;
import com.android.calendar.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AlarmScheduler {
    static final String[] INSTANCES_PROJECTION = {"event_id", "begin", "allDay"};
    static final String[] REMINDERS_PROJECTION = {"event_id", "minutes", "method"};

    public static void scheduleNextAlarm(Context context) throws Throwable {
        scheduleNextAlarm(context, AlertUtils.createAlarmManager(context), 50, System.currentTimeMillis());
    }

    static void scheduleNextAlarm(Context context, AlarmManagerInterface alarmManagerInterface, int i, long j) throws Throwable {
        Cursor cursor = null;
        try {
            Cursor cursorQueryUpcomingEvents = queryUpcomingEvents(context, context.getContentResolver(), j);
            if (cursorQueryUpcomingEvents != null) {
                try {
                    queryNextReminderAndSchedule(cursorQueryUpcomingEvents, context, context.getContentResolver(), alarmManagerInterface, i, j);
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQueryUpcomingEvents;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQueryUpcomingEvents != null) {
                cursorQueryUpcomingEvents.close();
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static Cursor queryUpcomingEvents(Context context, ContentResolver contentResolver, long j) {
        Time time = new Time();
        time.normalize(false);
        long j2 = j + 604800000;
        long j3 = j - (time.gmtoff * 1000);
        Uri.Builder builderBuildUpon = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builderBuildUpon, j - 86400000);
        ContentUris.appendId(builderBuildUpon, 86400000 + j2);
        return contentResolver.query(builderBuildUpon.build(), INSTANCES_PROJECTION, "(visible=? AND begin>=? AND begin<=? AND allDay=?) OR (visible=? AND begin>=? AND begin<=? AND allDay=?)", new String[]{"1", String.valueOf(j3), String.valueOf(604800000 + j3), "1", "1", String.valueOf(j), String.valueOf(j2), "0"}, null);
    }

    private static void queryNextReminderAndSchedule(Cursor cursor, Context context, ContentResolver contentResolver, AlarmManagerInterface alarmManagerInterface, int i, long j) throws Throwable {
        int i2;
        int i3;
        Cursor cursorQuery;
        int count = cursor.getCount();
        if (count == 0) {
            Log.d("AlarmScheduler", "No events found starting within 1 week.");
        } else {
            Log.d("AlarmScheduler", "Query result count for events starting within 1 week: " + count);
        }
        HashMap map = new HashMap();
        Time time = new Time();
        cursor.moveToPosition(-1);
        int i4 = 0;
        int i5 = 0;
        long j2 = Long.MAX_VALUE;
        while (!cursor.isAfterLast()) {
            map.clear();
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            int i6 = i4;
            while (true) {
                int i7 = i6 + 1;
                i2 = 3;
                i3 = 1;
                if (i6 >= i || !cursor.moveToNext()) {
                    break;
                }
                int i8 = cursor.getInt(i4);
                long jConvertAlldayUtcToLocal = cursor.getLong(1);
                boolean z = cursor.getInt(2) != 0;
                if (z) {
                    jConvertAlldayUtcToLocal = Utils.convertAlldayUtcToLocal(time, jConvertAlldayUtcToLocal, Time.getCurrentTimezone());
                }
                List arrayList = (List) map.get(Integer.valueOf(i8));
                if (arrayList == null) {
                    arrayList = new ArrayList();
                    map.put(Integer.valueOf(i8), arrayList);
                    sb.append(i8);
                    sb.append(",");
                }
                arrayList.add(Long.valueOf(jConvertAlldayUtcToLocal));
                if (Log.isLoggable("AlarmScheduler", 3)) {
                    time.set(jConvertAlldayUtcToLocal);
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Events cursor result -- eventId:");
                    sb2.append(i8);
                    sb2.append(", allDay:");
                    sb2.append(z);
                    sb2.append(", start:");
                    sb2.append(jConvertAlldayUtcToLocal);
                    sb2.append(" (");
                    sb2.append(time.format("%a, %b %d, %Y %I:%M%P"));
                    sb2.append(")");
                }
                i6 = i7;
                i4 = 0;
            }
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(')');
            try {
                cursorQuery = contentResolver.query(CalendarContract.Reminders.CONTENT_URI, REMINDERS_PROJECTION, "method=1 AND event_id IN " + ((Object) sb), null, null);
                if (cursorQuery != null) {
                    try {
                        cursorQuery.moveToPosition(-1);
                        while (cursorQuery.moveToNext()) {
                            int i9 = cursorQuery.getInt(0);
                            int i10 = cursorQuery.getInt(i3);
                            List<Long> list = (List) map.get(Integer.valueOf(i9));
                            if (list != null) {
                                for (Long l : list) {
                                    long jLongValue = l.longValue() - (((long) i10) * 60000);
                                    if (jLongValue > j && jLongValue < j2) {
                                        i5 = i9;
                                        j2 = jLongValue;
                                    }
                                    if (Log.isLoggable("AlarmScheduler", i2)) {
                                        time.set(jLongValue);
                                        StringBuilder sb3 = new StringBuilder();
                                        sb3.append("Reminders cursor result -- eventId:");
                                        sb3.append(i9);
                                        sb3.append(", startTime:");
                                        sb3.append(l);
                                        sb3.append(", minutes:");
                                        sb3.append(i10);
                                        sb3.append(", alarmTime:");
                                        sb3.append(jLongValue);
                                        sb3.append(" (");
                                        sb3.append(time.format("%a, %b %d, %Y %I:%M%P"));
                                        sb3.append(")");
                                    }
                                    i2 = 3;
                                }
                            }
                            i2 = 3;
                            i3 = 1;
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                i4 = 0;
            } catch (Throwable th2) {
                th = th2;
                cursorQuery = null;
            }
        }
        if (j2 < Long.MAX_VALUE) {
            scheduleAlarm(context, i5, j2, j, alarmManagerInterface);
        }
    }

    private static void scheduleAlarm(Context context, long j, long j2, long j3, AlarmManagerInterface alarmManagerInterface) {
        long j4 = j3 + 86400000;
        if (j2 > j4) {
            j2 = j4;
        }
        long j5 = j2 + 1000;
        Time time = new Time();
        time.set(j5);
        Log.d("AlarmScheduler", "Scheduling alarm for EVENT_REMINDER_APP broadcast for event " + j + " at " + j5 + " (" + time.format("%a, %b %d, %Y %I:%M%P") + ")");
        Intent intent = new Intent("com.android.calendar.EVENT_REMINDER_APP");
        intent.setClass(context, AlertReceiver.class);
        intent.putExtra("alarmTime", j5);
        alarmManagerInterface.set(0, j5, PendingIntent.getBroadcast(context, 0, intent, 0));
    }
}
