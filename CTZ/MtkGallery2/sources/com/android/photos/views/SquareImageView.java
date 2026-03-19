package com.android.photos.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class SquareImageView extends ImageView {
    public SquareImageView(Context context) {
        super(context);
    }

    public SquareImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public SquareImageView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int iMin;
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        if (mode == 1073741824 && mode2 != 1073741824) {
            int size = View.MeasureSpec.getSize(i);
            if (mode2 == Integer.MIN_VALUE) {
                iMin = Math.min(size, View.MeasureSpec.getSize(i2));
            } else {
                iMin = size;
            }
            setMeasuredDimension(size, iMin);
            return;
        }
        super.onMeasure(i, i2);
    }
}
