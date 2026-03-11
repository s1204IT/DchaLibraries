package jp.co.benesse.dcha.systemsettings;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import jp.co.benesse.dcha.util.Logger;

public class WifiTracker {
    public static int sVerboseLogging = 0;
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
    private Scanner mScanner;
    private HashMap<String, Integer> mSeenBssids;
    private final WifiManager mWifiManager;
    private final WorkHandler mWorkHandler;

    public interface WifiListener {
        void onAccessPointsChanged();

        void onConnectedChanged();

        void onWifiStateChanged(int i);
    }

    public WifiTracker(Context context, WifiListener wifiListener, Looper workerLooper, boolean includeSaved, boolean includeScans, boolean includePasspoints) {
        this(context, wifiListener, workerLooper, includeSaved, includeScans, includePasspoints, (WifiManager) context.getSystemService(WifiManager.class), (ConnectivityManager) context.getSystemService(ConnectivityManager.class), Looper.myLooper());
    }

    private WifiTracker(Context context, WifiListener wifiListener, Looper workerLooper, boolean includeSaved, boolean includeScans, boolean includePasspoints, WifiManager wifiManager, ConnectivityManager connectivityManager, Looper currentLooper) {
        this.mConnected = new AtomicBoolean(false);
        this.mAccessPoints = new ArrayList<>();
        this.mSeenBssids = new HashMap<>();
        this.mScanResultCache = new HashMap<>();
        this.mScanId = 0;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                Logger.d("WifiTracker", "onReceive 0001");
                if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                    Logger.d("WifiTracker", "onReceive 0002");
                    WifiTracker.this.updateWifiState(intent.getIntExtra("wifi_state", 4));
                    return;
                }
                if ("android.net.wifi.SCAN_RESULTS".equals(action) || "android.net.wifi.CONFIGURED_NETWORKS_CHANGE".equals(action) || "android.net.wifi.LINK_CONFIGURATION_CHANGED".equals(action)) {
                    Logger.d("WifiTracker", "onReceive 0003");
                    WifiTracker.this.mWorkHandler.sendEmptyMessage(0);
                } else {
                    if (!"android.net.wifi.STATE_CHANGE".equals(action)) {
                        return;
                    }
                    Logger.d("WifiTracker", "onReceive 0004");
                    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    WifiTracker.this.mConnected.set(info.isConnected());
                    WifiTracker.this.mMainHandler.sendEmptyMessage(0);
                    WifiTracker.this.mWorkHandler.sendEmptyMessage(0);
                    WifiTracker.this.mWorkHandler.obtainMessage(1, info).sendToTarget();
                }
            }
        };
        Logger.d("WifiTracker", "WifiTracker 0001");
        if (!includeSaved && !includeScans) {
            Logger.d("WifiTracker", "WifiTracker 0002");
            throw new IllegalArgumentException("Must include either saved or scans");
        }
        this.mContext = context;
        if (currentLooper == null) {
            Logger.d("WifiTracker", "WifiTracker 0003");
            currentLooper = Looper.getMainLooper();
        }
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
        Logger.d("WifiTracker", "WifiTracker 0004");
    }

    public void pauseScanning() {
        Logger.d("WifiTracker", "pauseScanning 0001");
        if (this.mScanner != null) {
            Logger.d("WifiTracker", "pauseScanning 0002");
            this.mScanner.pause();
            this.mScanner = null;
        }
        Logger.d("WifiTracker", "pauseScanning 0003");
    }

    public void resumeScanning() {
        Scanner scanner = null;
        Logger.d("WifiTracker", "resumeScanning 0001");
        if (this.mScanner == null) {
            Logger.d("WifiTracker", "resumeScanning 0002");
            this.mScanner = new Scanner(this, scanner);
        }
        this.mWorkHandler.sendEmptyMessage(2);
        if (this.mWifiManager.isWifiEnabled()) {
            Logger.d("WifiTracker", "resumeScanning 0003");
            this.mScanner.resume();
        }
        this.mWorkHandler.sendEmptyMessage(0);
        Logger.d("WifiTracker", "resumeScanning 0004");
    }

    public void startTracking() {
        Logger.d("WifiTracker", "startTracking 0001");
        resumeScanning();
        if (!this.mRegistered) {
            Logger.d("WifiTracker", "startTracking 0002");
            this.mContext.registerReceiver(this.mReceiver, this.mFilter);
            this.mNetworkCallback = new WifiTrackerNetworkCallback(this, null);
            this.mConnectivityManager.registerNetworkCallback(this.mNetworkRequest, this.mNetworkCallback);
            this.mRegistered = true;
        }
        Logger.d("WifiTracker", "startTracking 0003");
    }

    public void stopTracking() {
        Logger.d("WifiTracker", "stopTracking 0001");
        if (this.mRegistered) {
            Logger.d("WifiTracker", "stopTracking 0002");
            this.mWorkHandler.removeMessages(0);
            this.mWorkHandler.removeMessages(1);
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
            this.mRegistered = false;
        }
        pauseScanning();
        Logger.d("WifiTracker", "stopTracking 0003");
    }

    public List<AccessPoint> getAccessPoints() {
        ArrayList<AccessPoint> cachedaccessPoints;
        synchronized (this.mAccessPoints) {
            Logger.d("WifiTracker", "getAccessPoints 0001");
            cachedaccessPoints = new ArrayList<>();
            for (AccessPoint accessPoint : this.mAccessPoints) {
                cachedaccessPoints.add((AccessPoint) accessPoint.clone());
            }
            Logger.d("WifiTracker", "getAccessPoints 0002");
        }
        return cachedaccessPoints;
    }

    public WifiManager getManager() {
        Logger.d("WifiTracker", "getManager 0001");
        return this.mWifiManager;
    }

    public void handleResume() {
        Logger.d("WifiTracker", "handleResume 0001");
        this.mScanResultCache.clear();
        this.mSeenBssids.clear();
        this.mScanId = 0;
        Logger.d("WifiTracker", "handleResume 0002");
    }

    private Collection<ScanResult> fetchScanResults() {
        Logger.d("WifiTracker", "fetchScanResults 0001");
        this.mScanId = Integer.valueOf(this.mScanId.intValue() + 1);
        List<ScanResult> newResults = this.mWifiManager.getScanResults();
        for (ScanResult newResult : newResults) {
            if (newResult.SSID != null && !newResult.SSID.isEmpty()) {
                this.mScanResultCache.put(newResult.BSSID, newResult);
                this.mSeenBssids.put(newResult.BSSID, this.mScanId);
            }
        }
        if (this.mScanId.intValue() > 3) {
            Logger.d("WifiTracker", "fetchScanResults 0002");
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
        Logger.d("WifiTracker", "fetchScanResults 0003");
        return this.mScanResultCache.values();
    }

    private WifiConfiguration getWifiConfigurationForNetworkId(int networkId) {
        Logger.d("WifiTracker", "getWifiConfigurationForNetworkId 0001");
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            Logger.d("WifiTracker", "getWifiConfigurationForNetworkId 0002");
            for (WifiConfiguration config : configs) {
                if (this.mLastInfo != null && networkId == config.networkId && (!config.selfAdded || config.numAssociation != 0)) {
                    return config;
                }
            }
        }
        Logger.d("WifiTracker", "getWifiConfigurationForNetworkId 0003");
        return null;
    }

    public void updateAccessPoints() {
        WifiConfiguration config;
        Logger.d("WifiTracker", "updateAccessPoints 0001");
        List<AccessPoint> cachedAccessPoints = getAccessPoints();
        ArrayList<AccessPoint> accessPoints = new ArrayList<>();
        Iterator accessPoint$iterator = cachedAccessPoints.iterator();
        while (accessPoint$iterator.hasNext()) {
            ((AccessPoint) accessPoint$iterator.next()).clearConfig();
        }
        Multimap<String, AccessPoint> apMap = new Multimap<>(null);
        WifiConfiguration connectionConfig = null;
        if (this.mLastInfo != null) {
            Logger.d("WifiTracker", "updateAccessPoints 0002");
            connectionConfig = getWifiConfigurationForNetworkId(this.mLastInfo.getNetworkId());
        }
        Collection<ScanResult> results = fetchScanResults();
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            Logger.d("WifiTracker", "updateAccessPoints 0003");
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
            Logger.d("WifiTracker", "updateAccessPoints 0004");
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
        Logger.d("WifiTracker", "updateAccessPoints 0005");
        this.mAccessPoints = accessPoints;
        this.mMainHandler.sendEmptyMessage(2);
    }

    private AccessPoint getCachedOrCreate(ScanResult result, List<AccessPoint> cache) {
        Logger.d("WifiTracker", "getCachedOrCreate 0001");
        int N = cache.size();
        for (int i = 0; i < N; i++) {
            if (cache.get(i).matches(result)) {
                AccessPoint ret = cache.remove(i);
                ret.update(result);
                return ret;
            }
        }
        Logger.d("WifiTracker", "getCachedOrCreate 0002");
        return new AccessPoint(this.mContext, result);
    }

    private AccessPoint getCachedOrCreate(WifiConfiguration config, List<AccessPoint> cache) {
        Logger.d("WifiTracker", "getCachedOrCreate 0003");
        int N = cache.size();
        for (int i = 0; i < N; i++) {
            if (cache.get(i).matches(config)) {
                AccessPoint ret = cache.remove(i);
                ret.loadConfig(config);
                return ret;
            }
        }
        Logger.d("WifiTracker", "getCachedOrCreate 0004");
        return new AccessPoint(this.mContext, config);
    }

    public void updateNetworkInfo(NetworkInfo networkInfo) {
        Logger.d("WifiTracker", "updateNetworkInfo 0001");
        if (!this.mWifiManager.isWifiEnabled()) {
            Logger.d("WifiTracker", "updateNetworkInfo 0002");
            this.mMainHandler.sendEmptyMessage(4);
            return;
        }
        if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
            Logger.d("WifiTracker", "updateNetworkInfo 0003");
            this.mMainHandler.sendEmptyMessage(4);
        } else {
            Logger.d("WifiTracker", "updateNetworkInfo 0004");
            this.mMainHandler.sendEmptyMessage(3);
        }
        if (networkInfo != null) {
            Logger.d("WifiTracker", "updateNetworkInfo 0005");
            this.mLastNetworkInfo = networkInfo;
        }
        WifiConfiguration connectionConfig = null;
        this.mLastInfo = this.mWifiManager.getConnectionInfo();
        if (this.mLastInfo != null) {
            Logger.d("WifiTracker", "updateNetworkInfo 0006");
            connectionConfig = getWifiConfigurationForNetworkId(this.mLastInfo.getNetworkId());
        }
        boolean reorder = false;
        for (int i = this.mAccessPoints.size() - 1; i >= 0; i--) {
            if (this.mAccessPoints.get(i).update(connectionConfig, this.mLastInfo, this.mLastNetworkInfo)) {
                reorder = true;
            }
        }
        if (reorder) {
            synchronized (this.mAccessPoints) {
                Logger.d("WifiTracker", "updateNetworkInfo 0007");
                Collections.sort(this.mAccessPoints);
            }
            this.mMainHandler.sendEmptyMessage(2);
        }
        Logger.d("WifiTracker", "updateNetworkInfo 0008");
    }

    public void updateWifiState(int state) {
        Logger.d("WifiTracker", "updateWifiState 0001");
        this.mWorkHandler.obtainMessage(3, state, 0).sendToTarget();
        Logger.d("WifiTracker", "updateWifiState 0002");
    }

    private final class WifiTrackerNetworkCallback extends ConnectivityManager.NetworkCallback {
        WifiTrackerNetworkCallback(WifiTracker this$0, WifiTrackerNetworkCallback wifiTrackerNetworkCallback) {
            this();
        }

        private WifiTrackerNetworkCallback() {
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
            Logger.d("WifiTracker", "onCapabilitiesChanged 0001");
            if (!network.equals(WifiTracker.this.mWifiManager.getCurrentNetwork())) {
                return;
            }
            Logger.d("WifiTracker", "onCapabilitiesChanged 0002");
            WifiTracker.this.mWorkHandler.sendEmptyMessage(1);
        }
    }

    private final class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.d("WifiTracker", "MainHandler handleMessage 0001");
            if (WifiTracker.this.mListener == null) {
                Logger.d("WifiTracker", "MainHandler handleMessage 0002");
            }
            switch (msg.what) {
                case 0:
                    Logger.d("WifiTracker", "MainHandler handleMessage 0003");
                    WifiTracker.this.mListener.onConnectedChanged();
                    break;
                case 1:
                    Logger.d("WifiTracker", "MainHandler handleMessage 0004");
                    WifiTracker.this.mListener.onWifiStateChanged(msg.arg1);
                    break;
                case 2:
                    Logger.d("WifiTracker", "MainHandler handleMessage 0005");
                    WifiTracker.this.mListener.onAccessPointsChanged();
                    break;
                case 3:
                    Logger.d("WifiTracker", "MainHandler handleMessage 0006");
                    if (WifiTracker.this.mScanner != null) {
                        Logger.d("WifiTracker", "MainHandler handleMessage 0007");
                        WifiTracker.this.mScanner.resume();
                    }
                    break;
                case 4:
                    Logger.d("WifiTracker", "MainHandler handleMessage 0008");
                    if (WifiTracker.this.mScanner != null) {
                        Logger.d("WifiTracker", "MainHandler handleMessage 0009");
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
            Logger.d("WifiTracker", "WorkHandler handleMessage 0001");
            switch (msg.what) {
                case 0:
                    Logger.d("WifiTracker", "WorkHandler handleMessage 0002");
                    WifiTracker.this.updateAccessPoints();
                    break;
                case 1:
                    Logger.d("WifiTracker", "WorkHandler handleMessage 0003");
                    WifiTracker.this.updateNetworkInfo((NetworkInfo) msg.obj);
                    break;
                case 2:
                    Logger.d("WifiTracker", "WorkHandler handleMessage 0004");
                    WifiTracker.this.handleResume();
                    break;
                case 3:
                    Logger.d("WifiTracker", "WorkHandler handleMessage 0005");
                    if (msg.arg1 == 3) {
                        Logger.d("WifiTracker", "WorkHandler handleMessage 0006");
                        if (WifiTracker.this.mScanner != null) {
                            Logger.d("WifiTracker", "WorkHandler handleMessage 0007");
                            WifiTracker.this.mScanner.resume();
                        }
                    } else {
                        Logger.d("WifiTracker", "WorkHandler handleMessage 0008");
                        WifiTracker.this.mLastInfo = null;
                        WifiTracker.this.mLastNetworkInfo = null;
                        if (WifiTracker.this.mScanner != null) {
                            Logger.d("WifiTracker", "WorkHandler handleMessage 0009");
                            WifiTracker.this.mScanner.pause();
                        }
                    }
                    WifiTracker.this.mMainHandler.obtainMessage(1, msg.arg1, 0).sendToTarget();
                    break;
            }
        }
    }

    private class Scanner extends Handler {
        private int mRetry;

        Scanner(WifiTracker this$0, Scanner scanner) {
            this();
        }

        private Scanner() {
            this.mRetry = 0;
        }

        void resume() {
            if (hasMessages(0)) {
                return;
            }
            sendEmptyMessage(0);
        }

        void pause() {
            this.mRetry = 0;
            removeMessages(0);
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
