package com.android.internal.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class FaceUnlockView extends RelativeLayout {
    private static final String TAG = "FaceUnlockView";

    public FaceUnlockView(Context context) {
        this(context, null);
    }

    public FaceUnlockView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private int resolveMeasured(int measureSpec, int desired) {
        int specSize = View.MeasureSpec.getSize(measureSpec);
        switch (View.MeasureSpec.getMode(measureSpec)) {
            case Integer.MIN_VALUE:
                int result = Math.max(specSize, desired);
                return result;
            case 0:
                return desired;
            default:
                return specSize;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minimumWidth = getSuggestedMinimumWidth();
        int minimumHeight = getSuggestedMinimumHeight();
        int viewWidth = resolveMeasured(widthMeasureSpec, minimumWidth);
        int viewHeight = resolveMeasured(heightMeasureSpec, minimumHeight);
        int chosenSize = Math.min(viewWidth, viewHeight);
        int newWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(chosenSize, Integer.MIN_VALUE);
        int newHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(chosenSize, Integer.MIN_VALUE);
        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec);
    }
}
