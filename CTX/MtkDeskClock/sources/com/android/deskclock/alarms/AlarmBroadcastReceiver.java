package com.android.deskclock.alarms;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import com.android.deskclock.LogUtils;

public class AlarmBroadcastReceiver extends BroadcastReceiver {
    private static final String ACTION_ENCRPTION_TYPE_CHANGED = "com.mediatek.intent.extra.ACTION_ENCRYPTION_TYPE_CHANGED";
    private static final String ACTION_PACKAGE_DATA_CLEARED = "com.mediatek.intent.action.SETTINGS_PACKAGE_DATA_CLEARED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        LogUtils.d("AlarmBroadcastReceiver action:", intent.getAction());
        if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
            return;
        }
        if (ACTION_ENCRPTION_TYPE_CHANGED.equals(intent.getAction())) {
            if (1 == PowerOffAlarm.getPasswordType()) {
                AlarmStateManager.setPoweroffAlarm(context, AlarmStateManager.getNextFiringAlarm(context));
                return;
            } else {
                ((AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM)).cancelPoweroffAlarm(context.getPackageName());
                return;
            }
        }
        if (!ACTION_PACKAGE_DATA_CLEARED.equals(intent.getAction())) {
            return;
        }
        String stringExtra = intent.getStringExtra("packageName");
        LogUtils.v("AlarmBroadcastReceiver recevied pkgName = " + stringExtra, new Object[0]);
        if (stringExtra == null || !stringExtra.equals(context.getPackageName())) {
            return;
        }
        ((AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM)).cancelPoweroffAlarm(context.getPackageName());
        AlarmNotifications.registerNextAlarmWithAlarmManager(context, null);
    }
}
