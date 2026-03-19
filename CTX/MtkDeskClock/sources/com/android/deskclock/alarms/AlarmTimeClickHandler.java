package com.android.deskclock.alarms;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.alarms.dataadapter.AlarmItemHolder;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.ringtone.RingtonePickerActivity;
import java.util.Calendar;

public final class AlarmTimeClickHandler {
    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("AlarmTimeClickHandler");
    private final AlarmUpdateHandler mAlarmUpdateHandler;
    private final Context mContext;
    private final Fragment mFragment;
    private Bundle mPreviousDaysOfWeekMap;
    private final ScrollHandler mScrollHandler;
    private Alarm mSelectedAlarm;

    public AlarmTimeClickHandler(Fragment fragment, Bundle bundle, AlarmUpdateHandler alarmUpdateHandler, ScrollHandler scrollHandler) {
        this.mFragment = fragment;
        this.mContext = this.mFragment.getActivity().getApplicationContext();
        this.mAlarmUpdateHandler = alarmUpdateHandler;
        this.mScrollHandler = scrollHandler;
        if (bundle != null) {
            this.mPreviousDaysOfWeekMap = bundle.getBundle(KEY_PREVIOUS_DAY_MAP);
        }
        if (this.mPreviousDaysOfWeekMap == null) {
            this.mPreviousDaysOfWeekMap = new Bundle();
        }
    }

    public void setSelectedAlarm(Alarm alarm) {
        this.mSelectedAlarm = alarm;
    }

    public void saveInstance(Bundle bundle) {
        bundle.putBundle(KEY_PREVIOUS_DAY_MAP, this.mPreviousDaysOfWeekMap);
    }

    public void setAlarmEnabled(Alarm alarm, boolean z) {
        if (z != alarm.enabled) {
            alarm.enabled = z;
            Events.sendAlarmEvent(z ? R.string.action_enable : R.string.action_disable, R.string.label_deskclock);
            this.mAlarmUpdateHandler.asyncUpdateAlarm(alarm, alarm.enabled, false);
            LOGGER.d("Updating alarm enabled state to " + z, new Object[0]);
        }
    }

    public void setAlarmVibrationEnabled(Alarm alarm, boolean z) {
        if (z != alarm.vibrate) {
            alarm.vibrate = z;
            Events.sendAlarmEvent(R.string.action_toggle_vibrate, R.string.label_deskclock);
            this.mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
            LOGGER.d("Updating vibrate state to " + z, new Object[0]);
            if (z) {
                Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(300L);
                }
            }
        }
    }

    public void setAlarmRepeatEnabled(Alarm alarm, boolean z) {
        Calendar calendar = Calendar.getInstance();
        Calendar nextAlarmTime = alarm.getNextAlarmTime(calendar);
        String strValueOf = String.valueOf(alarm.id);
        if (z) {
            alarm.daysOfWeek = Weekdays.fromBits(this.mPreviousDaysOfWeekMap.getInt(strValueOf));
            if (!alarm.daysOfWeek.isRepeating()) {
                alarm.daysOfWeek = Weekdays.ALL;
            }
        } else {
            this.mPreviousDaysOfWeekMap.putInt(strValueOf, alarm.daysOfWeek.getBits());
            alarm.daysOfWeek = Weekdays.NONE;
        }
        boolean z2 = !nextAlarmTime.equals(alarm.getNextAlarmTime(calendar));
        Events.sendAlarmEvent(R.string.action_toggle_repeat_days, R.string.label_deskclock);
        this.mAlarmUpdateHandler.asyncUpdateAlarm(alarm, z2, false);
    }

    public void setDayOfWeekEnabled(Alarm alarm, boolean z, int i) {
        Calendar nextAlarmTime = alarm.getNextAlarmTime(Calendar.getInstance());
        alarm.daysOfWeek = alarm.daysOfWeek.setBit(DataModel.getDataModel().getWeekdayOrder().getCalendarDays().get(i).intValue(), z);
        this.mAlarmUpdateHandler.asyncUpdateAlarm(alarm, !nextAlarmTime.equals(alarm.getNextAlarmTime(r0)), false);
    }

    public void onDeleteClicked(AlarmItemHolder alarmItemHolder) {
        if (this.mFragment instanceof AlarmClockFragment) {
            ((AlarmClockFragment) this.mFragment).removeItem(alarmItemHolder);
        }
        Alarm alarm = (Alarm) alarmItemHolder.item;
        Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
        this.mAlarmUpdateHandler.asyncDeleteAlarm(alarm);
        LOGGER.d("Deleting alarm.", new Object[0]);
    }

    public void onClockClicked(Alarm alarm) {
        this.mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);
        TimePickerDialogFragment.show(this.mFragment, alarm.hour, alarm.minutes);
    }

    public void dismissAlarmInstance(AlarmInstance alarmInstance) {
        this.mContext.startService(AlarmStateManager.createStateChangeIntent(this.mContext, AlarmStateManager.ALARM_DISMISS_TAG, alarmInstance, 8));
        this.mAlarmUpdateHandler.showPredismissToast(alarmInstance);
    }

    public void onRingtoneClicked(Context context, Alarm alarm) {
        this.mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_ringtone, R.string.label_deskclock);
        context.startActivity(RingtonePickerActivity.createAlarmRingtonePickerIntent(context, alarm));
    }

    public void onEditLabelClicked(Alarm alarm) {
        Events.sendAlarmEvent(R.string.action_set_label, R.string.label_deskclock);
        LabelDialogFragment.show(this.mFragment.getFragmentManager(), LabelDialogFragment.newInstance(alarm, alarm.label, this.mFragment.getTag()));
    }

    public void onTimeSet(int i, int i2) {
        if (this.mSelectedAlarm == null) {
            Alarm alarm = new Alarm();
            alarm.hour = i;
            alarm.minutes = i2;
            alarm.enabled = true;
            this.mAlarmUpdateHandler.asyncAddAlarm(alarm);
            return;
        }
        this.mSelectedAlarm.hour = i;
        this.mSelectedAlarm.minutes = i2;
        this.mSelectedAlarm.enabled = true;
        this.mScrollHandler.setSmoothScrollStableId(this.mSelectedAlarm.id);
        this.mAlarmUpdateHandler.asyncUpdateAlarm(this.mSelectedAlarm, true, false);
        this.mSelectedAlarm = null;
    }
}
