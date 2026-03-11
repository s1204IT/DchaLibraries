package com.android.setupwizardlib.items;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.setupwizardlib.R$styleable;
import com.android.setupwizardlib.items.ItemHierarchy;

public class RecyclerItemAdapter extends RecyclerView.Adapter<ItemViewHolder> implements ItemHierarchy.Observer {
    private final ItemHierarchy mItemHierarchy;
    private OnItemSelectedListener mListener;

    public interface OnItemSelectedListener {
        void onItemSelected(IItem iItem);
    }

    public RecyclerItemAdapter(ItemHierarchy hierarchy) {
        this.mItemHierarchy = hierarchy;
        this.mItemHierarchy.registerObserver(this);
    }

    public IItem getItem(int position) {
        return this.mItemHierarchy.getItemAt(position);
    }

    @Override
    public long getItemId(int position) {
        int id;
        IItem mItem = getItem(position);
        if (!(mItem instanceof AbstractItem) || (id = ((AbstractItem) mItem).getId()) <= 0) {
            return -1L;
        }
        return id;
    }

    @Override
    public int getItemCount() {
        return this.mItemHierarchy.getCount();
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(viewType, parent, false);
        final ItemViewHolder viewHolder = new ItemViewHolder(view);
        TypedArray typedArray = parent.getContext().obtainStyledAttributes(R$styleable.SuwRecyclerItemAdapter);
        Drawable selectableItemBackground = typedArray.getDrawable(R$styleable.SuwRecyclerItemAdapter_android_selectableItemBackground);
        if (selectableItemBackground == null) {
            selectableItemBackground = typedArray.getDrawable(R$styleable.SuwRecyclerItemAdapter_selectableItemBackground);
        }
        Drawable background = typedArray.getDrawable(R$styleable.SuwRecyclerItemAdapter_android_colorBackground);
        if (selectableItemBackground == null || background == null) {
            Log.e("RecyclerItemAdapter", "Cannot resolve required attributes. selectableItemBackground=" + selectableItemBackground + " background=" + background);
        } else {
            Drawable[] layers = {background, selectableItemBackground};
            view.setBackgroundDrawable(new LayerDrawable(layers));
        }
        typedArray.recycle();
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                IItem item = viewHolder.getItem();
                if (RecyclerItemAdapter.this.mListener == null || item == null || !item.isEnabled()) {
                    return;
                }
                RecyclerItemAdapter.this.mListener.onItemSelected(item);
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        IItem item = getItem(position);
        item.onBindView(holder.itemView);
        holder.setEnabled(item.isEnabled());
        holder.setItem(item);
    }

    @Override
    public int getItemViewType(int position) {
        IItem item = getItem(position);
        return item.getLayoutResource();
    }

    @Override
    public void onChanged(ItemHierarchy hierarchy) {
        notifyDataSetChanged();
    }

    public ItemHierarchy findItemById(int id) {
        return this.mItemHierarchy.findItemById(id);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.mListener = listener;
    }
}
