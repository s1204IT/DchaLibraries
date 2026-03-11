package com.android.launcher3;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.launcher3.views.RecyclerViewFastScroller;

public abstract class BaseRecyclerView extends RecyclerView {
    protected RecyclerViewFastScroller mScrollbar;

    protected abstract int getAvailableScrollHeight();

    public abstract int getCurrentScrollY();

    public abstract void onUpdateScrollbar(int i);

    public abstract String scrollToPositionAtProgress(float f);

    public BaseRecyclerView(Context context) {
        this(context, null);
    }

    public BaseRecyclerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public BaseRecyclerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        bindFastScrollbar();
    }

    public void bindFastScrollbar() {
        ViewGroup viewGroup = (ViewGroup) getParent().getParent();
        this.mScrollbar = (RecyclerViewFastScroller) viewGroup.findViewById(R.id.fast_scroller);
        this.mScrollbar.setRecyclerView(this, (TextView) viewGroup.findViewById(R.id.fast_scroller_popup));
        onUpdateScrollbar(0);
    }

    public RecyclerViewFastScroller getScrollbar() {
        return this.mScrollbar;
    }

    public int getScrollBarTop() {
        return getPaddingTop();
    }

    public int getScrollbarTrackHeight() {
        return (this.mScrollbar.getHeight() - getScrollBarTop()) - getPaddingBottom();
    }

    protected int getAvailableScrollBarHeight() {
        return getScrollbarTrackHeight() - this.mScrollbar.getThumbHeight();
    }

    protected void synchronizeScrollBarThumbOffsetToViewScroll(int i, int i2) {
        if (i2 <= 0) {
            this.mScrollbar.setThumbOffsetY(-1);
        } else {
            this.mScrollbar.setThumbOffsetY((int) ((i / i2) * getAvailableScrollBarHeight()));
        }
    }

    public boolean shouldContainerScroll(MotionEvent motionEvent, View view) {
        int[] iArr = {(int) motionEvent.getX(), (int) motionEvent.getY()};
        Utilities.mapCoordInSelfToDescendant(this.mScrollbar, view, iArr);
        return !this.mScrollbar.shouldBlockIntercept(iArr[0], iArr[1]) && getCurrentScrollY() == 0;
    }

    public boolean supportsFastScrolling() {
        return true;
    }

    public void onFastScrollCompleted() {
    }
}
