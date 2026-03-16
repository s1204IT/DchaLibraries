package com.android.contacts.common.list;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class AutoScrollListView extends ListView {
    private int mRequestedScrollPosition;
    private boolean mSmoothScrollRequested;

    public AutoScrollListView(Context context) {
        super(context);
        this.mRequestedScrollPosition = -1;
    }

    public AutoScrollListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRequestedScrollPosition = -1;
    }

    public AutoScrollListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mRequestedScrollPosition = -1;
    }

    public void requestPositionToScreen(int position, boolean smoothScroll) {
        this.mRequestedScrollPosition = position;
        this.mSmoothScrollRequested = smoothScroll;
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (this.mRequestedScrollPosition != -1) {
            int position = this.mRequestedScrollPosition;
            this.mRequestedScrollPosition = -1;
            int firstPosition = getFirstVisiblePosition() + 1;
            int lastPosition = getLastVisiblePosition();
            if (position < firstPosition || position > lastPosition) {
                int offset = (int) (getHeight() * 0.33f);
                if (!this.mSmoothScrollRequested) {
                    setSelectionFromTop(position, offset);
                    super.layoutChildren();
                    return;
                }
                int twoScreens = (lastPosition - firstPosition) * 2;
                if (position < firstPosition) {
                    int preliminaryPosition = position + twoScreens;
                    if (preliminaryPosition >= getCount()) {
                        preliminaryPosition = getCount() - 1;
                    }
                    if (preliminaryPosition < firstPosition) {
                        setSelection(preliminaryPosition);
                        super.layoutChildren();
                    }
                } else {
                    int preliminaryPosition2 = position - twoScreens;
                    if (preliminaryPosition2 < 0) {
                        preliminaryPosition2 = 0;
                    }
                    if (preliminaryPosition2 > lastPosition) {
                        setSelection(preliminaryPosition2);
                        super.layoutChildren();
                    }
                }
                smoothScrollToPositionFromTop(position, offset);
            }
        }
    }
}
