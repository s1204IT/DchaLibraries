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

/* loaded from: classes.dex */
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
    private Scanner mScanner;
    private HashMap<String, Integer> mSeenBssids;
    private final WifiManager mWifiManager;
    private final WorkHandler mWorkHandler;

    public interface WifiListener {
        void onAccessPointsChanged();

        void onConnectedChanged();

        void onWifiStateChanged(int i);
    }

    public WifiTracker(Context context, WifiListener wifiListener, Looper looper, boolean z, boolean z2, boolean z3) {
        this(context, wifiListener, looper, z, z2, z3, (WifiManager) context.getSystemService(WifiManager.class), (ConnectivityManager) context.getSystemService(ConnectivityManager.class), Looper.myLooper());
    }

    private WifiTracker(Context context, WifiListener wifiListener, Looper looper, boolean z, boolean z2, boolean z3, WifiManager wifiManager, ConnectivityManager connectivityManager, Looper looper2) {
        Looper mainLooper;
        this.mConnected = new AtomicBoolean(false);
        this.mAccessPoints = new ArrayList<>();
        this.mSeenBssids = new HashMap<>();
        this.mScanResultCache = new HashMap<>();
        this.mScanId = 0;
        this.mReceiver = new BroadcastReceiver() { // from class: jp.co.benesse.dcha.systemsettings.WifiTracker.1
            @Override // android.content.BroadcastReceiver
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
                    return;
                }
                if ("android.net.wifi.supplicant.STATE_CHANGE".equals(action)) {
                    Logger.d("WifiTracker", "onReceive 0004");
                    WifiTracker.this.updateWifiState(WifiTracker.this.mWifiManager.getWifiState());
                    return;
                }
                if (!"android.net.wifi.STATE_CHANGE".equals(action)) {
                    if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
                        Logger.d("WifiTracker", "onReceive 0006");
                        WifiTracker.this.mWorkHandler.obtainMessage(1, WifiTracker.this.mConnectivityManager.getNetworkInfo(WifiTracker.this.mWifiManager.getCurrentNetwork())).sendToTarget();
                        return;
                    }
                    return;
                }
                Logger.d("WifiTracker", "onReceive 0005");
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                WifiTracker.this.mConnected.set(networkInfo != null ? networkInfo.isConnected() : false);
                WifiTracker.this.mWorkHandler.obtainMessage(1, networkInfo).sendToTarget();
                WifiTracker.this.mWorkHandler.sendEmptyMessage(0);
            }
        };
        Logger.d("WifiTracker", "WifiTracker 0001");
        if (!z && !z2) {
            Logger.d("WifiTracker", "WifiTracker 0002");
            throw new IllegalArgumentException("Must include either saved or scans");
        }
        this.mContext = context;
        if (looper2 == null) {
            Logger.d("WifiTracker", "WifiTracker 0003");
            mainLooper = Looper.getMainLooper();
        } else {
            mainLooper = looper2;
        }
        this.mMainHandler = new MainHandler(mainLooper);
        this.mWorkHandler = new WorkHandler(looper != null ? looper : mainLooper);
        this.mWifiManager = wifiManager;
        this.mIncludeSaved = z;
        this.mIncludeScans = z2;
        this.mIncludePasspoints = z3;
        this.mListener = wifiListener;
        this.mConnectivityManager = connectivityManager;
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mFilter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mFilter.addAction("android.net.wifi.NETWORK_IDS_CHANGED");
        this.mFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        this.mFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        this.mFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mFilter.addAction("android.net.wifi.RSSI_CHANGED");
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
        Logger.d("WifiTracker", "resumeScanning 0001");
        if (this.mScanner == null) {
            Logger.d("WifiTracker", "resumeScanning 0002");
            this.mScanner = new Scanner();
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
            this.mNetworkCallback = new WifiTrackerNetworkCallback();
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
        ArrayList arrayList;
        synchronized (this.mAccessPoints) {
            Logger.d("WifiTracker", "getAccessPoints 0001");
            arrayList = new ArrayList();
            Iterator<AccessPoint> it = this.mAccessPoints.iterator();
            while (it.hasNext()) {
                arrayList.add((AccessPoint) it.next().clone());
            }
            Logger.d("WifiTracker", "getAccessPoints 0002");
        }
        return arrayList;
    }

    public WifiManager getManager() {
        Logger.d("WifiTracker", "getManager 0001");
        return this.mWifiManager;
    }

    private void handleResume() {
        Logger.d("WifiTracker", "handleResume 0001");
        this.mScanResultCache.clear();
        this.mSeenBssids.clear();
        this.mScanId = 0;
        Logger.d("WifiTracker", "handleResume 0002");
    }

    private Collection<ScanResult> fetchScanResults() {
        Logger.d("WifiTracker", "fetchScanResults 0001");
        Integer num = this.mScanId;
        this.mScanId = Integer.valueOf(this.mScanId.intValue() + 1);
        for (ScanResult scanResult : this.mWifiManager.getScanResults()) {
            if (scanResult.SSID != null && !scanResult.SSID.isEmpty()) {
                this.mScanResultCache.put(scanResult.BSSID, scanResult);
                this.mSeenBssids.put(scanResult.BSSID, this.mScanId);
            }
        }
        if (this.mScanId.intValue() > 3) {
            Logger.d("WifiTracker", "fetchScanResults 0002");
            Integer numValueOf = Integer.valueOf(this.mScanId.intValue() - 3);
            Iterator<Map.Entry<String, Integer>> it = this.mSeenBssids.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> next = it.next();
                if (next.getValue().intValue() < numValueOf.intValue()) {
                    this.mScanResultCache.get(next.getKey());
                    this.mScanResultCache.remove(next.getKey());
                    it.remove();
                }
            }
        }
        Logger.d("WifiTracker", "fetchScanResults 0003");
        return this.mScanResultCache.values();
    }

    private WifiConfiguration getWifiConfigurationForNetworkId(int i) {
        Logger.d("WifiTracker", "getWifiConfigurationForNetworkId 0001");
        List<WifiConfiguration> configuredNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            Logger.d("WifiTracker", "getWifiConfigurationForNetworkId 0002");
            for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                if (this.mLastInfo != null && i == wifiConfiguration.networkId && (!wifiConfiguration.selfAdded || wifiConfiguration.numAssociation != 0)) {
                    return wifiConfiguration;
                }
            }
        }
        Logger.d("WifiTracker", "getWifiConfigurationForNetworkId 0003");
        return null;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r10v6, resolved type: byte */
    /* JADX DEBUG: Multi-variable search result rejected for r10v7, resolved type: byte */
    /* JADX DEBUG: Multi-variable search result rejected for r10v9, resolved type: byte */
    /* JADX DEBUG: Multi-variable search result rejected for r8v10, resolved type: byte */
    /* JADX DEBUG: Multi-variable search result rejected for r8v13, resolved type: byte */
    /* JADX DEBUG: Multi-variable search result rejected for r8v9, resolved type: byte */
    /* JADX WARN: Multi-variable type inference failed */
    private void updateAccessPoints() {
        byte b;
        byte b2;
        Logger.d("WifiTracker", "updateAccessPoints 0001");
        List<AccessPoint> accessPoints = getAccessPoints();
        ArrayList<AccessPoint> arrayList = new ArrayList<>();
        Iterator<AccessPoint> it = accessPoints.iterator();
        while (it.hasNext()) {
            it.next().clearConfig();
        }
        WifiConfiguration wifiConfigurationForNetworkId = null;
        Multimap multimap = new Multimap();
        if (this.mLastInfo != null) {
            Logger.d("WifiTracker", "updateAccessPoints 0002");
            wifiConfigurationForNetworkId = getWifiConfigurationForNetworkId(this.mLastInfo.getNetworkId());
        }
        Collection<ScanResult> collectionFetchScanResults = fetchScanResults();
        List<WifiConfiguration> configuredNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            Logger.d("WifiTracker", "updateAccessPoints 0003");
            this.mSavedNetworksExist = configuredNetworks.size() != 0;
            for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                if (!wifiConfiguration.selfAdded || wifiConfiguration.numAssociation != 0) {
                    AccessPoint cachedOrCreate = getCachedOrCreate(wifiConfiguration, accessPoints);
                    if (this.mLastInfo != null && this.mLastNetworkInfo != null && !wifiConfiguration.isPasspoint()) {
                        cachedOrCreate.update(wifiConfigurationForNetworkId, this.mLastInfo, this.mLastNetworkInfo);
                    }
                    if (this.mIncludeSaved) {
                        if (!wifiConfiguration.isPasspoint() || this.mIncludePasspoints) {
                            Iterator<ScanResult> it2 = collectionFetchScanResults.iterator();
                            while (true) {
                                if (it2.hasNext()) {
                                    if (it2.next().SSID.equals(cachedOrCreate.getSsidStr())) {
                                        b2 = true;
                                        break;
                                    }
                                } else {
                                    b2 = false;
                                    break;
                                }
                            }
                            if (b2 == false) {
                                cachedOrCreate.setRssi(Integer.MAX_VALUE);
                            }
                            arrayList.add(cachedOrCreate);
                        }
                        if (!wifiConfiguration.isPasspoint()) {
                            multimap.put(cachedOrCreate.getSsidStr(), cachedOrCreate);
                        }
                    } else {
                        accessPoints.add(cachedOrCreate);
                    }
                }
            }
        }
        if (collectionFetchScanResults != null) {
            Logger.d("WifiTracker", "updateAccessPoints 0004");
            for (ScanResult scanResult : collectionFetchScanResults) {
                if (scanResult.SSID != null && scanResult.SSID.length() != 0 && !scanResult.capabilities.contains("[IBSS]")) {
                    Iterator it3 = multimap.getAll(scanResult.SSID).iterator();
                    while (true) {
                        if (it3.hasNext()) {
                            if (((AccessPoint) it3.next()).update(scanResult)) {
                                b = true;
                                break;
                            }
                        } else {
                            b = false;
                            break;
                        }
                    }
                    if (b == false && this.mIncludeScans) {
                        AccessPoint cachedOrCreate2 = getCachedOrCreate(scanResult, accessPoints);
                        if (this.mLastInfo != null && this.mLastNetworkInfo != null) {
                            cachedOrCreate2.update(wifiConfigurationForNetworkId, this.mLastInfo, this.mLastNetworkInfo);
                        }
                        if (this.mLastInfo != null && this.mLastInfo.getBSSID() != null && this.mLastInfo.getBSSID().equals(scanResult.BSSID) && wifiConfigurationForNetworkId != null && wifiConfigurationForNetworkId.isPasspoint()) {
                            cachedOrCreate2.update(wifiConfigurationForNetworkId);
                        }
                        arrayList.add(cachedOrCreate2);
                        multimap.put(cachedOrCreate2.getSsidStr(), cachedOrCreate2);
                    }
                }
            }
        }
        Collections.sort(arrayList);
        Iterator<AccessPoint> it4 = this.mAccessPoints.iterator();
        while (it4.hasNext()) {
            AccessPoint next = it4.next();
            if (next.getSsid() != null) {
                String ssidStr = next.getSsidStr();
                Iterator<AccessPoint> it5 = arrayList.iterator();
                while (it5.hasNext()) {
                    AccessPoint next2 = it5.next();
                    if (next2.getSsid() == null || !next2.getSsid().equals(ssidStr)) {
                    }
                }
            }
        }
        Logger.d("WifiTracker", "updateAccessPoints 0005");
        this.mAccessPoints = arrayList;
        this.mMainHandler.sendEmptyMessage(2);
    }

    private AccessPoint getCachedOrCreate(ScanResult scanResult, List<AccessPoint> list) {
        Logger.d("WifiTracker", "getCachedOrCreate 0001");
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (list.get(i).matches(scanResult)) {
                AccessPoint accessPointRemove = list.remove(i);
                accessPointRemove.update(scanResult);
                return accessPointRemove;
            }
        }
        Logger.d("WifiTracker", "getCachedOrCreate 0002");
        return new AccessPoint(this.mContext, scanResult);
    }

    private AccessPoint getCachedOrCreate(WifiConfiguration wifiConfiguration, List<AccessPoint> list) {
        Logger.d("WifiTracker", "getCachedOrCreate 0003");
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (list.get(i).matches(wifiConfiguration)) {
                AccessPoint accessPointRemove = list.remove(i);
                accessPointRemove.loadConfig(wifiConfiguration);
                return accessPointRemove;
            }
        }
        Logger.d("WifiTracker", "getCachedOrCreate 0004");
        return new AccessPoint(this.mContext, wifiConfiguration);
    }

    private void updateNetworkInfo(NetworkInfo networkInfo) {
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
        WifiConfiguration wifiConfigurationForNetworkId = null;
        this.mLastInfo = this.mWifiManager.getConnectionInfo();
        if (this.mLastInfo != null) {
            Logger.d("WifiTracker", "updateNetworkInfo 0006");
            wifiConfigurationForNetworkId = getWifiConfigurationForNetworkId(this.mLastInfo.getNetworkId());
        }
        boolean z = false;
        for (int size = this.mAccessPoints.size() - 1; size >= 0; size--) {
            if (this.mAccessPoints.get(size).update(wifiConfigurationForNetworkId, this.mLastInfo, this.mLastNetworkInfo)) {
                z = true;
            }
        }
        if (z) {
            synchronized (this.mAccessPoints) {
                Logger.d("WifiTracker", "updateNetworkInfo 0007");
                Collections.sort(this.mAccessPoints);
            }
            this.mMainHandler.sendEmptyMessage(2);
        }
        Logger.d("WifiTracker", "updateNetworkInfo 0008");
    }

    private void updateWifiState(int i) {
        Logger.d("WifiTracker", "updateWifiState 0001");
        this.mWorkHandler.obtainMessage(3, i, 0).sendToTarget();
        Logger.d("WifiTracker", "updateWifiState 0002");
    }

    private final class WifiTrackerNetworkCallback extends ConnectivityManager.NetworkCallback {
        private WifiTrackerNetworkCallback() {
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            Logger.d("WifiTracker", "onCapabilitiesChanged 0001");
            if (network.equals(WifiTracker.this.mWifiManager.getCurrentNetwork())) {
                Logger.d("WifiTracker", "onCapabilitiesChanged 0002");
                WifiTracker.this.mWorkHandler.sendEmptyMessage(1);
            }
        }
    }

    private final class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            Logger.d("WifiTracker", "MainHandler handleMessage 0001");
            if (WifiTracker.this.mListener == null) {
                Logger.d("WifiTracker", "MainHandler handleMessage 0002");
            }
            switch (message.what) {
                case 0:
                    Logger.d("WifiTracker", "MainHandler handleMessage 0003");
                    WifiTracker.this.mListener.onConnectedChanged();
                    break;
                case 1:
                    Logger.d("WifiTracker", "MainHandler handleMessage 0004");
                    WifiTracker.this.mListener.onWifiStateChanged(message.arg1);
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
                        break;
                    }
                    break;
                case 4:
                    Logger.d("WifiTracker", "MainHandler handleMessage 0008");
                    if (WifiTracker.this.mScanner != null) {
                        Logger.d("WifiTracker", "MainHandler handleMessage 0009");
                        WifiTracker.this.mScanner.pause();
                        break;
                    }
                    break;
            }
        }
    }

    private final class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            Logger.d("WifiTracker", "WorkHandler handleMessage 0001");
            switch (message.what) {
                case 0:
                    Logger.d("WifiTracker", "WorkHandler handleMessage 0002");
                    WifiTracker.this.updateAccessPoints();
                    break;
                case 1:
                    Logger.d("WifiTracker", "WorkHandler handleMessage 0003");
                    WifiTracker.this.updateNetworkInfo((NetworkInfo) message.obj);
                    break;
                case 2:
                    Logger.d("WifiTracker", "WorkHandler handleMessage 0004");
                    WifiTracker.this.handleResume();
                    break;
                case 3:
                    Logger.d("WifiTracker", "WorkHandler handleMessage 0005");
                    if (message.arg1 == 3) {
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
                    WifiTracker.this.mMainHandler.obtainMessage(1, message.arg1, 0).sendToTarget();
                    break;
            }
        }
    }

    private class Scanner extends Handler {
        private int mRetry;

        private Scanner() {
            this.mRetry = 0;
        }

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void pause() {
            this.mRetry = 0;
            removeMessages(0);
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
        private final HashMap<K, List<V>> store;

        private Multimap() {
            this.store = new HashMap<>();
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
}
