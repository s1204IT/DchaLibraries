package com.android.okhttp.internal.http;

import com.android.okhttp.Address;
import com.android.okhttp.Connection;
import com.android.okhttp.Headers;
import com.android.okhttp.OkHttpClient;
import com.android.okhttp.OkResponseCache;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.ResponseSource;
import com.android.okhttp.Route;
import com.android.okhttp.TunnelRequest;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.http.CacheStrategy;
import com.android.okio.BufferedSink;
import com.android.okio.GzipSource;
import com.android.okio.Okio;
import com.android.okio.Sink;
import com.android.okio.Source;
import java.io.IOException;
import java.io.InputStream;
import java.net.CacheRequest;
import java.net.CookieHandler;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

public class HttpEngine {
    public final boolean bufferRequestBody;
    private BufferedSink bufferedRequestBody;
    private Response cacheResponse;
    final OkHttpClient client;
    private Connection connection;
    private Request networkRequest;
    private Response networkResponse;
    private final Response priorResponse;
    private Sink requestBodyOut;
    private Source responseBody;
    private InputStream responseBodyBytes;
    private ResponseSource responseSource;
    private Source responseTransferSource;
    private Route route;
    private RouteSelector routeSelector;
    long sentRequestMillis = -1;
    private CacheRequest storeRequest;
    private boolean transparentGzip;
    private Transport transport;
    private final Request userRequest;
    private Response userResponse;

    public HttpEngine(OkHttpClient client, Request request, boolean bufferRequestBody, Connection connection, RouteSelector routeSelector, RetryableSink requestBodyOut, Response priorResponse) {
        this.client = client;
        this.userRequest = request;
        this.bufferRequestBody = bufferRequestBody;
        this.connection = connection;
        this.routeSelector = routeSelector;
        this.requestBodyOut = requestBodyOut;
        this.priorResponse = priorResponse;
        if (connection != null) {
            connection.setOwner(this);
            this.route = connection.getRoute();
        } else {
            this.route = null;
        }
    }

