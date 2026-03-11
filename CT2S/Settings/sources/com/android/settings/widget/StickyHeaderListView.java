package com.android.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ListView;

public class StickyHeaderListView extends ListView {
    private boolean mDrawScrollBar;
    private int mStatusBarInset;
    private View mSticky;
    private View mStickyContainer;
    private RectF mStickyRect;

    public StickyHeaderListView(Context context) {
        super(context);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
    }

    public StickyHeaderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
    }

    public StickyHeaderListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
    }

    public StickyHeaderListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (this.mSticky == null) {
            updateStickyView();
        }
    }

    public void updateStickyView() {
        this.mSticky = findViewWithTag("sticky");
        this.mStickyContainer = findViewWithTag("stickyContainer");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!this.mStickyRect.contains(ev.getX(), ev.getY())) {
            return super.dispatchTouchEvent(ev);
        }
        ev.offsetLocation(-this.mStickyRect.left, -this.mStickyRect.top);
        return this.mStickyContainer.dispatchTouchEvent(ev);
    }

    @Override
    public void draw(Canvas canvas) {
        this.mDrawScrollBar = false;
        super.draw(canvas);
        if (this.mSticky != null) {
            int saveCount = canvas.save();
            View drawTarget = this.mStickyContainer != null ? this.mStickyContainer : this.mSticky;
            int drawOffset = this.mStickyContainer != null ? this.mSticky.getTop() : 0;
            int drawTop = drawTarget.getTop();
            if (drawTop + drawOffset < this.mStatusBarInset || !drawTarget.isShown()) {
                canvas.translate(0.0f, (-drawOffset) + this.mStatusBarInset);
                canvas.clipRect(0, 0, drawTarget.getWidth(), drawTarget.getHeight());
                drawTarget.draw(canvas);
                this.mStickyRect.set(0.0f, (-drawOffset) + this.mStatusBarInset, drawTarget.getWidth(), (drawTarget.getHeight() - drawOffset) + this.mStatusBarInset);
            } else {
                this.mStickyRect.setEmpty();
            }
            canvas.restoreToCount(saveCount);
        }
        this.mDrawScrollBar = true;
        onDrawScrollBars(canvas);
    }

    protected boolean isVerticalScrollBarHidden() {
        return super.isVerticalScrollBarHidden() || !this.mDrawScrollBar;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (getFitsSystemWindows()) {
            this.mStatusBarInset = insets.getSystemWindowInsetTop();
            insets.consumeSystemWindowInsets(false, true, false, false);
        }
        return insets;
    }
}
