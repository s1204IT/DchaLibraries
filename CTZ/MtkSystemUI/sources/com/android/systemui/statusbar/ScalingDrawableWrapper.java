package com.android.systemui.statusbar;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;

public class ScalingDrawableWrapper extends DrawableWrapper {
    private float mScaleFactor;

    public ScalingDrawableWrapper(Drawable drawable, float f) {
        super(drawable);
        this.mScaleFactor = f;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) (super.getIntrinsicWidth() * this.mScaleFactor);
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) (super.getIntrinsicHeight() * this.mScaleFactor);
    }
}
