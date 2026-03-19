package com.android.deskclock.settings;

import android.content.Intent;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.ListPreferenceDialogFragmentCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.TwoStatePreference;
import android.view.Menu;
import android.view.MenuItem;
import com.android.deskclock.BaseActivity;
import com.android.deskclock.DropShadowController;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NavUpMenuItemController;
import com.android.deskclock.actionbarmenu.OptionsMenuManager;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.TimeZones;
import com.android.deskclock.ringtone.RingtonePickerActivity;

public final class SettingsActivity extends BaseActivity {
    public static final String DEFAULT_VOLUME_BEHAVIOR = "0";
    public static final String KEY_ALARM_CRESCENDO = "alarm_crescendo_duration";
    public static final String KEY_ALARM_SNOOZE = "snooze_duration";
    public static final String KEY_AUTO_HOME_CLOCK = "automatic_home_clock";
    public static final String KEY_AUTO_SILENCE = "auto_silence";
    public static final String KEY_CLOCK_DISPLAY_SECONDS = "display_clock_seconds";
    public static final String KEY_CLOCK_STYLE = "clock_style";
    public static final String KEY_DATE_TIME = "date_time";
    public static final String KEY_HOME_TZ = "home_time_zone";
    public static final String KEY_TIMER_CRESCENDO = "timer_crescendo_duration";
    public static final String KEY_TIMER_RINGTONE = "timer_ringtone";
    public static final String KEY_TIMER_VIBRATE = "timer_vibrate";
    public static final String KEY_VOLUME_BUTTONS = "volume_button_setting";
    public static final String KEY_WEEK_START = "week_start";
    public static final String PREFERENCE_DIALOG_FRAGMENT_TAG = "preference_dialog";
    public static final String PREFS_FRAGMENT_TAG = "prefs_fragment";
    public static final String VOLUME_BEHAVIOR_DISMISS = "2";
    public static final String VOLUME_BEHAVIOR_SNOOZE = "1";
    private DropShadowController mDropShadowController;
    private final OptionsMenuManager mOptionsMenuManager = new OptionsMenuManager();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.settings);
        this.mOptionsMenuManager.addMenuItemController(new NavUpMenuItemController(this)).addMenuItemController(MenuItemControllerFactory.getInstance().buildMenuItemControllers(this));
        if (bundle == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.main, new PrefsFragment(), PREFS_FRAGMENT_TAG).disallowAddToBackStack().commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mDropShadowController = new DropShadowController(findViewById(R.id.drop_shadow), ((PrefsFragment) getSupportFragmentManager().findFragmentById(R.id.main)).getListView());
    }

    @Override
    protected void onPause() {
        this.mDropShadowController.stop();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.mOptionsMenuManager.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return this.mOptionsMenuManager.onOptionsItemSelected(menuItem) || super.onOptionsItemSelected(menuItem);
    }

    public static class PrefsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
        @Override
        public void onCreatePreferences(Bundle bundle, String str) {
            getPreferenceManager().setStorageDeviceProtected();
            addPreferencesFromResource(R.xml.settings);
            Preference preferenceFindPreference = findPreference(SettingsActivity.KEY_TIMER_VIBRATE);
            preferenceFindPreference.setVisible(((Vibrator) preferenceFindPreference.getContext().getSystemService("vibrator")).hasVibrator());
            loadTimeZoneList();
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);
            getActivity().setResult(0);
        }

        @Override
        public void onResume() {
            super.onResume();
            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object obj) {
            switch (preference.getKey()) {
                case "alarm_crescendo_duration":
                case "home_time_zone":
                case "snooze_duration":
                case "timer_crescendo_duration":
                    ListPreference listPreference = (ListPreference) preference;
                    listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue((String) obj)]);
                    break;
                case "clock_style":
                case "week_start":
                case "volume_button_setting":
                    SimpleMenuPreference simpleMenuPreference = (SimpleMenuPreference) preference;
                    preference.setSummary(simpleMenuPreference.getEntries()[simpleMenuPreference.findIndexOfValue((String) obj)]);
                    break;
                case "display_clock_seconds":
                    DataModel.getDataModel().setDisplayClockSeconds(((Boolean) obj).booleanValue());
                    break;
                case "auto_silence":
                    updateAutoSnoozeSummary((ListPreference) preference, (String) obj);
                    break;
                case "automatic_home_clock":
                    findPreference(SettingsActivity.KEY_HOME_TZ).setEnabled(!((TwoStatePreference) preference).isChecked());
                    break;
                case "timer_vibrate":
                    DataModel.getDataModel().setTimerVibrate(((TwoStatePreference) preference).isChecked());
                    break;
                case "timer_ringtone":
                    preference.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());
                    break;
            }
            getActivity().setResult(-1);
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            FragmentActivity activity = getActivity();
            if (activity == null) {
                return false;
            }
            String key = preference.getKey();
            byte b = -1;
            int iHashCode = key.hashCode();
            if (iHashCode != -1249918116) {
                if (iHashCode == -248858434 && key.equals(SettingsActivity.KEY_DATE_TIME)) {
                    b = 0;
                }
            } else if (key.equals(SettingsActivity.KEY_TIMER_RINGTONE)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    if (BenesseExtension.getDchaState() == 0) {
                        Intent intent = new Intent("android.settings.DATE_SETTINGS");
                        intent.addFlags(268435456);
                        startActivity(intent);
                        break;
                    }
                    break;
                case 1:
                    startActivity(RingtonePickerActivity.createTimerRingtonePickerIntent(activity));
                    break;
            }
            return false;
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            if (!(preference instanceof ListPreference)) {
                throw new IllegalArgumentException("Unsupported DialogPreference type");
            }
            showDialog(ListPreferenceDialogFragmentCompat.newInstance(preference.getKey()));
        }

        private void showDialog(PreferenceDialogFragmentCompat preferenceDialogFragmentCompat) {
            if (getFragmentManager().findFragmentByTag(SettingsActivity.PREFERENCE_DIALOG_FRAGMENT_TAG) != null) {
                return;
            }
            preferenceDialogFragmentCompat.setTargetFragment(this, 0);
            preferenceDialogFragmentCompat.show(getFragmentManager(), SettingsActivity.PREFERENCE_DIALOG_FRAGMENT_TAG);
        }

        private void loadTimeZoneList() {
            TimeZones timeZones = DataModel.getDataModel().getTimeZones();
            ListPreference listPreference = (ListPreference) findPreference(SettingsActivity.KEY_HOME_TZ);
            listPreference.setEntryValues(timeZones.getTimeZoneIds());
            listPreference.setEntries(timeZones.getTimeZoneNames());
            listPreference.setSummary(listPreference.getEntry());
            listPreference.setOnPreferenceChangeListener(this);
        }

        private void refresh() {
            ListPreference listPreference = (ListPreference) findPreference(SettingsActivity.KEY_AUTO_SILENCE);
            updateAutoSnoozeSummary(listPreference, listPreference.getValue());
            listPreference.setOnPreferenceChangeListener(this);
            SimpleMenuPreference simpleMenuPreference = (SimpleMenuPreference) findPreference(SettingsActivity.KEY_CLOCK_STYLE);
            simpleMenuPreference.setSummary(simpleMenuPreference.getEntry());
            simpleMenuPreference.setOnPreferenceChangeListener(this);
            SimpleMenuPreference simpleMenuPreference2 = (SimpleMenuPreference) findPreference(SettingsActivity.KEY_VOLUME_BUTTONS);
            simpleMenuPreference2.setSummary(simpleMenuPreference2.getEntry());
            simpleMenuPreference2.setOnPreferenceChangeListener(this);
            findPreference(SettingsActivity.KEY_CLOCK_DISPLAY_SECONDS).setOnPreferenceChangeListener(this);
            Preference preferenceFindPreference = findPreference(SettingsActivity.KEY_AUTO_HOME_CLOCK);
            boolean zIsChecked = ((TwoStatePreference) preferenceFindPreference).isChecked();
            preferenceFindPreference.setOnPreferenceChangeListener(this);
            ListPreference listPreference2 = (ListPreference) findPreference(SettingsActivity.KEY_HOME_TZ);
            listPreference2.setEnabled(zIsChecked);
            refreshListPreference(listPreference2);
            refreshListPreference((ListPreference) findPreference(SettingsActivity.KEY_ALARM_CRESCENDO));
            refreshListPreference((ListPreference) findPreference(SettingsActivity.KEY_TIMER_CRESCENDO));
            refreshListPreference((ListPreference) findPreference(SettingsActivity.KEY_ALARM_SNOOZE));
            findPreference(SettingsActivity.KEY_DATE_TIME).setOnPreferenceClickListener(this);
            SimpleMenuPreference simpleMenuPreference3 = (SimpleMenuPreference) findPreference(SettingsActivity.KEY_WEEK_START);
            int iFindIndexOfValue = simpleMenuPreference3.findIndexOfValue(String.valueOf(DataModel.getDataModel().getWeekdayOrder().getCalendarDays().get(0)));
            simpleMenuPreference3.setValueIndex(iFindIndexOfValue);
            simpleMenuPreference3.setSummary(simpleMenuPreference3.getEntries()[iFindIndexOfValue]);
            simpleMenuPreference3.setOnPreferenceChangeListener(this);
            Preference preferenceFindPreference2 = findPreference(SettingsActivity.KEY_TIMER_RINGTONE);
            preferenceFindPreference2.setOnPreferenceClickListener(this);
            preferenceFindPreference2.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());
        }

        private void refreshListPreference(ListPreference listPreference) {
            listPreference.setSummary(listPreference.getEntry());
            listPreference.setOnPreferenceChangeListener(this);
        }

        private void updateAutoSnoozeSummary(ListPreference listPreference, String str) {
            int i = Integer.parseInt(str);
            if (i == -1) {
                listPreference.setSummary(R.string.auto_silence_never);
            } else {
                listPreference.setSummary(Utils.getNumberFormattedQuantityString(getActivity(), R.plurals.auto_silence_summary, i));
            }
        }
    }
}
