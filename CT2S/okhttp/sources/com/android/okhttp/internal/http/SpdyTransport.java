package com.android.okhttp.internal.http;

import com.android.okhttp.Headers;
import com.android.okhttp.Protocol;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.spdy.ErrorCode;
import com.android.okhttp.internal.spdy.Header;
import com.android.okhttp.internal.spdy.SpdyConnection;
import com.android.okhttp.internal.spdy.SpdyStream;
import com.android.okio.ByteString;
import com.android.okio.Deadline;
import com.android.okio.OkBuffer;
import com.android.okio.Okio;
import com.android.okio.Sink;
import com.android.okio.Source;
import java.io.IOException;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SpdyTransport implements Transport {
    private final HttpEngine httpEngine;
    private final SpdyConnection spdyConnection;
    private SpdyStream stream;
    private static final List<ByteString> SPDY_3_PROHIBITED_HEADERS = Util.immutableList(ByteString.encodeUtf8("connection"), ByteString.encodeUtf8("host"), ByteString.encodeUtf8("keep-alive"), ByteString.encodeUtf8("proxy-connection"), ByteString.encodeUtf8("transfer-encoding"));
    private static final List<ByteString> HTTP_2_PROHIBITED_HEADERS = Util.immutableList(ByteString.encodeUtf8("connection"), ByteString.encodeUtf8("host"), ByteString.encodeUtf8("keep-alive"), ByteString.encodeUtf8("proxy-connection"), ByteString.encodeUtf8("te"), ByteString.encodeUtf8("transfer-encoding"), ByteString.encodeUtf8("encoding"), ByteString.encodeUtf8("upgrade"));

    public SpdyTransport(HttpEngine httpEngine, SpdyConnection spdyConnection) {
        this.httpEngine = httpEngine;
        this.spdyConnection = spdyConnection;
    }

    @Override
    public Sink createRequestBody(Request request) throws IOException {
        writeRequestHeaders(request);
        return this.stream.getSink();
    }

    @Override
    public void writeRequestHeaders(Request request) throws IOException {
        if (this.stream == null) {
            this.httpEngine.writingRequestHeaders();
            boolean hasRequestBody = this.httpEngine.hasRequestBody();
            String version = RequestLine.version(this.httpEngine.getConnection().getHttpMinorVersion());
            this.stream = this.spdyConnection.newStream(writeNameValueBlock(request, this.spdyConnection.getProtocol(), version), hasRequestBody, true);
            this.stream.setReadTimeout(this.httpEngine.client.getReadTimeout());
        }
    }

    @Override
    public void writeRequestBody(RetryableSink requestBody) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushRequest() throws IOException {
        this.stream.getSink().close();
    }

    @Override
    public Response.Builder readResponseHeaders() throws IOException {
        return readNameValueBlock(this.stream.getResponseHeaders(), this.spdyConnection.getProtocol());
    }

    public static List<Header> writeNameValueBlock(Request request, Protocol protocol, String version) {
        Headers headers = request.headers();
        List<Header> result = new ArrayList<>(headers.size() + 10);
        result.add(new Header(Header.TARGET_METHOD, request.method()));
        result.add(new Header(Header.TARGET_PATH, RequestLine.requestPath(request.url())));
        String host = HttpEngine.hostHeader(request.url());
        if (Protocol.SPDY_3 == protocol) {
            result.add(new Header(Header.VERSION, version));
            result.add(new Header(Header.TARGET_HOST, host));
        } else if (Protocol.HTTP_2 == protocol) {
            result.add(new Header(Header.TARGET_AUTHORITY, host));
        } else {
            throw new AssertionError();
        }
        result.add(new Header(Header.TARGET_SCHEME, request.url().getProtocol()));
        Set<ByteString> names = new LinkedHashSet<>();
        for (int i = 0; i < headers.size(); i++) {
            ByteString name = ByteString.encodeUtf8(headers.name(i).toLowerCase(Locale.US));
            String value = headers.value(i);
            if (!isProhibitedHeader(protocol, name) && !name.equals(Header.TARGET_METHOD) && !name.equals(Header.TARGET_PATH) && !name.equals(Header.TARGET_SCHEME) && !name.equals(Header.TARGET_AUTHORITY) && !name.equals(Header.TARGET_HOST) && !name.equals(Header.VERSION)) {
                if (names.add(name)) {
                    result.add(new Header(name, value));
                } else {
                    int j = 0;
                    while (true) {
                        if (j >= result.size()) {
                            break;
                        }
                        if (!result.get(j).name.equals(name)) {
                            j++;
                        } else {
                            String concatenated = joinOnNull(result.get(j).value.utf8(), value);
                            result.set(j, new Header(name, concatenated));
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private static String joinOnNull(String first, String second) {
        return first + (char) 0 + second;
    }

    public static Response.Builder readNameValueBlock(List<Header> headerBlock, Protocol protocol) throws IOException {
        String status = null;
        String version = "HTTP/1.1";
        Headers.Builder headersBuilder = new Headers.Builder();
        headersBuilder.set(OkHeaders.SELECTED_PROTOCOL, protocol.name.utf8());
        for (int i = 0; i < headerBlock.size(); i++) {
            ByteString name = headerBlock.get(i).name;
            String values = headerBlock.get(i).value.utf8();
            int start = 0;
            while (start < values.length()) {
                int end = values.indexOf(0, start);
                if (end == -1) {
                    end = values.length();
                }
                String value = values.substring(start, end);
                if (name.equals(Header.RESPONSE_STATUS)) {
                    status = value;
                } else if (name.equals(Header.VERSION)) {
                    version = value;
                } else if (!isProhibitedHeader(protocol, name)) {
                    headersBuilder.add(name.utf8(), value);
                }
                start = end + 1;
            }
        }
        if (status == null) {
            throw new ProtocolException("Expected ':status' header not present");
        }
        if (version == null) {
            throw new ProtocolException("Expected ':version' header not present");
        }
        return new Response.Builder().statusLine(new StatusLine(version + " " + status)).headers(headersBuilder.build());
    }

    @Override
    public void emptyTransferStream() {
    }

    @Override
    public Source getTransferStream(CacheRequest cacheRequest) throws IOException {
        return new SpdySource(this.stream, cacheRequest);
    }

    @Override
    public void releaseConnectionOnIdle() {
    }

    @Override
    public void disconnect(HttpEngine engine) throws IOException {
        this.stream.close(ErrorCode.CANCEL);
    }

    @Override
    public boolean canReuseConnection() {
        return true;
    }

    private static boolean isProhibitedHeader(Protocol protocol, ByteString name) {
        if (protocol == Protocol.SPDY_3) {
            return SPDY_3_PROHIBITED_HEADERS.contains(name);
        }
        if (protocol == Protocol.HTTP_2) {
            return HTTP_2_PROHIBITED_HEADERS.contains(name);
        }
        throw new AssertionError(protocol);
    }

    private static class SpdySource implements Source {
        private final OutputStream cacheBody;
        private final CacheRequest cacheRequest;
        private boolean closed;
        private boolean inputExhausted;
        private final Source source;
        private final SpdyStream stream;

        SpdySource(SpdyStream stream, CacheRequest cacheRequest) throws IOException {
            this.stream = stream;
            this.source = stream.getSource();
            OutputStream cacheBody = cacheRequest != null ? cacheRequest.getBody() : null;
            cacheRequest = cacheBody == null ? null : cacheRequest;
            this.cacheBody = cacheBody;
            this.cacheRequest = cacheRequest;
        }

        @Override
        public long read(OkBuffer sink, long byteCount) throws IOException {
            if (byteCount < 0) {
                throw new IllegalArgumentException("byteCount < 0: " + byteCount);
            }
            if (this.closed) {
                throw new IllegalStateException("closed");
            }
            if (this.inputExhausted) {
                return -1L;
            }
            long read = this.source.read(sink, byteCount);
            if (read == -1) {
                this.inputExhausted = true;
                if (this.cacheRequest != null) {
                    this.cacheBody.close();
                }
                return -1L;
            }
            if (this.cacheBody != null) {
                Okio.copy(sink, sink.size() - read, read, this.cacheBody);
                return read;
            }
            return read;
        }

        @Override
        public Source mo2deadline(Deadline deadline) {
            this.source.mo2deadline(deadline);
            return this;
        }

        @Override
        public void close() throws IOException {
            if (!this.closed) {
                if (!this.inputExhausted && this.cacheBody != null) {
                    discardStream();
                }
                this.closed = true;
                if (!this.inputExhausted) {
                    this.stream.closeLater(ErrorCode.CANCEL);
                    if (this.cacheRequest != null) {
                        this.cacheRequest.abort();
                    }
                }
            }
        }

        private boolean discardStream() {
            try {
                long socketTimeout = this.stream.getReadTimeoutMillis();
                this.stream.setReadTimeout(socketTimeout);
                this.stream.setReadTimeout(100L);
                try {
                    Util.skipAll(this, 100);
                    return true;
                } finally {
                    this.stream.setReadTimeout(socketTimeout);
                }
            } catch (IOException e) {
                return false;
            }
        }
    }
}
