package com.android.settings.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.AppListSwitchPreference;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import java.util.Collection;

public class AdvancedWifiSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final boolean WIFI_ACTIVE_ROAMING_SUPPORTED = SystemProperties.getBoolean("ro.wifi.active_roaming.enable", false);
    private IntentFilter mFilter;
    private NetworkScoreManager mNetworkScoreManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.net.wifi.LINK_CONFIGURATION_CHANGED") || action.equals("android.net.wifi.STATE_CHANGE")) {
                AdvancedWifiSettings.this.refreshWifiInfo();
            }
        }
    };
    private AppListSwitchPreference mWifiAssistantPreference;
    private WifiManager mWifiManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_advanced_settings);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mNetworkScoreManager = (NetworkScoreManager) getSystemService("network_score");
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
        getActivity().registerReceiver(this.mReceiver, this.mFilter);
        refreshWifiInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mReceiver);
    }

    private void initPreferences() {
        SwitchPreference notifyOpenNetworks = (SwitchPreference) findPreference("notify_open_networks");
        notifyOpenNetworks.setChecked(Settings.Global.getInt(getContentResolver(), "wifi_networks_available_notification_on", 0) == 1);
        notifyOpenNetworks.setEnabled(this.mWifiManager.isWifiEnabled());
        SwitchPreference scanAlwaysAvailable = (SwitchPreference) findPreference("wifi_scan_always_available");
        scanAlwaysAvailable.setChecked(Settings.Global.getInt(getContentResolver(), "wifi_scan_always_enabled", 0) == 1);
        Intent intent = new Intent("android.credentials.INSTALL_AS_USER");
        intent.setClassName("com.android.certinstaller", "com.android.certinstaller.CertInstallerMain");
        intent.putExtra("install_as_uid", 1010);
        Preference pref = findPreference("install_credentials");
        pref.setIntent(intent);
        Activity context = getActivity();
        this.mWifiAssistantPreference = (AppListSwitchPreference) findPreference("wifi_assistant");
        Collection<NetworkScorerAppManager.NetworkScorerAppData> scorers = NetworkScorerAppManager.getAllValidScorers(context);
        if (UserHandle.myUserId() == 0 && !scorers.isEmpty()) {
            this.mWifiAssistantPreference.setOnPreferenceChangeListener(this);
            initWifiAssistantPreference(scorers);
        } else if (this.mWifiAssistantPreference != null) {
            getPreferenceScreen().removePreference(this.mWifiAssistantPreference);
        }
        Intent wifiDirectIntent = new Intent(context, (Class<?>) Settings.WifiP2pSettingsActivity.class);
        Preference wifiDirectPref = findPreference("wifi_direct");
        wifiDirectPref.setIntent(wifiDirectIntent);
        Preference wpsPushPref = findPreference("wps_push_button");
        wpsPushPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                WpsFragment wpsFragment = new WpsFragment(0);
                wpsFragment.show(AdvancedWifiSettings.this.getFragmentManager(), "wps_push_button");
                return true;
            }
        });
        Preference wpsPinPref = findPreference("wps_pin_entry");
        wpsPinPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                WpsFragment wpsFragment = new WpsFragment(1);
                wpsFragment.show(AdvancedWifiSettings.this.getFragmentManager(), "wps_pin_entry");
                return true;
            }
        });
        ListPreference frequencyPref = (ListPreference) findPreference("frequency_band");
        if (this.mWifiManager.isDualBandSupported()) {
            frequencyPref.setOnPreferenceChangeListener(this);
            int value = this.mWifiManager.getFrequencyBand();
            if (value != -1) {
                frequencyPref.setValue(String.valueOf(value));
                updateFrequencyBandSummary(frequencyPref, value);
            } else {
                Log.e("AdvancedWifiSettings", "Failed to fetch frequency band");
            }
        } else if (frequencyPref != null) {
            getPreferenceScreen().removePreference(frequencyPref);
        }
        ListPreference sleepPolicyPref = (ListPreference) findPreference("sleep_policy");
        if (sleepPolicyPref != null) {
            if (Utils.isWifiOnly(context)) {
                sleepPolicyPref.setEntries(R.array.wifi_sleep_policy_entries_wifi_only);
            }
            sleepPolicyPref.setOnPreferenceChangeListener(this);
            String stringValue = String.valueOf(Settings.Global.getInt(getContentResolver(), "wifi_sleep_policy", 2));
            sleepPolicyPref.setValue(stringValue);
            updateSleepPolicySummary(sleepPolicyPref, stringValue);
        }
        Preference wapiCertMgmtPref = findPreference("wapi_cert_mgmt");
        if (wapiCertMgmtPref != null) {
            wapiCertMgmtPref.setOnPreferenceClickListener(this);
        }
        CheckBoxPreference activeRoamingCheckBox = (CheckBoxPreference) findPreference("enable_active_roaming");
        if (activeRoamingCheckBox != null) {
            if (!WIFI_ACTIVE_ROAMING_SUPPORTED) {
                getPreferenceScreen().removePreference(activeRoamingCheckBox);
                return;
            }
            activeRoamingCheckBox.setChecked(Settings.System.getInt(getContentResolver(), "wifi_active_roaming", 0) == 1);
            activeRoamingCheckBox.setEnabled(this.mWifiManager.isWifiEnabled());
            if (!this.mWifiManager.isWifiEnabled()) {
                activeRoamingCheckBox.setSummary(getActivity().getString(R.string.status_wifi_disabled));
            }
            activeRoamingCheckBox.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (key == null) {
            return false;
        }
        if (key.equals("wapi_cert_mgmt")) {
            Intent CertQuery = new Intent();
            ComponentName comp = new ComponentName("com.android.wapi", "com.android.wapi.WapiCertMgmt");
            CertQuery.setComponent(comp);
            try {
                startActivity(CertQuery);
            } catch (ActivityNotFoundException e) {
                new AlertDialog.Builder(preference.getContext()).setTitle(R.string.error_title).setIcon(android.R.drawable.ic_dialog_alert).setMessage(R.string.wifi_wapi_cert_mgmt_dont_exist).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
                return false;
            } catch (Exception e2) {
                new AlertDialog.Builder(preference.getContext()).setTitle(R.string.error_title).setIcon(android.R.drawable.ic_dialog_alert).setMessage(e2.toString()).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
                return false;
            }
        }
        return true;
    }

    private void initWifiAssistantPreference(Collection<NetworkScorerAppManager.NetworkScorerAppData> scorers) {
        int count = scorers.size();
        String[] packageNames = new String[count];
        int i = 0;
        for (NetworkScorerAppManager.NetworkScorerAppData scorer : scorers) {
            packageNames[i] = scorer.mPackageName;
            i++;
        }
        this.mWifiAssistantPreference.setPackageNames(packageNames, this.mNetworkScoreManager.getActiveScorerPackage());
    }

    private void updateSleepPolicySummary(Preference sleepPolicyPref, String value) {
        if (value != null) {
            String[] values = getResources().getStringArray(R.array.wifi_sleep_policy_values);
            int summaryArrayResId = Utils.isWifiOnly(getActivity()) ? R.array.wifi_sleep_policy_entries_wifi_only : R.array.wifi_sleep_policy_entries;
            String[] summaries = getResources().getStringArray(summaryArrayResId);
            for (int i = 0; i < values.length; i++) {
                if (value.equals(values[i]) && i < summaries.length) {
                    sleepPolicyPref.setSummary(summaries[i]);
                    return;
                }
            }
        }
        sleepPolicyPref.setSummary("");
        Log.e("AdvancedWifiSettings", "Invalid sleep policy value: " + value);
    }

    private void updateFrequencyBandSummary(Preference frequencyBandPref, int index) {
        String[] summaries = getResources().getStringArray(R.array.wifi_frequency_band_entries);
        frequencyBandPref.setSummary(summaries[index]);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if ("notify_open_networks".equals(key)) {
            Settings.Global.putInt(getContentResolver(), "wifi_networks_available_notification_on", ((SwitchPreference) preference).isChecked() ? 1 : 0);
            return true;
        }
        if ("wifi_scan_always_available".equals(key)) {
            Settings.Global.putInt(getContentResolver(), "wifi_scan_always_enabled", ((SwitchPreference) preference).isChecked() ? 1 : 0);
            return true;
        }
        if ("enable_active_roaming".equals(key)) {
            int i = ((CheckBoxPreference) preference).isChecked() ? 1 : 0;
            int i2 = Settings.System.getInt(getContentResolver(), "wifi_active_roaming", 0);
            Log.d("AdvancedWifiSettings", "newActiveRoam = " + i + "; oldActiveRoam = " + i2);
            if (i == i2) {
                return true;
            }
            if (this.mWifiManager.enableActiveRoaming(i == 1)) {
                Settings.System.putInt(getContentResolver(), "wifi_active_roaming", i);
                return true;
            }
            Log.e("AdvancedWifiSettings", "Fail to set ActiveRoaming: " + i + ";Reset the checkBox!");
            ((CheckBoxPreference) preference).setChecked(i2 == 1);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Context context = getActivity();
        String key = preference.getKey();
        if ("frequency_band".equals(key)) {
            try {
                int value = Integer.parseInt((String) newValue);
                this.mWifiManager.setFrequencyBand(value, true);
                updateFrequencyBandSummary(preference, value);
            } catch (NumberFormatException e) {
                Toast.makeText(context, R.string.wifi_setting_frequency_band_error, 0).show();
                return false;
            }
        } else if ("wifi_assistant".equals(key)) {
            NetworkScorerAppManager.NetworkScorerAppData wifiAssistant = NetworkScorerAppManager.getScorer(context, (String) newValue);
            if (wifiAssistant == null) {
                this.mNetworkScoreManager.setActiveScorer((String) null);
                return true;
            }
            Intent intent = new Intent();
            if (wifiAssistant.mConfigurationActivityClassName != null) {
                intent.setClassName(wifiAssistant.mPackageName, wifiAssistant.mConfigurationActivityClassName);
            } else {
                intent.setAction("android.net.scoring.CHANGE_ACTIVE");
                intent.putExtra("packageName", wifiAssistant.mPackageName);
            }
            startActivity(intent);
            return false;
        }
        if ("sleep_policy".equals(key)) {
            try {
                String stringValue = (String) newValue;
                Settings.Global.putInt(getContentResolver(), "wifi_sleep_policy", Integer.parseInt(stringValue));
                updateSleepPolicySummary(preference, stringValue);
            } catch (NumberFormatException e2) {
                Toast.makeText(context, R.string.wifi_setting_sleep_policy_error, 0).show();
                return false;
            }
        }
        return true;
    }

    public void refreshWifiInfo() {
        Context context = getActivity();
        android.net.wifi.WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        Preference wifiMacAddressPref = findPreference("mac_address");
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        if (TextUtils.isEmpty(macAddress)) {
            macAddress = context.getString(R.string.status_unavailable);
        }
        wifiMacAddressPref.setSummary(macAddress);
        wifiMacAddressPref.setSelectable(false);
        Preference wifiIpAddressPref = findPreference("current_ip_address");
        String ipAddress = Utils.getWifiIpAddresses(context);
        if (-1 == wifiInfo.getNetworkId()) {
            ipAddress = null;
        }
        if (ipAddress == null) {
            ipAddress = context.getString(R.string.status_unavailable);
        }
        wifiIpAddressPref.setSummary(ipAddress);
        wifiIpAddressPref.setSelectable(false);
    }

    public static class WpsFragment extends DialogFragment {
        private static int mWpsSetup;

        public WpsFragment() {
        }

        public WpsFragment(int wpsSetup) {
            mWpsSetup = wpsSetup;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new WpsDialog(getActivity(), mWpsSetup);
        }
    }
}
