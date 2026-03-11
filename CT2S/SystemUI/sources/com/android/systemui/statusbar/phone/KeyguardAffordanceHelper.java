package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.R;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.KeyguardAffordanceView;

public class KeyguardAffordanceHelper {
    private Interpolator mAppearInterpolator;
    private Callback mCallback;
    private KeyguardAffordanceView mCenterIcon;
    private final Context mContext;
    private Interpolator mDisappearInterpolator;
    private FlingAnimationUtils mFlingAnimationUtils;
    private int mHintGrowAmount;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private KeyguardAffordanceView mLeftIcon;
    private int mMinBackgroundRadius;
    private int mMinFlingVelocity;
    private int mMinTranslationAmount;
    private boolean mMotionCancelled;
    private boolean mMotionPerformedByUser;
    private KeyguardAffordanceView mRightIcon;
    private Animator mSwipeAnimator;
    private boolean mSwipingInProgress;
    private int mTouchSlop;
    private float mTranslation;
    private float mTranslationOnDown;
    private VelocityTracker mVelocityTracker;
    private AnimatorListenerAdapter mFlingEndListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            KeyguardAffordanceHelper.this.mSwipeAnimator = null;
            KeyguardAffordanceHelper.this.setSwipingInProgress(false);
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

        float getPageWidth();

        KeyguardAffordanceView getRightIcon();

        View getRightPreview();

        void onAnimationToSideEnded();

        void onAnimationToSideStarted(boolean z, float f, float f2);

