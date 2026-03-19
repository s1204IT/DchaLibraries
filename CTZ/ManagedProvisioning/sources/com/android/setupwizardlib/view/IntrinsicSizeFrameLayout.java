package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.android.setupwizardlib.R;

public class IntrinsicSizeFrameLayout extends FrameLayout {
    private int mIntrinsicHeight;
    private int mIntrinsicWidth;

    public IntrinsicSizeFrameLayout(Context context) {
        super(context);
        this.mIntrinsicHeight = 0;
        this.mIntrinsicWidth = 0;
        init(context, null, 0);
    }

    public IntrinsicSizeFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIntrinsicHeight = 0;
        this.mIntrinsicWidth = 0;
        init(context, attributeSet, 0);
    }

    @TargetApi(11)
    public IntrinsicSizeFrameLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIntrinsicHeight = 0;
        this.mIntrinsicWidth = 0;
        init(context, attributeSet, i);
    }

    private void init(Context context, AttributeSet attributeSet, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SuwIntrinsicSizeFrameLayout, i, 0);
        this.mIntrinsicHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwIntrinsicSizeFrameLayout_android_height, 0);
        this.mIntrinsicWidth = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwIntrinsicSizeFrameLayout_android_width, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(getIntrinsicMeasureSpec(i, this.mIntrinsicWidth), getIntrinsicMeasureSpec(i2, this.mIntrinsicHeight));
    }

    private int getIntrinsicMeasureSpec(int i, int i2) {
        if (i2 <= 0) {
            return i;
        }
        int mode = View.MeasureSpec.getMode(i);
        int size = View.MeasureSpec.getSize(i);
        if (mode == 0) {
            return View.MeasureSpec.makeMeasureSpec(this.mIntrinsicHeight, 1073741824);
        }
        if (mode == Integer.MIN_VALUE) {
            return View.MeasureSpec.makeMeasureSpec(Math.min(size, this.mIntrinsicHeight), 1073741824);
        }
        return i;
    }
}
