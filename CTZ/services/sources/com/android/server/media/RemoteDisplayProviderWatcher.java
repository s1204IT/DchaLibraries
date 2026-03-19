package com.android.server.media;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public final class RemoteDisplayProviderWatcher {
    private final Callback mCallback;
    private final Context mContext;
    private final Handler mHandler;
    private final PackageManager mPackageManager;
    private boolean mRunning;
    private final int mUserId;
    private static final String TAG = "RemoteDisplayProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private final ArrayList<RemoteDisplayProviderProxy> mProviders = new ArrayList<>();
    private final BroadcastReceiver mScanPackagesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RemoteDisplayProviderWatcher.DEBUG) {
                Slog.d(RemoteDisplayProviderWatcher.TAG, "Received package manager broadcast: " + intent);
            }
            RemoteDisplayProviderWatcher.this.scanPackages();
        }
    };
    private final Runnable mScanPackagesRunnable = new Runnable() {
        @Override
        public void run() {
            RemoteDisplayProviderWatcher.this.scanPackages();
        }
    };

    public interface Callback {
        void addProvider(RemoteDisplayProviderProxy remoteDisplayProviderProxy);

        void removeProvider(RemoteDisplayProviderProxy remoteDisplayProviderProxy);
    }

    public RemoteDisplayProviderWatcher(Context context, Callback callback, Handler handler, int i) {
        this.mContext = context;
        this.mCallback = callback;
        this.mHandler = handler;
        this.mUserId = i;
        this.mPackageManager = context.getPackageManager();
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "Watcher");
        printWriter.println(str + "  mUserId=" + this.mUserId);
        printWriter.println(str + "  mRunning=" + this.mRunning);
        printWriter.println(str + "  mProviders.size()=" + this.mProviders.size());
    }

    public void start() {
        if (!this.mRunning) {
            this.mRunning = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
            intentFilter.addAction("android.intent.action.PACKAGE_RESTARTED");
            intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
            this.mContext.registerReceiverAsUser(this.mScanPackagesReceiver, new UserHandle(this.mUserId), intentFilter, null, this.mHandler);
            this.mHandler.post(this.mScanPackagesRunnable);
        }
    }

    public void stop() {
        if (this.mRunning) {
            this.mRunning = false;
            this.mContext.unregisterReceiver(this.mScanPackagesReceiver);
            this.mHandler.removeCallbacks(this.mScanPackagesRunnable);
            for (int size = this.mProviders.size() - 1; size >= 0; size--) {
                this.mProviders.get(size).stop();
            }
        }
    }

    private void scanPackages() {
        int i;
        if (!this.mRunning) {
            return;
        }
        int i2 = 0;
        Iterator it = this.mPackageManager.queryIntentServicesAsUser(new Intent("com.android.media.remotedisplay.RemoteDisplayProvider"), 0, this.mUserId).iterator();
        while (it.hasNext()) {
            ServiceInfo serviceInfo = ((ResolveInfo) it.next()).serviceInfo;
            if (serviceInfo != null && verifyServiceTrusted(serviceInfo)) {
                int iFindProvider = findProvider(serviceInfo.packageName, serviceInfo.name);
                if (iFindProvider < 0) {
                    RemoteDisplayProviderProxy remoteDisplayProviderProxy = new RemoteDisplayProviderProxy(this.mContext, new ComponentName(serviceInfo.packageName, serviceInfo.name), this.mUserId);
                    remoteDisplayProviderProxy.start();
                    i = i2 + 1;
                    this.mProviders.add(i2, remoteDisplayProviderProxy);
                    this.mCallback.addProvider(remoteDisplayProviderProxy);
                } else if (iFindProvider >= i2) {
                    RemoteDisplayProviderProxy remoteDisplayProviderProxy2 = this.mProviders.get(iFindProvider);
                    remoteDisplayProviderProxy2.start();
                    remoteDisplayProviderProxy2.rebindIfDisconnected();
                    i = i2 + 1;
                    Collections.swap(this.mProviders, iFindProvider, i2);
                }
                i2 = i;
            }
        }
        if (i2 < this.mProviders.size()) {
            for (int size = this.mProviders.size() - 1; size >= i2; size--) {
                RemoteDisplayProviderProxy remoteDisplayProviderProxy3 = this.mProviders.get(size);
                this.mCallback.removeProvider(remoteDisplayProviderProxy3);
                this.mProviders.remove(remoteDisplayProviderProxy3);
                remoteDisplayProviderProxy3.stop();
            }
        }
    }

    private boolean verifyServiceTrusted(ServiceInfo serviceInfo) {
        if (serviceInfo.permission == null || !serviceInfo.permission.equals("android.permission.BIND_REMOTE_DISPLAY")) {
            Slog.w(TAG, "Ignoring remote display provider service because it did not require the BIND_REMOTE_DISPLAY permission in its manifest: " + serviceInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + serviceInfo.name);
            return false;
        }
        if (!hasCaptureVideoPermission(serviceInfo.packageName)) {
            Slog.w(TAG, "Ignoring remote display provider service because it does not have the CAPTURE_VIDEO_OUTPUT or CAPTURE_SECURE_VIDEO_OUTPUT permission: " + serviceInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + serviceInfo.name);
            return false;
        }
        return true;
    }

    private boolean hasCaptureVideoPermission(String str) {
        return this.mPackageManager.checkPermission("android.permission.CAPTURE_VIDEO_OUTPUT", str) == 0 || this.mPackageManager.checkPermission("android.permission.CAPTURE_SECURE_VIDEO_OUTPUT", str) == 0;
    }

    private int findProvider(String str, String str2) {
        int size = this.mProviders.size();
        for (int i = 0; i < size; i++) {
            if (this.mProviders.get(i).hasComponentName(str, str2)) {
                return i;
            }
        }
        return -1;
    }
}
