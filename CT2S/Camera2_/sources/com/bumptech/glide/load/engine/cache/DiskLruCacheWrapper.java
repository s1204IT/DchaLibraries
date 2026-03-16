package com.bumptech.glide.load.engine.cache;

import android.util.Log;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.jakewharton.disklrucache.DiskLruCache;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DiskLruCacheWrapper implements DiskCache {
    private static final int APP_VERSION = 1;
    private static final String TAG = "DiskLruCacheWrapper";
    private static final int VALUE_COUNT = 1;
    private static DiskLruCacheWrapper WRAPPER = null;
    private final File directory;
    private DiskLruCache diskLruCache;
    private final int maxSize;
    private final SafeKeyGenerator safeKeyGenerator = new SafeKeyGenerator();

    public static synchronized DiskCache get(File directory, int maxSize) {
        if (WRAPPER == null) {
            WRAPPER = new DiskLruCacheWrapper(directory, maxSize);
        }
        return WRAPPER;
    }

    protected DiskLruCacheWrapper(File directory, int maxSize) {
        this.directory = directory;
        this.maxSize = maxSize;
    }

    private synchronized DiskLruCache getDiskCache() throws IOException {
        if (this.diskLruCache == null) {
            this.diskLruCache = DiskLruCache.open(this.directory, 1, 1, this.maxSize);
        }
        return this.diskLruCache;
    }

    @Override
    public InputStream get(Key key) {
        String safeKey = this.safeKeyGenerator.getSafeKey(key);
        try {
            DiskLruCache.Snapshot snapshot = getDiskCache().get(safeKey);
            if (snapshot == null) {
                return null;
            }
            InputStream result = snapshot.getInputStream(0);
            return result;
        } catch (IOException e) {
            if (!Log.isLoggable(TAG, 5)) {
                return null;
            }
            Log.w(TAG, "Unable to get from disk cache", e);
            return null;
        }
    }

    @Override
    public void put(Key key, DiskCache.Writer writer) {
        String safeKey = this.safeKeyGenerator.getSafeKey(key);
        try {
            DiskLruCache.Editor editor = getDiskCache().edit(safeKey);
            if (editor != null) {
                OutputStream os = null;
                try {
                    os = editor.newOutputStream(0);
                    boolean success = writer.write(os);
                    if (success) {
                        editor.commit();
                    }
                } finally {
                    if (os != null) {
                        os.close();
                    }
                }
            }
        } catch (IOException e) {
            if (Log.isLoggable(TAG, 5)) {
                Log.w(TAG, "Unable to put to disk cache", e);
            }
        }
    }

    @Override
    public void delete(Key key) {
        String safeKey = this.safeKeyGenerator.getSafeKey(key);
        try {
            getDiskCache().remove(safeKey);
        } catch (IOException e) {
            if (Log.isLoggable(TAG, 5)) {
                Log.w(TAG, "Unable to delete from disk cache", e);
            }
        }
    }
}
