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

    public TabScrollView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    public TabScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public TabScrollView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        this.mAnimationDuration = context.getResources().getInteger(R.integer.tab_animation_duration);
        this.mTabOverlap = (int) context.getResources().getDimension(R.dimen.tab_overlap);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(2);
        this.mContentView = new TabLayout(context);
        this.mContentView.setOrientation(0);
        this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-2, -1));
        this.mContentView.setPadding((int) context.getResources().getDimension(R.dimen.tab_first_padding_left), 0, 0, 0);
        addView(this.mContentView);
        this.mSelected = -1;
        setScroll(getScroll());
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        ensureChildVisible(getSelectedTab());
    }

    protected void updateLayout() {
        int childCount = this.mContentView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((TabBar.TabView) this.mContentView.getChildAt(i)).updateLayoutParams();
        }
        ensureChildVisible(getSelectedTab());
    }

    void setSelectedTab(int i) {
        View selectedTab = getSelectedTab();
        if (selectedTab != null) {
            selectedTab.setActivated(false);
        }
        this.mSelected = i;
        View selectedTab2 = getSelectedTab();
        if (selectedTab2 != null) {
            selectedTab2.setActivated(true);
        }
        requestLayout();
    }

    int getChildIndex(View view) {
        return this.mContentView.indexOfChild(view);
    }

    View getSelectedTab() {
        if (this.mSelected >= 0 && this.mSelected < this.mContentView.getChildCount()) {
            return this.mContentView.getChildAt(this.mSelected);
        }
        return null;
    }

    void clearTabs() {
        this.mContentView.removeAllViews();
    }

    void addTab(View view) {
        this.mContentView.addView(view);
        view.setActivated(false);
    }

    void removeTab(View view) {
        int iIndexOfChild = this.mContentView.indexOfChild(view);
        if (iIndexOfChild == this.mSelected) {
            this.mSelected = -1;
        } else if (iIndexOfChild < this.mSelected) {
            this.mSelected--;
        }
        this.mContentView.removeView(view);
    }

    private void ensureChildVisible(View view) {
        if (view != null) {
            int left = view.getLeft();
            int width = view.getWidth() + left;
            int scrollX = getScrollX();
            int width2 = getWidth() + scrollX;
            if (left < scrollX) {
                animateScroll(left);
            } else if (width > width2) {
                animateScroll((width - width2) + scrollX);
            }
        }
    }

    private void animateScroll(int i) {
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "scroll", getScrollX(), i);
        objectAnimatorOfInt.setDuration(this.mAnimationDuration);
        objectAnimatorOfInt.start();
    }

    public void setScroll(int i) {
        scrollTo(i, getScrollY());
    }

    public int getScroll() {
        return getScrollX();
    }

    @Override
    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        if (isHardwareAccelerated()) {
            int childCount = this.mContentView.getChildCount();
            for (int i5 = 0; i5 < childCount; i5++) {
                this.mContentView.getChildAt(i5).invalidate();
            }
        }
    }

    class TabLayout extends LinearLayout {
        public TabLayout(Context context) {
            super(context);
            setChildrenDrawingOrderEnabled(true);
        }

        @Override
        protected void onMeasure(int i, int i2) {
            super.onMeasure(i, i2);
            setMeasuredDimension(getMeasuredWidth() - (Math.max(0, TabScrollView.this.mContentView.getChildCount() - 1) * TabScrollView.this.mTabOverlap), getMeasuredHeight());
        }

        @Override
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            int i5;
            super.onLayout(z, i, i2, i3, i4);
            if (getChildCount() > 1) {
                int right = getChildAt(0).getRight() - TabScrollView.this.mTabOverlap;
                int layoutDirection = getResources().getConfiguration().getLayoutDirection();
                if (layoutDirection == 1) {
                    right = TabScrollView.this.mTabOverlap + getChildAt(0).getLeft();
                }
                for (int i6 = 1; i6 < getChildCount(); i6++) {
                    View childAt = getChildAt(i6);
                    int right2 = childAt.getRight() - childAt.getLeft();
                    if (layoutDirection == 1) {
                        int i7 = right - right2;
                        childAt.layout(i7, childAt.getTop(), right, childAt.getBottom());
                        i5 = i7 + TabScrollView.this.mTabOverlap;
                    } else {
                        int i8 = right2 + right;
                        childAt.layout(right, childAt.getTop(), i8, childAt.getBottom());
                        i5 = i8 - TabScrollView.this.mTabOverlap;
                    }
                    right = i5;
                }
            }
        }

        @Override
        protected int getChildDrawingOrder(int i, int i2) {
            if (i2 == i - 1 && TabScrollView.this.mSelected >= 0 && TabScrollView.this.mSelected < i) {
                return TabScrollView.this.mSelected;
            }
            int i3 = (i - i2) - 1;
            if (i3 <= TabScrollView.this.mSelected && i3 > 0) {
                return i3 - 1;
            }
            return i3;
        }
    }
}
