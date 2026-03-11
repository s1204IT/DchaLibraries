package com.android.launcher2;

import android.graphics.Bitmap;

class BitmapCache extends SoftReferenceThreadLocal<Bitmap> {
    BitmapCache() {
    }

    @Override
    public Bitmap initialValue() {
        return null;
    }
}
