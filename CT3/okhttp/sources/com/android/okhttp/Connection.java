package com.android.okhttp;

import com.android.okhttp.HttpUrl;
import com.android.okhttp.Request;
import com.android.okhttp.internal.ConnectionSpecSelector;
import com.android.okhttp.internal.Platform;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.framed.FramedConnection;
import com.android.okhttp.internal.http.FramedTransport;
import com.android.okhttp.internal.http.HttpConnection;
import com.android.okhttp.internal.http.HttpEngine;
import com.android.okhttp.internal.http.HttpTransport;
import com.android.okhttp.internal.http.RouteException;
import com.android.okhttp.internal.http.Transport;
import com.android.okhttp.internal.tls.OkHostnameVerifier;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.BufferedSource;
import java.io.IOException;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownServiceException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class Connection {
    private FramedConnection framedConnection;
    private Handshake handshake;
    private HttpConnection httpConnection;
    private long idleStartTimeNs;
    private Object owner;
    private final ConnectionPool pool;
    private int recycleCount;
    private final Route route;
    private Socket socket;
    private boolean connected = false;
    private Protocol protocol = Protocol.HTTP_1_1;

    public Connection(ConnectionPool pool, Route route) {
        this.pool = pool;
        this.route = route;
    }

    Object getOwner() {
        Object obj;
        synchronized (this.pool) {
            obj = this.owner;
        }
        return obj;
    }

    void setOwner(Object owner) {
        if (isFramed()) {
            return;
        }
        synchronized (this.pool) {
            if (this.owner != null) {
                throw new IllegalStateException("Connection already has an owner!");
            }
            this.owner = owner;
        }
    }

    boolean clearOwner() {
        synchronized (this.pool) {
            if (this.owner == null) {
                return false;
            }
            this.owner = null;
            return true;
        }
    }

    void closeIfOwnedBy(Object owner) throws IOException {
        if (isFramed()) {
            throw new IllegalStateException();
        }
        synchronized (this.pool) {
            if (this.owner != owner) {
                System.out.println("Wrong owner. Perhaps a late disconnect");
                return;
            }
            this.owner = null;
            System.out.println("Close in OkHttp");
            try {
                try {
                    if (this.socket != null) {
                        this.socket.shutdownOutput();
                    }
                } catch (UnsupportedOperationException e) {
                }
            } catch (IOException e2) {
            }
            try {
                if (this.socket != null) {
                    this.socket.shutdownInput();
                }
            } catch (IOException e3) {
            }
            if (this.socket == null) {
                return;
            }
            this.socket.close();
        }
    }

    void connect(int connectTimeout, int readTimeout, int writeTimeout, Request request, List<ConnectionSpec> list, boolean connectionRetryEnabled) throws RouteException {
        Socket socketCreateSocket;
        if (this.connected) {
            throw new IllegalStateException("already connected");
        }
        RouteException routeException = null;
        ConnectionSpecSelector connectionSpecSelector = new ConnectionSpecSelector(list);
        Proxy proxy = this.route.getProxy();
        Address address = this.route.getAddress();
        if (this.route.address.getSslSocketFactory() == null && !list.contains(ConnectionSpec.CLEARTEXT)) {
            throw new RouteException(new UnknownServiceException("CLEARTEXT communication not supported: " + list));
        }
        while (!this.connected) {
            try {
                if (proxy.type() == Proxy.Type.DIRECT || proxy.type() == Proxy.Type.HTTP) {
                    socketCreateSocket = address.getSocketFactory().createSocket();
                } else {
                    socketCreateSocket = new Socket(proxy);
                }
                this.socket = socketCreateSocket;
                connectSocket(connectTimeout, readTimeout, writeTimeout, request, connectionSpecSelector);
                this.connected = true;
            } catch (IOException e) {
                Util.closeQuietly(this.socket);
                this.socket = null;
                if (routeException == null) {
                    routeException = new RouteException(e);
                } else {
                    routeException.addConnectException(e);
                }
                if (!connectionRetryEnabled) {
                    throw routeException;
                }
                if (!connectionSpecSelector.connectionFailed(e)) {
                    throw routeException;
                }
            }
        }
    }

    private void connectSocket(int connectTimeout, int readTimeout, int writeTimeout, Request request, ConnectionSpecSelector connectionSpecSelector) throws IOException {
        this.socket.setSoTimeout(readTimeout);
        Platform.get().connectSocket(this.socket, this.route.getSocketAddress(), connectTimeout);
        if (this.route.address.getSslSocketFactory() != null) {
            connectTls(readTimeout, writeTimeout, request, connectionSpecSelector);
        }
        if (this.protocol == Protocol.SPDY_3 || this.protocol == Protocol.HTTP_2) {
            this.socket.setSoTimeout(0);
            this.framedConnection = new FramedConnection.Builder(this.route.address.uriHost, true, this.socket).protocol(this.protocol).build();
            this.framedConnection.sendConnectionPreface();
            return;
        }
        this.httpConnection = new HttpConnection(this.pool, this, this.socket);
    }

    private void connectTls(int readTimeout, int writeTimeout, Request request, ConnectionSpecSelector connectionSpecSelector) throws IOException {
        if (this.route.requiresTunnel()) {
            createTunnel(readTimeout, writeTimeout, request);
        }
        Address address = this.route.getAddress();
        SSLSocketFactory sslSocketFactory = address.getSslSocketFactory();
        try {
            try {
                SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(this.socket, address.getRfc2732Host(), address.getUriPort(), true);
                ConnectionSpec connectionSpec = connectionSpecSelector.configureSecureSocket(sslSocket);
                if (connectionSpec.supportsTlsExtensions()) {
                    Platform.get().configureTlsExtensions(sslSocket, address.getRfc2732Host(), address.getProtocols());
                }
                sslSocket.startHandshake();
                Handshake unverifiedHandshake = Handshake.get(sslSocket.getSession());
                if (!address.getHostnameVerifier().verify(address.getRfc2732Host(), sslSocket.getSession())) {
                    X509Certificate cert = (X509Certificate) unverifiedHandshake.peerCertificates().get(0);
                    throw new SSLPeerUnverifiedException("Hostname " + address.getRfc2732Host() + " not verified:\n    certificate: " + CertificatePinner.pin(cert) + "\n    DN: " + cert.getSubjectDN().getName() + "\n    subjectAltNames: " + OkHostnameVerifier.allSubjectAltNames(cert));
                }
                address.getCertificatePinner().check(address.getRfc2732Host(), unverifiedHandshake.peerCertificates());
                String maybeProtocol = connectionSpec.supportsTlsExtensions() ? Platform.get().getSelectedProtocol(sslSocket) : null;
                this.protocol = maybeProtocol != null ? Protocol.get(maybeProtocol) : Protocol.HTTP_1_1;
                this.handshake = unverifiedHandshake;
                this.socket = sslSocket;
                if (sslSocket != null) {
                    Platform.get().afterHandshake(sslSocket);
                }
                if (1 == 0) {
                    Util.closeQuietly((Socket) sslSocket);
                }
            } catch (AssertionError e) {
                if (!Util.isAndroidGetsocknameError(e)) {
                    throw e;
                }
                throw new IOException(e);
            }
        } catch (Throwable th) {
            if (0 != 0) {
                Platform.get().afterHandshake(null);
            }
            if (0 == 0) {
                Util.closeQuietly((Socket) null);
            }
            throw th;
        }
    }

    private void createTunnel(int r13, int r14, com.android.okhttp.Request r15) throws java.io.IOException {
        throw new UnsupportedOperationException("Method not decompiled: com.android.okhttp.Connection.createTunnel(int, int, com.android.okhttp.Request):void");
    }

    private Request createTunnelRequest(Request request) throws IOException {
        HttpUrl tunnelUrl = new HttpUrl.Builder().scheme("https").host(request.httpUrl().host()).port(request.httpUrl().port()).build();
        Request.Builder result = new Request.Builder().url(tunnelUrl).header("Host", Util.hostHeader(tunnelUrl)).header("Proxy-Connection", "Keep-Alive");
        String userAgent = request.header("User-Agent");
        if (userAgent != null) {
            result.header("User-Agent", userAgent);
        }
        String proxyAuthorization = request.header("Proxy-Authorization");
        if (proxyAuthorization != null) {
            result.header("Proxy-Authorization", proxyAuthorization);
        }
        return result.build();
    }

    void connectAndSetOwner(OkHttpClient client, Object owner, Request request) throws RouteException {
        setOwner(owner);
        if (!isConnected()) {
            connect(client.getConnectTimeout(), client.getReadTimeout(), client.getWriteTimeout(), request, this.route.address.getConnectionSpecs(), client.getRetryOnConnectionFailure());
            if (isFramed()) {
                client.getConnectionPool().share(this);
            }
            client.routeDatabase().connected(getRoute());
        }
        setTimeouts(client.getReadTimeout(), client.getWriteTimeout());
    }

    boolean isConnected() {
        return this.connected;
    }

    public Route getRoute() {
        return this.route;
    }

    public Socket getSocket() {
        return this.socket;
    }

    BufferedSource rawSource() {
        if (this.httpConnection == null) {
            throw new UnsupportedOperationException();
        }
        return this.httpConnection.rawSource();
    }

    BufferedSink rawSink() {
        if (this.httpConnection == null) {
            throw new UnsupportedOperationException();
        }
        return this.httpConnection.rawSink();
    }

    boolean isAlive() {
        return (this.socket.isClosed() || this.socket.isInputShutdown() || this.socket.isOutputShutdown()) ? false : true;
    }

    boolean isReadable() {
        if (this.httpConnection != null) {
            return this.httpConnection.isReadable();
        }
        return true;
    }

    void resetIdleStartTime() {
        if (this.framedConnection != null) {
            throw new IllegalStateException("framedConnection != null");
        }
        this.idleStartTimeNs = System.nanoTime();
    }

    boolean isIdle() {
        if (this.framedConnection != null) {
            return this.framedConnection.isIdle();
        }
        return true;
    }

    long getIdleStartTimeNs() {
        return this.framedConnection == null ? this.idleStartTimeNs : this.framedConnection.getIdleStartTimeNs();
    }

    public Handshake getHandshake() {
        return this.handshake;
    }

    Transport newTransport(HttpEngine httpEngine) throws IOException {
        if (this.framedConnection != null) {
            return new FramedTransport(httpEngine, this.framedConnection);
        }
        return new HttpTransport(httpEngine, this.httpConnection);
    }

    boolean isFramed() {
        return this.framedConnection != null;
    }

    public Protocol getProtocol() {
        return this.protocol;
    }

    void setProtocol(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    void setTimeouts(int readTimeoutMillis, int writeTimeoutMillis) throws RouteException {
        if (!this.connected) {
            throw new IllegalStateException("setTimeouts - not connected");
        }
        if (this.httpConnection == null) {
            return;
        }
        try {
            this.socket.setSoTimeout(readTimeoutMillis);
            this.socket.setSoSndTimeout(writeTimeoutMillis);
            this.httpConnection.setTimeouts(readTimeoutMillis, writeTimeoutMillis);
        } catch (IOException e) {
            throw new RouteException(e);
        }
    }

    void incrementRecycleCount() {
        this.recycleCount++;
    }

    int recycleCount() {
        return this.recycleCount;
    }

    public String toString() {
        return "Connection{" + this.route.address.uriHost + ":" + this.route.address.uriPort + ", proxy=" + this.route.proxy + " hostAddress=" + this.route.inetSocketAddress.getAddress().getHostAddress() + " cipherSuite=" + (this.handshake != null ? this.handshake.cipherSuite() : "none") + " protocol=" + this.protocol + '}';
    }
}
