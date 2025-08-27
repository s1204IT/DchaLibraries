package com.android.launcher3.widget;

import android.content.Context;
import android.graphics.Point;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.android.launcher3.BaseRecyclerView;
import com.android.launcher3.R;

/* loaded from: classes.dex */
public class WidgetsRecyclerView extends BaseRecyclerView implements RecyclerView.OnItemTouchListener {
    private WidgetsListAdapter mAdapter;
    private final Point mFastScrollerOffset;
    private final int mScrollbarTop;
    private boolean mTouchDownOnScroller;

    public WidgetsRecyclerView(Context context) {
        this(context, null);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mFastScrollerOffset = new Point();
        this.mScrollbarTop = getResources().getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin);
        addOnItemTouchListener(this);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attributeSet, int i, int i2) {
        this(context, attributeSet, i);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override // android.support.v7.widget.RecyclerView
    public void setAdapter(RecyclerView.Adapter adapter) {
        super.setAdapter(adapter);
        this.mAdapter = (WidgetsListAdapter) adapter;
    }

    @Override // com.android.launcher3.BaseRecyclerView
    public String scrollToPositionAtProgress(float f) {
        if (isModelNotReady()) {
            return "";
        }
        stopScroll();
        float itemCount = this.mAdapter.getItemCount() * f;
        ((LinearLayoutManager) getLayoutManager()).scrollToPositionWithOffset(0, (int) (-(getAvailableScrollHeight() * f)));
        if (f == 1.0f) {
            itemCount -= 1.0f;
        }
        return this.mAdapter.getSectionName((int) itemCount);
    }

    @Override // com.android.launcher3.BaseRecyclerView
    public void onUpdateScrollbar(int i) {
        if (isModelNotReady()) {
            return;
        }
        int currentScrollY = getCurrentScrollY();
        if (currentScrollY < 0) {
            this.mScrollbar.setThumbOffsetY(-1);
        } else {
            synchronizeScrollBarThumbOffsetToViewScroll(currentScrollY, getAvailableScrollHeight());
        }
    }

    @Override // com.android.launcher3.BaseRecyclerView
    public int getCurrentScrollY() {
        if (isModelNotReady() || getChildCount() == 0) {
            return -1;
        }
        View childAt = getChildAt(0);
        int measuredHeight = childAt.getMeasuredHeight() * getChildPosition(childAt);
        return (getPaddingTop() + measuredHeight) - getLayoutManager().getDecoratedTop(childAt);
    }

    @Override // com.android.launcher3.BaseRecyclerView
    protected int getAvailableScrollHeight() {
        return ((getChildAt(0).getMeasuredHeight() * this.mAdapter.getItemCount()) - getScrollbarTrackHeight()) - this.mScrollbarTop;
    }

    private boolean isModelNotReady() {
        return this.mAdapter.getItemCount() == 0;
    }

    @Override // com.android.launcher3.BaseRecyclerView
    public int getScrollBarTop() {
        return this.mScrollbarTop;
    }

    @Override // android.support.v7.widget.RecyclerView.OnItemTouchListener
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mTouchDownOnScroller = this.mScrollbar.isHitInParent(motionEvent.getX(), motionEvent.getY(), this.mFastScrollerOffset);
        }
        if (this.mTouchDownOnScroller) {
            return this.mScrollbar.handleTouchEvent(motionEvent, this.mFastScrollerOffset);
        }
        return false;
    }

    @Override // android.support.v7.widget.RecyclerView.OnItemTouchListener
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (this.mTouchDownOnScroller) {
            this.mScrollbar.handleTouchEvent(motionEvent, this.mFastScrollerOffset);
        }
    }

    @Override // android.support.v7.widget.RecyclerView.OnItemTouchListener
    public void onRequestDisallowInterceptTouchEvent(boolean z) {
    }
}
