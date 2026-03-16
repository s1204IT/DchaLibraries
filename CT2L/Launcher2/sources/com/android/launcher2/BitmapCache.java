package com.android.launcher2;

import android.graphics.Bitmap;

class BitmapCache extends SoftReferenceThreadLocal<Bitmap> {
    BitmapCache() {
    }

    @Override
    protected Bitmap initialValue() {
        return null;
    }
}
