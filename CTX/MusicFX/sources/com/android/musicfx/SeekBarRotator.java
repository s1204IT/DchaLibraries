package com.android.musicfx;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class SeekBarRotator extends ViewGroup {
    public SeekBarRotator(Context context) {
        super(context);
    }

    public SeekBarRotator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public SeekBarRotator(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public SeekBarRotator(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        View childAt = getChildAt(0);
        if (childAt.getVisibility() != 8) {
            measureChild(childAt, i2, i);
            setMeasuredDimension(childAt.getMeasuredHeightAndState(), childAt.getMeasuredWidthAndState());
        } else {
            setMeasuredDimension(resolveSizeAndState(0, i, 0), resolveSizeAndState(0, i2, 0));
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        View childAt = getChildAt(0);
        if (childAt.getVisibility() != 8) {
            childAt.setPivotX(0.0f);
            childAt.setPivotY(0.0f);
            childAt.setRotation(-90.0f);
            int i5 = i4 - i2;
            childAt.layout(0, i5, i5, (i3 - i) + i5);
        }
    }
}
