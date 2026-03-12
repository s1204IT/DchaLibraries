package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AccessPointControllerImpl implements NetworkController.AccessPointController {
    private static final boolean DEBUG = Log.isLoggable("AccessPointController", 3);
    private static final int[] ICONS = {R.drawable.ic_qs_wifi_0, R.drawable.ic_qs_wifi_full_1, R.drawable.ic_qs_wifi_full_2, R.drawable.ic_qs_wifi_full_3, R.drawable.ic_qs_wifi_full_4};
    private final Context mContext;
    private NetworkControllerImpl mNetworkController;
    private boolean mScanning;
    private final UserManager mUserManager;
    private final WifiManager mWifiManager;
    private final ArrayList<NetworkController.AccessPointController.AccessPointCallback> mCallbacks = new ArrayList<>();
    private final Receiver mReceiver = new Receiver();
    private final WifiManager.ActionListener mConnectListener = new WifiManager.ActionListener() {
        public void onSuccess() {
            if (AccessPointControllerImpl.DEBUG) {
                Log.d("AccessPointController", "connect success");
            }
        }

        public void onFailure(int reason) {
            if (AccessPointControllerImpl.DEBUG) {
                Log.d("AccessPointController", "connect failure reason=" + reason);
            }
        }
    };
    private final Comparator<NetworkController.AccessPointController.AccessPoint> mByStrength = new Comparator<NetworkController.AccessPointController.AccessPoint>() {
        @Override
        public int compare(NetworkController.AccessPointController.AccessPoint lhs, NetworkController.AccessPointController.AccessPoint rhs) {
            return -Integer.compare(score(lhs), score(rhs));
        }

        private int score(NetworkController.AccessPointController.AccessPoint ap) {
            return (ap.isConnected ? 20 : 0) + ap.level + (ap.isConfigured ? 10 : 0);
        }
    };
    private int mCurrentUser = ActivityManager.getCurrentUser();

    public AccessPointControllerImpl(Context context) {
        this.mContext = context;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
    }

    void setNetworkController(NetworkControllerImpl networkController) {
        this.mNetworkController = networkController;
    }

    @Override
    public boolean canConfigWifi() {
        return !this.mUserManager.hasUserRestriction("no_config_wifi", new UserHandle(this.mCurrentUser));
    }

    public void onUserSwitched(int newUserId) {
        this.mCurrentUser = newUserId;
    }

    @Override
    public void addAccessPointCallback(NetworkController.AccessPointController.AccessPointCallback callback) {
        if (callback != null && !this.mCallbacks.contains(callback)) {
            if (DEBUG) {
                Log.d("AccessPointController", "addCallback " + callback);
            }
            this.mCallbacks.add(callback);
            this.mReceiver.setListening(!this.mCallbacks.isEmpty());
        }
    }

    @Override
    public void removeAccessPointCallback(NetworkController.AccessPointController.AccessPointCallback callback) {
        if (callback != null) {
            if (DEBUG) {
                Log.d("AccessPointController", "removeCallback " + callback);
            }
            this.mCallbacks.remove(callback);
            this.mReceiver.setListening(!this.mCallbacks.isEmpty());
        }
    }

    @Override
    public void scanForAccessPoints() {
        if (!this.mScanning) {
            if (DEBUG) {
                Log.d("AccessPointController", "scan!");
            }
            this.mScanning = this.mWifiManager.startScan();
            updateAccessPoints();
        }
    }

    @Override
    public boolean connect(NetworkController.AccessPointController.AccessPoint ap) {
        if (ap == null) {
            return false;
        }
        if (DEBUG) {
            Log.d("AccessPointController", "connect networkId=" + ap.networkId);
        }
        if (ap.networkId < 0) {
            if (ap.hasSecurity) {
                Intent intent = new Intent("android.settings.WIFI_SETTINGS");
                intent.putExtra("wifi_start_connect_ssid", ap.ssid);
                intent.addFlags(268435456);
                fireSettingsIntentCallback(intent);
                return true;
            }
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = "\"" + ap.ssid + "\"";
            config.allowedKeyManagement.set(0);
            this.mWifiManager.connect(config, this.mConnectListener);
            return false;
        }
        this.mWifiManager.connect(ap.networkId, this.mConnectListener);
        return false;
    }

    private void fireSettingsIntentCallback(Intent intent) {
        for (NetworkController.AccessPointController.AccessPointCallback callback : this.mCallbacks) {
            callback.onSettingsActivityTriggered(intent);
        }
    }

    private void fireAcccessPointsCallback(NetworkController.AccessPointController.AccessPoint[] aps) {
        for (NetworkController.AccessPointController.AccessPointCallback callback : this.mCallbacks) {
            callback.onAccessPointsChanged(aps);
        }
    }

    private static String trimDoubleQuotes(String v) {
        return (v == null || v.length() < 2 || v.charAt(0) != '\"' || v.charAt(v.length() + (-1)) != '\"') ? v : v.substring(1, v.length() - 1);
    }

    private int getConnectedNetworkId(WifiInfo wifiInfo) {
        if (wifiInfo != null) {
            return wifiInfo.getNetworkId();
        }
        return -1;
    }

    private ArrayMap<String, WifiConfiguration> getConfiguredNetworksBySsid() {
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs == null || configs.size() == 0) {
            return ArrayMap.EMPTY;
        }
        ArrayMap<String, WifiConfiguration> rt = new ArrayMap<>();
        for (WifiConfiguration config : configs) {
            rt.put(trimDoubleQuotes(config.SSID), config);
        }
        return rt;
    }

    public void updateAccessPoints() {
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        int connectedNetworkId = getConnectedNetworkId(wifiInfo);
        if (DEBUG) {
            Log.d("AccessPointController", "connectedNetworkId: " + connectedNetworkId);
        }
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        ArrayMap<String, WifiConfiguration> configured = getConfiguredNetworksBySsid();
        if (DEBUG) {
            Log.d("AccessPointController", "scanResults: " + scanResults);
        }
        List<NetworkController.AccessPointController.AccessPoint> aps = new ArrayList<>(scanResults.size());
        ArraySet<String> ssids = new ArraySet<>();
        for (ScanResult scanResult : scanResults) {
            if (scanResult != null) {
                String ssid = scanResult.SSID;
                if (!TextUtils.isEmpty(ssid) && !ssids.contains(ssid)) {
                    ssids.add(ssid);
                    WifiConfiguration config = configured.get(ssid);
                    int level = WifiManager.calculateSignalLevel(scanResult.level, ICONS.length);
                    NetworkController.AccessPointController.AccessPoint ap = new NetworkController.AccessPointController.AccessPoint();
                    ap.isConfigured = config != null;
                    ap.networkId = config != null ? config.networkId : -1;
                    ap.ssid = ssid;
                    ap.isConnected = (ap.networkId != -1 && ap.networkId == connectedNetworkId) || (ap.networkId == -1 && wifiInfo != null && ap.ssid.equals(trimDoubleQuotes(wifiInfo.getSSID())));
                    if (ap.isConnected && this.mNetworkController != null) {
                        ap.level = this.mNetworkController.getConnectedWifiLevel();
                    } else {
                        ap.level = level;
                    }
                    ap.iconId = ICONS[ap.level];
                    ap.hasSecurity = scanResult.capabilities.contains("WEP") || scanResult.capabilities.contains("PSK") || scanResult.capabilities.contains("EAP");
                    aps.add(ap);
                }
            }
        }
        Collections.sort(aps, this.mByStrength);
        fireAcccessPointsCallback((NetworkController.AccessPointController.AccessPoint[]) aps.toArray(new NetworkController.AccessPointController.AccessPoint[aps.size()]));
    }

    private final class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        private Receiver() {
        }

        public void setListening(boolean listening) {
            if (listening && !this.mRegistered) {
                if (AccessPointControllerImpl.DEBUG) {
                    Log.d("AccessPointController", "Registering receiver");
                }
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
                filter.addAction("android.net.wifi.SCAN_RESULTS");
                filter.addAction("android.net.wifi.NETWORK_IDS_CHANGED");
                filter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
                filter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
                filter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
                filter.addAction("android.net.wifi.STATE_CHANGE");
                filter.addAction("android.net.wifi.RSSI_CHANGED");
                AccessPointControllerImpl.this.mContext.registerReceiver(this, filter);
                this.mRegistered = true;
                return;
            }
            if (!listening && this.mRegistered) {
                if (AccessPointControllerImpl.DEBUG) {
                    Log.d("AccessPointController", "Unregistering receiver");
                }
                AccessPointControllerImpl.this.mContext.unregisterReceiver(this);
                this.mRegistered = false;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AccessPointControllerImpl.DEBUG) {
                Log.d("AccessPointController", "onReceive " + intent.getAction());
            }
            if ("android.net.wifi.SCAN_RESULTS".equals(intent.getAction())) {
                AccessPointControllerImpl.this.updateAccessPoints();
                AccessPointControllerImpl.this.mScanning = false;
            }
        }
    }
}
