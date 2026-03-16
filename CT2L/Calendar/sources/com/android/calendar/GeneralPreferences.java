package com.android.calendar;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.CalendarContract;
import android.provider.SearchRecentSuggestions;
import android.text.TextUtils;
import android.text.format.Time;
import android.widget.Toast;
import com.android.calendar.alerts.AlertReceiver;
import com.android.timezonepicker.TimeZoneInfo;
import com.android.timezonepicker.TimeZonePickerDialog;
import com.android.timezonepicker.TimeZonePickerUtils;

public class GeneralPreferences extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener, TimeZonePickerDialog.OnTimeZoneSetListener {
    CheckBoxPreference mAlert;
    ListPreference mDefaultReminder;
    CheckBoxPreference mHideDeclined;
    Preference mHomeTZ;
    CheckBoxPreference mPopup;
    RingtonePreference mRingtone;
    private String mTimeZoneId;
    TimeZonePickerUtils mTzPickerUtils;
    CheckBoxPreference mUseHomeTZ;
    CheckBoxPreference mVibrate;
    ListPreference mWeekStart;

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("com.android.calendar_preferences", 0);
    }

    public static void setDefaultValues(Context context) {
        PreferenceManager.setDefaultValues(context, "com.android.calendar_preferences", 0, R.xml.general_preferences, false);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Activity activity = getActivity();
        PreferenceManager preferenceManager = getPreferenceManager();
        SharedPreferences sharedPreferences = getSharedPreferences(activity);
        preferenceManager.setSharedPreferencesName("com.android.calendar_preferences");
        addPreferencesFromResource(R.xml.general_preferences);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mAlert = (CheckBoxPreference) preferenceScreen.findPreference("preferences_alerts");
        this.mVibrate = (CheckBoxPreference) preferenceScreen.findPreference("preferences_alerts_vibrate");
        Vibrator vibrator = (Vibrator) activity.getSystemService("vibrator");
        if (vibrator == null || !vibrator.hasVibrator()) {
            PreferenceCategory mAlertGroup = (PreferenceCategory) preferenceScreen.findPreference("preferences_alerts_category");
            mAlertGroup.removePreference(this.mVibrate);
        }
        this.mRingtone = (RingtonePreference) preferenceScreen.findPreference("preferences_alerts_ringtone");
        String ringToneUri = Utils.getRingTonePreference(activity);
        SharedPreferences.Editor editor = preferenceScreen.getEditor();
        editor.putString("preferences_alerts_ringtone", ringToneUri).apply();
        String ringtoneDisplayString = getRingtoneTitleFromUri(activity, ringToneUri);
        RingtonePreference ringtonePreference = this.mRingtone;
        if (ringtoneDisplayString == null) {
            ringtoneDisplayString = "";
        }
        ringtonePreference.setSummary(ringtoneDisplayString);
        this.mPopup = (CheckBoxPreference) preferenceScreen.findPreference("preferences_alerts_popup");
        this.mUseHomeTZ = (CheckBoxPreference) preferenceScreen.findPreference("preferences_home_tz_enabled");
        this.mHideDeclined = (CheckBoxPreference) preferenceScreen.findPreference("preferences_hide_declined");
        this.mWeekStart = (ListPreference) preferenceScreen.findPreference("preferences_week_start_day");
        this.mDefaultReminder = (ListPreference) preferenceScreen.findPreference("preferences_default_reminder");
        this.mHomeTZ = preferenceScreen.findPreference("preferences_home_tz");
        this.mWeekStart.setSummary(this.mWeekStart.getEntry());
        this.mDefaultReminder.setSummary(this.mDefaultReminder.getEntry());
        this.mTimeZoneId = Utils.getTimeZone(activity, null);
        SharedPreferences prefs = CalendarUtils.getSharedPreferences(activity, "com.android.calendar_preferences");
        if (!prefs.getBoolean("preferences_home_tz_enabled", false)) {
            this.mTimeZoneId = prefs.getString("preferences_home_tz", Time.getCurrentTimezone());
        }
        this.mHomeTZ.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                GeneralPreferences.this.showTimezoneDialog();
                return true;
            }
        });
        if (this.mTzPickerUtils == null) {
            this.mTzPickerUtils = new TimeZonePickerUtils(getActivity());
        }
        CharSequence timezoneName = this.mTzPickerUtils.getGmtDisplayName(getActivity(), this.mTimeZoneId, System.currentTimeMillis(), false);
        Preference preference = this.mHomeTZ;
        if (timezoneName == null) {
            timezoneName = this.mTimeZoneId;
        }
        preference.setSummary(timezoneName);
        TimeZonePickerDialog tzpd = (TimeZonePickerDialog) activity.getFragmentManager().findFragmentByTag("TimeZonePicker");
        if (tzpd != null) {
            tzpd.setOnTimeZoneSetListener(this);
        }
        migrateOldPreferences(sharedPreferences);
        updateChildPreferences();
    }

    private void showTimezoneDialog() {
        Activity activity = getActivity();
        if (activity != null) {
            Bundle b = new Bundle();
            b.putLong("bundle_event_start_time", System.currentTimeMillis());
            b.putString("bundle_event_time_zone", Utils.getTimeZone(activity, null));
            FragmentManager fm = getActivity().getFragmentManager();
            TimeZonePickerDialog tzpd = (TimeZonePickerDialog) fm.findFragmentByTag("TimeZonePicker");
            if (tzpd != null) {
                tzpd.dismiss();
            }
            TimeZonePickerDialog tzpd2 = new TimeZonePickerDialog();
            tzpd2.setArguments(b);
            tzpd2.setOnTimeZoneSetListener(this);
            tzpd2.show(fm, "TimeZonePicker");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        setPreferenceListeners(this);
    }

    private void setPreferenceListeners(Preference.OnPreferenceChangeListener listener) {
        this.mUseHomeTZ.setOnPreferenceChangeListener(listener);
        this.mHomeTZ.setOnPreferenceChangeListener(listener);
        this.mWeekStart.setOnPreferenceChangeListener(listener);
        this.mDefaultReminder.setOnPreferenceChangeListener(listener);
        this.mRingtone.setOnPreferenceChangeListener(listener);
        this.mHideDeclined.setOnPreferenceChangeListener(listener);
        this.mVibrate.setOnPreferenceChangeListener(listener);
    }

    @Override
    public void onStop() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        setPreferenceListeners(null);
        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Activity a = getActivity();
        if (key.equals("preferences_alerts")) {
            updateChildPreferences();
            if (a != null) {
                Intent intent = new Intent();
                intent.setClass(a, AlertReceiver.class);
                if (this.mAlert.isChecked()) {
                    intent.setAction("removeOldReminders");
                } else {
                    intent.setAction("com.android.calendar.EVENT_REMINDER_APP");
                }
                a.sendBroadcast(intent);
            }
        }
        if (a != null) {
            BackupManager.dataChanged(a.getPackageName());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String tz;
        Activity activity = getActivity();
        if (preference == this.mUseHomeTZ) {
            if (((Boolean) newValue).booleanValue()) {
                tz = this.mTimeZoneId;
            } else {
                tz = "auto";
            }
            Utils.setTimeZone(activity, tz);
            return true;
        }
        if (preference == this.mHideDeclined) {
            this.mHideDeclined.setChecked(((Boolean) newValue).booleanValue());
            Intent intent = new Intent(Utils.getWidgetScheduledUpdateAction(activity));
            intent.setDataAndType(CalendarContract.CONTENT_URI, "vnd.android.data/update");
            activity.sendBroadcast(intent);
            return true;
        }
        if (preference == this.mWeekStart) {
            this.mWeekStart.setValue((String) newValue);
            this.mWeekStart.setSummary(this.mWeekStart.getEntry());
        } else if (preference == this.mDefaultReminder) {
            this.mDefaultReminder.setValue((String) newValue);
            this.mDefaultReminder.setSummary(this.mDefaultReminder.getEntry());
        } else {
            if (preference == this.mRingtone) {
                if (newValue instanceof String) {
                    Utils.setRingTonePreference(activity, (String) newValue);
                    String ringtone = getRingtoneTitleFromUri(activity, (String) newValue);
                    RingtonePreference ringtonePreference = this.mRingtone;
                    if (ringtone == null) {
                        ringtone = "";
                    }
                    ringtonePreference.setSummary(ringtone);
                }
                return true;
            }
            if (preference != this.mVibrate) {
                return true;
            }
            this.mVibrate.setChecked(((Boolean) newValue).booleanValue());
            return true;
        }
        return false;
    }

    public String getRingtoneTitleFromUri(Context context, String uri) {
        Ringtone ring;
        if (TextUtils.isEmpty(uri) || (ring = RingtoneManager.getRingtone(getActivity(), Uri.parse(uri))) == null) {
            return null;
        }
        return ring.getTitle(context);
    }

    private void migrateOldPreferences(SharedPreferences prefs) {
        this.mVibrate.setChecked(Utils.getDefaultVibrate(getActivity(), prefs));
        if (!prefs.contains("preferences_alerts") && prefs.contains("preferences_alerts_type")) {
            String type = prefs.getString("preferences_alerts_type", "1");
            if (type.equals("2")) {
                this.mAlert.setChecked(false);
                this.mPopup.setChecked(false);
                this.mPopup.setEnabled(false);
            } else if (type.equals("1")) {
                this.mAlert.setChecked(true);
                this.mPopup.setChecked(false);
                this.mPopup.setEnabled(true);
            } else if (type.equals("0")) {
                this.mAlert.setChecked(true);
                this.mPopup.setChecked(true);
                this.mPopup.setEnabled(true);
            }
            prefs.edit().remove("preferences_alerts_type").commit();
        }
    }

    private void updateChildPreferences() {
        if (this.mAlert.isChecked()) {
            this.mVibrate.setEnabled(true);
            this.mRingtone.setEnabled(true);
            this.mPopup.setEnabled(true);
        } else {
            this.mVibrate.setEnabled(false);
            this.mRingtone.setEnabled(false);
            this.mPopup.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (!"preferences_clear_search_history".equals(key)) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(), Utils.getSearchAuthority(getActivity()), 1);
        suggestions.clearHistory();
        Toast.makeText(getActivity(), R.string.search_history_cleared, 0).show();
        return true;
    }

    @Override
    public void onTimeZoneSet(TimeZoneInfo tzi) {
        if (this.mTzPickerUtils == null) {
            this.mTzPickerUtils = new TimeZonePickerUtils(getActivity());
        }
        CharSequence timezoneName = this.mTzPickerUtils.getGmtDisplayName(getActivity(), tzi.mTzId, System.currentTimeMillis(), false);
        this.mHomeTZ.setSummary(timezoneName);
        Utils.setTimeZone(getActivity(), tzi.mTzId);
    }
}
