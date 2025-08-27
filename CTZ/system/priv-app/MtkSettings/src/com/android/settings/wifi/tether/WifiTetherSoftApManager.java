package com.android.settings.wifi.tether;

import android.net.wifi.WifiManager;
import android.os.Handler;

/* loaded from: classes.dex */
public class WifiTetherSoftApManager {
    private WifiManager mWifiManager;
    private WifiTetherSoftApCallback mWifiTetherSoftApCallback;
    private WifiManager.SoftApCallback mSoftApCallback = new WifiManager.SoftApCallback() { // from class: com.android.settings.wifi.tether.WifiTetherSoftApManager.1
        public void onStateChanged(int i, int i2) {
            WifiTetherSoftApManager.this.mWifiTetherSoftApCallback.onStateChanged(i, i2);
        }

        public void onNumClientsChanged(int i) {
            WifiTetherSoftApManager.this.mWifiTetherSoftApCallback.onNumClientsChanged(i);
        }
    };
    private Handler mHandler = new Handler();

    public interface WifiTetherSoftApCallback {
        void onNumClientsChanged(int i);

        void onStateChanged(int i, int i2);
    }

    WifiTetherSoftApManager(WifiManager wifiManager, WifiTetherSoftApCallback wifiTetherSoftApCallback) {
        this.mWifiManager = wifiManager;
        this.mWifiTetherSoftApCallback = wifiTetherSoftApCallback;
    }

    public void registerSoftApCallback() {
        this.mWifiManager.registerSoftApCallback(this.mSoftApCallback, this.mHandler);
    }

    public void unRegisterSoftApCallback() {
        this.mWifiManager.unregisterSoftApCallback(this.mSoftApCallback);
    }
}
