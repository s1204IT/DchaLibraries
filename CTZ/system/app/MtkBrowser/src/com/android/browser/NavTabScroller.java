package com.android.browser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.browser.view.ScrollerView;

/* loaded from: classes.dex */
public class NavTabScroller extends ScrollerView {
    static final float[] PULL_FACTOR = {2.5f, 0.9f};
    private BaseAdapter mAdapter;
    private AnimatorSet mAnimator;
    private ContentLayout mContentView;
    DecelerateInterpolator mCubic;
    private float mFlingVelocity;
    private int mGap;
    private ObjectAnimator mGapAnimator;
    private int mGapPosition;
    private OnLayoutListener mLayoutListener;
    private boolean mNeedsScroll;
    int mPullValue;
    private OnRemoveListener mRemoveListener;
    private int mScrollPosition;

    interface OnLayoutListener {
        void onLayout(int i, int i2, int i3, int i4);
    }

    interface OnRemoveListener {
        void onRemovePosition(int i);
    }

    public NavTabScroller(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    public NavTabScroller(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public NavTabScroller(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        this.mCubic = new DecelerateInterpolator(1.5f);
        this.mGapPosition = -1;
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setLayoutDirection(0);
        this.mContentView = new ContentLayout(context, this);
        this.mContentView.setOrientation(0);
        addView(this.mContentView);
        this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-2, -1));
        setGap(getGap());
        this.mFlingVelocity = getContext().getResources().getDisplayMetrics().density * 1500.0f;
    }

    protected int getScrollValue() {
        return this.mHorizontal ? this.mScrollX : this.mScrollY;
    }

    protected void setScrollValue(int i) {
        int i2 = this.mHorizontal ? i : 0;
        if (this.mHorizontal) {
            i = 0;
        }
        scrollTo(i2, i);
    }

    protected NavTabView getTabView(int i) {
        return (NavTabView) this.mContentView.getChildAt(i);
    }

    protected boolean isHorizontal() {
        return this.mHorizontal;
    }

