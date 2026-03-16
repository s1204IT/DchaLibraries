package com.android.okhttp;

import com.android.okhttp.Headers;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.http.HttpDate;
import com.android.okhttp.internal.http.OkHeaders;
import com.android.okhttp.internal.http.StatusLine;
import com.android.okio.Okio;
import com.android.okio.Source;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class Response {
    private final Body body;
    private volatile CacheControl cacheControl;
    private Response cacheResponse;
    private final Handshake handshake;
    private final Headers headers;
    private Response networkResponse;
    private volatile ParsedHeaders parsedHeaders;
    private final Response priorResponse;
    private final Request request;
    private final StatusLine statusLine;

    public interface Receiver {
        void onFailure(Failure failure);

        boolean onResponse(Response response) throws IOException;
    }

    private Response(Builder builder) {
        this.request = builder.request;
        this.statusLine = builder.statusLine;
        this.handshake = builder.handshake;
        this.headers = builder.headers.build();
        this.body = builder.body;
        this.networkResponse = builder.networkResponse;
        this.cacheResponse = builder.cacheResponse;
        this.priorResponse = builder.priorResponse;
    }

    public Request request() {
        return this.request;
    }

    public String statusLine() {
        return this.statusLine.getStatusLine();
    }

    public int code() {
        return this.statusLine.code();
    }

    public String statusMessage() {
        return this.statusLine.message();
    }

    public int httpMinorVersion() {
        return this.statusLine.httpMinorVersion();
    }

    public Handshake handshake() {
        return this.handshake;
    }

    public List<String> headers(String name) {
        return this.headers.values(name);
    }

    public String header(String name) {
        return header(name, null);
    }

    public String header(String name, String defaultValue) {
        String result = this.headers.get(name);
        return result != null ? result : defaultValue;
    }

    public Headers headers() {
        return this.headers;
    }

    public Body body() {
        return this.body;
    }

    public Builder newBuilder() {
        return new Builder();
    }

    public Response priorResponse() {
        return this.priorResponse;
    }

    public Response networkResponse() {
        return this.networkResponse;
    }

    public Response cacheResponse() {
        return this.cacheResponse;
    }

    public Set<String> getVaryFields() {
        return parsedHeaders().varyFields;
    }

    public boolean hasVaryAll() {
        return parsedHeaders().varyFields.contains("*");
    }

    public boolean varyMatches(Headers varyHeaders, Request newRequest) {
        for (String field : parsedHeaders().varyFields) {
            if (!Util.equal(varyHeaders.values(field), newRequest.headers(field))) {
                return false;
            }
        }
        return true;
    }

    public boolean validate(Response network) {
        if (network.code() == 304) {
            return true;
        }
        ParsedHeaders networkHeaders = network.parsedHeaders();
        return (parsedHeaders().lastModified == null || networkHeaders.lastModified == null || networkHeaders.lastModified.getTime() >= parsedHeaders().lastModified.getTime()) ? false : true;
    }

    public static abstract class Body implements Closeable {
        private Reader reader;
        private Source source;

        public abstract InputStream byteStream();

        public abstract long contentLength();

        public abstract MediaType contentType();

        public abstract boolean ready() throws IOException;

        public Source source() {
            Source s = this.source;
            if (s != null) {
                return s;
            }
            Source s2 = Okio.source(byteStream());
            this.source = s2;
            return s2;
        }

        public final byte[] bytes() throws IOException {
            long contentLength = contentLength();
            if (contentLength > 2147483647L) {
                throw new IOException("Cannot buffer entire body for content length: " + contentLength);
            }
            if (contentLength != -1) {
                byte[] content = new byte[(int) contentLength];
                InputStream in = byteStream();
                Util.readFully(in, content);
                if (in.read() != -1) {
                    throw new IOException("Content-Length and stream length disagree");
                }
                return content;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Util.copy(byteStream(), out);
            return out.toByteArray();
        }

        public final Reader charStream() {
            Reader r = this.reader;
            if (r != null) {
                return r;
            }
            Reader r2 = new InputStreamReader(byteStream(), charset());
            this.reader = r2;
            return r2;
        }

        public final String string() throws IOException {
            return new String(bytes(), charset().name());
        }

        private Charset charset() {
            MediaType contentType = contentType();
            return contentType != null ? contentType.charset(Util.UTF_8) : Util.UTF_8;
        }

        @Override
        public void close() throws IOException {
            byteStream().close();
        }
    }

    private ParsedHeaders parsedHeaders() {
        ParsedHeaders result = this.parsedHeaders;
        if (result != null) {
            return result;
        }
        ParsedHeaders result2 = new ParsedHeaders(this.headers);
        this.parsedHeaders = result2;
        return result2;
    }

    public CacheControl cacheControl() {
        CacheControl result = this.cacheControl;
        if (result != null) {
            return result;
        }
        CacheControl result2 = CacheControl.parse(this.headers);
        this.cacheControl = result2;
        return result2;
    }

    private static class ParsedHeaders {
        Date lastModified;
        private Set<String> varyFields;

        private ParsedHeaders(Headers headers) {
            this.varyFields = Collections.emptySet();
            for (int i = 0; i < headers.size(); i++) {
                String fieldName = headers.name(i);
                String value = headers.value(i);
                if ("Last-Modified".equalsIgnoreCase(fieldName)) {
                    this.lastModified = HttpDate.parse(value);
                } else if ("Vary".equalsIgnoreCase(fieldName)) {
                    if (this.varyFields.isEmpty()) {
                        this.varyFields = new TreeSet(String.CASE_INSENSITIVE_ORDER);
                    }
                    String[] arr$ = value.split(",");
                    for (String varyField : arr$) {
                        this.varyFields.add(varyField.trim());
                    }
                }
            }
        }
    }

    public static class Builder {
        private Body body;
        private Response cacheResponse;
        private Handshake handshake;
        private Headers.Builder headers;
        private Response networkResponse;
        private Response priorResponse;
        private Request request;
        private StatusLine statusLine;

        public Builder() {
            this.headers = new Headers.Builder();
        }

        private Builder(Response response) {
            this.request = response.request;
            this.statusLine = response.statusLine;
            this.handshake = response.handshake;
            this.headers = response.headers.newBuilder();
            this.body = response.body;
            this.networkResponse = response.networkResponse;
            this.cacheResponse = response.cacheResponse;
            this.priorResponse = response.priorResponse;
        }

        public Builder request(Request request) {
            this.request = request;
            return this;
        }

        public Builder statusLine(StatusLine statusLine) {
            if (statusLine == null) {
                throw new IllegalArgumentException("statusLine == null");
            }
            this.statusLine = statusLine;
            return this;
        }

        public Builder statusLine(String statusLine) {
            try {
                return statusLine(new StatusLine(statusLine));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public Builder handshake(Handshake handshake) {
            this.handshake = handshake;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.set(name, value);
            return this;
        }

        public Builder addHeader(String name, String value) {
            this.headers.add(name, value);
            return this;
        }

        public Builder removeHeader(String name) {
            this.headers.removeAll(name);
            return this;
        }

        public Builder headers(Headers headers) {
            this.headers = headers.newBuilder();
            return this;
        }

        public Builder body(Body body) {
            this.body = body;
            return this;
        }

        public Builder setResponseSource(ResponseSource responseSource) {
            return header(OkHeaders.RESPONSE_SOURCE, responseSource + " " + this.statusLine.code());
        }

        public Builder networkResponse(Response networkResponse) {
            if (networkResponse != null) {
                checkSupportResponse("networkResponse", networkResponse);
            }
            this.networkResponse = networkResponse;
            return this;
        }

        public Builder cacheResponse(Response cacheResponse) {
            if (cacheResponse != null) {
                checkSupportResponse("cacheResponse", cacheResponse);
            }
            this.cacheResponse = cacheResponse;
            return this;
        }

        private void checkSupportResponse(String name, Response response) {
            if (response.body == null) {
                if (response.networkResponse == null) {
                    if (response.cacheResponse == null) {
                        if (response.priorResponse != null) {
                            throw new IllegalArgumentException(name + ".priorResponse != null");
                        }
                        return;
                    }
                    throw new IllegalArgumentException(name + ".cacheResponse != null");
                }
                throw new IllegalArgumentException(name + ".networkResponse != null");
            }
            throw new IllegalArgumentException(name + ".body != null");
        }

        public Builder priorResponse(Response priorResponse) {
            this.priorResponse = priorResponse;
            return this;
        }

        public Response build() {
            if (this.request == null) {
                throw new IllegalStateException("request == null");
            }
            if (this.statusLine == null) {
                throw new IllegalStateException("statusLine == null");
            }
            return new Response(this);
        }
    }
}
