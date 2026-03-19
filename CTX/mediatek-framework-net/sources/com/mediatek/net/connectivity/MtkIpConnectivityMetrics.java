package com.mediatek.net.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetdEventCallback;
import android.net.NetworkCapabilities;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseBooleanArray;
import com.android.server.connectivity.NetdEventListenerService;
import com.android.server.net.BaseNetdEventCallback;
import com.mediatek.net.connectivity.IMtkIpConnectivityMetrics;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.powerhal.PowerHalManager;

public final class MtkIpConnectivityMetrics {
    private static final boolean DBG = true;
    private static final boolean FEATURE_SUPPORTED = true;
    private static final String TAG = MtkIpConnectivityMetrics.class.getSimpleName();
    private Context mContext;
    public Impl mImpl;
    private NetdEventListenerService mNetdEventListenerService;
    private PowerHalManager mPowerHalManager = MtkSystemServiceFactory.getInstance().makePowerHalManager();
    private final INetdEventCallback mNetdEventListener = new BaseNetdEventCallback() {
        public void onDnsEvent(String str, String[] strArr, int i, long j, int i2) {
            MtkIpConnectivityMetrics.this.mImpl.onCtaDnsEvent(i2);
            MtkIpConnectivityMetrics.this.mImpl.onMonitorDnsEvent(str, i, i2);
        }

        public synchronized void onConnectEvent(String str, int i, long j, int i2) {
            MtkIpConnectivityMetrics.this.mImpl.onCtaConnectEvent(i2);
            MtkIpConnectivityMetrics.this.mImpl.onMonitorConnectEvent(i2);
        }
    };

    public MtkIpConnectivityMetrics(Context context, NetdEventListenerService netdEventListenerService) {
        Log.d(TAG, "MtkIpConnectivityMetrics is created:true");
        this.mContext = context;
        this.mNetdEventListenerService = netdEventListenerService;
        this.mImpl = new Impl(this.mContext);
        try {
            this.mNetdEventListenerService.addNetdEventCallback(3, this.mNetdEventListener);
        } catch (Exception e) {
            Log.e(TAG, "MtkIpConnectivityMetrics addNetdEventCallback:" + e);
        }
    }

    public IBinder getMtkIpConnSrv() {
        return this.mImpl;
    }

    public final class Impl extends IMtkIpConnectivityMetrics.Stub {
        private ConnectivityManager mCm;
        private Context mContext;
        private INetdEventCallback mNetdEventCallback;
        private INetdEventCallback mSocketEventCallback;
        private SparseBooleanArray mUidSocketRules = new SparseBooleanArray();
        final Object mUidSockeRulestLock = new Object();

        public Impl(Context context) {
            this.mContext = context;
            this.mCm = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        }

        @Override
        public boolean registerMtkNetdEventCallback(INetdEventCallback iNetdEventCallback) {
            if (isPermissionAllowed()) {
                Log.d(MtkIpConnectivityMetrics.TAG, "registerMtkNetdEventCallback");
                this.mNetdEventCallback = iNetdEventCallback;
                return true;
            }
            return false;
        }

        @Override
        public boolean unregisterMtkNetdEventCallback() {
            if (isPermissionAllowed()) {
                Log.d(MtkIpConnectivityMetrics.TAG, "unregisterMtkNetdEventCallback");
                this.mNetdEventCallback = null;
                return true;
            }
            return false;
        }

        @Override
        public boolean registerMtkSocketEventCallback(INetdEventCallback iNetdEventCallback) {
            if (isPermissionAllowed()) {
                Log.d(MtkIpConnectivityMetrics.TAG, "registerMtkSocketEventCallback");
                this.mSocketEventCallback = iNetdEventCallback;
                return true;
            }
            return false;
        }

        @Override
        public boolean unregisterMtkSocketEventCallback() {
            if (isPermissionAllowed()) {
                Log.d(MtkIpConnectivityMetrics.TAG, "unregisterMtkSocketEventCallback");
                this.mSocketEventCallback = null;
                return true;
            }
            return false;
        }

        @Override
        public void updateCtaAppStatus(int i, boolean z) {
            if (!isPermissionAllowed() || i < 10000) {
                return;
            }
            synchronized (this.mUidSockeRulestLock) {
                Log.d(MtkIpConnectivityMetrics.TAG, "updateCtaAppStatus:" + i + ":" + z);
                this.mUidSocketRules.put(i, z);
            }
        }

        @Override
        public void setSpeedDownload(int i) {
            if (MtkIpConnectivityMetrics.this.mPowerHalManager != null) {
                Log.d(MtkIpConnectivityMetrics.TAG, "setSpeedDownload:" + i);
                MtkIpConnectivityMetrics.this.mPowerHalManager.setSpeedDownload(i);
            }
        }

        private void onCtaDnsEvent(int i) {
            if (this.mNetdEventCallback == null || i < 10000) {
                return;
            }
            synchronized (this.mUidSockeRulestLock) {
                boolean z = this.mUidSocketRules.get(i, true);
                Log.d(MtkIpConnectivityMetrics.TAG, "onDnsEvent:" + i + ":" + z);
                if (z) {
                    NetworkCapabilities networkCapabilities = this.mCm.getNetworkCapabilities(this.mCm.getActiveNetwork());
                    if (networkCapabilities == null) {
                        return;
                    }
                    if (networkCapabilities.hasTransport(0)) {
                        try {
                            this.mNetdEventCallback.onDnsEvent("", (String[]) null, 0, 0L, i);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        private void onCtaConnectEvent(int i) {
            if (this.mNetdEventCallback == null || i < 10000) {
                return;
            }
            synchronized (this.mUidSockeRulestLock) {
                boolean z = this.mUidSocketRules.get(i, true);
                Log.d(MtkIpConnectivityMetrics.TAG, "onDnsEvent:" + i + ":" + z);
                if (z) {
                    NetworkCapabilities networkCapabilities = this.mCm.getNetworkCapabilities(this.mCm.getActiveNetwork());
                    if (networkCapabilities == null) {
                        return;
                    }
                    if (networkCapabilities.hasTransport(0)) {
                        try {
                            this.mNetdEventCallback.onConnectEvent("", 0, 0L, i);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        private void onMonitorDnsEvent(String str, int i, int i2) {
            if (this.mSocketEventCallback == null) {
                return;
            }
            try {
                this.mSocketEventCallback.onDnsEvent(str, (String[]) null, i, 0L, i2);
            } catch (Exception e) {
            }
        }

        private void onMonitorConnectEvent(int i) {
            if (this.mSocketEventCallback == null) {
                return;
            }
            try {
                this.mSocketEventCallback.onConnectEvent("", 0, 0L, i);
            } catch (Exception e) {
            }
        }

        private boolean isPermissionAllowed() {
            enforceNetworkMonitorPermission();
            if (Binder.getCallingUid() != 1000) {
                Log.d(MtkIpConnectivityMetrics.TAG, "No permission:" + Binder.getCallingUid());
                return false;
            }
            return true;
        }

        private void enforceNetworkMonitorPermission() {
            int callingUid = Binder.getCallingUid();
            if (callingUid != 1000) {
                throw new SecurityException(String.format("Uid %d has no permission to change watchlist setting.", Integer.valueOf(callingUid)));
            }
        }
    }
}
