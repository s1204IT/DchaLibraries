package com.android.gallery3d.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.gallery3d.common.BlobCache;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class CacheManager {
    private static HashMap<String, BlobCache> sCacheMap = new HashMap<>();
    private static boolean sOldCheckDone = false;
    private static boolean sNoStorage = false;

    public static BlobCache getCache(Context context, String str, int i, int i2, int i3) {
        BlobCache blobCache;
        synchronized (sCacheMap) {
            if (!sOldCheckDone) {
                removeOldFilesIfNecessary(context);
                sOldCheckDone = true;
            }
            BlobCache blobCache2 = sCacheMap.get(str);
            if (blobCache2 == null) {
                File externalCacheDir = FeatureHelper.getExternalCacheDir(context);
                if (externalCacheDir == null) {
                    Log.e("Gallery2/CacheManager", "<getCache> failed to get cache dir");
                    return null;
                }
                String str2 = externalCacheDir.getAbsolutePath() + "/" + str;
                try {
                    Log.d("Gallery2/CacheManager", "<getCache> new BlobCache, path = " + str2);
                    blobCache = new BlobCache(str2, i, i2, false, i3);
                } catch (IOException e) {
                    e = e;
                }
                try {
                    sCacheMap.put(str, blobCache);
                    blobCache2 = blobCache;
                } catch (IOException e2) {
                    e = e2;
                    blobCache2 = blobCache;
                    Log.e("Gallery2/CacheManager", "Cannot instantiate cache!", e);
                }
            }
            return blobCache2;
        }
    }

    private static void removeOldFilesIfNecessary(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int i = 0;
        try {
            i = defaultSharedPreferences.getInt("cache-up-to-date", 0);
        } catch (Throwable th) {
        }
        if (i != 0) {
            return;
        }
        defaultSharedPreferences.edit().putInt("cache-up-to-date", 1).commit();
        File externalCacheDir = FeatureHelper.getExternalCacheDir(context);
        if (externalCacheDir == null) {
            Log.e("Gallery2/CacheManager", "<removeOldFilesIfNecessary> failed to get cache dir");
            return;
        }
        String str = externalCacheDir.getAbsolutePath() + "/";
        BlobCache.deleteFiles(str + "imgcache");
        BlobCache.deleteFiles(str + "rev_geocoding");
        BlobCache.deleteFiles(str + "bookmark");
    }

    public static void storageStateChanged(boolean z) {
        synchronized (sCacheMap) {
            try {
                if (z) {
                    sNoStorage = false;
                } else {
                    sNoStorage = true;
                    for (BlobCache blobCache : sCacheMap.values()) {
                        Log.d("Gallery2/CacheManager", " => closing " + blobCache);
                        blobCache.close();
                        Log.d("Gallery2/CacheManager", " <= closing " + blobCache);
                    }
                    sCacheMap.clear();
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }
}
