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

    protected AbstractPooledConnAdapter(ClientConnectionManager manager, AbstractPoolEntry entry) {
        super(manager, entry.connection);
        this.poolEntry = entry;
    }

    protected final void assertAttached() {
        if (this.poolEntry != null) {
        } else {
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
    public void open(HttpRoute route, HttpContext context, HttpParams params) throws IOException {
        assertAttached();
        this.poolEntry.open(route, context, params);
    }

    @Override
    public void tunnelTarget(boolean secure, HttpParams params) throws IOException {
        assertAttached();
        this.poolEntry.tunnelTarget(secure, params);
    }

    @Override
    public void tunnelProxy(HttpHost next, boolean secure, HttpParams params) throws IOException {
        assertAttached();
        this.poolEntry.tunnelProxy(next, secure, params);
    }

    @Override
    public void layerProtocol(HttpContext context, HttpParams params) throws IOException {
        assertAttached();
        this.poolEntry.layerProtocol(context, params);
    }

    @Override
    public void close() throws IOException {
        if (this.poolEntry != null) {
            this.poolEntry.shutdownEntry();
        }
        OperatedClientConnection conn = getWrappedConnection();
        if (conn == null) {
            return;
        }
        conn.close();
    }

    @Override
    public void shutdown() throws IOException {
        if (this.poolEntry != null) {
            this.poolEntry.shutdownEntry();
        }
        OperatedClientConnection conn = getWrappedConnection();
        if (conn == null) {
            return;
        }
        conn.shutdown();
    }

    @Override
    public Object getState() {
        assertAttached();
        return this.poolEntry.getState();
    }

    @Override
    public void setState(Object state) {
        assertAttached();
        this.poolEntry.setState(state);
    }
}
