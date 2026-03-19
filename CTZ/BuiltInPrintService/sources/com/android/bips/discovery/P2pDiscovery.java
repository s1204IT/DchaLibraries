package com.android.bips.discovery;

import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import com.android.bips.BuiltInPrintService;
import com.android.bips.p2p.P2pPeerListener;

public class P2pDiscovery extends SavedDiscovery implements P2pPeerListener {
    private static final String TAG = P2pDiscovery.class.getSimpleName();
    private boolean mDiscoveringPeers;

    public P2pDiscovery(BuiltInPrintService builtInPrintService) {
        super(builtInPrintService);
        this.mDiscoveringPeers = false;
    }

    public static DiscoveredPrinter toPrinter(WifiP2pDevice wifiP2pDevice) {
        Uri path = toPath(wifiP2pDevice);
        String str = wifiP2pDevice.deviceName;
        if (str.trim().isEmpty()) {
            str = wifiP2pDevice.deviceAddress;
        }
        return new DiscoveredPrinter(path, str, path, null);
    }

    public static Uri toPath(WifiP2pDevice wifiP2pDevice) {
        return Uri.parse("p2p://" + wifiP2pDevice.deviceAddress.replace(":", "-"));
    }

    @Override
    void onStart() {
        startPeerDiscovery();
    }

    @Override
    void onStop() {
        if (this.mDiscoveringPeers) {
            this.mDiscoveringPeers = false;
            getPrintService().getP2pMonitor().stopDiscover(this);
            allPrintersLost();
        }
    }

    private void startPeerDiscovery() {
        if (this.mDiscoveringPeers || getSavedPrinters().isEmpty()) {
            return;
        }
        this.mDiscoveringPeers = true;
        getPrintService().getP2pMonitor().discover(this);
    }

    @Override
    public void onPeerFound(WifiP2pDevice wifiP2pDevice) {
        DiscoveredPrinter printer = toPrinter(wifiP2pDevice);
        for (DiscoveredPrinter discoveredPrinter : getSavedPrinters()) {
            if (discoveredPrinter.path.equals(printer.path)) {
                printerFound(discoveredPrinter);
            }
        }
    }

    @Override
    public void onPeerLost(WifiP2pDevice wifiP2pDevice) {
        printerLost(toPrinter(wifiP2pDevice).getUri());
    }

    public void addValidPrinter(DiscoveredPrinter discoveredPrinter) {
        if (addSavedPrinter(discoveredPrinter)) {
            printerFound(discoveredPrinter);
            if (isStarted()) {
                startPeerDiscovery();
            }
        }
    }
}
