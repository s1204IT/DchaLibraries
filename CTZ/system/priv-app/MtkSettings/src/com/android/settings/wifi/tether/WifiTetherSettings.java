package com.android.settings.wifi.tether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.wifi.tether.WifiTetherBasePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiTetherSettingsExt;
import com.mediatek.settings.wifi.tether.WifiTetherMaxConnectionPreferenceController;
import com.mediatek.settings.wifi.tether.WifiTetherResetPreferenceController;
import com.mediatek.settings.wifi.tether.WifiTetherUserPreferenceController;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/* loaded from: classes.dex */
public class WifiTetherSettings extends RestrictedDashboardFragment implements WifiTetherBasePreferenceController.OnTetherConfigUpdateListener {
    private static final IntentFilter TETHER_STATE_CHANGE_FILTER = new IntentFilter("android.net.conn.TETHER_STATE_CHANGED");
    private WifiTetherApBandPreferenceController mApBandPreferenceController;
    private WifiTetherMaxConnectionPreferenceController mMaxConnectionPreferenceController;
    private WifiTetherPasswordPreferenceController mPasswordPreferenceController;
    private WifiTetherResetPreferenceController mResetPreferenceController;
    private boolean mRestartWifiApAfterConfigChange;
    private WifiTetherSSIDPreferenceController mSSIDPreferenceController;
    private WifiTetherSecurityPreferenceController mSecurityPreferenceController;
    private WifiTetherSwitchBarController mSwitchBarController;
    TetherChangeReceiver mTetherChangeReceiver;
    private WifiTetherUserPreferenceController mUserPreferenceController;
    private WifiManager mWifiManager;
    private IWifiTetherSettingsExt mWifiTetherSettingsExt;

    static {
        TETHER_STATE_CHANGE_FILTER.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
    }

    public WifiTetherSettings() {
        super("no_config_tethering");
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1014;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "WifiTetherSettings";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mTetherChangeReceiver = new TetherChangeReceiver();
    }

    @Override // com.android.settings.dashboard.RestrictedDashboardFragment, com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        SwitchBar switchBar = settingsActivity.getSwitchBar();
        this.mSwitchBarController = new WifiTetherSwitchBarController(settingsActivity, new SwitchBarController(switchBar));
        getLifecycle().addObserver(this.mSwitchBarController);
        switchBar.show();
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStart() {
        super.onStart();
        Context context = getContext();
        if (context != null) {
            context.registerReceiver(this.mTetherChangeReceiver, TETHER_STATE_CHANGE_FILTER);
        }
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        super.onStop();
        Context context = getContext();
        if (context != null) {
            context.unregisterReceiver(this.mTetherChangeReceiver);
        }
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_tether_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        this.mWifiTetherSettingsExt = UtilsExt.getWifiTetherSettingsExt(context);
        this.mSSIDPreferenceController = new WifiTetherSSIDPreferenceController(context, this);
        this.mSecurityPreferenceController = new WifiTetherSecurityPreferenceController(context, this);
        this.mPasswordPreferenceController = new WifiTetherPasswordPreferenceController(context, this);
        this.mApBandPreferenceController = new WifiTetherApBandPreferenceController(context, this);
        this.mMaxConnectionPreferenceController = new WifiTetherMaxConnectionPreferenceController(context, this);
        this.mResetPreferenceController = new WifiTetherResetPreferenceController(context, this, getFragmentManager());
        this.mUserPreferenceController = new WifiTetherUserPreferenceController(context, getLifecycle());
        arrayList.add(this.mSSIDPreferenceController);
        arrayList.add(this.mSecurityPreferenceController);
        arrayList.add(this.mPasswordPreferenceController);
        arrayList.add(this.mApBandPreferenceController);
        arrayList.add(new WifiTetherAutoOffPreferenceController(context, "wifi_tether_auto_turn_off"));
        arrayList.add(this.mMaxConnectionPreferenceController);
        arrayList.add(this.mResetPreferenceController);
        arrayList.add(this.mUserPreferenceController);
        this.mWifiTetherSettingsExt.addPreferenceController(context, arrayList, this);
        return arrayList;
    }

    @Override // com.android.settings.wifi.tether.WifiTetherBasePreferenceController.OnTetherConfigUpdateListener
    public void onTetherConfigUpdated() {
        WifiConfiguration wifiConfigurationBuildNewConfig = buildNewConfig();
        this.mPasswordPreferenceController.updateVisibility(wifiConfigurationBuildNewConfig.getAuthType());
        if (this.mWifiManager.getWifiApState() == 13) {
            Log.d("TetheringSettings", "Wifi AP config changed while enabled, stop and restart");
            this.mRestartWifiApAfterConfigChange = true;
            this.mSwitchBarController.stopTether();
        }
        this.mWifiManager.setWifiApConfiguration(wifiConfigurationBuildNewConfig);
    }

    private WifiConfiguration buildNewConfig() {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        int securityType = this.mSecurityPreferenceController.getSecurityType();
        wifiConfiguration.SSID = this.mSSIDPreferenceController.getSSID();
        wifiConfiguration.allowedKeyManagement.set(securityType);
        wifiConfiguration.preSharedKey = this.mPasswordPreferenceController.getPasswordValidated(securityType);
        wifiConfiguration.allowedAuthAlgorithms.set(0);
        wifiConfiguration.apBand = this.mApBandPreferenceController.getBandIndex();
        return wifiConfiguration;
    }

    private void startTether() {
        this.mRestartWifiApAfterConfigChange = false;
        this.mSwitchBarController.startTether();
    }

    private void updateDisplayWithNewConfig() {
        ((WifiTetherSSIDPreferenceController) use(WifiTetherSSIDPreferenceController.class)).updateDisplay();
        ((WifiTetherSecurityPreferenceController) use(WifiTetherSecurityPreferenceController.class)).updateDisplay();
        ((WifiTetherPasswordPreferenceController) use(WifiTetherPasswordPreferenceController.class)).updateDisplay();
        ((WifiTetherApBandPreferenceController) use(WifiTetherApBandPreferenceController.class)).updateDisplay();
    }

    class TetherChangeReceiver extends BroadcastReceiver {
        TetherChangeReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("WifiTetherSettings", "updating display config due to receiving broadcast action " + action);
            WifiTetherSettings.this.updateDisplayWithNewConfig();
            if (action.equals("android.net.conn.TETHER_STATE_CHANGED")) {
                if (WifiTetherSettings.this.mWifiManager.getWifiApState() == 11 && WifiTetherSettings.this.mRestartWifiApAfterConfigChange) {
                    WifiTetherSettings.this.startTether();
                    return;
                }
                return;
            }
            if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED") && intent.getIntExtra("wifi_state", 0) == 11 && WifiTetherSettings.this.mRestartWifiApAfterConfigChange) {
                WifiTetherSettings.this.startTether();
            }
        }
    }

    @Override // com.android.settings.wifi.tether.WifiTetherBasePreferenceController.OnTetherConfigUpdateListener
    public void onNetworkReset() {
        this.mSSIDPreferenceController.setSSID("AndroidAP_" + (new Random().nextInt(9000) + 1000));
        this.mSecurityPreferenceController.setSecurityType();
        this.mPasswordPreferenceController.setEnabled(true);
        String string = UUID.randomUUID().toString();
        this.mPasswordPreferenceController.setPassword(string.substring(0, 8) + string.substring(9, 13));
        onTetherConfigUpdated();
    }

    @Override // com.android.settings.wifi.tether.WifiTetherBasePreferenceController.OnTetherConfigUpdateListener
    public void onSecurityChanged() {
        onTetherConfigUpdated();
    }
}
