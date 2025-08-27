package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.android.settings.R;

/* loaded from: classes.dex */
public final class AspectRatioFrameLayout extends FrameLayout {
    float mAspectRatio;

    public AspectRatioFrameLayout(Context context) {
        this(context, null);
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mAspectRatio = 1.0f;
        if (attributeSet != null) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.AspectRatioFrameLayout);
            this.mAspectRatio = typedArrayObtainStyledAttributes.getFloat(0, 1.0f);
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    public void setAspectRatio(float f) {
        this.mAspectRatio = f;
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        if (measuredWidth == 0 || measuredHeight == 0) {
            return;
        }
        float f = measuredHeight;
        if (Math.abs(this.mAspectRatio - (measuredWidth / f)) <= 0.01f) {
            return;
        }
        super.onMeasure(View.MeasureSpec.makeMeasureSpec((int) (f * this.mAspectRatio), 1073741824), View.MeasureSpec.makeMeasureSpec(measuredHeight, 1073741824));
    }
}
