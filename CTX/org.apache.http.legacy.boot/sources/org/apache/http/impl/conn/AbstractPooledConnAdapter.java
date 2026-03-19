package org.apache.http.impl.conn;

import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

@Deprecated
public abstract class AbstractPooledConnAdapter extends AbstractClientConnAdapter {
    protected volatile AbstractPoolEntry poolEntry;

    protected AbstractPooledConnAdapter(ClientConnectionManager clientConnectionManager, AbstractPoolEntry abstractPoolEntry) {
        super(clientConnectionManager, abstractPoolEntry.connection);
        this.poolEntry = abstractPoolEntry;
    }

    protected final void assertAttached() {
        if (this.poolEntry == null) {
            throw new IllegalStateException("Adapter is detached.");
        }
    }

    @Override
    protected void detach() {
        super.detach();
        this.poolEntry = null;
    }

    @Override
    public HttpRoute getRoute() {
        assertAttached();
        if (this.poolEntry.tracker == null) {
            return null;
        }
        return this.poolEntry.tracker.toRoute();
    }

    @Override
    public void open(HttpRoute httpRoute, HttpContext httpContext, HttpParams httpParams) throws IOException {
        assertAttached();
        this.poolEntry.open(httpRoute, httpContext, httpParams);
    }

    @Override
    public void tunnelTarget(boolean z, HttpParams httpParams) throws IOException {
        assertAttached();
        this.poolEntry.tunnelTarget(z, httpParams);
    }

    @Override
    public void tunnelProxy(HttpHost httpHost, boolean z, HttpParams httpParams) throws IOException {
        assertAttached();
        this.poolEntry.tunnelProxy(httpHost, z, httpParams);
    }

    @Override
    public void layerProtocol(HttpContext httpContext, HttpParams httpParams) throws IOException {
        assertAttached();
        this.poolEntry.layerProtocol(httpContext, httpParams);
    }

    @Override
    public void close() throws IOException {
        if (this.poolEntry != null) {
            this.poolEntry.shutdownEntry();
        }
        OperatedClientConnection wrappedConnection = getWrappedConnection();
        if (wrappedConnection != null) {
            wrappedConnection.close();
        }
    }

    @Override
    public void shutdown() throws IOException {
        if (this.poolEntry != null) {
            this.poolEntry.shutdownEntry();
        }
        OperatedClientConnection wrappedConnection = getWrappedConnection();
        if (wrappedConnection != null) {
            wrappedConnection.shutdown();
        }
    }

    @Override
    public Object getState() {
        assertAttached();
        return this.poolEntry.getState();
    }

    @Override
    public void setState(Object obj) {
        assertAttached();
        this.poolEntry.setState(obj);
    }
}
