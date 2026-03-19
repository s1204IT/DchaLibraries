package com.android.providers.calendar;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Slog;

public class CalendarProviderBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, final Intent intent) {
        String action = intent.getAction();
        if (!"com.android.providers.calendar.intent.CalendarProvider2".equals(action) && !"android.intent.action.EVENT_REMINDER".equals(action)) {
            Log.e("CalendarProvider2", "Received invalid intent: " + intent);
            setResultCode(0);
            return;
        }
        if (Log.isLoggable("CalendarProvider2", 3)) {
            Log.d("CalendarProvider2", "Received intent: " + intent);
        }
        ContentProvider contentProviderCoerceToLocalContentProvider = ContentProvider.coerceToLocalContentProvider(context.getContentResolver().acquireProvider("com.android.calendar"));
        if (!(contentProviderCoerceToLocalContentProvider instanceof CalendarProvider2)) {
            Slog.wtf("CalendarProvider2", "CalendarProvider2 not found in CalendarProviderBroadcastReceiver.");
            return;
        }
        final CalendarProvider2 calendarProvider2 = (CalendarProvider2) contentProviderCoerceToLocalContentProvider;
        final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
        new Thread(new Runnable() {
            @Override
            public final void run() {
                CalendarProviderBroadcastReceiver.lambda$onReceive$0(intent, calendarProvider2, pendingResultGoAsync);
            }
        }).start();
    }

    static void lambda$onReceive$0(Intent intent, CalendarProvider2 calendarProvider2, BroadcastReceiver.PendingResult pendingResult) {
        calendarProvider2.getOrCreateCalendarAlarmManager().runScheduleNextAlarm(intent.getBooleanExtra("removeAlarms", false), calendarProvider2);
        if (Log.isLoggable("CalendarProvider2", 3)) {
            Log.d("CalendarProvider2", "Next alarm set.");
        }
        pendingResult.setResultCode(-1);
        pendingResult.finish();
    }
}
