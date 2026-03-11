package com.android.setupwizardlib.items;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.setupwizardlib.items.ItemHierarchy;
import com.android.setupwizardlib.items.ItemInflater;
import java.util.ArrayList;
import java.util.List;

public class ItemGroup extends AbstractItemHierarchy implements ItemHierarchy.Observer, ItemInflater.ItemParent {
    private List<ItemHierarchy> mChildren;
    private int mCount;
    private boolean mDirty;
    private SparseIntArray mHierarchyStart;

    private static int binarySearch(SparseIntArray sparseIntArray, int i) {
        int size = sparseIntArray.size() - 1;
        int i2 = 0;
        while (i2 <= size) {
            int i3 = (i2 + size) >>> 1;
            int iValueAt = sparseIntArray.valueAt(i3);
            if (iValueAt < i) {
                i2 = i3 + 1;
            } else if (iValueAt > i) {
                size = i3 - 1;
            } else {
                return sparseIntArray.keyAt(i3);
            }
        }
        return sparseIntArray.keyAt(i2 - 1);
    }

    private static <T> int identityIndexOf(List<T> list, T t) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (list.get(i) == t) {
                return i;
            }
        }
        return -1;
    }

    public ItemGroup() {
        this.mChildren = new ArrayList();
        this.mHierarchyStart = new SparseIntArray();
        this.mCount = 0;
        this.mDirty = false;
    }

    public ItemGroup(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mChildren = new ArrayList();
        this.mHierarchyStart = new SparseIntArray();
        this.mCount = 0;
        this.mDirty = false;
    }

    @Override
    public void addChild(ItemHierarchy itemHierarchy) {
        this.mDirty = true;
        this.mChildren.add(itemHierarchy);
        itemHierarchy.registerObserver(this);
        int count = itemHierarchy.getCount();
        if (count > 0) {
            notifyItemRangeInserted(getChildPosition(itemHierarchy), count);
        }
    }

    @Override
    public int getCount() {
        updateDataIfNeeded();
        return this.mCount;
    }

    @Override
    public IItem getItemAt(int i) {
        int itemIndex = getItemIndex(i);
        return this.mChildren.get(itemIndex).getItemAt(i - this.mHierarchyStart.get(itemIndex));
    }

    private int getChildPosition(ItemHierarchy itemHierarchy) {
        return getChildPosition(identityIndexOf(this.mChildren, itemHierarchy));
    }

    private int getChildPosition(int i) {
        updateDataIfNeeded();
        if (i == -1) {
            return -1;
        }
        int size = this.mChildren.size();
        int i2 = -1;
        for (int i3 = i; i2 < 0 && i3 < size; i3++) {
            i2 = this.mHierarchyStart.get(i3, -1);
        }
        if (i2 < 0) {
            return getCount();
        }
        return i2;
    }

    @Override
    public void onItemRangeChanged(ItemHierarchy itemHierarchy, int i, int i2) {
        int childPosition = getChildPosition(itemHierarchy);
        if (childPosition >= 0) {
            notifyItemRangeChanged(childPosition + i, i2);
            return;
        }
        Log.e("ItemGroup", "Unexpected child change " + itemHierarchy);
    }

    @Override
    public void onItemRangeInserted(ItemHierarchy itemHierarchy, int i, int i2) {
        this.mDirty = true;
        int childPosition = getChildPosition(itemHierarchy);
        if (childPosition >= 0) {
            notifyItemRangeInserted(childPosition + i, i2);
            return;
        }
        Log.e("ItemGroup", "Unexpected child insert " + itemHierarchy);
    }

    private void updateDataIfNeeded() {
        if (this.mDirty) {
            this.mCount = 0;
            this.mHierarchyStart.clear();
            for (int i = 0; i < this.mChildren.size(); i++) {
                ItemHierarchy itemHierarchy = this.mChildren.get(i);
                if (itemHierarchy.getCount() > 0) {
                    this.mHierarchyStart.put(i, this.mCount);
                }
                this.mCount += itemHierarchy.getCount();
            }
            this.mDirty = false;
        }
    }

    private int getItemIndex(int i) {
        updateDataIfNeeded();
        if (i < 0 || i >= this.mCount) {
            throw new IndexOutOfBoundsException("size=" + this.mCount + "; index=" + i);
        }
        int iBinarySearch = binarySearch(this.mHierarchyStart, i);
        if (iBinarySearch < 0) {
            throw new IllegalStateException("Cannot have item start index < 0");
        }
        return iBinarySearch;
    }
}
