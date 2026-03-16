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
import java.util.Map;

public class AlarmScheduler {
    static final String[] INSTANCES_PROJECTION = {"event_id", "begin", "allDay"};
    static final String[] REMINDERS_PROJECTION = {"event_id", "minutes", "method"};

    public static void scheduleNextAlarm(Context context) {
        scheduleNextAlarm(context, AlertUtils.createAlarmManager(context), 50, System.currentTimeMillis());
    }

    static void scheduleNextAlarm(Context context, AlarmManagerInterface alarmManager, int batchSize, long currentMillis) {
        Cursor instancesCursor = null;
        try {
            instancesCursor = queryUpcomingEvents(context, context.getContentResolver(), currentMillis);
            if (instancesCursor != null) {
                queryNextReminderAndSchedule(instancesCursor, context, context.getContentResolver(), alarmManager, batchSize, currentMillis);
            }
        } finally {
            if (instancesCursor != null) {
                instancesCursor.close();
            }
        }
    }

    private static Cursor queryUpcomingEvents(Context context, ContentResolver contentResolver, long currentMillis) {
        Time time = new Time();
        time.normalize(false);
        long localOffset = time.gmtoff * 1000;
        long localStartMax = currentMillis + 604800000;
        long utcStartMin = currentMillis - localOffset;
        long utcStartMax = utcStartMin + 604800000;
        Uri.Builder uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uriBuilder, currentMillis - 86400000);
        ContentUris.appendId(uriBuilder, 86400000 + localStartMax);
        String[] queryArgs = {"1", String.valueOf(utcStartMin), String.valueOf(utcStartMax), "1", "1", String.valueOf(currentMillis), String.valueOf(localStartMax), "0"};
        Cursor cursor = contentResolver.query(uriBuilder.build(), INSTANCES_PROJECTION, "(visible=? AND begin>=? AND begin<=? AND allDay=?) OR (visible=? AND begin>=? AND begin<=? AND allDay=?)", queryArgs, null);
        return cursor;
    }

    private static void queryNextReminderAndSchedule(Cursor instancesCursor, Context context, ContentResolver contentResolver, AlarmManagerInterface alarmManager, int batchSize, long currentMillis) {
        long localStartTime;
        int eventCount = instancesCursor.getCount();
        if (eventCount == 0) {
            Log.d("AlarmScheduler", "No events found starting within 1 week.");
        } else {
            Log.d("AlarmScheduler", "Query result count for events starting within 1 week: " + eventCount);
        }
        Map<Integer, List<Long>> eventMap = new HashMap<>();
        Time timeObj = new Time();
        long nextAlarmTime = Long.MAX_VALUE;
        int nextAlarmEventId = 0;
        instancesCursor.moveToPosition(-1);
        while (!instancesCursor.isAfterLast()) {
            int index = 0;
            eventMap.clear();
            StringBuilder eventIdsForQuery = new StringBuilder();
            eventIdsForQuery.append('(');
            while (true) {
                int index2 = index;
                index = index2 + 1;
                if (index2 >= batchSize || !instancesCursor.moveToNext()) {
                    break;
                }
                int eventId = instancesCursor.getInt(0);
                long begin = instancesCursor.getLong(1);
                boolean allday = instancesCursor.getInt(2) != 0;
                if (allday) {
                    localStartTime = Utils.convertAlldayUtcToLocal(timeObj, begin, Time.getCurrentTimezone());
                } else {
                    localStartTime = begin;
                }
                List<Long> startTimes = eventMap.get(Integer.valueOf(eventId));
                if (startTimes == null) {
                    startTimes = new ArrayList<>();
                    eventMap.put(Integer.valueOf(eventId), startTimes);
                    eventIdsForQuery.append(eventId);
                    eventIdsForQuery.append(",");
                }
                startTimes.add(Long.valueOf(localStartTime));
                if (Log.isLoggable("AlarmScheduler", 3)) {
                    timeObj.set(localStartTime);
                    StringBuilder msg = new StringBuilder();
                    msg.append("Events cursor result -- eventId:").append(eventId);
                    msg.append(", allDay:").append(allday);
                    msg.append(", start:").append(localStartTime);
                    msg.append(" (").append(timeObj.format("%a, %b %d, %Y %I:%M%P")).append(")");
                    Log.d("AlarmScheduler", msg.toString());
                }
            }
            if (eventIdsForQuery.charAt(eventIdsForQuery.length() - 1) == ',') {
                eventIdsForQuery.deleteCharAt(eventIdsForQuery.length() - 1);
            }
            eventIdsForQuery.append(')');
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(CalendarContract.Reminders.CONTENT_URI, REMINDERS_PROJECTION, "method=1 AND event_id IN " + ((Object) eventIdsForQuery), null, null);
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    int eventId2 = cursor.getInt(0);
                    int reminderMinutes = cursor.getInt(1);
                    List<Long> startTimes2 = eventMap.get(Integer.valueOf(eventId2));
                    if (startTimes2 != null) {
                        for (Long startTime : startTimes2) {
                            long alarmTime = startTime.longValue() - (((long) reminderMinutes) * 60000);
                            if (alarmTime > currentMillis && alarmTime < nextAlarmTime) {
                                nextAlarmTime = alarmTime;
                                nextAlarmEventId = eventId2;
                            }
                            if (Log.isLoggable("AlarmScheduler", 3)) {
                                timeObj.set(alarmTime);
                                StringBuilder msg2 = new StringBuilder();
                                msg2.append("Reminders cursor result -- eventId:").append(eventId2);
                                msg2.append(", startTime:").append(startTime);
                                msg2.append(", minutes:").append(reminderMinutes);
                                msg2.append(", alarmTime:").append(alarmTime);
                                msg2.append(" (").append(timeObj.format("%a, %b %d, %Y %I:%M%P")).append(")");
                                Log.d("AlarmScheduler", msg2.toString());
                            }
                        }
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
        }
        if (nextAlarmTime < Long.MAX_VALUE) {
            scheduleAlarm(context, nextAlarmEventId, nextAlarmTime, currentMillis, alarmManager);
        }
    }

    private static void scheduleAlarm(Context context, long eventId, long alarmTime, long currentMillis, AlarmManagerInterface alarmManager) {
        long maxAlarmTime = currentMillis + 86400000;
        if (alarmTime > maxAlarmTime) {
            alarmTime = maxAlarmTime;
        }
        long alarmTime2 = alarmTime + 1000;
        Time time = new Time();
        time.set(alarmTime2);
        String schedTime = time.format("%a, %b %d, %Y %I:%M%P");
        Log.d("AlarmScheduler", "Scheduling alarm for EVENT_REMINDER_APP broadcast for event " + eventId + " at " + alarmTime2 + " (" + schedTime + ")");
        Intent intent = new Intent("com.android.calendar.EVENT_REMINDER_APP");
        intent.setClass(context, AlertReceiver.class);
        intent.putExtra("alarmTime", alarmTime2);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.set(0, alarmTime2, pi);
    }
}
