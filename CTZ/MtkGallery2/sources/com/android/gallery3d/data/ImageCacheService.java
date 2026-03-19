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
    private static String IMAGE_CACHE_FILE = "imgcache";
    public static volatile String sForceObsoletePath = null;
    private BlobCache mCache;
    private Object mCacheLock = new Object();
    private Context mContext;

    public ImageCacheService(Context context) {
        this.mContext = context;
        this.mCache = CacheManager.getCache(context, IMAGE_CACHE_FILE, 5000, 209715200, 7);
        Log.d("Gallery2/ImageCacheService", " <ImageCacheService> IMAGE_CACHE_FILE = " + IMAGE_CACHE_FILE + " mCache = " + this.mCache);
    }

    public boolean getImageData(Path path, long j, int i, BytesBufferPool.BytesBuffer bytesBuffer) {
        BlobCache.LookupRequest lookupRequest;
        synchronized (this.mCacheLock) {
            if (this.mCache == null) {
                Log.e("Gallery2/ImageCacheService", "<getImageData>: cache file is null!");
                return false;
            }
            byte[] bArrMakeKey = makeKey(path, j, i);
            long jCrc64Long = Utils.crc64Long(bArrMakeKey);
            try {
                lookupRequest = new BlobCache.LookupRequest();
                lookupRequest.key = jCrc64Long;
                lookupRequest.buffer = bytesBuffer.data;
            } catch (IOException e) {
            }
            synchronized (this.mCacheLock) {
                if (this.mCache == null) {
                    return false;
                }
                if (!this.mCache.lookup(lookupRequest)) {
                    return false;
                }
                if (isSameKey(bArrMakeKey, lookupRequest.buffer)) {
                    bytesBuffer.data = lookupRequest.buffer;
                    bytesBuffer.offset = bArrMakeKey.length;
                    bytesBuffer.length = lookupRequest.length - bytesBuffer.offset;
                    return true;
                }
                return false;
            }
        }
    }

    public void putImageData(Path path, long j, int i, byte[] bArr) {
        synchronized (this.mCacheLock) {
            if (this.mCache == null) {
                Log.e("Gallery2/ImageCacheService", "<putImageData>: cache file is null!");
                return;
            }
            byte[] bArrMakeKey = makeKey(path, j, i);
            long jCrc64Long = Utils.crc64Long(bArrMakeKey);
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArrMakeKey.length + bArr.length);
            byteBufferAllocate.put(bArrMakeKey);
            byteBufferAllocate.put(bArr);
            synchronized (this.mCacheLock) {
                try {
                    if (this.mCache != null) {
                        this.mCache.insert(jCrc64Long, byteBufferAllocate.array());
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    public void clearImageData(Path path, long j, int i) {
        long jCrc64Long = Utils.crc64Long(makeKey(path, j, i));
        synchronized (this.mCacheLock) {
            try {
                if (this.mCache != null) {
                    this.mCache.clearEntry(jCrc64Long);
                }
            } catch (IOException e) {
            }
        }
    }

    private static byte[] makeKey(Path path, long j, int i) {
        return GalleryUtils.getBytes(path.toString() + "+" + j + "+" + i);
    }

    private static boolean isSameKey(byte[] bArr, byte[] bArr2) {
        int length = bArr.length;
        if (bArr2.length < length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (bArr[i] != bArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public void closeCache() {
        synchronized (this.mCacheLock) {
            this.mCache = null;
        }
    }

    public void openCache() {
        synchronized (this.mCacheLock) {
            if (this.mCache == null) {
                this.mCache = CacheManager.getCache(this.mContext, IMAGE_CACHE_FILE, 5000, 209715200, 7);
            }
        }
    }

    public static void setCacheName(String str) {
        IMAGE_CACHE_FILE = str;
    }
}
