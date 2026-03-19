package com.android.internal.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

@RemoteViews.RemoteView
public class RemeasuringLinearLayout extends LinearLayout {
    public RemeasuringLinearLayout(Context context) {
        super(context);
    }

    public RemeasuringLinearLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public RemeasuringLinearLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public RemeasuringLinearLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int childCount = getChildCount();
        int iMax = 0;
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt != null && childAt.getVisibility() != 8) {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) childAt.getLayoutParams();
                iMax = Math.max(iMax, childAt.getMeasuredHeight() + iMax + layoutParams.topMargin + layoutParams.bottomMargin);
            }
        }
        setMeasuredDimension(getMeasuredWidth(), iMax);
    }
}
