package org.apache.http.impl.conn;

import android.net.TrafficStats;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.RouteTracker;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.params.HttpParams;

@Deprecated
public class SingleClientConnManager implements ClientConnectionManager {
    public static final String MISUSE_MESSAGE = "Invalid use of SingleClientConnManager: connection still allocated.\nMake sure to release the connection before allocating another one.";
    protected boolean alwaysShutDown;
    protected ClientConnectionOperator connOperator;
    protected long connectionExpiresTime;
    protected volatile boolean isShutDown;
    protected long lastReleaseTime;
    private final Log log = LogFactory.getLog(getClass());
    protected ConnAdapter managedConn;
    protected SchemeRegistry schemeRegistry;
    protected PoolEntry uniquePoolEntry;

    public SingleClientConnManager(HttpParams httpParams, SchemeRegistry schemeRegistry) {
        if (schemeRegistry == null) {
            throw new IllegalArgumentException("Scheme registry must not be null.");
        }
        this.schemeRegistry = schemeRegistry;
        this.connOperator = createConnectionOperator(schemeRegistry);
        this.uniquePoolEntry = new PoolEntry();
        this.managedConn = null;
        this.lastReleaseTime = -1L;
        this.alwaysShutDown = false;
        this.isShutDown = false;
    }

    protected void finalize() throws Throwable {
        shutdown();
        super.finalize();
    }

    @Override
    public SchemeRegistry getSchemeRegistry() {
        return this.schemeRegistry;
    }

    protected ClientConnectionOperator createConnectionOperator(SchemeRegistry schemeRegistry) {
        return new DefaultClientConnectionOperator(schemeRegistry);
    }

    protected final void assertStillUp() throws IllegalStateException {
        if (this.isShutDown) {
            throw new IllegalStateException("Manager is shut down.");
        }
    }

    @Override
    public final ClientConnectionRequest requestConnection(final HttpRoute httpRoute, final Object obj) {
        return new ClientConnectionRequest() {
            @Override
            public void abortRequest() {
            }

            @Override
            public ManagedClientConnection getConnection(long j, TimeUnit timeUnit) {
                return SingleClientConnManager.this.getConnection(httpRoute, obj);
            }
        };
    }

    public ManagedClientConnection getConnection(HttpRoute httpRoute, Object obj) {
        boolean z;
        if (httpRoute == null) {
            throw new IllegalArgumentException("Route may not be null.");
        }
        assertStillUp();
        if (this.log.isDebugEnabled()) {
            this.log.debug("Get connection for route " + httpRoute);
        }
        if (this.managedConn != null) {
            revokeConnection();
        }
        closeExpiredConnections();
        boolean z2 = true;
        boolean z3 = false;
        if (this.uniquePoolEntry.connection.isOpen()) {
            RouteTracker routeTracker = this.uniquePoolEntry.tracker;
            z = routeTracker == null || !routeTracker.toRoute().equals(httpRoute);
        } else {
            z = false;
            z3 = true;
        }
        if (z) {
            try {
                this.uniquePoolEntry.shutdown();
            } catch (IOException e) {
                this.log.debug("Problem shutting down connection.", e);
            }
        } else {
            z2 = z3;
        }
        if (z2) {
            this.uniquePoolEntry = new PoolEntry();
        }
        try {
            Socket socket = this.uniquePoolEntry.connection.getSocket();
            if (socket != null) {
                TrafficStats.tagSocket(socket);
            }
        } catch (IOException e2) {
            this.log.debug("Problem tagging socket.", e2);
        }
        this.managedConn = new ConnAdapter(this.uniquePoolEntry, httpRoute);
        return this.managedConn;
    }

    @Override
    public void releaseConnection(ManagedClientConnection managedClientConnection, long j, TimeUnit timeUnit) {
        assertStillUp();
        if (!(managedClientConnection instanceof ConnAdapter)) {
            throw new IllegalArgumentException("Connection class mismatch, connection not obtained from this manager.");
        }
        if (this.log.isDebugEnabled()) {
            this.log.debug("Releasing connection " + managedClientConnection);
        }
        if (managedClientConnection.poolEntry == null) {
            return;
        }
        ClientConnectionManager manager = managedClientConnection.getManager();
        if (manager != null && manager != this) {
            throw new IllegalArgumentException("Connection not obtained from this manager.");
        }
        try {
            try {
                Socket socket = this.uniquePoolEntry.connection.getSocket();
                if (socket != null) {
                    TrafficStats.untagSocket(socket);
                }
                if (managedClientConnection.isOpen() && (this.alwaysShutDown || !managedClientConnection.isMarkedReusable())) {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("Released connection open but not reusable.");
                    }
                    managedClientConnection.shutdown();
                }
                managedClientConnection.detach();
                this.managedConn = null;
                this.lastReleaseTime = System.currentTimeMillis();
            } catch (IOException e) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Exception shutting down released connection.", e);
                }
                managedClientConnection.detach();
                this.managedConn = null;
                this.lastReleaseTime = System.currentTimeMillis();
            }
        } catch (Throwable th) {
            managedClientConnection.detach();
            this.managedConn = null;
            this.lastReleaseTime = System.currentTimeMillis();
            if (j > 0) {
                this.connectionExpiresTime = timeUnit.toMillis(j) + this.lastReleaseTime;
            } else {
                this.connectionExpiresTime = Long.MAX_VALUE;
            }
            throw th;
        }
    }

    @Override
    public void closeExpiredConnections() {
        if (System.currentTimeMillis() >= this.connectionExpiresTime) {
            closeIdleConnections(0L, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void closeIdleConnections(long j, TimeUnit timeUnit) {
        assertStillUp();
        if (timeUnit == null) {
            throw new IllegalArgumentException("Time unit must not be null.");
        }
        if (this.managedConn == null && this.uniquePoolEntry.connection.isOpen()) {
            if (this.lastReleaseTime <= System.currentTimeMillis() - timeUnit.toMillis(j)) {
                try {
                    this.uniquePoolEntry.close();
                } catch (IOException e) {
                    this.log.debug("Problem closing idle connection.", e);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        this.isShutDown = true;
        if (this.managedConn != null) {
            this.managedConn.detach();
        }
        try {
            try {
                if (this.uniquePoolEntry != null) {
                    this.uniquePoolEntry.shutdown();
                }
            } catch (IOException e) {
                this.log.debug("Problem while shutting down manager.", e);
            }
        } finally {
            this.uniquePoolEntry = null;
        }
    }

    protected void revokeConnection() {
        if (this.managedConn == null) {
            return;
        }
        this.log.warn(MISUSE_MESSAGE);
        this.managedConn.detach();
        try {
            this.uniquePoolEntry.shutdown();
        } catch (IOException e) {
            this.log.debug("Problem while shutting down connection.", e);
        }
    }

    protected class PoolEntry extends AbstractPoolEntry {
        protected PoolEntry() {
            super(SingleClientConnManager.this.connOperator, null);
        }

        protected void close() throws IOException {
            shutdownEntry();
            if (this.connection.isOpen()) {
                this.connection.close();
            }
        }

        protected void shutdown() throws IOException {
            shutdownEntry();
            if (this.connection.isOpen()) {
                this.connection.shutdown();
            }
        }
    }

    protected class ConnAdapter extends AbstractPooledConnAdapter {
        protected ConnAdapter(PoolEntry poolEntry, HttpRoute httpRoute) {
            super(SingleClientConnManager.this, poolEntry);
            markReusable();
            poolEntry.route = httpRoute;
        }
    }
}
