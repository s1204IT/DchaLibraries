package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;

public class ExpandedGridView extends GridView {
    public ExpandedGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(65536, Integer.MIN_VALUE);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec2);
    }
}
