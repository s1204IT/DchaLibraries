package org.apache.http.conn.params;

import java.net.InetAddress;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.params.HttpParams;

@Deprecated
public class ConnRouteParams implements ConnRoutePNames {
    public static final HttpHost NO_HOST = new HttpHost("127.0.0.255", 0, "no-host");
    public static final HttpRoute NO_ROUTE = new HttpRoute(NO_HOST);

    private ConnRouteParams() {
    }

    public static HttpHost getDefaultProxy(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        HttpHost httpHost = (HttpHost) httpParams.getParameter(ConnRoutePNames.DEFAULT_PROXY);
        if (httpHost != null && NO_HOST.equals(httpHost)) {
            return null;
        }
        return httpHost;
    }

    public static void setDefaultProxy(HttpParams httpParams, HttpHost httpHost) {
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, httpHost);
    }

    public static HttpRoute getForcedRoute(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        HttpRoute httpRoute = (HttpRoute) httpParams.getParameter(ConnRoutePNames.FORCED_ROUTE);
        if (httpRoute != null && NO_ROUTE.equals(httpRoute)) {
            return null;
        }
        return httpRoute;
    }

    public static void setForcedRoute(HttpParams httpParams, HttpRoute httpRoute) {
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        httpParams.setParameter(ConnRoutePNames.FORCED_ROUTE, httpRoute);
    }

    public static InetAddress getLocalAddress(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        return (InetAddress) httpParams.getParameter(ConnRoutePNames.LOCAL_ADDRESS);
    }

    public static void setLocalAddress(HttpParams httpParams, InetAddress inetAddress) {
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        httpParams.setParameter(ConnRoutePNames.LOCAL_ADDRESS, inetAddress);
    }
}
