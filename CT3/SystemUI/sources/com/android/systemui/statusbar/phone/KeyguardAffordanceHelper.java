package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.KeyguardAffordanceView;

public class KeyguardAffordanceHelper {
    private final Callback mCallback;
    private KeyguardAffordanceView mCenterIcon;
    private final Context mContext;
    private FalsingManager mFalsingManager;
    private FlingAnimationUtils mFlingAnimationUtils;
    private int mHintGrowAmount;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private KeyguardAffordanceView mLeftIcon;
    private int mMinBackgroundRadius;
    private int mMinFlingVelocity;
    private int mMinTranslationAmount;
    private boolean mMotionCancelled;
    private KeyguardAffordanceView mRightIcon;
    private Animator mSwipeAnimator;
    private boolean mSwipingInProgress;
    private View mTargetedView;
    private int mTouchSlop;
    private boolean mTouchSlopExeeded;
    private int mTouchTargetSize;
    private float mTranslation;
    private float mTranslationOnDown;
    private VelocityTracker mVelocityTracker;
    private AnimatorListenerAdapter mFlingEndListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            KeyguardAffordanceHelper.this.mSwipeAnimator = null;
            KeyguardAffordanceHelper.this.mSwipingInProgress = false;
            KeyguardAffordanceHelper.this.mTargetedView = null;
        }
    };
    private Runnable mAnimationEndRunnable = new Runnable() {
        @Override
        public void run() {
            KeyguardAffordanceHelper.this.mCallback.onAnimationToSideEnded();
        }
    };

    public interface Callback {
        float getAffordanceFalsingFactor();

        KeyguardAffordanceView getCenterIcon();

        KeyguardAffordanceView getLeftIcon();

        View getLeftPreview();

        float getMaxTranslationDistance();

        KeyguardAffordanceView getRightIcon();

        View getRightPreview();

        boolean needsAntiFalsing();

        void onAnimationToSideEnded();

        void onAnimationToSideStarted(boolean z, float f, float f2);

        void onIconClicked(boolean z);

        void onSwipingAborted();

        void onSwipingStarted(boolean z);
    }

    KeyguardAffordanceHelper(Callback callback, Context context) {
        this.mContext = context;
        this.mCallback = callback;
        initIcons();
        updateIcon(this.mLeftIcon, 0.0f, this.mLeftIcon.getRestingAlpha(), false, false, true, false);
        updateIcon(this.mCenterIcon, 0.0f, this.mCenterIcon.getRestingAlpha(), false, false, true, false);
        updateIcon(this.mRightIcon, 0.0f, this.mRightIcon.getRestingAlpha(), false, false, true, false);
        initDimens();
    }

    private void initDimens() {
        ViewConfiguration configuration = ViewConfiguration.get(this.mContext);
        this.mTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mMinFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMinTranslationAmount = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_min_swipe_amount);
        this.mMinBackgroundRadius = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_min_background_radius);
        this.mTouchTargetSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_touch_target_size);
        this.mHintGrowAmount = this.mContext.getResources().getDimensionPixelSize(R.dimen.hint_grow_amount_sideways);
        this.mFlingAnimationUtils = new FlingAnimationUtils(this.mContext, 0.4f);
        this.mFalsingManager = FalsingManager.getInstance(this.mContext);
    }

    private void initIcons() {
        this.mLeftIcon = this.mCallback.getLeftIcon();
        this.mCenterIcon = this.mCallback.getCenterIcon();
        this.mRightIcon = this.mCallback.getRightIcon();
        updatePreviews();
    }

    public void updatePreviews() {
        this.mLeftIcon.setPreviewView(this.mCallback.getLeftPreview());
        this.mRightIcon.setPreviewView(this.mCallback.getRightPreview());
    }

    public boolean onTouchEvent(MotionEvent event) {
        float distance;
        int action = event.getActionMasked();
        if (this.mMotionCancelled && action != 0) {
            return false;
        }
        float y = event.getY();
        float x = event.getX();
        boolean isUp = false;
        switch (action) {
            case 0:
                View targetView = getIconAtPosition(x, y);
                if (targetView == null || (this.mTargetedView != null && this.mTargetedView != targetView)) {
                    this.mMotionCancelled = true;
                    return false;
                }
                if (this.mTargetedView != null) {
                    cancelAnimation();
                } else {
                    this.mTouchSlopExeeded = false;
                }
                startSwiping(targetView);
                this.mInitialTouchX = x;
                this.mInitialTouchY = y;
                this.mTranslationOnDown = this.mTranslation;
                initVelocityTracker();
                trackMovement(event);
                this.mMotionCancelled = false;
                return true;
            case 1:
                isUp = true;
                break;
            case 2:
                trackMovement(event);
                float xDist = x - this.mInitialTouchX;
                float yDist = y - this.mInitialTouchY;
                float distance2 = (float) Math.hypot(xDist, yDist);
                if (!this.mTouchSlopExeeded && distance2 > this.mTouchSlop) {
                    this.mTouchSlopExeeded = true;
                }
                if (this.mSwipingInProgress) {
                    if (this.mTargetedView == this.mRightIcon) {
                        distance = Math.min(0.0f, this.mTranslationOnDown - distance2);
                    } else {
                        distance = Math.max(0.0f, distance2 + this.mTranslationOnDown);
                    }
                    setTranslation(distance, false, false);
                    return true;
                }
                return true;
            case 3:
                break;
            case 4:
            default:
                return true;
            case 5:
                this.mMotionCancelled = true;
                endMotion(true, x, y);
                return true;
        }
        boolean hintOnTheRight = this.mTargetedView == this.mRightIcon;
        trackMovement(event);
        endMotion(!isUp, x, y);
        if (!this.mTouchSlopExeeded && isUp) {
            this.mCallback.onIconClicked(hintOnTheRight);
            return true;
        }
        return true;
    }

    private void startSwiping(View targetView) {
        this.mCallback.onSwipingStarted(targetView == this.mRightIcon);
        this.mSwipingInProgress = true;
        this.mTargetedView = targetView;
    }

    private View getIconAtPosition(float x, float y) {
        if (leftSwipePossible() && isOnIcon(this.mLeftIcon, x, y)) {
            return this.mLeftIcon;
        }
        if (rightSwipePossible() && isOnIcon(this.mRightIcon, x, y)) {
            return this.mRightIcon;
        }
        return null;
    }

    public boolean isOnAffordanceIcon(float x, float y) {
        if (isOnIcon(this.mLeftIcon, x, y)) {
            return true;
        }
        return isOnIcon(this.mRightIcon, x, y);
    }

    private boolean isOnIcon(View icon, float x, float y) {
        float iconX = icon.getX() + (icon.getWidth() / 2.0f);
        float iconY = icon.getY() + (icon.getHeight() / 2.0f);
        double distance = Math.hypot(x - iconX, y - iconY);
        return distance <= ((double) (this.mTouchTargetSize / 2));
    }

    private void endMotion(boolean forceSnapBack, float lastX, float lastY) {
        if (this.mSwipingInProgress) {
            flingWithCurrentVelocity(forceSnapBack, lastX, lastY);
        } else {
            this.mTargetedView = null;
        }
        if (this.mVelocityTracker == null) {
            return;
        }
        this.mVelocityTracker.recycle();
        this.mVelocityTracker = null;
    }

    private boolean rightSwipePossible() {
        return this.mRightIcon.getVisibility() == 0;
    }

    private boolean leftSwipePossible() {
        return this.mLeftIcon.getVisibility() == 0;
    }

    public void startHintAnimation(boolean right, Runnable onFinishedListener) {
        cancelAnimation();
        startHintAnimationPhase1(right, onFinishedListener);
    }

    private void startHintAnimationPhase1(final boolean right, final Runnable onFinishedListener) {
        KeyguardAffordanceView targetView = right ? this.mRightIcon : this.mLeftIcon;
        ValueAnimator animator = getAnimatorToRadius(right, this.mHintGrowAmount);
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (this.mCancelled) {
                    KeyguardAffordanceHelper.this.mSwipeAnimator = null;
                    KeyguardAffordanceHelper.this.mTargetedView = null;
                    onFinishedListener.run();
                    return;
                }
                KeyguardAffordanceHelper.this.startUnlockHintAnimationPhase2(right, onFinishedListener);
            }
        });
        animator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        animator.setDuration(200L);
        animator.start();
        this.mSwipeAnimator = animator;
        this.mTargetedView = targetView;
    }

    public void startUnlockHintAnimationPhase2(boolean right, final Runnable onFinishedListener) {
        ValueAnimator animator = getAnimatorToRadius(right, 0);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceHelper.this.mSwipeAnimator = null;
                KeyguardAffordanceHelper.this.mTargetedView = null;
                onFinishedListener.run();
            }
        });
        animator.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
        animator.setDuration(350L);
        animator.setStartDelay(500L);
        animator.start();
        this.mSwipeAnimator = animator;
    }

    private ValueAnimator getAnimatorToRadius(final boolean right, int radius) {
        final KeyguardAffordanceView targetView = right ? this.mRightIcon : this.mLeftIcon;
        ValueAnimator animator = ValueAnimator.ofFloat(targetView.getCircleRadius(), radius);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float newRadius = ((Float) animation.getAnimatedValue()).floatValue();
                targetView.setCircleRadiusWithoutAnimation(newRadius);
                float translation = KeyguardAffordanceHelper.this.getTranslationFromRadius(newRadius);
                KeyguardAffordanceHelper keyguardAffordanceHelper = KeyguardAffordanceHelper.this;
                if (right) {
                    translation = -translation;
                }
                keyguardAffordanceHelper.mTranslation = translation;
                KeyguardAffordanceHelper.this.updateIconsFromTranslation(targetView);
            }
        });
        return animator;
    }

    private void cancelAnimation() {
        if (this.mSwipeAnimator == null) {
            return;
        }
        this.mSwipeAnimator.cancel();
    }

    private void flingWithCurrentVelocity(boolean forceSnapBack, float lastX, float lastY) {
        float vel = getCurrentVelocity(lastX, lastY);
        boolean snapBack = false;
        if (this.mCallback.needsAntiFalsing()) {
            snapBack = 0 == 0 ? this.mFalsingManager.isFalseTouch() : true;
        }
        boolean snapBack2 = !snapBack ? isBelowFalsingThreshold() : true;
        boolean velIsInWrongDirection = this.mTranslation * vel < 0.0f;
        boolean snapBack3 = snapBack2 | (Math.abs(vel) > ((float) this.mMinFlingVelocity) ? velIsInWrongDirection : false);
        if (snapBack3 ^ velIsInWrongDirection) {
            vel = 0.0f;
        }
        if (snapBack3) {
            forceSnapBack = true;
        }
        fling(vel, forceSnapBack, this.mTranslation < 0.0f);
    }

    private boolean isBelowFalsingThreshold() {
        return Math.abs(this.mTranslation) < Math.abs(this.mTranslationOnDown) + ((float) getMinTranslationAmount());
    }

    private int getMinTranslationAmount() {
        float factor = this.mCallback.getAffordanceFalsingFactor();
        return (int) (this.mMinTranslationAmount * factor);
    }

    private void fling(float vel, boolean snapBack, boolean right) {
        float target = right ? -this.mCallback.getMaxTranslationDistance() : this.mCallback.getMaxTranslationDistance();
        if (snapBack) {
            target = 0.0f;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(this.mTranslation, target);
        this.mFlingAnimationUtils.apply(animator, this.mTranslation, target, vel);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                KeyguardAffordanceHelper.this.mTranslation = ((Float) animation.getAnimatedValue()).floatValue();
            }
        });
        animator.addListener(this.mFlingEndListener);
        if (!snapBack) {
            startFinishingCircleAnimation(0.375f * vel, this.mAnimationEndRunnable, right);
            this.mCallback.onAnimationToSideStarted(right, this.mTranslation, vel);
        } else {
            reset(true);
        }
        animator.start();
        this.mSwipeAnimator = animator;
        if (!snapBack) {
            return;
        }
        this.mCallback.onSwipingAborted();
    }

    private void startFinishingCircleAnimation(float velocity, Runnable mAnimationEndRunnable, boolean right) {
        KeyguardAffordanceView targetView = right ? this.mRightIcon : this.mLeftIcon;
        targetView.finishAnimation(velocity, mAnimationEndRunnable);
    }

    private void setTranslation(float translation, boolean isReset, boolean animateReset) {
        if (!rightSwipePossible()) {
            translation = Math.max(0.0f, translation);
        }
        if (!leftSwipePossible()) {
            translation = Math.min(0.0f, translation);
        }
        float absTranslation = Math.abs(translation);
        if (translation == this.mTranslation && !isReset) {
            return;
        }
        KeyguardAffordanceView targetView = translation > 0.0f ? this.mLeftIcon : this.mRightIcon;
        KeyguardAffordanceView otherView = translation > 0.0f ? this.mRightIcon : this.mLeftIcon;
        float alpha = absTranslation / getMinTranslationAmount();
        float fadeOutAlpha = Math.max(1.0f - alpha, 0.0f);
        boolean z = isReset ? animateReset : false;
        boolean forceNoCircleAnimation = isReset && !animateReset;
        float radius = getRadiusFromTranslation(absTranslation);
        boolean zIsBelowFalsingThreshold = isReset ? isBelowFalsingThreshold() : false;
        if (!isReset) {
            updateIcon(targetView, radius, alpha + (targetView.getRestingAlpha() * fadeOutAlpha), false, false, false, false);
        } else {
            updateIcon(targetView, 0.0f, fadeOutAlpha * targetView.getRestingAlpha(), z, zIsBelowFalsingThreshold, false, forceNoCircleAnimation);
        }
        updateIcon(otherView, 0.0f, fadeOutAlpha * otherView.getRestingAlpha(), z, zIsBelowFalsingThreshold, false, forceNoCircleAnimation);
        updateIcon(this.mCenterIcon, 0.0f, fadeOutAlpha * this.mCenterIcon.getRestingAlpha(), z, zIsBelowFalsingThreshold, false, forceNoCircleAnimation);
        this.mTranslation = translation;
    }

    public void updateIconsFromTranslation(KeyguardAffordanceView targetView) {
        float absTranslation = Math.abs(this.mTranslation);
        float alpha = absTranslation / getMinTranslationAmount();
        float fadeOutAlpha = Math.max(0.0f, 1.0f - alpha);
        KeyguardAffordanceView otherView = targetView == this.mRightIcon ? this.mLeftIcon : this.mRightIcon;
        updateIconAlpha(targetView, (targetView.getRestingAlpha() * fadeOutAlpha) + alpha, false);
        updateIconAlpha(otherView, otherView.getRestingAlpha() * fadeOutAlpha, false);
        updateIconAlpha(this.mCenterIcon, this.mCenterIcon.getRestingAlpha() * fadeOutAlpha, false);
    }

    public float getTranslationFromRadius(float circleSize) {
        float translation = (circleSize - this.mMinBackgroundRadius) / 0.25f;
        if (translation > 0.0f) {
            return this.mTouchSlop + translation;
        }
        return 0.0f;
    }

    private float getRadiusFromTranslation(float translation) {
        if (translation <= this.mTouchSlop) {
            return 0.0f;
        }
        return ((translation - this.mTouchSlop) * 0.25f) + this.mMinBackgroundRadius;
    }

    public void animateHideLeftRightIcon() {
        cancelAnimation();
        updateIcon(this.mRightIcon, 0.0f, 0.0f, true, false, false, false);
        updateIcon(this.mLeftIcon, 0.0f, 0.0f, true, false, false, false);
    }

    private void updateIcon(KeyguardAffordanceView view, float circleRadius, float alpha, boolean animate, boolean slowRadiusAnimation, boolean force, boolean forceNoCircleAnimation) {
        if (view.getVisibility() != 0 && !force) {
            return;
        }
        if (forceNoCircleAnimation) {
            view.setCircleRadiusWithoutAnimation(circleRadius);
        } else {
            view.setCircleRadius(circleRadius, slowRadiusAnimation);
        }
        updateIconAlpha(view, alpha, animate);
    }

    private void updateIconAlpha(KeyguardAffordanceView view, float alpha, boolean animate) {
        float scale = getScale(alpha, view);
        view.setImageAlpha(Math.min(1.0f, alpha), animate);
        view.setImageScale(scale, animate);
    }

    private float getScale(float alpha, KeyguardAffordanceView icon) {
        float scale = ((alpha / icon.getRestingAlpha()) * 0.2f) + 0.8f;
        return Math.min(scale, 1.5f);
    }

    private void trackMovement(MotionEvent event) {
        if (this.mVelocityTracker == null) {
            return;
        }
        this.mVelocityTracker.addMovement(event);
    }

    private void initVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentVelocity(float lastX, float lastY) {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(1000);
        float aX = this.mVelocityTracker.getXVelocity();
        float aY = this.mVelocityTracker.getYVelocity();
        float bX = lastX - this.mInitialTouchX;
        float bY = lastY - this.mInitialTouchY;
        float bLen = (float) Math.hypot(bX, bY);
        float projectedVelocity = ((aX * bX) + (aY * bY)) / bLen;
        if (this.mTargetedView == this.mRightIcon) {
            return -projectedVelocity;
        }
        return projectedVelocity;
    }

    public void onConfigurationChanged() {
        initDimens();
        initIcons();
    }

    public void onRtlPropertiesChanged() {
        initIcons();
    }

    public void reset(boolean animate) {
        cancelAnimation();
        setTranslation(0.0f, true, animate);
        this.mMotionCancelled = true;
        if (!this.mSwipingInProgress) {
            return;
        }
        this.mCallback.onSwipingAborted();
        this.mSwipingInProgress = false;
    }

    public boolean isSwipingInProgress() {
        return this.mSwipingInProgress;
    }

    public void launchAffordance(boolean animate, boolean left) {
        if (this.mSwipingInProgress) {
            return;
        }
        KeyguardAffordanceView targetView = left ? this.mLeftIcon : this.mRightIcon;
        KeyguardAffordanceView otherView = left ? this.mRightIcon : this.mLeftIcon;
        startSwiping(targetView);
        if (animate) {
            fling(0.0f, false, !left);
            updateIcon(otherView, 0.0f, 0.0f, true, false, true, false);
            updateIcon(this.mCenterIcon, 0.0f, 0.0f, true, false, true, false);
            return;
        }
        this.mCallback.onAnimationToSideStarted(!left, this.mTranslation, 0.0f);
        this.mTranslation = left ? this.mCallback.getMaxTranslationDistance() : this.mCallback.getMaxTranslationDistance();
        updateIcon(this.mCenterIcon, 0.0f, 0.0f, false, false, true, false);
        updateIcon(otherView, 0.0f, 0.0f, false, false, true, false);
        targetView.instantFinishAnimation();
        this.mFlingEndListener.onAnimationEnd(null);
        this.mAnimationEndRunnable.run();
    }
}
