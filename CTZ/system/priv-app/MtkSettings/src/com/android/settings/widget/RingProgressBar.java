package com.android.settings.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import com.android.settings.R;

/* loaded from: classes.dex */
public class RingProgressBar extends ProgressBar {
    public RingProgressBar(Context context) {
        this(context, null);
    }

    public RingProgressBar(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RingProgressBar(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R.style.RingProgressBarStyle);
    }

    public RingProgressBar(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override // android.widget.ProgressBar, android.view.View
    protected synchronized void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int iMin = Math.min(getMeasuredHeight(), getMeasuredWidth());
        setMeasuredDimension(iMin, iMin);
    }
}
