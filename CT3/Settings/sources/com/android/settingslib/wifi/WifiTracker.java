package com.android.settingslib.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import com.android.settingslib.R$string;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WifiTracker {
    private ArrayList<AccessPoint> mAccessPoints;
    private final AtomicBoolean mConnected;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final IntentFilter mFilter;
    private final boolean mIncludePasspoints;
    private final boolean mIncludeSaved;
    private final boolean mIncludeScans;
    private WifiInfo mLastInfo;
    private NetworkInfo mLastNetworkInfo;
    private final WifiListener mListener;
    private final MainHandler mMainHandler;
    private WifiTrackerNetworkCallback mNetworkCallback;
    private final NetworkRequest mNetworkRequest;
    final BroadcastReceiver mReceiver;
    private boolean mRegistered;
    private boolean mSavedNetworksExist;
    private Integer mScanId;
    private HashMap<String, ScanResult> mScanResultCache;
    Scanner mScanner;
    private HashMap<String, Integer> mSeenBssids;
    private final WifiManager mWifiManager;
    private final WorkHandler mWorkHandler;
    public static int sVerboseLogging = 0;
    private static final ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

    public interface WifiListener {
        void onAccessPointsChanged();

        void onConnectedChanged();

        void onWifiStateChanged(int i);
    }

    public WifiTracker(Context context, WifiListener wifiListener, Looper workerLooper, boolean includeSaved, boolean includeScans, boolean includePasspoints) {
        this(context, wifiListener, workerLooper, includeSaved, includeScans, includePasspoints, (WifiManager) context.getSystemService(WifiManager.class), (ConnectivityManager) context.getSystemService(ConnectivityManager.class), Looper.myLooper());
    }

    WifiTracker(Context context, WifiListener wifiListener, Looper workerLooper, boolean includeSaved, boolean includeScans, boolean includePasspoints, WifiManager wifiManager, ConnectivityManager connectivityManager, Looper currentLooper) {
        this.mConnected = new AtomicBoolean(false);
        this.mAccessPoints = new ArrayList<>();
        this.mSeenBssids = new HashMap<>();
        this.mScanResultCache = new HashMap<>();
        this.mScanId = 0;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                    WifiTracker.this.updateWifiState(intent.getIntExtra("wifi_state", 4));
                    return;
                }
                if ("android.net.wifi.SCAN_RESULTS".equals(action) || "android.net.wifi.CONFIGURED_NETWORKS_CHANGE".equals(action) || "android.net.wifi.LINK_CONFIGURATION_CHANGED".equals(action)) {
                    WifiTracker.this.mWorkHandler.sendEmptyMessage(0);
                    return;
                }
                if (!"android.net.wifi.STATE_CHANGE".equals(action)) {
                    return;
                }
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                WifiTracker.this.mConnected.set(info.isConnected());
                WifiTracker.this.mMainHandler.sendEmptyMessage(0);
                WifiTracker.this.mWorkHandler.sendEmptyMessage(0);
                WifiTracker.this.mWorkHandler.obtainMessage(1, info).sendToTarget();
            }
        };
        if (!includeSaved && !includeScans) {
            throw new IllegalArgumentException("Must include either saved or scans");
        }
        this.mContext = context;
        currentLooper = currentLooper == null ? Looper.getMainLooper() : currentLooper;
        this.mMainHandler = new MainHandler(currentLooper);
        this.mWorkHandler = new WorkHandler(workerLooper == null ? currentLooper : workerLooper);
        this.mWifiManager = wifiManager;
        this.mIncludeSaved = includeSaved;
        this.mIncludeScans = includeScans;
        this.mIncludePasspoints = includePasspoints;
        this.mListener = wifiListener;
        this.mConnectivityManager = connectivityManager;
        sVerboseLogging = this.mWifiManager.getVerboseLoggingLevel();
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mFilter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mFilter.addAction("android.net.wifi.NETWORK_IDS_CHANGED");
        this.mFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        this.mFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        this.mFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mNetworkRequest = new NetworkRequest.Builder().clearCapabilities().addTransportType(1).build();
    }

    public void forceUpdate() {
        updateAccessPoints();
    }

    public void forceScan() {
        if (!this.mWifiManager.isWifiEnabled() || this.mScanner == null) {
            return;
        }
        this.mScanner.forceScan();
    }

    public void pauseScanning() {
        if (this.mScanner == null) {
            return;
        }
        this.mScanner.pause();
        this.mScanner = null;
    }

    public void resumeScanning() {
        if (this.mScanner == null) {
            this.mScanner = new Scanner();
        }
        this.mWorkHandler.sendEmptyMessage(2);
        if (this.mWifiManager.isWifiEnabled()) {
            this.mScanner.resume();
        }
        this.mWorkHandler.sendEmptyMessage(0);
    }

    public void startTracking() {
        resumeScanning();
        if (this.mRegistered) {
            return;
        }
        this.mContext.registerReceiver(this.mReceiver, this.mFilter);
        this.mNetworkCallback = new WifiTrackerNetworkCallback(this, null);
        this.mConnectivityManager.registerNetworkCallback(this.mNetworkRequest, this.mNetworkCallback);
        this.mRegistered = true;
    }

    public void stopTracking() {
        if (this.mRegistered) {
            this.mWorkHandler.removeMessages(0);
            this.mWorkHandler.removeMessages(1);
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
            this.mRegistered = false;
        }
        pauseScanning();
    }

    public List<AccessPoint> getAccessPoints() {
        ArrayList<AccessPoint> cachedaccessPoints;
        synchronized (this.mAccessPoints) {
            cachedaccessPoints = new ArrayList<>();
            for (AccessPoint accessPoint : this.mAccessPoints) {
                cachedaccessPoints.add((AccessPoint) accessPoint.clone());
            }
        }
        return cachedaccessPoints;
    }

    public WifiManager getManager() {
        return this.mWifiManager;
    }

    public boolean isWifiEnabled() {
        return this.mWifiManager.isWifiEnabled();
    }

    public boolean isConnected() {
        return this.mConnected.get();
    }

    public void handleResume() {
        this.mScanResultCache.clear();
        this.mSeenBssids.clear();
        this.mScanId = 0;
    }

    private Collection<ScanResult> fetchScanResults() {
        this.mScanId = Integer.valueOf(this.mScanId.intValue() + 1);
        List<ScanResult> newResults = this.mWifiManager.getScanResults();
        for (ScanResult newResult : newResults) {
            if (newResult.SSID != null && !newResult.SSID.isEmpty()) {
                this.mScanResultCache.put(newResult.BSSID, newResult);
                this.mSeenBssids.put(newResult.BSSID, this.mScanId);
            }
        }
        if (this.mScanId.intValue() > 3) {
            Integer threshold = Integer.valueOf(this.mScanId.intValue() - 3);
            Iterator<Map.Entry<String, Integer>> it = this.mSeenBssids.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> e = it.next();
                if (e.getValue().intValue() < threshold.intValue()) {
                    this.mScanResultCache.get(e.getKey());
                    this.mScanResultCache.remove(e.getKey());
                    it.remove();
                }
            }
        }
        return this.mScanResultCache.values();
    }

    private WifiConfiguration getWifiConfigurationForNetworkId(int networkId) {
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if (this.mLastInfo != null && networkId == config.networkId && (!config.selfAdded || config.numAssociation != 0)) {
                    return config;
                }
            }
        }
        return null;
    }

    public void updateAccessPoints() {
        WifiConfiguration config;
        List<AccessPoint> cachedAccessPoints = getAccessPoints();
        ArrayList<AccessPoint> accessPoints = new ArrayList<>();
        Iterator accessPoint$iterator = cachedAccessPoints.iterator();
        while (accessPoint$iterator.hasNext()) {
            ((AccessPoint) accessPoint$iterator.next()).clearConfig();
        }
        mReadWriteLock.readLock().lock();
        try {
            Multimap<String, AccessPoint> apMap = new Multimap<>(null);
            WifiConfiguration connectionConfig = null;
            if (this.mLastInfo != null) {
                connectionConfig = getWifiConfigurationForNetworkId(this.mLastInfo.getNetworkId());
            }
            Collection<ScanResult> results = fetchScanResults();
            List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
            if (configs != null) {
                this.mSavedNetworksExist = configs.size() != 0;
                for (WifiConfiguration config2 : configs) {
                    if (!config2.selfAdded || config2.numAssociation != 0) {
                        AccessPoint accessPoint = getCachedOrCreate(config2, cachedAccessPoints);
                        if (this.mLastInfo != null && this.mLastNetworkInfo != null && !config2.isPasspoint()) {
                            accessPoint.update(connectionConfig, this.mLastInfo, this.mLastNetworkInfo);
                        }
                        if (this.mIncludeSaved) {
                            if (!config2.isPasspoint() || this.mIncludePasspoints) {
                                boolean apFound = false;
                                Iterator result$iterator = results.iterator();
                                while (true) {
                                    if (result$iterator.hasNext()) {
                                        if (((ScanResult) result$iterator.next()).SSID.equals(accessPoint.getSsidStr())) {
                                            apFound = true;
                                            break;
                                        }
                                    } else {
                                        break;
                                    }
                                }
                                if (!apFound) {
                                    accessPoint.setRssi(Integer.MAX_VALUE);
                                }
                                accessPoints.add(accessPoint);
                            }
                            if (!config2.isPasspoint()) {
                                apMap.put(accessPoint.getSsidStr(), accessPoint);
                            }
                        } else {
                            cachedAccessPoints.add(accessPoint);
                        }
                    }
                }
            }
            if (results != null) {
                for (ScanResult result : results) {
                    if (result.SSID != null && result.SSID.length() != 0 && !result.capabilities.contains("[IBSS]")) {
                        boolean found = false;
                        Iterator accessPoint$iterator2 = apMap.getAll(result.SSID).iterator();
                        while (true) {
                            if (accessPoint$iterator2.hasNext()) {
                                if (((AccessPoint) accessPoint$iterator2.next()).update(result)) {
                                    found = true;
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                        if (!found && this.mIncludeScans) {
                            AccessPoint accessPoint2 = getCachedOrCreate(result, cachedAccessPoints);
                            if (this.mLastInfo != null && this.mLastNetworkInfo != null) {
                                accessPoint2.update(connectionConfig, this.mLastInfo, this.mLastNetworkInfo);
                            }
                            if (result.isPasspointNetwork() && (config = this.mWifiManager.getMatchingWifiConfig(result)) != null) {
                                accessPoint2.update(config);
                            }
                            if (this.mLastInfo != null && this.mLastInfo.getBSSID() != null && this.mLastInfo.getBSSID().equals(result.BSSID) && connectionConfig != null && connectionConfig.isPasspoint()) {
                                accessPoint2.update(connectionConfig);
                            }
                            accessPoints.add(accessPoint2);
                            apMap.put(accessPoint2.getSsidStr(), accessPoint2);
                        }
                    }
                }
            }
            mReadWriteLock.readLock().unlock();
            Collections.sort(accessPoints);
            for (AccessPoint prevAccessPoint : this.mAccessPoints) {
                if (prevAccessPoint.getSsid() != null) {
                    String prevSsid = prevAccessPoint.getSsidStr();
                    boolean found2 = false;
                    Iterator newAccessPoint$iterator = accessPoints.iterator();
                    while (true) {
                        if (!newAccessPoint$iterator.hasNext()) {
                            break;
                        }
                        AccessPoint newAccessPoint = (AccessPoint) newAccessPoint$iterator.next();
                        if (newAccessPoint.getSsid() != null && newAccessPoint.getSsid().equals(prevSsid)) {
                            found2 = true;
                            break;
                        }
                    }
                    if (found2) {
                    }
                }
            }
            this.mAccessPoints = accessPoints;
            this.mMainHandler.sendEmptyMessage(2);
        } catch (Throwable th) {
            mReadWriteLock.readLock().unlock();
            throw th;
        }
    }

    private AccessPoint getCachedOrCreate(ScanResult result, List<AccessPoint> cache) {
        int N = cache.size();
        for (int i = 0; i < N; i++) {
            if (cache.get(i).matches(result)) {
                AccessPoint ret = cache.remove(i);
                ret.update(result);
                return ret;
            }
        }
        return new AccessPoint(this.mContext, result);
    }

    private AccessPoint getCachedOrCreate(WifiConfiguration config, List<AccessPoint> cache) {
        int N = cache.size();
        for (int i = 0; i < N; i++) {
            if (cache.get(i).matches(config)) {
                AccessPoint ret = cache.remove(i);
                ret.loadConfig(config);
                return ret;
            }
        }
        return new AccessPoint(this.mContext, config);
    }

    public void updateNetworkInfo(NetworkInfo networkInfo) {
        if (!this.mWifiManager.isWifiEnabled()) {
            this.mMainHandler.sendEmptyMessage(4);
            return;
        }
        if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
            this.mMainHandler.sendEmptyMessage(4);
        } else {
            this.mMainHandler.sendEmptyMessage(3);
        }
        mReadWriteLock.writeLock().lock();
        if (networkInfo != null) {
            try {
                this.mLastNetworkInfo = networkInfo;
            } catch (Throwable th) {
                mReadWriteLock.writeLock().unlock();
                throw th;
            }
        }
        mReadWriteLock.writeLock().unlock();
        boolean reorder = false;
        mReadWriteLock.readLock().lock();
        WifiConfiguration connectionConfig = null;
        try {
            this.mLastInfo = this.mWifiManager.getConnectionInfo();
            if (this.mLastInfo != null) {
                connectionConfig = getWifiConfigurationForNetworkId(this.mLastInfo.getNetworkId());
            }
            for (int i = this.mAccessPoints.size() - 1; i >= 0; i--) {
                if (this.mAccessPoints.get(i).update(connectionConfig, this.mLastInfo, this.mLastNetworkInfo)) {
                    reorder = true;
                }
            }
            if (!reorder) {
                return;
            }
            synchronized (this.mAccessPoints) {
                Collections.sort(this.mAccessPoints);
            }
            this.mMainHandler.sendEmptyMessage(2);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    public void updateWifiState(int state) {
        this.mWorkHandler.obtainMessage(3, state, 0).sendToTarget();
    }

    public static List<AccessPoint> getCurrentAccessPoints(Context context, boolean includeSaved, boolean includeScans, boolean includePasspoints) {
        WifiTracker tracker = new WifiTracker(context, null, null, includeSaved, includeScans, includePasspoints);
        tracker.forceUpdate();
        return tracker.getAccessPoints();
    }

    private final class WifiTrackerNetworkCallback extends ConnectivityManager.NetworkCallback {
        WifiTrackerNetworkCallback(WifiTracker this$0, WifiTrackerNetworkCallback wifiTrackerNetworkCallback) {
            this();
        }

        private WifiTrackerNetworkCallback() {
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
            if (!network.equals(WifiTracker.this.mWifiManager.getCurrentNetwork())) {
                return;
            }
            WifiTracker.this.mWorkHandler.sendEmptyMessage(1);
        }
    }

    private final class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (WifiTracker.this.mListener == null) {
            }
            switch (msg.what) {
                case DefaultWfcSettingsExt.RESUME:
                    WifiTracker.this.mListener.onConnectedChanged();
                    break;
                case DefaultWfcSettingsExt.PAUSE:
                    WifiTracker.this.mListener.onWifiStateChanged(msg.arg1);
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    WifiTracker.this.mListener.onAccessPointsChanged();
                    break;
                case DefaultWfcSettingsExt.DESTROY:
                    if (WifiTracker.this.mScanner != null) {
                        WifiTracker.this.mScanner.resume();
                    }
                    break;
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    if (WifiTracker.this.mScanner != null) {
                        WifiTracker.this.mScanner.pause();
                    }
                    break;
            }
        }
    }

    private final class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.RESUME:
                    WifiTracker.this.updateAccessPoints();
                    return;
                case DefaultWfcSettingsExt.PAUSE:
                    WifiTracker.this.updateNetworkInfo((NetworkInfo) msg.obj);
                    return;
                case DefaultWfcSettingsExt.CREATE:
                    WifiTracker.this.handleResume();
                    return;
                case DefaultWfcSettingsExt.DESTROY:
                    if (msg.arg1 == 3) {
                        if (WifiTracker.this.mScanner != null) {
                            WifiTracker.this.mScanner.resume();
                        }
                    } else {
                        WifiTracker.mReadWriteLock.writeLock().lock();
                        try {
                            WifiTracker.this.mLastInfo = null;
                            WifiTracker.this.mLastNetworkInfo = null;
                            WifiTracker.mReadWriteLock.writeLock().unlock();
                            if (WifiTracker.this.mScanner != null) {
                                WifiTracker.this.mScanner.pause();
                            }
                        } catch (Throwable th) {
                            WifiTracker.mReadWriteLock.writeLock().unlock();
                            throw th;
                        }
                    }
                    WifiTracker.this.mMainHandler.obtainMessage(1, msg.arg1, 0).sendToTarget();
                    return;
                default:
                    return;
            }
        }
    }

    class Scanner extends Handler {
        private int mRetry = 0;

        Scanner() {
        }

        void resume() {
            if (hasMessages(0)) {
                return;
            }
            sendEmptyMessage(0);
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

        @Override
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
                        Toast.makeText(WifiTracker.this.mContext, R$string.wifi_fail_to_scan, 1).show();
                        return;
                    }
                    return;
                }
            }
            sendEmptyMessageDelayed(0, 10000L);
        }
    }

    private static class Multimap<K, V> {
        private final HashMap<K, List<V>> store;

        Multimap(Multimap multimap) {
            this();
        }

        private Multimap() {
            this.store = new HashMap<>();
        }

        List<V> getAll(K key) {
            List<V> values = this.store.get(key);
            return values != null ? values : Collections.emptyList();
        }

        void put(K key, V val) {
            List<V> curVals = this.store.get(key);
            if (curVals == null) {
                curVals = new ArrayList<>(3);
                this.store.put(key, curVals);
            }
            curVals.add(val);
        }
    }
}
