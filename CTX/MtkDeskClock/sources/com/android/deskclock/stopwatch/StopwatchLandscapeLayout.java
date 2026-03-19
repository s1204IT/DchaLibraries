package com.android.deskclock.stopwatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.deskclock.R;

public class StopwatchLandscapeLayout extends ViewGroup {
    private View mLapsListView;
    private View mStopwatchView;

    public StopwatchLandscapeLayout(Context context) {
        super(context);
    }

    public StopwatchLandscapeLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public StopwatchLandscapeLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLapsListView = findViewById(R.id.laps_list);
        this.mStopwatchView = findViewById(R.id.stopwatch_time_wrapper);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i2);
        int size2 = View.MeasureSpec.getSize(i);
        int i3 = size2 / 2;
        int iMax = 0;
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(size2, 0);
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(size, Integer.MIN_VALUE);
        if (this.mLapsListView != null && this.mLapsListView.getVisibility() != 8) {
            this.mLapsListView.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
            iMax = Math.max(this.mLapsListView.getMeasuredWidth(), i3);
            this.mLapsListView.measure(View.MeasureSpec.makeMeasureSpec(iMax, 1073741824), iMakeMeasureSpec2);
        }
        this.mStopwatchView.measure(View.MeasureSpec.makeMeasureSpec(size2 - iMax, 1073741824), iMakeMeasureSpec2);
        setMeasuredDimension(i, i2);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        int i8;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int width = getWidth() - getPaddingRight();
        int i9 = width - paddingLeft;
        int height = ((getHeight() - getPaddingBottom()) - paddingTop) / 2;
        int measuredWidth = 0;
        boolean z2 = getLayoutDirection() == 0;
        if (this.mLapsListView != null && this.mLapsListView.getVisibility() != 8) {
            measuredWidth = this.mLapsListView.getMeasuredWidth();
            int measuredHeight = this.mLapsListView.getMeasuredHeight();
            int i10 = (paddingTop + height) - (measuredHeight / 2);
            int i11 = measuredHeight + i10;
            if (z2) {
                i8 = width - measuredWidth;
                i7 = width;
            } else {
                i7 = paddingLeft + measuredWidth;
                i8 = paddingLeft;
            }
            this.mLapsListView.layout(i8, i10, i7, i11);
        }
        int measuredWidth2 = this.mStopwatchView.getMeasuredWidth();
        int measuredHeight2 = this.mStopwatchView.getMeasuredHeight();
        int i12 = (paddingTop + height) - (measuredHeight2 / 2);
        int i13 = measuredHeight2 + i12;
        if (z2) {
            i6 = paddingLeft + (((i9 - measuredWidth) - measuredWidth2) / 2);
            i5 = measuredWidth2 + i6;
        } else {
            int i14 = width - (((i9 - measuredWidth) - measuredWidth2) / 2);
            int i15 = i14 - measuredWidth2;
            i5 = i14;
            i6 = i15;
        }
        this.mStopwatchView.layout(i6, i12, i5, i13);
    }
}
