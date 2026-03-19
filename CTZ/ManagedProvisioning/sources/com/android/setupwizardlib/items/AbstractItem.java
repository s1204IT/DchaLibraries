package com.android.setupwizardlib.items;

public abstract class AbstractItem extends AbstractItemHierarchy implements IItem {
    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public IItem getItemAt(int i) {
        return this;
    }
}
