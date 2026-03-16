package com.android.systemui.statusbar.phone;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.ScrimView;

public class ScrimController implements ViewTreeObserver.OnPreDrawListener {
    private boolean mAnimateChange;
    private boolean mAnimateKeyguardFadingOut;
    private long mAnimationDelay;
    private boolean mAnimationStarted;
    private BackDropView mBackDropView;
    private boolean mBouncerShowing;
    private float mCurrentBehindAlpha;
    private float mCurrentInFrontAlpha;
    private boolean mDarkenWhileDragging;
    private float mDozeBehindAlpha;
    private float mDozeInFrontAlpha;
    private boolean mDozing;
    private boolean mExpanding;
    private float mFraction;
    private boolean mKeyguardShowing;
    private final Interpolator mLinearOutSlowInInterpolator;
    private Runnable mOnAnimationFinished;
    private final ScrimView mScrimBehind;
    private final ScrimView mScrimInFront;
    private boolean mScrimSrcEnabled;
    private final UnlockMethodCache mUnlockMethodCache;
    private boolean mUpdatePending;
    private long mDurationOverride = -1;
    private final Interpolator mInterpolator = new DecelerateInterpolator();

    public ScrimController(ScrimView scrimBehind, ScrimView scrimInFront, boolean scrimSrcEnabled) {
        this.mScrimBehind = scrimBehind;
        this.mScrimInFront = scrimInFront;
        Context context = scrimBehind.getContext();
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(context);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.linear_out_slow_in);
        this.mScrimSrcEnabled = scrimSrcEnabled;
    }

    public void setKeyguardShowing(boolean showing) {
        this.mKeyguardShowing = showing;
        scheduleUpdate();
    }

    public void onTrackingStarted() {
        this.mExpanding = true;
        this.mDarkenWhileDragging = this.mUnlockMethodCache.isCurrentlyInsecure() ? false : true;
    }

    public void onExpandingFinished() {
        this.mExpanding = false;
    }

    public void setPanelExpansion(float fraction) {
        if (this.mFraction != fraction) {
            this.mFraction = fraction;
            scheduleUpdate();
        }
    }

    public void setBouncerShowing(boolean showing) {
        this.mBouncerShowing = showing;
        this.mAnimateChange = !this.mExpanding;
        scheduleUpdate();
    }

    public void animateKeyguardFadingOut(long delay, long duration, Runnable onAnimationFinished) {
        this.mAnimateKeyguardFadingOut = true;
        this.mDurationOverride = duration;
        this.mAnimationDelay = delay;
        this.mAnimateChange = true;
        this.mOnAnimationFinished = onAnimationFinished;
        scheduleUpdate();
    }

    public void animateGoingToFullShade(long delay, long duration) {
        this.mDurationOverride = duration;
        this.mAnimationDelay = delay;
        this.mAnimateChange = true;
        scheduleUpdate();
    }

    public void setDozing(boolean dozing) {
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
        if (!this.mUpdatePending) {
            this.mScrimBehind.invalidate();
            this.mScrimBehind.getViewTreeObserver().addOnPreDrawListener(this);
            this.mUpdatePending = true;
        }
    }

    private void updateScrims() {
        if (this.mAnimateKeyguardFadingOut) {
            setScrimInFrontColor(0.0f);
            setScrimBehindColor(0.0f);
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
            setScrimBehindColor(0.55f * behindFraction2);
            return;
        }
        if (this.mBouncerShowing) {
            setScrimInFrontColor(0.75f);
            setScrimBehindColor(0.0f);
        } else {
            float fraction3 = Math.max(0.0f, Math.min(this.mFraction, 1.0f));
            setScrimInFrontColor(0.0f);
            setScrimBehindColor((0.35000002f * fraction3) + 0.2f);
        }
    }

    private void updateScrimNormal() {
        float frac = (1.2f * this.mFraction) - 0.2f;
        if (frac <= 0.0f) {
            setScrimBehindColor(0.0f);
        } else {
            float k = (float) (1.0d - (0.5d * (1.0d - Math.cos(3.141590118408203d * Math.pow(1.0f - frac, 2.0d)))));
            setScrimBehindColor(0.62f * k);
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

    private void setScrimColor(ScrimView scrim, float alpha) {
        Object runningAnim = scrim.getTag(com.android.systemui.R.id.scrim);
        if (runningAnim instanceof ValueAnimator) {
            ((ValueAnimator) runningAnim).cancel();
            scrim.setTag(com.android.systemui.R.id.scrim, null);
        }
        if (this.mAnimateChange) {
            startScrimAnimation(scrim, alpha);
        } else {
            setCurrentScrimAlpha(scrim, alpha);
            updateScrimColor(scrim);
        }
    }

    private float getDozeAlpha(View scrim) {
        return scrim == this.mScrimBehind ? this.mDozeBehindAlpha : this.mDozeInFrontAlpha;
    }

    private float getCurrentScrimAlpha(View scrim) {
        return scrim == this.mScrimBehind ? this.mCurrentBehindAlpha : this.mCurrentInFrontAlpha;
    }

    private void setCurrentScrimAlpha(View scrim, float alpha) {
        if (scrim == this.mScrimBehind) {
            this.mCurrentBehindAlpha = alpha;
        } else {
            this.mCurrentInFrontAlpha = alpha;
        }
    }

    private void updateScrimColor(ScrimView scrim) {
        float alpha1 = getCurrentScrimAlpha(scrim);
        float alpha2 = getDozeAlpha(scrim);
        float alpha = 1.0f - ((1.0f - alpha1) * (1.0f - alpha2));
        scrim.setScrimColor(Color.argb((int) (255.0f * alpha), 0, 0, 0));
    }

    private void startScrimAnimation(final ScrimView scrim, float target) {
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
                scrim.setTag(com.android.systemui.R.id.scrim, null);
            }
        });
        anim.start();
        scrim.setTag(com.android.systemui.R.id.scrim, anim);
        this.mAnimationStarted = true;
    }

    private Interpolator getInterpolator() {
        return this.mAnimateKeyguardFadingOut ? this.mLinearOutSlowInInterpolator : this.mInterpolator;
    }

    @Override
    public boolean onPreDraw() {
        this.mScrimBehind.getViewTreeObserver().removeOnPreDrawListener(this);
        this.mUpdatePending = false;
        updateScrims();
        this.mAnimateKeyguardFadingOut = false;
        this.mDurationOverride = -1L;
        this.mAnimationDelay = 0L;
        if (!this.mAnimationStarted && this.mOnAnimationFinished != null) {
            this.mOnAnimationFinished.run();
            this.mOnAnimationFinished = null;
        }
        this.mAnimationStarted = false;
        return true;
    }

    public void setBackDropView(BackDropView backDropView) {
        this.mBackDropView = backDropView;
        this.mBackDropView.setOnVisibilityChangedRunnable(new Runnable() {
            @Override
            public void run() {
                ScrimController.this.updateScrimBehindDrawingMode();
            }
        });
        updateScrimBehindDrawingMode();
    }

    private void updateScrimBehindDrawingMode() {
        boolean asSrc = this.mBackDropView.getVisibility() != 0 && this.mScrimSrcEnabled;
        this.mScrimBehind.setDrawAsSrc(asSrc);
    }
}
