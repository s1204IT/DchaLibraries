package com.android.okhttp;

import com.android.okhttp.internal.URLFilter;
import dalvik.system.PathClassLoader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ResponseCache;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import libcore.net.NetworkSecurityPolicy;

public class HttpHandler extends URLStreamHandler {
    private static Method sHttpCacheMethod;
    private final ConfigAwareConnectionPool configAwareConnectionPool = ConfigAwareConnectionPool.getInstance();
    private static final List<ConnectionSpec> CLEARTEXT_ONLY = Collections.singletonList(ConnectionSpec.CLEARTEXT);
    private static final CleartextURLFilter CLEARTEXT_FILTER = new CleartextURLFilter();

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        try {
            if (sHttpCacheMethod != null) {
                sHttpCacheMethod.invoke(null, url);
            } else {
                sHttpCacheMethod = new PathClassLoader("/system/framework/mediatek-framework-net.jar", ClassLoader.getSystemClassLoader()).loadClass("com.mediatek.net.http.HttpCacheExt").getDeclaredMethod("checkUrl", URL.class);
                sHttpCacheMethod.setAccessible(true);
                sHttpCacheMethod.invoke(null, url);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return newOkUrlFactory(null).open(url);
    }

    @Override
    protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        if (url == null || proxy == null) {
            throw new IllegalArgumentException("url == null || proxy == null");
        }
        return newOkUrlFactory(proxy).open(url);
    }

    @Override
    protected int getDefaultPort() {
        return 80;
    }

    protected OkUrlFactory newOkUrlFactory(Proxy proxy) {
        OkUrlFactory okUrlFactoryCreateHttpOkUrlFactory = createHttpOkUrlFactory(proxy);
        okUrlFactoryCreateHttpOkUrlFactory.client().setConnectionPool(this.configAwareConnectionPool.get());
        return okUrlFactoryCreateHttpOkUrlFactory;
    }

    public static OkUrlFactory createHttpOkUrlFactory(Proxy proxy) {
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(0L, TimeUnit.MILLISECONDS);
        okHttpClient.setReadTimeout(0L, TimeUnit.MILLISECONDS);
        okHttpClient.setWriteTimeout(0L, TimeUnit.MILLISECONDS);
        okHttpClient.setFollowRedirects(HttpURLConnection.getFollowRedirects());
        okHttpClient.setFollowSslRedirects(false);
        okHttpClient.setConnectionSpecs(CLEARTEXT_ONLY);
        if (proxy != null) {
            okHttpClient.setProxy(proxy);
        }
        OkUrlFactory okUrlFactory = new OkUrlFactory(okHttpClient);
        OkUrlFactories.setUrlFilter(okUrlFactory, CLEARTEXT_FILTER);
        ResponseCache responseCache = ResponseCache.getDefault();
        if (responseCache != null) {
            AndroidInternal.setResponseCache(okUrlFactory, responseCache);
        }
        return okUrlFactory;
    }

    private static final class CleartextURLFilter implements URLFilter {
        private CleartextURLFilter() {
        }

        @Override
        public void checkURLPermitted(URL url) throws IOException {
            String host = url.getHost();
            if (!NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(host)) {
                throw new IOException("Cleartext HTTP traffic to " + host + " not permitted");
            }
        }
    }
}
