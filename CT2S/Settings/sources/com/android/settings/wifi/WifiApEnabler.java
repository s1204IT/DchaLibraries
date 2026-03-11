package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.R;
import java.util.ArrayList;

public class WifiApEnabler {
    private static boolean mAvoidSoftapOnOffToggle = false;
    ConnectivityManager mCm;
    private final Context mContext;
    private final IntentFilter mIntentFilter;
    private final CharSequence mOriginalSummary;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
                WifiApEnabler.this.handleWifiApStateChanged(intent.getIntExtra("wifi_state", 14));
                return;
            }
            if ("android.net.conn.TETHER_STATE_CHANGED".equals(action)) {
                ArrayList<String> available = intent.getStringArrayListExtra("availableArray");
                ArrayList<String> active = intent.getStringArrayListExtra("activeArray");
                ArrayList<String> errored = intent.getStringArrayListExtra("erroredArray");
                WifiApEnabler.this.updateTetherState(available.toArray(), active.toArray(), errored.toArray());
                return;
            }
            if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                WifiApEnabler.this.enableWifiSwitch();
            }
        }
    };
    private final SwitchPreference mSwitch;
    private WifiManager mWifiManager;
    private String[] mWifiRegexs;

    public WifiApEnabler(Context context, SwitchPreference switchPreference) {
        this.mContext = context;
        this.mSwitch = switchPreference;
        this.mOriginalSummary = switchPreference != null ? switchPreference.getSummary() : "";
        if (switchPreference != null) {
            switchPreference.setPersistent(false);
        }
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mWifiRegexs = this.mCm.getTetherableWifiRegexs();
        this.mIntentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
        this.mIntentFilter.addAction("android.net.conn.TETHER_STATE_CHANGED");
        this.mIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
    }

    public void resume() {
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
        enableWifiSwitch();
    }

    public void pause() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    public void enableWifiSwitch() {
        boolean isAirplaneMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0;
        if (!isAirplaneMode) {
            this.mSwitch.setEnabled(true);
        } else {
            this.mSwitch.setSummary(this.mOriginalSummary);
            this.mSwitch.setEnabled(false);
        }
    }

    public void setSoftapEnabled(boolean enable) {
        ContentResolver cr = this.mContext.getContentResolver();
        int wifiState = this.mWifiManager.getWifiState();
        if (enable && (wifiState == 2 || wifiState == 3 || mAvoidSoftapOnOffToggle)) {
            if (mAvoidSoftapOnOffToggle) {
                mAvoidSoftapOnOffToggle = false;
            }
            this.mWifiManager.setWifiEnabled(false);
            Settings.Global.putInt(cr, "wifi_saved_state", 1);
        }
        if (this.mWifiManager.setWifiApEnabled(null, enable)) {
            if (this.mSwitch != null) {
                this.mSwitch.setEnabled(false);
            }
        } else if (this.mSwitch != null) {
            this.mSwitch.setSummary(R.string.wifi_error);
        }
        if (!enable) {
            int wifiSavedState = 0;
            try {
                wifiSavedState = Settings.Global.getInt(cr, "wifi_saved_state");
            } catch (Settings.SettingNotFoundException e) {
            }
            if (wifiSavedState == 1) {
                this.mWifiManager.setWifiEnabled(true);
                mAvoidSoftapOnOffToggle = true;
                Settings.Global.putInt(cr, "wifi_saved_state", 0);
            }
        }
    }

    public void updateConfigSummary(WifiConfiguration wifiConfig) {
        String s = this.mContext.getString(android.R.string.imProtocolSkype);
        SwitchPreference switchPreference = this.mSwitch;
        String string = this.mContext.getString(R.string.wifi_tether_enabled_subtext);
        Object[] objArr = new Object[1];
        if (wifiConfig != null) {
            s = wifiConfig.SSID;
        }
        objArr[0] = s;
        switchPreference.setSummary(String.format(string, objArr));
    }

    public void updateTetherState(Object[] available, Object[] tethered, Object[] errored) {
        boolean wifiTethered = false;
        boolean wifiErrored = false;
        for (Object o : tethered) {
            String s = (String) o;
            String[] arr$ = this.mWifiRegexs;
            for (String regex : arr$) {
                if (s.matches(regex)) {
                    wifiTethered = true;
                }
            }
        }
        for (Object o2 : errored) {
            String s2 = (String) o2;
            String[] arr$2 = this.mWifiRegexs;
            for (String regex2 : arr$2) {
                if (s2.matches(regex2)) {
                    wifiErrored = true;
                }
            }
        }
        if (wifiTethered) {
            WifiConfiguration wifiConfig = this.mWifiManager.getWifiApConfiguration();
            updateConfigSummary(wifiConfig);
        } else if (wifiErrored) {
            this.mSwitch.setSummary(R.string.wifi_error);
        }
    }

    public void handleWifiApStateChanged(int state) {
        switch (state) {
            case 10:
                this.mSwitch.setSummary(R.string.wifi_tether_stopping);
                this.mSwitch.setChecked(false);
                this.mSwitch.setEnabled(false);
                break;
            case 11:
                this.mSwitch.setChecked(false);
                this.mSwitch.setSummary(this.mOriginalSummary);
                enableWifiSwitch();
                break;
            case 12:
                this.mSwitch.setSummary(R.string.wifi_tether_starting);
                this.mSwitch.setEnabled(false);
                break;
            case 13:
                this.mSwitch.setChecked(true);
                this.mSwitch.setEnabled(true);
                break;
            default:
                this.mSwitch.setChecked(false);
                this.mSwitch.setSummary(R.string.wifi_error);
                enableWifiSwitch();
                break;
        }
    }
}
