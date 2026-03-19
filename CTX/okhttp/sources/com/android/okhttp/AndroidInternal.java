package com.android.okhttp;

import com.android.okhttp.internal.huc.CacheAdapter;
import java.net.ResponseCache;

public class AndroidInternal {
    private AndroidInternal() {
    }

    public static void setResponseCache(OkUrlFactory okUrlFactory, ResponseCache responseCache) {
        OkHttpClient okHttpClientClient = okUrlFactory.client();
        if (responseCache instanceof OkCacheContainer) {
            okHttpClientClient.setCache(((OkCacheContainer) responseCache).getCache());
        } else {
            okHttpClientClient.setInternalCache(responseCache != 0 ? new CacheAdapter(responseCache) : null);
        }
    }
}
