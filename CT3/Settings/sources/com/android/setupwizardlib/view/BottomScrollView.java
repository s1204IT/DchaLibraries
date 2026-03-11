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

    public BottomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRequiringScroll = false;
        this.mCheckScrollRunnable = new Runnable() {
            @Override
            public void run() {
                BottomScrollView.this.checkScroll();
            }
        };
    }

    public BottomScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        View child = getChildAt(0);
        if (child != null) {
            this.mScrollThreshold = Math.max(0, ((child.getMeasuredHeight() - b) + t) - getPaddingBottom());
        }
        if (b - t <= 0) {
            return;
        }
        post(this.mCheckScrollRunnable);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (oldt == t) {
            return;
        }
        checkScroll();
    }

    public void checkScroll() {
        if (this.mListener == null) {
            return;
        }
        if (getScrollY() >= this.mScrollThreshold) {
            this.mListener.onScrolledToBottom();
        } else {
            if (this.mRequiringScroll) {
                return;
            }
            this.mRequiringScroll = true;
            this.mListener.onRequiresScroll();
        }
    }
}
