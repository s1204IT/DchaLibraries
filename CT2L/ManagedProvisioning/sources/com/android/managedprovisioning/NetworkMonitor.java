package com.android.managedprovisioning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkMonitor {
    private Callback mCallback;
    private Context mContext;
    private boolean mReceiverRegistered;
    private boolean mNetworkConnected = false;
    public final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ProvisionLogger.logd("onReceive " + intent.toString());
            NetworkMonitor.this.mNetworkConnected = NetworkMonitor.isConnectedToWifi(context);
            if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE") || intent.getAction().equals("android.net.conn.INET_CONDITION_ACTION")) {
                if (NetworkMonitor.this.mNetworkConnected) {
                    NetworkMonitor.this.mCallback.onNetworkConnected();
                } else {
                    NetworkMonitor.this.mCallback.onNetworkDisconnected();
                }
            }
        }
    };

    public interface Callback {
        void onNetworkConnected();

        void onNetworkDisconnected();
    }

    public NetworkMonitor(Context context, Callback callback) {
        this.mContext = null;
        this.mCallback = null;
        this.mContext = context;
        this.mCallback = callback;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.conn.INET_CONDITION_ACTION");
        context.registerReceiver(this.mBroadcastReceiver, filter);
        this.mReceiverRegistered = true;
    }

    public synchronized void close() {
        if (this.mCallback != null) {
            this.mCallback = null;
            if (this.mReceiverRegistered) {
                this.mContext.unregisterReceiver(this.mBroadcastReceiver);
                this.mReceiverRegistered = false;
            }
        }
    }

    public static boolean isConnectedToWifi(Context context) {
        NetworkInfo ni;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (cm == null || (ni = cm.getActiveNetworkInfo()) == null) {
            return false;
        }
        NetworkInfo.DetailedState detailedState = ni.getDetailedState();
        return detailedState.equals(NetworkInfo.DetailedState.CONNECTED);
    }
}
