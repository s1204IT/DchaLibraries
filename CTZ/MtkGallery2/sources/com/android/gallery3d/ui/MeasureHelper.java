package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.view.View;

class MeasureHelper {
    private static MeasureHelper sInstance = new MeasureHelper(null);
    private GLView mComponent;
    private int mPreferredHeight;
    private int mPreferredWidth;

    private MeasureHelper(GLView gLView) {
        this.mComponent = gLView;
    }

    public static MeasureHelper getInstance(GLView gLView) {
        sInstance.mComponent = gLView;
        return sInstance;
    }

    public MeasureHelper setPreferredContentSize(int i, int i2) {
        this.mPreferredWidth = i;
        this.mPreferredHeight = i2;
        return this;
    }

    public void measure(int i, int i2) {
        Rect paddings = this.mComponent.getPaddings();
        setMeasuredSize(getLength(i, this.mPreferredWidth + paddings.left + paddings.right), getLength(i2, this.mPreferredHeight + paddings.top + paddings.bottom));
    }

    private static int getLength(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int mode = View.MeasureSpec.getMode(i);
        if (mode == Integer.MIN_VALUE) {
            return Math.min(i2, size);
        }
        if (mode == 1073741824) {
            return size;
        }
        return i2;
    }

    protected void setMeasuredSize(int i, int i2) {
        this.mComponent.setMeasuredSize(i, i2);
    }
}
