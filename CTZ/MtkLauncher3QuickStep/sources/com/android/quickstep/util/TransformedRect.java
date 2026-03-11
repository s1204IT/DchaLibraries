package com.android.quickstep.util;

import android.graphics.Rect;

public class TransformedRect {
    public final Rect rect = new Rect();
    public float scale = 1.0f;

    public void set(TransformedRect transformedRect) {
        this.rect.set(transformedRect.rect);
        this.scale = transformedRect.scale;
    }
}
