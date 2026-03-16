package com.android.deskclock;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static CharSequence[][] mTimezones;
    private long mTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(4);
        addPreferencesFromResource(R.xml.settings);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(4, 4);
        }
        ListPreference listPref = (ListPreference) findPreference("home_time_zone");
        if (mTimezones == null) {
            this.mTime = System.currentTimeMillis();
            mTimezones = getAllTimezones();
        }
        listPref.setEntryValues(mTimezones[0]);
        listPref.setEntries(mTimezones[1]);
        listPref.setSummary(listPref.getEntry());
        listPref.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setBackgroundColor(Utils.getCurrentHourColor());
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if ("auto_silence".equals(pref.getKey())) {
            String delay = (String) newValue;
            updateAutoSnoozeSummary((ListPreference) pref, delay);
        } else if ("clock_style".equals(pref.getKey())) {
            ListPreference listPref = (ListPreference) pref;
            int idx = listPref.findIndexOfValue((String) newValue);
            listPref.setSummary(listPref.getEntries()[idx]);
        } else if ("home_time_zone".equals(pref.getKey())) {
            ListPreference listPref2 = (ListPreference) pref;
            int idx2 = listPref2.findIndexOfValue((String) newValue);
            listPref2.setSummary(listPref2.getEntries()[idx2]);
            notifyHomeTimeZoneChanged();
        } else if ("automatic_home_clock".equals(pref.getKey())) {
            boolean state = ((CheckBoxPreference) pref).isChecked();
            Preference homeTimeZone = findPreference("home_time_zone");
            homeTimeZone.setEnabled(!state);
            notifyHomeTimeZoneChanged();
        } else if ("volume_button_setting".equals(pref.getKey())) {
            ListPreference listPref3 = (ListPreference) pref;
            int idx3 = listPref3.findIndexOfValue((String) newValue);
            listPref3.setSummary(listPref3.getEntries()[idx3]);
        }
        return true;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return false;
    }

    private void updateAutoSnoozeSummary(ListPreference listPref, String delay) {
        int i = Integer.parseInt(delay);
        if (i == -1) {
            listPref.setSummary(R.string.auto_silence_never);
        } else {
            listPref.setSummary(getString(R.string.auto_silence_summary, new Object[]{Integer.valueOf(i)}));
        }
    }

    private void notifyHomeTimeZoneChanged() {
        Intent i = new Intent("com.android.deskclock.worldclock.update");
        sendBroadcast(i);
    }

    private void refresh() {
        ListPreference listPref = (ListPreference) findPreference("auto_silence");
        String delay = listPref.getValue();
        updateAutoSnoozeSummary(listPref, delay);
        listPref.setOnPreferenceChangeListener(this);
        ListPreference listPref2 = (ListPreference) findPreference("clock_style");
        listPref2.setSummary(listPref2.getEntry());
        listPref2.setOnPreferenceChangeListener(this);
        Preference pref = findPreference("automatic_home_clock");
        boolean state = ((CheckBoxPreference) pref).isChecked();
        pref.setOnPreferenceChangeListener(this);
        ListPreference listPref3 = (ListPreference) findPreference("home_time_zone");
        listPref3.setEnabled(state);
        listPref3.setSummary(listPref3.getEntry());
        ListPreference listPref4 = (ListPreference) findPreference("volume_button_setting");
        listPref4.setSummary(listPref4.getEntry());
        listPref4.setOnPreferenceChangeListener(this);
        SnoozeLengthDialog snoozePref = (SnoozeLengthDialog) findPreference("snooze_duration");
        snoozePref.setSummary();
    }

    private class TimeZoneRow implements Comparable<TimeZoneRow> {
        public final String mDisplayName;
        public final String mId;
        public final int mOffset;

        public TimeZoneRow(String id, String name) {
            this.mId = id;
            TimeZone tz = TimeZone.getTimeZone(id);
            boolean useDaylightTime = tz.useDaylightTime();
            this.mOffset = tz.getOffset(SettingsActivity.this.mTime);
            this.mDisplayName = buildGmtDisplayName(id, name, useDaylightTime);
        }

        @Override
        public int compareTo(TimeZoneRow another) {
            return this.mOffset - another.mOffset;
        }

        public String buildGmtDisplayName(String id, String displayName, boolean useDaylightTime) {
            int p = Math.abs(this.mOffset);
            StringBuilder name = new StringBuilder("(GMT");
            name.append(this.mOffset < 0 ? '-' : '+');
            name.append(((long) p) / 3600000);
            name.append(':');
            int min = (p / 60000) % 60;
            if (min < 10) {
                name.append('0');
            }
            name.append(min);
            name.append(") ");
            name.append(displayName);
            if (useDaylightTime) {
            }
            return name.toString();
        }
    }

    public CharSequence[][] getAllTimezones() {
        Resources resources = getResources();
        String[] ids = resources.getStringArray(R.array.timezone_values);
        String[] labels = resources.getStringArray(R.array.timezone_labels);
        int minLength = ids.length;
        if (ids.length != labels.length) {
            minLength = Math.min(minLength, labels.length);
            LogUtils.e("Timezone ids and labels have different length!", new Object[0]);
        }
        List<TimeZoneRow> timezones = new ArrayList<>();
        for (int i = 0; i < minLength; i++) {
            timezones.add(new TimeZoneRow(ids[i], labels[i]));
        }
        Collections.sort(timezones);
        CharSequence[][] timeZones = (CharSequence[][]) Array.newInstance((Class<?>) CharSequence.class, 2, timezones.size());
        int i2 = 0;
        for (TimeZoneRow row : timezones) {
            timeZones[0][i2] = row.mId;
            timeZones[1][i2] = row.mDisplayName;
            i2++;
        }
        return timeZones;
    }
}
