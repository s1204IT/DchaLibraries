package android.support.v7.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import java.util.List;

public class LinearLayoutManager extends RecyclerView.LayoutManager {
    final AnchorInfo mAnchorInfo;
    private boolean mLastStackFromEnd;
    private LayoutState mLayoutState;
    int mOrientation;
    OrientationHelper mOrientationHelper;
    SavedState mPendingSavedState;
    int mPendingScrollPosition;
    int mPendingScrollPositionOffset;
    private boolean mRecycleChildrenOnDetach;
    private boolean mReverseLayout;
    boolean mShouldReverseLayout;
    private boolean mSmoothScrollbarEnabled;
    private boolean mStackFromEnd;

    public LinearLayoutManager(Context context) {
        this(context, 1, false);
    }

    public LinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        this.mReverseLayout = false;
        this.mShouldReverseLayout = false;
        this.mStackFromEnd = false;
        this.mSmoothScrollbarEnabled = true;
        this.mPendingScrollPosition = -1;
        this.mPendingScrollPositionOffset = Integer.MIN_VALUE;
        this.mPendingSavedState = null;
        this.mAnchorInfo = new AnchorInfo();
        setOrientation(orientation);
        setReverseLayout(reverseLayout);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(-2, -2);
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        if (this.mRecycleChildrenOnDetach) {
            removeAndRecycleAllViews(recycler);
            recycler.clear();
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (getChildCount() > 0) {
            AccessibilityRecordCompat record = AccessibilityEventCompat.asRecord(event);
            record.setFromIndex(findFirstVisibleItemPosition());
            record.setToIndex(findLastVisibleItemPosition());
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        if (this.mPendingSavedState != null) {
            return new SavedState(this.mPendingSavedState);
        }
        SavedState state = new SavedState();
        if (getChildCount() > 0) {
            ensureLayoutState();
            boolean didLayoutFromEnd = this.mLastStackFromEnd ^ this.mShouldReverseLayout;
            state.mAnchorLayoutFromEnd = didLayoutFromEnd;
            if (didLayoutFromEnd) {
                View refChild = getChildClosestToEnd();
                state.mAnchorOffset = this.mOrientationHelper.getEndAfterPadding() - this.mOrientationHelper.getDecoratedEnd(refChild);
                state.mAnchorPosition = getPosition(refChild);
                return state;
            }
            View refChild2 = getChildClosestToStart();
            state.mAnchorPosition = getPosition(refChild2);
            state.mAnchorOffset = this.mOrientationHelper.getDecoratedStart(refChild2) - this.mOrientationHelper.getStartAfterPadding();
            return state;
        }
        state.invalidateAnchor();
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            this.mPendingSavedState = (SavedState) state;
            requestLayout();
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        return this.mOrientation == 0;
    }

    @Override
    public boolean canScrollVertically() {
        return this.mOrientation == 1;
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setOrientation(int orientation) {
        if (orientation != 0 && orientation != 1) {
            throw new IllegalArgumentException("invalid orientation:" + orientation);
        }
        assertNotInLayoutOrScroll(null);
        if (orientation != this.mOrientation) {
            this.mOrientation = orientation;
            this.mOrientationHelper = null;
            requestLayout();
        }
    }

    private void resolveShouldLayoutReverse() {
        if (this.mOrientation == 1 || !isLayoutRTL()) {
            this.mShouldReverseLayout = this.mReverseLayout;
        } else {
            this.mShouldReverseLayout = this.mReverseLayout ? false : true;
        }
    }

    public void setReverseLayout(boolean reverseLayout) {
        assertNotInLayoutOrScroll(null);
        if (reverseLayout != this.mReverseLayout) {
            this.mReverseLayout = reverseLayout;
            requestLayout();
        }
    }

    @Override
    public View findViewByPosition(int position) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }
        int firstChild = getPosition(getChildAt(0));
        int viewPosition = position - firstChild;
        if (viewPosition < 0 || viewPosition >= childCount) {
            return null;
        }
        return getChildAt(viewPosition);
    }

