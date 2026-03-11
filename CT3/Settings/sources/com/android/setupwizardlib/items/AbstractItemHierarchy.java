package com.android.setupwizardlib.items;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import com.android.setupwizardlib.R$styleable;
import com.android.setupwizardlib.items.ItemHierarchy;
import java.util.ArrayList;

public abstract class AbstractItemHierarchy implements ItemHierarchy {
    private int mId;
    private ArrayList<ItemHierarchy.Observer> mObservers;

    public AbstractItemHierarchy() {
        this.mObservers = new ArrayList<>();
        this.mId = 0;
    }

    public AbstractItemHierarchy(Context context, AttributeSet attrs) {
        this.mObservers = new ArrayList<>();
        this.mId = 0;
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.SuwAbstractItem);
        this.mId = a.getResourceId(R$styleable.SuwAbstractItem_android_id, 0);
        a.recycle();
    }

    public int getId() {
        return this.mId;
    }

    @Override
    public void registerObserver(ItemHierarchy.Observer observer) {
        this.mObservers.add(observer);
    }

    public void notifyChanged() {
        for (ItemHierarchy.Observer observer : this.mObservers) {
            observer.onChanged(this);
        }
    }
}