    public final void sendRequest() throws IOException {
        if (this.responseSource == null) {
            if (this.transport != null) {
                throw new IllegalStateException();
            }
            Request request = networkRequest(this.userRequest);
            OkResponseCache responseCache = this.client.getOkResponseCache();
            Response cacheCandidate = responseCache != null ? responseCache.get(request) : null;
            long now = System.currentTimeMillis();
            CacheStrategy cacheStrategy = new CacheStrategy.Factory(now, request, cacheCandidate).get();
            this.responseSource = cacheStrategy.source;
            this.networkRequest = cacheStrategy.networkRequest;
            this.cacheResponse = cacheStrategy.cacheResponse;
            if (responseCache != null) {
                responseCache.trackResponse(this.responseSource);
            }
            if (cacheCandidate != null && (this.responseSource == ResponseSource.NONE || this.cacheResponse == null)) {
                Util.closeQuietly(cacheCandidate.body());
            }
            if (this.networkRequest != null) {
                if (this.connection == null) {
                    connect(this.networkRequest);
                }
                if (this.connection.getOwner() != this && !this.connection.isSpdy()) {
                    throw new AssertionError();
                }
                this.transport = (Transport) this.connection.newTransport(this);
                if (hasRequestBody() && this.requestBodyOut == null) {
                    this.requestBodyOut = this.transport.createRequestBody(request);
                    return;
                }
                return;
            }
            if (this.connection != null) {
                this.client.getConnectionPool().recycle(this.connection);
                this.connection = null;
            }
            this.userResponse = this.cacheResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).cacheResponse(stripBody(this.cacheResponse)).build();
            if (this.userResponse.body() != null) {
                initContentStream(this.userResponse.body().source());
            }
        }
    }

    private static Response stripBody(Response response) {
        return (response == null || response.body() == null) ? response : response.newBuilder().body(null).build();
    }

    private void connect(Request request) throws IOException {
        if (this.connection != null) {
            throw new IllegalStateException();
        }
        if (this.routeSelector == null) {
            String uriHost = request.url().getHost();
            if (uriHost == null || uriHost.length() == 0) {
                throw new UnknownHostException(request.url().toString());
            }
            SSLSocketFactory sslSocketFactory = null;
            HostnameVerifier hostnameVerifier = null;
            if (request.isHttps()) {
                sslSocketFactory = this.client.getSslSocketFactory();
                hostnameVerifier = this.client.getHostnameVerifier();
            }
            Address address = new Address(uriHost, Util.getEffectivePort(request.url()), this.client.getSocketFactory(), sslSocketFactory, hostnameVerifier, this.client.getAuthenticator(), this.client.getProxy(), this.client.getProtocols());
            this.routeSelector = new RouteSelector(address, request.uri(), this.client.getProxySelector(), this.client.getConnectionPool(), this.client.getHostResolver(), this.client.getRoutesDatabase());
        }
        this.connection = this.routeSelector.next(request.method());
        this.connection.setOwner(this);
        if (!this.connection.isConnected()) {
            this.connection.connect(this.client.getConnectTimeout(), this.client.getReadTimeout(), getTunnelConfig());
            if (this.connection.isSpdy()) {
                this.client.getConnectionPool().share(this.connection);
            }
            this.client.getRoutesDatabase().connected(this.connection.getRoute());
        } else if (!this.connection.isSpdy()) {
            this.connection.updateReadTimeout(this.client.getReadTimeout());
        }
        this.route = this.connection.getRoute();
    }

    public void writingRequestHeaders() {
        if (this.sentRequestMillis != -1) {
            throw new IllegalStateException();
        }
        this.sentRequestMillis = System.currentTimeMillis();
    }

    boolean hasRequestBody() {
        return HttpMethod.hasRequestBody(this.userRequest.method());
    }

    public final Sink getRequestBody() {
        if (this.responseSource == null) {
            throw new IllegalStateException();
        }
        return this.requestBodyOut;
    }

    public final BufferedSink getBufferedRequestBody() {
        BufferedSink result;
        BufferedSink result2 = this.bufferedRequestBody;
        if (result2 == null) {
            Sink requestBody = getRequestBody();
            if (requestBody != null) {
                result = Okio.buffer(requestBody);
                this.bufferedRequestBody = result;
            } else {
                result = null;
            }
            return result;
        }
        return result2;
    }

    public final boolean hasResponse() {
        return this.userResponse != null;
    }

    public final Request getRequest() {
        return this.userRequest;
    }

    public final Response getResponse() {
        if (this.userResponse == null) {
            throw new IllegalStateException();
        }
        return this.userResponse;
    }

    public final Source getResponseBody() {
        if (this.userResponse == null) {
            throw new IllegalStateException();
        }
        return this.responseBody;
    }

    public final InputStream getResponseBodyBytes() {
        InputStream result = this.responseBodyBytes;
        if (result != null) {
            return result;
        }
        InputStream result2 = Okio.buffer(getResponseBody()).inputStream();
        this.responseBodyBytes = result2;
        return result2;
    }

    public final Connection getConnection() {
        return this.connection;
    }

    public HttpEngine recover(IOException e) {
        if (this.routeSelector != null && this.connection != null) {
            this.routeSelector.connectFailed(this.connection, e);
        }
        boolean canRetryRequestBody = this.requestBodyOut == null || (this.requestBodyOut instanceof RetryableSink);
        if ((this.routeSelector == null && this.connection == null) || ((this.routeSelector != null && !this.routeSelector.hasNext()) || !isRecoverable(e) || !canRetryRequestBody)) {
            return null;
        }
        Connection connection = close();
        return new HttpEngine(this.client, this.userRequest, this.bufferRequestBody, connection, this.routeSelector, (RetryableSink) this.requestBodyOut, this.priorResponse);
    }

    private boolean isRecoverable(IOException e) {
        boolean sslFailure = (e instanceof SSLHandshakeException) && (e.getCause() instanceof CertificateException);
        boolean protocolFailure = e instanceof ProtocolException;
        return (sslFailure || protocolFailure) ? false : true;
    }

    public Route getRoute() {
        return this.route;
    }

    private void maybeCache() throws IOException {
        OkResponseCache responseCache = this.client.getOkResponseCache();
        if (responseCache != null) {
            if (!CacheStrategy.isCacheable(this.userResponse, this.networkRequest)) {
                responseCache.maybeRemove(this.networkRequest);
            } else {
                this.storeRequest = responseCache.put(stripBody(this.userResponse));
            }
        }
    }

    public final void releaseConnection() throws IOException {
        if (this.transport != null && this.connection != null) {
            this.transport.releaseConnectionOnIdle();
        }
        this.connection = null;
    }

    public final void disconnect() throws IOException {
        if (this.transport != null) {
            this.transport.disconnect(this);
        }
    }

    public final Connection close() {
        if (this.bufferedRequestBody != null) {
            Util.closeQuietly(this.bufferedRequestBody);
        } else if (this.requestBodyOut != null) {
            Util.closeQuietly(this.requestBodyOut);
        }
        if (this.responseBody == null) {
            Util.closeQuietly(this.connection);
            this.connection = null;
            return null;
        }
        Util.closeQuietly(this.responseBody);
        Util.closeQuietly(this.responseBodyBytes);
        if (this.transport != null && !this.transport.canReuseConnection()) {
            Util.closeQuietly(this.connection);
            this.connection = null;
            return null;
        }
        if (this.connection != null && !this.connection.clearOwner()) {
            this.connection = null;
        }
        Connection connection = this.connection;
        this.connection = null;
        return connection;
    }

    private void initContentStream(Source transferSource) throws IOException {
        this.responseTransferSource = transferSource;
        if (this.transparentGzip && "gzip".equalsIgnoreCase(this.userResponse.header("Content-Encoding"))) {
            this.userResponse = this.userResponse.newBuilder().removeHeader("Content-Encoding").removeHeader("Content-Length").build();
            this.responseBody = new GzipSource(transferSource);
        } else {
            this.responseBody = transferSource;
        }
    }

    public final boolean hasResponseBody() {
        if (this.userRequest.method().equals("HEAD")) {
            return false;
        }
        int responseCode = this.userResponse.code();
        if ((responseCode >= 100 && responseCode < 200) || responseCode == 204 || responseCode == 304) {
            return OkHeaders.contentLength(this.networkResponse) != -1 || "chunked".equalsIgnoreCase(this.networkResponse.header("Transfer-Encoding"));
        }
        return true;
    }

    private Request networkRequest(Request request) throws IOException {
        Request.Builder result = request.newBuilder();
        if (request.getUserAgent() == null) {
            result.setUserAgent(getDefaultUserAgent());
        }
        if (request.header("Host") == null) {
            result.header("Host", hostHeader(request.url()));
        }
        if ((this.connection == null || this.connection.getHttpMinorVersion() != 0) && request.header("Connection") == null) {
            result.header("Connection", "Keep-Alive");
        }
        if (request.header("Accept-Encoding") == null) {
            this.transparentGzip = true;
            result.header("Accept-Encoding", "gzip");
        }
        if (hasRequestBody() && request.header("Content-Type") == null) {
            result.header("Content-Type", "application/x-www-form-urlencoded");
        }
        CookieHandler cookieHandler = this.client.getCookieHandler();
        if (cookieHandler != null) {
            Map<String, List<String>> headers = OkHeaders.toMultimap(result.build().headers(), null);
            Map<String, List<String>> cookies = cookieHandler.get(request.uri(), headers);
            OkHeaders.addCookies(result, cookies);
        }
        return result.build();
    }

    public static String getDefaultUserAgent() {
        String agent = System.getProperty("http.agent");
        return agent != null ? agent : "Java" + System.getProperty("java.version");
    }

    public static String hostHeader(URL url) {
        return Util.getEffectivePort(url) != Util.getDefaultPort(url.getProtocol()) ? url.getHost() + ":" + url.getPort() : url.getHost();
    }

    public final void readResponse() throws IOException {
        if (this.userResponse == null) {
            if (this.networkRequest == null && this.cacheResponse == null) {
                throw new IllegalStateException("call sendRequest() first!");
            }
            if (this.networkRequest != null) {
                if (this.bufferedRequestBody != null && this.bufferedRequestBody.buffer().size() > 0) {
                    this.bufferedRequestBody.flush();
                }
                if (this.sentRequestMillis == -1) {
                    if (OkHeaders.contentLength(this.networkRequest) == -1 && (this.requestBodyOut instanceof RetryableSink)) {
                        long contentLength = ((RetryableSink) this.requestBodyOut).contentLength();
                        this.networkRequest = this.networkRequest.newBuilder().header("Content-Length", Long.toString(contentLength)).build();
                    }
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
                this.transport.flushRequest();
                this.networkResponse = this.transport.readResponseHeaders().request(this.networkRequest).handshake(this.connection.getHandshake()).header(OkHeaders.SENT_MILLIS, Long.toString(this.sentRequestMillis)).header(OkHeaders.RECEIVED_MILLIS, Long.toString(System.currentTimeMillis())).setResponseSource(this.responseSource).build();
                this.connection.setHttpMinorVersion(this.networkResponse.httpMinorVersion());
                receiveHeaders(this.networkResponse.headers());
                if (this.responseSource == ResponseSource.CONDITIONAL_CACHE) {
                    if (this.cacheResponse.validate(this.networkResponse)) {
                        this.userResponse = this.cacheResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).headers(combine(this.cacheResponse.headers(), this.networkResponse.headers())).cacheResponse(stripBody(this.cacheResponse)).networkResponse(stripBody(this.networkResponse)).build();
                        this.transport.emptyTransferStream();
                        releaseConnection();
                        OkResponseCache responseCache = this.client.getOkResponseCache();
                        responseCache.trackConditionalCacheHit();
                        responseCache.update(this.cacheResponse, stripBody(this.userResponse));
                        if (this.cacheResponse.body() != null) {
                            initContentStream(this.cacheResponse.body().source());
                            return;
                        }
                        return;
                    }
                    Util.closeQuietly(this.cacheResponse.body());
                }
                this.userResponse = this.networkResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).cacheResponse(stripBody(this.cacheResponse)).networkResponse(stripBody(this.networkResponse)).build();
                if (!hasResponseBody()) {
                    this.responseTransferSource = this.transport.getTransferStream(this.storeRequest);
                    this.responseBody = this.responseTransferSource;
                } else {
                    maybeCache();
                    initContentStream(this.transport.getTransferStream(this.storeRequest));
                }
            }
        }
    }

    private static Headers combine(Headers cachedHeaders, Headers networkHeaders) throws IOException {
        Headers.Builder result = new Headers.Builder();
        for (int i = 0; i < cachedHeaders.size(); i++) {
            String fieldName = cachedHeaders.name(i);
            String value = cachedHeaders.value(i);
            if ((!"Warning".equals(fieldName) || !value.startsWith("1")) && (!isEndToEnd(fieldName) || networkHeaders.get(fieldName) == null)) {
                result.add(fieldName, value);
            }
        }
        for (int i2 = 0; i2 < networkHeaders.size(); i2++) {
            String fieldName2 = networkHeaders.name(i2);
            if (isEndToEnd(fieldName2)) {
                result.add(fieldName2, networkHeaders.value(i2));
            }
        }
        return result.build();
    }

    private static boolean isEndToEnd(String fieldName) {
        return ("Connection".equalsIgnoreCase(fieldName) || "Keep-Alive".equalsIgnoreCase(fieldName) || "Proxy-Authenticate".equalsIgnoreCase(fieldName) || "Proxy-Authorization".equalsIgnoreCase(fieldName) || "TE".equalsIgnoreCase(fieldName) || "Trailers".equalsIgnoreCase(fieldName) || "Transfer-Encoding".equalsIgnoreCase(fieldName) || "Upgrade".equalsIgnoreCase(fieldName)) ? false : true;
    }

    private TunnelRequest getTunnelConfig() {
        if (!this.userRequest.isHttps()) {
            return null;
        }
        String userAgent = this.userRequest.getUserAgent();
        if (userAgent == null) {
            userAgent = getDefaultUserAgent();
        }
        URL url = this.userRequest.url();
        return new TunnelRequest(url.getHost(), Util.getEffectivePort(url), userAgent, this.userRequest.getProxyAuthorization());
    }

    public void receiveHeaders(Headers headers) throws IOException {
        CookieHandler cookieHandler = this.client.getCookieHandler();
        if (cookieHandler != null) {
            cookieHandler.put(this.userRequest.uri(), OkHeaders.toMultimap(headers, null));
        }
    }
}
