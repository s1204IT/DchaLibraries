package com.google.android.flexbox;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import com.google.android.flexbox.FlexboxHelper;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class FlexboxLayoutManager extends RecyclerView.LayoutManager implements RecyclerView.SmoothScroller.ScrollVectorProvider, FlexContainer {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final Rect TEMP_RECT = new Rect();
    private int mAlignItems;
    private AnchorInfo mAnchorInfo;
    private final Context mContext;
    private int mDirtyPosition;
    private int mFlexDirection;
    private List<FlexLine> mFlexLines;
    private FlexboxHelper.FlexLinesResult mFlexLinesResult;
    private int mFlexWrap;
    private final FlexboxHelper mFlexboxHelper;
    private boolean mFromBottomToTop;
    private boolean mIsRtl;
    private int mJustifyContent;
    private int mLastHeight;
    private int mLastWidth;
    private LayoutState mLayoutState;
    private OrientationHelper mOrientationHelper;
    private View mParent;
    private SavedState mPendingSavedState;
    private int mPendingScrollPosition;
    private int mPendingScrollPositionOffset;
    private boolean mRecycleChildrenOnDetach;
    private RecyclerView.Recycler mRecycler;
    private RecyclerView.State mState;
    private OrientationHelper mSubOrientationHelper;
    private SparseArray<View> mViewCache;

    public FlexboxLayoutManager(Context context) {
        this(context, 0, 1);
    }

    public FlexboxLayoutManager(Context context, int flexDirection) {
        this(context, flexDirection, 1);
    }

    public FlexboxLayoutManager(Context context, int flexDirection, int flexWrap) {
        this.mFlexLines = new ArrayList();
        this.mFlexboxHelper = new FlexboxHelper(this);
        this.mAnchorInfo = new AnchorInfo();
        this.mPendingScrollPosition = -1;
        this.mPendingScrollPositionOffset = Integer.MIN_VALUE;
        this.mLastWidth = Integer.MIN_VALUE;
        this.mLastHeight = Integer.MIN_VALUE;
        this.mViewCache = new SparseArray<>();
        this.mDirtyPosition = -1;
        this.mFlexLinesResult = new FlexboxHelper.FlexLinesResult();
        setFlexDirection(flexDirection);
        setFlexWrap(flexWrap);
        setAlignItems(4);
        setAutoMeasureEnabled(true);
        this.mContext = context;
    }

    public FlexboxLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.mFlexLines = new ArrayList();
        this.mFlexboxHelper = new FlexboxHelper(this);
        this.mAnchorInfo = new AnchorInfo();
        this.mPendingScrollPosition = -1;
        this.mPendingScrollPositionOffset = Integer.MIN_VALUE;
        this.mLastWidth = Integer.MIN_VALUE;
        this.mLastHeight = Integer.MIN_VALUE;
        this.mViewCache = new SparseArray<>();
        this.mDirtyPosition = -1;
        this.mFlexLinesResult = new FlexboxHelper.FlexLinesResult();
        RecyclerView.LayoutManager.Properties properties = getProperties(context, attrs, defStyleAttr, defStyleRes);
        switch (properties.orientation) {
            case 0:
                if (properties.reverseLayout) {
                    setFlexDirection(1);
                    break;
                } else {
                    setFlexDirection(0);
                    break;
                }
            case 1:
                if (properties.reverseLayout) {
                    setFlexDirection(3);
                    break;
                } else {
                    setFlexDirection(2);
                    break;
                }
        }
        setFlexWrap(1);
        setAlignItems(4);
        setAutoMeasureEnabled(true);
        this.mContext = context;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getFlexDirection() {
        return this.mFlexDirection;
    }

    public void setFlexDirection(int flexDirection) {
        if (this.mFlexDirection != flexDirection) {
            removeAllViews();
            this.mFlexDirection = flexDirection;
            this.mOrientationHelper = null;
            this.mSubOrientationHelper = null;
            clearFlexLines();
            requestLayout();
        }
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getFlexWrap() {
        return this.mFlexWrap;
    }

    public void setFlexWrap(int flexWrap) {
        if (flexWrap == 2) {
            throw new UnsupportedOperationException("wrap_reverse is not supported in FlexboxLayoutManager");
        }
        if (this.mFlexWrap != flexWrap) {
            if (this.mFlexWrap == 0 || flexWrap == 0) {
                removeAllViews();
                clearFlexLines();
            }
            this.mFlexWrap = flexWrap;
            this.mOrientationHelper = null;
            this.mSubOrientationHelper = null;
            requestLayout();
        }
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getAlignItems() {
        return this.mAlignItems;
    }

    public void setAlignItems(int alignItems) {
        if (this.mAlignItems != alignItems) {
            if (this.mAlignItems == 4 || alignItems == 4) {
                removeAllViews();
                clearFlexLines();
            }
            this.mAlignItems = alignItems;
            requestLayout();
        }
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getAlignContent() {
        return 5;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getDecorationLengthMainAxis(View view, int index, int indexInFlexLine) {
        if (isMainAxisDirectionHorizontal()) {
            return getLeftDecorationWidth(view) + getRightDecorationWidth(view);
        }
        return getTopDecorationHeight(view) + getBottomDecorationHeight(view);
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getDecorationLengthCrossAxis(View view) {
        if (isMainAxisDirectionHorizontal()) {
            return getTopDecorationHeight(view) + getBottomDecorationHeight(view);
        }
        return getLeftDecorationWidth(view) + getRightDecorationWidth(view);
    }

    @Override // com.google.android.flexbox.FlexContainer
    public void onNewFlexItemAdded(View view, int index, int indexInFlexLine, FlexLine flexLine) {
        calculateItemDecorationsForChild(view, TEMP_RECT);
        if (isMainAxisDirectionHorizontal()) {
            int decorationWidth = getLeftDecorationWidth(view) + getRightDecorationWidth(view);
            flexLine.mMainSize += decorationWidth;
            flexLine.mDividerLengthInMainSize += decorationWidth;
        } else {
            int decorationHeight = getTopDecorationHeight(view) + getBottomDecorationHeight(view);
            flexLine.mMainSize += decorationHeight;
            flexLine.mDividerLengthInMainSize += decorationHeight;
        }
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getFlexItemCount() {
        return this.mState.getItemCount();
    }

    @Override // com.google.android.flexbox.FlexContainer
    public View getFlexItemAt(int index) {
        View cachedView = this.mViewCache.get(index);
        if (cachedView != null) {
            return cachedView;
        }
        return this.mRecycler.getViewForPosition(index);
    }

    @Override // com.google.android.flexbox.FlexContainer
    public View getReorderedFlexItemAt(int index) {
        return getFlexItemAt(index);
    }

    @Override // com.google.android.flexbox.FlexContainer
    public void onNewFlexLineAdded(FlexLine flexLine) {
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getChildWidthMeasureSpec(int widthSpec, int padding, int childDimension) {
        return getChildMeasureSpec(getWidth(), getWidthMode(), padding, childDimension, canScrollHorizontally());
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getChildHeightMeasureSpec(int heightSpec, int padding, int childDimension) {
        return getChildMeasureSpec(getHeight(), getHeightMode(), padding, childDimension, canScrollVertically());
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getLargestMainSize() {
        if (this.mFlexLines.size() == 0) {
            return 0;
        }
        int largestSize = Integer.MIN_VALUE;
        int size = this.mFlexLines.size();
        for (int i = 0; i < size; i++) {
            FlexLine flexLine = this.mFlexLines.get(i);
            largestSize = Math.max(largestSize, flexLine.mMainSize);
        }
        return largestSize;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getSumOfCrossSize() {
        int sum = 0;
        int size = this.mFlexLines.size();
        for (int i = 0; i < size; i++) {
            FlexLine flexLine = this.mFlexLines.get(i);
            sum += flexLine.mCrossSize;
        }
        return sum;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public void setFlexLines(List<FlexLine> flexLines) {
        this.mFlexLines = flexLines;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public List<FlexLine> getFlexLinesInternal() {
        return this.mFlexLines;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public void updateViewCache(int position, View view) {
        this.mViewCache.put(position, view);
    }

    @Override // android.support.v7.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        int firstChildPos = getPosition(getChildAt(0));
        int direction = targetPosition < firstChildPos ? -1 : 1;
        if (isMainAxisDirectionHorizontal()) {
            return new PointF(0.0f, direction);
        }
        return new PointF(direction, 0.0f);
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public Parcelable onSaveInstanceState() {
        if (this.mPendingSavedState != null) {
            return new SavedState(this.mPendingSavedState);
        }
        SavedState savedState = new SavedState();
        if (getChildCount() > 0) {
            View firstView = getChildClosestToStart();
            savedState.mAnchorPosition = getPosition(firstView);
            savedState.mAnchorOffset = this.mOrientationHelper.getDecoratedStart(firstView) - this.mOrientationHelper.getStartAfterPadding();
        } else {
            savedState.invalidateAnchor();
        }
        return savedState;
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            this.mPendingSavedState = (SavedState) state;
            requestLayout();
        }
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsAdded(recyclerView, positionStart, itemCount);
        updateDirtyPosition(positionStart);
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        super.onItemsUpdated(recyclerView, positionStart, itemCount, payload);
        updateDirtyPosition(positionStart);
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsUpdated(recyclerView, positionStart, itemCount);
        updateDirtyPosition(positionStart);
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
        updateDirtyPosition(positionStart);
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        super.onItemsMoved(recyclerView, from, to, itemCount);
        updateDirtyPosition(Math.min(from, to));
    }

    private void updateDirtyPosition(int positionStart) {
        int firstVisiblePosition = findFirstVisibleItemPosition();
        int lastVisiblePosition = findLastVisibleItemPosition();
        if (positionStart >= lastVisiblePosition) {
            return;
        }
        int childCount = getChildCount();
        this.mFlexboxHelper.ensureMeasureSpecCache(childCount);
        this.mFlexboxHelper.ensureMeasuredSizeCache(childCount);
        this.mFlexboxHelper.ensureIndexToFlexLine(childCount);
        if (positionStart >= this.mFlexboxHelper.mIndexToFlexLine.length) {
            return;
        }
        this.mDirtyPosition = positionStart;
        View firstView = getChildClosestToStart();
        if (firstView == null) {
            return;
        }
        if (firstVisiblePosition <= positionStart && positionStart <= lastVisiblePosition) {
            return;
        }
        this.mPendingScrollPosition = getPosition(firstView);
        if (!isMainAxisDirectionHorizontal() && this.mIsRtl) {
            this.mPendingScrollPositionOffset = this.mOrientationHelper.getDecoratedEnd(firstView) + this.mOrientationHelper.getEndPadding();
        } else {
            this.mPendingScrollPositionOffset = this.mOrientationHelper.getDecoratedStart(firstView) - this.mOrientationHelper.getStartAfterPadding();
        }
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int startOffset;
        int endOffset;
        this.mRecycler = recycler;
        this.mState = state;
        int childCount = state.getItemCount();
        if (childCount == 0 && state.isPreLayout()) {
            return;
        }
        resolveLayoutDirection();
        ensureOrientationHelper();
        ensureLayoutState();
        this.mFlexboxHelper.ensureMeasureSpecCache(childCount);
        this.mFlexboxHelper.ensureMeasuredSizeCache(childCount);
        this.mFlexboxHelper.ensureIndexToFlexLine(childCount);
        this.mLayoutState.mShouldRecycle = false;
        if (this.mPendingSavedState != null && this.mPendingSavedState.hasValidAnchor(childCount)) {
            this.mPendingScrollPosition = this.mPendingSavedState.mAnchorPosition;
        }
        if (!this.mAnchorInfo.mValid || this.mPendingScrollPosition != -1 || this.mPendingSavedState != null) {
            this.mAnchorInfo.reset();
            updateAnchorInfoForLayout(state, this.mAnchorInfo);
            this.mAnchorInfo.mValid = true;
        }
        detachAndScrapAttachedViews(recycler);
        if (this.mAnchorInfo.mLayoutFromEnd) {
            updateLayoutStateToFillStart(this.mAnchorInfo, false, true);
        } else {
            updateLayoutStateToFillEnd(this.mAnchorInfo, false, true);
        }
        updateFlexLines(childCount);
        if (this.mAnchorInfo.mLayoutFromEnd) {
            fill(recycler, state, this.mLayoutState);
            int startOffset2 = this.mLayoutState.mOffset;
            updateLayoutStateToFillEnd(this.mAnchorInfo, true, false);
            fill(recycler, state, this.mLayoutState);
            int filledToEnd = this.mLayoutState.mOffset;
            startOffset = filledToEnd;
            endOffset = startOffset2;
        } else {
            fill(recycler, state, this.mLayoutState);
            startOffset = this.mLayoutState.mOffset;
            updateLayoutStateToFillStart(this.mAnchorInfo, true, false);
            fill(recycler, state, this.mLayoutState);
            endOffset = this.mLayoutState.mOffset;
        }
        int filledToStart = getChildCount();
        if (filledToStart <= 0) {
            return;
        }
        if (this.mAnchorInfo.mLayoutFromEnd) {
            int fixOffset = fixLayoutEndGap(startOffset, recycler, state, true);
            int startOffset3 = endOffset + fixOffset;
            fixLayoutStartGap(startOffset3, recycler, state, false);
        } else {
            int fixOffset2 = fixLayoutStartGap(endOffset, recycler, state, true);
            int endOffset2 = startOffset + fixOffset2;
            fixLayoutEndGap(endOffset2, recycler, state, false);
        }
    }

    private int fixLayoutStartGap(int startOffset, RecyclerView.Recycler recycler, RecyclerView.State state, boolean canOffsetChildren) {
        int fixOffset;
        int gap;
        if (!isMainAxisDirectionHorizontal() && this.mIsRtl) {
            int gap2 = this.mOrientationHelper.getEndAfterPadding() - startOffset;
            if (gap2 <= 0) {
                return 0;
            }
            fixOffset = handleScrollingCrossAxis(-gap2, recycler, state);
        } else {
            int gap3 = startOffset - this.mOrientationHelper.getStartAfterPadding();
            if (gap3 <= 0) {
                return 0;
            }
            fixOffset = -handleScrollingCrossAxis(gap3, recycler, state);
        }
        int startOffset2 = startOffset + fixOffset;
        if (canOffsetChildren && (gap = startOffset2 - this.mOrientationHelper.getStartAfterPadding()) > 0) {
            this.mOrientationHelper.offsetChildren(-gap);
            return fixOffset - gap;
        }
        return fixOffset;
    }

    private int fixLayoutEndGap(int endOffset, RecyclerView.Recycler recycler, RecyclerView.State state, boolean canOffsetChildren) {
        int fixOffset;
        int gap;
        boolean columnAndRtl = !isMainAxisDirectionHorizontal() && this.mIsRtl;
        if (columnAndRtl) {
            int gap2 = endOffset - this.mOrientationHelper.getStartAfterPadding();
            if (gap2 <= 0) {
                return 0;
            }
            fixOffset = handleScrollingCrossAxis(gap2, recycler, state);
        } else {
            int gap3 = this.mOrientationHelper.getEndAfterPadding() - endOffset;
            if (gap3 <= 0) {
                return 0;
            }
            fixOffset = -handleScrollingCrossAxis(-gap3, recycler, state);
        }
        int endOffset2 = endOffset + fixOffset;
        if (canOffsetChildren && (gap = this.mOrientationHelper.getEndAfterPadding() - endOffset2) > 0) {
            this.mOrientationHelper.offsetChildren(gap);
            return gap + fixOffset;
        }
        return fixOffset;
    }

    private void updateFlexLines(int childCount) {
        boolean isMainSizeChanged;
        int i;
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), getWidthMode());
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), getHeightMode());
        int width = getWidth();
        int height = getHeight();
        boolean z = false;
        if (isMainAxisDirectionHorizontal()) {
            if (this.mLastWidth != Integer.MIN_VALUE && this.mLastWidth != width) {
                z = true;
            }
            isMainSizeChanged = z;
            i = this.mLayoutState.mInfinite ? this.mContext.getResources().getDisplayMetrics().heightPixels : this.mLayoutState.mAvailable;
        } else {
            if (this.mLastHeight != Integer.MIN_VALUE && this.mLastHeight != height) {
                z = true;
            }
            isMainSizeChanged = z;
            i = this.mLayoutState.mInfinite ? this.mContext.getResources().getDisplayMetrics().widthPixels : this.mLayoutState.mAvailable;
        }
        boolean isMainSizeChanged2 = isMainSizeChanged;
        int needsToFill = i;
        this.mLastWidth = width;
        this.mLastHeight = height;
        if (this.mDirtyPosition != -1 || (this.mPendingScrollPosition == -1 && !isMainSizeChanged2)) {
            int fromIndex = this.mDirtyPosition != -1 ? Math.min(this.mDirtyPosition, this.mAnchorInfo.mPosition) : this.mAnchorInfo.mPosition;
            this.mFlexLinesResult.reset();
            if (isMainAxisDirectionHorizontal()) {
                if (this.mFlexLines.size() <= 0) {
                    this.mFlexboxHelper.ensureIndexToFlexLine(childCount);
                    this.mFlexboxHelper.calculateHorizontalFlexLines(this.mFlexLinesResult, widthMeasureSpec, heightMeasureSpec, needsToFill, 0, this.mFlexLines);
                } else {
                    this.mFlexboxHelper.clearFlexLines(this.mFlexLines, fromIndex);
                    this.mFlexboxHelper.calculateFlexLines(this.mFlexLinesResult, widthMeasureSpec, heightMeasureSpec, needsToFill, fromIndex, this.mAnchorInfo.mPosition, this.mFlexLines);
                }
            } else if (this.mFlexLines.size() <= 0) {
                this.mFlexboxHelper.ensureIndexToFlexLine(childCount);
                this.mFlexboxHelper.calculateVerticalFlexLines(this.mFlexLinesResult, widthMeasureSpec, heightMeasureSpec, needsToFill, 0, this.mFlexLines);
            } else {
                this.mFlexboxHelper.clearFlexLines(this.mFlexLines, fromIndex);
                this.mFlexboxHelper.calculateFlexLines(this.mFlexLinesResult, heightMeasureSpec, widthMeasureSpec, needsToFill, fromIndex, this.mAnchorInfo.mPosition, this.mFlexLines);
            }
            this.mFlexLines = this.mFlexLinesResult.mFlexLines;
            this.mFlexboxHelper.determineMainSize(widthMeasureSpec, heightMeasureSpec, fromIndex);
            this.mFlexboxHelper.stretchViews(fromIndex);
            return;
        }
        if (this.mAnchorInfo.mLayoutFromEnd) {
            return;
        }
        this.mFlexLines.clear();
        this.mFlexLinesResult.reset();
        if (isMainAxisDirectionHorizontal()) {
            this.mFlexboxHelper.calculateHorizontalFlexLinesToIndex(this.mFlexLinesResult, widthMeasureSpec, heightMeasureSpec, needsToFill, this.mAnchorInfo.mPosition, this.mFlexLines);
        } else {
            this.mFlexboxHelper.calculateVerticalFlexLinesToIndex(this.mFlexLinesResult, widthMeasureSpec, heightMeasureSpec, needsToFill, this.mAnchorInfo.mPosition, this.mFlexLines);
        }
        this.mFlexLines = this.mFlexLinesResult.mFlexLines;
        this.mFlexboxHelper.determineMainSize(widthMeasureSpec, heightMeasureSpec);
        this.mFlexboxHelper.stretchViews();
        this.mAnchorInfo.mFlexLinePosition = this.mFlexboxHelper.mIndexToFlexLine[this.mAnchorInfo.mPosition];
        this.mLayoutState.mFlexLinePosition = this.mAnchorInfo.mFlexLinePosition;
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        this.mPendingSavedState = null;
        this.mPendingScrollPosition = -1;
        this.mPendingScrollPositionOffset = Integer.MIN_VALUE;
        this.mDirtyPosition = -1;
        this.mAnchorInfo.reset();
        this.mViewCache.clear();
    }

    private void resolveLayoutDirection() {
        int layoutDirection = getLayoutDirection();
        switch (this.mFlexDirection) {
            case 0:
                this.mIsRtl = layoutDirection == 1;
                this.mFromBottomToTop = this.mFlexWrap == 2;
                break;
            case 1:
                this.mIsRtl = layoutDirection != 1;
                this.mFromBottomToTop = this.mFlexWrap == 2;
                break;
            case 2:
                this.mIsRtl = layoutDirection == 1;
                if (this.mFlexWrap == 2) {
                    this.mIsRtl = !this.mIsRtl;
                }
                this.mFromBottomToTop = false;
                break;
            case 3:
                this.mIsRtl = layoutDirection == 1;
                if (this.mFlexWrap == 2) {
                    this.mIsRtl = !this.mIsRtl;
                }
                this.mFromBottomToTop = true;
                break;
            default:
                this.mIsRtl = false;
                this.mFromBottomToTop = false;
                break;
        }
    }

    private void updateAnchorInfoForLayout(RecyclerView.State state, AnchorInfo anchorInfo) {
        if (updateAnchorFromPendingState(state, anchorInfo, this.mPendingSavedState) || updateAnchorFromChildren(state, anchorInfo)) {
            return;
        }
        anchorInfo.assignCoordinateFromPadding();
        anchorInfo.mPosition = 0;
        anchorInfo.mFlexLinePosition = 0;
    }

    private boolean updateAnchorFromPendingState(RecyclerView.State state, AnchorInfo anchorInfo, SavedState savedState) {
        int decoratedStart;
        if (state.isPreLayout() || this.mPendingScrollPosition == -1) {
            return false;
        }
        if (this.mPendingScrollPosition < 0 || this.mPendingScrollPosition >= state.getItemCount()) {
            this.mPendingScrollPosition = -1;
            this.mPendingScrollPositionOffset = Integer.MIN_VALUE;
            return false;
        }
        anchorInfo.mPosition = this.mPendingScrollPosition;
        anchorInfo.mFlexLinePosition = this.mFlexboxHelper.mIndexToFlexLine[anchorInfo.mPosition];
        if (this.mPendingSavedState == null || !this.mPendingSavedState.hasValidAnchor(state.getItemCount())) {
            if (this.mPendingScrollPositionOffset == Integer.MIN_VALUE) {
                View anchorView = findViewByPosition(this.mPendingScrollPosition);
                if (anchorView != null) {
                    if (this.mOrientationHelper.getDecoratedMeasurement(anchorView) <= this.mOrientationHelper.getTotalSpace()) {
                        int startGap = this.mOrientationHelper.getDecoratedStart(anchorView) - this.mOrientationHelper.getStartAfterPadding();
                        if (startGap >= 0) {
                            int endGap = this.mOrientationHelper.getEndAfterPadding() - this.mOrientationHelper.getDecoratedEnd(anchorView);
                            if (endGap < 0) {
                                anchorInfo.mCoordinate = this.mOrientationHelper.getEndAfterPadding();
                                anchorInfo.mLayoutFromEnd = true;
                                return true;
                            }
                            if (anchorInfo.mLayoutFromEnd) {
                                decoratedStart = this.mOrientationHelper.getDecoratedEnd(anchorView) + this.mOrientationHelper.getTotalSpaceChange();
                            } else {
                                decoratedStart = this.mOrientationHelper.getDecoratedStart(anchorView);
                            }
                            anchorInfo.mCoordinate = decoratedStart;
                        } else {
                            anchorInfo.mCoordinate = this.mOrientationHelper.getStartAfterPadding();
                            anchorInfo.mLayoutFromEnd = false;
                            return true;
                        }
                    } else {
                        anchorInfo.assignCoordinateFromPadding();
                        return true;
                    }
                } else {
                    if (getChildCount() > 0) {
                        int position = getPosition(getChildAt(0));
                        anchorInfo.mLayoutFromEnd = this.mPendingScrollPosition < position;
                    }
                    anchorInfo.assignCoordinateFromPadding();
                }
                return true;
            }
            if (isMainAxisDirectionHorizontal() || !this.mIsRtl) {
                anchorInfo.mCoordinate = this.mOrientationHelper.getStartAfterPadding() + this.mPendingScrollPositionOffset;
            } else {
                anchorInfo.mCoordinate = this.mPendingScrollPositionOffset - this.mOrientationHelper.getEndPadding();
            }
            return true;
        }
        anchorInfo.mCoordinate = this.mOrientationHelper.getStartAfterPadding() + savedState.mAnchorOffset;
        anchorInfo.mAssignedFromSavedState = true;
        anchorInfo.mFlexLinePosition = -1;
        return true;
    }

    private boolean updateAnchorFromChildren(RecyclerView.State state, AnchorInfo anchorInfo) {
        View referenceChild;
        int startAfterPadding;
        if (getChildCount() == 0) {
            return false;
        }
        if (anchorInfo.mLayoutFromEnd) {
            referenceChild = findLastReferenceChild(state.getItemCount());
        } else {
            referenceChild = findFirstReferenceChild(state.getItemCount());
        }
        if (referenceChild == null) {
            return false;
        }
        anchorInfo.assignFromView(referenceChild);
        if (!state.isPreLayout() && supportsPredictiveItemAnimations()) {
            boolean notVisible = this.mOrientationHelper.getDecoratedStart(referenceChild) >= this.mOrientationHelper.getEndAfterPadding() || this.mOrientationHelper.getDecoratedEnd(referenceChild) < this.mOrientationHelper.getStartAfterPadding();
            if (notVisible) {
                if (anchorInfo.mLayoutFromEnd) {
                    startAfterPadding = this.mOrientationHelper.getEndAfterPadding();
                } else {
                    startAfterPadding = this.mOrientationHelper.getStartAfterPadding();
                }
                anchorInfo.mCoordinate = startAfterPadding;
            }
        }
        return true;
    }

    private View findFirstReferenceChild(int itemCount) {
        View firstFound = findReferenceChild(0, getChildCount(), itemCount);
        if (firstFound == null) {
            return null;
        }
        int firstFoundPosition = getPosition(firstFound);
        int firstFoundLinePosition = this.mFlexboxHelper.mIndexToFlexLine[firstFoundPosition];
        if (firstFoundLinePosition == -1) {
            return null;
        }
        FlexLine firstFoundLine = this.mFlexLines.get(firstFoundLinePosition);
        return findFirstReferenceViewInLine(firstFound, firstFoundLine);
    }

    private View findLastReferenceChild(int itemCount) {
        View lastFound = findReferenceChild(getChildCount() - 1, -1, itemCount);
        if (lastFound == null) {
            return null;
        }
        int lastFoundPosition = getPosition(lastFound);
        int lastFoundLinePosition = this.mFlexboxHelper.mIndexToFlexLine[lastFoundPosition];
        FlexLine lastFoundLine = this.mFlexLines.get(lastFoundLinePosition);
        return findLastReferenceViewInLine(lastFound, lastFoundLine);
    }

    private View findReferenceChild(int start, int end, int itemCount) {
        ensureOrientationHelper();
        ensureLayoutState();
        View outOfBoundsMatch = null;
        int boundStart = this.mOrientationHelper.getStartAfterPadding();
        int boundEnd = this.mOrientationHelper.getEndAfterPadding();
        int diff = end > start ? 1 : -1;
        View invalidMatch = null;
        for (int i = start; i != end; i += diff) {
            View view = getChildAt(i);
            int position = getPosition(view);
            if (position >= 0 && position < itemCount) {
                if (((RecyclerView.LayoutParams) view.getLayoutParams()).isItemRemoved()) {
                    if (invalidMatch == null) {
                        invalidMatch = view;
                    }
                } else if (this.mOrientationHelper.getDecoratedStart(view) < boundStart || this.mOrientationHelper.getDecoratedEnd(view) > boundEnd) {
                    if (outOfBoundsMatch == null) {
                        outOfBoundsMatch = view;
                    }
                } else {
                    return view;
                }
            }
        }
        return outOfBoundsMatch != null ? outOfBoundsMatch : invalidMatch;
    }

    private View getChildClosestToStart() {
        return getChildAt(0);
    }

    private int fill(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutState layoutState) {
        if (layoutState.mScrollingOffset != Integer.MIN_VALUE) {
            if (layoutState.mAvailable < 0) {
                layoutState.mScrollingOffset += layoutState.mAvailable;
            }
            recycleByLayoutState(recycler, layoutState);
        }
        int start = layoutState.mAvailable;
        int remainingSpace = layoutState.mAvailable;
        int consumed = 0;
        boolean mainAxisHorizontal = isMainAxisDirectionHorizontal();
        while (true) {
            if ((remainingSpace <= 0 && !this.mLayoutState.mInfinite) || !layoutState.hasMore(state, this.mFlexLines)) {
                break;
            }
            FlexLine flexLine = this.mFlexLines.get(layoutState.mFlexLinePosition);
            layoutState.mPosition = flexLine.mFirstIndex;
            consumed += layoutFlexLine(flexLine, layoutState);
            if (mainAxisHorizontal || !this.mIsRtl) {
                layoutState.mOffset += flexLine.getCrossSize() * layoutState.mLayoutDirection;
            } else {
                layoutState.mOffset -= flexLine.getCrossSize() * layoutState.mLayoutDirection;
            }
            remainingSpace -= flexLine.getCrossSize();
        }
        layoutState.mAvailable -= consumed;
        if (layoutState.mScrollingOffset != Integer.MIN_VALUE) {
            layoutState.mScrollingOffset += consumed;
            if (layoutState.mAvailable < 0) {
                layoutState.mScrollingOffset += layoutState.mAvailable;
            }
            recycleByLayoutState(recycler, layoutState);
        }
        return start - layoutState.mAvailable;
    }

    private void recycleByLayoutState(RecyclerView.Recycler recycler, LayoutState layoutState) {
        if (!layoutState.mShouldRecycle) {
            return;
        }
        if (layoutState.mLayoutDirection == -1) {
            recycleFlexLinesFromEnd(recycler, layoutState);
        } else {
            recycleFlexLinesFromStart(recycler, layoutState);
        }
    }

    private void recycleFlexLinesFromStart(RecyclerView.Recycler recycler, LayoutState layoutState) {
        int childCount;
        if (layoutState.mScrollingOffset < 0 || (childCount = getChildCount()) == 0) {
            return;
        }
        View firstView = getChildAt(0);
        int currentLineIndex = this.mFlexboxHelper.mIndexToFlexLine[getPosition(firstView)];
        if (currentLineIndex == -1) {
            return;
        }
        FlexLine flexLine = this.mFlexLines.get(currentLineIndex);
        int recycleTo = -1;
        int currentLineIndex2 = currentLineIndex;
        for (int currentLineIndex3 = 0; currentLineIndex3 < childCount; currentLineIndex3++) {
            View view = getChildAt(currentLineIndex3);
            if (!canViewBeRecycledFromStart(view, layoutState.mScrollingOffset)) {
                break;
            }
            if (flexLine.mLastIndex == getPosition(view)) {
                recycleTo = currentLineIndex3;
                if (currentLineIndex2 >= this.mFlexLines.size() - 1) {
                    break;
                }
                currentLineIndex2 += layoutState.mLayoutDirection;
                FlexLine flexLine2 = this.mFlexLines.get(currentLineIndex2);
                flexLine = flexLine2;
            }
        }
        recycleChildren(recycler, 0, recycleTo);
    }

    private boolean canViewBeRecycledFromStart(View view, int scrollingOffset) {
        return (isMainAxisDirectionHorizontal() || !this.mIsRtl) ? this.mOrientationHelper.getDecoratedEnd(view) <= scrollingOffset : this.mOrientationHelper.getEnd() - this.mOrientationHelper.getDecoratedStart(view) <= scrollingOffset;
    }

    private void recycleFlexLinesFromEnd(RecyclerView.Recycler recycler, LayoutState layoutState) {
        if (layoutState.mScrollingOffset < 0) {
            return;
        }
        int end = this.mOrientationHelper.getEnd() - layoutState.mScrollingOffset;
        int childCount = getChildCount();
        if (childCount == 0) {
            return;
        }
        View lastView = getChildAt(childCount - 1);
        int currentLineIndex = this.mFlexboxHelper.mIndexToFlexLine[getPosition(lastView)];
        if (currentLineIndex == -1) {
            return;
        }
        int recycleTo = childCount - 1;
        int recycleFrom = childCount;
        FlexLine flexLine = this.mFlexLines.get(currentLineIndex);
        for (int i = childCount - 1; i >= 0; i--) {
            View view = getChildAt(i);
            if (!canViewBeRecycledFromEnd(view, layoutState.mScrollingOffset)) {
                break;
            }
            if (flexLine.mFirstIndex == getPosition(view)) {
                recycleFrom = i;
                if (currentLineIndex <= 0) {
                    break;
                }
                currentLineIndex += layoutState.mLayoutDirection;
                FlexLine flexLine2 = this.mFlexLines.get(currentLineIndex);
                flexLine = flexLine2;
            }
        }
        recycleChildren(recycler, recycleFrom, recycleTo);
    }

    private boolean canViewBeRecycledFromEnd(View view, int scrollingOffset) {
        return (isMainAxisDirectionHorizontal() || !this.mIsRtl) ? this.mOrientationHelper.getDecoratedStart(view) >= this.mOrientationHelper.getEnd() - scrollingOffset : this.mOrientationHelper.getDecoratedEnd(view) <= scrollingOffset;
    }

    private void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        for (int i = endIndex; i >= startIndex; i--) {
            removeAndRecycleViewAt(i, recycler);
        }
    }

    private int layoutFlexLine(FlexLine flexLine, LayoutState layoutState) {
        if (isMainAxisDirectionHorizontal()) {
            return layoutFlexLineMainAxisHorizontal(flexLine, layoutState);
        }
        return layoutFlexLineMainAxisVertical(flexLine, layoutState);
    }

    private int layoutFlexLineMainAxisHorizontal(FlexLine flexLine, LayoutState layoutState) {
        float childLeft;
        float childRight;
        int paddingLeft;
        float childRight2;
        int i;
        int paddingRight;
        int parentWidth;
        float childLeft2;
        View view;
        int paddingLeft2 = getPaddingLeft();
        int paddingRight2 = getPaddingRight();
        int parentWidth2 = getWidth();
        int childTop = layoutState.mOffset;
        if (layoutState.mLayoutDirection == -1) {
            childTop -= flexLine.mCrossSize;
        }
        int childTop2 = childTop;
        int startPosition = layoutState.mPosition;
        float spaceBetweenItem = 0.0f;
        int i2 = 1;
        switch (this.mJustifyContent) {
            case 0:
                childLeft = paddingLeft2;
                childRight = parentWidth2 - paddingRight2;
                break;
            case 1:
                childLeft = (parentWidth2 - flexLine.mMainSize) + paddingRight2;
                childRight = flexLine.mMainSize - paddingLeft2;
                break;
            case 2:
                float childLeft3 = paddingLeft2;
                childLeft = childLeft3 + ((parentWidth2 - flexLine.mMainSize) / 2.0f);
                childRight = (parentWidth2 - paddingRight2) - ((parentWidth2 - flexLine.mMainSize) / 2.0f);
                break;
            case 3:
                childLeft = paddingLeft2;
                float denominator = flexLine.mItemCount != 1 ? flexLine.mItemCount - 1 : 1.0f;
                spaceBetweenItem = (parentWidth2 - flexLine.mMainSize) / denominator;
                childRight = parentWidth2 - paddingRight2;
                break;
            case 4:
                if (flexLine.mItemCount != 0) {
                    spaceBetweenItem = (parentWidth2 - flexLine.mMainSize) / flexLine.mItemCount;
                }
                childLeft = paddingLeft2 + (spaceBetweenItem / 2.0f);
                childRight = (parentWidth2 - paddingRight2) - (spaceBetweenItem / 2.0f);
                break;
            default:
                throw new IllegalStateException("Invalid justifyContent is set: " + this.mJustifyContent);
        }
        float childLeft4 = childLeft - this.mAnchorInfo.mPerpendicularCoordinate;
        float childRight3 = childRight - this.mAnchorInfo.mPerpendicularCoordinate;
        float spaceBetweenItem2 = Math.max(spaceBetweenItem, 0.0f);
        int indexInFlexLine = 0;
        int itemCount = flexLine.getItemCount();
        int i3 = startPosition;
        while (true) {
            int itemCount2 = itemCount;
            if (i3 < startPosition + itemCount2) {
                View view2 = getFlexItemAt(i3);
                if (view2 == null) {
                    i = i3;
                    paddingLeft = paddingLeft2;
                    paddingRight = paddingRight2;
                    parentWidth = parentWidth2;
                } else {
                    if (layoutState.mLayoutDirection == i2) {
                        calculateItemDecorationsForChild(view2, TEMP_RECT);
                        addView(view2);
                    } else {
                        calculateItemDecorationsForChild(view2, TEMP_RECT);
                        addView(view2, indexInFlexLine);
                        indexInFlexLine++;
                    }
                    int indexInFlexLine2 = indexInFlexLine;
                    long measureSpec = this.mFlexboxHelper.mMeasureSpecCache[i3];
                    int widthSpec = this.mFlexboxHelper.extractLowerInt(measureSpec);
                    int heightSpec = this.mFlexboxHelper.extractHigherInt(measureSpec);
                    paddingLeft = paddingLeft2;
                    LayoutParams lp = (LayoutParams) view2.getLayoutParams();
                    if (shouldMeasureChild(view2, widthSpec, heightSpec, lp)) {
                        view2.measure(widthSpec, heightSpec);
                    }
                    float childLeft5 = childLeft4 + lp.leftMargin + getLeftDecorationWidth(view2);
                    float childRight4 = childRight3 - (lp.rightMargin + getRightDecorationWidth(view2));
                    int topWithDecoration = childTop2 + getTopDecorationHeight(view2);
                    if (this.mIsRtl) {
                        FlexboxHelper flexboxHelper = this.mFlexboxHelper;
                        int i4 = Math.round(childRight4) - view2.getMeasuredWidth();
                        int iRound = Math.round(childRight4);
                        int i5 = topWithDecoration + view2.getMeasuredHeight();
                        paddingRight = paddingRight2;
                        childLeft2 = childLeft5;
                        childRight2 = childRight4;
                        parentWidth = parentWidth2;
                        view = view2;
                        i = i3;
                        flexboxHelper.layoutSingleChildHorizontal(view2, flexLine, i4, topWithDecoration, iRound, i5);
                    } else {
                        childRight2 = childRight4;
                        i = i3;
                        paddingRight = paddingRight2;
                        parentWidth = parentWidth2;
                        childLeft2 = childLeft5;
                        view = view2;
                        this.mFlexboxHelper.layoutSingleChildHorizontal(view, flexLine, Math.round(childLeft2), topWithDecoration, Math.round(childLeft2) + view.getMeasuredWidth(), topWithDecoration + view.getMeasuredHeight());
                    }
                    childLeft4 = childLeft2 + view.getMeasuredWidth() + lp.rightMargin + getRightDecorationWidth(view) + spaceBetweenItem2;
                    childRight3 = childRight2 - (((view.getMeasuredWidth() + lp.leftMargin) + getLeftDecorationWidth(view)) + spaceBetweenItem2);
                    indexInFlexLine = indexInFlexLine2;
                }
                i3 = i + 1;
                itemCount = itemCount2;
                paddingLeft2 = paddingLeft;
                paddingRight2 = paddingRight;
                parentWidth2 = parentWidth;
                i2 = 1;
            } else {
                layoutState.mFlexLinePosition += this.mLayoutState.mLayoutDirection;
                return flexLine.getCrossSize();
            }
        }
    }

    private int layoutFlexLineMainAxisVertical(FlexLine flexLine, LayoutState layoutState) {
        float childTop;
        float childBottom;
        int i;
        int paddingTop;
        boolean z;
        int paddingBottom;
        int parentHeight;
        int childLeft;
        float childBottom2;
        float childTop2;
        View view;
        int paddingTop2 = getPaddingTop();
        int paddingBottom2 = getPaddingBottom();
        int parentHeight2 = getHeight();
        int childLeft2 = layoutState.mOffset;
        int childRight = layoutState.mOffset;
        if (layoutState.mLayoutDirection == -1) {
            childLeft2 -= flexLine.mCrossSize;
            childRight += flexLine.mCrossSize;
        }
        int childLeft3 = childLeft2;
        int childRight2 = childRight;
        int startPosition = layoutState.mPosition;
        float spaceBetweenItem = 0.0f;
        boolean z2 = true;
        switch (this.mJustifyContent) {
            case 0:
                childTop = paddingTop2;
                childBottom = parentHeight2 - paddingBottom2;
                break;
            case 1:
                childTop = (parentHeight2 - flexLine.mMainSize) + paddingBottom2;
                childBottom = flexLine.mMainSize - paddingTop2;
                break;
            case 2:
                float childTop3 = paddingTop2;
                childTop = childTop3 + ((parentHeight2 - flexLine.mMainSize) / 2.0f);
                childBottom = (parentHeight2 - paddingBottom2) - ((parentHeight2 - flexLine.mMainSize) / 2.0f);
                break;
            case 3:
                childTop = paddingTop2;
                float denominator = flexLine.mItemCount != 1 ? flexLine.mItemCount - 1 : 1.0f;
                spaceBetweenItem = (parentHeight2 - flexLine.mMainSize) / denominator;
                childBottom = parentHeight2 - paddingBottom2;
                break;
            case 4:
                if (flexLine.mItemCount != 0) {
                    spaceBetweenItem = (parentHeight2 - flexLine.mMainSize) / flexLine.mItemCount;
                }
                childTop = paddingTop2 + (spaceBetweenItem / 2.0f);
                childBottom = (parentHeight2 - paddingBottom2) - (spaceBetweenItem / 2.0f);
                break;
            default:
                throw new IllegalStateException("Invalid justifyContent is set: " + this.mJustifyContent);
        }
        float childTop4 = childTop - this.mAnchorInfo.mPerpendicularCoordinate;
        float childBottom3 = childBottom - this.mAnchorInfo.mPerpendicularCoordinate;
        float spaceBetweenItem2 = Math.max(spaceBetweenItem, 0.0f);
        int indexInFlexLine = 0;
        int itemCount = flexLine.getItemCount();
        int i2 = startPosition;
        while (true) {
            int itemCount2 = itemCount;
            if (i2 < startPosition + itemCount2) {
                View view2 = getFlexItemAt(i2);
                if (view2 == null) {
                    i = i2;
                    z = z2;
                    paddingTop = paddingTop2;
                    paddingBottom = paddingBottom2;
                    parentHeight = parentHeight2;
                    childLeft = childLeft3;
                } else {
                    long measureSpec = this.mFlexboxHelper.mMeasureSpecCache[i2];
                    int widthSpec = this.mFlexboxHelper.extractLowerInt(measureSpec);
                    i = i2;
                    int heightSpec = this.mFlexboxHelper.extractHigherInt(measureSpec);
                    paddingTop = paddingTop2;
                    LayoutParams lp = (LayoutParams) view2.getLayoutParams();
                    if (shouldMeasureChild(view2, widthSpec, heightSpec, lp)) {
                        view2.measure(widthSpec, heightSpec);
                    }
                    float childTop5 = childTop4 + lp.topMargin + getTopDecorationHeight(view2);
                    float childBottom4 = childBottom3 - (lp.rightMargin + getBottomDecorationHeight(view2));
                    if (layoutState.mLayoutDirection == 1) {
                        calculateItemDecorationsForChild(view2, TEMP_RECT);
                        addView(view2);
                    } else {
                        calculateItemDecorationsForChild(view2, TEMP_RECT);
                        addView(view2, indexInFlexLine);
                        indexInFlexLine++;
                    }
                    int indexInFlexLine2 = indexInFlexLine;
                    int indexInFlexLine3 = getLeftDecorationWidth(view2);
                    int leftWithDecoration = childLeft3 + indexInFlexLine3;
                    int rightWithDecoration = childRight2 - getRightDecorationWidth(view2);
                    if (this.mIsRtl) {
                        if (this.mFromBottomToTop) {
                            paddingBottom = paddingBottom2;
                            childBottom2 = childBottom4;
                            parentHeight = parentHeight2;
                            childTop2 = childTop5;
                            childLeft = childLeft3;
                            view = view2;
                            z = true;
                            this.mFlexboxHelper.layoutSingleChildVertical(view2, flexLine, this.mIsRtl, rightWithDecoration - view2.getMeasuredWidth(), Math.round(childBottom4) - view2.getMeasuredHeight(), rightWithDecoration, Math.round(childBottom4));
                        } else {
                            z = true;
                            paddingBottom = paddingBottom2;
                            parentHeight = parentHeight2;
                            childLeft = childLeft3;
                            childBottom2 = childBottom4;
                            childTop2 = childTop5;
                            view = view2;
                            this.mFlexboxHelper.layoutSingleChildVertical(view, flexLine, this.mIsRtl, rightWithDecoration - view.getMeasuredWidth(), Math.round(childTop2), rightWithDecoration, Math.round(childTop2) + view.getMeasuredHeight());
                        }
                    } else {
                        z = true;
                        paddingBottom = paddingBottom2;
                        parentHeight = parentHeight2;
                        childLeft = childLeft3;
                        childBottom2 = childBottom4;
                        childTop2 = childTop5;
                        view = view2;
                        if (this.mFromBottomToTop) {
                            this.mFlexboxHelper.layoutSingleChildVertical(view, flexLine, this.mIsRtl, leftWithDecoration, Math.round(childBottom2) - view.getMeasuredHeight(), leftWithDecoration + view.getMeasuredWidth(), Math.round(childBottom2));
                        } else {
                            this.mFlexboxHelper.layoutSingleChildVertical(view, flexLine, this.mIsRtl, leftWithDecoration, Math.round(childTop2), leftWithDecoration + view.getMeasuredWidth(), Math.round(childTop2) + view.getMeasuredHeight());
                        }
                    }
                    childBottom3 = childBottom2 - (((view.getMeasuredHeight() + lp.bottomMargin) + getTopDecorationHeight(view)) + spaceBetweenItem2);
                    childTop4 = childTop2 + view.getMeasuredHeight() + lp.topMargin + getBottomDecorationHeight(view) + spaceBetweenItem2;
                    indexInFlexLine = indexInFlexLine2;
                }
                i2 = i + 1;
                itemCount = itemCount2;
                paddingTop2 = paddingTop;
                z2 = z;
                paddingBottom2 = paddingBottom;
                parentHeight2 = parentHeight;
                childLeft3 = childLeft;
            } else {
                layoutState.mFlexLinePosition += this.mLayoutState.mLayoutDirection;
                return flexLine.getCrossSize();
            }
        }
    }

    @Override // com.google.android.flexbox.FlexContainer
    public boolean isMainAxisDirectionHorizontal() {
        return this.mFlexDirection == 0 || this.mFlexDirection == 1;
    }

    private void updateLayoutStateToFillEnd(AnchorInfo anchorInfo, boolean fromNextLine, boolean considerInfinite) {
        if (considerInfinite) {
            resolveInfiniteAmount();
        } else {
            this.mLayoutState.mInfinite = false;
        }
        if (!isMainAxisDirectionHorizontal() && this.mIsRtl) {
            this.mLayoutState.mAvailable = anchorInfo.mCoordinate - getPaddingRight();
        } else {
            this.mLayoutState.mAvailable = this.mOrientationHelper.getEndAfterPadding() - anchorInfo.mCoordinate;
        }
        this.mLayoutState.mPosition = anchorInfo.mPosition;
        this.mLayoutState.mItemDirection = 1;
        this.mLayoutState.mLayoutDirection = 1;
        this.mLayoutState.mOffset = anchorInfo.mCoordinate;
        this.mLayoutState.mScrollingOffset = Integer.MIN_VALUE;
        this.mLayoutState.mFlexLinePosition = anchorInfo.mFlexLinePosition;
        if (!fromNextLine || this.mFlexLines.size() <= 1 || anchorInfo.mFlexLinePosition < 0 || anchorInfo.mFlexLinePosition >= this.mFlexLines.size() - 1) {
            return;
        }
        FlexLine currentLine = this.mFlexLines.get(anchorInfo.mFlexLinePosition);
        LayoutState.access$1508(this.mLayoutState);
        this.mLayoutState.mPosition += currentLine.getItemCount();
    }

    private void updateLayoutStateToFillStart(AnchorInfo anchorInfo, boolean fromPreviousLine, boolean considerInfinite) {
        if (considerInfinite) {
            resolveInfiniteAmount();
        } else {
            this.mLayoutState.mInfinite = false;
        }
        if (!isMainAxisDirectionHorizontal() && this.mIsRtl) {
            this.mLayoutState.mAvailable = (this.mParent.getWidth() - anchorInfo.mCoordinate) - this.mOrientationHelper.getStartAfterPadding();
        } else {
            this.mLayoutState.mAvailable = anchorInfo.mCoordinate - this.mOrientationHelper.getStartAfterPadding();
        }
        this.mLayoutState.mPosition = anchorInfo.mPosition;
        this.mLayoutState.mItemDirection = 1;
        this.mLayoutState.mLayoutDirection = -1;
        this.mLayoutState.mOffset = anchorInfo.mCoordinate;
        this.mLayoutState.mScrollingOffset = Integer.MIN_VALUE;
        this.mLayoutState.mFlexLinePosition = anchorInfo.mFlexLinePosition;
        if (!fromPreviousLine || anchorInfo.mFlexLinePosition <= 0 || this.mFlexLines.size() <= anchorInfo.mFlexLinePosition) {
            return;
        }
        FlexLine currentLine = this.mFlexLines.get(anchorInfo.mFlexLinePosition);
        LayoutState.access$1510(this.mLayoutState);
        this.mLayoutState.mPosition -= currentLine.getItemCount();
    }

    private void resolveInfiniteAmount() {
        int crossMode;
        if (isMainAxisDirectionHorizontal()) {
            crossMode = getHeightMode();
        } else {
            crossMode = getWidthMode();
        }
        this.mLayoutState.mInfinite = crossMode == 0 || crossMode == Integer.MIN_VALUE;
    }

    private void ensureOrientationHelper() {
        if (this.mOrientationHelper != null) {
            return;
        }
        if (isMainAxisDirectionHorizontal()) {
            if (this.mFlexWrap == 0) {
                this.mOrientationHelper = OrientationHelper.createHorizontalHelper(this);
                this.mSubOrientationHelper = OrientationHelper.createVerticalHelper(this);
                return;
            } else {
                this.mOrientationHelper = OrientationHelper.createVerticalHelper(this);
                this.mSubOrientationHelper = OrientationHelper.createHorizontalHelper(this);
                return;
            }
        }
        if (this.mFlexWrap == 0) {
            this.mOrientationHelper = OrientationHelper.createVerticalHelper(this);
            this.mSubOrientationHelper = OrientationHelper.createHorizontalHelper(this);
        } else {
            this.mOrientationHelper = OrientationHelper.createHorizontalHelper(this);
            this.mSubOrientationHelper = OrientationHelper.createVerticalHelper(this);
        }
    }

    private void ensureLayoutState() {
        if (this.mLayoutState == null) {
            this.mLayoutState = new LayoutState();
        }
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void scrollToPosition(int position) {
        this.mPendingScrollPosition = position;
        this.mPendingScrollPositionOffset = Integer.MIN_VALUE;
        if (this.mPendingSavedState != null) {
            this.mPendingSavedState.invalidateAnchor();
        }
        requestLayout();
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onAttachedToWindow(RecyclerView recyclerView) {
        super.onAttachedToWindow(recyclerView);
        this.mParent = (View) recyclerView.getParent();
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        if (this.mRecycleChildrenOnDetach) {
            removeAndRecycleAllViews(recycler);
            recycler.clear();
        }
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public boolean canScrollHorizontally() {
        return !isMainAxisDirectionHorizontal() || getWidth() > this.mParent.getWidth();
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public boolean canScrollVertically() {
        return isMainAxisDirectionHorizontal() || getHeight() > this.mParent.getHeight();
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (!isMainAxisDirectionHorizontal()) {
            int scrolled = handleScrollingCrossAxis(dx, recycler, state);
            this.mViewCache.clear();
            return scrolled;
        }
        int scrolled2 = handleScrollingMainAxis(dx);
        this.mAnchorInfo.mPerpendicularCoordinate += scrolled2;
        this.mSubOrientationHelper.offsetChildren(-scrolled2);
        return scrolled2;
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (isMainAxisDirectionHorizontal()) {
            int scrolled = handleScrollingCrossAxis(dy, recycler, state);
            this.mViewCache.clear();
            return scrolled;
        }
        int scrolled2 = handleScrollingMainAxis(dy);
        this.mAnchorInfo.mPerpendicularCoordinate += scrolled2;
        this.mSubOrientationHelper.offsetChildren(-scrolled2);
        return scrolled2;
    }

    private int handleScrollingCrossAxis(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scrolled;
        if (getChildCount() == 0 || delta == 0) {
            return 0;
        }
        ensureOrientationHelper();
        int layoutDirection = 1;
        this.mLayoutState.mShouldRecycle = true;
        boolean columnAndRtl = !isMainAxisDirectionHorizontal() && this.mIsRtl;
        if (columnAndRtl) {
            if (delta >= 0) {
                layoutDirection = -1;
            }
        } else if (delta <= 0) {
            layoutDirection = -1;
        }
        int absDelta = Math.abs(delta);
        updateLayoutState(layoutDirection, absDelta);
        int freeScroll = this.mLayoutState.mScrollingOffset;
        int consumed = fill(recycler, state, this.mLayoutState) + freeScroll;
        if (consumed < 0) {
            return 0;
        }
        if (columnAndRtl) {
            scrolled = absDelta > consumed ? (-layoutDirection) * consumed : delta;
        } else {
            scrolled = absDelta > consumed ? layoutDirection * consumed : delta;
        }
        this.mOrientationHelper.offsetChildren(-scrolled);
        this.mLayoutState.mLastScrollDelta = scrolled;
        return scrolled;
    }

    private int handleScrollingMainAxis(int delta) {
        int delta2;
        if (getChildCount() == 0 || delta == 0) {
            return 0;
        }
        ensureOrientationHelper();
        boolean isMainAxisHorizontal = isMainAxisDirectionHorizontal();
        int parentLength = isMainAxisHorizontal ? this.mParent.getWidth() : this.mParent.getHeight();
        int mainAxisLength = isMainAxisHorizontal ? getWidth() : getHeight();
        boolean layoutRtl = getLayoutDirection() == 1;
        if (layoutRtl) {
            int absDelta = Math.abs(delta);
            if (delta < 0) {
                return -Math.min((this.mAnchorInfo.mPerpendicularCoordinate + mainAxisLength) - parentLength, absDelta);
            }
            return this.mAnchorInfo.mPerpendicularCoordinate + delta > 0 ? -this.mAnchorInfo.mPerpendicularCoordinate : delta;
        }
        if (delta > 0) {
            return Math.min((mainAxisLength - this.mAnchorInfo.mPerpendicularCoordinate) - parentLength, delta);
        }
        if (this.mAnchorInfo.mPerpendicularCoordinate + delta >= 0) {
            delta2 = delta;
        } else {
            delta2 = -this.mAnchorInfo.mPerpendicularCoordinate;
        }
        return delta2;
    }

    private void updateLayoutState(int layoutDirection, int absDelta) {
        int i;
        int i2;
        this.mLayoutState.mLayoutDirection = layoutDirection;
        boolean mainAxisHorizontal = isMainAxisDirectionHorizontal();
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), getWidthMode());
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), getHeightMode());
        boolean columnAndRtl = !mainAxisHorizontal && this.mIsRtl;
        if (layoutDirection == 1) {
            View lastVisible = getChildAt(getChildCount() - 1);
            this.mLayoutState.mOffset = this.mOrientationHelper.getDecoratedEnd(lastVisible);
            int lastVisiblePosition = getPosition(lastVisible);
            int lastVisibleLinePosition = this.mFlexboxHelper.mIndexToFlexLine[lastVisiblePosition];
            FlexLine lastVisibleLine = this.mFlexLines.get(lastVisibleLinePosition);
            View referenceView = findLastReferenceViewInLine(lastVisible, lastVisibleLine);
            this.mLayoutState.mItemDirection = 1;
            this.mLayoutState.mPosition = this.mLayoutState.mItemDirection + lastVisiblePosition;
            if (this.mFlexboxHelper.mIndexToFlexLine.length <= this.mLayoutState.mPosition) {
                this.mLayoutState.mFlexLinePosition = -1;
            } else {
                this.mLayoutState.mFlexLinePosition = this.mFlexboxHelper.mIndexToFlexLine[this.mLayoutState.mPosition];
            }
            if (columnAndRtl) {
                this.mLayoutState.mOffset = this.mOrientationHelper.getDecoratedStart(referenceView);
                this.mLayoutState.mScrollingOffset = (-this.mOrientationHelper.getDecoratedStart(referenceView)) + this.mOrientationHelper.getStartAfterPadding();
                LayoutState layoutState = this.mLayoutState;
                if (this.mLayoutState.mScrollingOffset < 0) {
                    i2 = 0;
                } else {
                    i2 = this.mLayoutState.mScrollingOffset;
                }
                layoutState.mScrollingOffset = i2;
            } else {
                this.mLayoutState.mOffset = this.mOrientationHelper.getDecoratedEnd(referenceView);
                this.mLayoutState.mScrollingOffset = this.mOrientationHelper.getDecoratedEnd(referenceView) - this.mOrientationHelper.getEndAfterPadding();
            }
            if ((this.mLayoutState.mFlexLinePosition == -1 || this.mLayoutState.mFlexLinePosition > this.mFlexLines.size() - 1) && this.mLayoutState.mPosition <= getFlexItemCount()) {
                int needsToFill = absDelta - this.mLayoutState.mScrollingOffset;
                this.mFlexLinesResult.reset();
                if (needsToFill > 0) {
                    if (mainAxisHorizontal) {
                        FlexboxHelper flexboxHelper = this.mFlexboxHelper;
                        FlexboxHelper.FlexLinesResult flexLinesResult = this.mFlexLinesResult;
                        int lastVisibleLinePosition2 = this.mLayoutState.mPosition;
                        flexboxHelper.calculateHorizontalFlexLines(flexLinesResult, widthMeasureSpec, heightMeasureSpec, needsToFill, lastVisibleLinePosition2, this.mFlexLines);
                    } else {
                        this.mFlexboxHelper.calculateVerticalFlexLines(this.mFlexLinesResult, widthMeasureSpec, heightMeasureSpec, needsToFill, this.mLayoutState.mPosition, this.mFlexLines);
                    }
                    this.mFlexboxHelper.determineMainSize(widthMeasureSpec, heightMeasureSpec, this.mLayoutState.mPosition);
                    this.mFlexboxHelper.stretchViews(this.mLayoutState.mPosition);
                }
            }
        } else {
            View firstVisible = getChildAt(0);
            this.mLayoutState.mOffset = this.mOrientationHelper.getDecoratedStart(firstVisible);
            int firstVisiblePosition = getPosition(firstVisible);
            int firstVisibleLinePosition = this.mFlexboxHelper.mIndexToFlexLine[firstVisiblePosition];
            FlexLine firstVisibleLine = this.mFlexLines.get(firstVisibleLinePosition);
            View referenceView2 = findFirstReferenceViewInLine(firstVisible, firstVisibleLine);
            this.mLayoutState.mItemDirection = 1;
            int flexLinePosition = this.mFlexboxHelper.mIndexToFlexLine[firstVisiblePosition];
            if (flexLinePosition == -1) {
                flexLinePosition = 0;
            }
            if (flexLinePosition > 0) {
                FlexLine previousLine = this.mFlexLines.get(flexLinePosition - 1);
                this.mLayoutState.mPosition = firstVisiblePosition - previousLine.getItemCount();
            } else {
                this.mLayoutState.mPosition = -1;
            }
            this.mLayoutState.mFlexLinePosition = flexLinePosition > 0 ? flexLinePosition - 1 : 0;
            if (columnAndRtl) {
                this.mLayoutState.mOffset = this.mOrientationHelper.getDecoratedEnd(referenceView2);
                this.mLayoutState.mScrollingOffset = this.mOrientationHelper.getDecoratedEnd(referenceView2) - this.mOrientationHelper.getEndAfterPadding();
                LayoutState layoutState2 = this.mLayoutState;
                if (this.mLayoutState.mScrollingOffset < 0) {
                    i = 0;
                } else {
                    i = this.mLayoutState.mScrollingOffset;
                }
                layoutState2.mScrollingOffset = i;
            } else {
                this.mLayoutState.mOffset = this.mOrientationHelper.getDecoratedStart(referenceView2);
                this.mLayoutState.mScrollingOffset = (-this.mOrientationHelper.getDecoratedStart(referenceView2)) + this.mOrientationHelper.getStartAfterPadding();
            }
        }
        this.mLayoutState.mAvailable = absDelta - this.mLayoutState.mScrollingOffset;
    }

    private View findFirstReferenceViewInLine(View firstView, FlexLine firstVisibleLine) {
        boolean mainAxisHorizontal = isMainAxisDirectionHorizontal();
        View referenceView = firstView;
        int to = firstVisibleLine.mItemCount;
        for (int i = 1; i < to; i++) {
            View viewInSameLine = getChildAt(i);
            if (viewInSameLine != null && viewInSameLine.getVisibility() != 8) {
                if (this.mIsRtl && !mainAxisHorizontal) {
                    if (this.mOrientationHelper.getDecoratedEnd(referenceView) < this.mOrientationHelper.getDecoratedEnd(viewInSameLine)) {
                        referenceView = viewInSameLine;
                    }
                } else if (this.mOrientationHelper.getDecoratedStart(referenceView) > this.mOrientationHelper.getDecoratedStart(viewInSameLine)) {
                    referenceView = viewInSameLine;
                }
            }
        }
        return referenceView;
    }

    private View findLastReferenceViewInLine(View lastView, FlexLine lastVisibleLine) {
        boolean mainAxisHorizontal = isMainAxisDirectionHorizontal();
        View referenceView = lastView;
        int to = (getChildCount() - lastVisibleLine.mItemCount) - 1;
        for (int i = getChildCount() - 2; i > to; i--) {
            View viewInSameLine = getChildAt(i);
            if (viewInSameLine != null && viewInSameLine.getVisibility() != 8) {
                if (this.mIsRtl && !mainAxisHorizontal) {
                    if (this.mOrientationHelper.getDecoratedStart(referenceView) > this.mOrientationHelper.getDecoratedStart(viewInSameLine)) {
                        referenceView = viewInSameLine;
                    }
                } else if (this.mOrientationHelper.getDecoratedEnd(referenceView) < this.mOrientationHelper.getDecoratedEnd(viewInSameLine)) {
                    referenceView = viewInSameLine;
                }
            }
        }
        return referenceView;
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public int computeHorizontalScrollExtent(RecyclerView.State state) {
        int scrollExtent = computeScrollExtent(state);
        return scrollExtent;
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        int scrollExtent = computeScrollExtent(state);
        return scrollExtent;
    }

    private int computeScrollExtent(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }
        int allChildrenCount = state.getItemCount();
        ensureOrientationHelper();
        View firstReferenceView = findFirstReferenceChild(allChildrenCount);
        View lastReferenceView = findLastReferenceChild(allChildrenCount);
        if (state.getItemCount() == 0 || firstReferenceView == null || lastReferenceView == null) {
            return 0;
        }
        int extend = this.mOrientationHelper.getDecoratedEnd(lastReferenceView) - this.mOrientationHelper.getDecoratedStart(firstReferenceView);
        return Math.min(this.mOrientationHelper.getTotalSpace(), extend);
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        computeScrollOffset(state);
        return computeScrollOffset(state);
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        int scrollOffset = computeScrollOffset(state);
        return scrollOffset;
    }

    private int computeScrollOffset(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }
        int allChildrenCount = state.getItemCount();
        View firstReferenceView = findFirstReferenceChild(allChildrenCount);
        View lastReferenceView = findLastReferenceChild(allChildrenCount);
        if (state.getItemCount() == 0 || firstReferenceView == null || lastReferenceView == null) {
            return 0;
        }
        int minPosition = getPosition(firstReferenceView);
        int maxPosition = getPosition(lastReferenceView);
        int laidOutArea = Math.abs(this.mOrientationHelper.getDecoratedEnd(lastReferenceView) - this.mOrientationHelper.getDecoratedStart(firstReferenceView));
        int firstLinePosition = this.mFlexboxHelper.mIndexToFlexLine[minPosition];
        if (firstLinePosition == 0 || firstLinePosition == -1) {
            return 0;
        }
        int lastLinePosition = this.mFlexboxHelper.mIndexToFlexLine[maxPosition];
        int lineRange = (lastLinePosition - firstLinePosition) + 1;
        float averageSizePerLine = laidOutArea / lineRange;
        return Math.round((firstLinePosition * averageSizePerLine) + (this.mOrientationHelper.getStartAfterPadding() - this.mOrientationHelper.getDecoratedStart(firstReferenceView)));
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        int scrollRange = computeScrollRange(state);
        return scrollRange;
    }

    @Override // android.support.v7.widget.RecyclerView.LayoutManager
    public int computeVerticalScrollRange(RecyclerView.State state) {
        int scrollRange = computeScrollRange(state);
        return scrollRange;
    }

    private int computeScrollRange(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }
        int allItemCount = state.getItemCount();
        View firstReferenceView = findFirstReferenceChild(allItemCount);
        View lastReferenceView = findLastReferenceChild(allItemCount);
        if (state.getItemCount() == 0 || firstReferenceView == null || lastReferenceView == null) {
            return 0;
        }
        int firstVisiblePosition = findFirstVisibleItemPosition();
        int lastVisiblePosition = findLastVisibleItemPosition();
        int laidOutArea = Math.abs(this.mOrientationHelper.getDecoratedEnd(lastReferenceView) - this.mOrientationHelper.getDecoratedStart(firstReferenceView));
        int laidOutRange = (lastVisiblePosition - firstVisiblePosition) + 1;
        return (int) ((laidOutArea / laidOutRange) * state.getItemCount());
    }

    private boolean shouldMeasureChild(View child, int widthSpec, int heightSpec, RecyclerView.LayoutParams lp) {
        return (!child.isLayoutRequested() && isMeasurementCacheEnabled() && isMeasurementUpToDate(child.getWidth(), widthSpec, lp.width) && isMeasurementUpToDate(child.getHeight(), heightSpec, lp.height)) ? false : true;
    }

    private static boolean isMeasurementUpToDate(int childSize, int spec, int dimension) {
        int specMode = View.MeasureSpec.getMode(spec);
        int specSize = View.MeasureSpec.getSize(spec);
        if (dimension > 0 && childSize != dimension) {
            return false;
        }
        if (specMode == Integer.MIN_VALUE) {
            return specSize >= childSize;
        }
        if (specMode != 0) {
            return specMode == 1073741824 && specSize == childSize;
        }
        return true;
    }

    private void clearFlexLines() {
        this.mFlexLines.clear();
        this.mAnchorInfo.reset();
        this.mAnchorInfo.mPerpendicularCoordinate = 0;
    }

    private int getChildLeft(View view) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedLeft(view) - params.leftMargin;
    }

    private int getChildRight(View view) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedRight(view) + params.rightMargin;
    }

    private int getChildTop(View view) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedTop(view) - params.topMargin;
    }

    private int getChildBottom(View view) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedBottom(view) + params.bottomMargin;
    }

    private boolean isViewVisible(View view, boolean completelyVisible) {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getWidth() - getPaddingRight();
        int bottom = getHeight() - getPaddingBottom();
        int childLeft = getChildLeft(view);
        int childTop = getChildTop(view);
        int childRight = getChildRight(view);
        int childBottom = getChildBottom(view);
        boolean horizontalCompletelyVisible = false;
        boolean horizontalPartiallyVisible = false;
        boolean verticalCompletelyVisible = false;
        boolean verticalPartiallyVisible = false;
        if (left <= childLeft && right >= childRight) {
            horizontalCompletelyVisible = true;
        }
        if (childLeft >= right || childRight >= left) {
            horizontalPartiallyVisible = true;
        }
        if (top <= childTop && bottom >= childBottom) {
            verticalCompletelyVisible = true;
        }
        if (childTop >= bottom || childBottom >= top) {
            verticalPartiallyVisible = true;
        }
        return completelyVisible ? horizontalCompletelyVisible && verticalCompletelyVisible : horizontalPartiallyVisible && verticalPartiallyVisible;
    }

    public int findFirstVisibleItemPosition() {
        View child = findOneVisibleChild(0, getChildCount(), false);
        if (child == null) {
            return -1;
        }
        return getPosition(child);
    }

    public int findLastVisibleItemPosition() {
        View child = findOneVisibleChild(getChildCount() - 1, -1, false);
        if (child == null) {
            return -1;
        }
        return getPosition(child);
    }

    private View findOneVisibleChild(int fromIndex, int toIndex, boolean completelyVisible) {
        int next = toIndex > fromIndex ? 1 : -1;
        for (int i = fromIndex; i != toIndex; i += next) {
            View view = getChildAt(i);
            if (isViewVisible(view, completelyVisible)) {
                return view;
            }
        }
        return null;
    }

    public static class LayoutParams extends RecyclerView.LayoutParams implements FlexItem {
        public static final Parcelable.Creator<LayoutParams> CREATOR = new Parcelable.Creator<LayoutParams>() { // from class: com.google.android.flexbox.FlexboxLayoutManager.LayoutParams.1
            /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public LayoutParams createFromParcel(Parcel source) {
                return new LayoutParams(source);
            }

            /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public LayoutParams[] newArray(int size) {
                return new LayoutParams[size];
            }
        };
        private int mAlignSelf;
        private float mFlexBasisPercent;
        private float mFlexGrow;
        private float mFlexShrink;
        private int mMaxHeight;
        private int mMaxWidth;
        private int mMinHeight;
        private int mMinWidth;
        private boolean mWrapBefore;

        @Override // com.google.android.flexbox.FlexItem
        public int getWidth() {
            return this.width;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getHeight() {
            return this.height;
        }

        @Override // com.google.android.flexbox.FlexItem
        public float getFlexGrow() {
            return this.mFlexGrow;
        }

        @Override // com.google.android.flexbox.FlexItem
        public float getFlexShrink() {
            return this.mFlexShrink;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getAlignSelf() {
            return this.mAlignSelf;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMinWidth() {
            return this.mMinWidth;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMinHeight() {
            return this.mMinHeight;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMaxWidth() {
            return this.mMaxWidth;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMaxHeight() {
            return this.mMaxHeight;
        }

        @Override // com.google.android.flexbox.FlexItem
        public boolean isWrapBefore() {
            return this.mWrapBefore;
        }

        @Override // com.google.android.flexbox.FlexItem
        public float getFlexBasisPercent() {
            return this.mFlexBasisPercent;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMarginLeft() {
            return this.leftMargin;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMarginTop() {
            return this.topMargin;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMarginRight() {
            return this.rightMargin;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMarginBottom() {
            return this.bottomMargin;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.mFlexGrow = 0.0f;
            this.mFlexShrink = 1.0f;
            this.mAlignSelf = -1;
            this.mFlexBasisPercent = -1.0f;
            this.mMaxWidth = 16777215;
            this.mMaxHeight = 16777215;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.mFlexGrow = 0.0f;
            this.mFlexShrink = 1.0f;
            this.mAlignSelf = -1;
            this.mFlexBasisPercent = -1.0f;
            this.mMaxWidth = 16777215;
            this.mMaxHeight = 16777215;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getOrder() {
            return 1;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeFloat(this.mFlexGrow);
            parcel.writeFloat(this.mFlexShrink);
            parcel.writeInt(this.mAlignSelf);
            parcel.writeFloat(this.mFlexBasisPercent);
            parcel.writeInt(this.mMinWidth);
            parcel.writeInt(this.mMinHeight);
            parcel.writeInt(this.mMaxWidth);
            parcel.writeInt(this.mMaxHeight);
            parcel.writeByte(this.mWrapBefore ? (byte) 1 : (byte) 0);
            parcel.writeInt(this.bottomMargin);
            parcel.writeInt(this.leftMargin);
            parcel.writeInt(this.rightMargin);
            parcel.writeInt(this.topMargin);
            parcel.writeInt(this.height);
            parcel.writeInt(this.width);
        }

        protected LayoutParams(Parcel in) {
            super(-2, -2);
            this.mFlexGrow = 0.0f;
            this.mFlexShrink = 1.0f;
            this.mAlignSelf = -1;
            this.mFlexBasisPercent = -1.0f;
            this.mMaxWidth = 16777215;
            this.mMaxHeight = 16777215;
            this.mFlexGrow = in.readFloat();
            this.mFlexShrink = in.readFloat();
            this.mAlignSelf = in.readInt();
            this.mFlexBasisPercent = in.readFloat();
            this.mMinWidth = in.readInt();
            this.mMinHeight = in.readInt();
            this.mMaxWidth = in.readInt();
            this.mMaxHeight = in.readInt();
            this.mWrapBefore = in.readByte() != 0;
            this.bottomMargin = in.readInt();
            this.leftMargin = in.readInt();
            this.rightMargin = in.readInt();
            this.topMargin = in.readInt();
            this.height = in.readInt();
            this.width = in.readInt();
        }
    }

    private class AnchorInfo {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private boolean mAssignedFromSavedState;
        private int mCoordinate;
        private int mFlexLinePosition;
        private boolean mLayoutFromEnd;
        private int mPerpendicularCoordinate;
        private int mPosition;
        private boolean mValid;

        private AnchorInfo() {
            this.mPerpendicularCoordinate = 0;
        }

        private void reset() {
            this.mPosition = -1;
            this.mFlexLinePosition = -1;
            this.mCoordinate = Integer.MIN_VALUE;
            this.mValid = false;
            this.mAssignedFromSavedState = false;
            if (FlexboxLayoutManager.this.isMainAxisDirectionHorizontal()) {
                if (FlexboxLayoutManager.this.mFlexWrap == 0) {
                    this.mLayoutFromEnd = FlexboxLayoutManager.this.mFlexDirection == 1;
                    return;
                } else {
                    this.mLayoutFromEnd = FlexboxLayoutManager.this.mFlexWrap == 2;
                    return;
                }
            }
            if (FlexboxLayoutManager.this.mFlexWrap == 0) {
                this.mLayoutFromEnd = FlexboxLayoutManager.this.mFlexDirection == 3;
            } else {
                this.mLayoutFromEnd = FlexboxLayoutManager.this.mFlexWrap == 2;
            }
        }

        private void assignCoordinateFromPadding() {
            if (!FlexboxLayoutManager.this.isMainAxisDirectionHorizontal() && FlexboxLayoutManager.this.mIsRtl) {
                this.mCoordinate = this.mLayoutFromEnd ? FlexboxLayoutManager.this.mOrientationHelper.getEndAfterPadding() : FlexboxLayoutManager.this.getWidth() - FlexboxLayoutManager.this.mOrientationHelper.getStartAfterPadding();
            } else {
                this.mCoordinate = this.mLayoutFromEnd ? FlexboxLayoutManager.this.mOrientationHelper.getEndAfterPadding() : FlexboxLayoutManager.this.mOrientationHelper.getStartAfterPadding();
            }
        }

        private void assignFromView(View anchor) {
            if (!FlexboxLayoutManager.this.isMainAxisDirectionHorizontal() && FlexboxLayoutManager.this.mIsRtl) {
                if (this.mLayoutFromEnd) {
                    this.mCoordinate = FlexboxLayoutManager.this.mOrientationHelper.getDecoratedStart(anchor) + FlexboxLayoutManager.this.mOrientationHelper.getTotalSpaceChange();
                } else {
                    this.mCoordinate = FlexboxLayoutManager.this.mOrientationHelper.getDecoratedEnd(anchor);
                }
            } else if (this.mLayoutFromEnd) {
                this.mCoordinate = FlexboxLayoutManager.this.mOrientationHelper.getDecoratedEnd(anchor) + FlexboxLayoutManager.this.mOrientationHelper.getTotalSpaceChange();
            } else {
                this.mCoordinate = FlexboxLayoutManager.this.mOrientationHelper.getDecoratedStart(anchor);
            }
            this.mPosition = FlexboxLayoutManager.this.getPosition(anchor);
            this.mAssignedFromSavedState = false;
            int flexLinePosition = FlexboxLayoutManager.this.mFlexboxHelper.mIndexToFlexLine[this.mPosition];
            this.mFlexLinePosition = flexLinePosition != -1 ? flexLinePosition : 0;
            if (FlexboxLayoutManager.this.mFlexLines.size() > this.mFlexLinePosition) {
                this.mPosition = ((FlexLine) FlexboxLayoutManager.this.mFlexLines.get(this.mFlexLinePosition)).mFirstIndex;
            }
        }

        public String toString() {
            return "AnchorInfo{mPosition=" + this.mPosition + ", mFlexLinePosition=" + this.mFlexLinePosition + ", mCoordinate=" + this.mCoordinate + ", mPerpendicularCoordinate=" + this.mPerpendicularCoordinate + ", mLayoutFromEnd=" + this.mLayoutFromEnd + ", mValid=" + this.mValid + ", mAssignedFromSavedState=" + this.mAssignedFromSavedState + '}';
        }
    }

    private static class LayoutState {
        private int mAvailable;
        private int mFlexLinePosition;
        private boolean mInfinite;
        private int mItemDirection;
        private int mLastScrollDelta;
        private int mLayoutDirection;
        private int mOffset;
        private int mPosition;
        private int mScrollingOffset;
        private boolean mShouldRecycle;

        private LayoutState() {
            this.mItemDirection = 1;
            this.mLayoutDirection = 1;
        }

        static /* synthetic */ int access$1508(LayoutState x0) {
            int i = x0.mFlexLinePosition;
            x0.mFlexLinePosition = i + 1;
            return i;
        }

        static /* synthetic */ int access$1510(LayoutState x0) {
            int i = x0.mFlexLinePosition;
            x0.mFlexLinePosition = i - 1;
            return i;
        }

        private boolean hasMore(RecyclerView.State state, List<FlexLine> flexLines) {
            return this.mPosition >= 0 && this.mPosition < state.getItemCount() && this.mFlexLinePosition >= 0 && this.mFlexLinePosition < flexLines.size();
        }

        public String toString() {
            return "LayoutState{mAvailable=" + this.mAvailable + ", mFlexLinePosition=" + this.mFlexLinePosition + ", mPosition=" + this.mPosition + ", mOffset=" + this.mOffset + ", mScrollingOffset=" + this.mScrollingOffset + ", mLastScrollDelta=" + this.mLastScrollDelta + ", mItemDirection=" + this.mItemDirection + ", mLayoutDirection=" + this.mLayoutDirection + '}';
        }
    }

    private static class SavedState implements Parcelable {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() { // from class: com.google.android.flexbox.FlexboxLayoutManager.SavedState.1
            /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        private int mAnchorOffset;
        private int mAnchorPosition;

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mAnchorPosition);
            dest.writeInt(this.mAnchorOffset);
        }

        SavedState() {
        }

        private SavedState(Parcel in) {
            this.mAnchorPosition = in.readInt();
            this.mAnchorOffset = in.readInt();
        }

        private SavedState(SavedState savedState) {
            this.mAnchorPosition = savedState.mAnchorPosition;
            this.mAnchorOffset = savedState.mAnchorOffset;
        }

        private void invalidateAnchor() {
            this.mAnchorPosition = -1;
        }

        private boolean hasValidAnchor(int itemCount) {
            return this.mAnchorPosition >= 0 && this.mAnchorPosition < itemCount;
        }

        public String toString() {
            return "SavedState{mAnchorPosition=" + this.mAnchorPosition + ", mAnchorOffset=" + this.mAnchorOffset + '}';
        }
    }
}
