package android.support.v17.leanback.widget;

import android.support.v4.util.CircularIntArray;

abstract class Grid {
    protected int mMargin;
    protected int mNumRows;
    protected Provider mProvider;
    protected boolean mReversedFlow;
    protected CircularIntArray[] mTmpItemPositionsInRows;
    protected int mFirstVisibleIndex = -1;
    protected int mLastVisibleIndex = -1;
    protected int mStartIndex = -1;

    public interface Provider {
        void addItem(Object obj, int i, int i2, int i3, int i4);

        int createItem(int i, boolean z, Object[] objArr);

        int getCount();

        int getEdge(int i);

        int getSize(int i);

        void removeItem(int i);
    }

    protected abstract boolean appendVisibleItems(int i, boolean z);

    protected abstract int findRowMax(boolean z, int i, int[] iArr);

    protected abstract int findRowMin(boolean z, int i, int[] iArr);

    public abstract CircularIntArray[] getItemPositionsInRows(int i, int i2);

    public abstract Location getLocation(int i);

    protected abstract boolean prependVisibleItems(int i, boolean z);

    Grid() {
    }

    public static class Location {
        public int row;

        public Location(int row) {
            this.row = row;
        }
    }

    public static Grid createGrid(int rows) {
        if (rows == 1) {
            return new SingleRow();
        }
        Grid grid = new StaggeredGridDefault();
        grid.setNumRows(rows);
        return grid;
    }

    public final void setMargin(int margin) {
        this.mMargin = margin;
    }

    public final void setReversedFlow(boolean reversedFlow) {
        this.mReversedFlow = reversedFlow;
    }

    public boolean isReversedFlow() {
        return this.mReversedFlow;
    }

    public void setProvider(Provider provider) {
        this.mProvider = provider;
    }

    public void setStart(int startIndex) {
        this.mStartIndex = startIndex;
    }

    public int getNumRows() {
        return this.mNumRows;
    }

    void setNumRows(int numRows) {
        if (numRows <= 0) {
            throw new IllegalArgumentException();
        }
        if (this.mNumRows == numRows) {
            return;
        }
        this.mNumRows = numRows;
        this.mTmpItemPositionsInRows = new CircularIntArray[this.mNumRows];
        for (int i = 0; i < this.mNumRows; i++) {
            this.mTmpItemPositionsInRows[i] = new CircularIntArray();
        }
    }

    public final int getFirstVisibleIndex() {
        return this.mFirstVisibleIndex;
    }

    public final int getLastVisibleIndex() {
        return this.mLastVisibleIndex;
    }

    public void resetVisibleIndex() {
        this.mLastVisibleIndex = -1;
        this.mFirstVisibleIndex = -1;
    }

    public void invalidateItemsAfter(int index) {
        if (index < 0 || this.mLastVisibleIndex < 0) {
            return;
        }
        while (this.mLastVisibleIndex >= index) {
            this.mProvider.removeItem(this.mLastVisibleIndex);
            this.mLastVisibleIndex--;
        }
        resetVisbileIndexIfEmpty();
        if (getFirstVisibleIndex() >= 0) {
            return;
        }
        setStart(index);
    }

    public final int getRowIndex(int index) {
        return getLocation(index).row;
    }

    public final int findRowMin(boolean findLarge, int[] indices) {
        return findRowMin(findLarge, this.mReversedFlow ? this.mLastVisibleIndex : this.mFirstVisibleIndex, indices);
    }

    public final int findRowMax(boolean findLarge, int[] indices) {
        return findRowMax(findLarge, this.mReversedFlow ? this.mFirstVisibleIndex : this.mLastVisibleIndex, indices);
    }

    protected final boolean checkAppendOverLimit(int toLimit) {
        if (this.mLastVisibleIndex < 0) {
            return false;
        }
        return this.mReversedFlow ? findRowMin(true, null) <= this.mMargin + toLimit : findRowMax(false, null) >= toLimit - this.mMargin;
    }

    protected final boolean checkPrependOverLimit(int toLimit) {
        if (this.mLastVisibleIndex < 0) {
            return false;
        }
        return this.mReversedFlow ? findRowMax(false, null) >= this.mMargin + toLimit : findRowMin(true, null) <= toLimit - this.mMargin;
    }

    public final CircularIntArray[] getItemPositionsInRows() {
        return getItemPositionsInRows(getFirstVisibleIndex(), getLastVisibleIndex());
    }

    public final boolean prependOneColumnVisibleItems() {
        return prependVisibleItems(this.mReversedFlow ? Integer.MIN_VALUE : Integer.MAX_VALUE, true);
    }

    public final void prependVisibleItems(int toLimit) {
        prependVisibleItems(toLimit, false);
    }

    public boolean appendOneColumnVisibleItems() {
        return appendVisibleItems(this.mReversedFlow ? Integer.MAX_VALUE : Integer.MIN_VALUE, true);
    }

    public final void appendVisibleItems(int toLimit) {
        appendVisibleItems(toLimit, false);
    }

    public void removeInvisibleItemsAtEnd(int aboveIndex, int toLimit) {
        boolean offEnd;
        while (this.mLastVisibleIndex >= this.mFirstVisibleIndex && this.mLastVisibleIndex > aboveIndex) {
            if (this.mReversedFlow) {
                offEnd = this.mProvider.getEdge(this.mLastVisibleIndex) <= toLimit;
            } else {
                offEnd = this.mProvider.getEdge(this.mLastVisibleIndex) >= toLimit;
            }
            if (!offEnd) {
                break;
            }
            this.mProvider.removeItem(this.mLastVisibleIndex);
            this.mLastVisibleIndex--;
        }
        resetVisbileIndexIfEmpty();
    }

    public void removeInvisibleItemsAtFront(int belowIndex, int toLimit) {
        boolean offFront;
        while (this.mLastVisibleIndex >= this.mFirstVisibleIndex && this.mFirstVisibleIndex < belowIndex) {
            if (!this.mReversedFlow) {
                offFront = this.mProvider.getEdge(this.mFirstVisibleIndex) + this.mProvider.getSize(this.mFirstVisibleIndex) <= toLimit;
            } else {
                offFront = this.mProvider.getEdge(this.mFirstVisibleIndex) - this.mProvider.getSize(this.mFirstVisibleIndex) >= toLimit;
            }
            if (!offFront) {
                break;
            }
            this.mProvider.removeItem(this.mFirstVisibleIndex);
            this.mFirstVisibleIndex++;
        }
        resetVisbileIndexIfEmpty();
    }

    private void resetVisbileIndexIfEmpty() {
        if (this.mLastVisibleIndex >= this.mFirstVisibleIndex) {
            return;
        }
        resetVisibleIndex();
    }
}
