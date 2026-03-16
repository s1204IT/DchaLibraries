package com.android.gallery3d.data;

import android.content.Context;
import com.android.gallery3d.common.BlobCache;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.BytesBufferPool;
import com.android.gallery3d.util.CacheManager;
import com.android.gallery3d.util.GalleryUtils;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageCacheService {
    private BlobCache mCache;

    public ImageCacheService(Context context) {
        this.mCache = CacheManager.getCache(context, "imgcache", 5000, 209715200, 7);
    }

    public boolean getImageData(Path path, long timeModified, int type, BytesBufferPool.BytesBuffer buffer) {
        boolean z = false;
        byte[] key = makeKey(path, timeModified, type);
        long cacheKey = Utils.crc64Long(key);
        try {
            BlobCache.LookupRequest request = new BlobCache.LookupRequest();
            request.key = cacheKey;
            request.buffer = buffer.data;
            synchronized (this.mCache) {
                if (this.mCache.lookup(request)) {
                    if (isSameKey(key, request.buffer)) {
                        buffer.data = request.buffer;
                        buffer.offset = key.length;
                        buffer.length = request.length - buffer.offset;
                        z = true;
                    }
                }
            }
        } catch (IOException e) {
        }
        return z;
    }

    public void putImageData(Path path, long timeModified, int type, byte[] value) {
        byte[] key = makeKey(path, timeModified, type);
        long cacheKey = Utils.crc64Long(key);
        ByteBuffer buffer = ByteBuffer.allocate(key.length + value.length);
        buffer.put(key);
        buffer.put(value);
        synchronized (this.mCache) {
            try {
                this.mCache.insert(cacheKey, buffer.array());
            } catch (IOException e) {
            }
        }
    }

    private static byte[] makeKey(Path path, long timeModified, int type) {
        return GalleryUtils.getBytes(path.toString() + "+" + timeModified + "+" + type);
    }

    private static boolean isSameKey(byte[] key, byte[] buffer) {
        int n = key.length;
        if (buffer.length < n) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (key[i] != buffer[i]) {
                return false;
            }
        }
        return true;
    }
}
