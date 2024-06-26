package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ListView;
import com.android.setupwizardlib.R;
/* loaded from: classes.dex */
public class StickyHeaderListView extends ListView {
    private int mStatusBarInset;
    private View mSticky;
    private View mStickyContainer;
    private RectF mStickyRect;

    public StickyHeaderListView(Context context) {
        super(context);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
        init(null, 16842868);
    }

    public StickyHeaderListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
        init(attributeSet, 16842868);
    }

    public StickyHeaderListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mStatusBarInset = 0;
        this.mStickyRect = new RectF();
        init(attributeSet, i);
    }

    private void init(AttributeSet attributeSet, int i) {
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.SuwStickyHeaderListView, i, 0);
        int resourceId = obtainStyledAttributes.getResourceId(R.styleable.SuwStickyHeaderListView_suwHeader, 0);
        if (resourceId != 0) {
            addHeaderView(LayoutInflater.from(getContext()).inflate(resourceId, (ViewGroup) this, false), null, false);
        }
        obtainStyledAttributes.recycle();
    }

    @Override // android.widget.AbsListView, android.widget.AdapterView, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mSticky == null) {
            updateStickyView();
        }
    }

    public void updateStickyView() {
        this.mSticky = findViewWithTag("sticky");
        this.mStickyContainer = findViewWithTag("stickyContainer");
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (this.mStickyRect.contains(motionEvent.getX(), motionEvent.getY())) {
            motionEvent.offsetLocation(-this.mStickyRect.left, -this.mStickyRect.top);
            return this.mStickyContainer.dispatchTouchEvent(motionEvent);
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override // android.widget.AbsListView, android.view.View
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.mSticky != null) {
            int save = canvas.save();
            View view = this.mStickyContainer != null ? this.mStickyContainer : this.mSticky;
            int top = this.mStickyContainer != null ? this.mSticky.getTop() : 0;
            if (view.getTop() + top < this.mStatusBarInset || !view.isShown()) {
                this.mStickyRect.set(0.0f, (-top) + this.mStatusBarInset, view.getWidth(), (view.getHeight() - top) + this.mStatusBarInset);
                canvas.translate(0.0f, this.mStickyRect.top);
                canvas.clipRect(0, 0, view.getWidth(), view.getHeight());
                view.draw(canvas);
            } else {
                this.mStickyRect.setEmpty();
            }
            canvas.restoreToCount(save);
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

    @Override // android.view.View
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        int i = this.mSticky != null ? 1 : 0;
        accessibilityEvent.setItemCount(accessibilityEvent.getItemCount() - i);
        accessibilityEvent.setFromIndex(Math.max(accessibilityEvent.getFromIndex() - i, 0));
        if (Build.VERSION.SDK_INT >= 14) {
            accessibilityEvent.setToIndex(Math.max(accessibilityEvent.getToIndex() - i, 0));
        }
    }
}
