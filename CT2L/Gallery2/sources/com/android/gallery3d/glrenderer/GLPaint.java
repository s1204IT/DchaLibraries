package com.android.gallery3d.glrenderer;

import junit.framework.Assert;

public class GLPaint {
    private float mLineWidth = 1.0f;
    private int mColor = 0;

    public void setColor(int color) {
        this.mColor = color;
    }

    public int getColor() {
        return this.mColor;
    }

    public void setLineWidth(float width) {
        Assert.assertTrue(width >= 0.0f);
        this.mLineWidth = width;
    }

    public float getLineWidth() {
        return this.mLineWidth;
    }
}
