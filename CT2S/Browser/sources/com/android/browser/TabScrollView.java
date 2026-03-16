package com.android.browser;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import com.android.browser.TabBar;

public class TabScrollView extends HorizontalScrollView {
    private int mAnimationDuration;
    private LinearLayout mContentView;
    private int mSelected;
    private int mTabOverlap;

    public TabScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public TabScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TabScrollView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context ctx) {
        this.mAnimationDuration = ctx.getResources().getInteger(R.integer.tab_animation_duration);
        this.mTabOverlap = (int) ctx.getResources().getDimension(R.dimen.tab_overlap);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(2);
        this.mContentView = new TabLayout(ctx);
        this.mContentView.setOrientation(0);
        this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-2, -1));
        this.mContentView.setPadding((int) ctx.getResources().getDimension(R.dimen.tab_first_padding_left), 0, 0, 0);
        addView(this.mContentView);
        this.mSelected = -1;
        setScroll(getScroll());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        ensureChildVisible(getSelectedTab());
    }

    protected void updateLayout() {
        int count = this.mContentView.getChildCount();
        for (int i = 0; i < count; i++) {
            TabBar.TabView tv = (TabBar.TabView) this.mContentView.getChildAt(i);
            tv.updateLayoutParams();
        }
        ensureChildVisible(getSelectedTab());
    }

    void setSelectedTab(int position) {
        View v = getSelectedTab();
        if (v != null) {
            v.setActivated(false);
        }
        this.mSelected = position;
        View v2 = getSelectedTab();
        if (v2 != null) {
            v2.setActivated(true);
        }
        requestLayout();
    }

    int getChildIndex(View v) {
        return this.mContentView.indexOfChild(v);
    }

    View getSelectedTab() {
        if (this.mSelected < 0 || this.mSelected >= this.mContentView.getChildCount()) {
            return null;
        }
        return this.mContentView.getChildAt(this.mSelected);
    }

    void clearTabs() {
        this.mContentView.removeAllViews();
    }

    void addTab(View tab) {
        this.mContentView.addView(tab);
        tab.setActivated(false);
    }

    void removeTab(View tab) {
        int ix = this.mContentView.indexOfChild(tab);
        if (ix == this.mSelected) {
            this.mSelected = -1;
        } else if (ix < this.mSelected) {
            this.mSelected--;
        }
        this.mContentView.removeView(tab);
    }

    private void ensureChildVisible(View child) {
        if (child != null) {
            int childl = child.getLeft();
            int childr = childl + child.getWidth();
            int viewl = getScrollX();
            int viewr = viewl + getWidth();
            if (childl < viewl) {
                animateScroll(childl);
            } else if (childr > viewr) {
                animateScroll((childr - viewr) + viewl);
            }
        }
    }

    private void animateScroll(int newscroll) {
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "scroll", getScrollX(), newscroll);
        animator.setDuration(this.mAnimationDuration);
        animator.start();
    }

    public void setScroll(int newscroll) {
        scrollTo(newscroll, getScrollY());
    }

    public int getScroll() {
        return getScrollX();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (isHardwareAccelerated()) {
            int count = this.mContentView.getChildCount();
            for (int i = 0; i < count; i++) {
                this.mContentView.getChildAt(i).invalidate();
            }
        }
    }

    class TabLayout extends LinearLayout {
        public TabLayout(Context context) {
            super(context);
            setChildrenDrawingOrderEnabled(true);
        }

        @Override
        protected void onMeasure(int hspec, int vspec) {
            super.onMeasure(hspec, vspec);
            int w = getMeasuredWidth();
            setMeasuredDimension(w - (Math.max(0, TabScrollView.this.mContentView.getChildCount() - 1) * TabScrollView.this.mTabOverlap), getMeasuredHeight());
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (getChildCount() > 1) {
                int nextLeft = getChildAt(0).getRight() - TabScrollView.this.mTabOverlap;
                for (int i = 1; i < getChildCount(); i++) {
                    View tab = getChildAt(i);
                    int w = tab.getRight() - tab.getLeft();
                    tab.layout(nextLeft, tab.getTop(), nextLeft + w, tab.getBottom());
                    nextLeft += w - TabScrollView.this.mTabOverlap;
                }
            }
        }

        @Override
        protected int getChildDrawingOrder(int count, int i) {
            if (i == count - 1 && TabScrollView.this.mSelected >= 0 && TabScrollView.this.mSelected < count) {
                return TabScrollView.this.mSelected;
            }
            int next = (count - i) - 1;
            if (next <= TabScrollView.this.mSelected && next > 0) {
                return next - 1;
            }
            return next;
        }
    }
}
