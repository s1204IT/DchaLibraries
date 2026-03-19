package android.support.v17.leanback.widget;

import android.support.v17.leanback.widget.Grid;
import android.support.v4.util.CircularArray;
import android.support.v4.util.CircularIntArray;

abstract class StaggeredGrid extends Grid {
    protected Object mPendingItem;
    protected int mPendingItemSize;
    protected CircularArray<Location> mLocations = new CircularArray<>(64);
    protected int mFirstIndex = -1;

    protected abstract boolean appendVisibleItemsWithoutCache(int i, boolean z);

    protected abstract boolean prependVisibleItemsWithoutCache(int i, boolean z);

    StaggeredGrid() {
    }

    public static class Location extends Grid.Location {
        public int offset;
        public int size;

        public Location(int row, int offset, int size) {
            super(row);
            this.offset = offset;
            this.size = size;
        }
    }

    public final int getFirstIndex() {
        return this.mFirstIndex;
    }

    public final int getLastIndex() {
        return (this.mFirstIndex + this.mLocations.size()) - 1;
    }

    @Override
    public final Location getLocation(int index) {
        int indexInArray = index - this.mFirstIndex;
        if (indexInArray < 0 || indexInArray >= this.mLocations.size()) {
            return null;
        }
        return this.mLocations.get(indexInArray);
    }

    @Override
    protected final boolean prependVisibleItems(int toLimit, boolean oneColumnMode) {
        boolean zPrependVisibleItemsWithoutCache;
        if (this.mProvider.getCount() == 0) {
            return false;
        }
        if (!oneColumnMode && checkPrependOverLimit(toLimit)) {
            return false;
        }
        try {
            if (prependVisbleItemsWithCache(toLimit, oneColumnMode)) {
                zPrependVisibleItemsWithoutCache = true;
                this.mTmpItem[0] = null;
            } else {
                zPrependVisibleItemsWithoutCache = prependVisibleItemsWithoutCache(toLimit, oneColumnMode);
                this.mTmpItem[0] = null;
            }
            this.mPendingItem = null;
            return zPrependVisibleItemsWithoutCache;
        } catch (Throwable th) {
            this.mTmpItem[0] = null;
            this.mPendingItem = null;
            throw th;
        }
    }

    protected final boolean prependVisbleItemsWithCache(int toLimit, boolean oneColumnMode) {
        int edge;
        int offset;
        int itemIndex;
        if (this.mLocations.size() == 0) {
            return false;
        }
        if (this.mFirstVisibleIndex >= 0) {
            edge = this.mProvider.getEdge(this.mFirstVisibleIndex);
            offset = getLocation(this.mFirstVisibleIndex).offset;
            itemIndex = this.mFirstVisibleIndex - 1;
        } else {
            edge = Integer.MAX_VALUE;
            offset = 0;
            itemIndex = this.mStartIndex != -1 ? this.mStartIndex : 0;
            if (itemIndex > getLastIndex() || itemIndex < getFirstIndex() - 1) {
                this.mLocations.clear();
                return false;
            }
            if (itemIndex < getFirstIndex()) {
                return false;
            }
        }
        int firstIndex = Math.max(this.mProvider.getMinIndex(), this.mFirstIndex);
        while (itemIndex >= firstIndex) {
            Location loc = getLocation(itemIndex);
            int rowIndex = loc.row;
            int size = this.mProvider.createItem(itemIndex, false, this.mTmpItem, false);
            if (size != loc.size) {
                this.mLocations.removeFromStart((itemIndex + 1) - this.mFirstIndex);
                this.mFirstIndex = this.mFirstVisibleIndex;
                this.mPendingItem = this.mTmpItem[0];
                this.mPendingItemSize = size;
                return false;
            }
            this.mFirstVisibleIndex = itemIndex;
            if (this.mLastVisibleIndex < 0) {
                this.mLastVisibleIndex = itemIndex;
            }
            this.mProvider.addItem(this.mTmpItem[0], itemIndex, size, rowIndex, edge - offset);
            if (!oneColumnMode && checkPrependOverLimit(toLimit)) {
                return true;
            }
            edge = this.mProvider.getEdge(itemIndex);
            offset = loc.offset;
            if (rowIndex == 0 && oneColumnMode) {
                return true;
            }
            itemIndex--;
        }
        return false;
    }

