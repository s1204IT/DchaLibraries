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

    private HttpRoute(InetAddress inetAddress, HttpHost httpHost, HttpHost[] httpHostArr, boolean z, RouteInfo.TunnelType tunnelType, RouteInfo.LayerType layerType) {
        if (httpHost == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }
        if (tunnelType == RouteInfo.TunnelType.TUNNELLED && httpHostArr == null) {
            throw new IllegalArgumentException("Proxy required if tunnelled.");
        }
        tunnelType = tunnelType == null ? RouteInfo.TunnelType.PLAIN : tunnelType;
        layerType = layerType == null ? RouteInfo.LayerType.PLAIN : layerType;
        this.targetHost = httpHost;
        this.localAddress = inetAddress;
        this.proxyChain = httpHostArr;
        this.secure = z;
        this.tunnelled = tunnelType;
        this.layered = layerType;
    }

    public HttpRoute(HttpHost httpHost, InetAddress inetAddress, HttpHost[] httpHostArr, boolean z, RouteInfo.TunnelType tunnelType, RouteInfo.LayerType layerType) {
        this(inetAddress, httpHost, toChain(httpHostArr), z, tunnelType, layerType);
    }

    public HttpRoute(HttpHost httpHost, InetAddress inetAddress, HttpHost httpHost2, boolean z, RouteInfo.TunnelType tunnelType, RouteInfo.LayerType layerType) {
        this(inetAddress, httpHost, toChain(httpHost2), z, tunnelType, layerType);
    }

    public HttpRoute(HttpHost httpHost, InetAddress inetAddress, boolean z) {
        this(inetAddress, httpHost, (HttpHost[]) null, z, RouteInfo.TunnelType.PLAIN, RouteInfo.LayerType.PLAIN);
    }

    public HttpRoute(HttpHost httpHost) {
        this((InetAddress) null, httpHost, (HttpHost[]) null, false, RouteInfo.TunnelType.PLAIN, RouteInfo.LayerType.PLAIN);
    }

    public HttpRoute(HttpHost httpHost, InetAddress inetAddress, HttpHost httpHost2, boolean z) {
        this(inetAddress, httpHost, toChain(httpHost2), z, z ? RouteInfo.TunnelType.TUNNELLED : RouteInfo.TunnelType.PLAIN, z ? RouteInfo.LayerType.LAYERED : RouteInfo.LayerType.PLAIN);
        if (httpHost2 == null) {
            throw new IllegalArgumentException("Proxy host may not be null.");
        }
    }

    private static HttpHost[] toChain(HttpHost httpHost) {
        if (httpHost == null) {
            return null;
        }
        return new HttpHost[]{httpHost};
    }

    private static HttpHost[] toChain(HttpHost[] httpHostArr) {
        if (httpHostArr == null || httpHostArr.length < 1) {
            return null;
        }
        for (HttpHost httpHost : httpHostArr) {
            if (httpHost == null) {
                throw new IllegalArgumentException("Proxy chain may not contain null elements.");
            }
        }
        HttpHost[] httpHostArr2 = new HttpHost[httpHostArr.length];
        System.arraycopy(httpHostArr, 0, httpHostArr2, 0, httpHostArr.length);
        return httpHostArr2;
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
        return 1 + this.proxyChain.length;
    }

    @Override
    public final HttpHost getHopTarget(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Hop index must not be negative: " + i);
        }
        int hopCount = getHopCount();
        if (i >= hopCount) {
            throw new IllegalArgumentException("Hop index " + i + " exceeds route length " + hopCount);
        }
        if (i < hopCount - 1) {
            return this.proxyChain[i];
        }
        return this.targetHost;
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

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HttpRoute)) {
            return false;
        }
        HttpRoute httpRoute = (HttpRoute) obj;
        boolean zEquals = (this.secure == httpRoute.secure && this.tunnelled == httpRoute.tunnelled && this.layered == httpRoute.layered) & this.targetHost.equals(httpRoute.targetHost) & (this.localAddress == httpRoute.localAddress || (this.localAddress != null && this.localAddress.equals(httpRoute.localAddress))) & (this.proxyChain == httpRoute.proxyChain || !(this.proxyChain == null || httpRoute.proxyChain == null || this.proxyChain.length != httpRoute.proxyChain.length));
        if (zEquals && this.proxyChain != null) {
            for (int i = 0; zEquals && i < this.proxyChain.length; i++) {
                zEquals = this.proxyChain[i].equals(httpRoute.proxyChain[i]);
            }
        }
        return zEquals;
    }

    public final int hashCode() {
        int iHashCode = this.targetHost.hashCode();
        if (this.localAddress != null) {
            iHashCode ^= this.localAddress.hashCode();
        }
        if (this.proxyChain != null) {
            iHashCode ^= this.proxyChain.length;
            for (HttpHost httpHost : this.proxyChain) {
                iHashCode ^= httpHost.hashCode();
            }
        }
        if (this.secure) {
            iHashCode ^= 286331153;
        }
        return (iHashCode ^ this.tunnelled.hashCode()) ^ this.layered.hashCode();
    }

    public final String toString() {
        StringBuilder sb = new StringBuilder(50 + (getHopCount() * 30));
        sb.append("HttpRoute[");
        if (this.localAddress != null) {
            sb.append(this.localAddress);
            sb.append("->");
        }
        sb.append('{');
        if (this.tunnelled == RouteInfo.TunnelType.TUNNELLED) {
            sb.append('t');
        }
        if (this.layered == RouteInfo.LayerType.LAYERED) {
            sb.append('l');
        }
        if (this.secure) {
            sb.append('s');
        }
        sb.append("}->");
        if (this.proxyChain != null) {
            for (HttpHost httpHost : this.proxyChain) {
                sb.append(httpHost);
                sb.append("->");
            }
        }
        sb.append(this.targetHost);
        sb.append(']');
        return sb.toString();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
