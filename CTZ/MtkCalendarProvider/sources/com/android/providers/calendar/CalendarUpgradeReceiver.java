package com.android.providers.calendar;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class CalendarUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            long jCurrentTimeMillis = System.currentTimeMillis();
            SharedPreferences sharedPreferences = context.getSharedPreferences("CalendarUpgradeReceiver", 0);
            if (sharedPreferences.getInt("db_version", 0) != 600) {
                sharedPreferences.edit().putInt("db_version", 600).commit();
                CalendarDatabaseHelper calendarDatabaseHelper = CalendarDatabaseHelper.getInstance(context);
                if (context.getDatabasePath(calendarDatabaseHelper.getDatabaseName()).exists()) {
                    Log.i("CalendarUpgradeReceiver", "Creating or opening calendar database");
                    calendarDatabaseHelper.getWritableDatabase();
                }
                EventLogTags.writeCalendarUpgradeReceiver(System.currentTimeMillis() - jCurrentTimeMillis);
            }
        } catch (Throwable th) {
            Log.wtf("CalendarUpgradeReceiver", "Error during upgrade attempt. Disabling receiver.", th);
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, getClass()), 2, 1);
        }
    }
}
