package android.net.http;

import android.content.ContentResolver;
import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AUTH;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.SM;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;

public final class AndroidHttpClient implements HttpClient {
    private static final int SOCKET_OPERATION_TIMEOUT = 60000;
    private static final String TAG = "AndroidHttpClient";
    private volatile LoggingConfiguration curlConfiguration;
    private final HttpClient delegate;
    private RuntimeException mLeakedException = new IllegalStateException("AndroidHttpClient created and never closed");
    public static long DEFAULT_SYNC_MIN_GZIP_BYTES = 256;
    private static String[] textContentTypes = {"text/", "application/xml", "application/json"};
    private static final HttpRequestInterceptor sThreadCheckInterceptor = new HttpRequestInterceptor() {
        @Override
        public void process(HttpRequest httpRequest, HttpContext httpContext) {
            if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
                throw new RuntimeException("This thread forbids HTTP requests");
            }
        }
    };

    public static AndroidHttpClient newInstance(String str, Context context) {
        BasicHttpParams basicHttpParams = new BasicHttpParams();
        HttpConnectionParams.setStaleCheckingEnabled(basicHttpParams, false);
        HttpConnectionParams.setConnectionTimeout(basicHttpParams, SOCKET_OPERATION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(basicHttpParams, SOCKET_OPERATION_TIMEOUT);
        HttpConnectionParams.setSocketBufferSize(basicHttpParams, 8192);
        HttpClientParams.setRedirecting(basicHttpParams, false);
        SSLSessionCache sSLSessionCache = context == null ? null : new SSLSessionCache(context);
        HttpProtocolParams.setUserAgent(basicHttpParams, str);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme(HttpHost.DEFAULT_SCHEME_NAME, PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLCertificateSocketFactory.getHttpSocketFactory(SOCKET_OPERATION_TIMEOUT, sSLSessionCache), 443));
        return new AndroidHttpClient(new ThreadSafeClientConnManager(basicHttpParams, schemeRegistry), basicHttpParams);
    }

    public static AndroidHttpClient newInstance(String str) {
        return newInstance(str, null);
    }

    private AndroidHttpClient(ClientConnectionManager clientConnectionManager, HttpParams httpParams) {
        this.delegate = new DefaultHttpClient(clientConnectionManager, httpParams) {
            @Override
            protected BasicHttpProcessor createHttpProcessor() {
                BasicHttpProcessor basicHttpProcessorCreateHttpProcessor = super.createHttpProcessor();
                basicHttpProcessorCreateHttpProcessor.addRequestInterceptor(AndroidHttpClient.sThreadCheckInterceptor);
                basicHttpProcessorCreateHttpProcessor.addRequestInterceptor(new CurlLogger());
                return basicHttpProcessorCreateHttpProcessor;
            }

            @Override
            protected HttpContext createHttpContext() {
                BasicHttpContext basicHttpContext = new BasicHttpContext();
                basicHttpContext.setAttribute(ClientContext.AUTHSCHEME_REGISTRY, getAuthSchemes());
                basicHttpContext.setAttribute(ClientContext.COOKIESPEC_REGISTRY, getCookieSpecs());
                basicHttpContext.setAttribute(ClientContext.CREDS_PROVIDER, getCredentialsProvider());
                return basicHttpContext;
            }
        };
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (this.mLeakedException != null) {
            Log.e(TAG, "Leak found", this.mLeakedException);
            this.mLeakedException = null;
        }
    }

    public static void modifyRequestToAcceptGzipResponse(HttpRequest httpRequest) {
        httpRequest.addHeader("Accept-Encoding", "gzip");
    }

    public static InputStream getUngzippedContent(HttpEntity httpEntity) throws IOException {
        Header contentEncoding;
        String value;
        InputStream content = httpEntity.getContent();
        return (content == null || (contentEncoding = httpEntity.getContentEncoding()) == null || (value = contentEncoding.getValue()) == null) ? content : value.contains("gzip") ? new GZIPInputStream(content) : content;
    }

    public void close() {
        if (this.mLeakedException != null) {
            getConnectionManager().shutdown();
            this.mLeakedException = null;
        }
    }

    @Override
    public HttpParams getParams() {
        return this.delegate.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return this.delegate.getConnectionManager();
    }

    @Override
    public HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException {
        return this.delegate.execute(httpUriRequest);
    }

    @Override
    public HttpResponse execute(HttpUriRequest httpUriRequest, HttpContext httpContext) throws IOException {
        return this.delegate.execute(httpUriRequest, httpContext);
    }

    @Override
    public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest) throws IOException {
        return this.delegate.execute(httpHost, httpRequest);
    }

    @Override
    public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws IOException {
        return this.delegate.execute(httpHost, httpRequest, httpContext);
    }

    @Override
    public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler) throws IOException {
        return (T) this.delegate.execute(httpUriRequest, responseHandler);
    }

    @Override
    public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException {
        return (T) this.delegate.execute(httpUriRequest, responseHandler, httpContext);
    }

    @Override
    public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler) throws IOException {
        return (T) this.delegate.execute(httpHost, httpRequest, responseHandler);
    }

    @Override
    public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException {
        return (T) this.delegate.execute(httpHost, httpRequest, responseHandler, httpContext);
    }

    public static AbstractHttpEntity getCompressedEntity(byte[] bArr, ContentResolver contentResolver) throws IOException {
        if (bArr.length < getMinGzipSize(contentResolver)) {
            return new ByteArrayEntity(bArr);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gZIPOutputStream.write(bArr);
        gZIPOutputStream.close();
        ByteArrayEntity byteArrayEntity = new ByteArrayEntity(byteArrayOutputStream.toByteArray());
        byteArrayEntity.setContentEncoding("gzip");
        return byteArrayEntity;
    }

    public static long getMinGzipSize(ContentResolver contentResolver) {
        return DEFAULT_SYNC_MIN_GZIP_BYTES;
    }

    private static class LoggingConfiguration {
        private final int level;
        private final String tag;

        private LoggingConfiguration(String str, int i) {
            this.tag = str;
            this.level = i;
        }

        private boolean isLoggable() {
            return Log.isLoggable(this.tag, this.level);
        }

        private void println(String str) {
            Log.println(this.level, this.tag, str);
        }
    }

    public void enableCurlLogging(String str, int i) {
        if (str == null) {
            throw new NullPointerException("name");
        }
        if (i < 2 || i > 7) {
            throw new IllegalArgumentException("Level is out of range [2..7]");
        }
        this.curlConfiguration = new LoggingConfiguration(str, i);
    }

    public void disableCurlLogging() {
        this.curlConfiguration = null;
    }

    private class CurlLogger implements HttpRequestInterceptor {
        private CurlLogger() {
        }

        @Override
        public void process(HttpRequest httpRequest, HttpContext httpContext) throws org.apache.http.HttpException, IOException {
            LoggingConfiguration loggingConfiguration = AndroidHttpClient.this.curlConfiguration;
            if (loggingConfiguration != null && loggingConfiguration.isLoggable() && (httpRequest instanceof HttpUriRequest)) {
                loggingConfiguration.println(AndroidHttpClient.toCurl((HttpUriRequest) httpRequest, false));
            }
        }
    }

    private static String toCurl(HttpUriRequest httpUriRequest, boolean z) throws IOException {
        HttpEntity entity;
        StringBuilder sb = new StringBuilder();
        sb.append("curl ");
        sb.append("-X ");
        sb.append(httpUriRequest.getMethod());
        sb.append(" ");
        for (Header header : httpUriRequest.getAllHeaders()) {
            if (z || (!header.getName().equals(AUTH.WWW_AUTH_RESP) && !header.getName().equals(SM.COOKIE))) {
                sb.append("--header \"");
                sb.append(header.toString().trim());
                sb.append("\" ");
            }
        }
        URI uri = httpUriRequest.getURI();
        if (httpUriRequest instanceof RequestWrapper) {
            HttpRequest original = httpUriRequest.getOriginal();
            if (original instanceof HttpUriRequest) {
                uri = ((HttpUriRequest) original).getURI();
            }
        }
        sb.append("\"");
        sb.append(uri);
        sb.append("\"");
        if ((httpUriRequest instanceof HttpEntityEnclosingRequest) && (entity = ((HttpEntityEnclosingRequest) httpUriRequest).getEntity()) != null && entity.isRepeatable()) {
            if (entity.getContentLength() < 1024) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                entity.writeTo(byteArrayOutputStream);
                if (isBinaryContent(httpUriRequest)) {
                    sb.insert(0, "echo '" + Base64.encodeToString(byteArrayOutputStream.toByteArray(), 2) + "' | base64 -d > /tmp/$$.bin; ");
                    sb.append(" --data-binary @/tmp/$$.bin");
                } else {
                    String string = byteArrayOutputStream.toString();
                    sb.append(" --data-ascii \"");
                    sb.append(string);
                    sb.append("\"");
                }
            } else {
                sb.append(" [TOO MUCH DATA TO INCLUDE]");
            }
        }
        return sb.toString();
    }

    private static boolean isBinaryContent(HttpUriRequest httpUriRequest) {
        Header[] headers = httpUriRequest.getHeaders(Headers.CONTENT_ENCODING);
        if (headers != null) {
            for (Header header : headers) {
                if ("gzip".equalsIgnoreCase(header.getValue())) {
                    return true;
                }
            }
        }
        Header[] headers2 = httpUriRequest.getHeaders(Headers.CONTENT_TYPE);
        if (headers2 != null) {
            for (Header header2 : headers2) {
                for (String str : textContentTypes) {
                    if (header2.getValue().startsWith(str)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static long parseDate(String str) {
        return LegacyHttpDateTime.parse(str);
    }
}
