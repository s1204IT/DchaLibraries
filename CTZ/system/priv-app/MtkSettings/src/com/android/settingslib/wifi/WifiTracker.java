package com.android.settingslib.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkKey;
import android.net.NetworkRequest;
import android.net.NetworkScoreManager;
import android.net.ScoredNetwork;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkScoreCache;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;
import com.android.settingslib.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.wifi.WifiTracker;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: classes.dex */
public class WifiTracker implements LifecycleObserver, OnDestroy, OnStart, OnStop {
    private static final long DEFAULT_MAX_CACHED_SCORE_AGE_MILLIS = 1200000;
    private static final long MAX_SCAN_RESULT_AGE_MILLIS = 25000;
    private static final String TAG = "WifiTracker";
    private static final int WIFI_RESCAN_INTERVAL_MS = 10000;
    public static boolean sVerboseLogging;
    private final AtomicBoolean mConnected;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final IntentFilter mFilter;
    private final List<AccessPoint> mInternalAccessPoints;
    private WifiInfo mLastInfo;
    private NetworkInfo mLastNetworkInfo;
    private final WifiListenerExecutor mListener;
    private final Object mLock;
    private long mMaxSpeedLabelScoreCacheAge;
    private WifiTrackerNetworkCallback mNetworkCallback;
    private final NetworkRequest mNetworkRequest;
    private final NetworkScoreManager mNetworkScoreManager;
    private boolean mNetworkScoringUiEnabled;
    final BroadcastReceiver mReceiver;
    private boolean mRegistered;
    private final Set<NetworkKey> mRequestedScores;
    private final HashMap<String, ScanResult> mScanResultCache;
    Scanner mScanner;
    private WifiNetworkScoreCache mScoreCache;
    private boolean mStaleScanResults;
    private final WifiManager mWifiManager;
    Handler mWorkHandler;
    private HandlerThread mWorkThread;

    public interface WifiListener {
        void onAccessPointsChanged();

        void onConnectedChanged();

        void onWifiStateChanged(int i);
    }

    private static final boolean DBG() {
        return Log.isLoggable(TAG, 3);
    }

    private static boolean isVerboseLoggingEnabled() {
        return sVerboseLogging || Log.isLoggable(TAG, 2);
    }

