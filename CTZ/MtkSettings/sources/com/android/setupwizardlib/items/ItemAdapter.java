package com.android.setupwizardlib.items;

import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.setupwizardlib.items.ItemHierarchy;

public class ItemAdapter extends BaseAdapter implements ItemHierarchy.Observer {
    private final ItemHierarchy mItemHierarchy;
    private ViewTypes mViewTypes = new ViewTypes();

    public ItemAdapter(ItemHierarchy itemHierarchy) {
        this.mItemHierarchy = itemHierarchy;
        this.mItemHierarchy.registerObserver(this);
        refreshViewTypes();
    }

    @Override
    public int getCount() {
        return this.mItemHierarchy.getCount();
    }

    @Override
    public IItem getItem(int i) {
        return this.mItemHierarchy.getItemAt(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemViewType(int i) {
        return this.mViewTypes.get(getItem(i).getLayoutResource());
    }

    @Override
    public int getViewTypeCount() {
        return this.mViewTypes.size();
    }

    private void refreshViewTypes() {
        for (int i = 0; i < getCount(); i++) {
            this.mViewTypes.add(getItem(i).getLayoutResource());
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        IItem item = getItem(i);
        if (view == null) {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(item.getLayoutResource(), viewGroup, false);
        }
        item.onBindView(view);
        return view;
    }

    public void onChanged(ItemHierarchy itemHierarchy) {
        refreshViewTypes();
        notifyDataSetChanged();
    }

    @Override
    public void onItemRangeChanged(ItemHierarchy itemHierarchy, int i, int i2) {
        onChanged(itemHierarchy);
    }

    @Override
    public void onItemRangeInserted(ItemHierarchy itemHierarchy, int i, int i2) {
        onChanged(itemHierarchy);
    }

    @Override
    public boolean isEnabled(int i) {
        return getItem(i).isEnabled();
    }

    private static class ViewTypes {
        private SparseIntArray mPositionMap;
        private int nextPosition;

        private ViewTypes() {
            this.mPositionMap = new SparseIntArray();
            this.nextPosition = 0;
        }

        public int add(int i) {
            if (this.mPositionMap.indexOfKey(i) < 0) {
                this.mPositionMap.put(i, this.nextPosition);
                this.nextPosition++;
            }
            return this.mPositionMap.get(i);
        }

        public int size() {
            return this.mPositionMap.size();
        }

        public int get(int i) {
            return this.mPositionMap.get(i);
        }
    }
}
