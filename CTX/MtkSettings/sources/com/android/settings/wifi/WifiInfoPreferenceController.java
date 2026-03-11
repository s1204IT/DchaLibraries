package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v4.text.BidiFormatter;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;

public class WifiInfoPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private ISettingsMiscExt mExt;
    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;
    private Preference mWifiIpAddressPref;
    private Preference mWifiMacAddressPref;
    private final WifiManager mWifiManager;

    public WifiInfoPreferenceController(Context context, Lifecycle lifecycle, WifiManager wifiManager) {
        super(context);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.net.wifi.LINK_CONFIGURATION_CHANGED") || action.equals("android.net.wifi.STATE_CHANGE")) {
                    WifiInfoPreferenceController.this.updateWifiInfo();
                }
            }
        };
        this.mExt = UtilsExt.getMiscPlugin(this.mContext);
        this.mWifiManager = wifiManager;
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mWifiMacAddressPref = preferenceScreen.findPreference("mac_address");
        this.mWifiMacAddressPref.setSelectable(false);
        this.mWifiIpAddressPref = preferenceScreen.findPreference("current_ip_address");
        this.mWifiIpAddressPref.setSelectable(false);
    }

    @Override
    public void onResume() {
        this.mContext.registerReceiver(this.mReceiver, this.mFilter);
        updateWifiInfo();
    }

    @Override
    public void onPause() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    public void updateWifiInfo() {
        String strUnicodeWrap;
        if (this.mWifiMacAddressPref != null) {
            android.net.wifi.WifiInfo connectionInfo = this.mWifiManager.getConnectionInfo();
            int i = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_connected_mac_randomization_enabled", 0);
            String macAddress = connectionInfo == null ? null : connectionInfo.getMacAddress();
            Log.i("mtk81234", macAddress == null ? "macAddress = null" : macAddress);
            if (TextUtils.isEmpty(macAddress)) {
                this.mWifiMacAddressPref.setSummary(R.string.status_unavailable);
            } else if (i == 1 && "02:00:00:00:00:00".equals(macAddress)) {
                this.mWifiMacAddressPref.setSummary(R.string.wifi_status_mac_randomized);
            } else {
                this.mWifiMacAddressPref.setSummary(this.mExt.customizeMacAddressString(macAddress, this.mContext.getString(R.string.status_unavailable)));
            }
        }
        if (this.mWifiIpAddressPref != null) {
            String wifiIpAddresses = Utils.getWifiIpAddresses(this.mContext);
            Preference preference = this.mWifiIpAddressPref;
            if (wifiIpAddresses == null) {
                strUnicodeWrap = this.mContext.getString(R.string.status_unavailable);
            } else {
                strUnicodeWrap = BidiFormatter.getInstance().unicodeWrap(wifiIpAddresses);
            }
            preference.setSummary(strUnicodeWrap);
        }
    }
}
