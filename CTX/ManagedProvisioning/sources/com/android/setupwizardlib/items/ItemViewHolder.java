package com.android.setupwizardlib.items;

import android.support.v7.widget.RecyclerView;
import android.view.View;

class ItemViewHolder extends RecyclerView.ViewHolder {
    private boolean mIsEnabled;
    private IItem mItem;

    ItemViewHolder(View view) {
        super(view);
    }

    public void setEnabled(boolean z) {
        this.mIsEnabled = z;
        this.itemView.setClickable(z);
        this.itemView.setEnabled(z);
        this.itemView.setFocusable(z);
    }

    public void setItem(IItem iItem) {
        this.mItem = iItem;
    }

    public IItem getItem() {
        return this.mItem;
    }
}
