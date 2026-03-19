package com.android.documentsui;

import android.support.v7.widget.RecyclerView;
import com.android.documentsui.selection.DefaultSelectionHelper;
import com.android.documentsui.selection.MutableSelection;
import com.android.documentsui.selection.Selection;
import com.android.documentsui.selection.SelectionHelper;
import java.util.Set;

public final class DocsSelectionHelper extends SelectionHelper {
    private SelectionHelper mDelegate = new DummySelectionHelper();
    private final DelegateFactory mFactory;
    private final int mSelectionMode;

    DocsSelectionHelper(DelegateFactory delegateFactory, int i) {
        this.mFactory = delegateFactory;
        this.mSelectionMode = i;
    }

    public SelectionHelper reset(RecyclerView.Adapter<?> adapter, SelectionHelper.StableIdProvider stableIdProvider, SelectionHelper.SelectionPredicate selectionPredicate) {
        if (this.mDelegate != null) {
            this.mDelegate.clearSelection();
        }
        this.mDelegate = this.mFactory.create(this.mSelectionMode, adapter, stableIdProvider, selectionPredicate);
        return this;
    }

    @Override
    public void addObserver(SelectionHelper.SelectionObserver selectionObserver) {
        this.mDelegate.addObserver(selectionObserver);
    }

    @Override
    public boolean hasSelection() {
        return this.mDelegate.hasSelection();
    }

    @Override
    public Selection getSelection() {
        return this.mDelegate.getSelection();
    }

    @Override
    public void copySelection(Selection selection) {
        this.mDelegate.copySelection(selection);
    }

    @Override
    public boolean isSelected(String str) {
        return this.mDelegate.isSelected(str);
    }

    public void replaceSelection(Iterable<String> iterable) {
        this.mDelegate.clearSelection();
        this.mDelegate.setItemsSelected(iterable, true);
    }

    @Override
    public void restoreSelection(Selection selection) {
        this.mDelegate.restoreSelection(selection);
    }

    @Override
    public boolean setItemsSelected(Iterable<String> iterable, boolean z) {
        return this.mDelegate.setItemsSelected(iterable, z);
    }

    @Override
    public void clearSelection() {
        this.mDelegate.clearSelection();
    }

    @Override
    public boolean select(String str) {
        return this.mDelegate.select(str);
    }

    @Override
    public boolean deselect(String str) {
        return this.mDelegate.deselect(str);
    }

    @Override
    public void startRange(int i) {
        this.mDelegate.startRange(i);
    }

    @Override
    public void extendRange(int i) {
        this.mDelegate.extendRange(i);
    }

    @Override
    public void extendProvisionalRange(int i) {
        this.mDelegate.extendProvisionalRange(i);
    }

    @Override
    public void clearProvisionalSelection() {
        this.mDelegate.clearProvisionalSelection();
    }

    @Override
    public void setProvisionalSelection(Set<String> set) {
        this.mDelegate.setProvisionalSelection(set);
    }

    @Override
    public void mergeProvisionalSelection() {
        this.mDelegate.mergeProvisionalSelection();
    }

    @Override
    public void endRange() {
        this.mDelegate.endRange();
    }

    @Override
    public boolean isRangeActive() {
        return this.mDelegate.isRangeActive();
    }

    @Override
    public void anchorRange(int i) {
        this.mDelegate.anchorRange(i);
    }

    public static DocsSelectionHelper createMultiSelect() {
        return new DocsSelectionHelper(DelegateFactory.INSTANCE, 0);
    }

    public static DocsSelectionHelper createSingleSelect() {
        return new DocsSelectionHelper(DelegateFactory.INSTANCE, 1);
    }

    static class DelegateFactory {
        static final DelegateFactory INSTANCE = new DelegateFactory();

        DelegateFactory() {
        }

        SelectionHelper create(int i, RecyclerView.Adapter<?> adapter, SelectionHelper.StableIdProvider stableIdProvider, SelectionHelper.SelectionPredicate selectionPredicate) {
            return new DefaultSelectionHelper(i, adapter, stableIdProvider, selectionPredicate);
        }
    }

    private static final class DummySelectionHelper extends SelectionHelper {
        private DummySelectionHelper() {
        }

        @Override
        public void addObserver(SelectionHelper.SelectionObserver selectionObserver) {
        }

        @Override
        public boolean hasSelection() {
            return false;
        }

        @Override
        public Selection getSelection() {
            return new MutableSelection();
        }

        @Override
        public void copySelection(Selection selection) {
        }

        @Override
        public boolean isSelected(String str) {
            return false;
        }

        public void replaceSelection(Iterable<String> iterable) {
        }

        @Override
        public void restoreSelection(Selection selection) {
        }

        @Override
        public boolean setItemsSelected(Iterable<String> iterable, boolean z) {
            return false;
        }

        @Override
        public void clearSelection() {
        }

        @Override
        public boolean select(String str) {
            return false;
        }

        @Override
        public boolean deselect(String str) {
            return false;
        }

        @Override
        public void startRange(int i) {
        }

        @Override
        public void extendRange(int i) {
        }

        @Override
        public void extendProvisionalRange(int i) {
        }

        @Override
        public void clearProvisionalSelection() {
        }

        @Override
        public void setProvisionalSelection(Set<String> set) {
        }

        @Override
        public void mergeProvisionalSelection() {
        }

        @Override
        public void endRange() {
        }

        @Override
        public boolean isRangeActive() {
            return false;
        }

        @Override
        public void anchorRange(int i) {
        }
    }
}
