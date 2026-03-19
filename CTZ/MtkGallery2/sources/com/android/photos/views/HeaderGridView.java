package com.android.photos.views;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;
import java.util.ArrayList;
import java.util.Iterator;

public class HeaderGridView extends GridView {
    private ArrayList<FixedViewInfo> mHeaderViewInfos;

    private static class FixedViewInfo {
        public Object data;
        public boolean isSelectable;
        public View view;
        public ViewGroup viewContainer;

        private FixedViewInfo() {
        }
    }

    private void initHeaderGridView() {
        super.setClipChildren(false);
    }

    public HeaderGridView(Context context) {
        super(context);
        this.mHeaderViewInfos = new ArrayList<>();
        initHeaderGridView();
    }

    public HeaderGridView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHeaderViewInfos = new ArrayList<>();
        initHeaderGridView();
    }

    public HeaderGridView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mHeaderViewInfos = new ArrayList<>();
        initHeaderGridView();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        ?? adapter = getAdapter();
        if (adapter == 0 || !(adapter instanceof HeaderViewGridAdapter)) {
            return;
        }
        adapter.setNumColumns(getNumColumns());
    }

    @Override
    public void setClipChildren(boolean z) {
    }

    public void addHeaderView(View view, Object obj, boolean z) {
        ListAdapter adapter = getAdapter();
        if (adapter != null && !(adapter instanceof HeaderViewGridAdapter)) {
            throw new IllegalStateException("Cannot add header view to grid -- setAdapter has already been called.");
        }
        FixedViewInfo fixedViewInfo = new FixedViewInfo();
        FullWidthFixedViewLayout fullWidthFixedViewLayout = new FullWidthFixedViewLayout(getContext());
        fullWidthFixedViewLayout.addView(view);
        fixedViewInfo.view = view;
        fixedViewInfo.viewContainer = fullWidthFixedViewLayout;
        fixedViewInfo.data = obj;
        fixedViewInfo.isSelectable = z;
        this.mHeaderViewInfos.add(fixedViewInfo);
        if (adapter != null) {
            ((HeaderViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    @Override
    public void setAdapter(ListAdapter listAdapter) {
        if (this.mHeaderViewInfos.size() > 0) {
            HeaderViewGridAdapter headerViewGridAdapter = new HeaderViewGridAdapter(this.mHeaderViewInfos, listAdapter);
            int numColumns = getNumColumns();
            if (numColumns > 1) {
                headerViewGridAdapter.setNumColumns(numColumns);
            }
            super.setAdapter((ListAdapter) headerViewGridAdapter);
            return;
        }
        super.setAdapter(listAdapter);
    }

    private class FullWidthFixedViewLayout extends FrameLayout {
        public FullWidthFixedViewLayout(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int i, int i2) {
            super.onMeasure(View.MeasureSpec.makeMeasureSpec((HeaderGridView.this.getMeasuredWidth() - HeaderGridView.this.getPaddingLeft()) - HeaderGridView.this.getPaddingRight(), View.MeasureSpec.getMode(i)), i2);
        }
    }

    private static class HeaderViewGridAdapter implements Filterable, WrapperListAdapter {
        private final ListAdapter mAdapter;
        boolean mAreAllFixedViewsSelectable;
        ArrayList<FixedViewInfo> mHeaderViewInfos;
        private final boolean mIsFilterable;
        private final DataSetObservable mDataSetObservable = new DataSetObservable();
        private int mNumColumns = 1;

        public HeaderViewGridAdapter(ArrayList<FixedViewInfo> arrayList, ListAdapter listAdapter) {
            this.mAdapter = listAdapter;
            this.mIsFilterable = listAdapter instanceof Filterable;
            if (arrayList == null) {
                throw new IllegalArgumentException("headerViewInfos cannot be null");
            }
            this.mHeaderViewInfos = arrayList;
            this.mAreAllFixedViewsSelectable = areAllListInfosSelectable(this.mHeaderViewInfos);
        }

        public int getHeadersCount() {
            return this.mHeaderViewInfos.size();
        }

        @Override
        public boolean isEmpty() {
            return (this.mAdapter == null || this.mAdapter.isEmpty()) && getHeadersCount() == 0;
        }

        public void setNumColumns(int i) {
            if (i < 1) {
                throw new IllegalArgumentException("Number of columns must be 1 or more");
            }
            if (this.mNumColumns != i) {
                this.mNumColumns = i;
                notifyDataSetChanged();
            }
        }

        private boolean areAllListInfosSelectable(ArrayList<FixedViewInfo> arrayList) {
            if (arrayList != null) {
                Iterator<FixedViewInfo> it = arrayList.iterator();
                while (it.hasNext()) {
                    if (!it.next().isSelectable) {
                        return false;
                    }
                }
                return true;
            }
            return true;
        }

        @Override
        public int getCount() {
            if (this.mAdapter != null) {
                return (getHeadersCount() * this.mNumColumns) + this.mAdapter.getCount();
            }
            return getHeadersCount() * this.mNumColumns;
        }

        @Override
        public boolean areAllItemsEnabled() {
            if (this.mAdapter != null) {
                return this.mAreAllFixedViewsSelectable && this.mAdapter.areAllItemsEnabled();
            }
            return true;
        }

        @Override
        public boolean isEnabled(int i) {
            int headersCount = getHeadersCount() * this.mNumColumns;
            if (i < headersCount) {
                return i % this.mNumColumns == 0 && this.mHeaderViewInfos.get(i / this.mNumColumns).isSelectable;
            }
            int i2 = i - headersCount;
            if (this.mAdapter != null && i2 < this.mAdapter.getCount()) {
                return this.mAdapter.isEnabled(i2);
            }
            throw new ArrayIndexOutOfBoundsException(i);
        }

        @Override
        public Object getItem(int i) {
            int headersCount = getHeadersCount() * this.mNumColumns;
            if (i < headersCount) {
                if (i % this.mNumColumns == 0) {
                    return this.mHeaderViewInfos.get(i / this.mNumColumns).data;
                }
                return null;
            }
            int i2 = i - headersCount;
            if (this.mAdapter != null && i2 < this.mAdapter.getCount()) {
                return this.mAdapter.getItem(i2);
            }
            throw new ArrayIndexOutOfBoundsException(i);
        }

        @Override
        public long getItemId(int i) {
            int i2;
            int headersCount = getHeadersCount() * this.mNumColumns;
            if (this.mAdapter != null && i >= headersCount && (i2 = i - headersCount) < this.mAdapter.getCount()) {
                return this.mAdapter.getItemId(i2);
            }
            return -1L;
        }

        @Override
        public boolean hasStableIds() {
            if (this.mAdapter != null) {
                return this.mAdapter.hasStableIds();
            }
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int headersCount = getHeadersCount() * this.mNumColumns;
            if (i < headersCount) {
                ViewGroup viewGroup2 = this.mHeaderViewInfos.get(i / this.mNumColumns).viewContainer;
                if (i % this.mNumColumns == 0) {
                    return viewGroup2;
                }
                if (view == null) {
                    view = new View(viewGroup.getContext());
                }
                view.setVisibility(4);
                view.setMinimumHeight(viewGroup2.getHeight());
                return view;
            }
            int i2 = i - headersCount;
            if (this.mAdapter != null && i2 < this.mAdapter.getCount()) {
                return this.mAdapter.getView(i2, view, viewGroup);
            }
            throw new ArrayIndexOutOfBoundsException(i);
        }

        @Override
        public int getItemViewType(int i) {
            int i2;
            int headersCount = getHeadersCount() * this.mNumColumns;
            if (i < headersCount && i % this.mNumColumns != 0) {
                if (this.mAdapter != null) {
                    return this.mAdapter.getViewTypeCount();
                }
                return 1;
            }
            if (this.mAdapter != null && i >= headersCount && (i2 = i - headersCount) < this.mAdapter.getCount()) {
                return this.mAdapter.getItemViewType(i2);
            }
            return -2;
        }

        @Override
        public int getViewTypeCount() {
            if (this.mAdapter != null) {
                return this.mAdapter.getViewTypeCount() + 1;
            }
            return 2;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver dataSetObserver) {
            this.mDataSetObservable.registerObserver(dataSetObserver);
            if (this.mAdapter != null) {
                this.mAdapter.registerDataSetObserver(dataSetObserver);
            }
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
            this.mDataSetObservable.unregisterObserver(dataSetObserver);
            if (this.mAdapter != null) {
                this.mAdapter.unregisterDataSetObserver(dataSetObserver);
            }
        }

        @Override
        public Filter getFilter() {
            if (this.mIsFilterable) {
                return ((Filterable) this.mAdapter).getFilter();
            }
            return null;
        }

        @Override
        public ListAdapter getWrappedAdapter() {
            return this.mAdapter;
        }

        public void notifyDataSetChanged() {
            this.mDataSetObservable.notifyChanged();
        }
    }
}
