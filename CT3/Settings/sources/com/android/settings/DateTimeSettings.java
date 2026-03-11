package com.android.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.TimePicker;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.datetime.ZoneGetter;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.Calendar;
import java.util.Date;

public class DateTimeSettings extends SettingsPreferenceFragment implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener, Preference.OnPreferenceChangeListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    private com.mediatek.settingslib.RestrictedListPreference mAutoTimePref;
    private RestrictedSwitchPreference mAutoTimePrefDef;
    private SwitchPreference mAutoTimeZonePref;
    private Preference mDatePref;
    private Calendar mDummyDate;
    private Preference mTime24Pref;
    private Preference mTimePref;
    private Preference mTimeZone;
    private LocationManager mLocationManager = null;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Activity activity = DateTimeSettings.this.getActivity();
            if (activity == null) {
                return;
            }
            DateTimeSettings.this.updateTimeAndDateDisplay(activity);
        }
    };

    @Override
    protected int getMetricsCategory() {
        return 38;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.date_time_prefs);
        initUI();
    }

    private void initUI() {
        int index;
        boolean autoTimeEnabled = getAutoState("auto_time");
        boolean autoTimeZoneEnabled = getAutoState("auto_time_zone");
        this.mLocationManager = (LocationManager) getSystemService("location");
        this.mAutoTimePrefDef = null;
        this.mAutoTimePref = null;
        if (this.mLocationManager.getProvider("gps") == null) {
            this.mAutoTimePrefDef = (RestrictedSwitchPreference) findPreference("auto_time");
            this.mAutoTimePrefDef.setOnPreferenceChangeListener(this);
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfAutoTimeRequired(getActivity());
            this.mAutoTimePrefDef.setDisabledByAdmin(admin);
            this.mAutoTimePrefDef.setChecked(autoTimeEnabled);
            this.mAutoTimePref = (com.mediatek.settingslib.RestrictedListPreference) findPreference("auto_time_list");
            getPreferenceScreen().removePreference(this.mAutoTimePref);
        } else {
            this.mAutoTimePref = (com.mediatek.settingslib.RestrictedListPreference) findPreference("auto_time_list");
            this.mAutoTimePref.setOnPreferenceChangeListener(this);
            RestrictedLockUtils.EnforcedAdmin admin2 = RestrictedLockUtils.checkIfAutoTimeRequired(getActivity());
            this.mAutoTimePref.setDisabledByAdmin(admin2);
            this.mAutoTimePrefDef = (RestrictedSwitchPreference) findPreference("auto_time");
            getPreferenceScreen().removePreference(this.mAutoTimePrefDef);
        }
        Intent intent = getActivity().getIntent();
        boolean isFirstRun = intent.getBooleanExtra("firstRun", false);
        boolean autoTimeGpsEnabled = getAutoState("auto_time_gps");
        this.mDummyDate = Calendar.getInstance();
        if (this.mLocationManager.getProvider("gps") != null) {
            if (autoTimeEnabled) {
                index = 0;
            } else if (autoTimeGpsEnabled) {
                index = 1;
            } else {
                index = 2;
            }
            this.mAutoTimePref.setValueIndex(index);
            this.mAutoTimePref.setSummary(this.mAutoTimePref.getEntries()[index]);
        }
        this.mAutoTimeZonePref = (SwitchPreference) findPreference("auto_zone");
        this.mAutoTimeZonePref.setOnPreferenceChangeListener(this);
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
        boolean z = !autoTimeEnabled ? autoTimeGpsEnabled : true;
        this.mTimePref.setEnabled(!z);
        this.mDatePref.setEnabled(!z);
        this.mTimeZone.setEnabled(autoTimeZoneEnabled ? false : true);
    }

    @Override
    public void onResume() {
        super.onResume();
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
    }

    public void updateTimeAndDateDisplay(Context context) {
        Calendar now = Calendar.getInstance();
        this.mDummyDate.setTimeZone(now.getTimeZone());
        this.mDummyDate.set(now.get(1), 11, 31, 13, 0, 0);
        Date dummyDate = this.mDummyDate.getTime();
        this.mDatePref.setSummary(DateFormat.getLongDateFormat(context).format(now.getTime()));
        this.mTimePref.setSummary(DateFormat.getTimeFormat(getActivity()).format(now.getTime()));
        this.mTimeZone.setSummary(ZoneGetter.getTimeZoneOffsetAndName(now.getTimeZone(), now.getTime()));
        this.mTime24Pref.setSummary(DateFormat.getTimeFormat(getActivity()).format(dummyDate));
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        setDate(activity, year, month, day);
        updateTimeAndDateDisplay(activity);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        setTime(activity, hourOfDay, minute);
        updateTimeAndDateDisplay(activity);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals("auto_time")) {
            boolean autoEnabled = ((Boolean) newValue).booleanValue();
            Settings.Global.putInt(getContentResolver(), "auto_time", autoEnabled ? 1 : 0);
            this.mTimePref.setEnabled(!autoEnabled);
            this.mDatePref.setEnabled(autoEnabled ? false : true);
        } else if (preference.getKey().equals("auto_time_list")) {
            int index = Integer.parseInt(newValue.toString());
            boolean autoEnabled2 = true;
            if (index == 0) {
                Settings.Global.putInt(getContentResolver(), "auto_time", 1);
                Settings.Global.putInt(getContentResolver(), "auto_time_gps", 0);
            } else if (index == 1) {
                showDialog(2);
                setOnCancelListener(this);
            } else {
                Settings.Global.putInt(getContentResolver(), "auto_time", 0);
                Settings.Global.putInt(getContentResolver(), "auto_time_gps", 0);
                autoEnabled2 = false;
            }
            this.mAutoTimePref.setSummary(this.mAutoTimePref.getEntries()[index]);
            this.mTimePref.setEnabled(!autoEnabled2);
            this.mDatePref.setEnabled(autoEnabled2 ? false : true);
        } else if (preference.getKey().equals("auto_zone")) {
            boolean autoZoneEnabled = ((Boolean) newValue).booleanValue();
            Settings.Global.putInt(getContentResolver(), "auto_time_zone", autoZoneEnabled ? 1 : 0);
            this.mTimeZone.setEnabled(autoZoneEnabled ? false : true);
        }
        return true;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Calendar calendar = Calendar.getInstance();
        switch (id) {
            case DefaultWfcSettingsExt.RESUME:
                DatePickerDialog d = new DatePickerDialog(getActivity(), this, calendar.get(1), calendar.get(2), calendar.get(5));
                configureDatePicker(d.getDatePicker());
                return d;
            case DefaultWfcSettingsExt.PAUSE:
                return new TimePickerDialog(getActivity(), this, calendar.get(11), calendar.get(12), DateFormat.is24HourFormat(getActivity()));
            case DefaultWfcSettingsExt.CREATE:
                int msg = Settings.Secure.isLocationProviderEnabled(getContentResolver(), "gps") ? R.string.gps_time_sync_attention_gps_on : R.string.gps_time_sync_attention_gps_off;
                return new AlertDialog.Builder(getActivity()).setMessage(getActivity().getResources().getString(msg)).setTitle(R.string.proxy_error).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).create();
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
    public boolean onPreferenceTreeClick(Preference preference) {
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
        return super.onPreferenceTreeClick(preference);
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
        long when = Math.max(c.getTimeInMillis(), 1194220800000L);
        if (when / 1000 >= 2147483647L) {
            return;
        }
        ((AlarmManager) context.getSystemService("alarm")).setTime(when);
    }

    static void setTime(Context context, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(11, hourOfDay);
        c.set(12, minute);
        c.set(13, 0);
        c.set(14, 0);
        long when = Math.max(c.getTimeInMillis(), 1194220800000L);
        if (when / 1000 >= 2147483647L) {
            return;
        }
        ((AlarmManager) context.getSystemService("alarm")).setTime(when);
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            Calendar now = Calendar.getInstance();
            this.mSummaryLoader.setSummary(this, ZoneGetter.getTimeZoneOffsetAndName(now.getTimeZone(), now.getTime()));
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            Log.d("DateTimeSettings", "Enable GPS time sync");
            boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(getContentResolver(), "gps");
            if (!gpsEnabled) {
                Settings.Secure.setLocationProviderEnabled(getContentResolver(), "gps", true);
            }
            Settings.Global.putInt(getContentResolver(), "auto_time", 0);
            Settings.Global.putInt(getContentResolver(), "auto_time_gps", 1);
            this.mAutoTimePref.setValueIndex(1);
            this.mAutoTimePref.setSummary(this.mAutoTimePref.getEntries()[1]);
            return;
        }
        if (which != -2) {
            return;
        }
        Log.d("DateTimeSettings", "DialogInterface.BUTTON_NEGATIVE");
        reSetAutoTimePref();
    }

    private void reSetAutoTimePref() {
        int index;
        Log.d("DateTimeSettings", "reset AutoTimePref as cancel the selection");
        boolean autoTimeEnabled = getAutoState("auto_time");
        boolean autoTimeGpsEnabled = getAutoState("auto_time_gps");
        if (autoTimeEnabled) {
            index = 0;
        } else if (autoTimeGpsEnabled) {
            index = 1;
        } else {
            index = 2;
        }
        this.mAutoTimePref.setValueIndex(index);
        this.mAutoTimePref.setSummary(this.mAutoTimePref.getEntries()[index]);
        boolean z = !autoTimeEnabled ? autoTimeGpsEnabled : true;
        Log.d("DateTimeSettings", "reset AutoTimePref as cancel the selection autoEnabled: " + z);
        this.mTimePref.setEnabled(!z);
        this.mDatePref.setEnabled(z ? false : true);
    }

    @Override
    public void onCancel(DialogInterface arg0) {
        Log.d("DateTimeSettings", "onCancel Dialog");
        reSetAutoTimePref();
    }
}
