package com.android.deskclock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class TimerCircleFrameLayout extends FrameLayout {
    public TimerCircleFrameLayout(Context context) {
        super(context);
    }

    public TimerCircleFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public TimerCircleFrameLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int iMin = Math.min(Math.min((View.MeasureSpec.getSize(i) - paddingLeft) - paddingRight, (View.MeasureSpec.getSize(i2) - paddingTop) - paddingBottom), getResources().getDimensionPixelSize(R.dimen.max_timer_circle_size));
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(paddingLeft + iMin + paddingRight, 1073741824), View.MeasureSpec.makeMeasureSpec(iMin + paddingTop + paddingBottom, 1073741824));
    }
}
