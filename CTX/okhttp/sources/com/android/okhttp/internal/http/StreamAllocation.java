package com.android.okhttp.internal.http;

import com.android.okhttp.Address;
import com.android.okhttp.ConnectionPool;
import com.android.okhttp.internal.Internal;
import com.android.okhttp.internal.RouteDatabase;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.io.RealConnection;
import com.android.okhttp.okio.Sink;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

public final class StreamAllocation {
    public final Address address;
    private boolean canceled;
    private RealConnection connection;
    private final ConnectionPool connectionPool;
    private boolean released;
    private RouteSelector routeSelector;
    private HttpStream stream;

    public StreamAllocation(ConnectionPool connectionPool, Address address) {
        this.connectionPool = connectionPool;
        this.address = address;
    }

    public HttpStream newStream(int i, int i2, int i3, boolean z, boolean z2) throws RouteException, IOException {
        HttpStream http1xStream;
        try {
            RealConnection realConnectionFindHealthyConnection = findHealthyConnection(i, i2, i3, z, z2);
            if (realConnectionFindHealthyConnection.framedConnection != null) {
                http1xStream = new Http2xStream(this, realConnectionFindHealthyConnection.framedConnection);
            } else {
                realConnectionFindHealthyConnection.getSocket().setSoTimeout(i2);
                realConnectionFindHealthyConnection.source.timeout().timeout(i2, TimeUnit.MILLISECONDS);
                realConnectionFindHealthyConnection.sink.timeout().timeout(i3, TimeUnit.MILLISECONDS);
                http1xStream = new Http1xStream(this, realConnectionFindHealthyConnection.source, realConnectionFindHealthyConnection.sink);
            }
            synchronized (this.connectionPool) {
                realConnectionFindHealthyConnection.streamCount++;
                this.stream = http1xStream;
            }
            return http1xStream;
        } catch (IOException e) {
            throw new RouteException(e);
        }
    }

    private RealConnection findHealthyConnection(int i, int i2, int i3, boolean z, boolean z2) throws RouteException, IOException {
        while (true) {
            RealConnection realConnectionFindConnection = findConnection(i, i2, i3, z);
            synchronized (this.connectionPool) {
                if (realConnectionFindConnection.streamCount == 0) {
                    return realConnectionFindConnection;
                }
                if (realConnectionFindConnection.isHealthy(z2)) {
                    return realConnectionFindConnection;
                }
                connectionFailed();
            }
        }
    }

    private RealConnection findConnection(int i, int i2, int i3, boolean z) throws RouteException, IOException {
        synchronized (this.connectionPool) {
            if (this.released) {
                throw new IllegalStateException("released");
            }
            if (this.stream != null) {
                throw new IllegalStateException("stream != null");
            }
            if (this.canceled) {
                throw new IOException("Canceled");
            }
            RealConnection realConnection = this.connection;
            if (realConnection != null && !realConnection.noNewStreams) {
                return realConnection;
            }
            RealConnection realConnection2 = Internal.instance.get(this.connectionPool, this.address, this);
            if (realConnection2 != null) {
                this.connection = realConnection2;
                return realConnection2;
            }
            if (this.routeSelector == null) {
                this.routeSelector = new RouteSelector(this.address, routeDatabase());
            }
            RealConnection realConnection3 = new RealConnection(this.routeSelector.next());
            acquire(realConnection3);
            synchronized (this.connectionPool) {
                Internal.instance.put(this.connectionPool, realConnection3);
                this.connection = realConnection3;
                if (this.canceled) {
                    throw new IOException("Canceled");
                }
            }
            realConnection3.connect(i, i2, i3, this.address.getConnectionSpecs(), z);
            routeDatabase().connected(realConnection3.getRoute());
            return realConnection3;
        }
    }

    public void streamFinished(HttpStream httpStream) {
        synchronized (this.connectionPool) {
            if (httpStream != null) {
                if (httpStream == this.stream) {
                }
            }
            throw new IllegalStateException("expected " + this.stream + " but was " + httpStream);
        }
        deallocate(false, false, true);
    }

