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

    public HeaderGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHeaderViewInfos = new ArrayList<>();
        initHeaderGridView();
    }

    public HeaderGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mHeaderViewInfos = new ArrayList<>();
        initHeaderGridView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ListAdapter adapter = getAdapter();
        if (adapter != null && (adapter instanceof HeaderViewGridAdapter)) {
            ((HeaderViewGridAdapter) adapter).setNumColumns(getNumColumns());
        }
    }

    @Override
    public void setClipChildren(boolean clipChildren) {
    }

    public void addHeaderView(View v, Object data, boolean isSelectable) {
        ListAdapter adapter = getAdapter();
        if (adapter != null && !(adapter instanceof HeaderViewGridAdapter)) {
            throw new IllegalStateException("Cannot add header view to grid -- setAdapter has already been called.");
        }
        FixedViewInfo info = new FixedViewInfo();
        FrameLayout fl = new FullWidthFixedViewLayout(getContext());
        fl.addView(v);
        info.view = v;
        info.viewContainer = fl;
        info.data = data;
        info.isSelectable = isSelectable;
        this.mHeaderViewInfos.add(info);
        if (adapter != null) {
            ((HeaderViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (this.mHeaderViewInfos.size() > 0) {
            HeaderViewGridAdapter hadapter = new HeaderViewGridAdapter(this.mHeaderViewInfos, adapter);
            int numColumns = getNumColumns();
            if (numColumns > 1) {
                hadapter.setNumColumns(numColumns);
            }
            super.setAdapter((ListAdapter) hadapter);
            return;
        }
        super.setAdapter(adapter);
    }

    private class FullWidthFixedViewLayout extends FrameLayout {
        public FullWidthFixedViewLayout(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int targetWidth = (HeaderGridView.this.getMeasuredWidth() - HeaderGridView.this.getPaddingLeft()) - HeaderGridView.this.getPaddingRight();
            super.onMeasure(View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.getMode(widthMeasureSpec)), heightMeasureSpec);
        }
    }

    private static class HeaderViewGridAdapter implements Filterable, WrapperListAdapter {
        private final ListAdapter mAdapter;
        boolean mAreAllFixedViewsSelectable;
        ArrayList<FixedViewInfo> mHeaderViewInfos;
        private final boolean mIsFilterable;
        private final DataSetObservable mDataSetObservable = new DataSetObservable();
        private int mNumColumns = 1;

        public HeaderViewGridAdapter(ArrayList<FixedViewInfo> headerViewInfos, ListAdapter adapter) {
            this.mAdapter = adapter;
            this.mIsFilterable = adapter instanceof Filterable;
            if (headerViewInfos == null) {
                throw new IllegalArgumentException("headerViewInfos cannot be null");
            }
            this.mHeaderViewInfos = headerViewInfos;
            this.mAreAllFixedViewsSelectable = areAllListInfosSelectable(this.mHeaderViewInfos);
        }

        public int getHeadersCount() {
            return this.mHeaderViewInfos.size();
        }

        @Override
        public boolean isEmpty() {
            return (this.mAdapter == null || this.mAdapter.isEmpty()) && getHeadersCount() == 0;
        }

        public void setNumColumns(int numColumns) {
            if (numColumns < 1) {
                throw new IllegalArgumentException("Number of columns must be 1 or more");
            }
            if (this.mNumColumns != numColumns) {
                this.mNumColumns = numColumns;
                notifyDataSetChanged();
            }
        }

        private boolean areAllListInfosSelectable(ArrayList<FixedViewInfo> infos) {
            if (infos != null) {
                for (FixedViewInfo info : infos) {
                    if (!info.isSelectable) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public int getCount() {
            return this.mAdapter != null ? (getHeadersCount() * this.mNumColumns) + this.mAdapter.getCount() : getHeadersCount() * this.mNumColumns;
        }

        @Override
        public boolean areAllItemsEnabled() {
            if (this.mAdapter != null) {
                return this.mAreAllFixedViewsSelectable && this.mAdapter.areAllItemsEnabled();
            }
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * this.mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                return position % this.mNumColumns == 0 && this.mHeaderViewInfos.get(position / this.mNumColumns).isSelectable;
            }
            int adjPosition = position - numHeadersAndPlaceholders;
            if (this.mAdapter != null) {
                int adapterCount = this.mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return this.mAdapter.isEnabled(adjPosition);
                }
            }
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public Object getItem(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * this.mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                if (position % this.mNumColumns == 0) {
                    return this.mHeaderViewInfos.get(position / this.mNumColumns).data;
                }
                return null;
            }
            int adjPosition = position - numHeadersAndPlaceholders;
            if (this.mAdapter != null) {
                int adapterCount = this.mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return this.mAdapter.getItem(adjPosition);
                }
            }
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public long getItemId(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * this.mNumColumns;
            if (this.mAdapter != null && position >= numHeadersAndPlaceholders) {
                int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = this.mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return this.mAdapter.getItemId(adjPosition);
                }
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
        public View getView(int position, View convertView, ViewGroup parent) {
            int numHeadersAndPlaceholders = getHeadersCount() * this.mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                View headerViewContainer = this.mHeaderViewInfos.get(position / this.mNumColumns).viewContainer;
                if (position % this.mNumColumns != 0) {
                    if (convertView == null) {
                        convertView = new View(parent.getContext());
                    }
                    convertView.setVisibility(4);
                    convertView.setMinimumHeight(headerViewContainer.getHeight());
                    return convertView;
                }
                return headerViewContainer;
            }
            int adjPosition = position - numHeadersAndPlaceholders;
            if (this.mAdapter != null) {
                int adapterCount = this.mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return this.mAdapter.getView(adjPosition, convertView, parent);
                }
            }
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public int getItemViewType(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * this.mNumColumns;
            if (position < numHeadersAndPlaceholders && position % this.mNumColumns != 0) {
                if (this.mAdapter != null) {
                    return this.mAdapter.getViewTypeCount();
                }
                return 1;
            }
            if (this.mAdapter != null && position >= numHeadersAndPlaceholders) {
                int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = this.mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return this.mAdapter.getItemViewType(adjPosition);
                }
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
        public void registerDataSetObserver(DataSetObserver observer) {
            this.mDataSetObservable.registerObserver(observer);
            if (this.mAdapter != null) {
                this.mAdapter.registerDataSetObserver(observer);
            }
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            this.mDataSetObservable.unregisterObserver(observer);
            if (this.mAdapter != null) {
                this.mAdapter.unregisterDataSetObserver(observer);
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
