package com.android.calendar;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.widget.TimePicker;

public class OtherPreferences extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private Preference mCopyDb;
    private boolean mIs24HourMode;
    private CheckBoxPreference mQuietHours;
    private Preference mQuietHoursEnd;
    private TimePickerDialog mQuietHoursEndDialog;
    private TimeSetListener mQuietHoursEndListener;
    private Preference mQuietHoursStart;
    private TimePickerDialog mQuietHoursStartDialog;
    private TimeSetListener mQuietHoursStartListener;
    private ListPreference mSkipReminders;
    private TimePickerDialog mTimePickerDialog;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName("com.android.calendar_preferences");
        SharedPreferences prefs = manager.getSharedPreferences();
        addPreferencesFromResource(R.xml.other_preferences);
        this.mCopyDb = findPreference("preferences_copy_db");
        this.mSkipReminders = (ListPreference) findPreference("preferences_reminders_responded");
        String skipPreferencesValue = null;
        if (this.mSkipReminders != null) {
            skipPreferencesValue = this.mSkipReminders.getValue();
            this.mSkipReminders.setOnPreferenceChangeListener(this);
        }
        updateSkipRemindersSummary(skipPreferencesValue);
        Activity activity = getActivity();
        if (activity == null) {
            Log.d("CalendarOtherPreferences", "Activity was null");
        }
        this.mIs24HourMode = DateFormat.is24HourFormat(activity);
        this.mQuietHours = (CheckBoxPreference) findPreference("preferences_reminders_quiet_hours");
        int startHour = prefs.getInt("preferences_reminders_quiet_hours_start_hour", 22);
        int startMinute = prefs.getInt("preferences_reminders_quiet_hours_start_minute", 0);
        this.mQuietHoursStart = findPreference("preferences_reminders_quiet_hours_start");
        this.mQuietHoursStartListener = new TimeSetListener(1);
        this.mQuietHoursStartDialog = new TimePickerDialog(activity, this.mQuietHoursStartListener, startHour, startMinute, this.mIs24HourMode);
        this.mQuietHoursStart.setSummary(formatTime(startHour, startMinute));
        int endHour = prefs.getInt("preferences_reminders_quiet_hours_end_hour", 8);
        int endMinute = prefs.getInt("preferences_reminders_quiet_hours_end_minute", 0);
        this.mQuietHoursEnd = findPreference("preferences_reminders_quiet_hours_end");
        this.mQuietHoursEndListener = new TimeSetListener(2);
        this.mQuietHoursEndDialog = new TimePickerDialog(activity, this.mQuietHoursEndListener, endHour, endMinute, this.mIs24HourMode);
        this.mQuietHoursEnd.setSummary(formatTime(endHour, endMinute));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        String key = preference.getKey();
        if ("preferences_reminders_responded".equals(key)) {
            String value = String.valueOf(objValue);
            updateSkipRemindersSummary(value);
            return true;
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference == this.mCopyDb) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(new ComponentName("com.android.providers.calendar", "com.android.providers.calendar.CalendarDebugActivity"));
            startActivity(intent);
        } else if (preference == this.mQuietHoursStart) {
            if (this.mTimePickerDialog == null) {
                this.mTimePickerDialog = this.mQuietHoursStartDialog;
                this.mTimePickerDialog.show();
            } else {
                Log.v("CalendarOtherPreferences", "not null");
            }
        } else if (preference == this.mQuietHoursEnd) {
            if (this.mTimePickerDialog == null) {
                this.mTimePickerDialog = this.mQuietHoursEndDialog;
                this.mTimePickerDialog.show();
            } else {
                Log.v("CalendarOtherPreferences", "not null");
            }
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }
        return true;
    }

    private class TimeSetListener implements TimePickerDialog.OnTimeSetListener {
        private int mListenerId;

        public TimeSetListener(int listenerId) {
            this.mListenerId = listenerId;
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            OtherPreferences.this.mTimePickerDialog = null;
            SharedPreferences prefs = OtherPreferences.this.getPreferenceManager().getSharedPreferences();
            SharedPreferences.Editor editor = prefs.edit();
            String summary = OtherPreferences.this.formatTime(hourOfDay, minute);
            switch (this.mListenerId) {
                case 1:
                    OtherPreferences.this.mQuietHoursStart.setSummary(summary);
                    editor.putInt("preferences_reminders_quiet_hours_start_hour", hourOfDay);
                    editor.putInt("preferences_reminders_quiet_hours_start_minute", minute);
                    break;
                case 2:
                    OtherPreferences.this.mQuietHoursEnd.setSummary(summary);
                    editor.putInt("preferences_reminders_quiet_hours_end_hour", hourOfDay);
                    editor.putInt("preferences_reminders_quiet_hours_end_minute", minute);
                    break;
                default:
                    Log.d("CalendarOtherPreferences", "Set time for unknown listener: " + this.mListenerId);
                    break;
            }
            editor.commit();
        }
    }

    private String formatTime(int hourOfDay, int minute) {
        Time time = new Time();
        time.hour = hourOfDay;
        time.minute = minute;
        String format = this.mIs24HourMode ? "%H:%M" : "%I:%M%P";
        return time.format(format);
    }

    private void updateSkipRemindersSummary(String value) {
        if (this.mSkipReminders != null) {
            int index = 0;
            CharSequence[] values = this.mSkipReminders.getEntryValues();
            CharSequence[] entries = this.mSkipReminders.getEntries();
            int value_i = 0;
            while (true) {
                if (value_i >= values.length) {
                    break;
                }
                if (!values[value_i].equals(value)) {
                    value_i++;
                } else {
                    index = value_i;
                    break;
                }
            }
            this.mSkipReminders.setSummary(entries[index].toString());
            if (value == null) {
                this.mSkipReminders.setValue(values[index].toString());
            }
        }
    }
}
