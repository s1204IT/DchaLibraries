package org.apache.http.client.params;

import org.apache.http.params.HttpParams;

@Deprecated
public class HttpClientParams {
    private HttpClientParams() {
    }

    public static boolean isRedirecting(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        return httpParams.getBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
    }

    public static void setRedirecting(HttpParams httpParams, boolean z) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        httpParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, z);
    }

    public static boolean isAuthenticating(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        return httpParams.getBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, true);
    }

    public static void setAuthenticating(HttpParams httpParams, boolean z) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        httpParams.setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, z);
    }

    public static String getCookiePolicy(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        String str = (String) httpParams.getParameter(ClientPNames.COOKIE_POLICY);
        if (str == null) {
            return CookiePolicy.BEST_MATCH;
        }
        return str;
    }

    public static void setCookiePolicy(HttpParams httpParams, String str) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        httpParams.setParameter(ClientPNames.COOKIE_POLICY, str);
    }
}
