package com.android.setupwizardlib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class BottomScrollView extends ScrollView {
    private final Runnable mCheckScrollRunnable;
    private BottomScrollListener mListener;
    private boolean mRequiringScroll;
    private int mScrollThreshold;

    public interface BottomScrollListener {
        void onRequiresScroll();

        void onScrolledToBottom();
    }

    public BottomScrollView(Context context) {
        super(context);
        this.mRequiringScroll = false;
        this.mCheckScrollRunnable = new Runnable() {
            @Override
            public void run() {
                BottomScrollView.this.checkScroll();
            }
        };
    }

    public BottomScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRequiringScroll = false;
        this.mCheckScrollRunnable = new Runnable() {
            @Override
            public void run() {
                BottomScrollView.this.checkScroll();
            }
        };
    }

    public BottomScrollView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mRequiringScroll = false;
        this.mCheckScrollRunnable = new Runnable() {
            @Override
            public void run() {
                BottomScrollView.this.checkScroll();
            }
        };
    }

    public int getScrollThreshold() {
        return this.mScrollThreshold;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        View childAt = getChildAt(0);
        if (childAt != null) {
            this.mScrollThreshold = Math.max(0, ((childAt.getMeasuredHeight() - i4) + i2) - getPaddingBottom());
        }
        if (i4 - i2 > 0) {
            post(this.mCheckScrollRunnable);
        }
    }

    @Override
    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        if (i4 != i2) {
            checkScroll();
        }
    }

    private void checkScroll() {
        if (this.mListener != null) {
            if (getScrollY() >= this.mScrollThreshold) {
                this.mListener.onScrolledToBottom();
            } else if (!this.mRequiringScroll) {
                this.mRequiringScroll = true;
                this.mListener.onRequiresScroll();
            }
        }
    }
}
