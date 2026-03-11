package android.support.v17.leanback.widget;

import android.support.v17.leanback.widget.StaggeredGrid;

final class StaggeredGridDefault extends StaggeredGrid {
    StaggeredGridDefault() {
    }

    int getRowMax(int rowIndex) {
        StaggeredGrid.Location loc;
        if (this.mFirstVisibleIndex < 0) {
            return Integer.MIN_VALUE;
        }
        if (this.mReversedFlow) {
            int edge = this.mProvider.getEdge(this.mFirstVisibleIndex);
            if (getLocation(this.mFirstVisibleIndex).row == rowIndex) {
                return edge;
            }
            int i = this.mFirstVisibleIndex;
            do {
                i++;
                if (i <= getLastIndex()) {
                    loc = getLocation(i);
                    edge += loc.offset;
                }
            } while (loc.row != rowIndex);
            return edge;
        }
        int edge2 = this.mProvider.getEdge(this.mLastVisibleIndex);
        StaggeredGrid.Location loc2 = getLocation(this.mLastVisibleIndex);
        if (loc2.row == rowIndex) {
            return loc2.size + edge2;
        }
        int i2 = this.mLastVisibleIndex;
        do {
            i2--;
            if (i2 >= getFirstIndex()) {
                edge2 -= loc2.offset;
                loc2 = getLocation(i2);
            }
        } while (loc2.row != rowIndex);
        return loc2.size + edge2;
        return Integer.MIN_VALUE;
    }

    int getRowMin(int rowIndex) {
        StaggeredGrid.Location loc;
        if (this.mFirstVisibleIndex < 0) {
            return Integer.MAX_VALUE;
        }
        if (this.mReversedFlow) {
            int edge = this.mProvider.getEdge(this.mLastVisibleIndex);
            StaggeredGrid.Location loc2 = getLocation(this.mLastVisibleIndex);
            if (loc2.row == rowIndex) {
                return edge - loc2.size;
            }
            int i = this.mLastVisibleIndex;
            do {
                i--;
                if (i >= getFirstIndex()) {
                    edge -= loc2.offset;
                    loc2 = getLocation(i);
                }
            } while (loc2.row != rowIndex);
            return edge - loc2.size;
        }
        int edge2 = this.mProvider.getEdge(this.mFirstVisibleIndex);
        if (getLocation(this.mFirstVisibleIndex).row == rowIndex) {
            return edge2;
        }
        int i2 = this.mFirstVisibleIndex;
        do {
            i2++;
            if (i2 <= getLastIndex()) {
                loc = getLocation(i2);
                edge2 += loc.offset;
            }
        } while (loc.row != rowIndex);
        return edge2;
        return Integer.MAX_VALUE;
    }

    @Override
    public int findRowMax(boolean findLarge, int indexLimit, int[] indices) {
        int value;
        int edge = this.mProvider.getEdge(indexLimit);
        StaggeredGrid.Location loc = getLocation(indexLimit);
        int row = loc.row;
        int index = indexLimit;
        int visitedRows = 1;
        int visitRow = row;
        if (this.mReversedFlow) {
            value = edge;
            for (int i = indexLimit + 1; visitedRows < this.mNumRows && i <= this.mLastVisibleIndex; i++) {
                StaggeredGrid.Location loc2 = getLocation(i);
                edge += loc2.offset;
                if (loc2.row != visitRow) {
                    visitRow = loc2.row;
                    visitedRows++;
                    if (findLarge) {
                        if (edge > value) {
                            row = visitRow;
                            value = edge;
                            index = i;
                        }
                    } else if (edge < value) {
                    }
                }
            }
        } else {
            value = this.mProvider.getSize(indexLimit) + edge;
            for (int i2 = indexLimit - 1; visitedRows < this.mNumRows && i2 >= this.mFirstVisibleIndex; i2--) {
                edge -= loc.offset;
                loc = getLocation(i2);
                if (loc.row != visitRow) {
                    visitRow = loc.row;
                    visitedRows++;
                    int newValue = this.mProvider.getSize(i2) + edge;
                    if (findLarge) {
                        if (newValue > value) {
                            row = visitRow;
                            value = newValue;
                            index = i2;
                        }
                    } else if (newValue < value) {
                    }
                }
            }
        }
        if (indices != null) {
            indices[0] = row;
            indices[1] = index;
        }
        return value;
    }

