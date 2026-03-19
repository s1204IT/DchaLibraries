package com.android.settings.wifi.tether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.BidiFormatter;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.wifi.tether.WifiTetherSoftApManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.wifi.AccessPoint;

public class WifiTetherPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {
    private static final IntentFilter AIRPLANE_INTENT_FILTER = new IntentFilter("android.intent.action.AIRPLANE_MODE");
    private final ConnectivityManager mConnectivityManager;
    private final Lifecycle mLifecycle;
    Preference mPreference;
    private final BroadcastReceiver mReceiver;
    private int mSoftApState;
    private final WifiManager mWifiManager;
    private final String[] mWifiRegexs;
    WifiTetherSoftApManager mWifiTetherSoftApManager;

    public WifiTetherPreferenceController(Context context, Lifecycle lifecycle) {
        this(context, lifecycle, true);
    }

    WifiTetherPreferenceController(Context context, Lifecycle lifecycle, boolean z) {
        super(context);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                    WifiTetherPreferenceController.this.clearSummaryForAirplaneMode(R.string.wifi_hotspot_off_subtext);
                }
            }
        };
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mWifiRegexs = this.mConnectivityManager.getTetherableWifiRegexs();
        this.mLifecycle = lifecycle;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        if (z) {
            initWifiTetherSoftApManager();
        }
    }

    @Override
    public boolean isAvailable() {
        return (this.mWifiRegexs == null || this.mWifiRegexs.length == 0 || Utils.isMonkeyRunning()) ? false : true;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference("wifi_tether");
        if (this.mPreference == null) {
        }
    }

    @Override
    public String getPreferenceKey() {
        return "wifi_tether";
    }

    @Override
    public void onStart() {
        if (this.mPreference != null) {
            this.mContext.registerReceiver(this.mReceiver, AIRPLANE_INTENT_FILTER);
            clearSummaryForAirplaneMode();
            if (this.mWifiTetherSoftApManager != null) {
                this.mWifiTetherSoftApManager.registerSoftApCallback();
            }
        }
    }

    @Override
    public void onStop() {
        if (this.mPreference != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            if (this.mWifiTetherSoftApManager != null) {
                this.mWifiTetherSoftApManager.unRegisterSoftApCallback();
            }
        }
    }

    void initWifiTetherSoftApManager() {
        this.mWifiTetherSoftApManager = new WifiTetherSoftApManager(this.mWifiManager, new WifiTetherSoftApManager.WifiTetherSoftApCallback() {
            @Override
            public void onStateChanged(int i, int i2) {
                WifiTetherPreferenceController.this.mSoftApState = i;
                WifiTetherPreferenceController.this.handleWifiApStateChanged(i, i2);
            }

            @Override
            public void onNumClientsChanged(int i) {
                if (WifiTetherPreferenceController.this.mPreference != null && WifiTetherPreferenceController.this.mSoftApState == 13) {
                    WifiTetherPreferenceController.this.mPreference.setSummary(WifiTetherPreferenceController.this.mContext.getResources().getQuantityString(R.plurals.wifi_tether_connected_summary, i, Integer.valueOf(i)));
                }
            }
        });
    }

    void handleWifiApStateChanged(int i, int i2) {
        switch (i) {
            case AccessPoint.Speed.MODERATE:
                this.mPreference.setSummary(R.string.wifi_tether_stopping);
                break;
            case 11:
                this.mPreference.setSummary(R.string.wifi_hotspot_off_subtext);
                clearSummaryForAirplaneMode();
                break;
            case 12:
                this.mPreference.setSummary(R.string.wifi_tether_starting);
                break;
            case 13:
                updateConfigSummary(this.mWifiManager.getWifiApConfiguration());
                break;
            default:
                if (i2 == 1) {
                    this.mPreference.setSummary(R.string.wifi_sap_no_channel_error);
                } else {
                    this.mPreference.setSummary(R.string.wifi_error);
                }
                clearSummaryForAirplaneMode();
                break;
        }
    }

    private void updateConfigSummary(WifiConfiguration wifiConfiguration) {
        String string = this.mContext.getString(android.R.string.notification_feedback_indicator_alerted);
        Preference preference = this.mPreference;
        Context context = this.mContext;
        Object[] objArr = new Object[1];
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        if (wifiConfiguration != null) {
            string = wifiConfiguration.SSID;
        }
        objArr[0] = bidiFormatter.unicodeWrap(string);
        preference.setSummary(context.getString(R.string.wifi_tether_enabled_subtext, objArr));
    }

    private void clearSummaryForAirplaneMode() {
        clearSummaryForAirplaneMode(-1);
    }

    private void clearSummaryForAirplaneMode(int i) {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0) {
            this.mPreference.setSummary(R.string.wifi_tether_disabled_by_airplane);
        } else if (i != -1) {
            this.mPreference.setSummary(i);
        }
    }
}
