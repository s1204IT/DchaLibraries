package com.android.server.net.watchlist;

import android.content.Context;
import android.net.IIpConnectivityMetrics;
import android.net.INetdEventCallback;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.INetworkWatchlistManager;
import com.android.internal.util.DumpUtils;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.net.BaseNetdEventCallback;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class NetworkWatchlistService extends INetworkWatchlistManager.Stub {
    static final boolean DEBUG = false;
    private static final int MAX_NUM_OF_WATCHLIST_DIGESTS = 10000;
    private static final String TAG = NetworkWatchlistService.class.getSimpleName();
    private final WatchlistConfig mConfig;
    private final Context mContext;
    private final ServiceThread mHandlerThread;

    @VisibleForTesting
    IIpConnectivityMetrics mIpConnectivityMetrics;

    @GuardedBy("mLoggingSwitchLock")
    private volatile boolean mIsLoggingEnabled;
    private final Object mLoggingSwitchLock;
    private final INetdEventCallback mNetdEventCallback;

    @VisibleForTesting
    WatchlistLoggingHandler mNetworkWatchlistHandler;

    public static class Lifecycle extends SystemService {
        private NetworkWatchlistService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            if (Settings.Global.getInt(getContext().getContentResolver(), "network_watchlist_enabled", 1) == 0) {
                Slog.i(NetworkWatchlistService.TAG, "Network Watchlist service is disabled");
            } else {
                this.mService = new NetworkWatchlistService(getContext());
                publishBinderService("network_watchlist", this.mService);
            }
        }

        @Override
        public void onBootPhase(int i) {
            if (i == 550) {
                if (Settings.Global.getInt(getContext().getContentResolver(), "network_watchlist_enabled", 1) == 0) {
                    Slog.i(NetworkWatchlistService.TAG, "Network Watchlist service is disabled");
                    return;
                }
                try {
                    this.mService.init();
                    this.mService.initIpConnectivityMetrics();
                    this.mService.startWatchlistLogging();
                } catch (RemoteException e) {
                }
                ReportWatchlistJobService.schedule(getContext());
            }
        }
    }

    public NetworkWatchlistService(Context context) {
        this.mIsLoggingEnabled = false;
        this.mLoggingSwitchLock = new Object();
        this.mNetdEventCallback = new BaseNetdEventCallback() {
            public void onDnsEvent(String str, String[] strArr, int i, long j, int i2) {
                if (!NetworkWatchlistService.this.mIsLoggingEnabled) {
                    return;
                }
                NetworkWatchlistService.this.mNetworkWatchlistHandler.asyncNetworkEvent(str, strArr, i2);
            }

            public void onConnectEvent(String str, int i, long j, int i2) {
                if (!NetworkWatchlistService.this.mIsLoggingEnabled) {
                    return;
                }
                NetworkWatchlistService.this.mNetworkWatchlistHandler.asyncNetworkEvent(null, new String[]{str}, i2);
            }
        };
        this.mContext = context;
        this.mConfig = WatchlistConfig.getInstance();
        this.mHandlerThread = new ServiceThread(TAG, 10, false);
        this.mHandlerThread.start();
        this.mNetworkWatchlistHandler = new WatchlistLoggingHandler(this.mContext, this.mHandlerThread.getLooper());
        this.mNetworkWatchlistHandler.reportWatchlistIfNecessary();
    }

    @VisibleForTesting
    NetworkWatchlistService(Context context, ServiceThread serviceThread, WatchlistLoggingHandler watchlistLoggingHandler, IIpConnectivityMetrics iIpConnectivityMetrics) {
        this.mIsLoggingEnabled = false;
        this.mLoggingSwitchLock = new Object();
        this.mNetdEventCallback = new BaseNetdEventCallback() {
            public void onDnsEvent(String str, String[] strArr, int i, long j, int i2) {
                if (!NetworkWatchlistService.this.mIsLoggingEnabled) {
                    return;
                }
                NetworkWatchlistService.this.mNetworkWatchlistHandler.asyncNetworkEvent(str, strArr, i2);
            }

            public void onConnectEvent(String str, int i, long j, int i2) {
                if (!NetworkWatchlistService.this.mIsLoggingEnabled) {
                    return;
                }
                NetworkWatchlistService.this.mNetworkWatchlistHandler.asyncNetworkEvent(null, new String[]{str}, i2);
            }
        };
        this.mContext = context;
        this.mConfig = WatchlistConfig.getInstance();
        this.mHandlerThread = serviceThread;
        this.mNetworkWatchlistHandler = watchlistLoggingHandler;
        this.mIpConnectivityMetrics = iIpConnectivityMetrics;
    }

    private void init() {
        this.mConfig.removeTestModeConfig();
    }

    private void initIpConnectivityMetrics() {
        this.mIpConnectivityMetrics = IIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService("connmetrics"));
    }

    private boolean isCallerShell() {
        int callingUid = Binder.getCallingUid();
        return callingUid == 2000 || callingUid == 0;
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        if (!isCallerShell()) {
            Slog.w(TAG, "Only shell is allowed to call network watchlist shell commands");
        } else {
            new NetworkWatchlistShellCommand(this, this.mContext).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
        }
    }

    @VisibleForTesting
    protected boolean startWatchlistLoggingImpl() throws RemoteException {
        synchronized (this.mLoggingSwitchLock) {
            if (this.mIsLoggingEnabled) {
                Slog.w(TAG, "Watchlist logging is already running");
                return true;
            }
            try {
                if (!this.mIpConnectivityMetrics.addNetdEventCallback(2, this.mNetdEventCallback)) {
                    return false;
                }
                this.mIsLoggingEnabled = true;
                return true;
            } catch (RemoteException e) {
                return false;
            }
        }
    }

    public boolean startWatchlistLogging() throws RemoteException {
        enforceWatchlistLoggingPermission();
        return startWatchlistLoggingImpl();
    }

    @VisibleForTesting
    protected boolean stopWatchlistLoggingImpl() {
        synchronized (this.mLoggingSwitchLock) {
            if (!this.mIsLoggingEnabled) {
                Slog.w(TAG, "Watchlist logging is not running");
                return true;
            }
            this.mIsLoggingEnabled = false;
            try {
                return this.mIpConnectivityMetrics.removeNetdEventCallback(2);
            } catch (RemoteException e) {
                return false;
            }
        }
    }

    public boolean stopWatchlistLogging() throws RemoteException {
        enforceWatchlistLoggingPermission();
        return stopWatchlistLoggingImpl();
    }

    public byte[] getWatchlistConfigHash() {
        return this.mConfig.getWatchlistConfigHash();
    }

    private void enforceWatchlistLoggingPermission() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            throw new SecurityException(String.format("Uid %d has no permission to change watchlist setting.", Integer.valueOf(callingUid)));
        }
    }

    public void reloadWatchlist() throws RemoteException {
        enforceWatchlistLoggingPermission();
        Slog.i(TAG, "Reloading watchlist");
        this.mConfig.reloadConfig();
    }

    public void reportWatchlistIfNecessary() {
        this.mNetworkWatchlistHandler.reportWatchlistIfNecessary();
    }

    public boolean forceReportWatchlistForTest(long j) {
        if (this.mConfig.isConfigSecure()) {
            return false;
        }
        this.mNetworkWatchlistHandler.forceReportWatchlistForTest(j);
        return true;
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            this.mConfig.dump(fileDescriptor, printWriter, strArr);
        }
    }
}
