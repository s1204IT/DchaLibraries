package com.android.server.wifi;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiSettingsStore {
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

    public synchronized boolean isAirplaneModeOn() {
        return this.mAirplaneModeOn;
    }

    public synchronized boolean isScanAlwaysAvailable() {
        boolean z;
        if (!this.mAirplaneModeOn) {
            z = this.mScanAlwaysAvailable;
        }
        return z;
    }

    public synchronized boolean handleWifiToggled(boolean z) {
        if (this.mAirplaneModeOn && !isAirplaneToggleable()) {
            return false;
        }
        if (z) {
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
        } else if (testAndClearWifiSavedState() || this.mPersistWifiState == 2) {
            persistWifiState(1);
        }
        return true;
    }

    synchronized void handleWifiScanAlwaysAvailableToggled() {
        this.mScanAlwaysAvailable = getPersistedScanAlwaysAvailable();
    }

    void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("mPersistWifiState " + this.mPersistWifiState);
        printWriter.println("mAirplaneModeOn " + this.mAirplaneModeOn);
    }

    private void persistWifiState(int i) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mPersistWifiState = i;
        Settings.Global.putInt(contentResolver, "wifi_on", i);
    }

    private boolean isAirplaneSensitive() {
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "airplane_mode_radios");
        return string == null || string.contains("wifi");
    }

    private boolean isAirplaneToggleable() {
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "airplane_mode_toggleable_radios");
        return string != null && string.contains("wifi");
    }

    private boolean testAndClearWifiSavedState() {
        int wifiSavedState = getWifiSavedState();
        if (wifiSavedState == 1) {
            setWifiSavedState(0);
        }
        return wifiSavedState == 1;
    }

    public void setWifiSavedState(int i) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_saved_state", i);
    }

    public int getWifiSavedState() {
        try {
            return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_saved_state");
        } catch (Settings.SettingNotFoundException e) {
            return 0;
        }
    }

    private int getPersistedWifiState() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        try {
            return Settings.Global.getInt(contentResolver, "wifi_on");
        } catch (Settings.SettingNotFoundException e) {
            Settings.Global.putInt(contentResolver, "wifi_on", 0);
            return 0;
        }
    }

    private boolean getPersistedAirplaneModeOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private boolean getPersistedScanAlwaysAvailable() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) == 1;
    }

    public int getLocationModeSetting(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "location_mode", 0);
    }
}
