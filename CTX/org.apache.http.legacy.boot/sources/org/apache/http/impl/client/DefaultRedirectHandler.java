package org.apache.http.impl.client;

import android.net.http.Headers;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

@Deprecated
public class DefaultRedirectHandler implements RedirectHandler {
    private static final String REDIRECT_LOCATIONS = "http.protocol.redirect-locations";
    private final Log log = LogFactory.getLog(getClass());

    @Override
    public boolean isRedirectRequested(HttpResponse httpResponse, HttpContext httpContext) {
        if (httpResponse == null) {
            throw new IllegalArgumentException("HTTP response may not be null");
        }
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode != 307) {
            switch (statusCode) {
                case HttpStatus.SC_MOVED_PERMANENTLY:
                case HttpStatus.SC_MOVED_TEMPORARILY:
                case HttpStatus.SC_SEE_OTHER:
                    return true;
                default:
                    return false;
            }
        }
        return true;
    }

    @Override
    public URI getLocationURI(HttpResponse httpResponse, HttpContext httpContext) throws ProtocolException {
        URI uriRewriteURI;
        if (httpResponse == null) {
            throw new IllegalArgumentException("HTTP response may not be null");
        }
        Header firstHeader = httpResponse.getFirstHeader(Headers.LOCATION);
        if (firstHeader == null) {
            throw new ProtocolException("Received redirect response " + httpResponse.getStatusLine() + " but no location header");
        }
        String value = firstHeader.getValue();
        if (this.log.isDebugEnabled()) {
            this.log.debug("Redirect requested to location '" + value + "'");
        }
        try {
            URI uri = new URI(value);
            HttpParams params = httpResponse.getParams();
            if (!uri.isAbsolute()) {
                if (params.isParameterTrue(ClientPNames.REJECT_RELATIVE_REDIRECT)) {
                    throw new ProtocolException("Relative redirect location '" + uri + "' not allowed");
                }
                HttpHost httpHost = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (httpHost == null) {
                    throw new IllegalStateException("Target host not available in the HTTP context");
                }
                try {
                    uri = URIUtils.resolve(URIUtils.rewriteURI(new URI(((HttpRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST)).getRequestLine().getUri()), httpHost, true), uri);
                } catch (URISyntaxException e) {
                    throw new ProtocolException(e.getMessage(), e);
                }
            }
            if (params.isParameterFalse(ClientPNames.ALLOW_CIRCULAR_REDIRECTS)) {
                RedirectLocations redirectLocations = (RedirectLocations) httpContext.getAttribute(REDIRECT_LOCATIONS);
                if (redirectLocations == null) {
                    redirectLocations = new RedirectLocations();
                    httpContext.setAttribute(REDIRECT_LOCATIONS, redirectLocations);
                }
                if (uri.getFragment() != null) {
                    try {
                        uriRewriteURI = URIUtils.rewriteURI(uri, new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), true);
                    } catch (URISyntaxException e2) {
                        throw new ProtocolException(e2.getMessage(), e2);
                    }
                } else {
                    uriRewriteURI = uri;
                }
                if (redirectLocations.contains(uriRewriteURI)) {
                    throw new CircularRedirectException("Circular redirect to '" + uriRewriteURI + "'");
                }
                redirectLocations.add(uriRewriteURI);
            }
            return uri;
        } catch (URISyntaxException e3) {
            throw new ProtocolException("Invalid redirect URI: " + value, e3);
        }
    }
}
