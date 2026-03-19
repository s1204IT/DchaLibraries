package com.android.server.wm;

import android.animation.AnimationHandler;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.ArrayMap;
import android.view.Choreographer;
import android.view.SurfaceControl;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.graphics.SfVsyncFrameCallbackProvider;
import com.android.server.AnimationThread;
import com.android.server.wm.LocalAnimationAdapter;

class SurfaceAnimationRunner {
    private final AnimationHandler mAnimationHandler;

    @GuardedBy("mLock")
    private boolean mAnimationStartDeferred;
    private final AnimatorFactory mAnimatorFactory;
    private boolean mApplyScheduled;
    private final Runnable mApplyTransactionRunnable;
    private final Object mCancelLock;

    @VisibleForTesting
    Choreographer mChoreographer;
    private final SurfaceControl.Transaction mFrameTransaction;
    private final Object mLock;

    @GuardedBy("mLock")
    @VisibleForTesting
    final ArrayMap<SurfaceControl, RunningAnimation> mPendingAnimations;

    @GuardedBy("mLock")
    @VisibleForTesting
    final ArrayMap<SurfaceControl, RunningAnimation> mRunningAnimations;

    @VisibleForTesting
    interface AnimatorFactory {
        ValueAnimator makeAnimator();
    }

    SurfaceAnimationRunner() {
        this(null, null, new SurfaceControl.Transaction());
    }

