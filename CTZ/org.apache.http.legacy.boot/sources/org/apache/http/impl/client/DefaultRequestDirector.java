package org.apache.http.impl.client;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.NonRepeatableRequestException;
import org.apache.http.client.RedirectException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.BasicManagedEntity;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.routing.BasicRouteDirector;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

@Deprecated
public class DefaultRequestDirector implements RequestDirector {
    private static Method cleartextTrafficPermittedMethod;
    private static Object networkSecurityPolicy;
    protected final ClientConnectionManager connManager;
    protected final HttpProcessor httpProcessor;
    protected final ConnectionKeepAliveStrategy keepAliveStrategy;
    private final Log log = LogFactory.getLog(getClass());
    protected ManagedClientConnection managedConn;
    private int maxRedirects;
    protected final HttpParams params;
    private final AuthenticationHandler proxyAuthHandler;
    private final AuthState proxyAuthState;
    private int redirectCount;
    protected final RedirectHandler redirectHandler;
    protected final HttpRequestExecutor requestExec;
    protected final HttpRequestRetryHandler retryHandler;
    protected final ConnectionReuseStrategy reuseStrategy;
    protected final HttpRoutePlanner routePlanner;
    private final AuthenticationHandler targetAuthHandler;
    private final AuthState targetAuthState;
    private final UserTokenHandler userTokenHandler;

    public DefaultRequestDirector(HttpRequestExecutor httpRequestExecutor, ClientConnectionManager clientConnectionManager, ConnectionReuseStrategy connectionReuseStrategy, ConnectionKeepAliveStrategy connectionKeepAliveStrategy, HttpRoutePlanner httpRoutePlanner, HttpProcessor httpProcessor, HttpRequestRetryHandler httpRequestRetryHandler, RedirectHandler redirectHandler, AuthenticationHandler authenticationHandler, AuthenticationHandler authenticationHandler2, UserTokenHandler userTokenHandler, HttpParams httpParams) {
        if (httpRequestExecutor == null) {
            throw new IllegalArgumentException("Request executor may not be null.");
        }
        if (clientConnectionManager == null) {
            throw new IllegalArgumentException("Client connection manager may not be null.");
        }
        if (connectionReuseStrategy == null) {
            throw new IllegalArgumentException("Connection reuse strategy may not be null.");
        }
        if (connectionKeepAliveStrategy == null) {
            throw new IllegalArgumentException("Connection keep alive strategy may not be null.");
        }
        if (httpRoutePlanner == null) {
            throw new IllegalArgumentException("Route planner may not be null.");
        }
        if (httpProcessor == null) {
            throw new IllegalArgumentException("HTTP protocol processor may not be null.");
        }
        if (httpRequestRetryHandler == null) {
            throw new IllegalArgumentException("HTTP request retry handler may not be null.");
        }
        if (redirectHandler == null) {
            throw new IllegalArgumentException("Redirect handler may not be null.");
        }
        if (authenticationHandler == null) {
            throw new IllegalArgumentException("Target authentication handler may not be null.");
        }
        if (authenticationHandler2 == null) {
            throw new IllegalArgumentException("Proxy authentication handler may not be null.");
        }
        if (userTokenHandler == null) {
            throw new IllegalArgumentException("User token handler may not be null.");
        }
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.requestExec = httpRequestExecutor;
        this.connManager = clientConnectionManager;
        this.reuseStrategy = connectionReuseStrategy;
        this.keepAliveStrategy = connectionKeepAliveStrategy;
        this.routePlanner = httpRoutePlanner;
        this.httpProcessor = httpProcessor;
        this.retryHandler = httpRequestRetryHandler;
        this.redirectHandler = redirectHandler;
        this.targetAuthHandler = authenticationHandler;
        this.proxyAuthHandler = authenticationHandler2;
        this.userTokenHandler = userTokenHandler;
        this.params = httpParams;
        this.managedConn = null;
        this.redirectCount = 0;
        this.maxRedirects = this.params.getIntParameter(ClientPNames.MAX_REDIRECTS, 100);
        this.targetAuthState = new AuthState();
        this.proxyAuthState = new AuthState();
    }

    private RequestWrapper wrapRequest(HttpRequest httpRequest) throws ProtocolException {
        if (httpRequest instanceof HttpEntityEnclosingRequest) {
            return new EntityEnclosingRequestWrapper((HttpEntityEnclosingRequest) httpRequest);
        }
        return new RequestWrapper(httpRequest);
    }

