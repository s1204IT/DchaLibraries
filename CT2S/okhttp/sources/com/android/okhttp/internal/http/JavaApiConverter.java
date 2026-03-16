package com.android.okhttp.internal.http;

import com.android.okhttp.Handshake;
import com.android.okhttp.Headers;
import com.android.okhttp.MediaType;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.ResponseSource;
import com.android.okhttp.internal.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SecureCacheResponse;
import java.net.URI;
import java.net.URLConnection;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

public final class JavaApiConverter {
    private JavaApiConverter() {
    }

    public static Response createOkResponse(URI uri, URLConnection urlConnection) throws IOException {
        Certificate[] peerCertificates;
        HttpURLConnection httpUrlConnection = (HttpURLConnection) urlConnection;
        Response.Builder okResponseBuilder = new Response.Builder();
        Request okRequest = createOkRequest(uri, httpUrlConnection.getRequestMethod(), null);
        okResponseBuilder.request(okRequest);
        String statusLine = extractStatusLine(httpUrlConnection);
        okResponseBuilder.statusLine(statusLine);
        Headers okHeaders = extractOkResponseHeaders(httpUrlConnection);
        okResponseBuilder.headers(okHeaders);
        okResponseBuilder.setResponseSource(ResponseSource.NETWORK);
        Response.Body okBody = createOkBody(okHeaders, urlConnection.getInputStream());
        okResponseBuilder.body(okBody);
        if (httpUrlConnection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) httpUrlConnection;
            try {
                peerCertificates = httpsUrlConnection.getServerCertificates();
            } catch (SSLPeerUnverifiedException e) {
                peerCertificates = null;
            }
            Certificate[] localCertificates = httpsUrlConnection.getLocalCertificates();
            Handshake handshake = Handshake.get(httpsUrlConnection.getCipherSuite(), nullSafeImmutableList(peerCertificates), nullSafeImmutableList(localCertificates));
            okResponseBuilder.handshake(handshake);
        }
        return okResponseBuilder.build();
    }

    static Response createOkResponse(Request request, CacheResponse javaResponse) throws IOException {
        List<Certificate> peerCertificates;
        Response.Builder okResponseBuilder = new Response.Builder();
        okResponseBuilder.request(request);
        okResponseBuilder.statusLine(extractStatusLine(javaResponse));
        Headers okHeaders = extractOkHeaders(javaResponse);
        okResponseBuilder.headers(okHeaders);
        okResponseBuilder.setResponseSource(ResponseSource.CACHE);
        Response.Body okBody = createOkBody(okHeaders, javaResponse.getBody());
        okResponseBuilder.body(okBody);
        if (javaResponse instanceof SecureCacheResponse) {
            SecureCacheResponse javaSecureCacheResponse = (SecureCacheResponse) javaResponse;
            try {
                peerCertificates = javaSecureCacheResponse.getServerCertificateChain();
            } catch (SSLPeerUnverifiedException e) {
                peerCertificates = Collections.emptyList();
            }
            List<Certificate> localCertificates = javaSecureCacheResponse.getLocalCertificateChain();
            if (localCertificates == null) {
                localCertificates = Collections.emptyList();
            }
            Handshake handshake = Handshake.get(javaSecureCacheResponse.getCipherSuite(), peerCertificates, localCertificates);
            okResponseBuilder.handshake(handshake);
        }
        return okResponseBuilder.build();
    }

    public static Request createOkRequest(URI uri, String requestMethod, Map<String, List<String>> requestHeaders) {
        Request.Builder builder = new Request.Builder().url(uri.toString()).method(requestMethod, null);
        if (requestHeaders != null) {
            Headers headers = extractOkHeaders(requestHeaders);
            builder.headers(headers);
        }
        return builder.build();
    }

    public static CacheResponse createJavaCacheResponse(final Response response) {
        final Headers headers = response.headers();
        final Response.Body body = response.body();
        if (!response.request().isHttps()) {
            return new CacheResponse() {
                @Override
                public Map<String, List<String>> getHeaders() throws IOException {
                    return OkHeaders.toMultimap(headers, response.statusLine());
                }

                @Override
                public InputStream getBody() throws IOException {
                    if (body == null) {
                        return null;
                    }
                    return body.byteStream();
                }
            };
        }
        final Handshake handshake = response.handshake();
        return new SecureCacheResponse() {
            @Override
            public String getCipherSuite() {
                if (handshake != null) {
                    return handshake.cipherSuite();
                }
                return null;
            }

            @Override
            public List<Certificate> getLocalCertificateChain() {
                if (handshake == null) {
                    return null;
                }
                List<Certificate> certificates = handshake.localCertificates();
                if (certificates.size() <= 0) {
                    certificates = null;
                }
                return certificates;
            }

            @Override
            public List<Certificate> getServerCertificateChain() throws SSLPeerUnverifiedException {
                if (handshake == null) {
                    return null;
                }
                List<Certificate> certificates = handshake.peerCertificates();
                if (certificates.size() <= 0) {
                    certificates = null;
                }
                return certificates;
            }

            @Override
            public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
                if (handshake == null) {
                    return null;
                }
                return handshake.peerPrincipal();
            }

            @Override
            public Principal getLocalPrincipal() {
                if (handshake == null) {
                    return null;
                }
                return handshake.localPrincipal();
            }

            @Override
            public Map<String, List<String>> getHeaders() throws IOException {
                return OkHeaders.toMultimap(headers, response.statusLine());
            }

            @Override
            public InputStream getBody() throws IOException {
                if (body == null) {
                    return null;
                }
                return body.byteStream();
            }
        };
    }

    static HttpURLConnection createJavaUrlConnection(Response okResponse) {
        Request request = okResponse.request();
        return request.isHttps() ? new CacheHttpsURLConnection(new CacheHttpURLConnection(okResponse)) : new CacheHttpURLConnection(okResponse);
    }

    static Map<String, List<String>> extractJavaHeaders(Request request) {
        return OkHeaders.toMultimap(request.headers(), null);
    }

    private static Headers extractOkHeaders(CacheResponse javaResponse) throws IOException {
        Map<String, List<String>> javaResponseHeaders = javaResponse.getHeaders();
        return extractOkHeaders(javaResponseHeaders);
    }

    private static Headers extractOkResponseHeaders(HttpURLConnection httpUrlConnection) {
        Map<String, List<String>> javaResponseHeaders = httpUrlConnection.getHeaderFields();
        return extractOkHeaders(javaResponseHeaders);
    }

    static Headers extractOkHeaders(Map<String, List<String>> javaHeaders) {
        Headers.Builder okHeadersBuilder = new Headers.Builder();
        for (Map.Entry<String, List<String>> javaHeader : javaHeaders.entrySet()) {
            String name = javaHeader.getKey();
            if (name != null) {
                for (String value : javaHeader.getValue()) {
                    okHeadersBuilder.add(name, value);
                }
            }
        }
        return okHeadersBuilder.build();
    }

    private static String extractStatusLine(HttpURLConnection httpUrlConnection) {
        return httpUrlConnection.getHeaderField((String) null);
    }

    private static String extractStatusLine(CacheResponse javaResponse) throws IOException {
        Map<String, List<String>> javaResponseHeaders = javaResponse.getHeaders();
        return extractStatusLine(javaResponseHeaders);
    }

    static String extractStatusLine(Map<String, List<String>> javaResponseHeaders) {
        List<String> values = javaResponseHeaders.get(null);
        if (values == null || values.size() == 0) {
            return null;
        }
        return values.get(0);
    }

    private static Response.Body createOkBody(final Headers okHeaders, final InputStream body) {
        return new Response.Body() {
            @Override
            public boolean ready() throws IOException {
                return true;
            }

            @Override
            public MediaType contentType() {
                String contentTypeHeader = okHeaders.get("Content-Type");
                if (contentTypeHeader == null) {
                    return null;
                }
                return MediaType.parse(contentTypeHeader);
            }

            @Override
            public long contentLength() {
                return OkHeaders.contentLength(okHeaders);
            }

            @Override
            public InputStream byteStream() {
                return body;
            }
        };
    }

    private static final class CacheHttpURLConnection extends HttpURLConnection {
        private final Request request;
        private final Response response;

        public CacheHttpURLConnection(Response response) {
            super(response.request().url());
            this.request = response.request();
            this.response = response;
            this.connected = true;
            this.doOutput = response.body() == null;
            this.method = this.request.method();
        }

        @Override
        public void connect() throws IOException {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void disconnect() {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void setRequestProperty(String key, String value) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void addRequestProperty(String key, String value) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public String getRequestProperty(String key) {
            return this.request.header(key);
        }

        @Override
        public Map<String, List<String>> getRequestProperties() {
            throw JavaApiConverter.throwRequestHeaderAccessException();
        }

        @Override
        public void setFixedLengthStreamingMode(int contentLength) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void setFixedLengthStreamingMode(long contentLength) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void setChunkedStreamingMode(int chunklen) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void setInstanceFollowRedirects(boolean followRedirects) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getInstanceFollowRedirects() {
            return super.getInstanceFollowRedirects();
        }

        @Override
        public void setRequestMethod(String method) throws ProtocolException {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public String getRequestMethod() {
            return this.request.method();
        }

        @Override
        public String getHeaderFieldKey(int position) {
            if (position < 0) {
                throw new IllegalArgumentException("Invalid header index: " + position);
            }
            if (position == 0) {
                return null;
            }
            return this.response.headers().name(position - 1);
        }

        @Override
        public String getHeaderField(int position) {
            if (position < 0) {
                throw new IllegalArgumentException("Invalid header index: " + position);
            }
            return position == 0 ? this.response.statusLine() : this.response.headers().value(position - 1);
        }

        @Override
        public String getHeaderField(String fieldName) {
            return fieldName == null ? this.response.statusLine() : this.response.headers().get(fieldName);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return OkHeaders.toMultimap(this.response.headers(), this.response.statusLine());
        }

        @Override
        public int getResponseCode() throws IOException {
            return this.response.code();
        }

        @Override
        public String getResponseMessage() throws IOException {
            return this.response.statusMessage();
        }

        @Override
        public InputStream getErrorStream() {
            return null;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void setConnectTimeout(int timeout) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public int getConnectTimeout() {
            return 0;
        }

        @Override
        public void setReadTimeout(int timeout) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public int getReadTimeout() {
            return 0;
        }

        @Override
        public Object getContent() throws IOException {
            throw JavaApiConverter.throwResponseBodyAccessException();
        }

        @Override
        public Object getContent(Class[] classes) throws IOException {
            throw JavaApiConverter.throwResponseBodyAccessException();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw JavaApiConverter.throwResponseBodyAccessException();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void setDoInput(boolean doInput) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getDoInput() {
            return true;
        }

        @Override
        public void setDoOutput(boolean doOutput) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getDoOutput() {
            return this.request.body() != null;
        }

        @Override
        public void setAllowUserInteraction(boolean allowUserInteraction) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getAllowUserInteraction() {
            return false;
        }

        @Override
        public void setUseCaches(boolean useCaches) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getUseCaches() {
            return super.getUseCaches();
        }

        @Override
        public void setIfModifiedSince(long ifModifiedSince) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public long getIfModifiedSince() {
            return 0L;
        }

        @Override
        public boolean getDefaultUseCaches() {
            return super.getDefaultUseCaches();
        }

        @Override
        public void setDefaultUseCaches(boolean defaultUseCaches) {
            super.setDefaultUseCaches(defaultUseCaches);
        }
    }

    private static final class CacheHttpsURLConnection extends DelegatingHttpsURLConnection {
        private final CacheHttpURLConnection delegate;

        public CacheHttpsURLConnection(CacheHttpURLConnection delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        protected Handshake handshake() {
            return this.delegate.response.handshake();
        }

        @Override
        public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public HostnameVerifier getHostnameVerifier() {
            throw JavaApiConverter.throwRequestSslAccessException();
        }

        @Override
        public void setSSLSocketFactory(SSLSocketFactory socketFactory) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public SSLSocketFactory getSSLSocketFactory() {
            throw JavaApiConverter.throwRequestSslAccessException();
        }

        @Override
        public void setFixedLengthStreamingMode(long contentLength) {
            this.delegate.setFixedLengthStreamingMode(contentLength);
        }
    }

    private static RuntimeException throwRequestModificationException() {
        throw new UnsupportedOperationException("ResponseCache cannot modify the request.");
    }

    private static RuntimeException throwRequestHeaderAccessException() {
        throw new UnsupportedOperationException("ResponseCache cannot access request headers");
    }

    private static RuntimeException throwRequestSslAccessException() {
        throw new UnsupportedOperationException("ResponseCache cannot access SSL internals");
    }

    private static RuntimeException throwResponseBodyAccessException() {
        throw new UnsupportedOperationException("ResponseCache cannot access the response body.");
    }

    private static <T> List<T> nullSafeImmutableList(T[] elements) {
        return elements == null ? Collections.emptyList() : Util.immutableList(elements);
    }
}
