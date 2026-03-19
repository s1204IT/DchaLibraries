package com.android.bips.p2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import com.android.bips.BuiltInPrintService;
import com.android.bips.util.BroadcastMonitor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

class P2pDiscoveryProcedure {
    private BroadcastMonitor mBroadcastMonitor;
    private WifiP2pManager.Channel mChannel;
    private final WifiP2pManager mP2pManager;
    private static final String TAG = P2pDiscoveryProcedure.class.getSimpleName();
    private static final Pattern PRINTER_PATTERN = Pattern.compile("^(^3-.+-[145])|(0003.+000[145])$");
    private final List<P2pPeerListener> mListeners = new CopyOnWriteArrayList();
    private final List<WifiP2pDevice> mPeers = new ArrayList();

    P2pDiscoveryProcedure(BuiltInPrintService builtInPrintService, WifiP2pManager wifiP2pManager, P2pPeerListener p2pPeerListener) {
        this.mP2pManager = wifiP2pManager;
        this.mChannel = this.mP2pManager.initialize(builtInPrintService, Looper.getMainLooper(), null);
        this.mListeners.add(p2pPeerListener);
        this.mBroadcastMonitor = builtInPrintService.receiveBroadcasts(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.net.wifi.p2p.STATE_CHANGED".equals(action)) {
                    if (intent.getIntExtra("wifi_p2p_state", -1) == 2) {
                        P2pDiscoveryProcedure.this.mP2pManager.stopPeerDiscovery(P2pDiscoveryProcedure.this.mChannel, null);
                        P2pDiscoveryProcedure.this.mP2pManager.discoverPeers(P2pDiscoveryProcedure.this.mChannel, null);
                        return;
                    }
                    return;
                }
                if ("android.net.wifi.p2p.PEERS_CHANGED".equals(action)) {
                    Collection<WifiP2pDevice> deviceList = ((WifiP2pDeviceList) intent.getParcelableExtra("wifiP2pDeviceList")).getDeviceList();
                    P2pDiscoveryProcedure.this.updatePeers(deviceList);
                    if (deviceList.isEmpty()) {
                        P2pDiscoveryProcedure.this.mP2pManager.stopPeerDiscovery(P2pDiscoveryProcedure.this.mChannel, null);
                        P2pDiscoveryProcedure.this.mP2pManager.discoverPeers(P2pDiscoveryProcedure.this.mChannel, null);
                    }
                }
            }
        }, "android.net.wifi.p2p.STATE_CHANGED", "android.net.wifi.p2p.PEERS_CHANGED");
        this.mP2pManager.discoverPeers(this.mChannel, null);
    }

    void addListener(P2pPeerListener p2pPeerListener) {
        this.mListeners.add(p2pPeerListener);
        if (!this.mPeers.isEmpty()) {
            Iterator<WifiP2pDevice> it = this.mPeers.iterator();
            while (it.hasNext()) {
                p2pPeerListener.onPeerFound(it.next());
            }
        }
    }

    void removeListener(P2pPeerListener p2pPeerListener) {
        this.mListeners.remove(p2pPeerListener);
    }

    List<P2pPeerListener> getListeners() {
        return this.mListeners;
    }

    private void updatePeers(Collection<WifiP2pDevice> collection) {
        ArrayList<WifiP2pDevice> arrayList = new ArrayList(this.mPeers);
        this.mPeers.clear();
        for (WifiP2pDevice wifiP2pDevice : collection) {
            if (PRINTER_PATTERN.matcher(wifiP2pDevice.primaryDeviceType).find()) {
                this.mPeers.add(wifiP2pDevice);
            }
        }
        HashSet hashSet = new HashSet();
        for (WifiP2pDevice wifiP2pDevice2 : this.mPeers) {
            hashSet.add(wifiP2pDevice2.deviceAddress);
            WifiP2pDevice device = getDevice(arrayList, wifiP2pDevice2.deviceAddress);
            if (device == null || !device.equals(wifiP2pDevice2)) {
                Iterator<P2pPeerListener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    it.next().onPeerFound(wifiP2pDevice2);
                }
            }
        }
        for (WifiP2pDevice wifiP2pDevice3 : arrayList) {
            if (!hashSet.contains(wifiP2pDevice3.deviceAddress)) {
                Iterator<P2pPeerListener> it2 = this.mListeners.iterator();
                while (it2.hasNext()) {
                    it2.next().onPeerLost(wifiP2pDevice3);
                }
            }
        }
    }

    private WifiP2pDevice getDevice(Collection<WifiP2pDevice> collection, String str) {
        for (WifiP2pDevice wifiP2pDevice : collection) {
            if (wifiP2pDevice.deviceAddress.equals(str)) {
                return wifiP2pDevice;
            }
        }
        return null;
    }

    public void cancel() {
        this.mBroadcastMonitor.close();
        if (this.mChannel != null) {
            this.mP2pManager.stopPeerDiscovery(this.mChannel, null);
            this.mChannel.close();
            this.mChannel = null;
        }
    }
}
