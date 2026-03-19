package com.android.contacts.widget;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

public class CompositeListAdapter extends BaseAdapter {
    private static final int INITIAL_CAPACITY = 2;
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

    public CompositeListAdapter(int i) {
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

    void addAdapter(ListAdapter listAdapter) {
        if (this.mSize >= this.mAdapters.length) {
            int i = this.mSize + 2;
            ListAdapter[] listAdapterArr = new ListAdapter[i];
            System.arraycopy(this.mAdapters, 0, listAdapterArr, 0, this.mSize);
            this.mAdapters = listAdapterArr;
            int[] iArr = new int[i];
            System.arraycopy(this.mCounts, 0, iArr, 0, this.mSize);
            this.mCounts = iArr;
            int[] iArr2 = new int[i];
            System.arraycopy(this.mViewTypeCounts, 0, iArr2, 0, this.mSize);
            this.mViewTypeCounts = iArr2;
        }
        listAdapter.registerDataSetObserver(this.mDataSetObserver);
        int count = listAdapter.getCount();
        int viewTypeCount = listAdapter.getViewTypeCount();
        this.mAdapters[this.mSize] = listAdapter;
        this.mCounts[this.mSize] = count;
        this.mCount += count;
        this.mAllItemsEnabled = listAdapter.areAllItemsEnabled() & this.mAllItemsEnabled;
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
        if (this.mCacheValid) {
            return;
        }
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

    @Override
    public int getCount() {
        ensureCacheValid();
        return this.mCount;
    }

    @Override
    public Object getItem(int i) {
        ensureCacheValid();
        int i2 = 0;
        int i3 = 0;
        while (i2 < this.mCounts.length) {
            int i4 = this.mCounts[i2] + i3;
            if (i < i3 || i >= i4) {
                i2++;
                i3 = i4;
            } else {
                return this.mAdapters[i2].getItem(i - i3);
            }
        }
        throw new ArrayIndexOutOfBoundsException(i);
    }

    @Override
    public long getItemId(int i) {
        ensureCacheValid();
        int i2 = 0;
        int i3 = 0;
        while (i2 < this.mCounts.length) {
            int i4 = this.mCounts[i2] + i3;
            if (i < i3 || i >= i4) {
                i2++;
                i3 = i4;
            } else {
                return this.mAdapters[i2].getItemId(i - i3);
            }
        }
        throw new ArrayIndexOutOfBoundsException(i);
    }

    @Override
    public int getViewTypeCount() {
        ensureCacheValid();
        return this.mViewTypeCount;
    }

    @Override
    public int getItemViewType(int i) {
        ensureCacheValid();
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (i2 < this.mCounts.length) {
            int i5 = this.mCounts[i2] + i3;
            if (i >= i3 && i < i5) {
                return i4 + this.mAdapters[i2].getItemViewType(i - i3);
            }
            i4 += this.mViewTypeCounts[i2];
            i2++;
            i3 = i5;
        }
        throw new ArrayIndexOutOfBoundsException(i);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ensureCacheValid();
        int i2 = 0;
        int i3 = 0;
        while (i2 < this.mCounts.length) {
            int i4 = this.mCounts[i2] + i3;
            if (i < i3 || i >= i4) {
                i2++;
                i3 = i4;
            } else {
                return this.mAdapters[i2].getView(i - i3, view, viewGroup);
            }
        }
        throw new ArrayIndexOutOfBoundsException(i);
    }

    @Override
    public boolean areAllItemsEnabled() {
        ensureCacheValid();
        return this.mAllItemsEnabled;
    }

    @Override
    public boolean isEnabled(int i) {
        ensureCacheValid();
        int i2 = 0;
        int i3 = 0;
        while (i2 < this.mCounts.length) {
            int i4 = this.mCounts[i2] + i3;
            if (i < i3 || i >= i4) {
                i2++;
                i3 = i4;
            } else {
                if (!this.mAdapters[i2].areAllItemsEnabled() && !this.mAdapters[i2].isEnabled(i - i3)) {
                    return false;
                }
                return true;
            }
        }
        throw new ArrayIndexOutOfBoundsException(i);
    }
}
