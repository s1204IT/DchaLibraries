package com.android.calendar.alerts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import com.android.calendar.EventInfoActivity;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.MTKUtils;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AlertUtils {
    static boolean BYPASS_DB = true;

    public static AlarmManagerInterface createAlarmManager(Context context) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        return new AlarmManagerInterface() {
            @Override
            public void set(int i, long j, PendingIntent pendingIntent) {
                if (Utils.isKeyLimePieOrLater()) {
                    alarmManager.setExact(i, j, pendingIntent);
                } else {
                    alarmManager.set(i, j, pendingIntent);
                }
            }
        };
    }

    public static void scheduleAlarm(Context context, AlarmManagerInterface alarmManagerInterface, long j) {
        scheduleAlarmHelper(context, alarmManagerInterface, j, false);
    }

    static void scheduleNextNotificationRefresh(Context context, AlarmManagerInterface alarmManagerInterface, long j) {
        scheduleAlarmHelper(context, alarmManagerInterface, j, true);
    }

    private static void scheduleAlarmHelper(Context context, AlarmManagerInterface alarmManagerInterface, long j, boolean z) {
        int i;
        if (alarmManagerInterface == null) {
            alarmManagerInterface = createAlarmManager(context);
        }
        Intent intent = new Intent("com.android.calendar.EVENT_REMINDER_APP");
        intent.setClass(context, AlertReceiver.class);
        if (z) {
            i = 1;
        } else {
            Uri.Builder builderBuildUpon = CalendarContract.CalendarAlerts.CONTENT_URI.buildUpon();
            ContentUris.appendId(builderBuildUpon, j);
            intent.setData(builderBuildUpon.build());
            i = 0;
        }
        intent.putExtra("alarmTime", j);
        alarmManagerInterface.set(i, j, PendingIntent.getBroadcast(context, 0, intent, 134217728));
    }

    static String formatTimeLocation(Context context, long j, boolean z, String str) {
        int i;
        String timeZone = Utils.getTimeZone(context, null);
        Time time = new Time(timeZone);
        time.setToNow();
        int julianDay = Time.getJulianDay(time.toMillis(false), time.gmtoff);
        time.set(j);
        int julianDay2 = Time.getJulianDay(time.toMillis(false), z ? 0L : time.gmtoff);
        if (!z) {
            i = 98305;
            if (DateFormat.is24HourFormat(context)) {
                i = 98433;
            }
        } else {
            i = 106496;
        }
        if (julianDay2 < julianDay || julianDay2 > julianDay + 1) {
            i |= 16;
        }
        StringBuilder sb = new StringBuilder(Utils.formatDateRange(context, j, j, i));
        if (!z && timeZone != Time.getCurrentTimezone()) {
            time.set(j);
            boolean z2 = time.isDst != 0;
            sb.append(" ");
            sb.append(TimeZone.getTimeZone(timeZone).getDisplayName(z2, 0, Locale.getDefault()));
        }
        if (julianDay2 == julianDay + 1) {
            sb.append(", ");
            sb.append(context.getString(R.string.tomorrow));
        }
        if (str != null) {
            String strTrim = str.trim();
            if (!TextUtils.isEmpty(strTrim)) {
                sb.append(", ");
                sb.append(strTrim);
            }
        }
        return sb.toString();
    }

    public static ContentValues makeContentValues(long j, long j2, long j3, long j4, int i) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("event_id", Long.valueOf(j));
        contentValues.put("begin", Long.valueOf(j2));
        contentValues.put("end", Long.valueOf(j3));
        contentValues.put("alarmTime", Long.valueOf(j4));
        contentValues.put("creationTime", Long.valueOf(System.currentTimeMillis()));
        contentValues.put("receivedTime", (Integer) 0);
        contentValues.put("notifyTime", (Integer) 0);
        contentValues.put("state", (Integer) 0);
        contentValues.put("minutes", Integer.valueOf(i));
        return contentValues;
    }

    public static Intent buildEventViewIntent(Context context, long j, long j2, long j3) {
        Intent intent = new Intent("android.intent.action.VIEW");
        Uri.Builder builderBuildUpon = CalendarContract.CONTENT_URI.buildUpon();
        builderBuildUpon.appendEncodedPath("events/" + j);
        intent.setData(builderBuildUpon.build());
        intent.setClass(context, EventInfoActivity.class);
        intent.putExtra("beginTime", j2);
        intent.putExtra("endTime", j3);
        return intent;
    }

    public static SharedPreferences getFiredAlertsTable(Context context) {
        return context.getSharedPreferences("calendar_alerts", 0);
    }

    private static String getFiredAlertsKey(long j, long j2, long j3) {
        return "preference_alert_" + j + "_" + j2 + "_" + j3;
    }

    static boolean hasAlertFiredInSharedPrefs(Context context, long j, long j2, long j3) {
        return getFiredAlertsTable(context).contains(getFiredAlertsKey(j, j2, j3));
    }

    static void setAlertFiredInSharedPrefs(Context context, long j, long j2, long j3) {
        SharedPreferences.Editor editorEdit = getFiredAlertsTable(context).edit();
        editorEdit.putLong(getFiredAlertsKey(j, j2, j3), j3);
        editorEdit.apply();
    }

    static void flushOldAlertsFromInternalStorage(Context context) {
        if (BYPASS_DB) {
            SharedPreferences firedAlertsTable = getFiredAlertsTable(context);
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (jCurrentTimeMillis - firedAlertsTable.getLong("preference_flushTimeMs", 0L) > 86400000) {
                Log.d("AlertUtils", "Flushing old alerts from shared prefs table");
                SharedPreferences.Editor editorEdit = firedAlertsTable.edit();
                Time time = new Time();
                for (Map.Entry<String, ?> entry : firedAlertsTable.getAll().entrySet()) {
                    String key = entry.getKey();
                    ?? value = entry.getValue();
                    if (key.startsWith("preference_alert_")) {
                        if (value instanceof Long) {
                            long jLongValue = value.longValue();
                            if (jCurrentTimeMillis - jLongValue >= 86400000) {
                                editorEdit.remove(key);
                                Log.d("AlertUtils", "SharedPrefs key " + key + ": removed (" + getIntervalInDays(jLongValue, jCurrentTimeMillis, time) + " days old)");
                            } else {
                                Log.d("AlertUtils", "SharedPrefs key " + key + ": keep (" + getIntervalInDays(jLongValue, jCurrentTimeMillis, time) + " days old)");
                            }
                        } else {
                            Log.e("AlertUtils", "SharedPrefs key " + key + " did not have Long value: " + ((Object) value));
                        }
                    }
                }
                editorEdit.putLong("preference_flushTimeMs", jCurrentTimeMillis);
                editorEdit.apply();
            }
        }
    }

    private static int getIntervalInDays(long j, long j2, Time time) {
        time.set(j);
        int julianDay = Time.getJulianDay(j, time.gmtoff);
        time.set(j2);
        return Time.getJulianDay(j2, time.gmtoff) - julianDay;
    }

    public static void removeEventNotification(Context context, long j, long j2, long j3) {
        Integer num = AlertService.getEventIdToNotificationIdMap().get(Long.valueOf(j));
        Intent intent = new Intent();
        intent.setClass(context, DismissAlarmsService.class);
        intent.putExtra("eventid", j);
        intent.putExtra("eventstart", j2);
        intent.putExtra("eventend", j3);
        intent.putExtra("eventshowed", true);
        if (num != null) {
            intent.putExtra("notificationid", num.intValue());
        } else {
            intent.putExtra("notificationid", -1);
        }
        Uri.Builder builderBuildUpon = CalendarContract.Events.CONTENT_URI.buildUpon();
        ContentUris.appendId(builderBuildUpon, j);
        ContentUris.appendId(builderBuildUpon, j2);
        intent.setData(builderBuildUpon.build());
        intent.setAction("com.android.calendar.DELETE");
        context.startService(intent);
    }

    public static void postUnreadNumber(Context context) {
        int count;
        Cursor cursorQuery = context.getContentResolver().query(CalendarContract.CalendarAlerts.CONTENT_URI, null, "state=1 OR state=100", null, null);
        if (cursorQuery != null) {
            try {
                count = cursorQuery.getCount();
            } finally {
                cursorQuery.close();
            }
        } else {
            count = 0;
        }
        LogUtil.d("AlertUtils", "WriteUnreadReminders(unReadMsgNumber).");
        MTKUtils.writeUnreadReminders(context, count);
    }
}
