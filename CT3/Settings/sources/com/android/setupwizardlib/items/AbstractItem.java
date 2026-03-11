package com.android.setupwizardlib.items;

import android.content.Context;
import android.util.AttributeSet;

public abstract class AbstractItem extends AbstractItemHierarchy implements IItem {
    public AbstractItem() {
    }

    public AbstractItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public IItem getItemAt(int position) {
        return this;
    }

    @Override
    public ItemHierarchy findItemById(int id) {
        if (id == getId()) {
            return this;
        }
        return null;
    }
}
