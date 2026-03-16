package com.android.launcher2;

import android.graphics.Paint;

class PaintCache extends SoftReferenceThreadLocal<Paint> {
    PaintCache() {
    }

    @Override
    protected Paint initialValue() {
        return null;
    }
}