    public HttpStream stream() {
        HttpStream httpStream;
        synchronized (this.connectionPool) {
            httpStream = this.stream;
        }
        return httpStream;
    }

    private RouteDatabase routeDatabase() {
        return Internal.instance.routeDatabase(this.connectionPool);
    }

    public synchronized RealConnection connection() {
        return this.connection;
    }

    public void release() {
        deallocate(false, true, false);
    }

    public void noNewStreams() {
        deallocate(true, false, false);
    }

    private void deallocate(boolean z, boolean z2, boolean z3) {
        RealConnection realConnection;
        synchronized (this.connectionPool) {
            if (z3) {
                try {
                    this.stream = null;
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (z2) {
                this.released = true;
            }
            if (this.connection != null) {
                if (z) {
                    this.connection.noNewStreams = true;
                }
                if (this.stream == null && (this.released || this.connection.noNewStreams)) {
                    release(this.connection);
                    if (this.connection.streamCount > 0) {
                        this.routeSelector = null;
                    }
                    if (this.connection.allocations.isEmpty()) {
                        this.connection.idleAtNanos = System.nanoTime();
                        if (Internal.instance.connectionBecameIdle(this.connectionPool, this.connection)) {
                            realConnection = this.connection;
                        } else {
                            realConnection = null;
                        }
                        this.connection = null;
                    }
                }
            } else {
                realConnection = null;
            }
        }
        if (realConnection != null) {
            Util.closeQuietly(realConnection.getSocket());
        }
    }

    public void cancel() {
        HttpStream httpStream;
        RealConnection realConnection;
        synchronized (this.connectionPool) {
            this.canceled = true;
            httpStream = this.stream;
            realConnection = this.connection;
        }
        if (httpStream != null) {
            httpStream.cancel();
        } else if (realConnection != null) {
            realConnection.cancel();
        }
    }

    private void connectionFailed(IOException iOException) {
        synchronized (this.connectionPool) {
            if (this.routeSelector != null) {
                if (this.connection.streamCount == 0) {
                    this.routeSelector.connectFailed(this.connection.getRoute(), iOException);
                } else {
                    this.routeSelector = null;
                }
            }
        }
        connectionFailed();
    }

    public void connectionFailed() {
        deallocate(true, false, true);
    }

    public void acquire(RealConnection realConnection) {
        realConnection.allocations.add(new WeakReference(this));
    }

    private void release(RealConnection realConnection) {
        int size = realConnection.allocations.size();
        for (int i = 0; i < size; i++) {
            if (realConnection.allocations.get(i).get() == this) {
                realConnection.allocations.remove(i);
                return;
            }
        }
        throw new IllegalStateException();
    }

    public boolean recover(RouteException routeException) {
        if (this.canceled) {
            return false;
        }
        if (this.connection != null) {
            connectionFailed(routeException.getLastConnectException());
        }
        return (this.routeSelector == null || this.routeSelector.hasNext()) && isRecoverable(routeException);
    }

    public boolean recover(IOException iOException, Sink sink) {
        if (this.connection != null) {
            int i = this.connection.streamCount;
            connectionFailed(iOException);
            if (i == 1) {
                return false;
            }
        }
        return (this.routeSelector == null || this.routeSelector.hasNext()) && isRecoverable(iOException) && (sink == null || (sink instanceof RetryableSink));
    }

    private boolean isRecoverable(IOException iOException) {
        return ((iOException instanceof ProtocolException) || (iOException instanceof InterruptedIOException)) ? false : true;
    }

    private boolean isRecoverable(RouteException routeException) {
        IOException lastConnectException = routeException.getLastConnectException();
        if (lastConnectException instanceof ProtocolException) {
            return false;
        }
        if (lastConnectException instanceof InterruptedIOException) {
            return lastConnectException instanceof SocketTimeoutException;
        }
        return (((lastConnectException instanceof SSLHandshakeException) && (lastConnectException.getCause() instanceof CertificateException)) || (lastConnectException instanceof SSLPeerUnverifiedException)) ? false : true;
    }

    public String toString() {
        return this.address.toString();
    }
}
