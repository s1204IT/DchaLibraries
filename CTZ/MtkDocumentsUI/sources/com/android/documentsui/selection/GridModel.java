package com.android.documentsui.selection;

import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.util.Preconditions;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.documentsui.selection.BandSelectionHelper;
import com.android.documentsui.selection.SelectionHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class GridModel {
    private final BandSelectionHelper.BandHost mHost;
    private boolean mIsActive;
    private RelativePoint mRelativeOrigin;
    private RelativePoint mRelativePointer;
    private final SelectionHelper.SelectionPredicate mSelectionPredicate;
    private final SelectionHelper.StableIdProvider mStableIds;
    private final List<SelectionObserver> mOnSelectionChangedListeners = new ArrayList();
    private final SparseArray<SparseIntArray> mColumns = new SparseArray<>();
    private final List<Limits> mColumnBounds = new ArrayList();
    private final List<Limits> mRowBounds = new ArrayList();
    private final SparseBooleanArray mKnownPositions = new SparseBooleanArray();
    private final Set<String> mSelection = new HashSet();
    private Point mPointer = null;
    private int mPositionNearestOrigin = -1;
    private final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
            GridModel.this.onScrolled(recyclerView, i, i2);
        }
    };

    public static abstract class SelectionObserver {
        abstract void onSelectionChanged(Set<String> set);
    }

    GridModel(BandSelectionHelper.BandHost bandHost, SelectionHelper.StableIdProvider stableIdProvider, SelectionHelper.SelectionPredicate selectionPredicate) {
        this.mHost = bandHost;
        this.mStableIds = stableIdProvider;
        this.mSelectionPredicate = selectionPredicate;
        this.mHost.addOnScrollListener(this.mScrollListener);
    }

    void stopListening() {
        this.mHost.removeOnScrollListener(this.mScrollListener);
    }

    void startCapturing(Point point) {
        recordVisibleChildren();
        if (isEmpty()) {
            return;
        }
        this.mIsActive = true;
        this.mPointer = this.mHost.createAbsolutePoint(point);
        this.mRelativeOrigin = new RelativePoint(this.mPointer);
        this.mRelativePointer = new RelativePoint(this.mPointer);
        computeCurrentSelection();
        notifySelectionChanged();
    }

    void stopCapturing() {
        this.mIsActive = false;
    }

    void resizeSelection(Point point) {
        this.mPointer = this.mHost.createAbsolutePoint(point);
        updateModel();
    }

    int getPositionNearestOrigin() {
        return this.mPositionNearestOrigin;
    }

    private void onScrolled(RecyclerView recyclerView, int i, int i2) {
        if (!this.mIsActive) {
            return;
        }
        this.mPointer.x += i;
        this.mPointer.y += i2;
        recordVisibleChildren();
        updateModel();
    }

    private void recordVisibleChildren() {
        for (int i = 0; i < this.mHost.getVisibleChildCount(); i++) {
            int adapterPositionAt = this.mHost.getAdapterPositionAt(i);
            if (this.mHost.hasView(adapterPositionAt) && this.mSelectionPredicate.canSetStateAtPosition(adapterPositionAt, true) && !this.mKnownPositions.get(adapterPositionAt)) {
                this.mKnownPositions.put(adapterPositionAt, true);
                recordItemData(this.mHost.getAbsoluteRectForChildViewAt(i), adapterPositionAt);
            }
        }
    }

    private boolean isEmpty() {
        return this.mColumnBounds.size() == 0 || this.mRowBounds.size() == 0;
    }

    private void recordItemData(Rect rect, int i) {
        if (this.mColumnBounds.size() != this.mHost.getColumnCount()) {
            recordLimits(this.mColumnBounds, new Limits(rect.left, rect.right));
        }
        recordLimits(this.mRowBounds, new Limits(rect.top, rect.bottom));
        SparseIntArray sparseIntArray = this.mColumns.get(rect.left);
        if (sparseIntArray == null) {
            sparseIntArray = new SparseIntArray();
            this.mColumns.put(rect.left, sparseIntArray);
        }
        sparseIntArray.put(rect.top, i);
    }

    private void recordLimits(List<Limits> list, Limits limits) {
        int iBinarySearch = Collections.binarySearch(list, limits);
        if (iBinarySearch < 0) {
            list.add(~iBinarySearch, limits);
        }
    }

    private void updateModel() {
        RelativePoint relativePoint = this.mRelativePointer;
        this.mRelativePointer = new RelativePoint(this.mPointer);
        if (relativePoint != null && this.mRelativePointer.equals(relativePoint)) {
            return;
        }
        computeCurrentSelection();
        notifySelectionChanged();
    }

    private void computeCurrentSelection() {
        if (areItemsCoveredByBand(this.mRelativePointer, this.mRelativeOrigin)) {
            updateSelection(computeBounds());
        } else {
            this.mSelection.clear();
            this.mPositionNearestOrigin = -1;
        }
    }

    private void notifySelectionChanged() {
        Iterator<SelectionObserver> it = this.mOnSelectionChangedListeners.iterator();
        while (it.hasNext()) {
            it.next().onSelectionChanged(this.mSelection);
        }
    }

    private void updateSelection(Rect rect) {
        int iBinarySearch = Collections.binarySearch(this.mColumnBounds, new Limits(rect.left, rect.left));
        Preconditions.checkArgument(iBinarySearch >= 0, "Rect doesn't intesect any known column.");
        int i = iBinarySearch;
        int i2 = i;
        while (i < this.mColumnBounds.size() && this.mColumnBounds.get(i).lowerLimit <= rect.right) {
            i2 = i;
            i++;
        }
        int iBinarySearch2 = Collections.binarySearch(this.mRowBounds, new Limits(rect.top, rect.top));
        if (iBinarySearch2 < 0) {
            this.mPositionNearestOrigin = -1;
            return;
        }
        int i3 = iBinarySearch2;
        int i4 = i3;
        while (i3 < this.mRowBounds.size() && this.mRowBounds.get(i3).lowerLimit <= rect.bottom) {
            i4 = i3;
            i3++;
        }
        updateSelection(iBinarySearch, i2, iBinarySearch2, i4);
    }

    private void updateSelection(int i, int i2, int i3, int i4) {
        this.mSelection.clear();
        for (int i5 = i; i5 <= i2; i5++) {
            SparseIntArray sparseIntArray = this.mColumns.get(this.mColumnBounds.get(i5).lowerLimit);
            for (int i6 = i3; i6 <= i4; i6++) {
                int i7 = sparseIntArray.get(this.mRowBounds.get(i6).lowerLimit, -1);
                if (i7 != -1) {
                    String stableId = this.mStableIds.getStableId(i7);
                    if (stableId != null && canSelect(stableId)) {
                        this.mSelection.add(stableId);
                    }
                    if (isPossiblePositionNearestOrigin(i5, i, i2, i6, i3, i4)) {
                        this.mPositionNearestOrigin = i7;
                    }
                }
            }
        }
    }

    private boolean canSelect(String str) {
        return this.mSelectionPredicate.canSetStateForId(str, true);
    }

    private boolean isPossiblePositionNearestOrigin(int i, int i2, int i3, int i4, int i5, int i6) {
        switch (computeCornerNearestOrigin()) {
            case 0:
                return i == i2 && i4 == i5;
            case 1:
                return i == i2 && i4 == i6;
            case 2:
                return i == i3 && i4 == i5;
            case 3:
                return i4 == i6;
            default:
                throw new RuntimeException("Invalid corner type.");
        }
    }

    void addOnSelectionChangedListener(SelectionObserver selectionObserver) {
        this.mOnSelectionChangedListeners.add(selectionObserver);
    }

    void onDestroy() {
        this.mOnSelectionChangedListeners.clear();
        stopListening();
    }

    private static class Limits implements Comparable<Limits> {
        int lowerLimit;
        int upperLimit;

        Limits(int i, int i2) {
            this.lowerLimit = i;
            this.upperLimit = i2;
        }

        @Override
        public int compareTo(Limits limits) {
            return this.lowerLimit - limits.lowerLimit;
        }

        public int hashCode() {
            return this.lowerLimit ^ this.upperLimit;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Limits)) {
                return false;
            }
            Limits limits = (Limits) obj;
            return limits.lowerLimit == this.lowerLimit && limits.upperLimit == this.upperLimit;
        }

        public String toString() {
            return "(" + this.lowerLimit + ", " + this.upperLimit + ")";
        }
    }

    private static class RelativeCoordinate implements Comparable<RelativeCoordinate> {
        Limits limitsAfterCoordinate;
        Limits limitsBeforeCoordinate;
        Limits mFirstKnownItem;
        Limits mLastKnownItem;
        final int type;

        RelativeCoordinate(List<Limits> list, int i) {
            int iBinarySearch = Collections.binarySearch(list, new Limits(i, i));
            if (iBinarySearch >= 0) {
                this.type = 3;
                this.limitsBeforeCoordinate = list.get(iBinarySearch);
                return;
            }
            int i2 = ~iBinarySearch;
            if (i2 == 0) {
                this.type = 1;
                this.mFirstKnownItem = list.get(0);
                return;
            }
            if (i2 == list.size()) {
                Limits limits = list.get(list.size() - 1);
                if (limits.lowerLimit <= i && i <= limits.upperLimit) {
                    this.type = 3;
                    this.limitsBeforeCoordinate = limits;
                    return;
                } else {
                    this.type = 0;
                    this.mLastKnownItem = limits;
                    return;
                }
            }
            int i3 = i2 - 1;
            Limits limits2 = list.get(i3);
            if (limits2.lowerLimit <= i && i <= limits2.upperLimit) {
                this.type = 3;
                this.limitsBeforeCoordinate = list.get(i3);
            } else {
                this.type = 2;
                this.limitsBeforeCoordinate = list.get(i3);
                this.limitsAfterCoordinate = list.get(i2);
            }
        }

        int toComparisonValue() {
            if (this.type == 1) {
                return this.mFirstKnownItem.lowerLimit - 1;
            }
            if (this.type == 0) {
                return this.mLastKnownItem.upperLimit + 1;
            }
            if (this.type == 2) {
                return this.limitsBeforeCoordinate.upperLimit + 1;
            }
            return this.limitsBeforeCoordinate.lowerLimit;
        }

        public int hashCode() {
            return ((this.mFirstKnownItem.lowerLimit ^ this.mLastKnownItem.upperLimit) ^ this.limitsBeforeCoordinate.upperLimit) ^ this.limitsBeforeCoordinate.lowerLimit;
        }

        public boolean equals(Object obj) {
            return (obj instanceof RelativeCoordinate) && toComparisonValue() == ((RelativeCoordinate) obj).toComparisonValue();
        }

        @Override
        public int compareTo(RelativeCoordinate relativeCoordinate) {
            return toComparisonValue() - relativeCoordinate.toComparisonValue();
        }
    }

    private class RelativePoint {
        final RelativeCoordinate xLocation;
        final RelativeCoordinate yLocation;

        RelativePoint(Point point) {
            this.xLocation = new RelativeCoordinate(GridModel.this.mColumnBounds, point.x);
            this.yLocation = new RelativeCoordinate(GridModel.this.mRowBounds, point.y);
        }

        public int hashCode() {
            return this.xLocation.toComparisonValue() ^ this.yLocation.toComparisonValue();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof RelativePoint)) {
                return false;
            }
            RelativePoint relativePoint = (RelativePoint) obj;
            return this.xLocation.equals(relativePoint.xLocation) && this.yLocation.equals(relativePoint.yLocation);
        }
    }

    private Rect computeBounds() {
        Rect rect = new Rect();
        rect.left = getCoordinateValue(min(this.mRelativeOrigin.xLocation, this.mRelativePointer.xLocation), this.mColumnBounds, true);
        rect.right = getCoordinateValue(max(this.mRelativeOrigin.xLocation, this.mRelativePointer.xLocation), this.mColumnBounds, false);
        rect.top = getCoordinateValue(min(this.mRelativeOrigin.yLocation, this.mRelativePointer.yLocation), this.mRowBounds, true);
        rect.bottom = getCoordinateValue(max(this.mRelativeOrigin.yLocation, this.mRelativePointer.yLocation), this.mRowBounds, false);
        return rect;
    }

    private int computeCornerNearestOrigin() {
        int i;
        if (this.mRelativeOrigin.yLocation != min(this.mRelativeOrigin.yLocation, this.mRelativePointer.yLocation)) {
            i = 1;
        } else {
            i = 0;
        }
        if (this.mRelativeOrigin.xLocation == min(this.mRelativeOrigin.xLocation, this.mRelativePointer.xLocation)) {
            return i | 0;
        }
        return i | 2;
    }

    private RelativeCoordinate min(RelativeCoordinate relativeCoordinate, RelativeCoordinate relativeCoordinate2) {
        return relativeCoordinate.compareTo(relativeCoordinate2) < 0 ? relativeCoordinate : relativeCoordinate2;
    }

    private RelativeCoordinate max(RelativeCoordinate relativeCoordinate, RelativeCoordinate relativeCoordinate2) {
        return relativeCoordinate.compareTo(relativeCoordinate2) > 0 ? relativeCoordinate : relativeCoordinate2;
    }

    private int getCoordinateValue(RelativeCoordinate relativeCoordinate, List<Limits> list, boolean z) {
        switch (relativeCoordinate.type) {
            case 0:
                return list.get(list.size() - 1).upperLimit;
            case 1:
                return list.get(0).lowerLimit;
            case 2:
                if (z) {
                    return relativeCoordinate.limitsAfterCoordinate.lowerLimit;
                }
                return relativeCoordinate.limitsBeforeCoordinate.upperLimit;
            case 3:
                return relativeCoordinate.limitsBeforeCoordinate.lowerLimit;
            default:
                throw new RuntimeException("Invalid coordinate value.");
        }
    }

    private boolean areItemsCoveredByBand(RelativePoint relativePoint, RelativePoint relativePoint2) {
        return doesCoordinateLocationCoverItems(relativePoint.xLocation, relativePoint2.xLocation) && doesCoordinateLocationCoverItems(relativePoint.yLocation, relativePoint2.yLocation);
    }

    private boolean doesCoordinateLocationCoverItems(RelativeCoordinate relativeCoordinate, RelativeCoordinate relativeCoordinate2) {
        if (relativeCoordinate.type == 1 && relativeCoordinate2.type == 1) {
            return false;
        }
        if (relativeCoordinate.type == 0 && relativeCoordinate2.type == 0) {
            return false;
        }
        return (relativeCoordinate.type == 2 && relativeCoordinate2.type == 2 && relativeCoordinate.limitsBeforeCoordinate.equals(relativeCoordinate2.limitsBeforeCoordinate) && relativeCoordinate.limitsAfterCoordinate.equals(relativeCoordinate2.limitsAfterCoordinate)) ? false : true;
    }
}
