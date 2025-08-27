package com.android.setupwizardlib.items;

import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.setupwizardlib.items.ItemHierarchy;

/* loaded from: classes.dex */
public class ItemAdapter extends BaseAdapter implements ItemHierarchy.Observer {
    private final ItemHierarchy mItemHierarchy;
    private ViewTypes mViewTypes = new ViewTypes();

    public ItemAdapter(ItemHierarchy itemHierarchy) {
        this.mItemHierarchy = itemHierarchy;
        this.mItemHierarchy.registerObserver(this);
        refreshViewTypes();
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.mItemHierarchy.getCount();
    }

    /* JADX DEBUG: Method merged with bridge method: getItem(I)Ljava/lang/Object; */
    @Override // android.widget.Adapter
    public IItem getItem(int i) {
        return this.mItemHierarchy.getItemAt(i);
    }

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return i;
    }

    @Override // android.widget.BaseAdapter, android.widget.Adapter
    public int getItemViewType(int i) {
        return this.mViewTypes.get(getItem(i).getLayoutResource());
    }

    @Override // android.widget.BaseAdapter, android.widget.Adapter
    public int getViewTypeCount() {
        return this.mViewTypes.size();
    }

    private void refreshViewTypes() {
        for (int i = 0; i < getCount(); i++) {
            this.mViewTypes.add(getItem(i).getLayoutResource());
        }
    }

    @Override // android.widget.Adapter
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

    @Override // com.android.setupwizardlib.items.ItemHierarchy.Observer
    public void onItemRangeChanged(ItemHierarchy itemHierarchy, int i, int i2) {
        onChanged(itemHierarchy);
    }

    @Override // com.android.setupwizardlib.items.ItemHierarchy.Observer
    public void onItemRangeInserted(ItemHierarchy itemHierarchy, int i, int i2) {
        onChanged(itemHierarchy);
    }

    @Override // android.widget.BaseAdapter, android.widget.ListAdapter
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