    @Override
    public int findRowMin(boolean findLarge, int indexLimit, int[] indices) {
        int value;
        int edge = this.mProvider.getEdge(indexLimit);
        StaggeredGrid.Location loc = getLocation(indexLimit);
        int row = loc.row;
        int index = indexLimit;
        int visitedRows = 1;
        int visitRow = row;
        if (this.mReversedFlow) {
            value = edge - this.mProvider.getSize(indexLimit);
            for (int i = indexLimit - 1; visitedRows < this.mNumRows && i >= this.mFirstVisibleIndex; i--) {
                edge -= loc.offset;
                loc = getLocation(i);
                if (loc.row != visitRow) {
                    visitRow = loc.row;
                    visitedRows++;
                    int newValue = edge - this.mProvider.getSize(i);
                    if (findLarge) {
                        if (newValue > value) {
                            value = newValue;
                            row = visitRow;
                            index = i;
                        }
                    } else if (newValue < value) {
                    }
                }
            }
        } else {
            value = edge;
            for (int i2 = indexLimit + 1; visitedRows < this.mNumRows && i2 <= this.mLastVisibleIndex; i2++) {
                StaggeredGrid.Location loc2 = getLocation(i2);
                edge += loc2.offset;
                if (loc2.row != visitRow) {
                    visitRow = loc2.row;
                    visitedRows++;
                    if (findLarge) {
                        if (edge > value) {
                            value = edge;
                            row = visitRow;
                            index = i2;
                        }
                    } else if (edge < value) {
                    }
                }
            }
        }
        if (indices != null) {
            indices[0] = row;
            indices[1] = index;
        }
        return value;
    }

    private int findRowEdgeLimitSearchIndex(boolean append) {
        boolean wrapped = false;
        if (append) {
            for (int index = this.mLastVisibleIndex; index >= this.mFirstVisibleIndex; index--) {
                int row = getLocation(index).row;
                if (row == 0) {
                    wrapped = true;
                } else if (wrapped && row == this.mNumRows - 1) {
                    return index;
                }
            }
            return -1;
        }
        for (int index2 = this.mFirstVisibleIndex; index2 <= this.mLastVisibleIndex; index2++) {
            int row2 = getLocation(index2).row;
            if (row2 == this.mNumRows - 1) {
                wrapped = true;
            } else if (wrapped && row2 == 0) {
                return index2;
            }
        }
        return -1;
    }

