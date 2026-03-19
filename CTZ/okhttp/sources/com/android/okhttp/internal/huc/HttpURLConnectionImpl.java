package com.android.okhttp.internal.huc;

import com.android.okhttp.Connection;
import com.android.okhttp.Handshake;
import com.android.okhttp.Headers;
import com.android.okhttp.HttpUrl;
import com.android.okhttp.MediaType;
import com.android.okhttp.OkHttpClient;
import com.android.okhttp.Protocol;
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
import com.android.okhttp.internal.http.StreamAllocation;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.Sink;
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

    public HttpURLConnectionImpl(URL url, OkHttpClient okHttpClient) {
        super(url);
        this.requestHeaders = new Headers.Builder();
        this.fixedContentLength = -1L;
        this.client = okHttpClient;
    }

    public HttpURLConnectionImpl(URL url, OkHttpClient okHttpClient, URLFilter uRLFilter) {
        this(url, okHttpClient);
        this.urlFilter = uRLFilter;
    }

    @Override
    public final void connect() throws IOException {
        initHttpEngine();
        while (!execute(false)) {
        }
    }

    @Override
    public final void disconnect() {
        if (this.httpEngine == null) {
            return;
        }
        this.httpEngine.cancel();
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
            this.responseHeaders = response.headers().newBuilder().add(OkHeaders.SELECTED_PROTOCOL, response.protocol().toString()).add(OkHeaders.RESPONSE_SOURCE, responseSourceHeader(response)).build();
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
    public final String getHeaderField(int i) {
        try {
            return getHeaders().value(i);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public final String getHeaderField(String str) {
        String string;
        try {
            if (str == null) {
                string = StatusLine.get(getResponse().getResponse()).toString();
            } else {
                string = getHeaders().get(str);
            }
            return string;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public final String getHeaderFieldKey(int i) {
        try {
            return getHeaders().name(i);
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
        if (getResponseCode() >= 400) {
            throw new FileNotFoundException(this.url.toString());
        }
        return response.getResponse().body().byteStream();
    }

    @Override
    public final OutputStream getOutputStream() throws IOException {
        connect();
        BufferedSink bufferedRequestBody = this.httpEngine.getBufferedRequestBody();
        if (bufferedRequestBody == null) {
            throw new ProtocolException("method does not support a request body: " + this.method);
        }
        if (this.httpEngine.hasResponse()) {
            throw new ProtocolException("cannot write request body after response has been read");
        }
        return bufferedRequestBody.outputStream();
    }

    @Override
    public final Permission getPermission() throws IOException {
        int iDefaultPort;
        URL url = getURL();
        String host = url.getHost();
        if (url.getPort() != -1) {
            iDefaultPort = url.getPort();
        } else {
            iDefaultPort = HttpUrl.defaultPort(url.getProtocol());
        }
        if (usingProxy()) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) this.client.getProxy().address();
            host = inetSocketAddress.getHostName();
            iDefaultPort = inetSocketAddress.getPort();
        }
        return new SocketPermission(host + ":" + iDefaultPort, "connect, resolve");
    }

    @Override
    public final String getRequestProperty(String str) {
        if (str == null) {
            return null;
        }
        return this.requestHeaders.get(str);
    }

    @Override
    public void setConnectTimeout(int i) {
        this.client.setConnectTimeout(i, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setInstanceFollowRedirects(boolean z) {
        this.client.setFollowRedirects(z);
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
    public void setReadTimeout(int i) {
        this.client.setReadTimeout(i, TimeUnit.MILLISECONDS);
    }

    @Override
    public int getReadTimeout() {
        return this.client.getReadTimeout();
    }

    public void setWriteTimeout(int i) {
        this.client.setWriteTimeout(i, TimeUnit.MILLISECONDS);
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

    private HttpEngine newHttpEngine(String str, StreamAllocation streamAllocation, RetryableSink retryableSink, Response response) throws MalformedURLException, UnknownHostException {
        RequestBody requestBody;
        if (HttpMethod.requiresRequestBody(str)) {
            requestBody = EMPTY_REQUEST_BODY;
        } else {
            requestBody = null;
        }
        Request.Builder builderMethod = new Request.Builder().url(Internal.instance.getHttpUrlChecked(getURL().toString())).method(str, requestBody);
        Headers headersBuild = this.requestHeaders.build();
        int size = headersBuild.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            builderMethod.addHeader(headersBuild.name(i), headersBuild.value(i));
        }
        if (HttpMethod.permitsRequestBody(str)) {
            if (this.fixedContentLength != -1) {
                builderMethod.header("Content-Length", Long.toString(this.fixedContentLength));
            } else if (this.chunkLength > 0) {
                builderMethod.header("Transfer-Encoding", "chunked");
            } else {
                z = true;
            }
            if (headersBuild.get("Content-Type") == null) {
                builderMethod.header("Content-Type", "application/x-www-form-urlencoded");
            }
        }
        boolean z2 = z;
        if (headersBuild.get("User-Agent") == null) {
            builderMethod.header("User-Agent", defaultUserAgent());
        }
        Request requestBuild = builderMethod.build();
        OkHttpClient okHttpClient = this.client;
        return new HttpEngine((Internal.instance.internalCache(okHttpClient) == null || getUseCaches()) ? okHttpClient : this.client.m0clone().setCache(null), requestBuild, z2, true, false, streamAllocation, retryableSink, response);
    }

    private String defaultUserAgent() {
        String property = System.getProperty("http.agent");
        return property != null ? Util.toHumanReadableAscii(property) : Version.userAgent();
    }

    private HttpEngine getResponse() throws IOException {
        initHttpEngine();
        if (this.httpEngine.hasResponse()) {
            return this.httpEngine;
        }
        while (true) {
            if (execute(true)) {
                Response response = this.httpEngine.getResponse();
                Request requestFollowUpRequest = this.httpEngine.followUpRequest();
                if (requestFollowUpRequest != null) {
                    int i = this.followUpCount + 1;
                    this.followUpCount = i;
                    if (i > 20) {
                        throw new ProtocolException("Too many follow-up requests: " + this.followUpCount);
                    }
                    this.url = requestFollowUpRequest.url();
                    this.requestHeaders = requestFollowUpRequest.headers().newBuilder();
                    Sink requestBody = this.httpEngine.getRequestBody();
                    if (!requestFollowUpRequest.method().equals(this.method)) {
                        requestBody = null;
                    }
                    if (requestBody != null && !(requestBody instanceof RetryableSink)) {
                        throw new HttpRetryException("Cannot retry streamed HTTP body", this.responseCode);
                    }
                    StreamAllocation streamAllocationClose = this.httpEngine.close();
                    if (!this.httpEngine.sameConnection(requestFollowUpRequest.httpUrl())) {
                        streamAllocationClose.release();
                        streamAllocationClose = null;
                    }
                    this.httpEngine = newHttpEngine(requestFollowUpRequest.method(), streamAllocationClose, (RetryableSink) requestBody, response);
                } else {
                    this.httpEngine.releaseStreamAllocation();
                    return this.httpEngine;
                }
            }
        }
    }

    private boolean execute(boolean z) throws Throwable {
        if (this.urlFilter != null) {
            this.urlFilter.checkURLPermitted(this.httpEngine.getRequest().url());
        }
        boolean z2 = true;
        try {
            try {
                try {
                    try {
                        try {
                            this.httpEngine.sendRequest();
                            Connection connection = this.httpEngine.getConnection();
                            if (connection != null) {
                                this.route = connection.getRoute();
                                this.handshake = connection.getHandshake();
                            } else {
                                this.route = null;
                                this.handshake = null;
                            }
                            if (z) {
                                this.httpEngine.readResponse();
                            }
                            return true;
                        } catch (RouteException e) {
                            HttpEngine httpEngineRecover = this.httpEngine.recover(e);
                            if (httpEngineRecover != null) {
                                this.httpEngine = httpEngineRecover;
                                return false;
                            }
                            IOException lastConnectException = e.getLastConnectException();
                            this.httpEngineFailure = lastConnectException;
                            throw lastConnectException;
                        }
                    } catch (IOException e2) {
                        HttpEngine httpEngineRecover2 = this.httpEngine.recover(e2);
                        if (httpEngineRecover2 != null) {
                            this.httpEngine = httpEngineRecover2;
                            return false;
                        }
                        this.httpEngineFailure = e2;
                        throw e2;
                    }
                } catch (RequestException e3) {
                    IOException cause = e3.getCause();
                    this.httpEngineFailure = cause;
                    throw cause;
                }
            } catch (Throwable th) {
                th = th;
                if (z2) {
                    this.httpEngine.close().release();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            z2 = false;
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
    public final void setRequestProperty(String str, String str2) {
        if (this.connected) {
            throw new IllegalStateException("Cannot set request property after connection is made");
        }
        if (str == null) {
            throw new NullPointerException("field == null");
        }
        if (str2 == null) {
            Platform.get().logW("Ignoring header " + str + " because its value was null.");
            return;
        }
        if ("X-Android-Transports".equals(str) || "X-Android-Protocols".equals(str)) {
            setProtocols(str2, false);
        } else {
            this.requestHeaders.set(str, str2);
        }
    }

    @Override
    public void setIfModifiedSince(long j) {
        super.setIfModifiedSince(j);
        if (this.ifModifiedSince != 0) {
            this.requestHeaders.set("If-Modified-Since", HttpDate.format(new Date(this.ifModifiedSince)));
        } else {
            this.requestHeaders.removeAll("If-Modified-Since");
        }
    }

    @Override
    public final void addRequestProperty(String str, String str2) {
        if (this.connected) {
            throw new IllegalStateException("Cannot add request property after connection is made");
        }
        if (str == null) {
            throw new NullPointerException("field == null");
        }
        if (str2 == null) {
            Platform.get().logW("Ignoring header " + str + " because its value was null.");
            return;
        }
        if ("X-Android-Transports".equals(str) || "X-Android-Protocols".equals(str)) {
            setProtocols(str2, true);
        } else {
            this.requestHeaders.add(str, str2);
        }
    }

    private void setProtocols(String str, boolean z) {
        ArrayList arrayList = new ArrayList();
        if (z) {
            arrayList.addAll(this.client.getProtocols());
        }
        for (String str2 : str.split(",", -1)) {
            try {
                arrayList.add(Protocol.get(str2));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        this.client.setProtocols(arrayList);
    }

    @Override
    public void setRequestMethod(String str) throws ProtocolException {
        if (!METHODS.contains(str)) {
            throw new ProtocolException("Expected one of " + METHODS + " but was " + str);
        }
        this.method = str;
    }

    @Override
    public void setFixedLengthStreamingMode(int i) {
        setFixedLengthStreamingMode(i);
    }

    @Override
    public void setFixedLengthStreamingMode(long j) {
        if (((HttpURLConnection) this).connected) {
            throw new IllegalStateException("Already connected");
        }
        if (this.chunkLength > 0) {
            throw new IllegalStateException("Already in chunked mode");
        }
        if (j < 0) {
            throw new IllegalArgumentException("contentLength < 0");
        }
        this.fixedContentLength = j;
        ((HttpURLConnection) this).fixedContentLength = (int) Math.min(j, 2147483647L);
    }
}
