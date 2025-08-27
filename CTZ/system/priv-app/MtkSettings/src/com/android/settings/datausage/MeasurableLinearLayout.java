package com.android.settings.datausage;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/* loaded from: classes.dex */
public class MeasurableLinearLayout extends LinearLayout {
    private View mDisposableView;
    private View mFixedView;

    public MeasurableLinearLayout(Context context) {
        super(context, null);
    }

    public MeasurableLinearLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public MeasurableLinearLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i, 0);
    }

    public MeasurableLinearLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override // android.widget.LinearLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        if (this.mDisposableView != null && getMeasuredWidth() - this.mFixedView.getMeasuredWidth() < this.mDisposableView.getMeasuredWidth()) {
            this.mDisposableView.setVisibility(8);
            super.onMeasure(i, i2);
        } else if (this.mDisposableView != null && this.mDisposableView.getVisibility() != 0) {
            this.mDisposableView.setVisibility(0);
            super.onMeasure(i, i2);
        }
    }

    public void setChildren(View view, View view2) {
        this.mFixedView = view;
        this.mDisposableView = view2;
    }
}
