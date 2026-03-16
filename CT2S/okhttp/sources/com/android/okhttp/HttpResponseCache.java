package com.android.okhttp;

import com.android.okhttp.Headers;
import com.android.okhttp.Response;
import com.android.okhttp.internal.DiskLruCache;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.http.HttpMethod;
import com.android.okhttp.internal.http.HttpURLConnectionImpl;
import com.android.okhttp.internal.http.HttpsURLConnectionImpl;
import com.android.okhttp.internal.http.JavaApiConverter;
import com.android.okio.BufferedSource;
import com.android.okio.ByteString;
import com.android.okio.Okio;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class HttpResponseCache extends ResponseCache implements OkResponseCache {
    private static final int ENTRY_BODY = 1;
    private static final int ENTRY_COUNT = 2;
    private static final int ENTRY_METADATA = 0;
    private static final int VERSION = 201105;
    private final DiskLruCache cache;
    private int hitCount;
    private int networkCount;
    private int requestCount;
    private int writeAbortCount;
    private int writeSuccessCount;

    static int access$208(HttpResponseCache x0) {
        int i = x0.writeSuccessCount;
        x0.writeSuccessCount = i + ENTRY_BODY;
        return i;
    }

    static int access$308(HttpResponseCache x0) {
        int i = x0.writeAbortCount;
        x0.writeAbortCount = i + ENTRY_BODY;
        return i;
    }

    public HttpResponseCache(File directory, long maxSize) throws IOException {
        this.cache = DiskLruCache.open(directory, VERSION, ENTRY_COUNT, maxSize);
    }

    @Override
    public CacheResponse get(URI uri, String requestMethod, Map<String, List<String>> requestHeaders) throws IOException {
        Request request = JavaApiConverter.createOkRequest(uri, requestMethod, requestHeaders);
        Response response = get(request);
        if (response == null) {
            return null;
        }
        return JavaApiConverter.createJavaCacheResponse(response);
    }

    private static String urlToKey(Request requst) {
        return Util.hash(requst.urlString());
    }

    @Override
    public Response get(Request request) {
        String key = urlToKey(request);
        try {
            DiskLruCache.Snapshot snapshot = this.cache.get(key);
            if (snapshot == null) {
                return null;
            }
            Entry entry = new Entry(snapshot.getInputStream(ENTRY_METADATA));
            Response response = entry.response(request, snapshot);
            if (!entry.matches(request, response)) {
                Util.closeQuietly(response.body());
                return null;
            }
            return response;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public CacheRequest put(URI uri, URLConnection urlConnection) throws IOException {
        if (isCacheableConnection(urlConnection)) {
            return put(JavaApiConverter.createOkResponse(uri, urlConnection));
        }
        return null;
    }

    private static boolean isCacheableConnection(URLConnection httpConnection) {
        return (httpConnection instanceof HttpURLConnectionImpl) || (httpConnection instanceof HttpsURLConnectionImpl);
    }

    @Override
    public CacheRequest put(Response response) throws IOException {
        String requestMethod = response.request().method();
        if (maybeRemove(response.request()) || !requestMethod.equals("GET") || response.hasVaryAll()) {
            return null;
        }
        Entry entry = new Entry(response);
        DiskLruCache.Editor editor = null;
        try {
            editor = this.cache.edit(urlToKey(response.request()));
            if (editor == null) {
                return null;
            }
            entry.writeTo(editor);
            return new CacheRequestImpl(editor);
        } catch (IOException e) {
            abortQuietly(editor);
            return null;
        }
    }

    @Override
    public boolean maybeRemove(Request request) {
        if (!HttpMethod.invalidatesCache(request.method())) {
            return false;
        }
        try {
            this.cache.remove(urlToKey(request));
        } catch (IOException e) {
        }
        return true;
    }

    @Override
    public void update(Response cached, Response network) {
        Entry entry = new Entry(network);
        DiskLruCache.Snapshot snapshot = ((CacheResponseBody) cached.body()).snapshot;
        DiskLruCache.Editor editor = null;
        try {
            editor = snapshot.edit();
            if (editor != null) {
                entry.writeTo(editor);
                editor.commit();
            }
        } catch (IOException e) {
            abortQuietly(editor);
        }
    }

    private void abortQuietly(DiskLruCache.Editor editor) {
        if (editor != null) {
            try {
                editor.abort();
            } catch (IOException e) {
            }
        }
    }

    public void delete() throws IOException {
        this.cache.delete();
    }

    public synchronized int getWriteAbortCount() {
        return this.writeAbortCount;
    }

    public synchronized int getWriteSuccessCount() {
        return this.writeSuccessCount;
    }

    public long getSize() {
        return this.cache.size();
    }

    public long getMaxSize() {
        return this.cache.getMaxSize();
    }

    public void flush() throws IOException {
        this.cache.flush();
    }

    public void close() throws IOException {
        this.cache.close();
    }

    public File getDirectory() {
        return this.cache.getDirectory();
    }

    public boolean isClosed() {
        return this.cache.isClosed();
    }

    @Override
    public synchronized void trackResponse(ResponseSource source) {
        this.requestCount += ENTRY_BODY;
        switch (AnonymousClass1.$SwitchMap$com$squareup$okhttp$ResponseSource[source.ordinal()]) {
            case ENTRY_BODY:
                this.hitCount += ENTRY_BODY;
                break;
            case ENTRY_COUNT:
            case 3:
                this.networkCount += ENTRY_BODY;
                break;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$squareup$okhttp$ResponseSource = new int[ResponseSource.values().length];

        static {
            try {
                $SwitchMap$com$squareup$okhttp$ResponseSource[ResponseSource.CACHE.ordinal()] = HttpResponseCache.ENTRY_BODY;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$squareup$okhttp$ResponseSource[ResponseSource.CONDITIONAL_CACHE.ordinal()] = HttpResponseCache.ENTRY_COUNT;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$squareup$okhttp$ResponseSource[ResponseSource.NETWORK.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    @Override
    public synchronized void trackConditionalCacheHit() {
        this.hitCount += ENTRY_BODY;
    }

    public synchronized int getNetworkCount() {
        return this.networkCount;
    }

    public synchronized int getHitCount() {
        return this.hitCount;
    }

    public synchronized int getRequestCount() {
        return this.requestCount;
    }

    private final class CacheRequestImpl extends CacheRequest {
        private OutputStream body;
        private OutputStream cacheOut;
        private boolean done;
        private final DiskLruCache.Editor editor;

        public CacheRequestImpl(final DiskLruCache.Editor editor) throws IOException {
            this.editor = editor;
            this.cacheOut = editor.newOutputStream(HttpResponseCache.ENTRY_BODY);
            this.body = new FilterOutputStream(this.cacheOut) {
                @Override
                public void close() throws IOException {
                    synchronized (HttpResponseCache.this) {
                        if (!CacheRequestImpl.this.done) {
                            CacheRequestImpl.this.done = true;
                            HttpResponseCache.access$208(HttpResponseCache.this);
                            super.close();
                            editor.commit();
                        }
                    }
                }

                @Override
                public void write(byte[] buffer, int offset, int length) throws IOException {
                    this.out.write(buffer, offset, length);
                }
            };
        }

        @Override
        public void abort() {
            synchronized (HttpResponseCache.this) {
                if (!this.done) {
                    this.done = true;
                    HttpResponseCache.access$308(HttpResponseCache.this);
                    Util.closeQuietly(this.cacheOut);
                    try {
                        this.editor.abort();
                    } catch (IOException e) {
                    }
                }
            }
        }

        @Override
        public OutputStream getBody() throws IOException {
            return this.body;
        }
    }

    private static final class Entry {
        private final Handshake handshake;
        private final String requestMethod;
        private final Headers responseHeaders;
        private final String statusLine;
        private final String url;
        private final Headers varyHeaders;

        public Entry(InputStream in) throws IOException {
            try {
                BufferedSource source = Okio.buffer(Okio.source(in));
                this.url = source.readUtf8LineStrict();
                this.requestMethod = source.readUtf8LineStrict();
                Headers.Builder varyHeadersBuilder = new Headers.Builder();
                int varyRequestHeaderLineCount = HttpResponseCache.readInt(source);
                for (int i = HttpResponseCache.ENTRY_METADATA; i < varyRequestHeaderLineCount; i += HttpResponseCache.ENTRY_BODY) {
                    varyHeadersBuilder.addLine(source.readUtf8LineStrict());
                }
                this.varyHeaders = varyHeadersBuilder.build();
                this.statusLine = source.readUtf8LineStrict();
                Headers.Builder responseHeadersBuilder = new Headers.Builder();
                int responseHeaderLineCount = HttpResponseCache.readInt(source);
                for (int i2 = HttpResponseCache.ENTRY_METADATA; i2 < responseHeaderLineCount; i2 += HttpResponseCache.ENTRY_BODY) {
                    responseHeadersBuilder.addLine(source.readUtf8LineStrict());
                }
                this.responseHeaders = responseHeadersBuilder.build();
                if (isHttps()) {
                    String blank = source.readUtf8LineStrict();
                    if (blank.length() > 0) {
                        throw new IOException("expected \"\" but was \"" + blank + "\"");
                    }
                    String cipherSuite = source.readUtf8LineStrict();
                    List<Certificate> peerCertificates = readCertificateList(source);
                    List<Certificate> localCertificates = readCertificateList(source);
                    this.handshake = Handshake.get(cipherSuite, peerCertificates, localCertificates);
                } else {
                    this.handshake = null;
                }
            } finally {
                in.close();
            }
        }

        public Entry(Response response) {
            this.url = response.request().urlString();
            this.varyHeaders = response.request().headers().getAll(response.getVaryFields());
            this.requestMethod = response.request().method();
            this.statusLine = response.statusLine();
            this.responseHeaders = response.headers();
            this.handshake = response.handshake();
        }

        public void writeTo(DiskLruCache.Editor editor) throws IOException {
            OutputStream out = editor.newOutputStream(HttpResponseCache.ENTRY_METADATA);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, Util.UTF_8));
            writer.write(this.url + '\n');
            writer.write(this.requestMethod + '\n');
            writer.write(Integer.toString(this.varyHeaders.size()) + '\n');
            for (int i = HttpResponseCache.ENTRY_METADATA; i < this.varyHeaders.size(); i += HttpResponseCache.ENTRY_BODY) {
                writer.write(this.varyHeaders.name(i) + ": " + this.varyHeaders.value(i) + '\n');
            }
            writer.write(this.statusLine + '\n');
            writer.write(Integer.toString(this.responseHeaders.size()) + '\n');
            for (int i2 = HttpResponseCache.ENTRY_METADATA; i2 < this.responseHeaders.size(); i2 += HttpResponseCache.ENTRY_BODY) {
                writer.write(this.responseHeaders.name(i2) + ": " + this.responseHeaders.value(i2) + '\n');
            }
            if (isHttps()) {
                writer.write(10);
                writer.write(this.handshake.cipherSuite() + '\n');
                writeCertArray(writer, this.handshake.peerCertificates());
                writeCertArray(writer, this.handshake.localCertificates());
            }
            writer.close();
        }

        private boolean isHttps() {
            return this.url.startsWith("https://");
        }

        private List<Certificate> readCertificateList(BufferedSource source) throws IOException {
            int length = HttpResponseCache.readInt(source);
            if (length == -1) {
                return Collections.emptyList();
            }
            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                List<Certificate> result = new ArrayList<>(length);
                for (int i = HttpResponseCache.ENTRY_METADATA; i < length; i += HttpResponseCache.ENTRY_BODY) {
                    String line = source.readUtf8LineStrict();
                    byte[] bytes = ByteString.decodeBase64(line).toByteArray();
                    result.add(certificateFactory.generateCertificate(new ByteArrayInputStream(bytes)));
                }
                return result;
            } catch (CertificateException e) {
                throw new IOException(e.getMessage());
            }
        }

        private void writeCertArray(Writer writer, List<Certificate> certificates) throws IOException {
            try {
                writer.write(Integer.toString(certificates.size()) + '\n');
                int size = certificates.size();
                for (int i = HttpResponseCache.ENTRY_METADATA; i < size; i += HttpResponseCache.ENTRY_BODY) {
                    byte[] bytes = certificates.get(i).getEncoded();
                    String line = ByteString.of(bytes).base64();
                    writer.write(line + '\n');
                }
            } catch (CertificateEncodingException e) {
                throw new IOException(e.getMessage());
            }
        }

        public boolean matches(Request request, Response response) {
            return this.url.equals(request.urlString()) && this.requestMethod.equals(request.method()) && response.varyMatches(this.varyHeaders, request);
        }

        public Response response(Request request, DiskLruCache.Snapshot snapshot) {
            String contentType = this.responseHeaders.get("Content-Type");
            String contentLength = this.responseHeaders.get("Content-Length");
            return new Response.Builder().request(request).statusLine(this.statusLine).headers(this.responseHeaders).body(new CacheResponseBody(snapshot, contentType, contentLength)).handshake(this.handshake).build();
        }
    }

    private static int readInt(BufferedSource source) throws IOException {
        String line = source.readUtf8LineStrict();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            throw new IOException("Expected an integer but was \"" + line + "\"");
        }
    }

    private static class CacheResponseBody extends Response.Body {
        private final InputStream bodyIn;
        private final String contentLength;
        private final String contentType;
        private final DiskLruCache.Snapshot snapshot;

        public CacheResponseBody(final DiskLruCache.Snapshot snapshot, String contentType, String contentLength) {
            this.snapshot = snapshot;
            this.contentType = contentType;
            this.contentLength = contentLength;
            this.bodyIn = new FilterInputStream(snapshot.getInputStream(HttpResponseCache.ENTRY_BODY)) {
                @Override
                public void close() throws IOException {
                    snapshot.close();
                    super.close();
                }
            };
        }

        @Override
        public boolean ready() throws IOException {
            return true;
        }

        @Override
        public MediaType contentType() {
            if (this.contentType != null) {
                return MediaType.parse(this.contentType);
            }
            return null;
        }

        @Override
        public long contentLength() {
            try {
                if (this.contentLength != null) {
                    return Long.parseLong(this.contentLength);
                }
                return -1L;
            } catch (NumberFormatException e) {
                return -1L;
            }
        }

        @Override
        public InputStream byteStream() {
            return this.bodyIn;
        }
    }
}
