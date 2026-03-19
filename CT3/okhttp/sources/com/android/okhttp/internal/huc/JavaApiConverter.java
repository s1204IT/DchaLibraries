package com.android.okhttp.internal.huc;

import com.android.okhttp.Handshake;
import com.android.okhttp.Headers;
import com.android.okhttp.MediaType;
import com.android.okhttp.Request;
import com.android.okhttp.RequestBody;
import com.android.okhttp.Response;
import com.android.okhttp.ResponseBody;
import com.android.okhttp.internal.Internal;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.http.HttpMethod;
import com.android.okhttp.internal.http.OkHeaders;
import com.android.okhttp.internal.http.StatusLine;
import com.android.okhttp.okio.BufferedSource;
import com.android.okhttp.okio.Okio;
import com.android.okhttp.okio.Sink;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
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
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

public final class JavaApiConverter {
    private static final RequestBody EMPTY_REQUEST_BODY = RequestBody.create((MediaType) null, new byte[0]);

    private JavaApiConverter() {
    }

    public static Response createOkResponseForCachePut(URI uri, URLConnection urlConnection) throws IOException {
        RequestBody requestBody;
        Certificate[] serverCertificates;
        HttpURLConnection httpUrlConnection = (HttpURLConnection) urlConnection;
        Response.Builder okResponseBuilder = new Response.Builder();
        Headers responseHeaders = createHeaders(urlConnection.getHeaderFields());
        Headers varyHeaders = varyHeaders(urlConnection, responseHeaders);
        if (varyHeaders == null) {
            return null;
        }
        String requestMethod = httpUrlConnection.getRequestMethod();
        if (HttpMethod.requiresRequestBody(requestMethod)) {
            requestBody = EMPTY_REQUEST_BODY;
        } else {
            requestBody = null;
        }
        Request okRequest = new Request.Builder().url(uri.toString()).method(requestMethod, requestBody).headers(varyHeaders).build();
        okResponseBuilder.request(okRequest);
        StatusLine statusLine = StatusLine.parse(extractStatusLine(httpUrlConnection));
        okResponseBuilder.protocol(statusLine.protocol);
        okResponseBuilder.code(statusLine.code);
        okResponseBuilder.message(statusLine.message);
        Response networkResponse = okResponseBuilder.build();
        okResponseBuilder.networkResponse(networkResponse);
        Headers okHeaders = extractOkResponseHeaders(httpUrlConnection);
        okResponseBuilder.headers(okHeaders);
        ResponseBody okBody = createOkBody(urlConnection);
        okResponseBuilder.body(okBody);
        if (httpUrlConnection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) httpUrlConnection;
            try {
                serverCertificates = httpsUrlConnection.getServerCertificates();
            } catch (SSLPeerUnverifiedException e) {
                serverCertificates = null;
            }
            Certificate[] localCertificates = httpsUrlConnection.getLocalCertificates();
            Handshake handshake = Handshake.get(httpsUrlConnection.getCipherSuite(), nullSafeImmutableList(serverCertificates), nullSafeImmutableList(localCertificates));
            okResponseBuilder.handshake(handshake);
        }
        return okResponseBuilder.build();
    }

    private static Headers createHeaders(Map<String, List<String>> headers) {
        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if (header.getKey() != null && header.getValue() != null) {
                String name = header.getKey().trim();
                for (String value : header.getValue()) {
                    String trimmedValue = value.trim();
                    Internal.instance.addLenient(builder, name, trimmedValue);
                }
            }
        }
        return builder.build();
    }

    private static Headers varyHeaders(URLConnection urlConnection, Headers responseHeaders) {
        boolean z;
        if (OkHeaders.hasVaryAll(responseHeaders)) {
            return null;
        }
        Set<String> varyFields = OkHeaders.varyFields(responseHeaders);
        if (varyFields.isEmpty()) {
            return new Headers.Builder().build();
        }
        if (urlConnection instanceof CacheHttpURLConnection) {
            z = true;
        } else {
            z = urlConnection instanceof CacheHttpsURLConnection;
        }
        if (!z) {
            return null;
        }
        Map<String, List<String>> requestProperties = urlConnection.getRequestProperties();
        Headers.Builder result = new Headers.Builder();
        for (String fieldName : varyFields) {
            List<String> fieldValues = requestProperties.get(fieldName);
            if (fieldValues == null) {
                if (fieldName.equals("Accept-Encoding")) {
                    result.add("Accept-Encoding", "gzip");
                }
            } else {
                for (String fieldValue : fieldValues) {
                    Internal.instance.addLenient(result, fieldName, fieldValue);
                }
            }
        }
        return result.build();
    }

    static Response createOkResponseForCacheGet(Request request, CacheResponse cacheResponse) throws IOException {
        Headers varyHeaders;
        List<Certificate> peerCertificates;
        Headers responseHeaders = createHeaders(cacheResponse.getHeaders());
        if (OkHeaders.hasVaryAll(responseHeaders)) {
            varyHeaders = new Headers.Builder().build();
        } else {
            varyHeaders = OkHeaders.varyHeaders(request.headers(), responseHeaders);
        }
        Request cacheRequest = new Request.Builder().url(request.httpUrl()).method(request.method(), null).headers(varyHeaders).build();
        Response.Builder okResponseBuilder = new Response.Builder();
        okResponseBuilder.request(cacheRequest);
        StatusLine statusLine = StatusLine.parse(extractStatusLine(cacheResponse));
        okResponseBuilder.protocol(statusLine.protocol);
        okResponseBuilder.code(statusLine.code);
        okResponseBuilder.message(statusLine.message);
        Headers okHeaders = extractOkHeaders(cacheResponse);
        okResponseBuilder.headers(okHeaders);
        ResponseBody okBody = createOkBody(okHeaders, cacheResponse);
        okResponseBuilder.body(okBody);
        if (cacheResponse instanceof SecureCacheResponse) {
            try {
                peerCertificates = cacheResponse.getServerCertificateChain();
            } catch (SSLPeerUnverifiedException e) {
                peerCertificates = Collections.emptyList();
            }
            List<Certificate> localCertificates = cacheResponse.getLocalCertificateChain();
            if (localCertificates == null) {
                localCertificates = Collections.emptyList();
            }
            Handshake handshake = Handshake.get(cacheResponse.getCipherSuite(), peerCertificates, localCertificates);
            okResponseBuilder.handshake(handshake);
        }
        return okResponseBuilder.build();
    }

    public static Request createOkRequest(URI uri, String requestMethod, Map<String, List<String>> requestHeaders) {
        RequestBody requestBody;
        if (HttpMethod.requiresRequestBody(requestMethod)) {
            requestBody = EMPTY_REQUEST_BODY;
        } else {
            requestBody = null;
        }
        Request.Builder builder = new Request.Builder().url(uri.toString()).method(requestMethod, requestBody);
        if (requestHeaders != null) {
            Headers headers = extractOkHeaders(requestHeaders);
            builder.headers(headers);
        }
        return builder.build();
    }

    public static CacheResponse createJavaCacheResponse(final Response response) {
        final Headers headers = response.headers();
        final ResponseBody body = response.body();
        if (response.request().isHttps()) {
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
                    if (certificates.size() > 0) {
                        return certificates;
                    }
                    return null;
                }

                @Override
                public List<Certificate> getServerCertificateChain() throws SSLPeerUnverifiedException {
                    if (handshake == null) {
                        return null;
                    }
                    List<Certificate> certificates = handshake.peerCertificates();
                    if (certificates.size() > 0) {
                        return certificates;
                    }
                    return null;
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
                    return OkHeaders.toMultimap(headers, StatusLine.get(response).toString());
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
        return new CacheResponse() {
            @Override
            public Map<String, List<String>> getHeaders() throws IOException {
                return OkHeaders.toMultimap(headers, StatusLine.get(response).toString());
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

    public static CacheRequest createJavaCacheRequest(final com.android.okhttp.internal.http.CacheRequest okCacheRequest) {
        return new CacheRequest() {
            @Override
            public void abort() {
                okCacheRequest.abort();
            }

            @Override
            public OutputStream getBody() throws IOException {
                Sink body = okCacheRequest.body();
                if (body == null) {
                    return null;
                }
                return Okio.buffer(body).outputStream();
            }
        };
    }

    static HttpURLConnection createJavaUrlConnectionForCachePut(Response okResponse) {
        Request request = okResponse.request();
        if (request.isHttps()) {
            return new CacheHttpsURLConnection(new CacheHttpURLConnection(okResponse));
        }
        return new CacheHttpURLConnection(okResponse);
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
                    Internal.instance.addLenient(okHeadersBuilder, name, value);
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

    static String extractStatusLine(Map<String, List<String>> javaResponseHeaders) throws ProtocolException {
        List<String> values = javaResponseHeaders.get(null);
        if (values == null || values.size() == 0) {
            throw new ProtocolException("CacheResponse is missing a 'null' header containing the status line. Headers=" + javaResponseHeaders);
        }
        return values.get(0);
    }

    private static ResponseBody createOkBody(final Headers okHeaders, final CacheResponse cacheResponse) {
        return new ResponseBody() {
            private BufferedSource body;

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
            public BufferedSource source() throws IOException {
                if (this.body == null) {
                    InputStream is = cacheResponse.getBody();
                    this.body = Okio.buffer(Okio.source(is));
                }
                return this.body;
            }
        };
    }

    private static ResponseBody createOkBody(final URLConnection urlConnection) {
        if (!urlConnection.getDoInput()) {
            return null;
        }
        return new ResponseBody() {
            private BufferedSource body;

            @Override
            public MediaType contentType() {
                String contentTypeHeader = urlConnection.getContentType();
                if (contentTypeHeader == null) {
                    return null;
                }
                return MediaType.parse(contentTypeHeader);
            }

            @Override
            public long contentLength() {
                String s = urlConnection.getHeaderField("Content-Length");
                return JavaApiConverter.stringToLong(s);
            }

            @Override
            public BufferedSource source() throws IOException {
                if (this.body == null) {
                    InputStream is = urlConnection.getInputStream();
                    this.body = Okio.buffer(Okio.source(is));
                }
                return this.body;
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
            ((URLConnection) this).connected = true;
            ((URLConnection) this).doOutput = this.request.body() != null;
            ((URLConnection) this).doInput = true;
            ((URLConnection) this).useCaches = true;
            ((HttpURLConnection) this).method = this.request.method();
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
            return OkHeaders.toMultimap(this.request.headers(), null);
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
            if (position == 0) {
                return StatusLine.get(this.response).toString();
            }
            return this.response.headers().value(position - 1);
        }

        @Override
        public String getHeaderField(String fieldName) {
            if (fieldName == null) {
                return StatusLine.get(this.response).toString();
            }
            return this.response.headers().get(fieldName);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return OkHeaders.toMultimap(this.response.headers(), StatusLine.get(this.response).toString());
        }

        @Override
        public int getResponseCode() throws IOException {
            return this.response.code();
        }

        @Override
        public String getResponseMessage() throws IOException {
            return this.response.message();
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
            return ((URLConnection) this).doInput;
        }

        @Override
        public void setDoOutput(boolean doOutput) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getDoOutput() {
            return ((URLConnection) this).doOutput;
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
            return JavaApiConverter.stringToLong(this.request.headers().get("If-Modified-Since"));
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

    private static long stringToLong(String s) {
        if (s == null) {
            return -1L;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
