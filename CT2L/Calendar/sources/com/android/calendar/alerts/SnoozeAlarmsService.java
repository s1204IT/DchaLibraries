package com.android.calendar.alerts;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.provider.CalendarContract;

public class SnoozeAlarmsService extends IntentService {
    private static final String[] PROJECTION = {"state"};

    public SnoozeAlarmsService() {
        super("SnoozeAlarmsService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onHandleIntent(Intent intent) {
        long eventId = intent.getLongExtra("eventid", -1L);
        long eventStart = intent.getLongExtra("eventstart", -1L);
        long eventEnd = intent.getLongExtra("eventend", -1L);
        int notificationId = intent.getIntExtra("notificationid", 0);
        if (eventId != -1) {
            ContentResolver resolver = getContentResolver();
            if (notificationId != 0) {
                NotificationManager nm = (NotificationManager) getSystemService("notification");
                nm.cancel(notificationId);
            }
            Uri uri = CalendarContract.CalendarAlerts.CONTENT_URI;
            String selection = "state=1 AND event_id=" + eventId;
            ContentValues dismissValues = new ContentValues();
            dismissValues.put(PROJECTION[0], (Integer) 2);
            resolver.update(uri, dismissValues, selection, null);
            long alarmTime = System.currentTimeMillis() + 300000;
            ContentValues values = AlertUtils.makeContentValues(eventId, eventStart, eventEnd, alarmTime, 0);
            resolver.insert(uri, values);
            AlertUtils.scheduleAlarm(this, AlertUtils.createAlarmManager(this), alarmTime);
        }
        AlertService.updateAlertNotification(this);
        stopSelf();
    }
}
