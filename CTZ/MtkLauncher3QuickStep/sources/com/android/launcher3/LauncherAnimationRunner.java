package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.os.Handler;
import android.support.annotation.BinderThread;
import android.support.annotation.UiThread;
import com.android.systemui.shared.system.RemoteAnimationRunnerCompat;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;

@TargetApi(28)
public abstract class LauncherAnimationRunner implements RemoteAnimationRunnerCompat {
    private AnimationResult mAnimationResult;
    private final Handler mHandler;
    private final boolean mStartAtFrontOfQueue;

    @UiThread
    public abstract void onCreateAnimation(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, AnimationResult animationResult);

    public LauncherAnimationRunner(Handler handler, boolean z) {
        this.mHandler = handler;
        this.mStartAtFrontOfQueue = z;
    }

    @Override
    @BinderThread
    public void onAnimationStart(final RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, final Runnable runnable) {
        Runnable runnable2 = new Runnable() {
            @Override
            public final void run() {
                LauncherAnimationRunner.lambda$onAnimationStart$0(this.f$0, runnable, remoteAnimationTargetCompatArr);
            }
        };
        if (this.mStartAtFrontOfQueue) {
            com.android.systemui.shared.recents.utilities.Utilities.postAtFrontOfQueueAsynchronously(this.mHandler, runnable2);
        } else {
            Utilities.postAsyncCallback(this.mHandler, runnable2);
        }
    }

    public static void lambda$onAnimationStart$0(LauncherAnimationRunner launcherAnimationRunner, Runnable runnable, RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
        launcherAnimationRunner.finishExistingAnimation();
        launcherAnimationRunner.mAnimationResult = new AnimationResult(runnable);
        launcherAnimationRunner.onCreateAnimation(remoteAnimationTargetCompatArr, launcherAnimationRunner.mAnimationResult);
    }

    @UiThread
    public void finishExistingAnimation() {
        if (this.mAnimationResult == null) {
            return;
        }
        this.mAnimationResult.finish();
        this.mAnimationResult = null;
    }

    @Override
    @BinderThread
    public void onAnimationCancelled() {
        Utilities.postAsyncCallback(this.mHandler, new Runnable() {
            @Override
            public final void run() {
                this.f$0.finishExistingAnimation();
            }
        });
    }

    public static final class AnimationResult {
        private AnimatorSet mAnimator;
        private final Runnable mFinishRunnable;
        private boolean mFinished;
        private boolean mInitialized;

        private AnimationResult(Runnable runnable) {
            this.mFinished = false;
            this.mInitialized = false;
            this.mFinishRunnable = runnable;
        }

        @UiThread
        public void finish() {
            if (!this.mFinished) {
                this.mFinishRunnable.run();
                this.mFinished = true;
            }
        }

        @UiThread
        public void setAnimation(AnimatorSet animatorSet) {
            if (this.mInitialized) {
                throw new IllegalStateException("Animation already initialized");
            }
            this.mInitialized = true;
            this.mAnimator = animatorSet;
            if (this.mAnimator == null) {
                finish();
                return;
            }
            if (this.mFinished) {
                this.mAnimator.start();
                this.mAnimator.end();
            } else {
                this.mAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        AnimationResult.this.finish();
                    }
                });
                this.mAnimator.start();
                this.mAnimator.setCurrentPlayTime(16L);
            }
        }
    }
}
