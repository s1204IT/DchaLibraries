package com.android.systemui.statusbar.phone;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeLog;

public class DozeScrimController {
    private static final boolean DEBUG = Log.isLoggable("DozeScrimController", 3);
    private Animator mBehindAnimator;
    private float mBehindTarget;
    private final Interpolator mDozeAnimationInterpolator;
    private final DozeParameters mDozeParameters;
    private boolean mDozing;
    private Animator mInFrontAnimator;
    private float mInFrontTarget;
    private DozeHost.PulseCallback mPulseCallback;
    private final Interpolator mPulseInInterpolatorPickup;
    private int mPulseReason;
    private final ScrimController mScrimController;
    private final Interpolator mPulseInInterpolator = PhoneStatusBar.ALPHA_OUT;
    private final Interpolator mPulseOutInterpolator = PhoneStatusBar.ALPHA_IN;
    private final Handler mHandler = new Handler();
    private final Runnable mPulseIn = new Runnable() {
        @Override
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse in, mDozing=" + DozeScrimController.this.mDozing + " mPulseReason=" + DozeLog.pulseReasonToString(DozeScrimController.this.mPulseReason));
            }
            if (DozeScrimController.this.mDozing) {
                DozeLog.tracePulseStart(DozeScrimController.this.mPulseReason);
                boolean pickup = DozeScrimController.this.mPulseReason == 3;
                DozeScrimController.this.startScrimAnimation(true, 0.0f, DozeScrimController.this.mDozeParameters.getPulseInDuration(pickup), pickup ? DozeScrimController.this.mPulseInInterpolatorPickup : DozeScrimController.this.mPulseInInterpolator, DozeScrimController.this.mDozeParameters.getPulseInDelay(pickup), DozeScrimController.this.mPulseInFinished);
                DozeScrimController.this.pulseStarted();
            }
        }
    };
    private final Runnable mPulseInFinished = new Runnable() {
        @Override
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse in finished, mDozing=" + DozeScrimController.this.mDozing);
            }
            if (DozeScrimController.this.mDozing) {
                DozeScrimController.this.mHandler.postDelayed(DozeScrimController.this.mPulseOut, DozeScrimController.this.mDozeParameters.getPulseVisibleDuration());
            }
        }
    };
    private final Runnable mPulseOut = new Runnable() {
        @Override
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse out, mDozing=" + DozeScrimController.this.mDozing);
            }
            if (DozeScrimController.this.mDozing) {
                DozeScrimController.this.startScrimAnimation(true, 1.0f, DozeScrimController.this.mDozeParameters.getPulseOutDuration(), DozeScrimController.this.mPulseOutInterpolator, 0L, DozeScrimController.this.mPulseOutFinished);
            }
        }
    };
    private final Runnable mPulseOutFinished = new Runnable() {
        @Override
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse out finished");
            }
            DozeLog.tracePulseFinish();
            DozeScrimController.this.pulseFinished();
        }
    };

    public DozeScrimController(ScrimController scrimController, Context context) {
        this.mScrimController = scrimController;
        this.mDozeParameters = new DozeParameters(context);
        Interpolator interpolatorLoadInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.linear_out_slow_in);
        this.mPulseInInterpolatorPickup = interpolatorLoadInterpolator;
        this.mDozeAnimationInterpolator = interpolatorLoadInterpolator;
    }

    public void setDozing(boolean dozing, boolean animate) {
        if (this.mDozing != dozing) {
            this.mDozing = dozing;
            if (this.mDozing) {
                abortAnimations();
                this.mScrimController.setDozeBehindAlpha(1.0f);
                this.mScrimController.setDozeInFrontAlpha(1.0f);
                return;
            }
            cancelPulsing();
            if (animate) {
                startScrimAnimation(false, 0.0f, 700L, this.mDozeAnimationInterpolator);
                startScrimAnimation(true, 0.0f, 700L, this.mDozeAnimationInterpolator);
            } else {
                abortAnimations();
                this.mScrimController.setDozeBehindAlpha(0.0f);
                this.mScrimController.setDozeInFrontAlpha(0.0f);
            }
        }
    }

    public void pulse(DozeHost.PulseCallback callback, int reason) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        if (!this.mDozing || this.mPulseCallback != null) {
            callback.onPulseFinished();
            return;
        }
        this.mPulseCallback = callback;
        this.mPulseReason = reason;
        this.mHandler.post(this.mPulseIn);
    }

    public boolean isPulsing() {
        return this.mPulseCallback != null;
    }

    private void cancelPulsing() {
        if (DEBUG) {
            Log.d("DozeScrimController", "Cancel pulsing");
        }
        if (this.mPulseCallback != null) {
            this.mHandler.removeCallbacks(this.mPulseIn);
            this.mHandler.removeCallbacks(this.mPulseOut);
            pulseFinished();
        }
    }

    private void pulseStarted() {
        if (this.mPulseCallback != null) {
            this.mPulseCallback.onPulseStarted();
        }
    }

    private void pulseFinished() {
        if (this.mPulseCallback != null) {
            this.mPulseCallback.onPulseFinished();
            this.mPulseCallback = null;
        }
    }

    private void abortAnimations() {
        if (this.mInFrontAnimator != null) {
            this.mInFrontAnimator.cancel();
        }
        if (this.mBehindAnimator != null) {
            this.mBehindAnimator.cancel();
        }
    }

    private void startScrimAnimation(boolean inFront, float target, long duration, Interpolator interpolator) {
        startScrimAnimation(inFront, target, duration, interpolator, 0L, null);
    }

    private void startScrimAnimation(final boolean inFront, float target, long duration, Interpolator interpolator, long delay, final Runnable endRunnable) {
        Animator current = getCurrentAnimator(inFront);
        if (current != null) {
            float currentTarget = getCurrentTarget(inFront);
            if (currentTarget != target) {
                current.cancel();
            } else {
                return;
            }
        }
        ValueAnimator anim = ValueAnimator.ofFloat(getDozeAlpha(inFront), target);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) animation.getAnimatedValue()).floatValue();
                DozeScrimController.this.setDozeAlpha(inFront, value);
            }
        });
        anim.setInterpolator(interpolator);
        anim.setDuration(duration);
        anim.setStartDelay(delay);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                DozeScrimController.this.setCurrentAnimator(inFront, null);
                if (endRunnable != null) {
                    endRunnable.run();
                }
            }
        });
        anim.start();
        setCurrentAnimator(inFront, anim);
        setCurrentTarget(inFront, target);
    }

    private float getCurrentTarget(boolean inFront) {
        return inFront ? this.mInFrontTarget : this.mBehindTarget;
    }

    private void setCurrentTarget(boolean inFront, float target) {
        if (inFront) {
            this.mInFrontTarget = target;
        } else {
            this.mBehindTarget = target;
        }
    }

    private Animator getCurrentAnimator(boolean inFront) {
        return inFront ? this.mInFrontAnimator : this.mBehindAnimator;
    }

    private void setCurrentAnimator(boolean inFront, Animator animator) {
        if (inFront) {
            this.mInFrontAnimator = animator;
        } else {
            this.mBehindAnimator = animator;
        }
    }

    private void setDozeAlpha(boolean inFront, float alpha) {
        if (inFront) {
            this.mScrimController.setDozeInFrontAlpha(alpha);
        } else {
            this.mScrimController.setDozeBehindAlpha(alpha);
        }
    }

    private float getDozeAlpha(boolean inFront) {
        return inFront ? this.mScrimController.getDozeInFrontAlpha() : this.mScrimController.getDozeBehindAlpha();
    }
}
