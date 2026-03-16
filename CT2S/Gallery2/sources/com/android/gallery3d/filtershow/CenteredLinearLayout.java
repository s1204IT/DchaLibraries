package com.android.gallery3d.filtershow;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import com.android.gallery3d.R;

public class CenteredLinearLayout extends LinearLayout {
    private final int mMaxWidth;

    public CenteredLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CenteredLinearLayout);
        this.mMaxWidth = a.getDimensionPixelSize(0, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        View.MeasureSpec.getSize(heightMeasureSpec);
        Resources r = getContext().getResources();
        TypedValue.applyDimension(1, parentWidth, r.getDisplayMetrics());
        if (this.mMaxWidth > 0 && parentWidth > this.mMaxWidth) {
            int measureMode = View.MeasureSpec.getMode(widthMeasureSpec);
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mMaxWidth, measureMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
