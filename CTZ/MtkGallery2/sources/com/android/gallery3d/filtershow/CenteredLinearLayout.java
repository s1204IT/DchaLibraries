package com.android.gallery3d.filtershow;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import com.android.gallery3d.R;

public class CenteredLinearLayout extends LinearLayout {
    private final int mMaxWidth;

    public CenteredLinearLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMaxWidth = getContext().obtainStyledAttributes(attributeSet, R.styleable.CenteredLinearLayout).getDimensionPixelSize(0, 0);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        View.MeasureSpec.getSize(i2);
        TypedValue.applyDimension(1, size, getContext().getResources().getDisplayMetrics());
        if (this.mMaxWidth > 0 && size > this.mMaxWidth) {
            i = View.MeasureSpec.makeMeasureSpec(this.mMaxWidth, View.MeasureSpec.getMode(i));
        }
        super.onMeasure(i, i2);
    }
}
