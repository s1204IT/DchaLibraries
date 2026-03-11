package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeLog;

public class DozeScrimController {
    private static final boolean DEBUG = Log.isLoggable("DozeScrimController", 3);
    private Animator mBehindAnimator;
    private float mBehindTarget;
    private final DozeParameters mDozeParameters;
    private boolean mDozing;
    private Animator mInFrontAnimator;
    private float mInFrontTarget;
    private DozeHost.PulseCallback mPulseCallback;
    private int mPulseReason;
    private final ScrimController mScrimController;
    private final Handler mHandler = new Handler();
    private final Runnable mPulseIn = new Runnable() {
        @Override
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse in, mDozing=" + DozeScrimController.this.mDozing + " mPulseReason=" + DozeLog.pulseReasonToString(DozeScrimController.this.mPulseReason));
            }
            if (DozeScrimController.this.mDozing) {
                DozeLog.tracePulseStart(DozeScrimController.this.mPulseReason);
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
                DozeScrimController.this.startScrimAnimation(true, 1.0f, DozeScrimController.this.mDozeParameters.getPulseOutDuration(), Interpolators.ALPHA_IN, DozeScrimController.this.mPulseOutFinished);
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
    }

    public void setDozing(boolean dozing, boolean animate) {
        if (this.mDozing == dozing) {
            return;
        }
        this.mDozing = dozing;
        if (this.mDozing) {
            abortAnimations();
            this.mScrimController.setDozeBehindAlpha(1.0f);
            this.mScrimController.setDozeInFrontAlpha(1.0f);
            return;
        }
        cancelPulsing();
        if (animate) {
            startScrimAnimation(false, 0.0f, 700L, Interpolators.LINEAR_OUT_SLOW_IN);
            startScrimAnimation(true, 0.0f, 700L, Interpolators.LINEAR_OUT_SLOW_IN);
        } else {
            abortAnimations();
            this.mScrimController.setDozeBehindAlpha(0.0f);
            this.mScrimController.setDozeInFrontAlpha(0.0f);
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

    public void abortPulsing() {
        cancelPulsing();
        if (!this.mDozing) {
            return;
        }
        this.mScrimController.setDozeBehindAlpha(1.0f);
        this.mScrimController.setDozeInFrontAlpha(1.0f);
    }

    public void onScreenTurnedOn() {
        if (!isPulsing()) {
            return;
        }
        boolean pickup = this.mPulseReason == 3;
        startScrimAnimation(true, 0.0f, this.mDozeParameters.getPulseInDuration(pickup), pickup ? Interpolators.LINEAR_OUT_SLOW_IN : Interpolators.ALPHA_OUT, this.mPulseInFinished);
    }

    public boolean isPulsing() {
        return this.mPulseCallback != null;
    }

    private void cancelPulsing() {
        if (DEBUG) {
            Log.d("DozeScrimController", "Cancel pulsing");
        }
        if (this.mPulseCallback == null) {
            return;
        }
        this.mHandler.removeCallbacks(this.mPulseIn);
        this.mHandler.removeCallbacks(this.mPulseOut);
        pulseFinished();
    }

    public void pulseStarted() {
        if (this.mPulseCallback == null) {
            return;
        }
        this.mPulseCallback.onPulseStarted();
    }

    public void pulseFinished() {
        if (this.mPulseCallback == null) {
            return;
        }
        this.mPulseCallback.onPulseFinished();
        this.mPulseCallback = null;
    }

    private void abortAnimations() {
        if (this.mInFrontAnimator != null) {
            this.mInFrontAnimator.cancel();
        }
        if (this.mBehindAnimator == null) {
            return;
        }
        this.mBehindAnimator.cancel();
    }

    private void startScrimAnimation(boolean inFront, float target, long duration, Interpolator interpolator) {
        startScrimAnimation(inFront, target, duration, interpolator, null);
    }

    public void startScrimAnimation(final boolean inFront, float target, long duration, Interpolator interpolator, final Runnable endRunnable) {
        Animator current = getCurrentAnimator(inFront);
        if (current != null) {
            float currentTarget = getCurrentTarget(inFront);
            if (currentTarget == target) {
                return;
            } else {
                current.cancel();
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
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                DozeScrimController.this.setCurrentAnimator(inFront, null);
                if (endRunnable == null) {
                    return;
                }
                endRunnable.run();
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

    public void setCurrentAnimator(boolean inFront, Animator animator) {
        if (inFront) {
            this.mInFrontAnimator = animator;
        } else {
            this.mBehindAnimator = animator;
        }
    }

    public void setDozeAlpha(boolean inFront, float alpha) {
        if (inFront) {
            this.mScrimController.setDozeInFrontAlpha(alpha);
        } else {
            this.mScrimController.setDozeBehindAlpha(alpha);
        }
    }

    private float getDozeAlpha(boolean inFront) {
        if (inFront) {
            return this.mScrimController.getDozeInFrontAlpha();
        }
        return this.mScrimController.getDozeBehindAlpha();
    }
}
