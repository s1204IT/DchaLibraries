package com.android.calendar.alerts;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.util.Log;

public class InitAlarmsService extends IntentService {
    private static final Uri SCHEDULE_ALARM_REMOVE_URI = Uri.withAppendedPath(CalendarContract.CONTENT_URI, "schedule_alarms_remove");

    public InitAlarmsService() {
        super("InitAlarmsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SystemClock.sleep(30000L);
        Log.d("InitAlarmsService", "Clearing and rescheduling alarms.");
        try {
            getContentResolver().update(SCHEDULE_ALARM_REMOVE_URI, new ContentValues(), null, null);
        } catch (IllegalArgumentException e) {
            Log.e("InitAlarmsService", "update failed: " + e.toString());
        }
    }
}
