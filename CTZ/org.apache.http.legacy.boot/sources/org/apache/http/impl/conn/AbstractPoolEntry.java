package org.apache.http.impl.conn;

import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.RouteTracker;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

@Deprecated
public abstract class AbstractPoolEntry {
    protected final ClientConnectionOperator connOperator;
    protected final OperatedClientConnection connection;
    protected volatile HttpRoute route;
    protected volatile Object state;
    protected volatile RouteTracker tracker;

    protected AbstractPoolEntry(ClientConnectionOperator clientConnectionOperator, HttpRoute httpRoute) {
        if (clientConnectionOperator == null) {
            throw new IllegalArgumentException("Connection operator may not be null");
        }
        this.connOperator = clientConnectionOperator;
        this.connection = clientConnectionOperator.createConnection();
        this.route = httpRoute;
        this.tracker = null;
    }

    public Object getState() {
        return this.state;
    }

    public void setState(Object obj) {
        this.state = obj;
    }

    public void open(HttpRoute httpRoute, HttpContext httpContext, HttpParams httpParams) throws IOException {
        HttpHost targetHost;
        if (httpRoute == null) {
            throw new IllegalArgumentException("Route must not be null.");
        }
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        if (this.tracker != null && this.tracker.isConnected()) {
            throw new IllegalStateException("Connection already open.");
        }
        this.tracker = new RouteTracker(httpRoute);
        HttpHost proxyHost = httpRoute.getProxyHost();
        ClientConnectionOperator clientConnectionOperator = this.connOperator;
        OperatedClientConnection operatedClientConnection = this.connection;
        if (proxyHost == null) {
            targetHost = httpRoute.getTargetHost();
        } else {
            targetHost = proxyHost;
        }
        clientConnectionOperator.openConnection(operatedClientConnection, targetHost, httpRoute.getLocalAddress(), httpContext, httpParams);
        RouteTracker routeTracker = this.tracker;
        if (routeTracker == null) {
            throw new IOException("Request aborted");
        }
        if (proxyHost == null) {
            routeTracker.connectTarget(this.connection.isSecure());
        } else {
            routeTracker.connectProxy(proxyHost, this.connection.isSecure());
        }
    }

    public void tunnelTarget(boolean z, HttpParams httpParams) throws IOException {
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        if (this.tracker == null || !this.tracker.isConnected()) {
            throw new IllegalStateException("Connection not open.");
        }
        if (this.tracker.isTunnelled()) {
            throw new IllegalStateException("Connection is already tunnelled.");
        }
        this.connection.update(null, this.tracker.getTargetHost(), z, httpParams);
        this.tracker.tunnelTarget(z);
    }

    public void tunnelProxy(HttpHost httpHost, boolean z, HttpParams httpParams) throws IOException {
        if (httpHost == null) {
            throw new IllegalArgumentException("Next proxy must not be null.");
        }
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        if (this.tracker == null || !this.tracker.isConnected()) {
            throw new IllegalStateException("Connection not open.");
        }
        this.connection.update(null, httpHost, z, httpParams);
        this.tracker.tunnelProxy(httpHost, z);
    }

    public void layerProtocol(HttpContext httpContext, HttpParams httpParams) throws IOException {
        if (httpParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        if (this.tracker == null || !this.tracker.isConnected()) {
            throw new IllegalStateException("Connection not open.");
        }
        if (!this.tracker.isTunnelled()) {
            throw new IllegalStateException("Protocol layering without a tunnel not supported.");
        }
        if (this.tracker.isLayered()) {
            throw new IllegalStateException("Multiple protocol layering not supported.");
        }
        this.connOperator.updateSecureConnection(this.connection, this.tracker.getTargetHost(), httpContext, httpParams);
        this.tracker.layerProtocol(this.connection.isSecure());
    }

    protected void shutdownEntry() {
        this.tracker = null;
    }
}
