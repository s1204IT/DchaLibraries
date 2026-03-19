package com.android.deskclock;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import com.android.deskclock.LogUtils;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.controller.Controller;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.ClockContract;
import com.android.deskclock.settings.SettingsActivity;
import com.android.deskclock.timer.TimerFragment;
import com.android.deskclock.timer.TimerService;
import com.android.deskclock.uidata.UiDataModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HandleApiCalls extends Activity {
    static final String ACTION_SHOW_TIMERS = "android.intent.action.SHOW_TIMERS";
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("HandleApiCalls");
    private Context mAppContext;

    @Override
    protected void onCreate(Bundle bundle) {
        Intent intent;
        String action;
        super.onCreate(bundle);
        this.mAppContext = getApplicationContext();
        try {
            try {
                intent = getIntent();
                action = intent == null ? null : intent.getAction();
            } catch (Exception e) {
                LOGGER.wtf(e);
            }
            if (action == null) {
                return;
            }
            byte b = 0;
            LOGGER.i("onCreate: " + intent, new Object[0]);
            switch (action.hashCode()) {
                case -805737507:
                    b = !action.equals("android.intent.action.SNOOZE_ALARM") ? (byte) -1 : (byte) 5;
                    break;
                case 128174967:
                    if (action.equals("android.intent.action.DISMISS_ALARM")) {
                        b = 4;
                        break;
                    }
                    break;
                case 252113103:
                    if (action.equals("android.intent.action.SET_ALARM")) {
                        break;
                    }
                    break;
                case 269581763:
                    if (action.equals("android.intent.action.SET_TIMER")) {
                        b = 2;
                        break;
                    }
                    break;
                case 1112785375:
                    if (action.equals("android.intent.action.SHOW_ALARMS")) {
                        b = 1;
                        break;
                    }
                    break;
                case 1654313835:
                    if (action.equals(ACTION_SHOW_TIMERS)) {
                        b = 3;
                        break;
                    }
                    break;
                default:
                    break;
            }
            switch (b) {
                case 0:
                    handleSetAlarm(intent);
                    break;
                case 1:
                    handleShowAlarms(intent);
                    break;
                case 2:
                    handleSetTimer(intent);
                    break;
                case 3:
                    handleShowTimers(intent);
                    break;
                case 4:
                    handleDismissAlarm(intent);
                    break;
                case 5:
                    handleSnoozeAlarm(intent);
                    break;
            }
        } finally {
            finish();
        }
    }

    private void handleDismissAlarm(Intent intent) {
        UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.ALARMS);
        startActivity(new Intent(this.mAppContext, (Class<?>) DeskClock.class));
        new DismissAlarmAsync(this.mAppContext, intent, this).execute(new Void[0]);
    }

    public static void dismissAlarm(Alarm alarm, Activity activity) {
        Context applicationContext = activity.getApplicationContext();
        AlarmInstance nextUpcomingInstanceByAlarmId = AlarmInstance.getNextUpcomingInstanceByAlarmId(applicationContext.getContentResolver(), alarm.id);
        if (nextUpcomingInstanceByAlarmId == null) {
            Controller.getController().notifyVoiceFailure(activity, applicationContext.getString(R.string.no_alarm_scheduled_for_this_time));
            LOGGER.i("No alarm instance to dismiss", new Object[0]);
            return;
        }
        dismissAlarmInstance(nextUpcomingInstanceByAlarmId, activity);
    }

    public static void dismissAlarmInstance(AlarmInstance alarmInstance, Activity activity) throws Exception {
        Utils.enforceNotMainLooper();
        Context applicationContext = activity.getApplicationContext();
        String str = DateFormat.getTimeFormat(applicationContext).format(alarmInstance.getAlarmTime().getTime());
        if (alarmInstance.mAlarmState == 5 || alarmInstance.mAlarmState == 4) {
            AlarmStateManager.deleteInstanceAndUpdateParent(applicationContext, alarmInstance);
        } else if (Utils.isAlarmWithin24Hours(alarmInstance)) {
            AlarmStateManager.setPreDismissState(applicationContext, alarmInstance);
        } else {
            Controller.getController().notifyVoiceFailure(activity, applicationContext.getString(R.string.alarm_cant_be_dismissed_still_more_than_24_hours_away, str));
            LOGGER.i("Can't dismiss alarm more than 24 hours in advance", new Object[0]);
        }
        Controller.getController().notifyVoiceSuccess(activity, applicationContext.getString(R.string.alarm_is_dismissed, str));
        LOGGER.i("Alarm dismissed: " + alarmInstance, new Object[0]);
        Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_intent);
    }

    private static class DismissAlarmAsync extends AsyncTask<Void, Void, Void> {
        private final Activity mActivity;
        private final Context mContext;
        private final Intent mIntent;

        public DismissAlarmAsync(Context context, Intent intent, Activity activity) {
            this.mContext = context;
            this.mIntent = intent;
            this.mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            List<Alarm> enabledAlarms = getEnabledAlarms(this.mContext);
            if (enabledAlarms.isEmpty()) {
                Controller.getController().notifyVoiceFailure(this.mActivity, this.mContext.getString(R.string.no_scheduled_alarms));
                HandleApiCalls.LOGGER.i("No scheduled alarms", new Object[0]);
                return null;
            }
            Iterator<Alarm> it = enabledAlarms.iterator();
            while (it.hasNext()) {
                AlarmInstance nextUpcomingInstanceByAlarmId = AlarmInstance.getNextUpcomingInstanceByAlarmId(contentResolver, it.next().id);
                if (nextUpcomingInstanceByAlarmId == null || nextUpcomingInstanceByAlarmId.mAlarmState > 5) {
                    it.remove();
                }
            }
            String stringExtra = this.mIntent.getStringExtra("android.intent.extra.alarm.SEARCH_MODE");
            if (stringExtra == null && enabledAlarms.size() > 1) {
                this.mContext.startActivity(new Intent(this.mContext, (Class<?>) AlarmSelectionActivity.class).setFlags(268435456).putExtra(AlarmSelectionActivity.EXTRA_ACTION, 0).putExtra(AlarmSelectionActivity.EXTRA_ALARMS, (Parcelable[]) enabledAlarms.toArray(new Parcelable[enabledAlarms.size()])));
                Controller.getController().notifyVoiceSuccess(this.mActivity, this.mContext.getString(R.string.pick_alarm_to_dismiss));
                return null;
            }
            FetchMatchingAlarmsAction fetchMatchingAlarmsAction = new FetchMatchingAlarmsAction(this.mContext, enabledAlarms, this.mIntent, this.mActivity);
            fetchMatchingAlarmsAction.run();
            List<Alarm> matchingAlarms = fetchMatchingAlarmsAction.getMatchingAlarms();
            if (!"android.all".equals(stringExtra) && matchingAlarms.size() > 1) {
                this.mContext.startActivity(new Intent(this.mContext, (Class<?>) AlarmSelectionActivity.class).setFlags(268435456).putExtra(AlarmSelectionActivity.EXTRA_ACTION, 0).putExtra(AlarmSelectionActivity.EXTRA_ALARMS, (Parcelable[]) matchingAlarms.toArray(new Parcelable[matchingAlarms.size()])));
                Controller.getController().notifyVoiceSuccess(this.mActivity, this.mContext.getString(R.string.pick_alarm_to_dismiss));
                return null;
            }
            for (Alarm alarm : matchingAlarms) {
                HandleApiCalls.dismissAlarm(alarm, this.mActivity);
                HandleApiCalls.LOGGER.i("Alarm dismissed: " + alarm, new Object[0]);
            }
            return null;
        }

        private static List<Alarm> getEnabledAlarms(Context context) {
            return Alarm.getAlarms(context.getContentResolver(), String.format("%s=?", ClockContract.AlarmsColumns.ENABLED), SettingsActivity.VOLUME_BEHAVIOR_SNOOZE);
        }
    }

    private void handleSnoozeAlarm(Intent intent) {
        new SnoozeAlarmAsync(intent, this).execute(new Void[0]);
    }

    private static class SnoozeAlarmAsync extends AsyncTask<Void, Void, Void> {
        private final Activity mActivity;
        private final Context mContext;
        private final Intent mIntent;

        public SnoozeAlarmAsync(Intent intent, Activity activity) {
            this.mContext = activity.getApplicationContext();
            this.mIntent = intent;
            this.mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            List<AlarmInstance> instancesByState = AlarmInstance.getInstancesByState(this.mContext.getContentResolver(), 5);
            if (instancesByState.isEmpty()) {
                Controller.getController().notifyVoiceFailure(this.mActivity, this.mContext.getString(R.string.no_firing_alarms));
                HandleApiCalls.LOGGER.i("No firing alarms", new Object[0]);
                return null;
            }
            Iterator<AlarmInstance> it = instancesByState.iterator();
            while (it.hasNext()) {
                HandleApiCalls.snoozeAlarm(it.next(), this.mContext, this.mActivity);
            }
            return null;
        }
    }

    static void snoozeAlarm(AlarmInstance alarmInstance, Context context, Activity activity) {
        Utils.enforceNotMainLooper();
        String string = context.getString(R.string.alarm_is_snoozed, DateFormat.getTimeFormat(context).format(alarmInstance.getAlarmTime().getTime()));
        AlarmStateManager.setSnoozeState(context, alarmInstance, true);
        Controller.getController().notifyVoiceSuccess(activity, string);
        LOGGER.i("Alarm snoozed: " + alarmInstance, new Object[0]);
        Events.sendAlarmEvent(R.string.action_snooze, R.string.label_intent);
    }

    private void handleSetAlarm(Intent intent) throws Exception {
        int i;
        Alarm alarm;
        if (intent.hasExtra("android.intent.extra.alarm.HOUR")) {
            int intExtra = intent.getIntExtra("android.intent.extra.alarm.HOUR", -1);
            if (intExtra < 0 || intExtra > 23) {
                Controller.getController().notifyVoiceFailure(this, getString(R.string.invalid_time, new Object[]{Integer.valueOf(intExtra), Integer.valueOf(intent.getIntExtra("android.intent.extra.alarm.MINUTES", 0)), " "}));
                LOGGER.i("Illegal hour: " + intExtra, new Object[0]);
                return;
            }
            i = intExtra;
        } else {
            i = -1;
        }
        int intExtra2 = intent.getIntExtra("android.intent.extra.alarm.MINUTES", 0);
        if (intExtra2 < 0 || intExtra2 > 59) {
            Controller.getController().notifyVoiceFailure(this, getString(R.string.invalid_time, new Object[]{Integer.valueOf(i), Integer.valueOf(intExtra2), " "}));
            LOGGER.i("Illegal minute: " + intExtra2, new Object[0]);
            return;
        }
        boolean booleanExtra = intent.getBooleanExtra("android.intent.extra.alarm.SKIP_UI", false);
        ContentResolver contentResolver = getContentResolver();
        if (i == -1) {
            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.ALARMS);
            startActivity(Alarm.createIntent(this, DeskClock.class, -1L).addFlags(268435456).putExtra(AlarmClockFragment.ALARM_CREATE_NEW_INTENT_EXTRA, true));
            Controller.getController().notifyVoiceFailure(this, getString(R.string.invalid_time, new Object[]{Integer.valueOf(i), Integer.valueOf(intExtra2), " "}));
            LOGGER.i("Missing alarm time; opening UI", new Object[0]);
            return;
        }
        StringBuilder sb = new StringBuilder();
        ArrayList arrayList = new ArrayList();
        setSelectionFromIntent(intent, i, intExtra2, sb, arrayList);
        List<Alarm> alarms = Alarm.getAlarms(contentResolver, sb.toString(), (String[]) arrayList.toArray(new String[arrayList.size()]));
        if (!alarms.isEmpty()) {
            alarm = alarms.get(0);
            alarm.enabled = true;
            Alarm.updateAlarm(contentResolver, alarm);
            AlarmStateManager.deleteAllInstances(this, alarm.id);
            Events.sendAlarmEvent(R.string.action_update, R.string.label_intent);
            LOGGER.i("Updated alarm: " + alarm, new Object[0]);
        } else {
            Alarm alarm2 = new Alarm();
            updateAlarmFromIntent(alarm2, intent);
            alarm2.deleteAfterUse = !alarm2.daysOfWeek.isRepeating() && booleanExtra;
            Alarm.addAlarm(contentResolver, alarm2);
            Events.sendAlarmEvent(R.string.action_create, R.string.label_intent);
            LOGGER.i("Created new alarm: " + alarm2, new Object[0]);
            alarm = alarm2;
        }
        AlarmInstance alarmInstanceCreateInstanceAfter = alarm.createInstanceAfter(DataModel.getDataModel().getCalendar());
        setupInstance(alarmInstanceCreateInstanceAfter, booleanExtra);
        Controller.getController().notifyVoiceSuccess(this, getString(R.string.alarm_is_set, new Object[]{DateFormat.getTimeFormat(this).format(alarmInstanceCreateInstanceAfter.getAlarmTime().getTime())}));
    }

    private void handleShowAlarms(Intent intent) {
        Events.sendAlarmEvent(R.string.action_show, R.string.label_intent);
        UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.ALARMS);
        startActivity(new Intent(this, (Class<?>) DeskClock.class));
    }

    private void handleShowTimers(Intent intent) {
        Events.sendTimerEvent(R.string.action_show, R.string.label_intent);
        Intent intent2 = new Intent(this, (Class<?>) DeskClock.class);
        List<Timer> timers = DataModel.getDataModel().getTimers();
        if (!timers.isEmpty()) {
            intent2.putExtra(TimerService.EXTRA_TIMER_ID, timers.get(timers.size() - 1).getId());
        }
        UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.TIMERS);
        startActivity(intent2);
    }

    private void handleSetTimer(Intent intent) {
        if (!intent.hasExtra("android.intent.extra.alarm.LENGTH")) {
            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.TIMERS);
            startActivity(TimerFragment.createTimerSetupIntent(this));
            LOGGER.i("Showing timer setup", new Object[0]);
            return;
        }
        long intExtra = ((long) intent.getIntExtra("android.intent.extra.alarm.LENGTH", 0)) * 1000;
        if (intExtra < 1000) {
            Controller.getController().notifyVoiceFailure(this, getString(R.string.invalid_timer_length));
            LOGGER.i("Invalid timer length requested: " + intExtra, new Object[0]);
            return;
        }
        String labelFromIntent = getLabelFromIntent(intent, com.google.android.flexbox.BuildConfig.FLAVOR);
        boolean booleanExtra = intent.getBooleanExtra("android.intent.extra.alarm.SKIP_UI", false);
        Timer timerAddTimer = null;
        Iterator<Timer> it = DataModel.getDataModel().getTimers().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Timer next = it.next();
            if (next.isReset() && next.getLength() == intExtra && TextUtils.equals(labelFromIntent, next.getLabel())) {
                timerAddTimer = next;
                break;
            }
        }
        if (timerAddTimer == null) {
            timerAddTimer = DataModel.getDataModel().addTimer(intExtra, labelFromIntent, booleanExtra);
            Events.sendTimerEvent(R.string.action_create, R.string.label_intent);
        }
        DataModel.getDataModel().startTimer(timerAddTimer);
        Events.sendTimerEvent(R.string.action_start, R.string.label_intent);
        Controller.getController().notifyVoiceSuccess(this, getString(R.string.timer_created));
        if (!booleanExtra) {
            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.TIMERS);
            startActivity(new Intent(this, (Class<?>) DeskClock.class).putExtra(TimerService.EXTRA_TIMER_ID, timerAddTimer.getId()));
        }
    }

    private void setupInstance(AlarmInstance alarmInstance, boolean z) throws Exception {
        AlarmInstance alarmInstanceAddInstance = AlarmInstance.addInstance(getContentResolver(), alarmInstance);
        AlarmStateManager.registerInstance(this, alarmInstanceAddInstance, true);
        AlarmUtils.popAlarmSetToast(this, alarmInstanceAddInstance.getAlarmTime().getTimeInMillis());
        if (!z) {
            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.ALARMS);
            startActivity(Alarm.createIntent(this, DeskClock.class, alarmInstanceAddInstance.mAlarmId.longValue()).putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, alarmInstanceAddInstance.mAlarmId).addFlags(268435456));
        }
    }

    private static void updateAlarmFromIntent(Alarm alarm, Intent intent) {
        alarm.enabled = true;
        alarm.hour = intent.getIntExtra("android.intent.extra.alarm.HOUR", alarm.hour);
        alarm.minutes = intent.getIntExtra("android.intent.extra.alarm.MINUTES", alarm.minutes);
        alarm.vibrate = intent.getBooleanExtra("android.intent.extra.alarm.VIBRATE", alarm.vibrate);
        alarm.alert = getAlertFromIntent(intent, alarm.alert);
        alarm.label = getLabelFromIntent(intent, alarm.label);
        alarm.daysOfWeek = getDaysFromIntent(intent, alarm.daysOfWeek);
    }

    private static String getLabelFromIntent(Intent intent, String str) {
        String string = intent.getExtras().getString("android.intent.extra.alarm.MESSAGE", str);
        return string == null ? com.google.android.flexbox.BuildConfig.FLAVOR : string;
    }

    private static Weekdays getDaysFromIntent(Intent intent, Weekdays weekdays) {
        if (!intent.hasExtra("android.intent.extra.alarm.DAYS")) {
            return weekdays;
        }
        ArrayList<Integer> integerArrayListExtra = intent.getIntegerArrayListExtra("android.intent.extra.alarm.DAYS");
        if (integerArrayListExtra != null) {
            int[] iArr = new int[integerArrayListExtra.size()];
            for (int i = 0; i < integerArrayListExtra.size(); i++) {
                iArr[i] = integerArrayListExtra.get(i).intValue();
            }
            return Weekdays.fromCalendarDays(iArr);
        }
        int[] intArrayExtra = intent.getIntArrayExtra("android.intent.extra.alarm.DAYS");
        if (intArrayExtra != null) {
            return Weekdays.fromCalendarDays(intArrayExtra);
        }
        return weekdays;
    }

    private static Uri getAlertFromIntent(Intent intent, Uri uri) {
        String stringExtra = intent.getStringExtra("android.intent.extra.alarm.RINGTONE");
        if (stringExtra == null) {
            return uri;
        }
        if ("silent".equals(stringExtra) || stringExtra.isEmpty()) {
            return Alarm.NO_RINGTONE_URI;
        }
        return Uri.parse(stringExtra);
    }

    private void setSelectionFromIntent(Intent intent, int i, int i2, StringBuilder sb, List<String> list) {
        sb.append("hour");
        sb.append("=?");
        list.add(String.valueOf(i));
        sb.append(" AND ");
        sb.append("minutes");
        sb.append("=?");
        list.add(String.valueOf(i2));
        if (intent.hasExtra("android.intent.extra.alarm.MESSAGE")) {
            sb.append(" AND ");
            sb.append(ClockContract.AlarmSettingColumns.LABEL);
            sb.append("=?");
            list.add(getLabelFromIntent(intent, com.google.android.flexbox.BuildConfig.FLAVOR));
        }
        sb.append(" AND ");
        sb.append(ClockContract.AlarmsColumns.DAYS_OF_WEEK);
        sb.append("=?");
        list.add(String.valueOf(getDaysFromIntent(intent, Weekdays.NONE).getBits()));
        if (intent.hasExtra("android.intent.extra.alarm.VIBRATE")) {
            sb.append(" AND ");
            sb.append(ClockContract.AlarmSettingColumns.VIBRATE);
            sb.append("=?");
            list.add(intent.getBooleanExtra("android.intent.extra.alarm.VIBRATE", false) ? SettingsActivity.VOLUME_BEHAVIOR_SNOOZE : SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);
        }
        if (intent.hasExtra("android.intent.extra.alarm.RINGTONE")) {
            sb.append(" AND ");
            sb.append(ClockContract.AlarmSettingColumns.RINGTONE);
            sb.append("=?");
            list.add(getAlertFromIntent(intent, DataModel.getDataModel().getDefaultAlarmRingtoneUri()).toString());
        }
    }
}
