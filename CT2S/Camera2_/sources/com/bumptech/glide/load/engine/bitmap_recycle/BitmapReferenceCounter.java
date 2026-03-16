package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;

public interface BitmapReferenceCounter {
    void acquireBitmap(Bitmap bitmap);

    void releaseBitmap(Bitmap bitmap);
}
