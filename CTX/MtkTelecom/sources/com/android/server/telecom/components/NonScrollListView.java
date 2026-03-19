package com.android.server.telecom.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

public class NonScrollListView extends ListView {
    public NonScrollListView(Context context) {
        super(context);
    }

    public NonScrollListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public NonScrollListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, View.MeasureSpec.makeMeasureSpec(536870911, Integer.MIN_VALUE));
        getLayoutParams().height = getMeasuredHeight();
    }
}
