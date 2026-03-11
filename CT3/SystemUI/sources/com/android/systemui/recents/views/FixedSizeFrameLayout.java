package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class FixedSizeFrameLayout extends FrameLayout {
    private final Rect mLayoutBounds;

    public FixedSizeFrameLayout(Context context) {
        super(context);
        this.mLayoutBounds = new Rect();
    }

    public FixedSizeFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLayoutBounds = new Rect();
    }

    public FixedSizeFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mLayoutBounds = new Rect();
    }

    public FixedSizeFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mLayoutBounds = new Rect();
    }

    @Override
    protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureContents(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected final void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mLayoutBounds.set(left, top, right, bottom);
        layoutContents(this.mLayoutBounds, changed);
    }

    @Override
    public final void requestLayout() {
        if (this.mLayoutBounds == null || this.mLayoutBounds.isEmpty()) {
            super.requestLayout();
        } else {
            measureContents(getMeasuredWidth(), getMeasuredHeight());
            layoutContents(this.mLayoutBounds, false);
        }
    }

    protected void measureContents(int width, int height) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(width, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(height, Integer.MIN_VALUE));
    }

    protected void layoutContents(Rect bounds, boolean changed) {
        super.onLayout(changed, bounds.left, bounds.top, bounds.right, bounds.bottom);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        onSizeChanged(width, height, width, height);
    }
}
