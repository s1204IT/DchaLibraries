package com.android.setupwizardlib.template;

import android.widget.AbsListView;
import android.widget.ListView;
import com.android.setupwizardlib.template.RequireScrollMixin;

public class ListViewScrollHandlingDelegate implements AbsListView.OnScrollListener, RequireScrollMixin.ScrollHandlingDelegate {
    private final ListView mListView;
    private final RequireScrollMixin mRequireScrollMixin;

    public ListViewScrollHandlingDelegate(RequireScrollMixin requireScrollMixin, ListView listView) {
        this.mRequireScrollMixin = requireScrollMixin;
        this.mListView = listView;
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        if (i + i2 >= i3) {
            this.mRequireScrollMixin.notifyScrollabilityChange(false);
        } else {
            this.mRequireScrollMixin.notifyScrollabilityChange(true);
        }
    }
}
