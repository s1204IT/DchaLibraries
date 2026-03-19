package com.android.setupwizardlib.items;

import java.util.ArrayList;

public abstract class AbstractItemHierarchy implements ItemHierarchy {
    private ArrayList<Object> mObservers = new ArrayList<>();
    private int mId = 0;

    public int getId() {
        return this.mId;
    }
}
