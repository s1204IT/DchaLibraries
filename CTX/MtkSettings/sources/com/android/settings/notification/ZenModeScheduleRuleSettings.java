package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.app.Dialog;
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
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class ZenModeScheduleRuleSettings extends ZenModeRuleSettingsBase {
    private final SimpleDateFormat mDayFormat = new SimpleDateFormat("EEE");
    private Preference mDays;
    private TimePickerPreference mEnd;
    private SwitchPreference mExitAtAlarm;
    private ZenModeConfig.ScheduleInfo mSchedule;
    private TimePickerPreference mStart;

    @Override
    protected boolean setRule(AutomaticZenRule automaticZenRule) {
        this.mSchedule = automaticZenRule != null ? ZenModeConfig.tryParseScheduleConditionId(automaticZenRule.getConditionId()) : null;
        return this.mSchedule != null;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_schedule_rule_settings;
    }

    @Override
    protected void onCreateInternal() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mDays = preferenceScreen.findPreference("days");
        this.mDays.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ZenModeScheduleRuleSettings.this.showDaysDialog();
                return true;
            }
        });
        FragmentManager fragmentManager = getFragmentManager();
        this.mStart = new TimePickerPreference(getPrefContext(), fragmentManager);
        this.mStart.setKey("start_time");
        this.mStart.setTitle(R.string.zen_mode_start_time);
        this.mStart.setCallback(new TimePickerPreference.Callback() {
            @Override
            public boolean onSetTime(int i, int i2) {
                if (ZenModeScheduleRuleSettings.this.mDisableListeners) {
                    return true;
                }
                if (!ZenModeConfig.isValidHour(i) || !ZenModeConfig.isValidMinute(i2)) {
                    return false;
                }
                if (i == ZenModeScheduleRuleSettings.this.mSchedule.startHour && i2 == ZenModeScheduleRuleSettings.this.mSchedule.startMinute) {
                    return true;
                }
                if (ZenModeRuleSettingsBase.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange start h=" + i + " m=" + i2);
                }
                ZenModeScheduleRuleSettings.this.mSchedule.startHour = i;
                ZenModeScheduleRuleSettings.this.mSchedule.startMinute = i2;
                ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
                return true;
            }
        });
        preferenceScreen.addPreference(this.mStart);
        this.mStart.setDependency(this.mDays.getKey());
        this.mEnd = new TimePickerPreference(getPrefContext(), fragmentManager);
        this.mEnd.setKey("end_time");
        this.mEnd.setTitle(R.string.zen_mode_end_time);
        this.mEnd.setCallback(new TimePickerPreference.Callback() {
            @Override
            public boolean onSetTime(int i, int i2) {
                if (ZenModeScheduleRuleSettings.this.mDisableListeners) {
                    return true;
                }
                if (!ZenModeConfig.isValidHour(i) || !ZenModeConfig.isValidMinute(i2)) {
                    return false;
                }
                if (i == ZenModeScheduleRuleSettings.this.mSchedule.endHour && i2 == ZenModeScheduleRuleSettings.this.mSchedule.endMinute) {
                    return true;
                }
                if (ZenModeRuleSettingsBase.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange end h=" + i + " m=" + i2);
                }
                ZenModeScheduleRuleSettings.this.mSchedule.endHour = i;
                ZenModeScheduleRuleSettings.this.mSchedule.endMinute = i2;
                ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
                return true;
            }
        });
        preferenceScreen.addPreference(this.mEnd);
        this.mEnd.setDependency(this.mDays.getKey());
        this.mExitAtAlarm = (SwitchPreference) preferenceScreen.findPreference("exit_at_alarm");
        this.mExitAtAlarm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                ZenModeScheduleRuleSettings.this.mSchedule.exitAtAlarm = ((Boolean) obj).booleanValue();
                ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
                return true;
            }
        });
    }

    public void updateDays() {
        int[] iArr = this.mSchedule.days;
        if (iArr != null && iArr.length > 0) {
            StringBuilder sb = new StringBuilder();
            Calendar calendar = Calendar.getInstance();
            for (int i : ZenModeScheduleDaysSelection.getDaysOfWeekForLocale(calendar)) {
                int i2 = 0;
                while (true) {
                    if (i2 >= iArr.length) {
                        break;
                    }
                    if (i != iArr[i2]) {
                        i2++;
                    } else {
                        calendar.set(7, i);
                        if (sb.length() > 0) {
                            sb.append(this.mContext.getString(R.string.summary_divider_text));
                        }
                        sb.append(this.mDayFormat.format(calendar.getTime()));
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
        this.mEnd.setSummaryFormat((this.mSchedule.startHour * 60) + this.mSchedule.startMinute >= (60 * this.mSchedule.endHour) + this.mSchedule.endMinute ? R.string.zen_mode_end_time_next_day_summary_format : 0);
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
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        this.mHeader = new ZenAutomaticRuleHeaderPreferenceController(context, this, getLifecycle());
        this.mSwitch = new ZenAutomaticRuleSwitchPreferenceController(context, this, getLifecycle());
        arrayList.add(this.mHeader);
        arrayList.add(this.mSwitch);
        return arrayList;
    }

    @Override
    public int getMetricsCategory() {
        return 144;
    }

    public void showDaysDialog() {
        new AlertDialog.Builder(this.mContext).setTitle(R.string.zen_mode_schedule_rule_days).setView(new ZenModeScheduleDaysSelection(this.mContext, this.mSchedule.days) {
            @Override
            protected void onChanged(int[] iArr) {
                if (ZenModeScheduleRuleSettings.this.mDisableListeners || Arrays.equals(iArr, ZenModeScheduleRuleSettings.this.mSchedule.days)) {
                    return;
                }
                if (ZenModeRuleSettingsBase.DEBUG) {
                    Log.d("ZenModeSettings", "days.onChanged days=" + Arrays.asList(iArr));
                }
                ZenModeScheduleRuleSettings.this.mSchedule.days = iArr;
                ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
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

        public TimePickerPreference(Context context, final FragmentManager fragmentManager) {
            super(context);
            this.mContext = context;
            setPersistent(false);
            setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    TimePickerFragment timePickerFragment = new TimePickerFragment();
                    timePickerFragment.pref = TimePickerPreference.this;
                    timePickerFragment.show(fragmentManager, TimePickerPreference.class.getName());
                    return true;
                }
            });
        }

        public void setCallback(Callback callback) {
            this.mCallback = callback;
        }

        public void setSummaryFormat(int i) {
            this.mSummaryFormat = i;
            updateSummary();
        }

        public void setTime(int i, int i2) {
            if (this.mCallback == null || this.mCallback.onSetTime(i, i2)) {
                this.mHourOfDay = i;
                this.mMinute = i2;
                updateSummary();
            }
        }

        private void updateSummary() {
            Calendar calendar = Calendar.getInstance();
            calendar.set(11, this.mHourOfDay);
            calendar.set(12, this.mMinute);
            String string = DateFormat.getTimeFormat(this.mContext).format(calendar.getTime());
            if (this.mSummaryFormat != 0) {
                string = this.mContext.getResources().getString(this.mSummaryFormat, string);
            }
            setSummary(string);
        }

        public static class TimePickerFragment extends InstrumentedDialogFragment implements TimePickerDialog.OnTimeSetListener {
            public TimePickerPreference pref;

            @Override
            public int getMetricsCategory() {
                return 556;
            }

            @Override
            public Dialog onCreateDialog(Bundle bundle) {
                int i;
                int i2;
                boolean z = this.pref != null && this.pref.mHourOfDay >= 0 && this.pref.mMinute >= 0;
                Calendar calendar = Calendar.getInstance();
                if (z) {
                    i = this.pref.mHourOfDay;
                } else {
                    i = calendar.get(11);
                }
                int i3 = i;
                if (z) {
                    i2 = this.pref.mMinute;
                } else {
                    i2 = calendar.get(12);
                }
                return new TimePickerDialog(getActivity(), this, i3, i2, DateFormat.is24HourFormat(getActivity()));
            }

            @Override
            public void onTimeSet(TimePicker timePicker, int i, int i2) {
                if (this.pref != null) {
                    this.pref.setTime(i, i2);
                }
            }
        }
    }
}
