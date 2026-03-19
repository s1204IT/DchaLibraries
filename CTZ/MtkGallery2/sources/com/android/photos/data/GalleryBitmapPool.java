package com.android.photos.data;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Pools;
import com.android.photos.data.SparseArrayBitmapPool;

public class GalleryBitmapPool {
    private static final Point[] COMMON_PHOTO_ASPECT_RATIOS = {new Point(4, 3), new Point(3, 2), new Point(16, 9)};
    private static GalleryBitmapPool sInstance = new GalleryBitmapPool(20971520);
    private int mCapacityBytes;
    private Pools.Pool<SparseArrayBitmapPool.Node> mSharedNodePool = new Pools.SynchronizedPool(128);
    private SparseArrayBitmapPool[] mPools = new SparseArrayBitmapPool[3];

    private GalleryBitmapPool(int i) {
        int i2 = i / 3;
        this.mPools[0] = new SparseArrayBitmapPool(i2, this.mSharedNodePool);
        this.mPools[1] = new SparseArrayBitmapPool(i2, this.mSharedNodePool);
        this.mPools[2] = new SparseArrayBitmapPool(i2, this.mSharedNodePool);
        this.mCapacityBytes = i;
    }

    public static GalleryBitmapPool getInstance() {
        return sInstance;
    }

    private SparseArrayBitmapPool getPoolForDimensions(int i, int i2) {
        int poolIndexForDimensions = getPoolIndexForDimensions(i, i2);
        if (poolIndexForDimensions == -1) {
            return null;
        }
        return this.mPools[poolIndexForDimensions];
    }

    private int getPoolIndexForDimensions(int i, int i2) {
        if (i <= 0 || i2 <= 0) {
            return -1;
        }
        if (i == i2) {
            return 0;
        }
        if (i > i2) {
            i2 = i;
            i = i2;
        }
        for (Point point : COMMON_PHOTO_ASPECT_RATIOS) {
            if (point.x * i == point.y * i2) {
                return 1;
            }
        }
        return 2;
    }

    public Bitmap get(int i, int i2) {
        SparseArrayBitmapPool poolForDimensions = getPoolForDimensions(i, i2);
        if (poolForDimensions == null) {
            return null;
        }
        return poolForDimensions.get(i, i2);
    }

    public boolean put(Bitmap bitmap) {
        if (bitmap == null || bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            return false;
        }
        SparseArrayBitmapPool poolForDimensions = getPoolForDimensions(bitmap.getWidth(), bitmap.getHeight());
        if (poolForDimensions == null) {
            bitmap.recycle();
            return false;
        }
        return poolForDimensions.put(bitmap);
    }

    public void clear() {
        for (SparseArrayBitmapPool sparseArrayBitmapPool : this.mPools) {
            sparseArrayBitmapPool.clear();
        }
    }
}
