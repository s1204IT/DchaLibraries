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
import org.apache.http.ProtocolVersion;
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
import org.apache.http.conn.routing.HttpRouteDirector;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.scheme.Scheme;
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

    public DefaultRequestDirector(HttpRequestExecutor requestExec, ClientConnectionManager conman, ConnectionReuseStrategy reustrat, ConnectionKeepAliveStrategy kastrat, HttpRoutePlanner rouplan, HttpProcessor httpProcessor, HttpRequestRetryHandler retryHandler, RedirectHandler redirectHandler, AuthenticationHandler targetAuthHandler, AuthenticationHandler proxyAuthHandler, UserTokenHandler userTokenHandler, HttpParams params) {
        if (requestExec == null) {
            throw new IllegalArgumentException("Request executor may not be null.");
        }
        if (conman == null) {
            throw new IllegalArgumentException("Client connection manager may not be null.");
        }
        if (reustrat == null) {
            throw new IllegalArgumentException("Connection reuse strategy may not be null.");
        }
        if (kastrat == null) {
            throw new IllegalArgumentException("Connection keep alive strategy may not be null.");
        }
        if (rouplan == null) {
            throw new IllegalArgumentException("Route planner may not be null.");
        }
        if (httpProcessor == null) {
            throw new IllegalArgumentException("HTTP protocol processor may not be null.");
        }
        if (retryHandler == null) {
            throw new IllegalArgumentException("HTTP request retry handler may not be null.");
        }
        if (redirectHandler == null) {
            throw new IllegalArgumentException("Redirect handler may not be null.");
        }
        if (targetAuthHandler == null) {
            throw new IllegalArgumentException("Target authentication handler may not be null.");
        }
        if (proxyAuthHandler == null) {
            throw new IllegalArgumentException("Proxy authentication handler may not be null.");
        }
        if (userTokenHandler == null) {
            throw new IllegalArgumentException("User token handler may not be null.");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.requestExec = requestExec;
        this.connManager = conman;
        this.reuseStrategy = reustrat;
        this.keepAliveStrategy = kastrat;
        this.routePlanner = rouplan;
        this.httpProcessor = httpProcessor;
        this.retryHandler = retryHandler;
        this.redirectHandler = redirectHandler;
        this.targetAuthHandler = targetAuthHandler;
        this.proxyAuthHandler = proxyAuthHandler;
        this.userTokenHandler = userTokenHandler;
        this.params = params;
        this.managedConn = null;
        this.redirectCount = 0;
        this.maxRedirects = this.params.getIntParameter(ClientPNames.MAX_REDIRECTS, 100);
        this.targetAuthState = new AuthState();
        this.proxyAuthState = new AuthState();
    }

    private RequestWrapper wrapRequest(HttpRequest request) throws ProtocolException {
        if (request instanceof HttpEntityEnclosingRequest) {
            return new EntityEnclosingRequestWrapper((HttpEntityEnclosingRequest) request);
        }
        return new RequestWrapper(request);
    }

    protected void rewriteRequestURI(RequestWrapper request, HttpRoute route) throws ProtocolException {
        try {
            URI uri = request.getURI();
            if (route.getProxyHost() != null && !route.isTunnelled()) {
                if (uri.isAbsolute()) {
                    return;
                }
                HttpHost target = route.getTargetHost();
                request.setURI(URIUtils.rewriteURI(uri, target));
                return;
            }
            if (!uri.isAbsolute()) {
                return;
            }
            request.setURI(URIUtils.rewriteURI(uri, null));
        } catch (URISyntaxException ex) {
            throw new ProtocolException("Invalid URI: " + request.getRequestLine().getUri(), ex);
        }
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException, IOException {
        RequestWrapper origWrapper = wrapRequest(request);
        origWrapper.setParams(this.params);
        HttpRoute origRoute = determineRoute(target, origWrapper, context);
        RoutedRequest roureq = new RoutedRequest(origWrapper, origRoute);
        long timeout = ConnManagerParams.getTimeout(this.params);
        int execCount = 0;
        boolean reuse = false;
        HttpResponse response = null;
        boolean done = false;
        while (!done) {
            try {
                try {
                    RequestWrapper wrapper = roureq.getRequest();
                    HttpRoute route = roureq.getRoute();
                    Object userToken = context.getAttribute(ClientContext.USER_TOKEN);
                    if (this.managedConn == null) {
                        ClientConnectionRequest connRequest = this.connManager.requestConnection(route, userToken);
                        if (request instanceof AbortableHttpRequest) {
                            ((AbortableHttpRequest) request).setConnectionRequest(connRequest);
                        }
                        try {
                            this.managedConn = connRequest.getConnection(timeout, TimeUnit.MILLISECONDS);
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
                        } catch (InterruptedException interrupted) {
                            InterruptedIOException iox = new InterruptedIOException();
                            iox.initCause(interrupted);
                            throw iox;
                        }
                    }
                    if (request instanceof AbortableHttpRequest) {
                        ((AbortableHttpRequest) request).setReleaseTrigger(this.managedConn);
                    }
                    if (!this.managedConn.isOpen()) {
                        this.managedConn.open(route, context, this.params);
                    } else {
                        this.managedConn.setSocketTimeout(HttpConnectionParams.getSoTimeout(this.params));
                    }
                    try {
                        establishRoute(route, context);
                        wrapper.resetHeaders();
                        rewriteRequestURI(wrapper, route);
                        HttpHost target2 = (HttpHost) wrapper.getParams().getParameter(ClientPNames.VIRTUAL_HOST);
                        if (target2 == null) {
                            target2 = route.getTargetHost();
                        }
                        HttpHost proxy = route.getProxyHost();
                        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, target2);
                        context.setAttribute(ExecutionContext.HTTP_PROXY_HOST, proxy);
                        context.setAttribute(ExecutionContext.HTTP_CONNECTION, this.managedConn);
                        context.setAttribute(ClientContext.TARGET_AUTH_STATE, this.targetAuthState);
                        context.setAttribute(ClientContext.PROXY_AUTH_STATE, this.proxyAuthState);
                        this.requestExec.preProcess(wrapper, this.httpProcessor, context);
                        context.setAttribute(ExecutionContext.HTTP_REQUEST, wrapper);
                        boolean retrying = true;
                        while (retrying) {
                            execCount++;
                            wrapper.incrementExecCount();
                            if (wrapper.getExecCount() > 1 && !wrapper.isRepeatable()) {
                                throw new NonRepeatableRequestException("Cannot retry request with a non-repeatable request entity");
                            }
                            try {
                                if (this.log.isDebugEnabled()) {
                                    this.log.debug("Attempt " + execCount + " to execute request");
                                }
                                if (!route.isSecure() && !isCleartextTrafficPermitted(route.getTargetHost().getHostName())) {
                                    throw new IOException("Cleartext traffic not permitted: " + route.getTargetHost());
                                }
                                response = this.requestExec.execute(wrapper, this.managedConn, context);
                                retrying = false;
                            } catch (IOException ex) {
                                this.log.debug("Closing the connection.");
                                this.managedConn.close();
                                if (this.retryHandler.retryRequest(ex, execCount, context)) {
                                    if (this.log.isInfoEnabled()) {
                                        this.log.info("I/O exception (" + ex.getClass().getName() + ") caught when processing request: " + ex.getMessage());
                                    }
                                    if (this.log.isDebugEnabled()) {
                                        this.log.debug(ex.getMessage(), ex);
                                    }
                                    this.log.info("Retrying request");
                                    if (route.getHopCount() == 1) {
                                        this.log.debug("Reopening the direct connection.");
                                        this.managedConn.open(route, context, this.params);
                                    } else {
                                        throw ex;
                                    }
                                } else {
                                    throw ex;
                                }
                            }
                        }
                        response.setParams(this.params);
                        this.requestExec.postProcess(response, this.httpProcessor, context);
                        reuse = this.reuseStrategy.keepAlive(response, context);
                        if (reuse) {
                            long duration = this.keepAliveStrategy.getKeepAliveDuration(response, context);
                            this.managedConn.setIdleDuration(duration, TimeUnit.MILLISECONDS);
                        }
                        RoutedRequest followup = handleResponse(roureq, response, context);
                        if (followup == null) {
                            done = true;
                        } else {
                            if (reuse) {
                                this.log.debug("Connection kept alive");
                                HttpEntity entity = response.getEntity();
                                if (entity != null) {
                                    entity.consumeContent();
                                }
                                this.managedConn.markReusable();
                            } else {
                                this.managedConn.close();
                            }
                            if (!followup.getRoute().equals(roureq.getRoute())) {
                                releaseConnection();
                            }
                            roureq = followup;
                        }
                        Object userToken2 = this.userTokenHandler.getUserToken(context);
                        context.setAttribute(ClientContext.USER_TOKEN, userToken2);
                        if (this.managedConn != null) {
                            this.managedConn.setState(userToken2);
                        }
                    } catch (TunnelRefusedException ex2) {
                        if (this.log.isDebugEnabled()) {
                            this.log.debug(ex2.getMessage());
                        }
                        response = ex2.getResponse();
                    }
                } catch (IOException ex3) {
                    abortConnection();
                    throw ex3;
                }
            } catch (RuntimeException ex4) {
                abortConnection();
                throw ex4;
            } catch (HttpException ex5) {
                abortConnection();
                throw ex5;
            }
        }
        if (response == null || response.getEntity() == null || !response.getEntity().isStreaming()) {
            if (reuse) {
                this.managedConn.markReusable();
            }
            releaseConnection();
        } else {
            response.setEntity(new BasicManagedEntity(response.getEntity(), this.managedConn, reuse));
        }
        return response;
    }

    protected void releaseConnection() {
        try {
            this.managedConn.releaseConnection();
        } catch (IOException ignored) {
            this.log.debug("IOException releasing connection", ignored);
        }
        this.managedConn = null;
    }

    protected HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        URI uri;
        if (target == null) {
            target = (HttpHost) request.getParams().getParameter(ClientPNames.DEFAULT_HOST);
        }
        if (target == null) {
            String scheme = null;
            String host = null;
            String path = null;
            if ((request instanceof HttpUriRequest) && (uri = ((HttpUriRequest) request).getURI()) != null) {
                scheme = uri.getScheme();
                host = uri.getHost();
                path = uri.getPath();
            }
            throw new IllegalStateException("Target host must not be null, or set in parameters. scheme=" + scheme + ", host=" + host + ", path=" + path);
        }
        return this.routePlanner.determineRoute(target, request, context);
    }

    protected void establishRoute(HttpRoute route, HttpContext context) throws HttpException, IOException {
        int step;
        HttpRouteDirector rowdy = new BasicRouteDirector();
        do {
            HttpRoute fact = this.managedConn.getRoute();
            step = rowdy.nextStep(route, fact);
            switch (step) {
                case -1:
                    throw new IllegalStateException("Unable to establish route.\nplanned = " + route + "\ncurrent = " + fact);
                case 0:
                    break;
                case 1:
                case 2:
                    this.managedConn.open(route, context, this.params);
                    break;
                case 3:
                    boolean secure = createTunnelToTarget(route, context);
                    this.log.debug("Tunnel to target created.");
                    this.managedConn.tunnelTarget(secure, this.params);
                    break;
                case 4:
                    int hop = fact.getHopCount() - 1;
                    boolean secure2 = createTunnelToProxy(route, hop, context);
                    this.log.debug("Tunnel to proxy created.");
                    this.managedConn.tunnelProxy(route.getHopTarget(hop), secure2, this.params);
                    break;
                case 5:
                    this.managedConn.layerProtocol(context, this.params);
                    break;
                default:
                    throw new IllegalStateException("Unknown step indicator " + step + " from RouteDirector.");
            }
        } while (step > 0);
    }

    protected boolean createTunnelToTarget(HttpRoute route, HttpContext context) throws HttpException, IOException {
        int status;
        HttpHost proxy = route.getProxyHost();
        HttpHost target = route.getTargetHost();
        HttpResponse response = null;
        boolean done = false;
        while (true) {
            if (done) {
                break;
            }
            done = true;
            if (!this.managedConn.isOpen()) {
                this.managedConn.open(route, context, this.params);
            }
            HttpRequest connect = createConnectRequest(route, context);
            String agent = HttpProtocolParams.getUserAgent(this.params);
            if (agent != null) {
                connect.addHeader(HTTP.USER_AGENT, agent);
            }
            connect.addHeader(HTTP.TARGET_HOST, target.toHostString());
            AuthScheme authScheme = this.proxyAuthState.getAuthScheme();
            AuthScope authScope = this.proxyAuthState.getAuthScope();
            Credentials creds = this.proxyAuthState.getCredentials();
            if (creds != null && (authScope != null || !authScheme.isConnectionBased())) {
                try {
                    connect.addHeader(authScheme.authenticate(creds, connect));
                } catch (AuthenticationException ex) {
                    if (this.log.isErrorEnabled()) {
                        this.log.error("Proxy authentication error: " + ex.getMessage());
                    }
                }
            }
            response = this.requestExec.execute(connect, this.managedConn, context);
            int status2 = response.getStatusLine().getStatusCode();
            if (status2 < 200) {
                throw new HttpException("Unexpected response to CONNECT request: " + response.getStatusLine());
            }
            CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
            if (credsProvider != null && HttpClientParams.isAuthenticating(this.params)) {
                if (this.proxyAuthHandler.isAuthenticationRequested(response, context)) {
                    this.log.debug("Proxy requested authentication");
                    Map<String, Header> challenges = this.proxyAuthHandler.getChallenges(response, context);
                    try {
                        processChallenges(challenges, this.proxyAuthState, this.proxyAuthHandler, response, context);
                    } catch (AuthenticationException ex2) {
                        if (this.log.isWarnEnabled()) {
                            this.log.warn("Authentication error: " + ex2.getMessage());
                            status = response.getStatusLine().getStatusCode();
                            if (status <= 299) {
                            }
                        }
                    }
                    updateAuthState(this.proxyAuthState, proxy, credsProvider);
                    if (this.proxyAuthState.getCredentials() != null) {
                        done = false;
                        if (this.reuseStrategy.keepAlive(response, context)) {
                            this.log.debug("Connection kept alive");
                            HttpEntity entity = response.getEntity();
                            if (entity != null) {
                                entity.consumeContent();
                            }
                        } else {
                            this.managedConn.close();
                        }
                    }
                } else {
                    this.proxyAuthState.setAuthScope(null);
                }
            }
        }
        status = response.getStatusLine().getStatusCode();
        if (status <= 299) {
            HttpEntity entity2 = response.getEntity();
            if (entity2 != null) {
                response.setEntity(new BufferedHttpEntity(entity2));
            }
            this.managedConn.close();
            throw new TunnelRefusedException("CONNECT refused by proxy: " + response.getStatusLine(), response);
        }
        this.managedConn.markReusable();
        return false;
    }

    protected boolean createTunnelToProxy(HttpRoute route, int hop, HttpContext context) throws HttpException, IOException {
        throw new UnsupportedOperationException("Proxy chains are not supported.");
    }

    protected HttpRequest createConnectRequest(HttpRoute route, HttpContext context) {
        HttpHost target = route.getTargetHost();
        String host = target.getHostName();
        int port = target.getPort();
        if (port < 0) {
            Scheme scheme = this.connManager.getSchemeRegistry().getScheme(target.getSchemeName());
            port = scheme.getDefaultPort();
        }
        StringBuilder buffer = new StringBuilder(host.length() + 6);
        buffer.append(host);
        buffer.append(':');
        buffer.append(Integer.toString(port));
        String authority = buffer.toString();
        ProtocolVersion ver = HttpProtocolParams.getVersion(this.params);
        HttpRequest req = new BasicHttpRequest("CONNECT", authority, ver);
        return req;
    }

    protected RoutedRequest handleResponse(RoutedRequest roureq, HttpResponse response, HttpContext context) throws HttpException, IOException {
        HttpRoute route = roureq.getRoute();
        HttpHost proxy = route.getProxyHost();
        RequestWrapper request = roureq.getRequest();
        HttpParams params = request.getParams();
        if (HttpClientParams.isRedirecting(params) && this.redirectHandler.isRedirectRequested(response, context)) {
            if (this.redirectCount >= this.maxRedirects) {
                throw new RedirectException("Maximum redirects (" + this.maxRedirects + ") exceeded");
            }
            this.redirectCount++;
            URI uri = this.redirectHandler.getLocationURI(response, context);
            HttpHost newTarget = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
            HttpGet redirect = new HttpGet(uri);
            HttpRequest orig = request.getOriginal();
            redirect.setHeaders(orig.getAllHeaders());
            RequestWrapper wrapper = new RequestWrapper(redirect);
            wrapper.setParams(params);
            HttpRoute newRoute = determineRoute(newTarget, wrapper, context);
            RoutedRequest newRequest = new RoutedRequest(wrapper, newRoute);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Redirecting to '" + uri + "' via " + newRoute);
            }
            return newRequest;
        }
        CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
        if (credsProvider != null && HttpClientParams.isAuthenticating(params)) {
            if (this.targetAuthHandler.isAuthenticationRequested(response, context)) {
                HttpHost target = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (target == null) {
                    target = route.getTargetHost();
                }
                this.log.debug("Target requested authentication");
                Map<String, Header> challenges = this.targetAuthHandler.getChallenges(response, context);
                try {
                    processChallenges(challenges, this.targetAuthState, this.targetAuthHandler, response, context);
                } catch (AuthenticationException ex) {
                    if (this.log.isWarnEnabled()) {
                        this.log.warn("Authentication error: " + ex.getMessage());
                        return null;
                    }
                }
                updateAuthState(this.targetAuthState, target, credsProvider);
                if (this.targetAuthState.getCredentials() != null) {
                    return roureq;
                }
                return null;
            }
            this.targetAuthState.setAuthScope(null);
            if (this.proxyAuthHandler.isAuthenticationRequested(response, context)) {
                this.log.debug("Proxy requested authentication");
                Map<String, Header> challenges2 = this.proxyAuthHandler.getChallenges(response, context);
                try {
                    processChallenges(challenges2, this.proxyAuthState, this.proxyAuthHandler, response, context);
                } catch (AuthenticationException ex2) {
                    if (this.log.isWarnEnabled()) {
                        this.log.warn("Authentication error: " + ex2.getMessage());
                        return null;
                    }
                }
                updateAuthState(this.proxyAuthState, proxy, credsProvider);
                if (this.proxyAuthState.getCredentials() != null) {
                    return roureq;
                }
                return null;
            }
            this.proxyAuthState.setAuthScope(null);
            return null;
        }
        return null;
    }

    private void abortConnection() {
        ManagedClientConnection mcc = this.managedConn;
        if (mcc == null) {
            return;
        }
        this.managedConn = null;
        try {
            mcc.abortConnection();
        } catch (IOException ex) {
            if (this.log.isDebugEnabled()) {
                this.log.debug(ex.getMessage(), ex);
            }
        }
        try {
            mcc.releaseConnection();
        } catch (IOException ignored) {
            this.log.debug("Error releasing connection", ignored);
        }
    }

    private void processChallenges(Map<String, Header> challenges, AuthState authState, AuthenticationHandler authHandler, HttpResponse response, HttpContext context) throws AuthenticationException, MalformedChallengeException {
        AuthScheme authScheme = authState.getAuthScheme();
        if (authScheme == null) {
            authScheme = authHandler.selectScheme(challenges, response, context);
            authState.setAuthScheme(authScheme);
        }
        String id = authScheme.getSchemeName();
        Header challenge = challenges.get(id.toLowerCase(Locale.ENGLISH));
        if (challenge == null) {
            throw new AuthenticationException(id + " authorization challenge expected, but not found");
        }
        authScheme.processChallenge(challenge);
        this.log.debug("Authorization challenge processed");
    }

    private void updateAuthState(AuthState authState, HttpHost host, CredentialsProvider credsProvider) {
        if (!authState.isValid()) {
            return;
        }
        String hostname = host.getHostName();
        int port = host.getPort();
        if (port < 0) {
            Scheme scheme = this.connManager.getSchemeRegistry().getScheme(host);
            port = scheme.getDefaultPort();
        }
        AuthScheme authScheme = authState.getAuthScheme();
        AuthScope authScope = new AuthScope(hostname, port, authScheme.getRealm(), authScheme.getSchemeName());
        if (this.log.isDebugEnabled()) {
            this.log.debug("Authentication scope: " + authScope);
        }
        Credentials creds = authState.getCredentials();
        if (creds == null) {
            creds = credsProvider.getCredentials(authScope);
            if (this.log.isDebugEnabled()) {
                if (creds != null) {
                    this.log.debug("Found credentials");
                } else {
                    this.log.debug("Credentials not found");
                }
            }
        } else if (authScheme.isComplete()) {
            this.log.debug("Authentication failed");
            creds = null;
        }
        authState.setAuthScope(authScope);
        authState.setCredentials(creds);
    }

    private static boolean isCleartextTrafficPermitted(String hostname) {
        Object policy;
        Method method;
        try {
            synchronized (DefaultRequestDirector.class) {
                if (cleartextTrafficPermittedMethod == null) {
                    Class<?> cls = Class.forName("android.security.NetworkSecurityPolicy");
                    Method getInstanceMethod = cls.getMethod("getInstance", new Class[0]);
                    networkSecurityPolicy = getInstanceMethod.invoke(null, new Object[0]);
                    cleartextTrafficPermittedMethod = cls.getMethod("isCleartextTrafficPermitted", String.class);
                }
                policy = networkSecurityPolicy;
                method = cleartextTrafficPermittedMethod;
            }
            return ((Boolean) method.invoke(policy, hostname)).booleanValue();
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }
}
