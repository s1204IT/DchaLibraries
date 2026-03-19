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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

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

    public RemoteDisplayProviderWatcher(Context context, Callback callback, Handler handler, int userId) {
        this.mContext = context;
        this.mCallback = callback;
        this.mHandler = handler;
        this.mUserId = userId;
        this.mPackageManager = context.getPackageManager();
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "Watcher");
        pw.println(prefix + "  mUserId=" + this.mUserId);
        pw.println(prefix + "  mRunning=" + this.mRunning);
        pw.println(prefix + "  mProviders.size()=" + this.mProviders.size());
    }

    public void start() {
        if (this.mRunning) {
            return;
        }
        this.mRunning = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_RESTARTED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mScanPackagesReceiver, new UserHandle(this.mUserId), filter, null, this.mHandler);
        this.mHandler.post(this.mScanPackagesRunnable);
    }

    public void stop() {
        if (!this.mRunning) {
            return;
        }
        this.mRunning = false;
        this.mContext.unregisterReceiver(this.mScanPackagesReceiver);
        this.mHandler.removeCallbacks(this.mScanPackagesRunnable);
        for (int i = this.mProviders.size() - 1; i >= 0; i--) {
            this.mProviders.get(i).stop();
        }
    }

    private void scanPackages() {
        if (!this.mRunning) {
            return;
        }
        int targetIndex = 0;
        Intent intent = new Intent("com.android.media.remotedisplay.RemoteDisplayProvider");
        for (ResolveInfo resolveInfo : this.mPackageManager.queryIntentServicesAsUser(intent, 0, this.mUserId)) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo != null && verifyServiceTrusted(serviceInfo)) {
                int sourceIndex = findProvider(serviceInfo.packageName, serviceInfo.name);
                if (sourceIndex < 0) {
                    RemoteDisplayProviderProxy provider = new RemoteDisplayProviderProxy(this.mContext, new ComponentName(serviceInfo.packageName, serviceInfo.name), this.mUserId);
                    provider.start();
                    this.mProviders.add(targetIndex, provider);
                    this.mCallback.addProvider(provider);
                    targetIndex++;
                } else if (sourceIndex >= targetIndex) {
                    RemoteDisplayProviderProxy provider2 = this.mProviders.get(sourceIndex);
                    provider2.start();
                    provider2.rebindIfDisconnected();
                    Collections.swap(this.mProviders, sourceIndex, targetIndex);
                    targetIndex++;
                }
            }
        }
        if (targetIndex >= this.mProviders.size()) {
            return;
        }
        for (int i = this.mProviders.size() - 1; i >= targetIndex; i--) {
            RemoteDisplayProviderProxy provider3 = this.mProviders.get(i);
            this.mCallback.removeProvider(provider3);
            this.mProviders.remove(provider3);
            provider3.stop();
        }
    }

    private boolean verifyServiceTrusted(ServiceInfo serviceInfo) {
        if (serviceInfo.permission == null || !serviceInfo.permission.equals("android.permission.BIND_REMOTE_DISPLAY")) {
            Slog.w(TAG, "Ignoring remote display provider service because it did not require the BIND_REMOTE_DISPLAY permission in its manifest: " + serviceInfo.packageName + "/" + serviceInfo.name);
            return false;
        }
        if (hasCaptureVideoPermission(serviceInfo.packageName)) {
            return true;
        }
        Slog.w(TAG, "Ignoring remote display provider service because it does not have the CAPTURE_VIDEO_OUTPUT or CAPTURE_SECURE_VIDEO_OUTPUT permission: " + serviceInfo.packageName + "/" + serviceInfo.name);
        return false;
    }

    private boolean hasCaptureVideoPermission(String packageName) {
        return this.mPackageManager.checkPermission("android.permission.CAPTURE_VIDEO_OUTPUT", packageName) == 0 || this.mPackageManager.checkPermission("android.permission.CAPTURE_SECURE_VIDEO_OUTPUT", packageName) == 0;
    }

    private int findProvider(String packageName, String className) {
        int count = this.mProviders.size();
        for (int i = 0; i < count; i++) {
            RemoteDisplayProviderProxy provider = this.mProviders.get(i);
            if (provider.hasComponentName(packageName, className)) {
                return i;
            }
        }
        return -1;
    }
}
