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

    class TabLayout extends LinearLayout {
        final TabScrollView this$0;

        public TabLayout(TabScrollView tabScrollView, Context context) {
            super(context);
            this.this$0 = tabScrollView;
            setChildrenDrawingOrderEnabled(true);
        }

        @Override
        protected int getChildDrawingOrder(int i, int i2) {
            if (i2 == i - 1 && this.this$0.mSelected >= 0 && this.this$0.mSelected < i) {
                return this.this$0.mSelected;
            }
            int i3 = (i - i2) - 1;
            return (i3 > this.this$0.mSelected || i3 <= 0) ? i3 : i3 - 1;
        }

        @Override
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            super.onLayout(z, i, i2, i3, i4);
            if (getChildCount() > 1) {
                int right = getChildAt(0).getRight() - this.this$0.mTabOverlap;
                int layoutDirection = getResources().getConfiguration().getLayoutDirection();
                if (layoutDirection == 1) {
                    right = getChildAt(0).getLeft() + this.this$0.mTabOverlap;
                }
                int i5 = right;
                for (int i6 = 1; i6 < getChildCount(); i6++) {
                    View childAt = getChildAt(i6);
                    int right2 = childAt.getRight() - childAt.getLeft();
                    if (layoutDirection == 1) {
                        int i7 = i5 - right2;
                        childAt.layout(i7, childAt.getTop(), i5, childAt.getBottom());
                        i5 = i7 + this.this$0.mTabOverlap;
                    } else {
                        int i8 = right2 + i5;
                        childAt.layout(i5, childAt.getTop(), i8, childAt.getBottom());
                        i5 = i8 - this.this$0.mTabOverlap;
                    }
                }
            }
        }

        @Override
        protected void onMeasure(int i, int i2) {
            super.onMeasure(i, i2);
            setMeasuredDimension(getMeasuredWidth() - (Math.max(0, this.this$0.mContentView.getChildCount() - 1) * this.this$0.mTabOverlap), getMeasuredHeight());
        }
    }

    public TabScrollView(Context context) {
        super(context);
        init(context);
    }

    public TabScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public TabScrollView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    private void animateScroll(int i) {
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "scroll", getScrollX(), i);
        objectAnimatorOfInt.setDuration(this.mAnimationDuration);
        objectAnimatorOfInt.start();
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

    private void init(Context context) {
        this.mAnimationDuration = context.getResources().getInteger(2131623939);
        this.mTabOverlap = (int) context.getResources().getDimension(2131427330);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(2);
        this.mContentView = new TabLayout(this, context);
        this.mContentView.setOrientation(0);
        this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-2, -1));
        this.mContentView.setPadding((int) context.getResources().getDimension(2131427358), 0, 0, 0);
        addView(this.mContentView);
        this.mSelected = -1;
        setScroll(getScroll());
    }

    void addTab(View view) {
        this.mContentView.addView(view);
        view.setActivated(false);
    }

    void clearTabs() {
        this.mContentView.removeAllViews();
    }

    int getChildIndex(View view) {
        return this.mContentView.indexOfChild(view);
    }

    public int getScroll() {
        return getScrollX();
    }

    View getSelectedTab() {
        if (this.mSelected < 0 || this.mSelected >= this.mContentView.getChildCount()) {
            return null;
        }
        return this.mContentView.getChildAt(this.mSelected);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        ensureChildVisible(getSelectedTab());
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

    void removeTab(View view) {
        int iIndexOfChild = this.mContentView.indexOfChild(view);
        if (iIndexOfChild == this.mSelected) {
            this.mSelected = -1;
        } else if (iIndexOfChild < this.mSelected) {
            this.mSelected--;
        }
        this.mContentView.removeView(view);
    }

    public void setScroll(int i) {
        scrollTo(i, getScrollY());
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

    protected void updateLayout() {
        int childCount = this.mContentView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((TabBar.TabView) this.mContentView.getChildAt(i)).updateLayoutParams();
        }
        ensureChildVisible(getSelectedTab());
    }
}
