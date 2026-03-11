package com.android.systemui.recent;

import android.graphics.drawable.ColorDrawable;

public class ColorDrawableWithDimensions extends ColorDrawable {
    private int mHeight;
    private int mWidth;

    public ColorDrawableWithDimensions(int color, int width, int height) {
        super(color);
        this.mWidth = width;
        this.mHeight = height;
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mHeight;
    }
}
