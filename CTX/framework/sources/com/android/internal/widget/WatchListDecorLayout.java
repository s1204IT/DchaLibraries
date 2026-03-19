package com.android.internal.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ListView;
import java.util.ArrayList;

public class WatchListDecorLayout extends FrameLayout implements ViewTreeObserver.OnScrollChangedListener {
    private View mBottomPanel;
    private int mForegroundPaddingBottom;
    private int mForegroundPaddingLeft;
    private int mForegroundPaddingRight;
    private int mForegroundPaddingTop;
    private ListView mListView;
    private final ArrayList<View> mMatchParentChildren;
    private ViewTreeObserver mObserver;
    private int mPendingScroll;
    private View mTopPanel;

    public WatchListDecorLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mForegroundPaddingLeft = 0;
        this.mForegroundPaddingTop = 0;
        this.mForegroundPaddingRight = 0;
        this.mForegroundPaddingBottom = 0;
        this.mMatchParentChildren = new ArrayList<>(1);
    }

    public WatchListDecorLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mForegroundPaddingLeft = 0;
        this.mForegroundPaddingTop = 0;
        this.mForegroundPaddingRight = 0;
        this.mForegroundPaddingBottom = 0;
        this.mMatchParentChildren = new ArrayList<>(1);
    }

    public WatchListDecorLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mForegroundPaddingLeft = 0;
        this.mForegroundPaddingTop = 0;
        this.mForegroundPaddingRight = 0;
        this.mForegroundPaddingBottom = 0;
        this.mMatchParentChildren = new ArrayList<>(1);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mPendingScroll = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof ListView) {
                if (this.mListView != null) {
                    throw new IllegalArgumentException("only one ListView child allowed");
                }
                this.mListView = (ListView) childAt;
                this.mListView.setNestedScrollingEnabled(true);
                this.mObserver = this.mListView.getViewTreeObserver();
                this.mObserver.addOnScrollChangedListener(this);
            } else {
                int i2 = ((FrameLayout.LayoutParams) childAt.getLayoutParams()).gravity & 112;
                if (i2 == 48 && this.mTopPanel == null) {
                    this.mTopPanel = childAt;
                } else if (i2 == 80 && this.mBottomPanel == null) {
                    this.mBottomPanel = childAt;
                }
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        this.mListView = null;
        this.mBottomPanel = null;
        this.mTopPanel = null;
        if (this.mObserver != null) {
            if (this.mObserver.isAlive()) {
                this.mObserver.removeOnScrollChangedListener(this);
            }
            this.mObserver = null;
        }
    }

    private void applyMeasureToChild(View view, int i, int i2) {
        int childMeasureSpec;
        int childMeasureSpec2;
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        if (marginLayoutParams.width == -1) {
            childMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.max(0, (((getMeasuredWidth() - getPaddingLeftWithForeground()) - getPaddingRightWithForeground()) - marginLayoutParams.leftMargin) - marginLayoutParams.rightMargin), 1073741824);
        } else {
            childMeasureSpec = getChildMeasureSpec(i, getPaddingLeftWithForeground() + getPaddingRightWithForeground() + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin, marginLayoutParams.width);
        }
        if (marginLayoutParams.height == -1) {
            childMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(Math.max(0, (((getMeasuredHeight() - getPaddingTopWithForeground()) - getPaddingBottomWithForeground()) - marginLayoutParams.topMargin) - marginLayoutParams.bottomMargin), 1073741824);
        } else {
            childMeasureSpec2 = getChildMeasureSpec(i2, getPaddingTopWithForeground() + getPaddingBottomWithForeground() + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin, marginLayoutParams.height);
        }
        view.measure(childMeasureSpec, childMeasureSpec2);
    }

    private int measureAndGetHeight(View view, int i, int i2) {
        if (view != null) {
            if (view.getVisibility() != 8) {
                applyMeasureToChild(this.mBottomPanel, i, i2);
                return view.getMeasuredHeight();
            }
            if (getMeasureAllChildren()) {
                applyMeasureToChild(this.mBottomPanel, i, i2);
                return 0;
            }
            return 0;
        }
        return 0;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int i3;
        int childCount = getChildCount();
        boolean z = (View.MeasureSpec.getMode(i) == 1073741824 && View.MeasureSpec.getMode(i2) == 1073741824) ? false : true;
        this.mMatchParentChildren.clear();
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        for (int i7 = 0; i7 < childCount; i7++) {
            View childAt = getChildAt(i7);
            if (getMeasureAllChildren() || childAt.getVisibility() != 8) {
                measureChildWithMargins(childAt, i, 0, i2, 0);
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) childAt.getLayoutParams();
                int iMax = Math.max(i5, childAt.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin);
                int iMax2 = Math.max(i6, childAt.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin);
                int iCombineMeasuredStates = combineMeasuredStates(i4, childAt.getMeasuredState());
                if (z && (layoutParams.width == -1 || layoutParams.height == -1)) {
                    this.mMatchParentChildren.add(childAt);
                }
                i5 = iMax;
                i6 = iMax2;
                i4 = iCombineMeasuredStates;
            }
        }
        int i8 = i4;
        int paddingLeftWithForeground = i5 + getPaddingLeftWithForeground() + getPaddingRightWithForeground();
        int iMax3 = Math.max(i6 + getPaddingTopWithForeground() + getPaddingBottomWithForeground(), getSuggestedMinimumHeight());
        int iMax4 = Math.max(paddingLeftWithForeground, getSuggestedMinimumWidth());
        Drawable foreground = getForeground();
        if (foreground != null) {
            iMax3 = Math.max(iMax3, foreground.getMinimumHeight());
            iMax4 = Math.max(iMax4, foreground.getMinimumWidth());
        }
        setMeasuredDimension(resolveSizeAndState(iMax4, i, i8), resolveSizeAndState(iMax3, i2, i8 << 16));
        if (this.mListView != null) {
            if (this.mPendingScroll != 0) {
                this.mListView.scrollListBy(this.mPendingScroll);
                i3 = 0;
                this.mPendingScroll = 0;
            } else {
                i3 = 0;
            }
            int iMax5 = Math.max(this.mListView.getPaddingTop(), measureAndGetHeight(this.mTopPanel, i, i2));
            int iMax6 = Math.max(this.mListView.getPaddingBottom(), measureAndGetHeight(this.mBottomPanel, i, i2));
            if (iMax5 != this.mListView.getPaddingTop() || iMax6 != this.mListView.getPaddingBottom()) {
                this.mPendingScroll += this.mListView.getPaddingTop() - iMax5;
                this.mListView.setPadding(this.mListView.getPaddingLeft(), iMax5, this.mListView.getPaddingRight(), iMax6);
            }
        } else {
            i3 = 0;
        }
        int size = this.mMatchParentChildren.size();
        if (size > 1) {
            while (i3 < size) {
                View view = this.mMatchParentChildren.get(i3);
                if (this.mListView == null || (view != this.mTopPanel && view != this.mBottomPanel)) {
                    applyMeasureToChild(view, i, i2);
                }
                i3++;
            }
        }
    }

    @Override
    public void setForegroundGravity(int i) {
        if (getForegroundGravity() != i) {
            super.setForegroundGravity(i);
            Drawable foreground = getForeground();
            if (getForegroundGravity() == 119 && foreground != null) {
                Rect rect = new Rect();
                if (foreground.getPadding(rect)) {
                    this.mForegroundPaddingLeft = rect.left;
                    this.mForegroundPaddingTop = rect.top;
                    this.mForegroundPaddingRight = rect.right;
                    this.mForegroundPaddingBottom = rect.bottom;
                    return;
                }
                return;
            }
            this.mForegroundPaddingLeft = 0;
            this.mForegroundPaddingTop = 0;
            this.mForegroundPaddingRight = 0;
            this.mForegroundPaddingBottom = 0;
        }
    }

    private int getPaddingLeftWithForeground() {
        return isForegroundInsidePadding() ? Math.max(this.mPaddingLeft, this.mForegroundPaddingLeft) : this.mPaddingLeft + this.mForegroundPaddingLeft;
    }

    private int getPaddingRightWithForeground() {
        return isForegroundInsidePadding() ? Math.max(this.mPaddingRight, this.mForegroundPaddingRight) : this.mPaddingRight + this.mForegroundPaddingRight;
    }

    private int getPaddingTopWithForeground() {
        return isForegroundInsidePadding() ? Math.max(this.mPaddingTop, this.mForegroundPaddingTop) : this.mPaddingTop + this.mForegroundPaddingTop;
    }

    private int getPaddingBottomWithForeground() {
        return isForegroundInsidePadding() ? Math.max(this.mPaddingBottom, this.mForegroundPaddingBottom) : this.mPaddingBottom + this.mForegroundPaddingBottom;
    }

    @Override
    public void onScrollChanged() {
        if (this.mListView == null) {
            return;
        }
        if (this.mTopPanel != null) {
            if (this.mListView.getChildCount() > 0) {
                if (this.mListView.getFirstVisiblePosition() == 0) {
                    setScrolling(this.mTopPanel, (this.mListView.getChildAt(0).getY() - this.mTopPanel.getHeight()) - this.mTopPanel.getTop());
                } else {
                    setScrolling(this.mTopPanel, -this.mTopPanel.getHeight());
                }
            } else {
                setScrolling(this.mTopPanel, 0.0f);
            }
        }
        if (this.mBottomPanel != null) {
            if (this.mListView.getChildCount() > 0) {
                if (this.mListView.getLastVisiblePosition() >= this.mListView.getCount() - 1) {
                    setScrolling(this.mBottomPanel, Math.max(0.0f, (this.mListView.getChildAt(this.mListView.getChildCount() - 1).getY() + r0.getHeight()) - this.mBottomPanel.getTop()));
                    return;
                } else {
                    setScrolling(this.mBottomPanel, this.mBottomPanel.getHeight());
                    return;
                }
            }
            setScrolling(this.mBottomPanel, 0.0f);
        }
    }

    private void setScrolling(View view, float f) {
        if (view.getTranslationY() != f) {
            view.setTranslationY(f);
        }
    }
}
