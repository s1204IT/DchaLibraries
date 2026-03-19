package com.android.bips.p2p;

import android.net.wifi.p2p.WifiP2pDevice;

public interface P2pPeerListener {
    void onPeerFound(WifiP2pDevice wifiP2pDevice);

    void onPeerLost(WifiP2pDevice wifiP2pDevice);
}
