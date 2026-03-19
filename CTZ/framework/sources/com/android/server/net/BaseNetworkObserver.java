package com.android.server.net;

import android.net.INetworkManagementEventObserver;
import android.net.LinkAddress;
import android.net.RouteInfo;

public class BaseNetworkObserver extends INetworkManagementEventObserver.Stub {
    @Override
    public void interfaceStatusChanged(String str, boolean z) {
    }

    @Override
    public void interfaceRemoved(String str) {
    }

    @Override
    public void addressUpdated(String str, LinkAddress linkAddress) {
    }

    @Override
    public void addressRemoved(String str, LinkAddress linkAddress) {
    }

    @Override
    public void interfaceLinkStateChanged(String str, boolean z) {
    }

    @Override
    public void interfaceAdded(String str) {
    }

    @Override
    public void interfaceClassDataActivityChanged(String str, boolean z, long j) {
    }

    @Override
    public void limitReached(String str, String str2) {
    }

    @Override
    public void interfaceDnsServerInfo(String str, long j, String[] strArr) {
    }

    @Override
    public void routeUpdated(RouteInfo routeInfo) {
    }

    @Override
    public void routeRemoved(RouteInfo routeInfo) {
    }
}