    protected int getExtraLayoutSpace(RecyclerView.State state) {
        if (state.hasTargetScrollPosition()) {
            return this.mOrientationHelper.getTotalSpace();
        }
        return 0;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int extraForStart;
        int extraForEnd;
        int endOffset;
        int startOffset;
        View existing;
        int upcomingOffset;
        if (this.mPendingSavedState != null && this.mPendingSavedState.hasValidAnchor()) {
            this.mPendingScrollPosition = this.mPendingSavedState.mAnchorPosition;
        }
        ensureLayoutState();
        this.mLayoutState.mRecycle = false;
        resolveShouldLayoutReverse();
        this.mAnchorInfo.reset();
        this.mAnchorInfo.mLayoutFromEnd = this.mShouldReverseLayout ^ this.mStackFromEnd;
        updateAnchorInfoForLayout(state, this.mAnchorInfo);
        int extra = getExtraLayoutSpace(state);
        boolean before = state.hasTargetScrollPosition() && state.getTargetScrollPosition() < this.mAnchorInfo.mPosition;
        if (before == this.mAnchorInfo.mLayoutFromEnd) {
            extraForEnd = extra;
            extraForStart = 0;
        } else {
            extraForStart = extra;
            extraForEnd = 0;
        }
        int extraForStart2 = extraForStart + this.mOrientationHelper.getStartAfterPadding();
        int extraForEnd2 = extraForEnd + this.mOrientationHelper.getEndPadding();
        if (state.isPreLayout() && this.mPendingScrollPosition != -1 && this.mPendingScrollPositionOffset != Integer.MIN_VALUE && (existing = findViewByPosition(this.mPendingScrollPosition)) != null) {
            if (this.mShouldReverseLayout) {
                int current = this.mOrientationHelper.getEndAfterPadding() - this.mOrientationHelper.getDecoratedEnd(existing);
                upcomingOffset = current - this.mPendingScrollPositionOffset;
            } else {
                int current2 = this.mOrientationHelper.getDecoratedStart(existing) - this.mOrientationHelper.getStartAfterPadding();
                upcomingOffset = this.mPendingScrollPositionOffset - current2;
            }
            if (upcomingOffset > 0) {
                extraForStart2 += upcomingOffset;
            } else {
                extraForEnd2 -= upcomingOffset;
            }
        }
        onAnchorReady(state, this.mAnchorInfo);
        detachAndScrapAttachedViews(recycler);
        this.mLayoutState.mIsPreLayout = state.isPreLayout();
        if (this.mAnchorInfo.mLayoutFromEnd) {
            updateLayoutStateToFillStart(this.mAnchorInfo);
            this.mLayoutState.mExtra = extraForStart2;
            fill(recycler, this.mLayoutState, state, false);
            startOffset = this.mLayoutState.mOffset;
            if (this.mLayoutState.mAvailable > 0) {
                extraForEnd2 += this.mLayoutState.mAvailable;
            }
            updateLayoutStateToFillEnd(this.mAnchorInfo);
            this.mLayoutState.mExtra = extraForEnd2;
            this.mLayoutState.mCurrentPosition += this.mLayoutState.mItemDirection;
            fill(recycler, this.mLayoutState, state, false);
            endOffset = this.mLayoutState.mOffset;
        } else {
            updateLayoutStateToFillEnd(this.mAnchorInfo);
            this.mLayoutState.mExtra = extraForEnd2;
            fill(recycler, this.mLayoutState, state, false);
            endOffset = this.mLayoutState.mOffset;
            if (this.mLayoutState.mAvailable > 0) {
                extraForStart2 += this.mLayoutState.mAvailable;
            }
            updateLayoutStateToFillStart(this.mAnchorInfo);
            this.mLayoutState.mExtra = extraForStart2;
            this.mLayoutState.mCurrentPosition += this.mLayoutState.mItemDirection;
            fill(recycler, this.mLayoutState, state, false);
            startOffset = this.mLayoutState.mOffset;
        }
        if (getChildCount() > 0) {
            if (this.mShouldReverseLayout ^ this.mStackFromEnd) {
                int fixOffset = fixLayoutEndGap(endOffset, recycler, state, true);
                int startOffset2 = startOffset + fixOffset;
                int endOffset2 = endOffset + fixOffset;
                int fixOffset2 = fixLayoutStartGap(startOffset2, recycler, state, false);
                startOffset = startOffset2 + fixOffset2;
                endOffset = endOffset2 + fixOffset2;
            } else {
                int fixOffset3 = fixLayoutStartGap(startOffset, recycler, state, true);
                int startOffset3 = startOffset + fixOffset3;
                int endOffset3 = endOffset + fixOffset3;
                int fixOffset4 = fixLayoutEndGap(endOffset3, recycler, state, false);
                startOffset = startOffset3 + fixOffset4;
                endOffset = endOffset3 + fixOffset4;
            }
        }
        layoutForPredictiveAnimations(recycler, state, startOffset, endOffset);
        if (!state.isPreLayout()) {
            this.mPendingScrollPosition = -1;
            this.mPendingScrollPositionOffset = Integer.MIN_VALUE;
            this.mOrientationHelper.onLayoutComplete();
        }
        this.mLastStackFromEnd = this.mStackFromEnd;
        this.mPendingSavedState = null;
    }

    void onAnchorReady(RecyclerView.State state, AnchorInfo anchorInfo) {
    }

