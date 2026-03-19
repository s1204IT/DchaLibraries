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
import java.util.Iterator;
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

    public static Response createOkResponseForCachePut(URI uri, URLConnection uRLConnection) throws IOException {
        RequestBody requestBody;
        Certificate[] serverCertificates;
        HttpURLConnection httpURLConnection = (HttpURLConnection) uRLConnection;
        Response.Builder builder = new Response.Builder();
        Headers headersVaryHeaders = varyHeaders(uRLConnection, createHeaders(uRLConnection.getHeaderFields()));
        if (headersVaryHeaders == null) {
            return null;
        }
        String requestMethod = httpURLConnection.getRequestMethod();
        if (HttpMethod.requiresRequestBody(requestMethod)) {
            requestBody = EMPTY_REQUEST_BODY;
        } else {
            requestBody = null;
        }
        builder.request(new Request.Builder().url(uri.toString()).method(requestMethod, requestBody).headers(headersVaryHeaders).build());
        StatusLine statusLine = StatusLine.parse(extractStatusLine(httpURLConnection));
        builder.protocol(statusLine.protocol);
        builder.code(statusLine.code);
        builder.message(statusLine.message);
        builder.networkResponse(builder.build());
        builder.headers(extractOkResponseHeaders(httpURLConnection));
        builder.body(createOkBody(uRLConnection));
        if (httpURLConnection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) httpURLConnection;
            try {
                serverCertificates = httpsURLConnection.getServerCertificates();
            } catch (SSLPeerUnverifiedException e) {
                serverCertificates = null;
            }
            builder.handshake(Handshake.get(httpsURLConnection.getCipherSuite(), nullSafeImmutableList(serverCertificates), nullSafeImmutableList(httpsURLConnection.getLocalCertificates())));
        }
        return builder.build();
    }

    private static Headers createHeaders(Map<String, List<String>> map) {
        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                String strTrim = entry.getKey().trim();
                Iterator<String> it = entry.getValue().iterator();
                while (it.hasNext()) {
                    Internal.instance.addLenient(builder, strTrim, it.next().trim());
                }
            }
        }
        return builder.build();
    }

    private static Headers varyHeaders(URLConnection uRLConnection, Headers headers) {
        if (OkHeaders.hasVaryAll(headers)) {
            return null;
        }
        Set<String> setVaryFields = OkHeaders.varyFields(headers);
        if (setVaryFields.isEmpty()) {
            return new Headers.Builder().build();
        }
        if (!(uRLConnection instanceof CacheHttpURLConnection) && !(uRLConnection instanceof CacheHttpsURLConnection)) {
            return null;
        }
        Map<String, List<String>> requestProperties = uRLConnection.getRequestProperties();
        Headers.Builder builder = new Headers.Builder();
        for (String str : setVaryFields) {
            List<String> list = requestProperties.get(str);
            if (list == null) {
                if (str.equals("Accept-Encoding")) {
                    builder.add("Accept-Encoding", "gzip");
                }
            } else {
                Iterator<String> it = list.iterator();
                while (it.hasNext()) {
                    Internal.instance.addLenient(builder, str, it.next());
                }
            }
        }
        return builder.build();
    }

    static Response createOkResponseForCacheGet(Request request, CacheResponse cacheResponse) throws IOException {
        Headers headersVaryHeaders;
        List<Certificate> listEmptyList;
        Headers headersCreateHeaders = createHeaders(cacheResponse.getHeaders());
        if (OkHeaders.hasVaryAll(headersCreateHeaders)) {
            headersVaryHeaders = new Headers.Builder().build();
        } else {
            headersVaryHeaders = OkHeaders.varyHeaders(request.headers(), headersCreateHeaders);
        }
        Request requestBuild = new Request.Builder().url(request.httpUrl()).method(request.method(), null).headers(headersVaryHeaders).build();
        Response.Builder builder = new Response.Builder();
        builder.request(requestBuild);
        StatusLine statusLine = StatusLine.parse(extractStatusLine(cacheResponse));
        builder.protocol(statusLine.protocol);
        builder.code(statusLine.code);
        builder.message(statusLine.message);
        Headers headersExtractOkHeaders = extractOkHeaders(cacheResponse);
        builder.headers(headersExtractOkHeaders);
        builder.body(createOkBody(headersExtractOkHeaders, cacheResponse));
        if (cacheResponse instanceof SecureCacheResponse) {
            SecureCacheResponse secureCacheResponse = (SecureCacheResponse) cacheResponse;
            try {
                listEmptyList = secureCacheResponse.getServerCertificateChain();
            } catch (SSLPeerUnverifiedException e) {
                listEmptyList = Collections.emptyList();
            }
            List<Certificate> localCertificateChain = secureCacheResponse.getLocalCertificateChain();
            if (localCertificateChain == null) {
                localCertificateChain = Collections.emptyList();
            }
            builder.handshake(Handshake.get(secureCacheResponse.getCipherSuite(), listEmptyList, localCertificateChain));
        }
        return builder.build();
    }

    public static Request createOkRequest(URI uri, String str, Map<String, List<String>> map) {
        RequestBody requestBody;
        if (HttpMethod.requiresRequestBody(str)) {
            requestBody = EMPTY_REQUEST_BODY;
        } else {
            requestBody = null;
        }
        Request.Builder builderMethod = new Request.Builder().url(uri.toString()).method(str, requestBody);
        if (map != null) {
            builderMethod.headers(extractOkHeaders(map));
        }
        return builderMethod.build();
    }

    public static CacheResponse createJavaCacheResponse(final Response response) {
        final Headers headers = response.headers();
        final ResponseBody responseBodyBody = response.body();
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
                    List<Certificate> listLocalCertificates = handshake.localCertificates();
                    if (listLocalCertificates.size() > 0) {
                        return listLocalCertificates;
                    }
                    return null;
                }

                @Override
                public List<Certificate> getServerCertificateChain() throws SSLPeerUnverifiedException {
                    if (handshake == null) {
                        return null;
                    }
                    List<Certificate> listPeerCertificates = handshake.peerCertificates();
                    if (listPeerCertificates.size() > 0) {
                        return listPeerCertificates;
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
                    if (responseBodyBody == null) {
                        return null;
                    }
                    return responseBodyBody.byteStream();
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
                if (responseBodyBody == null) {
                    return null;
                }
                return responseBodyBody.byteStream();
            }
        };
    }

    public static CacheRequest createJavaCacheRequest(final com.android.okhttp.internal.http.CacheRequest cacheRequest) {
        return new CacheRequest() {
            @Override
            public void abort() {
                cacheRequest.abort();
            }

            @Override
            public OutputStream getBody() throws IOException {
                Sink sinkBody = cacheRequest.body();
                if (sinkBody == null) {
                    return null;
                }
                return Okio.buffer(sinkBody).outputStream();
            }
        };
    }

    static HttpURLConnection createJavaUrlConnectionForCachePut(Response response) {
        if (response.request().isHttps()) {
            return new CacheHttpsURLConnection(new CacheHttpURLConnection(response));
        }
        return new CacheHttpURLConnection(response);
    }

    static Map<String, List<String>> extractJavaHeaders(Request request) {
        return OkHeaders.toMultimap(request.headers(), null);
    }

    private static Headers extractOkHeaders(CacheResponse cacheResponse) throws IOException {
        return extractOkHeaders(cacheResponse.getHeaders());
    }

    private static Headers extractOkResponseHeaders(HttpURLConnection httpURLConnection) {
        return extractOkHeaders(httpURLConnection.getHeaderFields());
    }

    static Headers extractOkHeaders(Map<String, List<String>> map) {
        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key != null) {
                Iterator<String> it = entry.getValue().iterator();
                while (it.hasNext()) {
                    Internal.instance.addLenient(builder, key, it.next());
                }
            }
        }
        return builder.build();
    }

    private static String extractStatusLine(HttpURLConnection httpURLConnection) {
        return httpURLConnection.getHeaderField((String) null);
    }

    private static String extractStatusLine(CacheResponse cacheResponse) throws IOException {
        return extractStatusLine(cacheResponse.getHeaders());
    }

    static String extractStatusLine(Map<String, List<String>> map) throws ProtocolException {
        List<String> list = map.get(null);
        if (list == null || list.size() == 0) {
            throw new ProtocolException("CacheResponse is missing a 'null' header containing the status line. Headers=" + map);
        }
        return list.get(0);
    }

    private static ResponseBody createOkBody(final Headers headers, final CacheResponse cacheResponse) {
        return new ResponseBody() {
            private BufferedSource body;

            @Override
            public MediaType contentType() {
                String str = headers.get("Content-Type");
                if (str == null) {
                    return null;
                }
                return MediaType.parse(str);
            }

            @Override
            public long contentLength() {
                return OkHeaders.contentLength(headers);
            }

            @Override
            public BufferedSource source() throws IOException {
                if (this.body == null) {
                    this.body = Okio.buffer(Okio.source(cacheResponse.getBody()));
                }
                return this.body;
            }
        };
    }

    private static ResponseBody createOkBody(final URLConnection uRLConnection) {
        if (!uRLConnection.getDoInput()) {
            return null;
        }
        return new ResponseBody() {
            private BufferedSource body;

            @Override
            public MediaType contentType() {
                String contentType = uRLConnection.getContentType();
                if (contentType == null) {
                    return null;
                }
                return MediaType.parse(contentType);
            }

            @Override
            public long contentLength() {
                return JavaApiConverter.stringToLong(uRLConnection.getHeaderField("Content-Length"));
            }

            @Override
            public BufferedSource source() throws IOException {
                if (this.body == null) {
                    this.body = Okio.buffer(Okio.source(uRLConnection.getInputStream()));
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
            this.connected = true;
            this.doOutput = this.request.body() != null;
            this.doInput = true;
            this.useCaches = true;
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
        public void setRequestProperty(String str, String str2) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void addRequestProperty(String str, String str2) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public String getRequestProperty(String str) {
            return this.request.header(str);
        }

        @Override
        public Map<String, List<String>> getRequestProperties() {
            return OkHeaders.toMultimap(this.request.headers(), null);
        }

        @Override
        public void setFixedLengthStreamingMode(int i) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void setFixedLengthStreamingMode(long j) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void setChunkedStreamingMode(int i) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public void setInstanceFollowRedirects(boolean z) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getInstanceFollowRedirects() {
            return super.getInstanceFollowRedirects();
        }

        @Override
        public void setRequestMethod(String str) throws ProtocolException {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public String getRequestMethod() {
            return this.request.method();
        }

        @Override
        public String getHeaderFieldKey(int i) {
            if (i < 0) {
                throw new IllegalArgumentException("Invalid header index: " + i);
            }
            if (i == 0) {
                return null;
            }
            return this.response.headers().name(i - 1);
        }

        @Override
        public String getHeaderField(int i) {
            if (i < 0) {
                throw new IllegalArgumentException("Invalid header index: " + i);
            }
            if (i == 0) {
                return StatusLine.get(this.response).toString();
            }
            return this.response.headers().value(i - 1);
        }

        @Override
        public String getHeaderField(String str) {
            if (str == null) {
                return StatusLine.get(this.response).toString();
            }
            return this.response.headers().get(str);
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
        public void setConnectTimeout(int i) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public int getConnectTimeout() {
            return 0;
        }

        @Override
        public void setReadTimeout(int i) {
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
        public Object getContent(Class[] clsArr) throws IOException {
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
        public void setDoInput(boolean z) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getDoInput() {
            return this.doInput;
        }

        @Override
        public void setDoOutput(boolean z) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getDoOutput() {
            return this.doOutput;
        }

        @Override
        public void setAllowUserInteraction(boolean z) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getAllowUserInteraction() {
            return false;
        }

        @Override
        public void setUseCaches(boolean z) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public boolean getUseCaches() {
            return super.getUseCaches();
        }

        @Override
        public void setIfModifiedSince(long j) {
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
        public void setDefaultUseCaches(boolean z) {
            super.setDefaultUseCaches(z);
        }
    }

    private static final class CacheHttpsURLConnection extends DelegatingHttpsURLConnection {
        private final CacheHttpURLConnection delegate;

        public CacheHttpsURLConnection(CacheHttpURLConnection cacheHttpURLConnection) {
            super(cacheHttpURLConnection);
            this.delegate = cacheHttpURLConnection;
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
        public void setSSLSocketFactory(SSLSocketFactory sSLSocketFactory) {
            throw JavaApiConverter.throwRequestModificationException();
        }

        @Override
        public SSLSocketFactory getSSLSocketFactory() {
            throw JavaApiConverter.throwRequestSslAccessException();
        }

        @Override
        public long getContentLengthLong() {
            return this.delegate.getContentLengthLong();
        }

        @Override
        public void setFixedLengthStreamingMode(long j) {
            this.delegate.setFixedLengthStreamingMode(j);
        }

        @Override
        public long getHeaderFieldLong(String str, long j) {
            return this.delegate.getHeaderFieldLong(str, j);
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

    private static <T> List<T> nullSafeImmutableList(T[] tArr) {
        return tArr == null ? Collections.emptyList() : Util.immutableList(tArr);
    }

    private static long stringToLong(String str) {
        if (str == null) {
            return -1L;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
