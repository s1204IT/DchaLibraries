package com.android.bips.p2p;

import android.net.wifi.p2p.WifiP2pInfo;

public interface P2pConnectionListener {
    void onConnectionClosed();

    void onConnectionDelayed(boolean z);

    void onConnectionOpen(String str, WifiP2pInfo wifiP2pInfo);
}