    private int calculateOffsetAfterLastItem(int row) {
        int cachedIndex = getLastIndex();
        boolean foundCachedItemInSameRow = false;
        while (true) {
            if (cachedIndex < this.mFirstIndex) {
                break;
            }
            Location loc = getLocation(cachedIndex);
            if (loc.row == row) {
                foundCachedItemInSameRow = true;
                break;
            }
            cachedIndex--;
        }
        if (!foundCachedItemInSameRow) {
            cachedIndex = getLastIndex();
        }
        int offset = isReversedFlow() ? (-getLocation(cachedIndex).size) - this.mSpacing : getLocation(cachedIndex).size + this.mSpacing;
        for (int i = cachedIndex + 1; i <= getLastIndex(); i++) {
            offset -= getLocation(i).offset;
        }
        return offset;
    }

    protected final int prependVisibleItemToRow(int itemIndex, int rowIndex, int edge) {
        Object item;
        if (this.mFirstVisibleIndex >= 0 && (this.mFirstVisibleIndex != getFirstIndex() || this.mFirstVisibleIndex != itemIndex + 1)) {
            throw new IllegalStateException();
        }
        Location oldFirstLoc = this.mFirstIndex >= 0 ? getLocation(this.mFirstIndex) : null;
        int oldFirstEdge = this.mProvider.getEdge(this.mFirstIndex);
        Location loc = new Location(rowIndex, 0, 0);
        this.mLocations.addFirst(loc);
        if (this.mPendingItem != null) {
            loc.size = this.mPendingItemSize;
            item = this.mPendingItem;
            this.mPendingItem = null;
        } else {
            loc.size = this.mProvider.createItem(itemIndex, false, this.mTmpItem, false);
            item = this.mTmpItem[0];
        }
        Object item2 = item;
        this.mFirstVisibleIndex = itemIndex;
        this.mFirstIndex = itemIndex;
        if (this.mLastVisibleIndex < 0) {
            this.mLastVisibleIndex = itemIndex;
        }
        int thisEdge = !this.mReversedFlow ? edge - loc.size : loc.size + edge;
        if (oldFirstLoc != null) {
            oldFirstLoc.offset = oldFirstEdge - thisEdge;
        }
        this.mProvider.addItem(item2, itemIndex, loc.size, rowIndex, thisEdge);
        return loc.size;
    }

    @Override
    protected final boolean appendVisibleItems(int toLimit, boolean oneColumnMode) {
        boolean zAppendVisibleItemsWithoutCache;
        if (this.mProvider.getCount() == 0) {
            return false;
        }
        if (!oneColumnMode && checkAppendOverLimit(toLimit)) {
            return false;
        }
        try {
            if (appendVisbleItemsWithCache(toLimit, oneColumnMode)) {
                zAppendVisibleItemsWithoutCache = true;
                this.mTmpItem[0] = null;
            } else {
                zAppendVisibleItemsWithoutCache = appendVisibleItemsWithoutCache(toLimit, oneColumnMode);
                this.mTmpItem[0] = null;
            }
            this.mPendingItem = null;
            return zAppendVisibleItemsWithoutCache;
        } catch (Throwable th) {
            this.mTmpItem[0] = null;
            this.mPendingItem = null;
            throw th;
        }
    }

