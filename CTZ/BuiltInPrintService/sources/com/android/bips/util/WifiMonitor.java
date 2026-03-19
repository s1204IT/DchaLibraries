package com.android.bips.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.android.bips.BuiltInPrintService;

public class WifiMonitor {
    private static final String TAG = WifiMonitor.class.getSimpleName();
    private BroadcastMonitor mBroadcasts;
    private Boolean mConnected;
    private Listener mListener;

    public interface Listener {
        void onConnectionStateChanged(boolean z);
    }

    public WifiMonitor(BuiltInPrintService builtInPrintService, Listener listener) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) builtInPrintService.getSystemService("connectivity");
        if (connectivityManager == null) {
            return;
        }
        this.mListener = listener;
        this.mBroadcasts = builtInPrintService.receiveBroadcasts(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    boolean z = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                    if (WifiMonitor.this.mListener != null) {
                        if (WifiMonitor.this.mConnected == null || WifiMonitor.this.mConnected.booleanValue() != z) {
                            WifiMonitor.this.mConnected = Boolean.valueOf(z);
                            WifiMonitor.this.mListener.onConnectionStateChanged(WifiMonitor.this.mConnected.booleanValue());
                        }
                    }
                }
            }
        }, "android.net.conn.CONNECTIVITY_CHANGE");
    }

    public void close() {
        if (this.mBroadcasts != null) {
            this.mBroadcasts.close();
        }
        this.mListener = null;
    }
}
