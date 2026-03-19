package org.apache.http.client.utils;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpHost;

@Deprecated
public class URIUtils {
    public static URI createURI(String str, String str2, int i, String str3, String str4, String str5) throws URISyntaxException {
        StringBuilder sb = new StringBuilder();
        if (str2 != null) {
            if (str != null) {
                sb.append(str);
                sb.append("://");
            }
            sb.append(str2);
            if (i > 0) {
                sb.append(':');
                sb.append(i);
            }
        }
        if (str3 == null || !str3.startsWith("/")) {
            sb.append('/');
        }
        if (str3 != null) {
            sb.append(str3);
        }
        if (str4 != null) {
            sb.append('?');
            sb.append(str4);
        }
        if (str5 != null) {
            sb.append('#');
            sb.append(str5);
        }
        return new URI(sb.toString());
    }

    public static URI rewriteURI(URI uri, HttpHost httpHost, boolean z) throws URISyntaxException {
        if (uri == null) {
            throw new IllegalArgumentException("URI may nor be null");
        }
        String rawFragment = null;
        if (httpHost != null) {
            String schemeName = httpHost.getSchemeName();
            String hostName = httpHost.getHostName();
            int port = httpHost.getPort();
            String rawPath = uri.getRawPath();
            String rawQuery = uri.getRawQuery();
            if (!z) {
                rawFragment = uri.getRawFragment();
            }
            return createURI(schemeName, hostName, port, rawPath, rawQuery, rawFragment);
        }
        String rawPath2 = uri.getRawPath();
        String rawQuery2 = uri.getRawQuery();
        if (!z) {
            rawFragment = uri.getRawFragment();
        }
        return createURI(null, null, -1, rawPath2, rawQuery2, rawFragment);
    }

    public static URI rewriteURI(URI uri, HttpHost httpHost) throws URISyntaxException {
        return rewriteURI(uri, httpHost, false);
    }

    public static URI resolve(URI uri, String str) {
        return resolve(uri, URI.create(str));
    }

    public static URI resolve(URI uri, URI uri2) {
        if (uri == null) {
            throw new IllegalArgumentException("Base URI may nor be null");
        }
        if (uri2 == null) {
            throw new IllegalArgumentException("Reference URI may nor be null");
        }
        boolean z = uri2.toString().length() == 0;
        if (z) {
            uri2 = URI.create("#");
        }
        URI uriResolve = uri.resolve(uri2);
        if (z) {
            String string = uriResolve.toString();
            return URI.create(string.substring(0, string.indexOf(35)));
        }
        return uriResolve;
    }

    private URIUtils() {
    }
}
