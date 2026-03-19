package org.apache.http.impl.conn;

import java.net.InetAddress;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.protocol.HttpContext;

@Deprecated
public class DefaultHttpRoutePlanner implements HttpRoutePlanner {
    protected SchemeRegistry schemeRegistry;

    public DefaultHttpRoutePlanner(SchemeRegistry schreg) {
        if (schreg == null) {
            throw new IllegalArgumentException("SchemeRegistry must not be null.");
        }
        this.schemeRegistry = schreg;
    }

    @Override
    public HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        if (request == null) {
            throw new IllegalStateException("Request must not be null.");
        }
        HttpRoute route = ConnRouteParams.getForcedRoute(request.getParams());
        if (route != null) {
            return route;
        }
        if (target == null) {
            throw new IllegalStateException("Target host must not be null.");
        }
        InetAddress local = ConnRouteParams.getLocalAddress(request.getParams());
        HttpHost proxy = ConnRouteParams.getDefaultProxy(request.getParams());
        Scheme schm = this.schemeRegistry.getScheme(target.getSchemeName());
        boolean secure = schm.isLayered();
        if (proxy == null) {
            return new HttpRoute(target, local, secure);
        }
        return new HttpRoute(target, local, proxy, secure);
    }
}
