package com.android.server.wifi;

import android.text.TextUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiCountryCode {
    private static final String TAG = "WifiCountryCode";
    private String mDefaultCountryCode;
    private boolean mRevertCountryCodeOnCellularLoss;
    private final WifiNative mWifiNative;
    private boolean DBG = false;
    private boolean mReady = false;
    private String mTelephonyCountryCode = null;
    private String mCurrentCountryCode = null;

    public WifiCountryCode(WifiNative wifiNative, String str, boolean z) {
        this.mDefaultCountryCode = null;
        this.mWifiNative = wifiNative;
        this.mRevertCountryCodeOnCellularLoss = z;
        if (!TextUtils.isEmpty(str)) {
            this.mDefaultCountryCode = str.toUpperCase();
        } else if (this.mRevertCountryCodeOnCellularLoss) {
            Log.w(TAG, "config_wifi_revert_country_code_on_cellular_loss is set, but there is no default country code.");
            this.mRevertCountryCodeOnCellularLoss = false;
            return;
        }
        if (this.mRevertCountryCodeOnCellularLoss) {
            Log.d(TAG, "Country code will be reverted to " + this.mDefaultCountryCode + " on MCC loss");
        }
    }

    public void enableVerboseLogging(int i) {
        if (i > 0) {
            this.DBG = true;
        } else {
            this.DBG = false;
        }
    }

    public synchronized void airplaneModeEnabled() {
        if (this.DBG) {
            Log.d(TAG, "Airplane Mode Enabled");
        }
        this.mTelephonyCountryCode = null;
    }

    public synchronized void setReadyForChange(boolean z) {
        if (this.DBG) {
            Log.d(TAG, "Set ready: " + z);
        }
        this.mReady = z;
        if (this.mReady) {
            updateCountryCode();
        }
    }

    public synchronized boolean setCountryCode(String str) {
        if (this.DBG) {
            Log.d(TAG, "Receive set country code request: " + str);
        }
        if (TextUtils.isEmpty(str)) {
            if (this.mRevertCountryCodeOnCellularLoss) {
                if (this.DBG) {
                    Log.d(TAG, "Received empty country code, reset to default country code");
                }
                this.mTelephonyCountryCode = null;
            }
        } else {
            this.mTelephonyCountryCode = str.toUpperCase();
        }
        if (this.mReady) {
            updateCountryCode();
        }
        return true;
    }

    public synchronized String getCountryCodeSentToDriver() {
        return this.mCurrentCountryCode;
    }

    public synchronized String getCountryCode() {
        return pickCountryCode();
    }

    public synchronized void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mCurrentCountryCode != null) {
            printWriter.println("CountryCode sent to driver: " + this.mCurrentCountryCode);
        } else if (pickCountryCode() != null) {
            printWriter.println("CountryCode: " + pickCountryCode() + " was not sent to driver");
        } else {
            printWriter.println("CountryCode was not initialized");
        }
    }

    private void updateCountryCode() {
        if (this.DBG) {
            Log.d(TAG, "Update country code");
        }
        String strPickCountryCode = pickCountryCode();
        if (strPickCountryCode != null) {
            setCountryCodeNative(strPickCountryCode);
        }
    }

    private String pickCountryCode() {
        if (this.mTelephonyCountryCode != null) {
            return this.mTelephonyCountryCode;
        }
        if (this.mDefaultCountryCode != null) {
            return this.mDefaultCountryCode;
        }
        return null;
    }

    private boolean setCountryCodeNative(String str) {
        if (this.mWifiNative.setCountryCode(this.mWifiNative.getClientInterfaceName(), str)) {
            Log.d(TAG, "Succeeded to set country code to: " + str);
            this.mCurrentCountryCode = str;
            return true;
        }
        Log.d(TAG, "Failed to set country code to: " + str);
        return false;
    }
}
