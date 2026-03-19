package com.android.deskclock.alarms;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.format.DateFormat;
import android.widget.Toast;
import com.android.deskclock.AlarmAlertWakeLock;
import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.AsyncHandler;
import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.google.android.flexbox.BuildConfig;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public final class AlarmStateManager extends BroadcastReceiver {
    public static final String ACTION_ALARM_CHANGED = "com.android.deskclock.ALARM_CHANGED";
    public static final String ALARM_DELETE_TAG = "DELETE_TAG";
    public static final String ALARM_DISMISS_TAG = "DISMISS_TAG";
    public static final int ALARM_FIRE_BUFFER = 15;
    private static final String ALARM_GLOBAL_ID_EXTRA = "intent.extra.alarm.global.id";
    private static final String ALARM_MANAGER_TAG = "ALARM_MANAGER";
    public static final String ALARM_SNOOZE_TAG = "SNOOZE_TAG";
    public static final String ALARM_STATE_EXTRA = "intent.extra.alarm.state";
    public static final String CHANGE_STATE_ACTION = "com.android.deskclock.change_state";
    public static final String FROM_NOTIFICATION_EXTRA = "intent.extra.from.notification";
    private static final String INDICATOR_ACTION = "indicator";
    public static final int POWER_OFF_WAKE_UP = 7;
    public static final String SHOW_AND_DISMISS_ALARM_ACTION = "show_and_dismiss_alarm";
    private static CurrentTimeFactory sCurrentTimeFactory;
    private static StateChangeScheduler sStateChangeScheduler = new AlarmManagerStateChangeScheduler();

    interface CurrentTimeFactory {
        Calendar getCurrentTime();
    }

    interface StateChangeScheduler {
        void cancelScheduledInstanceStateChange(Context context, AlarmInstance alarmInstance);

        void scheduleInstanceStateChange(Context context, Calendar calendar, AlarmInstance alarmInstance, int i);
    }

    private static Calendar getCurrentTime() {
        if (sCurrentTimeFactory == null) {
            return DataModel.getDataModel().getCalendar();
        }
        return sCurrentTimeFactory.getCurrentTime();
    }

    static void setCurrentTimeFactory(CurrentTimeFactory currentTimeFactory) {
        sCurrentTimeFactory = currentTimeFactory;
    }

    static void setStateChangeScheduler(StateChangeScheduler stateChangeScheduler) {
        if (stateChangeScheduler == null) {
            stateChangeScheduler = new AlarmManagerStateChangeScheduler();
        }
        sStateChangeScheduler = stateChangeScheduler;
    }

    private static void updateNextAlarm(Context context) {
        AlarmInstance nextFiringAlarm = getNextFiringAlarm(context);
        if (Utils.isPreL()) {
            updateNextAlarmInSystemSettings(context, nextFiringAlarm);
        } else {
            updateNextAlarmInAlarmManager(context, nextFiringAlarm);
        }
        AlarmNotifications.registerNextAlarmWithAlarmManager(context, nextFiringAlarm);
        setPoweroffAlarm(context, nextFiringAlarm);
    }

    public static AlarmInstance getNextFiringAlarm(Context context) {
        AlarmInstance alarmInstance = null;
        for (AlarmInstance alarmInstance2 : AlarmInstance.getInstances(context.getContentResolver(), "alarm_state<5", new String[0])) {
            if (alarmInstance == null || alarmInstance2.getAlarmTime().before(alarmInstance.getAlarmTime())) {
                alarmInstance = alarmInstance2;
            }
        }
        return alarmInstance;
    }

    @TargetApi(19)
    private static void updateNextAlarmInSystemSettings(Context context, AlarmInstance alarmInstance) {
        String formattedTime = BuildConfig.FLAVOR;
        if (alarmInstance != null) {
            formattedTime = AlarmUtils.getFormattedTime(context, alarmInstance.getAlarmTime());
        }
        try {
            Settings.System.putString(context.getContentResolver(), "next_alarm_formatted", formattedTime);
            LogUtils.i("Updated next alarm time to: '" + formattedTime + '\'', new Object[0]);
            context.sendBroadcast(new Intent(ACTION_ALARM_CHANGED));
        } catch (SecurityException e) {
            LogUtils.e("Unable to update next alarm to: '" + formattedTime + '\'', e);
        }
    }

    @TargetApi(21)
    private static void updateNextAlarmInAlarmManager(Context context, AlarmInstance alarmInstance) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, createIndicatorIntent(context), alarmInstance == null ? 536870912 : 0);
        if (alarmInstance == null) {
            if (broadcast != null) {
                LogUtils.i("Canceling upcoming AlarmClockInfo", new Object[0]);
                alarmManager.cancel(broadcast);
                return;
            }
            return;
        }
        LogUtils.i("Setting upcoming AlarmClockInfo for alarm: " + alarmInstance.mId, new Object[0]);
        Utils.updateNextAlarm(alarmManager, new AlarmManager.AlarmClockInfo(alarmInstance.getAlarmTime().getTimeInMillis(), PendingIntent.getActivity(context, alarmInstance.hashCode(), AlarmNotifications.createViewAlarmIntent(context, alarmInstance), 134217728)), broadcast);
    }

    private static void updateParentAlarm(Context context, AlarmInstance alarmInstance) throws Exception {
        ContentResolver contentResolver = context.getContentResolver();
        Alarm alarm = Alarm.getAlarm(contentResolver, alarmInstance.mAlarmId.longValue());
        if (alarm == null) {
            LogUtils.e("Parent has been deleted with instance: " + alarmInstance.toString(), new Object[0]);
            return;
        }
        if (!alarm.daysOfWeek.isRepeating()) {
            if (alarm.deleteAfterUse) {
                LogUtils.i("Deleting parent alarm: " + alarm.id, new Object[0]);
                Alarm.deleteAlarm(contentResolver, alarm.id);
                return;
            }
            LogUtils.i("Disabling parent alarm: " + alarm.id, new Object[0]);
            alarm.enabled = false;
            Alarm.updateAlarm(contentResolver, alarm);
            return;
        }
        AlarmInstance alarmInstanceCreateInstanceAfter = alarm.createInstanceAfter(getCurrentTime());
        if (alarmInstance.mAlarmState > 5 && alarmInstanceCreateInstanceAfter.getAlarmTime().equals(alarmInstance.getAlarmTime())) {
            alarmInstanceCreateInstanceAfter = alarm.createInstanceAfter(alarmInstance.getAlarmTime());
        }
        LogUtils.i("Creating new instance for repeating alarm " + alarm.id + " at " + AlarmUtils.getFormattedTime(context, alarmInstanceCreateInstanceAfter.getAlarmTime()), new Object[0]);
        AlarmInstance.addInstance(contentResolver, alarmInstanceCreateInstanceAfter);
        registerInstance(context, alarmInstanceCreateInstanceAfter, false);
    }

    public static Intent createStateChangeIntent(Context context, String str, AlarmInstance alarmInstance, Integer num) {
        Intent intentCreateIntent = AlarmInstance.createIntent(context, AlarmService.class, alarmInstance.mId);
        intentCreateIntent.setAction(CHANGE_STATE_ACTION);
        intentCreateIntent.addCategory(str);
        intentCreateIntent.putExtra(ALARM_GLOBAL_ID_EXTRA, DataModel.getDataModel().getGlobalIntentId());
        if (num != null) {
            intentCreateIntent.putExtra(ALARM_STATE_EXTRA, num.intValue());
        }
        return intentCreateIntent;
    }

    private static void scheduleInstanceStateChange(Context context, Calendar calendar, AlarmInstance alarmInstance, int i) {
        sStateChangeScheduler.scheduleInstanceStateChange(context, calendar, alarmInstance, i);
    }

    private static void cancelScheduledInstanceStateChange(Context context, AlarmInstance alarmInstance) {
        sStateChangeScheduler.cancelScheduledInstanceStateChange(context, alarmInstance);
    }

    public static void setSilentState(Context context, AlarmInstance alarmInstance) {
        LogUtils.i("Setting silent state to instance " + alarmInstance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        alarmInstance.mAlarmState = 0;
        AlarmInstance.updateInstance(contentResolver, alarmInstance);
        AlarmNotifications.clearNotification(context, alarmInstance);
        scheduleInstanceStateChange(context, alarmInstance.getLowNotificationTime(), alarmInstance, 1);
    }

    public static void setLowNotificationState(Context context, AlarmInstance alarmInstance) {
        LogUtils.i("Setting low notification state to instance " + alarmInstance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        alarmInstance.mAlarmState = 1;
        AlarmInstance.updateInstance(contentResolver, alarmInstance);
        AlarmNotifications.showLowPriorityNotification(context, alarmInstance);
        scheduleInstanceStateChange(context, alarmInstance.getHighNotificationTime(), alarmInstance, 3);
    }

    public static void setHideNotificationState(Context context, AlarmInstance alarmInstance) {
        LogUtils.i("Setting hide notification state to instance " + alarmInstance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        alarmInstance.mAlarmState = 2;
        AlarmInstance.updateInstance(contentResolver, alarmInstance);
        AlarmNotifications.clearNotification(context, alarmInstance);
        scheduleInstanceStateChange(context, alarmInstance.getHighNotificationTime(), alarmInstance, 3);
    }

    public static void setHighNotificationState(Context context, AlarmInstance alarmInstance) {
        LogUtils.i("Setting high notification state to instance " + alarmInstance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        alarmInstance.mAlarmState = 3;
        AlarmInstance.updateInstance(contentResolver, alarmInstance);
        AlarmNotifications.showHighPriorityNotification(context, alarmInstance);
        scheduleInstanceStateChange(context, alarmInstance.getAlarmTime(), alarmInstance, 5);
    }

    public static void setFiredState(Context context, AlarmInstance alarmInstance) {
        LogUtils.i("Setting fire state to instance " + alarmInstance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        alarmInstance.mAlarmState = 5;
        AlarmInstance.updateInstance(contentResolver, alarmInstance);
        if (alarmInstance.mAlarmId != null) {
            AlarmInstance.deleteOtherInstances(context, contentResolver, alarmInstance.mAlarmId.longValue(), alarmInstance.mId);
        }
        Events.sendAlarmEvent(R.string.action_fire, 0);
        Calendar timeout = alarmInstance.getTimeout();
        if (timeout != null) {
            scheduleInstanceStateChange(context, timeout, alarmInstance, 6);
        }
        updateNextAlarm(context);
    }

    public static void setSnoozeState(final Context context, AlarmInstance alarmInstance, boolean z) {
        AlarmService.stopAlarm(context, alarmInstance);
        final int snoozeLength = DataModel.getDataModel().getSnoozeLength();
        Calendar calendar = Calendar.getInstance();
        calendar.add(12, snoozeLength);
        LogUtils.i("Setting snoozed state to instance " + alarmInstance.mId + " for " + AlarmUtils.getFormattedTime(context, calendar), new Object[0]);
        alarmInstance.setAlarmTime(calendar);
        alarmInstance.mAlarmState = 4;
        AlarmInstance.updateInstance(context.getContentResolver(), alarmInstance);
        AlarmNotifications.showSnoozeNotification(context, alarmInstance);
        scheduleInstanceStateChange(context, alarmInstance.getAlarmTime(), alarmInstance, 5);
        if (z) {
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, String.format(context.getResources().getQuantityText(R.plurals.alarm_alert_snooze_set, snoozeLength).toString(), Integer.valueOf(snoozeLength)), 1).show();
                }
            });
        }
        updateNextAlarm(context);
    }

    public static void setMissedState(Context context, AlarmInstance alarmInstance) throws Exception {
        LogUtils.i("Setting missed state to instance " + alarmInstance.mId, new Object[0]);
        AlarmService.stopAlarm(context, alarmInstance);
        if (alarmInstance.mAlarmId != null) {
            updateParentAlarm(context, alarmInstance);
        }
        ContentResolver contentResolver = context.getContentResolver();
        alarmInstance.mAlarmState = 6;
        AlarmInstance.updateInstance(contentResolver, alarmInstance);
        AlarmNotifications.showMissedNotification(context, alarmInstance);
        scheduleInstanceStateChange(context, alarmInstance.getMissedTimeToLive(), alarmInstance, 7);
        updateNextAlarm(context);
    }

    public static void setPreDismissState(Context context, AlarmInstance alarmInstance) throws Exception {
        LogUtils.i("Setting predismissed state to instance " + alarmInstance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        alarmInstance.mAlarmState = 8;
        AlarmInstance.updateInstance(contentResolver, alarmInstance);
        AlarmNotifications.clearNotification(context, alarmInstance);
        scheduleInstanceStateChange(context, alarmInstance.getAlarmTime(), alarmInstance, 7);
        if (alarmInstance.mAlarmId != null) {
            updateParentAlarm(context, alarmInstance);
        }
        updateNextAlarm(context);
    }

    public static void setDismissState(Context context, AlarmInstance alarmInstance) {
        LogUtils.i("Setting dismissed state to instance " + alarmInstance.mId, new Object[0]);
        alarmInstance.mAlarmState = 7;
        AlarmInstance.updateInstance(context.getContentResolver(), alarmInstance);
    }

    public static void deleteInstanceAndUpdateParent(Context context, AlarmInstance alarmInstance) throws Exception {
        LogUtils.i("Deleting instance " + alarmInstance.mId + " and updating parent alarm.", new Object[0]);
        unregisterInstance(context, alarmInstance);
        if (alarmInstance.mAlarmId != null) {
            updateParentAlarm(context, alarmInstance);
        }
        AlarmInstance.deleteInstance(context.getContentResolver(), alarmInstance.mId);
        updateNextAlarm(context);
    }

    public static void unregisterInstance(Context context, AlarmInstance alarmInstance) {
        LogUtils.i("Unregistering instance " + alarmInstance.mId, new Object[0]);
        AlarmService.stopAlarm(context, alarmInstance);
        AlarmNotifications.clearNotification(context, alarmInstance);
        cancelScheduledInstanceStateChange(context, alarmInstance);
        setDismissState(context, alarmInstance);
    }

    public static void registerInstance(Context context, AlarmInstance alarmInstance, boolean z) throws Exception {
        boolean z2 = false;
        LogUtils.i("Registering instance: " + alarmInstance.mId, new Object[0]);
        ContentResolver contentResolver = context.getContentResolver();
        Alarm alarm = Alarm.getAlarm(contentResolver, alarmInstance.mAlarmId.longValue());
        Calendar currentTime = getCurrentTime();
        Calendar alarmTime = alarmInstance.getAlarmTime();
        Calendar timeout = alarmInstance.getTimeout();
        Calendar lowNotificationTime = alarmInstance.getLowNotificationTime();
        Calendar highNotificationTime = alarmInstance.getHighNotificationTime();
        Calendar missedTimeToLive = alarmInstance.getMissedTimeToLive();
        if (alarmInstance.mAlarmState == 7) {
            LogUtils.e("Alarm Instance is dismissed, but never deleted", new Object[0]);
            deleteInstanceAndUpdateParent(context, alarmInstance);
            return;
        }
        if (alarmInstance.mAlarmState == 5) {
            if (timeout != null && currentTime.after(timeout)) {
                z2 = true;
            }
            if (!z2) {
                setFiredState(context, alarmInstance);
                return;
            }
        } else if (alarmInstance.mAlarmState == 6) {
            if (currentTime.before(alarmTime)) {
                if (alarmInstance.mAlarmId == null) {
                    LogUtils.i("Cannot restore missed instance for one-time alarm", new Object[0]);
                    deleteInstanceAndUpdateParent(context, alarmInstance);
                    return;
                } else {
                    alarm.enabled = true;
                    Alarm.updateAlarm(contentResolver, alarm);
                }
            }
        } else if (alarmInstance.mAlarmState == 8) {
            if (currentTime.before(alarmTime)) {
                setPreDismissState(context, alarmInstance);
                return;
            } else {
                deleteInstanceAndUpdateParent(context, alarmInstance);
                return;
            }
        }
        if (currentTime.after(missedTimeToLive)) {
            deleteInstanceAndUpdateParent(context, alarmInstance);
        } else if (currentTime.after(alarmTime)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(alarmTime.getTime());
            calendar.add(13, 15);
            if (currentTime.before(calendar)) {
                setFiredState(context, alarmInstance);
            } else {
                setMissedState(context, alarmInstance);
            }
        } else if (alarmInstance.mAlarmState == 4) {
            AlarmNotifications.showSnoozeNotification(context, alarmInstance);
            scheduleInstanceStateChange(context, alarmInstance.getAlarmTime(), alarmInstance, 5);
        } else if (currentTime.after(highNotificationTime)) {
            setHighNotificationState(context, alarmInstance);
        } else if (currentTime.after(lowNotificationTime)) {
            if (alarmInstance.mAlarmState == 2) {
                setHideNotificationState(context, alarmInstance);
            } else {
                setLowNotificationState(context, alarmInstance);
            }
        } else {
            setSilentState(context, alarmInstance);
        }
        if (z) {
            updateNextAlarm(context);
        }
    }

    public static void deleteAllInstances(Context context, long j) {
        LogUtils.i("Deleting all instances of alarm: " + j, new Object[0]);
        for (AlarmInstance alarmInstance : AlarmInstance.getInstancesByAlarmId(context.getContentResolver(), j)) {
            unregisterInstance(context, alarmInstance);
            AlarmInstance.deleteInstance(context.getContentResolver(), alarmInstance.mId);
        }
        updateNextAlarm(context);
    }

    public static void deleteNonSnoozeInstances(Context context, long j) {
        LogUtils.i("Deleting all non-snooze instances of alarm: " + j, new Object[0]);
        for (AlarmInstance alarmInstance : AlarmInstance.getInstancesByAlarmId(context.getContentResolver(), j)) {
            if (alarmInstance.mAlarmState != 4) {
                unregisterInstance(context, alarmInstance);
                AlarmInstance.deleteInstance(context.getContentResolver(), alarmInstance.mId);
            }
        }
        updateNextAlarm(context);
    }

    public static void fixAlarmInstances(Context context) throws Exception {
        LogUtils.i("Fixing alarm instances", new Object[0]);
        HashMap map = new HashMap();
        ContentResolver contentResolver = context.getContentResolver();
        Calendar currentTime = getCurrentTime();
        List<AlarmInstance> instances = AlarmInstance.getInstances(contentResolver, null, new String[0]);
        Collections.sort(instances, new Comparator<AlarmInstance>() {
            @Override
            public int compare(AlarmInstance alarmInstance, AlarmInstance alarmInstance2) {
                return alarmInstance2.getAlarmTime().compareTo(alarmInstance.getAlarmTime());
            }
        });
        for (AlarmInstance alarmInstance : instances) {
            Alarm alarm = Alarm.getAlarm(contentResolver, alarmInstance.mAlarmId.longValue());
            if (alarm == null) {
                unregisterInstance(context, alarmInstance);
                AlarmInstance.deleteInstance(contentResolver, alarmInstance.mId);
                LogUtils.e("Found instance without matching alarm; deleting instance %s", alarmInstance);
            } else {
                Calendar previousAlarmTime = alarm.getPreviousAlarmTime(alarmInstance.getAlarmTime());
                Calendar missedTimeToLive = alarmInstance.getMissedTimeToLive();
                if (currentTime.before(previousAlarmTime) || currentTime.after(missedTimeToLive)) {
                    LogUtils.i("A time change has caused an existing alarm scheduled to fire at %s to be replaced by a new alarm scheduled to fire at %s", DateFormat.format("MM/dd/yyyy hh:mm a", alarmInstance.getAlarmTime()), DateFormat.format("MM/dd/yyyy hh:mm a", alarm.getNextAlarmTime(currentTime)));
                    deleteInstanceAndUpdateParent(context, alarmInstance);
                } else if (map.get(alarmInstance.mAlarmId) == null) {
                    map.put(alarmInstance.mAlarmId, alarmInstance);
                    AlarmInstance fixedAlarmInstance = getFixedAlarmInstance(context, alarmInstance);
                    if (fixedAlarmInstance != null) {
                        registerInstance(context, fixedAlarmInstance, false);
                    }
                } else {
                    AlarmInstance.deleteInstance(contentResolver, alarmInstance.mId);
                }
            }
        }
        updateNextAlarm(context);
    }

    private static void setAlarmState(Context context, AlarmInstance alarmInstance, int i) throws Exception {
        if (alarmInstance == null) {
            LogUtils.e("Null alarm instance while setting state to %d", Integer.valueOf(i));
        }
        switch (i) {
            case 0:
                setSilentState(context, alarmInstance);
                break;
            case 1:
                setLowNotificationState(context, alarmInstance);
                break;
            case 2:
                setHideNotificationState(context, alarmInstance);
                break;
            case 3:
                setHighNotificationState(context, alarmInstance);
                break;
            case 4:
                setSnoozeState(context, alarmInstance, true);
                break;
            case 5:
                setFiredState(context, alarmInstance);
                break;
            case 6:
                setMissedState(context, alarmInstance);
                break;
            case 7:
                deleteInstanceAndUpdateParent(context, alarmInstance);
                break;
            case 8:
                setPreDismissState(context, alarmInstance);
                break;
            default:
                LogUtils.e("Trying to change to unknown alarm state: " + i, new Object[0]);
                break;
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (INDICATOR_ACTION.equals(intent.getAction())) {
            return;
        }
        final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
        final PowerManager.WakeLock wakeLockCreatePartialWakeLock = AlarmAlertWakeLock.createPartialWakeLock(context);
        wakeLockCreatePartialWakeLock.acquire();
        AsyncHandler.post(new Runnable() {
            @Override
            public void run() throws Exception {
                AlarmStateManager.handleIntent(context, intent);
                pendingResultGoAsync.finish();
                wakeLockCreatePartialWakeLock.release();
            }
        });
    }

    public static void handleIntent(Context context, Intent intent) throws Exception {
        String action = intent.getAction();
        LogUtils.v("AlarmStateManager received intent " + intent, new Object[0]);
        if (!CHANGE_STATE_ACTION.equals(action)) {
            if (SHOW_AND_DISMISS_ALARM_ACTION.equals(action)) {
                AlarmInstance alarmInstance = AlarmInstance.getInstance(context.getContentResolver(), AlarmInstance.getId(intent.getData()));
                if (alarmInstance == null) {
                    LogUtils.e("Null alarminstance for SHOW_AND_DISMISS", new Object[0]);
                    int intExtra = intent.getIntExtra("extra_notification_id", -1);
                    if (intExtra != -1) {
                        NotificationManagerCompat.from(context).cancel(intExtra);
                        return;
                    }
                    return;
                }
                long jLongValue = alarmInstance.mAlarmId == null ? -1L : alarmInstance.mAlarmId.longValue();
                context.startActivity(Alarm.createIntent(context, DeskClock.class, jLongValue).putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, jLongValue).addFlags(268435456));
                deleteInstanceAndUpdateParent(context, alarmInstance);
                return;
            }
            return;
        }
        Uri data = intent.getData();
        AlarmInstance alarmInstance2 = AlarmInstance.getInstance(context.getContentResolver(), AlarmInstance.getId(data));
        if (alarmInstance2 == null) {
            LogUtils.e("Can not change state for unknown instance: " + data, new Object[0]);
            return;
        }
        int globalIntentId = DataModel.getDataModel().getGlobalIntentId();
        int intExtra2 = intent.getIntExtra(ALARM_GLOBAL_ID_EXTRA, -1);
        int intExtra3 = intent.getIntExtra(ALARM_STATE_EXTRA, -1);
        if (intExtra2 != globalIntentId) {
            LogUtils.i("IntentId: " + intExtra2 + " GlobalId: " + globalIntentId + " AlarmState: " + intExtra3, new Object[0]);
            if (!intent.hasCategory(ALARM_DISMISS_TAG) && !intent.hasCategory(ALARM_SNOOZE_TAG)) {
                LogUtils.i("Ignoring old Intent", new Object[0]);
                return;
            }
        }
        if (intent.getBooleanExtra(FROM_NOTIFICATION_EXTRA, false)) {
            if (intent.hasCategory(ALARM_DISMISS_TAG)) {
                Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_notification);
            } else if (intent.hasCategory(ALARM_SNOOZE_TAG)) {
                Events.sendAlarmEvent(R.string.action_snooze, R.string.label_notification);
            }
        }
        if (intExtra3 >= 0) {
            setAlarmState(context, alarmInstance2, intExtra3);
        } else {
            registerInstance(context, alarmInstance2, true);
        }
    }

    public static Intent createIndicatorIntent(Context context) {
        return new Intent(context, (Class<?>) AlarmStateManager.class).setAction(INDICATOR_ACTION);
    }

    private static class AlarmManagerStateChangeScheduler implements StateChangeScheduler {
        private AlarmManagerStateChangeScheduler() {
        }

        @Override
        public void scheduleInstanceStateChange(Context context, Calendar calendar, AlarmInstance alarmInstance, int i) {
            long timeInMillis = calendar.getTimeInMillis();
            LogUtils.i("Scheduling state change %d to instance %d at %s (%d)", Integer.valueOf(i), Long.valueOf(alarmInstance.mId), AlarmUtils.getFormattedTime(context, calendar), Long.valueOf(timeInMillis));
            Intent intentCreateStateChangeIntent = AlarmStateManager.createStateChangeIntent(context, AlarmStateManager.ALARM_MANAGER_TAG, alarmInstance, Integer.valueOf(i));
            intentCreateStateChangeIntent.addFlags(268435456);
            PendingIntent service = PendingIntent.getService(context, alarmInstance.hashCode(), intentCreateStateChangeIntent, 134217728);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
            if (Utils.isMOrLater()) {
                alarmManager.setExactAndAllowWhileIdle(0, timeInMillis, service);
            } else {
                alarmManager.setExact(0, timeInMillis, service);
            }
            if (alarmInstance.mAlarmState == 8) {
                alarmManager.cancelPoweroffAlarm(context.getPackageName());
            }
        }

        @Override
        public void cancelScheduledInstanceStateChange(Context context, AlarmInstance alarmInstance) {
            LogUtils.v("Canceling instance " + alarmInstance.mId + " timers", new Object[0]);
            PendingIntent service = PendingIntent.getService(context, alarmInstance.hashCode(), AlarmStateManager.createStateChangeIntent(context, AlarmStateManager.ALARM_MANAGER_TAG, alarmInstance, null), 536870912);
            if (service != null) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
                alarmManager.cancel(service);
                alarmManager.cancelPoweroffAlarm(context.getPackageName());
                service.cancel();
            }
        }
    }

    private static AlarmInstance getFixedAlarmInstance(Context context, AlarmInstance alarmInstance) throws Exception {
        ContentResolver contentResolver = context.getContentResolver();
        Alarm alarm = Alarm.getAlarm(contentResolver, alarmInstance.mAlarmId.longValue());
        if (alarm == null) {
            LogUtils.e("getFixedAlarmInstance alarm has not been found with instance: " + alarmInstance.toString(), new Object[0]);
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        Calendar alarmTime = alarm.createInstanceAfter(calendar).getAlarmTime();
        Calendar alarmTime2 = alarmInstance.getAlarmTime();
        if (alarmTime.before(alarmTime2)) {
            int i = alarmTime.get(1);
            int i2 = alarmTime.get(2);
            int i3 = alarmTime.get(5);
            alarmTime2.set(1, i);
            alarmTime2.set(2, i2);
            alarmTime2.set(5, i3);
            while (alarmTime2.before(calendar)) {
                alarmTime2.add(5, 1);
            }
            alarmInstance.setAlarmTime(alarmTime2);
            AlarmInstance.updateInstance(contentResolver, alarmInstance);
        }
        return alarmInstance;
    }

    public static void setPoweroffAlarm(Context context, AlarmInstance alarmInstance) {
        if (alarmInstance != null && PowerOffAlarm.canEnablePowerOffAlarm()) {
            long timeInMillis = alarmInstance.getAlarmTime().getTimeInMillis();
            PendingIntent broadcast = PendingIntent.getBroadcast(context, alarmInstance.hashCode(), createStateChangeIntent(context, ALARM_MANAGER_TAG, alarmInstance, 5), 134217728);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
            LogUtils.v("Set for PowerOffAlarm alarmType = 8, time = " + AlarmUtils.getFormattedTime(context, alarmInstance.getAlarmTime()), new Object[0]);
            alarmManager.setExact(7, timeInMillis, broadcast);
        }
    }

    public static void resetServiceForAlarm(Context context, AlarmInstance alarmInstance) {
        LogUtils.v("Resetting service launch for 2nd alarm", new Object[0]);
        if (alarmInstance == null) {
            return;
        }
        long timeInMillis = alarmInstance.getAlarmTime().getTimeInMillis();
        Intent intentCreateStateChangeIntent = createStateChangeIntent(context, ALARM_MANAGER_TAG, alarmInstance, 5);
        intentCreateStateChangeIntent.addFlags(268435456);
        ((AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM)).setExact(0, timeInMillis, PendingIntent.getService(context, alarmInstance.hashCode(), intentCreateStateChangeIntent, 134217728));
    }
}
