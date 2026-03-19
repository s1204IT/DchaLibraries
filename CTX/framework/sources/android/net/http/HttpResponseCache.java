package android.net.http;

import com.android.okhttp.AndroidShimResponseCache;
import com.android.okhttp.Cache;
import com.android.okhttp.OkCacheContainer;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public final class HttpResponseCache extends ResponseCache implements Closeable, OkCacheContainer {
    private final AndroidShimResponseCache delegate;

    private HttpResponseCache(AndroidShimResponseCache androidShimResponseCache) {
        this.delegate = androidShimResponseCache;
    }

    public static HttpResponseCache getInstalled() {
        ResponseCache responseCache = ResponseCache.getDefault();
        if (responseCache instanceof HttpResponseCache) {
            return (HttpResponseCache) responseCache;
        }
        return null;
    }

    public static synchronized HttpResponseCache install(File file, long j) throws IOException {
        ResponseCache responseCache = ResponseCache.getDefault();
        if (responseCache instanceof HttpResponseCache) {
            HttpResponseCache httpResponseCache = (HttpResponseCache) responseCache;
            AndroidShimResponseCache androidShimResponseCache = httpResponseCache.delegate;
            if (androidShimResponseCache.isEquivalent(file, j)) {
                return httpResponseCache;
            }
            androidShimResponseCache.close();
        }
        HttpResponseCache httpResponseCache2 = new HttpResponseCache(AndroidShimResponseCache.create(file, j));
        ResponseCache.setDefault(httpResponseCache2);
        return httpResponseCache2;
    }

    @Override
    public CacheResponse get(URI uri, String str, Map<String, List<String>> map) throws IOException {
        return this.delegate.get(uri, str, map);
    }

    @Override
    public CacheRequest put(URI uri, URLConnection uRLConnection) throws IOException {
        return this.delegate.put(uri, uRLConnection);
    }

    public long size() {
        try {
            return this.delegate.size();
        } catch (IOException e) {
            return -1L;
        }
    }

    public long maxSize() {
        return this.delegate.maxSize();
    }

    public void flush() {
        try {
            this.delegate.flush();
        } catch (IOException e) {
        }
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

    @Override
    public void close() throws IOException {
        if (ResponseCache.getDefault() == this) {
            ResponseCache.setDefault(null);
        }
        this.delegate.close();
    }

    public void delete() throws IOException {
        if (ResponseCache.getDefault() == this) {
            ResponseCache.setDefault(null);
        }
        this.delegate.delete();
    }

    public Cache getCache() {
        return this.delegate.getCache();
    }
}
