package com.android.server.net;

import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.RouteInfo;
import java.util.Arrays;

public class NetlinkTracker extends BaseNetworkObserver {
    private static final boolean DBG = false;
    private final String TAG;
    private final Callback mCallback;
    private DnsServerRepository mDnsServerRepository;
    private final String mInterfaceName;
    private final LinkProperties mLinkProperties = new LinkProperties();

    public interface Callback {
        void update();
    }

    public NetlinkTracker(String str, Callback callback) {
        this.TAG = "NetlinkTracker/" + str;
        this.mInterfaceName = str;
        this.mCallback = callback;
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
        this.mDnsServerRepository = new DnsServerRepository();
    }

    private void maybeLog(String str, String str2, LinkAddress linkAddress) {
    }

    private void maybeLog(String str, Object obj) {
    }

    @Override
    public void interfaceRemoved(String str) {
        maybeLog("interfaceRemoved", str);
        if (this.mInterfaceName.equals(str)) {
            clearLinkProperties();
            this.mCallback.update();
        }
    }

    @Override
    public void addressUpdated(String str, LinkAddress linkAddress) {
        boolean zAddLinkAddress;
        if (this.mInterfaceName.equals(str)) {
            maybeLog("addressUpdated", str, linkAddress);
            synchronized (this) {
                zAddLinkAddress = this.mLinkProperties.addLinkAddress(linkAddress);
            }
            if (zAddLinkAddress) {
                this.mCallback.update();
            }
        }
    }

    @Override
    public void addressRemoved(String str, LinkAddress linkAddress) {
        boolean zRemoveLinkAddress;
        if (this.mInterfaceName.equals(str)) {
            maybeLog("addressRemoved", str, linkAddress);
            synchronized (this) {
                zRemoveLinkAddress = this.mLinkProperties.removeLinkAddress(linkAddress);
            }
            if (zRemoveLinkAddress) {
                this.mCallback.update();
            }
        }
    }

    @Override
    public void routeUpdated(RouteInfo routeInfo) {
        boolean zAddRoute;
        if (this.mInterfaceName.equals(routeInfo.getInterface())) {
            maybeLog("routeUpdated", routeInfo);
            synchronized (this) {
                zAddRoute = this.mLinkProperties.addRoute(routeInfo);
            }
            if (zAddRoute) {
                this.mCallback.update();
            }
        }
    }

    @Override
    public void routeRemoved(RouteInfo routeInfo) {
        boolean zRemoveRoute;
        if (this.mInterfaceName.equals(routeInfo.getInterface())) {
            maybeLog("routeRemoved", routeInfo);
            synchronized (this) {
                zRemoveRoute = this.mLinkProperties.removeRoute(routeInfo);
            }
            if (zRemoveRoute) {
                this.mCallback.update();
            }
        }
    }

    @Override
    public void interfaceDnsServerInfo(String str, long j, String[] strArr) {
        if (this.mInterfaceName.equals(str)) {
            maybeLog("interfaceDnsServerInfo", Arrays.toString(strArr));
            if (this.mDnsServerRepository.addServers(j, strArr)) {
                synchronized (this) {
                    this.mDnsServerRepository.setDnsServersOn(this.mLinkProperties);
                }
                this.mCallback.update();
            }
        }
    }

    public synchronized LinkProperties getLinkProperties() {
        return new LinkProperties(this.mLinkProperties);
    }

    public synchronized void clearLinkProperties() {
        this.mDnsServerRepository = new DnsServerRepository();
        this.mLinkProperties.clear();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
    }
}
