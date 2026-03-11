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

    static class ContentLayout extends LinearLayout {
        NavTabScroller mScroller;

        public ContentLayout(Context context, NavTabScroller navTabScroller) {
            super(context);
            this.mScroller = navTabScroller;
        }

        @Override
        protected void onMeasure(int i, int i2) {
            View childAt;
            super.onMeasure(i, i2);
            if (this.mScroller.getGap() == 0 || (childAt = getChildAt(0)) == null) {
                return;
            }
            if (this.mScroller.isHorizontal()) {
                setMeasuredDimension(childAt.getMeasuredWidth() + getMeasuredWidth(), getMeasuredHeight());
            } else {
                setMeasuredDimension(getMeasuredWidth(), childAt.getMeasuredHeight() + getMeasuredHeight());
            }
        }
    }

    interface OnLayoutListener {
        void onLayout(int i, int i2, int i3, int i4);
    }

    interface OnRemoveListener {
        void onRemovePosition(int i);
    }

    public NavTabScroller(Context context) {
        super(context);
        init(context);
    }

    public NavTabScroller(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public NavTabScroller(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    private void adjustViewGap(View view, int i) {
        if ((this.mGap >= 0 || i <= this.mGapPosition) && (this.mGap <= 0 || i >= this.mGapPosition)) {
            return;
        }
        if (this.mHorizontal) {
            view.setTranslationX(this.mGap);
        } else {
            view.setTranslationY(this.mGap);
        }
    }

    private void animateOut(View view, float f) {
        animateOut(view, f, this.mHorizontal ? view.getTranslationY() : view.getTranslationX());
    }

    private void animateOut(View view, float f, float f2) {
        int height;
        int i;
        int i2;
        int i3;
        if (view == null || this.mAnimator != null) {
            return;
        }
        int iIndexOfChild = this.mContentView.indexOfChild(view);
        if (f < 0.0f) {
            height = -(this.mHorizontal ? getHeight() : getWidth());
        } else {
            height = this.mHorizontal ? getHeight() : getWidth();
        }
        long jAbs = (long) ((Math.abs(height - (this.mHorizontal ? view.getTop() : view.getLeft())) * 1000) / Math.abs(f));
        int width = this.mHorizontal ? view.getWidth() : view.getHeight();
        int viewCenter = getViewCenter(view);
        int screenCenter = getScreenCenter();
        int i4 = -1;
        int i5 = width / 2;
        if (viewCenter < screenCenter - i5) {
            int i6 = -((screenCenter - viewCenter) - width);
            if (iIndexOfChild <= 0) {
                width = 0;
            }
            i = width;
            i4 = iIndexOfChild;
            i2 = i6;
        } else if (viewCenter > i5 + screenCenter) {
            int i7 = -((screenCenter + width) - viewCenter);
            if (iIndexOfChild < this.mAdapter.getCount() - 1) {
                i = -width;
                i2 = i7;
            } else {
                i3 = i7;
                i = 0;
                i2 = i3;
            }
        } else {
            int i8 = -(screenCenter - viewCenter);
            if (iIndexOfChild < this.mAdapter.getCount() - 1) {
                i = -width;
                i2 = i8;
            } else {
                i3 = i8 - width;
                i = 0;
                i2 = i3;
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
        ObjectAnimator objectAnimatorOfInt = i2 != 0 ? this.mHorizontal ? ObjectAnimator.ofInt(this, "scrollX", getScrollX(), i2 + getScrollX()) : ObjectAnimator.ofInt(this, "scrollY", getScrollY(), i2 + getScrollY()) : null;
        ObjectAnimator objectAnimatorOfInt2 = i != 0 ? ObjectAnimator.ofInt(this, "gap", 0, i) : null;
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
        this.mAnimator.addListener(new AnimatorListenerAdapter(this, iIndexOfChild, i4) {
            final NavTabScroller this$0;
            final int val$pos;
            final int val$position;

            {
                this.this$0 = this;
                this.val$position = iIndexOfChild;
                this.val$pos = i4;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (this.this$0.mRemoveListener != null) {
                    this.this$0.mRemoveListener.onRemovePosition(this.val$position);
                    this.this$0.mAnimator = null;
                    this.this$0.mGapPosition = -1;
                    this.this$0.mGap = 0;
                    this.this$0.handleDataChanged(this.val$pos);
                }
            }
        });
        this.mAnimator.start();
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

    private float ease(DecelerateInterpolator decelerateInterpolator, float f, float f2, float f3, float f4) {
        return (decelerateInterpolator.getInterpolation(f / f4) * f3) + f2;
    }

    private float getAlpha(View view, float f) {
        return 1.0f - (Math.abs(f) / (this.mHorizontal ? view.getHeight() : view.getWidth()));
    }

    private int getScreenCenter() {
        return this.mHorizontal ? getScrollX() + (getWidth() / 2) : getScrollY() + (getHeight() / 2);
    }

    private int getViewCenter(View view) {
        return this.mHorizontal ? view.getLeft() + (view.getWidth() / 2) : view.getTop() + (view.getHeight() / 2);
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

    private void offsetView(View view, float f) {
        view.setAlpha(getAlpha(view, f));
        if (this.mHorizontal) {
            view.setTranslationY(f);
        } else {
            view.setTranslationX(f);
        }
    }

    void adjustGap() {
        for (int i = 0; i < this.mContentView.getChildCount(); i++) {
            adjustViewGap(this.mContentView.getChildAt(i), i);
        }
    }

    protected void animateOut(View view) {
        if (view == null) {
            return;
        }
        animateOut(view, -this.mFlingVelocity);
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mGapPosition > -1) {
            adjustGap();
        }
        super.draw(canvas);
    }

    @Override
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

    protected void finishScroller() {
        this.mScroller.forceFinished(true);
    }

    public int getGap() {
        return this.mGap;
    }

    protected int getScrollValue() {
        return this.mHorizontal ? this.mScrollX : this.mScrollY;
    }

    protected NavTabView getTabView(int i) {
        return (NavTabView) this.mContentView.getChildAt(i);
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
        for (int i2 = 0; i2 < this.mAdapter.getCount(); i2++) {
            View view = this.mAdapter.getView(i2, null, this.mContentView);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
            layoutParams.gravity = this.mHorizontal ? 16 : 1;
            this.mContentView.addView(view, layoutParams);
            if (this.mGapPosition > -1) {
                adjustViewGap(view, i2);
            }
        }
        if (i <= -1) {
            setScrollValue(scrollValue);
            return;
        }
        int iMin = Math.min(this.mAdapter.getCount() - 1, i);
        this.mNeedsScroll = true;
        this.mScrollPosition = iMin;
        requestLayout();
    }

    protected boolean isHorizontal() {
        return this.mHorizontal;
    }

    @Override
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

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        calcPadding();
    }

    @Override
    protected void onOrthoDrag(View view, float f) {
        if (view == null || this.mAnimator != null) {
            return;
        }
        offsetView(view, f);
    }

    @Override
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

    @Override
    protected void onOrthoFling(View view, float f) {
        if (view == null) {
            return;
        }
        if (this.mAnimator != null || Math.abs(f) <= this.mFlingVelocity / 2.0f) {
            offsetView(view, 0.0f);
        } else {
            animateOut(view, f);
        }
    }

    @Override
    protected void onPull(int i) {
        if (i == 0 && this.mPullValue == 0) {
            return;
        }
        if (i != 0 || this.mPullValue == 0) {
            if (this.mPullValue == 0) {
            }
            this.mPullValue += i;
        } else {
            for (int i2 = 0; i2 < 2; i2++) {
                View childAt = this.mContentView.getChildAt(this.mPullValue < 0 ? i2 : (this.mContentView.getChildCount() - 1) - i2);
                if (childAt == null) {
                    break;
                }
                ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(childAt, this.mHorizontal ? "translationX" : "translationY", this.mHorizontal ? getTranslationX() : getTranslationY(), 0.0f);
                ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(childAt, this.mHorizontal ? "rotationY" : "rotationX", this.mHorizontal ? getRotationY() : getRotationX(), 0.0f);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfFloat2);
                animatorSet.setDuration(100L);
                animatorSet.start();
            }
            this.mPullValue = 0;
        }
        int width = this.mHorizontal ? getWidth() : getHeight();
        int iAbs = Math.abs(this.mPullValue);
        int i3 = this.mPullValue <= 0 ? 1 : -1;
        for (int i4 = 0; i4 < 2; i4++) {
            View childAt2 = this.mContentView.getChildAt(this.mPullValue < 0 ? i4 : (this.mContentView.getChildCount() - 1) - i4);
            if (childAt2 == null) {
                return;
            }
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
        }
    }

    protected void setAdapter(BaseAdapter baseAdapter, int i) {
        this.mAdapter = baseAdapter;
        this.mAdapter.registerDataSetObserver(new DataSetObserver(this) {
            final NavTabScroller this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onChanged() {
                super.onChanged();
                this.this$0.handleDataChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
            }
        });
        handleDataChanged(i);
    }

    public void setGap(int i) {
        if (this.mGapPosition != -1) {
            this.mGap = i;
            postInvalidate();
        }
    }

    public void setOnLayoutListener(OnLayoutListener onLayoutListener) {
        this.mLayoutListener = onLayoutListener;
    }

    public void setOnRemoveListener(OnRemoveListener onRemoveListener) {
        this.mRemoveListener = onRemoveListener;
    }

    @Override
    public void setOrientation(int i) {
        this.mContentView.setOrientation(i);
        if (i == 0) {
            this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-2, -1));
        } else {
            this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-1, -2));
        }
        super.setOrientation(i);
    }

    protected void setScrollValue(int i) {
        scrollTo(this.mHorizontal ? i : 0, this.mHorizontal ? 0 : i);
    }

    void snapToSelected(int i, boolean z) {
        View childAt;
        int bottom;
        int right;
        if (i >= 0 && (childAt = this.mContentView.getChildAt(i)) != null) {
            if (this.mHorizontal) {
                right = ((childAt.getRight() + childAt.getLeft()) - getWidth()) / 2;
                bottom = 0;
            } else {
                bottom = ((childAt.getBottom() + childAt.getTop()) - getHeight()) / 2;
                right = 0;
            }
            if (right == this.mScrollX && bottom == this.mScrollY) {
                return;
            }
            if (z) {
                smoothScrollTo(right, bottom);
            } else {
                scrollTo(right, bottom);
            }
        }
    }
}
