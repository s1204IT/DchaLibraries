package com.android.launcher2;

import android.graphics.Canvas;

class CanvasCache extends SoftReferenceThreadLocal<Canvas> {
    CanvasCache() {
    }

    @Override
    public Canvas initialValue() {
        return new Canvas();
    }
}
