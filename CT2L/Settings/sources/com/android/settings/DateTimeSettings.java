package com.android.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeSettings extends SettingsPreferenceFragment implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private SwitchPreference mAutoTimePref;
    private SwitchPreference mAutoTimeZonePref;
    private Preference mDatePref;
    private Calendar mDummyDate;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Activity activity = DateTimeSettings.this.getActivity();
            if (activity != null) {
                DateTimeSettings.this.updateTimeAndDateDisplay(activity);
            }
        }
    };
    private Preference mTime24Pref;
    private Preference mTimePref;
    private Preference mTimeZone;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.date_time_prefs);
        initUI();
    }

    private void initUI() {
        boolean autoTimeEnabled = getAutoState("auto_time");
        boolean autoTimeZoneEnabled = getAutoState("auto_time_zone");
        this.mAutoTimePref = (SwitchPreference) findPreference("auto_time");
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService("device_policy");
        if (dpm.getAutoTimeRequired()) {
            this.mAutoTimePref.setEnabled(false);
        }
        Intent intent = getActivity().getIntent();
        boolean isFirstRun = intent.getBooleanExtra("firstRun", false);
        this.mDummyDate = Calendar.getInstance();
        this.mAutoTimePref.setChecked(autoTimeEnabled);
        this.mAutoTimeZonePref = (SwitchPreference) findPreference("auto_zone");
        if (Utils.isWifiOnly(getActivity()) || isFirstRun) {
            getPreferenceScreen().removePreference(this.mAutoTimeZonePref);
            autoTimeZoneEnabled = false;
        }
        this.mAutoTimeZonePref.setChecked(autoTimeZoneEnabled);
        this.mTimePref = findPreference("time");
        this.mTime24Pref = findPreference("24 hour");
        this.mTimeZone = findPreference("timezone");
        this.mDatePref = findPreference("date");
        if (isFirstRun) {
            getPreferenceScreen().removePreference(this.mTime24Pref);
        }
        this.mTimePref.setEnabled(!autoTimeEnabled);
        this.mDatePref.setEnabled(!autoTimeEnabled);
        this.mTimeZone.setEnabled(autoTimeZoneEnabled ? false : true);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        ((SwitchPreference) this.mTime24Pref).setChecked(is24Hour());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_TICK");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        getActivity().registerReceiver(this.mIntentReceiver, filter, null, null);
        updateTimeAndDateDisplay(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mIntentReceiver);
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void updateTimeAndDateDisplay(Context context) {
        Calendar now = Calendar.getInstance();
        this.mDummyDate.setTimeZone(now.getTimeZone());
        this.mDummyDate.set(now.get(1), 11, 31, 13, 0, 0);
        Date dummyDate = this.mDummyDate.getTime();
        this.mDatePref.setSummary(DateFormat.getLongDateFormat(context).format(now.getTime()));
        this.mTimePref.setSummary(DateFormat.getTimeFormat(getActivity()).format(now.getTime()));
        this.mTimeZone.setSummary(getTimeZoneText(now.getTimeZone(), true));
        this.mTime24Pref.setSummary(DateFormat.getTimeFormat(getActivity()).format(dummyDate));
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Activity activity = getActivity();
        if (activity != null) {
            setDate(activity, year, month, day);
            updateTimeAndDateDisplay(activity);
        }
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Activity activity = getActivity();
        if (activity != null) {
            setTime(activity, hourOfDay, minute);
            updateTimeAndDateDisplay(activity);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (key.equals("auto_time")) {
            boolean autoEnabled = preferences.getBoolean(key, true);
            Settings.Global.putInt(getContentResolver(), "auto_time", autoEnabled ? 1 : 0);
            this.mTimePref.setEnabled(!autoEnabled);
            this.mDatePref.setEnabled(autoEnabled ? false : true);
            return;
        }
        if (key.equals("auto_zone")) {
            boolean autoZoneEnabled = preferences.getBoolean(key, true);
            Settings.Global.putInt(getContentResolver(), "auto_time_zone", autoZoneEnabled ? 1 : 0);
            this.mTimeZone.setEnabled(autoZoneEnabled ? false : true);
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Calendar calendar = Calendar.getInstance();
        switch (id) {
            case 0:
                DatePickerDialog d = new DatePickerDialog(getActivity(), this, calendar.get(1), calendar.get(2), calendar.get(5));
                configureDatePicker(d.getDatePicker());
                return d;
            case 1:
                return new TimePickerDialog(getActivity(), this, calendar.get(11), calendar.get(12), DateFormat.is24HourFormat(getActivity()));
            default:
                throw new IllegalArgumentException();
        }
    }

    static void configureDatePicker(DatePicker datePicker) {
        Calendar t = Calendar.getInstance();
        t.clear();
        t.set(1970, 0, 1);
        datePicker.setMinDate(t.getTimeInMillis());
        t.clear();
        t.set(2037, 11, 31);
        datePicker.setMaxDate(t.getTimeInMillis());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == this.mDatePref) {
            showDialog(0);
        } else if (preference == this.mTimePref) {
            removeDialog(1);
            showDialog(1);
        } else if (preference == this.mTime24Pref) {
            boolean is24Hour = ((SwitchPreference) this.mTime24Pref).isChecked();
            set24Hour(is24Hour);
            updateTimeAndDateDisplay(getActivity());
            timeUpdated(is24Hour);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateTimeAndDateDisplay(getActivity());
    }

    private void timeUpdated(boolean is24Hour) {
        Intent timeChanged = new Intent("android.intent.action.TIME_SET");
        timeChanged.putExtra("android.intent.extra.TIME_PREF_24_HOUR_FORMAT", is24Hour);
        getActivity().sendBroadcast(timeChanged);
    }

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(getActivity());
    }

    private void set24Hour(boolean is24Hour) {
        Settings.System.putString(getContentResolver(), "time_12_24", is24Hour ? "24" : "12");
    }

    private boolean getAutoState(String name) {
        try {
            return Settings.Global.getInt(getContentResolver(), name) > 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    static void setDate(Context context, int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(1, year);
        c.set(2, month);
        c.set(5, day);
        long when = c.getTimeInMillis();
        if (when / 1000 < 2147483647L) {
            if (when / 1000 < 315532800) {
                Toast.makeText(context, R.string.invalid_date, 0).show();
            } else {
                ((AlarmManager) context.getSystemService("alarm")).setTime(when);
            }
        }
    }

    static void setTime(Context context, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(11, hourOfDay);
        c.set(12, minute);
        c.set(13, 0);
        c.set(14, 0);
        long when = c.getTimeInMillis();
        if (when / 1000 < 2147483647L) {
            ((AlarmManager) context.getSystemService("alarm")).setTime(when);
        }
    }

    public static String getTimeZoneText(TimeZone tz, boolean includeName) {
        Date now = new Date();
        SimpleDateFormat gmtFormatter = new SimpleDateFormat("ZZZZ");
        gmtFormatter.setTimeZone(tz);
        String gmtString = gmtFormatter.format(now);
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        Locale l = Locale.getDefault();
        boolean isRtl = TextUtils.getLayoutDirectionFromLocale(l) == 1;
        String gmtString2 = bidiFormatter.unicodeWrap(gmtString, isRtl ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR);
        if (includeName) {
            SimpleDateFormat zoneNameFormatter = new SimpleDateFormat("zzzz");
            zoneNameFormatter.setTimeZone(tz);
            String zoneNameString = zoneNameFormatter.format(now);
            return gmtString2 + " " + zoneNameString;
        }
        return gmtString2;
    }
}
