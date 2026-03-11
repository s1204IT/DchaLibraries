package android.support.v7.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import java.util.Arrays;

public class GridLayoutManager extends LinearLayoutManager {
    int[] mCachedBorders;
    final Rect mDecorInsets;
    boolean mPendingSpanCountChange;
    final SparseIntArray mPreLayoutSpanIndexCache;
    final SparseIntArray mPreLayoutSpanSizeCache;
    View[] mSet;
    int mSpanCount;
    SpanSizeLookup mSpanSizeLookup;

    public GridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mPendingSpanCountChange = false;
        this.mSpanCount = -1;
        this.mPreLayoutSpanSizeCache = new SparseIntArray();
        this.mPreLayoutSpanIndexCache = new SparseIntArray();
        this.mSpanSizeLookup = new DefaultSpanSizeLookup();
        this.mDecorInsets = new Rect();
        RecyclerView.LayoutManager.Properties properties = getProperties(context, attrs, defStyleAttr, defStyleRes);
        setSpanCount(properties.spanCount);
    }

    public GridLayoutManager(Context context, int spanCount) {
        super(context);
        this.mPendingSpanCountChange = false;
        this.mSpanCount = -1;
        this.mPreLayoutSpanSizeCache = new SparseIntArray();
        this.mPreLayoutSpanIndexCache = new SparseIntArray();
        this.mSpanSizeLookup = new DefaultSpanSizeLookup();
        this.mDecorInsets = new Rect();
        setSpanCount(spanCount);
    }

    public GridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        this.mPendingSpanCountChange = false;
        this.mSpanCount = -1;
        this.mPreLayoutSpanSizeCache = new SparseIntArray();
        this.mPreLayoutSpanIndexCache = new SparseIntArray();
        this.mSpanSizeLookup = new DefaultSpanSizeLookup();
        this.mDecorInsets = new Rect();
        setSpanCount(spanCount);
    }

    @Override
    public void setStackFromEnd(boolean stackFromEnd) {
        if (stackFromEnd) {
            throw new UnsupportedOperationException("GridLayoutManager does not support stack from end. Consider using reverse layout");
        }
        super.setStackFromEnd(false);
    }

    @Override
    public int getRowCountForAccessibility(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (this.mOrientation == 0) {
            return this.mSpanCount;
        }
        if (state.getItemCount() < 1) {
            return 0;
        }
        return getSpanGroupIndex(recycler, state, state.getItemCount() - 1) + 1;
    }

    @Override
    public int getColumnCountForAccessibility(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (this.mOrientation == 1) {
            return this.mSpanCount;
        }
        if (state.getItemCount() < 1) {
            return 0;
        }
        return getSpanGroupIndex(recycler, state, state.getItemCount() - 1) + 1;
    }

    @Override
    public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler, RecyclerView.State state, View host, AccessibilityNodeInfoCompat info) {
        ViewGroup.LayoutParams lp = host.getLayoutParams();
        if (!(lp instanceof LayoutParams)) {
            super.onInitializeAccessibilityNodeInfoForItem(host, info);
            return;
        }
        LayoutParams glp = (LayoutParams) lp;
        int spanGroupIndex = getSpanGroupIndex(recycler, state, glp.getViewLayoutPosition());
        if (this.mOrientation == 0) {
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(glp.getSpanIndex(), glp.getSpanSize(), spanGroupIndex, 1, this.mSpanCount > 1 && glp.getSpanSize() == this.mSpanCount, false));
        } else {
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(spanGroupIndex, 1, glp.getSpanIndex(), glp.getSpanSize(), this.mSpanCount > 1 && glp.getSpanSize() == this.mSpanCount, false));
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.isPreLayout()) {
            cachePreLayoutSpanMapping();
        }
        super.onLayoutChildren(recycler, state);
        clearPreLayoutSpanMappingCache();
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        this.mPendingSpanCountChange = false;
    }

    private void clearPreLayoutSpanMappingCache() {
        this.mPreLayoutSpanSizeCache.clear();
        this.mPreLayoutSpanIndexCache.clear();
    }

    private void cachePreLayoutSpanMapping() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            int viewPosition = lp.getViewLayoutPosition();
            this.mPreLayoutSpanSizeCache.put(viewPosition, lp.getSpanSize());
            this.mPreLayoutSpanIndexCache.put(viewPosition, lp.getSpanIndex());
        }
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        if (this.mOrientation == 0) {
            return new LayoutParams(-2, -1);
        }
        return new LayoutParams(-1, -2);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
        this.mSpanSizeLookup = spanSizeLookup;
    }

    public SpanSizeLookup getSpanSizeLookup() {
        return this.mSpanSizeLookup;
    }

    private void updateMeasurements() {
        int totalSpace;
        if (getOrientation() == 1) {
            totalSpace = (getWidth() - getPaddingRight()) - getPaddingLeft();
        } else {
            int totalSpace2 = getHeight();
            totalSpace = (totalSpace2 - getPaddingBottom()) - getPaddingTop();
        }
        calculateItemBorders(totalSpace);
    }

    @Override
    public void setMeasuredDimension(Rect childrenBounds, int wSpec, int hSpec) {
        int height;
        int width;
        if (this.mCachedBorders == null) {
            super.setMeasuredDimension(childrenBounds, wSpec, hSpec);
        }
        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        if (this.mOrientation == 1) {
            int usedHeight = childrenBounds.height() + verticalPadding;
            int height2 = chooseSize(hSpec, usedHeight, getMinimumHeight());
            int usedHeight2 = chooseSize(wSpec, this.mCachedBorders[this.mCachedBorders.length - 1] + horizontalPadding, getMinimumWidth());
            height = usedHeight2;
            width = height2;
        } else {
            int width2 = childrenBounds.width();
            int usedWidth = width2 + horizontalPadding;
            height = chooseSize(wSpec, usedWidth, getMinimumWidth());
            width = chooseSize(hSpec, this.mCachedBorders[this.mCachedBorders.length - 1] + verticalPadding, getMinimumHeight());
        }
        setMeasuredDimension(height, width);
    }

    private void calculateItemBorders(int totalSpace) {
        this.mCachedBorders = calculateItemBorders(this.mCachedBorders, this.mSpanCount, totalSpace);
    }

    static int[] calculateItemBorders(int[] cachedBorders, int spanCount, int totalSpace) {
        if (cachedBorders == null || cachedBorders.length != spanCount + 1 || cachedBorders[cachedBorders.length - 1] != totalSpace) {
            cachedBorders = new int[spanCount + 1];
        }
        cachedBorders[0] = 0;
        int sizePerSpan = totalSpace / spanCount;
        int sizePerSpanRemainder = totalSpace % spanCount;
        int consumedPixels = 0;
        int additionalSize = 0;
        for (int i = 1; i <= spanCount; i++) {
            int itemSize = sizePerSpan;
            additionalSize += sizePerSpanRemainder;
            if (additionalSize > 0 && spanCount - additionalSize < sizePerSpanRemainder) {
                itemSize++;
                additionalSize -= spanCount;
            }
            consumedPixels += itemSize;
            cachedBorders[i] = consumedPixels;
        }
        return cachedBorders;
    }

    int getSpaceForSpanRange(int startSpan, int spanSize) {
        if (this.mOrientation == 1 && isLayoutRTL()) {
            return this.mCachedBorders[this.mSpanCount - startSpan] - this.mCachedBorders[(this.mSpanCount - startSpan) - spanSize];
        }
        return this.mCachedBorders[startSpan + spanSize] - this.mCachedBorders[startSpan];
    }

    @Override
    void onAnchorReady(RecyclerView.Recycler recycler, RecyclerView.State state, LinearLayoutManager.AnchorInfo anchorInfo, int itemDirection) {
        super.onAnchorReady(recycler, state, anchorInfo, itemDirection);
        updateMeasurements();
        if (state.getItemCount() > 0 && !state.isPreLayout()) {
            ensureAnchorIsInCorrectSpan(recycler, state, anchorInfo, itemDirection);
        }
        ensureViewSet();
    }

    private void ensureViewSet() {
        if (this.mSet == null || this.mSet.length != this.mSpanCount) {
            this.mSet = new View[this.mSpanCount];
        }
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        updateMeasurements();
        ensureViewSet();
        return super.scrollHorizontallyBy(dx, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        updateMeasurements();
        ensureViewSet();
        return super.scrollVerticallyBy(dy, recycler, state);
    }

    private void ensureAnchorIsInCorrectSpan(RecyclerView.Recycler recycler, RecyclerView.State state, LinearLayoutManager.AnchorInfo anchorInfo, int itemDirection) {
        boolean layingOutInPrimaryDirection = itemDirection == 1;
        int span = getSpanIndex(recycler, state, anchorInfo.mPosition);
        if (!layingOutInPrimaryDirection) {
            int indexLimit = state.getItemCount() - 1;
            int pos = anchorInfo.mPosition;
            int pos2 = pos;
            int bestSpan = span;
            while (pos2 < indexLimit) {
                int next = getSpanIndex(recycler, state, pos2 + 1);
                if (next <= bestSpan) {
                    break;
                }
                pos2++;
                bestSpan = next;
            }
            anchorInfo.mPosition = pos2;
            return;
        }
        while (span > 0 && anchorInfo.mPosition > 0) {
            anchorInfo.mPosition--;
            span = getSpanIndex(recycler, state, anchorInfo.mPosition);
        }
    }

    @Override
    View findReferenceChild(RecyclerView.Recycler recycler, RecyclerView.State state, int start, int end, int itemCount) {
        ensureLayoutState();
        View outOfBoundsMatch = null;
        int boundsStart = this.mOrientationHelper.getStartAfterPadding();
        int boundsEnd = this.mOrientationHelper.getEndAfterPadding();
        int diff = end > start ? 1 : -1;
        View invalidMatch = null;
        for (int i = start; i != end; i += diff) {
            View view = getChildAt(i);
            int position = getPosition(view);
            if (position >= 0 && position < itemCount) {
                int span = getSpanIndex(recycler, state, position);
                if (span != 0) {
                    continue;
                } else if (((RecyclerView.LayoutParams) view.getLayoutParams()).isItemRemoved()) {
                    if (invalidMatch == null) {
                        invalidMatch = view;
                    }
                } else if (this.mOrientationHelper.getDecoratedStart(view) >= boundsEnd || this.mOrientationHelper.getDecoratedEnd(view) < boundsStart) {
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

    private int getSpanGroupIndex(RecyclerView.Recycler recycler, RecyclerView.State state, int viewPosition) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getSpanGroupIndex(viewPosition, this.mSpanCount);
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(viewPosition);
        if (adapterPosition == -1) {
            Log.w("GridLayoutManager", "Cannot find span size for pre layout position. " + viewPosition);
            return 0;
        }
        return this.mSpanSizeLookup.getSpanGroupIndex(adapterPosition, this.mSpanCount);
    }

    private int getSpanIndex(RecyclerView.Recycler recycler, RecyclerView.State state, int pos) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getCachedSpanIndex(pos, this.mSpanCount);
        }
        int cached = this.mPreLayoutSpanIndexCache.get(pos, -1);
        if (cached != -1) {
            return cached;
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition == -1) {
            Log.w("GridLayoutManager", "Cannot find span size for pre layout position. It is not cached, not in the adapter. Pos:" + pos);
            return 0;
        }
        return this.mSpanSizeLookup.getCachedSpanIndex(adapterPosition, this.mSpanCount);
    }

    private int getSpanSize(RecyclerView.Recycler recycler, RecyclerView.State state, int pos) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getSpanSize(pos);
        }
        int cached = this.mPreLayoutSpanSizeCache.get(pos, -1);
        if (cached != -1) {
            return cached;
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition == -1) {
            Log.w("GridLayoutManager", "Cannot find span size for pre layout position. It is not cached, not in the adapter. Pos:" + pos);
            return 1;
        }
        return this.mSpanSizeLookup.getSpanSize(adapterPosition);
    }

    @Override
    void collectPrefetchPositionsForLayoutState(RecyclerView.State state, LinearLayoutManager.LayoutState layoutState, RecyclerView.LayoutManager.LayoutPrefetchRegistry layoutPrefetchRegistry) {
        int remainingSpan = this.mSpanCount;
        int remainingSpan2 = remainingSpan;
        for (int remainingSpan3 = 0; remainingSpan3 < this.mSpanCount && layoutState.hasMore(state) && remainingSpan2 > 0; remainingSpan3++) {
            int pos = layoutState.mCurrentPosition;
            layoutPrefetchRegistry.addPosition(pos, Math.max(0, layoutState.mScrollingOffset));
            int spanSize = this.mSpanSizeLookup.getSpanSize(pos);
            remainingSpan2 -= spanSize;
            layoutState.mCurrentPosition += layoutState.mItemDirection;
        }
    }

    @Override
    void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, LinearLayoutManager.LayoutState layoutState, LinearLayoutManager.LayoutChunkResult result) {
        int maxSize;
        int left;
        int right;
        int top;
        int bottom;
        float maxSizeInOther;
        int otherDirSpecMode;
        int remainingSpan;
        int wSpec;
        int hSpec;
        View view;
        int otherDirSpecMode2 = this.mOrientationHelper.getModeInOther();
        ?? r13 = 0;
        boolean flexibleInOtherDir = otherDirSpecMode2 != 1073741824;
        int currentOtherDirSize = getChildCount() > 0 ? this.mCachedBorders[this.mSpanCount] : 0;
        if (flexibleInOtherDir) {
            updateMeasurements();
        }
        boolean layingOutInPrimaryDirection = layoutState.mItemDirection == 1;
        int remainingSpan2 = this.mSpanCount;
        if (!layingOutInPrimaryDirection) {
            int itemSpanIndex = getSpanIndex(recycler, state, layoutState.mCurrentPosition);
            int itemSpanSize = getSpanSize(recycler, state, layoutState.mCurrentPosition);
            remainingSpan2 = itemSpanIndex + itemSpanSize;
        }
        int count = 0;
        int consumedSpanCount = 0;
        while (count < this.mSpanCount && layoutState.hasMore(state) && remainingSpan2 > 0) {
            int pos = layoutState.mCurrentPosition;
            int spanSize = getSpanSize(recycler, state, pos);
            if (spanSize > this.mSpanCount) {
                throw new IllegalArgumentException("Item at position " + pos + " requires " + spanSize + " spans but GridLayoutManager has only " + this.mSpanCount + " spans.");
            }
            remainingSpan2 -= spanSize;
            if (remainingSpan2 < 0 || (view = layoutState.next(recycler)) == null) {
                break;
            }
            consumedSpanCount += spanSize;
            this.mSet[count] = view;
            count++;
        }
        int remainingSpan3 = remainingSpan2;
        if (count == 0) {
            result.mFinished = true;
            return;
        }
        int i = count;
        int count2 = count;
        int count3 = consumedSpanCount;
        assignSpans(recycler, state, i, count3, layingOutInPrimaryDirection);
        int i2 = 0;
        int maxSize2 = 0;
        float maxSizeInOther2 = 0.0f;
        while (i2 < count2) {
            View view2 = this.mSet[i2];
            if (layoutState.mScrapList == null) {
                if (layingOutInPrimaryDirection) {
                    addView(view2);
                } else {
                    addView(view2, r13);
                }
            } else if (layingOutInPrimaryDirection) {
                addDisappearingView(view2);
            } else {
                addDisappearingView(view2, r13);
            }
            calculateItemDecorationsForChild(view2, this.mDecorInsets);
            measureChild(view2, otherDirSpecMode2, r13);
            int size = this.mOrientationHelper.getDecoratedMeasurement(view2);
            if (size > maxSize2) {
                maxSize2 = size;
            }
            float otherSize = (1.0f * this.mOrientationHelper.getDecoratedMeasurementInOther(view2)) / ((LayoutParams) view2.getLayoutParams()).mSpanSize;
            if (otherSize > maxSizeInOther2) {
                maxSizeInOther2 = otherSize;
            }
            i2++;
            r13 = 0;
        }
        if (flexibleInOtherDir) {
            guessMeasurement(maxSizeInOther2, currentOtherDirSize);
            int maxSize3 = 0;
            for (int maxSize4 = 0; maxSize4 < count2; maxSize4++) {
                View view3 = this.mSet[maxSize4];
                measureChild(view3, 1073741824, true);
                int size2 = this.mOrientationHelper.getDecoratedMeasurement(view3);
                if (size2 > maxSize3) {
                    maxSize3 = size2;
                }
            }
            maxSize = maxSize3;
        } else {
            maxSize = maxSize2;
        }
        int i3 = 0;
        while (i3 < count2) {
            View view4 = this.mSet[i3];
            if (this.mOrientationHelper.getDecoratedMeasurement(view4) == maxSize) {
                maxSizeInOther = maxSizeInOther2;
                otherDirSpecMode = otherDirSpecMode2;
                remainingSpan = remainingSpan3;
            } else {
                LayoutParams lp = (LayoutParams) view4.getLayoutParams();
                Rect decorInsets = lp.mDecorInsets;
                maxSizeInOther = maxSizeInOther2;
                int verticalInsets = decorInsets.top + decorInsets.bottom + lp.topMargin + lp.bottomMargin;
                int horizontalInsets = decorInsets.left + decorInsets.right + lp.leftMargin + lp.rightMargin;
                int totalSpaceInOther = getSpaceForSpanRange(lp.mSpanIndex, lp.mSpanSize);
                otherDirSpecMode = otherDirSpecMode2;
                if (this.mOrientation == 1) {
                    remainingSpan = remainingSpan3;
                    wSpec = getChildMeasureSpec(totalSpaceInOther, 1073741824, horizontalInsets, lp.width, false);
                    hSpec = View.MeasureSpec.makeMeasureSpec(maxSize - verticalInsets, 1073741824);
                } else {
                    remainingSpan = remainingSpan3;
                    wSpec = View.MeasureSpec.makeMeasureSpec(maxSize - horizontalInsets, 1073741824);
                    hSpec = getChildMeasureSpec(totalSpaceInOther, 1073741824, verticalInsets, lp.height, false);
                }
                measureChildWithDecorationsAndMargin(view4, wSpec, hSpec, true);
            }
            i3++;
            maxSizeInOther2 = maxSizeInOther;
            otherDirSpecMode2 = otherDirSpecMode;
            remainingSpan3 = remainingSpan;
        }
        float maxSizeInOther3 = maxSizeInOther2;
        result.mConsumed = maxSize;
        int left2 = 0;
        int right2 = 0;
        int top2 = 0;
        int bottom2 = 0;
        if (this.mOrientation == 1) {
            if (layoutState.mLayoutDirection == -1) {
                bottom2 = layoutState.mOffset;
                top2 = bottom2 - maxSize;
            } else {
                top2 = layoutState.mOffset;
                bottom2 = top2 + maxSize;
            }
        } else if (layoutState.mLayoutDirection == -1) {
            right2 = layoutState.mOffset;
            left2 = right2 - maxSize;
        } else {
            left2 = layoutState.mOffset;
            right2 = left2 + maxSize;
        }
        int i4 = 0;
        while (true) {
            int i5 = i4;
            if (i5 < count2) {
                View view5 = this.mSet[i5];
                LayoutParams params = (LayoutParams) view5.getLayoutParams();
                if (this.mOrientation == 1) {
                    if (isLayoutRTL()) {
                        int right3 = getPaddingLeft() + this.mCachedBorders[this.mSpanCount - params.mSpanIndex];
                        left = right3 - this.mOrientationHelper.getDecoratedMeasurementInOther(view5);
                        top = top2;
                        bottom = bottom2;
                        right = right3;
                    } else {
                        int left3 = getPaddingLeft() + this.mCachedBorders[params.mSpanIndex];
                        left = left3;
                        right = this.mOrientationHelper.getDecoratedMeasurementInOther(view5) + left3;
                        top = top2;
                        bottom = bottom2;
                    }
                } else {
                    left = left2;
                    right = right2;
                    int top3 = getPaddingTop() + this.mCachedBorders[params.mSpanIndex];
                    top = top3;
                    bottom = this.mOrientationHelper.getDecoratedMeasurementInOther(view5) + top3;
                }
                int maxSize5 = maxSize;
                int maxSize6 = right;
                float maxSizeInOther4 = maxSizeInOther3;
                layoutDecoratedWithMargins(view5, left, top, maxSize6, bottom);
                if (params.isItemRemoved() || params.isItemChanged()) {
                    result.mIgnoreConsumed = true;
                }
                result.mFocusable |= view5.hasFocusable();
                i4 = i5 + 1;
                top2 = top;
                bottom2 = bottom;
                maxSize = maxSize5;
                maxSizeInOther3 = maxSizeInOther4;
                left2 = left;
                right2 = right;
            } else {
                Arrays.fill(this.mSet, (Object) null);
                return;
            }
        }
    }

    private void measureChild(View view, int otherDirParentSpecMode, boolean alreadyMeasured) {
        int hSpec;
        int wSpec;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        Rect decorInsets = lp.mDecorInsets;
        int verticalInsets = decorInsets.top + decorInsets.bottom + lp.topMargin + lp.bottomMargin;
        int horizontalInsets = decorInsets.left + decorInsets.right + lp.leftMargin + lp.rightMargin;
        int availableSpaceInOther = getSpaceForSpanRange(lp.mSpanIndex, lp.mSpanSize);
        if (this.mOrientation == 1) {
            wSpec = getChildMeasureSpec(availableSpaceInOther, otherDirParentSpecMode, horizontalInsets, lp.width, false);
            hSpec = getChildMeasureSpec(this.mOrientationHelper.getTotalSpace(), getHeightMode(), verticalInsets, lp.height, true);
        } else {
            int wSpec2 = lp.height;
            hSpec = getChildMeasureSpec(availableSpaceInOther, otherDirParentSpecMode, verticalInsets, wSpec2, false);
            wSpec = getChildMeasureSpec(this.mOrientationHelper.getTotalSpace(), getWidthMode(), horizontalInsets, lp.width, true);
        }
        measureChildWithDecorationsAndMargin(view, wSpec, hSpec, alreadyMeasured);
    }

    private void guessMeasurement(float maxSizeInOther, int currentOtherDirSize) {
        int contentSize = Math.round(this.mSpanCount * maxSizeInOther);
        calculateItemBorders(Math.max(contentSize, currentOtherDirSize));
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec, boolean alreadyMeasured) {
        boolean measure;
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        if (alreadyMeasured) {
            measure = shouldReMeasureChild(child, widthSpec, heightSpec, lp);
        } else {
            measure = shouldMeasureChild(child, widthSpec, heightSpec, lp);
        }
        if (measure) {
            child.measure(widthSpec, heightSpec);
        }
    }

    private void assignSpans(RecyclerView.Recycler recycler, RecyclerView.State state, int count, int consumedSpanCount, boolean layingOutInPrimaryDirection) {
        int start;
        int end;
        int diff;
        if (layingOutInPrimaryDirection) {
            start = 0;
            end = count;
            diff = 1;
        } else {
            start = count - 1;
            end = -1;
            diff = -1;
        }
        int span = 0;
        for (int span2 = start; span2 != end; span2 += diff) {
            View view = this.mSet[span2];
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            params.mSpanSize = getSpanSize(recycler, state, getPosition(view));
            params.mSpanIndex = span;
            span += params.mSpanSize;
        }
    }

    public int getSpanCount() {
        return this.mSpanCount;
    }

    public void setSpanCount(int spanCount) {
        if (spanCount == this.mSpanCount) {
            return;
        }
        this.mPendingSpanCountChange = true;
        if (spanCount < 1) {
            throw new IllegalArgumentException("Span count should be at least 1. Provided " + spanCount);
        }
        this.mSpanCount = spanCount;
        this.mSpanSizeLookup.invalidateSpanIndexCache();
        requestLayout();
    }

    public static abstract class SpanSizeLookup {
        final SparseIntArray mSpanIndexCache = new SparseIntArray();
        private boolean mCacheSpanIndices = false;

        public abstract int getSpanSize(int i);

        public void invalidateSpanIndexCache() {
            this.mSpanIndexCache.clear();
        }

        int getCachedSpanIndex(int position, int spanCount) {
            if (!this.mCacheSpanIndices) {
                return getSpanIndex(position, spanCount);
            }
            int existing = this.mSpanIndexCache.get(position, -1);
            if (existing != -1) {
                return existing;
            }
            int value = getSpanIndex(position, spanCount);
            this.mSpanIndexCache.put(position, value);
            return value;
        }

        public int getSpanIndex(int position, int spanCount) {
            int prevKey;
            int positionSpanSize = getSpanSize(position);
            if (positionSpanSize == spanCount) {
                return 0;
            }
            int span = 0;
            int startPos = 0;
            if (this.mCacheSpanIndices && this.mSpanIndexCache.size() > 0 && (prevKey = findReferenceIndexFromCache(position)) >= 0) {
                span = this.mSpanIndexCache.get(prevKey) + getSpanSize(prevKey);
                startPos = prevKey + 1;
            }
            int span2 = span;
            for (int span3 = startPos; span3 < position; span3++) {
                int size = getSpanSize(span3);
                span2 += size;
                if (span2 == spanCount) {
                    span2 = 0;
                } else if (span2 > spanCount) {
                    span2 = size;
                }
            }
            int i = span2 + positionSpanSize;
            if (i > spanCount) {
                return 0;
            }
            return span2;
        }

        int findReferenceIndexFromCache(int position) {
            int lo = 0;
            int hi = this.mSpanIndexCache.size() - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int midVal = this.mSpanIndexCache.keyAt(mid);
                if (midVal < position) {
                    lo = mid + 1;
                } else {
                    int hi2 = mid - 1;
                    hi = hi2;
                }
            }
            int index = lo - 1;
            if (index >= 0 && index < this.mSpanIndexCache.size()) {
                return this.mSpanIndexCache.keyAt(index);
            }
            return -1;
        }

        public int getSpanGroupIndex(int adapterPosition, int spanCount) {
            int span = 0;
            int group = 0;
            int positionSpanSize = getSpanSize(adapterPosition);
            for (int i = 0; i < adapterPosition; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                    group++;
                } else if (span > spanCount) {
                    span = size;
                    group++;
                }
            }
            int i2 = span + positionSpanSize;
            if (i2 > spanCount) {
                return group + 1;
            }
            return group;
        }
    }

    @Override
    public View onFocusSearchFailed(View focused, int focusDirection, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int start;
        int inc;
        int limit;
        View prevFocusedChild;
        int focusableSpanGroupIndex;
        int focusableWeakCandidateSpanIndex;
        int focusableWeakCandidateOverlap;
        int overlap;
        boolean z;
        RecyclerView.Recycler recycler2 = recycler;
        RecyclerView.State state2 = state;
        View prevFocusedChild2 = findContainingItemView(focused);
        if (prevFocusedChild2 != null) {
            LayoutParams lp = (LayoutParams) prevFocusedChild2.getLayoutParams();
            int prevSpanStart = lp.mSpanIndex;
            int prevSpanEnd = lp.mSpanIndex + lp.mSpanSize;
            View view = super.onFocusSearchFailed(focused, focusDirection, recycler, state);
            if (view != null) {
                int layoutDir = convertFocusDirectionToLayoutDirection(focusDirection);
                boolean ascend = (layoutDir == 1) != this.mShouldReverseLayout;
                if (ascend) {
                    start = getChildCount() - 1;
                    inc = -1;
                    limit = -1;
                } else {
                    start = 0;
                    inc = 1;
                    limit = getChildCount();
                }
                boolean preferLastSpan = this.mOrientation == 1 && isLayoutRTL();
                View unfocusableWeakCandidate = null;
                int focusableSpanGroupIndex2 = getSpanGroupIndex(recycler2, state2, start);
                int focusableWeakCandidateSpanIndex2 = -1;
                int focusableWeakCandidateOverlap2 = 0;
                int unfocusableWeakCandidateOverlap = -1;
                int layoutDir2 = 0;
                View focusableWeakCandidate = null;
                int i = start;
                while (true) {
                    int i2 = i;
                    boolean ascend2 = ascend;
                    if (i2 == limit) {
                        break;
                    }
                    int start2 = start;
                    int start3 = getSpanGroupIndex(recycler2, state2, i2);
                    View candidate = getChildAt(i2);
                    if (candidate == prevFocusedChild2) {
                        break;
                    }
                    if (!candidate.hasFocusable() || start3 == focusableSpanGroupIndex2) {
                        LayoutParams candidateLp = (LayoutParams) candidate.getLayoutParams();
                        prevFocusedChild = prevFocusedChild2;
                        int candidateStart = candidateLp.mSpanIndex;
                        focusableSpanGroupIndex = focusableSpanGroupIndex2;
                        int focusableSpanGroupIndex3 = candidateLp.mSpanIndex;
                        int spanGroupIndex = candidateLp.mSpanSize;
                        int candidateEnd = focusableSpanGroupIndex3 + spanGroupIndex;
                        if (candidate.hasFocusable() && candidateStart == prevSpanStart && candidateEnd == prevSpanEnd) {
                            return candidate;
                        }
                        if (!(candidate.hasFocusable() && focusableWeakCandidate == null) && (candidate.hasFocusable() || unfocusableWeakCandidate != null)) {
                            int maxStart = Math.max(candidateStart, prevSpanStart);
                            int minEnd = Math.min(candidateEnd, prevSpanEnd);
                            int overlap2 = minEnd - maxStart;
                            if (!candidate.hasFocusable()) {
                                focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                                if (focusableWeakCandidate == null) {
                                    focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                                    if (isViewPartiallyVisible(candidate, false, true)) {
                                        if (overlap2 > layoutDir2) {
                                            overlap = 1;
                                        } else if (overlap2 == layoutDir2) {
                                            if (preferLastSpan == (candidateStart > unfocusableWeakCandidateOverlap)) {
                                                overlap = 1;
                                            }
                                        }
                                        if (overlap == 0) {
                                        }
                                    }
                                    i = i2 + inc;
                                    ascend = ascend2;
                                    start = start2;
                                    prevFocusedChild2 = prevFocusedChild;
                                    focusableSpanGroupIndex2 = focusableSpanGroupIndex;
                                    recycler2 = recycler;
                                    state2 = state;
                                } else {
                                    focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                                }
                            } else if (overlap2 > focusableWeakCandidateOverlap2) {
                                focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                                focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                                overlap = 1;
                                if (overlap == 0) {
                                    if (candidate.hasFocusable()) {
                                        int focusableWeakCandidateSpanIndex3 = candidateLp.mSpanIndex;
                                        int focusableWeakCandidateOverlap3 = Math.min(candidateEnd, prevSpanEnd) - Math.max(candidateStart, prevSpanStart);
                                        focusableWeakCandidate = candidate;
                                        focusableWeakCandidateSpanIndex2 = focusableWeakCandidateSpanIndex3;
                                        focusableWeakCandidateOverlap2 = focusableWeakCandidateOverlap3;
                                    } else {
                                        int unfocusableWeakCandidateSpanIndex = candidateLp.mSpanIndex;
                                        int unfocusableWeakCandidateSpanIndex2 = Math.min(candidateEnd, prevSpanEnd);
                                        int unfocusableWeakCandidateOverlap2 = unfocusableWeakCandidateSpanIndex2 - Math.max(candidateStart, prevSpanStart);
                                        unfocusableWeakCandidate = candidate;
                                        layoutDir2 = unfocusableWeakCandidateOverlap2;
                                        focusableWeakCandidateSpanIndex2 = focusableWeakCandidateSpanIndex;
                                        unfocusableWeakCandidateOverlap = unfocusableWeakCandidateSpanIndex;
                                        focusableWeakCandidateOverlap2 = focusableWeakCandidateOverlap;
                                    }
                                }
                                i = i2 + inc;
                                ascend = ascend2;
                                start = start2;
                                prevFocusedChild2 = prevFocusedChild;
                                focusableSpanGroupIndex2 = focusableSpanGroupIndex;
                                recycler2 = recycler;
                                state2 = state;
                            } else if (overlap2 == focusableWeakCandidateOverlap2) {
                                if (candidateStart > focusableWeakCandidateSpanIndex2) {
                                    focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                                    z = true;
                                } else {
                                    focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                                    z = false;
                                }
                                if (preferLastSpan == z) {
                                    overlap = 1;
                                } else {
                                    focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                                }
                            } else {
                                focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                                focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                            }
                            overlap = 0;
                            if (overlap == 0) {
                            }
                            i = i2 + inc;
                            ascend = ascend2;
                            start = start2;
                            prevFocusedChild2 = prevFocusedChild;
                            focusableSpanGroupIndex2 = focusableSpanGroupIndex;
                            recycler2 = recycler;
                            state2 = state;
                        } else {
                            overlap = 1;
                            focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                        }
                        focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                        if (overlap == 0) {
                        }
                        i = i2 + inc;
                        ascend = ascend2;
                        start = start2;
                        prevFocusedChild2 = prevFocusedChild;
                        focusableSpanGroupIndex2 = focusableSpanGroupIndex;
                        recycler2 = recycler;
                        state2 = state;
                    } else {
                        if (focusableWeakCandidate != null) {
                            break;
                        }
                        prevFocusedChild = prevFocusedChild2;
                        focusableWeakCandidateSpanIndex = focusableWeakCandidateSpanIndex2;
                        focusableWeakCandidateOverlap = focusableWeakCandidateOverlap2;
                        focusableSpanGroupIndex = focusableSpanGroupIndex2;
                    }
                    focusableWeakCandidateSpanIndex2 = focusableWeakCandidateSpanIndex;
                    focusableWeakCandidateOverlap2 = focusableWeakCandidateOverlap;
                    i = i2 + inc;
                    ascend = ascend2;
                    start = start2;
                    prevFocusedChild2 = prevFocusedChild;
                    focusableSpanGroupIndex2 = focusableSpanGroupIndex;
                    recycler2 = recycler;
                    state2 = state;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return this.mPendingSavedState == null && !this.mPendingSpanCountChange;
    }

    public static final class DefaultSpanSizeLookup extends SpanSizeLookup {
        @Override
        public int getSpanSize(int position) {
            return 1;
        }

        @Override
        public int getSpanIndex(int position, int spanCount) {
            return position % spanCount;
        }
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {
        int mSpanIndex;
        int mSpanSize;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.mSpanIndex = -1;
            this.mSpanSize = 0;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.mSpanIndex = -1;
            this.mSpanSize = 0;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
            this.mSpanIndex = -1;
            this.mSpanSize = 0;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.mSpanIndex = -1;
            this.mSpanSize = 0;
        }

        public int getSpanIndex() {
            return this.mSpanIndex;
        }

        public int getSpanSize() {
            return this.mSpanSize;
        }
    }
}