    protected void rewriteRequestURI(RequestWrapper requestWrapper, HttpRoute httpRoute) throws ProtocolException {
        try {
            URI uri = requestWrapper.getURI();
            if (httpRoute.getProxyHost() != null && !httpRoute.isTunnelled()) {
                if (!uri.isAbsolute()) {
                    requestWrapper.setURI(URIUtils.rewriteURI(uri, httpRoute.getTargetHost()));
                }
            } else if (uri.isAbsolute()) {
                requestWrapper.setURI(URIUtils.rewriteURI(uri, null));
            }
        } catch (URISyntaxException e) {
            throw new ProtocolException("Invalid URI: " + requestWrapper.getRequestLine().getUri(), e);
        }
    }

    @Override
    public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        TunnelRefusedException tunnelRefusedException;
        RequestWrapper requestWrapperWrapRequest = wrapRequest(httpRequest);
        requestWrapperWrapRequest.setParams(this.params);
        RoutedRequest routedRequest = new RoutedRequest(requestWrapperWrapRequest, determineRoute(httpHost, requestWrapperWrapRequest, httpContext));
        long timeout = ConnManagerParams.getTimeout(this.params);
        HttpResponse response = null;
        RoutedRequest routedRequest2 = routedRequest;
        int i = 0;
        boolean z = false;
        int i2 = 0;
        while (i == 0) {
            try {
                try {
                    RequestWrapper request = routedRequest2.getRequest();
                    HttpRoute route = routedRequest2.getRoute();
                    Object attribute = httpContext.getAttribute(ClientContext.USER_TOKEN);
                    if (this.managedConn == null) {
                        ClientConnectionRequest clientConnectionRequestRequestConnection = this.connManager.requestConnection(route, attribute);
                        if (httpRequest instanceof AbortableHttpRequest) {
                            ((AbortableHttpRequest) httpRequest).setConnectionRequest(clientConnectionRequestRequestConnection);
                        }
                        try {
                            this.managedConn = clientConnectionRequestRequestConnection.getConnection(timeout, TimeUnit.MILLISECONDS);
                            if (HttpConnectionParams.isStaleCheckingEnabled(this.params)) {
                                this.log.debug("Stale connection check");
                                if (this.managedConn.isStale()) {
                                    this.log.debug("Stale connection detected");
                                    try {
                                        this.managedConn.close();
                                    } catch (IOException e) {
                                    }
                                }
                            }
                        } catch (InterruptedException e2) {
                            InterruptedIOException interruptedIOException = new InterruptedIOException();
                            interruptedIOException.initCause(e2);
                            throw interruptedIOException;
                        }
                    }
                    if (httpRequest instanceof AbortableHttpRequest) {
                        ((AbortableHttpRequest) httpRequest).setReleaseTrigger(this.managedConn);
                    }
                    if (!this.managedConn.isOpen()) {
                        this.managedConn.open(route, httpContext, this.params);
                    } else {
                        this.managedConn.setSocketTimeout(HttpConnectionParams.getSoTimeout(this.params));
                    }
                    try {
                        establishRoute(route, httpContext);
                        request.resetHeaders();
                        rewriteRequestURI(request, route);
                        HttpHost targetHost = (HttpHost) request.getParams().getParameter(ClientPNames.VIRTUAL_HOST);
                        if (targetHost == null) {
                            targetHost = route.getTargetHost();
                        }
                        HttpHost proxyHost = route.getProxyHost();
                        httpContext.setAttribute(ExecutionContext.HTTP_TARGET_HOST, targetHost);
                        httpContext.setAttribute(ExecutionContext.HTTP_PROXY_HOST, proxyHost);
                        httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, this.managedConn);
                        httpContext.setAttribute(ClientContext.TARGET_AUTH_STATE, this.targetAuthState);
                        httpContext.setAttribute(ClientContext.PROXY_AUTH_STATE, this.proxyAuthState);
                        this.requestExec.preProcess(request, this.httpProcessor, httpContext);
                        httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, request);
                        int i3 = 1;
                        HttpResponse httpResponseExecute = response;
                        boolean z2 = true;
                        while (z2) {
                            i2++;
                            request.incrementExecCount();
                            if (request.getExecCount() > i3 && !request.isRepeatable()) {
                                throw new NonRepeatableRequestException("Cannot retry request with a non-repeatable request entity");
                            }
                            try {
                                if (this.log.isDebugEnabled()) {
                                    this.log.debug("Attempt " + i2 + " to execute request");
                                }
                                if (!route.isSecure() && !isCleartextTrafficPermitted(route.getTargetHost().getHostName())) {
                                    throw new IOException("Cleartext traffic not permitted: " + route.getTargetHost());
                                }
                                httpResponseExecute = this.requestExec.execute(request, this.managedConn, httpContext);
                                z2 = false;
                            } catch (IOException e3) {
                                this.log.debug("Closing the connection.");
                                this.managedConn.close();
                                if (this.retryHandler.retryRequest(e3, i2, httpContext)) {
                                    if (this.log.isInfoEnabled()) {
                                        this.log.info("I/O exception (" + e3.getClass().getName() + ") caught when processing request: " + e3.getMessage());
                                    }
                                    if (this.log.isDebugEnabled()) {
                                        this.log.debug(e3.getMessage(), e3);
                                    }
                                    this.log.info("Retrying request");
                                    i3 = 1;
                                    if (route.getHopCount() == 1) {
                                        this.log.debug("Reopening the direct connection.");
                                        this.managedConn.open(route, httpContext, this.params);
                                    } else {
                                        throw e3;
                                    }
                                } else {
                                    throw e3;
                                }
                            }
                        }
                        httpResponseExecute.setParams(this.params);
                        this.requestExec.postProcess(httpResponseExecute, this.httpProcessor, httpContext);
                        boolean zKeepAlive = this.reuseStrategy.keepAlive(httpResponseExecute, httpContext);
                        if (zKeepAlive) {
                            this.managedConn.setIdleDuration(this.keepAliveStrategy.getKeepAliveDuration(httpResponseExecute, httpContext), TimeUnit.MILLISECONDS);
                        }
                        RoutedRequest routedRequestHandleResponse = handleResponse(routedRequest2, httpResponseExecute, httpContext);
                        if (routedRequestHandleResponse != null) {
                            if (zKeepAlive) {
                                this.log.debug("Connection kept alive");
                                HttpEntity entity = httpResponseExecute.getEntity();
                                if (entity != null) {
                                    entity.consumeContent();
                                }
                                this.managedConn.markReusable();
                            } else {
                                this.managedConn.close();
                            }
                            if (!routedRequestHandleResponse.getRoute().equals(routedRequest2.getRoute())) {
                                releaseConnection();
                            }
                            routedRequest2 = routedRequestHandleResponse;
                        } else {
                            i = i3;
                        }
                        Object userToken = this.userTokenHandler.getUserToken(httpContext);
                        httpContext.setAttribute(ClientContext.USER_TOKEN, userToken);
                        if (this.managedConn != null) {
                            this.managedConn.setState(userToken);
                        }
                        z = zKeepAlive;
                        response = httpResponseExecute;
                    } catch (TunnelRefusedException e4) {
                        if (this.log.isDebugEnabled()) {
                            tunnelRefusedException = e4;
                            this.log.debug(tunnelRefusedException.getMessage());
                        } else {
                            tunnelRefusedException = e4;
                        }
                        response = tunnelRefusedException.getResponse();
                    }
                } catch (IOException e5) {
                    abortConnection();
                    throw e5;
                }
            } catch (RuntimeException e6) {
                abortConnection();
                throw e6;
            } catch (HttpException e7) {
                abortConnection();
                throw e7;
            }
        }
        if (response == null || response.getEntity() == null || !response.getEntity().isStreaming()) {
            if (z) {
                this.managedConn.markReusable();
            }
            releaseConnection();
        } else {
            response.setEntity(new BasicManagedEntity(response.getEntity(), this.managedConn, z));
        }
        return response;
    }

    protected void releaseConnection() {
        try {
            this.managedConn.releaseConnection();
        } catch (IOException e) {
            this.log.debug("IOException releasing connection", e);
        }
        this.managedConn = null;
    }

    protected HttpRoute determineRoute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws HttpException {
        String path;
        String host;
        URI uri;
        if (httpHost == null) {
            httpHost = (HttpHost) httpRequest.getParams().getParameter(ClientPNames.DEFAULT_HOST);
        }
        if (httpHost == null) {
            String scheme = null;
            if ((httpRequest instanceof HttpUriRequest) && (uri = ((HttpUriRequest) httpRequest).getURI()) != null) {
                scheme = uri.getScheme();
                host = uri.getHost();
                path = uri.getPath();
            } else {
                path = null;
                host = null;
            }
            throw new IllegalStateException("Target host must not be null, or set in parameters. scheme=" + scheme + ", host=" + host + ", path=" + path);
        }
        return this.routePlanner.determineRoute(httpHost, httpRequest, httpContext);
    }

    protected void establishRoute(HttpRoute httpRoute, HttpContext httpContext) throws HttpException, IOException {
        int iNextStep;
        BasicRouteDirector basicRouteDirector = new BasicRouteDirector();
        do {
            HttpRoute route = this.managedConn.getRoute();
            iNextStep = basicRouteDirector.nextStep(httpRoute, route);
            switch (iNextStep) {
                case -1:
                    throw new IllegalStateException("Unable to establish route.\nplanned = " + httpRoute + "\ncurrent = " + route);
                case 0:
                    break;
                case 1:
                case 2:
                    this.managedConn.open(httpRoute, httpContext, this.params);
                    break;
                case 3:
                    boolean zCreateTunnelToTarget = createTunnelToTarget(httpRoute, httpContext);
                    this.log.debug("Tunnel to target created.");
                    this.managedConn.tunnelTarget(zCreateTunnelToTarget, this.params);
                    break;
                case 4:
                    int hopCount = route.getHopCount() - 1;
                    boolean zCreateTunnelToProxy = createTunnelToProxy(httpRoute, hopCount, httpContext);
                    this.log.debug("Tunnel to proxy created.");
                    this.managedConn.tunnelProxy(httpRoute.getHopTarget(hopCount), zCreateTunnelToProxy, this.params);
                    break;
                case 5:
                    this.managedConn.layerProtocol(httpContext, this.params);
                    break;
                default:
                    throw new IllegalStateException("Unknown step indicator " + iNextStep + " from RouteDirector.");
            }
        } while (iNextStep > 0);
    }

    protected boolean createTunnelToTarget(HttpRoute httpRoute, HttpContext httpContext) throws HttpException, IOException {
        HttpResponse httpResponseExecute;
        CredentialsProvider credentialsProvider;
        boolean z;
        HttpHost proxyHost = httpRoute.getProxyHost();
        HttpHost targetHost = httpRoute.getTargetHost();
        HttpResponse httpResponse = null;
        boolean z2 = false;
        while (true) {
            if (!z2) {
                if (!this.managedConn.isOpen()) {
                    this.managedConn.open(httpRoute, httpContext, this.params);
                }
                HttpRequest httpRequestCreateConnectRequest = createConnectRequest(httpRoute, httpContext);
                String userAgent = HttpProtocolParams.getUserAgent(this.params);
                if (userAgent != null) {
                    httpRequestCreateConnectRequest.addHeader(HTTP.USER_AGENT, userAgent);
                }
                httpRequestCreateConnectRequest.addHeader(HTTP.TARGET_HOST, targetHost.toHostString());
                AuthScheme authScheme = this.proxyAuthState.getAuthScheme();
                AuthScope authScope = this.proxyAuthState.getAuthScope();
                Credentials credentials = this.proxyAuthState.getCredentials();
                if (credentials != null && (authScope != null || !authScheme.isConnectionBased())) {
                    try {
                        httpRequestCreateConnectRequest.addHeader(authScheme.authenticate(credentials, httpRequestCreateConnectRequest));
                    } catch (AuthenticationException e) {
                        if (this.log.isErrorEnabled()) {
                            this.log.error("Proxy authentication error: " + e.getMessage());
                        }
                    }
                }
                httpResponseExecute = this.requestExec.execute(httpRequestCreateConnectRequest, this.managedConn, httpContext);
                if (httpResponseExecute.getStatusLine().getStatusCode() < 200) {
                    throw new HttpException("Unexpected response to CONNECT request: " + httpResponseExecute.getStatusLine());
                }
                CredentialsProvider credentialsProvider2 = (CredentialsProvider) httpContext.getAttribute(ClientContext.CREDS_PROVIDER);
                if (credentialsProvider2 != null && HttpClientParams.isAuthenticating(this.params)) {
                    if (!this.proxyAuthHandler.isAuthenticationRequested(httpResponseExecute, httpContext)) {
                        this.proxyAuthState.setAuthScope(null);
                        z2 = true;
                    } else {
                        this.log.debug("Proxy requested authentication");
                        try {
                            credentialsProvider = credentialsProvider2;
                            try {
                                processChallenges(this.proxyAuthHandler.getChallenges(httpResponseExecute, httpContext), this.proxyAuthState, this.proxyAuthHandler, httpResponseExecute, httpContext);
                            } catch (AuthenticationException e2) {
                                e = e2;
                                if (this.log.isWarnEnabled()) {
                                    this.log.warn("Authentication error: " + e.getMessage());
                                    if (httpResponseExecute.getStatusLine().getStatusCode() <= 299) {
                                    }
                                }
                            }
                        } catch (AuthenticationException e3) {
                            e = e3;
                            credentialsProvider = credentialsProvider2;
                        }
                        updateAuthState(this.proxyAuthState, proxyHost, credentialsProvider);
                        if (this.proxyAuthState.getCredentials() != null) {
                            if (this.reuseStrategy.keepAlive(httpResponseExecute, httpContext)) {
                                this.log.debug("Connection kept alive");
                                HttpEntity entity = httpResponseExecute.getEntity();
                                if (entity != null) {
                                    entity.consumeContent();
                                }
                            } else {
                                this.managedConn.close();
                            }
                            z = false;
                        } else {
                            z = true;
                        }
                        z2 = z;
                    }
                } else {
                    z2 = true;
                }
                httpResponse = httpResponseExecute;
            } else {
                httpResponseExecute = httpResponse;
                break;
            }
        }
        if (httpResponseExecute.getStatusLine().getStatusCode() <= 299) {
            HttpEntity entity2 = httpResponseExecute.getEntity();
            if (entity2 != null) {
                httpResponseExecute.setEntity(new BufferedHttpEntity(entity2));
            }
            this.managedConn.close();
            throw new TunnelRefusedException("CONNECT refused by proxy: " + httpResponseExecute.getStatusLine(), httpResponseExecute);
        }
        this.managedConn.markReusable();
        return false;
    }

    protected boolean createTunnelToProxy(HttpRoute httpRoute, int i, HttpContext httpContext) throws HttpException, IOException {
        throw new UnsupportedOperationException("Proxy chains are not supported.");
    }

    protected HttpRequest createConnectRequest(HttpRoute httpRoute, HttpContext httpContext) {
        HttpHost targetHost = httpRoute.getTargetHost();
        String hostName = targetHost.getHostName();
        int port = targetHost.getPort();
        if (port < 0) {
            port = this.connManager.getSchemeRegistry().getScheme(targetHost.getSchemeName()).getDefaultPort();
        }
        StringBuilder sb = new StringBuilder(hostName.length() + 6);
        sb.append(hostName);
        sb.append(':');
        sb.append(Integer.toString(port));
        return new BasicHttpRequest("CONNECT", sb.toString(), HttpProtocolParams.getVersion(this.params));
    }

    protected RoutedRequest handleResponse(RoutedRequest routedRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
        HttpRoute route = routedRequest.getRoute();
        HttpHost proxyHost = route.getProxyHost();
        RequestWrapper request = routedRequest.getRequest();
        HttpParams params = request.getParams();
        if (HttpClientParams.isRedirecting(params) && this.redirectHandler.isRedirectRequested(httpResponse, httpContext)) {
            if (this.redirectCount >= this.maxRedirects) {
                throw new RedirectException("Maximum redirects (" + this.maxRedirects + ") exceeded");
            }
            this.redirectCount++;
            URI locationURI = this.redirectHandler.getLocationURI(httpResponse, httpContext);
            HttpHost httpHost = new HttpHost(locationURI.getHost(), locationURI.getPort(), locationURI.getScheme());
            HttpGet httpGet = new HttpGet(locationURI);
            httpGet.setHeaders(request.getOriginal().getAllHeaders());
            RequestWrapper requestWrapper = new RequestWrapper(httpGet);
            requestWrapper.setParams(params);
            HttpRoute httpRouteDetermineRoute = determineRoute(httpHost, requestWrapper, httpContext);
            RoutedRequest routedRequest2 = new RoutedRequest(requestWrapper, httpRouteDetermineRoute);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Redirecting to '" + locationURI + "' via " + httpRouteDetermineRoute);
            }
            return routedRequest2;
        }
        CredentialsProvider credentialsProvider = (CredentialsProvider) httpContext.getAttribute(ClientContext.CREDS_PROVIDER);
        if (credentialsProvider != null && HttpClientParams.isAuthenticating(params)) {
            if (!this.targetAuthHandler.isAuthenticationRequested(httpResponse, httpContext)) {
                this.targetAuthState.setAuthScope(null);
                if (!this.proxyAuthHandler.isAuthenticationRequested(httpResponse, httpContext)) {
                    this.proxyAuthState.setAuthScope(null);
                } else {
                    this.log.debug("Proxy requested authentication");
                    try {
                        processChallenges(this.proxyAuthHandler.getChallenges(httpResponse, httpContext), this.proxyAuthState, this.proxyAuthHandler, httpResponse, httpContext);
                    } catch (AuthenticationException e) {
                        if (this.log.isWarnEnabled()) {
                            this.log.warn("Authentication error: " + e.getMessage());
                            return null;
                        }
                    }
                    updateAuthState(this.proxyAuthState, proxyHost, credentialsProvider);
                    if (this.proxyAuthState.getCredentials() == null) {
                        return null;
                    }
                    return routedRequest;
                }
            } else {
                HttpHost targetHost = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (targetHost == null) {
                    targetHost = route.getTargetHost();
                }
                this.log.debug("Target requested authentication");
                try {
                    processChallenges(this.targetAuthHandler.getChallenges(httpResponse, httpContext), this.targetAuthState, this.targetAuthHandler, httpResponse, httpContext);
                } catch (AuthenticationException e2) {
                    if (this.log.isWarnEnabled()) {
                        this.log.warn("Authentication error: " + e2.getMessage());
                        return null;
                    }
                }
                updateAuthState(this.targetAuthState, targetHost, credentialsProvider);
                if (this.targetAuthState.getCredentials() == null) {
                    return null;
                }
                return routedRequest;
            }
        }
        return null;
    }

    private void abortConnection() {
        ManagedClientConnection managedClientConnection = this.managedConn;
        if (managedClientConnection != null) {
            this.managedConn = null;
            try {
                managedClientConnection.abortConnection();
            } catch (IOException e) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug(e.getMessage(), e);
                }
            }
            try {
                managedClientConnection.releaseConnection();
            } catch (IOException e2) {
                this.log.debug("Error releasing connection", e2);
            }
        }
    }

    private void processChallenges(Map<String, Header> map, AuthState authState, AuthenticationHandler authenticationHandler, HttpResponse httpResponse, HttpContext httpContext) throws AuthenticationException, MalformedChallengeException {
        AuthScheme authScheme = authState.getAuthScheme();
        if (authScheme == null) {
            authScheme = authenticationHandler.selectScheme(map, httpResponse, httpContext);
            authState.setAuthScheme(authScheme);
        }
        String schemeName = authScheme.getSchemeName();
        Header header = map.get(schemeName.toLowerCase(Locale.ENGLISH));
        if (header == null) {
            throw new AuthenticationException(schemeName + " authorization challenge expected, but not found");
        }
        authScheme.processChallenge(header);
        this.log.debug("Authorization challenge processed");
    }

    private void updateAuthState(AuthState authState, HttpHost httpHost, CredentialsProvider credentialsProvider) {
        if (!authState.isValid()) {
            return;
        }
        String hostName = httpHost.getHostName();
        int port = httpHost.getPort();
        if (port < 0) {
            port = this.connManager.getSchemeRegistry().getScheme(httpHost).getDefaultPort();
        }
        AuthScheme authScheme = authState.getAuthScheme();
        AuthScope authScope = new AuthScope(hostName, port, authScheme.getRealm(), authScheme.getSchemeName());
        if (this.log.isDebugEnabled()) {
            this.log.debug("Authentication scope: " + authScope);
        }
        Credentials credentials = authState.getCredentials();
        if (credentials == null) {
            credentials = credentialsProvider.getCredentials(authScope);
            if (this.log.isDebugEnabled()) {
                if (credentials != null) {
                    this.log.debug("Found credentials");
                } else {
                    this.log.debug("Credentials not found");
                }
            }
        } else if (authScheme.isComplete()) {
            this.log.debug("Authentication failed");
            credentials = null;
        }
        authState.setAuthScope(authScope);
        authState.setCredentials(credentials);
    }

    private static boolean isCleartextTrafficPermitted(String str) {
        Object obj;
        Method method;
        try {
            synchronized (DefaultRequestDirector.class) {
                if (cleartextTrafficPermittedMethod == null) {
                    Class<?> cls = Class.forName("android.security.NetworkSecurityPolicy");
                    networkSecurityPolicy = cls.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
                    cleartextTrafficPermittedMethod = cls.getMethod("isCleartextTrafficPermitted", String.class);
                }
                obj = networkSecurityPolicy;
                method = cleartextTrafficPermittedMethod;
            }
            return ((Boolean) method.invoke(obj, str)).booleanValue();
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }
}
