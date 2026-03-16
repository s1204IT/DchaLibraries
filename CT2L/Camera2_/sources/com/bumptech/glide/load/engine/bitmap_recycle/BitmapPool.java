package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;

public interface BitmapPool {
    void clearMemory();

    Bitmap get(int i, int i2, Bitmap.Config config);

    boolean put(Bitmap bitmap);

    void setSizeMultiplier(float f);

    void trimMemory(int i);
}
