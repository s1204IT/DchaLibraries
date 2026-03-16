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

    interface OnLayoutListener {
        void onLayout(int i, int i2, int i3, int i4);
    }

    interface OnRemoveListener {
        void onRemovePosition(int i);
    }

    public NavTabScroller(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public NavTabScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NavTabScroller(Context context) {
        super(context);
        init(context);
    }

    private void init(Context ctx) {
        this.mCubic = new DecelerateInterpolator(1.5f);
        this.mGapPosition = -1;
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        this.mContentView = new ContentLayout(ctx, this);
        this.mContentView.setOrientation(0);
        addView(this.mContentView);
        this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-2, -1));
        setGap(getGap());
        this.mFlingVelocity = getContext().getResources().getDisplayMetrics().density * 1500.0f;
    }

    protected int getScrollValue() {
        return this.mHorizontal ? this.mScrollX : this.mScrollY;
    }

    protected void setScrollValue(int value) {
        scrollTo(this.mHorizontal ? value : 0, this.mHorizontal ? 0 : value);
    }

    protected NavTabView getTabView(int pos) {
        return (NavTabView) this.mContentView.getChildAt(pos);
    }

    protected boolean isHorizontal() {
        return this.mHorizontal;
    }

    @Override
    public void setOrientation(int orientation) {
        this.mContentView.setOrientation(orientation);
        if (orientation == 0) {
            this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-2, -1));
        } else {
            this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(-1, -2));
        }
        super.setOrientation(orientation);
    }

    @Override
    protected void onMeasure(int wspec, int hspec) {
        super.onMeasure(wspec, hspec);
        calcPadding();
    }

    private void calcPadding() {
        if (this.mAdapter.getCount() > 0) {
            View v = this.mContentView.getChildAt(0);
            if (this.mHorizontal) {
                int pad = ((getMeasuredWidth() - v.getMeasuredWidth()) / 2) + 2;
                this.mContentView.setPadding(pad, 0, pad, 0);
            } else {
                int pad2 = ((getMeasuredHeight() - v.getMeasuredHeight()) / 2) + 2;
                this.mContentView.setPadding(0, pad2, 0, pad2);
            }
        }
    }

    public void setOnRemoveListener(OnRemoveListener l) {
        this.mRemoveListener = l;
    }

    public void setOnLayoutListener(OnLayoutListener l) {
        this.mLayoutListener = l;
    }

    protected void setAdapter(BaseAdapter adapter, int selection) {
        this.mAdapter = adapter;
        this.mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                NavTabScroller.this.handleDataChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
            }
        });
        handleDataChanged(selection);
    }

    protected void handleDataChanged() {
        handleDataChanged(-1);
    }

    void handleDataChanged(int newscroll) {
        int scroll = getScrollValue();
        if (this.mGapAnimator != null) {
            this.mGapAnimator.cancel();
        }
        this.mContentView.removeAllViews();
        for (int i = 0; i < this.mAdapter.getCount(); i++) {
            View v = this.mAdapter.getView(i, null, this.mContentView);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.gravity = this.mHorizontal ? 16 : 1;
            this.mContentView.addView(v, lp);
            if (this.mGapPosition > -1) {
                adjustViewGap(v, i);
            }
        }
        if (newscroll > -1) {
            int newscroll2 = Math.min(this.mAdapter.getCount() - 1, newscroll);
            this.mNeedsScroll = true;
            this.mScrollPosition = newscroll2;
            requestLayout();
            return;
        }
        setScrollValue(scroll);
    }

    protected void finishScroller() {
        this.mScroller.forceFinished(true);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (this.mNeedsScroll) {
            this.mScroller.forceFinished(true);
            snapToSelected(this.mScrollPosition, false);
            this.mNeedsScroll = false;
        }
        if (this.mLayoutListener != null) {
            this.mLayoutListener.onLayout(l, t, r, b);
            this.mLayoutListener = null;
        }
    }

    void snapToSelected(int pos, boolean smooth) {
        View v;
        if (pos >= 0 && (v = this.mContentView.getChildAt(pos)) != null) {
            int sx = 0;
            int sy = 0;
            if (this.mHorizontal) {
                sx = ((v.getLeft() + v.getRight()) - getWidth()) / 2;
            } else {
                sy = ((v.getTop() + v.getBottom()) - getHeight()) / 2;
            }
            if (sx != this.mScrollX || sy != this.mScrollY) {
                if (smooth) {
                    smoothScrollTo(sx, sy);
                } else {
                    scrollTo(sx, sy);
                }
            }
        }
    }

    protected void animateOut(View v) {
        if (v != null) {
            animateOut(v, -this.mFlingVelocity);
        }
    }

    private void animateOut(View v, float velocity) {
        float start = this.mHorizontal ? v.getTranslationY() : v.getTranslationX();
        animateOut(v, velocity, start);
    }

    private void animateOut(View v, float velocity, float start) {
        int target;
        int scroll;
        if (v != null && this.mAnimator == null) {
            final int position = this.mContentView.indexOfChild(v);
            if (velocity < 0.0f) {
                target = this.mHorizontal ? -getHeight() : -getWidth();
            } else {
                target = this.mHorizontal ? getHeight() : getWidth();
            }
            int distance = target - (this.mHorizontal ? v.getTop() : v.getLeft());
            long duration = (long) ((Math.abs(distance) * 1000) / Math.abs(velocity));
            int translate = 0;
            int gap = this.mHorizontal ? v.getWidth() : v.getHeight();
            int centerView = getViewCenter(v);
            int centerScreen = getScreenCenter();
            int newpos = -1;
            if (centerView < centerScreen - (gap / 2)) {
                scroll = -((centerScreen - centerView) - gap);
                translate = position > 0 ? gap : 0;
                newpos = position;
            } else if (centerView > (gap / 2) + centerScreen) {
                scroll = -((centerScreen + gap) - centerView);
                if (position < this.mAdapter.getCount() - 1) {
                    translate = -gap;
                }
            } else {
                scroll = -(centerScreen - centerView);
                if (position < this.mAdapter.getCount() - 1) {
                    translate = -gap;
                } else {
                    scroll -= gap;
                }
            }
            this.mGapPosition = position;
            final int pos = newpos;
            ObjectAnimator trans = ObjectAnimator.ofFloat(v, (Property<View, Float>) (this.mHorizontal ? TRANSLATION_Y : TRANSLATION_X), start, target);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(v, (Property<View, Float>) ALPHA, getAlpha(v, start), getAlpha(v, target));
            AnimatorSet set1 = new AnimatorSet();
            set1.playTogether(trans, alpha);
            set1.setDuration(duration);
            this.mAnimator = new AnimatorSet();
            ObjectAnimator trans2 = null;
            ObjectAnimator scroll1 = null;
            if (scroll != 0) {
                if (this.mHorizontal) {
                    scroll1 = ObjectAnimator.ofInt(this, "scrollX", getScrollX(), getScrollX() + scroll);
                } else {
                    scroll1 = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), getScrollY() + scroll);
                }
            }
            if (translate != 0) {
                trans2 = ObjectAnimator.ofInt(this, "gap", 0, translate);
            }
            if (scroll1 != null) {
                if (trans2 != null) {
                    AnimatorSet set2 = new AnimatorSet();
                    set2.playTogether(scroll1, trans2);
                    set2.setDuration(200L);
                    this.mAnimator.playSequentially(set1, set2);
                } else {
                    scroll1.setDuration(200L);
                    this.mAnimator.playSequentially(set1, scroll1);
                }
            } else if (trans2 != null) {
                trans2.setDuration(200L);
                this.mAnimator.playSequentially(set1, trans2);
            }
            this.mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator a) {
                    if (NavTabScroller.this.mRemoveListener != null) {
                        NavTabScroller.this.mRemoveListener.onRemovePosition(position);
                        NavTabScroller.this.mAnimator = null;
                        NavTabScroller.this.mGapPosition = -1;
                        NavTabScroller.this.mGap = 0;
                        NavTabScroller.this.handleDataChanged(pos);
                    }
                }
            });
            this.mAnimator.start();
        }
    }

    public void setGap(int gap) {
        if (this.mGapPosition != -1) {
            this.mGap = gap;
            postInvalidate();
        }
    }

    public int getGap() {
        return this.mGap;
    }

    void adjustGap() {
        for (int i = 0; i < this.mContentView.getChildCount(); i++) {
            View child = this.mContentView.getChildAt(i);
            adjustViewGap(child, i);
        }
    }

    private void adjustViewGap(View view, int pos) {
        if ((this.mGap < 0 && pos > this.mGapPosition) || (this.mGap > 0 && pos < this.mGapPosition)) {
            if (this.mHorizontal) {
                view.setTranslationX(this.mGap);
            } else {
                view.setTranslationY(this.mGap);
            }
        }
    }

    private int getViewCenter(View v) {
        return this.mHorizontal ? v.getLeft() + (v.getWidth() / 2) : v.getTop() + (v.getHeight() / 2);
    }

    private int getScreenCenter() {
        return this.mHorizontal ? getScrollX() + (getWidth() / 2) : getScrollY() + (getHeight() / 2);
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mGapPosition > -1) {
            adjustGap();
        }
        super.draw(canvas);
    }

    @Override
    protected View findViewAt(int x, int y) {
        int x2 = x + this.mScrollX;
        int y2 = y + this.mScrollY;
        int count = this.mContentView.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = this.mContentView.getChildAt(i);
            if (child.getVisibility() == 0 && x2 >= child.getLeft() && x2 < child.getRight() && y2 >= child.getTop() && y2 < child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    @Override
    protected void onOrthoDrag(View v, float distance) {
        if (v != null && this.mAnimator == null) {
            offsetView(v, distance);
        }
    }

    @Override
    protected void onOrthoDragFinished(View downView) {
        if (this.mAnimator == null && this.mIsOrthoDragged && downView != null) {
            float diff = this.mHorizontal ? downView.getTranslationY() : downView.getTranslationX();
            if (Math.abs(diff) > (this.mHorizontal ? downView.getHeight() : downView.getWidth()) / 2) {
                animateOut(downView, Math.signum(diff) * this.mFlingVelocity, diff);
            } else {
                offsetView(downView, 0.0f);
            }
        }
    }

    @Override
    protected void onOrthoFling(View v, float velocity) {
        if (v != null) {
            if (this.mAnimator == null && Math.abs(velocity) > this.mFlingVelocity / 2.0f) {
                animateOut(v, velocity);
            } else {
                offsetView(v, 0.0f);
            }
        }
    }

    private void offsetView(View v, float distance) {
        v.setAlpha(getAlpha(v, distance));
        if (this.mHorizontal) {
            v.setTranslationY(distance);
        } else {
            v.setTranslationX(distance);
        }
    }

    private float getAlpha(View v, float distance) {
        return 1.0f - (Math.abs(distance) / (this.mHorizontal ? v.getHeight() : v.getWidth()));
    }

    private float ease(DecelerateInterpolator inter, float value, float start, float dist, float duration) {
        return (inter.getInterpolation(value / duration) * dist) + start;
    }

    @Override
    protected void onPull(int delta) {
        boolean layer = false;
        if (delta != 0 || this.mPullValue != 0) {
            if (delta == 0 && this.mPullValue != 0) {
                for (int i = 0; i < 2; i++) {
                    View child = this.mContentView.getChildAt(this.mPullValue < 0 ? i : (this.mContentView.getChildCount() - 1) - i);
                    if (child == null) {
                        break;
                    }
                    String str = this.mHorizontal ? "translationX" : "translationY";
                    float[] fArr = new float[2];
                    fArr[0] = this.mHorizontal ? getTranslationX() : getTranslationY();
                    fArr[1] = 0.0f;
                    ObjectAnimator trans = ObjectAnimator.ofFloat(child, str, fArr);
                    String str2 = this.mHorizontal ? "rotationY" : "rotationX";
                    float[] fArr2 = new float[2];
                    fArr2[0] = this.mHorizontal ? getRotationY() : getRotationX();
                    fArr2[1] = 0.0f;
                    ObjectAnimator rot = ObjectAnimator.ofFloat(child, str2, fArr2);
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(trans, rot);
                    set.setDuration(100L);
                    set.start();
                }
                this.mPullValue = 0;
            } else {
                if (this.mPullValue == 0) {
                    layer = true;
                }
                this.mPullValue += delta;
            }
            int height = this.mHorizontal ? getWidth() : getHeight();
            int oscroll = Math.abs(this.mPullValue);
            int factor = this.mPullValue <= 0 ? 1 : -1;
            for (int i2 = 0; i2 < 2; i2++) {
                View child2 = this.mContentView.getChildAt(this.mPullValue < 0 ? i2 : (this.mContentView.getChildCount() - 1) - i2);
                if (child2 != null) {
                    if (layer) {
                    }
                    float k = PULL_FACTOR[i2];
                    float rot2 = (-factor) * ease(this.mCubic, oscroll, 0.0f, k * 2.0f, height);
                    int y = factor * ((int) ease(this.mCubic, oscroll, 0.0f, k * 20.0f, height));
                    if (this.mHorizontal) {
                        child2.setTranslationX(y);
                    } else {
                        child2.setTranslationY(y);
                    }
                    if (this.mHorizontal) {
                        child2.setRotationY(-rot2);
                    } else {
                        child2.setRotationX(rot2);
                    }
                } else {
                    return;
                }
            }
        }
    }

    static class ContentLayout extends LinearLayout {
        NavTabScroller mScroller;

        public ContentLayout(Context context, NavTabScroller scroller) {
            super(context);
            this.mScroller = scroller;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            View v;
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (this.mScroller.getGap() != 0 && (v = getChildAt(0)) != null) {
                if (this.mScroller.isHorizontal()) {
                    int total = v.getMeasuredWidth() + getMeasuredWidth();
                    setMeasuredDimension(total, getMeasuredHeight());
                } else {
                    int total2 = v.getMeasuredHeight() + getMeasuredHeight();
                    setMeasuredDimension(getMeasuredWidth(), total2);
                }
            }
        }
    }
}
