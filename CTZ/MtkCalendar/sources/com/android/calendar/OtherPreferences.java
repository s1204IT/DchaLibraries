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
    public void onCreate(Bundle bundle) {
        String value;
        super.onCreate(bundle);
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName("com.android.calendar_preferences");
        SharedPreferences sharedPreferences = preferenceManager.getSharedPreferences();
        addPreferencesFromResource(R.xml.other_preferences);
        this.mCopyDb = findPreference("preferences_copy_db");
        this.mSkipReminders = (ListPreference) findPreference("preferences_reminders_responded");
        if (this.mSkipReminders != null) {
            value = this.mSkipReminders.getValue();
            this.mSkipReminders.setOnPreferenceChangeListener(this);
        } else {
            value = null;
        }
        updateSkipRemindersSummary(value);
        Activity activity = getActivity();
        if (activity == null) {
            Log.d("CalendarOtherPreferences", "Activity was null");
        }
        this.mIs24HourMode = DateFormat.is24HourFormat(activity);
        this.mQuietHours = (CheckBoxPreference) findPreference("preferences_reminders_quiet_hours");
        int i = sharedPreferences.getInt("preferences_reminders_quiet_hours_start_hour", 22);
        int i2 = sharedPreferences.getInt("preferences_reminders_quiet_hours_start_minute", 0);
        this.mQuietHoursStart = findPreference("preferences_reminders_quiet_hours_start");
        this.mQuietHoursStartListener = new TimeSetListener(1);
        this.mQuietHoursStartDialog = new TimePickerDialog(activity, this.mQuietHoursStartListener, i, i2, this.mIs24HourMode);
        this.mQuietHoursStart.setSummary(formatTime(i, i2));
        int i3 = sharedPreferences.getInt("preferences_reminders_quiet_hours_end_hour", 8);
        int i4 = sharedPreferences.getInt("preferences_reminders_quiet_hours_end_minute", 0);
        this.mQuietHoursEnd = findPreference("preferences_reminders_quiet_hours_end");
        this.mQuietHoursEndListener = new TimeSetListener(2);
        this.mQuietHoursEndDialog = new TimePickerDialog(activity, this.mQuietHoursEndListener, i3, i4, this.mIs24HourMode);
        this.mQuietHoursEnd.setSummary(formatTime(i3, i4));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if ("preferences_reminders_responded".equals(preference.getKey())) {
            updateSkipRemindersSummary(String.valueOf(obj));
            return true;
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == this.mCopyDb) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(new ComponentName("com.android.providers.calendar", "com.android.providers.calendar.CalendarDebugActivity"));
            startActivity(intent);
            return true;
        }
        if (preference == this.mQuietHoursStart) {
            if (this.mTimePickerDialog == null) {
                this.mTimePickerDialog = this.mQuietHoursStartDialog;
                this.mTimePickerDialog.show();
                return true;
            }
            Log.v("CalendarOtherPreferences", "not null");
            return true;
        }
        if (preference == this.mQuietHoursEnd) {
            if (this.mTimePickerDialog == null) {
                this.mTimePickerDialog = this.mQuietHoursEndDialog;
                this.mTimePickerDialog.show();
                return true;
            }
            Log.v("CalendarOtherPreferences", "not null");
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private class TimeSetListener implements TimePickerDialog.OnTimeSetListener {
        private int mListenerId;

        public TimeSetListener(int i) {
            this.mListenerId = i;
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int i, int i2) {
            OtherPreferences.this.mTimePickerDialog = null;
            SharedPreferences.Editor editorEdit = OtherPreferences.this.getPreferenceManager().getSharedPreferences().edit();
            String time = OtherPreferences.this.formatTime(i, i2);
            switch (this.mListenerId) {
                case 1:
                    OtherPreferences.this.mQuietHoursStart.setSummary(time);
                    editorEdit.putInt("preferences_reminders_quiet_hours_start_hour", i);
                    editorEdit.putInt("preferences_reminders_quiet_hours_start_minute", i2);
                    break;
                case 2:
                    OtherPreferences.this.mQuietHoursEnd.setSummary(time);
                    editorEdit.putInt("preferences_reminders_quiet_hours_end_hour", i);
                    editorEdit.putInt("preferences_reminders_quiet_hours_end_minute", i2);
                    break;
                default:
                    Log.d("CalendarOtherPreferences", "Set time for unknown listener: " + this.mListenerId);
                    break;
            }
            editorEdit.commit();
        }
    }

    private String formatTime(int i, int i2) {
        Time time = new Time();
        time.hour = i;
        time.minute = i2;
        return time.format(this.mIs24HourMode ? "%H:%M" : "%I:%M%P");
    }

    private void updateSkipRemindersSummary(String str) {
        if (this.mSkipReminders != null) {
            CharSequence[] entryValues = this.mSkipReminders.getEntryValues();
            CharSequence[] entries = this.mSkipReminders.getEntries();
            int i = 0;
            int i2 = 0;
            while (true) {
                if (i2 >= entryValues.length) {
                    break;
                }
                if (!entryValues[i2].equals(str)) {
                    i2++;
                } else {
                    i = i2;
                    break;
                }
            }
            this.mSkipReminders.setSummary(entries[i].toString());
            if (str == null) {
                this.mSkipReminders.setValue(entryValues[i].toString());
            }
        }
    }
}
