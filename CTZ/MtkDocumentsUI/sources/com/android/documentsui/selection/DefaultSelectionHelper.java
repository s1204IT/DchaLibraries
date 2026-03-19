package com.android.documentsui.selection;

import android.support.v4.util.Preconditions;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import com.android.documentsui.selection.Range;
import com.android.documentsui.selection.SelectionHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DefaultSelectionHelper extends SelectionHelper {
    private final RecyclerView.Adapter<?> mAdapter;
    private final RecyclerView.AdapterDataObserver mAdapterObserver;
    private Range mRange;
    private final RangeCallbacks mRangeCallbacks;
    private final SelectionHelper.SelectionPredicate mSelectionPredicate;
    private final boolean mSingleSelect;
    private final SelectionHelper.StableIdProvider mStableIds;
    private final Selection mSelection = new Selection();
    private final List<SelectionHelper.SelectionObserver> mObservers = new ArrayList(1);

    public DefaultSelectionHelper(int i, RecyclerView.Adapter<?> adapter, SelectionHelper.StableIdProvider stableIdProvider, SelectionHelper.SelectionPredicate selectionPredicate) {
        Preconditions.checkArgument(i == 1 || i == 0);
        Preconditions.checkArgument(adapter != null);
        Preconditions.checkArgument(stableIdProvider != null);
        Preconditions.checkArgument(selectionPredicate != null);
        this.mAdapter = adapter;
        this.mStableIds = stableIdProvider;
        this.mSelectionPredicate = selectionPredicate;
        this.mAdapterObserver = new AdapterObserver();
        this.mRangeCallbacks = new RangeCallbacks();
        this.mSingleSelect = i == 1;
        this.mAdapter.registerAdapterDataObserver(this.mAdapterObserver);
    }

    @Override
    public void addObserver(SelectionHelper.SelectionObserver selectionObserver) {
        Preconditions.checkArgument(selectionObserver != null);
        this.mObservers.add(selectionObserver);
    }

    @Override
    public boolean hasSelection() {
        return !this.mSelection.isEmpty();
    }

    @Override
    public Selection getSelection() {
        return this.mSelection;
    }

    @Override
    public void copySelection(Selection selection) {
        selection.copyFrom(this.mSelection);
    }

    @Override
    public boolean isSelected(String str) {
        return this.mSelection.contains(str);
    }

    @Override
    public void restoreSelection(Selection selection) {
        setItemsSelectedQuietly(selection.mSelection, true);
        notifySelectionRestored();
    }

    @Override
    public boolean setItemsSelected(Iterable<String> iterable, boolean z) {
        boolean itemsSelectedQuietly = setItemsSelectedQuietly(iterable, z);
        notifySelectionChanged();
        return itemsSelectedQuietly;
    }

    private boolean setItemsSelectedQuietly(Iterable<String> iterable, boolean z) {
        boolean z2 = false;
        for (String str : iterable) {
            boolean z3 = true;
            if (!z ? !canSetState(str, false) || !this.mSelection.remove(str) : !canSetState(str, true) || !this.mSelection.add(str)) {
                z3 = false;
            }
            if (z3) {
                notifyItemStateChanged(str, z);
            }
            z2 |= z3;
        }
        return z2;
    }

    @Override
    public void clearSelection() {
        if (!hasSelection()) {
            return;
        }
        notifySelectionCleared(clearSelectionQuietly());
        notifySelectionChanged();
    }

    private Selection clearSelectionQuietly() {
        this.mRange = null;
        Selection selection = new Selection();
        if (hasSelection()) {
            copySelection(selection);
            this.mSelection.clear();
        }
        return selection;
    }

    @Override
    public boolean select(String str) {
        Preconditions.checkArgument(str != null);
        if (this.mSelection.contains(str) || !canSetState(str, true)) {
            return false;
        }
        if (this.mSingleSelect && hasSelection()) {
            notifySelectionCleared(clearSelectionQuietly());
        }
        this.mSelection.add(str);
        notifyItemStateChanged(str, true);
        notifySelectionChanged();
        return true;
    }

    @Override
    public boolean deselect(String str) {
        Preconditions.checkArgument(str != null);
        if (!this.mSelection.contains(str) || !canSetState(str, false)) {
            return false;
        }
        this.mSelection.remove(str);
        notifyItemStateChanged(str, false);
        notifySelectionChanged();
        if (this.mSelection.isEmpty() && isRangeActive()) {
            endRange();
        }
        return true;
    }

    @Override
    public void startRange(int i) {
        select(this.mStableIds.getStableId(i));
        anchorRange(i);
    }

    @Override
    public void extendRange(int i) {
        extendRange(i, 0);
    }

    @Override
    public void endRange() {
        this.mRange = null;
        clearProvisionalSelection();
    }

    @Override
    public void anchorRange(int i) {
        Preconditions.checkArgument(i != -1);
        if (this.mSelection.contains(this.mStableIds.getStableId(i))) {
            this.mRange = new Range(this.mRangeCallbacks, i);
        }
    }

    @Override
    public void extendProvisionalRange(int i) {
        extendRange(i, 1);
    }

    private void extendRange(int i, int i2) {
        Preconditions.checkState(isRangeActive(), "Range start point not set.");
        this.mRange.extendSelection(i, i2);
        notifySelectionChanged();
    }

    @Override
    public void setProvisionalSelection(Set<String> set) {
        for (Map.Entry<String, Boolean> entry : this.mSelection.setProvisionalSelection(set).entrySet()) {
            notifyItemStateChanged(entry.getKey(), entry.getValue().booleanValue());
        }
        notifySelectionChanged();
    }

    @Override
    public void mergeProvisionalSelection() {
        this.mSelection.mergeProvisionalSelection();
    }

    @Override
    public void clearProvisionalSelection() {
        Iterator<String> it = this.mSelection.mProvisionalSelection.iterator();
        while (it.hasNext()) {
            notifyItemStateChanged(it.next(), false);
        }
        this.mSelection.clearProvisionalSelection();
    }

    @Override
    public boolean isRangeActive() {
        return this.mRange != null;
    }

    private boolean canSetState(String str, boolean z) {
        return this.mSelectionPredicate.canSetStateForId(str, z);
    }

    private void onDataSetChanged() {
        this.mSelection.clearProvisionalSelection();
        this.mSelection.intersect(this.mStableIds.getStableIds());
        notifySelectionReset();
        for (String str : this.mSelection) {
            if (canSetState(str, true)) {
                for (int size = this.mObservers.size() - 1; size >= 0; size--) {
                    this.mObservers.get(size).onItemStateChanged(str, true);
                }
            } else {
                deselect(str);
            }
        }
        notifySelectionChanged();
    }

    private void onDataSetItemRangeInserted(int i, int i2) {
        this.mSelection.clearProvisionalSelection();
    }

    private void onDataSetItemRangeRemoved(int i, int i2) {
        Preconditions.checkArgument(i >= 0);
        Preconditions.checkArgument(i2 > 0);
        this.mSelection.clearProvisionalSelection();
        this.mSelection.intersect(this.mStableIds.getStableIds());
    }

    private void notifyItemStateChanged(String str, boolean z) {
        Preconditions.checkArgument(str != null);
        for (int size = this.mObservers.size() - 1; size >= 0; size--) {
            this.mObservers.get(size).onItemStateChanged(str, z);
        }
        int position = this.mStableIds.getPosition(str);
        if (position >= 0) {
            this.mAdapter.notifyItemChanged(position, "Selection-Changed");
            return;
        }
        Log.w("SelectionHelper", "Item change notification received for unknown item: " + str);
    }

    private void notifySelectionCleared(Selection selection) {
        Iterator<String> it = selection.mSelection.iterator();
        while (it.hasNext()) {
            notifyItemStateChanged(it.next(), false);
        }
        Iterator<String> it2 = selection.mProvisionalSelection.iterator();
        while (it2.hasNext()) {
            notifyItemStateChanged(it2.next(), false);
        }
    }

    private void notifySelectionChanged() {
        for (int size = this.mObservers.size() - 1; size >= 0; size--) {
            this.mObservers.get(size).onSelectionChanged();
        }
    }

    private void notifySelectionRestored() {
        for (int size = this.mObservers.size() - 1; size >= 0; size--) {
            this.mObservers.get(size).onSelectionRestored();
        }
    }

    private void notifySelectionReset() {
        for (int size = this.mObservers.size() - 1; size >= 0; size--) {
            this.mObservers.get(size).onSelectionReset();
        }
    }

    private void updateForRegularRange(int i, int i2, boolean z) {
        Preconditions.checkArgument(i2 >= i);
        while (i <= i2) {
            String stableId = this.mStableIds.getStableId(i);
            if (stableId != null) {
                if (z) {
                    select(stableId);
                } else {
                    deselect(stableId);
                }
            }
            i++;
        }
    }

    private void updateForProvisionalRange(int i, int i2, boolean z) {
        boolean z2;
        Preconditions.checkArgument(i2 >= i);
        while (i <= i2) {
            String stableId = this.mStableIds.getStableId(i);
            if (stableId != null) {
                if (z) {
                    if (!canSetState(stableId, true) || this.mSelection.mSelection.contains(stableId)) {
                        z2 = false;
                    } else {
                        this.mSelection.mProvisionalSelection.add(stableId);
                        z2 = true;
                    }
                } else {
                    this.mSelection.mProvisionalSelection.remove(stableId);
                    z2 = true;
                }
                if (z2) {
                    notifyItemStateChanged(stableId, z);
                }
            }
            i++;
        }
        notifySelectionChanged();
    }

    private final class AdapterObserver extends RecyclerView.AdapterDataObserver {
        private AdapterObserver() {
        }

        @Override
        public void onChanged() {
            DefaultSelectionHelper.this.onDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int i, int i2, Object obj) {
        }

        @Override
        public void onItemRangeInserted(int i, int i2) {
            DefaultSelectionHelper.this.onDataSetItemRangeInserted(i, i2);
        }

        @Override
        public void onItemRangeRemoved(int i, int i2) {
            DefaultSelectionHelper.this.onDataSetItemRangeRemoved(i, i2);
        }
    }

    private final class RangeCallbacks extends Range.Callbacks {
        private RangeCallbacks() {
        }

        @Override
        void updateForRange(int i, int i2, boolean z, int i3) {
            switch (i3) {
                case 0:
                    DefaultSelectionHelper.this.updateForRegularRange(i, i2, z);
                    return;
                case 1:
                    DefaultSelectionHelper.this.updateForProvisionalRange(i, i2, z);
                    return;
                default:
                    throw new IllegalArgumentException("Invalid range type: " + i3);
            }
        }
    }
}
