package com.android.setupwizardlib.items;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.android.setupwizardlib.DividerItemDecoration;

class ItemViewHolder extends RecyclerView.ViewHolder implements DividerItemDecoration.DividedViewHolder {
    private boolean mIsEnabled;
    private IItem mItem;

    ItemViewHolder(View view) {
        super(view);
    }

    @Override
    public boolean isDividerAllowedAbove() {
        return this.mIsEnabled;
    }

    @Override
    public boolean isDividerAllowedBelow() {
        return this.mIsEnabled;
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
