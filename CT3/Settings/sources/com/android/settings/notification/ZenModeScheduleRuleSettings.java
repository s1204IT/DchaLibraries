package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.service.notification.ZenModeConfig;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;
import com.android.settings.R;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class ZenModeScheduleRuleSettings extends ZenModeRuleSettingsBase {
    private final SimpleDateFormat mDayFormat = new SimpleDateFormat("EEE");
    private Preference mDays;
    private TimePickerPreference mEnd;
    private SwitchPreference mExitAtAlarm;
    private ZenModeConfig.ScheduleInfo mSchedule;
    private TimePickerPreference mStart;

    @Override
    protected boolean setRule(AutomaticZenRule rule) {
        this.mSchedule = rule != null ? ZenModeConfig.tryParseScheduleConditionId(rule.getConditionId()) : null;
        return this.mSchedule != null;
    }

    @Override
    protected String getZenModeDependency() {
        return this.mDays.getKey();
    }

    @Override
    protected int getEnabledToastText() {
        return R.string.zen_schedule_rule_enabled_toast;
    }

    @Override
    protected void onCreateInternal() {
        addPreferencesFromResource(R.xml.zen_mode_schedule_rule_settings);
        PreferenceScreen root = getPreferenceScreen();
        this.mDays = root.findPreference("days");
        this.mDays.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ZenModeScheduleRuleSettings.this.showDaysDialog();
                return true;
            }
        });
        FragmentManager mgr = getFragmentManager();
        this.mStart = new TimePickerPreference(getPrefContext(), mgr);
        this.mStart.setKey("start_time");
        this.mStart.setTitle(R.string.zen_mode_start_time);
        this.mStart.setCallback(new TimePickerPreference.Callback() {
            @Override
            public boolean onSetTime(int hour, int minute) {
                if (ZenModeScheduleRuleSettings.this.mDisableListeners) {
                    return true;
                }
                if (!ZenModeConfig.isValidHour(hour) || !ZenModeConfig.isValidMinute(minute)) {
                    return false;
                }
                if (hour == ZenModeScheduleRuleSettings.this.mSchedule.startHour && minute == ZenModeScheduleRuleSettings.this.mSchedule.startMinute) {
                    return true;
                }
                if (ZenModeScheduleRuleSettings.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange start h=" + hour + " m=" + minute);
                }
                ZenModeScheduleRuleSettings.this.mSchedule.startHour = hour;
                ZenModeScheduleRuleSettings.this.mSchedule.startMinute = minute;
                ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
                return true;
            }
        });
        root.addPreference(this.mStart);
        this.mStart.setDependency(this.mDays.getKey());
        this.mEnd = new TimePickerPreference(getPrefContext(), mgr);
        this.mEnd.setKey("end_time");
        this.mEnd.setTitle(R.string.zen_mode_end_time);
        this.mEnd.setCallback(new TimePickerPreference.Callback() {
            @Override
            public boolean onSetTime(int hour, int minute) {
                if (ZenModeScheduleRuleSettings.this.mDisableListeners) {
                    return true;
                }
                if (!ZenModeConfig.isValidHour(hour) || !ZenModeConfig.isValidMinute(minute)) {
                    return false;
                }
                if (hour == ZenModeScheduleRuleSettings.this.mSchedule.endHour && minute == ZenModeScheduleRuleSettings.this.mSchedule.endMinute) {
                    return true;
                }
                if (ZenModeScheduleRuleSettings.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange end h=" + hour + " m=" + minute);
                }
                ZenModeScheduleRuleSettings.this.mSchedule.endHour = hour;
                ZenModeScheduleRuleSettings.this.mSchedule.endMinute = minute;
                ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
                return true;
            }
        });
        root.addPreference(this.mEnd);
        this.mEnd.setDependency(this.mDays.getKey());
        this.mExitAtAlarm = (SwitchPreference) root.findPreference("exit_at_alarm");
        this.mExitAtAlarm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                ZenModeScheduleRuleSettings.this.mSchedule.exitAtAlarm = ((Boolean) o).booleanValue();
                ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
                return true;
            }
        });
    }

    public void updateDays() {
        int[] days = this.mSchedule.days;
        if (days != null && days.length > 0) {
            StringBuilder sb = new StringBuilder();
            Calendar c = Calendar.getInstance();
            for (int i = 0; i < ZenModeScheduleDaysSelection.DAYS.length; i++) {
                int day = ZenModeScheduleDaysSelection.DAYS[i];
                int j = 0;
                while (true) {
                    if (j >= days.length) {
                        break;
                    }
                    if (day != days[j]) {
                        j++;
                    } else {
                        c.set(7, day);
                        if (sb.length() > 0) {
                            sb.append(this.mContext.getString(R.string.summary_divider_text));
                        }
                        sb.append(this.mDayFormat.format(c.getTime()));
                    }
                }
            }
            if (sb.length() > 0) {
                this.mDays.setSummary(sb);
                this.mDays.notifyDependencyChange(false);
                return;
            }
        }
        this.mDays.setSummary(R.string.zen_mode_schedule_rule_days_none);
        this.mDays.notifyDependencyChange(true);
    }

    private void updateEndSummary() {
        int startMin = (this.mSchedule.startHour * 60) + this.mSchedule.startMinute;
        int endMin = (this.mSchedule.endHour * 60) + this.mSchedule.endMinute;
        boolean nextDay = startMin >= endMin;
        int summaryFormat = nextDay ? R.string.zen_mode_end_time_next_day_summary_format : 0;
        this.mEnd.setSummaryFormat(summaryFormat);
    }

    @Override
    protected void updateControlsInternal() {
        updateDays();
        this.mStart.setTime(this.mSchedule.startHour, this.mSchedule.startMinute);
        this.mEnd.setTime(this.mSchedule.endHour, this.mSchedule.endMinute);
        this.mExitAtAlarm.setChecked(this.mSchedule.exitAtAlarm);
        updateEndSummary();
    }

    @Override
    protected int getMetricsCategory() {
        return 144;
    }

    public void showDaysDialog() {
        new AlertDialog.Builder(this.mContext).setTitle(R.string.zen_mode_schedule_rule_days).setView(new ZenModeScheduleDaysSelection(this.mContext, this.mSchedule.days) {
            @Override
            protected void onChanged(int[] days) {
                if (ZenModeScheduleRuleSettings.this.mDisableListeners || Arrays.equals(days, ZenModeScheduleRuleSettings.this.mSchedule.days)) {
                    return;
                }
                if (ZenModeScheduleRuleSettings.DEBUG) {
                    Log.d("ZenModeSettings", "days.onChanged days=" + Arrays.asList(days));
                }
                ZenModeScheduleRuleSettings.this.mSchedule.days = days;
                ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ZenModeScheduleRuleSettings.this.updateDays();
            }
        }).setPositiveButton(R.string.done_button, (DialogInterface.OnClickListener) null).show();
    }

    private static class TimePickerPreference extends Preference {
        private Callback mCallback;
        private final Context mContext;
        private int mHourOfDay;
        private int mMinute;
        private int mSummaryFormat;

        public interface Callback {
            boolean onSetTime(int i, int i2);
        }

        public TimePickerPreference(Context context, final FragmentManager mgr) {
            super(context);
            this.mContext = context;
            setPersistent(false);
            setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    TimePickerFragment frag = new TimePickerFragment();
                    frag.pref = TimePickerPreference.this;
                    frag.show(mgr, TimePickerPreference.class.getName());
                    return true;
                }
            });
        }

        public void setCallback(Callback callback) {
            this.mCallback = callback;
        }

        public void setSummaryFormat(int resId) {
            this.mSummaryFormat = resId;
            updateSummary();
        }

        public void setTime(int hourOfDay, int minute) {
            if (this.mCallback != null && !this.mCallback.onSetTime(hourOfDay, minute)) {
                return;
            }
            this.mHourOfDay = hourOfDay;
            this.mMinute = minute;
            updateSummary();
        }

        private void updateSummary() {
            Calendar c = Calendar.getInstance();
            c.set(11, this.mHourOfDay);
            c.set(12, this.mMinute);
            String time = DateFormat.getTimeFormat(this.mContext).format(c.getTime());
            if (this.mSummaryFormat != 0) {
                time = this.mContext.getResources().getString(this.mSummaryFormat, time);
            }
            setSummary(time);
        }

        public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
            public TimePickerPreference pref;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                boolean usePref = this.pref != null && this.pref.mHourOfDay >= 0 && this.pref.mMinute >= 0;
                Calendar c = Calendar.getInstance();
                int hour = usePref ? this.pref.mHourOfDay : c.get(11);
                int minute = usePref ? this.pref.mMinute : c.get(12);
                return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
            }

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (this.pref == null) {
                    return;
                }
                this.pref.setTime(hourOfDay, minute);
            }
        }
    }
}
