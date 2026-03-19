package com.android.gallery3d.glrenderer;

import com.android.gallery3d.common.Utils;

public class GLPaint {
    private float mLineWidth = 1.0f;
    private int mColor = 0;

    public void setColor(int i) {
        this.mColor = i;
    }

    public int getColor() {
        return this.mColor;
    }

    public void setLineWidth(float f) {
        Utils.assertTrue(f >= 0.0f);
        this.mLineWidth = f;
    }

    public float getLineWidth() {
        return this.mLineWidth;
    }
}
