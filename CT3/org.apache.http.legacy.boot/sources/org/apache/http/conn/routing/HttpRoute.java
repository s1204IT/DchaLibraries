package org.apache.http.conn.routing;

import java.net.InetAddress;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.RouteInfo;

@Deprecated
public final class HttpRoute implements RouteInfo, Cloneable {
    private final RouteInfo.LayerType layered;
    private final InetAddress localAddress;
    private final HttpHost[] proxyChain;
    private final boolean secure;
    private final HttpHost targetHost;
    private final RouteInfo.TunnelType tunnelled;

    private HttpRoute(InetAddress local, HttpHost target, HttpHost[] proxies, boolean secure, RouteInfo.TunnelType tunnelled, RouteInfo.LayerType layered) {
        if (target == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }
        if (tunnelled == RouteInfo.TunnelType.TUNNELLED && proxies == null) {
            throw new IllegalArgumentException("Proxy required if tunnelled.");
        }
        tunnelled = tunnelled == null ? RouteInfo.TunnelType.PLAIN : tunnelled;
        layered = layered == null ? RouteInfo.LayerType.PLAIN : layered;
        this.targetHost = target;
        this.localAddress = local;
        this.proxyChain = proxies;
        this.secure = secure;
        this.tunnelled = tunnelled;
        this.layered = layered;
    }

    public HttpRoute(HttpHost target, InetAddress local, HttpHost[] proxies, boolean secure, RouteInfo.TunnelType tunnelled, RouteInfo.LayerType layered) {
        this(local, target, toChain(proxies), secure, tunnelled, layered);
    }

    public HttpRoute(HttpHost target, InetAddress local, HttpHost proxy, boolean secure, RouteInfo.TunnelType tunnelled, RouteInfo.LayerType layered) {
        this(local, target, toChain(proxy), secure, tunnelled, layered);
    }

    public HttpRoute(HttpHost target, InetAddress local, boolean secure) {
        this(local, target, (HttpHost[]) null, secure, RouteInfo.TunnelType.PLAIN, RouteInfo.LayerType.PLAIN);
    }

    public HttpRoute(HttpHost target) {
        this((InetAddress) null, target, (HttpHost[]) null, false, RouteInfo.TunnelType.PLAIN, RouteInfo.LayerType.PLAIN);
    }

    public HttpRoute(HttpHost target, InetAddress local, HttpHost proxy, boolean secure) {
        this(local, target, toChain(proxy), secure, secure ? RouteInfo.TunnelType.TUNNELLED : RouteInfo.TunnelType.PLAIN, secure ? RouteInfo.LayerType.LAYERED : RouteInfo.LayerType.PLAIN);
        if (proxy != null) {
        } else {
            throw new IllegalArgumentException("Proxy host may not be null.");
        }
    }

    private static HttpHost[] toChain(HttpHost proxy) {
        if (proxy == null) {
            return null;
        }
        return new HttpHost[]{proxy};
    }

    private static HttpHost[] toChain(HttpHost[] proxies) {
        if (proxies == null || proxies.length < 1) {
            return null;
        }
        for (HttpHost proxy : proxies) {
            if (proxy == null) {
                throw new IllegalArgumentException("Proxy chain may not contain null elements.");
            }
        }
        HttpHost[] result = new HttpHost[proxies.length];
        System.arraycopy(proxies, 0, result, 0, proxies.length);
        return result;
    }

    @Override
    public final HttpHost getTargetHost() {
        return this.targetHost;
    }

    @Override
    public final InetAddress getLocalAddress() {
        return this.localAddress;
    }

    @Override
    public final int getHopCount() {
        if (this.proxyChain == null) {
            return 1;
        }
        return this.proxyChain.length + 1;
    }

    @Override
    public final HttpHost getHopTarget(int hop) {
        if (hop < 0) {
            throw new IllegalArgumentException("Hop index must not be negative: " + hop);
        }
        int hopcount = getHopCount();
        if (hop >= hopcount) {
            throw new IllegalArgumentException("Hop index " + hop + " exceeds route length " + hopcount);
        }
        if (hop < hopcount - 1) {
            HttpHost result = this.proxyChain[hop];
            return result;
        }
        HttpHost result2 = this.targetHost;
        return result2;
    }

    @Override
    public final HttpHost getProxyHost() {
        if (this.proxyChain == null) {
            return null;
        }
        return this.proxyChain[0];
    }

    @Override
    public final RouteInfo.TunnelType getTunnelType() {
        return this.tunnelled;
    }

    @Override
    public final boolean isTunnelled() {
        return this.tunnelled == RouteInfo.TunnelType.TUNNELLED;
    }

    @Override
    public final RouteInfo.LayerType getLayerType() {
        return this.layered;
    }

    @Override
    public final boolean isLayered() {
        return this.layered == RouteInfo.LayerType.LAYERED;
    }

    @Override
    public final boolean isSecure() {
        return this.secure;
    }

    public final boolean equals(Object o) {
        boolean zEquals;
        boolean z;
        boolean z2 = true;
        if (o == this) {
            return true;
        }
        if (!(o instanceof HttpRoute)) {
            return false;
        }
        HttpRoute that = (HttpRoute) o;
        boolean equal = this.targetHost.equals(that.targetHost);
        if (this.localAddress != that.localAddress) {
            zEquals = this.localAddress != null ? this.localAddress.equals(that.localAddress) : false;
        } else {
            zEquals = true;
        }
        boolean equal2 = equal & zEquals;
        if (this.proxyChain != that.proxyChain) {
            z = (this.proxyChain == null || that.proxyChain == null || this.proxyChain.length != that.proxyChain.length) ? false : true;
        } else {
            z = true;
        }
        boolean equal3 = equal2 & z;
        if (this.secure != that.secure || this.tunnelled != that.tunnelled || this.layered != that.layered) {
            z2 = false;
        }
        boolean equal4 = equal3 & z2;
        if (equal4 && this.proxyChain != null) {
            for (int i = 0; equal4 && i < this.proxyChain.length; i++) {
                equal4 = this.proxyChain[i].equals(that.proxyChain[i]);
            }
        }
        return equal4;
    }

    public final int hashCode() {
        int hc = this.targetHost.hashCode();
        if (this.localAddress != null) {
            hc ^= this.localAddress.hashCode();
        }
        if (this.proxyChain != null) {
            hc ^= this.proxyChain.length;
            for (HttpHost aProxyChain : this.proxyChain) {
                hc ^= aProxyChain.hashCode();
            }
        }
        if (this.secure) {
            hc ^= 286331153;
        }
        return (hc ^ this.tunnelled.hashCode()) ^ this.layered.hashCode();
    }

    public final String toString() {
        StringBuilder cab = new StringBuilder((getHopCount() * 30) + 50);
        cab.append("HttpRoute[");
        if (this.localAddress != null) {
            cab.append(this.localAddress);
            cab.append("->");
        }
        cab.append('{');
        if (this.tunnelled == RouteInfo.TunnelType.TUNNELLED) {
            cab.append('t');
        }
        if (this.layered == RouteInfo.LayerType.LAYERED) {
            cab.append('l');
        }
        if (this.secure) {
            cab.append('s');
        }
        cab.append("}->");
        if (this.proxyChain != null) {
            for (HttpHost aProxyChain : this.proxyChain) {
                cab.append(aProxyChain);
                cab.append("->");
            }
        }
        cab.append(this.targetHost);
        cab.append(']');
        return cab.toString();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
