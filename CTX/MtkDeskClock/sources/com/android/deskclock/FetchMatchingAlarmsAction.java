package com.android.deskclock;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.controller.Controller;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.ClockContract;
import com.android.deskclock.settings.SettingsActivity;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class FetchMatchingAlarmsAction implements Runnable {
    private final Activity mActivity;
    private final List<Alarm> mAlarms;
    private final Context mContext;
    private final Intent mIntent;
    private final List<Alarm> mMatchingAlarms = new ArrayList();

    public FetchMatchingAlarmsAction(Context context, List<Alarm> list, Intent intent, Activity activity) {
        this.mContext = context;
        this.mAlarms = list;
        this.mIntent = intent;
        this.mActivity = activity;
    }

    @Override
    public void run() {
        byte b;
        Utils.enforceNotMainLooper();
        String stringExtra = this.mIntent.getStringExtra("android.intent.extra.alarm.SEARCH_MODE");
        if (stringExtra == null) {
            this.mMatchingAlarms.addAll(this.mAlarms);
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int iHashCode = stringExtra.hashCode();
        if (iHashCode != -2087071051) {
            if (iHashCode != -1037092078) {
                if (iHashCode != -1036909844) {
                    b = (iHashCode == 936364450 && stringExtra.equals("android.all")) ? (byte) 2 : (byte) -1;
                } else if (stringExtra.equals("android.time")) {
                    b = 0;
                }
            } else if (stringExtra.equals("android.next")) {
                b = 1;
            }
        } else if (stringExtra.equals("android.label")) {
            b = 3;
        }
        switch (b) {
            case 0:
                int intExtra = this.mIntent.getIntExtra("android.intent.extra.alarm.HOUR", -1);
                int intExtra2 = this.mIntent.getIntExtra("android.intent.extra.alarm.MINUTES", 0);
                Boolean bool = (Boolean) this.mIntent.getExtras().get("android.intent.extra.alarm.IS_PM");
                if ((bool != null && intExtra > 12 && bool.booleanValue()) | (intExtra < 0 || intExtra > 23) | (intExtra2 < 0 || intExtra2 > 59)) {
                    String[] amPmStrings = new DateFormatSymbols().getAmPmStrings();
                    notifyFailureAndLog(this.mContext.getString(R.string.invalid_time, Integer.valueOf(intExtra), Integer.valueOf(intExtra2), bool == null ? com.google.android.flexbox.BuildConfig.FLAVOR : bool.booleanValue() ? amPmStrings[1] : amPmStrings[0]), this.mActivity);
                } else {
                    if (Boolean.TRUE.equals(bool) && intExtra < 12) {
                        intExtra += 12;
                    }
                    for (Alarm alarm : this.mAlarms) {
                        if (alarm.hour == intExtra && alarm.minutes == intExtra2) {
                            this.mMatchingAlarms.add(alarm);
                        }
                    }
                    if (this.mMatchingAlarms.isEmpty()) {
                        notifyFailureAndLog(this.mContext.getString(R.string.no_alarm_at, Integer.valueOf(intExtra), Integer.valueOf(intExtra2)), this.mActivity);
                    }
                }
                break;
            case 1:
                for (Alarm alarm2 : this.mAlarms) {
                    AlarmInstance nextUpcomingInstanceByAlarmId = AlarmInstance.getNextUpcomingInstanceByAlarmId(contentResolver, alarm2.id);
                    if (nextUpcomingInstanceByAlarmId != null && nextUpcomingInstanceByAlarmId.mAlarmState == 5) {
                        this.mMatchingAlarms.add(alarm2);
                    }
                }
                if (this.mMatchingAlarms.isEmpty()) {
                    AlarmInstance nextFiringAlarm = AlarmStateManager.getNextFiringAlarm(this.mContext);
                    if (nextFiringAlarm == null) {
                        notifyFailureAndLog(this.mContext.getString(R.string.no_scheduled_alarms), this.mActivity);
                    } else {
                        Calendar alarmTime = nextFiringAlarm.getAlarmTime();
                        this.mMatchingAlarms.addAll(getAlarmsByHourMinutes(alarmTime.get(11), alarmTime.get(12), contentResolver));
                    }
                    break;
                }
                break;
            case 2:
                this.mMatchingAlarms.addAll(this.mAlarms);
                break;
            case 3:
                String stringExtra2 = this.mIntent.getStringExtra("android.intent.extra.alarm.MESSAGE");
                if (stringExtra2 == null) {
                    notifyFailureAndLog(this.mContext.getString(R.string.no_label_specified), this.mActivity);
                } else {
                    for (Alarm alarm3 : this.mAlarms) {
                        if (alarm3.label.contains(stringExtra2)) {
                            this.mMatchingAlarms.add(alarm3);
                        }
                    }
                    if (this.mMatchingAlarms.isEmpty()) {
                        notifyFailureAndLog(this.mContext.getString(R.string.no_alarms_with_label), this.mActivity);
                    }
                }
                break;
        }
    }

    private List<Alarm> getAlarmsByHourMinutes(int i, int i2, ContentResolver contentResolver) {
        return Alarm.getAlarms(contentResolver, String.format("%s=? AND %s=? AND %s=?", "hour", "minutes", ClockContract.AlarmsColumns.ENABLED), String.valueOf(i), String.valueOf(i2), SettingsActivity.VOLUME_BEHAVIOR_SNOOZE);
    }

    public List<Alarm> getMatchingAlarms() {
        return this.mMatchingAlarms;
    }

    private void notifyFailureAndLog(String str, Activity activity) {
        LogUtils.e(str, new Object[0]);
        Controller.getController().notifyVoiceFailure(activity, str);
    }
}
