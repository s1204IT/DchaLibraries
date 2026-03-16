package com.android.server.connectivity;

import android.content.Context;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.net.BaseNetworkObserver;
import java.net.Inet4Address;

public class Nat464Xlat extends BaseNetworkObserver {
    private static final String CLAT_PREFIX = "v4-";
    private static final String TAG = "Nat464Xlat";
    private String mBaseIface;
    private final Handler mHandler;
    private String mIface;
    private boolean mIsRunning;
    private final INetworkManagementService mNMService;
    private final NetworkAgentInfo mNetwork;

    public Nat464Xlat(Context context, INetworkManagementService nmService, Handler handler, NetworkAgentInfo nai) {
        this.mNMService = nmService;
        this.mHandler = handler;
        this.mNetwork = nai;
    }

    public static boolean requiresClat(NetworkAgentInfo nai) {
        int netType = nai.networkInfo.getType();
        boolean connected = nai.networkInfo.isConnected();
        boolean hasIPv4Address = nai.linkProperties != null ? nai.linkProperties.hasIPv4Address() : false;
        return connected && !hasIPv4Address && (netType == 0 || netType == 1);
    }

    public boolean isStarted() {
        return this.mIface != null;
    }

    private void clear() {
        this.mIface = null;
        this.mBaseIface = null;
        this.mIsRunning = false;
    }

    public void start() {
        if (isStarted()) {
            Slog.e(TAG, "startClat: already started");
            return;
        }
        if (this.mNetwork.linkProperties == null) {
            Slog.e(TAG, "startClat: Can't start clat with null LinkProperties");
            return;
        }
        try {
            this.mNMService.registerObserver(this);
            this.mBaseIface = this.mNetwork.linkProperties.getInterfaceName();
            if (this.mBaseIface == null) {
                Slog.e(TAG, "startClat: Can't start clat on null interface");
                return;
            }
            this.mIface = CLAT_PREFIX + this.mBaseIface;
            Slog.i(TAG, "Starting clatd on " + this.mBaseIface);
            try {
                this.mNMService.startClatd(this.mBaseIface);
            } catch (RemoteException | IllegalStateException e) {
                Slog.e(TAG, "Error starting clatd: " + e);
            }
        } catch (RemoteException e2) {
            Slog.e(TAG, "startClat: Can't register interface observer for clat on " + this.mNetwork);
        }
    }

    public void stop() {
        if (isStarted()) {
            Slog.i(TAG, "Stopping clatd");
            try {
                this.mNMService.stopClatd(this.mBaseIface);
                return;
            } catch (RemoteException | IllegalStateException e) {
                Slog.e(TAG, "Error stopping clatd: " + e);
                return;
            }
        }
        Slog.e(TAG, "clatd: already stopped");
    }

    private void updateConnectivityService(LinkProperties lp) {
        Message msg = this.mHandler.obtainMessage(528387, lp);
        msg.replyTo = this.mNetwork.messenger;
        Slog.i(TAG, "sending message to ConnectivityService: " + msg);
        msg.sendToTarget();
    }

    public void fixupLinkProperties(LinkProperties oldLp) {
        if (this.mNetwork.clatd != null && this.mIsRunning && this.mNetwork.linkProperties != null && !this.mNetwork.linkProperties.getAllInterfaceNames().contains(this.mIface)) {
            Slog.d(TAG, "clatd running, updating NAI for " + this.mIface);
            for (LinkProperties stacked : oldLp.getStackedLinks()) {
                if (this.mIface.equals(stacked.getInterfaceName())) {
                    this.mNetwork.linkProperties.addStackedLink(stacked);
                    return;
                }
            }
        }
    }

    private LinkProperties makeLinkProperties(LinkAddress clatAddress) {
        LinkProperties stacked = new LinkProperties();
        stacked.setInterfaceName(this.mIface);
        RouteInfo ipv4Default = new RouteInfo(new LinkAddress(Inet4Address.ANY, 0), clatAddress.getAddress(), this.mIface);
        stacked.addRoute(ipv4Default);
        stacked.addLinkAddress(clatAddress);
        return stacked;
    }

    private LinkAddress getLinkAddress(String iface) {
        try {
            InterfaceConfiguration config = this.mNMService.getInterfaceConfig(iface);
            return config.getLinkAddress();
        } catch (RemoteException | IllegalStateException e) {
            Slog.e(TAG, "Error getting link properties: " + e);
            return null;
        }
    }

    private void maybeSetIpv6NdOffload(String iface, boolean on) {
        if (this.mNetwork.networkInfo.getType() == 1) {
            try {
                Slog.d(TAG, (on ? "En" : "Dis") + "abling ND offload on " + iface);
                this.mNMService.setInterfaceIpv6NdOffload(iface, on);
            } catch (RemoteException | IllegalStateException e) {
                Slog.w(TAG, "Changing IPv6 ND offload on " + iface + "failed: " + e);
            }
        }
    }

    public void interfaceLinkStateChanged(String iface, boolean up) {
        LinkAddress clatAddress;
        if (isStarted() && up && this.mIface.equals(iface)) {
            Slog.i(TAG, "interface " + iface + " is up, mIsRunning " + this.mIsRunning + "->true");
            if (!this.mIsRunning && (clatAddress = getLinkAddress(iface)) != null) {
                this.mIsRunning = true;
                maybeSetIpv6NdOffload(this.mBaseIface, false);
                LinkProperties lp = new LinkProperties(this.mNetwork.linkProperties);
                lp.addStackedLink(makeLinkProperties(clatAddress));
                Slog.i(TAG, "Adding stacked link " + this.mIface + " on top of " + this.mBaseIface);
                updateConnectivityService(lp);
            }
        }
    }

    public void interfaceRemoved(String iface) {
        if (isStarted() && this.mIface.equals(iface)) {
            Slog.i(TAG, "interface " + iface + " removed, mIsRunning " + this.mIsRunning + "->false");
            if (this.mIsRunning) {
                try {
                    this.mNMService.unregisterObserver(this);
                    this.mNMService.stopClatd(this.mBaseIface);
                } catch (RemoteException e) {
                } catch (IllegalStateException e2) {
                }
                maybeSetIpv6NdOffload(this.mBaseIface, true);
                LinkProperties lp = new LinkProperties(this.mNetwork.linkProperties);
                lp.removeStackedLink(this.mIface);
                clear();
                updateConnectivityService(lp);
            }
        }
    }
}
