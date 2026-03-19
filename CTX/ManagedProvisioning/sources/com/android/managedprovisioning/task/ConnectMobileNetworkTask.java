package com.android.managedprovisioning.task;

import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.wifi.NetworkMonitor;

public class ConnectMobileNetworkTask extends AbstractProvisioningTask implements NetworkMonitor.NetworkConnectedCallback {
    private Handler mHandler;
    private final NetworkMonitor mNetworkMonitor;
    private boolean mTaskDone;
    private Runnable mTimeoutRunnable;
    private final Utils mUtils;

    public ConnectMobileNetworkTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mTaskDone = false;
        this.mNetworkMonitor = new NetworkMonitor(context);
        this.mUtils = new Utils();
    }

    @Override
    public void run(int i) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "device_provisioning_mobile_data", 1);
        if (this.mUtils.isConnectedToNetwork(this.mContext)) {
            success();
            return;
        }
        this.mTaskDone = false;
        this.mHandler = new Handler();
        this.mNetworkMonitor.startListening(this);
        this.mTimeoutRunnable = new Runnable() {
            @Override
            public final void run() {
                this.f$0.finishTask(false);
            }
        };
        this.mHandler.postDelayed(this.mTimeoutRunnable, 60000L);
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_connect_to_mobile_network;
    }

    @Override
    public void onNetworkConnected() {
        ProvisionLogger.logd("onNetworkConnected");
        if (this.mUtils.isConnectedToNetwork(this.mContext)) {
            ProvisionLogger.logd("Connected to the mobile network");
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
}
