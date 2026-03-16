package com.android.volley.toolbox;

import android.os.Handler;
import android.os.Looper;
import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

public class ClearCacheRequest extends Request<Object> {
    private final Cache mCache;
    private final Runnable mCallback;

    public ClearCacheRequest(Cache cache, Runnable callback) {
        super(0, null, null);
        this.mCache = cache;
        this.mCallback = callback;
    }

    @Override
    public boolean isCanceled() {
        this.mCache.clear();
        if (this.mCallback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postAtFrontOfQueue(this.mCallback);
            return true;
        }
        return true;
    }

    @Override
    public Request.Priority getPriority() {
        return Request.Priority.IMMEDIATE;
    }

    @Override
    protected Response<Object> parseNetworkResponse(NetworkResponse response) {
        return null;
    }

    @Override
    protected void deliverResponse(Object response) {
    }
}
