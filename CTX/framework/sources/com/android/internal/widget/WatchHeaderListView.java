package com.android.internal.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.function.Predicate;

public class WatchHeaderListView extends ListView {
    private View mTopPanel;

    public WatchHeaderListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public WatchHeaderListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public WatchHeaderListView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override
    protected HeaderViewListAdapter wrapHeaderListAdapterInternal(ArrayList<ListView.FixedViewInfo> arrayList, ArrayList<ListView.FixedViewInfo> arrayList2, ListAdapter listAdapter) {
        return new WatchHeaderListAdapter(arrayList, arrayList2, listAdapter);
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams layoutParams) {
        if (this.mTopPanel == null) {
            setTopPanel(view);
            return;
        }
        throw new IllegalStateException("WatchHeaderListView can host only one header");
    }

    public void setTopPanel(View view) {
        this.mTopPanel = view;
        wrapAdapterIfNecessary();
    }

    @Override
    public void setAdapter(ListAdapter listAdapter) {
        super.setAdapter(listAdapter);
        wrapAdapterIfNecessary();
    }

    @Override
    protected View findViewTraversal(int i) {
        View viewFindViewTraversal = super.findViewTraversal(i);
        if (viewFindViewTraversal == null && this.mTopPanel != null && !this.mTopPanel.isRootNamespace()) {
            return this.mTopPanel.findViewById(i);
        }
        return viewFindViewTraversal;
    }

    @Override
    protected View findViewWithTagTraversal(Object obj) {
        View viewFindViewWithTagTraversal = super.findViewWithTagTraversal(obj);
        if (viewFindViewWithTagTraversal == null && this.mTopPanel != null && !this.mTopPanel.isRootNamespace()) {
            return this.mTopPanel.findViewWithTag(obj);
        }
        return viewFindViewWithTagTraversal;
    }

    @Override
    protected <T extends View> T findViewByPredicateTraversal(Predicate<View> predicate, View view) {
        T t = (T) super.findViewByPredicateTraversal(predicate, view);
        if (t == null && this.mTopPanel != null && this.mTopPanel != view && !this.mTopPanel.isRootNamespace()) {
            return (T) this.mTopPanel.findViewByPredicate(predicate);
        }
        return t;
    }

    @Override
    public int getHeaderViewsCount() {
        if (this.mTopPanel == null) {
            return super.getHeaderViewsCount();
        }
        return super.getHeaderViewsCount() + (this.mTopPanel.getVisibility() == 8 ? 0 : 1);
    }

    private void wrapAdapterIfNecessary() {
        ListAdapter adapter = getAdapter();
        if (adapter != null && this.mTopPanel != null) {
            if (!(adapter instanceof WatchHeaderListAdapter)) {
                wrapHeaderListAdapterInternal();
            }
            ((WatchHeaderListAdapter) getAdapter()).setTopPanel(this.mTopPanel);
            dispatchDataSetObserverOnChangedInternal();
        }
    }

    private static class WatchHeaderListAdapter extends HeaderViewListAdapter {
        private View mTopPanel;

        public WatchHeaderListAdapter(ArrayList<ListView.FixedViewInfo> arrayList, ArrayList<ListView.FixedViewInfo> arrayList2, ListAdapter listAdapter) {
            super(arrayList, arrayList2, listAdapter);
        }

        public void setTopPanel(View view) {
            this.mTopPanel = view;
        }

        private int getTopPanelCount() {
            return (this.mTopPanel == null || this.mTopPanel.getVisibility() == 8) ? 0 : 1;
        }

        @Override
        public int getCount() {
            return super.getCount() + getTopPanelCount();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return getTopPanelCount() == 0 && super.areAllItemsEnabled();
        }

        @Override
        public boolean isEnabled(int i) {
            int topPanelCount = getTopPanelCount();
            if (i < topPanelCount) {
                return false;
            }
            return super.isEnabled(i - topPanelCount);
        }

        @Override
        public Object getItem(int i) {
            int topPanelCount = getTopPanelCount();
            if (i < topPanelCount) {
                return null;
            }
            return super.getItem(i - topPanelCount);
        }

        @Override
        public long getItemId(int i) {
            int i2;
            int headersCount = getHeadersCount() + getTopPanelCount();
            if (getWrappedAdapter() != null && i >= headersCount && (i2 = i - headersCount) < getWrappedAdapter().getCount()) {
                return getWrappedAdapter().getItemId(i2);
            }
            return -1L;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int topPanelCount = getTopPanelCount();
            return i < topPanelCount ? this.mTopPanel : super.getView(i - topPanelCount, view, viewGroup);
        }

        @Override
        public int getItemViewType(int i) {
            int i2;
            int headersCount = getHeadersCount() + getTopPanelCount();
            if (getWrappedAdapter() != null && i >= headersCount && (i2 = i - headersCount) < getWrappedAdapter().getCount()) {
                return getWrappedAdapter().getItemViewType(i2);
            }
            return -2;
        }
    }
}
