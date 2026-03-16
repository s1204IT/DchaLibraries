package com.android.calendar.alerts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import com.android.calendar.EventInfoActivity;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AlertUtils {
    static boolean BYPASS_DB = true;

    public static AlarmManagerInterface createAlarmManager(Context context) {
        final AlarmManager mgr = (AlarmManager) context.getSystemService("alarm");
        return new AlarmManagerInterface() {
            @Override
            public void set(int type, long triggerAtMillis, PendingIntent operation) {
                if (Utils.isKeyLimePieOrLater()) {
                    mgr.setExact(type, triggerAtMillis, operation);
                } else {
                    mgr.set(type, triggerAtMillis, operation);
                }
            }
        };
    }

    public static void scheduleAlarm(Context context, AlarmManagerInterface manager, long alarmTime) {
        scheduleAlarmHelper(context, manager, alarmTime, false);
    }

    static void scheduleNextNotificationRefresh(Context context, AlarmManagerInterface manager, long alarmTime) {
        scheduleAlarmHelper(context, manager, alarmTime, true);
    }

    private static void scheduleAlarmHelper(Context context, AlarmManagerInterface manager, long alarmTime, boolean quietUpdate) {
        int alarmType = 0;
        Intent intent = new Intent("com.android.calendar.EVENT_REMINDER_APP");
        intent.setClass(context, AlertReceiver.class);
        if (quietUpdate) {
            alarmType = 1;
        } else {
            Uri.Builder builder = CalendarContract.CalendarAlerts.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, alarmTime);
            intent.setData(builder.build());
        }
        intent.putExtra("alarmTime", alarmTime);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 134217728);
        manager.set(alarmType, alarmTime, pi);
    }

    static String formatTimeLocation(Context context, long startMillis, boolean allDay, String location) {
        int flags;
        String tz = Utils.getTimeZone(context, null);
        Time time = new Time(tz);
        time.setToNow();
        int today = Time.getJulianDay(time.toMillis(false), time.gmtoff);
        time.set(startMillis);
        int eventDay = Time.getJulianDay(time.toMillis(false), allDay ? 0L : time.gmtoff);
        if (!allDay) {
            flags = 524288 | 1;
            if (DateFormat.is24HourFormat(context)) {
                flags |= 128;
            }
        } else {
            flags = 524288 | 8192;
        }
        if (eventDay < today || eventDay > today + 1) {
            flags |= 16;
        }
        StringBuilder sb = new StringBuilder(Utils.formatDateRange(context, startMillis, startMillis, flags));
        if (!allDay && tz != Time.getCurrentTimezone()) {
            time.set(startMillis);
            boolean isDST = time.isDst != 0;
            sb.append(" ").append(TimeZone.getTimeZone(tz).getDisplayName(isDST, 0, Locale.getDefault()));
        }
        if (eventDay == today + 1) {
            sb.append(", ");
            sb.append(context.getString(R.string.tomorrow));
        }
        if (location != null) {
            String loc = location.trim();
            if (!TextUtils.isEmpty(loc)) {
                sb.append(", ");
                sb.append(loc);
            }
        }
        return sb.toString();
    }

    public static ContentValues makeContentValues(long eventId, long begin, long end, long alarmTime, int minutes) {
        ContentValues values = new ContentValues();
        values.put("event_id", Long.valueOf(eventId));
        values.put("begin", Long.valueOf(begin));
        values.put("end", Long.valueOf(end));
        values.put("alarmTime", Long.valueOf(alarmTime));
        long currentTime = System.currentTimeMillis();
        values.put("creationTime", Long.valueOf(currentTime));
        values.put("receivedTime", (Integer) 0);
        values.put("notifyTime", (Integer) 0);
        values.put("state", (Integer) 0);
        values.put("minutes", Integer.valueOf(minutes));
        return values;
    }

    public static Intent buildEventViewIntent(Context c, long eventId, long begin, long end) {
        Intent i = new Intent("android.intent.action.VIEW");
        Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
        builder.appendEncodedPath("events/" + eventId);
        i.setData(builder.build());
        i.setClass(c, EventInfoActivity.class);
        i.putExtra("beginTime", begin);
        i.putExtra("endTime", end);
        return i;
    }

    public static SharedPreferences getFiredAlertsTable(Context context) {
        return context.getSharedPreferences("calendar_alerts", 0);
    }

    private static String getFiredAlertsKey(long eventId, long beginTime, long alarmTime) {
        return "preference_alert_" + eventId + "_" + beginTime + "_" + alarmTime;
    }

    static boolean hasAlertFiredInSharedPrefs(Context context, long eventId, long beginTime, long alarmTime) {
        SharedPreferences prefs = getFiredAlertsTable(context);
        return prefs.contains(getFiredAlertsKey(eventId, beginTime, alarmTime));
    }

    static void setAlertFiredInSharedPrefs(Context context, long eventId, long beginTime, long alarmTime) {
        SharedPreferences prefs = getFiredAlertsTable(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(getFiredAlertsKey(eventId, beginTime, alarmTime), alarmTime);
        editor.apply();
    }

    static void flushOldAlertsFromInternalStorage(Context context) {
        if (BYPASS_DB) {
            SharedPreferences prefs = getFiredAlertsTable(context);
            long nowTime = System.currentTimeMillis();
            long lastFlushTimeMs = prefs.getLong("preference_flushTimeMs", 0L);
            if (nowTime - lastFlushTimeMs > 86400000) {
                Log.d("AlertUtils", "Flushing old alerts from shared prefs table");
                SharedPreferences.Editor editor = prefs.edit();
                Time timeObj = new Time();
                for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key.startsWith("preference_alert_")) {
                        if (value instanceof Long) {
                            long alertTime = ((Long) value).longValue();
                            if (nowTime - alertTime >= 86400000) {
                                editor.remove(key);
                                int ageInDays = getIntervalInDays(alertTime, nowTime, timeObj);
                                Log.d("AlertUtils", "SharedPrefs key " + key + ": removed (" + ageInDays + " days old)");
                            } else {
                                int ageInDays2 = getIntervalInDays(alertTime, nowTime, timeObj);
                                Log.d("AlertUtils", "SharedPrefs key " + key + ": keep (" + ageInDays2 + " days old)");
                            }
                        } else {
                            Log.e("AlertUtils", "SharedPrefs key " + key + " did not have Long value: " + value);
                        }
                    }
                }
                editor.putLong("preference_flushTimeMs", nowTime);
                editor.apply();
            }
        }
    }

    private static int getIntervalInDays(long startMillis, long endMillis, Time timeObj) {
        timeObj.set(startMillis);
        int startDay = Time.getJulianDay(startMillis, timeObj.gmtoff);
        timeObj.set(endMillis);
        return Time.getJulianDay(endMillis, timeObj.gmtoff) - startDay;
    }
}
