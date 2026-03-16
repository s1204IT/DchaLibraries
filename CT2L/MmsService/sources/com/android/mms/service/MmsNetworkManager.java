package com.android.mms.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.SystemClock;
import android.util.Log;
import com.android.mms.service.exception.MmsNetworkException;
import com.android.okhttp.ConnectionPool;
import com.android.okhttp.HostResolver;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MmsNetworkManager implements HostResolver {
    private static final InetAddress[] EMPTY_ADDRESS_ARRAY;
    private static final boolean httpKeepAlive = Boolean.parseBoolean(System.getProperty("http.keepAlive", "true"));
    private static final long httpKeepAliveDurationMs;
    private static final int httpMaxConnections;
    private final Context mContext;
    private final NetworkRequest mNetworkRequest;
    private final int mSubId;
    private ConnectivityManager.NetworkCallback mNetworkCallback = null;
    private Network mNetwork = null;
    private int mMmsRequestCount = 0;
    private volatile ConnectivityManager mConnectivityManager = null;
    private ConnectionPool mConnectionPool = null;
    private MmsHttpClient mMmsHttpClient = null;

    static {
        httpMaxConnections = httpKeepAlive ? Integer.parseInt(System.getProperty("http.maxConnections", "5")) : 0;
        httpKeepAliveDurationMs = Long.parseLong(System.getProperty("http.keepAliveDuration", "300000"));
        EMPTY_ADDRESS_ARRAY = new InetAddress[0];
    }

    public MmsNetworkManager(Context context, int subId) {
        this.mContext = context;
        this.mSubId = subId;
        this.mNetworkRequest = new NetworkRequest.Builder().addTransportType(0).addCapability(0).setNetworkSpecifier(Integer.toString(this.mSubId)).build();
    }

    public void acquireNetwork() throws MmsNetworkException {
        synchronized (this) {
            this.mMmsRequestCount++;
            if (this.mNetwork != null) {
                Log.d("MmsService", "MmsNetworkManager: already available");
                return;
            }
            Log.d("MmsService", "MmsNetworkManager: start new network request");
            newRequest();
            long shouldEnd = SystemClock.elapsedRealtime() + 65000;
            for (long waitTime = 65000; waitTime > 0; waitTime = shouldEnd - SystemClock.elapsedRealtime()) {
                try {
                    wait(waitTime);
                } catch (InterruptedException e) {
                    Log.w("MmsService", "MmsNetworkManager: acquire network wait interrupted");
                }
                if (this.mNetwork != null) {
                    return;
                }
            }
            Log.d("MmsService", "MmsNetworkManager: timed out");
            releaseRequestLocked(this.mNetworkCallback);
            throw new MmsNetworkException("Acquiring network timed out");
        }
    }

    public void releaseNetwork() {
        synchronized (this) {
            if (this.mMmsRequestCount > 0) {
                this.mMmsRequestCount--;
                Log.d("MmsService", "MmsNetworkManager: release, count=" + this.mMmsRequestCount);
                if (this.mMmsRequestCount < 1) {
                    releaseRequestLocked(this.mNetworkCallback);
                }
            }
        }
    }

    private void newRequest() {
        ConnectivityManager connectivityManager = getConnectivityManager();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d("MmsService", "NetworkCallbackListener.onAvailable: network=" + network);
                synchronized (MmsNetworkManager.this) {
                    MmsNetworkManager.this.mNetwork = network;
                    MmsNetworkManager.this.notifyAll();
                }
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.d("MmsService", "NetworkCallbackListener.onLost: network=" + network);
                synchronized (MmsNetworkManager.this) {
                    MmsNetworkManager.this.releaseRequestLocked(this);
                    MmsNetworkManager.this.notifyAll();
                }
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.d("MmsService", "NetworkCallbackListener.onUnavailable");
                synchronized (MmsNetworkManager.this) {
                    MmsNetworkManager.this.releaseRequestLocked(this);
                    MmsNetworkManager.this.notifyAll();
                }
            }
        };
        connectivityManager.requestNetwork(this.mNetworkRequest, this.mNetworkCallback, 60000);
    }

    private void releaseRequestLocked(ConnectivityManager.NetworkCallback callback) {
        if (callback != null) {
            ConnectivityManager connectivityManager = getConnectivityManager();
            connectivityManager.unregisterNetworkCallback(callback);
        }
        resetLocked();
    }

    private void resetLocked() {
        this.mNetworkCallback = null;
        this.mNetwork = null;
        this.mMmsRequestCount = 0;
        this.mConnectionPool = null;
        this.mMmsHttpClient = null;
    }

    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        synchronized (this) {
            if (this.mNetwork == null) {
                return EMPTY_ADDRESS_ARRAY;
            }
            Network network = this.mNetwork;
            return network.getAllByName(host);
        }
    }

    private ConnectivityManager getConnectivityManager() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mConnectivityManager;
    }

    private ConnectionPool getOrCreateConnectionPoolLocked() {
        if (this.mConnectionPool == null) {
            this.mConnectionPool = new ConnectionPool(httpMaxConnections, httpKeepAliveDurationMs);
        }
        return this.mConnectionPool;
    }

    public MmsHttpClient getOrCreateHttpClient() {
        MmsHttpClient mmsHttpClient;
        synchronized (this) {
            if (this.mMmsHttpClient == null && this.mNetwork != null) {
                this.mMmsHttpClient = new MmsHttpClient(this.mContext, this.mNetwork.getSocketFactory(), this, getOrCreateConnectionPoolLocked());
            }
            mmsHttpClient = this.mMmsHttpClient;
        }
        return mmsHttpClient;
    }

    public String getApnName() {
        String apnName;
        synchronized (this) {
            if (this.mNetwork == null) {
                Log.d("MmsService", "MmsNetworkManager: getApnName: network not available");
                apnName = null;
            } else {
                Network network = this.mNetwork;
                apnName = null;
                ConnectivityManager connectivityManager = getConnectivityManager();
                NetworkInfo mmsNetworkInfo = connectivityManager.getNetworkInfo(network);
                if (mmsNetworkInfo != null) {
                    apnName = mmsNetworkInfo.getExtraInfo();
                }
                Log.d("MmsService", "MmsNetworkManager: getApnName: " + apnName);
            }
        }
        return apnName;
    }
}
