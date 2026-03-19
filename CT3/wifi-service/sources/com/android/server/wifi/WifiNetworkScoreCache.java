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
    private static final boolean DBG = false;
    public static final int INVALID_NETWORK_SCORE = -128;
    private static final String TAG = "WifiNetworkScoreCache";
    private final Context mContext;
    private final Map<String, ScoredNetwork> mNetworkCache = new HashMap();

    public WifiNetworkScoreCache(Context context) {
        this.mContext = context;
    }

    public final void updateScores(List<ScoredNetwork> networks) {
        if (networks == null) {
            return;
        }
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
        ScoredNetwork network = getScoredNetwork(result);
        if (network == null || network.rssiCurve == null) {
            return INVALID_NETWORK_SCORE;
        }
        int score = network.rssiCurve.lookupScore(result.level);
        return score;
    }

    public boolean getMeteredHint(ScanResult result) {
        ScoredNetwork network = getScoredNetwork(result);
        if (network != null) {
            return network.meteredHint;
        }
        return false;
    }

    public int getNetworkScore(ScanResult result, boolean isActiveNetwork) {
        ScoredNetwork network = getScoredNetwork(result);
        if (network == null || network.rssiCurve == null) {
            return INVALID_NETWORK_SCORE;
        }
        int score = network.rssiCurve.lookupScore(result.level, isActiveNetwork);
        return score;
    }

    private ScoredNetwork getScoredNetwork(ScanResult result) {
        ScoredNetwork network;
        String key = buildNetworkKey(result);
        if (key == null) {
            return null;
        }
        synchronized (this.mNetworkCache) {
            network = this.mNetworkCache.get(key);
        }
        return network;
    }

    private String buildNetworkKey(ScoredNetwork network) {
        String key;
        if (network == null || network.networkKey == null || network.networkKey.wifiKey == null || network.networkKey.type != 1 || (key = network.networkKey.wifiKey.ssid) == null) {
            return null;
        }
        if (network.networkKey.wifiKey.bssid != null) {
            return key + network.networkKey.wifiKey.bssid;
        }
        return key;
    }

    private String buildNetworkKey(ScanResult result) {
        if (result == null || result.SSID == null) {
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
        writer.println(TAG);
        writer.println("  All score curves:");
        for (Map.Entry<String, ScoredNetwork> entry : this.mNetworkCache.entrySet()) {
            ScoredNetwork scoredNetwork = entry.getValue();
            writer.println("    " + entry.getKey() + ": " + scoredNetwork.rssiCurve + ", meteredHint=" + scoredNetwork.meteredHint);
        }
        writer.println("  Current network scores:");
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        for (ScanResult scanResult : wifiManager.getScanResults()) {
            writer.println("    " + buildNetworkKey(scanResult) + ": " + getNetworkScore(scanResult));
        }
    }
}
