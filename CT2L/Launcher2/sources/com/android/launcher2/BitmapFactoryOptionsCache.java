package com.android.launcher2;

import android.graphics.BitmapFactory;

class BitmapFactoryOptionsCache extends SoftReferenceThreadLocal<BitmapFactory.Options> {
    BitmapFactoryOptionsCache() {
    }

    @Override
    protected BitmapFactory.Options initialValue() {
        return new BitmapFactory.Options();
    }
}
