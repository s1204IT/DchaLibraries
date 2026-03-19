package com.android.documentsui.selection;

import android.graphics.Point;
import android.os.Build;
import android.support.v4.util.Preconditions;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.android.documentsui.selection.ViewAutoScroller;

public final class GestureSelectionHelper extends ViewAutoScroller.ScrollHost implements RecyclerView.OnItemTouchListener {
    private final ItemDetailsLookup mItemLookup;
    private Point mLastInterceptedPoint;
    private final ContentLock mLock;
    private final Runnable mScroller;
    private final SelectionHelper mSelectionMgr;
    private final ViewDelegate mView;
    private int mLastTouchedItemPosition = -1;
    private boolean mStarted = false;

    GestureSelectionHelper(SelectionHelper selectionHelper, ViewDelegate viewDelegate, ContentLock contentLock, ItemDetailsLookup itemDetailsLookup) {
        boolean z;
        boolean z2;
        boolean z3;
        if (selectionHelper == null) {
            z = false;
        } else {
            z = true;
        }
        Preconditions.checkArgument(z);
        if (viewDelegate == null) {
            z2 = false;
        } else {
            z2 = true;
        }
        Preconditions.checkArgument(z2);
        if (contentLock == null) {
            z3 = false;
        } else {
            z3 = true;
        }
        Preconditions.checkArgument(z3);
        Preconditions.checkArgument(itemDetailsLookup != null);
        this.mSelectionMgr = selectionHelper;
        this.mView = viewDelegate;
        this.mLock = contentLock;
        this.mItemLookup = itemDetailsLookup;
        this.mScroller = new ViewAutoScroller(this, this.mView);
    }

    public void start() {
        Preconditions.checkState(!this.mStarted);
        if (this.mLastTouchedItemPosition < 0) {
            Log.w("GestureSelectionHelper", "Illegal state. Can't start without valid mLastStartedItemPos.");
            return;
        }
        Preconditions.checkState(this.mSelectionMgr.isRangeActive());
        this.mLock.checkUnlocked();
        this.mStarted = true;
        this.mLock.block();
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        MotionEvents.isMouseEvent(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            return handleInterceptedDownEvent(motionEvent);
        }
        if (actionMasked == 2) {
            return this.mStarted;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (!this.mSelectionMgr.isRangeActive()) {
        }
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(this.mStarted);
        }
        switch (motionEvent.getActionMasked()) {
            case 1:
                handleUpEvent(motionEvent);
                break;
            case 2:
                handleMoveEvent(motionEvent);
                break;
            case 3:
                handleCancelEvent(motionEvent);
                break;
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean z) {
    }

    private boolean handleInterceptedDownEvent(MotionEvent motionEvent) {
        if (this.mItemLookup.getItemDetails(motionEvent) == null) {
            return false;
        }
        this.mLastTouchedItemPosition = this.mView.getItemUnder(motionEvent);
        return this.mLastTouchedItemPosition != -1;
    }

    private void handleUpEvent(MotionEvent motionEvent) {
        this.mSelectionMgr.mergeProvisionalSelection();
        endSelection();
        if (this.mLastTouchedItemPosition > -1) {
            this.mSelectionMgr.startRange(this.mLastTouchedItemPosition);
        }
    }

    private void handleCancelEvent(MotionEvent motionEvent) {
        this.mSelectionMgr.clearProvisionalSelection();
        endSelection();
    }

    private void endSelection() {
        Preconditions.checkState(this.mStarted);
        this.mLastTouchedItemPosition = -1;
        this.mStarted = false;
        this.mLock.unblock();
    }

    private void handleMoveEvent(MotionEvent motionEvent) {
        this.mLastInterceptedPoint = MotionEvents.getOrigin(motionEvent);
        int lastGlidedItemPosition = this.mView.getLastGlidedItemPosition(motionEvent);
        if (lastGlidedItemPosition != -1) {
            doGestureMultiSelect(lastGlidedItemPosition);
        }
        scrollIfNecessary();
    }

    private static float getInboundY(float f, float f2) {
        if (f2 < 0.0f) {
            return 0.0f;
        }
        if (f2 > f) {
            return f;
        }
        return f2;
    }

    private void doGestureMultiSelect(int i) {
        this.mSelectionMgr.extendProvisionalRange(i);
    }

    private void scrollIfNecessary() {
        this.mScroller.run();
    }

    @Override
    public Point getCurrentPosition() {
        return this.mLastInterceptedPoint;
    }

    @Override
    public int getViewHeight() {
        return this.mView.getHeight();
    }

    @Override
    public boolean isActive() {
        return this.mStarted && this.mSelectionMgr.hasSelection();
    }

    public static GestureSelectionHelper create(SelectionHelper selectionHelper, RecyclerView recyclerView, ContentLock contentLock, ItemDetailsLookup itemDetailsLookup) {
        return new GestureSelectionHelper(selectionHelper, new RecyclerViewDelegate(recyclerView), contentLock, itemDetailsLookup);
    }

    static abstract class ViewDelegate extends ViewAutoScroller.ScrollerCallbacks {
        abstract int getHeight();

        abstract int getItemUnder(MotionEvent motionEvent);

        abstract int getLastGlidedItemPosition(MotionEvent motionEvent);

        ViewDelegate() {
        }
    }

    static final class RecyclerViewDelegate extends ViewDelegate {
        private final RecyclerView mView;

        RecyclerViewDelegate(RecyclerView recyclerView) {
            Preconditions.checkArgument(recyclerView != null);
            this.mView = recyclerView;
        }

        @Override
        int getHeight() {
            return this.mView.getHeight();
        }

        @Override
        int getItemUnder(MotionEvent motionEvent) {
            View viewFindChildViewUnder = this.mView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
            if (viewFindChildViewUnder != null) {
                return this.mView.getChildAdapterPosition(viewFindChildViewUnder);
            }
            return -1;
        }

        @Override
        int getLastGlidedItemPosition(MotionEvent motionEvent) {
            View childAt = this.mView.getLayoutManager().getChildAt(this.mView.getLayoutManager().getChildCount() - 1);
            return isPastLastItem(childAt.getTop(), childAt.getLeft(), childAt.getRight(), motionEvent, this.mView.getContext().getResources().getConfiguration().getLayoutDirection()) ? this.mView.getAdapter().getItemCount() - 1 : this.mView.getChildAdapterPosition(this.mView.findChildViewUnder(motionEvent.getX(), GestureSelectionHelper.getInboundY(this.mView.getHeight(), motionEvent.getY())));
        }

        static boolean isPastLastItem(int i, int i2, int i3, MotionEvent motionEvent, int i4) {
            return i4 == 0 ? motionEvent.getX() > ((float) i3) && motionEvent.getY() > ((float) i) : motionEvent.getX() < ((float) i2) && motionEvent.getY() > ((float) i);
        }

        @Override
        public void scrollBy(int i) {
            this.mView.scrollBy(0, i);
        }

        @Override
        public void runAtNextFrame(Runnable runnable) {
            this.mView.postOnAnimation(runnable);
        }

        @Override
        public void removeCallback(Runnable runnable) {
            this.mView.removeCallbacks(runnable);
        }
    }
}
