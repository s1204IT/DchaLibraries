package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import java.util.ArrayList;

public class KeyguardWidgetCarousel extends KeyguardWidgetPager {
    private float mAdjacentPagesAngle;
    protected AnimatorSet mChildrenTransformsAnimator;
    Interpolator mFastFadeInterpolator;
    Interpolator mSlowFadeInterpolator;
    float[] mTmpTransform;
    private static float MAX_SCROLL_PROGRESS = 1.3f;
    private static float CAMERA_DISTANCE = 10000.0f;

    public KeyguardWidgetCarousel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardWidgetCarousel(Context context) {
        this(context, null, 0);
    }

    public KeyguardWidgetCarousel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mTmpTransform = new float[3];
        this.mFastFadeInterpolator = new Interpolator() {
            Interpolator mInternal = new DecelerateInterpolator(1.5f);
            float mFactor = 2.5f;

            @Override
            public float getInterpolation(float input) {
                return this.mInternal.getInterpolation(Math.min(this.mFactor * input, 1.0f));
            }
        };
        this.mSlowFadeInterpolator = new Interpolator() {
            Interpolator mInternal = new AccelerateInterpolator(1.5f);
            float mFactor = 1.3f;

            @Override
            public float getInterpolation(float input) {
                return this.mInternal.getInterpolation(this.mFactor * Math.max(input - (1.0f - (1.0f / this.mFactor)), 0.0f));
            }
        };
        this.mAdjacentPagesAngle = context.getResources().getInteger(R.integer.kg_carousel_angle);
    }

    @Override
    protected float getMaxScrollProgress() {
        return MAX_SCROLL_PROGRESS;
    }

    @Override
    public float getAlphaForPage(int screenCenter, int index, boolean showSidePages) {
        View child = getChildAt(index);
        if (child == null) {
            return 0.0f;
        }
        boolean inVisibleRange = index >= getNextPage() + (-1) && index <= getNextPage() + 1;
        float scrollProgress = getScrollProgress(screenCenter, child, index);
        if (isOverScrollChild(index, scrollProgress)) {
            return 1.0f;
        }
        if ((!showSidePages || !inVisibleRange) && index != getNextPage()) {
            return 0.0f;
        }
        float scrollProgress2 = getBoundedScrollProgress(screenCenter, child, index);
        return 1.0f - (Math.abs(scrollProgress2 / MAX_SCROLL_PROGRESS) * 1.0f);
    }

    @Override
    public float getOutlineAlphaForPage(int screenCenter, int index, boolean showSidePages) {
        boolean inVisibleRange = index >= getNextPage() + (-1) && index <= getNextPage() + 1;
        if (inVisibleRange) {
            return super.getOutlineAlphaForPage(screenCenter, index, showSidePages);
        }
        return 0.0f;
    }

    private void updatePageAlphaValues(int screenCenter) {
        if (this.mChildrenOutlineFadeAnimation != null) {
            this.mChildrenOutlineFadeAnimation.cancel();
            this.mChildrenOutlineFadeAnimation = null;
        }
        boolean showSidePages = this.mShowingInitialHints || isPageMoving();
        if (!isReordering(false)) {
            for (int i = 0; i < getChildCount(); i++) {
                KeyguardWidgetFrame child = getWidgetPageAt(i);
                if (child != null) {
                    float outlineAlpha = getOutlineAlphaForPage(screenCenter, i, showSidePages);
                    float contentAlpha = getAlphaForPage(screenCenter, i, showSidePages);
                    child.setBackgroundAlpha(outlineAlpha);
                    child.setContentAlpha(contentAlpha);
                }
            }
        }
    }

    @Override
    protected void screenScrolled(int screenCenter) {
        this.mScreenCenter = screenCenter;
        updatePageAlphaValues(screenCenter);
        if (!isReordering(false)) {
            for (int i = 0; i < getChildCount(); i++) {
                KeyguardWidgetFrame v = getWidgetPageAt(i);
                float scrollProgress = getScrollProgress(screenCenter, v, i);
                float boundedProgress = getBoundedScrollProgress(screenCenter, v, i);
                if (v != this.mDragView && v != null) {
                    v.setCameraDistance(CAMERA_DISTANCE);
                    if (isOverScrollChild(i, scrollProgress)) {
                        v.setRotationY((-OVERSCROLL_MAX_ROTATION) * scrollProgress);
                        v.setOverScrollAmount(Math.abs(scrollProgress), scrollProgress < 0.0f);
                    } else {
                        int width = v.getMeasuredWidth();
                        float pivotX = (width / 2.0f) + ((width / 2.0f) * boundedProgress);
                        float pivotY = v.getMeasuredHeight() / 2;
                        float rotationY = (-this.mAdjacentPagesAngle) * boundedProgress;
                        v.setPivotX(pivotX);
                        v.setPivotY(pivotY);
                        v.setRotationY(rotationY);
                        v.setOverScrollAmount(0.0f, false);
                    }
                    float alpha = v.getAlpha();
                    if (alpha == 0.0f) {
                        v.setVisibility(4);
                    } else if (v.getVisibility() != 0) {
                        v.setVisibility(0);
                    }
                }
            }
        }
    }

    void animatePagesToNeutral() {
        if (this.mChildrenTransformsAnimator != null) {
            this.mChildrenTransformsAnimator.cancel();
            this.mChildrenTransformsAnimator = null;
        }
        int count = getChildCount();
        ArrayList<Animator> anims = new ArrayList<>();
        int i = 0;
        while (i < count) {
            KeyguardWidgetFrame child = getWidgetPageAt(i);
            boolean inVisibleRange = i >= this.mCurrentPage + (-1) && i <= this.mCurrentPage + 1;
            if (!inVisibleRange) {
                child.setRotationY(0.0f);
            }
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("contentAlpha", 1.0f);
            PropertyValuesHolder outlineAlpha = PropertyValuesHolder.ofFloat("backgroundAlpha", 0.6f);
            PropertyValuesHolder rotationY = PropertyValuesHolder.ofFloat("rotationY", 0.0f);
            ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(child, alpha, outlineAlpha, rotationY);
            child.setVisibility(0);
            if (!inVisibleRange) {
                a.setInterpolator(this.mSlowFadeInterpolator);
            }
            anims.add(a);
            i++;
        }
        int duration = this.REORDERING_ZOOM_IN_OUT_DURATION;
        this.mChildrenTransformsAnimator = new AnimatorSet();
        this.mChildrenTransformsAnimator.playTogether(anims);
        this.mChildrenTransformsAnimator.setDuration(duration);
        this.mChildrenTransformsAnimator.start();
    }

    private void getTransformForPage(int screenCenter, int index, float[] transform) {
        View child = getChildAt(index);
        float boundedProgress = getBoundedScrollProgress(screenCenter, child, index);
        float rotationY = (-this.mAdjacentPagesAngle) * boundedProgress;
        int width = child.getMeasuredWidth();
        float pivotX = (width / 2.0f) + ((width / 2.0f) * boundedProgress);
        float pivotY = child.getMeasuredHeight() / 2;
        transform[0] = pivotX;
        transform[1] = pivotY;
        transform[2] = rotationY;
    }

    void animatePagesToCarousel() {
        ObjectAnimator a;
        if (this.mChildrenTransformsAnimator != null) {
            this.mChildrenTransformsAnimator.cancel();
            this.mChildrenTransformsAnimator = null;
        }
        int count = getChildCount();
        ArrayList<Animator> anims = new ArrayList<>();
        int i = 0;
        while (i < count) {
            KeyguardWidgetFrame child = getWidgetPageAt(i);
            float finalAlpha = getAlphaForPage(this.mScreenCenter, i, true);
            float finalOutlineAlpha = getOutlineAlphaForPage(this.mScreenCenter, i, true);
            getTransformForPage(this.mScreenCenter, i, this.mTmpTransform);
            boolean inVisibleRange = i >= this.mCurrentPage + (-1) && i <= this.mCurrentPage + 1;
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("contentAlpha", finalAlpha);
            PropertyValuesHolder outlineAlpha = PropertyValuesHolder.ofFloat("backgroundAlpha", finalOutlineAlpha);
            PropertyValuesHolder pivotX = PropertyValuesHolder.ofFloat("pivotX", this.mTmpTransform[0]);
            PropertyValuesHolder pivotY = PropertyValuesHolder.ofFloat("pivotY", this.mTmpTransform[1]);
            PropertyValuesHolder rotationY = PropertyValuesHolder.ofFloat("rotationY", this.mTmpTransform[2]);
            if (inVisibleRange) {
                a = ObjectAnimator.ofPropertyValuesHolder(child, alpha, outlineAlpha, pivotX, pivotY, rotationY);
            } else {
                a = ObjectAnimator.ofPropertyValuesHolder(child, alpha, outlineAlpha);
                a.setInterpolator(this.mFastFadeInterpolator);
            }
            anims.add(a);
            i++;
        }
        int duration = this.REORDERING_ZOOM_IN_OUT_DURATION;
        this.mChildrenTransformsAnimator = new AnimatorSet();
        this.mChildrenTransformsAnimator.playTogether(anims);
        this.mChildrenTransformsAnimator.setDuration(duration);
        this.mChildrenTransformsAnimator.start();
    }

    @Override
    protected void reorderStarting() {
        this.mViewStateManager.fadeOutSecurity(this.REORDERING_ZOOM_IN_OUT_DURATION);
        animatePagesToNeutral();
    }

    @Override
    protected boolean zoomIn(Runnable onCompleteRunnable) {
        animatePagesToCarousel();
        return super.zoomIn(onCompleteRunnable);
    }

    @Override
    protected void onEndReordering() {
        super.onEndReordering();
        this.mViewStateManager.fadeInSecurity(this.REORDERING_ZOOM_IN_OUT_DURATION);
    }
}
