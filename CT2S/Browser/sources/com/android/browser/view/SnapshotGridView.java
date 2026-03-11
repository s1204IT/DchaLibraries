package com.android.browser.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;

public class SnapshotGridView extends GridView {
    private int mColWidth;

    public SnapshotGridView(Context context) {
        super(context);
    }

    public SnapshotGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SnapshotGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        if (widthSize > 0 && this.mColWidth > 0) {
            int numCols = widthSize / this.mColWidth;
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.min(Math.min(numCols, 5) * this.mColWidth, widthSize), widthMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setColumnWidth(int columnWidth) {
        this.mColWidth = columnWidth;
        super.setColumnWidth(columnWidth);
    }
}
