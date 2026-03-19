package org.apache.http.client.protocol;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.protocol.HttpContext;

@Deprecated
public class RequestTargetAuthentication implements HttpRequestInterceptor {
    private final Log log = LogFactory.getLog(getClass());

    @Override
    public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        AuthState authState;
        AuthScheme authScheme;
        if (httpRequest == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (httpContext == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        if (httpRequest.containsHeader(AUTH.WWW_AUTH_RESP) || (authState = (AuthState) httpContext.getAttribute(ClientContext.TARGET_AUTH_STATE)) == null || (authScheme = authState.getAuthScheme()) == null) {
            return;
        }
        Credentials credentials = authState.getCredentials();
        if (credentials == null) {
            this.log.debug("User credentials not available");
            return;
        }
        if (authState.getAuthScope() != null || !authScheme.isConnectionBased()) {
            try {
                httpRequest.addHeader(authScheme.authenticate(credentials, httpRequest));
            } catch (AuthenticationException e) {
                if (this.log.isErrorEnabled()) {
                    this.log.error("Authentication error: " + e.getMessage());
                }
            }
        }
    }
}
