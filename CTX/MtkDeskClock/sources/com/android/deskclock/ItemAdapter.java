package com.android.deskclock;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import com.android.deskclock.ItemAdapter.ItemHolder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ItemAdapter<T extends ItemHolder> extends RecyclerView.Adapter<ItemViewHolder> {
    private List<T> mItemHolders;
    private OnItemChangedListener mOnItemChangedListener;
    private final OnItemChangedListener mItemChangedNotifier = new OnItemChangedListener() {
        @Override
        public void onItemChanged(ItemHolder<?> itemHolder) {
            if (ItemAdapter.this.mOnItemChangedListener != null) {
                ItemAdapter.this.mOnItemChangedListener.onItemChanged(itemHolder);
            }
            int iIndexOf = ItemAdapter.this.mItemHolders.indexOf(itemHolder);
            if (iIndexOf != -1) {
                ItemAdapter.this.notifyItemChanged(iIndexOf);
            }
        }

        @Override
        public void onItemChanged(ItemHolder<?> itemHolder, Object obj) {
            if (ItemAdapter.this.mOnItemChangedListener != null) {
                ItemAdapter.this.mOnItemChangedListener.onItemChanged(itemHolder, obj);
            }
            int iIndexOf = ItemAdapter.this.mItemHolders.indexOf(itemHolder);
            if (iIndexOf != -1) {
                ItemAdapter.this.notifyItemChanged(iIndexOf, obj);
            }
        }
    };
    private final OnItemClickedListener mOnItemClickedListener = new OnItemClickedListener() {
        @Override
        public void onItemClicked(ItemViewHolder<?> itemViewHolder, int i) {
            OnItemClickedListener onItemClickedListener = (OnItemClickedListener) ItemAdapter.this.mListenersByViewType.get(itemViewHolder.getItemViewType());
            if (onItemClickedListener != null) {
                onItemClickedListener.onItemClicked(itemViewHolder, i);
            }
        }
    };
    private final SparseArray<ItemViewHolder.Factory> mFactoriesByViewType = new SparseArray<>();
    private final SparseArray<OnItemClickedListener> mListenersByViewType = new SparseArray<>();

    public interface OnItemChangedListener {
        void onItemChanged(ItemHolder<?> itemHolder);

        void onItemChanged(ItemHolder<?> itemHolder, Object obj);
    }

    public interface OnItemClickedListener {
        void onItemClicked(ItemViewHolder<?> itemViewHolder, int i);
    }

    public ItemAdapter setHasStableIds() {
        setHasStableIds(true);
        return this;
    }

    public ItemAdapter withViewTypes(ItemViewHolder.Factory factory, OnItemClickedListener onItemClickedListener, int... iArr) {
        for (int i : iArr) {
            this.mFactoriesByViewType.put(i, factory);
            this.mListenersByViewType.put(i, onItemClickedListener);
        }
        return this;
    }

    public final List<T> getItems() {
        return this.mItemHolders;
    }

    public ItemAdapter setItems(List<T> list) {
        List<T> list2 = this.mItemHolders;
        if (list2 != list) {
            if (list2 != null) {
                Iterator<T> it = list2.iterator();
                while (it.hasNext()) {
                    it.next().removeOnItemChangedListener(this.mItemChangedNotifier);
                }
            }
            if (list2 != null && list != null && hasStableIds()) {
                Bundle bundle = new Bundle();
                for (T t : list) {
                    Iterator<T> it2 = list2.iterator();
                    while (true) {
                        if (it2.hasNext()) {
                            T next = it2.next();
                            if (t.itemId == next.itemId && t != next) {
                                bundle.clear();
                                next.onSaveInstanceState(bundle);
                                t.onRestoreInstanceState(bundle);
                                break;
                            }
                        }
                    }
                }
            }
            if (list != null) {
                Iterator<T> it3 = list.iterator();
                while (it3.hasNext()) {
                    it3.next().addOnItemChangedListener(this.mItemChangedNotifier);
                }
            }
            this.mItemHolders = list;
            notifyDataSetChanged();
        }
        return this;
    }

    public ItemAdapter addItem(int i, @NonNull T t) {
        t.addOnItemChangedListener(this.mItemChangedNotifier);
        int iMin = Math.min(i, this.mItemHolders.size());
        this.mItemHolders.add(iMin, t);
        notifyItemInserted(iMin);
        return this;
    }

    public ItemAdapter removeItem(@NonNull T t) {
        int iIndexOf = this.mItemHolders.indexOf(t);
        if (iIndexOf >= 0) {
            this.mItemHolders.remove(iIndexOf).removeOnItemChangedListener(this.mItemChangedNotifier);
            notifyItemRemoved(iIndexOf);
        }
        return this;
    }

    public void setOnItemChangedListener(OnItemChangedListener onItemChangedListener) {
        this.mOnItemChangedListener = onItemChangedListener;
    }

    @Override
    public int getItemCount() {
        if (this.mItemHolders == null) {
            return 0;
        }
        return this.mItemHolders.size();
    }

    @Override
    public long getItemId(int i) {
        if (hasStableIds()) {
            return this.mItemHolders.get(i).itemId;
        }
        return -1L;
    }

    public T findItemById(long j) {
        for (T t : this.mItemHolders) {
            if (t.itemId == j) {
                return t;
            }
        }
        return null;
    }

    @Override
    public int getItemViewType(int i) {
        return this.mItemHolders.get(i).getItemViewType();
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        ItemViewHolder.Factory factory = this.mFactoriesByViewType.get(i);
        if (factory != null) {
            return factory.createViewHolder(viewGroup, i);
        }
        throw new IllegalArgumentException("Unsupported view type: " + i);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder itemViewHolder, int i) {
        itemViewHolder.bindItemView(this.mItemHolders.get(i));
        itemViewHolder.setOnItemClickedListener(this.mOnItemClickedListener);
    }

    @Override
    public void onViewRecycled(ItemViewHolder itemViewHolder) {
        itemViewHolder.setOnItemClickedListener(null);
        itemViewHolder.recycleItemView();
    }

    public static abstract class ItemHolder<T> {
        public final T item;
        public final long itemId;
        private final List<OnItemChangedListener> mOnItemChangedListeners = new ArrayList();

        public abstract int getItemViewType();

        public ItemHolder(T t, long j) {
            this.item = t;
            this.itemId = j;
        }

        public final void addOnItemChangedListener(OnItemChangedListener onItemChangedListener) {
            if (!this.mOnItemChangedListeners.contains(onItemChangedListener)) {
                this.mOnItemChangedListeners.add(onItemChangedListener);
            }
        }

        public final void removeOnItemChangedListener(OnItemChangedListener onItemChangedListener) {
            this.mOnItemChangedListeners.remove(onItemChangedListener);
        }

        public final void notifyItemChanged() {
            Iterator<OnItemChangedListener> it = this.mOnItemChangedListeners.iterator();
            while (it.hasNext()) {
                it.next().onItemChanged(this);
            }
        }

        public final void notifyItemChanged(Object obj) {
            Iterator<OnItemChangedListener> it = this.mOnItemChangedListeners.iterator();
            while (it.hasNext()) {
                it.next().onItemChanged(this, obj);
            }
        }

        public void onSaveInstanceState(Bundle bundle) {
        }

        public void onRestoreInstanceState(Bundle bundle) {
        }
    }

    public static class ItemViewHolder<T extends ItemHolder> extends RecyclerView.ViewHolder {
        private T mItemHolder;
        private OnItemClickedListener mOnItemClickedListener;

        public interface Factory {
            ItemViewHolder<?> createViewHolder(ViewGroup viewGroup, int i);
        }

        public ItemViewHolder(View view) {
            super(view);
        }

        public final T getItemHolder() {
            return this.mItemHolder;
        }

        public final void bindItemView(T t) {
            this.mItemHolder = t;
            onBindItemView(t);
        }

        protected void onBindItemView(T t) {
        }

        public final void recycleItemView() {
            this.mItemHolder = null;
            this.mOnItemClickedListener = null;
            onRecycleItemView();
        }

        protected void onRecycleItemView() {
        }

        public final void setOnItemClickedListener(OnItemClickedListener onItemClickedListener) {
            this.mOnItemClickedListener = onItemClickedListener;
        }

        public final void notifyItemClicked(int i) {
            if (this.mOnItemClickedListener != null) {
                this.mOnItemClickedListener.onItemClicked(this, i);
            }
        }
    }
}
