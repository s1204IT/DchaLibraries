package com.mediatek.wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiExt;

public class ConfigureWifiSettingsExt {
    private Activity mActivity;
    private IWifiExt mExt;
    private SettingsPreferenceFragment mFragment;
    private IntentFilter mIntentFilter;
    private Preference mIpAddressPref;
    private Preference mIpv6AddressPref;
    private Preference mMacAddressPref;
    private SwitchPreference mNotifyOpenNetworks;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                return;
            }
            int state = intent.getIntExtra("wifi_state", 4);
            if (state == 3) {
                ConfigureWifiSettingsExt.this.mNotifyOpenNetworks.setEnabled(true);
            } else {
                if (state != 1) {
                    return;
                }
                ConfigureWifiSettingsExt.this.mNotifyOpenNetworks.setEnabled(false);
            }
        }
    };

    public ConfigureWifiSettingsExt(SettingsPreferenceFragment fragment) {
        Log.d("ConfigureWifiSettingsExt", "AdvancedWifiSettingsExt");
        this.mFragment = fragment;
        if (fragment == null) {
            return;
        }
        this.mActivity = fragment.getActivity();
    }

    public void onCreate() {
        Log.d("ConfigureWifiSettingsExt", "onCreate");
        this.mExt = UtilsExt.getWifiPlugin(this.mActivity);
        this.mIntentFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
    }

    public void onActivityCreated(ContentResolver cr) {
        Log.d("ConfigureWifiSettingsExt", "onActivityCreated");
        this.mExt.initConnectView(this.mActivity, this.mFragment.getPreferenceScreen());
        this.mExt.initPreference(cr);
        addWifiInfoPreference();
        this.mExt.initNetworkInfoView(this.mFragment.getPreferenceScreen());
    }

    public void onResume() {
        Log.d("ConfigureWifiSettingsExt", "onResume");
        initPreferences();
        this.mActivity.registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    public void onPause() {
        this.mActivity.unregisterReceiver(this.mReceiver);
    }

    private void initPreferences() {
        this.mNotifyOpenNetworks = (SwitchPreference) this.mFragment.findPreference("notify_open_networks");
        ListPreference sleepPolicyPref = (ListPreference) this.mFragment.findPreference("sleep_policy");
        if (sleepPolicyPref == null) {
            return;
        }
        this.mExt.setSleepPolicyPreference(sleepPolicyPref, this.mFragment.getResources().getStringArray(Utils.isWifiOnly(this.mActivity) ? R.array.wifi_sleep_policy_entries_wifi_only : R.array.wifi_sleep_policy_entries), this.mFragment.getResources().getStringArray(R.array.wifi_sleep_policy_values));
    }

    private void addWifiInfoPreference() {
        this.mMacAddressPref = this.mFragment.findPreference("mac_address");
        this.mIpAddressPref = this.mFragment.findPreference("current_ip_address");
        if (this.mMacAddressPref == null || this.mIpAddressPref == null) {
            return;
        }
        PreferenceScreen screen = this.mFragment.getPreferenceScreen();
        int order = 0;
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference preference = screen.getPreference(i);
            if (!"mac_address".equals(preference.getKey()) && !"current_ip_address".equals(preference.getKey())) {
                preference.setOrder(order);
                order++;
            }
        }
        int order2 = order + 1;
        this.mMacAddressPref.setOrder(order);
        if (this.mIpAddressPref != null) {
            int i2 = order2 + 1;
            this.mIpAddressPref.setOrder(order2);
        }
        if (!FeatureOption.MTK_DHCPV6C_WIFI) {
            return;
        }
        this.mIpAddressPref.setTitle(R.string.wifi_advanced_ipv4_address_title);
        this.mIpv6AddressPref = new Preference(this.mActivity, null, android.R.attr.preferenceInformationStyle);
        this.mIpv6AddressPref.setTitle(R.string.wifi_advanced_ipv6_address_title);
        this.mIpv6AddressPref.setKey("current_ipv6_address");
        this.mFragment.getPreferenceScreen().addPreference(this.mIpv6AddressPref);
    }

    public void refreshWifiInfo() {
        if (FeatureOption.MTK_DHCPV6C_WIFI) {
            String ipAddress = UtilsExt.getWifiIpAddresses();
            Log.d("ConfigureWifiSettingsExt", "refreshWifiInfo, the ipAddress is : " + ipAddress);
            if (ipAddress != null) {
                String[] ipAddresses = ipAddress.split(", ");
                int ipAddressesLength = ipAddresses.length;
                Log.d("ConfigureWifiSettingsExt", "ipAddressesLength is : " + ipAddressesLength);
                for (int i = 0; i < ipAddressesLength; i++) {
                    if (ipAddresses[i].indexOf(":") == -1) {
                        Log.d("ConfigureWifiSettingsExt", "ipAddresses[i] is : " + ipAddresses[i]);
                        this.mIpAddressPref.setSummary(ipAddresses[i] == null ? this.mActivity.getString(R.string.status_unavailable) : ipAddresses[i]);
                        if (ipAddressesLength == 1) {
                            this.mFragment.getPreferenceScreen().removePreference(this.mIpv6AddressPref);
                        }
                    } else {
                        String ipSummary = "";
                        if (ipAddresses[i] == null) {
                            ipSummary = this.mActivity.getString(R.string.status_unavailable);
                        } else {
                            String[] ipv6Addresses = ipAddresses[i].split("; ");
                            for (String str : ipv6Addresses) {
                                ipSummary = ipSummary + str + "\n";
                            }
                        }
                        this.mIpv6AddressPref.setSummary(ipSummary);
                        if (ipAddressesLength == 1) {
                            this.mFragment.getPreferenceScreen().removePreference(this.mIpAddressPref);
                        }
                    }
                }
            } else {
                this.mFragment.getPreferenceScreen().removePreference(this.mIpv6AddressPref);
                setDefaultIPAddress();
            }
        }
        this.mExt.refreshNetworkInfoView();
    }

    private void setDefaultIPAddress() {
        String ipAddress = Utils.getWifiIpAddresses(this.mActivity);
        Log.d("ConfigureWifiSettingsExt", "default ipAddress = " + ipAddress);
        if (this.mIpAddressPref == null) {
            return;
        }
        Preference preference = this.mIpAddressPref;
        if (ipAddress == null) {
            ipAddress = this.mActivity.getString(R.string.status_unavailable);
        }
        preference.setSummary(ipAddress);
        this.mIpAddressPref.setSelectable(false);
    }
}
