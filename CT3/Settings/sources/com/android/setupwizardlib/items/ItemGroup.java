package com.android.setupwizardlib.items;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import com.android.setupwizardlib.items.ItemHierarchy;
import com.android.setupwizardlib.items.ItemInflater;
import java.util.ArrayList;
import java.util.List;

public class ItemGroup extends AbstractItemHierarchy implements ItemInflater.ItemParent, ItemHierarchy.Observer {
    private List<ItemHierarchy> mChildren;
    private int mCount;
    private boolean mDirty;
    private SparseIntArray mHierarchyStart;

    private static int binarySearch(SparseIntArray array, int value) {
        int size = array.size();
        int lo = 0;
        int hi = size - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int midVal = array.valueAt(mid);
            if (midVal < value) {
                lo = mid + 1;
            } else if (midVal > value) {
                hi = mid - 1;
            } else {
                return array.keyAt(mid);
            }
        }
        return array.keyAt(lo - 1);
    }

    public ItemGroup() {
        this.mChildren = new ArrayList();
        this.mHierarchyStart = new SparseIntArray();
        this.mCount = 0;
        this.mDirty = false;
    }

    public ItemGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mChildren = new ArrayList();
        this.mHierarchyStart = new SparseIntArray();
        this.mCount = 0;
        this.mDirty = false;
    }

    @Override
    public void addChild(ItemHierarchy child) {
        this.mChildren.add(child);
        child.registerObserver(this);
        onHierarchyChanged();
    }

    @Override
    public int getCount() {
        updateDataIfNeeded();
        return this.mCount;
    }

    @Override
    public IItem getItemAt(int position) {
        int itemIndex = getItemIndex(position);
        ItemHierarchy item = this.mChildren.get(itemIndex);
        int subpos = position - this.mHierarchyStart.get(itemIndex);
        return item.getItemAt(subpos);
    }

    @Override
    public void onChanged(ItemHierarchy hierarchy) {
        this.mDirty = true;
        notifyChanged();
    }

    private void onHierarchyChanged() {
        onChanged(null);
    }

    @Override
    public ItemHierarchy findItemById(int id) {
        if (id == getId()) {
            return this;
        }
        for (ItemHierarchy child : this.mChildren) {
            ItemHierarchy childFindItem = child.findItemById(id);
            if (childFindItem != null) {
                return childFindItem;
            }
        }
        return null;
    }

    private void updateDataIfNeeded() {
        if (!this.mDirty) {
            return;
        }
        this.mCount = 0;
        this.mHierarchyStart.clear();
        for (int itemIndex = 0; itemIndex < this.mChildren.size(); itemIndex++) {
            ItemHierarchy item = this.mChildren.get(itemIndex);
            if (item.getCount() > 0) {
                this.mHierarchyStart.put(itemIndex, this.mCount);
            }
            this.mCount += item.getCount();
        }
        this.mDirty = false;
    }

    private int getItemIndex(int position) {
        updateDataIfNeeded();
        if (position < 0 || position >= this.mCount) {
            throw new IndexOutOfBoundsException("size=" + this.mCount + "; index=" + position);
        }
        int result = binarySearch(this.mHierarchyStart, position);
        if (result < 0) {
            throw new IllegalStateException("Cannot have item start index < 0");
        }
        return result;
    }
}
