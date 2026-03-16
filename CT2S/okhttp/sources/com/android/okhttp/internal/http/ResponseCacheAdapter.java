package com.android.okhttp.internal.http;

import com.android.okhttp.OkResponseCache;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.ResponseSource;
import java.io.IOException;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class ResponseCacheAdapter implements OkResponseCache {
    private final ResponseCache delegate;

    public ResponseCacheAdapter(ResponseCache delegate) {
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
        return JavaApiConverter.createOkResponse(request, javaResponse);
    }

    @Override
    public CacheRequest put(Response response) throws IOException {
        URI uri = response.request().uri();
        HttpURLConnection connection = JavaApiConverter.createJavaUrlConnection(response);
        return this.delegate.put(uri, connection);
    }

    @Override
    public boolean maybeRemove(Request request) throws IOException {
        return false;
    }

    @Override
    public void update(Response cached, Response network) throws IOException {
    }

    @Override
    public void trackConditionalCacheHit() {
    }

    @Override
    public void trackResponse(ResponseSource source) {
    }

    private CacheResponse getJavaCachedResponse(Request request) throws IOException {
        Map<String, List<String>> headers = JavaApiConverter.extractJavaHeaders(request);
        return this.delegate.get(request.uri(), request.method(), headers);
    }
}
