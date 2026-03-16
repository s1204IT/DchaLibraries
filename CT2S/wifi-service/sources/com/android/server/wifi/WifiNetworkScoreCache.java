package com.android.server.wifi;

import android.content.Context;
import android.net.INetworkScoreCache;
import android.net.ScoredNetwork;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiNetworkScoreCache extends INetworkScoreCache.Stub {
    public static int INVALID_NETWORK_SCORE = -128;
    private static String TAG = "WifiNetworkScoreCache";
    private final Context mContext;
    private boolean DBG = true;
    private final Map<String, ScoredNetwork> mNetworkCache = new HashMap();

    public WifiNetworkScoreCache(Context context) {
        this.mContext = context;
    }

    public final void updateScores(List<ScoredNetwork> networks) {
        if (networks != null) {
            Log.e(TAG, "updateScores list size=" + networks.size());
            synchronized (this.mNetworkCache) {
                for (ScoredNetwork network : networks) {
                    String networkKey = buildNetworkKey(network);
                    if (networkKey != null) {
                        this.mNetworkCache.put(networkKey, network);
                    }
                }
            }
        }
    }

    public final void clearScores() {
        synchronized (this.mNetworkCache) {
            this.mNetworkCache.clear();
        }
    }

    public boolean isScoredNetwork(ScanResult result) {
        return getScoredNetwork(result) != null;
    }

    public boolean hasScoreCurve(ScanResult result) {
        ScoredNetwork network = getScoredNetwork(result);
        return (network == null || network.rssiCurve == null) ? false : true;
    }

    public int getNetworkScore(ScanResult result) {
        int score = INVALID_NETWORK_SCORE;
        ScoredNetwork network = getScoredNetwork(result);
        if (network != null && network.rssiCurve != null) {
            score = network.rssiCurve.lookupScore(result.level);
            if (this.DBG) {
                Log.e(TAG, "getNetworkScore found scored network " + network.networkKey + " score " + Integer.toString(score) + " RSSI " + result.level);
            }
        }
        return score;
    }

    public int getNetworkScore(ScanResult result, boolean isActiveNetwork) {
        int score = INVALID_NETWORK_SCORE;
        ScoredNetwork network = getScoredNetwork(result);
        if (network != null && network.rssiCurve != null) {
            score = network.rssiCurve.lookupScore(result.level, isActiveNetwork);
            if (this.DBG) {
                Log.e(TAG, "getNetworkScore found scored network " + network.networkKey + " score " + Integer.toString(score) + " RSSI " + result.level + " isActiveNetwork " + isActiveNetwork);
            }
        }
        return score;
    }

    private ScoredNetwork getScoredNetwork(ScanResult result) {
        ScoredNetwork scoredNetwork;
        String key = buildNetworkKey(result);
        if (key == null) {
            return null;
        }
        synchronized (this.mNetworkCache) {
            scoredNetwork = this.mNetworkCache.get(key);
        }
        return scoredNetwork;
    }

    private String buildNetworkKey(ScoredNetwork network) {
        String key;
        if (network.networkKey != null && network.networkKey.wifiKey != null && network.networkKey.type == 1 && (key = network.networkKey.wifiKey.ssid) != null) {
            if (network.networkKey.wifiKey.bssid != null) {
                return key + network.networkKey.wifiKey.bssid;
            }
            return key;
        }
        return null;
    }

    private String buildNetworkKey(ScanResult result) {
        if (result.SSID == null) {
            return null;
        }
        StringBuilder key = new StringBuilder("\"");
        key.append(result.SSID);
        key.append("\"");
        if (result.BSSID != null) {
            key.append(result.BSSID);
        }
        return key.toString();
    }

    protected final void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        writer.println("WifiNetworkScoreCache");
        writer.println("  All score curves:");
        for (Map.Entry<String, ScoredNetwork> entry : this.mNetworkCache.entrySet()) {
            writer.println("    " + entry.getKey() + ": " + entry.getValue().rssiCurve);
        }
        writer.println("  Current network scores:");
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        for (ScanResult scanResult : wifiManager.getScanResults()) {
            writer.println("    " + buildNetworkKey(scanResult) + ": " + getNetworkScore(scanResult));
        }
    }
}