    private void layoutForPredictiveAnimations(RecyclerView.Recycler recycler, RecyclerView.State state, int startOffset, int endOffset) {
        if (state.willRunPredictiveAnimations() && getChildCount() != 0 && !state.isPreLayout() && supportsPredictiveItemAnimations()) {
            int scrapExtraStart = 0;
            int scrapExtraEnd = 0;
            List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
            int scrapSize = scrapList.size();
            int firstChildPos = getPosition(getChildAt(0));
            for (int i = 0; i < scrapSize; i++) {
                RecyclerView.ViewHolder scrap = scrapList.get(i);
                int position = scrap.getLayoutPosition();
                int direction = (position < firstChildPos) != this.mShouldReverseLayout ? -1 : 1;
                if (direction == -1) {
                    scrapExtraStart += this.mOrientationHelper.getDecoratedMeasurement(scrap.itemView);
                } else {
                    scrapExtraEnd += this.mOrientationHelper.getDecoratedMeasurement(scrap.itemView);
                }
            }
            this.mLayoutState.mScrapList = scrapList;
            if (scrapExtraStart > 0) {
                View anchor = getChildClosestToStart();
                updateLayoutStateToFillStart(getPosition(anchor), startOffset);
                this.mLayoutState.mExtra = scrapExtraStart;
                this.mLayoutState.mAvailable = 0;
                LayoutState layoutState = this.mLayoutState;
                layoutState.mCurrentPosition = (this.mShouldReverseLayout ? 1 : -1) + layoutState.mCurrentPosition;
                fill(recycler, this.mLayoutState, state, false);
            }
            if (scrapExtraEnd > 0) {
                View anchor2 = getChildClosestToEnd();
                updateLayoutStateToFillEnd(getPosition(anchor2), endOffset);
                this.mLayoutState.mExtra = scrapExtraEnd;
                this.mLayoutState.mAvailable = 0;
                LayoutState layoutState2 = this.mLayoutState;
                layoutState2.mCurrentPosition = (this.mShouldReverseLayout ? -1 : 1) + layoutState2.mCurrentPosition;
                fill(recycler, this.mLayoutState, state, false);
            }
            this.mLayoutState.mScrapList = null;
        }
    }

    private void updateAnchorInfoForLayout(RecyclerView.State state, AnchorInfo anchorInfo) {
        if (!updateAnchorFromPendingData(state, anchorInfo) && !updateAnchorFromChildren(state, anchorInfo)) {
            anchorInfo.assignCoordinateFromPadding();
            anchorInfo.mPosition = this.mStackFromEnd ? state.getItemCount() - 1 : 0;
        }
    }

    private boolean updateAnchorFromChildren(RecyclerView.State state, AnchorInfo anchorInfo) {
        if (getChildCount() == 0) {
            return false;
        }
        View focused = getFocusedChild();
        if (focused != null && anchorInfo.assignFromViewIfValid(focused, state)) {
            return true;
        }
        if (this.mLastStackFromEnd != this.mStackFromEnd) {
            return false;
        }
        View referenceChild = anchorInfo.mLayoutFromEnd ? findReferenceChildClosestToEnd(state) : findReferenceChildClosestToStart(state);
        if (referenceChild == null) {
            return false;
        }
        anchorInfo.assignFromView(referenceChild);
        if (!state.isPreLayout() && supportsPredictiveItemAnimations()) {
            boolean notVisible = this.mOrientationHelper.getDecoratedStart(referenceChild) >= this.mOrientationHelper.getEndAfterPadding() || this.mOrientationHelper.getDecoratedEnd(referenceChild) < this.mOrientationHelper.getStartAfterPadding();
            if (notVisible) {
                anchorInfo.mCoordinate = anchorInfo.mLayoutFromEnd ? this.mOrientationHelper.getEndAfterPadding() : this.mOrientationHelper.getStartAfterPadding();
            }
        }
        return true;
    }

    private boolean updateAnchorFromPendingData(RecyclerView.State state, AnchorInfo anchorInfo) {
        if (state.isPreLayout() || this.mPendingScrollPosition == -1) {
            return false;
        }
        if (this.mPendingScrollPosition < 0 || this.mPendingScrollPosition >= state.getItemCount()) {
            this.mPendingScrollPosition = -1;
            this.mPendingScrollPositionOffset = Integer.MIN_VALUE;
            return false;
        }
        anchorInfo.mPosition = this.mPendingScrollPosition;
        if (this.mPendingSavedState != null && this.mPendingSavedState.hasValidAnchor()) {
            anchorInfo.mLayoutFromEnd = this.mPendingSavedState.mAnchorLayoutFromEnd;
            if (anchorInfo.mLayoutFromEnd) {
                anchorInfo.mCoordinate = this.mOrientationHelper.getEndAfterPadding() - this.mPendingSavedState.mAnchorOffset;
                return true;
            }
            anchorInfo.mCoordinate = this.mOrientationHelper.getStartAfterPadding() + this.mPendingSavedState.mAnchorOffset;
            return true;
        }
        if (this.mPendingScrollPositionOffset == Integer.MIN_VALUE) {
            View child = findViewByPosition(this.mPendingScrollPosition);
            if (child != null) {
                int childSize = this.mOrientationHelper.getDecoratedMeasurement(child);
                if (childSize > this.mOrientationHelper.getTotalSpace()) {
                    anchorInfo.assignCoordinateFromPadding();
                    return true;
                }
                int startGap = this.mOrientationHelper.getDecoratedStart(child) - this.mOrientationHelper.getStartAfterPadding();
                if (startGap < 0) {
                    anchorInfo.mCoordinate = this.mOrientationHelper.getStartAfterPadding();
                    anchorInfo.mLayoutFromEnd = false;
                    return true;
                }
                int endGap = this.mOrientationHelper.getEndAfterPadding() - this.mOrientationHelper.getDecoratedEnd(child);
                if (endGap < 0) {
                    anchorInfo.mCoordinate = this.mOrientationHelper.getEndAfterPadding();
                    anchorInfo.mLayoutFromEnd = true;
                    return true;
                }
                anchorInfo.mCoordinate = anchorInfo.mLayoutFromEnd ? this.mOrientationHelper.getDecoratedEnd(child) + this.mOrientationHelper.getTotalSpaceChange() : this.mOrientationHelper.getDecoratedStart(child);
                return true;
            }
            if (getChildCount() > 0) {
                int pos = getPosition(getChildAt(0));
                anchorInfo.mLayoutFromEnd = (this.mPendingScrollPosition < pos) == this.mShouldReverseLayout;
            }
            anchorInfo.assignCoordinateFromPadding();
            return true;
        }
        anchorInfo.mLayoutFromEnd = this.mShouldReverseLayout;
        if (this.mShouldReverseLayout) {
            anchorInfo.mCoordinate = this.mOrientationHelper.getEndAfterPadding() - this.mPendingScrollPositionOffset;
            return true;
        }
        anchorInfo.mCoordinate = this.mOrientationHelper.getStartAfterPadding() + this.mPendingScrollPositionOffset;
        return true;
    }

