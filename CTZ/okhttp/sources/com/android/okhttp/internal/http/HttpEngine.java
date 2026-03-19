package com.android.okhttp.internal.http;

import com.android.okhttp.Address;
import com.android.okhttp.CertificatePinner;
import com.android.okhttp.Connection;
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
import com.android.okhttp.internal.cta.CtaAdapter;
import com.android.okhttp.internal.http.CacheStrategy;
import com.android.okhttp.internal.io.RealConnection;
import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.BufferedSource;
import com.android.okhttp.okio.GzipSource;
import com.android.okhttp.okio.Okio;
import com.android.okhttp.okio.Sink;
import com.android.okhttp.okio.Source;
import com.android.okhttp.okio.Timeout;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.ProtocolException;
import java.net.Proxy;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
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
    public static final int MAX_FOLLOW_UPS = 20;
    public final boolean bufferRequestBody;
    private BufferedSink bufferedRequestBody;
    private Response cacheResponse;
    private CacheStrategy cacheStrategy;
    private final boolean callerWritesRequestBody;
    final OkHttpClient client;
    private final boolean forWebSocket;
    private HttpStream httpStream;
    private Request networkRequest;
    private final Response priorResponse;
    private Sink requestBodyOut;
    private CacheRequest storeRequest;
    public final StreamAllocation streamAllocation;
    private boolean transparentGzip;
    private final Request userRequest;
    private Response userResponse;
    long sentRequestMillis = -1;
    private boolean momsPermitted = true;

    public HttpEngine(OkHttpClient okHttpClient, Request request, boolean z, boolean z2, boolean z3, StreamAllocation streamAllocation, RetryableSink retryableSink, Response response) {
        this.client = okHttpClient;
        this.userRequest = request;
        this.bufferRequestBody = z;
        this.callerWritesRequestBody = z2;
        this.forWebSocket = z3;
        this.streamAllocation = streamAllocation == null ? new StreamAllocation(okHttpClient.getConnectionPool(), createAddress(okHttpClient, request)) : streamAllocation;
        this.requestBodyOut = retryableSink;
        this.priorResponse = response;
    }

    public void sendRequest() throws RouteException, IOException, RequestException {
        Response response;
        if (this.cacheStrategy != null) {
            return;
        }
        if (this.httpStream != null) {
            throw new IllegalStateException();
        }
        Request requestNetworkRequest = networkRequest(this.userRequest);
        InternalCache internalCache = Internal.instance.internalCache(this.client);
        if (internalCache != null) {
            response = internalCache.get(requestNetworkRequest);
        } else {
            response = null;
        }
        this.cacheStrategy = new CacheStrategy.Factory(System.currentTimeMillis(), requestNetworkRequest, response).get();
        this.networkRequest = this.cacheStrategy.networkRequest;
        this.cacheResponse = this.cacheStrategy.cacheResponse;
        if (internalCache != null) {
            internalCache.trackResponse(this.cacheStrategy);
        }
        if (response != null && this.cacheResponse == null) {
            Util.closeQuietly(response.body());
        }
        if (this.networkRequest != null) {
            synchronized (HttpEngine.class) {
                if (!CtaAdapter.isSendingPermitted(this.userRequest)) {
                    this.momsPermitted = false;
                    return;
                }
                this.httpStream = connect();
                this.httpStream.setHttpEngine(this);
                CtaAdapter.updateMmsBufferSize(this.userRequest, getConnection());
                if (this.callerWritesRequestBody && permitsRequestBody(this.networkRequest) && this.requestBodyOut == null) {
                    long jContentLength = OkHeaders.contentLength(requestNetworkRequest);
                    if (!this.bufferRequestBody) {
                        this.httpStream.writeRequestHeaders(this.networkRequest);
                        this.requestBodyOut = this.httpStream.createRequestBody(this.networkRequest, jContentLength);
                        return;
                    } else {
                        if (jContentLength > 2147483647L) {
                            throw new IllegalStateException("Use setFixedLengthStreamingMode() or setChunkedStreamingMode() for requests larger than 2 GiB.");
                        }
                        if (jContentLength != -1) {
                            this.httpStream.writeRequestHeaders(this.networkRequest);
                            this.requestBodyOut = new RetryableSink((int) jContentLength);
                            return;
                        } else {
                            this.requestBodyOut = new RetryableSink();
                            return;
                        }
                    }
                }
                return;
            }
        }
        if (this.cacheResponse != null) {
            this.userResponse = this.cacheResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).cacheResponse(stripBody(this.cacheResponse)).build();
        } else {
            this.userResponse = new Response.Builder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).protocol(Protocol.HTTP_1_1).code(504).message("Unsatisfiable Request (only-if-cached)").body(EMPTY_BODY).build();
        }
        this.userResponse = unzip(this.userResponse);
    }

    private HttpStream connect() throws RouteException, RequestException, IOException {
        return this.streamAllocation.newStream(this.client.getConnectTimeout(), this.client.getReadTimeout(), this.client.getWriteTimeout(), this.client.getRetryOnConnectionFailure(), !this.networkRequest.method().equals("GET"));
    }

    private static Response stripBody(Response response) {
        if (response == null || response.body() == null) {
            return response;
        }
        return response.newBuilder().body(null).build();
    }

    public void writingRequestHeaders() {
        if (this.sentRequestMillis != -1) {
            throw new IllegalStateException();
        }
        this.sentRequestMillis = System.currentTimeMillis();
    }

    boolean permitsRequestBody(Request request) {
        return HttpMethod.permitsRequestBody(request.method());
    }

    public Sink getRequestBody() {
        if (this.cacheStrategy == null) {
            throw new IllegalStateException();
        }
        return this.requestBodyOut;
    }

    public BufferedSink getBufferedRequestBody() {
        BufferedSink bufferedSink = this.bufferedRequestBody;
        if (bufferedSink != null) {
            return bufferedSink;
        }
        Sink requestBody = getRequestBody();
        if (requestBody != null) {
            BufferedSink bufferedSinkBuffer = Okio.buffer(requestBody);
            this.bufferedRequestBody = bufferedSinkBuffer;
            return bufferedSinkBuffer;
        }
        return null;
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
        return this.streamAllocation.connection();
    }

    public HttpEngine recover(RouteException routeException) {
        if (!this.streamAllocation.recover(routeException) || !this.client.getRetryOnConnectionFailure()) {
            return null;
        }
        return new HttpEngine(this.client, this.userRequest, this.bufferRequestBody, this.callerWritesRequestBody, this.forWebSocket, close(), (RetryableSink) this.requestBodyOut, this.priorResponse);
    }

    public HttpEngine recover(IOException iOException, Sink sink) {
        if (!this.streamAllocation.recover(iOException, sink) || !this.client.getRetryOnConnectionFailure()) {
            return null;
        }
        return new HttpEngine(this.client, this.userRequest, this.bufferRequestBody, this.callerWritesRequestBody, this.forWebSocket, close(), (RetryableSink) sink, this.priorResponse);
    }

    public HttpEngine recover(IOException iOException) {
        return recover(iOException, this.requestBodyOut);
    }

    private void maybeCache() throws IOException {
        InternalCache internalCache = Internal.instance.internalCache(this.client);
        if (internalCache == null) {
            return;
        }
        if (!CacheStrategy.isCacheable(this.userResponse, this.networkRequest)) {
            if (HttpMethod.invalidatesCache(this.networkRequest.method())) {
                try {
                    internalCache.remove(this.networkRequest);
                    return;
                } catch (IOException e) {
                    return;
                }
            }
            return;
        }
        this.storeRequest = internalCache.put(stripBody(this.userResponse));
    }

    public void releaseStreamAllocation() throws IOException {
        this.streamAllocation.release();
    }

    public void cancel() {
        this.streamAllocation.cancel();
    }

    public StreamAllocation close() {
        if (this.bufferedRequestBody != null) {
            Util.closeQuietly(this.bufferedRequestBody);
        } else if (this.requestBodyOut != null) {
            Util.closeQuietly(this.requestBodyOut);
        }
        if (this.userResponse != null) {
            Util.closeQuietly(this.userResponse.body());
        } else {
            this.streamAllocation.connectionFailed();
        }
        return this.streamAllocation;
    }

    private Response unzip(Response response) throws IOException {
        if (!this.transparentGzip || !"gzip".equalsIgnoreCase(this.userResponse.header("Content-Encoding")) || response.body() == null) {
            return response;
        }
        GzipSource gzipSource = new GzipSource(response.body().source());
        Headers headersBuild = response.headers().newBuilder().removeAll("Content-Encoding").removeAll("Content-Length").build();
        return response.newBuilder().headers(headersBuild).body(new RealResponseBody(headersBuild, Okio.buffer(gzipSource))).build();
    }

    public static boolean hasBody(Response response) {
        if (response.request().method().equals("HEAD")) {
            return false;
        }
        int iCode = response.code();
        return (((iCode >= 100 && iCode < 200) || iCode == 204 || iCode == 304) && OkHeaders.contentLength(response) == -1 && !"chunked".equalsIgnoreCase(response.header("Transfer-Encoding"))) ? false : true;
    }

    private Request networkRequest(Request request) throws IOException {
        Request.Builder builderNewBuilder = request.newBuilder();
        if (request.header("Host") == null) {
            builderNewBuilder.header("Host", Util.hostHeader(request.httpUrl(), false));
        }
        if (request.header("Connection") == null) {
            builderNewBuilder.header("Connection", "Keep-Alive");
        }
        if (request.header("Accept-Encoding") == null) {
            this.transparentGzip = true;
            builderNewBuilder.header("Accept-Encoding", "gzip");
        }
        CookieHandler cookieHandler = this.client.getCookieHandler();
        if (cookieHandler != null) {
            OkHeaders.addCookies(builderNewBuilder, cookieHandler.get(request.uri(), OkHeaders.toMultimap(builderNewBuilder.build().headers(), null)));
        }
        if (request.header("User-Agent") == null) {
            builderNewBuilder.header("User-Agent", Version.userAgent());
        }
        return builderNewBuilder.build();
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
            this.userResponse = CtaAdapter.getBadHttpResponse();
            return;
        }
        if (this.forWebSocket) {
            this.httpStream.writeRequestHeaders(this.networkRequest);
            networkResponse = readNetworkResponse();
        } else if (!this.callerWritesRequestBody) {
            networkResponse = new NetworkInterceptorChain(0, this.networkRequest).proceed(this.networkRequest);
        } else {
            if (this.bufferedRequestBody != null && this.bufferedRequestBody.buffer().size() > 0) {
                this.bufferedRequestBody.emit();
            }
            if (this.sentRequestMillis == -1) {
                if (OkHeaders.contentLength(this.networkRequest) == -1 && (this.requestBodyOut instanceof RetryableSink)) {
                    this.networkRequest = this.networkRequest.newBuilder().header("Content-Length", Long.toString(((RetryableSink) this.requestBodyOut).contentLength())).build();
                }
                System.out.println("[OkHttp] sendRequest>>");
                this.httpStream.writeRequestHeaders(this.networkRequest);
            }
            if (this.requestBodyOut != null) {
                if (this.bufferedRequestBody != null) {
                    this.bufferedRequestBody.close();
                } else {
                    this.requestBodyOut.close();
                }
                if (this.requestBodyOut instanceof RetryableSink) {
                    this.httpStream.writeRequestBody((RetryableSink) this.requestBodyOut);
                }
            }
            System.out.println("[OkHttp] sendRequest<<");
            networkResponse = readNetworkResponse();
        }
        receiveHeaders(networkResponse.headers());
        if (this.cacheResponse != null) {
            if (validate(this.cacheResponse, networkResponse)) {
                this.userResponse = this.cacheResponse.newBuilder().request(this.userRequest).priorResponse(stripBody(this.priorResponse)).headers(combine(this.cacheResponse.headers(), networkResponse.headers())).cacheResponse(stripBody(this.cacheResponse)).networkResponse(stripBody(networkResponse)).build();
                networkResponse.body().close();
                releaseStreamAllocation();
                InternalCache internalCache = Internal.instance.internalCache(this.client);
                internalCache.trackConditionalCacheHit();
                internalCache.update(this.cacheResponse, stripBody(this.userResponse));
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

        NetworkInterceptorChain(int i, Request request) {
            this.index = i;
            this.request = request;
        }

        @Override
        public Connection connection() {
            return HttpEngine.this.streamAllocation.connection();
        }

        @Override
        public Request request() {
            return this.request;
        }

        @Override
        public Response proceed(Request request) throws IOException {
            this.calls++;
            if (this.index > 0) {
                Interceptor interceptor = HttpEngine.this.client.networkInterceptors().get(this.index - 1);
                Address address = connection().getRoute().getAddress();
                if (!request.httpUrl().host().equals(address.getUriHost()) || request.httpUrl().port() != address.getUriPort()) {
                    throw new IllegalStateException("network interceptor " + interceptor + " must retain the same host and port");
                }
                if (this.calls > 1) {
                    throw new IllegalStateException("network interceptor " + interceptor + " must call proceed() exactly once");
                }
            }
            if (this.index >= HttpEngine.this.client.networkInterceptors().size()) {
                HttpEngine.this.httpStream.writeRequestHeaders(request);
                HttpEngine.this.networkRequest = request;
                if (HttpEngine.this.permitsRequestBody(request) && request.body() != null) {
                    BufferedSink bufferedSinkBuffer = Okio.buffer(HttpEngine.this.httpStream.createRequestBody(request, request.body().contentLength()));
                    request.body().writeTo(bufferedSinkBuffer);
                    bufferedSinkBuffer.close();
                }
                Response networkResponse = HttpEngine.this.readNetworkResponse();
                int iCode = networkResponse.code();
                if ((iCode == 204 || iCode == 205) && networkResponse.body().contentLength() > 0) {
                    throw new ProtocolException("HTTP " + iCode + " had non-zero Content-Length: " + networkResponse.body().contentLength());
                }
                return networkResponse;
            }
            NetworkInterceptorChain networkInterceptorChain = HttpEngine.this.new NetworkInterceptorChain(this.index + 1, request);
            Interceptor interceptor2 = HttpEngine.this.client.networkInterceptors().get(this.index);
            Response responseIntercept = interceptor2.intercept(networkInterceptorChain);
            if (networkInterceptorChain.calls != 1) {
                throw new IllegalStateException("network interceptor " + interceptor2 + " must call proceed() exactly once");
            }
            if (responseIntercept == null) {
                throw new NullPointerException("network interceptor " + interceptor2 + " returned null");
            }
            return responseIntercept;
        }
    }

    private Response readNetworkResponse() throws IOException {
        this.httpStream.finishRequest();
        Response responseBuild = this.httpStream.readResponseHeaders().request(this.networkRequest).handshake(this.streamAllocation.connection().getHandshake()).header(OkHeaders.SENT_MILLIS, Long.toString(this.sentRequestMillis)).header(OkHeaders.RECEIVED_MILLIS, Long.toString(System.currentTimeMillis())).build();
        if (!this.forWebSocket) {
            responseBuild = responseBuild.newBuilder().body(this.httpStream.openResponseBody(responseBuild)).build();
        }
        if ("close".equalsIgnoreCase(responseBuild.request().header("Connection")) || "close".equalsIgnoreCase(responseBuild.header("Connection"))) {
            this.streamAllocation.noNewStreams();
        }
        return responseBuild;
    }

    private Response cacheWritingResponse(final CacheRequest cacheRequest, Response response) throws IOException {
        Sink sinkBody;
        if (cacheRequest == null || (sinkBody = cacheRequest.body()) == null) {
            return response;
        }
        final BufferedSource bufferedSourceSource = response.body().source();
        final BufferedSink bufferedSinkBuffer = Okio.buffer(sinkBody);
        return response.newBuilder().body(new RealResponseBody(response.headers(), Okio.buffer(new Source() {
            boolean cacheRequestClosed;

            @Override
            public long read(Buffer buffer, long j) throws IOException {
                try {
                    long j2 = bufferedSourceSource.read(buffer, j);
                    if (j2 == -1) {
                        if (!this.cacheRequestClosed) {
                            this.cacheRequestClosed = true;
                            bufferedSinkBuffer.close();
                        }
                        return -1L;
                    }
                    buffer.copyTo(bufferedSinkBuffer.buffer(), buffer.size() - j2, j2);
                    bufferedSinkBuffer.emitCompleteSegments();
                    return j2;
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
                return bufferedSourceSource.timeout();
            }

            @Override
            public void close() throws IOException {
                if (!this.cacheRequestClosed && !Util.discard(this, 100, TimeUnit.MILLISECONDS)) {
                    this.cacheRequestClosed = true;
                    cacheRequest.abort();
                }
                bufferedSourceSource.close();
            }
        }))).build();
    }

    private static boolean validate(Response response, Response response2) {
        Date date;
        if (response2.code() == 304) {
            return true;
        }
        Date date2 = response.headers().getDate("Last-Modified");
        return (date2 == null || (date = response2.headers().getDate("Last-Modified")) == null || date.getTime() >= date2.getTime()) ? false : true;
    }

    private static Headers combine(Headers headers, Headers headers2) throws IOException {
        Headers.Builder builder = new Headers.Builder();
        int size = headers.size();
        for (int i = 0; i < size; i++) {
            String strName = headers.name(i);
            String strValue = headers.value(i);
            if ((!"Warning".equalsIgnoreCase(strName) || !strValue.startsWith("1")) && (!OkHeaders.isEndToEnd(strName) || headers2.get(strName) == null)) {
                builder.add(strName, strValue);
            }
        }
        int size2 = headers2.size();
        for (int i2 = 0; i2 < size2; i2++) {
            String strName2 = headers2.name(i2);
            if (!"Content-Length".equalsIgnoreCase(strName2) && OkHeaders.isEndToEnd(strName2)) {
                builder.add(strName2, headers2.value(i2));
            }
        }
        return builder.build();
    }

    public void receiveHeaders(Headers headers) throws IOException {
        CookieHandler cookieHandler = this.client.getCookieHandler();
        if (cookieHandler != null) {
            cookieHandler.put(this.userRequest.uri(), OkHeaders.toMultimap(headers, null));
        }
    }

    public Request followUpRequest() throws IOException {
        Route route;
        Proxy proxy;
        String strHeader;
        HttpUrl httpUrlResolve;
        if (this.userResponse == null) {
            throw new IllegalStateException();
        }
        RealConnection realConnectionConnection = this.streamAllocation.connection();
        if (realConnectionConnection != null) {
            route = realConnectionConnection.getRoute();
        } else {
            route = null;
        }
        if (route != null) {
            proxy = route.getProxy();
        } else {
            proxy = this.client.getProxy();
        }
        int iCode = this.userResponse.code();
        String strMethod = this.userRequest.method();
        if (iCode != 401) {
            if (iCode == 407) {
                if (proxy.type() != Proxy.Type.HTTP) {
                    throw new ProtocolException("Received HTTP_PROXY_AUTH (407) code while not using proxy");
                }
            } else {
                switch (iCode) {
                    case 300:
                    case 301:
                    case 302:
                    case 303:
                        break;
                    default:
                        switch (iCode) {
                            case StatusLine.HTTP_TEMP_REDIRECT:
                            case StatusLine.HTTP_PERM_REDIRECT:
                                if (!strMethod.equals("GET") && !strMethod.equals("HEAD")) {
                                    return null;
                                }
                                break;
                            default:
                                return null;
                        }
                        break;
                }
                if (!this.client.getFollowRedirects() || (strHeader = this.userResponse.header("Location")) == null || (httpUrlResolve = this.userRequest.httpUrl().resolve(strHeader)) == null) {
                    return null;
                }
                if (!httpUrlResolve.scheme().equals(this.userRequest.httpUrl().scheme()) && !this.client.getFollowSslRedirects()) {
                    return null;
                }
                Request.Builder builderNewBuilder = this.userRequest.newBuilder();
                if (HttpMethod.permitsRequestBody(strMethod)) {
                    if (HttpMethod.redirectsToGet(strMethod)) {
                        builderNewBuilder.method("GET", null);
                    } else {
                        builderNewBuilder.method(strMethod, null);
                    }
                    builderNewBuilder.removeHeader("Transfer-Encoding");
                    builderNewBuilder.removeHeader("Content-Length");
                    builderNewBuilder.removeHeader("Content-Type");
                }
                if (!sameConnection(httpUrlResolve)) {
                    builderNewBuilder.removeHeader("Authorization");
                }
                return builderNewBuilder.url(httpUrlResolve).build();
            }
        }
        return OkHeaders.processAuthHeader(this.client.getAuthenticator(), this.userResponse, proxy);
    }

    public boolean sameConnection(HttpUrl httpUrl) {
        HttpUrl httpUrl2 = this.userRequest.httpUrl();
        return httpUrl2.host().equals(httpUrl.host()) && httpUrl2.port() == httpUrl.port() && httpUrl2.scheme().equals(httpUrl.scheme());
    }

    private static Address createAddress(OkHttpClient okHttpClient, Request request) {
        SSLSocketFactory sSLSocketFactory;
        HostnameVerifier hostnameVerifier;
        CertificatePinner certificatePinner;
        if (request.isHttps()) {
            SSLSocketFactory sslSocketFactory = okHttpClient.getSslSocketFactory();
            hostnameVerifier = okHttpClient.getHostnameVerifier();
            sSLSocketFactory = sslSocketFactory;
            certificatePinner = okHttpClient.getCertificatePinner();
        } else {
            sSLSocketFactory = null;
            hostnameVerifier = null;
            certificatePinner = null;
        }
        return new Address(request.httpUrl().host(), request.httpUrl().port(), okHttpClient.getDns(), okHttpClient.getSocketFactory(), sSLSocketFactory, hostnameVerifier, certificatePinner, okHttpClient.getAuthenticator(), okHttpClient.getProxy(), okHttpClient.getProtocols(), okHttpClient.getConnectionSpecs(), okHttpClient.getProxySelector());
    }
}
