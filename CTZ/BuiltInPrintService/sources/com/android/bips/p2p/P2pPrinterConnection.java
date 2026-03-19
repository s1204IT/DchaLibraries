package com.android.bips.p2p;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import com.android.bips.BuiltInPrintService;
import com.android.bips.DelayedAction;
import com.android.bips.discovery.ConnectionListener;
import com.android.bips.discovery.DiscoveredPrinter;
import com.android.bips.discovery.Discovery;
import com.android.bips.discovery.P2pDiscovery;
import com.android.bips.ipp.CapabilitiesCache;
import com.android.bips.jni.LocalPrinterCapabilities;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class P2pPrinterConnection implements Discovery.Listener, P2pConnectionListener, P2pPeerListener {
    private static final String TAG = P2pPrinterConnection.class.getSimpleName();
    private NetworkInterface mInterface;
    private ConnectionListener mListener;
    private final Discovery mMdnsDiscovery;
    private DelayedAction mMdnsDiscoveryTimeout;
    private WifiP2pDevice mPeer;
    private DiscoveredPrinter mPrinter;
    private final BuiltInPrintService mService;

    private P2pPrinterConnection(BuiltInPrintService builtInPrintService, ConnectionListener connectionListener) {
        this.mService = builtInPrintService;
        this.mListener = connectionListener;
        this.mMdnsDiscovery = this.mService.getMdnsDiscovery();
    }

    public P2pPrinterConnection(BuiltInPrintService builtInPrintService, WifiP2pDevice wifiP2pDevice, ConnectionListener connectionListener) {
        this(builtInPrintService, connectionListener);
        connectToPeer(wifiP2pDevice);
    }

    public P2pPrinterConnection(BuiltInPrintService builtInPrintService, DiscoveredPrinter discoveredPrinter, ConnectionListener connectionListener) {
        this(builtInPrintService, connectionListener);
        if (P2pUtils.isOnConnectedInterface(builtInPrintService, discoveredPrinter)) {
            connectToPeer(builtInPrintService.getP2pMonitor().getConnection().getPeer());
        } else {
            this.mPrinter = discoveredPrinter;
            builtInPrintService.getP2pMonitor().discover(this);
        }
    }

    private void connectToPeer(WifiP2pDevice wifiP2pDevice) {
        this.mPeer = wifiP2pDevice;
        this.mService.getP2pMonitor().connect(this.mPeer, this);
    }

    @Override
    public void onPeerFound(WifiP2pDevice wifiP2pDevice) {
        if (this.mListener == null) {
            return;
        }
        if (wifiP2pDevice.deviceAddress.equals(this.mPrinter.path.getHost().replaceAll("-", ":"))) {
            this.mService.getP2pMonitor().stopDiscover(this);
            connectToPeer(wifiP2pDevice);
        }
    }

    @Override
    public void onPeerLost(WifiP2pDevice wifiP2pDevice) {
    }

    @Override
    public void onConnectionOpen(String str, WifiP2pInfo wifiP2pInfo) {
        if (this.mListener == null) {
            return;
        }
        try {
            this.mInterface = NetworkInterface.getByName(str);
        } catch (SocketException e) {
        }
        if (this.mInterface == null) {
            this.mListener.onConnectionComplete(null);
            close();
        } else {
            this.mMdnsDiscoveryTimeout = this.mService.delay(15000, new Runnable() {
                @Override
                public final void run() {
                    P2pPrinterConnection.lambda$onConnectionOpen$0(this.f$0);
                }
            });
            this.mMdnsDiscovery.start(this);
        }
    }

    public static void lambda$onConnectionOpen$0(P2pPrinterConnection p2pPrinterConnection) {
        p2pPrinterConnection.mMdnsDiscovery.stop(p2pPrinterConnection);
        if (p2pPrinterConnection.mListener != null) {
            p2pPrinterConnection.mListener.onConnectionComplete(null);
        }
        p2pPrinterConnection.close();
    }

    @Override
    public void onConnectionClosed() {
        if (this.mListener != null) {
            this.mListener.onConnectionComplete(null);
        }
        close();
    }

    @Override
    public void onConnectionDelayed(boolean z) {
        if (this.mListener == null) {
            return;
        }
        this.mListener.onConnectionDelayed(z);
    }

    @Override
    public void onPrinterFound(final DiscoveredPrinter discoveredPrinter) {
        if (this.mListener == null) {
            return;
        }
        try {
            Inet4Address inet4Address = (Inet4Address) Inet4Address.getByName(discoveredPrinter.path.getHost());
            if (this.mInterface != null && P2pUtils.isOnInterface(this.mInterface, inet4Address)) {
                this.mMdnsDiscovery.stop(this);
                this.mMdnsDiscoveryTimeout.cancel();
                this.mService.getCapabilitiesCache().request(discoveredPrinter, true, new CapabilitiesCache.OnLocalPrinterCapabilities() {
                    @Override
                    public final void onCapabilities(LocalPrinterCapabilities localPrinterCapabilities) {
                        this.f$0.onCapabilities(discoveredPrinter, localPrinterCapabilities);
                    }
                });
            }
        } catch (UnknownHostException e) {
        }
    }

    private void onCapabilities(DiscoveredPrinter discoveredPrinter, LocalPrinterCapabilities localPrinterCapabilities) {
        if (this.mListener == null) {
            return;
        }
        if (localPrinterCapabilities == null) {
            this.mListener.onConnectionComplete(null);
            close();
        } else {
            this.mListener.onConnectionComplete(new DiscoveredPrinter(discoveredPrinter.uuid, discoveredPrinter.name, P2pDiscovery.toPath(this.mPeer), discoveredPrinter.location));
        }
    }

    @Override
    public void onPrinterLost(DiscoveredPrinter discoveredPrinter) {
    }

    public void close() {
        this.mMdnsDiscovery.stop(this);
        if (this.mMdnsDiscoveryTimeout != null) {
            this.mMdnsDiscoveryTimeout.cancel();
        }
        this.mService.getP2pMonitor().stopDiscover(this);
        this.mService.getP2pMonitor().stopConnect(this);
        this.mListener = null;
    }
}
