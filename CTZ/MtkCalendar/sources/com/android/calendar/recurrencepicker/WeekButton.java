package com.android.calendar.recurrencepicker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ToggleButton;

public class WeekButton extends ToggleButton {
    private static int mWidth;

    public WeekButton(Context context) {
        super(context);
    }

    public WeekButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public WeekButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public static void setSuggestedWidth(int i) {
        mWidth = i;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int measuredHeight = getMeasuredHeight();
        int measuredWidth = getMeasuredWidth();
        if (measuredHeight > 0 && measuredWidth > 0) {
            if (measuredWidth < measuredHeight) {
                if (View.MeasureSpec.getMode(getMeasuredHeightAndState()) != 1073741824) {
                    measuredHeight = measuredWidth;
                }
            } else if (measuredHeight < measuredWidth && View.MeasureSpec.getMode(getMeasuredWidthAndState()) != 1073741824) {
                measuredWidth = measuredHeight;
            }
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }
}
