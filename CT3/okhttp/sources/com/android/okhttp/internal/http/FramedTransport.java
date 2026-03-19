package com.android.okhttp.internal.http;

import com.android.okhttp.Headers;
import com.android.okhttp.Protocol;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.ResponseBody;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.framed.ErrorCode;
import com.android.okhttp.internal.framed.FramedConnection;
import com.android.okhttp.internal.framed.FramedStream;
import com.android.okhttp.internal.framed.Header;
import com.android.okhttp.okio.ByteString;
import com.android.okhttp.okio.Okio;
import com.android.okhttp.okio.Sink;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class FramedTransport implements Transport {
    private final FramedConnection framedConnection;
    private final HttpEngine httpEngine;
    private FramedStream stream;
    private static final List<ByteString> SPDY_3_PROHIBITED_HEADERS = Util.immutableList(ByteString.encodeUtf8("connection"), ByteString.encodeUtf8("host"), ByteString.encodeUtf8("keep-alive"), ByteString.encodeUtf8("proxy-connection"), ByteString.encodeUtf8("transfer-encoding"));
    private static final List<ByteString> HTTP_2_PROHIBITED_HEADERS = Util.immutableList(ByteString.encodeUtf8("connection"), ByteString.encodeUtf8("host"), ByteString.encodeUtf8("keep-alive"), ByteString.encodeUtf8("proxy-connection"), ByteString.encodeUtf8("te"), ByteString.encodeUtf8("transfer-encoding"), ByteString.encodeUtf8("encoding"), ByteString.encodeUtf8("upgrade"));

    public FramedTransport(HttpEngine httpEngine, FramedConnection framedConnection) {
        this.httpEngine = httpEngine;
        this.framedConnection = framedConnection;
    }

    @Override
    public Sink createRequestBody(Request request, long contentLength) throws IOException {
        return this.stream.getSink();
    }

    @Override
    public void writeRequestHeaders(Request request) throws IOException {
        if (this.stream != null) {
            return;
        }
        this.httpEngine.writingRequestHeaders();
        boolean permitsRequestBody = this.httpEngine.permitsRequestBody();
        String version = RequestLine.version(this.httpEngine.getConnection().getProtocol());
        this.stream = this.framedConnection.newStream(writeNameValueBlock(request, this.framedConnection.getProtocol(), version), permitsRequestBody, true);
        this.stream.readTimeout().timeout(this.httpEngine.client.getReadTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void writeRequestBody(RetryableSink requestBody) throws IOException {
        requestBody.writeToSocket(this.stream.getSink());
    }

    @Override
    public void finishRequest() throws IOException {
        this.stream.getSink().close();
    }

    @Override
    public Response.Builder readResponseHeaders() throws IOException {
        return readNameValueBlock(this.stream.getResponseHeaders(), this.framedConnection.getProtocol());
    }

    public static List<Header> writeNameValueBlock(Request request, Protocol protocol, String version) {
        Headers headers = request.headers();
        List<com.squareup.okhttp.internal.framed.Header> result = new ArrayList<>(headers.size() + 10);
        result.add(new Header(Header.TARGET_METHOD, request.method()));
        result.add(new Header(Header.TARGET_PATH, RequestLine.requestPath(request.httpUrl())));
        String host = Util.hostHeader(request.httpUrl());
        if (Protocol.SPDY_3 == protocol) {
            result.add(new Header(Header.VERSION, version));
            result.add(new Header(Header.TARGET_HOST, host));
        } else {
            if (Protocol.HTTP_2 != protocol) {
                throw new AssertionError();
            }
            result.add(new Header(Header.TARGET_AUTHORITY, host));
        }
        result.add(new Header(Header.TARGET_SCHEME, request.httpUrl().scheme()));
        Set<okio.ByteString> names = new LinkedHashSet<>();
        int size = headers.size();
        for (int i = 0; i < size; i++) {
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
                        if (((Header) result.get(j)).name.equals(name)) {
                            String concatenated = joinOnNull(((Header) result.get(j)).value.utf8(), value);
                            result.set(j, new Header(name, concatenated));
                            break;
                        }
                        j++;
                    }
                }
            }
        }
        return result;
    }

    private static String joinOnNull(String first, String second) {
        return first + (char) 0 + second;
    }

    public static Response.Builder readNameValueBlock(List<Header> list, Protocol protocol) throws IOException {
        String status = null;
        String version = "HTTP/1.1";
        Headers.Builder headersBuilder = new Headers.Builder();
        headersBuilder.set(OkHeaders.SELECTED_PROTOCOL, protocol.toString());
        int size = list.size();
        for (int i = 0; i < size; i++) {
            ByteString name = list.get(i).name;
            String values = list.get(i).value.utf8();
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
        StatusLine statusLine = StatusLine.parse(version + " " + status);
        return new Response.Builder().protocol(protocol).code(statusLine.code).message(statusLine.message).headers(headersBuilder.build());
    }

    @Override
    public ResponseBody openResponseBody(Response response) throws IOException {
        return new RealResponseBody(response.headers(), Okio.buffer(this.stream.getSource()));
    }

    @Override
    public void releaseConnectionOnIdle() {
    }

    @Override
    public void disconnect(HttpEngine engine) throws IOException {
        if (this.stream != null) {
            this.stream.close(ErrorCode.CANCEL);
        }
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
}
