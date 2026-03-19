package com.android.okhttp;

import com.android.okhttp.internal.huc.JavaApiConverter;
import java.io.File;
import java.io.IOException;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class AndroidShimResponseCache extends ResponseCache {
    private final Cache delegate;

    private AndroidShimResponseCache(Cache cache) {
        this.delegate = cache;
    }

    public static AndroidShimResponseCache create(File file, long j) throws IOException {
        return new AndroidShimResponseCache(new Cache(file, j));
    }

    public boolean isEquivalent(File file, long j) {
        Cache cache = getCache();
        return cache.getDirectory().equals(file) && cache.getMaxSize() == j && !cache.isClosed();
    }

    public Cache getCache() {
        return this.delegate;
    }

    @Override
    public CacheResponse get(URI uri, String str, Map<String, List<String>> map) throws IOException {
        Response response = this.delegate.internalCache.get(JavaApiConverter.createOkRequest(uri, str, map));
        if (response == null) {
            return null;
        }
        return JavaApiConverter.createJavaCacheResponse(response);
    }

    @Override
    public CacheRequest put(URI uri, URLConnection uRLConnection) throws IOException {
        com.android.okhttp.internal.http.CacheRequest cacheRequestPut;
        Response responseCreateOkResponseForCachePut = JavaApiConverter.createOkResponseForCachePut(uri, uRLConnection);
        if (responseCreateOkResponseForCachePut == null || (cacheRequestPut = this.delegate.internalCache.put(responseCreateOkResponseForCachePut)) == null) {
            return null;
        }
        return JavaApiConverter.createJavaCacheRequest(cacheRequestPut);
    }

    public long size() throws IOException {
        return this.delegate.getSize();
    }

    public long maxSize() {
        return this.delegate.getMaxSize();
    }

    public void flush() throws IOException {
        this.delegate.flush();
    }

    public int getNetworkCount() {
        return this.delegate.getNetworkCount();
    }

    public int getHitCount() {
        return this.delegate.getHitCount();
    }

    public int getRequestCount() {
        return this.delegate.getRequestCount();
    }

    public void close() throws IOException {
        this.delegate.close();
    }

    public void delete() throws IOException {
        this.delegate.delete();
    }
}
