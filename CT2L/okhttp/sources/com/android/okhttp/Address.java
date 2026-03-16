package com.android.okhttp;

import com.android.okhttp.internal.Util;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.List;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

public final class Address {
    final OkAuthenticator authenticator;
    final HostnameVerifier hostnameVerifier;
    final List<Protocol> protocols;
    final Proxy proxy;
    final SocketFactory socketFactory;
    final SSLSocketFactory sslSocketFactory;
    final String uriHost;
    final int uriPort;

    public Address(String uriHost, int uriPort, SocketFactory socketFactory, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier, OkAuthenticator authenticator, Proxy proxy, List<Protocol> protocols) throws UnknownHostException {
        if (uriHost == null) {
            throw new NullPointerException("uriHost == null");
        }
        if (uriPort <= 0) {
            throw new IllegalArgumentException("uriPort <= 0: " + uriPort);
        }
        if (authenticator == null) {
            throw new IllegalArgumentException("authenticator == null");
        }
        if (protocols == null) {
            throw new IllegalArgumentException("protocols == null");
        }
        this.proxy = proxy;
        this.uriHost = uriHost;
        this.uriPort = uriPort;
        this.socketFactory = socketFactory;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
        this.authenticator = authenticator;
        this.protocols = Util.immutableList(protocols);
    }

    public String getUriHost() {
        return this.uriHost;
    }

    public int getUriPort() {
        return this.uriPort;
    }

    public SocketFactory getSocketFactory() {
        return this.socketFactory;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return this.sslSocketFactory;
    }

    public HostnameVerifier getHostnameVerifier() {
        return this.hostnameVerifier;
    }

    public OkAuthenticator getAuthenticator() {
        return this.authenticator;
    }

    public List<Protocol> getProtocols() {
        return this.protocols;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Address)) {
            return false;
        }
        Address that = (Address) other;
        return Util.equal(this.proxy, that.proxy) && this.uriHost.equals(that.uriHost) && this.uriPort == that.uriPort && Util.equal(this.sslSocketFactory, that.sslSocketFactory) && Util.equal(this.hostnameVerifier, that.hostnameVerifier) && Util.equal(this.authenticator, that.authenticator) && Util.equal(this.protocols, that.protocols);
    }

    public int hashCode() {
        int result = this.uriHost.hashCode() + 527;
        return (((((((((((result * 31) + this.uriPort) * 31) + (this.sslSocketFactory != null ? this.sslSocketFactory.hashCode() : 0)) * 31) + (this.hostnameVerifier != null ? this.hostnameVerifier.hashCode() : 0)) * 31) + (this.authenticator != null ? this.authenticator.hashCode() : 0)) * 31) + (this.proxy != null ? this.proxy.hashCode() : 0)) * 31) + this.protocols.hashCode();
    }
}
