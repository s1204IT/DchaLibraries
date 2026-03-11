package com.android.launcher2;

import android.graphics.Rect;

class RectCache extends SoftReferenceThreadLocal<Rect> {
    RectCache() {
    }

    @Override
    public Rect initialValue() {
        return new Rect();
    }
}
