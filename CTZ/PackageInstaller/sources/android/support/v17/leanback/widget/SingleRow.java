package android.support.v17.leanback.widget;

import android.support.v17.leanback.widget.Grid;
import android.support.v4.util.CircularIntArray;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;

class SingleRow extends Grid {
    private final Grid.Location mTmpLocation = new Grid.Location(0);

    SingleRow() {
        setNumRows(1);
    }

    @Override
    public final Grid.Location getLocation(int index) {
        return this.mTmpLocation;
    }

    int getStartIndexForAppend() {
        if (this.mLastVisibleIndex >= 0) {
            return this.mLastVisibleIndex + 1;
        }
        if (this.mStartIndex != -1) {
            return Math.min(this.mStartIndex, this.mProvider.getCount() - 1);
        }
        return 0;
    }

    int getStartIndexForPrepend() {
        if (this.mFirstVisibleIndex >= 0) {
            return this.mFirstVisibleIndex - 1;
        }
        if (this.mStartIndex != -1) {
            return Math.min(this.mStartIndex, this.mProvider.getCount() - 1);
        }
        return this.mProvider.getCount() - 1;
    }

    @Override
    protected final boolean prependVisibleItems(int toLimit, boolean oneColumnMode) {
        int edge;
        if (this.mProvider.getCount() == 0) {
            return false;
        }
        if (!oneColumnMode && checkPrependOverLimit(toLimit)) {
            return false;
        }
        boolean filledOne = false;
        int minIndex = this.mProvider.getMinIndex();
        for (int index = getStartIndexForPrepend(); index >= minIndex; index--) {
            int size = this.mProvider.createItem(index, false, this.mTmpItem, false);
            if (this.mFirstVisibleIndex < 0 || this.mLastVisibleIndex < 0) {
                edge = this.mReversedFlow ? Integer.MIN_VALUE : Preference.DEFAULT_ORDER;
                this.mFirstVisibleIndex = index;
                this.mLastVisibleIndex = index;
            } else {
                if (this.mReversedFlow) {
                    edge = this.mProvider.getEdge(index + 1) + this.mSpacing + size;
                } else {
                    edge = (this.mProvider.getEdge(index + 1) - this.mSpacing) - size;
                }
                this.mFirstVisibleIndex = index;
            }
            this.mProvider.addItem(this.mTmpItem[0], index, size, 0, edge);
            filledOne = true;
            if (oneColumnMode || checkPrependOverLimit(toLimit)) {
                break;
            }
        }
        return filledOne;
    }

    @Override
    protected final boolean appendVisibleItems(int toLimit, boolean oneColumnMode) {
        int edge;
        if (this.mProvider.getCount() == 0) {
            return false;
        }
        if (!oneColumnMode && checkAppendOverLimit(toLimit)) {
            return false;
        }
        boolean filledOne = false;
        for (int index = getStartIndexForAppend(); index < this.mProvider.getCount(); index++) {
            int size = this.mProvider.createItem(index, true, this.mTmpItem, false);
            if (this.mFirstVisibleIndex < 0 || this.mLastVisibleIndex < 0) {
                edge = this.mReversedFlow ? Preference.DEFAULT_ORDER : Integer.MIN_VALUE;
                this.mFirstVisibleIndex = index;
                this.mLastVisibleIndex = index;
            } else {
                if (this.mReversedFlow) {
                    edge = (this.mProvider.getEdge(index - 1) - this.mProvider.getSize(index - 1)) - this.mSpacing;
                } else {
                    edge = this.mProvider.getEdge(index - 1) + this.mProvider.getSize(index - 1) + this.mSpacing;
                }
                this.mLastVisibleIndex = index;
            }
            this.mProvider.addItem(this.mTmpItem[0], index, size, 0, edge);
            filledOne = true;
            if (oneColumnMode || checkAppendOverLimit(toLimit)) {
                break;
            }
        }
        return filledOne;
    }

    @Override
    public void collectAdjacentPrefetchPositions(int fromLimit, int da, RecyclerView.LayoutManager.LayoutPrefetchRegistry layoutPrefetchRegistry) {
        int indexToPrefetch;
        int itemSizeWithSpace;
        if (!this.mReversedFlow ? da < 0 : da > 0) {
            if (getFirstVisibleIndex() == 0) {
                return;
            }
            indexToPrefetch = getStartIndexForPrepend();
            itemSizeWithSpace = this.mProvider.getEdge(this.mFirstVisibleIndex) + (this.mReversedFlow ? this.mSpacing : -this.mSpacing);
        } else {
            int indexToPrefetch2 = getLastVisibleIndex();
            if (indexToPrefetch2 == this.mProvider.getCount() - 1) {
                return;
            }
            indexToPrefetch = getStartIndexForAppend();
            int itemSizeWithSpace2 = this.mProvider.getSize(this.mLastVisibleIndex) + this.mSpacing;
            itemSizeWithSpace = this.mProvider.getEdge(this.mLastVisibleIndex) + (this.mReversedFlow ? -itemSizeWithSpace2 : itemSizeWithSpace2);
        }
        int distance = Math.abs(itemSizeWithSpace - fromLimit);
        layoutPrefetchRegistry.addPosition(indexToPrefetch, distance);
    }

    @Override
    public final CircularIntArray[] getItemPositionsInRows(int startPos, int endPos) {
        this.mTmpItemPositionsInRows[0].clear();
        this.mTmpItemPositionsInRows[0].addLast(startPos);
        this.mTmpItemPositionsInRows[0].addLast(endPos);
        return this.mTmpItemPositionsInRows;
    }

    @Override
    protected final int findRowMin(boolean findLarge, int indexLimit, int[] indices) {
        if (indices != null) {
            indices[0] = 0;
            indices[1] = indexLimit;
        }
        return this.mReversedFlow ? this.mProvider.getEdge(indexLimit) - this.mProvider.getSize(indexLimit) : this.mProvider.getEdge(indexLimit);
    }

    @Override
    protected final int findRowMax(boolean findLarge, int indexLimit, int[] indices) {
        if (indices != null) {
            indices[0] = 0;
            indices[1] = indexLimit;
        }
        return this.mReversedFlow ? this.mProvider.getEdge(indexLimit) : this.mProvider.getEdge(indexLimit) + this.mProvider.getSize(indexLimit);
    }
}
