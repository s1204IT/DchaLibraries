package com.android.setupwizardlib.items;

import android.content.Context;
import android.util.AttributeSet;

public abstract class AbstractItem extends AbstractItemHierarchy implements IItem {
    public AbstractItem() {
    }

    public AbstractItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public IItem getItemAt(int i) {
        return this;
    }

    public void notifyItemChanged() {
        notifyItemRangeChanged(0, 1);
    }
}
