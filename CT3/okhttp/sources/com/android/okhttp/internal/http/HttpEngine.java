package com.android.okhttp.internal.http;

import com.android.okhttp.Address;
import com.android.okhttp.CertificatePinner;
import com.android.okhttp.Connection;
import com.android.okhttp.ConnectionPool;
import com.android.okhttp.Headers;
import com.android.okhttp.HttpUrl;
import com.android.okhttp.Interceptor;
import com.android.okhttp.MediaType;
import com.android.okhttp.OkHttpClient;
import com.android.okhttp.Protocol;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.ResponseBody;
import com.android.okhttp.Route;
import com.android.okhttp.internal.Internal;
import com.android.okhttp.internal.InternalCache;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.Version;
import com.android.okhttp.internal.http.CacheStrategy;
import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.BufferedSource;
import com.android.okhttp.okio.GzipSource;
import com.android.okhttp.okio.Okio;
import com.android.okhttp.okio.Sink;
import com.android.okhttp.okio.Source;
import com.android.okhttp.okio.Timeout;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

public final class HttpEngine {
    private static final ResponseBody EMPTY_BODY = new ResponseBody() {
        @Override
        public MediaType contentType() {
            return null;
        }

        @Override
        public long contentLength() {
            return 0L;
        }

        @Override
        public BufferedSource source() {
            return new Buffer();
        }
    };
    public static final int MAX_AUTHENTICTORS = 10;
    public static final int MAX_FOLLOW_UPS = 20;
    private static Method enforceCheckPermissionMethod;
    private Address address;
    private int authenticateCount;
    public final boolean bufferRequestBody;
    private BufferedSink bufferedRequestBody;
    private Response cacheResponse;
    private CacheStrategy cacheStrategy;
    private final boolean callerWritesRequestBody;
    final OkHttpClient client;
    private Connection connection;
    private final boolean forWebSocket;
    private Request networkRequest;
    private final Response priorResponse;
    private Sink requestBodyOut;
    private Route route;
    private RouteSelector routeSelector;
    private CacheRequest storeRequest;
    private boolean transparentGzip;
    private Transport transport;
    private final Request userRequest;
    private Response userResponse;
    long sentRequestMillis = -1;
    private boolean momsPermitted = true;

    public HttpEngine(OkHttpClient client, Request request, boolean bufferRequestBody, boolean callerWritesRequestBody, boolean forWebSocket, Connection connection, RouteSelector routeSelector, RetryableSink requestBodyOut, Response priorResponse) {
        this.client = client;
        this.userRequest = request;
        this.bufferRequestBody = bufferRequestBody;
        this.callerWritesRequestBody = callerWritesRequestBody;
        this.forWebSocket = forWebSocket;
        this.connection = connection;
        this.routeSelector = routeSelector;
        this.requestBodyOut = requestBodyOut;
        this.priorResponse = priorResponse;
        if (connection != null) {
            Internal.instance.setOwner(connection, this);
            this.route = connection.getRoute();
        } else {
            this.route = null;
        }
    }

