package com.android.setupwizardlib.items;

import android.content.Context;
import android.util.AttributeSet;

/* loaded from: classes.dex */
public abstract class AbstractItem extends AbstractItemHierarchy implements IItem {
    public AbstractItem() {
    }

    public AbstractItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // com.android.setupwizardlib.items.ItemHierarchy
    public int getCount() {
        return 1;
    }

    @Override // com.android.setupwizardlib.items.ItemHierarchy
    public IItem getItemAt(int i) {
        return this;
    }

    public void notifyItemChanged() {
        notifyItemRangeChanged(0, 1);
    }
}
