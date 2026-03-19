package com.android.managedprovisioning.task;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.wifi.NetworkMonitor;
import com.android.managedprovisioning.task.wifi.WifiConfigurationProvider;

public class AddWifiNetworkTask extends AbstractProvisioningTask implements NetworkMonitor.NetworkConnectedCallback {

    @VisibleForTesting
    static final int ADD_NETWORK_FAIL = -1;
    private Handler mHandler;
    private Injector mInjector;
    private final NetworkMonitor mNetworkMonitor;
    private boolean mTaskDone;
    private Runnable mTimeoutRunnable;
    private final Utils mUtils;
    private final WifiConfigurationProvider mWifiConfigurationProvider;
    private final WifiManager mWifiManager;

    public AddWifiNetworkTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        this(new NetworkMonitor(context), new WifiConfigurationProvider(), context, provisioningParams, callback, new Utils(), new Injector());
    }

    @VisibleForTesting
    AddWifiNetworkTask(NetworkMonitor networkMonitor, WifiConfigurationProvider wifiConfigurationProvider, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback, Utils utils, Injector injector) {
        super(context, provisioningParams, callback);
        this.mTaskDone = false;
        this.mNetworkMonitor = (NetworkMonitor) Preconditions.checkNotNull(networkMonitor);
        this.mWifiConfigurationProvider = (WifiConfigurationProvider) Preconditions.checkNotNull(wifiConfigurationProvider);
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
        this.mInjector = (Injector) Preconditions.checkNotNull(injector);
    }

    @Override
    public void run(int i) {
        if (this.mProvisioningParams.wifiInfo == null) {
            success();
            return;
        }
        if (this.mWifiManager == null || !enableWifi()) {
            ProvisionLogger.loge("Failed to enable wifi");
            error(0);
        } else {
            if (isConnectedToSpecifiedWifi()) {
                success();
                return;
            }
            this.mTaskDone = false;
            this.mHandler = new Handler();
            this.mNetworkMonitor.startListening(this);
            connectToProvidedNetwork();
        }
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_connect_to_wifi;
    }

    private void connectToProvidedNetwork() {
        WifiConfiguration wifiConfigurationGenerateWifiConfiguration = this.mWifiConfigurationProvider.generateWifiConfiguration(this.mProvisioningParams.wifiInfo);
        if (wifiConfigurationGenerateWifiConfiguration == null) {
            ProvisionLogger.loge("WifiConfiguration is null");
            error(0);
            return;
        }
        int iTryAddingNetwork = tryAddingNetwork(wifiConfigurationGenerateWifiConfiguration);
        if (iTryAddingNetwork == ADD_NETWORK_FAIL) {
            ProvisionLogger.loge("Unable to add network after trying 6 times.");
            error(0);
            return;
        }
        this.mWifiManager.enableNetwork(iTryAddingNetwork, true);
        this.mWifiManager.saveConfiguration();
        if (!this.mWifiManager.reconnect()) {
            ProvisionLogger.loge("Unable to connect to wifi");
            error(0);
        } else {
            this.mTimeoutRunnable = new Runnable() {
                @Override
                public final void run() {
                    this.f$0.finishTask(false);
                }
            };
            this.mHandler.postDelayed(this.mTimeoutRunnable, 60000L);
        }
    }

    private int tryAddingNetwork(WifiConfiguration wifiConfiguration) {
        int iAddNetwork = this.mWifiManager.addNetwork(wifiConfiguration);
        int i = 6;
        int i2 = 500;
        while (iAddNetwork == ADD_NETWORK_FAIL && i > 0) {
            ProvisionLogger.loge("Retrying in " + i2 + " ms.");
            try {
                this.mInjector.threadSleep(i2);
            } catch (InterruptedException e) {
                ProvisionLogger.loge("Retry interrupted.");
            }
            i2 *= 2;
            i += ADD_NETWORK_FAIL;
            iAddNetwork = this.mWifiManager.addNetwork(wifiConfiguration);
        }
        return iAddNetwork;
    }

    private boolean enableWifi() {
        return this.mWifiManager.isWifiEnabled() || this.mWifiManager.setWifiEnabled(true);
    }

    @Override
    public void onNetworkConnected() {
        ProvisionLogger.logd("onNetworkConnected");
        if (isConnectedToSpecifiedWifi()) {
            ProvisionLogger.logd("Connected to the correct network");
            finishTask(true);
            this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        }
    }

    private synchronized void finishTask(boolean z) {
        if (this.mTaskDone) {
            return;
        }
        this.mTaskDone = true;
        this.mNetworkMonitor.stopListening();
        if (z) {
            success();
        } else {
            error(0);
        }
    }

    private boolean isConnectedToSpecifiedWifi() {
        if (!this.mUtils.isConnectedToWifi(this.mContext)) {
            ProvisionLogger.logd("Not connected to WIFI");
            return false;
        }
        if (this.mWifiManager.getConnectionInfo() == null) {
            ProvisionLogger.logd("connection info is null");
            return false;
        }
        String ssid = this.mWifiManager.getConnectionInfo().getSSID();
        if (!this.mProvisioningParams.wifiInfo.ssid.equals(ssid)) {
            ProvisionLogger.logd("Wanted to connect SSID " + this.mProvisioningParams.wifiInfo.ssid + ", but it is now connected to " + ssid);
            return false;
        }
        return true;
    }

    @VisibleForTesting
    static class Injector {
        Injector() {
        }

        public void threadSleep(long j) throws InterruptedException {
            Thread.sleep(j);
        }
    }
}