    private static IntentFilter newIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.SCAN_RESULTS");
        intentFilter.addAction("android.net.wifi.NETWORK_IDS_CHANGED");
        intentFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        intentFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        return intentFilter;
    }

    @Deprecated
    public WifiTracker(Context context, WifiListener wifiListener, boolean z, boolean z2) {
        this(context, wifiListener, (WifiManager) context.getSystemService(WifiManager.class), (ConnectivityManager) context.getSystemService(ConnectivityManager.class), (NetworkScoreManager) context.getSystemService(NetworkScoreManager.class), newIntentFilter());
    }

    public WifiTracker(Context context, WifiListener wifiListener, Lifecycle lifecycle, boolean z, boolean z2) {
        this(context, wifiListener, (WifiManager) context.getSystemService(WifiManager.class), (ConnectivityManager) context.getSystemService(ConnectivityManager.class), (NetworkScoreManager) context.getSystemService(NetworkScoreManager.class), newIntentFilter());
        lifecycle.addObserver(this);
    }

    WifiTracker(Context context, WifiListener wifiListener, WifiManager wifiManager, ConnectivityManager connectivityManager, NetworkScoreManager networkScoreManager, IntentFilter intentFilter) {
        boolean z = false;
        this.mConnected = new AtomicBoolean(false);
        this.mLock = new Object();
        this.mInternalAccessPoints = new ArrayList();
        this.mRequestedScores = new ArraySet();
        this.mStaleScanResults = true;
        this.mScanResultCache = new HashMap<>();
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.settingslib.wifi.WifiTracker.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                    WifiTracker.this.updateWifiState(intent.getIntExtra("wifi_state", 4));
                    return;
                }
                if ("android.net.wifi.SCAN_RESULTS".equals(action)) {
                    WifiTracker.this.mStaleScanResults = false;
                    WifiTracker.this.fetchScansAndConfigsAndUpdateAccessPoints();
                    return;
                }
                if ("android.net.wifi.CONFIGURED_NETWORKS_CHANGE".equals(action) || "android.net.wifi.LINK_CONFIGURATION_CHANGED".equals(action)) {
                    WifiTracker.this.fetchScansAndConfigsAndUpdateAccessPoints();
                    return;
                }
                if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                    WifiTracker.this.updateNetworkInfo((NetworkInfo) intent.getParcelableExtra("networkInfo"));
                    WifiTracker.this.fetchScansAndConfigsAndUpdateAccessPoints();
                } else if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
                    WifiTracker.this.updateNetworkInfo(WifiTracker.this.mConnectivityManager.getNetworkInfo(WifiTracker.this.mWifiManager.getCurrentNetwork()));
                }
            }
        };
        this.mContext = context;
        this.mWifiManager = wifiManager;
        this.mListener = new WifiListenerExecutor(wifiListener);
        this.mConnectivityManager = connectivityManager;
        if (this.mWifiManager != null && this.mWifiManager.getVerboseLoggingLevel() > 0) {
            z = true;
        }
        sVerboseLogging = z;
        this.mFilter = intentFilter;
        this.mNetworkRequest = new NetworkRequest.Builder().clearCapabilities().addCapability(15).addTransportType(1).build();
        this.mNetworkScoreManager = networkScoreManager;
        HandlerThread handlerThread = new HandlerThread("WifiTracker{" + Integer.toHexString(System.identityHashCode(this)) + "}", 10);
        handlerThread.start();
        setWorkThread(handlerThread);
    }

    void setWorkThread(HandlerThread handlerThread) {
        this.mWorkThread = handlerThread;
        this.mWorkHandler = new Handler(handlerThread.getLooper());
        this.mScoreCache = new WifiNetworkScoreCache(this.mContext, new WifiNetworkScoreCache.CacheListener(this.mWorkHandler) { // from class: com.android.settingslib.wifi.WifiTracker.1
            public void networkCacheUpdated(List<ScoredNetwork> list) {
                if (WifiTracker.this.mRegistered) {
                    if (Log.isLoggable(WifiTracker.TAG, 2)) {
                        Log.v(WifiTracker.TAG, "Score cache was updated with networks: " + list);
                    }
                    WifiTracker.this.updateNetworkScores();
                }
            }
        });
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnDestroy
    public void onDestroy() {
        this.mWorkThread.quit();
    }

    public void forceScan() {
        if (this.mWifiManager != null && this.mWifiManager.isWifiEnabled() && this.mScanner != null) {
            this.mScanner.forceScan();
        }
    }

    private void pauseScanning() {
        if (this.mScanner != null) {
            this.mScanner.pause();
            this.mScanner = null;
        }
        this.mStaleScanResults = true;
    }

    public void resumeScanning() {
        if (this.mScanner == null) {
            this.mScanner = new Scanner();
        }
        if (isWifiEnabled()) {
            this.mScanner.resume();
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        forceUpdate();
        registerScoreCache();
        this.mNetworkScoringUiEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "network_scoring_ui_enabled", 0) == 1;
        this.mMaxSpeedLabelScoreCacheAge = Settings.Global.getLong(this.mContext.getContentResolver(), "speed_label_cache_eviction_age_millis", DEFAULT_MAX_CACHED_SCORE_AGE_MILLIS);
        resumeScanning();
        if (!this.mRegistered) {
            this.mContext.registerReceiver(this.mReceiver, this.mFilter, null, this.mWorkHandler);
            this.mNetworkCallback = new WifiTrackerNetworkCallback();
            this.mConnectivityManager.registerNetworkCallback(this.mNetworkRequest, this.mNetworkCallback, this.mWorkHandler);
            this.mRegistered = true;
        }
    }

    private void forceUpdate() {
        this.mLastInfo = this.mWifiManager.getConnectionInfo();
        this.mLastNetworkInfo = this.mConnectivityManager.getNetworkInfo(this.mWifiManager.getCurrentNetwork());
        fetchScansAndConfigsAndUpdateAccessPoints();
    }

    private void registerScoreCache() {
        this.mNetworkScoreManager.registerNetworkScoreCache(1, this.mScoreCache, 2);
    }

    private void requestScoresForNetworkKeys(Collection<NetworkKey> collection) {
        if (collection.isEmpty()) {
            return;
        }
        if (DBG()) {
            Log.d(TAG, "Requesting scores for Network Keys: " + collection);
        }
        this.mNetworkScoreManager.requestScores((NetworkKey[]) collection.toArray(new NetworkKey[collection.size()]));
        synchronized (this.mLock) {
            this.mRequestedScores.addAll(collection);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        if (this.mRegistered) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
            this.mRegistered = false;
        }
        unregisterScoreCache();
        pauseScanning();
        this.mWorkHandler.removeCallbacksAndMessages(null);
    }

    private void unregisterScoreCache() {
        this.mNetworkScoreManager.unregisterNetworkScoreCache(1, this.mScoreCache);
        synchronized (this.mLock) {
            this.mRequestedScores.clear();
        }
    }

    public List<AccessPoint> getAccessPoints() {
        ArrayList arrayList;
        synchronized (this.mLock) {
            arrayList = new ArrayList(this.mInternalAccessPoints);
        }
        return arrayList;
    }

    public WifiManager getManager() {
        return this.mWifiManager;
    }

    public boolean isWifiEnabled() {
        return this.mWifiManager != null && this.mWifiManager.isWifiEnabled();
    }

    public int getNumSavedNetworks() {
        return WifiSavedConfigUtils.getAllConfigs(this.mContext, this.mWifiManager).size();
    }

    public boolean isConnected() {
        return this.mConnected.get();
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("  - wifi tracker ------");
        Iterator<AccessPoint> it = getAccessPoints().iterator();
        while (it.hasNext()) {
            printWriter.println("  " + it.next());
        }
    }

    private ArrayMap<String, List<ScanResult>> updateScanResultCache(List<ScanResult> list) {
        List<ScanResult> list2;
        for (ScanResult scanResult : list) {
            if (scanResult.SSID != null && !scanResult.SSID.isEmpty()) {
                this.mScanResultCache.put(scanResult.BSSID, scanResult);
            }
        }
        if (!this.mStaleScanResults) {
            evictOldScans();
        }
        ArrayMap<String, List<ScanResult>> arrayMap = new ArrayMap<>();
        for (ScanResult scanResult2 : this.mScanResultCache.values()) {
            if (scanResult2.SSID != null && scanResult2.SSID.length() != 0 && !scanResult2.capabilities.contains("[IBSS]")) {
                String key = AccessPoint.getKey(scanResult2);
                if (arrayMap.containsKey(key)) {
                    list2 = arrayMap.get(key);
                } else {
                    ArrayList arrayList = new ArrayList();
                    arrayMap.put(key, arrayList);
                    list2 = arrayList;
                }
                list2.add(scanResult2);
            }
        }
        return arrayMap;
    }

    private void evictOldScans() {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        Iterator<ScanResult> it = this.mScanResultCache.values().iterator();
        while (it.hasNext()) {
            if (jElapsedRealtime - (it.next().timestamp / 1000) > MAX_SCAN_RESULT_AGE_MILLIS) {
                it.remove();
            }
        }
    }

    private WifiConfiguration getWifiConfigurationForNetworkId(int i, List<WifiConfiguration> list) {
        if (list != null) {
            for (WifiConfiguration wifiConfiguration : list) {
                if (this.mLastInfo != null && i == wifiConfiguration.networkId && (!wifiConfiguration.selfAdded || wifiConfiguration.numAssociation != 0)) {
                    return wifiConfiguration;
                }
            }
            return null;
        }
        return null;
    }

    private void fetchScansAndConfigsAndUpdateAccessPoints() {
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        if (isVerboseLoggingEnabled()) {
            Log.i(TAG, "Fetched scan results: " + scanResults);
        }
        updateAccessPoints(scanResults, this.mWifiManager.getConfiguredNetworks());
    }

    private void updateAccessPoints(List<ScanResult> list, List<WifiConfiguration> list2) {
        ArrayMap arrayMap = new ArrayMap(list2.size());
        if (list2 != null) {
            for (WifiConfiguration wifiConfiguration : list2) {
                arrayMap.put(AccessPoint.getKey(wifiConfiguration), wifiConfiguration);
            }
        }
        ArrayMap<String, List<ScanResult>> arrayMapUpdateScanResultCache = updateScanResultCache(list);
        WifiConfiguration wifiConfigurationForNetworkId = null;
        if (this.mLastInfo != null) {
            wifiConfigurationForNetworkId = getWifiConfigurationForNetworkId(this.mLastInfo.getNetworkId(), list2);
        }
        synchronized (this.mLock) {
            List<AccessPoint> arrayList = new ArrayList<>(this.mInternalAccessPoints);
            ArrayList arrayList2 = new ArrayList();
            ArrayList arrayList3 = new ArrayList();
            for (Map.Entry<String, List<ScanResult>> entry : arrayMapUpdateScanResultCache.entrySet()) {
                Iterator<ScanResult> it = entry.getValue().iterator();
                while (it.hasNext()) {
                    NetworkKey networkKeyCreateFromScanResult = NetworkKey.createFromScanResult(it.next());
                    if (networkKeyCreateFromScanResult != null && !this.mRequestedScores.contains(networkKeyCreateFromScanResult)) {
                        arrayList3.add(networkKeyCreateFromScanResult);
                    }
                }
                AccessPoint cachedOrCreate = getCachedOrCreate(entry.getValue(), arrayList);
                if (this.mLastInfo != null && this.mLastNetworkInfo != null) {
                    cachedOrCreate.update(wifiConfigurationForNetworkId, this.mLastInfo, this.mLastNetworkInfo);
                }
                cachedOrCreate.update((WifiConfiguration) arrayMap.get(entry.getKey()));
                arrayList2.add(cachedOrCreate);
            }
            if (arrayList2.isEmpty() && wifiConfigurationForNetworkId != null) {
                AccessPoint accessPoint = new AccessPoint(this.mContext, wifiConfigurationForNetworkId);
                accessPoint.update(wifiConfigurationForNetworkId, this.mLastInfo, this.mLastNetworkInfo);
                arrayList2.add(accessPoint);
                arrayList3.add(NetworkKey.createFromWifiInfo(this.mLastInfo));
            }
            requestScoresForNetworkKeys(arrayList3);
            Iterator it2 = arrayList2.iterator();
            while (it2.hasNext()) {
                ((AccessPoint) it2.next()).update(this.mScoreCache, this.mNetworkScoringUiEnabled, this.mMaxSpeedLabelScoreCacheAge);
            }
            Collections.sort(arrayList2);
            if (DBG()) {
                Log.d(TAG, "------ Dumping SSIDs that were not seen on this scan ------");
                for (AccessPoint accessPoint2 : this.mInternalAccessPoints) {
                    if (accessPoint2.getSsid() != null) {
                        String ssidStr = accessPoint2.getSsidStr();
                        boolean z = false;
                        Iterator it3 = arrayList2.iterator();
                        while (true) {
                            if (!it3.hasNext()) {
                                break;
                            }
                            AccessPoint accessPoint3 = (AccessPoint) it3.next();
                            if (accessPoint3.getSsidStr() != null && accessPoint3.getSsidStr().equals(ssidStr)) {
                                z = true;
                                break;
                            }
                        }
                        if (!z) {
                            Log.d(TAG, "Did not find " + ssidStr + " in this scan");
                        }
                    }
                }
                Log.d(TAG, "---- Done dumping SSIDs that were not seen on this scan ----");
            }
            this.mInternalAccessPoints.clear();
            this.mInternalAccessPoints.addAll(arrayList2);
        }
        conditionallyNotifyListeners();
    }

    AccessPoint getCachedOrCreate(List<ScanResult> list, List<AccessPoint> list2) {
        int size = list2.size();
        for (int i = 0; i < size; i++) {
            if (list2.get(i).getKey().equals(AccessPoint.getKey(list.get(0)))) {
                AccessPoint accessPointRemove = list2.remove(i);
                accessPointRemove.setScanResults(list);
                return accessPointRemove;
            }
        }
        return new AccessPoint(this.mContext, list);
    }

    private void updateNetworkInfo(NetworkInfo networkInfo) {
        if (!this.mWifiManager.isWifiEnabled()) {
            clearAccessPointsAndConditionallyUpdate();
            return;
        }
        if (networkInfo != null) {
            this.mLastNetworkInfo = networkInfo;
            if (DBG()) {
                Log.d(TAG, "mLastNetworkInfo set: " + this.mLastNetworkInfo);
            }
            if (networkInfo.isConnected() != this.mConnected.getAndSet(networkInfo.isConnected())) {
                this.mListener.onConnectedChanged();
            }
        }
        WifiConfiguration wifiConfigurationForNetworkId = null;
        this.mLastInfo = this.mWifiManager.getConnectionInfo();
        if (DBG()) {
            Log.d(TAG, "mLastInfo set as: " + this.mLastInfo);
        }
        if (this.mLastInfo != null) {
            wifiConfigurationForNetworkId = getWifiConfigurationForNetworkId(this.mLastInfo.getNetworkId(), this.mWifiManager.getConfiguredNetworks());
        }
        synchronized (this.mLock) {
            boolean z = false;
            boolean z2 = false;
            for (int size = this.mInternalAccessPoints.size() - 1; size >= 0; size--) {
                AccessPoint accessPoint = this.mInternalAccessPoints.get(size);
                boolean zIsActive = accessPoint.isActive();
                if (accessPoint.update(wifiConfigurationForNetworkId, this.mLastInfo, this.mLastNetworkInfo)) {
                    if (zIsActive != accessPoint.isActive()) {
                        z = true;
                        z2 = true;
                    } else {
                        z2 = true;
                    }
                }
                if (accessPoint.update(this.mScoreCache, this.mNetworkScoringUiEnabled, this.mMaxSpeedLabelScoreCacheAge)) {
                    z = true;
                    z2 = true;
                }
            }
            if (z) {
                Collections.sort(this.mInternalAccessPoints);
            }
            if (z2) {
                conditionallyNotifyListeners();
            }
        }
    }

    private void clearAccessPointsAndConditionallyUpdate() {
        synchronized (this.mLock) {
            if (!this.mInternalAccessPoints.isEmpty()) {
                this.mInternalAccessPoints.clear();
                conditionallyNotifyListeners();
            }
        }
    }

    private void updateNetworkScores() {
        synchronized (this.mLock) {
            boolean z = false;
            for (int i = 0; i < this.mInternalAccessPoints.size(); i++) {
                if (this.mInternalAccessPoints.get(i).update(this.mScoreCache, this.mNetworkScoringUiEnabled, this.mMaxSpeedLabelScoreCacheAge)) {
                    z = true;
                }
            }
            if (z) {
                Collections.sort(this.mInternalAccessPoints);
                conditionallyNotifyListeners();
            }
        }
    }

    private void updateWifiState(int i) {
        if (i == 3) {
            if (this.mScanner != null) {
                this.mScanner.resume();
            }
        } else {
            clearAccessPointsAndConditionallyUpdate();
            this.mLastInfo = null;
            this.mLastNetworkInfo = null;
            if (this.mScanner != null) {
                this.mScanner.pause();
            }
            this.mStaleScanResults = true;
        }
        this.mListener.onWifiStateChanged(i);
    }

    private final class WifiTrackerNetworkCallback extends ConnectivityManager.NetworkCallback {
        private WifiTrackerNetworkCallback() {
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            if (network.equals(WifiTracker.this.mWifiManager.getCurrentNetwork())) {
                WifiTracker.this.updateNetworkInfo(null);
            }
        }
    }

    class Scanner extends Handler {
        static final int MSG_SCAN = 0;
        private int mRetry = 0;

        Scanner() {
        }

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void pause() {
            this.mRetry = 0;
            removeMessages(0);
        }

        boolean isScanning() {
            return hasMessages(0);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (message.what != 0) {
                return;
            }
            if (WifiTracker.this.mWifiManager.startScan()) {
                this.mRetry = 0;
            } else {
                int i = this.mRetry + 1;
                this.mRetry = i;
                if (i >= 3) {
                    this.mRetry = 0;
                    if (WifiTracker.this.mContext != null) {
                        Toast.makeText(WifiTracker.this.mContext, R.string.wifi_fail_to_scan, 1).show();
                        return;
                    }
                    return;
                }
            }
            sendEmptyMessageDelayed(0, 10000L);
        }
    }

    private static class Multimap<K, V> {
        private final HashMap<K, List<V>> store = new HashMap<>();

        private Multimap() {
        }

        List<V> getAll(K k) {
            List<V> list = this.store.get(k);
            return list != null ? list : Collections.emptyList();
        }

        void put(K k, V v) {
            List<V> arrayList = this.store.get(k);
            if (arrayList == null) {
                arrayList = new ArrayList<>(3);
                this.store.put(k, arrayList);
            }
            arrayList.add(v);
        }
    }

    class WifiListenerExecutor implements WifiListener {
        private final WifiListener mDelegatee;

        public WifiListenerExecutor(WifiListener wifiListener) {
            this.mDelegatee = wifiListener;
        }

        @Override // com.android.settingslib.wifi.WifiTracker.WifiListener
        public void onWifiStateChanged(final int i) {
            runAndLog(new Runnable() { // from class: com.android.settingslib.wifi.-$$Lambda$WifiTracker$WifiListenerExecutor$PZBvWEzpVHhaI95PbZNbzEgAH1I
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.mDelegatee.onWifiStateChanged(i);
                }
            }, String.format("Invoking onWifiStateChanged callback with state %d", Integer.valueOf(i)));
        }

        @Override // com.android.settingslib.wifi.WifiTracker.WifiListener
        public void onConnectedChanged() {
            final WifiListener wifiListener = this.mDelegatee;
            Objects.requireNonNull(wifiListener);
            runAndLog(new Runnable() { // from class: com.android.settingslib.wifi.-$$Lambda$6PbPNXCvqbAnKbPWPJrs-dDWQEQ
                @Override // java.lang.Runnable
                public final void run() {
                    wifiListener.onConnectedChanged();
                }
            }, "Invoking onConnectedChanged callback");
        }

        @Override // com.android.settingslib.wifi.WifiTracker.WifiListener
        public void onAccessPointsChanged() {
            final WifiListener wifiListener = this.mDelegatee;
            Objects.requireNonNull(wifiListener);
            runAndLog(new Runnable() { // from class: com.android.settingslib.wifi.-$$Lambda$evcvquoPxZkPmBIit31UXvhXEJk
                @Override // java.lang.Runnable
                public final void run() {
                    wifiListener.onAccessPointsChanged();
                }
            }, "Invoking onAccessPointsChanged callback");
        }

        private void runAndLog(final Runnable runnable, final String str) {
            ThreadUtils.postOnMainThread(new Runnable() { // from class: com.android.settingslib.wifi.-$$Lambda$WifiTracker$WifiListenerExecutor$BMWc3s6WnR_Ijg_9a3gQADAjI3Y
                @Override // java.lang.Runnable
                public final void run() {
                    WifiTracker.WifiListenerExecutor.lambda$runAndLog$1(this.f$0, str, runnable);
                }
            });
        }

        public static /* synthetic */ void lambda$runAndLog$1(WifiListenerExecutor wifiListenerExecutor, String str, Runnable runnable) {
            if (WifiTracker.this.mRegistered) {
                if (WifiTracker.isVerboseLoggingEnabled()) {
                    Log.i(WifiTracker.TAG, str);
                }
                runnable.run();
            }
        }
    }

    private void conditionallyNotifyListeners() {
        if (this.mStaleScanResults) {
            return;
        }
        this.mListener.onAccessPointsChanged();
    }
}
