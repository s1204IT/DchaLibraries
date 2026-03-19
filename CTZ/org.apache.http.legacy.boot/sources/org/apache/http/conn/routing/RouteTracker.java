package org.apache.http.conn.routing;

import java.net.InetAddress;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.RouteInfo;

@Deprecated
public final class RouteTracker implements RouteInfo, Cloneable {
    private boolean connected;
    private RouteInfo.LayerType layered;
    private final InetAddress localAddress;
    private HttpHost[] proxyChain;
    private boolean secure;
    private final HttpHost targetHost;
    private RouteInfo.TunnelType tunnelled;

    public RouteTracker(HttpHost httpHost, InetAddress inetAddress) {
        if (httpHost == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }
        this.targetHost = httpHost;
        this.localAddress = inetAddress;
        this.tunnelled = RouteInfo.TunnelType.PLAIN;
        this.layered = RouteInfo.LayerType.PLAIN;
    }

    public RouteTracker(HttpRoute httpRoute) {
        this(httpRoute.getTargetHost(), httpRoute.getLocalAddress());
    }

    public final void connectTarget(boolean z) {
        if (this.connected) {
            throw new IllegalStateException("Already connected.");
        }
        this.connected = true;
        this.secure = z;
    }

    public final void connectProxy(HttpHost httpHost, boolean z) {
        if (httpHost == null) {
            throw new IllegalArgumentException("Proxy host may not be null.");
        }
        if (this.connected) {
            throw new IllegalStateException("Already connected.");
        }
        this.connected = true;
        this.proxyChain = new HttpHost[]{httpHost};
        this.secure = z;
    }

    public final void tunnelTarget(boolean z) {
        if (!this.connected) {
            throw new IllegalStateException("No tunnel unless connected.");
        }
        if (this.proxyChain == null) {
            throw new IllegalStateException("No tunnel without proxy.");
        }
        this.tunnelled = RouteInfo.TunnelType.TUNNELLED;
        this.secure = z;
    }

    public final void tunnelProxy(HttpHost httpHost, boolean z) {
        if (httpHost == null) {
            throw new IllegalArgumentException("Proxy host may not be null.");
        }
        if (!this.connected) {
            throw new IllegalStateException("No tunnel unless connected.");
        }
        if (this.proxyChain == null) {
            throw new IllegalStateException("No proxy tunnel without proxy.");
        }
        HttpHost[] httpHostArr = new HttpHost[this.proxyChain.length + 1];
        System.arraycopy(this.proxyChain, 0, httpHostArr, 0, this.proxyChain.length);
        httpHostArr[httpHostArr.length - 1] = httpHost;
        this.proxyChain = httpHostArr;
        this.secure = z;
    }

    public final void layerProtocol(boolean z) {
        if (!this.connected) {
            throw new IllegalStateException("No layered protocol unless connected.");
        }
        this.layered = RouteInfo.LayerType.LAYERED;
        this.secure = z;
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
        if (this.connected) {
            if (this.proxyChain == null) {
                return 1;
            }
            return 1 + this.proxyChain.length;
        }
        return 0;
    }

    @Override
    public final HttpHost getHopTarget(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Hop index must not be negative: " + i);
        }
        int hopCount = getHopCount();
        if (i >= hopCount) {
            throw new IllegalArgumentException("Hop index " + i + " exceeds tracked route length " + hopCount + ".");
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

    public final boolean isConnected() {
        return this.connected;
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

    public final HttpRoute toRoute() {
        if (this.connected) {
            return new HttpRoute(this.targetHost, this.localAddress, this.proxyChain, this.secure, this.tunnelled, this.layered);
        }
        return null;
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RouteTracker)) {
            return false;
        }
        RouteTracker routeTracker = (RouteTracker) obj;
        boolean zEquals = (this.connected == routeTracker.connected && this.secure == routeTracker.secure && this.tunnelled == routeTracker.tunnelled && this.layered == routeTracker.layered) & this.targetHost.equals(routeTracker.targetHost) & (this.localAddress == routeTracker.localAddress || (this.localAddress != null && this.localAddress.equals(routeTracker.localAddress))) & (this.proxyChain == routeTracker.proxyChain || !(this.proxyChain == null || routeTracker.proxyChain == null || this.proxyChain.length != routeTracker.proxyChain.length));
        if (zEquals && this.proxyChain != null) {
            for (int i = 0; zEquals && i < this.proxyChain.length; i++) {
                zEquals = this.proxyChain[i].equals(routeTracker.proxyChain[i]);
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
            for (int i = 0; i < this.proxyChain.length; i++) {
                iHashCode ^= this.proxyChain[i].hashCode();
            }
        }
        if (this.connected) {
            iHashCode ^= 286331153;
        }
        if (this.secure) {
            iHashCode ^= 572662306;
        }
        return (iHashCode ^ this.tunnelled.hashCode()) ^ this.layered.hashCode();
    }

    public final String toString() {
        StringBuilder sb = new StringBuilder(50 + (getHopCount() * 30));
        sb.append("RouteTracker[");
        if (this.localAddress != null) {
            sb.append(this.localAddress);
            sb.append("->");
        }
        sb.append('{');
        if (this.connected) {
            sb.append('c');
        }
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
            for (int i = 0; i < this.proxyChain.length; i++) {
                sb.append(this.proxyChain[i]);
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
