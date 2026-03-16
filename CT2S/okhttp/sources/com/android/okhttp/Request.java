package com.android.okhttp;

import com.android.okhttp.Headers;
import com.android.okhttp.internal.Platform;
import com.android.okhttp.internal.Util;
import com.android.okio.BufferedSink;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public final class Request {
    private final Body body;
    private volatile CacheControl cacheControl;
    private final Headers headers;
    private final String method;
    private volatile ParsedHeaders parsedHeaders;
    private final Object tag;
    private volatile URI uri;
    private final URL url;

    private Request(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers.build();
        this.body = builder.body;
        this.tag = builder.tag != null ? builder.tag : this;
    }

    public URL url() {
        return this.url;
    }

    public URI uri() throws IOException {
        try {
            URI result = this.uri;
            if (result != null) {
                return result;
            }
            URI result2 = Platform.get().toUriLenient(this.url);
            this.uri = result2;
            return result2;
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage());
        }
    }

    public String urlString() {
        return this.url.toString();
    }

    public String method() {
        return this.method;
    }

    public Headers headers() {
        return this.headers;
    }

    public String header(String name) {
        return this.headers.get(name);
    }

    public List<String> headers(String name) {
        return this.headers.values(name);
    }

    public Body body() {
        return this.body;
    }

    public Object tag() {
        return this.tag;
    }

    public Builder newBuilder() {
        return new Builder();
    }

    public Headers getHeaders() {
        return this.headers;
    }

    public String getUserAgent() {
        return parsedHeaders().userAgent;
    }

    public String getProxyAuthorization() {
        return parsedHeaders().proxyAuthorization;
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

    public boolean isHttps() {
        return url().getProtocol().equals("https");
    }

    private static class ParsedHeaders {
        private String proxyAuthorization;
        private String userAgent;

        public ParsedHeaders(Headers headers) {
            for (int i = 0; i < headers.size(); i++) {
                String fieldName = headers.name(i);
                String value = headers.value(i);
                if ("User-Agent".equalsIgnoreCase(fieldName)) {
                    this.userAgent = value;
                } else if ("Proxy-Authorization".equalsIgnoreCase(fieldName)) {
                    this.proxyAuthorization = value;
                }
            }
        }
    }

    public static abstract class Body {
        public abstract MediaType contentType();

        public abstract void writeTo(BufferedSink bufferedSink) throws IOException;

        public long contentLength() {
            return -1L;
        }

        public static Body create(MediaType contentType, String content) {
            if (contentType.charset() == null) {
                contentType = MediaType.parse(contentType + "; charset=utf-8");
            }
            try {
                byte[] bytes = content.getBytes(contentType.charset().name());
                return create(contentType, bytes);
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError();
            }
        }

        public static Body create(final MediaType contentType, final byte[] content) {
            if (contentType == null) {
                throw new NullPointerException("contentType == null");
            }
            if (content == null) {
                throw new NullPointerException("content == null");
            }
            return new Body() {
                @Override
                public MediaType contentType() {
                    return contentType;
                }

                @Override
                public long contentLength() {
                    return content.length;
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    sink.write(content);
                }
            };
        }

        public static Body create(final MediaType contentType, final File file) {
            if (contentType == null) {
                throw new NullPointerException("contentType == null");
            }
            if (file == null) {
                throw new NullPointerException("content == null");
            }
            return new Body() {
                @Override
                public MediaType contentType() {
                    return contentType;
                }

                @Override
                public long contentLength() {
                    return file.length();
                }

                @Override
                public void writeTo(BufferedSink sink) throws Throwable {
                    InputStream in;
                    long length = contentLength();
                    if (length != 0) {
                        InputStream in2 = null;
                        try {
                            in = new FileInputStream(file);
                        } catch (Throwable th) {
                            th = th;
                        }
                        try {
                            byte[] buffer = new byte[(int) Math.min(8192L, length)];
                            while (true) {
                                int c = in.read(buffer);
                                if (c != -1) {
                                    sink.write(buffer, 0, c);
                                } else {
                                    Util.closeQuietly(in);
                                    return;
                                }
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            in2 = in;
                            Util.closeQuietly(in2);
                            throw th;
                        }
                    }
                }
            };
        }
    }

    public static class Builder {
        private Body body;
        private Headers.Builder headers;
        private String method;
        private Object tag;
        private URL url;

        public Builder() {
            this.method = "GET";
            this.headers = new Headers.Builder();
        }

        private Builder(Request request) {
            this.url = request.url;
            this.method = request.method;
            this.body = request.body;
            this.tag = request.tag;
            this.headers = request.headers.newBuilder();
        }

        public Builder url(String url) {
            try {
                return url(new URL(url));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Malformed URL: " + url);
            }
        }

        public Builder url(URL url) {
            if (url == null) {
                throw new IllegalArgumentException("url == null");
            }
            this.url = url;
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

        public Builder setUserAgent(String userAgent) {
            return header("User-Agent", userAgent);
        }

        public Builder get() {
            return method("GET", null);
        }

        public Builder head() {
            return method("HEAD", null);
        }

        public Builder post(Body body) {
            return method("POST", body);
        }

        public Builder put(Body body) {
            return method("PUT", body);
        }

        public Builder method(String method, Body body) {
            if (method == null || method.length() == 0) {
                throw new IllegalArgumentException("method == null || method.length() == 0");
            }
            this.method = method;
            this.body = body;
            return this;
        }

        public Builder tag(Object tag) {
            this.tag = tag;
            return this;
        }

        public Request build() {
            if (this.url == null) {
                throw new IllegalStateException("url == null");
            }
            return new Request(this);
        }
    }
}
