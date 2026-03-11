package com.android.settings.wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.AppListSwitchPreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.wifi.ConfigureWifiSettingsExt;
import java.util.Collection;
import java.util.List;

public class ConfigureWifiSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private ConfigureWifiSettingsExt mConfigureWifiSettingsExt;
    private ISettingsMiscExt mExt;
    private IntentFilter mFilter;
    private NetworkScoreManager mNetworkScoreManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals("android.net.wifi.LINK_CONFIGURATION_CHANGED") && !action.equals("android.net.wifi.STATE_CHANGE")) {
                return;
            }
            ConfigureWifiSettings.this.refreshWifiInfo();
        }
    };
    private AppListSwitchPreference mWifiAssistantPreference;
    private WifiManager mWifiManager;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.wifi_configure_settings);
        this.mConfigureWifiSettingsExt = new ConfigureWifiSettingsExt(this);
        this.mConfigureWifiSettingsExt.onCreate();
        this.mExt = UtilsExt.getMiscPlugin(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mNetworkScoreManager = (NetworkScoreManager) getSystemService("network_score");
        this.mConfigureWifiSettingsExt.onActivityCreated(getContentResolver());
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
        getActivity().registerReceiver(this.mReceiver, this.mFilter);
        this.mConfigureWifiSettingsExt.onResume();
        refreshWifiInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mReceiver);
        this.mConfigureWifiSettingsExt.onPause();
    }

    private void initPreferences() {
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs == null || configs.size() == 0) {
            removePreference("saved_networks");
        }
        SwitchPreference notifyOpenNetworks = (SwitchPreference) findPreference("notify_open_networks");
        notifyOpenNetworks.setChecked(Settings.Global.getInt(getContentResolver(), "wifi_networks_available_notification_on", 0) == 1);
        notifyOpenNetworks.setEnabled(this.mWifiManager.isWifiEnabled());
        Activity context = getActivity();
        this.mWifiAssistantPreference = (AppListSwitchPreference) findPreference("wifi_assistant");
        Collection<NetworkScorerAppManager.NetworkScorerAppData> scorers = NetworkScorerAppManager.getAllValidScorers(context);
        if (UserManager.get(context).isAdminUser() && !scorers.isEmpty()) {
            this.mWifiAssistantPreference.setOnPreferenceChangeListener(this);
            initWifiAssistantPreference(scorers);
        } else if (this.mWifiAssistantPreference != null) {
            getPreferenceScreen().removePreference(this.mWifiAssistantPreference);
        }
        ListPreference sleepPolicyPref = (ListPreference) findPreference("sleep_policy");
        if (sleepPolicyPref == null) {
            return;
        }
        if (Utils.isWifiOnly(context)) {
            sleepPolicyPref.setEntries(R.array.wifi_sleep_policy_entries_wifi_only);
        }
        sleepPolicyPref.setOnPreferenceChangeListener(this);
        int value = Settings.Global.getInt(getContentResolver(), "wifi_sleep_policy", 2);
        String stringValue = String.valueOf(value);
        sleepPolicyPref.setValue(stringValue);
        updateSleepPolicySummary(sleepPolicyPref, stringValue);
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
        Log.e("ConfigureWifiSettings", "Invalid sleep policy value: " + value);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if ("notify_open_networks".equals(key)) {
            Settings.Global.putInt(getContentResolver(), "wifi_networks_available_notification_on", ((SwitchPreference) preference).isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Context context = getActivity();
        String key = preference.getKey();
        if ("wifi_assistant".equals(key)) {
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
            } catch (NumberFormatException e) {
                Toast.makeText(context, R.string.wifi_setting_sleep_policy_error, 0).show();
                return false;
            }
        }
        return true;
    }

    public void refreshWifiInfo() {
        String string;
        Context context = getActivity();
        android.net.wifi.WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        Preference wifiMacAddressPref = findPreference("mac_address");
        String macAddress = wifiInfo != null ? wifiInfo.getMacAddress() : null;
        if (!TextUtils.isEmpty(macAddress)) {
            string = this.mExt.customizeMacAddressString(macAddress, context.getString(R.string.status_unavailable));
        } else {
            string = context.getString(R.string.status_unavailable);
        }
        wifiMacAddressPref.setSummary(string);
        wifiMacAddressPref.setSelectable(false);
        if (FeatureOption.MTK_DHCPV6C_WIFI) {
            this.mConfigureWifiSettingsExt.refreshWifiInfo();
            return;
        }
        Preference wifiIpAddressPref = findPreference("current_ip_address");
        String ipAddress = Utils.getWifiIpAddresses(context);
        if (ipAddress == null) {
            ipAddress = context.getString(R.string.status_unavailable);
        }
        wifiIpAddressPref.setSummary(ipAddress);
        wifiIpAddressPref.setSelectable(false);
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

    @Override
    protected int getMetricsCategory() {
        return 338;
    }
}
