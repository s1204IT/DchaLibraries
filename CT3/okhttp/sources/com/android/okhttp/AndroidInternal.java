package com.android.okhttp;

import com.android.okhttp.internal.huc.CacheAdapter;
import java.net.ResponseCache;

public class AndroidInternal {
    private AndroidInternal() {
    }

    public static void setResponseCache(OkUrlFactory okUrlFactory, ResponseCache responseCache) {
        OkHttpClient client = okUrlFactory.client();
        if (responseCache instanceof OkCacheContainer) {
            OkCacheContainer okCacheContainer = (OkCacheContainer) responseCache;
            client.setCache(okCacheContainer.getCache());
        } else {
            client.setInternalCache(responseCache != 0 ? new CacheAdapter(responseCache) : null);
        }
    }
}
