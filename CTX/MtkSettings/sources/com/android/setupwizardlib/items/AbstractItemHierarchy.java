package com.android.setupwizardlib.items;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.items.ItemHierarchy;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class AbstractItemHierarchy implements ItemHierarchy {
    private int mId;
    private ArrayList<ItemHierarchy.Observer> mObservers;

    public AbstractItemHierarchy() {
        this.mObservers = new ArrayList<>();
        this.mId = 0;
    }

    public AbstractItemHierarchy(Context context, AttributeSet attributeSet) {
        this.mObservers = new ArrayList<>();
        this.mId = 0;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SuwAbstractItem);
        this.mId = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwAbstractItem_android_id, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    public int getId() {
        return this.mId;
    }

    public int getViewId() {
        return getId();
    }

    @Override
    public void registerObserver(ItemHierarchy.Observer observer) {
        this.mObservers.add(observer);
    }

    public void notifyItemRangeChanged(int i, int i2) {
        if (i < 0) {
            Log.w("AbstractItemHierarchy", "notifyItemRangeChanged: Invalid position=" + i);
            return;
        }
        if (i2 < 0) {
            Log.w("AbstractItemHierarchy", "notifyItemRangeChanged: Invalid itemCount=" + i2);
            return;
        }
        Iterator<ItemHierarchy.Observer> it = this.mObservers.iterator();
        while (it.hasNext()) {
            it.next().onItemRangeChanged(this, i, i2);
        }
    }

    public void notifyItemRangeInserted(int i, int i2) {
        if (i < 0) {
            Log.w("AbstractItemHierarchy", "notifyItemRangeInserted: Invalid position=" + i);
            return;
        }
        if (i2 < 0) {
            Log.w("AbstractItemHierarchy", "notifyItemRangeInserted: Invalid itemCount=" + i2);
            return;
        }
        Iterator<ItemHierarchy.Observer> it = this.mObservers.iterator();
        while (it.hasNext()) {
            it.next().onItemRangeInserted(this, i, i2);
        }
    }
}
