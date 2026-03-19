package com.android.managedprovisioning.task.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.Utils;

public class NetworkMonitor {

    @VisibleForTesting
    static final IntentFilter FILTER = new IntentFilter();
    private final BroadcastReceiver mBroadcastReceiver;
    private NetworkConnectedCallback mCallback;
    private final Context mContext;
    private final Utils mUtils;

    public interface NetworkConnectedCallback {
        void onNetworkConnected();
    }

    static {
        FILTER.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        FILTER.addAction("android.net.conn.INET_CONDITION_ACTION");
    }

    public NetworkMonitor(Context context) {
        this(context, new Utils());
    }

    @VisibleForTesting
    NetworkMonitor(Context context, Utils utils) {
        this.mCallback = null;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                ProvisionLogger.logd("NetworkMonitor.onReceive: " + intent);
                if (!NetworkMonitor.FILTER.matchAction(intent.getAction())) {
                    return;
                }
                synchronized (NetworkMonitor.this) {
                    if (NetworkMonitor.this.mUtils.isConnectedToNetwork(context2)) {
                        if (NetworkMonitor.this.mCallback != null) {
                            NetworkMonitor.this.mCallback.onNetworkConnected();
                        }
                    } else {
                        ProvisionLogger.logd("NetworkMonitor: not connected to network");
                    }
                }
            }
        };
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
    }

    public synchronized void startListening(NetworkConnectedCallback networkConnectedCallback) {
        this.mCallback = (NetworkConnectedCallback) Preconditions.checkNotNull(networkConnectedCallback);
        this.mContext.registerReceiver(this.mBroadcastReceiver, FILTER);
    }

    public synchronized void stopListening() {
        if (this.mCallback == null) {
            return;
        }
        this.mCallback = null;
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }
}
