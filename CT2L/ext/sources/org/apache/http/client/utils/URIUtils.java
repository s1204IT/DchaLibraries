package org.apache.http.client.utils;

import gov.nist.core.Separators;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpHost;

@Deprecated
public class URIUtils {
    public static URI createURI(String scheme, String host, int port, String path, String query, String fragment) throws URISyntaxException {
        StringBuilder buffer = new StringBuilder();
        if (host != null) {
            if (scheme != null) {
                buffer.append(scheme);
                buffer.append("://");
            }
            buffer.append(host);
            if (port > 0) {
                buffer.append(':');
                buffer.append(port);
            }
        }
        if (path == null || !path.startsWith(Separators.SLASH)) {
            buffer.append('/');
        }
        if (path != null) {
            buffer.append(path);
        }
        if (query != null) {
            buffer.append('?');
            buffer.append(query);
        }
        if (fragment != null) {
            buffer.append('#');
            buffer.append(fragment);
        }
        return new URI(buffer.toString());
    }

    public static URI rewriteURI(URI uri, HttpHost target, boolean dropFragment) throws URISyntaxException {
        if (uri == null) {
            throw new IllegalArgumentException("URI may nor be null");
        }
        if (target != null) {
            return createURI(target.getSchemeName(), target.getHostName(), target.getPort(), uri.getRawPath(), uri.getRawQuery(), dropFragment ? null : uri.getRawFragment());
        }
        return createURI(null, null, -1, uri.getRawPath(), uri.getRawQuery(), dropFragment ? null : uri.getRawFragment());
    }

    public static URI rewriteURI(URI uri, HttpHost target) throws URISyntaxException {
        return rewriteURI(uri, target, false);
    }

    public static URI resolve(URI baseURI, String reference) {
        return resolve(baseURI, URI.create(reference));
    }

    public static URI resolve(URI baseURI, URI reference) {
        if (baseURI == null) {
            throw new IllegalArgumentException("Base URI may nor be null");
        }
        if (reference == null) {
            throw new IllegalArgumentException("Reference URI may nor be null");
        }
        boolean emptyReference = reference.toString().length() == 0;
        if (emptyReference) {
            reference = URI.create(Separators.POUND);
        }
        URI resolved = baseURI.resolve(reference);
        if (emptyReference) {
            String resolvedString = resolved.toString();
            return URI.create(resolvedString.substring(0, resolvedString.indexOf(35)));
        }
        return resolved;
    }

    private URIUtils() {
    }
}
