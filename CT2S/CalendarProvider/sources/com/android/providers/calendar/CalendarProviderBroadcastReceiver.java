package com.android.providers.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CalendarProviderBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"com.android.providers.calendar.intent.CalendarProvider2".equals(intent.getAction())) {
            setResultCode(0);
            return;
        }
        CalendarProvider2 provider = CalendarProvider2.getInstance();
        provider.getOrCreateCalendarAlarmManager().acquireScheduleNextAlarmWakeLock();
        setResultCode(-1);
        intent.setClass(context, CalendarProviderIntentService.class);
        context.startService(intent);
    }
}
