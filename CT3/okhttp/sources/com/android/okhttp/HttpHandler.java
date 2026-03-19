package com.android.okhttp;

import com.android.okhttp.internal.URLFilter;
import java.io.IOException;
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
    private final ConfigAwareConnectionPool configAwareConnectionPool = ConfigAwareConnectionPool.getInstance();
    private static final List<ConnectionSpec> CLEARTEXT_ONLY = Collections.singletonList(ConnectionSpec.CLEARTEXT);
    private static final CleartextURLFilter CLEARTEXT_FILTER = new CleartextURLFilter(null);

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
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
        OkUrlFactory okUrlFactory = createHttpOkUrlFactory(proxy);
        okUrlFactory.client().setConnectionPool(this.configAwareConnectionPool.get());
        return okUrlFactory;
    }

    public static OkUrlFactory createHttpOkUrlFactory(Proxy proxy) {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(0L, TimeUnit.MILLISECONDS);
        client.setReadTimeout(0L, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(0L, TimeUnit.MILLISECONDS);
        client.setFollowRedirects(HttpURLConnection.getFollowRedirects());
        client.setFollowSslRedirects(false);
        client.setConnectionSpecs(CLEARTEXT_ONLY);
        if (proxy != null) {
            client.setProxy(proxy);
        }
        OkUrlFactory okUrlFactory = new OkUrlFactory(client);
        okUrlFactory.setUrlFilter(CLEARTEXT_FILTER);
        ResponseCache responseCache = ResponseCache.getDefault();
        if (responseCache != null) {
            AndroidInternal.setResponseCache(okUrlFactory, responseCache);
        }
        return okUrlFactory;
    }

    private static final class CleartextURLFilter implements URLFilter {
        CleartextURLFilter(CleartextURLFilter cleartextURLFilter) {
            this();
        }

        private CleartextURLFilter() {
        }

        @Override
        public void checkURLPermitted(URL url) throws IOException {
            String host = url.getHost();
            if (NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(host)) {
            } else {
                throw new IOException("Cleartext HTTP traffic to " + host + " not permitted");
            }
        }
    }
}