    @VisibleForTesting
    SurfaceAnimationRunner(AnimationHandler.AnimationFrameCallbackProvider animationFrameCallbackProvider, AnimatorFactory animatorFactory, SurfaceControl.Transaction transaction) {
        this.mLock = new Object();
        this.mCancelLock = new Object();
        this.mApplyTransactionRunnable = new Runnable() {
            @Override
            public final void run() {
                this.f$0.applyTransaction();
            }
        };
        this.mPendingAnimations = new ArrayMap<>();
        this.mRunningAnimations = new ArrayMap<>();
        SurfaceAnimationThread.getHandler().runWithScissors(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mChoreographer = Choreographer.getSfInstance();
            }
        }, 0L);
        this.mFrameTransaction = transaction;
        this.mAnimationHandler = new AnimationHandler();
        this.mAnimationHandler.setProvider(animationFrameCallbackProvider == null ? new SfVsyncFrameCallbackProvider(this.mChoreographer) : animationFrameCallbackProvider);
        this.mAnimatorFactory = animatorFactory == null ? new AnimatorFactory() {
            @Override
            public final ValueAnimator makeAnimator() {
                return SurfaceAnimationRunner.lambda$new$1(this.f$0);
            }
        } : animatorFactory;
    }

    public static ValueAnimator lambda$new$1(SurfaceAnimationRunner surfaceAnimationRunner) {
        return surfaceAnimationRunner.new SfValueAnimator();
    }

    void deferStartingAnimations() {
        synchronized (this.mLock) {
            this.mAnimationStartDeferred = true;
        }
    }

    void continueStartingAnimations() {
        synchronized (this.mLock) {
            this.mAnimationStartDeferred = false;
            if (!this.mPendingAnimations.isEmpty()) {
                this.mChoreographer.postFrameCallback(new $$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60(this));
            }
        }
    }

    void startAnimation(LocalAnimationAdapter.AnimationSpec animationSpec, SurfaceControl surfaceControl, SurfaceControl.Transaction transaction, Runnable runnable) {
        synchronized (this.mLock) {
            RunningAnimation runningAnimation = new RunningAnimation(animationSpec, surfaceControl, runnable);
            this.mPendingAnimations.put(surfaceControl, runningAnimation);
            if (!this.mAnimationStartDeferred) {
                this.mChoreographer.postFrameCallback(new $$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60(this));
            }
            applyTransformation(runningAnimation, transaction, 0L);
        }
    }

    void onAnimationCancelled(SurfaceControl surfaceControl) {
        synchronized (this.mLock) {
            if (this.mPendingAnimations.containsKey(surfaceControl)) {
                this.mPendingAnimations.remove(surfaceControl);
                return;
            }
            final RunningAnimation runningAnimation = this.mRunningAnimations.get(surfaceControl);
            if (runningAnimation != null) {
                this.mRunningAnimations.remove(surfaceControl);
                synchronized (this.mCancelLock) {
                    runningAnimation.mCancelled = true;
                }
                SurfaceAnimationThread.getHandler().post(new Runnable() {
                    @Override
                    public final void run() {
                        SurfaceAnimationRunner.lambda$onAnimationCancelled$2(this.f$0, runningAnimation);
                    }
                });
            }
        }
    }

    public static void lambda$onAnimationCancelled$2(SurfaceAnimationRunner surfaceAnimationRunner, RunningAnimation runningAnimation) {
        runningAnimation.mAnim.cancel();
        surfaceAnimationRunner.applyTransaction();
    }

    @GuardedBy("mLock")
    private void startPendingAnimationsLocked() {
        for (int size = this.mPendingAnimations.size() - 1; size >= 0; size--) {
            startAnimationLocked(this.mPendingAnimations.valueAt(size));
        }
        this.mPendingAnimations.clear();
    }

    @GuardedBy("mLock")
    private void startAnimationLocked(final RunningAnimation runningAnimation) {
        final ValueAnimator valueAnimatorMakeAnimator = this.mAnimatorFactory.makeAnimator();
        valueAnimatorMakeAnimator.overrideDurationScale(1.0f);
        valueAnimatorMakeAnimator.setDuration(runningAnimation.mAnimSpec.getDuration());
        valueAnimatorMakeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                SurfaceAnimationRunner.lambda$startAnimationLocked$3(this.f$0, runningAnimation, valueAnimatorMakeAnimator, valueAnimator);
            }
        });
        valueAnimatorMakeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                synchronized (SurfaceAnimationRunner.this.mCancelLock) {
                    if (!runningAnimation.mCancelled) {
                        SurfaceAnimationRunner.this.mFrameTransaction.show(runningAnimation.mLeash);
                    }
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                synchronized (SurfaceAnimationRunner.this.mLock) {
                    SurfaceAnimationRunner.this.mRunningAnimations.remove(runningAnimation.mLeash);
                    synchronized (SurfaceAnimationRunner.this.mCancelLock) {
                        if (!runningAnimation.mCancelled) {
                            AnimationThread.getHandler().post(runningAnimation.mFinishCallback);
                        }
                    }
                }
            }
        });
        runningAnimation.mAnim = valueAnimatorMakeAnimator;
        this.mRunningAnimations.put(runningAnimation.mLeash, runningAnimation);
        valueAnimatorMakeAnimator.start();
        if (runningAnimation.mAnimSpec.canSkipFirstFrame()) {
            valueAnimatorMakeAnimator.setCurrentPlayTime(this.mChoreographer.getFrameIntervalNanos() / 1000000);
        }
        valueAnimatorMakeAnimator.doAnimationFrame(this.mChoreographer.getFrameTime());
    }

    public static void lambda$startAnimationLocked$3(SurfaceAnimationRunner surfaceAnimationRunner, RunningAnimation runningAnimation, ValueAnimator valueAnimator, ValueAnimator valueAnimator2) {
        synchronized (surfaceAnimationRunner.mCancelLock) {
            if (!runningAnimation.mCancelled) {
                long duration = valueAnimator.getDuration();
                long currentPlayTime = valueAnimator.getCurrentPlayTime();
                if (currentPlayTime <= duration) {
                    duration = currentPlayTime;
                }
                surfaceAnimationRunner.applyTransformation(runningAnimation, surfaceAnimationRunner.mFrameTransaction, duration);
            }
        }
        surfaceAnimationRunner.scheduleApplyTransaction();
    }

    private void applyTransformation(RunningAnimation runningAnimation, SurfaceControl.Transaction transaction, long j) {
        if (runningAnimation.mAnimSpec.needsEarlyWakeup()) {
            transaction.setEarlyWakeup();
        }
        runningAnimation.mAnimSpec.apply(transaction, runningAnimation.mLeash, j);
    }

    private void startAnimations(long j) {
        synchronized (this.mLock) {
            startPendingAnimationsLocked();
        }
    }

    private void scheduleApplyTransaction() {
        if (!this.mApplyScheduled) {
            this.mChoreographer.postCallback(2, this.mApplyTransactionRunnable, null);
            this.mApplyScheduled = true;
        }
    }

    private void applyTransaction() {
        this.mFrameTransaction.setAnimationTransaction();
        this.mFrameTransaction.apply();
        this.mApplyScheduled = false;
    }

    private static final class RunningAnimation {
        ValueAnimator mAnim;
        final LocalAnimationAdapter.AnimationSpec mAnimSpec;

        @GuardedBy("mCancelLock")
        private boolean mCancelled;
        final Runnable mFinishCallback;
        final SurfaceControl mLeash;

        RunningAnimation(LocalAnimationAdapter.AnimationSpec animationSpec, SurfaceControl surfaceControl, Runnable runnable) {
            this.mAnimSpec = animationSpec;
            this.mLeash = surfaceControl;
            this.mFinishCallback = runnable;
        }
    }

    private class SfValueAnimator extends ValueAnimator {
        SfValueAnimator() {
            setFloatValues(0.0f, 1.0f);
        }

        public AnimationHandler getAnimationHandler() {
            return SurfaceAnimationRunner.this.mAnimationHandler;
        }
    }
}
