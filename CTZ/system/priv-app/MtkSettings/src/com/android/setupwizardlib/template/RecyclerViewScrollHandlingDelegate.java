package com.android.setupwizardlib.template;

import android.support.v7.widget.RecyclerView;
import com.android.setupwizardlib.template.RequireScrollMixin;

/* loaded from: classes.dex */
public class RecyclerViewScrollHandlingDelegate implements RequireScrollMixin.ScrollHandlingDelegate {
    private final RecyclerView mRecyclerView;
    private final RequireScrollMixin mRequireScrollMixin;

    public RecyclerViewScrollHandlingDelegate(RequireScrollMixin requireScrollMixin, RecyclerView recyclerView) {
        this.mRequireScrollMixin = requireScrollMixin;
        this.mRecyclerView = recyclerView;
    }
}
