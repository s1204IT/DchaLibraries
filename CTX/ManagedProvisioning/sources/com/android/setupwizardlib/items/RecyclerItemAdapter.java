package com.android.setupwizardlib.items;

import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.setupwizardlib.R;

public class RecyclerItemAdapter extends RecyclerView.Adapter<ItemViewHolder> {
    private final ItemHierarchy mItemHierarchy;
    private OnItemSelectedListener mListener;

    public interface OnItemSelectedListener {
        void onItemSelected(IItem iItem);
    }

    public IItem getItem(int i) {
        return this.mItemHierarchy.getItemAt(i);
    }

    @Override
    public long getItemId(int i) {
        int id;
        IItem item = getItem(i);
        if (!(item instanceof AbstractItem) || (id = ((AbstractItem) item).getId()) <= 0) {
            return -1L;
        }
        return id;
    }

    @Override
    public int getItemCount() {
        return this.mItemHierarchy.getCount();
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View viewInflate = LayoutInflater.from(viewGroup.getContext()).inflate(i, viewGroup, false);
        final ItemViewHolder itemViewHolder = new ItemViewHolder(viewInflate);
        if (!"noBackground".equals(viewInflate.getTag())) {
            TypedArray typedArrayObtainStyledAttributes = viewGroup.getContext().obtainStyledAttributes(R.styleable.SuwRecyclerItemAdapter);
            Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(R.styleable.SuwRecyclerItemAdapter_android_selectableItemBackground);
            if (drawable == null) {
                drawable = typedArrayObtainStyledAttributes.getDrawable(R.styleable.SuwRecyclerItemAdapter_selectableItemBackground);
            }
            Drawable background = viewInflate.getBackground();
            if (background == null) {
                background = typedArrayObtainStyledAttributes.getDrawable(R.styleable.SuwRecyclerItemAdapter_android_colorBackground);
            }
            if (drawable == null || background == null) {
                Log.e("RecyclerItemAdapter", "Cannot resolve required attributes. selectableItemBackground=" + drawable + " background=" + background);
            } else {
                viewInflate.setBackgroundDrawable(new PatchedLayerDrawable(new Drawable[]{background, drawable}));
            }
            typedArrayObtainStyledAttributes.recycle();
        }
        viewInflate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IItem item = itemViewHolder.getItem();
                if (RecyclerItemAdapter.this.mListener != null && item != null && item.isEnabled()) {
                    RecyclerItemAdapter.this.mListener.onItemSelected(item);
                }
            }
        });
        return itemViewHolder;
    }

    @Override
    public void onBindViewHolder(ItemViewHolder itemViewHolder, int i) {
        IItem item = getItem(i);
        itemViewHolder.setEnabled(item.isEnabled());
        itemViewHolder.setItem(item);
        item.onBindView(itemViewHolder.itemView);
    }

    @Override
    public int getItemViewType(int i) {
        return getItem(i).getLayoutResource();
    }

    static class PatchedLayerDrawable extends LayerDrawable {
        PatchedLayerDrawable(Drawable[] drawableArr) {
            super(drawableArr);
        }

        @Override
        public boolean getPadding(Rect rect) {
            return super.getPadding(rect) && !(rect.left == 0 && rect.top == 0 && rect.right == 0 && rect.bottom == 0);
        }
    }
}
