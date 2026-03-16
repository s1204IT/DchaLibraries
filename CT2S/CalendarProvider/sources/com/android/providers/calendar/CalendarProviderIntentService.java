package com.android.providers.calendar;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class CalendarProviderIntentService extends IntentService {
    public CalendarProviderIntentService() {
        super("CalendarProviderIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Log.isLoggable("CalendarProvider2", 3)) {
            Log.d("CalendarProvider2", "Received Intent: " + intent);
        }
        String action = intent.getAction();
        if (!"com.android.providers.calendar.intent.CalendarProvider2".equals(action)) {
            if (Log.isLoggable("CalendarProvider2", 3)) {
                Log.d("CalendarProvider2", "Invalid Intent action: " + action);
            }
        } else {
            CalendarProvider2 provider = CalendarProvider2.getInstance();
            boolean removeAlarms = intent.getBooleanExtra("removeAlarms", false);
            provider.getOrCreateCalendarAlarmManager().runScheduleNextAlarm(removeAlarms, provider);
            provider.getOrCreateCalendarAlarmManager().releaseScheduleNextAlarmWakeLock();
        }
    }
}
