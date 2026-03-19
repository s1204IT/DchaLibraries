package com.android.documentsui.selection;

import java.util.List;
import java.util.Set;

public abstract class SelectionHelper {

    public static abstract class StableIdProvider {
        public abstract int getPosition(String str);

        public abstract String getStableId(int i);

        public abstract List<String> getStableIds();
    }

    public abstract void addObserver(SelectionObserver selectionObserver);

    public abstract void anchorRange(int i);

    public abstract void clearProvisionalSelection();

    public abstract void clearSelection();

    public abstract void copySelection(Selection selection);

    public abstract boolean deselect(String str);

    public abstract void endRange();

    public abstract void extendProvisionalRange(int i);

    public abstract void extendRange(int i);

    public abstract Selection getSelection();

    public abstract boolean hasSelection();

    public abstract boolean isRangeActive();

    public abstract boolean isSelected(String str);

    public abstract void mergeProvisionalSelection();

    public abstract void restoreSelection(Selection selection);

    public abstract boolean select(String str);

    public abstract boolean setItemsSelected(Iterable<String> iterable, boolean z);

    public abstract void setProvisionalSelection(Set<String> set);

    public abstract void startRange(int i);

    public static abstract class SelectionObserver {
        public void onItemStateChanged(String str, boolean z) {
        }

        public void onSelectionReset() {
        }

        public void onSelectionChanged() {
        }

        public void onSelectionRestored() {
        }
    }

    public static abstract class SelectionPredicate {
        public abstract boolean canSetStateAtPosition(int i, boolean z);

        public abstract boolean canSetStateForId(String str, boolean z);

        public boolean canSelectMultiple() {
            return true;
        }
    }
}
