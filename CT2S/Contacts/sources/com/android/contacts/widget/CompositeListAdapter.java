package com.android.contacts.widget;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

public class CompositeListAdapter extends BaseAdapter {
    private ListAdapter[] mAdapters;
    private boolean mAllItemsEnabled;
    private boolean mCacheValid;
    private int mCount;
    private int[] mCounts;
    private DataSetObserver mDataSetObserver;
    private int mSize;
    private int mViewTypeCount;
    private int[] mViewTypeCounts;

    public CompositeListAdapter() {
        this(2);
    }

    public CompositeListAdapter(int initialCapacity) {
        this.mSize = 0;
        this.mCount = 0;
        this.mViewTypeCount = 0;
        this.mAllItemsEnabled = true;
        this.mCacheValid = true;
        this.mDataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                CompositeListAdapter.this.invalidate();
                CompositeListAdapter.this.notifyDataChanged();
            }

            @Override
            public void onInvalidated() {
                CompositeListAdapter.this.invalidate();
                CompositeListAdapter.this.notifyDataChanged();
            }
        };
        this.mAdapters = new ListAdapter[2];
        this.mCounts = new int[2];
        this.mViewTypeCounts = new int[2];
    }

    void addAdapter(ListAdapter adapter) {
        if (this.mSize >= this.mAdapters.length) {
            int newCapacity = this.mSize + 2;
            ListAdapter[] newAdapters = new ListAdapter[newCapacity];
            System.arraycopy(this.mAdapters, 0, newAdapters, 0, this.mSize);
            this.mAdapters = newAdapters;
            int[] newCounts = new int[newCapacity];
            System.arraycopy(this.mCounts, 0, newCounts, 0, this.mSize);
            this.mCounts = newCounts;
            int[] newViewTypeCounts = new int[newCapacity];
            System.arraycopy(this.mViewTypeCounts, 0, newViewTypeCounts, 0, this.mSize);
            this.mViewTypeCounts = newViewTypeCounts;
        }
        adapter.registerDataSetObserver(this.mDataSetObserver);
        int count = adapter.getCount();
        int viewTypeCount = adapter.getViewTypeCount();
        this.mAdapters[this.mSize] = adapter;
        this.mCounts[this.mSize] = count;
        this.mCount += count;
        this.mAllItemsEnabled &= adapter.areAllItemsEnabled();
        this.mViewTypeCounts[this.mSize] = viewTypeCount;
        this.mViewTypeCount += viewTypeCount;
        this.mSize++;
        notifyDataChanged();
    }

    protected void notifyDataChanged() {
        if (getCount() > 0) {
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
    }

    protected void invalidate() {
        this.mCacheValid = false;
    }

    protected void ensureCacheValid() {
        if (!this.mCacheValid) {
            this.mCount = 0;
            this.mAllItemsEnabled = true;
            this.mViewTypeCount = 0;
            for (int i = 0; i < this.mSize; i++) {
                int count = this.mAdapters[i].getCount();
                int viewTypeCount = this.mAdapters[i].getViewTypeCount();
                this.mCounts[i] = count;
                this.mCount += count;
                this.mAllItemsEnabled &= this.mAdapters[i].areAllItemsEnabled();
                this.mViewTypeCount += viewTypeCount;
            }
            this.mCacheValid = true;
        }
    }

    @Override
    public int getCount() {
        ensureCacheValid();
        return this.mCount;
    }

    @Override
    public Object getItem(int position) {
        ensureCacheValid();
        int start = 0;
        for (int i = 0; i < this.mCounts.length; i++) {
            int end = start + this.mCounts[i];
            if (position >= start && position < end) {
                return this.mAdapters[i].getItem(position - start);
            }
            start = end;
        }
        throw new ArrayIndexOutOfBoundsException(position);
    }

    @Override
    public long getItemId(int position) {
        ensureCacheValid();
        int start = 0;
        for (int i = 0; i < this.mCounts.length; i++) {
            int end = start + this.mCounts[i];
            if (position >= start && position < end) {
                return this.mAdapters[i].getItemId(position - start);
            }
            start = end;
        }
        throw new ArrayIndexOutOfBoundsException(position);
    }

    @Override
    public int getViewTypeCount() {
        ensureCacheValid();
        return this.mViewTypeCount;
    }

    @Override
    public int getItemViewType(int position) {
        ensureCacheValid();
        int start = 0;
        int viewTypeOffset = 0;
        for (int i = 0; i < this.mCounts.length; i++) {
            int end = start + this.mCounts[i];
            if (position >= start && position < end) {
                return this.mAdapters[i].getItemViewType(position - start) + viewTypeOffset;
            }
            viewTypeOffset += this.mViewTypeCounts[i];
            start = end;
        }
        throw new ArrayIndexOutOfBoundsException(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ensureCacheValid();
        int start = 0;
        for (int i = 0; i < this.mCounts.length; i++) {
            int end = start + this.mCounts[i];
            if (position >= start && position < end) {
                return this.mAdapters[i].getView(position - start, convertView, parent);
            }
            start = end;
        }
        throw new ArrayIndexOutOfBoundsException(position);
    }

    @Override
    public boolean areAllItemsEnabled() {
        ensureCacheValid();
        return this.mAllItemsEnabled;
    }

    @Override
    public boolean isEnabled(int position) {
        ensureCacheValid();
        int start = 0;
        for (int i = 0; i < this.mCounts.length; i++) {
            int end = start + this.mCounts[i];
            if (position >= start && position < end) {
                return this.mAdapters[i].areAllItemsEnabled() || this.mAdapters[i].isEnabled(position - start);
            }
            start = end;
        }
        throw new ArrayIndexOutOfBoundsException(position);
    }
}
