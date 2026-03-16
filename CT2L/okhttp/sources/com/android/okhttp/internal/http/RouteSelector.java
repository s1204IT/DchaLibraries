package com.android.okhttp.internal.http;

import com.android.okhttp.Address;
import com.android.okhttp.Connection;
import com.android.okhttp.ConnectionPool;
import com.android.okhttp.HostResolver;
import com.android.okhttp.Route;
import com.android.okhttp.RouteDatabase;
import com.android.okhttp.internal.Util;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;

public final class RouteSelector {
    private static final int TLS_MODE_COMPATIBLE = 0;
    private static final int TLS_MODE_MODERN = 1;
    private static final int TLS_MODE_NULL = -1;
    private final Address address;
    private boolean hasNextProxy;
    private final HostResolver hostResolver;
    private InetSocketAddress lastInetSocketAddress;
    private Proxy lastProxy;
    private int nextSocketAddressIndex;
    private final ConnectionPool pool;
    private final ProxySelector proxySelector;
    private Iterator<Proxy> proxySelectorProxies;
    private final RouteDatabase routeDatabase;
    private InetAddress[] socketAddresses;
    private int socketPort;
    private final URI uri;
    private Proxy userSpecifiedProxy;
    private int nextTlsMode = TLS_MODE_NULL;
    private final List<Route> postponedRoutes = new LinkedList();

    public RouteSelector(Address address, URI uri, ProxySelector proxySelector, ConnectionPool pool, HostResolver hostResolver, RouteDatabase routeDatabase) {
        this.address = address;
        this.uri = uri;
        this.proxySelector = proxySelector;
        this.pool = pool;
        this.hostResolver = hostResolver;
        this.routeDatabase = routeDatabase;
        resetNextProxy(uri, address.getProxy());
    }

    public boolean hasNext() {
        return hasNextTlsMode() || hasNextInetSocketAddress() || hasNextProxy() || hasNextPostponed();
    }

    public Connection next(String method) throws IOException {
        while (true) {
            Connection pooled = this.pool.get(this.address);
            if (pooled != null) {
                if (!method.equals("GET") && !pooled.isReadable()) {
                    pooled.close();
                } else {
                    return pooled;
                }
            } else {
                if (!hasNextTlsMode()) {
                    if (!hasNextInetSocketAddress()) {
                        if (!hasNextProxy()) {
                            if (!hasNextPostponed()) {
                                throw new NoSuchElementException();
                            }
                            return new Connection(this.pool, nextPostponed());
                        }
                        this.lastProxy = nextProxy();
                        resetNextInetSocketAddress(this.lastProxy);
                    }
                    this.lastInetSocketAddress = nextInetSocketAddress();
                    resetNextTlsMode();
                }
                boolean modernTls = nextTlsMode() == TLS_MODE_MODERN;
                Route route = new Route(this.address, this.lastProxy, this.lastInetSocketAddress, modernTls);
                if (this.routeDatabase.shouldPostpone(route)) {
                    this.postponedRoutes.add(route);
                    return next(method);
                }
                return new Connection(this.pool, route);
            }
        }
    }

    public void connectFailed(Connection connection, IOException failure) {
        if (connection.recycleCount() <= 0) {
            Route failedRoute = connection.getRoute();
            if (failedRoute.getProxy().type() != Proxy.Type.DIRECT && this.proxySelector != null) {
                this.proxySelector.connectFailed(this.uri, failedRoute.getProxy().address(), failure);
            }
            this.routeDatabase.failed(failedRoute);
            if (hasNextTlsMode() && !(failure instanceof SSLHandshakeException) && !(failure instanceof SSLProtocolException)) {
                boolean modernTls = nextTlsMode() == TLS_MODE_MODERN;
                Route routeToSuppress = new Route(this.address, this.lastProxy, this.lastInetSocketAddress, modernTls);
                this.routeDatabase.failed(routeToSuppress);
            }
        }
    }

    private void resetNextProxy(URI uri, Proxy proxy) {
        this.hasNextProxy = true;
        if (proxy != null) {
            this.userSpecifiedProxy = proxy;
            return;
        }
        List<Proxy> proxyList = this.proxySelector.select(uri);
        if (proxyList != null) {
            this.proxySelectorProxies = proxyList.iterator();
        }
    }

    private boolean hasNextProxy() {
        return this.hasNextProxy;
    }

    private Proxy nextProxy() {
        if (this.userSpecifiedProxy != null) {
            this.hasNextProxy = false;
            return this.userSpecifiedProxy;
        }
        if (this.proxySelectorProxies != null) {
            while (this.proxySelectorProxies.hasNext()) {
                Proxy candidate = this.proxySelectorProxies.next();
                if (candidate.type() != Proxy.Type.DIRECT) {
                    return candidate;
                }
            }
        }
        this.hasNextProxy = false;
        return Proxy.NO_PROXY;
    }

    private void resetNextInetSocketAddress(Proxy proxy) throws UnknownHostException {
        String socketHost;
        this.socketAddresses = null;
        if (proxy.type() == Proxy.Type.DIRECT) {
            socketHost = this.address.getUriHost();
            this.socketPort = Util.getEffectivePort(this.uri);
        } else {
            SocketAddress proxyAddress = proxy.address();
            if (!(proxyAddress instanceof InetSocketAddress)) {
                throw new IllegalArgumentException("Proxy.address() is not an InetSocketAddress: " + proxyAddress.getClass());
            }
            InetSocketAddress proxySocketAddress = (InetSocketAddress) proxyAddress;
            socketHost = getHostString(proxySocketAddress);
            this.socketPort = proxySocketAddress.getPort();
        }
        this.socketAddresses = this.hostResolver.getAllByName(socketHost);
        this.nextSocketAddressIndex = TLS_MODE_COMPATIBLE;
    }

    static String getHostString(InetSocketAddress socketAddress) {
        InetAddress address = socketAddress.getAddress();
        return address == null ? socketAddress.getHostName() : address.getHostAddress();
    }

    private boolean hasNextInetSocketAddress() {
        return this.socketAddresses != null;
    }

    private InetSocketAddress nextInetSocketAddress() throws UnknownHostException {
        InetAddress[] inetAddressArr = this.socketAddresses;
        int i = this.nextSocketAddressIndex;
        this.nextSocketAddressIndex = i + TLS_MODE_MODERN;
        InetSocketAddress result = new InetSocketAddress(inetAddressArr[i], this.socketPort);
        if (this.nextSocketAddressIndex == this.socketAddresses.length) {
            this.socketAddresses = null;
            this.nextSocketAddressIndex = TLS_MODE_COMPATIBLE;
        }
        return result;
    }

    private void resetNextTlsMode() {
        this.nextTlsMode = this.address.getSslSocketFactory() != null ? TLS_MODE_MODERN : TLS_MODE_COMPATIBLE;
    }

    private boolean hasNextTlsMode() {
        return this.nextTlsMode != TLS_MODE_NULL;
    }

    private int nextTlsMode() {
        if (this.nextTlsMode == TLS_MODE_MODERN) {
            this.nextTlsMode = TLS_MODE_COMPATIBLE;
            return TLS_MODE_MODERN;
        }
        if (this.nextTlsMode == 0) {
            this.nextTlsMode = TLS_MODE_NULL;
            return TLS_MODE_COMPATIBLE;
        }
        throw new AssertionError();
    }

    private boolean hasNextPostponed() {
        return !this.postponedRoutes.isEmpty();
    }

    private Route nextPostponed() {
        return this.postponedRoutes.remove(TLS_MODE_COMPATIBLE);
    }
}
