package com.android.gallery3d.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.gallery3d.common.BlobCache;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class CacheManager {
    private static HashMap<String, BlobCache> sCacheMap = new HashMap<>();
    private static boolean sOldCheckDone = false;

    public static BlobCache getCache(Context context, String filename, int maxEntries, int maxBytes, int version) {
        BlobCache cache;
        synchronized (sCacheMap) {
            if (!sOldCheckDone) {
                removeOldFilesIfNecessary(context);
                sOldCheckDone = true;
            }
            BlobCache cache2 = sCacheMap.get(filename);
            if (cache2 == null) {
                File cacheDir = context.getExternalCacheDir();
                String path = cacheDir.getAbsolutePath() + "/" + filename;
                try {
                    cache = new BlobCache(path, maxEntries, maxBytes, false, version);
                } catch (IOException e) {
                    e = e;
                    cache = cache2;
                }
                try {
                    sCacheMap.put(filename, cache);
                } catch (IOException e2) {
                    e = e2;
                    Log.e("CacheManager", "Cannot instantiate cache!", e);
                }
            } else {
                cache = cache2;
            }
        }
        return cache;
    }

    private static void removeOldFilesIfNecessary(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int n = 0;
        try {
            n = pref.getInt("cache-up-to-date", 0);
        } catch (Throwable th) {
        }
        if (n == 0) {
            pref.edit().putInt("cache-up-to-date", 1).commit();
            File cacheDir = context.getExternalCacheDir();
            String prefix = cacheDir.getAbsolutePath() + "/";
            BlobCache.deleteFiles(prefix + "imgcache");
            BlobCache.deleteFiles(prefix + "rev_geocoding");
            BlobCache.deleteFiles(prefix + "bookmark");
        }
    }
}
