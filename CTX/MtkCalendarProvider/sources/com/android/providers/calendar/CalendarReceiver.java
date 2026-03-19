package com.android.providers.calendar;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarReceiver extends BroadcastReceiver {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (this.mWakeLock == null) {
            this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "CalendarReceiver_Provider");
            this.mWakeLock.setReferenceCounted(true);
        }
        this.mWakeLock.acquire();
        final String action = intent.getAction();
        final ContentResolver contentResolver = context.getContentResolver();
        final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (action.equals("com.android.providers.calendar.SCHEDULE_ALARM")) {
                        contentResolver.update(CalendarAlarmManager.SCHEDULE_ALARM_URI, null, null, null);
                    } else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                        CalendarReceiver.this.removeScheduledAlarms(contentResolver);
                    }
                } finally {
                    pendingResultGoAsync.finish();
                    CalendarReceiver.this.mWakeLock.release();
                }
            }
        });
    }

    private void removeScheduledAlarms(ContentResolver contentResolver) {
        if (Log.isLoggable("CalendarReceiver", 3)) {
            Log.d("CalendarReceiver", "Removing scheduled alarms");
        }
        contentResolver.update(CalendarAlarmManager.SCHEDULE_ALARM_REMOVE_URI, null, null, null);
    }
}
