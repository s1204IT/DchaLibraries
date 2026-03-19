package org.apache.http.impl.conn;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import org.apache.http.HttpHost;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

@Deprecated
public class DefaultClientConnectionOperator implements ClientConnectionOperator {
    private static final PlainSocketFactory staticPlainSocketFactory = new PlainSocketFactory();
    protected SchemeRegistry schemeRegistry;

    public DefaultClientConnectionOperator(SchemeRegistry schemeRegistry) {
        if (schemeRegistry == null) {
            throw new IllegalArgumentException("Scheme registry must not be null.");
        }
        this.schemeRegistry = schemeRegistry;
    }

    @Override
    public OperatedClientConnection createConnection() {
        return new DefaultClientConnection();
    }

    @Override
    public void openConnection(OperatedClientConnection operatedClientConnection, HttpHost httpHost, InetAddress inetAddress, HttpContext httpContext, HttpParams httpParams) throws IOException {
        LayeredSocketFactory layeredSocketFactory;
        SocketFactory socketFactory;
        int i;
        InetAddress[] inetAddressArr;
        SocketFactory socketFactory2;
        int i2;
        int i3;
        InetAddress[] inetAddressArr2;
        ConnectException connectException;
        if (operatedClientConnection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        if (httpHost == null) {
            throw new IllegalArgumentException("Target host must not be null.");
        }
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        if (operatedClientConnection.isOpen()) {
            throw new IllegalArgumentException("Connection must not be open.");
        }
        Scheme scheme = this.schemeRegistry.getScheme(httpHost.getSchemeName());
        SocketFactory socketFactory3 = scheme.getSocketFactory();
        if (socketFactory3 instanceof LayeredSocketFactory) {
            socketFactory = staticPlainSocketFactory;
            layeredSocketFactory = (LayeredSocketFactory) socketFactory3;
        } else {
            layeredSocketFactory = null;
            socketFactory = socketFactory3;
        }
        InetAddress[] allByName = InetAddress.getAllByName(httpHost.getHostName());
        int i4 = 0;
        while (i4 < allByName.length) {
            Socket socketCreateSocket = socketFactory.createSocket();
            operatedClientConnection.opening(socketCreateSocket, httpHost);
            try {
                try {
                    try {
                        SocketFactory socketFactory4 = socketFactory;
                        socketFactory2 = socketFactory;
                        i2 = 1;
                        i3 = i4;
                        inetAddressArr2 = allByName;
                        try {
                            Socket socketConnectSocket = socketFactory4.connectSocket(socketCreateSocket, allByName[i4].getHostAddress(), scheme.resolvePort(httpHost.getPort()), inetAddress, 0, httpParams);
                            if (socketCreateSocket != socketConnectSocket) {
                                operatedClientConnection.opening(socketConnectSocket, httpHost);
                                socketCreateSocket = socketConnectSocket;
                            }
                        } catch (SocketException e) {
                            e = e;
                        } catch (ConnectTimeoutException e2) {
                            e = e2;
                        }
                    } catch (SocketException e3) {
                        e = e3;
                        socketFactory2 = socketFactory;
                        i2 = 1;
                        i = i4;
                        inetAddressArr = allByName;
                    }
                } catch (ConnectTimeoutException e4) {
                    e = e4;
                    i3 = i4;
                    inetAddressArr2 = allByName;
                    socketFactory2 = socketFactory;
                    i2 = 1;
                }
            } catch (SocketException e5) {
                e = e5;
                i = i4;
                inetAddressArr = allByName;
                socketFactory2 = socketFactory;
                i2 = 1;
            }
            try {
                prepareSocket(socketCreateSocket, httpContext, httpParams);
                if (layeredSocketFactory == null) {
                    operatedClientConnection.openCompleted(socketFactory3.isSecure(socketCreateSocket), httpParams);
                    return;
                }
                Socket socketCreateSocket2 = layeredSocketFactory.createSocket(socketCreateSocket, httpHost.getHostName(), scheme.resolvePort(httpHost.getPort()), true);
                if (socketCreateSocket2 != socketCreateSocket) {
                    operatedClientConnection.opening(socketCreateSocket2, httpHost);
                }
                operatedClientConnection.openCompleted(socketFactory3.isSecure(socketCreateSocket2), httpParams);
                return;
            } catch (SocketException e6) {
                e = e6;
                i = i3;
                inetAddressArr = inetAddressArr2;
                if (i != inetAddressArr.length - i2) {
                    i4 = i + 1;
                    allByName = inetAddressArr;
                    socketFactory = socketFactory2;
                } else {
                    if (e instanceof ConnectException) {
                        connectException = (ConnectException) e;
                    } else {
                        ConnectException connectException2 = new ConnectException(e.getMessage());
                        connectException2.initCause(e);
                        connectException = connectException2;
                    }
                    throw new HttpHostConnectException(httpHost, connectException);
                }
            } catch (ConnectTimeoutException e7) {
                e = e7;
                inetAddressArr = inetAddressArr2;
                i = i3;
                if (i != inetAddressArr.length - i2) {
                    i4 = i + 1;
                    allByName = inetAddressArr;
                    socketFactory = socketFactory2;
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void updateSecureConnection(OperatedClientConnection operatedClientConnection, HttpHost httpHost, HttpContext httpContext, HttpParams httpParams) throws IOException {
        if (operatedClientConnection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        if (httpHost == null) {
            throw new IllegalArgumentException("Target host must not be null.");
        }
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        if (!operatedClientConnection.isOpen()) {
            throw new IllegalArgumentException("Connection must be open.");
        }
        Scheme scheme = this.schemeRegistry.getScheme(httpHost.getSchemeName());
        if (!(scheme.getSocketFactory() instanceof LayeredSocketFactory)) {
            throw new IllegalArgumentException("Target scheme (" + scheme.getName() + ") must have layered socket factory.");
        }
        LayeredSocketFactory layeredSocketFactory = (LayeredSocketFactory) scheme.getSocketFactory();
        try {
            Socket socketCreateSocket = layeredSocketFactory.createSocket(operatedClientConnection.getSocket(), httpHost.getHostName(), scheme.resolvePort(httpHost.getPort()), true);
            prepareSocket(socketCreateSocket, httpContext, httpParams);
            operatedClientConnection.update(socketCreateSocket, httpHost, layeredSocketFactory.isSecure(socketCreateSocket), httpParams);
        } catch (ConnectException e) {
            throw new HttpHostConnectException(httpHost, e);
        }
    }

    protected void prepareSocket(Socket socket, HttpContext httpContext, HttpParams httpParams) throws IOException {
        socket.setTcpNoDelay(HttpConnectionParams.getTcpNoDelay(httpParams));
        socket.setSoTimeout(HttpConnectionParams.getSoTimeout(httpParams));
        int linger = HttpConnectionParams.getLinger(httpParams);
        if (linger >= 0) {
            socket.setSoLinger(linger > 0, linger);
        }
    }
}
