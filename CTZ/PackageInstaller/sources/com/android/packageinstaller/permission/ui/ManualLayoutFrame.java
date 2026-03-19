package com.android.packageinstaller.permission.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ManualLayoutFrame extends ViewGroup {
    private int mContentBottom;
    private int mWidth;

    public ManualLayoutFrame(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void onConfigurationChanged() {
        this.mContentBottom = 0;
        this.mWidth = 0;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        if (this.mWidth != 0) {
            int iMin = this.mWidth;
            int mode = View.MeasureSpec.getMode(i);
            if (mode == Integer.MIN_VALUE) {
                iMin = Math.min(this.mWidth, View.MeasureSpec.getSize(i));
            } else if (mode == 1073741824) {
                iMin = View.MeasureSpec.getSize(i);
            }
            if (iMin != this.mWidth) {
                this.mWidth = iMin;
            }
            i = View.MeasureSpec.makeMeasureSpec(this.mWidth, 1073741824);
        }
        super.onMeasure(i, i2);
        if (this.mWidth == 0) {
            this.mWidth = getMeasuredWidth();
        }
        measureChild(getChildAt(0), i, i2);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        View childAt = getChildAt(0);
        if (this.mContentBottom == 0 || childAt.getMeasuredHeight() > this.mContentBottom) {
            this.mContentBottom = (getMeasuredHeight() + childAt.getMeasuredHeight()) / 2;
        }
        int measuredWidth = (getMeasuredWidth() - childAt.getMeasuredWidth()) / 2;
        childAt.layout(measuredWidth, this.mContentBottom - childAt.getMeasuredHeight(), childAt.getMeasuredWidth() + measuredWidth, this.mContentBottom);
    }
}
