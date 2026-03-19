package android.support.v17.leanback.widget;

import android.support.v4.util.CircularIntArray;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import java.util.Arrays;

abstract class Grid {
    protected int mNumRows;
    protected Provider mProvider;
    protected boolean mReversedFlow;
    protected int mSpacing;
    protected CircularIntArray[] mTmpItemPositionsInRows;
    Object[] mTmpItem = new Object[1];
    protected int mFirstVisibleIndex = -1;
    protected int mLastVisibleIndex = -1;
    protected int mStartIndex = -1;

    public interface Provider {
        void addItem(Object obj, int i, int i2, int i3, int i4);

        int createItem(int i, boolean z, Object[] objArr, boolean z2);

        int getCount();

        int getEdge(int i);

        int getMinIndex();

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

    public final void setSpacing(int spacing) {
        this.mSpacing = spacing;
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
        if (this.mLastVisibleIndex >= index) {
            this.mLastVisibleIndex = index - 1;
        }
        resetVisibleIndexIfEmpty();
        if (getFirstVisibleIndex() < 0) {
            setStart(index);
        }
    }

    public final int getRowIndex(int index) {
        Location location = getLocation(index);
        if (location == null) {
            return -1;
        }
        return location.row;
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
        if (this.mReversedFlow) {
            if (findRowMin(true, null) > this.mSpacing + toLimit) {
                return false;
            }
        } else if (findRowMax(false, null) < toLimit - this.mSpacing) {
            return false;
        }
        return true;
    }

    protected final boolean checkPrependOverLimit(int toLimit) {
        if (this.mLastVisibleIndex < 0) {
            return false;
        }
        if (this.mReversedFlow) {
            if (findRowMax(false, null) < toLimit - this.mSpacing) {
                return false;
            }
        } else if (findRowMin(true, null) > this.mSpacing + toLimit) {
            return false;
        }
        return true;
    }

    public final CircularIntArray[] getItemPositionsInRows() {
        return getItemPositionsInRows(getFirstVisibleIndex(), getLastVisibleIndex());
    }

    public final boolean prependOneColumnVisibleItems() {
        return prependVisibleItems(this.mReversedFlow ? Integer.MIN_VALUE : Preference.DEFAULT_ORDER, true);
    }

    public final void prependVisibleItems(int toLimit) {
        prependVisibleItems(toLimit, false);
    }

    public boolean appendOneColumnVisibleItems() {
        return appendVisibleItems(this.mReversedFlow ? Preference.DEFAULT_ORDER : Integer.MIN_VALUE, true);
    }

    public final void appendVisibleItems(int toLimit) {
        appendVisibleItems(toLimit, false);
    }

    public void removeInvisibleItemsAtEnd(int aboveIndex, int toLimit) {
        while (this.mLastVisibleIndex >= this.mFirstVisibleIndex && this.mLastVisibleIndex > aboveIndex) {
            boolean z = false;
            if (this.mReversedFlow ? this.mProvider.getEdge(this.mLastVisibleIndex) <= toLimit : this.mProvider.getEdge(this.mLastVisibleIndex) >= toLimit) {
                z = true;
            }
            boolean offEnd = z;
            if (!offEnd) {
                break;
            }
            this.mProvider.removeItem(this.mLastVisibleIndex);
            this.mLastVisibleIndex--;
        }
        resetVisibleIndexIfEmpty();
    }

    public void removeInvisibleItemsAtFront(int belowIndex, int toLimit) {
        while (this.mLastVisibleIndex >= this.mFirstVisibleIndex && this.mFirstVisibleIndex < belowIndex) {
            int size = this.mProvider.getSize(this.mFirstVisibleIndex);
            boolean z = false;
            if (this.mReversedFlow ? this.mProvider.getEdge(this.mFirstVisibleIndex) - size >= toLimit : this.mProvider.getEdge(this.mFirstVisibleIndex) + size <= toLimit) {
                z = true;
            }
            boolean offFront = z;
            if (!offFront) {
                break;
            }
            this.mProvider.removeItem(this.mFirstVisibleIndex);
            this.mFirstVisibleIndex++;
        }
        resetVisibleIndexIfEmpty();
    }

    private void resetVisibleIndexIfEmpty() {
        if (this.mLastVisibleIndex < this.mFirstVisibleIndex) {
            resetVisibleIndex();
        }
    }

    public void fillDisappearingItems(int[] positions, int positionsLength, SparseIntArray positionToRow) {
        int resultSearchLast;
        int resultSearchFirst;
        int edge;
        int edge2;
        int edge3;
        int i;
        int lastPos = getLastVisibleIndex();
        if (lastPos < 0) {
            resultSearchLast = 0;
        } else {
            resultSearchLast = Arrays.binarySearch(positions, 0, positionsLength, lastPos);
        }
        if (resultSearchLast < 0) {
            int firstDisappearingIndex = (-resultSearchLast) - 1;
            if (this.mReversedFlow) {
                edge3 = (this.mProvider.getEdge(lastPos) - this.mProvider.getSize(lastPos)) - this.mSpacing;
            } else {
                edge3 = this.mProvider.getEdge(lastPos) + this.mProvider.getSize(lastPos) + this.mSpacing;
            }
            int edge4 = edge3;
            for (int edge5 = firstDisappearingIndex; edge5 < positionsLength; edge5++) {
                int disappearingIndex = positions[edge5];
                int disappearingRow = positionToRow.get(disappearingIndex);
                if (disappearingRow < 0) {
                    disappearingRow = 0;
                }
                int size = this.mProvider.createItem(disappearingIndex, true, this.mTmpItem, true);
                this.mProvider.addItem(this.mTmpItem[0], disappearingIndex, size, disappearingRow, edge4);
                if (this.mReversedFlow) {
                    i = (edge4 - size) - this.mSpacing;
                } else {
                    i = edge4 + size + this.mSpacing;
                }
                edge4 = i;
            }
        }
        int firstPos = getFirstVisibleIndex();
        if (firstPos < 0) {
            resultSearchFirst = 0;
        } else {
            resultSearchFirst = Arrays.binarySearch(positions, 0, positionsLength, firstPos);
        }
        if (resultSearchFirst < 0) {
            int firstDisappearingIndex2 = (-resultSearchFirst) - 2;
            if (this.mReversedFlow) {
                edge = this.mProvider.getEdge(firstPos);
            } else {
                edge = this.mProvider.getEdge(firstPos);
            }
            int edge6 = edge;
            for (int edge7 = firstDisappearingIndex2; edge7 >= 0; edge7--) {
                int disappearingIndex2 = positions[edge7];
                int disappearingRow2 = positionToRow.get(disappearingIndex2);
                if (disappearingRow2 < 0) {
                    disappearingRow2 = 0;
                }
                int disappearingRow3 = disappearingRow2;
                int size2 = this.mProvider.createItem(disappearingIndex2, false, this.mTmpItem, true);
                if (this.mReversedFlow) {
                    edge2 = this.mSpacing + edge6 + size2;
                } else {
                    int edge8 = this.mSpacing;
                    edge2 = (edge6 - edge8) - size2;
                }
                edge6 = edge2;
                this.mProvider.addItem(this.mTmpItem[0], disappearingIndex2, size2, disappearingRow3, edge6);
            }
        }
    }

    public void collectAdjacentPrefetchPositions(int fromLimit, int da, RecyclerView.LayoutManager.LayoutPrefetchRegistry layoutPrefetchRegistry) {
    }
}
