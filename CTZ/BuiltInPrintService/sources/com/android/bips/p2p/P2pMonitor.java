package com.android.bips.p2p;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import com.android.bips.BuiltInPrintService;
import java.util.Objects;

public class P2pMonitor {
    private static final String TAG = P2pMonitor.class.getSimpleName();
    private String mConnectedInterface;
    private P2pConnectionProcedure mConnection;
    private final WifiP2pManager mP2pManager;
    private P2pDiscoveryProcedure mPeerDiscovery;
    private final BuiltInPrintService mService;

    public P2pMonitor(BuiltInPrintService builtInPrintService) {
        this.mService = builtInPrintService;
        this.mP2pManager = (WifiP2pManager) this.mService.getSystemService("wifip2p");
    }

    public void discover(P2pPeerListener p2pPeerListener) {
        if (this.mP2pManager == null) {
            return;
        }
        if (this.mPeerDiscovery == null) {
            this.mPeerDiscovery = new P2pDiscoveryProcedure(this.mService, this.mP2pManager, p2pPeerListener);
        } else {
            this.mPeerDiscovery.addListener(p2pPeerListener);
        }
    }

    public void stopDiscover(P2pPeerListener p2pPeerListener) {
        if (this.mPeerDiscovery != null) {
            this.mPeerDiscovery.removeListener(p2pPeerListener);
            if (this.mPeerDiscovery.getListeners().isEmpty()) {
                this.mPeerDiscovery.cancel();
                this.mPeerDiscovery = null;
            }
        }
    }

    public void connect(WifiP2pDevice wifiP2pDevice, final P2pConnectionListener p2pConnectionListener) {
        if (this.mP2pManager == null) {
            Handler mainHandler = this.mService.getMainHandler();
            Objects.requireNonNull(p2pConnectionListener);
            mainHandler.post(new Runnable() {
                @Override
                public final void run() {
                    p2pConnectionListener.onConnectionClosed();
                }
            });
            return;
        }
        if (this.mConnection != null && !wifiP2pDevice.deviceAddress.equals(this.mConnection.getPeer().deviceAddress)) {
            if (this.mConnection.getListenerCount() == 1) {
                this.mConnection.close();
                this.mConnection = null;
            } else {
                Handler mainHandler2 = this.mService.getMainHandler();
                Objects.requireNonNull(p2pConnectionListener);
                mainHandler2.post(new Runnable() {
                    @Override
                    public final void run() {
                        p2pConnectionListener.onConnectionClosed();
                    }
                });
                return;
            }
        }
        if (this.mConnection == null) {
            this.mConnection = new P2pConnectionProcedure(this.mService, this.mP2pManager, wifiP2pDevice, new P2pConnectionListener() {
                @Override
                public void onConnectionOpen(String str, WifiP2pInfo wifiP2pInfo) {
                    P2pMonitor.this.mConnectedInterface = str;
                }

                @Override
                public void onConnectionClosed() {
                    P2pMonitor.this.mConnectedInterface = null;
                }

                @Override
                public void onConnectionDelayed(boolean z) {
                }
            });
        }
        this.mConnection.addListener(p2pConnectionListener);
    }

    void stopConnect(P2pConnectionListener p2pConnectionListener) {
        if (this.mConnection == null || !this.mConnection.hasListener(p2pConnectionListener)) {
            return;
        }
        this.mConnection.removeListener(p2pConnectionListener);
        if (this.mConnection.getListenerCount() == 1 && this.mConnectedInterface == null) {
            this.mConnection.close();
            this.mConnection = null;
        }
    }

    P2pConnectionProcedure getConnection() {
        return this.mConnection;
    }

    public String getConnectedInterface() {
        return this.mConnectedInterface;
    }

    public void stopAll() {
        if (this.mConnection != null) {
            this.mConnection.close();
            this.mConnection = null;
            this.mConnectedInterface = null;
        }
        if (this.mPeerDiscovery != null) {
            this.mPeerDiscovery.cancel();
            this.mPeerDiscovery = null;
        }
    }
}
