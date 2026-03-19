package com.android.server.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

public class NetworkPinner extends ConnectivityManager.NetworkCallback {

    @GuardedBy("sLock")
    private static ConnectivityManager sCM;

    @GuardedBy("sLock")
    private static Callback sCallback;

    @GuardedBy("sLock")
    @VisibleForTesting
    protected static Network sNetwork;
    private static final String TAG = NetworkPinner.class.getSimpleName();

    @VisibleForTesting
    protected static final Object sLock = new Object();

    private static void maybeInitConnectivityManager(Context context) {
        if (sCM == null) {
            sCM = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (sCM == null) {
                throw new IllegalStateException("Bad luck, ConnectivityService not started.");
            }
        }
    }

    private static class Callback extends ConnectivityManager.NetworkCallback {
        private Callback() {
        }

        @Override
        public void onAvailable(Network network) {
            synchronized (NetworkPinner.sLock) {
                if (this != NetworkPinner.sCallback) {
                    return;
                }
                if (NetworkPinner.sCM.getBoundNetworkForProcess() == null && NetworkPinner.sNetwork == null) {
                    NetworkPinner.sCM.bindProcessToNetwork(network);
                    NetworkPinner.sNetwork = network;
                    Log.d(NetworkPinner.TAG, "Wifi alternate reality enabled on network " + network);
                }
                NetworkPinner.sLock.notify();
            }
        }

        @Override
        public void onLost(Network network) {
            synchronized (NetworkPinner.sLock) {
                if (this != NetworkPinner.sCallback) {
                    return;
                }
                if (network.equals(NetworkPinner.sNetwork) && network.equals(NetworkPinner.sCM.getBoundNetworkForProcess())) {
                    NetworkPinner.unpin();
                    Log.d(NetworkPinner.TAG, "Wifi alternate reality disabled on network " + network);
                }
                NetworkPinner.sLock.notify();
            }
        }
    }

    public static void pin(Context context, NetworkRequest networkRequest) {
        synchronized (sLock) {
            if (sCallback == null) {
                maybeInitConnectivityManager(context);
                sCallback = new Callback();
                try {
                    sCM.registerNetworkCallback(networkRequest, sCallback);
                } catch (SecurityException e) {
                    Log.d(TAG, "Failed to register network callback", e);
                    sCallback = null;
                }
            }
        }
    }

    public static void unpin() {
        synchronized (sLock) {
            if (sCallback != null) {
                try {
                    sCM.bindProcessToNetwork(null);
                    sCM.unregisterNetworkCallback(sCallback);
                } catch (SecurityException e) {
                    Log.d(TAG, "Failed to unregister network callback", e);
                }
                sCallback = null;
                sNetwork = null;
            }
        }
    }
}