    @Override
    protected boolean appendVisibleItemsWithoutCache(int toLimit, boolean oneColumnMode) {
        int itemIndex;
        int rowIndex;
        int edgeLimit;
        boolean edgeLimitIsValid;
        int location;
        int edgeLimit2;
        int count = this.mProvider.getCount();
        if (this.mLastVisibleIndex >= 0) {
            if (this.mLastVisibleIndex < getLastIndex()) {
                return false;
            }
            itemIndex = this.mLastVisibleIndex + 1;
            rowIndex = getLocation(this.mLastVisibleIndex).row;
            int edgeLimitSearchIndex = findRowEdgeLimitSearchIndex(true);
            if (edgeLimitSearchIndex < 0) {
                edgeLimit2 = Integer.MIN_VALUE;
                for (int edgeLimit3 = 0; edgeLimit3 < this.mNumRows; edgeLimit3++) {
                    edgeLimit2 = this.mReversedFlow ? getRowMin(edgeLimit3) : getRowMax(edgeLimit3);
                    if (edgeLimit2 != Integer.MIN_VALUE) {
                        break;
                    }
                }
            } else {
                edgeLimit2 = this.mReversedFlow ? findRowMin(false, edgeLimitSearchIndex, null) : findRowMax(true, edgeLimitSearchIndex, null);
            }
            edgeLimit = edgeLimit2;
            if (!this.mReversedFlow ? getRowMax(rowIndex) >= edgeLimit : getRowMin(rowIndex) <= edgeLimit) {
                rowIndex++;
                if (rowIndex == this.mNumRows) {
                    rowIndex = 0;
                    edgeLimit = this.mReversedFlow ? findRowMin(false, null) : findRowMax(true, null);
                }
            }
            edgeLimitIsValid = true;
        } else {
            itemIndex = this.mStartIndex != -1 ? this.mStartIndex : 0;
            rowIndex = (this.mLocations.size() > 0 ? getLocation(getLastIndex()).row + 1 : itemIndex) % this.mNumRows;
            edgeLimit = 0;
            edgeLimitIsValid = false;
        }
        boolean edgeLimitIsValid2 = edgeLimitIsValid;
        int itemIndex2 = itemIndex;
        boolean filledOne = false;
        loop1: while (true) {
            if (rowIndex < this.mNumRows) {
                if (itemIndex2 == count || (!oneColumnMode && checkAppendOverLimit(toLimit))) {
                    break;
                }
                int location2 = this.mReversedFlow ? getRowMin(rowIndex) : getRowMax(rowIndex);
                if (location2 == Integer.MAX_VALUE || location2 == Integer.MIN_VALUE) {
                    if (rowIndex == 0) {
                        location = this.mReversedFlow ? getRowMin(this.mNumRows - 1) : getRowMax(this.mNumRows - 1);
                        if (location != Integer.MAX_VALUE && location != Integer.MIN_VALUE) {
                            location += this.mReversedFlow ? -this.mSpacing : this.mSpacing;
                        }
                    } else {
                        location = this.mReversedFlow ? getRowMax(rowIndex - 1) : getRowMin(rowIndex - 1);
                    }
                } else {
                    location = location2 + (this.mReversedFlow ? -this.mSpacing : this.mSpacing);
                }
                int itemIndex3 = itemIndex2 + 1;
                int size = appendVisibleItemToRow(itemIndex2, rowIndex, location);
                filledOne = true;
                if (edgeLimitIsValid2) {
                    while (true) {
                        if (this.mReversedFlow) {
                            if (location - size <= edgeLimit) {
                                break;
                            }
                            if (itemIndex3 == count || (!oneColumnMode && checkAppendOverLimit(toLimit))) {
                                break loop1;
                            }
                            location += this.mReversedFlow ? (-size) - this.mSpacing : this.mSpacing + size;
                            size = appendVisibleItemToRow(itemIndex3, rowIndex, location);
                            itemIndex3++;
                        } else {
                            if (location + size >= edgeLimit) {
                                break;
                            }
                            if (itemIndex3 == count) {
                                break loop1;
                            }
                            break loop1;
                            break loop1;
                        }
                    }
                } else {
                    edgeLimitIsValid2 = true;
                    int size2 = this.mReversedFlow ? getRowMin(rowIndex) : getRowMax(rowIndex);
                    edgeLimit = size2;
                }
                itemIndex2 = itemIndex3;
                rowIndex++;
            } else {
                if (oneColumnMode) {
                    return filledOne;
                }
                edgeLimit = this.mReversedFlow ? findRowMin(false, null) : findRowMax(true, null);
                rowIndex = 0;
            }
        }
    }

