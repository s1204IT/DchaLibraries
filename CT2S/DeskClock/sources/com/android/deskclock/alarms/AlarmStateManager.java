package com.android.deskclock.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.android.deskclock.AlarmAlertWakeLock;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.AsyncHandler;
import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import java.util.Calendar;
import java.util.List;

public final class AlarmStateManager extends BroadcastReceiver {
    public static int getGlobalIntentId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("intent.extra.alarm.global.id", -1);
    }

    public static void updateGlobalIntentId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int globalId = prefs.getInt("intent.extra.alarm.global.id", -1) + 1;
        prefs.edit().putInt("intent.extra.alarm.global.id", globalId).commit();
    }

    public static void updateNextAlarm(Context context) {
        AlarmInstance nextAlarm = null;
        ContentResolver cr = context.getContentResolver();
        for (AlarmInstance instance : AlarmInstance.getInstances(cr, "alarm_state<5", new String[0])) {
            if (nextAlarm == null || instance.getAlarmTime().before(nextAlarm.getAlarmTime())) {
                nextAlarm = instance;
            }
        }
        AlarmNotifications.registerNextAlarmWithAlarmManager(context, nextAlarm);
    }

    private static void updateParentAlarm(Context context, AlarmInstance instance) {
        ContentResolver cr = context.getContentResolver();
        Alarm alarm = Alarm.getAlarm(cr, instance.mAlarmId.longValue());
        if (alarm == null) {
            LogUtils.e("Parent has been deleted with instance: " + instance.toString(), new Object[0]);
            return;
        }
        if (!alarm.daysOfWeek.isRepeating()) {
            if (alarm.deleteAfterUse) {
                LogUtils.i("Deleting parent alarm: " + alarm.id, new Object[0]);
                Alarm.deleteAlarm(cr, alarm.id);
                return;
            } else {
                LogUtils.i("Disabling parent alarm: " + alarm.id, new Object[0]);
                alarm.enabled = false;
                Alarm.updateAlarm(cr, alarm);
                return;
            }
        }
        Calendar currentTime = Calendar.getInstance();
        Calendar alarmTime = instance.getAlarmTime();
        if (currentTime.after(alarmTime)) {
            alarmTime = currentTime;
        }
        AlarmInstance nextRepeatedInstance = alarm.createInstanceAfter(alarmTime);
        LogUtils.i("Creating new instance for repeating alarm " + alarm.id + " at " + AlarmUtils.getFormattedTime(context, nextRepeatedInstance.getAlarmTime()), new Object[0]);
        AlarmInstance.addInstance(cr, nextRepeatedInstance);
        registerInstance(context, nextRepeatedInstance, true);
    }

    public static Intent createStateChangeIntent(Context context, String tag, AlarmInstance instance, Integer state) {
        Intent intent = AlarmInstance.createIntent(context, AlarmStateManager.class, instance.mId);
        intent.setAction("change_state");
        intent.addCategory(tag);
        intent.putExtra("intent.extra.alarm.global.id", getGlobalIntentId(context));
        if (state != null) {
            intent.putExtra("intent.extra.alarm.state", state.intValue());
        }
        return intent;
    }

    private static void scheduleInstanceStateChange(Context context, Calendar time, AlarmInstance instance, int newState) {
        long timeInMillis = time.getTimeInMillis();
        LogUtils.v("Scheduling state change " + newState + " to instance " + instance.mId + " at " + AlarmUtils.getFormattedTime(context, time) + " (" + timeInMillis + ")", new Object[0]);
        Intent stateChangeIntent = createStateChangeIntent(context, "ALARM_MANAGER", instance, Integer.valueOf(newState));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(), stateChangeIntent, 134217728);
        AlarmManager am = (AlarmManager) context.getSystemService("alarm");
        if (Utils.isKitKatOrLater()) {
            am.setExact(5, timeInMillis, pendingIntent);
        } else {
            am.set(0, timeInMillis, pendingIntent);
        }
    }

    private static void cancelScheduledInstance(Context context, AlarmInstance instance) {
        LogUtils.v("Canceling instance " + instance.mId + " timers", new Object[0]);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(), createStateChangeIntent(context, "ALARM_MANAGER", instance, null), 134217728);
        AlarmManager am = (AlarmManager) context.getSystemService("alarm");
        am.cancel(pendingIntent);
    }

    public static void setSilentState(Context context, AlarmInstance instance) {
        LogUtils.v("Setting silent state to instance " + instance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = 0;
        AlarmInstance.updateInstance(contentResolver, instance);
        AlarmNotifications.clearNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getLowNotificationTime(), instance, 1);
    }

    public static void setLowNotificationState(Context context, AlarmInstance instance) {
        LogUtils.v("Setting low notification state to instance " + instance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = 1;
        AlarmInstance.updateInstance(contentResolver, instance);
        AlarmNotifications.showLowPriorityNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getHighNotificationTime(), instance, 3);
    }

    public static void setHideNotificationState(Context context, AlarmInstance instance) {
        LogUtils.v("Setting hide notification state to instance " + instance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = 2;
        AlarmInstance.updateInstance(contentResolver, instance);
        AlarmNotifications.clearNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getHighNotificationTime(), instance, 3);
    }

    public static void setHighNotificationState(Context context, AlarmInstance instance) {
        LogUtils.v("Setting high notification state to instance " + instance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = 3;
        AlarmInstance.updateInstance(contentResolver, instance);
        AlarmNotifications.showHighPriorityNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getAlarmTime(), instance, 5);
    }

    public static void setFiredState(Context context, AlarmInstance instance) {
        LogUtils.v("Setting fire state to instance " + instance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = 5;
        AlarmInstance.updateInstance(contentResolver, instance);
        AlarmService.startAlarm(context, instance);
        Calendar timeout = instance.getTimeout(context);
        if (timeout != null) {
            scheduleInstanceStateChange(context, timeout, instance, 6);
        }
        updateNextAlarm(context);
    }

    public static void setSnoozeState(Context context, AlarmInstance instance, boolean showToast) {
        AlarmService.stopAlarm(context, instance);
        String snoozeMinutesStr = PreferenceManager.getDefaultSharedPreferences(context).getString("snooze_duration", "10");
        int snoozeMinutes = Integer.parseInt(snoozeMinutesStr);
        Calendar newAlarmTime = Calendar.getInstance();
        newAlarmTime.add(12, snoozeMinutes);
        LogUtils.v("Setting snoozed state to instance " + instance.mId + " for " + AlarmUtils.getFormattedTime(context, newAlarmTime), new Object[0]);
        instance.setAlarmTime(newAlarmTime);
        instance.mAlarmState = 4;
        AlarmInstance.updateInstance(context.getContentResolver(), instance);
        AlarmNotifications.showSnoozeNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getAlarmTime(), instance, 5);
        if (showToast) {
            String displayTime = String.format(context.getResources().getQuantityText(R.plurals.alarm_alert_snooze_set, snoozeMinutes).toString(), Integer.valueOf(snoozeMinutes));
            Toast.makeText(context, displayTime, 1).show();
        }
        updateNextAlarm(context);
    }

    public static int getSnoozedMinutes(Context context) {
        String snoozeMinutesStr = PreferenceManager.getDefaultSharedPreferences(context).getString("snooze_duration", "10");
        return Integer.parseInt(snoozeMinutesStr);
    }

    public static void setMissedState(Context context, AlarmInstance instance) {
        LogUtils.v("Setting missed state to instance " + instance.mId, new Object[0]);
        AlarmService.stopAlarm(context, instance);
        if (instance.mAlarmId != null) {
            updateParentAlarm(context, instance);
        }
        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = 6;
        AlarmInstance.updateInstance(contentResolver, instance);
        AlarmNotifications.showMissedNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getMissedTimeToLive(), instance, 7);
        updateNextAlarm(context);
    }

    public static void setDismissState(Context context, AlarmInstance instance) {
        LogUtils.v("Setting dismissed state to instance " + instance.mId, new Object[0]);
        unregisterInstance(context, instance);
        if (instance.mAlarmId != null) {
            updateParentAlarm(context, instance);
        }
        AlarmInstance.deleteInstance(context.getContentResolver(), instance.mId);
        updateNextAlarm(context);
    }

    public static void unregisterInstance(Context context, AlarmInstance instance) {
        AlarmService.stopAlarm(context, instance);
        AlarmNotifications.clearNotification(context, instance);
        cancelScheduledInstance(context, instance);
    }

    public static void registerInstance(Context context, AlarmInstance instance, boolean updateNextAlarm) {
        Calendar currentTime = Calendar.getInstance();
        Calendar alarmTime = instance.getAlarmTime();
        Calendar timeoutTime = instance.getTimeout(context);
        Calendar lowNotificationTime = instance.getLowNotificationTime();
        Calendar highNotificationTime = instance.getHighNotificationTime();
        Calendar missedTTL = instance.getMissedTimeToLive();
        if (instance.mAlarmState == 7) {
            LogUtils.e("Alarm Instance is dismissed, but never deleted", new Object[0]);
            setDismissState(context, instance);
            return;
        }
        if (instance.mAlarmState == 5) {
            boolean hasTimeout = timeoutTime != null && currentTime.after(timeoutTime);
            if (!hasTimeout) {
                setFiredState(context, instance);
                return;
            }
        } else if (instance.mAlarmState == 6 && currentTime.before(alarmTime)) {
            if (instance.mAlarmId == null) {
                setDismissState(context, instance);
                return;
            }
            ContentResolver cr = context.getContentResolver();
            Alarm alarm = Alarm.getAlarm(cr, instance.mAlarmId.longValue());
            alarm.enabled = true;
            Alarm.updateAlarm(cr, alarm);
        }
        if (currentTime.after(missedTTL)) {
            setDismissState(context, instance);
        } else if (currentTime.after(alarmTime)) {
            Calendar alarmBuffer = Calendar.getInstance();
            alarmBuffer.setTime(alarmTime.getTime());
            alarmBuffer.add(13, 15);
            if (currentTime.before(alarmBuffer)) {
                setFiredState(context, instance);
            } else {
                setMissedState(context, instance);
            }
        } else if (instance.mAlarmState == 4) {
            AlarmNotifications.showSnoozeNotification(context, instance);
            scheduleInstanceStateChange(context, instance.getAlarmTime(), instance, 5);
        } else if (currentTime.after(highNotificationTime)) {
            setHighNotificationState(context, instance);
        } else if (currentTime.after(lowNotificationTime)) {
            if (instance.mAlarmState == 2) {
                setHideNotificationState(context, instance);
            } else {
                setLowNotificationState(context, instance);
            }
        } else {
            setSilentState(context, instance);
        }
        if (updateNextAlarm) {
            updateNextAlarm(context);
        }
    }

    public static void deleteAllInstances(Context context, long alarmId) {
        ContentResolver cr = context.getContentResolver();
        List<AlarmInstance> instances = AlarmInstance.getInstancesByAlarmId(cr, alarmId);
        for (AlarmInstance instance : instances) {
            unregisterInstance(context, instance);
            AlarmInstance.deleteInstance(context.getContentResolver(), instance.mId);
        }
        updateNextAlarm(context);
    }

    public static void fixAlarmInstances(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        for (AlarmInstance instance : AlarmInstance.getInstances(contentResolver, null, new String[0])) {
            registerInstance(context, instance, false);
        }
        updateNextAlarm(context);
    }

    public void setAlarmState(Context context, AlarmInstance instance, int state) {
        switch (state) {
            case 0:
                setSilentState(context, instance);
                break;
            case 1:
                setLowNotificationState(context, instance);
                break;
            case 2:
                setHideNotificationState(context, instance);
                break;
            case 3:
                setHighNotificationState(context, instance);
                break;
            case 4:
                setSnoozeState(context, instance, true);
                break;
            case 5:
                setFiredState(context, instance);
                break;
            case 6:
                setMissedState(context, instance);
                break;
            case 7:
                setDismissState(context, instance);
                break;
            default:
                LogUtils.e("Trying to change to unknown alarm state: " + state, new Object[0]);
                break;
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (!"indicator".equals(intent.getAction())) {
            final BroadcastReceiver.PendingResult result = goAsync();
            final PowerManager.WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
            wl.acquire();
            AsyncHandler.post(new Runnable() {
                @Override
                public void run() {
                    AlarmStateManager.this.handleIntent(context, intent);
                    result.finish();
                    wl.release();
                }
            });
        }
    }

    private void handleIntent(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtils.v("AlarmStateManager received intent " + intent, new Object[0]);
        if ("change_state".equals(action)) {
            Uri uri = intent.getData();
            AlarmInstance instance = AlarmInstance.getInstance(context.getContentResolver(), AlarmInstance.getId(uri));
            if (instance == null) {
                LogUtils.e("Can not change state for unknown instance: " + uri, new Object[0]);
                return;
            }
            int globalId = getGlobalIntentId(context);
            int intentId = intent.getIntExtra("intent.extra.alarm.global.id", -1);
            int alarmState = intent.getIntExtra("intent.extra.alarm.state", -1);
            if (intentId != globalId) {
                LogUtils.i("IntentId: " + intentId + " GlobalId: " + globalId + " AlarmState: " + alarmState, new Object[0]);
                if (!intent.hasCategory("DISMISS_TAG") && !intent.hasCategory("SNOOZE_TAG")) {
                    LogUtils.i("Ignoring old Intent", new Object[0]);
                    return;
                }
            }
            if (alarmState >= 0) {
                setAlarmState(context, instance, alarmState);
                return;
            } else {
                registerInstance(context, instance, true);
                return;
            }
        }
        if ("show_and_dismiss_alarm".equals(action)) {
            AlarmInstance instance2 = AlarmInstance.getInstance(context.getContentResolver(), AlarmInstance.getId(intent.getData()));
            long alarmId = instance2.mAlarmId == null ? -1L : instance2.mAlarmId.longValue();
            Intent viewAlarmIntent = Alarm.createIntent(context, DeskClock.class, alarmId);
            viewAlarmIntent.putExtra("deskclock.select.tab", 0);
            viewAlarmIntent.putExtra("deskclock.scroll.to.alarm", alarmId);
            viewAlarmIntent.addFlags(268435456);
            context.startActivity(viewAlarmIntent);
            setDismissState(context, instance2);
        }
    }

    public static Intent createIndicatorIntent(Context context) {
        return new Intent(context, (Class<?>) AlarmStateManager.class).setAction("indicator");
    }
}
