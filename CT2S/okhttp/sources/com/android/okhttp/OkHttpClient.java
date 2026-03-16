package com.android.okhttp;

import com.android.okhttp.Response;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.http.HttpAuthenticator;
import com.android.okhttp.internal.http.HttpURLConnectionImpl;
import com.android.okhttp.internal.http.HttpsURLConnectionImpl;
import com.android.okhttp.internal.http.ResponseCacheAdapter;
import com.android.okhttp.internal.tls.OkHostnameVerifier;
import com.android.okio.ByteString;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ResponseCache;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public final class OkHttpClient implements URLStreamHandlerFactory, Cloneable {
    private OkAuthenticator authenticator;
    private int connectTimeout;
    private ConnectionPool connectionPool;
    private CookieHandler cookieHandler;
    private HostResolver hostResolver;
    private HostnameVerifier hostnameVerifier;
    private List<Protocol> protocols;
    private Proxy proxy;
    private ProxySelector proxySelector;
    private int readTimeout;
    private OkResponseCache responseCache;
    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;
    private boolean followProtocolRedirects = true;
    private final RouteDatabase routeDatabase = new RouteDatabase();
    private Dispatcher dispatcher = new Dispatcher();

    public void setConnectTimeout(long timeout, TimeUnit unit) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }
        if (unit == null) {
            throw new IllegalArgumentException("unit == null");
        }
        long millis = unit.toMillis(timeout);
        if (millis > 2147483647L) {
            throw new IllegalArgumentException("Timeout too large.");
        }
        this.connectTimeout = (int) millis;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public void setReadTimeout(long timeout, TimeUnit unit) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }
        if (unit == null) {
            throw new IllegalArgumentException("unit == null");
        }
        long millis = unit.toMillis(timeout);
        if (millis > 2147483647L) {
            throw new IllegalArgumentException("Timeout too large.");
        }
        this.readTimeout = (int) millis;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public OkHttpClient setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public OkHttpClient setProxySelector(ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
        return this;
    }

    public ProxySelector getProxySelector() {
        return this.proxySelector;
    }

    public OkHttpClient setCookieHandler(CookieHandler cookieHandler) {
        this.cookieHandler = cookieHandler;
        return this;
    }

    public CookieHandler getCookieHandler() {
        return this.cookieHandler;
    }

    public OkHttpClient setResponseCache(ResponseCache responseCache) {
        return setOkResponseCache(toOkResponseCache(responseCache));
    }

    public ResponseCache getResponseCache() {
        if (this.responseCache instanceof ResponseCacheAdapter) {
            return ((ResponseCacheAdapter) this.responseCache).getDelegate();
        }
        return null;
    }

    public OkHttpClient setOkResponseCache(OkResponseCache responseCache) {
        this.responseCache = responseCache;
        return this;
    }

    public OkResponseCache getOkResponseCache() {
        return this.responseCache;
    }

    public OkHttpClient setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
        return this;
    }

    public SocketFactory getSocketFactory() {
        return this.socketFactory;
    }

    public OkHttpClient setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        return this;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return this.sslSocketFactory;
    }

    public OkHttpClient setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    public HostnameVerifier getHostnameVerifier() {
        return this.hostnameVerifier;
    }

    public OkHttpClient setAuthenticator(OkAuthenticator authenticator) {
        this.authenticator = authenticator;
        return this;
    }

    public OkAuthenticator getAuthenticator() {
        return this.authenticator;
    }

    public OkHttpClient setConnectionPool(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        return this;
    }

    public ConnectionPool getConnectionPool() {
        return this.connectionPool;
    }

    public OkHttpClient setFollowProtocolRedirects(boolean followProtocolRedirects) {
        this.followProtocolRedirects = followProtocolRedirects;
        return this;
    }

    public boolean getFollowProtocolRedirects() {
        return this.followProtocolRedirects;
    }

    public RouteDatabase getRoutesDatabase() {
        return this.routeDatabase;
    }

    public OkHttpClient setDispatcher(Dispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher == null");
        }
        this.dispatcher = dispatcher;
        return this;
    }

    public Dispatcher getDispatcher() {
        return this.dispatcher;
    }

    @Deprecated
    public OkHttpClient setTransports(List<String> transports) {
        List<Protocol> protocols = new ArrayList<>(transports.size());
        int size = transports.size();
        for (int i = 0; i < size; i++) {
            try {
                Protocol protocol = Protocol.find(ByteString.encodeUtf8(transports.get(i)));
                protocols.add(protocol);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return setProtocols(protocols);
    }

    public OkHttpClient setProtocols(List<Protocol> protocols) {
        List<Protocol> protocols2 = Util.immutableList(protocols);
        if (!protocols2.contains(Protocol.HTTP_11)) {
            throw new IllegalArgumentException("protocols doesn't contain http/1.1: " + protocols2);
        }
        if (protocols2.contains(null)) {
            throw new IllegalArgumentException("protocols must not contain null");
        }
        this.protocols = Util.immutableList(protocols2);
        return this;
    }

    @Deprecated
    public List<String> getTransports() {
        List<String> transports = new ArrayList<>(this.protocols.size());
        int size = this.protocols.size();
        for (int i = 0; i < size; i++) {
            transports.add(this.protocols.get(i).name.utf8());
        }
        return transports;
    }

    public List<Protocol> getProtocols() {
        return this.protocols;
    }

    public OkHttpClient setHostResolver(HostResolver hostResolver) {
        this.hostResolver = hostResolver;
        return this;
    }

    public HostResolver getHostResolver() {
        return this.hostResolver;
    }

    public Response execute(Request request) throws IOException {
        OkHttpClient client = copyWithDefaults();
        Job job = new Job(this.dispatcher, client, request, null);
        Response result = job.getResponse();
        job.engine.releaseConnection();
        return result;
    }

    public void enqueue(Request request, Response.Receiver responseReceiver) {
        this.dispatcher.enqueue(this, request, responseReceiver);
    }

    public void cancel(Object tag) {
        this.dispatcher.cancel(tag);
    }

    public HttpURLConnection open(URL url) {
        return open(url, this.proxy);
    }

    HttpURLConnection open(URL url, Proxy proxy) {
        String protocol = url.getProtocol();
        OkHttpClient copy = copyWithDefaults();
        copy.proxy = proxy;
        if (protocol.equals("http")) {
            return new HttpURLConnectionImpl(url, copy);
        }
        if (protocol.equals("https")) {
            return new HttpsURLConnectionImpl(url, copy);
        }
        throw new IllegalArgumentException("Unexpected protocol: " + protocol);
    }

    OkHttpClient copyWithDefaults() {
        OkHttpClient result = m0clone();
        if (result.proxySelector == null) {
            result.proxySelector = ProxySelector.getDefault();
        }
        if (result.cookieHandler == null) {
            result.cookieHandler = CookieHandler.getDefault();
        }
        if (result.responseCache == null) {
            result.responseCache = toOkResponseCache(ResponseCache.getDefault());
        }
        if (result.socketFactory == null) {
            result.socketFactory = SocketFactory.getDefault();
        }
        if (result.sslSocketFactory == null) {
            result.sslSocketFactory = getDefaultSSLSocketFactory();
        }
        if (result.hostnameVerifier == null) {
            result.hostnameVerifier = OkHostnameVerifier.INSTANCE;
        }
        if (result.authenticator == null) {
            result.authenticator = HttpAuthenticator.SYSTEM_DEFAULT;
        }
        if (result.connectionPool == null) {
            result.connectionPool = ConnectionPool.getDefault();
        }
        if (result.protocols == null) {
            result.protocols = Protocol.HTTP2_SPDY3_AND_HTTP;
        }
        if (result.hostResolver == null) {
            result.hostResolver = HostResolver.DEFAULT;
        }
        return result;
    }

    private synchronized SSLSocketFactory getDefaultSSLSocketFactory() {
        if (this.sslSocketFactory == null) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, null);
                this.sslSocketFactory = sslContext.getSocketFactory();
            } catch (GeneralSecurityException e) {
                throw new AssertionError();
            }
        }
        return this.sslSocketFactory;
    }

    public OkHttpClient m0clone() {
        try {
            return (OkHttpClient) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    private OkResponseCache toOkResponseCache(ResponseCache responseCache) {
        return (responseCache == 0 || (responseCache instanceof OkResponseCache)) ? (OkResponseCache) responseCache : new ResponseCacheAdapter(responseCache);
    }

    @Override
    public URLStreamHandler createURLStreamHandler(final String protocol) {
        if (protocol.equals("http") || protocol.equals("https")) {
            return new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) {
                    return OkHttpClient.this.open(url);
                }

                @Override
                protected URLConnection openConnection(URL url, Proxy proxy) {
                    return OkHttpClient.this.open(url, proxy);
                }

                @Override
                protected int getDefaultPort() {
                    if (protocol.equals("http")) {
                        return 80;
                    }
                    if (protocol.equals("https")) {
                        return 443;
                    }
                    throw new AssertionError();
                }
            };
        }
        return null;
    }
}
