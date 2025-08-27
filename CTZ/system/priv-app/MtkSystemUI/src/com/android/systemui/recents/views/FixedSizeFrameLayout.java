package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/* loaded from: classes.dex */
public class FixedSizeFrameLayout extends FrameLayout {
    private final Rect mLayoutBounds;

    public FixedSizeFrameLayout(Context context) {
        super(context);
        this.mLayoutBounds = new Rect();
    }

    public FixedSizeFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLayoutBounds = new Rect();
    }

    public FixedSizeFrameLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mLayoutBounds = new Rect();
    }

    public FixedSizeFrameLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mLayoutBounds = new Rect();
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected final void onMeasure(int i, int i2) {
        measureContents(View.MeasureSpec.getSize(i), View.MeasureSpec.getSize(i2));
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected final void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mLayoutBounds.set(i, i2, i3, i4);
        layoutContents(this.mLayoutBounds, z);
    }

    @Override // android.view.View, android.view.ViewParent
    public final void requestLayout() {
        if (this.mLayoutBounds == null || this.mLayoutBounds.isEmpty()) {
            super.requestLayout();
        } else {
            measureContents(getMeasuredWidth(), getMeasuredHeight());
            layoutContents(this.mLayoutBounds, false);
        }
    }

    protected void measureContents(int i, int i2) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(i, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(i2, Integer.MIN_VALUE));
    }

    protected void layoutContents(Rect rect, boolean z) {
        super.onLayout(z, rect.left, rect.top, rect.right, rect.bottom);
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        onSizeChanged(measuredWidth, measuredHeight, measuredWidth, measuredHeight);
    }
}
