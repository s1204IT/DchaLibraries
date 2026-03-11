package com.android.launcher3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.android.launcher3.compat.PackageInstallerCompat;

public abstract class BaseRecyclerView extends RecyclerView implements RecyclerView.OnItemTouchListener {
    protected Rect mBackgroundPadding;
    private float mDeltaThreshold;
    private int mDownX;
    private int mDownY;
    int mDy;
    private int mLastY;
    protected BaseRecyclerViewFastScrollBar mScrollbar;

    public static class ScrollPositionState {
        public int itemPos;
        public int rowIndex;
        public int rowTopOffset;
    }

    protected abstract int getTop(int i);

    protected abstract void onUpdateScrollbar(int i);

    protected abstract String scrollToPositionAtProgress(float f);

    public BaseRecyclerView(Context context) {
        this(context, null);
    }

    public BaseRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mDy = 0;
        this.mBackgroundPadding = new Rect();
        this.mDeltaThreshold = getResources().getDisplayMetrics().density * 4.0f;
        this.mScrollbar = new BaseRecyclerViewFastScrollBar(this, getResources());
        ScrollListener listener = new ScrollListener();
        setOnScrollListener(listener);
    }

    private class ScrollListener extends RecyclerView.OnScrollListener {
        public ScrollListener() {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            BaseRecyclerView.this.mDy = dy;
            BaseRecyclerView.this.onUpdateScrollbar(dy);
        }
    }

    public void reset() {
        this.mScrollbar.reattachThumbToScroll();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addOnItemTouchListener(this);
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent ev) {
        handleTouchEvent(ev);
    }

    private boolean handleTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (action) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                this.mDownX = x;
                this.mLastY = y;
                this.mDownY = y;
                if (shouldStopScroll(ev)) {
                    stopScroll();
                }
                this.mScrollbar.handleTouchEvent(ev, this.mDownX, this.mDownY, this.mLastY);
                break;
            case PackageInstallerCompat.STATUS_INSTALLING:
            case 3:
                onFastScrollCompleted();
                this.mScrollbar.handleTouchEvent(ev, this.mDownX, this.mDownY, this.mLastY);
                break;
            case PackageInstallerCompat.STATUS_FAILED:
                this.mLastY = y;
                this.mScrollbar.handleTouchEvent(ev, this.mDownX, this.mDownY, this.mLastY);
                break;
        }
        return this.mScrollbar.isDraggingThumb();
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    protected boolean shouldStopScroll(MotionEvent ev) {
        return ev.getAction() == 0 && ((float) Math.abs(this.mDy)) < this.mDeltaThreshold && getScrollState() != 0;
    }

    public void updateBackgroundPadding(Rect padding) {
        this.mBackgroundPadding.set(padding);
    }

    public Rect getBackgroundPadding() {
        return this.mBackgroundPadding;
    }

    public int getMaxScrollbarWidth() {
        return this.mScrollbar.getThumbMaxWidth();
    }

    protected int getVisibleHeight() {
        int visibleHeight = (getHeight() - this.mBackgroundPadding.top) - this.mBackgroundPadding.bottom;
        return visibleHeight;
    }

    protected int getAvailableScrollHeight(int rowCount) {
        int totalHeight = getPaddingTop() + getTop(rowCount) + getPaddingBottom();
        int availableScrollHeight = totalHeight - getVisibleHeight();
        return availableScrollHeight;
    }

    protected int getAvailableScrollBarHeight() {
        int availableScrollBarHeight = getVisibleHeight() - this.mScrollbar.getThumbHeight();
        return availableScrollBarHeight;
    }

    public int getFastScrollerTrackColor(int defaultTrackColor) {
        return defaultTrackColor;
    }

    public int getFastScrollerThumbInactiveColor(int defaultInactiveThumbColor) {
        return defaultInactiveThumbColor;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        onUpdateScrollbar(0);
        this.mScrollbar.draw(canvas);
    }

    protected void synchronizeScrollBarThumbOffsetToViewScroll(ScrollPositionState scrollPosState, int rowCount) {
        int scrollBarX;
        int availableScrollBarHeight = getAvailableScrollBarHeight();
        int availableScrollHeight = getAvailableScrollHeight(rowCount);
        if (availableScrollHeight <= 0) {
            this.mScrollbar.setThumbOffset(-1, -1);
            return;
        }
        int scrollY = getScrollTop(scrollPosState);
        int scrollBarY = this.mBackgroundPadding.top + ((int) ((scrollY / availableScrollHeight) * availableScrollBarHeight));
        if (Utilities.isRtl(getResources())) {
            scrollBarX = this.mBackgroundPadding.left;
        } else {
            scrollBarX = (getWidth() - this.mBackgroundPadding.right) - this.mScrollbar.getThumbWidth();
        }
        this.mScrollbar.setThumbOffset(scrollBarX, scrollBarY);
    }

    protected boolean supportsFastScrolling() {
        return true;
    }

    protected int getScrollTop(ScrollPositionState scrollPosState) {
        return (getPaddingTop() + getTop(scrollPosState.rowIndex)) - scrollPosState.rowTopOffset;
    }

    protected void onFastScrollCompleted() {
    }
}
