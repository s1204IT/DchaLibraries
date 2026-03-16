package com.android.providers.calendar;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;

public class CalendarUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            long startTime = System.currentTimeMillis();
            SharedPreferences prefs = context.getSharedPreferences("CalendarUpgradeReceiver", 0);
            int prefVersion = prefs.getInt("db_version", 0);
            if (prefVersion != 600) {
                prefs.edit().putInt("db_version", 600).commit();
                CalendarDatabaseHelper helper = CalendarDatabaseHelper.getInstance(context);
                if (context.getDatabasePath(helper.getDatabaseName()).exists()) {
                    Log.i("CalendarUpgradeReceiver", "Creating or opening calendar database");
                    try {
                        ActivityManagerNative.getDefault().showBootMessage(context.getText(R.string.upgrade_msg), true);
                    } catch (RemoteException e) {
                    }
                    helper.getWritableDatabase();
                }
                helper.close();
                EventLogTags.writeCalendarUpgradeReceiver(System.currentTimeMillis() - startTime);
            }
        } catch (Throwable t) {
            Log.wtf("CalendarUpgradeReceiver", "Error during upgrade attempt. Disabling receiver.", t);
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, getClass()), 2, 1);
        }
    }
}