    private int fixLayoutEndGap(int endOffset, RecyclerView.Recycler recycler, RecyclerView.State state, boolean canOffsetChildren) {
        int gap;
        int gap2 = this.mOrientationHelper.getEndAfterPadding() - endOffset;
        if (gap2 > 0) {
            int fixOffset = -scrollBy(-gap2, recycler, state);
            int endOffset2 = endOffset + fixOffset;
            if (!canOffsetChildren || (gap = this.mOrientationHelper.getEndAfterPadding() - endOffset2) <= 0) {
                return fixOffset;
            }
            this.mOrientationHelper.offsetChildren(gap);
            return gap + fixOffset;
        }
        return 0;
    }

    private int fixLayoutStartGap(int startOffset, RecyclerView.Recycler recycler, RecyclerView.State state, boolean canOffsetChildren) {
        int gap;
        int gap2 = startOffset - this.mOrientationHelper.getStartAfterPadding();
        if (gap2 > 0) {
            int fixOffset = -scrollBy(gap2, recycler, state);
            int startOffset2 = startOffset + fixOffset;
            if (!canOffsetChildren || (gap = startOffset2 - this.mOrientationHelper.getStartAfterPadding()) <= 0) {
                return fixOffset;
            }
            this.mOrientationHelper.offsetChildren(-gap);
            return fixOffset - gap;
        }
        return 0;
    }

    private void updateLayoutStateToFillEnd(AnchorInfo anchorInfo) {
        updateLayoutStateToFillEnd(anchorInfo.mPosition, anchorInfo.mCoordinate);
    }

    private void updateLayoutStateToFillEnd(int itemPosition, int offset) {
        this.mLayoutState.mAvailable = this.mOrientationHelper.getEndAfterPadding() - offset;
        this.mLayoutState.mItemDirection = this.mShouldReverseLayout ? -1 : 1;
        this.mLayoutState.mCurrentPosition = itemPosition;
        this.mLayoutState.mLayoutDirection = 1;
        this.mLayoutState.mOffset = offset;
        this.mLayoutState.mScrollingOffset = Integer.MIN_VALUE;
    }

    private void updateLayoutStateToFillStart(AnchorInfo anchorInfo) {
        updateLayoutStateToFillStart(anchorInfo.mPosition, anchorInfo.mCoordinate);
    }

    private void updateLayoutStateToFillStart(int itemPosition, int offset) {
        this.mLayoutState.mAvailable = offset - this.mOrientationHelper.getStartAfterPadding();
        this.mLayoutState.mCurrentPosition = itemPosition;
        this.mLayoutState.mItemDirection = this.mShouldReverseLayout ? 1 : -1;
        this.mLayoutState.mLayoutDirection = -1;
        this.mLayoutState.mOffset = offset;
        this.mLayoutState.mScrollingOffset = Integer.MIN_VALUE;
    }

    protected boolean isLayoutRTL() {
        return getLayoutDirection() == 1;
    }

    void ensureLayoutState() {
        if (this.mLayoutState == null) {
            this.mLayoutState = new LayoutState();
        }
        if (this.mOrientationHelper == null) {
            this.mOrientationHelper = OrientationHelper.createOrientationHelper(this, this.mOrientation);
        }
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (this.mOrientation == 1) {
            return 0;
        }
        return scrollBy(dx, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (this.mOrientation == 0) {
            return 0;
        }
        return scrollBy(dy, recycler, state);
    }

    @Override
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        return computeScrollOffset(state);
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        return computeScrollOffset(state);
    }

    @Override
    public int computeHorizontalScrollExtent(RecyclerView.State state) {
        return computeScrollExtent(state);
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return computeScrollExtent(state);
    }

