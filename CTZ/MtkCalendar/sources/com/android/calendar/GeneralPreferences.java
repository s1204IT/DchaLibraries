package com.android.calendar;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.CalendarContract;
import android.provider.SearchRecentSuggestions;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;
import com.android.calendar.alerts.AlertReceiver;
import com.android.timezonepicker.TimeZoneInfo;
import com.android.timezonepicker.TimeZonePickerDialog;
import com.android.timezonepicker.TimeZonePickerUtils;
import com.mediatek.calendar.MTKToast;

public class GeneralPreferences extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener, TimeZonePickerDialog.OnTimeZoneSetListener {
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};
    CheckBoxPreference mAlert;
    ListPreference mDefaultReminder;
    CheckBoxPreference mHideDeclined;
    Preference mHomeTZ;
    Preference mNotificationChannel;
    CheckBoxPreference mPopup;
    private String mTimeZoneId;
    TimeZonePickerUtils mTzPickerUtils;
    CheckBoxPreference mUseHomeTZ;
    ListPreference mWeekStart;

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("com.android.calendar_preferences", 0);
    }

    public static void setDefaultValues(Context context) {
        PreferenceManager.setDefaultValues(context, "com.android.calendar_preferences", 0, R.xml.general_preferences, false);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        final Activity activity = getActivity();
        PreferenceManager preferenceManager = getPreferenceManager();
        SharedPreferences sharedPreferences = getSharedPreferences(activity);
        preferenceManager.setSharedPreferencesName("com.android.calendar_preferences");
        addPreferencesFromResource(R.xml.general_preferences);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mAlert = (CheckBoxPreference) preferenceScreen.findPreference("preferences_alerts");
        preferenceScreen.getEditor();
        this.mNotificationChannel = findPreference("preferences_notification_channel");
        this.mNotificationChannel.setSummary(R.string.notification_channel_settings_summary);
        this.mPopup = (CheckBoxPreference) preferenceScreen.findPreference("preferences_alerts_popup");
        this.mUseHomeTZ = (CheckBoxPreference) preferenceScreen.findPreference("preferences_home_tz_enabled");
        this.mHideDeclined = (CheckBoxPreference) preferenceScreen.findPreference("preferences_hide_declined");
        this.mWeekStart = (ListPreference) preferenceScreen.findPreference("preferences_week_start_day");
        this.mDefaultReminder = (ListPreference) preferenceScreen.findPreference("preferences_default_reminder");
        this.mHomeTZ = preferenceScreen.findPreference("preferences_home_tz");
        this.mWeekStart.setSummary(this.mWeekStart.getEntry());
        this.mDefaultReminder.setSummary(this.mDefaultReminder.getEntry());
        this.mTimeZoneId = Utils.getTimeZone(activity, null);
        SharedPreferences sharedPreferences2 = CalendarUtils.getSharedPreferences(activity, "com.android.calendar_preferences");
        if (!sharedPreferences2.getBoolean("preferences_home_tz_enabled", false)) {
            this.mTimeZoneId = sharedPreferences2.getString("preferences_home_tz", Time.getCurrentTimezone());
        }
        this.mHomeTZ.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (GeneralPreferences.this.checkPermissions()) {
                    GeneralPreferences.this.showTimezoneDialog();
                    return true;
                }
                Intent intent = new Intent(activity, (Class<?>) PermissionDeniedActivity.class);
                intent.addFlags(268435456);
                GeneralPreferences.this.startActivity(intent);
                return false;
            }
        });
        this.mNotificationChannel.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (BenesseExtension.getDchaState() != 0) {
                    return false;
                }
                String packageName = GeneralPreferences.this.getActivity().getPackageName();
                Log.d("GeneralPreferences", "OnPreferenceClickListener package=" + packageName + ", id = calendar_notif_channel_default");
                Intent intent = new Intent("android.settings.CHANNEL_NOTIFICATION_SETTINGS");
                intent.putExtra("android.provider.extra.APP_PACKAGE", packageName);
                intent.putExtra("android.provider.extra.CHANNEL_ID", "calendar_notif_channel_default");
                GeneralPreferences.this.startActivity(intent);
                return false;
            }
        });
        if (this.mTzPickerUtils == null) {
            this.mTzPickerUtils = new TimeZonePickerUtils(getActivity());
        }
        CharSequence gmtDisplayName = this.mTzPickerUtils.getGmtDisplayName(getActivity(), this.mTimeZoneId, System.currentTimeMillis(), false);
        Preference preference = this.mHomeTZ;
        if (gmtDisplayName == null) {
            gmtDisplayName = this.mTimeZoneId;
        }
        preference.setSummary(gmtDisplayName);
        TimeZonePickerDialog timeZonePickerDialog = (TimeZonePickerDialog) activity.getFragmentManager().findFragmentByTag("TimeZonePicker");
        if (timeZonePickerDialog != null) {
            timeZonePickerDialog.setOnTimeZoneSetListener(this);
        }
        migrateOldPreferences(sharedPreferences);
        updateChildPreferences();
    }

    private void showTimezoneDialog() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putLong("bundle_event_start_time", System.currentTimeMillis());
        bundle.putString("bundle_event_time_zone", Utils.getTimeZone(activity, null));
        FragmentManager fragmentManager = getActivity().getFragmentManager();
        TimeZonePickerDialog timeZonePickerDialog = (TimeZonePickerDialog) fragmentManager.findFragmentByTag("TimeZonePicker");
        if (timeZonePickerDialog != null) {
            timeZonePickerDialog.dismiss();
        }
        TimeZonePickerDialog timeZonePickerDialog2 = new TimeZonePickerDialog();
        timeZonePickerDialog2.setArguments(bundle);
        timeZonePickerDialog2.setOnTimeZoneSetListener(this);
        timeZonePickerDialog2.show(fragmentManager, "TimeZonePicker");
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
        setPreferenceListeners(this);
    }

    private void setPreferenceListeners(Preference.OnPreferenceChangeListener onPreferenceChangeListener) {
        this.mUseHomeTZ.setOnPreferenceChangeListener(onPreferenceChangeListener);
        this.mHomeTZ.setOnPreferenceChangeListener(onPreferenceChangeListener);
        this.mWeekStart.setOnPreferenceChangeListener(onPreferenceChangeListener);
        this.mDefaultReminder.setOnPreferenceChangeListener(onPreferenceChangeListener);
        this.mHideDeclined.setOnPreferenceChangeListener(onPreferenceChangeListener);
        this.mNotificationChannel.setOnPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public void onStop() {
        if (getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
        setPreferenceListeners(null);
        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        Activity activity = getActivity();
        if (activity != null) {
            if (str.equals("preferences_alerts")) {
                updateChildPreferences();
                if (Utils.canUseProviderByUri(activity.getContentResolver(), CalendarContract.CalendarAlerts.CONTENT_URI)) {
                    Intent intent = new Intent();
                    intent.setClass(activity, AlertReceiver.class);
                    if (this.mAlert.isChecked()) {
                        intent.setAction("removeOldReminders");
                    } else {
                        intent.setAction("com.android.calendar.EVENT_REMINDER_APP");
                    }
                    activity.sendBroadcast(intent);
                } else {
                    Toast.makeText(getActivity(), R.string.operation_failed, 1).show();
                }
            }
            BackupManager.dataChanged(activity.getPackageName());
        }
    }

    protected boolean hasRequiredPermission(String[] strArr) {
        for (String str : strArr) {
            if (getActivity().checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPermissions() {
        if (!hasRequiredPermission(CALENDAR_PERMISSION)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        String str;
        Activity activity = getActivity();
        if (preference == this.mUseHomeTZ) {
            if (((Boolean) obj).booleanValue()) {
                str = this.mTimeZoneId;
            } else {
                str = "auto";
            }
            if (checkPermissions()) {
                Utils.setTimeZone(activity, str);
                return true;
            }
            Intent intent = new Intent(activity, (Class<?>) PermissionDeniedActivity.class);
            intent.addFlags(268435456);
            startActivity(intent);
            return false;
        }
        if (preference == this.mHideDeclined) {
            this.mHideDeclined.setChecked(((Boolean) obj).booleanValue());
            Intent intent2 = new Intent(Utils.getWidgetScheduledUpdateAction(activity));
            intent2.setDataAndType(CalendarContract.CONTENT_URI, "vnd.android.data/update");
            activity.sendBroadcast(intent2);
            return true;
        }
        if (preference == this.mWeekStart) {
            this.mWeekStart.setValue((String) obj);
            this.mWeekStart.setSummary(this.mWeekStart.getEntry());
        } else if (preference == this.mDefaultReminder) {
            this.mDefaultReminder.setValue((String) obj);
            this.mDefaultReminder.setSummary(this.mDefaultReminder.getEntry());
        } else {
            if (preference != this.mNotificationChannel) {
                return true;
            }
            Log.d("GeneralPreferences", "onPreferenceChange mNotificationChannel");
        }
        return false;
    }

    private void migrateOldPreferences(SharedPreferences sharedPreferences) {
        if (!sharedPreferences.contains("preferences_alerts") && sharedPreferences.contains("preferences_alerts_type")) {
            String string = sharedPreferences.getString("preferences_alerts_type", "1");
            if (string.equals("2")) {
                this.mAlert.setChecked(false);
                this.mPopup.setChecked(false);
                this.mPopup.setEnabled(false);
            } else if (string.equals("1")) {
                this.mAlert.setChecked(true);
                this.mPopup.setChecked(false);
                this.mPopup.setEnabled(true);
            } else if (string.equals("0")) {
                this.mAlert.setChecked(true);
                this.mPopup.setChecked(true);
                this.mPopup.setEnabled(true);
            }
            sharedPreferences.edit().remove("preferences_alerts_type").commit();
        }
    }

    private void updateChildPreferences() {
        if (this.mAlert.isChecked()) {
            this.mPopup.setEnabled(true);
            this.mNotificationChannel.setEnabled(true);
        } else {
            this.mPopup.setEnabled(false);
            this.mNotificationChannel.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if ("preferences_clear_search_history".equals(preference.getKey())) {
            new SearchRecentSuggestions(getActivity(), Utils.getSearchAuthority(getActivity()), 1).clearHistory();
            MTKToast.toast(getActivity(), R.string.search_history_cleared);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onTimeZoneSet(TimeZoneInfo timeZoneInfo) {
        if (!checkPermissions()) {
            Intent intent = new Intent(getActivity(), (Class<?>) PermissionDeniedActivity.class);
            intent.addFlags(268435456);
            startActivity(intent);
        } else {
            if (this.mTzPickerUtils == null) {
                this.mTzPickerUtils = new TimeZonePickerUtils(getActivity());
            }
            this.mHomeTZ.setSummary(this.mTzPickerUtils.getGmtDisplayName(getActivity(), timeZoneInfo.mTzId, System.currentTimeMillis(), false));
            Utils.setTimeZone(getActivity(), timeZoneInfo.mTzId);
        }
    }
}
