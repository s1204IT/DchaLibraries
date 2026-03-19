package com.android.gallery3d.filtershow.cache;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.android.gallery3d.filtershow.pipeline.CacheProcessing;
import com.mediatek.gallery3d.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class BitmapCache {
    private CacheProcessing mCacheProcessing;
    private HashMap<Long, ArrayList<WeakReference<Bitmap>>> mBitmapCache = new HashMap<>();
    private final int mMaxItemsPerKey = 4;
    private int[] mTracking = new int[14];
    private ArrayList<Object> mBitmapTracking = new ArrayList<>();

    public void showBitmapCounts() {
    }

    public void setCacheProcessing(CacheProcessing cacheProcessing) {
        this.mCacheProcessing = cacheProcessing;
    }

    public synchronized boolean cache(Bitmap bitmap) {
        if (bitmap == null) {
            return true;
        }
        if (this.mCacheProcessing != null && this.mCacheProcessing.contains(bitmap)) {
            Log.e("BitmapCache", "Trying to cache a bitmap still used in the pipeline");
            return false;
        }
        if (!bitmap.isMutable()) {
            Log.e("BitmapCache", "Trying to cache a non mutable bitmap");
            return true;
        }
        Long lCalcKey = calcKey(bitmap.getWidth(), bitmap.getHeight());
        ArrayList<WeakReference<Bitmap>> arrayList = this.mBitmapCache.get(lCalcKey);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.mBitmapCache.put(lCalcKey, arrayList);
        }
        int i = 0;
        while (i < arrayList.size()) {
            if (arrayList.get(i).get() == null) {
                arrayList.remove(i);
            } else {
                i++;
            }
        }
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            if (arrayList.get(i2).get() == null) {
                arrayList.remove(i2);
            }
        }
        if (arrayList.size() < 4) {
            for (int i3 = 0; i3 < arrayList.size(); i3++) {
                if (arrayList.get(i3).get() == bitmap) {
                    return true;
                }
            }
            arrayList.add(new WeakReference<>(bitmap));
        }
        return true;
    }

    public synchronized Bitmap getBitmap(int i, int i2, int i3) {
        Bitmap bitmapCreateBitmap;
        WeakReference<Bitmap> weakReferenceRemove;
        Long lCalcKey = calcKey(i, i2);
        ArrayList<WeakReference<Bitmap>> arrayList = this.mBitmapCache.get(lCalcKey);
        bitmapCreateBitmap = null;
        if (arrayList != null && arrayList.size() > 0) {
            weakReferenceRemove = arrayList.remove(0);
            if (arrayList.size() == 0) {
                this.mBitmapCache.remove(lCalcKey);
            }
        } else {
            weakReferenceRemove = null;
        }
        if (weakReferenceRemove != null) {
            bitmapCreateBitmap = weakReferenceRemove.get();
        }
        if (bitmapCreateBitmap == null || bitmapCreateBitmap.getWidth() != i || bitmapCreateBitmap.getHeight() != i2) {
            bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
            showBitmapCounts();
        }
        return bitmapCreateBitmap;
    }

    public synchronized Bitmap getBitmapCopy(Bitmap bitmap, int i) {
        Bitmap bitmap2;
        bitmap2 = getBitmap(bitmap.getWidth(), bitmap.getHeight(), i);
        bitmap2.eraseColor(0);
        new Canvas(bitmap2).drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
        return bitmap2;
    }

    private Long calcKey(long j, long j2) {
        return Long.valueOf((j << 32) | j2);
    }
}
