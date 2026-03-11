package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;

public class StickyHeaderScrollView extends BottomScrollView {
    private int mStatusBarInset;
    private View mSticky;
    private View mStickyContainer;

    public StickyHeaderScrollView(Context context) {
        super(context);
        this.mStatusBarInset = 0;
    }

    public StickyHeaderScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mStatusBarInset = 0;
    }

    public StickyHeaderScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mStatusBarInset = 0;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (this.mSticky == null) {
            updateStickyView();
        }
        updateStickyHeaderPosition();
    }

    public void updateStickyView() {
        this.mSticky = findViewWithTag("sticky");
        this.mStickyContainer = findViewWithTag("stickyContainer");
    }

    private void updateStickyHeaderPosition() {
        if (Build.VERSION.SDK_INT < 11 || this.mSticky == null) {
            return;
        }
        View drawTarget = this.mStickyContainer != null ? this.mStickyContainer : this.mSticky;
        int drawOffset = this.mStickyContainer != null ? this.mSticky.getTop() : 0;
        int drawTop = drawTarget.getTop() - getScrollY();
        if (drawTop + drawOffset < this.mStatusBarInset || !drawTarget.isShown()) {
            drawTarget.setTranslationY(getScrollY() - drawOffset);
        } else {
            drawTarget.setTranslationY(0.0f);
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        updateStickyHeaderPosition();
    }

    @Override
    @TargetApi(21)
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (getFitsSystemWindows()) {
            this.mStatusBarInset = insets.getSystemWindowInsetTop();
            return insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
        }
        return insets;
    }
}
