package com.android.bips.p2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import com.android.bips.BuiltInPrintService;
import com.android.bips.DelayedAction;
import com.android.bips.util.BroadcastMonitor;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class P2pConnectionProcedure extends BroadcastReceiver {
    private static final String TAG = P2pConnectionProcedure.class.getSimpleName();
    private WifiP2pManager.Channel mChannel;
    private final BroadcastMonitor mConnectionMonitor;
    private DelayedAction mDetectDelayed;
    private WifiP2pInfo mInfo;
    private String mNetwork;
    private final WifiP2pManager mP2pManager;
    private final WifiP2pDevice mPeer;
    private final BuiltInPrintService mService;
    private final List<P2pConnectionListener> mListeners = new CopyOnWriteArrayList();
    private boolean mInvited = false;
    private boolean mDelayed = false;

    P2pConnectionProcedure(BuiltInPrintService builtInPrintService, WifiP2pManager wifiP2pManager, WifiP2pDevice wifiP2pDevice, P2pConnectionListener p2pConnectionListener) {
        this.mService = builtInPrintService;
        this.mP2pManager = wifiP2pManager;
        this.mPeer = wifiP2pDevice;
        this.mConnectionMonitor = builtInPrintService.receiveBroadcasts(this, "android.net.wifi.p2p.CONNECTION_STATE_CHANGE", "android.net.wifi.p2p.PEERS_CHANGED");
        this.mChannel = this.mP2pManager.initialize(builtInPrintService, Looper.getMainLooper(), null);
        this.mListeners.add(p2pConnectionListener);
        this.mP2pManager.connect(this.mChannel, configForPeer(wifiP2pDevice), null);
    }

    private WifiP2pConfig configForPeer(WifiP2pDevice wifiP2pDevice) {
        WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
        wifiP2pConfig.deviceAddress = wifiP2pDevice.deviceAddress;
        if (wifiP2pDevice.wpsPbcSupported()) {
            wifiP2pConfig.wps.setup = 0;
        } else if (wifiP2pDevice.wpsKeypadSupported()) {
            wifiP2pConfig.wps.setup = 2;
        } else {
            wifiP2pConfig.wps.setup = 1;
        }
        return wifiP2pConfig;
    }

    public WifiP2pDevice getPeer() {
        return this.mPeer;
    }

    boolean hasListener(P2pConnectionListener p2pConnectionListener) {
        return this.mListeners.contains(p2pConnectionListener);
    }

    void addListener(P2pConnectionListener p2pConnectionListener) {
        if (this.mInfo != null) {
            p2pConnectionListener.onConnectionOpen(this.mNetwork, this.mInfo);
        }
        this.mListeners.add(p2pConnectionListener);
    }

    void removeListener(P2pConnectionListener p2pConnectionListener) {
        this.mListeners.remove(p2pConnectionListener);
    }

    int getListenerCount() {
        return this.mListeners.size();
    }

    public void close() {
        this.mListeners.clear();
        this.mConnectionMonitor.close();
        if (this.mDetectDelayed != null) {
            this.mDetectDelayed.cancel();
        }
        if (this.mChannel != null) {
            this.mP2pManager.cancelConnect(this.mChannel, null);
            this.mP2pManager.removeGroup(this.mChannel, null);
            this.mChannel.close();
            this.mChannel = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.net.wifi.p2p.CONNECTION_STATE_CHANGE".equals(intent.getAction())) {
            if ("android.net.wifi.p2p.PEERS_CHANGED".equals(intent.getAction())) {
                WifiP2pDevice wifiP2pDevice = ((WifiP2pDeviceList) intent.getParcelableExtra("wifiP2pDeviceList")).get(this.mPeer.deviceAddress);
                if (!this.mInvited && wifiP2pDevice != null && wifiP2pDevice.status == 1) {
                    this.mInvited = true;
                    this.mDetectDelayed = this.mService.delay(3000, new Runnable() {
                        @Override
                        public final void run() {
                            P2pConnectionProcedure.lambda$onReceive$0(this.f$0);
                        }
                    });
                    return;
                }
                return;
            }
            return;
        }
        NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
        WifiP2pGroup wifiP2pGroup = (WifiP2pGroup) intent.getParcelableExtra("p2pGroupInfo");
        WifiP2pInfo wifiP2pInfo = (WifiP2pInfo) intent.getParcelableExtra("wifiP2pInfo");
        if (networkInfo.isConnected()) {
            if (isConnectedToPeer(wifiP2pGroup)) {
                if (this.mDelayed) {
                    Iterator<P2pConnectionListener> it = this.mListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onConnectionDelayed(false);
                    }
                } else if (this.mDetectDelayed != null) {
                    this.mDetectDelayed.cancel();
                }
                this.mNetwork = wifiP2pGroup.getInterface();
                this.mInfo = wifiP2pInfo;
                Iterator<P2pConnectionListener> it2 = this.mListeners.iterator();
                while (it2.hasNext()) {
                    it2.next().onConnectionOpen(this.mNetwork, this.mInfo);
                }
                return;
            }
            return;
        }
        if (this.mInvited) {
            Iterator<P2pConnectionListener> it3 = this.mListeners.iterator();
            while (it3.hasNext()) {
                it3.next().onConnectionClosed();
            }
            close();
        }
    }

    public static void lambda$onReceive$0(P2pConnectionProcedure p2pConnectionProcedure) {
        p2pConnectionProcedure.mDelayed = true;
        Iterator<P2pConnectionListener> it = p2pConnectionProcedure.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConnectionDelayed(true);
        }
    }

    private boolean isConnectedToPeer(WifiP2pGroup wifiP2pGroup) {
        WifiP2pDevice owner = wifiP2pGroup.getOwner();
        if (owner != null && owner.deviceAddress.equals(this.mPeer.deviceAddress)) {
            return true;
        }
        Iterator<WifiP2pDevice> it = wifiP2pGroup.getClientList().iterator();
        while (it.hasNext()) {
            if (it.next().deviceAddress.equals(this.mPeer.deviceAddress)) {
                return true;
            }
        }
        return false;
    }
}
