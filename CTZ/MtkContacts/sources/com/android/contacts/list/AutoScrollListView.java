package com.android.contacts.list;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ListView;

public class AutoScrollListView extends ListView {
    private int mRequestedScrollPosition;
    private boolean mSmoothScrollRequested;

    public AutoScrollListView(Context context) {
        super(context);
        this.mRequestedScrollPosition = -1;
    }

    public AutoScrollListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRequestedScrollPosition = -1;
    }

    public AutoScrollListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mRequestedScrollPosition = -1;
    }

    public void requestPositionToScreen(int i, boolean z) {
        this.mRequestedScrollPosition = i;
        this.mSmoothScrollRequested = z;
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (this.mRequestedScrollPosition == -1) {
            return;
        }
        int i = this.mRequestedScrollPosition;
        this.mRequestedScrollPosition = -1;
        int firstVisiblePosition = getFirstVisiblePosition() + 1;
        int lastVisiblePosition = getLastVisiblePosition();
        if (i >= firstVisiblePosition && i <= lastVisiblePosition) {
            return;
        }
        int height = (int) (getHeight() * 0.33f);
        if (!this.mSmoothScrollRequested) {
            setSelectionFromTop(i, height);
            super.layoutChildren();
            return;
        }
        int i2 = (lastVisiblePosition - firstVisiblePosition) * 2;
        if (i < firstVisiblePosition) {
            int count = i2 + i;
            if (count >= getCount()) {
                count = getCount() - 1;
            }
            if (count < firstVisiblePosition) {
                setSelection(count);
                super.layoutChildren();
            }
        } else {
            int i3 = i - i2;
            if (i3 < 0) {
                i3 = 0;
            }
            if (i3 > lastVisiblePosition) {
                setSelection(i3);
                super.layoutChildren();
            }
        }
        smoothScrollToPositionFromTop(i, height);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) {
            layoutChildren();
        }
    }
}
