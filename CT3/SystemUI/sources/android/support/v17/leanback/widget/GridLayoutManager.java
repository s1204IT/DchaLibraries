package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v17.leanback.widget.Grid;
import android.support.v17.leanback.widget.ItemAlignmentFacet;
import android.support.v4.util.CircularIntArray;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

final class GridLayoutManager extends RecyclerView.LayoutManager {
    private static final Rect sTempRect = new Rect();
    private static int[] sTwoInts = new int[2];
    private final BaseGridView mBaseGridView;
    private int mExtraLayoutSpace;
    private FacetProviderAdapter mFacetProviderAdapter;
    private int mFixedRowSizeSecondary;
    private boolean mFocusOutEnd;
    private boolean mFocusOutFront;
    private boolean mFocusSearchDisabled;
    private boolean mForceFullLayout;
    Grid mGrid;
    private int mHorizontalMargin;
    private boolean mInFastRelayout;
    private boolean mInLayout;
    private boolean mInLayoutSearchFocus;
    private boolean mInScroll;
    private int mMarginPrimary;
    private int mMarginSecondary;
    private int mMaxSizeSecondary;
    private int mNumRows;
    private PendingMoveSmoothScroller mPendingMoveSmoothScroller;
    private int mPrimaryScrollExtra;
    private RecyclerView.Recycler mRecycler;
    private boolean mRowSecondarySizeRefresh;
    private int[] mRowSizeSecondary;
    private int mRowSizeSecondaryRequested;
    private int mScrollOffsetPrimary;
    private int mScrollOffsetSecondary;
    private int mSizePrimary;
    private RecyclerView.State mState;
    private int mVerticalMargin;
    private int mOrientation = 0;
    private OrientationHelper mOrientationHelper = OrientationHelper.createHorizontalHelper(this);
    private boolean mInSelection = false;
    private OnChildSelectedListener mChildSelectedListener = null;
    private ArrayList<OnChildViewHolderSelectedListener> mChildViewHolderSelectedListeners = null;
    private OnChildLaidOutListener mChildLaidOutListener = null;
    private int mFocusPosition = -1;
    private int mSubFocusPosition = 0;
    private int mFocusPositionOffset = 0;
    private boolean mLayoutEnabled = true;
    private int mChildVisibility = -1;
    private int mGravity = 8388659;
    private int mNumRowsRequested = 1;
    private int mFocusScrollStrategy = 0;
    private final WindowAlignment mWindowAlignment = new WindowAlignment();
    private final ItemAlignment mItemAlignment = new ItemAlignment();
    private boolean mFocusOutSideStart = true;
    private boolean mFocusOutSideEnd = true;
    private boolean mPruneChild = true;
    private boolean mScrollEnabled = true;
    private boolean mReverseFlowPrimary = false;
    private boolean mReverseFlowSecondary = false;
    private int[] mMeasuredDimension = new int[2];
    final ViewsStateBundle mChildrenStates = new ViewsStateBundle();
    private final Runnable mRequestLayoutRunnable = new Runnable() {
        @Override
        public void run() {
            GridLayoutManager.this.requestLayout();
        }
    };
    private final Runnable mAskFocusRunnable = new Runnable() {
        @Override
        public void run() {
            if (GridLayoutManager.this.hasFocus()) {
                return;
            }
            View view = GridLayoutManager.this.findViewByPosition(GridLayoutManager.this.mFocusPosition);
            if (view != null && view.hasFocusable()) {
                GridLayoutManager.this.mBaseGridView.focusableViewAvailable(view);
                return;
            }
            int count = GridLayoutManager.this.getChildCount();
            for (int i = 0; i < count; i++) {
                View view2 = GridLayoutManager.this.getChildAt(i);
                if (view2 != null && view2.hasFocusable()) {
                    GridLayoutManager.this.mBaseGridView.focusableViewAvailable(view2);
                    return;
                }
            }
        }
    };
    private Grid.Provider mGridProvider = new Grid.Provider() {
        @Override
        public int getCount() {
            return GridLayoutManager.this.mState.getItemCount();
        }

        @Override
        public int createItem(int index, boolean append, Object[] item) {
            View v = GridLayoutManager.this.getViewForPosition(index);
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            RecyclerView.ViewHolder vh = GridLayoutManager.this.mBaseGridView.getChildViewHolder(v);
            lp.setItemAlignmentFacet((ItemAlignmentFacet) GridLayoutManager.this.getFacet(vh, ItemAlignmentFacet.class));
            if (!lp.isItemRemoved()) {
                if (append) {
                    GridLayoutManager.this.addView(v);
                } else {
                    GridLayoutManager.this.addView(v, 0);
                }
                if (GridLayoutManager.this.mChildVisibility != -1) {
                    v.setVisibility(GridLayoutManager.this.mChildVisibility);
                }
                if (GridLayoutManager.this.mPendingMoveSmoothScroller != null) {
                    GridLayoutManager.this.mPendingMoveSmoothScroller.consumePendingMovesBeforeLayout();
                }
                int subindex = GridLayoutManager.this.getSubPositionByView(v, v.findFocus());
                if (!GridLayoutManager.this.mInLayout) {
                    if (index == GridLayoutManager.this.mFocusPosition && subindex == GridLayoutManager.this.mSubFocusPosition && GridLayoutManager.this.mPendingMoveSmoothScroller == null) {
                        GridLayoutManager.this.dispatchChildSelected();
                    }
                } else if (!GridLayoutManager.this.mInFastRelayout) {
                    if (!GridLayoutManager.this.mInLayoutSearchFocus && index == GridLayoutManager.this.mFocusPosition && subindex == GridLayoutManager.this.mSubFocusPosition) {
                        GridLayoutManager.this.dispatchChildSelected();
                    } else if (GridLayoutManager.this.mInLayoutSearchFocus && index >= GridLayoutManager.this.mFocusPosition && v.hasFocusable()) {
                        GridLayoutManager.this.mFocusPosition = index;
                        GridLayoutManager.this.mSubFocusPosition = subindex;
                        GridLayoutManager.this.mInLayoutSearchFocus = false;
                        GridLayoutManager.this.dispatchChildSelected();
                    }
                }
                GridLayoutManager.this.measureChild(v);
            }
            item[0] = v;
            return GridLayoutManager.this.mOrientation == 0 ? GridLayoutManager.this.getDecoratedMeasuredWidthWithMargin(v) : GridLayoutManager.this.getDecoratedMeasuredHeightWithMargin(v);
        }

        @Override
        public void addItem(Object item, int index, int length, int rowIndex, int edge) {
            int start;
            int end;
            View v = (View) item;
            if (edge == Integer.MIN_VALUE || edge == Integer.MAX_VALUE) {
                edge = !GridLayoutManager.this.mGrid.isReversedFlow() ? GridLayoutManager.this.mWindowAlignment.mainAxis().getPaddingLow() : GridLayoutManager.this.mWindowAlignment.mainAxis().getSize() - GridLayoutManager.this.mWindowAlignment.mainAxis().getPaddingHigh();
            }
            boolean edgeIsMin = !GridLayoutManager.this.mGrid.isReversedFlow();
            if (edgeIsMin) {
                start = edge;
                end = edge + length;
            } else {
                start = edge - length;
                end = edge;
            }
            int startSecondary = GridLayoutManager.this.getRowStartSecondary(rowIndex) - GridLayoutManager.this.mScrollOffsetSecondary;
            GridLayoutManager.this.mChildrenStates.loadView(v, index);
            GridLayoutManager.this.layoutChild(rowIndex, v, start, end, startSecondary);
            if (index == GridLayoutManager.this.mGrid.getFirstVisibleIndex()) {
                if (!GridLayoutManager.this.mGrid.isReversedFlow()) {
                    GridLayoutManager.this.updateScrollMin();
                } else {
                    GridLayoutManager.this.updateScrollMax();
                }
            }
            if (index == GridLayoutManager.this.mGrid.getLastVisibleIndex()) {
                if (!GridLayoutManager.this.mGrid.isReversedFlow()) {
                    GridLayoutManager.this.updateScrollMax();
                } else {
                    GridLayoutManager.this.updateScrollMin();
                }
            }
            if (!GridLayoutManager.this.mInLayout && GridLayoutManager.this.mPendingMoveSmoothScroller != null) {
                GridLayoutManager.this.mPendingMoveSmoothScroller.consumePendingMovesAfterLayout();
            }
            if (GridLayoutManager.this.mChildLaidOutListener == null) {
                return;
            }
            RecyclerView.ViewHolder vh = GridLayoutManager.this.mBaseGridView.getChildViewHolder(v);
            GridLayoutManager.this.mChildLaidOutListener.onChildLaidOut(GridLayoutManager.this.mBaseGridView, v, index, vh == null ? -1L : vh.getItemId());
        }

        @Override
        public void removeItem(int index) {
            View v = GridLayoutManager.this.findViewByPosition(index);
            if (GridLayoutManager.this.mInLayout) {
                GridLayoutManager.this.detachAndScrapView(v, GridLayoutManager.this.mRecycler);
            } else {
                GridLayoutManager.this.removeAndRecycleView(v, GridLayoutManager.this.mRecycler);
            }
        }

        @Override
        public int getEdge(int index) {
            if (GridLayoutManager.this.mReverseFlowPrimary) {
                return GridLayoutManager.this.getViewMax(GridLayoutManager.this.findViewByPosition(index));
            }
            return GridLayoutManager.this.getViewMin(GridLayoutManager.this.findViewByPosition(index));
        }

        @Override
        public int getSize(int index) {
            return GridLayoutManager.this.getViewPrimarySize(GridLayoutManager.this.findViewByPosition(index));
        }
    };

    static final class LayoutParams extends RecyclerView.LayoutParams {
        private int[] mAlignMultiple;
        private int mAlignX;
        private int mAlignY;
        private ItemAlignmentFacet mAlignmentFacet;
        private int mBottomInset;
        private int mLeftInset;
        private int mRightInset;
        private int mTopInset;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super((RecyclerView.LayoutParams) source);
        }

        int getAlignX() {
            return this.mAlignX;
        }

        int getAlignY() {
            return this.mAlignY;
        }

        int getOpticalLeft(View view) {
            return view.getLeft() + this.mLeftInset;
        }

        int getOpticalTop(View view) {
            return view.getTop() + this.mTopInset;
        }

        int getOpticalRight(View view) {
            return view.getRight() - this.mRightInset;
        }

        int getOpticalWidth(View view) {
            return (view.getWidth() - this.mLeftInset) - this.mRightInset;
        }

        int getOpticalHeight(View view) {
            return (view.getHeight() - this.mTopInset) - this.mBottomInset;
        }

        int getOpticalLeftInset() {
            return this.mLeftInset;
        }

        int getOpticalTopInset() {
            return this.mTopInset;
        }

        void setAlignX(int alignX) {
            this.mAlignX = alignX;
        }

        void setAlignY(int alignY) {
            this.mAlignY = alignY;
        }

