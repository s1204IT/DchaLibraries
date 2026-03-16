package com.android.server.net;

import android.net.INetworkManagementEventObserver;
import android.net.LinkAddress;
import android.net.RouteInfo;

public class BaseNetworkObserver extends INetworkManagementEventObserver.Stub {
    @Override
    public void interfaceStatusChanged(String iface, boolean up) {
    }

    @Override
    public void interfaceRemoved(String iface) {
    }

    @Override
    public void addressUpdated(String iface, LinkAddress address) {
    }

    @Override
    public void addressRemoved(String iface, LinkAddress address) {
    }

    @Override
    public void interfaceLinkStateChanged(String iface, boolean up) {
    }

    @Override
    public void interfaceAdded(String iface) {
    }

    @Override
    public void interfaceClassDataActivityChanged(String label, boolean active, long tsNanos) {
    }

    @Override
    public void limitReached(String limitName, String iface) {
    }

    @Override
    public void interfaceDnsServerInfo(String iface, long lifetime, String[] servers) {
    }

    @Override
    public void routeUpdated(RouteInfo route) {
    }

    @Override
    public void routeRemoved(RouteInfo route) {
    }
}
