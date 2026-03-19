package com.android.setupwizardlib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.setupwizardlib.R;

public class FillContentLayout extends FrameLayout {
    private int mMaxHeight;
    private int mMaxWidth;

    public FillContentLayout(Context context) {
        this(context, null);
    }

    public FillContentLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.suwFillContentLayoutStyle);
    }

    public FillContentLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context, attributeSet, i);
    }

    private void init(Context context, AttributeSet attributeSet, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SuwFillContentLayout, i, 0);
        this.mMaxHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwFillContentLayout_android_maxHeight, -1);
        this.mMaxWidth = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwFillContentLayout_android_maxWidth, -1);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), i), getDefaultSize(getSuggestedMinimumHeight(), i2));
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            measureIllustrationChild(getChildAt(i3), getMeasuredWidth(), getMeasuredHeight());
        }
    }

    private void measureIllustrationChild(View view, int i, int i2) {
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        view.measure(getMaxSizeMeasureSpec(Math.min(this.mMaxWidth, i), getPaddingLeft() + getPaddingRight() + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin, ((ViewGroup.LayoutParams) marginLayoutParams).width), getMaxSizeMeasureSpec(Math.min(this.mMaxHeight, i2), getPaddingTop() + getPaddingBottom() + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin, ((ViewGroup.LayoutParams) marginLayoutParams).height));
    }

    private static int getMaxSizeMeasureSpec(int i, int i2, int i3) {
        int iMax = Math.max(0, i - i2);
        if (i3 >= 0) {
            return View.MeasureSpec.makeMeasureSpec(i3, 1073741824);
        }
        if (i3 == -1) {
            return View.MeasureSpec.makeMeasureSpec(iMax, 1073741824);
        }
        if (i3 == -2) {
            return View.MeasureSpec.makeMeasureSpec(iMax, Integer.MIN_VALUE);
        }
        return 0;
    }
}
