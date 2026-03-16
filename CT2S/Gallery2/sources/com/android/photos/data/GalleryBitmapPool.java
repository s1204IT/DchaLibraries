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

    private GalleryBitmapPool(int capacityBytes) {
        this.mPools[0] = new SparseArrayBitmapPool(capacityBytes / 3, this.mSharedNodePool);
        this.mPools[1] = new SparseArrayBitmapPool(capacityBytes / 3, this.mSharedNodePool);
        this.mPools[2] = new SparseArrayBitmapPool(capacityBytes / 3, this.mSharedNodePool);
        this.mCapacityBytes = capacityBytes;
    }

    public static GalleryBitmapPool getInstance() {
        return sInstance;
    }

    private SparseArrayBitmapPool getPoolForDimensions(int width, int height) {
        int index = getPoolIndexForDimensions(width, height);
        if (index == -1) {
            return null;
        }
        return this.mPools[index];
    }

    private int getPoolIndexForDimensions(int width, int height) {
        int min;
        int max;
        if (width <= 0 || height <= 0) {
            return -1;
        }
        if (width == height) {
            return 0;
        }
        if (width > height) {
            min = height;
            max = width;
        } else {
            min = width;
            max = height;
        }
        Point[] arr$ = COMMON_PHOTO_ASPECT_RATIOS;
        for (Point ar : arr$) {
            if (ar.x * min == ar.y * max) {
                return 1;
            }
        }
        return 2;
    }

    public Bitmap get(int width, int height) {
        SparseArrayBitmapPool pool = getPoolForDimensions(width, height);
        if (pool == null) {
            return null;
        }
        return pool.get(width, height);
    }

    public boolean put(Bitmap b) {
        if (b == null || b.getConfig() != Bitmap.Config.ARGB_8888) {
            return false;
        }
        SparseArrayBitmapPool pool = getPoolForDimensions(b.getWidth(), b.getHeight());
        if (pool == null) {
            b.recycle();
            return false;
        }
        return pool.put(b);
    }

    public void clear() {
        SparseArrayBitmapPool[] arr$ = this.mPools;
        for (SparseArrayBitmapPool p : arr$) {
            p.clear();
        }
    }
}
