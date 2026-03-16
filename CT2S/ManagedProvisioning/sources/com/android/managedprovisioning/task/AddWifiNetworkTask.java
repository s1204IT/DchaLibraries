package com.android.managedprovisioning.task;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import com.android.managedprovisioning.NetworkMonitor;
import com.android.managedprovisioning.ProvisionLogger;
import com.android.managedprovisioning.WifiConfig;

public class AddWifiNetworkTask implements NetworkMonitor.Callback {
    private final Callback mCallback;
    private final Context mContext;
    private Handler mHandler;
    private final boolean mHidden;
    private NetworkMonitor mNetworkMonitor;
    private final String mPacUrl;
    private final String mPassword;
    private final String mProxyBypassHosts;
    private final String mProxyHost;
    private final int mProxyPort;
    private final String mSecurityType;
    private final String mSsid;
    private WifiConfig mWifiConfig;
    private WifiManager mWifiManager;
    private boolean mTaskDone = false;
    private int mDurationNextSleep = 500;
    private int mRetriesLeft = 6;

    public static abstract class Callback {
        public abstract void onError();

        public abstract void onSuccess();
    }

    public AddWifiNetworkTask(Context context, String ssid, boolean hidden, String securityType, String password, String proxyHost, int proxyPort, String proxyBypassHosts, String pacUrl, Callback callback) {
        this.mCallback = callback;
        this.mContext = context;
        if (TextUtils.isEmpty(ssid)) {
            throw new IllegalArgumentException("The ssid must be non-empty.");
        }
        this.mSsid = ssid;
        this.mHidden = hidden;
        this.mSecurityType = securityType;
        this.mPassword = password;
        this.mProxyHost = proxyHost;
        this.mProxyPort = proxyPort;
        this.mProxyBypassHosts = proxyBypassHosts;
        this.mPacUrl = pacUrl;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mWifiConfig = new WifiConfig(this.mWifiManager);
        HandlerThread thread = new HandlerThread("Timeout thread", 10);
        thread.start();
        Looper looper = thread.getLooper();
        this.mHandler = new Handler(looper);
    }

    public void run() {
        if (!enableWifi()) {
            ProvisionLogger.loge("Failed to enable wifi");
            this.mCallback.onError();
        } else if (isConnectedToSpecifiedWifi()) {
            this.mCallback.onSuccess();
        } else {
            this.mNetworkMonitor = new NetworkMonitor(this.mContext, this);
            connectToProvidedNetwork();
        }
    }

    private void connectToProvidedNetwork() {
        int netId = this.mWifiConfig.addNetwork(this.mSsid, this.mHidden, this.mSecurityType, this.mPassword, this.mProxyHost, this.mProxyPort, this.mProxyBypassHosts, this.mPacUrl);
        if (netId == -1) {
            ProvisionLogger.loge("Failed to save network.");
            if (this.mRetriesLeft > 0) {
                ProvisionLogger.loge("Retrying in " + this.mDurationNextSleep + " ms.");
                try {
                    Thread.sleep(this.mDurationNextSleep);
                } catch (InterruptedException e) {
                    ProvisionLogger.loge("Retry interrupted.");
                }
                this.mDurationNextSleep *= 2;
                this.mRetriesLeft--;
                connectToProvidedNetwork();
                return;
            }
            ProvisionLogger.loge("Already retried 6 times. Quit retrying and report error.");
            this.mCallback.onError();
            return;
        }
        if (!this.mWifiManager.reconnect()) {
            ProvisionLogger.loge("Unable to connect to wifi");
            this.mCallback.onError();
        } else {
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        if (!AddWifiNetworkTask.this.mTaskDone) {
                            AddWifiNetworkTask.this.mTaskDone = true;
                            ProvisionLogger.loge("Setting up wifi connection timed out.");
                            AddWifiNetworkTask.this.mCallback.onError();
                        }
                    }
                }
            }, 30000L);
        }
    }

    private boolean enableWifi() {
        return this.mWifiManager != null && (this.mWifiManager.isWifiEnabled() || this.mWifiManager.setWifiEnabled(true));
    }

    @Override
    public void onNetworkConnected() {
        if (isConnectedToSpecifiedWifi()) {
            synchronized (this) {
                if (!this.mTaskDone) {
                    this.mTaskDone = true;
                    ProvisionLogger.logd("Connected to the correct network");
                    this.mHandler.removeCallbacksAndMessages(null);
                    cleanUp();
                    this.mCallback.onSuccess();
                }
            }
        }
    }

    @Override
    public void onNetworkDisconnected() {
    }

    public void cleanUp() {
        if (this.mNetworkMonitor != null) {
            this.mNetworkMonitor.close();
            this.mNetworkMonitor = null;
        }
    }

    private boolean isConnectedToSpecifiedWifi() {
        return NetworkMonitor.isConnectedToWifi(this.mContext) && this.mWifiManager.getConnectionInfo() != null && this.mSsid.equals(this.mWifiManager.getConnectionInfo().getSSID());
    }

    public static boolean isConnectedToWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        NetworkInfo info = cm.getNetworkInfo(1);
        return info.isConnected();
    }

    public static Intent getWifiPickIntent() {
        Intent wifiIntent = new Intent("android.net.wifi.PICK_WIFI_NETWORK");
        wifiIntent.putExtra("only_access_points", true);
        wifiIntent.putExtra("extra_prefs_show_button_bar", true);
        wifiIntent.putExtra("wifi_enable_next_on_connect", true);
        return wifiIntent;
    }
}