    protected final boolean appendVisbleItemsWithCache(int toLimit, boolean oneColumnMode) {
        int edge;
        int itemIndex;
        ?? r2 = 0;
        if (this.mLocations.size() == 0) {
            return false;
        }
        int count = this.mProvider.getCount();
        if (this.mLastVisibleIndex >= 0) {
            itemIndex = this.mLastVisibleIndex + 1;
            edge = this.mProvider.getEdge(this.mLastVisibleIndex);
        } else {
            edge = Integer.MAX_VALUE;
            itemIndex = this.mStartIndex != -1 ? this.mStartIndex : 0;
            if (itemIndex > getLastIndex() + 1 || itemIndex < getFirstIndex()) {
                this.mLocations.clear();
                return false;
            }
            if (itemIndex > getLastIndex()) {
                return false;
            }
        }
        int lastIndex = getLastIndex();
        while (itemIndex < count && itemIndex <= lastIndex) {
            Location loc = getLocation(itemIndex);
            if (edge != Integer.MAX_VALUE) {
                edge += loc.offset;
            }
            int rowIndex = loc.row;
            int size = this.mProvider.createItem(itemIndex, true, this.mTmpItem, r2);
            if (size != loc.size) {
                loc.size = size;
                this.mLocations.removeFromEnd(lastIndex - itemIndex);
                lastIndex = itemIndex;
            }
            this.mLastVisibleIndex = itemIndex;
            if (this.mFirstVisibleIndex < 0) {
                this.mFirstVisibleIndex = itemIndex;
            }
            this.mProvider.addItem(this.mTmpItem[r2], itemIndex, size, rowIndex, edge);
            if (!oneColumnMode && checkAppendOverLimit(toLimit)) {
                return true;
            }
            if (edge == Integer.MAX_VALUE) {
                edge = this.mProvider.getEdge(itemIndex);
            }
            if (rowIndex == this.mNumRows - 1 && oneColumnMode) {
                return true;
            }
            itemIndex++;
            r2 = 0;
        }
        return false;
    }

    protected final int appendVisibleItemToRow(int itemIndex, int rowIndex, int location) {
        int offset;
        Object item;
        if (this.mLastVisibleIndex >= 0 && (this.mLastVisibleIndex != getLastIndex() || this.mLastVisibleIndex != itemIndex - 1)) {
            throw new IllegalStateException();
        }
        if (this.mLastVisibleIndex < 0) {
            if (this.mLocations.size() > 0 && itemIndex == getLastIndex() + 1) {
                offset = calculateOffsetAfterLastItem(rowIndex);
            } else {
                offset = 0;
            }
        } else {
            offset = location - this.mProvider.getEdge(this.mLastVisibleIndex);
        }
        Location loc = new Location(rowIndex, offset, 0);
        this.mLocations.addLast(loc);
        if (this.mPendingItem != null) {
            loc.size = this.mPendingItemSize;
            item = this.mPendingItem;
            this.mPendingItem = null;
        } else {
            loc.size = this.mProvider.createItem(itemIndex, true, this.mTmpItem, false);
            item = this.mTmpItem[0];
        }
        Object item2 = item;
        if (this.mLocations.size() == 1) {
            this.mLastVisibleIndex = itemIndex;
            this.mFirstVisibleIndex = itemIndex;
            this.mFirstIndex = itemIndex;
        } else if (this.mLastVisibleIndex < 0) {
            this.mLastVisibleIndex = itemIndex;
            this.mFirstVisibleIndex = itemIndex;
        } else {
            this.mLastVisibleIndex++;
        }
        this.mProvider.addItem(item2, itemIndex, loc.size, rowIndex, location);
        return loc.size;
    }

    @Override
    public final CircularIntArray[] getItemPositionsInRows(int startPos, int endPos) {
        for (int i = 0; i < this.mNumRows; i++) {
            this.mTmpItemPositionsInRows[i].clear();
        }
        if (startPos >= 0) {
            for (int i2 = startPos; i2 <= endPos; i2++) {
                CircularIntArray row = this.mTmpItemPositionsInRows[getLocation(i2).row];
                if (row.size() > 0 && row.getLast() == i2 - 1) {
                    row.popLast();
                    row.addLast(i2);
                } else {
                    row.addLast(i2);
                    row.addLast(i2);
                }
            }
        }
        return this.mTmpItemPositionsInRows;
    }

    @Override
    public void invalidateItemsAfter(int index) {
        super.invalidateItemsAfter(index);
        this.mLocations.removeFromEnd((getLastIndex() - index) + 1);
        if (this.mLocations.size() == 0) {
            this.mFirstIndex = -1;
        }
    }
}
