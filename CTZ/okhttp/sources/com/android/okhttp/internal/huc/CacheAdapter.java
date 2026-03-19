package com.android.okhttp.internal.huc;

import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.internal.InternalCache;
import com.android.okhttp.internal.http.CacheRequest;
import com.android.okhttp.internal.http.CacheStrategy;
import com.android.okhttp.okio.Okio;
import com.android.okhttp.okio.Sink;
import java.io.IOException;
import java.io.OutputStream;
import java.net.CacheResponse;
import java.net.ResponseCache;

public final class CacheAdapter implements InternalCache {
    private final ResponseCache delegate;

    public CacheAdapter(ResponseCache responseCache) {
        this.delegate = responseCache;
    }

    public ResponseCache getDelegate() {
        return this.delegate;
    }

    @Override
    public Response get(Request request) throws IOException {
        CacheResponse javaCachedResponse = getJavaCachedResponse(request);
        if (javaCachedResponse == null) {
            return null;
        }
        return JavaApiConverter.createOkResponseForCacheGet(request, javaCachedResponse);
    }

    @Override
    public CacheRequest put(Response response) throws IOException {
        final java.net.CacheRequest cacheRequestPut = this.delegate.put(response.request().uri(), JavaApiConverter.createJavaUrlConnectionForCachePut(response));
        if (cacheRequestPut == null) {
            return null;
        }
        return new CacheRequest() {
            @Override
            public Sink body() throws IOException {
                OutputStream body = cacheRequestPut.getBody();
                if (body != null) {
                    return Okio.sink(body);
                }
                return null;
            }

            @Override
            public void abort() {
                cacheRequestPut.abort();
            }
        };
    }

    @Override
    public void remove(Request request) throws IOException {
    }

    @Override
    public void update(Response response, Response response2) throws IOException {
    }

    @Override
    public void trackConditionalCacheHit() {
    }

    @Override
    public void trackResponse(CacheStrategy cacheStrategy) {
    }

    private CacheResponse getJavaCachedResponse(Request request) throws IOException {
        return this.delegate.get(request.uri(), request.method(), JavaApiConverter.extractJavaHeaders(request));
    }
}
