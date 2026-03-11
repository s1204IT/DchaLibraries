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

    public ChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mOptimalWidth = -1;
        this.mOptimalWidthWeight = 0.0f;
        this.mContent = new Rect();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChartView, defStyle, 0);
        setOptimalWidth(a.getDimensionPixelSize(0, -1), a.getFloat(1, 0.0f));
        a.recycle();
        setClipToPadding(false);
        setClipChildren(false);
    }

    void init(ChartAxis horiz, ChartAxis vert) {
        this.mHoriz = (ChartAxis) Preconditions.checkNotNull(horiz, "missing horiz");
        this.mVert = (ChartAxis) Preconditions.checkNotNull(vert, "missing vert");
    }

    public void setOptimalWidth(int optimalWidth, float optimalWidthWeight) {
        this.mOptimalWidth = optimalWidth;
        this.mOptimalWidthWeight = optimalWidthWeight;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int slack = getMeasuredWidth() - this.mOptimalWidth;
        if (this.mOptimalWidth > 0 && slack > 0) {
            int targetWidth = (int) (this.mOptimalWidth + (slack * this.mOptimalWidthWeight));
            int widthMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(targetWidth, 1073741824);
            super.onMeasure(widthMeasureSpec2, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        this.mContent.set(getPaddingLeft(), getPaddingTop(), (r - l) - getPaddingRight(), (b - t) - getPaddingBottom());
        int width = this.mContent.width();
        int height = this.mContent.height();
        this.mHoriz.setSize(width);
        this.mVert.setSize(height);
        Rect parentRect = new Rect();
        Rect childRect = new Rect();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) child.getLayoutParams();
            parentRect.set(this.mContent);
            if (child instanceof ChartNetworkSeriesView) {
                Gravity.apply(params.gravity, width, height, parentRect, childRect);
                child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
            } else if (child instanceof ChartGridView) {
                Gravity.apply(params.gravity, width, height, parentRect, childRect);
                child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom + child.getPaddingBottom());
            } else if (child instanceof ChartSweepView) {
                layoutSweep((ChartSweepView) child, parentRect, childRect);
                child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
            }
        }
    }

    protected void layoutSweep(ChartSweepView sweep) {
        Rect parentRect = new Rect(this.mContent);
        Rect childRect = new Rect();
        layoutSweep(sweep, parentRect, childRect);
        sweep.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
    }

    protected void layoutSweep(ChartSweepView sweep, Rect parentRect, Rect childRect) {
        Rect sweepMargins = sweep.getMargins();
        if (sweep.getFollowAxis() == 1) {
            parentRect.top += sweepMargins.top + ((int) sweep.getPoint());
            parentRect.bottom = parentRect.top;
            parentRect.left += sweepMargins.left;
            parentRect.right += sweepMargins.right;
            Gravity.apply(8388659, parentRect.width(), sweep.getMeasuredHeight(), parentRect, childRect);
            return;
        }
        parentRect.left += sweepMargins.left + ((int) sweep.getPoint());
        parentRect.right = parentRect.left;
        parentRect.top += sweepMargins.top;
        parentRect.bottom += sweepMargins.bottom;
        Gravity.apply(8388659, sweep.getMeasuredWidth(), parentRect.height(), parentRect, childRect);
    }
}
