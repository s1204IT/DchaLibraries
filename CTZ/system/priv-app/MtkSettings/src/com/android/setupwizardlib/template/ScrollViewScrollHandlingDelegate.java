package com.android.setupwizardlib.template;

import android.util.Log;
import android.widget.ScrollView;
import com.android.setupwizardlib.template.RequireScrollMixin;
import com.android.setupwizardlib.view.BottomScrollView;

/* loaded from: classes.dex */
public class ScrollViewScrollHandlingDelegate implements RequireScrollMixin.ScrollHandlingDelegate, BottomScrollView.BottomScrollListener {
    private final RequireScrollMixin mRequireScrollMixin;
    private final BottomScrollView mScrollView;

    public ScrollViewScrollHandlingDelegate(RequireScrollMixin requireScrollMixin, ScrollView scrollView) {
        this.mRequireScrollMixin = requireScrollMixin;
        if (scrollView instanceof BottomScrollView) {
            this.mScrollView = (BottomScrollView) scrollView;
            return;
        }
        Log.w("ScrollViewDelegate", "Cannot set non-BottomScrollView. Found=" + scrollView);
        this.mScrollView = null;
    }

    @Override // com.android.setupwizardlib.view.BottomScrollView.BottomScrollListener
    public void onScrolledToBottom() {
        this.mRequireScrollMixin.notifyScrollabilityChange(false);
    }

    @Override // com.android.setupwizardlib.view.BottomScrollView.BottomScrollListener
    public void onRequiresScroll() {
        this.mRequireScrollMixin.notifyScrollabilityChange(true);
    }
}
