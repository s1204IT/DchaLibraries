package com.android.mms.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import com.android.mms.service.exception.MmsNetworkException;

public class MmsNetworkManager {
    private final Context mContext;
    private final NetworkRequest mNetworkRequest;
    private final int mSubId;
    private ConnectivityManager.NetworkCallback mNetworkCallback = null;
    private Network mNetwork = null;
    private int mMmsRequestCount = 0;
    private volatile ConnectivityManager mConnectivityManager = null;
    private TelephonyManager mTelephonyManager = null;
    private MmsHttpClient mMmsHttpClient = null;
    private boolean mIsNetworkLost = true;
    private boolean mIsNetworkReleased = true;
    private final Handler mReleaseHandler = new Handler(Looper.getMainLooper());
    private final Runnable mNetworkReleaseTask = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                if (MmsNetworkManager.this.mMmsRequestCount < 1) {
                    MmsNetworkManager.this.releaseRequestLocked(MmsNetworkManager.this.mNetworkCallback);
                }
            }
        }
    };

    private class NetworkRequestCallback extends ConnectivityManager.NetworkCallback {
        private NetworkRequestCallback() {
        }

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            LogUtil.i("NetworkCallbackListener.onAvailable: network=" + network);
            synchronized (MmsNetworkManager.this) {
                if (!MmsNetworkManager.this.mIsNetworkReleased) {
                    MmsNetworkManager.this.mNetwork = network;
                    MmsNetworkManager.this.notifyAll();
                }
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            LogUtil.w("NetworkCallbackListener.onLost: network=" + network);
            synchronized (MmsNetworkManager.this) {
                MmsNetworkManager.this.releaseRequestLocked(this);
                MmsNetworkManager.this.notifyAll();
            }
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            LogUtil.w("NetworkCallbackListener.onUnavailable");
            synchronized (MmsNetworkManager.this) {
                MmsNetworkManager.this.releaseRequestLocked(this);
                MmsNetworkManager.this.notifyAll();
            }
        }
    }

    public MmsNetworkManager(Context context, int i) {
        this.mContext = context;
        this.mSubId = i;
        this.mNetworkRequest = new NetworkRequest.Builder().addTransportType(0).addCapability(0).setNetworkSpecifier(Integer.toString(this.mSubId)).build();
    }

    public void acquireNetwork(String str) throws MmsNetworkException {
        LogUtil.d(str, "MmsNetworkManager: acquireNetwork start");
        synchronized (this) {
            this.mReleaseHandler.removeCallbacks(this.mNetworkReleaseTask);
            this.mMmsRequestCount++;
            this.mIsNetworkLost = false;
            this.mIsNetworkReleased = false;
            if (this.mNetwork != null) {
                LogUtil.d(str, "MmsNetworkManager: already available");
                return;
            }
            if (this.mNetworkCallback == null) {
                LogUtil.d(str, "MmsNetworkManager: start new network request");
                startNewNetworkRequestLocked();
            }
            long jElapsedRealtime = 605000;
            long jElapsedRealtime2 = SystemClock.elapsedRealtime() + 605000;
            while (true) {
                if (jElapsedRealtime <= 0) {
                    break;
                }
                try {
                    wait(jElapsedRealtime);
                } catch (InterruptedException e) {
                    LogUtil.w(str, "MmsNetworkManager: acquire network wait interrupted");
                }
                if (this.mNetwork == null) {
                    if (this.mIsNetworkLost) {
                        LogUtil.d(str, "MmsNetworkManager: network already lost!");
                        break;
                    }
                    jElapsedRealtime = jElapsedRealtime2 - SystemClock.elapsedRealtime();
                } else {
                    return;
                }
            }
            LogUtil.e(str, "MmsNetworkManager: timed out");
            releaseRequestLocked(this.mNetworkCallback);
            notifyAll();
            throw new MmsNetworkException("Acquiring network timed out");
        }
    }

    public void releaseNetwork(String str, boolean z) {
        synchronized (this) {
            if (this.mMmsRequestCount > 0) {
                this.mMmsRequestCount--;
                LogUtil.d(str, "MmsNetworkManager: release, count=" + this.mMmsRequestCount + ", delayRelease=" + z);
                if (this.mMmsRequestCount < 1) {
                    if (z) {
                        this.mReleaseHandler.removeCallbacks(this.mNetworkReleaseTask);
                        this.mReleaseHandler.postDelayed(this.mNetworkReleaseTask, 5000L);
                    } else {
                        releaseRequestLocked(this.mNetworkCallback);
                    }
                }
            }
        }
    }

    private void startNewNetworkRequestLocked() {
        ConnectivityManager connectivityManager = getConnectivityManager();
        this.mNetworkCallback = new NetworkRequestCallback();
        if (getTelephonyManager().getDataNetworkType(this.mSubId) == 0) {
            connectivityManager.requestNetwork(this.mNetworkRequest, this.mNetworkCallback, 60000);
        } else {
            connectivityManager.requestNetwork(this.mNetworkRequest, this.mNetworkCallback, 600000);
        }
    }

    private void releaseRequestLocked(ConnectivityManager.NetworkCallback networkCallback) {
        if (networkCallback != null) {
            try {
                getConnectivityManager().unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException e) {
                LogUtil.w("Unregister network callback exception", e);
            }
        }
        resetLocked();
    }

    private void resetLocked() {
        this.mIsNetworkLost = true;
        this.mIsNetworkReleased = true;
        this.mNetworkCallback = null;
        this.mNetwork = null;
        this.mMmsRequestCount = 0;
        this.mMmsHttpClient = null;
    }

    private ConnectivityManager getConnectivityManager() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mConnectivityManager;
    }

    private TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        return this.mTelephonyManager;
    }

    public MmsHttpClient getOrCreateHttpClient() {
        MmsHttpClient mmsHttpClient;
        synchronized (this) {
            if (this.mMmsHttpClient == null && this.mNetwork != null) {
                this.mMmsHttpClient = new MmsHttpClient(this.mContext, this.mNetwork, this.mConnectivityManager);
            }
            mmsHttpClient = this.mMmsHttpClient;
        }
        return mmsHttpClient;
    }

    public String getApnName() {
        synchronized (this) {
            if (this.mNetwork == null) {
                return null;
            }
            NetworkInfo networkInfo = getConnectivityManager().getNetworkInfo(this.mNetwork);
            if (networkInfo != null) {
                return networkInfo.getExtraInfo();
            }
            return null;
        }
    }
}
