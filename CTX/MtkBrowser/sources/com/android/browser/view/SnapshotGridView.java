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

    public SnapshotGridView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public SnapshotGridView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int iMakeMeasureSpec = (size <= 0 || this.mColWidth <= 0) ? i : View.MeasureSpec.makeMeasureSpec(Math.min(Math.min(size / this.mColWidth, 6) * this.mColWidth, size), View.MeasureSpec.getMode(i));
        if (i != iMakeMeasureSpec) {
            setPaddingRelative((i - iMakeMeasureSpec) / 2, 0, 0, 0);
        }
        super.onMeasure(i, i2);
    }

    @Override
    public void setColumnWidth(int i) {
        this.mColWidth = i;
        super.setColumnWidth(i);
    }
}
