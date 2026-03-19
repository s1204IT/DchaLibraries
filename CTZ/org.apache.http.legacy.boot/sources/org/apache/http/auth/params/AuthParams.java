package org.apache.http.auth.params;

import org.apache.http.params.HttpParams;

@Deprecated
public final class AuthParams {
    private AuthParams() {
    }

    public static String getCredentialCharset(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        String str = (String) httpParams.getParameter(AuthPNames.CREDENTIAL_CHARSET);
        if (str == null) {
            return "US-ASCII";
        }
        return str;
    }

    public static void setCredentialCharset(HttpParams httpParams, String str) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        httpParams.setParameter(AuthPNames.CREDENTIAL_CHARSET, str);
    }
}