    @Override
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        return computeScrollRange(state);
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        return computeScrollRange(state);
    }

    private int computeScrollOffset(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }
        ensureLayoutState();
        return ScrollbarHelper.computeScrollOffset(state, this.mOrientationHelper, findFirstVisibleChildClosestToStart(!this.mSmoothScrollbarEnabled, true), findFirstVisibleChildClosestToEnd(this.mSmoothScrollbarEnabled ? false : true, true), this, this.mSmoothScrollbarEnabled, this.mShouldReverseLayout);
    }

    private int computeScrollExtent(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }
        ensureLayoutState();
        return ScrollbarHelper.computeScrollExtent(state, this.mOrientationHelper, findFirstVisibleChildClosestToStart(!this.mSmoothScrollbarEnabled, true), findFirstVisibleChildClosestToEnd(this.mSmoothScrollbarEnabled ? false : true, true), this, this.mSmoothScrollbarEnabled);
    }

    private int computeScrollRange(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }
        ensureLayoutState();
        return ScrollbarHelper.computeScrollRange(state, this.mOrientationHelper, findFirstVisibleChildClosestToStart(!this.mSmoothScrollbarEnabled, true), findFirstVisibleChildClosestToEnd(this.mSmoothScrollbarEnabled ? false : true, true), this, this.mSmoothScrollbarEnabled);
    }

    private void updateLayoutState(int layoutDirection, int requiredSpace, boolean canUseExistingSpace, RecyclerView.State state) {
        int fastScrollSpace;
        this.mLayoutState.mExtra = getExtraLayoutSpace(state);
        this.mLayoutState.mLayoutDirection = layoutDirection;
        if (layoutDirection == 1) {
            this.mLayoutState.mExtra += this.mOrientationHelper.getEndPadding();
            View child = getChildClosestToEnd();
            this.mLayoutState.mItemDirection = this.mShouldReverseLayout ? -1 : 1;
            this.mLayoutState.mCurrentPosition = getPosition(child) + this.mLayoutState.mItemDirection;
            this.mLayoutState.mOffset = this.mOrientationHelper.getDecoratedEnd(child);
            fastScrollSpace = this.mOrientationHelper.getDecoratedEnd(child) - this.mOrientationHelper.getEndAfterPadding();
        } else {
            View child2 = getChildClosestToStart();
            this.mLayoutState.mExtra += this.mOrientationHelper.getStartAfterPadding();
            this.mLayoutState.mItemDirection = this.mShouldReverseLayout ? 1 : -1;
            this.mLayoutState.mCurrentPosition = getPosition(child2) + this.mLayoutState.mItemDirection;
            this.mLayoutState.mOffset = this.mOrientationHelper.getDecoratedStart(child2);
            fastScrollSpace = (-this.mOrientationHelper.getDecoratedStart(child2)) + this.mOrientationHelper.getStartAfterPadding();
        }
        this.mLayoutState.mAvailable = requiredSpace;
        if (canUseExistingSpace) {
            this.mLayoutState.mAvailable -= fastScrollSpace;
        }
        this.mLayoutState.mScrollingOffset = fastScrollSpace;
    }

    int scrollBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scrolled = 0;
        if (getChildCount() != 0 && dy != 0) {
            this.mLayoutState.mRecycle = true;
            ensureLayoutState();
            int layoutDirection = dy > 0 ? 1 : -1;
            int absDy = Math.abs(dy);
            updateLayoutState(layoutDirection, absDy, true, state);
            int freeScroll = this.mLayoutState.mScrollingOffset;
            int consumed = freeScroll + fill(recycler, this.mLayoutState, state, false);
            if (consumed >= 0) {
                scrolled = absDy > consumed ? layoutDirection * consumed : dy;
                this.mOrientationHelper.offsetChildren(-scrolled);
            }
        }
        return scrolled;
    }

    @Override
    public void assertNotInLayoutOrScroll(String message) {
        if (this.mPendingSavedState == null) {
            super.assertNotInLayoutOrScroll(message);
        }
    }

    private void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        if (startIndex != endIndex) {
            if (endIndex > startIndex) {
                for (int i = endIndex - 1; i >= startIndex; i--) {
                    removeAndRecycleViewAt(i, recycler);
                }
                return;
            }
            for (int i2 = startIndex; i2 > endIndex; i2--) {
                removeAndRecycleViewAt(i2, recycler);
            }
        }
    }

    private void recycleViewsFromStart(RecyclerView.Recycler recycler, int dt) {
        if (dt >= 0) {
            int childCount = getChildCount();
            if (this.mShouldReverseLayout) {
                for (int i = childCount - 1; i >= 0; i--) {
                    View child = getChildAt(i);
                    if (this.mOrientationHelper.getDecoratedEnd(child) > dt) {
                        recycleChildren(recycler, childCount - 1, i);
                        return;
                    }
                }
                return;
            }
            for (int i2 = 0; i2 < childCount; i2++) {
                View child2 = getChildAt(i2);
                if (this.mOrientationHelper.getDecoratedEnd(child2) > dt) {
                    recycleChildren(recycler, 0, i2);
                    return;
                }
            }
        }
    }

    private void recycleViewsFromEnd(RecyclerView.Recycler recycler, int dt) {
        int childCount = getChildCount();
        if (dt >= 0) {
            int limit = this.mOrientationHelper.getEnd() - dt;
            if (this.mShouldReverseLayout) {
                for (int i = 0; i < childCount; i++) {
                    View child = getChildAt(i);
                    if (this.mOrientationHelper.getDecoratedStart(child) < limit) {
                        recycleChildren(recycler, 0, i);
                        return;
                    }
                }
                return;
            }
            for (int i2 = childCount - 1; i2 >= 0; i2--) {
                View child2 = getChildAt(i2);
                if (this.mOrientationHelper.getDecoratedStart(child2) < limit) {
                    recycleChildren(recycler, childCount - 1, i2);
                    return;
                }
            }
        }
    }

    private void recycleByLayoutState(RecyclerView.Recycler recycler, LayoutState layoutState) {
        if (layoutState.mRecycle) {
            if (layoutState.mLayoutDirection == -1) {
                recycleViewsFromEnd(recycler, layoutState.mScrollingOffset);
            } else {
                recycleViewsFromStart(recycler, layoutState.mScrollingOffset);
            }
        }
    }

    int fill(RecyclerView.Recycler recycler, LayoutState layoutState, RecyclerView.State state, boolean stopOnFocusable) {
        int start = layoutState.mAvailable;
        if (layoutState.mScrollingOffset != Integer.MIN_VALUE) {
            if (layoutState.mAvailable < 0) {
                layoutState.mScrollingOffset += layoutState.mAvailable;
            }
            recycleByLayoutState(recycler, layoutState);
        }
        int remainingSpace = layoutState.mAvailable + layoutState.mExtra;
        LayoutChunkResult layoutChunkResult = new LayoutChunkResult();
        while (remainingSpace > 0 && layoutState.hasMore(state)) {
            layoutChunkResult.resetInternal();
            layoutChunk(recycler, state, layoutState, layoutChunkResult);
            if (!layoutChunkResult.mFinished) {
                layoutState.mOffset += layoutChunkResult.mConsumed * layoutState.mLayoutDirection;
                if (!layoutChunkResult.mIgnoreConsumed || this.mLayoutState.mScrapList != null || !state.isPreLayout()) {
                    layoutState.mAvailable -= layoutChunkResult.mConsumed;
                    remainingSpace -= layoutChunkResult.mConsumed;
                }
                if (layoutState.mScrollingOffset != Integer.MIN_VALUE) {
                    layoutState.mScrollingOffset += layoutChunkResult.mConsumed;
                    if (layoutState.mAvailable < 0) {
                        layoutState.mScrollingOffset += layoutState.mAvailable;
                    }
                    recycleByLayoutState(recycler, layoutState);
                }
                if (stopOnFocusable && layoutChunkResult.mFocusable) {
                    break;
                }
            } else {
                break;
            }
        }
        return start - layoutState.mAvailable;
    }

    void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutState layoutState, LayoutChunkResult result) {
        int top;
        int bottom;
        int left;
        int right;
        View view = layoutState.next(recycler);
        if (view == null) {
            result.mFinished = true;
            return;
        }
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutState.mScrapList == null) {
            if (this.mShouldReverseLayout == (layoutState.mLayoutDirection == -1)) {
                addView(view);
            } else {
                addView(view, 0);
            }
        } else {
            if (this.mShouldReverseLayout == (layoutState.mLayoutDirection == -1)) {
                addDisappearingView(view);
            } else {
                addDisappearingView(view, 0);
            }
        }
        measureChildWithMargins(view, 0, 0);
        result.mConsumed = this.mOrientationHelper.getDecoratedMeasurement(view);
        if (this.mOrientation == 1) {
            if (isLayoutRTL()) {
                right = getWidth() - getPaddingRight();
                left = right - this.mOrientationHelper.getDecoratedMeasurementInOther(view);
            } else {
                left = getPaddingLeft();
                right = left + this.mOrientationHelper.getDecoratedMeasurementInOther(view);
            }
            if (layoutState.mLayoutDirection == -1) {
                bottom = layoutState.mOffset;
                top = layoutState.mOffset - result.mConsumed;
            } else {
                top = layoutState.mOffset;
                bottom = layoutState.mOffset + result.mConsumed;
            }
        } else {
            top = getPaddingTop();
            bottom = top + this.mOrientationHelper.getDecoratedMeasurementInOther(view);
            if (layoutState.mLayoutDirection == -1) {
                right = layoutState.mOffset;
                left = layoutState.mOffset - result.mConsumed;
            } else {
                left = layoutState.mOffset;
                right = layoutState.mOffset + result.mConsumed;
            }
        }
        layoutDecorated(view, left + params.leftMargin, top + params.topMargin, right - params.rightMargin, bottom - params.bottomMargin);
        if (params.isItemRemoved() || params.isItemChanged()) {
            result.mIgnoreConsumed = true;
        }
        result.mFocusable = view.isFocusable();
    }

    private int convertFocusDirectionToLayoutDirection(int focusDirection) {
        switch (focusDirection) {
            case 1:
                return -1;
            case 2:
                return 1;
            case 17:
                return this.mOrientation != 0 ? Integer.MIN_VALUE : -1;
            case 33:
                return this.mOrientation != 1 ? Integer.MIN_VALUE : -1;
            case 66:
                return this.mOrientation != 0 ? Integer.MIN_VALUE : 1;
            case 130:
                return this.mOrientation == 1 ? 1 : Integer.MIN_VALUE;
            default:
                return Integer.MIN_VALUE;
        }
    }

    private View getChildClosestToStart() {
        return getChildAt(this.mShouldReverseLayout ? getChildCount() - 1 : 0);
    }

    private View getChildClosestToEnd() {
        return getChildAt(this.mShouldReverseLayout ? 0 : getChildCount() - 1);
    }

    private View findFirstVisibleChildClosestToStart(boolean completelyVisible, boolean acceptPartiallyVisible) {
        return this.mShouldReverseLayout ? findOneVisibleChild(getChildCount() - 1, -1, completelyVisible, acceptPartiallyVisible) : findOneVisibleChild(0, getChildCount(), completelyVisible, acceptPartiallyVisible);
    }

    private View findFirstVisibleChildClosestToEnd(boolean completelyVisible, boolean acceptPartiallyVisible) {
        return this.mShouldReverseLayout ? findOneVisibleChild(0, getChildCount(), completelyVisible, acceptPartiallyVisible) : findOneVisibleChild(getChildCount() - 1, -1, completelyVisible, acceptPartiallyVisible);
    }

    private View findReferenceChildClosestToEnd(RecyclerView.State state) {
        return this.mShouldReverseLayout ? findFirstReferenceChild(state.getItemCount()) : findLastReferenceChild(state.getItemCount());
    }

    private View findReferenceChildClosestToStart(RecyclerView.State state) {
        return this.mShouldReverseLayout ? findLastReferenceChild(state.getItemCount()) : findFirstReferenceChild(state.getItemCount());
    }

    private View findFirstReferenceChild(int itemCount) {
        return findReferenceChild(0, getChildCount(), itemCount);
    }

    private View findLastReferenceChild(int itemCount) {
        return findReferenceChild(getChildCount() - 1, -1, itemCount);
    }

    private View findReferenceChild(int start, int end, int itemCount) {
        ensureLayoutState();
        View invalidMatch = null;
        View outOfBoundsMatch = null;
        int boundsStart = this.mOrientationHelper.getStartAfterPadding();
        int boundsEnd = this.mOrientationHelper.getEndAfterPadding();
        int diff = end > start ? 1 : -1;
        for (int i = start; i != end; i += diff) {
            View view = getChildAt(i);
            int position = getPosition(view);
            if (position >= 0 && position < itemCount) {
                if (((RecyclerView.LayoutParams) view.getLayoutParams()).isItemRemoved()) {
                    if (invalidMatch == null) {
                        invalidMatch = view;
                    }
                } else {
                    if (this.mOrientationHelper.getDecoratedStart(view) < boundsEnd && this.mOrientationHelper.getDecoratedEnd(view) >= boundsStart) {
                        return view;
                    }
                    if (outOfBoundsMatch == null) {
                        outOfBoundsMatch = view;
                    }
                }
            }
        }
        if (outOfBoundsMatch == null) {
            outOfBoundsMatch = invalidMatch;
        }
        return outOfBoundsMatch;
    }

    public int findFirstVisibleItemPosition() {
        View child = findOneVisibleChild(0, getChildCount(), false, true);
        if (child == null) {
            return -1;
        }
        return getPosition(child);
    }

    public int findLastVisibleItemPosition() {
        View child = findOneVisibleChild(getChildCount() - 1, -1, false, true);
        if (child == null) {
            return -1;
        }
        return getPosition(child);
    }

    View findOneVisibleChild(int fromIndex, int toIndex, boolean completelyVisible, boolean acceptPartiallyVisible) {
        ensureLayoutState();
        int start = this.mOrientationHelper.getStartAfterPadding();
        int end = this.mOrientationHelper.getEndAfterPadding();
        int next = toIndex > fromIndex ? 1 : -1;
        View partiallyVisible = null;
        for (int i = fromIndex; i != toIndex; i += next) {
            View child = getChildAt(i);
            int childStart = this.mOrientationHelper.getDecoratedStart(child);
            int childEnd = this.mOrientationHelper.getDecoratedEnd(child);
            if (childStart < end && childEnd > start) {
                if (completelyVisible) {
                    if (childStart < start || childEnd > end) {
                        if (acceptPartiallyVisible && partiallyVisible == null) {
                            partiallyVisible = child;
                        }
                    } else {
                        return child;
                    }
                } else {
                    return child;
                }
            }
        }
        return partiallyVisible;
    }

    @Override
    public View onFocusSearchFailed(View focused, int focusDirection, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int layoutDir;
        View referenceChild;
        View nextFocus;
        resolveShouldLayoutReverse();
        if (getChildCount() != 0 && (layoutDir = convertFocusDirectionToLayoutDirection(focusDirection)) != Integer.MIN_VALUE) {
            ensureLayoutState();
            if (layoutDir == -1) {
                referenceChild = findReferenceChildClosestToStart(state);
            } else {
                referenceChild = findReferenceChildClosestToEnd(state);
            }
            if (referenceChild == null) {
                return null;
            }
            ensureLayoutState();
            int maxScroll = (int) (0.33f * this.mOrientationHelper.getTotalSpace());
            updateLayoutState(layoutDir, maxScroll, false, state);
            this.mLayoutState.mScrollingOffset = Integer.MIN_VALUE;
            this.mLayoutState.mRecycle = false;
            fill(recycler, this.mLayoutState, state, true);
            if (layoutDir == -1) {
                nextFocus = getChildClosestToStart();
            } else {
                nextFocus = getChildClosestToEnd();
            }
            if (nextFocus == referenceChild || !nextFocus.isFocusable()) {
                return null;
            }
            return nextFocus;
        }
        return null;
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return this.mPendingSavedState == null && this.mLastStackFromEnd == this.mStackFromEnd;
    }

    static class LayoutState {
        int mAvailable;
        int mCurrentPosition;
        int mItemDirection;
        int mLayoutDirection;
        int mOffset;
        int mScrollingOffset;
        boolean mRecycle = true;
        int mExtra = 0;
        boolean mIsPreLayout = false;
        List<RecyclerView.ViewHolder> mScrapList = null;

        LayoutState() {
        }

        boolean hasMore(RecyclerView.State state) {
            return this.mCurrentPosition >= 0 && this.mCurrentPosition < state.getItemCount();
        }

        View next(RecyclerView.Recycler recycler) {
            if (this.mScrapList != null) {
                return nextFromLimitedList();
            }
            View viewForPosition = recycler.getViewForPosition(this.mCurrentPosition);
            this.mCurrentPosition += this.mItemDirection;
            return viewForPosition;
        }

        private View nextFromLimitedList() {
            int distance;
            int size = this.mScrapList.size();
            RecyclerView.ViewHolder closest = null;
            int closestDistance = Integer.MAX_VALUE;
            for (int i = 0; i < size; i++) {
                RecyclerView.ViewHolder viewHolder = this.mScrapList.get(i);
                if ((this.mIsPreLayout || !viewHolder.isRemoved()) && (distance = (viewHolder.getLayoutPosition() - this.mCurrentPosition) * this.mItemDirection) >= 0 && distance < closestDistance) {
                    closest = viewHolder;
                    closestDistance = distance;
                    if (distance == 0) {
                        break;
                    }
                }
            }
            if (closest == null) {
                return null;
            }
            this.mCurrentPosition = closest.getLayoutPosition() + this.mItemDirection;
            return closest.itemView;
        }
    }

    static class SavedState implements Parcelable {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        boolean mAnchorLayoutFromEnd;
        int mAnchorOffset;
        int mAnchorPosition;

        public SavedState() {
        }

        SavedState(Parcel in) {
            this.mAnchorPosition = in.readInt();
            this.mAnchorOffset = in.readInt();
            this.mAnchorLayoutFromEnd = in.readInt() == 1;
        }

        public SavedState(SavedState other) {
            this.mAnchorPosition = other.mAnchorPosition;
            this.mAnchorOffset = other.mAnchorOffset;
            this.mAnchorLayoutFromEnd = other.mAnchorLayoutFromEnd;
        }

        boolean hasValidAnchor() {
            return this.mAnchorPosition >= 0;
        }

        void invalidateAnchor() {
            this.mAnchorPosition = -1;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mAnchorPosition);
            dest.writeInt(this.mAnchorOffset);
            dest.writeInt(this.mAnchorLayoutFromEnd ? 1 : 0);
        }
    }

    class AnchorInfo {
        int mCoordinate;
        boolean mLayoutFromEnd;
        int mPosition;

        AnchorInfo() {
        }

        void reset() {
            this.mPosition = -1;
            this.mCoordinate = Integer.MIN_VALUE;
            this.mLayoutFromEnd = false;
        }

        void assignCoordinateFromPadding() {
            this.mCoordinate = this.mLayoutFromEnd ? LinearLayoutManager.this.mOrientationHelper.getEndAfterPadding() : LinearLayoutManager.this.mOrientationHelper.getStartAfterPadding();
        }

        public String toString() {
            return "AnchorInfo{mPosition=" + this.mPosition + ", mCoordinate=" + this.mCoordinate + ", mLayoutFromEnd=" + this.mLayoutFromEnd + '}';
        }

        public boolean assignFromViewIfValid(View child, RecyclerView.State state) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
            if (lp.isItemRemoved() || lp.getViewLayoutPosition() < 0 || lp.getViewLayoutPosition() >= state.getItemCount()) {
                return false;
            }
            assignFromView(child);
            return true;
        }

        public void assignFromView(View child) {
            if (this.mLayoutFromEnd) {
                this.mCoordinate = LinearLayoutManager.this.mOrientationHelper.getDecoratedEnd(child) + LinearLayoutManager.this.mOrientationHelper.getTotalSpaceChange();
            } else {
                this.mCoordinate = LinearLayoutManager.this.mOrientationHelper.getDecoratedStart(child);
            }
            this.mPosition = LinearLayoutManager.this.getPosition(child);
        }
    }

    protected static class LayoutChunkResult {
        public int mConsumed;
        public boolean mFinished;
        public boolean mFocusable;
        public boolean mIgnoreConsumed;

        protected LayoutChunkResult() {
        }

        void resetInternal() {
            this.mConsumed = 0;
            this.mFinished = false;
            this.mIgnoreConsumed = false;
            this.mFocusable = false;
        }
    }
}
