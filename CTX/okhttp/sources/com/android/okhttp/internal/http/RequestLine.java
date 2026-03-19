package com.android.okhttp.internal.http;

import com.android.okhttp.HttpUrl;
import com.android.okhttp.Request;
import java.net.Proxy;

public final class RequestLine {
    private RequestLine() {
    }

    static String get(Request request, Proxy.Type type) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.method());
        sb.append(' ');
        if (includeAuthorityInRequestLine(request, type)) {
            sb.append(request.httpUrl());
        } else {
            sb.append(requestPath(request.httpUrl()));
        }
        sb.append(" HTTP/1.1");
        return sb.toString();
    }

    private static boolean includeAuthorityInRequestLine(Request request, Proxy.Type type) {
        return !request.isHttps() && type == Proxy.Type.HTTP;
    }

    public static String requestPath(HttpUrl httpUrl) {
        String strEncodedPath = httpUrl.encodedPath();
        String strEncodedQuery = httpUrl.encodedQuery();
        if (strEncodedQuery == null) {
            return strEncodedPath;
        }
        return strEncodedPath + '?' + strEncodedQuery;
    }
}