        void setItemAlignmentFacet(ItemAlignmentFacet facet) {
            this.mAlignmentFacet = facet;
        }

        ItemAlignmentFacet getItemAlignmentFacet() {
            return this.mAlignmentFacet;
        }

        void calculateItemAlignments(int orientation, View view) {
            ItemAlignmentFacet.ItemAlignmentDef[] defs = this.mAlignmentFacet.getAlignmentDefs();
            if (this.mAlignMultiple == null || this.mAlignMultiple.length != defs.length) {
                this.mAlignMultiple = new int[defs.length];
            }
            for (int i = 0; i < defs.length; i++) {
                this.mAlignMultiple[i] = ItemAlignmentFacetHelper.getAlignmentPosition(view, defs[i], orientation);
            }
            if (orientation == 0) {
                this.mAlignX = this.mAlignMultiple[0];
            } else {
                this.mAlignY = this.mAlignMultiple[0];
            }
        }

        int[] getAlignMultiple() {
            return this.mAlignMultiple;
        }

        void setOpticalInsets(int leftInset, int topInset, int rightInset, int bottomInset) {
            this.mLeftInset = leftInset;
            this.mTopInset = topInset;
            this.mRightInset = rightInset;
            this.mBottomInset = bottomInset;
        }
    }

    abstract class GridLinearSmoothScroller extends LinearSmoothScroller {
        GridLinearSmoothScroller() {
            super(GridLayoutManager.this.mBaseGridView.getContext());
        }

        @Override
        protected void onStop() {
            View targetView = findViewByPosition(getTargetPosition());
            if (targetView == null) {
                if (getTargetPosition() >= 0) {
                    GridLayoutManager.this.scrollToSelection(getTargetPosition(), 0, false, 0);
                }
                super.onStop();
            } else {
                if (GridLayoutManager.this.hasFocus()) {
                    GridLayoutManager.this.mInSelection = true;
                    targetView.requestFocus();
                    GridLayoutManager.this.mInSelection = false;
                }
                GridLayoutManager.this.dispatchChildSelected();
                super.onStop();
            }
        }

        @Override
        protected int calculateTimeForScrolling(int dx) {
            int ms = super.calculateTimeForScrolling(dx);
            if (GridLayoutManager.this.mWindowAlignment.mainAxis().getSize() > 0) {
                float minMs = (30.0f / GridLayoutManager.this.mWindowAlignment.mainAxis().getSize()) * dx;
                if (ms < minMs) {
                    return (int) minMs;
                }
                return ms;
            }
            return ms;
        }

        @Override
        protected void onTargetFound(View targetView, RecyclerView.State state, RecyclerView.SmoothScroller.Action action) {
            int dx;
            int dy;
            if (!GridLayoutManager.this.getScrollPosition(targetView, null, GridLayoutManager.sTwoInts)) {
                return;
            }
            if (GridLayoutManager.this.mOrientation == 0) {
                dx = GridLayoutManager.sTwoInts[0];
                dy = GridLayoutManager.sTwoInts[1];
            } else {
                dx = GridLayoutManager.sTwoInts[1];
                dy = GridLayoutManager.sTwoInts[0];
            }
            int distance = (int) Math.sqrt((dx * dx) + (dy * dy));
            int time = calculateTimeForDeceleration(distance);
            action.update(dx, dy, time, this.mDecelerateInterpolator);
        }
    }

    final class PendingMoveSmoothScroller extends GridLinearSmoothScroller {
        private int mPendingMoves;
        private final boolean mStaggeredGrid;

        PendingMoveSmoothScroller(int initialPendingMoves, boolean staggeredGrid) {
            super();
            this.mPendingMoves = initialPendingMoves;
            this.mStaggeredGrid = staggeredGrid;
            setTargetPosition(-2);
        }

        void increasePendingMoves() {
            if (this.mPendingMoves >= 10) {
                return;
            }
            this.mPendingMoves++;
        }

        void decreasePendingMoves() {
            if (this.mPendingMoves <= -10) {
                return;
            }
            this.mPendingMoves--;
        }

        void consumePendingMovesBeforeLayout() {
            View v;
            if (this.mStaggeredGrid || this.mPendingMoves == 0) {
                return;
            }
            View newSelected = null;
            int startPos = this.mPendingMoves > 0 ? GridLayoutManager.this.mFocusPosition + GridLayoutManager.this.mNumRows : GridLayoutManager.this.mFocusPosition - GridLayoutManager.this.mNumRows;
            int pos = startPos;
            while (this.mPendingMoves != 0 && (v = findViewByPosition(pos)) != null) {
                if (GridLayoutManager.this.canScrollTo(v)) {
                    newSelected = v;
                    GridLayoutManager.this.mFocusPosition = pos;
                    GridLayoutManager.this.mSubFocusPosition = 0;
                    if (this.mPendingMoves > 0) {
                        this.mPendingMoves--;
                    } else {
                        this.mPendingMoves++;
                    }
                }
                pos = this.mPendingMoves > 0 ? pos + GridLayoutManager.this.mNumRows : pos - GridLayoutManager.this.mNumRows;
            }
            if (newSelected == null || !GridLayoutManager.this.hasFocus()) {
                return;
            }
            GridLayoutManager.this.mInSelection = true;
            newSelected.requestFocus();
            GridLayoutManager.this.mInSelection = false;
        }

        void consumePendingMovesAfterLayout() {
            if (this.mStaggeredGrid && this.mPendingMoves != 0) {
                this.mPendingMoves = GridLayoutManager.this.processSelectionMoves(true, this.mPendingMoves);
            }
            if (this.mPendingMoves != 0 && ((this.mPendingMoves <= 0 || !GridLayoutManager.this.hasCreatedLastItem()) && (this.mPendingMoves >= 0 || !GridLayoutManager.this.hasCreatedFirstItem()))) {
                return;
            }
            setTargetPosition(GridLayoutManager.this.mFocusPosition);
            stop();
        }

        @Override
        protected void updateActionForInterimTarget(RecyclerView.SmoothScroller.Action action) {
            if (this.mPendingMoves == 0) {
                return;
            }
            super.updateActionForInterimTarget(action);
        }

        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            if (this.mPendingMoves == 0) {
                return null;
            }
            int direction = (!GridLayoutManager.this.mReverseFlowPrimary ? this.mPendingMoves >= 0 : this.mPendingMoves <= 0) ? -1 : 1;
            if (GridLayoutManager.this.mOrientation == 0) {
                return new PointF(direction, 0.0f);
            }
            return new PointF(0.0f, direction);
        }

        @Override
        protected void onStop() {
            super.onStop();
            this.mPendingMoves = 0;
            GridLayoutManager.this.mPendingMoveSmoothScroller = null;
            View v = findViewByPosition(getTargetPosition());
            if (v != null) {
                GridLayoutManager.this.scrollToView(v, true);
            }
        }
    }

    private String getTag() {
        return "GridLayoutManager:" + this.mBaseGridView.getId();
    }

    public GridLayoutManager(BaseGridView baseGridView) {
        this.mBaseGridView = baseGridView;
    }

    public void setOrientation(int orientation) {
        if (orientation != 0 && orientation != 1) {
            return;
        }
        this.mOrientation = orientation;
        this.mOrientationHelper = OrientationHelper.createOrientationHelper(this, this.mOrientation);
        this.mWindowAlignment.setOrientation(orientation);
        this.mItemAlignment.setOrientation(orientation);
        this.mForceFullLayout = true;
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        if (this.mOrientation == 0) {
            this.mReverseFlowPrimary = layoutDirection == 1;
            this.mReverseFlowSecondary = false;
        } else {
            this.mReverseFlowSecondary = layoutDirection == 1;
            this.mReverseFlowPrimary = false;
        }
        this.mWindowAlignment.horizontal.setReversedFlow(layoutDirection == 1);
    }

    public void setWindowAlignment(int windowAlignment) {
        this.mWindowAlignment.mainAxis().setWindowAlignment(windowAlignment);
    }

    public void setFocusOutAllowed(boolean throughFront, boolean throughEnd) {
        this.mFocusOutFront = throughFront;
        this.mFocusOutEnd = throughEnd;
    }

    public void setFocusOutSideAllowed(boolean throughStart, boolean throughEnd) {
        this.mFocusOutSideStart = throughStart;
        this.mFocusOutSideEnd = throughEnd;
    }

    public void setNumRows(int numRows) {
        if (numRows < 0) {
            throw new IllegalArgumentException();
        }
        this.mNumRowsRequested = numRows;
    }

    public void setRowHeight(int height) {
        if (height >= 0 || height == -2) {
            this.mRowSizeSecondaryRequested = height;
            return;
        }
        throw new IllegalArgumentException("Invalid row height: " + height);
    }

    public void setVerticalMargin(int margin) {
        if (this.mOrientation == 0) {
            this.mVerticalMargin = margin;
            this.mMarginSecondary = margin;
        } else {
            this.mVerticalMargin = margin;
            this.mMarginPrimary = margin;
        }
    }

    public void setHorizontalMargin(int margin) {
        if (this.mOrientation == 0) {
            this.mHorizontalMargin = margin;
            this.mMarginPrimary = margin;
        } else {
            this.mHorizontalMargin = margin;
            this.mMarginSecondary = margin;
        }
    }

    public void setGravity(int gravity) {
        this.mGravity = gravity;
    }

    protected boolean hasDoneFirstLayout() {
        return this.mGrid != null;
    }

    public void setOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        if (listener == null) {
            this.mChildViewHolderSelectedListeners = null;
            return;
        }
        if (this.mChildViewHolderSelectedListeners == null) {
            this.mChildViewHolderSelectedListeners = new ArrayList<>();
        } else {
            this.mChildViewHolderSelectedListeners.clear();
        }
        this.mChildViewHolderSelectedListeners.add(listener);
    }

    boolean hasOnChildViewHolderSelectedListener() {
        return this.mChildViewHolderSelectedListeners != null && this.mChildViewHolderSelectedListeners.size() > 0;
    }

    void fireOnChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
        if (this.mChildViewHolderSelectedListeners == null) {
            return;
        }
        for (int i = this.mChildViewHolderSelectedListeners.size() - 1; i >= 0; i--) {
            this.mChildViewHolderSelectedListeners.get(i).onChildViewHolderSelected(parent, child, position, subposition);
        }
    }

    private int getPositionByView(View view) {
        LayoutParams params;
        if (view == null || (params = (LayoutParams) view.getLayoutParams()) == null || params.isItemRemoved()) {
            return -1;
        }
        return params.getViewPosition();
    }

    public int getSubPositionByView(View view, View childView) {
        if (view == null || childView == null) {
            return 0;
        }
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        ItemAlignmentFacet facet = lp.getItemAlignmentFacet();
        if (facet != null) {
            ItemAlignmentFacet.ItemAlignmentDef[] defs = facet.getAlignmentDefs();
            if (defs.length > 1) {
                while (childView != view) {
                    int id = childView.getId();
                    if (id != -1) {
                        for (int i = 1; i < defs.length; i++) {
                            if (defs[i].getItemAlignmentFocusViewId() == id) {
                                return i;
                            }
                        }
                    }
                    childView = (View) childView.getParent();
                }
            }
        }
        return 0;
    }

    private int getPositionByIndex(int index) {
        return getPositionByView(getChildAt(index));
    }

    public void dispatchChildSelected() {
        if (this.mChildSelectedListener == null && !hasOnChildViewHolderSelectedListener()) {
            return;
        }
        View view = this.mFocusPosition == -1 ? null : findViewByPosition(this.mFocusPosition);
        if (view != null) {
            RecyclerView.ViewHolder vh = this.mBaseGridView.getChildViewHolder(view);
            if (this.mChildSelectedListener != null) {
                this.mChildSelectedListener.onChildSelected(this.mBaseGridView, view, this.mFocusPosition, vh == null ? -1L : vh.getItemId());
            }
            fireOnChildViewHolderSelected(this.mBaseGridView, vh, this.mFocusPosition, this.mSubFocusPosition);
        } else {
            if (this.mChildSelectedListener != null) {
                this.mChildSelectedListener.onChildSelected(this.mBaseGridView, null, -1, -1L);
            }
            fireOnChildViewHolderSelected(this.mBaseGridView, null, -1, 0);
        }
        if (this.mInLayout || this.mBaseGridView.isLayoutRequested()) {
            return;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (getChildAt(i).isLayoutRequested()) {
                forceRequestLayout();
                return;
            }
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        return this.mOrientation == 0 || this.mNumRows > 1;
    }

    @Override
    public boolean canScrollVertically() {
        return this.mOrientation == 1 || this.mNumRows > 1;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context context, AttributeSet attrs) {
        return new LayoutParams(context, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        }
        if (lp instanceof RecyclerView.LayoutParams) {
            return new LayoutParams((RecyclerView.LayoutParams) lp);
        }
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    protected View getViewForPosition(int position) {
        return this.mRecycler.getViewForPosition(position);
    }

    final int getOpticalLeft(View v) {
        return ((LayoutParams) v.getLayoutParams()).getOpticalLeft(v);
    }

    final int getOpticalRight(View v) {
        return ((LayoutParams) v.getLayoutParams()).getOpticalRight(v);
    }

    @Override
    public int getDecoratedLeft(View child) {
        return ((LayoutParams) child.getLayoutParams()).mLeftInset + super.getDecoratedLeft(child);
    }

    @Override
    public int getDecoratedTop(View child) {
        return ((LayoutParams) child.getLayoutParams()).mTopInset + super.getDecoratedTop(child);
    }

    @Override
    public int getDecoratedRight(View child) {
        return super.getDecoratedRight(child) - ((LayoutParams) child.getLayoutParams()).mRightInset;
    }

    @Override
    public int getDecoratedBottom(View child) {
        return super.getDecoratedBottom(child) - ((LayoutParams) child.getLayoutParams()).mBottomInset;
    }

    @Override
    public void getDecoratedBoundsWithMargins(View view, Rect outBounds) {
        super.getDecoratedBoundsWithMargins(view, outBounds);
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        outBounds.left += params.mLeftInset;
        outBounds.top += params.mTopInset;
        outBounds.right -= params.mRightInset;
        outBounds.bottom -= params.mBottomInset;
    }

    public int getViewMin(View v) {
        return this.mOrientationHelper.getDecoratedStart(v);
    }

    public int getViewMax(View v) {
        return this.mOrientationHelper.getDecoratedEnd(v);
    }

    public int getViewPrimarySize(View view) {
        getDecoratedBoundsWithMargins(view, sTempRect);
        return this.mOrientation == 0 ? sTempRect.width() : sTempRect.height();
    }

    private int getViewCenter(View view) {
        return this.mOrientation == 0 ? getViewCenterX(view) : getViewCenterY(view);
    }

    private int getViewCenterSecondary(View view) {
        return this.mOrientation == 0 ? getViewCenterY(view) : getViewCenterX(view);
    }

    private int getViewCenterX(View v) {
        LayoutParams p = (LayoutParams) v.getLayoutParams();
        return p.getOpticalLeft(v) + p.getAlignX();
    }

    private int getViewCenterY(View v) {
        LayoutParams p = (LayoutParams) v.getLayoutParams();
        return p.getOpticalTop(v) + p.getAlignY();
    }

    private void saveContext(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (this.mRecycler != null || this.mState != null) {
            Log.e("GridLayoutManager", "Recycler information was not released, bug!");
        }
        this.mRecycler = recycler;
        this.mState = state;
    }

    private void leaveContext() {
        this.mRecycler = null;
        this.mState = null;
    }

    private boolean layoutInit() {
        boolean focusViewWasInTree = this.mGrid != null && this.mFocusPosition >= 0 && this.mFocusPosition >= this.mGrid.getFirstVisibleIndex() && this.mFocusPosition <= this.mGrid.getLastVisibleIndex();
        int newItemCount = this.mState.getItemCount();
        if (newItemCount == 0) {
            this.mFocusPosition = -1;
            this.mSubFocusPosition = 0;
        } else if (this.mFocusPosition >= newItemCount) {
            this.mFocusPosition = newItemCount - 1;
            this.mSubFocusPosition = 0;
        } else if (this.mFocusPosition == -1 && newItemCount > 0) {
            this.mFocusPosition = 0;
            this.mSubFocusPosition = 0;
        }
        if (!this.mState.didStructureChange() && this.mGrid.getFirstVisibleIndex() >= 0 && !this.mForceFullLayout && this.mGrid != null && this.mGrid.getNumRows() == this.mNumRows) {
            updateScrollController();
            updateScrollSecondAxis();
            this.mGrid.setMargin(this.mMarginPrimary);
            if (!focusViewWasInTree && this.mFocusPosition != -1) {
                this.mGrid.setStart(this.mFocusPosition);
                return true;
            }
            return true;
        }
        this.mForceFullLayout = false;
        int firstVisibleIndex = focusViewWasInTree ? this.mGrid.getFirstVisibleIndex() : 0;
        if (this.mGrid == null || this.mNumRows != this.mGrid.getNumRows() || this.mReverseFlowPrimary != this.mGrid.isReversedFlow()) {
            this.mGrid = Grid.createGrid(this.mNumRows);
            this.mGrid.setProvider(this.mGridProvider);
            this.mGrid.setReversedFlow(this.mReverseFlowPrimary);
        }
        initScrollController();
        updateScrollSecondAxis();
        this.mGrid.setMargin(this.mMarginPrimary);
        detachAndScrapAttachedViews(this.mRecycler);
        this.mGrid.resetVisibleIndex();
        if (this.mFocusPosition == -1) {
            this.mBaseGridView.clearFocus();
        }
        this.mWindowAlignment.mainAxis().invalidateScrollMin();
        this.mWindowAlignment.mainAxis().invalidateScrollMax();
        if (focusViewWasInTree && firstVisibleIndex <= this.mFocusPosition) {
            this.mGrid.setStart(firstVisibleIndex);
        } else {
            this.mGrid.setStart(this.mFocusPosition);
        }
        return false;
    }

    private int getRowSizeSecondary(int rowIndex) {
        if (this.mFixedRowSizeSecondary != 0) {
            return this.mFixedRowSizeSecondary;
        }
        if (this.mRowSizeSecondary == null) {
            return 0;
        }
        return this.mRowSizeSecondary[rowIndex];
    }

    public int getRowStartSecondary(int rowIndex) {
        int start = 0;
        if (this.mReverseFlowSecondary) {
            for (int i = this.mNumRows - 1; i > rowIndex; i--) {
                start += getRowSizeSecondary(i) + this.mMarginSecondary;
            }
        } else {
            for (int i2 = 0; i2 < rowIndex; i2++) {
                start += getRowSizeSecondary(i2) + this.mMarginSecondary;
            }
        }
        return start;
    }

    private int getSizeSecondary() {
        int rightmostIndex = this.mReverseFlowSecondary ? 0 : this.mNumRows - 1;
        return getRowStartSecondary(rightmostIndex) + getRowSizeSecondary(rightmostIndex);
    }

    int getDecoratedMeasuredWidthWithMargin(View v) {
        LayoutParams lp = (LayoutParams) v.getLayoutParams();
        return getDecoratedMeasuredWidth(v) + lp.leftMargin + lp.rightMargin;
    }

    int getDecoratedMeasuredHeightWithMargin(View v) {
        LayoutParams lp = (LayoutParams) v.getLayoutParams();
        return getDecoratedMeasuredHeight(v) + lp.topMargin + lp.bottomMargin;
    }

    private void measureScrapChild(int position, int widthSpec, int heightSpec, int[] measuredDimension) {
        View view = this.mRecycler.getViewForPosition(position);
        if (view == null) {
            return;
        }
        LayoutParams p = (LayoutParams) view.getLayoutParams();
        calculateItemDecorationsForChild(view, sTempRect);
        int widthUsed = p.leftMargin + p.rightMargin + sTempRect.left + sTempRect.right;
        int heightUsed = p.topMargin + p.bottomMargin + sTempRect.top + sTempRect.bottom;
        int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec, getPaddingLeft() + getPaddingRight() + widthUsed, p.width);
        int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec, getPaddingTop() + getPaddingBottom() + heightUsed, p.height);
        view.measure(childWidthSpec, childHeightSpec);
        measuredDimension[0] = getDecoratedMeasuredWidthWithMargin(view);
        measuredDimension[1] = getDecoratedMeasuredHeightWithMargin(view);
        this.mRecycler.recycleView(view);
    }

    private boolean processRowSizeSecondary(boolean measure) {
        int position;
        int secondarySize;
        if (this.mFixedRowSizeSecondary != 0 || this.mRowSizeSecondary == null) {
            return false;
        }
        CircularIntArray[] rows = this.mGrid == null ? null : this.mGrid.getItemPositionsInRows();
        boolean changed = false;
        int scrapChildWidth = -1;
        int scrapChildHeight = -1;
        for (int rowIndex = 0; rowIndex < this.mNumRows; rowIndex++) {
            CircularIntArray row = rows == null ? null : rows[rowIndex];
            int rowItemsPairCount = row == null ? 0 : row.size();
            int rowSize = -1;
            for (int rowItemPairIndex = 0; rowItemPairIndex < rowItemsPairCount; rowItemPairIndex += 2) {
                int rowIndexStart = row.get(rowItemPairIndex);
                int rowIndexEnd = row.get(rowItemPairIndex + 1);
                for (int i = rowIndexStart; i <= rowIndexEnd; i++) {
                    View view = findViewByPosition(i);
                    if (view != null) {
                        if (measure) {
                            measureChild(view);
                        }
                        if (this.mOrientation == 0) {
                            secondarySize = getDecoratedMeasuredHeightWithMargin(view);
                        } else {
                            secondarySize = getDecoratedMeasuredWidthWithMargin(view);
                        }
                        if (secondarySize > rowSize) {
                            rowSize = secondarySize;
                        }
                    }
                }
            }
            int itemCount = this.mState.getItemCount();
            if (!this.mBaseGridView.hasFixedSize() && measure && rowSize < 0 && itemCount > 0) {
                if (scrapChildWidth < 0 && scrapChildHeight < 0) {
                    if (this.mFocusPosition == -1) {
                        position = 0;
                    } else if (this.mFocusPosition >= itemCount) {
                        position = itemCount - 1;
                    } else {
                        position = this.mFocusPosition;
                    }
                    measureScrapChild(position, View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0), this.mMeasuredDimension);
                    scrapChildWidth = this.mMeasuredDimension[0];
                    scrapChildHeight = this.mMeasuredDimension[1];
                }
                rowSize = this.mOrientation == 0 ? scrapChildHeight : scrapChildWidth;
            }
            if (rowSize < 0) {
                rowSize = 0;
            }
            if (this.mRowSizeSecondary[rowIndex] != rowSize) {
                this.mRowSizeSecondary[rowIndex] = rowSize;
                changed = true;
            }
        }
        return changed;
    }

    private void updateRowSecondarySizeRefresh() {
        this.mRowSecondarySizeRefresh = processRowSizeSecondary(false);
        if (!this.mRowSecondarySizeRefresh) {
            return;
        }
        forceRequestLayout();
    }

    private void forceRequestLayout() {
        ViewCompat.postOnAnimation(this.mBaseGridView, this.mRequestLayoutRunnable);
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {
        int sizeSecondary;
        int sizePrimary;
        int modeSecondary;
        int paddingSecondary;
        int measuredSizeSecondary;
        int childrenSize;
        saveContext(recycler, state);
        if (this.mOrientation == 0) {
            sizePrimary = View.MeasureSpec.getSize(widthSpec);
            sizeSecondary = View.MeasureSpec.getSize(heightSpec);
            modeSecondary = View.MeasureSpec.getMode(heightSpec);
            paddingSecondary = getPaddingTop() + getPaddingBottom();
        } else {
            sizeSecondary = View.MeasureSpec.getSize(widthSpec);
            sizePrimary = View.MeasureSpec.getSize(heightSpec);
            modeSecondary = View.MeasureSpec.getMode(widthSpec);
            paddingSecondary = getPaddingLeft() + getPaddingRight();
        }
        this.mMaxSizeSecondary = sizeSecondary;
        if (this.mRowSizeSecondaryRequested == -2) {
            this.mNumRows = this.mNumRowsRequested == 0 ? 1 : this.mNumRowsRequested;
            this.mFixedRowSizeSecondary = 0;
            if (this.mRowSizeSecondary == null || this.mRowSizeSecondary.length != this.mNumRows) {
                this.mRowSizeSecondary = new int[this.mNumRows];
            }
            processRowSizeSecondary(true);
            switch (modeSecondary) {
                case Integer.MIN_VALUE:
                    measuredSizeSecondary = Math.min(getSizeSecondary() + paddingSecondary, this.mMaxSizeSecondary);
                    break;
                case 0:
                    measuredSizeSecondary = getSizeSecondary() + paddingSecondary;
                    break;
                case 1073741824:
                    measuredSizeSecondary = this.mMaxSizeSecondary;
                    break;
                default:
                    throw new IllegalStateException("wrong spec");
            }
        } else {
            switch (modeSecondary) {
                case Integer.MIN_VALUE:
                case 1073741824:
                    if (this.mNumRowsRequested == 0 && this.mRowSizeSecondaryRequested == 0) {
                        this.mNumRows = 1;
                        this.mFixedRowSizeSecondary = sizeSecondary - paddingSecondary;
                    } else if (this.mNumRowsRequested == 0) {
                        this.mFixedRowSizeSecondary = this.mRowSizeSecondaryRequested;
                        this.mNumRows = (this.mMarginSecondary + sizeSecondary) / (this.mRowSizeSecondaryRequested + this.mMarginSecondary);
                    } else if (this.mRowSizeSecondaryRequested == 0) {
                        this.mNumRows = this.mNumRowsRequested;
                        this.mFixedRowSizeSecondary = ((sizeSecondary - paddingSecondary) - (this.mMarginSecondary * (this.mNumRows - 1))) / this.mNumRows;
                    } else {
                        this.mNumRows = this.mNumRowsRequested;
                        this.mFixedRowSizeSecondary = this.mRowSizeSecondaryRequested;
                    }
                    measuredSizeSecondary = sizeSecondary;
                    if (modeSecondary == Integer.MIN_VALUE && (childrenSize = (this.mFixedRowSizeSecondary * this.mNumRows) + (this.mMarginSecondary * (this.mNumRows - 1)) + paddingSecondary) < measuredSizeSecondary) {
                        measuredSizeSecondary = childrenSize;
                    }
                    break;
                case 0:
                    this.mFixedRowSizeSecondary = this.mRowSizeSecondaryRequested == 0 ? sizeSecondary - paddingSecondary : this.mRowSizeSecondaryRequested;
                    this.mNumRows = this.mNumRowsRequested != 0 ? this.mNumRowsRequested : 1;
                    measuredSizeSecondary = (this.mFixedRowSizeSecondary * this.mNumRows) + (this.mMarginSecondary * (this.mNumRows - 1)) + paddingSecondary;
                    break;
                default:
                    throw new IllegalStateException("wrong spec");
            }
        }
        if (this.mOrientation == 0) {
            setMeasuredDimension(sizePrimary, measuredSizeSecondary);
        } else {
            setMeasuredDimension(measuredSizeSecondary, sizePrimary);
        }
        leaveContext();
    }

    public void measureChild(View child) {
        int secondarySpec;
        int heightSpec;
        int widthSpec;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        calculateItemDecorationsForChild(child, sTempRect);
        int widthUsed = lp.leftMargin + lp.rightMargin + sTempRect.left + sTempRect.right;
        int heightUsed = lp.topMargin + lp.bottomMargin + sTempRect.top + sTempRect.bottom;
        if (this.mRowSizeSecondaryRequested == -2) {
            secondarySpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        } else {
            secondarySpec = View.MeasureSpec.makeMeasureSpec(this.mFixedRowSizeSecondary, 1073741824);
        }
        if (this.mOrientation == 0) {
            widthSpec = ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(0, 0), widthUsed, lp.width);
            heightSpec = ViewGroup.getChildMeasureSpec(secondarySpec, heightUsed, lp.height);
        } else {
            heightSpec = ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(0, 0), heightUsed, lp.height);
            widthSpec = ViewGroup.getChildMeasureSpec(secondarySpec, widthUsed, lp.width);
        }
        child.measure(widthSpec, heightSpec);
    }

    public <E> E getFacet(RecyclerView.ViewHolder viewHolder, Class<? extends E> cls) {
        FacetProvider facetProvider;
        E e = null;
        if (viewHolder instanceof FacetProvider) {
            e = (E) ((FacetProvider) viewHolder).getFacet(cls);
        }
        return (e != null || this.mFacetProviderAdapter == null || (facetProvider = this.mFacetProviderAdapter.getFacetProvider(viewHolder.getItemViewType())) == null) ? e : (E) facetProvider.getFacet(cls);
    }

    public void layoutChild(int rowIndex, View v, int start, int end, int startSecondary) {
        int horizontalGravity;
        int top;
        int left;
        int bottom;
        int right;
        int sizeSecondary = this.mOrientation == 0 ? getDecoratedMeasuredHeightWithMargin(v) : getDecoratedMeasuredWidthWithMargin(v);
        if (this.mFixedRowSizeSecondary > 0) {
            sizeSecondary = Math.min(sizeSecondary, this.mFixedRowSizeSecondary);
        }
        int verticalGravity = this.mGravity & 112;
        if (this.mReverseFlowPrimary || this.mReverseFlowSecondary) {
            horizontalGravity = Gravity.getAbsoluteGravity(this.mGravity & 8388615, 1);
        } else {
            horizontalGravity = this.mGravity & 7;
        }
        if ((this.mOrientation != 0 || verticalGravity != 48) && (this.mOrientation != 1 || horizontalGravity != 3)) {
            if ((this.mOrientation == 0 && verticalGravity == 80) || (this.mOrientation == 1 && horizontalGravity == 5)) {
                startSecondary += getRowSizeSecondary(rowIndex) - sizeSecondary;
            } else if ((this.mOrientation == 0 && verticalGravity == 16) || (this.mOrientation == 1 && horizontalGravity == 1)) {
                startSecondary += (getRowSizeSecondary(rowIndex) - sizeSecondary) / 2;
            }
        }
        if (this.mOrientation == 0) {
            left = start;
            top = startSecondary;
            right = end;
            bottom = startSecondary + sizeSecondary;
        } else {
            top = start;
            left = startSecondary;
            bottom = end;
            right = startSecondary + sizeSecondary;
        }
        LayoutParams params = (LayoutParams) v.getLayoutParams();
        layoutDecoratedWithMargins(v, left, top, right, bottom);
        super.getDecoratedBoundsWithMargins(v, sTempRect);
        params.setOpticalInsets(left - sTempRect.left, top - sTempRect.top, sTempRect.right - right, sTempRect.bottom - bottom);
        updateChildAlignments(v);
    }

    private void updateChildAlignments(View v) {
        LayoutParams p = (LayoutParams) v.getLayoutParams();
        if (p.getItemAlignmentFacet() == null) {
            p.setAlignX(this.mItemAlignment.horizontal.getAlignmentPosition(v));
            p.setAlignY(this.mItemAlignment.vertical.getAlignmentPosition(v));
            return;
        }
        p.calculateItemAlignments(this.mOrientation, v);
        if (this.mOrientation == 0) {
            p.setAlignY(this.mItemAlignment.vertical.getAlignmentPosition(v));
        } else {
            p.setAlignX(this.mItemAlignment.horizontal.getAlignmentPosition(v));
        }
    }

    private void removeInvisibleViewsAtEnd() {
        if (!this.mPruneChild) {
            return;
        }
        this.mGrid.removeInvisibleItemsAtEnd(this.mFocusPosition, this.mReverseFlowPrimary ? -this.mExtraLayoutSpace : this.mSizePrimary + this.mExtraLayoutSpace);
    }

    private void removeInvisibleViewsAtFront() {
        if (!this.mPruneChild) {
            return;
        }
        this.mGrid.removeInvisibleItemsAtFront(this.mFocusPosition, this.mReverseFlowPrimary ? this.mSizePrimary + this.mExtraLayoutSpace : -this.mExtraLayoutSpace);
    }

    private boolean appendOneColumnVisibleItems() {
        return this.mGrid.appendOneColumnVisibleItems();
    }

    private boolean prependOneColumnVisibleItems() {
        return this.mGrid.prependOneColumnVisibleItems();
    }

    private void appendVisibleItems() {
        this.mGrid.appendVisibleItems(this.mReverseFlowPrimary ? -this.mExtraLayoutSpace : this.mSizePrimary + this.mExtraLayoutSpace);
    }

    private void prependVisibleItems() {
        this.mGrid.prependVisibleItems(this.mReverseFlowPrimary ? this.mSizePrimary + this.mExtraLayoutSpace : -this.mExtraLayoutSpace);
    }

    private void fastRelayout() {
        int primarySize;
        int end;
        boolean invalidateAfter = false;
        int childCount = getChildCount();
        int position = -1;
        int index = 0;
        while (true) {
            if (index >= childCount) {
                break;
            }
            View view = getChildAt(index);
            position = getPositionByIndex(index);
            Grid.Location location = this.mGrid.getLocation(position);
            if (location == null) {
                invalidateAfter = true;
                break;
            }
            int startSecondary = getRowStartSecondary(location.row) - this.mScrollOffsetSecondary;
            int start = getViewMin(view);
            int oldPrimarySize = getViewPrimarySize(view);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.viewNeedsUpdate()) {
                int viewIndex = this.mBaseGridView.indexOfChild(view);
                detachAndScrapView(view, this.mRecycler);
                view = getViewForPosition(position);
                addView(view, viewIndex);
            }
            measureChild(view);
            if (this.mOrientation == 0) {
                primarySize = getDecoratedMeasuredWidthWithMargin(view);
                end = start + primarySize;
            } else {
                primarySize = getDecoratedMeasuredHeightWithMargin(view);
                end = start + primarySize;
            }
            layoutChild(location.row, view, start, end, startSecondary);
            if (oldPrimarySize == primarySize) {
                index++;
            } else {
                invalidateAfter = true;
                break;
            }
        }
        if (invalidateAfter) {
            int savedLastPos = this.mGrid.getLastVisibleIndex();
            this.mGrid.invalidateItemsAfter(position);
            if (this.mPruneChild) {
                appendVisibleItems();
                if (this.mFocusPosition >= 0 && this.mFocusPosition <= savedLastPos) {
                    while (this.mGrid.getLastVisibleIndex() < this.mFocusPosition) {
                        this.mGrid.appendOneColumnVisibleItems();
                    }
                }
            } else {
                while (this.mGrid.appendOneColumnVisibleItems() && this.mGrid.getLastVisibleIndex() < savedLastPos) {
                }
            }
        }
        updateScrollMin();
        updateScrollMax();
        updateScrollSecondAxis();
    }

    @Override
    public void removeAndRecycleAllViews(RecyclerView.Recycler recycler) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            removeAndRecycleViewAt(i, recycler);
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        View focusView;
        if (this.mNumRows == 0) {
            return;
        }
        int itemCount = state.getItemCount();
        if (itemCount < 0) {
            return;
        }
        if (!this.mLayoutEnabled) {
            discardLayoutInfo();
            removeAndRecycleAllViews(recycler);
            return;
        }
        this.mInLayout = true;
        if (state.didStructureChange()) {
            this.mBaseGridView.stopScroll();
        }
        boolean scrollToFocus = !isSmoothScrolling() && this.mFocusScrollStrategy == 0;
        if (this.mFocusPosition != -1 && this.mFocusPositionOffset != Integer.MIN_VALUE) {
            this.mFocusPosition += this.mFocusPositionOffset;
            this.mSubFocusPosition = 0;
        }
        this.mFocusPositionOffset = 0;
        saveContext(recycler, state);
        View savedFocusView = findViewByPosition(this.mFocusPosition);
        int savedFocusPos = this.mFocusPosition;
        int savedSubFocusPos = this.mSubFocusPosition;
        boolean hadFocus = this.mBaseGridView.hasFocus();
        int delta = 0;
        int deltaSecondary = 0;
        if (this.mFocusPosition != -1 && scrollToFocus && this.mBaseGridView.getScrollState() != 0 && savedFocusView != null && getScrollPosition(savedFocusView, savedFocusView.findFocus(), sTwoInts)) {
            delta = sTwoInts[0];
            deltaSecondary = sTwoInts[1];
        }
        boolean zLayoutInit = layoutInit();
        this.mInFastRelayout = zLayoutInit;
        if (zLayoutInit) {
            fastRelayout();
            if (this.mFocusPosition != -1 && (focusView = findViewByPosition(this.mFocusPosition)) != null) {
                if (scrollToFocus) {
                    scrollToView(focusView, false);
                }
                if (hadFocus && !focusView.hasFocus()) {
                    focusView.requestFocus();
                }
            }
        } else {
            this.mInLayoutSearchFocus = hadFocus;
            if (this.mFocusPosition != -1) {
                while (appendOneColumnVisibleItems() && findViewByPosition(this.mFocusPosition) == null) {
                }
            }
            while (true) {
                updateScrollMin();
                updateScrollMax();
                int oldFirstVisible = this.mGrid.getFirstVisibleIndex();
                int oldLastVisible = this.mGrid.getLastVisibleIndex();
                View focusView2 = findViewByPosition(this.mFocusPosition);
                scrollToView(focusView2, false);
                if (focusView2 != null && hadFocus && !focusView2.hasFocus()) {
                    focusView2.requestFocus();
                }
                appendVisibleItems();
                prependVisibleItems();
                removeInvisibleViewsAtFront();
                removeInvisibleViewsAtEnd();
                if (this.mGrid.getFirstVisibleIndex() == oldFirstVisible && this.mGrid.getLastVisibleIndex() == oldLastVisible) {
                    break;
                }
            }
        }
        if (scrollToFocus) {
            scrollDirectionPrimary(-delta);
            scrollDirectionSecondary(-deltaSecondary);
        }
        appendVisibleItems();
        prependVisibleItems();
        removeInvisibleViewsAtFront();
        removeInvisibleViewsAtEnd();
        if (this.mRowSecondarySizeRefresh) {
            this.mRowSecondarySizeRefresh = false;
        } else {
            updateRowSecondarySizeRefresh();
        }
        if (this.mInFastRelayout && (this.mFocusPosition != savedFocusPos || this.mSubFocusPosition != savedSubFocusPos || findViewByPosition(this.mFocusPosition) != savedFocusView)) {
            dispatchChildSelected();
        } else if (!this.mInFastRelayout && this.mInLayoutSearchFocus) {
            dispatchChildSelected();
        }
        this.mInLayout = false;
        leaveContext();
        if (hadFocus || this.mInFastRelayout || !this.mBaseGridView.hasFocusable()) {
            return;
        }
        ViewCompat.postOnAnimation(this.mBaseGridView, this.mAskFocusRunnable);
    }

    private void offsetChildrenSecondary(int increment) {
        int childCount = getChildCount();
        if (this.mOrientation == 0) {
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).offsetTopAndBottom(increment);
            }
            return;
        }
        for (int i2 = 0; i2 < childCount; i2++) {
            getChildAt(i2).offsetLeftAndRight(increment);
        }
    }

    private void offsetChildrenPrimary(int increment) {
        int childCount = getChildCount();
        if (this.mOrientation == 1) {
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).offsetTopAndBottom(increment);
            }
            return;
        }
        for (int i2 = 0; i2 < childCount; i2++) {
            getChildAt(i2).offsetLeftAndRight(increment);
        }
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int result;
        if (!this.mLayoutEnabled || !hasDoneFirstLayout()) {
            return 0;
        }
        saveContext(recycler, state);
        this.mInScroll = true;
        if (this.mOrientation == 0) {
            result = scrollDirectionPrimary(dx);
        } else {
            result = scrollDirectionSecondary(dx);
        }
        leaveContext();
        this.mInScroll = false;
        return result;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int result;
        if (!this.mLayoutEnabled || !hasDoneFirstLayout()) {
            return 0;
        }
        this.mInScroll = true;
        saveContext(recycler, state);
        if (this.mOrientation == 1) {
            result = scrollDirectionPrimary(dy);
        } else {
            result = scrollDirectionSecondary(dy);
        }
        leaveContext();
        this.mInScroll = false;
        return result;
    }

    private int scrollDirectionPrimary(int da) {
        int minScroll;
        int maxScroll;
        if (da > 0) {
            boolean isMaxUnknown = this.mWindowAlignment.mainAxis().isMaxUnknown();
            if (!isMaxUnknown && this.mScrollOffsetPrimary + da > (maxScroll = this.mWindowAlignment.mainAxis().getMaxScroll())) {
                da = maxScroll - this.mScrollOffsetPrimary;
            }
        } else if (da < 0) {
            boolean isMinUnknown = this.mWindowAlignment.mainAxis().isMinUnknown();
            if (!isMinUnknown && this.mScrollOffsetPrimary + da < (minScroll = this.mWindowAlignment.mainAxis().getMinScroll())) {
                da = minScroll - this.mScrollOffsetPrimary;
            }
        }
        if (da == 0) {
            return 0;
        }
        offsetChildrenPrimary(-da);
        this.mScrollOffsetPrimary += da;
        if (this.mInLayout) {
            return da;
        }
        int childCount = getChildCount();
        if (!this.mReverseFlowPrimary ? da < 0 : da > 0) {
            prependVisibleItems();
        } else {
            appendVisibleItems();
        }
        boolean updated = getChildCount() > childCount;
        int childCount2 = getChildCount();
        if (!this.mReverseFlowPrimary ? da < 0 : da > 0) {
            removeInvisibleViewsAtEnd();
        } else {
            removeInvisibleViewsAtFront();
        }
        if (updated | (getChildCount() < childCount2)) {
            updateRowSecondarySizeRefresh();
        }
        this.mBaseGridView.invalidate();
        return da;
    }

    private int scrollDirectionSecondary(int dy) {
        if (dy == 0) {
            return 0;
        }
        offsetChildrenSecondary(-dy);
        this.mScrollOffsetSecondary += dy;
        this.mBaseGridView.invalidate();
        return dy;
    }

    public void updateScrollMax() {
        int highVisiblePos = !this.mReverseFlowPrimary ? this.mGrid.getLastVisibleIndex() : this.mGrid.getFirstVisibleIndex();
        int highMaxPos = !this.mReverseFlowPrimary ? this.mState.getItemCount() - 1 : 0;
        if (highVisiblePos < 0) {
            return;
        }
        boolean highAvailable = highVisiblePos == highMaxPos;
        boolean maxUnknown = this.mWindowAlignment.mainAxis().isMaxUnknown();
        if (!highAvailable && maxUnknown) {
            return;
        }
        int maxEdge = this.mGrid.findRowMax(true, sTwoInts) + this.mScrollOffsetPrimary;
        int i = sTwoInts[0];
        int pos = sTwoInts[1];
        int savedMaxEdge = this.mWindowAlignment.mainAxis().getMaxEdge();
        this.mWindowAlignment.mainAxis().setMaxEdge(maxEdge);
        int maxScroll = getPrimarySystemScrollPositionOfChildMax(findViewByPosition(pos));
        this.mWindowAlignment.mainAxis().setMaxEdge(savedMaxEdge);
        if (highAvailable) {
            this.mWindowAlignment.mainAxis().setMaxEdge(maxEdge);
            this.mWindowAlignment.mainAxis().setMaxScroll(maxScroll);
        } else {
            this.mWindowAlignment.mainAxis().invalidateScrollMax();
        }
    }

    public void updateScrollMin() {
        int lowVisiblePos = !this.mReverseFlowPrimary ? this.mGrid.getFirstVisibleIndex() : this.mGrid.getLastVisibleIndex();
        int lowMinPos = !this.mReverseFlowPrimary ? 0 : this.mState.getItemCount() - 1;
        if (lowVisiblePos < 0) {
            return;
        }
        boolean lowAvailable = lowVisiblePos == lowMinPos;
        boolean minUnknown = this.mWindowAlignment.mainAxis().isMinUnknown();
        if (!lowAvailable && minUnknown) {
            return;
        }
        int minEdge = this.mGrid.findRowMin(false, sTwoInts) + this.mScrollOffsetPrimary;
        int i = sTwoInts[0];
        int pos = sTwoInts[1];
        int savedMinEdge = this.mWindowAlignment.mainAxis().getMinEdge();
        this.mWindowAlignment.mainAxis().setMinEdge(minEdge);
        int minScroll = getPrimarySystemScrollPosition(findViewByPosition(pos));
        this.mWindowAlignment.mainAxis().setMinEdge(savedMinEdge);
        if (lowAvailable) {
            this.mWindowAlignment.mainAxis().setMinEdge(minEdge);
            this.mWindowAlignment.mainAxis().setMinScroll(minScroll);
        } else {
            this.mWindowAlignment.mainAxis().invalidateScrollMin();
        }
    }

    private void updateScrollSecondAxis() {
        this.mWindowAlignment.secondAxis().setMinEdge(0);
        this.mWindowAlignment.secondAxis().setMaxEdge(getSizeSecondary());
    }

    private void initScrollController() {
        this.mWindowAlignment.reset();
        this.mWindowAlignment.horizontal.setSize(getWidth());
        this.mWindowAlignment.vertical.setSize(getHeight());
        this.mWindowAlignment.horizontal.setPadding(getPaddingLeft(), getPaddingRight());
        this.mWindowAlignment.vertical.setPadding(getPaddingTop(), getPaddingBottom());
        this.mSizePrimary = this.mWindowAlignment.mainAxis().getSize();
        this.mScrollOffsetPrimary = -this.mWindowAlignment.mainAxis().getPaddingLow();
        this.mScrollOffsetSecondary = -this.mWindowAlignment.secondAxis().getPaddingLow();
    }

    private void updateScrollController() {
        int paddingPrimaryDiff;
        int paddingSecondaryDiff;
        if (this.mOrientation == 0) {
            paddingPrimaryDiff = getPaddingLeft() - this.mWindowAlignment.horizontal.getPaddingLow();
            paddingSecondaryDiff = getPaddingTop() - this.mWindowAlignment.vertical.getPaddingLow();
        } else {
            paddingPrimaryDiff = getPaddingTop() - this.mWindowAlignment.vertical.getPaddingLow();
            paddingSecondaryDiff = getPaddingLeft() - this.mWindowAlignment.horizontal.getPaddingLow();
        }
        this.mScrollOffsetPrimary -= paddingPrimaryDiff;
        this.mScrollOffsetSecondary -= paddingSecondaryDiff;
        this.mWindowAlignment.horizontal.setSize(getWidth());
        this.mWindowAlignment.vertical.setSize(getHeight());
        this.mWindowAlignment.horizontal.setPadding(getPaddingLeft(), getPaddingRight());
        this.mWindowAlignment.vertical.setPadding(getPaddingTop(), getPaddingBottom());
        this.mSizePrimary = this.mWindowAlignment.mainAxis().getSize();
    }

    @Override
    public void scrollToPosition(int position) {
        setSelection(position, 0, false, 0);
    }

    public void setSelection(int position, int primaryScrollExtra) {
        setSelection(position, 0, false, primaryScrollExtra);
    }

    public void setSelectionSmooth(int position) {
        setSelection(position, 0, true, 0);
    }

    public int getSelection() {
        return this.mFocusPosition;
    }

    public void setSelection(int position, int subposition, boolean smooth, int primaryScrollExtra) {
        if ((this.mFocusPosition == position || position == -1) && subposition == this.mSubFocusPosition && primaryScrollExtra == this.mPrimaryScrollExtra) {
            return;
        }
        scrollToSelection(position, subposition, smooth, primaryScrollExtra);
    }

    public void scrollToSelection(int position, int subposition, boolean smooth, int primaryScrollExtra) {
        this.mPrimaryScrollExtra = primaryScrollExtra;
        View view = findViewByPosition(position);
        if (view != null) {
            this.mInSelection = true;
            scrollToView(view, smooth);
            this.mInSelection = false;
            return;
        }
        this.mFocusPosition = position;
        this.mSubFocusPosition = subposition;
        this.mFocusPositionOffset = Integer.MIN_VALUE;
        if (!this.mLayoutEnabled) {
            return;
        }
        if (smooth) {
            if (!hasDoneFirstLayout()) {
                Log.w(getTag(), "setSelectionSmooth should not be called before first layout pass");
                return;
            } else {
                startPositionSmoothScroller(position);
                return;
            }
        }
        this.mForceFullLayout = true;
        requestLayout();
    }

    void startPositionSmoothScroller(int position) {
        LinearSmoothScroller linearSmoothScroller = new GridLinearSmoothScroller(this) {
            {
                super();
            }

            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                boolean isStart = true;
                if (getChildCount() == 0) {
                    return null;
                }
                int firstChildPos = this.getPosition(this.getChildAt(0));
                if (this.mReverseFlowPrimary) {
                    if (targetPosition <= firstChildPos) {
                        isStart = false;
                    }
                } else if (targetPosition >= firstChildPos) {
                    isStart = false;
                }
                int direction = isStart ? -1 : 1;
                if (this.mOrientation == 0) {
                    return new PointF(direction, 0.0f);
                }
                return new PointF(0.0f, direction);
            }
        };
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    private void processPendingMovement(boolean forward) {
        if (forward ? hasCreatedLastItem() : hasCreatedFirstItem()) {
            return;
        }
        if (this.mPendingMoveSmoothScroller == null) {
            this.mBaseGridView.stopScroll();
            PendingMoveSmoothScroller linearSmoothScroller = new PendingMoveSmoothScroller(forward ? 1 : -1, this.mNumRows > 1);
            this.mFocusPositionOffset = 0;
            startSmoothScroll(linearSmoothScroller);
            if (!linearSmoothScroller.isRunning()) {
                return;
            }
            this.mPendingMoveSmoothScroller = linearSmoothScroller;
            return;
        }
        if (forward) {
            this.mPendingMoveSmoothScroller.increasePendingMoves();
        } else {
            this.mPendingMoveSmoothScroller.decreasePendingMoves();
        }
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        if (this.mFocusPosition != -1 && this.mGrid != null && this.mGrid.getFirstVisibleIndex() >= 0 && this.mFocusPositionOffset != Integer.MIN_VALUE) {
            int pos = this.mFocusPosition + this.mFocusPositionOffset;
            if (positionStart <= pos) {
                this.mFocusPositionOffset += itemCount;
            }
        }
        this.mChildrenStates.clear();
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        this.mFocusPositionOffset = 0;
        this.mChildrenStates.clear();
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        int pos;
        if (this.mFocusPosition != -1 && this.mGrid != null && this.mGrid.getFirstVisibleIndex() >= 0 && this.mFocusPositionOffset != Integer.MIN_VALUE && positionStart <= (pos = this.mFocusPosition + this.mFocusPositionOffset)) {
            if (positionStart + itemCount > pos) {
                this.mFocusPositionOffset = Integer.MIN_VALUE;
            } else {
                this.mFocusPositionOffset -= itemCount;
            }
        }
        this.mChildrenStates.clear();
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int fromPosition, int toPosition, int itemCount) {
        if (this.mFocusPosition != -1 && this.mFocusPositionOffset != Integer.MIN_VALUE) {
            int pos = this.mFocusPosition + this.mFocusPositionOffset;
            if (fromPosition <= pos && pos < fromPosition + itemCount) {
                this.mFocusPositionOffset += toPosition - fromPosition;
            } else if (fromPosition < pos && toPosition > pos - itemCount) {
                this.mFocusPositionOffset -= itemCount;
            } else if (fromPosition > pos && toPosition < pos) {
                this.mFocusPositionOffset += itemCount;
            }
        }
        this.mChildrenStates.clear();
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        int end = positionStart + itemCount;
        for (int i = positionStart; i < end; i++) {
            this.mChildrenStates.remove(i);
        }
    }

    @Override
    public boolean onRequestChildFocus(RecyclerView parent, View child, View focused) {
        if (!this.mFocusSearchDisabled && getPositionByView(child) != -1 && !this.mInLayout && !this.mInSelection && !this.mInScroll) {
            scrollToView(child, focused, true);
        }
        return true;
    }

    @Override
    public boolean requestChildRectangleOnScreen(RecyclerView parent, View view, Rect rect, boolean immediate) {
        return false;
    }

    private int getPrimarySystemScrollPosition(View view) {
        boolean isMax;
        boolean isMin;
        int viewCenterPrimary = this.mScrollOffsetPrimary + getViewCenter(view);
        int viewMin = getViewMin(view);
        int viewMax = getViewMax(view);
        if (!this.mReverseFlowPrimary) {
            isMin = this.mGrid.getFirstVisibleIndex() == 0;
            isMax = this.mGrid.getLastVisibleIndex() == (this.mState == null ? getItemCount() : this.mState.getItemCount()) + (-1);
        } else {
            isMax = this.mGrid.getFirstVisibleIndex() == 0;
            isMin = this.mGrid.getLastVisibleIndex() == (this.mState == null ? getItemCount() : this.mState.getItemCount()) + (-1);
        }
        int i = getChildCount() - 1;
        while (true) {
            if ((!isMin && !isMax) || i < 0) {
                break;
            }
            View v = getChildAt(i);
            if (v != view && v != null) {
                if (isMin && getViewMin(v) < viewMin) {
                    isMin = false;
                }
                if (isMax && getViewMax(v) > viewMax) {
                    isMax = false;
                }
            }
            i--;
        }
        return this.mWindowAlignment.mainAxis().getSystemScrollPos(viewCenterPrimary, isMin, isMax);
    }

    private int getPrimarySystemScrollPositionOfChildMax(View view) {
        int scrollPosition = getPrimarySystemScrollPosition(view);
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        int[] multipleAligns = lp.getAlignMultiple();
        if (multipleAligns != null && multipleAligns.length > 0) {
            return scrollPosition + (multipleAligns[multipleAligns.length - 1] - multipleAligns[0]);
        }
        return scrollPosition;
    }

    private int getAdjustedPrimaryScrollPosition(int scrollPrimary, View view, View childView) {
        int subindex = getSubPositionByView(view, childView);
        if (subindex != 0) {
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            return scrollPrimary + (lp.getAlignMultiple()[subindex] - lp.getAlignMultiple()[0]);
        }
        return scrollPrimary;
    }

    private int getSecondarySystemScrollPosition(View view) {
        boolean isMax;
        boolean isMin;
        int viewCenterSecondary = this.mScrollOffsetSecondary + getViewCenterSecondary(view);
        int pos = getPositionByView(view);
        Grid.Location location = this.mGrid.getLocation(pos);
        int row = location.row;
        if (!this.mReverseFlowSecondary) {
            isMin = row == 0;
            isMax = row == this.mGrid.getNumRows() + (-1);
        } else {
            isMax = row == 0;
            isMin = row == this.mGrid.getNumRows() + (-1);
        }
        return this.mWindowAlignment.secondAxis().getSystemScrollPos(viewCenterSecondary, isMin, isMax);
    }

    public void scrollToView(View view, boolean smooth) {
        scrollToView(view, view != null ? view.findFocus() : null, smooth);
    }

    private void scrollToView(View view, View childView, boolean smooth) {
        int newFocusPosition = getPositionByView(view);
        int newSubFocusPosition = getSubPositionByView(view, childView);
        if (newFocusPosition != this.mFocusPosition || newSubFocusPosition != this.mSubFocusPosition) {
            this.mFocusPosition = newFocusPosition;
            this.mSubFocusPosition = newSubFocusPosition;
            this.mFocusPositionOffset = 0;
            if (!this.mInLayout) {
                dispatchChildSelected();
            }
            if (this.mBaseGridView.isChildrenDrawingOrderEnabledInternal()) {
                this.mBaseGridView.invalidate();
            }
        }
        if (view == null) {
            return;
        }
        if (!view.hasFocus() && this.mBaseGridView.hasFocus()) {
            view.requestFocus();
        }
        if ((!this.mScrollEnabled && smooth) || !getScrollPosition(view, childView, sTwoInts)) {
            return;
        }
        scrollGrid(sTwoInts[0], sTwoInts[1], smooth);
    }

    public boolean getScrollPosition(View view, View childView, int[] deltas) {
        switch (this.mFocusScrollStrategy) {
            case 1:
            case 2:
                return getNoneAlignedPosition(view, deltas);
            default:
                return getAlignedPosition(view, childView, deltas);
        }
    }

    private boolean getNoneAlignedPosition(View view, int[] deltas) {
        View secondaryAlignedView;
        int pos = getPositionByView(view);
        int viewMin = getViewMin(view);
        int viewMax = getViewMax(view);
        View firstView = null;
        View lastView = null;
        int paddingLow = this.mWindowAlignment.mainAxis().getPaddingLow();
        int clientSize = this.mWindowAlignment.mainAxis().getClientSize();
        int row = this.mGrid.getRowIndex(pos);
        if (viewMin < paddingLow) {
            firstView = view;
            if (this.mFocusScrollStrategy == 2) {
                while (true) {
                    if (!prependOneColumnVisibleItems()) {
                        break;
                    }
                    CircularIntArray positions = this.mGrid.getItemPositionsInRows(this.mGrid.getFirstVisibleIndex(), pos)[row];
                    firstView = findViewByPosition(positions.get(0));
                    if (viewMax - getViewMin(firstView) > clientSize) {
                        if (positions.size() > 2) {
                            firstView = findViewByPosition(positions.get(2));
                        }
                    }
                }
            }
        } else if (viewMax > clientSize + paddingLow) {
            if (this.mFocusScrollStrategy == 2) {
                firstView = view;
                while (true) {
                    lastView = findViewByPosition(this.mGrid.getItemPositionsInRows(pos, this.mGrid.getLastVisibleIndex())[row].get(r5.size() - 1));
                    if (getViewMax(lastView) - viewMin > clientSize) {
                        lastView = null;
                        break;
                    }
                    if (!appendOneColumnVisibleItems()) {
                        break;
                    }
                }
                if (lastView != null) {
                    firstView = null;
                }
            } else {
                lastView = view;
            }
        }
        int scrollPrimary = 0;
        if (firstView != null) {
            scrollPrimary = getViewMin(firstView) - paddingLow;
        } else if (lastView != null) {
            scrollPrimary = getViewMax(lastView) - (paddingLow + clientSize);
        }
        if (firstView != null) {
            secondaryAlignedView = firstView;
        } else if (lastView != null) {
            secondaryAlignedView = lastView;
        } else {
            secondaryAlignedView = view;
        }
        int scrollSecondary = getSecondarySystemScrollPosition(secondaryAlignedView) - this.mScrollOffsetSecondary;
        if (scrollPrimary != 0 || scrollSecondary != 0) {
            deltas[0] = scrollPrimary;
            deltas[1] = scrollSecondary;
            return true;
        }
        return false;
    }

    private boolean getAlignedPosition(View view, View childView, int[] deltas) {
        int scrollPrimary = getPrimarySystemScrollPosition(view);
        if (childView != null) {
            scrollPrimary = getAdjustedPrimaryScrollPosition(scrollPrimary, view, childView);
        }
        int scrollSecondary = getSecondarySystemScrollPosition(view);
        int scrollPrimary2 = scrollPrimary - this.mScrollOffsetPrimary;
        int scrollSecondary2 = scrollSecondary - this.mScrollOffsetSecondary;
        int scrollPrimary3 = scrollPrimary2 + this.mPrimaryScrollExtra;
        if (scrollPrimary3 == 0 && scrollSecondary2 == 0) {
            return false;
        }
        deltas[0] = scrollPrimary3;
        deltas[1] = scrollSecondary2;
        return true;
    }

    private void scrollGrid(int scrollPrimary, int scrollSecondary, boolean smooth) {
        int scrollX;
        int scrollY;
        if (this.mInLayout) {
            scrollDirectionPrimary(scrollPrimary);
            scrollDirectionSecondary(scrollSecondary);
            return;
        }
        if (this.mOrientation == 0) {
            scrollX = scrollPrimary;
            scrollY = scrollSecondary;
        } else {
            scrollX = scrollSecondary;
            scrollY = scrollPrimary;
        }
        if (smooth) {
            this.mBaseGridView.smoothScrollBy(scrollX, scrollY);
        } else {
            this.mBaseGridView.scrollBy(scrollX, scrollY);
        }
    }

    private int findImmediateChildIndex(View view) {
        View view2;
        if (this.mBaseGridView != null && view != this.mBaseGridView && (view2 = findContainingItemView(view)) != null) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                if (getChildAt(i) == view2) {
                    return i;
                }
            }
            return -1;
        }
        return -1;
    }

    void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (!gainFocus) {
            return;
        }
        int i = this.mFocusPosition;
        while (true) {
            View view = findViewByPosition(i);
            if (view == null) {
                return;
            }
            if (view.getVisibility() != 0 || !view.hasFocusable()) {
                i++;
            } else {
                view.requestFocus();
                return;
            }
        }
    }

    @Override
    public View onInterceptFocusSearch(View focused, int direction) {
        if (this.mFocusSearchDisabled) {
            return focused;
        }
        FocusFinder ff = FocusFinder.getInstance();
        View result = null;
        if (direction == 2 || direction == 1) {
            if (canScrollVertically()) {
                int absDir = direction == 2 ? 130 : 33;
                result = ff.findNextFocus(this.mBaseGridView, focused, absDir);
            }
            if (canScrollHorizontally()) {
                boolean rtl = getLayoutDirection() == 1;
                int absDir2 = (direction == 2) ^ rtl ? 66 : 17;
                result = ff.findNextFocus(this.mBaseGridView, focused, absDir2);
            }
        } else {
            result = ff.findNextFocus(this.mBaseGridView, focused, direction);
        }
        if (result != null) {
            return result;
        }
        int movement = getMovement(direction);
        boolean isScroll = this.mBaseGridView.getScrollState() != 0;
        if (movement == 1) {
            if (isScroll || !this.mFocusOutEnd) {
                result = focused;
            }
            if (this.mScrollEnabled && !hasCreatedLastItem()) {
                processPendingMovement(true);
                result = focused;
            }
        } else if (movement == 0) {
            if (isScroll || !this.mFocusOutFront) {
                result = focused;
            }
            if (this.mScrollEnabled && !hasCreatedFirstItem()) {
                processPendingMovement(false);
                result = focused;
            }
        } else if (movement == 3) {
            if (isScroll || !this.mFocusOutSideEnd) {
                result = focused;
            }
        } else if (movement == 2 && (isScroll || !this.mFocusOutSideStart)) {
            result = focused;
        }
        if (result != null) {
            return result;
        }
        View result2 = this.mBaseGridView.getParent().focusSearch(focused, direction);
        if (result2 != null) {
            return result2;
        }
        return focused != null ? focused : this.mBaseGridView;
    }

    @Override
    public boolean onAddFocusables(RecyclerView recyclerView, ArrayList<View> views, int direction, int focusableMode) {
        int loop_start;
        if (this.mFocusSearchDisabled) {
            return true;
        }
        if (recyclerView.hasFocus()) {
            if (this.mPendingMoveSmoothScroller != null) {
                return true;
            }
            int movement = getMovement(direction);
            View focused = recyclerView.findFocus();
            int focusedIndex = findImmediateChildIndex(focused);
            int focusedPos = getPositionByIndex(focusedIndex);
            if (focusedPos != -1) {
                findViewByPosition(focusedPos).addFocusables(views, direction, focusableMode);
            }
            if (this.mGrid == null || getChildCount() == 0) {
                return true;
            }
            if ((movement == 3 || movement == 2) && this.mGrid.getNumRows() <= 1) {
                return true;
            }
            int focusedRow = (this.mGrid == null || focusedPos == -1) ? -1 : this.mGrid.getLocation(focusedPos).row;
            int focusableCount = views.size();
            int inc = (movement == 1 || movement == 3) ? 1 : -1;
            int loop_end = inc > 0 ? getChildCount() - 1 : 0;
            if (focusedIndex == -1) {
                loop_start = inc > 0 ? 0 : getChildCount() - 1;
            } else {
                loop_start = focusedIndex + inc;
            }
            int i = loop_start;
            while (true) {
                if (inc > 0) {
                    if (i > loop_end) {
                        return true;
                    }
                } else if (i < loop_end) {
                    return true;
                }
                View child = getChildAt(i);
                if (child.getVisibility() == 0 && child.hasFocusable()) {
                    if (focusedPos == -1) {
                        child.addFocusables(views, direction, focusableMode);
                        if (views.size() > focusableCount) {
                            return true;
                        }
                    } else {
                        int position = getPositionByIndex(i);
                        Grid.Location loc = this.mGrid.getLocation(position);
                        if (loc == null) {
                            continue;
                        } else if (movement == 1) {
                            if (loc.row == focusedRow && position > focusedPos) {
                                child.addFocusables(views, direction, focusableMode);
                                if (views.size() > focusableCount) {
                                    return true;
                                }
                            }
                        } else if (movement == 0) {
                            if (loc.row == focusedRow && position < focusedPos) {
                                child.addFocusables(views, direction, focusableMode);
                                if (views.size() > focusableCount) {
                                    return true;
                                }
                            }
                        } else if (movement == 3) {
                            if (loc.row == focusedRow) {
                                continue;
                            } else if (loc.row >= focusedRow) {
                                child.addFocusables(views, direction, focusableMode);
                            } else {
                                return true;
                            }
                        } else if (movement == 2 && loc.row != focusedRow) {
                            if (loc.row <= focusedRow) {
                                child.addFocusables(views, direction, focusableMode);
                            } else {
                                return true;
                            }
                        }
                    }
                }
                i += inc;
            }
        } else {
            int focusableCount2 = views.size();
            if (this.mFocusScrollStrategy != 0) {
                int left = this.mWindowAlignment.mainAxis().getPaddingLow();
                int right = this.mWindowAlignment.mainAxis().getClientSize() + left;
                int count = getChildCount();
                for (int i2 = 0; i2 < count; i2++) {
                    View child2 = getChildAt(i2);
                    if (child2.getVisibility() == 0 && getViewMin(child2) >= left && getViewMax(child2) <= right) {
                        child2.addFocusables(views, direction, focusableMode);
                    }
                }
                if (views.size() == focusableCount2) {
                    int count2 = getChildCount();
                    for (int i3 = 0; i3 < count2; i3++) {
                        View child3 = getChildAt(i3);
                        if (child3.getVisibility() == 0) {
                            child3.addFocusables(views, direction, focusableMode);
                        }
                    }
                }
            } else {
                View view = findViewByPosition(this.mFocusPosition);
                if (view != null) {
                    view.addFocusables(views, direction, focusableMode);
                }
            }
            if (views.size() == focusableCount2 && recyclerView.isFocusable()) {
                views.add(recyclerView);
                return true;
            }
            return true;
        }
    }

    public boolean hasCreatedLastItem() {
        int count = getItemCount();
        return count == 0 || this.mBaseGridView.findViewHolderForAdapterPosition(count + (-1)) != null;
    }

    public boolean hasCreatedFirstItem() {
        int count = getItemCount();
        return count == 0 || this.mBaseGridView.findViewHolderForAdapterPosition(0) != null;
    }

    boolean canScrollTo(View view) {
        if (view.getVisibility() != 0) {
            return false;
        }
        if (hasFocus()) {
            return view.hasFocusable();
        }
        return true;
    }

    boolean gridOnRequestFocusInDescendants(RecyclerView recyclerView, int direction, Rect previouslyFocusedRect) {
        switch (this.mFocusScrollStrategy) {
            case 1:
            case 2:
                return gridOnRequestFocusInDescendantsUnaligned(recyclerView, direction, previouslyFocusedRect);
            default:
                return gridOnRequestFocusInDescendantsAligned(recyclerView, direction, previouslyFocusedRect);
        }
    }

    private boolean gridOnRequestFocusInDescendantsAligned(RecyclerView recyclerView, int direction, Rect previouslyFocusedRect) {
        View view = findViewByPosition(this.mFocusPosition);
        if (view != null) {
            boolean result = view.requestFocus(direction, previouslyFocusedRect);
            if (!result) {
            }
            return result;
        }
        return false;
    }

    private boolean gridOnRequestFocusInDescendantsUnaligned(RecyclerView recyclerView, int direction, Rect previouslyFocusedRect) {
        int index;
        int increment;
        int end;
        int count = getChildCount();
        if ((direction & 2) != 0) {
            index = 0;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        }
        int left = this.mWindowAlignment.mainAxis().getPaddingLow();
        int right = this.mWindowAlignment.mainAxis().getClientSize() + left;
        for (int i = index; i != end; i += increment) {
            View child = getChildAt(i);
            if (child.getVisibility() == 0 && getViewMin(child) >= left && getViewMax(child) <= right && child.requestFocus(direction, previouslyFocusedRect)) {
                return true;
            }
        }
        return false;
    }

    private int getMovement(int direction) {
        if (this.mOrientation == 0) {
            switch (direction) {
                case 17:
                    if (!this.mReverseFlowPrimary) {
                    }
                    break;
                case 33:
                    break;
                case 66:
                    if (!this.mReverseFlowPrimary) {
                    }
                    break;
                case 130:
                    break;
            }
            return 17;
        }
        if (this.mOrientation != 1) {
            return 17;
        }
        switch (direction) {
            case 17:
                if (!this.mReverseFlowSecondary) {
                }
                break;
            case 33:
                break;
            case 66:
                if (!this.mReverseFlowSecondary) {
                }
                break;
            case 130:
                break;
        }
        return 17;
    }

    int getChildDrawingOrder(RecyclerView recyclerView, int childCount, int i) {
        int focusIndex;
        View view = findViewByPosition(this.mFocusPosition);
        if (view == null || i < (focusIndex = recyclerView.indexOfChild(view))) {
            return i;
        }
        if (i < childCount - 1) {
            return ((focusIndex + childCount) - 1) - i;
        }
        return focusIndex;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter adapter) {
        if (oldAdapter != null) {
            discardLayoutInfo();
            this.mFocusPosition = -1;
            this.mFocusPositionOffset = 0;
            this.mChildrenStates.clear();
        }
        if (adapter instanceof FacetProviderAdapter) {
            this.mFacetProviderAdapter = (FacetProviderAdapter) adapter;
        } else {
            this.mFacetProviderAdapter = null;
        }
        super.onAdapterChanged(oldAdapter, adapter);
    }

    private void discardLayoutInfo() {
        this.mGrid = null;
        this.mRowSizeSecondary = null;
        this.mRowSecondarySizeRefresh = false;
    }

    static final class SavedState implements Parcelable {
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
        Bundle childStates;
        int index;

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(this.index);
            out.writeBundle(this.childStates);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        SavedState(Parcel in) {
            this.childStates = Bundle.EMPTY;
            this.index = in.readInt();
            this.childStates = in.readBundle(GridLayoutManager.class.getClassLoader());
        }

        SavedState() {
            this.childStates = Bundle.EMPTY;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState();
        ss.index = getSelection();
        Bundle bundle = this.mChildrenStates.saveAsBundle();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            int position = getPositionByView(view);
            if (position != -1) {
                bundle = this.mChildrenStates.saveOnScreenView(bundle, view, position);
            }
        }
        ss.childStates = bundle;
        return ss;
    }

    void onChildRecycled(RecyclerView.ViewHolder holder) {
        int position = holder.getAdapterPosition();
        if (position == -1) {
            return;
        }
        this.mChildrenStates.saveOffscreenView(holder.itemView, position);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            return;
        }
        SavedState loadingState = (SavedState) state;
        this.mFocusPosition = loadingState.index;
        this.mFocusPositionOffset = 0;
        this.mChildrenStates.loadFromBundle(loadingState.childStates);
        this.mForceFullLayout = true;
        requestLayout();
    }

    @Override
    public int getRowCountForAccessibility(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (this.mOrientation == 0 && this.mGrid != null) {
            return this.mGrid.getNumRows();
        }
        return super.getRowCountForAccessibility(recycler, state);
    }

    @Override
    public int getColumnCountForAccessibility(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (this.mOrientation == 1 && this.mGrid != null) {
            return this.mGrid.getNumRows();
        }
        return super.getColumnCountForAccessibility(recycler, state);
    }

    @Override
    public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler, RecyclerView.State state, View host, AccessibilityNodeInfoCompat info) {
        ViewGroup.LayoutParams lp = host.getLayoutParams();
        if (this.mGrid == null || !(lp instanceof LayoutParams)) {
            super.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info);
            return;
        }
        LayoutParams glp = (LayoutParams) lp;
        int position = glp.getViewLayoutPosition();
        int rowIndex = this.mGrid.getRowIndex(position);
        int guessSpanIndex = position / this.mGrid.getNumRows();
        if (this.mOrientation == 0) {
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(rowIndex, 1, guessSpanIndex, 1, false, false));
        } else {
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(guessSpanIndex, 1, rowIndex, 1, false, false));
        }
    }

    @Override
    public boolean performAccessibilityAction(RecyclerView.Recycler recycler, RecyclerView.State state, int action, Bundle args) {
        saveContext(recycler, state);
        switch (action) {
            case 4096:
                processSelectionMoves(false, this.mState.getItemCount());
                break;
            case 8192:
                processSelectionMoves(false, -this.mState.getItemCount());
                break;
        }
        leaveContext();
        return true;
    }

    public int processSelectionMoves(boolean preventScroll, int moves) {
        if (this.mGrid == null) {
            return moves;
        }
        int focusPosition = this.mFocusPosition;
        int focusedRow = focusPosition != -1 ? this.mGrid.getRowIndex(focusPosition) : -1;
        View newSelected = null;
        int count = getChildCount();
        for (int i = 0; i < count && moves != 0; i++) {
            int index = moves > 0 ? i : (count - 1) - i;
            View child = getChildAt(index);
            if (canScrollTo(child)) {
                int position = getPositionByIndex(index);
                int rowIndex = this.mGrid.getRowIndex(position);
                if (focusedRow == -1) {
                    focusPosition = position;
                    newSelected = child;
                    focusedRow = rowIndex;
                } else if (rowIndex == focusedRow && ((moves > 0 && position > focusPosition) || (moves < 0 && position < focusPosition))) {
                    focusPosition = position;
                    newSelected = child;
                    moves = moves > 0 ? moves - 1 : moves + 1;
                }
            }
        }
        if (newSelected != null) {
            if (preventScroll) {
                if (hasFocus()) {
                    this.mInSelection = true;
                    newSelected.requestFocus();
                    this.mInSelection = false;
                }
                this.mFocusPosition = focusPosition;
                this.mSubFocusPosition = 0;
            } else {
                scrollToView(newSelected, true);
            }
        }
        return moves;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(RecyclerView.Recycler recycler, RecyclerView.State state, AccessibilityNodeInfoCompat info) {
        saveContext(recycler, state);
        if (this.mScrollEnabled && !hasCreatedFirstItem()) {
            info.addAction(8192);
            info.setScrollable(true);
        }
        if (this.mScrollEnabled && !hasCreatedLastItem()) {
            info.addAction(4096);
            info.setScrollable(true);
        }
        AccessibilityNodeInfoCompat.CollectionInfoCompat collectionInfo = AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(getRowCountForAccessibility(recycler, state), getColumnCountForAccessibility(recycler, state), isLayoutHierarchical(recycler, state), getSelectionModeForAccessibility(recycler, state));
        info.setCollectionInfo(collectionInfo);
        leaveContext();
    }
}
