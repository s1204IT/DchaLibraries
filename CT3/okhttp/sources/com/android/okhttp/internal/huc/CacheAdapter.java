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
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.URI;
import java.util.List;
import java.util.Map;

public final class CacheAdapter implements InternalCache {
    private final ResponseCache delegate;

    public CacheAdapter(ResponseCache delegate) {
        this.delegate = delegate;
    }

    public ResponseCache getDelegate() {
        return this.delegate;
    }

    @Override
    public Response get(Request request) throws IOException {
        CacheResponse javaResponse = getJavaCachedResponse(request);
        if (javaResponse == null) {
            return null;
        }
        return JavaApiConverter.createOkResponseForCacheGet(request, javaResponse);
    }

    @Override
    public CacheRequest put(Response response) throws IOException {
        URI uri = response.request().uri();
        HttpURLConnection connection = JavaApiConverter.createJavaUrlConnectionForCachePut(response);
        final java.net.CacheRequest request = this.delegate.put(uri, connection);
        if (request == null) {
            return null;
        }
        return new CacheRequest() {
            @Override
            public Sink body() throws IOException {
                OutputStream body = request.getBody();
                if (body != null) {
                    return Okio.sink(body);
                }
                return null;
            }

            @Override
            public void abort() {
                request.abort();
            }
        };
    }

    @Override
    public void remove(Request request) throws IOException {
    }

    @Override
    public void update(Response cached, Response network) throws IOException {
    }

    @Override
    public void trackConditionalCacheHit() {
    }

    @Override
    public void trackResponse(CacheStrategy cacheStrategy) {
    }

    private CacheResponse getJavaCachedResponse(Request request) throws IOException {
        Map<String, List<String>> headers = JavaApiConverter.extractJavaHeaders(request);
        return this.delegate.get(request.uri(), request.method(), headers);
    }
}
