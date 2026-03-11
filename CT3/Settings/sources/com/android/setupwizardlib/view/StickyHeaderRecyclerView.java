package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

public class StickyHeaderRecyclerView extends HeaderRecyclerView {
    private int mStatusBarInset;
    private View mSticky;
    private RectF mStickyRect;

    public StickyHeaderRecyclerView(Context context) {
        super(context);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
    }

    public StickyHeaderRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
    }

    public StickyHeaderRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View headerView;
        super.onLayout(changed, l, t, r, b);
        if (this.mSticky == null) {
            updateStickyView();
        }
        if (this.mSticky == null || (headerView = getHeader()) == null || headerView.getHeight() != 0) {
            return;
        }
        headerView.layout(0, -headerView.getMeasuredHeight(), headerView.getMeasuredWidth(), 0);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (this.mSticky == null) {
            return;
        }
        measureChild(getHeader(), widthSpec, heightSpec);
    }

    public void updateStickyView() {
        View header = getHeader();
        if (header == null) {
            return;
        }
        this.mSticky = header.findViewWithTag("sticky");
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.mSticky == null) {
            return;
        }
        View headerView = getHeader();
        int saveCount = canvas.save();
        View drawTarget = headerView != null ? headerView : this.mSticky;
        int drawOffset = headerView != null ? this.mSticky.getTop() : 0;
        int drawTop = drawTarget.getTop();
        if (drawTop + drawOffset < this.mStatusBarInset || !drawTarget.isShown()) {
            this.mStickyRect.set(0.0f, (-drawOffset) + this.mStatusBarInset, drawTarget.getWidth(), (drawTarget.getHeight() - drawOffset) + this.mStatusBarInset);
            canvas.translate(0.0f, this.mStickyRect.top);
            canvas.clipRect(0, 0, drawTarget.getWidth(), drawTarget.getHeight());
            drawTarget.draw(canvas);
        } else {
            this.mStickyRect.setEmpty();
        }
        canvas.restoreToCount(saveCount);
    }

    @Override
    @TargetApi(21)
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (getFitsSystemWindows()) {
            this.mStatusBarInset = insets.getSystemWindowInsetTop();
            insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
        }
        return insets;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (this.mStickyRect.contains(ev.getX(), ev.getY())) {
            ev.offsetLocation(-this.mStickyRect.left, -this.mStickyRect.top);
            return getHeader().dispatchTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }
}
