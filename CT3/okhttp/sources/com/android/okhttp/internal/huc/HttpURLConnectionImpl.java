package com.android.okhttp.internal.huc;

import com.android.okhttp.Connection;
import com.android.okhttp.Handshake;
import com.android.okhttp.Headers;
import com.android.okhttp.HttpUrl;
import com.android.okhttp.MediaType;
import com.android.okhttp.OkHttpClient;
import com.android.okhttp.Request;
import com.android.okhttp.RequestBody;
import com.android.okhttp.Response;
import com.android.okhttp.Route;
import com.android.okhttp.internal.Internal;
import com.android.okhttp.internal.Platform;
import com.android.okhttp.internal.URLFilter;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.Version;
import com.android.okhttp.internal.http.HttpDate;
import com.android.okhttp.internal.http.HttpEngine;
import com.android.okhttp.internal.http.HttpMethod;
import com.android.okhttp.internal.http.OkHeaders;
import com.android.okhttp.internal.http.RequestException;
import com.android.okhttp.internal.http.RetryableSink;
import com.android.okhttp.internal.http.RouteException;
import com.android.okhttp.internal.http.StatusLine;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.Sink;
import com.squareup.okhttp.Protocol;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketPermission;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HttpURLConnectionImpl extends HttpURLConnection {
    final OkHttpClient client;
    private long fixedContentLength;
    private int followUpCount;
    Handshake handshake;
    protected HttpEngine httpEngine;
    protected IOException httpEngineFailure;
    private Headers.Builder requestHeaders;
    private Headers responseHeaders;
    private Route route;
    private URLFilter urlFilter;
    private static final Set<String> METHODS = new LinkedHashSet(Arrays.asList("OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "PATCH"));
    private static final RequestBody EMPTY_REQUEST_BODY = RequestBody.create((MediaType) null, new byte[0]);

    public HttpURLConnectionImpl(URL url, OkHttpClient client) {
        super(url);
        this.requestHeaders = new Headers.Builder();
        this.fixedContentLength = -1L;
        this.client = client;
    }

    public HttpURLConnectionImpl(URL url, OkHttpClient client, URLFilter urlFilter) {
        this(url, client);
        this.urlFilter = urlFilter;
    }

    @Override
    public final void connect() throws IOException {
        boolean success;
        initHttpEngine();
        do {
            success = execute(false);
        } while (!success);
    }

    @Override
    public final void disconnect() {
        if (this.httpEngine == null) {
            return;
        }
        this.httpEngine.disconnect();
    }

    @Override
    public final InputStream getErrorStream() {
        try {
            HttpEngine response = getResponse();
            if (!HttpEngine.hasBody(response.getResponse()) || response.getResponse().code() < 400) {
                return null;
            }
            return response.getResponse().body().byteStream();
        } catch (IOException e) {
            return null;
        }
    }

    private Headers getHeaders() throws IOException {
        if (this.responseHeaders == null) {
            Response response = getResponse().getResponse();
            Headers headers = response.headers();
            this.responseHeaders = headers.newBuilder().add(Platform.get().getPrefix() + "-Response-Source", responseSourceHeader(response)).build();
        }
        return this.responseHeaders;
    }

    private static String responseSourceHeader(Response response) {
        if (response.networkResponse() == null) {
            if (response.cacheResponse() == null) {
                return "NONE";
            }
            return "CACHE " + response.code();
        }
        if (response.cacheResponse() == null) {
            return "NETWORK " + response.code();
        }
        return "CONDITIONAL_CACHE " + response.networkResponse().code();
    }

    @Override
    public final String getHeaderField(int position) {
        try {
            return getHeaders().value(position);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public final String getHeaderField(String fieldName) {
        String string;
        try {
            if (fieldName == null) {
                string = StatusLine.get(getResponse().getResponse()).toString();
            } else {
                string = getHeaders().get(fieldName);
            }
            return string;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public final String getHeaderFieldKey(int position) {
        try {
            return getHeaders().name(position);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public final Map<String, List<String>> getHeaderFields() {
        try {
            return OkHeaders.toMultimap(getHeaders(), StatusLine.get(getResponse().getResponse()).toString());
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public final Map<String, List<String>> getRequestProperties() {
        if (this.connected) {
            throw new IllegalStateException("Cannot access request header fields after connection is set");
        }
        return OkHeaders.toMultimap(this.requestHeaders.build(), null);
    }

    @Override
    public final InputStream getInputStream() throws IOException {
        if (!this.doInput) {
            throw new ProtocolException("This protocol does not support input");
        }
        HttpEngine response = getResponse();
        if (getResponseCode() >= 400 && (getResponseCode() != 409 || !"true".equals(System.getProperty("xcap.handl409")))) {
            throw new FileNotFoundException(this.url.toString());
        }
        return response.getResponse().body().byteStream();
    }

    @Override
    public final OutputStream getOutputStream() throws IOException {
        connect();
        BufferedSink sink = this.httpEngine.getBufferedRequestBody();
        if (sink == null) {
            throw new ProtocolException("method does not support a request body: " + this.method);
        }
        if (this.httpEngine.hasResponse()) {
            throw new ProtocolException("cannot write request body after response has been read");
        }
        return sink.outputStream();
    }

    @Override
    public final Permission getPermission() throws IOException {
        int hostPort;
        URL url = getURL();
        String hostName = url.getHost();
        if (url.getPort() != -1) {
            hostPort = url.getPort();
        } else {
            hostPort = HttpUrl.defaultPort(url.getProtocol());
        }
        if (usingProxy()) {
            InetSocketAddress proxyAddress = (InetSocketAddress) this.client.getProxy().address();
            hostName = proxyAddress.getHostName();
            hostPort = proxyAddress.getPort();
        }
        return new SocketPermission(hostName + ":" + hostPort, "connect, resolve");
    }

    @Override
    public final String getRequestProperty(String field) {
        if (field == null) {
            return null;
        }
        return this.requestHeaders.get(field);
    }

    @Override
    public void setConnectTimeout(int timeoutMillis) {
        this.client.setConnectTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setInstanceFollowRedirects(boolean followRedirects) {
        this.client.setFollowRedirects(followRedirects);
    }

    @Override
    public boolean getInstanceFollowRedirects() {
        return this.client.getFollowRedirects();
    }

    @Override
    public int getConnectTimeout() {
        return this.client.getConnectTimeout();
    }

    @Override
    public void setReadTimeout(int timeoutMillis) {
        this.client.setReadTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public int getReadTimeout() {
        return this.client.getReadTimeout();
    }

    public void setWriteTimeout(int timeoutMillis) {
        this.client.setWriteTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public int getWriteTimeout() {
        return this.client.getWriteTimeout();
    }

    private void initHttpEngine() throws IOException {
        if (this.httpEngineFailure != null) {
            throw this.httpEngineFailure;
        }
        if (this.httpEngine != null) {
            return;
        }
        this.connected = true;
        try {
            if (this.doOutput) {
                if (this.method.equals("GET")) {
                    this.method = "POST";
                } else if (!HttpMethod.permitsRequestBody(this.method)) {
                    throw new ProtocolException(this.method + " does not support writing");
                }
            }
            this.httpEngine = newHttpEngine(this.method, null, null, null);
        } catch (IOException e) {
            this.httpEngineFailure = e;
            throw e;
        }
    }

    private HttpEngine newHttpEngine(String method, Connection connection, RetryableSink requestBody, Response priorResponse) throws MalformedURLException, UnknownHostException {
        RequestBody requestBody2;
        if (HttpMethod.requiresRequestBody(method)) {
            requestBody2 = EMPTY_REQUEST_BODY;
        } else {
            requestBody2 = null;
        }
        URL url = getURL();
        HttpUrl httpUrl = Internal.instance.getHttpUrlChecked(url.toString());
        Request.Builder builder = new Request.Builder().url(httpUrl).method(method, requestBody2);
        Headers headers = this.requestHeaders.build();
        int size = headers.size();
        for (int i = 0; i < size; i++) {
            builder.addHeader(headers.name(i), headers.value(i));
        }
        boolean bufferRequestBody = false;
        if (HttpMethod.permitsRequestBody(method)) {
            if (this.fixedContentLength != -1) {
                builder.header("Content-Length", Long.toString(this.fixedContentLength));
            } else if (this.chunkLength > 0) {
                builder.header("Transfer-Encoding", "chunked");
            } else {
                bufferRequestBody = true;
            }
            if (headers.get("Content-Type") == null) {
                builder.header("Content-Type", "application/x-www-form-urlencoded");
            }
        }
        if (headers.get("User-Agent") == null) {
            builder.header("User-Agent", defaultUserAgent());
        }
        Request request = builder.build();
        OkHttpClient engineClient = this.client;
        if (Internal.instance.internalCache(engineClient) != null && !getUseCaches()) {
            engineClient = this.client.m37clone().setCache(null);
        }
        return new HttpEngine(engineClient, request, bufferRequestBody, true, false, connection, null, requestBody, priorResponse);
    }

    private String defaultUserAgent() {
        String agent = System.getProperty("http.agent");
        return agent != null ? Util.toHumanReadableAscii(agent) : Version.userAgent();
    }

    private HttpEngine getResponse() throws IOException {
        initHttpEngine();
        if (this.httpEngine.hasResponse()) {
            return this.httpEngine;
        }
        long stopTime = System.currentTimeMillis() + 240000;
        while (System.currentTimeMillis() <= stopTime) {
            if (execute(true)) {
                Response response = this.httpEngine.getResponse();
                Request followUp = this.httpEngine.followUpRequest();
                if (followUp == null) {
                    this.httpEngine.releaseConnection();
                    return this.httpEngine;
                }
                int i = this.followUpCount + 1;
                this.followUpCount = i;
                if (i > 20) {
                    throw new ProtocolException("Too many follow-up requests: " + this.followUpCount);
                }
                this.url = followUp.url();
                this.requestHeaders = followUp.headers().newBuilder();
                Sink requestBody = this.httpEngine.getRequestBody();
                if (!followUp.method().equals(this.method)) {
                    requestBody = null;
                }
                if (requestBody != null && !(requestBody instanceof RetryableSink)) {
                    throw new HttpRetryException("Cannot retry streamed HTTP body", this.responseCode);
                }
                if (!this.httpEngine.sameConnection(followUp.httpUrl())) {
                    this.httpEngine.releaseConnection();
                }
                Connection connection = this.httpEngine.close();
                this.httpEngine = newHttpEngine(followUp.method(), connection, (RetryableSink) requestBody, response);
            }
        }
        System.out.println("Cannot retry due to connection time");
        throw new HttpRetryException("Cannot retry due to connection time", 403);
    }

    private boolean execute(boolean readResponse) throws IOException {
        if (this.urlFilter != null) {
            this.urlFilter.checkURLPermitted(this.httpEngine.getRequest().url());
        }
        try {
            this.httpEngine.sendRequest();
            this.route = this.httpEngine.getRoute();
            this.handshake = this.httpEngine.getConnection() != null ? this.httpEngine.getConnection().getHandshake() : null;
            if (readResponse) {
                this.httpEngine.readResponse();
                return true;
            }
            return true;
        } catch (RequestException e) {
            IOException toThrow = e.getCause();
            this.httpEngineFailure = toThrow;
            throw toThrow;
        } catch (RouteException e2) {
            HttpEngine retryEngine = this.httpEngine.recover(e2);
            if (retryEngine != null) {
                this.httpEngine = retryEngine;
                return false;
            }
            IOException toThrow2 = e2.getLastConnectException();
            this.httpEngineFailure = toThrow2;
            throw toThrow2;
        } catch (IOException e3) {
            HttpEngine retryEngine2 = this.httpEngine.recover(e3);
            if (retryEngine2 != null) {
                this.httpEngine = retryEngine2;
                return false;
            }
            this.httpEngineFailure = e3;
            throw e3;
        }
    }

    @Override
    public final boolean usingProxy() {
        Proxy proxy;
        if (this.route != null) {
            proxy = this.route.getProxy();
        } else {
            proxy = this.client.getProxy();
        }
        return (proxy == null || proxy.type() == Proxy.Type.DIRECT) ? false : true;
    }

    @Override
    public String getResponseMessage() throws IOException {
        return getResponse().getResponse().message();
    }

    @Override
    public final int getResponseCode() throws IOException {
        return getResponse().getResponse().code();
    }

    @Override
    public final void setRequestProperty(String field, String newValue) {
        if (this.connected) {
            throw new IllegalStateException("Cannot set request property after connection is made");
        }
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        if (newValue == null) {
            Platform.get().logW("Ignoring header " + field + " because its value was null.");
        } else if ("X-Android-Transports".equals(field) || "X-Android-Protocols".equals(field)) {
            setProtocols(newValue, false);
        } else {
            this.requestHeaders.set(field, newValue);
        }
    }

    @Override
    public void setIfModifiedSince(long newValue) {
        super.setIfModifiedSince(newValue);
        if (this.ifModifiedSince != 0) {
            this.requestHeaders.set("If-Modified-Since", HttpDate.format(new Date(this.ifModifiedSince)));
        } else {
            this.requestHeaders.removeAll("If-Modified-Since");
        }
    }

    @Override
    public final void addRequestProperty(String field, String value) {
        if (this.connected) {
            throw new IllegalStateException("Cannot add request property after connection is made");
        }
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        if (value == null) {
            Platform.get().logW("Ignoring header " + field + " because its value was null.");
        } else if ("X-Android-Transports".equals(field) || "X-Android-Protocols".equals(field)) {
            setProtocols(value, true);
        } else {
            this.requestHeaders.add(field, value);
        }
    }

    private void setProtocols(String protocolsString, boolean append) {
        List<Protocol> protocolsList = new ArrayList<>();
        if (append) {
            protocolsList.addAll(this.client.getProtocols());
        }
        for (String protocol : protocolsString.split(",", -1)) {
            try {
                protocolsList.add(com.android.okhttp.Protocol.get(protocol));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        this.client.setProtocols(protocolsList);
    }

    @Override
    public void setRequestMethod(String method) throws ProtocolException {
        if (!METHODS.contains(method)) {
            throw new ProtocolException("Expected one of " + METHODS + " but was " + method);
        }
        this.method = method;
    }

    @Override
    public void setFixedLengthStreamingMode(int contentLength) {
        setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setFixedLengthStreamingMode(long contentLength) {
        if (((HttpURLConnection) this).connected) {
            throw new IllegalStateException("Already connected");
        }
        if (this.chunkLength > 0) {
            throw new IllegalStateException("Already in chunked mode");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength < 0");
        }
        this.fixedContentLength = contentLength;
        ((HttpURLConnection) this).fixedContentLength = (int) Math.min(contentLength, 2147483647L);
    }
}
