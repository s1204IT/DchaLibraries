package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.stack.StackStateAnimator;

public class ScrimController implements ViewTreeObserver.OnPreDrawListener, HeadsUpManager.OnHeadsUpChangedListener {
    public static final Interpolator KEYGUARD_FADE_OUT_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.7f, 1.0f);
    private boolean mAnimateChange;
    private boolean mAnimateKeyguardFadingOut;
    private long mAnimationDelay;
    protected boolean mBouncerShowing;
    private float mCurrentBehindAlpha;
    private float mCurrentInFrontAlpha;
    private boolean mDarkenWhileDragging;
    private boolean mDontAnimateBouncerChanges;
    private float mDozeBehindAlpha;
    private float mDozeInFrontAlpha;
    private boolean mDozing;
    private View mDraggedHeadsUpView;
    private boolean mExpanding;
    private boolean mForceHideScrims;
    private float mFraction;
    private final View mHeadsUpScrim;
    private ValueAnimator mKeyguardFadeoutAnimation;
    private boolean mKeyguardFadingOutInProgress;
    protected boolean mKeyguardShowing;
    private Runnable mOnAnimationFinished;
    private int mPinnedHeadsUpCount;
    protected final ScrimView mScrimBehind;
    private final ScrimView mScrimInFront;
    private boolean mSkipFirstFrame;
    private float mTopHeadsUpDragAmount;
    private final UnlockMethodCache mUnlockMethodCache;
    private boolean mUpdatePending;
    private boolean mWakeAndUnlocking;
    private float mScrimBehindAlpha = 0.62f;
    private float mScrimBehindAlphaKeyguard = 0.45f;
    private float mScrimBehindAlphaUnlocking = 0.2f;
    private long mDurationOverride = -1;
    private final Interpolator mInterpolator = new DecelerateInterpolator();
    private float mCurrentHeadsUpAlpha = 1.0f;

    public ScrimController(ScrimView scrimBehind, ScrimView scrimInFront, View headsUpScrim) {
        this.mScrimBehind = scrimBehind;
        this.mScrimInFront = scrimInFront;
        this.mHeadsUpScrim = headsUpScrim;
        Context context = scrimBehind.getContext();
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(context);
        updateHeadsUpScrim(false);
    }

    public void setKeyguardShowing(boolean showing) {
        this.mKeyguardShowing = showing;
        scheduleUpdate();
    }

    public void onTrackingStarted() {
        this.mExpanding = true;
        this.mDarkenWhileDragging = this.mUnlockMethodCache.canSkipBouncer() ? false : true;
    }

    public void onExpandingFinished() {
        this.mExpanding = false;
    }

    public void setPanelExpansion(float fraction) {
        if (this.mFraction == fraction) {
            return;
        }
        this.mFraction = fraction;
        scheduleUpdate();
        if (this.mPinnedHeadsUpCount != 0) {
            updateHeadsUpScrim(false);
        }
        if (this.mKeyguardFadeoutAnimation == null) {
            return;
        }
        this.mKeyguardFadeoutAnimation.cancel();
    }

    public void setBouncerShowing(boolean showing) {
        boolean z = false;
        this.mBouncerShowing = showing;
        if (!this.mExpanding && !this.mDontAnimateBouncerChanges) {
            z = true;
        }
        this.mAnimateChange = z;
        scheduleUpdate();
    }

    public void setWakeAndUnlocking() {
        this.mWakeAndUnlocking = true;
        scheduleUpdate();
    }

    public void animateKeyguardFadingOut(long delay, long duration, Runnable onAnimationFinished, boolean skipFirstFrame) {
        this.mWakeAndUnlocking = false;
        this.mAnimateKeyguardFadingOut = true;
        this.mDurationOverride = duration;
        this.mAnimationDelay = delay;
        this.mAnimateChange = true;
        this.mSkipFirstFrame = skipFirstFrame;
        this.mOnAnimationFinished = onAnimationFinished;
        scheduleUpdate();
        onPreDraw();
    }

    public void abortKeyguardFadingOut() {
        if (!this.mAnimateKeyguardFadingOut) {
            return;
        }
        endAnimateKeyguardFadingOut(true);
    }

    public void animateGoingToFullShade(long delay, long duration) {
        this.mDurationOverride = duration;
        this.mAnimationDelay = delay;
        this.mAnimateChange = true;
        scheduleUpdate();
    }

    public void animateNextChange() {
        this.mAnimateChange = true;
    }

    public void setDozing(boolean dozing) {
        if (this.mDozing == dozing) {
            return;
        }
        this.mDozing = dozing;
        scheduleUpdate();
    }

    public void setDozeInFrontAlpha(float alpha) {
        this.mDozeInFrontAlpha = alpha;
        updateScrimColor(this.mScrimInFront);
    }

    public void setDozeBehindAlpha(float alpha) {
        this.mDozeBehindAlpha = alpha;
        updateScrimColor(this.mScrimBehind);
    }

    public float getDozeBehindAlpha() {
        return this.mDozeBehindAlpha;
    }

    public float getDozeInFrontAlpha() {
        return this.mDozeInFrontAlpha;
    }

    private void scheduleUpdate() {
        if (this.mUpdatePending) {
            return;
        }
        this.mScrimBehind.invalidate();
        this.mScrimBehind.getViewTreeObserver().addOnPreDrawListener(this);
        this.mUpdatePending = true;
    }

    protected void updateScrims() {
        if (this.mAnimateKeyguardFadingOut || this.mForceHideScrims) {
            setScrimInFrontColor(0.0f);
            setScrimBehindColor(0.0f);
        } else if (this.mWakeAndUnlocking) {
            if (this.mDozing) {
                setScrimInFrontColor(0.0f);
                setScrimBehindColor(1.0f);
            } else {
                setScrimInFrontColor(1.0f);
                setScrimBehindColor(0.0f);
            }
        } else if (!this.mKeyguardShowing && !this.mBouncerShowing) {
            updateScrimNormal();
            setScrimInFrontColor(0.0f);
        } else {
            updateScrimKeyguard();
        }
        this.mAnimateChange = false;
    }

    private void updateScrimKeyguard() {
        if (this.mExpanding && this.mDarkenWhileDragging) {
            float behindFraction = Math.max(0.0f, Math.min(this.mFraction, 1.0f));
            float fraction = 1.0f - behindFraction;
            float fraction2 = (float) Math.pow(fraction, 0.800000011920929d);
            float behindFraction2 = (float) Math.pow(behindFraction, 0.800000011920929d);
            setScrimInFrontColor(fraction2 * 0.75f);
            setScrimBehindColor(this.mScrimBehindAlphaKeyguard * behindFraction2);
            return;
        }
        if (this.mBouncerShowing) {
            setScrimInFrontColor(0.75f);
            setScrimBehindColor(0.0f);
        } else {
            float fraction3 = Math.max(0.0f, Math.min(this.mFraction, 1.0f));
            setScrimInFrontColor(0.0f);
            setScrimBehindColor(((this.mScrimBehindAlphaKeyguard - this.mScrimBehindAlphaUnlocking) * fraction3) + this.mScrimBehindAlphaUnlocking);
        }
    }

    private void updateScrimNormal() {
        float frac = (1.2f * this.mFraction) - 0.2f;
        if (frac <= 0.0f) {
            setScrimBehindColor(0.0f);
        } else {
            float k = (float) (1.0d - ((1.0d - Math.cos(Math.pow(1.0f - frac, 2.0d) * 3.141590118408203d)) * 0.5d));
            setScrimBehindColor(this.mScrimBehindAlpha * k);
        }
    }

    private void setScrimBehindColor(float alpha) {
        setScrimColor(this.mScrimBehind, alpha);
    }

    private void setScrimInFrontColor(float alpha) {
        setScrimColor(this.mScrimInFront, alpha);
        if (alpha == 0.0f) {
            this.mScrimInFront.setClickable(false);
        } else {
            this.mScrimInFront.setClickable(this.mDozing ? false : true);
        }
    }

    private void setScrimColor(View scrim, float alpha) {
        updateScrim(this.mAnimateChange, scrim, alpha, getCurrentScrimAlpha(scrim));
    }

    private float getDozeAlpha(View scrim) {
        return scrim == this.mScrimBehind ? this.mDozeBehindAlpha : this.mDozeInFrontAlpha;
    }

    private float getCurrentScrimAlpha(View scrim) {
        return scrim == this.mScrimBehind ? this.mCurrentBehindAlpha : scrim == this.mScrimInFront ? this.mCurrentInFrontAlpha : this.mCurrentHeadsUpAlpha;
    }

    public void setCurrentScrimAlpha(View scrim, float alpha) {
        if (scrim == this.mScrimBehind) {
            this.mCurrentBehindAlpha = alpha;
        } else if (scrim == this.mScrimInFront) {
            this.mCurrentInFrontAlpha = alpha;
        } else {
            this.mCurrentHeadsUpAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        }
    }

    public void updateScrimColor(View scrim) {
        float alpha1 = getCurrentScrimAlpha(scrim);
        if (scrim instanceof ScrimView) {
            float alpha2 = getDozeAlpha(scrim);
            float alpha = 1.0f - ((1.0f - alpha1) * (1.0f - alpha2));
            ((ScrimView) scrim).setScrimColor(Color.argb((int) (255.0f * Math.max(0.0f, Math.min(1.0f, alpha))), 0, 0, 0));
            return;
        }
        scrim.setAlpha(alpha1);
    }

    private void startScrimAnimation(final View scrim, float target) {
        float current = getCurrentScrimAlpha(scrim);
        ValueAnimator anim = ValueAnimator.ofFloat(current, target);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = ((Float) animation.getAnimatedValue()).floatValue();
                ScrimController.this.setCurrentScrimAlpha(scrim, alpha);
                ScrimController.this.updateScrimColor(scrim);
            }
        });
        anim.setInterpolator(getInterpolator());
        anim.setStartDelay(this.mAnimationDelay);
        anim.setDuration(this.mDurationOverride != -1 ? this.mDurationOverride : 220L);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (ScrimController.this.mOnAnimationFinished != null) {
                    ScrimController.this.mOnAnimationFinished.run();
                    ScrimController.this.mOnAnimationFinished = null;
                }
                if (ScrimController.this.mKeyguardFadingOutInProgress) {
                    ScrimController.this.mKeyguardFadeoutAnimation = null;
                    ScrimController.this.mKeyguardFadingOutInProgress = false;
                }
                scrim.setTag(R.id.scrim, null);
                scrim.setTag(R.id.scrim_target, null);
            }
        });
        anim.start();
        if (this.mAnimateKeyguardFadingOut) {
            this.mKeyguardFadingOutInProgress = true;
            this.mKeyguardFadeoutAnimation = anim;
        }
        if (this.mSkipFirstFrame) {
            anim.setCurrentPlayTime(16L);
        }
        scrim.setTag(R.id.scrim, anim);
        scrim.setTag(R.id.scrim_target, Float.valueOf(target));
    }

    private Interpolator getInterpolator() {
        return this.mAnimateKeyguardFadingOut ? KEYGUARD_FADE_OUT_INTERPOLATOR : this.mInterpolator;
    }

    @Override
    public boolean onPreDraw() {
        this.mScrimBehind.getViewTreeObserver().removeOnPreDrawListener(this);
        this.mUpdatePending = false;
        if (this.mDontAnimateBouncerChanges) {
            this.mDontAnimateBouncerChanges = false;
        }
        updateScrims();
        this.mDurationOverride = -1L;
        this.mAnimationDelay = 0L;
        this.mSkipFirstFrame = false;
        endAnimateKeyguardFadingOut(false);
        return true;
    }

    private void endAnimateKeyguardFadingOut(boolean force) {
        this.mAnimateKeyguardFadingOut = false;
        if (!force && (isAnimating(this.mScrimInFront) || isAnimating(this.mScrimBehind))) {
            return;
        }
        if (this.mOnAnimationFinished != null) {
            this.mOnAnimationFinished.run();
            this.mOnAnimationFinished = null;
        }
        this.mKeyguardFadingOutInProgress = false;
    }

    private boolean isAnimating(View scrim) {
        return scrim.getTag(R.id.scrim) != null;
    }

    public void setDrawBehindAsSrc(boolean asSrc) {
        this.mScrimBehind.setDrawAsSrc(asSrc);
    }

    @Override
    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
    }

    @Override
    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        this.mPinnedHeadsUpCount++;
        updateHeadsUpScrim(true);
    }

    @Override
    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
        this.mPinnedHeadsUpCount--;
        if (headsUp == this.mDraggedHeadsUpView) {
            this.mDraggedHeadsUpView = null;
            this.mTopHeadsUpDragAmount = 0.0f;
        }
        updateHeadsUpScrim(true);
    }

    @Override
    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
    }

    private void updateHeadsUpScrim(boolean animate) {
        updateScrim(animate, this.mHeadsUpScrim, calculateHeadsUpAlpha(), this.mCurrentHeadsUpAlpha);
    }

    private void updateScrim(boolean animate, View scrim, float alpha, float currentAlpha) {
        if (this.mKeyguardFadingOutInProgress) {
            return;
        }
        ValueAnimator previousAnimator = (ValueAnimator) StackStateAnimator.getChildTag(scrim, R.id.scrim);
        float animEndValue = -1.0f;
        if (previousAnimator != null) {
            if (animate || alpha == currentAlpha) {
                previousAnimator.cancel();
            } else {
                animEndValue = ((Float) StackStateAnimator.getChildTag(scrim, R.id.scrim_alpha_end)).floatValue();
            }
        }
        if (alpha == currentAlpha || alpha == animEndValue) {
            return;
        }
        if (animate) {
            startScrimAnimation(scrim, alpha);
            scrim.setTag(R.id.scrim_alpha_start, Float.valueOf(currentAlpha));
            scrim.setTag(R.id.scrim_alpha_end, Float.valueOf(alpha));
        } else {
            if (previousAnimator != null) {
                float previousStartValue = ((Float) StackStateAnimator.getChildTag(scrim, R.id.scrim_alpha_start)).floatValue();
                float previousEndValue = ((Float) StackStateAnimator.getChildTag(scrim, R.id.scrim_alpha_end)).floatValue();
                PropertyValuesHolder[] values = previousAnimator.getValues();
                float relativeDiff = alpha - previousEndValue;
                float newStartValue = Math.max(0.0f, Math.min(1.0f, previousStartValue + relativeDiff));
                values[0].setFloatValues(newStartValue, alpha);
                scrim.setTag(R.id.scrim_alpha_start, Float.valueOf(newStartValue));
                scrim.setTag(R.id.scrim_alpha_end, Float.valueOf(alpha));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            }
            setCurrentScrimAlpha(scrim, alpha);
            updateScrimColor(scrim);
        }
    }

    public void setTopHeadsUpDragAmount(View draggedHeadsUpView, float topHeadsUpDragAmount) {
        this.mTopHeadsUpDragAmount = topHeadsUpDragAmount;
        this.mDraggedHeadsUpView = draggedHeadsUpView;
        updateHeadsUpScrim(false);
    }

    private float calculateHeadsUpAlpha() {
        float alpha;
        if (this.mPinnedHeadsUpCount >= 2) {
            alpha = 1.0f;
        } else if (this.mPinnedHeadsUpCount == 0) {
            alpha = 0.0f;
        } else {
            alpha = 1.0f - this.mTopHeadsUpDragAmount;
        }
        float expandFactor = 1.0f - this.mFraction;
        return alpha * Math.max(expandFactor, 0.0f);
    }

    public void forceHideScrims(boolean hide) {
        this.mForceHideScrims = hide;
        this.mAnimateChange = false;
        scheduleUpdate();
    }

    public void dontAnimateBouncerChangesUntilNextFrame() {
        this.mDontAnimateBouncerChanges = true;
    }

    public void setExcludedBackgroundArea(Rect area) {
        this.mScrimBehind.setExcludedArea(area);
    }

    public int getScrimBehindColor() {
        return this.mScrimBehind.getScrimColorWithAlpha();
    }

    public void setScrimBehindChangeRunnable(Runnable changeRunnable) {
        this.mScrimBehind.setChangeRunnable(changeRunnable);
    }

    public void onDensityOrFontScaleChanged() {
        ViewGroup.LayoutParams layoutParams = this.mHeadsUpScrim.getLayoutParams();
        layoutParams.height = this.mHeadsUpScrim.getResources().getDimensionPixelSize(R.dimen.heads_up_scrim_height);
        this.mHeadsUpScrim.setLayoutParams(layoutParams);
    }
}
