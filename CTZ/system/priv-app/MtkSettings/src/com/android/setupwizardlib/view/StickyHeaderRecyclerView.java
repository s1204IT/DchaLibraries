package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

/* loaded from: classes.dex */
public class StickyHeaderRecyclerView extends HeaderRecyclerView {
    private int mStatusBarInset;
    private View mSticky;
    private RectF mStickyRect;

    public StickyHeaderRecyclerView(Context context) {
        super(context);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
    }

    public StickyHeaderRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
    }

    public StickyHeaderRecyclerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
    }

    @Override // android.support.v7.widget.RecyclerView, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        View header;
        super.onLayout(z, i, i2, i3, i4);
        if (this.mSticky == null) {
            updateStickyView();
        }
        if (this.mSticky != null && (header = getHeader()) != null && header.getHeight() == 0) {
            header.layout(0, -header.getMeasuredHeight(), header.getMeasuredWidth(), 0);
        }
    }

    @Override // android.support.v7.widget.RecyclerView, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        if (this.mSticky != null) {
            measureChild(getHeader(), i, i2);
        }
    }

    public void updateStickyView() {
        View header = getHeader();
        if (header != null) {
            this.mSticky = header.findViewWithTag("sticky");
        }
    }

    @Override // android.support.v7.widget.RecyclerView, android.view.View
    public void draw(Canvas canvas) {
        View view;
        super.draw(canvas);
        if (this.mSticky != null) {
            View header = getHeader();
            int iSave = canvas.save();
            if (header == null) {
                view = this.mSticky;
            } else {
                view = header;
            }
            if (view.getTop() + (header != null ? this.mSticky.getTop() : 0) < this.mStatusBarInset || !view.isShown()) {
                this.mStickyRect.set(0.0f, (-r0) + this.mStatusBarInset, view.getWidth(), (view.getHeight() - r0) + this.mStatusBarInset);
                canvas.translate(0.0f, this.mStickyRect.top);
                canvas.clipRect(0, 0, view.getWidth(), view.getHeight());
                view.draw(canvas);
            } else {
                this.mStickyRect.setEmpty();
            }
            canvas.restoreToCount(iSave);
        }
    }

    @Override // android.view.View
    @TargetApi(21)
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        if (getFitsSystemWindows()) {
            this.mStatusBarInset = windowInsets.getSystemWindowInsetTop();
            windowInsets.replaceSystemWindowInsets(windowInsets.getSystemWindowInsetLeft(), 0, windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom());
        }
        return windowInsets;
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (this.mStickyRect.contains(motionEvent.getX(), motionEvent.getY())) {
            motionEvent.offsetLocation(-this.mStickyRect.left, -this.mStickyRect.top);
            return getHeader().dispatchTouchEvent(motionEvent);
        }
        return super.dispatchTouchEvent(motionEvent);
    }
}
