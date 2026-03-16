package com.android.server.wifi;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import java.io.FileDescriptor;
import java.io.PrintWriter;

final class WifiSettingsStore {
    private static final int WIFI_DISABLED = 0;
    private static final int WIFI_DISABLED_AIRPLANE_ON = 3;
    private static final int WIFI_ENABLED = 1;
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

    synchronized boolean isWifiToggleEnabled() {
        boolean z = true;
        synchronized (this) {
            if (!this.mCheckSavedStateAtBoot) {
                this.mCheckSavedStateAtBoot = true;
                if (!testAndClearWifiSavedState()) {
                    if (this.mAirplaneModeOn) {
                        if (this.mPersistWifiState != 2) {
                            z = false;
                        }
                    } else if (this.mPersistWifiState == 0) {
                        z = false;
                    }
                }
            }
        }
        return z;
    }

    synchronized boolean isAirplaneModeOn() {
        return this.mAirplaneModeOn;
    }

    synchronized boolean isScanAlwaysAvailable() {
        boolean z;
        if (!this.mAirplaneModeOn) {
            z = this.mScanAlwaysAvailable;
        }
        return z;
    }

    synchronized boolean handleWifiToggled(boolean wifiEnabled) {
        boolean z = false;
        synchronized (this) {
            if (!this.mAirplaneModeOn || isAirplaneToggleable()) {
                if (wifiEnabled) {
                    if (this.mAirplaneModeOn) {
                        persistWifiState(2);
                    } else {
                        persistWifiState(1);
                    }
                } else {
                    persistWifiState(0);
                }
                z = true;
            }
        }
        return z;
    }

    synchronized boolean handleAirplaneModeToggled() {
        boolean z = true;
        synchronized (this) {
            if (!isAirplaneSensitive()) {
                z = false;
            } else {
                this.mAirplaneModeOn = getPersistedAirplaneModeOn();
                if (this.mAirplaneModeOn) {
                    if (this.mPersistWifiState == 1) {
                        persistWifiState(3);
                    }
                } else if (testAndClearWifiSavedState() || this.mPersistWifiState == 2) {
                    persistWifiState(1);
                }
            }
        }
        return z;
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
        return airplaneModeRadios == null || airplaneModeRadios.contains("wifi");
    }

    private boolean isAirplaneToggleable() {
        String toggleableRadios = Settings.Global.getString(this.mContext.getContentResolver(), "airplane_mode_toggleable_radios");
        return toggleableRadios != null && toggleableRadios.contains("wifi");
    }

    private boolean testAndClearWifiSavedState() {
        ContentResolver cr = this.mContext.getContentResolver();
        int wifiSavedState = 0;
        try {
            wifiSavedState = Settings.Global.getInt(cr, "wifi_saved_state");
            if (wifiSavedState == 1) {
                Settings.Global.putInt(cr, "wifi_saved_state", 0);
            }
        } catch (Settings.SettingNotFoundException e) {
        }
        return wifiSavedState == 1;
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
}
