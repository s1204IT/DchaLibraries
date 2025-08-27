package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.widget.FrameLayout;
import com.android.internal.util.Preconditions;
import com.android.settings.R;

/* loaded from: classes.dex */
public class ChartView extends FrameLayout {
    private Rect mContent;
    ChartAxis mHoriz;

    @ViewDebug.ExportedProperty
    private int mOptimalWidth;
    private float mOptimalWidthWeight;
    ChartAxis mVert;

    public ChartView(Context context) {
        this(context, null, 0);
    }

    public ChartView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ChartView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mOptimalWidth = -1;
        this.mOptimalWidthWeight = 0.0f;
        this.mContent = new Rect();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ChartView, i, 0);
        setOptimalWidth(typedArrayObtainStyledAttributes.getDimensionPixelSize(0, -1), typedArrayObtainStyledAttributes.getFloat(1, 0.0f));
        typedArrayObtainStyledAttributes.recycle();
        setClipToPadding(false);
        setClipChildren(false);
    }

    void init(ChartAxis chartAxis, ChartAxis chartAxis2) {
        this.mHoriz = (ChartAxis) Preconditions.checkNotNull(chartAxis, "missing horiz");
        this.mVert = (ChartAxis) Preconditions.checkNotNull(chartAxis2, "missing vert");
    }

    public void setOptimalWidth(int i, float f) {
        this.mOptimalWidth = i;
        this.mOptimalWidthWeight = f;
        requestLayout();
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int measuredWidth = getMeasuredWidth() - this.mOptimalWidth;
        if (this.mOptimalWidth > 0 && measuredWidth > 0) {
            super.onMeasure(View.MeasureSpec.makeMeasureSpec((int) (this.mOptimalWidth + (measuredWidth * this.mOptimalWidthWeight)), 1073741824), i2);
        }
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mContent.set(getPaddingLeft(), getPaddingTop(), (i3 - i) - getPaddingRight(), (i4 - i2) - getPaddingBottom());
        int iWidth = this.mContent.width();
        int iHeight = this.mContent.height();
        this.mHoriz.setSize(iWidth);
        this.mVert.setSize(iHeight);
        Rect rect = new Rect();
        Rect rect2 = new Rect();
        for (int i5 = 0; i5 < getChildCount(); i5++) {
            View childAt = getChildAt(i5);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) childAt.getLayoutParams();
            rect.set(this.mContent);
            if (childAt instanceof ChartNetworkSeriesView) {
                Gravity.apply(layoutParams.gravity, iWidth, iHeight, rect, rect2);
                childAt.layout(rect2.left, rect2.top, rect2.right, rect2.bottom);
            } else if (childAt instanceof ChartGridView) {
                Gravity.apply(layoutParams.gravity, iWidth, iHeight, rect, rect2);
                childAt.layout(rect2.left, rect2.top, rect2.right, rect2.bottom + childAt.getPaddingBottom());
            } else if (childAt instanceof ChartSweepView) {
                layoutSweep((ChartSweepView) childAt, rect, rect2);
                childAt.layout(rect2.left, rect2.top, rect2.right, rect2.bottom);
            }
        }
    }

    protected void layoutSweep(ChartSweepView chartSweepView) {
        Rect rect = new Rect(this.mContent);
        Rect rect2 = new Rect();
        layoutSweep(chartSweepView, rect, rect2);
        chartSweepView.layout(rect2.left, rect2.top, rect2.right, rect2.bottom);
    }

    protected void layoutSweep(ChartSweepView chartSweepView, Rect rect, Rect rect2) {
        Rect margins = chartSweepView.getMargins();
        if (chartSweepView.getFollowAxis() == 1) {
            rect.top += margins.top + ((int) chartSweepView.getPoint());
            rect.bottom = rect.top;
            rect.left += margins.left;
            rect.right += margins.right;
            Gravity.apply(8388659, rect.width(), chartSweepView.getMeasuredHeight(), rect, rect2);
            return;
        }
        rect.left += margins.left + ((int) chartSweepView.getPoint());
        rect.right = rect.left;
        rect.top += margins.top;
        rect.bottom += margins.bottom;
        Gravity.apply(8388659, chartSweepView.getMeasuredWidth(), rect.height(), rect, rect2);
    }
}
