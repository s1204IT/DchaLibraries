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

    public StickyHeaderScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mStatusBarInset = 0;
    }

    public StickyHeaderScrollView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mStatusBarInset = 0;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
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
        if (Build.VERSION.SDK_INT >= 11 && this.mSticky != null) {
            View view = this.mStickyContainer != null ? this.mStickyContainer : this.mSticky;
            if ((view.getTop() - getScrollY()) + (this.mStickyContainer != null ? this.mSticky.getTop() : 0) < this.mStatusBarInset || !view.isShown()) {
                view.setTranslationY(getScrollY() - r1);
            } else {
                view.setTranslationY(0.0f);
            }
        }
    }

    @Override
    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        updateStickyHeaderPosition();
    }

    @Override
    @TargetApi(21)
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        if (getFitsSystemWindows()) {
            this.mStatusBarInset = windowInsets.getSystemWindowInsetTop();
            return windowInsets.replaceSystemWindowInsets(windowInsets.getSystemWindowInsetLeft(), 0, windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom());
        }
        return windowInsets;
    }
}
