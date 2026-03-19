package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.os.Binder;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.mediatek.server.wifi.MtkWifiApmDelegate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class ScanRequestProxy {

    @VisibleForTesting
    public static final int SCAN_REQUEST_THROTTLE_INTERVAL_BG_APPS_MS = 1800000;

    @VisibleForTesting
    public static final int SCAN_REQUEST_THROTTLE_MAX_IN_TIME_WINDOW_FG_APPS = 4;

    @VisibleForTesting
    public static final int SCAN_REQUEST_THROTTLE_TIME_WINDOW_FG_APPS_MS = 120000;
    private static final String TAG = "WifiScanRequestProxy";
    private final ActivityManager mActivityManager;
    private final AppOpsManager mAppOps;
    private final Clock mClock;
    private final Context mContext;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiInjector mWifiInjector;
    private final WifiMetrics mWifiMetrics;
    private final WifiPermissionsUtil mWifiPermissionsUtil;
    private WifiScanner mWifiScanner;
    private boolean mVerboseLoggingEnabled = false;
    private boolean mScanningForHiddenNetworksEnabled = false;
    private boolean mIsScanProcessingComplete = true;
    private long mLastScanTimestampForBgApps = 0;
    private final ArrayMap<Pair<Integer, String>, LinkedList<Long>> mLastScanTimestampsForFgApps = new ArrayMap<>();
    private final List<ScanResult> mLastScanResults = new ArrayList();

    private class ScanRequestProxyScanListener implements WifiScanner.ScanListener {
        private ScanRequestProxyScanListener() {
        }

        public void onSuccess() {
            if (ScanRequestProxy.this.mVerboseLoggingEnabled) {
                Log.d(ScanRequestProxy.TAG, "Scan request succeeded");
            }
        }

        public void onFailure(int i, String str) {
            Log.e(ScanRequestProxy.TAG, "Scan failure received. reason: " + i + ",description: " + str);
            ScanRequestProxy.this.sendScanResultBroadcastIfScanProcessingNotComplete(false);
        }

        public void onResults(WifiScanner.ScanData[] scanDataArr) {
            if (ScanRequestProxy.this.mVerboseLoggingEnabled) {
                Log.d(ScanRequestProxy.TAG, "Scan results received");
            }
            if (scanDataArr.length != 1) {
                Log.wtf(ScanRequestProxy.TAG, "Found more than 1 batch of scan results, Failing...");
                ScanRequestProxy.this.sendScanResultBroadcastIfScanProcessingNotComplete(false);
                return;
            }
            ScanResult[] results = scanDataArr[0].getResults();
            if (ScanRequestProxy.this.mVerboseLoggingEnabled) {
                Log.d(ScanRequestProxy.TAG, "Received " + results.length + " scan results");
            }
            ScanRequestProxy.this.mLastScanResults.clear();
            ScanRequestProxy.this.mLastScanResults.addAll(Arrays.asList(results));
            ScanRequestProxy.this.sendScanResultBroadcastIfScanProcessingNotComplete(true);
        }

        public void onFullResult(ScanResult scanResult) {
        }

        public void onPeriodChanged(int i) {
        }
    }

    ScanRequestProxy(Context context, AppOpsManager appOpsManager, ActivityManager activityManager, WifiInjector wifiInjector, WifiConfigManager wifiConfigManager, WifiPermissionsUtil wifiPermissionsUtil, WifiMetrics wifiMetrics, Clock clock) {
        this.mContext = context;
        this.mAppOps = appOpsManager;
        this.mActivityManager = activityManager;
        this.mWifiInjector = wifiInjector;
        this.mWifiConfigManager = wifiConfigManager;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mWifiMetrics = wifiMetrics;
        this.mClock = clock;
    }

    public void enableVerboseLogging(int i) {
        this.mVerboseLoggingEnabled = i > 0;
    }

    public void enableScanningForHiddenNetworks(boolean z) {
        if (this.mVerboseLoggingEnabled) {
            StringBuilder sb = new StringBuilder();
            sb.append("Scanning for hidden networks is ");
            sb.append(z ? "enabled" : "disabled");
            Log.d(TAG, sb.toString());
        }
        this.mScanningForHiddenNetworksEnabled = z;
    }

    private boolean retrieveWifiScannerIfNecessary() {
        if (this.mWifiScanner == null) {
            this.mWifiScanner = this.mWifiInjector.getWifiScanner();
        }
        return this.mWifiScanner != null;
    }

    private void sendScanResultBroadcastIfScanProcessingNotComplete(boolean z) {
        if (this.mIsScanProcessingComplete) {
            Log.i(TAG, "No ongoing scan request. Don't send scan broadcast.");
        } else {
            sendScanResultBroadcast(z);
            this.mIsScanProcessingComplete = true;
        }
    }

    private void sendScanResultBroadcast(boolean z) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
            intent.addFlags(67108864);
            intent.putExtra("resultsUpdated", z);
            MtkWifiApmDelegate.getInstance().fillExtraInfo(intent);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void sendScanResultFailureBroadcastToPackage(String str) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
            intent.addFlags(67108864);
            intent.putExtra("resultsUpdated", false);
            intent.setPackage(str);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void trimPastScanRequestTimesForForegroundApp(List<Long> list, long j) {
        Iterator<Long> it = list.iterator();
        while (it.hasNext() && j - it.next().longValue() > 120000) {
            it.remove();
        }
    }

    private LinkedList<Long> getOrCreateScanRequestTimestampsForForegroundApp(int i, String str) {
        Pair<Integer, String> pairCreate = Pair.create(Integer.valueOf(i), str);
        LinkedList<Long> linkedList = this.mLastScanTimestampsForFgApps.get(pairCreate);
        if (linkedList == null) {
            LinkedList<Long> linkedList2 = new LinkedList<>();
            this.mLastScanTimestampsForFgApps.put(pairCreate, linkedList2);
            return linkedList2;
        }
        return linkedList;
    }

    private boolean shouldScanRequestBeThrottledForForegroundApp(int i, String str) {
        LinkedList<Long> orCreateScanRequestTimestampsForForegroundApp = getOrCreateScanRequestTimestampsForForegroundApp(i, str);
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        trimPastScanRequestTimesForForegroundApp(orCreateScanRequestTimestampsForForegroundApp, elapsedSinceBootMillis);
        if (orCreateScanRequestTimestampsForForegroundApp.size() >= 4) {
            return true;
        }
        orCreateScanRequestTimestampsForForegroundApp.addLast(Long.valueOf(elapsedSinceBootMillis));
        return false;
    }

    private boolean shouldScanRequestBeThrottledForBackgroundApp() {
        long j = this.mLastScanTimestampForBgApps;
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        if (j != 0 && elapsedSinceBootMillis - j < 1800000) {
            return true;
        }
        this.mLastScanTimestampForBgApps = elapsedSinceBootMillis;
        return false;
    }

    private boolean isRequestFromBackground(int i, String str) {
        this.mAppOps.checkPackage(i, str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mActivityManager.getPackageImportance(str) > 125;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean shouldScanRequestBeThrottledForApp(int i, String str) {
        boolean zShouldScanRequestBeThrottledForForegroundApp;
        if (isRequestFromBackground(i, str)) {
            zShouldScanRequestBeThrottledForForegroundApp = shouldScanRequestBeThrottledForBackgroundApp();
            if (zShouldScanRequestBeThrottledForForegroundApp) {
                if (this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "Background scan app request [" + i + ", " + str + "]");
                }
                this.mWifiMetrics.incrementExternalBackgroundAppOneshotScanRequestsThrottledCount();
            }
        } else {
            zShouldScanRequestBeThrottledForForegroundApp = shouldScanRequestBeThrottledForForegroundApp(i, str);
            if (zShouldScanRequestBeThrottledForForegroundApp) {
                if (this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "Foreground scan app request [" + i + ", " + str + "]");
                }
                this.mWifiMetrics.incrementExternalForegroundAppOneshotScanRequestsThrottledCount();
            }
        }
        this.mWifiMetrics.incrementExternalAppOneshotScanRequestsCount();
        return zShouldScanRequestBeThrottledForForegroundApp;
    }

    public boolean startScan(int i, String str) {
        if (!retrieveWifiScannerIfNecessary()) {
            Log.e(TAG, "Failed to retrieve wifiscanner");
            sendScanResultFailureBroadcastToPackage(str);
            return false;
        }
        boolean z = this.mWifiPermissionsUtil.checkNetworkSettingsPermission(i) || this.mWifiPermissionsUtil.checkNetworkSetupWizardPermission(i);
        if (!z && shouldScanRequestBeThrottledForApp(i, str)) {
            Log.i(TAG, "Scan request from " + str + " throttled");
            sendScanResultFailureBroadcastToPackage(str);
            return false;
        }
        WorkSource workSource = new WorkSource(i);
        WifiScanner.ScanSettings scanSettings = new WifiScanner.ScanSettings();
        if (z) {
            scanSettings.type = 2;
        }
        scanSettings.band = 7;
        scanSettings.reportEvents = 3;
        if (this.mScanningForHiddenNetworksEnabled) {
            List<WifiScanner.ScanSettings.HiddenNetwork> listRetrieveHiddenNetworkList = this.mWifiConfigManager.retrieveHiddenNetworkList();
            scanSettings.hiddenNetworks = (WifiScanner.ScanSettings.HiddenNetwork[]) listRetrieveHiddenNetworkList.toArray(new WifiScanner.ScanSettings.HiddenNetwork[listRetrieveHiddenNetworkList.size()]);
        }
        this.mWifiScanner.startScan(scanSettings, new ScanRequestProxyScanListener(), workSource);
        this.mIsScanProcessingComplete = false;
        return true;
    }

    public List<ScanResult> getScanResults() {
        return this.mLastScanResults;
    }

    public void clearScanResults() {
        this.mLastScanResults.clear();
        this.mLastScanTimestampForBgApps = 0L;
        this.mLastScanTimestampsForFgApps.clear();
    }

    public void clearScanRequestTimestampsForApp(String str, int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Clearing scan request timestamps for uid=" + i + ", packageName=" + str);
        }
        this.mLastScanTimestampsForFgApps.remove(Pair.create(Integer.valueOf(i), str));
    }
}