    @Override
    protected boolean prependVisibleItemsWithoutCache(int toLimit, boolean oneColumnMode) {
        int itemIndex;
        int rowIndex;
        int edgeLimit;
        boolean edgeLimitIsValid;
        int location;
        if (this.mFirstVisibleIndex >= 0) {
            if (this.mFirstVisibleIndex > getFirstIndex()) {
                return false;
            }
            itemIndex = this.mFirstVisibleIndex - 1;
            rowIndex = getLocation(this.mFirstVisibleIndex).row;
            int edgeLimitSearchIndex = findRowEdgeLimitSearchIndex(false);
            if (edgeLimitSearchIndex < 0) {
                rowIndex--;
                edgeLimit = Integer.MAX_VALUE;
                for (int i = this.mNumRows - 1; i >= 0; i--) {
                    edgeLimit = this.mReversedFlow ? getRowMax(i) : getRowMin(i);
                    if (edgeLimit != Integer.MAX_VALUE) {
                        break;
                    }
                }
            } else {
                edgeLimit = this.mReversedFlow ? findRowMax(true, edgeLimitSearchIndex, null) : findRowMin(false, edgeLimitSearchIndex, null);
            }
            if (!this.mReversedFlow ? getRowMin(rowIndex) <= edgeLimit : getRowMax(rowIndex) >= edgeLimit) {
                rowIndex--;
                if (rowIndex < 0) {
                    rowIndex = this.mNumRows - 1;
                    edgeLimit = this.mReversedFlow ? findRowMax(true, null) : findRowMin(false, null);
                }
            }
            edgeLimitIsValid = true;
        } else {
            itemIndex = this.mStartIndex != -1 ? this.mStartIndex : 0;
            rowIndex = (this.mLocations.size() > 0 ? (getLocation(getFirstIndex()).row + this.mNumRows) - 1 : itemIndex) % this.mNumRows;
            edgeLimit = 0;
            edgeLimitIsValid = false;
        }
        boolean edgeLimitIsValid2 = edgeLimitIsValid;
        int itemIndex2 = itemIndex;
        boolean filledOne = false;
        loop1: while (true) {
            if (rowIndex >= 0) {
                if (itemIndex2 < 0 || (!oneColumnMode && checkPrependOverLimit(toLimit))) {
                    break;
                }
                int location2 = this.mReversedFlow ? getRowMax(rowIndex) : getRowMin(rowIndex);
                if (location2 == Integer.MAX_VALUE || location2 == Integer.MIN_VALUE) {
                    if (rowIndex == this.mNumRows - 1) {
                        location = this.mReversedFlow ? getRowMax(0) : getRowMin(0);
                        if (location != Integer.MAX_VALUE && location != Integer.MIN_VALUE) {
                            location += this.mReversedFlow ? this.mSpacing : -this.mSpacing;
                        }
                    } else {
                        location = this.mReversedFlow ? getRowMin(rowIndex + 1) : getRowMax(rowIndex + 1);
                    }
                } else {
                    location = location2 + (this.mReversedFlow ? this.mSpacing : -this.mSpacing);
                }
                int itemIndex3 = itemIndex2 - 1;
                int size = prependVisibleItemToRow(itemIndex2, rowIndex, location);
                filledOne = true;
                if (edgeLimitIsValid2) {
                    while (true) {
                        if (this.mReversedFlow) {
                            if (location + size >= edgeLimit) {
                                break;
                            }
                            if (itemIndex3 < 0 || (!oneColumnMode && checkPrependOverLimit(toLimit))) {
                                break loop1;
                            }
                            location += this.mReversedFlow ? this.mSpacing + size : (-size) - this.mSpacing;
                            size = prependVisibleItemToRow(itemIndex3, rowIndex, location);
                            itemIndex3--;
                        } else {
                            if (location - size <= edgeLimit) {
                                break;
                            }
                            if (itemIndex3 < 0) {
                                break loop1;
                            }
                            break loop1;
                            break loop1;
                        }
                    }
                } else {
                    edgeLimitIsValid2 = true;
                    int size2 = this.mReversedFlow ? getRowMax(rowIndex) : getRowMin(rowIndex);
                    edgeLimit = size2;
                }
                itemIndex2 = itemIndex3;
                rowIndex--;
            } else {
                if (oneColumnMode) {
                    return filledOne;
                }
                edgeLimit = this.mReversedFlow ? findRowMax(true, null) : findRowMin(false, null);
                rowIndex = this.mNumRows - 1;
            }
        }
    }
}