    @Override // com.android.browser.view.ScrollerView
    public void setOrientation(int i) {
        this.mContentView.setOrientation(i);
        if (i == 0) {
            this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-2, -1));
        } else {
            this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-1, -2));
        }
        super.setOrientation(i);
    }

    @Override // com.android.browser.view.ScrollerView, android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        calcPadding();
    }

    private void calcPadding() {
        if (this.mAdapter.getCount() > 0) {
            View childAt = this.mContentView.getChildAt(0);
            if (this.mHorizontal) {
                int measuredWidth = ((getMeasuredWidth() - childAt.getMeasuredWidth()) / 2) + 2;
                this.mContentView.setPadding(measuredWidth, 0, measuredWidth, 0);
            } else {
                int measuredHeight = ((getMeasuredHeight() - childAt.getMeasuredHeight()) / 2) + 2;
                this.mContentView.setPadding(0, measuredHeight, 0, measuredHeight);
            }
        }
    }

    public void setOnRemoveListener(OnRemoveListener onRemoveListener) {
        this.mRemoveListener = onRemoveListener;
    }

    public void setOnLayoutListener(OnLayoutListener onLayoutListener) {
        this.mLayoutListener = onLayoutListener;
    }

    protected void setAdapter(BaseAdapter baseAdapter, int i) {
        this.mAdapter = baseAdapter;
        this.mAdapter.registerDataSetObserver(new DataSetObserver() { // from class: com.android.browser.NavTabScroller.1
            @Override // android.database.DataSetObserver
            public void onChanged() {
                super.onChanged();
                NavTabScroller.this.handleDataChanged();
            }

            @Override // android.database.DataSetObserver
            public void onInvalidated() {
                super.onInvalidated();
            }
        });
        handleDataChanged(i);
    }

    protected void handleDataChanged() {
        handleDataChanged(-1);
    }

    void handleDataChanged(int i) {
        int scrollValue = getScrollValue();
        if (this.mGapAnimator != null) {
            this.mGapAnimator.cancel();
        }
        this.mContentView.removeAllViews();
        int i2 = 0;
        while (true) {
            if (i2 >= this.mAdapter.getCount()) {
                break;
            }
            View view = this.mAdapter.getView(i2, null, this.mContentView);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
            layoutParams.gravity = this.mHorizontal ? 16 : 1;
            this.mContentView.addView(view, layoutParams);
            if (this.mGapPosition > -1) {
                adjustViewGap(view, i2);
            }
            i2++;
        }
        if (i > -1) {
            int iMin = Math.min(this.mAdapter.getCount() - 1, i);
            this.mNeedsScroll = true;
            this.mScrollPosition = iMin;
            requestLayout();
            return;
        }
        setScrollValue(scrollValue);
    }

    protected void finishScroller() {
        this.mScroller.forceFinished(true);
    }

    @Override // com.android.browser.view.ScrollerView, android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mNeedsScroll) {
            this.mScroller.forceFinished(true);
            snapToSelected(this.mScrollPosition, false);
            this.mNeedsScroll = false;
        }
        if (this.mLayoutListener != null) {
            this.mLayoutListener.onLayout(i, i2, i3, i4);
            this.mLayoutListener = null;
        }
    }

    void snapToSelected(int i, boolean z) {
        View childAt;
        int left;
        if (i >= 0 && (childAt = this.mContentView.getChildAt(i)) != null) {
            int top = 0;
            if (this.mHorizontal) {
                left = ((childAt.getLeft() + childAt.getRight()) - getWidth()) / 2;
            } else {
                top = ((childAt.getTop() + childAt.getBottom()) - getHeight()) / 2;
                left = 0;
            }
            if (left != this.mScrollX || top != this.mScrollY) {
                if (z) {
                    smoothScrollTo(left, top);
                } else {
                    scrollTo(left, top);
                }
            }
        }
    }

    protected void animateOut(View view) {
        if (view == null) {
            return;
        }
        animateOut(view, -this.mFlingVelocity);
    }

    private void animateOut(View view, float f) {
        animateOut(view, f, this.mHorizontal ? view.getTranslationY() : view.getTranslationX());
    }

    private void animateOut(View view, float f, float f2) {
        int height;
        int i;
        ObjectAnimator objectAnimatorOfInt;
        if (view == null || this.mAnimator != null) {
            return;
        }
        final int iIndexOfChild = this.mContentView.indexOfChild(view);
        if (f < 0.0f) {
            height = -(this.mHorizontal ? getHeight() : getWidth());
        } else {
            height = this.mHorizontal ? getHeight() : getWidth();
        }
        long jAbs = (long) ((Math.abs(height - (this.mHorizontal ? view.getTop() : view.getLeft())) * 1000) / Math.abs(f));
        int width = this.mHorizontal ? view.getWidth() : view.getHeight();
        int viewCenter = getViewCenter(view);
        int screenCenter = getScreenCenter();
        final int i2 = -1;
        int i3 = width / 2;
        if (viewCenter < screenCenter - i3) {
            i = -((screenCenter - viewCenter) - width);
            if (iIndexOfChild <= 0) {
                width = 0;
            }
            i2 = iIndexOfChild;
        } else if (viewCenter > i3 + screenCenter) {
            i = -((screenCenter + width) - viewCenter);
            if (iIndexOfChild < this.mAdapter.getCount() - 1) {
                width = -width;
            } else {
                width = 0;
            }
        } else {
            i = -(screenCenter - viewCenter);
            if (iIndexOfChild < this.mAdapter.getCount() - 1) {
                width = -width;
            } else {
                i -= width;
                width = 0;
            }
        }
        this.mGapPosition = iIndexOfChild;
        float f3 = height;
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) (this.mHorizontal ? TRANSLATION_Y : TRANSLATION_X), f2, f3);
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(view, (Property<View, Float>) ALPHA, getAlpha(view, f2), getAlpha(view, f3));
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfFloat2);
        animatorSet.setDuration(jAbs);
        this.mAnimator = new AnimatorSet();
        ObjectAnimator objectAnimatorOfInt2 = null;
        if (i != 0) {
            if (this.mHorizontal) {
                objectAnimatorOfInt = ObjectAnimator.ofInt(this, "scrollX", getScrollX(), getScrollX() + i);
            } else {
                objectAnimatorOfInt = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), getScrollY() + i);
            }
        } else {
            objectAnimatorOfInt = null;
        }
        if (width != 0) {
            objectAnimatorOfInt2 = ObjectAnimator.ofInt(this, "gap", 0, width);
        }
        if (objectAnimatorOfInt != null) {
            if (objectAnimatorOfInt2 != null) {
                AnimatorSet animatorSet2 = new AnimatorSet();
                animatorSet2.playTogether(objectAnimatorOfInt, objectAnimatorOfInt2);
                animatorSet2.setDuration(200L);
                this.mAnimator.playSequentially(animatorSet, animatorSet2);
            } else {
                objectAnimatorOfInt.setDuration(200L);
                this.mAnimator.playSequentially(animatorSet, objectAnimatorOfInt);
            }
        } else if (objectAnimatorOfInt2 != null) {
            objectAnimatorOfInt2.setDuration(200L);
            this.mAnimator.playSequentially(animatorSet, objectAnimatorOfInt2);
        }
        this.mAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.browser.NavTabScroller.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (NavTabScroller.this.mRemoveListener != null) {
                    NavTabScroller.this.mRemoveListener.onRemovePosition(iIndexOfChild);
                    NavTabScroller.this.mAnimator = null;
                    NavTabScroller.this.mGapPosition = -1;
                    NavTabScroller.this.mGap = 0;
                    NavTabScroller.this.handleDataChanged(i2);
                }
            }
        });
        this.mAnimator.start();
    }

    public void setGap(int i) {
        if (this.mGapPosition != -1) {
            this.mGap = i;
            postInvalidate();
        }
    }

    public int getGap() {
        return this.mGap;
    }

    void adjustGap() {
        for (int i = 0; i < this.mContentView.getChildCount(); i++) {
            adjustViewGap(this.mContentView.getChildAt(i), i);
        }
    }

    private void adjustViewGap(View view, int i) {
        if ((this.mGap < 0 && i > this.mGapPosition) || (this.mGap > 0 && i < this.mGapPosition)) {
            if (this.mHorizontal) {
                view.setTranslationX(this.mGap);
            } else {
                view.setTranslationY(this.mGap);
            }
        }
    }

    private int getViewCenter(View view) {
        if (this.mHorizontal) {
            return view.getLeft() + (view.getWidth() / 2);
        }
        return view.getTop() + (view.getHeight() / 2);
    }

    private int getScreenCenter() {
        if (this.mHorizontal) {
            return getScrollX() + (getWidth() / 2);
        }
        return getScrollY() + (getHeight() / 2);
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        if (this.mGapPosition > -1) {
            adjustGap();
        }
        super.draw(canvas);
    }

    @Override // com.android.browser.view.ScrollerView
    protected View findViewAt(int i, int i2) {
        int i3 = i + this.mScrollX;
        int i4 = i2 + this.mScrollY;
        for (int childCount = this.mContentView.getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = this.mContentView.getChildAt(childCount);
            if (childAt.getVisibility() == 0 && i3 >= childAt.getLeft() && i3 < childAt.getRight() && i4 >= childAt.getTop() && i4 < childAt.getBottom()) {
                return childAt;
            }
        }
        return null;
    }

    @Override // com.android.browser.view.ScrollerView
    protected void onOrthoDrag(View view, float f) {
        if (view != null && this.mAnimator == null) {
            offsetView(view, f);
        }
    }

    @Override // com.android.browser.view.ScrollerView
    protected void onOrthoDragFinished(View view) {
        if (this.mAnimator == null && this.mIsOrthoDragged && view != null) {
            float translationY = this.mHorizontal ? view.getTranslationY() : view.getTranslationX();
            if (Math.abs(translationY) > (this.mHorizontal ? view.getHeight() : view.getWidth()) / 2) {
                animateOut(view, Math.signum(translationY) * this.mFlingVelocity, translationY);
            } else {
                offsetView(view, 0.0f);
            }
        }
    }

    @Override // com.android.browser.view.ScrollerView
    protected void onOrthoFling(View view, float f) {
        if (view == null) {
            return;
        }
        if (this.mAnimator == null && Math.abs(f) > this.mFlingVelocity / 2.0f) {
            animateOut(view, f);
        } else {
            offsetView(view, 0.0f);
        }
    }

    private void offsetView(View view, float f) {
        view.setAlpha(getAlpha(view, f));
        if (this.mHorizontal) {
            view.setTranslationY(f);
        } else {
            view.setTranslationX(f);
        }
    }

    private float getAlpha(View view, float f) {
        return 1.0f - (Math.abs(f) / (this.mHorizontal ? view.getHeight() : view.getWidth()));
    }

    private float ease(DecelerateInterpolator decelerateInterpolator, float f, float f2, float f3, float f4) {
        return f2 + (f3 * decelerateInterpolator.getInterpolation(f / f4));
    }

    @Override // com.android.browser.view.ScrollerView
    protected void onPull(int i) {
        int childCount;
        int childCount2;
        if (i == 0 && this.mPullValue == 0) {
            return;
        }
        if (i == 0 && this.mPullValue != 0) {
            for (int i2 = 0; i2 < 2; i2++) {
                ContentLayout contentLayout = this.mContentView;
                if (this.mPullValue < 0) {
                    childCount2 = i2;
                } else {
                    childCount2 = (this.mContentView.getChildCount() - 1) - i2;
                }
                View childAt = contentLayout.getChildAt(childCount2);
                if (childAt == null) {
                    break;
                }
                String str = this.mHorizontal ? "translationX" : "translationY";
                float[] fArr = new float[2];
                fArr[0] = this.mHorizontal ? getTranslationX() : getTranslationY();
                fArr[1] = 0.0f;
                ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(childAt, str, fArr);
                String str2 = this.mHorizontal ? "rotationY" : "rotationX";
                float[] fArr2 = new float[2];
                fArr2[0] = this.mHorizontal ? getRotationY() : getRotationX();
                fArr2[1] = 0.0f;
                ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(childAt, str2, fArr2);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfFloat2);
                animatorSet.setDuration(100L);
                animatorSet.start();
            }
            this.mPullValue = 0;
        } else {
            if (this.mPullValue == 0) {
            }
            this.mPullValue += i;
        }
        int width = this.mHorizontal ? getWidth() : getHeight();
        int iAbs = Math.abs(this.mPullValue);
        int i3 = this.mPullValue <= 0 ? 1 : -1;
        for (int i4 = 0; i4 < 2; i4++) {
            ContentLayout contentLayout2 = this.mContentView;
            if (this.mPullValue < 0) {
                childCount = i4;
            } else {
                childCount = (this.mContentView.getChildCount() - 1) - i4;
            }
            View childAt2 = contentLayout2.getChildAt(childCount);
            if (childAt2 != null) {
                float f = PULL_FACTOR[i4];
                float f2 = iAbs;
                float f3 = width;
                float fEase = (-i3) * ease(this.mCubic, f2, 0.0f, f * 2.0f, f3);
                int iEase = ((int) ease(this.mCubic, f2, 0.0f, f * 20.0f, f3)) * i3;
                if (this.mHorizontal) {
                    childAt2.setTranslationX(iEase);
                } else {
                    childAt2.setTranslationY(iEase);
                }
                if (this.mHorizontal) {
                    childAt2.setRotationY(-fEase);
                } else {
                    childAt2.setRotationX(fEase);
                }
            } else {
                return;
            }
        }
    }

    static class ContentLayout extends LinearLayout {
        NavTabScroller mScroller;

        public ContentLayout(Context context, NavTabScroller navTabScroller) {
            super(context);
            this.mScroller = navTabScroller;
        }

        @Override // android.widget.LinearLayout, android.view.View
        protected void onMeasure(int i, int i2) {
            View childAt;
            super.onMeasure(i, i2);
            if (this.mScroller.getGap() != 0 && (childAt = getChildAt(0)) != null) {
                if (this.mScroller.isHorizontal()) {
                    setMeasuredDimension(childAt.getMeasuredWidth() + getMeasuredWidth(), getMeasuredHeight());
                } else {
                    setMeasuredDimension(getMeasuredWidth(), childAt.getMeasuredHeight() + getMeasuredHeight());
                }
            }
        }
    }
}
