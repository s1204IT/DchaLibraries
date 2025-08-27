package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;

/* loaded from: classes.dex */
public class FirstFrameAnimatorHelper extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
    private static final boolean DEBUG = false;
    private static final int MAX_DELAY = 1000;
    private static final String TAG = "FirstFrameAnimatorHlpr";
    private static ViewTreeObserver.OnDrawListener sGlobalDrawListener;
    static long sGlobalFrameCounter;
    private static boolean sVisible;
    private boolean mAdjustedSecondFrameTime;
    private boolean mHandlingOnAnimationUpdate;
    private long mStartFrame;
    private long mStartTime = -1;
    private final View mTarget;

    public FirstFrameAnimatorHelper(ValueAnimator valueAnimator, View view) {
        this.mTarget = view;
        valueAnimator.addUpdateListener(this);
    }

    public FirstFrameAnimatorHelper(ViewPropertyAnimator viewPropertyAnimator, View view) {
        this.mTarget = view;
        viewPropertyAnimator.setListener(this);
    }

    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
    public void onAnimationStart(Animator animator) {
        ValueAnimator valueAnimator = (ValueAnimator) animator;
        valueAnimator.addUpdateListener(this);
        onAnimationUpdate(valueAnimator);
    }

    public static void setIsVisible(boolean z) {
        sVisible = z;
    }

    public static void initializeDrawListener(View view) {
        if (sGlobalDrawListener != null) {
            view.getViewTreeObserver().removeOnDrawListener(sGlobalDrawListener);
        }
        sGlobalDrawListener = new ViewTreeObserver.OnDrawListener() { // from class: com.android.launcher3.-$$Lambda$FirstFrameAnimatorHelper$Rt9GS5WQ2aT33ZHWNuz2uhuk63s
            @Override // android.view.ViewTreeObserver.OnDrawListener
            public final void onDraw() {
                FirstFrameAnimatorHelper.lambda$initializeDrawListener$0();
            }
        };
        view.getViewTreeObserver().addOnDrawListener(sGlobalDrawListener);
        sVisible = true;
    }

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: ?: ARITH (wrap:long:0x0000: SGET  A[WRAPPED] (LINE:76) com.android.launcher3.FirstFrameAnimatorHelper.sGlobalFrameCounter long) += (1 long) A[ARITH_ONEARG] */
    static /* synthetic */ void lambda$initializeDrawListener$0() {
        sGlobalFrameCounter++;
    }

    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
    public void onAnimationUpdate(final ValueAnimator valueAnimator) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.mStartTime == -1) {
            this.mStartFrame = sGlobalFrameCounter;
            this.mStartTime = jCurrentTimeMillis;
        }
        long currentPlayTime = valueAnimator.getCurrentPlayTime();
        boolean z = Float.compare(1.0f, valueAnimator.getAnimatedFraction()) == 0;
        if (!this.mHandlingOnAnimationUpdate && sVisible && currentPlayTime < valueAnimator.getDuration() && !z) {
            this.mHandlingOnAnimationUpdate = true;
            long j = sGlobalFrameCounter - this.mStartFrame;
            if (j == 0 && jCurrentTimeMillis < this.mStartTime + 1000 && currentPlayTime > 0) {
                this.mTarget.getRootView().invalidate();
                valueAnimator.setCurrentPlayTime(0L);
            } else if (j == 1 && jCurrentTimeMillis < this.mStartTime + 1000 && !this.mAdjustedSecondFrameTime && jCurrentTimeMillis > this.mStartTime + 16 && currentPlayTime > 16) {
                valueAnimator.setCurrentPlayTime(16L);
                this.mAdjustedSecondFrameTime = true;
            } else if (j > 1) {
                this.mTarget.post(new Runnable() { // from class: com.android.launcher3.FirstFrameAnimatorHelper.1
                    @Override // java.lang.Runnable
                    public void run() {
                        valueAnimator.removeUpdateListener(FirstFrameAnimatorHelper.this);
                    }
                });
            }
            this.mHandlingOnAnimationUpdate = false;
        }
    }

    public void print(ValueAnimator valueAnimator) {
        Log.d(TAG, sGlobalFrameCounter + "(" + (sGlobalFrameCounter - this.mStartFrame) + ") " + this.mTarget + " dirty? " + this.mTarget.isDirty() + " " + (valueAnimator.getCurrentPlayTime() / valueAnimator.getDuration()) + " " + this + " " + valueAnimator);
    }
}
