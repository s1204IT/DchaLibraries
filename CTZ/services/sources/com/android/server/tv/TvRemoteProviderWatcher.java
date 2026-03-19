package com.android.server.tv;

import android.R;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

final class TvRemoteProviderWatcher {
    private final Context mContext;
    private final Handler mHandler;
    private final PackageManager mPackageManager;
    private final ProviderMethods mProvider;
    private boolean mRunning;
    private final String mUnbundledServicePackage;
    private static final String TAG = "TvRemoteProvWatcher";
    private static final boolean DEBUG = Log.isLoggable(TAG, 2);
    private final ArrayList<TvRemoteProviderProxy> mProviderProxies = new ArrayList<>();
    private final BroadcastReceiver mScanPackagesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TvRemoteProviderWatcher.DEBUG) {
                Slog.d(TvRemoteProviderWatcher.TAG, "Received package manager broadcast: " + intent);
            }
            TvRemoteProviderWatcher.this.mHandler.post(TvRemoteProviderWatcher.this.mScanPackagesRunnable);
        }
    };
    private final Runnable mScanPackagesRunnable = new Runnable() {
        @Override
        public void run() {
            TvRemoteProviderWatcher.this.scanPackages();
        }
    };
    private final int mUserId = UserHandle.myUserId();

    public interface ProviderMethods {
        void addProvider(TvRemoteProviderProxy tvRemoteProviderProxy);

        void removeProvider(TvRemoteProviderProxy tvRemoteProviderProxy);
    }

    public TvRemoteProviderWatcher(Context context, ProviderMethods providerMethods, Handler handler) {
        this.mContext = context;
        this.mProvider = providerMethods;
        this.mHandler = handler;
        this.mPackageManager = context.getPackageManager();
        this.mUnbundledServicePackage = context.getString(R.string.app_category_audio);
    }

    public void start() {
        if (DEBUG) {
            Slog.d(TAG, "start()");
        }
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
            for (int size = this.mProviderProxies.size() - 1; size >= 0; size--) {
                this.mProviderProxies.get(size).stop();
            }
        }
    }

    private void scanPackages() {
        int i;
        if (!this.mRunning) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "scanPackages()");
        }
        int i2 = 0;
        Iterator it = this.mPackageManager.queryIntentServicesAsUser(new Intent("com.android.media.tv.remoteprovider.TvRemoteProvider"), 0, this.mUserId).iterator();
        while (it.hasNext()) {
            ServiceInfo serviceInfo = ((ResolveInfo) it.next()).serviceInfo;
            if (serviceInfo != null && verifyServiceTrusted(serviceInfo)) {
                int iFindProvider = findProvider(serviceInfo.packageName, serviceInfo.name);
                if (iFindProvider < 0) {
                    TvRemoteProviderProxy tvRemoteProviderProxy = new TvRemoteProviderProxy(this.mContext, new ComponentName(serviceInfo.packageName, serviceInfo.name), this.mUserId, serviceInfo.applicationInfo.uid);
                    tvRemoteProviderProxy.start();
                    i = i2 + 1;
                    this.mProviderProxies.add(i2, tvRemoteProviderProxy);
                    this.mProvider.addProvider(tvRemoteProviderProxy);
                } else if (iFindProvider >= i2) {
                    TvRemoteProviderProxy tvRemoteProviderProxy2 = this.mProviderProxies.get(iFindProvider);
                    tvRemoteProviderProxy2.start();
                    tvRemoteProviderProxy2.rebindIfDisconnected();
                    i = i2 + 1;
                    Collections.swap(this.mProviderProxies, iFindProvider, i2);
                }
                i2 = i;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "scanPackages() targetIndex " + i2);
        }
        if (i2 < this.mProviderProxies.size()) {
            for (int size = this.mProviderProxies.size() - 1; size >= i2; size--) {
                TvRemoteProviderProxy tvRemoteProviderProxy3 = this.mProviderProxies.get(size);
                this.mProvider.removeProvider(tvRemoteProviderProxy3);
                this.mProviderProxies.remove(tvRemoteProviderProxy3);
                tvRemoteProviderProxy3.stop();
            }
        }
    }

    private boolean verifyServiceTrusted(ServiceInfo serviceInfo) {
        if (serviceInfo.permission == null || !serviceInfo.permission.equals("android.permission.BIND_TV_REMOTE_SERVICE")) {
            Slog.w(TAG, "Ignoring atv remote provider service because it did not require the BIND_TV_REMOTE_SERVICE permission in its manifest: " + serviceInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + serviceInfo.name);
            return false;
        }
        if (!serviceInfo.packageName.equals(this.mUnbundledServicePackage)) {
            Slog.w(TAG, "Ignoring atv remote provider service because the package has not been set and/or whitelisted: " + serviceInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + serviceInfo.name);
            return false;
        }
        if (!hasNecessaryPermissions(serviceInfo.packageName)) {
            Slog.w(TAG, "Ignoring atv remote provider service because its package does not have TV_VIRTUAL_REMOTE_CONTROLLER permission: " + serviceInfo.packageName);
            return false;
        }
        return true;
    }

    private boolean hasNecessaryPermissions(String str) {
        if (this.mPackageManager.checkPermission("android.permission.TV_VIRTUAL_REMOTE_CONTROLLER", str) == 0) {
            return true;
        }
        return false;
    }

    private int findProvider(String str, String str2) {
        int size = this.mProviderProxies.size();
        for (int i = 0; i < size; i++) {
            if (this.mProviderProxies.get(i).hasComponentName(str, str2)) {
                return i;
            }
        }
        return -1;
    }
}
