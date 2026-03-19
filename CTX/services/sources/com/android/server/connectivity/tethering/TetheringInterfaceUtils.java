package com.android.server.connectivity.tethering;

import android.net.LinkProperties;
import android.net.NetworkState;
import android.net.RouteInfo;
import android.net.util.InterfaceSet;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

public final class TetheringInterfaceUtils {
    public static InterfaceSet getTetheringInterfaces(NetworkState networkState) {
        if (networkState == null) {
            return null;
        }
        String interfaceForDestination = getInterfaceForDestination(networkState.linkProperties, Inet4Address.ANY);
        String iPv6Interface = getIPv6Interface(networkState);
        if (interfaceForDestination == null && iPv6Interface == null) {
            return null;
        }
        return new InterfaceSet(interfaceForDestination, iPv6Interface);
    }

    public static String getIPv6Interface(NetworkState networkState) {
        boolean z = false;
        if (networkState != null && networkState.network != null && networkState.linkProperties != null && networkState.networkCapabilities != null && networkState.linkProperties.hasIPv6DnsServer() && networkState.linkProperties.hasGlobalIPv6Address() && networkState.networkCapabilities.hasTransport(0)) {
            z = true;
        }
        if (z) {
            return getInterfaceForDestination(networkState.linkProperties, Inet6Address.ANY);
        }
        return null;
    }

    private static String getInterfaceForDestination(LinkProperties linkProperties, InetAddress inetAddress) {
        RouteInfo routeInfoSelectBestRoute;
        if (linkProperties != null) {
            routeInfoSelectBestRoute = RouteInfo.selectBestRoute(linkProperties.getAllRoutes(), inetAddress);
        } else {
            routeInfoSelectBestRoute = null;
        }
        if (routeInfoSelectBestRoute != null) {
            return routeInfoSelectBestRoute.getInterface();
        }
        return null;
    }
}
