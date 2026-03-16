package com.android.okhttp;

import com.android.okhttp.Failure;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.internal.NamedRunnable;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.http.HttpAuthenticator;
import com.android.okhttp.internal.http.HttpEngine;
import com.android.okhttp.internal.http.OkHeaders;
import com.android.okhttp.internal.http.StatusLine;
import com.android.okio.BufferedSink;
import com.android.okio.Okio;
import com.android.okio.Source;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;

final class Job extends NamedRunnable {
    volatile boolean canceled;
    private final OkHttpClient client;
    private final Dispatcher dispatcher;
    HttpEngine engine;
    private int redirectionCount;
    private Request request;
    private final Response.Receiver responseReceiver;

    public Job(Dispatcher dispatcher, OkHttpClient client, Request request, Response.Receiver responseReceiver) {
        super("OkHttp %s", request.urlString());
        this.dispatcher = dispatcher;
        this.client = client;
        this.request = request;
        this.responseReceiver = responseReceiver;
    }

    String host() {
        return this.request.url().getHost();
    }

    Request request() {
        return this.request;
    }

    Object tag() {
        return this.request.tag();
    }

    @Override
    protected void execute() {
        try {
            Response response = getResponse();
            if (response != null && !this.canceled) {
                this.responseReceiver.onResponse(response);
            }
        } catch (IOException e) {
            this.responseReceiver.onFailure(new Failure.Builder().request(this.request).exception(e).build());
        } finally {
            this.engine.close();
            this.dispatcher.finished(this);
        }
    }

    Response getResponse() throws IOException {
        Response response;
        Request redirect;
        Response redirectedBy = null;
        Request.Body body = this.request.body();
        if (body != null) {
            MediaType contentType = body.contentType();
            if (contentType == null) {
                throw new IllegalStateException("contentType == null");
            }
            Request.Builder requestBuilder = this.request.newBuilder();
            requestBuilder.header("Content-Type", contentType.toString());
            long contentLength = body.contentLength();
            if (contentLength != -1) {
                requestBuilder.header("Content-Length", Long.toString(contentLength));
                requestBuilder.removeHeader("Transfer-Encoding");
            } else {
                requestBuilder.header("Transfer-Encoding", "chunked");
                requestBuilder.removeHeader("Content-Length");
            }
            this.request = requestBuilder.build();
        }
        this.engine = new HttpEngine(this.client, this.request, false, null, null, null, null);
        while (!this.canceled) {
            try {
                this.engine.sendRequest();
                if (body != null) {
                    BufferedSink sink = Okio.buffer(this.engine.getRequestBody());
                    body.writeTo(sink);
                    sink.flush();
                }
                this.engine.readResponse();
                response = this.engine.getResponse();
                redirect = processResponse(this.engine, response);
            } catch (IOException e) {
                HttpEngine retryEngine = this.engine.recover(e);
                if (retryEngine != null) {
                    this.engine = retryEngine;
                } else {
                    throw e;
                }
            }
            if (redirect == null) {
                this.engine.releaseConnection();
                return response.newBuilder().body(new RealResponseBody(response, this.engine.getResponseBody())).priorResponse(redirectedBy).build();
            }
            if (!sameConnection(this.request, redirect)) {
                this.engine.releaseConnection();
            }
            Connection connection = this.engine.close();
            redirectedBy = response.newBuilder().priorResponse(redirectedBy).build();
            this.request = redirect;
            this.engine = new HttpEngine(this.client, this.request, false, connection, null, null, null);
        }
        return null;
    }

    private Request processResponse(HttpEngine engine, Response response) throws IOException {
        String location;
        Request request = response.request();
        Proxy selectedProxy = engine.getRoute() != null ? engine.getRoute().getProxy() : this.client.getProxy();
        int responseCode = response.code();
        switch (responseCode) {
            case 300:
            case 301:
            case 302:
            case 303:
            case StatusLine.HTTP_TEMP_REDIRECT:
                if (!this.client.getFollowProtocolRedirects()) {
                    return null;
                }
                int i = this.redirectionCount + 1;
                this.redirectionCount = i;
                if (i > 20) {
                    throw new ProtocolException("Too many redirects: " + this.redirectionCount);
                }
                String method = request.method();
                if ((responseCode == 307 && !method.equals("GET") && !method.equals("HEAD")) || (location = response.header("Location")) == null) {
                    return null;
                }
                URL url = new URL(request.url(), location);
                if (url.getProtocol().equals("https") || url.getProtocol().equals("http")) {
                    return this.request.newBuilder().url(url).build();
                }
                return null;
            case 401:
                break;
            case 407:
                if (selectedProxy.type() != Proxy.Type.HTTP) {
                    throw new ProtocolException("Received HTTP_PROXY_AUTH (407) code while not using proxy");
                }
                break;
            default:
                return null;
        }
        return HttpAuthenticator.processAuthHeader(this.client.getAuthenticator(), response, selectedProxy);
    }

    static boolean sameConnection(Request a, Request b) {
        return a.url().getHost().equals(b.url().getHost()) && Util.getEffectivePort(a.url()) == Util.getEffectivePort(b.url()) && a.url().getProtocol().equals(b.url().getProtocol());
    }

    static class RealResponseBody extends Response.Body {
        private InputStream in;
        private final Response response;
        private final Source source;

        RealResponseBody(Response response, Source source) {
            this.response = response;
            this.source = source;
        }

        @Override
        public boolean ready() throws IOException {
            return true;
        }

        @Override
        public MediaType contentType() {
            String contentType = this.response.header("Content-Type");
            if (contentType != null) {
                return MediaType.parse(contentType);
            }
            return null;
        }

        @Override
        public long contentLength() {
            return OkHeaders.contentLength(this.response);
        }

        @Override
        public Source source() {
            return this.source;
        }

        @Override
        public InputStream byteStream() {
            InputStream result = this.in;
            if (result != null) {
                return result;
            }
            InputStream result2 = Okio.buffer(this.source).inputStream();
            this.in = result2;
            return result2;
        }
    }
}
