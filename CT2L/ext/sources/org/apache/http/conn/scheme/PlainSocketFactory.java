package org.apache.http.conn.scheme;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

@Deprecated
public final class PlainSocketFactory implements SocketFactory {
    private static final PlainSocketFactory DEFAULT_FACTORY = new PlainSocketFactory();
    private final HostNameResolver nameResolver;

    public static PlainSocketFactory getSocketFactory() {
        return DEFAULT_FACTORY;
    }

    public PlainSocketFactory(HostNameResolver nameResolver) {
        this.nameResolver = nameResolver;
    }

    public PlainSocketFactory() {
        this(null);
    }

    @Override
    public Socket createSocket() {
        return new Socket();
    }

    @Override
    public Socket connectSocket(Socket sock, String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException {
        InetSocketAddress remoteAddress;
        if (host == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null.");
        }
        if (sock == null) {
            sock = createSocket();
        }
        if (localAddress != null || localPort > 0) {
            if (localPort < 0) {
                localPort = 0;
            }
            InetSocketAddress isa = new InetSocketAddress(localAddress, localPort);
            sock.bind(isa);
        }
        int timeout = HttpConnectionParams.getConnectionTimeout(params);
        if (this.nameResolver != null) {
            remoteAddress = new InetSocketAddress(this.nameResolver.resolve(host), port);
        } else {
            remoteAddress = new InetSocketAddress(host, port);
        }
        try {
            sock.connect(remoteAddress, timeout);
            return sock;
        } catch (SocketTimeoutException e) {
            throw new ConnectTimeoutException("Connect to " + remoteAddress + " timed out");
        }
    }

    @Override
    public final boolean isSecure(Socket sock) throws IllegalArgumentException {
        if (sock == null) {
            throw new IllegalArgumentException("Socket may not be null.");
        }
        if (sock.getClass() != Socket.class) {
            throw new IllegalArgumentException("Socket not created by this factory.");
        }
        if (sock.isClosed()) {
            throw new IllegalArgumentException("Socket is closed.");
        }
        return false;
    }

    public boolean equals(Object obj) {
        return obj == this;
    }

    public int hashCode() {
        return PlainSocketFactory.class.hashCode();
    }
}
