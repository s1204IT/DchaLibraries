package com.android.gallery3d.filtershow.cache;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import com.android.gallery3d.filtershow.pipeline.CacheProcessing;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class BitmapCache {
    private CacheProcessing mCacheProcessing;
    private HashMap<Long, ArrayList<WeakReference<Bitmap>>> mBitmapCache = new HashMap<>();
    private final int mMaxItemsPerKey = 4;
    private int[] mTracking = new int[14];
    private ArrayList mBitmapTracking = new ArrayList();

    public void showBitmapCounts() {
    }

    public void setCacheProcessing(CacheProcessing cache) {
        this.mCacheProcessing = cache;
    }

    public synchronized boolean cache(Bitmap bitmap) {
        boolean z;
        if (bitmap == null) {
            z = true;
        } else if (this.mCacheProcessing != null && this.mCacheProcessing.contains(bitmap)) {
            Log.e("BitmapCache", "Trying to cache a bitmap still used in the pipeline");
            z = false;
        } else if (!bitmap.isMutable()) {
            Log.e("BitmapCache", "Trying to cache a non mutable bitmap");
            z = true;
        } else {
            Long key = calcKey(bitmap.getWidth(), bitmap.getHeight());
            ArrayList<WeakReference<Bitmap>> list = this.mBitmapCache.get(key);
            if (list == null) {
                list = new ArrayList<>();
                this.mBitmapCache.put(key, list);
            }
            int i = 0;
            while (i < list.size()) {
                if (list.get(i).get() == null) {
                    list.remove(i);
                } else {
                    i++;
                }
            }
            for (int i2 = 0; i2 < list.size(); i2++) {
                if (list.get(i2).get() == null) {
                    list.remove(i2);
                }
            }
            if (list.size() < 4) {
                for (int i3 = 0; i3 < list.size(); i3++) {
                    WeakReference<Bitmap> ref = list.get(i3);
                    if (ref.get() == bitmap) {
                        z = true;
                        break;
                    }
                }
                list.add(new WeakReference<>(bitmap));
                z = true;
            } else {
                z = true;
            }
        }
        return z;
    }

    public synchronized Bitmap getBitmap(int w, int h, int type) {
        Bitmap bitmap;
        Long key = calcKey(w, h);
        WeakReference<Bitmap> ref = null;
        ArrayList<WeakReference<Bitmap>> list = this.mBitmapCache.get(key);
        if (list != null && list.size() > 0) {
            ref = list.remove(0);
            if (list.size() == 0) {
                this.mBitmapCache.remove(key);
            }
        }
        bitmap = null;
        if (ref != null) {
            Bitmap bitmap2 = ref.get();
            bitmap = bitmap2;
        }
        if (bitmap == null || bitmap.getWidth() != w || bitmap.getHeight() != h) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            showBitmapCounts();
        }
        return bitmap;
    }

    public synchronized Bitmap getBitmapCopy(Bitmap source, int type) {
        Bitmap bitmap;
        bitmap = getBitmap(source.getWidth(), source.getHeight(), type);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(source, 0.0f, 0.0f, (Paint) null);
        return bitmap;
    }

    private Long calcKey(long w, long h) {
        return Long.valueOf((w << 32) | h);
    }
}
