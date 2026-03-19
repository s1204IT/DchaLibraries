package com.android.server.wifi;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Slog;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiSettingsStore {
    private static final String TAG = "WifiSettingsStore";
    static final int WIFI_DISABLED = 0;
    private static final int WIFI_DISABLED_AIRPLANE_ON = 3;
    static final int WIFI_ENABLED = 1;
    private static final int WIFI_ENABLED_AIRPLANE_OVERRIDE = 2;
    private boolean mAirplaneModeOn;
    private final Context mContext;
    private int mPersistWifiState;
    private boolean mCheckSavedStateAtBoot = false;
    private boolean mScanAlwaysAvailable = getPersistedScanAlwaysAvailable();

    WifiSettingsStore(Context context) {
        this.mPersistWifiState = 0;
        this.mAirplaneModeOn = false;
        this.mContext = context;
        this.mAirplaneModeOn = getPersistedAirplaneModeOn();
        this.mPersistWifiState = getPersistedWifiState();
    }

    public synchronized boolean isWifiToggleEnabled() {
        synchronized (this) {
            if (!this.mCheckSavedStateAtBoot) {
                this.mCheckSavedStateAtBoot = true;
                if (testAndClearWifiSavedState()) {
                    return true;
                }
            }
            if (this.mAirplaneModeOn) {
                return this.mPersistWifiState == 2;
            }
            return this.mPersistWifiState != 0;
        }
    }

    public synchronized boolean isAirplaneModeOn() {
        return this.mAirplaneModeOn;
    }

    public synchronized boolean isScanAlwaysAvailable() {
        return !this.mAirplaneModeOn ? this.mScanAlwaysAvailable : false;
    }

    public synchronized boolean handleWifiToggled(boolean wifiEnabled) {
        if (this.mAirplaneModeOn && !isAirplaneToggleable()) {
            return false;
        }
        if (wifiEnabled) {
            if (this.mAirplaneModeOn) {
                persistWifiState(2);
            } else {
                persistWifiState(1);
            }
        } else {
            persistWifiState(0);
        }
        return true;
    }

    synchronized boolean handleAirplaneModeToggled() {
        if (!isAirplaneSensitive()) {
            return false;
        }
        this.mAirplaneModeOn = getPersistedAirplaneModeOn();
        if (this.mAirplaneModeOn) {
            if (this.mPersistWifiState == 1) {
                persistWifiState(3);
            }
        } else if (testAndClearWifiSavedState() || this.mPersistWifiState == 2 || this.mPersistWifiState == 3) {
            persistWifiState(1);
        }
        return true;
    }

    synchronized void handleWifiScanAlwaysAvailableToggled() {
        this.mScanAlwaysAvailable = getPersistedScanAlwaysAvailable();
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("mPersistWifiState " + this.mPersistWifiState);
        pw.println("mAirplaneModeOn " + this.mAirplaneModeOn);
    }

    private void persistWifiState(int state) {
        ContentResolver cr = this.mContext.getContentResolver();
        this.mPersistWifiState = state;
        Settings.Global.putInt(cr, "wifi_on", state);
    }

    private boolean isAirplaneSensitive() {
        String airplaneModeRadios = Settings.Global.getString(this.mContext.getContentResolver(), "airplane_mode_radios");
        if (airplaneModeRadios != null) {
            return airplaneModeRadios.contains("wifi");
        }
        return true;
    }

    private boolean isAirplaneToggleable() {
        String toggleableRadios = Settings.Global.getString(this.mContext.getContentResolver(), "airplane_mode_toggleable_radios");
        if (toggleableRadios != null) {
            return toggleableRadios.contains("wifi");
        }
        return false;
    }

    private boolean testAndClearWifiSavedState() {
        int wifiSavedState = getWifiSavedState();
        if (wifiSavedState == 1) {
            setWifiSavedState(0);
        }
        return wifiSavedState == 1;
    }

    public void setWifiSavedState(int state) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_saved_state", state);
    }

    public int getWifiSavedState() {
        try {
            return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_saved_state");
        } catch (Settings.SettingNotFoundException e) {
            return 0;
        }
    }

    private int getPersistedWifiState() {
        ContentResolver cr = this.mContext.getContentResolver();
        try {
            return Settings.Global.getInt(cr, "wifi_on");
        } catch (Settings.SettingNotFoundException e) {
            Settings.Global.putInt(cr, "wifi_on", 0);
            return 0;
        }
    }

    private boolean getPersistedAirplaneModeOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private boolean getPersistedScanAlwaysAvailable() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) == 1;
    }

    public boolean hasConnectableAp() {
        int persistedWifiState = getPersistedWifiState();
        Slog.d(TAG, "hasConnectableAp, mPersistWifiState:" + persistedWifiState);
        return (persistedWifiState == 0 || persistedWifiState == 3) ? false : true;
    }

    public void setCheckSavedStateAtBoot(boolean flag) {
        this.mCheckSavedStateAtBoot = flag;
    }
}
