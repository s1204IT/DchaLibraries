package com.android.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ResizingSpace extends View {
    private final int mHeight;
    private final int mWidth;

    public ResizingSpace(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        if (getVisibility() == 0) {
            setVisibility(4);
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, android.R.styleable.ViewGroup_Layout);
        this.mWidth = typedArrayObtainStyledAttributes.getResourceId(0, 0);
        this.mHeight = typedArrayObtainStyledAttributes.getResourceId(1, 0);
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        boolean z;
        int dimensionPixelOffset;
        int dimensionPixelOffset2;
        super.onConfigurationChanged(configuration);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (this.mWidth <= 0 || (dimensionPixelOffset2 = getContext().getResources().getDimensionPixelOffset(this.mWidth)) == layoutParams.width) {
            z = false;
        } else {
            layoutParams.width = dimensionPixelOffset2;
            z = true;
        }
        if (this.mHeight > 0 && (dimensionPixelOffset = getContext().getResources().getDimensionPixelOffset(this.mHeight)) != layoutParams.height) {
            layoutParams.height = dimensionPixelOffset;
            z = true;
        }
        if (z) {
            setLayoutParams(layoutParams);
        }
    }

    @Override
    public void draw(Canvas canvas) {
    }

    private static int getDefaultSize2(int i, int i2) {
        int mode = View.MeasureSpec.getMode(i2);
        int size = View.MeasureSpec.getSize(i2);
        if (mode != Integer.MIN_VALUE) {
            return (mode == 0 || mode != 1073741824) ? i : size;
        }
        return Math.min(i, size);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(getDefaultSize2(getSuggestedMinimumWidth(), i), getDefaultSize2(getSuggestedMinimumHeight(), i2));
    }
}