        void onSwipingStarted();
    }

    KeyguardAffordanceHelper(Callback callback, Context context) {
        this.mContext = context;
        this.mCallback = callback;
        initIcons();
        updateIcon(this.mLeftIcon, 0.0f, 0.5f, false, false);
        updateIcon(this.mCenterIcon, 0.0f, 0.5f, false, false);
        updateIcon(this.mRightIcon, 0.0f, 0.5f, false, false);
        initDimens();
    }

    private void initDimens() {
        ViewConfiguration configuration = ViewConfiguration.get(this.mContext);
        this.mTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mMinFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMinTranslationAmount = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_min_swipe_amount);
        this.mMinBackgroundRadius = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_min_background_radius);
        this.mHintGrowAmount = this.mContext.getResources().getDimensionPixelSize(R.dimen.hint_grow_amount_sideways);
        this.mFlingAnimationUtils = new FlingAnimationUtils(this.mContext, 0.4f);
        this.mAppearInterpolator = AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.linear_out_slow_in);
        this.mDisappearInterpolator = AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_linear_in);
    }

    private void initIcons() {
        this.mLeftIcon = this.mCallback.getLeftIcon();
        this.mLeftIcon.setIsLeft(true);
        this.mCenterIcon = this.mCallback.getCenterIcon();
        this.mRightIcon = this.mCallback.getRightIcon();
        this.mRightIcon.setIsLeft(false);
        this.mLeftIcon.setPreviewView(this.mCallback.getLeftPreview());
        this.mRightIcon.setPreviewView(this.mCallback.getRightPreview());
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mMotionCancelled && event.getActionMasked() != 0) {
            return false;
        }
        float y = event.getY();
        float x = event.getX();
        boolean isUp = false;
        switch (event.getActionMasked()) {
            case 0:
                if (this.mSwipingInProgress) {
                    cancelAnimation();
                }
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                this.mTranslationOnDown = this.mTranslation;
                initVelocityTracker();
                trackMovement(event);
                this.mMotionPerformedByUser = false;
                this.mMotionCancelled = false;
                break;
            case 1:
                isUp = true;
                trackMovement(event);
                endMotion(event, isUp ? false : true);
                break;
            case 2:
                float w = x - this.mInitialTouchX;
                trackMovement(event);
                if (((leftSwipePossible() && w > this.mTouchSlop) || (rightSwipePossible() && w < (-this.mTouchSlop))) && Math.abs(w) > Math.abs(y - this.mInitialTouchY) && !this.mSwipingInProgress) {
                    cancelAnimation();
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    this.mTranslationOnDown = this.mTranslation;
                    setSwipingInProgress(true);
                }
                if (this.mSwipingInProgress) {
                    setTranslation((this.mTranslationOnDown + x) - this.mInitialTouchX, false, false);
                }
                break;
            case 3:
                trackMovement(event);
                endMotion(event, isUp ? false : true);
                break;
            case 5:
                this.mMotionCancelled = true;
                endMotion(event, true);
                break;
        }
        return true;
    }

    private void endMotion(MotionEvent event, boolean forceSnapBack) {
        if (this.mSwipingInProgress) {
            flingWithCurrentVelocity(forceSnapBack);
        }
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public void setSwipingInProgress(boolean inProgress) {
        this.mSwipingInProgress = inProgress;
        if (inProgress) {
            this.mCallback.onSwipingStarted();
        }
    }

    private boolean rightSwipePossible() {
        return this.mRightIcon.getVisibility() == 0;
    }

    private boolean leftSwipePossible() {
        return this.mLeftIcon.getVisibility() == 0;
    }

    public void startHintAnimation(boolean right, Runnable onFinishedListener) {
        startHintAnimationPhase1(right, onFinishedListener);
    }

    private void startHintAnimationPhase1(final boolean right, final Runnable onFinishedListener) {
        final KeyguardAffordanceView targetView = right ? this.mRightIcon : this.mLeftIcon;
        targetView.showArrow(true);
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
                    onFinishedListener.run();
                    targetView.showArrow(false);
                    return;
                }
                KeyguardAffordanceHelper.this.startUnlockHintAnimationPhase2(right, onFinishedListener);
            }
        });
        animator.setInterpolator(this.mAppearInterpolator);
        animator.setDuration(200L);
        animator.start();
        this.mSwipeAnimator = animator;
    }

    public void startUnlockHintAnimationPhase2(boolean right, final Runnable onFinishedListener) {
        final KeyguardAffordanceView targetView = right ? this.mRightIcon : this.mLeftIcon;
        ValueAnimator animator = getAnimatorToRadius(right, 0);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceHelper.this.mSwipeAnimator = null;
                targetView.showArrow(false);
                onFinishedListener.run();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                targetView.showArrow(false);
            }
        });
        animator.setInterpolator(this.mDisappearInterpolator);
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
                KeyguardAffordanceHelper.this.updateIconsFromRadius(targetView, newRadius);
            }
        });
        return animator;
    }

    private void cancelAnimation() {
        if (this.mSwipeAnimator != null) {
            this.mSwipeAnimator.cancel();
        }
    }

    private void flingWithCurrentVelocity(boolean forceSnapBack) {
        float vel = getCurrentVelocity();
        boolean snapBack = isBelowFalsingThreshold();
        boolean velIsInWrongDirection = this.mTranslation * vel < 0.0f;
        boolean snapBack2 = snapBack | (Math.abs(vel) > ((float) this.mMinFlingVelocity) && velIsInWrongDirection);
        if (snapBack2 ^ velIsInWrongDirection) {
            vel = 0.0f;
        }
        fling(vel, snapBack2 || forceSnapBack);
    }

    private boolean isBelowFalsingThreshold() {
        return Math.abs(this.mTranslation) < Math.abs(this.mTranslationOnDown) + ((float) getMinTranslationAmount());
    }

    private int getMinTranslationAmount() {
        float factor = this.mCallback.getAffordanceFalsingFactor();
        return (int) (this.mMinTranslationAmount * factor);
    }

    private void fling(float vel, boolean snapBack) {
        float target = this.mTranslation < 0.0f ? -this.mCallback.getPageWidth() : this.mCallback.getPageWidth();
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
            startFinishingCircleAnimation(0.375f * vel, this.mAnimationEndRunnable);
            this.mCallback.onAnimationToSideStarted(this.mTranslation < 0.0f, this.mTranslation, vel);
        } else {
            reset(true);
        }
        animator.start();
        this.mSwipeAnimator = animator;
    }

    private void startFinishingCircleAnimation(float velocity, Runnable mAnimationEndRunnable) {
        KeyguardAffordanceView targetView = this.mTranslation > 0.0f ? this.mLeftIcon : this.mRightIcon;
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
        if (absTranslation > Math.abs(this.mTranslationOnDown) + getMinTranslationAmount() || this.mMotionPerformedByUser) {
            this.mMotionPerformedByUser = true;
        }
        if (translation != this.mTranslation || isReset) {
            KeyguardAffordanceView targetView = translation > 0.0f ? this.mLeftIcon : this.mRightIcon;
            KeyguardAffordanceView otherView = translation > 0.0f ? this.mRightIcon : this.mLeftIcon;
            float alpha = absTranslation / getMinTranslationAmount();
            float fadeOutAlpha = Math.max(0.0f, 0.5f * (1.0f - alpha));
            float alpha2 = alpha + fadeOutAlpha;
            boolean animateIcons = isReset && animateReset;
            float radius = getRadiusFromTranslation(absTranslation);
            boolean slowAnimation = isReset && isBelowFalsingThreshold();
            if (!isReset) {
                updateIcon(targetView, radius, alpha2, false, false);
            } else {
                updateIcon(targetView, 0.0f, fadeOutAlpha, animateIcons, slowAnimation);
            }
            updateIcon(otherView, 0.0f, fadeOutAlpha, animateIcons, slowAnimation);
            updateIcon(this.mCenterIcon, 0.0f, fadeOutAlpha, animateIcons, slowAnimation);
            this.mTranslation = translation;
        }
    }

    public void updateIconsFromRadius(KeyguardAffordanceView targetView, float newRadius) {
        float alpha = newRadius / this.mMinBackgroundRadius;
        float fadeOutAlpha = Math.max(0.0f, 0.5f * (1.0f - alpha));
        float alpha2 = alpha + fadeOutAlpha;
        KeyguardAffordanceView otherView = targetView == this.mRightIcon ? this.mLeftIcon : this.mRightIcon;
        updateIconAlpha(targetView, alpha2, false);
        updateIconAlpha(otherView, fadeOutAlpha, false);
        updateIconAlpha(this.mCenterIcon, fadeOutAlpha, false);
    }

    public float getTranslationFromRadius(float circleSize) {
        float translation = (circleSize - this.mMinBackgroundRadius) / 0.15f;
        return Math.max(0.0f, translation);
    }

    private float getRadiusFromTranslation(float translation) {
        return (0.15f * translation) + this.mMinBackgroundRadius;
    }

    public void animateHideLeftRightIcon() {
        updateIcon(this.mRightIcon, 0.0f, 0.0f, true, false);
        updateIcon(this.mLeftIcon, 0.0f, 0.0f, true, false);
    }

    private void updateIcon(KeyguardAffordanceView view, float circleRadius, float alpha, boolean animate, boolean slowRadiusAnimation) {
        if (view.getVisibility() == 0) {
            view.setCircleRadius(circleRadius, slowRadiusAnimation);
            updateIconAlpha(view, alpha, animate);
        }
    }

    private void updateIconAlpha(KeyguardAffordanceView view, float alpha, boolean animate) {
        float scale = getScale(alpha);
        view.setImageAlpha(Math.min(1.0f, alpha), animate);
        view.setImageScale(scale, animate);
    }

    private float getScale(float alpha) {
        float scale = ((alpha / 0.5f) * 0.2f) + 0.8f;
        return Math.min(scale, 1.5f);
    }

    private void trackMovement(MotionEvent event) {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
        }
    }

    private void initVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentVelocity() {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(1000);
        return this.mVelocityTracker.getXVelocity();
    }

    public void onConfigurationChanged() {
        initDimens();
        initIcons();
    }

    public void onRtlPropertiesChanged() {
        initIcons();
    }

    public void reset(boolean animate) {
        if (this.mSwipeAnimator != null) {
            this.mSwipeAnimator.cancel();
        }
        setTranslation(0.0f, true, animate);
        setSwipingInProgress(false);
    }
}
