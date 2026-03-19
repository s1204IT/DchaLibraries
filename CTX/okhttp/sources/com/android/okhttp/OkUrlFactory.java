package com.android.okhttp;

import com.android.okhttp.internal.URLFilter;
import com.android.okhttp.internal.huc.HttpURLConnectionImpl;
import com.android.okhttp.internal.huc.HttpsURLConnectionImpl;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public final class OkUrlFactory implements URLStreamHandlerFactory, Cloneable {
    private final OkHttpClient client;
    private URLFilter urlFilter;

    public OkUrlFactory(OkHttpClient okHttpClient) {
        this.client = okHttpClient;
    }

    public OkHttpClient client() {
        return this.client;
    }

    void setUrlFilter(URLFilter uRLFilter) {
        this.urlFilter = uRLFilter;
    }

    public OkUrlFactory m1clone() {
        return new OkUrlFactory(this.client.m0clone());
    }

    public HttpURLConnection open(URL url) {
        return open(url, this.client.getProxy());
    }

    HttpURLConnection open(URL url, Proxy proxy) {
        String protocol = url.getProtocol();
        OkHttpClient okHttpClientCopyWithDefaults = this.client.copyWithDefaults();
        okHttpClientCopyWithDefaults.setProxy(proxy);
        if (protocol.equals("http")) {
            return new HttpURLConnectionImpl(url, okHttpClientCopyWithDefaults, this.urlFilter);
        }
        if (protocol.equals("https")) {
            return new HttpsURLConnectionImpl(url, okHttpClientCopyWithDefaults, this.urlFilter);
        }
        throw new IllegalArgumentException("Unexpected protocol: " + protocol);
    }

    @Override
    public URLStreamHandler createURLStreamHandler(final String str) {
        if (str.equals("http") || str.equals("https")) {
            return new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) {
                    return OkUrlFactory.this.open(url);
                }

                @Override
                protected URLConnection openConnection(URL url, Proxy proxy) {
                    return OkUrlFactory.this.open(url, proxy);
                }

                @Override
                protected int getDefaultPort() {
                    if (str.equals("http")) {
                        return 80;
                    }
                    if (str.equals("https")) {
                        return 443;
                    }
                    throw new AssertionError();
                }
            };
        }
        return null;
    }
}
