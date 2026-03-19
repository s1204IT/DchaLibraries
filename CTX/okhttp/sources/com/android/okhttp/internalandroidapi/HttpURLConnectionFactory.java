package com.android.okhttp.internalandroidapi;

import com.android.okhttp.ConnectionPool;
import com.android.okhttp.HttpHandler;
import com.android.okhttp.HttpsHandler;
import com.android.okhttp.OkHttpClient;
import com.android.okhttp.OkUrlFactories;
import com.android.okhttp.OkUrlFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;

public final class HttpURLConnectionFactory {
    private ConnectionPool connectionPool;
    private com.android.okhttp.Dns dns;

    public void setNewConnectionPool(int i, long j, TimeUnit timeUnit) {
        this.connectionPool = new ConnectionPool(i, j, timeUnit);
    }

    public void setDns(Dns dns) {
        Objects.requireNonNull(dns);
        this.dns = new DnsAdapter(dns);
    }

    public URLConnection openConnection(URL url) throws IOException {
        return internalOpenConnection(url, null, null);
    }

    public URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        Objects.requireNonNull(proxy);
        return internalOpenConnection(url, null, proxy);
    }

    public URLConnection openConnection(URL url, SocketFactory socketFactory) throws IOException {
        Objects.requireNonNull(socketFactory);
        return internalOpenConnection(url, socketFactory, null);
    }

    public URLConnection openConnection(URL url, SocketFactory socketFactory, Proxy proxy) throws IOException {
        Objects.requireNonNull(socketFactory);
        Objects.requireNonNull(proxy);
        return internalOpenConnection(url, socketFactory, proxy);
    }

    private URLConnection internalOpenConnection(URL url, SocketFactory socketFactory, Proxy proxy) throws IOException {
        OkUrlFactory okUrlFactoryCreateHttpsOkUrlFactory;
        String protocol = url.getProtocol();
        if (protocol.equals("http")) {
            okUrlFactoryCreateHttpsOkUrlFactory = HttpHandler.createHttpOkUrlFactory(proxy);
        } else if (protocol.equals("https")) {
            okUrlFactoryCreateHttpsOkUrlFactory = HttpsHandler.createHttpsOkUrlFactory(proxy);
        } else {
            throw new MalformedURLException("Invalid URL or unrecognized protocol " + protocol);
        }
        OkHttpClient okHttpClientClient = okUrlFactoryCreateHttpsOkUrlFactory.client();
        if (this.connectionPool != null) {
            okHttpClientClient.setConnectionPool(this.connectionPool);
        }
        if (this.dns != null) {
            okHttpClientClient.setDns(this.dns);
        }
        if (socketFactory != null) {
            okHttpClientClient.setSocketFactory(socketFactory);
        }
        if (proxy == null) {
            return okUrlFactoryCreateHttpsOkUrlFactory.open(url);
        }
        return OkUrlFactories.open(okUrlFactoryCreateHttpsOkUrlFactory, url, proxy);
    }

    static final class DnsAdapter implements com.android.okhttp.Dns {
        private final Dns adaptee;

        DnsAdapter(Dns dns) {
            this.adaptee = (Dns) Objects.requireNonNull(dns);
        }

        @Override
        public List<InetAddress> lookup(String str) throws UnknownHostException {
            return this.adaptee.lookup(str);
        }

        public int hashCode() {
            return (31 * DnsAdapter.class.hashCode()) + this.adaptee.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof DnsAdapter)) {
                return false;
            }
            return this.adaptee.equals(((DnsAdapter) obj).adaptee);
        }

        public String toString() {
            return this.adaptee.toString();
        }
    }
}
