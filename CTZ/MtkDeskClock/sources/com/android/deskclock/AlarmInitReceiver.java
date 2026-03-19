package com.android.deskclock;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.controller.Controller;
import com.android.deskclock.data.DataModel;

public class AlarmInitReceiver extends BroadcastReceiver {

    @SuppressLint({"InlinedApi"})
    private static final String ACTION_BOOT_COMPLETED;

    static {
        ACTION_BOOT_COMPLETED = Utils.isNOrLater() ? "android.intent.action.LOCKED_BOOT_COMPLETED" : "android.intent.action.BOOT_COMPLETED";
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        LogUtils.i("AlarmInitReceiver " + action, new Object[0]);
        final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
        final PowerManager.WakeLock wakeLockCreatePartialWakeLock = AlarmAlertWakeLock.createPartialWakeLock(context);
        wakeLockCreatePartialWakeLock.acquire();
        DataModel.getDataModel().updateGlobalIntentId();
        if (ACTION_BOOT_COMPLETED.equals(action)) {
            DataModel.getDataModel().updateAfterReboot();
        } else if ("android.intent.action.TIME_SET".equals(action)) {
            DataModel.getDataModel().updateAfterTimeSet();
        }
        if ("android.intent.action.BOOT_COMPLETED".equals(action) || "android.intent.action.LOCALE_CHANGED".equals(action)) {
            Controller.getController().updateShortcuts();
        }
        if ("android.intent.action.MY_PACKAGE_REPLACED".equals(action)) {
            DataModel.getDataModel().updateAllNotifications();
            Controller.getController().updateShortcuts();
            NotificationChannelManager.getInstance().initChannels(context);
        }
        AsyncHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!DeskClockBackupAgent.processRestoredData(context)) {
                        AlarmStateManager.fixAlarmInstances(context);
                    }
                    pendingResultGoAsync.finish();
                    wakeLockCreatePartialWakeLock.release();
                    LogUtils.v("AlarmInitReceiver finished", new Object[0]);
                } catch (Throwable th) {
                    pendingResultGoAsync.finish();
                    wakeLockCreatePartialWakeLock.release();
                    LogUtils.v("AlarmInitReceiver finished", new Object[0]);
                    throw th;
                }
            }
        });
    }
}
