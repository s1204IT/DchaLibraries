package com.android.server.devicepolicy;

import android.app.admin.ConnectEvent;
import android.app.admin.DnsEvent;
import android.app.admin.NetworkEvent;
import android.content.pm.PackageManagerInternal;
import android.net.IIpConnectivityMetrics;
import android.net.INetdEventCallback;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.server.ServiceThread;
import com.android.server.net.BaseNetdEventCallback;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class NetworkLogger {
    private static final String TAG = NetworkLogger.class.getSimpleName();
    private final DevicePolicyManagerService mDpm;
    private ServiceThread mHandlerThread;
    private IIpConnectivityMetrics mIpConnectivityMetrics;
    private final AtomicBoolean mIsLoggingEnabled = new AtomicBoolean(false);
    private final INetdEventCallback mNetdEventCallback = new BaseNetdEventCallback() {
        public void onDnsEvent(String str, String[] strArr, int i, long j, int i2) {
            if (!NetworkLogger.this.mIsLoggingEnabled.get()) {
                return;
            }
            sendNetworkEvent(new DnsEvent(str, strArr, i, NetworkLogger.this.mPm.getNameForUid(i2), j));
        }

        public void onConnectEvent(String str, int i, long j, int i2) {
            if (!NetworkLogger.this.mIsLoggingEnabled.get()) {
                return;
            }
            sendNetworkEvent(new ConnectEvent(str, i, NetworkLogger.this.mPm.getNameForUid(i2), j));
        }

        private void sendNetworkEvent(NetworkEvent networkEvent) {
            Message messageObtainMessage = NetworkLogger.this.mNetworkLoggingHandler.obtainMessage(1);
            Bundle bundle = new Bundle();
            bundle.putParcelable("network_event", networkEvent);
            messageObtainMessage.setData(bundle);
            NetworkLogger.this.mNetworkLoggingHandler.sendMessage(messageObtainMessage);
        }
    };
    private NetworkLoggingHandler mNetworkLoggingHandler;
    private final PackageManagerInternal mPm;

    NetworkLogger(DevicePolicyManagerService devicePolicyManagerService, PackageManagerInternal packageManagerInternal) {
        this.mDpm = devicePolicyManagerService;
        this.mPm = packageManagerInternal;
    }

    private boolean checkIpConnectivityMetricsService() {
        if (this.mIpConnectivityMetrics != null) {
            return true;
        }
        IIpConnectivityMetrics iIpConnectivityMetrics = this.mDpm.mInjector.getIIpConnectivityMetrics();
        if (iIpConnectivityMetrics == null) {
            return false;
        }
        this.mIpConnectivityMetrics = iIpConnectivityMetrics;
        return true;
    }

    boolean startNetworkLogging() {
        Log.d(TAG, "Starting network logging.");
        if (!checkIpConnectivityMetricsService()) {
            Slog.wtf(TAG, "Failed to register callback with IIpConnectivityMetrics.");
            return false;
        }
        try {
            if (!this.mIpConnectivityMetrics.addNetdEventCallback(1, this.mNetdEventCallback)) {
                return false;
            }
            this.mHandlerThread = new ServiceThread(TAG, 10, false);
            this.mHandlerThread.start();
            this.mNetworkLoggingHandler = new NetworkLoggingHandler(this.mHandlerThread.getLooper(), this.mDpm);
            this.mNetworkLoggingHandler.scheduleBatchFinalization();
            this.mIsLoggingEnabled.set(true);
            return true;
        } catch (RemoteException e) {
            Slog.wtf(TAG, "Failed to make remote calls to register the callback", e);
            return false;
        }
    }

    boolean stopNetworkLogging() {
        Log.d(TAG, "Stopping network logging");
        this.mIsLoggingEnabled.set(false);
        discardLogs();
        try {
            try {
                if (checkIpConnectivityMetricsService()) {
                    boolean zRemoveNetdEventCallback = this.mIpConnectivityMetrics.removeNetdEventCallback(1);
                    if (this.mHandlerThread != null) {
                        this.mHandlerThread.quitSafely();
                    }
                    return zRemoveNetdEventCallback;
                }
                Slog.wtf(TAG, "Failed to unregister callback with IIpConnectivityMetrics.");
                if (this.mHandlerThread != null) {
                    this.mHandlerThread.quitSafely();
                }
                return true;
            } catch (RemoteException e) {
                Slog.wtf(TAG, "Failed to make remote calls to unregister the callback", e);
                if (this.mHandlerThread != null) {
                    this.mHandlerThread.quitSafely();
                }
                return true;
            }
        } catch (Throwable th) {
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quitSafely();
            }
            throw th;
        }
    }

    void pause() {
        if (this.mNetworkLoggingHandler != null) {
            this.mNetworkLoggingHandler.pause();
        }
    }

    void resume() {
        if (this.mNetworkLoggingHandler != null) {
            this.mNetworkLoggingHandler.resume();
        }
    }

    void discardLogs() {
        if (this.mNetworkLoggingHandler != null) {
            this.mNetworkLoggingHandler.discardLogs();
        }
    }

    List<NetworkEvent> retrieveLogs(long j) {
        return this.mNetworkLoggingHandler.retrieveFullLogBatch(j);
    }
}
