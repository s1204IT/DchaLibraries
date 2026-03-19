package com.android.okhttp;

import java.net.InetSocketAddress;
import java.net.Proxy;

public final class Route {
    final Address address;
    final InetSocketAddress inetSocketAddress;
    final Proxy proxy;

    public Route(Address address, Proxy proxy, InetSocketAddress inetSocketAddress) {
        if (address == null) {
            throw new NullPointerException("address == null");
        }
        if (proxy == null) {
            throw new NullPointerException("proxy == null");
        }
        if (inetSocketAddress == null) {
            throw new NullPointerException("inetSocketAddress == null");
        }
        this.address = address;
        this.proxy = proxy;
        this.inetSocketAddress = inetSocketAddress;
    }

    public Address getAddress() {
        return this.address;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public InetSocketAddress getSocketAddress() {
        return this.inetSocketAddress;
    }

    public boolean requiresTunnel() {
        return this.address.sslSocketFactory != null && this.proxy.type() == Proxy.Type.HTTP;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Route)) {
            return false;
        }
        Route other = (Route) obj;
        if (this.address.equals(other.address) && this.proxy.equals(other.proxy)) {
            return this.inetSocketAddress.equals(other.inetSocketAddress);
        }
        return false;
    }

    public int hashCode() {
        int result = this.address.hashCode() + 527;
        return (((result * 31) + this.proxy.hashCode()) * 31) + this.inetSocketAddress.hashCode();
    }
}
