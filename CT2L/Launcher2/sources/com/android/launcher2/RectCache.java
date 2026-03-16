package com.android.launcher2;

import android.graphics.Rect;

class RectCache extends SoftReferenceThreadLocal<Rect> {
    RectCache() {
    }

    @Override
    protected Rect initialValue() {
        return new Rect();
    }
}