    public void sendRequest() throws RouteException, RequestException, IOException {
        if (this.cacheStrategy != null) {
            return;
        }
        if (this.transport != null) {
            throw new IllegalStateException();
        }
        Request request = networkRequest(this.userRequest);
        InternalCache responseCache = Internal.instance.internalCache(this.client);
        Response response = responseCache != null ? responseCache.get(request) : null;
        long now = System.currentTimeMillis();
        this.cacheStrategy = new CacheStrategy.Factory(now, request, response).get();
        this.networkRequest = this.cacheStrategy.networkRequest;
        this.cacheResponse = this.cacheStrategy.cacheResponse;
        if (responseCache != null) {
            responseCache.trackResponse(this.cacheStrategy);
        }
        if (response != null && this.cacheResponse == null) {
            Util.closeQuietly(response.body());
        }
        if (this.networkRequest == null) {
            if (this.connection != null) {
                Internal.instance.recycle(this.client.getConnectionPool(), this.connection);
                this.connection = null;
            }
            if (this.cacheResponse != null) {
                this.userResponse = this.cacheResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).cacheResponse(stripBody(this.cacheResponse)).build();
            } else {
                this.userResponse = new Response.Builder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).protocol(Protocol.HTTP_1_1).code(504).message("Unsatisfiable Request (only-if-cached)").body(EMPTY_BODY).build();
            }
            this.userResponse = unzip(this.userResponse);
            return;
        }
        if (!isMmsAndEmailSendingPermitted(this.userRequest)) {
            this.momsPermitted = false;
            System.out.println("isMmsAndEmailSendingPermitted ? no");
            return;
        }
        if (this.connection == null) {
            connect();
        }
        this.transport = Internal.instance.newTransport(this.connection, this);
        if (this.callerWritesRequestBody && permitsRequestBody() && this.requestBodyOut == null) {
            long contentLength = OkHeaders.contentLength(request);
            if (!this.bufferRequestBody) {
                this.transport.writeRequestHeaders(this.networkRequest);
                this.requestBodyOut = this.transport.createRequestBody(this.networkRequest, contentLength);
            } else {
                if (contentLength > 2147483647L) {
                    throw new IllegalStateException("Use setFixedLengthStreamingMode() or setChunkedStreamingMode() for requests larger than 2 GiB.");
                }
                if (contentLength == -1) {
                    this.requestBodyOut = new RetryableSink();
                } else {
                    this.transport.writeRequestHeaders(this.networkRequest);
                    this.requestBodyOut = new RetryableSink((int) contentLength);
                }
            }
        }
    }

    private static Response stripBody(Response response) {
        if (response == null || response.body() == null) {
            return response;
        }
        return response.newBuilder().body(null).build();
    }

    private void connect() throws RouteException, RequestException {
        if (this.connection != null) {
            throw new IllegalStateException();
        }
        if (this.routeSelector == null) {
            this.address = createAddress(this.client, this.networkRequest);
            try {
                this.routeSelector = RouteSelector.get(this.address, this.networkRequest, this.client);
            } catch (IOException e) {
                throw new RequestException(e);
            }
        }
        this.connection = createNextConnection();
        Internal.instance.connectAndSetOwner(this.client, this.connection, this, this.networkRequest);
        this.route = this.connection.getRoute();
    }

    private Connection createNextConnection() throws RouteException {
        Connection pooled;
        ConnectionPool pool = this.client.getConnectionPool();
        while (true) {
            pooled = pool.get(this.address);
            if (pooled != null) {
                if (this.networkRequest.method().equals("GET") || Internal.instance.isReadable(pooled)) {
                    break;
                }
                Util.closeQuietly(pooled.getSocket());
            } else {
                try {
                    Route route = this.routeSelector.next();
                    return new Connection(pool, route);
                } catch (IOException e) {
                    throw new RouteException(e);
                }
            }
        }
        return pooled;
    }

    public void writingRequestHeaders() {
        if (this.sentRequestMillis != -1) {
            throw new IllegalStateException();
        }
        this.sentRequestMillis = System.currentTimeMillis();
    }

    boolean permitsRequestBody() {
        return HttpMethod.permitsRequestBody(this.userRequest.method());
    }

    public Sink getRequestBody() {
        if (this.cacheStrategy == null) {
            throw new IllegalStateException();
        }
        return this.requestBodyOut;
    }

    public BufferedSink getBufferedRequestBody() {
        BufferedSink result = this.bufferedRequestBody;
        if (result != null) {
            return result;
        }
        Sink requestBody = getRequestBody();
        if (requestBody == null) {
            return null;
        }
        BufferedSink bufferedSinkBuffer = Okio.buffer(requestBody);
        this.bufferedRequestBody = bufferedSinkBuffer;
        return bufferedSinkBuffer;
    }

    public boolean hasResponse() {
        return this.userResponse != null;
    }

    public Request getRequest() {
        return this.userRequest;
    }

    public Response getResponse() {
        if (this.userResponse == null) {
            throw new IllegalStateException();
        }
        return this.userResponse;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public HttpEngine recover(RouteException e) {
        if (this.routeSelector != null && this.connection != null) {
            connectFailed(this.routeSelector, e.getLastConnectException());
        }
        if ((this.routeSelector == null && this.connection == null) || ((this.routeSelector != null && !this.routeSelector.hasNext()) || !isRecoverable(e))) {
            return null;
        }
        Connection connection = close();
        return new HttpEngine(this.client, this.userRequest, this.bufferRequestBody, this.callerWritesRequestBody, this.forWebSocket, connection, this.routeSelector, (RetryableSink) this.requestBodyOut, this.priorResponse);
    }

    private boolean isRecoverable(RouteException e) {
        if (!this.client.getRetryOnConnectionFailure()) {
            return false;
        }
        IOException ioe = e.getLastConnectException();
        if (ioe instanceof ProtocolException) {
            return false;
        }
        if (ioe instanceof InterruptedIOException) {
            return ioe instanceof SocketTimeoutException;
        }
        return (((ioe instanceof SSLHandshakeException) && (ioe.getCause() instanceof CertificateException)) || (ioe instanceof SSLPeerUnverifiedException)) ? false : true;
    }

    public HttpEngine recover(IOException e, Sink requestBodyOut) {
        if (this.routeSelector != null && this.connection != null) {
            connectFailed(this.routeSelector, e);
        }
        boolean z = requestBodyOut != null ? requestBodyOut instanceof RetryableSink : true;
        if ((this.routeSelector == null && this.connection == null) || ((this.routeSelector != null && !this.routeSelector.hasNext()) || !isRecoverable(e) || !z)) {
            return null;
        }
        Connection connection = close();
        return new HttpEngine(this.client, this.userRequest, this.bufferRequestBody, this.callerWritesRequestBody, this.forWebSocket, connection, this.routeSelector, (RetryableSink) requestBodyOut, this.priorResponse);
    }

    private void connectFailed(RouteSelector routeSelector, IOException e) {
        if (Internal.instance.recycleCount(this.connection) > 0) {
            return;
        }
        Route failedRoute = this.connection.getRoute();
        routeSelector.connectFailed(failedRoute, e);
    }

    public HttpEngine recover(IOException e) {
        return recover(e, this.requestBodyOut);
    }

    private boolean isRecoverable(IOException e) {
        return (!this.client.getRetryOnConnectionFailure() || (e instanceof ProtocolException) || (e instanceof InterruptedIOException)) ? false : true;
    }

    public Route getRoute() {
        return this.route;
    }

    private void maybeCache() throws IOException {
        InternalCache responseCache = Internal.instance.internalCache(this.client);
        if (responseCache == null) {
            return;
        }
        if (!CacheStrategy.isCacheable(this.userResponse, this.networkRequest)) {
            if (HttpMethod.invalidatesCache(this.networkRequest.method())) {
                try {
                    responseCache.remove(this.networkRequest);
                    return;
                } catch (IOException e) {
                    return;
                }
            }
            return;
        }
        this.storeRequest = responseCache.put(stripBody(this.userResponse));
    }

    public void releaseConnection() throws IOException {
        if (this.transport != null && this.connection != null) {
            this.transport.releaseConnectionOnIdle();
        }
        this.connection = null;
    }

    public void disconnect() {
        try {
            if (this.transport != null) {
                this.transport.disconnect(this);
            } else {
                Connection connection = this.connection;
                if (connection != null) {
                    Internal.instance.closeIfOwnedBy(connection, this);
                }
            }
        } catch (IOException e) {
        }
    }

    public Connection close() {
        if (this.bufferedRequestBody != null) {
            Util.closeQuietly(this.bufferedRequestBody);
        } else if (this.requestBodyOut != null) {
            Util.closeQuietly(this.requestBodyOut);
        }
        if (this.userResponse == null) {
            if (this.connection != null) {
                Util.closeQuietly(this.connection.getSocket());
            }
            this.connection = null;
            return null;
        }
        Util.closeQuietly(this.userResponse.body());
        if (this.transport != null && this.connection != null && !this.transport.canReuseConnection()) {
            Util.closeQuietly(this.connection.getSocket());
            this.connection = null;
            return null;
        }
        if (this.connection != null && !Internal.instance.clearOwner(this.connection)) {
            this.connection = null;
        }
        Connection result = this.connection;
        this.connection = null;
        return result;
    }

    private Response unzip(Response response) throws IOException {
        if (!this.transparentGzip || !"gzip".equalsIgnoreCase(this.userResponse.header("Content-Encoding")) || response.body() == null) {
            return response;
        }
        GzipSource responseBody = new GzipSource(response.body().source());
        Headers strippedHeaders = response.headers().newBuilder().removeAll("Content-Encoding").removeAll("Content-Length").build();
        return response.newBuilder().headers(strippedHeaders).body(new RealResponseBody(strippedHeaders, Okio.buffer(responseBody))).build();
    }

    public static boolean hasBody(Response response) {
        if (response.request().method().equals("HEAD")) {
            return false;
        }
        int responseCode = response.code();
        return (((responseCode >= 100 && responseCode < 200) || responseCode == 204 || responseCode == 304) && OkHeaders.contentLength(response) == -1 && !"chunked".equalsIgnoreCase(response.header("Transfer-Encoding"))) ? false : true;
    }

    private Request networkRequest(Request request) throws IOException {
        Request.Builder result = request.newBuilder();
        if (request.header("Host") == null) {
            result.header("Host", Util.hostHeader(request.httpUrl()));
        }
        if ((this.connection == null || this.connection.getProtocol() != Protocol.HTTP_1_0) && request.header("Connection") == null) {
            result.header("Connection", "Keep-Alive");
        }
        if (request.header("Accept-Encoding") == null && !"true".equals(System.getProperty("xcap.req"))) {
            this.transparentGzip = true;
            result.header("Accept-Encoding", "gzip");
        }
        CookieHandler cookieHandler = this.client.getCookieHandler();
        if (cookieHandler != null) {
            Map<String, List<String>> headers = OkHeaders.toMultimap(result.build().headers(), null);
            Map<String, List<String>> cookies = cookieHandler.get(request.uri(), headers);
            OkHeaders.addCookies(result, cookies);
        }
        if (request.header("User-Agent") == null) {
            result.header("User-Agent", Version.userAgent());
        }
        return result.build();
    }

    public void readResponse() throws IOException {
        Response networkResponse;
        if (this.userResponse != null) {
            return;
        }
        if (this.networkRequest == null && this.cacheResponse == null) {
            throw new IllegalStateException("call sendRequest() first!");
        }
        if (this.networkRequest == null) {
            return;
        }
        if (!this.momsPermitted) {
            this.userResponse = getBadHttpResponse();
            System.out.println("Mms or Email Sending is not Permitted");
            return;
        }
        if (this.forWebSocket) {
            this.transport.writeRequestHeaders(this.networkRequest);
            networkResponse = readNetworkResponse();
        } else if (this.callerWritesRequestBody) {
            if (this.bufferedRequestBody != null && this.bufferedRequestBody.buffer().size() > 0) {
                this.bufferedRequestBody.emit();
            }
            if (this.sentRequestMillis == -1) {
                if (OkHeaders.contentLength(this.networkRequest) == -1 && (this.requestBodyOut instanceof RetryableSink)) {
                    long contentLength = ((RetryableSink) this.requestBodyOut).contentLength();
                    this.networkRequest = this.networkRequest.newBuilder().header("Content-Length", Long.toString(contentLength)).build();
                }
                System.out.println("[OkHttp] sendRequest>>");
                this.transport.writeRequestHeaders(this.networkRequest);
            }
            if (this.requestBodyOut != null) {
                if (this.bufferedRequestBody != null) {
                    this.bufferedRequestBody.close();
                } else {
                    this.requestBodyOut.close();
                }
                if (this.requestBodyOut instanceof RetryableSink) {
                    this.transport.writeRequestBody((RetryableSink) this.requestBodyOut);
                }
            }
            System.out.println("[OkHttp] sendRequest<<");
            networkResponse = readNetworkResponse();
        } else {
            networkResponse = new NetworkInterceptorChain(0, this.networkRequest).proceed(this.networkRequest);
        }
        receiveHeaders(networkResponse.headers());
        if (this.cacheResponse != null) {
            if (validate(this.cacheResponse, networkResponse)) {
                this.userResponse = this.cacheResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).headers(combine(this.cacheResponse.headers(), networkResponse.headers())).cacheResponse(stripBody(this.cacheResponse)).networkResponse(stripBody(networkResponse)).build();
                networkResponse.body().close();
                releaseConnection();
                InternalCache responseCache = Internal.instance.internalCache(this.client);
                responseCache.trackConditionalCacheHit();
                responseCache.update(this.cacheResponse, stripBody(this.userResponse));
                this.userResponse = unzip(this.userResponse);
                return;
            }
            Util.closeQuietly(this.cacheResponse.body());
        }
        this.userResponse = networkResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).cacheResponse(stripBody(this.cacheResponse)).networkResponse(stripBody(networkResponse)).build();
        if (hasBody(this.userResponse)) {
            maybeCache();
            this.userResponse = unzip(cacheWritingResponse(this.storeRequest, this.userResponse));
        }
    }

    class NetworkInterceptorChain implements Interceptor.Chain {
        private int calls;
        private final int index;
        private final Request request;

        NetworkInterceptorChain(int index, Request request) {
            this.index = index;
            this.request = request;
        }

        @Override
        public Connection connection() {
            return HttpEngine.this.connection;
        }

        @Override
        public Request request() {
            return this.request;
        }

        @Override
        public Response proceed(Request request) throws IOException {
            this.calls++;
            if (this.index > 0) {
                Interceptor caller = HttpEngine.this.client.networkInterceptors().get(this.index - 1);
                Address address = connection().getRoute().getAddress();
                if (!request.httpUrl().rfc2732host().equals(address.getRfc2732Host()) || request.httpUrl().port() != address.getUriPort()) {
                    throw new IllegalStateException("network interceptor " + caller + " must retain the same host and port");
                }
                if (this.calls > 1) {
                    throw new IllegalStateException("network interceptor " + caller + " must call proceed() exactly once");
                }
            }
            if (this.index < HttpEngine.this.client.networkInterceptors().size()) {
                NetworkInterceptorChain chain = HttpEngine.this.new NetworkInterceptorChain(this.index + 1, request);
                Interceptor interceptor = HttpEngine.this.client.networkInterceptors().get(this.index);
                Response interceptedResponse = interceptor.intercept(chain);
                if (chain.calls != 1) {
                    throw new IllegalStateException("network interceptor " + interceptor + " must call proceed() exactly once");
                }
                return interceptedResponse;
            }
            HttpEngine.this.transport.writeRequestHeaders(request);
            HttpEngine.this.networkRequest = request;
            if (HttpEngine.this.permitsRequestBody() && request.body() != null) {
                Sink requestBodyOut = HttpEngine.this.transport.createRequestBody(request, request.body().contentLength());
                BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);
                request.body().writeTo(bufferedRequestBody);
                bufferedRequestBody.close();
            }
            Response response = HttpEngine.this.readNetworkResponse();
            int code = response.code();
            if ((code == 204 || code == 205) && response.body().contentLength() > 0) {
                throw new ProtocolException("HTTP " + code + " had non-zero Content-Length: " + response.body().contentLength());
            }
            return response;
        }
    }

    private Response readNetworkResponse() throws IOException {
        this.transport.finishRequest();
        Response networkResponse = this.transport.readResponseHeaders().request(this.networkRequest).handshake(this.connection.getHandshake()).header(OkHeaders.SENT_MILLIS, Long.toString(this.sentRequestMillis)).header(OkHeaders.RECEIVED_MILLIS, Long.toString(System.currentTimeMillis())).build();
        if (!this.forWebSocket) {
            networkResponse = networkResponse.newBuilder().body(this.transport.openResponseBody(networkResponse)).build();
        }
        Internal.instance.setProtocol(this.connection, networkResponse.protocol());
        return networkResponse;
    }

    private Response cacheWritingResponse(final CacheRequest cacheRequest, Response response) throws IOException {
        Sink cacheBodyUnbuffered;
        if (cacheRequest == null || (cacheBodyUnbuffered = cacheRequest.body()) == null) {
            return response;
        }
        final BufferedSource source = response.body().source();
        final BufferedSink cacheBody = Okio.buffer(cacheBodyUnbuffered);
        Source cacheWritingSource = new Source() {
            boolean cacheRequestClosed;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                try {
                    long bytesRead = source.read(sink, byteCount);
                    if (bytesRead == -1) {
                        if (!this.cacheRequestClosed) {
                            this.cacheRequestClosed = true;
                            cacheBody.close();
                        }
                        return -1L;
                    }
                    sink.copyTo(cacheBody.buffer(), sink.size() - bytesRead, bytesRead);
                    cacheBody.emitCompleteSegments();
                    return bytesRead;
                } catch (IOException e) {
                    if (!this.cacheRequestClosed) {
                        this.cacheRequestClosed = true;
                        cacheRequest.abort();
                    }
                    throw e;
                }
            }

            @Override
            public Timeout timeout() {
                return source.timeout();
            }

            @Override
            public void close() throws IOException {
                if (!this.cacheRequestClosed && !Util.discard(this, 100, TimeUnit.MILLISECONDS)) {
                    this.cacheRequestClosed = true;
                    cacheRequest.abort();
                }
                source.close();
            }
        };
        return response.newBuilder().body(new RealResponseBody(response.headers(), Okio.buffer(cacheWritingSource))).build();
    }

    private static boolean validate(Response cached, Response network) {
        Date networkLastModified;
        if (network.code() == 304) {
            return true;
        }
        Date lastModified = cached.headers().getDate("Last-Modified");
        return (lastModified == null || (networkLastModified = network.headers().getDate("Last-Modified")) == null || networkLastModified.getTime() >= lastModified.getTime()) ? false : true;
    }

    private static Headers combine(Headers cachedHeaders, Headers networkHeaders) throws IOException {
        Headers.Builder result = new Headers.Builder();
        int size = cachedHeaders.size();
        for (int i = 0; i < size; i++) {
            String fieldName = cachedHeaders.name(i);
            String value = cachedHeaders.value(i);
            if ((!"Warning".equalsIgnoreCase(fieldName) || !value.startsWith("1")) && (!OkHeaders.isEndToEnd(fieldName) || networkHeaders.get(fieldName) == null)) {
                result.add(fieldName, value);
            }
        }
        int size2 = networkHeaders.size();
        for (int i2 = 0; i2 < size2; i2++) {
            String fieldName2 = networkHeaders.name(i2);
            if (!"Content-Length".equalsIgnoreCase(fieldName2) && OkHeaders.isEndToEnd(fieldName2)) {
                result.add(fieldName2, networkHeaders.value(i2));
            }
        }
        return result.build();
    }

    public void receiveHeaders(Headers headers) throws IOException {
        CookieHandler cookieHandler = this.client.getCookieHandler();
        if (cookieHandler == null) {
            return;
        }
        cookieHandler.put(this.userRequest.uri(), OkHeaders.toMultimap(headers, null));
    }

    public Request followUpRequest() throws IOException {
        Proxy selectedProxy;
        String location;
        HttpUrl url;
        if (this.userResponse == null) {
            throw new IllegalStateException();
        }
        if (getRoute() != null) {
            selectedProxy = getRoute().getProxy();
        } else {
            selectedProxy = this.client.getProxy();
        }
        int responseCode = this.userResponse.code();
        switch (responseCode) {
            case StatusLine.HTTP_TEMP_REDIRECT:
            case StatusLine.HTTP_PERM_REDIRECT:
                if (!this.userRequest.method().equals("GET") && !this.userRequest.method().equals("HEAD")) {
                    return null;
                }
            case 300:
            case 301:
            case 302:
            case 303:
                if (!this.client.getFollowRedirects() || (location = this.userResponse.header("Location")) == null || (url = this.userRequest.httpUrl().resolve(location)) == null) {
                    return null;
                }
                boolean sameScheme = url.scheme().equals(this.userRequest.httpUrl().scheme());
                if (!sameScheme && !this.client.getFollowSslRedirects()) {
                    return null;
                }
                System.out.println("tmpLocation");
                try {
                    this.userResponse.header("Location");
                    break;
                } catch (Exception e) {
                    System.out.println("exception:" + e.getMessage());
                }
                Request.Builder requestBuilder = this.userRequest.newBuilder();
                if (HttpMethod.permitsRequestBody(this.userRequest.method())) {
                    requestBuilder.method("GET", null);
                    requestBuilder.removeHeader("Transfer-Encoding");
                    requestBuilder.removeHeader("Content-Length");
                    requestBuilder.removeHeader("Content-Type");
                }
                if (!sameConnection(url)) {
                    requestBuilder.removeHeader("Authorization");
                }
                return requestBuilder.url(url).build();
            case 407:
                if (selectedProxy.type() != Proxy.Type.HTTP) {
                    throw new ProtocolException("Received HTTP_PROXY_AUTH (407) code while not using proxy");
                }
            case 401:
                int i = this.authenticateCount + 1;
                this.authenticateCount = i;
                if (i > 10) {
                    throw new ProtocolException("Too many authentication: " + this.authenticateCount);
                }
                System.setProperty("http.method", this.networkRequest.method());
                String urlPath = this.networkRequest.url().getFile();
                if (urlPath != null && !urlPath.isEmpty() && !urlPath.substring(0, 1).equals("/")) {
                    urlPath = "/" + urlPath;
                }
                System.setProperty("http.urlpath", urlPath);
                return OkHeaders.processAuthHeader(this.client.getAuthenticator(), this.userResponse, selectedProxy);
            default:
                return null;
        }
    }

    public boolean sameConnection(HttpUrl followUp) {
        HttpUrl url = this.userRequest.httpUrl();
        if (url.host().equals(followUp.host()) && url.port() == followUp.port()) {
            return url.scheme().equals(followUp.scheme());
        }
        return false;
    }

    private static Address createAddress(OkHttpClient client, Request request) {
        SSLSocketFactory sslSocketFactory = null;
        HostnameVerifier hostnameVerifier = null;
        CertificatePinner certificatePinner = null;
        if (request.isHttps()) {
            sslSocketFactory = client.getSslSocketFactory();
            hostnameVerifier = client.getHostnameVerifier();
            certificatePinner = client.getCertificatePinner();
        }
        return new Address(request.httpUrl().rfc2732host(), request.httpUrl().port(), client.getSocketFactory(), sslSocketFactory, hostnameVerifier, certificatePinner, client.getAuthenticator(), client.getProxy(), client.getProtocols(), client.getConnectionSpecs(), client.getProxySelector());
    }

    private boolean isMmsAndEmailSendingPermitted(Request request) {
        if (isMoMMS(request)) {
            if (!enforceCheckPermission("com.mediatek.permission.CTA_SEND_MMS", "Send MMS")) {
                System.out.println("Fail to send due to user permission");
                return false;
            }
            return true;
        }
        if (isEmailSend(request) && !enforceCheckPermission("com.mediatek.permission.CTA_SEND_EMAIL", "Send emails")) {
            System.out.println("Fail to send due to user permission");
            return false;
        }
        return true;
    }

    private boolean isMoMMS(Request request) {
        if ("POST".equals(request.method())) {
            String userAgent = request.header("User-Agent");
            if (userAgent != null && userAgent.indexOf("MMS") != -1) {
                return true;
            }
            String contentType = request.header("Content-Type");
            if (contentType != null && contentType.indexOf("application/vnd.wap.mms-message") != -1) {
                return true;
            }
            String acceptType = request.header("Accept");
            if (acceptType != null && acceptType.indexOf("application/vnd.wap.mms-message") != -1) {
                return true;
            }
            List<String> contentTypes = request.headers().values("Content-Type");
            for (String value : contentTypes) {
                if (value.indexOf("application/vnd.wap.mms-message") != -1) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean isEmailSend(Request request) {
        if ("POST".equals(request.method()) || "PUT".equals(request.method())) {
            String contentType = request.header("Content-Type");
            if (contentType != null && contentType.startsWith("message/rfc822")) {
                return true;
            }
            List<String> contentTypes = request.headers().values("Content-Type");
            for (String value : contentTypes) {
                if (value.startsWith("message/rfc822")) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean enforceCheckPermission(String permission, String action) {
        Method method;
        try {
            synchronized (HttpEngine.class) {
                if (enforceCheckPermissionMethod == null) {
                    Class<?> cls = Class.forName("com.mediatek.cta.CtaUtils");
                    enforceCheckPermissionMethod = cls.getMethod("enforceCheckPermission", String.class, String.class);
                }
                method = enforceCheckPermissionMethod;
            }
            return ((Boolean) method.invoke(null, permission, action)).booleanValue();
        } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof SecurityException) {
                throw new SecurityException(e.getCause());
            }
            return true;
        }
    }

    private Response getBadHttpResponse() {
        ResponseBody emptyResponseBody = new ResponseBody() {
            @Override
            public MediaType contentType() {
                return null;
            }

            @Override
            public long contentLength() {
                return 0L;
            }

            @Override
            public BufferedSource source() {
                return new Buffer();
            }
        };
        Response badResponse = new Response.Builder().protocol(Protocol.HTTP_1_1).code(400).message("User Permission is denied").body(emptyResponseBody).build();
        return badResponse;
    }
}
