package com.android.server.wm;

import android.R;
import android.animation.AnimationHandler;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wm.BoundsAnimationController;
import com.android.server.wm.WindowManagerInternal;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class BoundsAnimationController {
    private static final boolean DEBUG = WindowManagerDebugConfig.DEBUG_ANIM;
    private static final int DEBUG_ANIMATION_SLOW_DOWN_FACTOR = 1;
    private static final boolean DEBUG_LOCAL = false;
    private static final int DEFAULT_TRANSITION_DURATION = 425;
    public static final int NO_PIP_MODE_CHANGED_CALLBACKS = 0;
    public static final int SCHEDULE_PIP_MODE_CHANGED_ON_END = 2;
    public static final int SCHEDULE_PIP_MODE_CHANGED_ON_START = 1;
    private static final String TAG = "WindowManager";
    private static final int WAIT_FOR_DRAW_TIMEOUT_MS = 3000;
    private final AnimationHandler mAnimationHandler;
    private final AppTransition mAppTransition;
    private final Interpolator mFastOutSlowInInterpolator;
    private final Handler mHandler;
    private ArrayMap<BoundsAnimationTarget, BoundsAnimator> mRunningAnimations = new ArrayMap<>();
    private final AppTransitionNotifier mAppTransitionNotifier = new AppTransitionNotifier();
    private boolean mFinishAnimationAfterTransition = false;

    @Retention(RetentionPolicy.SOURCE)
    public @interface SchedulePipModeChangedState {
    }

    private final class AppTransitionNotifier extends WindowManagerInternal.AppTransitionListener implements Runnable {
        private AppTransitionNotifier() {
        }

        public void onAppTransitionCancelledLocked() {
            if (BoundsAnimationController.DEBUG) {
                Slog.d("WindowManager", "onAppTransitionCancelledLocked: mFinishAnimationAfterTransition=" + BoundsAnimationController.this.mFinishAnimationAfterTransition);
            }
            animationFinished();
        }

        @Override
        public void onAppTransitionFinishedLocked(IBinder iBinder) {
            if (BoundsAnimationController.DEBUG) {
                Slog.d("WindowManager", "onAppTransitionFinishedLocked: mFinishAnimationAfterTransition=" + BoundsAnimationController.this.mFinishAnimationAfterTransition);
            }
            animationFinished();
        }

        private void animationFinished() {
            if (BoundsAnimationController.this.mFinishAnimationAfterTransition) {
                BoundsAnimationController.this.mHandler.removeCallbacks(this);
                BoundsAnimationController.this.mHandler.post(this);
            }
        }

        @Override
        public void run() {
            for (int i = 0; i < BoundsAnimationController.this.mRunningAnimations.size(); i++) {
                ((BoundsAnimator) BoundsAnimationController.this.mRunningAnimations.valueAt(i)).onAnimationEnd(null);
            }
        }
    }

    BoundsAnimationController(Context context, AppTransition appTransition, Handler handler, AnimationHandler animationHandler) {
        this.mHandler = handler;
        this.mAppTransition = appTransition;
        this.mAppTransition.registerListenerLocked(this.mAppTransitionNotifier);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_slow_in);
        this.mAnimationHandler = animationHandler;
    }

    @VisibleForTesting
    final class BoundsAnimator extends ValueAnimator implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        private final int mFrozenTaskHeight;
        private final int mFrozenTaskWidth;
        private boolean mMoveFromFullscreen;
        private boolean mMoveToFullscreen;
        private int mPrevSchedulePipModeChangedState;
        private int mSchedulePipModeChangedState;
        private boolean mSkipAnimationEnd;
        private boolean mSkipFinalResize;
        private final BoundsAnimationTarget mTarget;
        private final Rect mFrom = new Rect();
        private final Rect mTo = new Rect();
        private final Rect mTmpRect = new Rect();
        private final Rect mTmpTaskBounds = new Rect();
        private final Runnable mResumeRunnable = new Runnable() {
            @Override
            public final void run() {
                BoundsAnimationController.BoundsAnimator.lambda$new$0(this.f$0);
            }
        };

        public static void lambda$new$0(BoundsAnimator boundsAnimator) {
            if (BoundsAnimationController.DEBUG) {
                Slog.d("WindowManager", "pause: timed out waiting for windows drawn");
            }
            boundsAnimator.resume();
        }

        BoundsAnimator(BoundsAnimationTarget boundsAnimationTarget, Rect rect, Rect rect2, int i, int i2, boolean z, boolean z2) {
            this.mTarget = boundsAnimationTarget;
            this.mFrom.set(rect);
            this.mTo.set(rect2);
            this.mSchedulePipModeChangedState = i;
            this.mPrevSchedulePipModeChangedState = i2;
            this.mMoveFromFullscreen = z;
            this.mMoveToFullscreen = z2;
            addUpdateListener(this);
            addListener(this);
            if (animatingToLargerSize()) {
                this.mFrozenTaskWidth = this.mTo.width();
                this.mFrozenTaskHeight = this.mTo.height();
            } else {
                this.mFrozenTaskWidth = this.mFrom.width();
                this.mFrozenTaskHeight = this.mFrom.height();
            }
        }

        @Override
        public void onAnimationStart(Animator animator) {
            if (BoundsAnimationController.DEBUG) {
                Slog.d("WindowManager", "onAnimationStart: mTarget=" + this.mTarget + " mPrevSchedulePipModeChangedState=" + this.mPrevSchedulePipModeChangedState + " mSchedulePipModeChangedState=" + this.mSchedulePipModeChangedState);
            }
            BoundsAnimationController.this.mFinishAnimationAfterTransition = false;
            this.mTmpRect.set(this.mFrom.left, this.mFrom.top, this.mFrom.left + this.mFrozenTaskWidth, this.mFrom.top + this.mFrozenTaskHeight);
            BoundsAnimationController.this.updateBooster();
            if (this.mPrevSchedulePipModeChangedState == 0) {
                this.mTarget.onAnimationStart(this.mSchedulePipModeChangedState == 1, false);
                if (this.mMoveFromFullscreen && this.mTarget.shouldDeferStartOnMoveToFullscreen()) {
                    pause();
                }
            } else if (this.mPrevSchedulePipModeChangedState == 2 && this.mSchedulePipModeChangedState == 1) {
                this.mTarget.onAnimationStart(true, true);
            }
            if (animatingToLargerSize()) {
                this.mTarget.setPinnedStackSize(this.mFrom, this.mTmpRect);
                if (this.mMoveToFullscreen) {
                    pause();
                }
            }
        }

        @Override
        public void pause() {
            if (BoundsAnimationController.DEBUG) {
                Slog.d("WindowManager", "pause: waiting for windows drawn");
            }
            super.pause();
            BoundsAnimationController.this.mHandler.postDelayed(this.mResumeRunnable, 3000L);
        }

        @Override
        public void resume() {
            if (BoundsAnimationController.DEBUG) {
                Slog.d("WindowManager", "resume:");
            }
            BoundsAnimationController.this.mHandler.removeCallbacks(this.mResumeRunnable);
            super.resume();
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            float f = 1.0f - fFloatValue;
            this.mTmpRect.left = (int) ((this.mFrom.left * f) + (this.mTo.left * fFloatValue) + 0.5f);
            this.mTmpRect.top = (int) ((this.mFrom.top * f) + (this.mTo.top * fFloatValue) + 0.5f);
            this.mTmpRect.right = (int) ((this.mFrom.right * f) + (this.mTo.right * fFloatValue) + 0.5f);
            this.mTmpRect.bottom = (int) ((this.mFrom.bottom * f) + (this.mTo.bottom * fFloatValue) + 0.5f);
            if (BoundsAnimationController.DEBUG) {
                Slog.d("WindowManager", "animateUpdate: mTarget=" + this.mTarget + " mBounds=" + this.mTmpRect + " from=" + this.mFrom + " mTo=" + this.mTo + " value=" + fFloatValue + " remains=" + f);
            }
            this.mTmpTaskBounds.set(this.mTmpRect.left, this.mTmpRect.top, this.mTmpRect.left + this.mFrozenTaskWidth, this.mTmpRect.top + this.mFrozenTaskHeight);
            if (!this.mTarget.setPinnedStackSize(this.mTmpRect, this.mTmpTaskBounds)) {
                if (BoundsAnimationController.DEBUG) {
                    Slog.d("WindowManager", "animateUpdate: cancelled");
                }
                if (this.mSchedulePipModeChangedState == 1) {
                    this.mSchedulePipModeChangedState = 2;
                }
                cancelAndCallAnimationEnd();
            }
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (BoundsAnimationController.DEBUG) {
                Slog.d("WindowManager", "onAnimationEnd: mTarget=" + this.mTarget + " mSkipFinalResize=" + this.mSkipFinalResize + " mFinishAnimationAfterTransition=" + BoundsAnimationController.this.mFinishAnimationAfterTransition + " mAppTransitionIsRunning=" + BoundsAnimationController.this.mAppTransition.isRunning() + " callers=" + Debug.getCallers(2));
            }
            if (BoundsAnimationController.this.mAppTransition.isRunning() && !BoundsAnimationController.this.mFinishAnimationAfterTransition) {
                BoundsAnimationController.this.mFinishAnimationAfterTransition = true;
                return;
            }
            if (!this.mSkipAnimationEnd) {
                if (BoundsAnimationController.DEBUG) {
                    Slog.d("WindowManager", "onAnimationEnd: mTarget=" + this.mTarget + " moveToFullscreen=" + this.mMoveToFullscreen);
                }
                this.mTarget.onAnimationEnd(this.mSchedulePipModeChangedState == 2, !this.mSkipFinalResize ? this.mTo : null, this.mMoveToFullscreen);
            }
            removeListener(this);
            removeUpdateListener(this);
            BoundsAnimationController.this.mRunningAnimations.remove(this.mTarget);
            BoundsAnimationController.this.updateBooster();
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            this.mSkipFinalResize = true;
            this.mMoveToFullscreen = false;
        }

        private void cancelAndCallAnimationEnd() {
            if (BoundsAnimationController.DEBUG) {
                Slog.d("WindowManager", "cancelAndCallAnimationEnd: mTarget=" + this.mTarget);
            }
            this.mSkipAnimationEnd = false;
            super.cancel();
        }

        @Override
        public void cancel() {
            if (BoundsAnimationController.DEBUG) {
                Slog.d("WindowManager", "cancel: mTarget=" + this.mTarget);
            }
            this.mSkipAnimationEnd = true;
            super.cancel();
        }

        boolean isAnimatingTo(Rect rect) {
            return this.mTo.equals(rect);
        }

        @VisibleForTesting
        boolean animatingToLargerSize() {
            return this.mFrom.width() * this.mFrom.height() <= this.mTo.width() * this.mTo.height();
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }

        public AnimationHandler getAnimationHandler() {
            if (BoundsAnimationController.this.mAnimationHandler != null) {
                return BoundsAnimationController.this.mAnimationHandler;
            }
            return super.getAnimationHandler();
        }
    }

    public void animateBounds(BoundsAnimationTarget boundsAnimationTarget, Rect rect, Rect rect2, int i, int i2, boolean z, boolean z2) {
        animateBoundsImpl(boundsAnimationTarget, rect, rect2, i, i2, z, z2);
    }

    @VisibleForTesting
    BoundsAnimator animateBoundsImpl(BoundsAnimationTarget boundsAnimationTarget, Rect rect, Rect rect2, int i, int i2, boolean z, boolean z2) {
        Rect rect3;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        int i3 = i2;
        BoundsAnimator boundsAnimator = this.mRunningAnimations.get(boundsAnimationTarget);
        int i4 = 0;
        boolean z7 = boundsAnimator != null;
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("animateBounds: target=");
            sb.append(boundsAnimationTarget);
            sb.append(" from=");
            rect3 = rect;
            sb.append(rect3);
            sb.append(" to=");
            sb.append(rect2);
            sb.append(" schedulePipModeChangedState=");
            sb.append(i3);
            sb.append(" replacing=");
            sb.append(z7);
            Slog.d("WindowManager", sb.toString());
        } else {
            rect3 = rect;
        }
        if (!z7) {
            z3 = z;
            z4 = z2;
        } else if (!boundsAnimator.isAnimatingTo(rect2) || ((z2 && !boundsAnimator.mMoveToFullscreen) || (z && !boundsAnimator.mMoveFromFullscreen))) {
            i4 = boundsAnimator.mSchedulePipModeChangedState;
            if (boundsAnimator.mSchedulePipModeChangedState != 1) {
                if (boundsAnimator.mSchedulePipModeChangedState == 2) {
                    if (i3 == 1) {
                        if (DEBUG) {
                            Slog.d("WindowManager", "animateBounds: non-fullscreen animation canceled, callback on start will be processed");
                        }
                    } else {
                        if (DEBUG) {
                            Slog.d("WindowManager", "animateBounds: still animating from fullscreen, keep existing deferred state");
                        }
                        i3 = 2;
                    }
                }
                if (z) {
                }
            } else if (i3 == 1) {
                if (DEBUG) {
                    Slog.d("WindowManager", "animateBounds: still animating to fullscreen, keep existing deferred state");
                }
                if (z || z2) {
                    z5 = z;
                    z6 = z2;
                } else {
                    z6 = boundsAnimator.mMoveToFullscreen;
                    z5 = boundsAnimator.mMoveFromFullscreen;
                }
                boundsAnimator.cancel();
                z3 = z5;
                z4 = z6;
            } else {
                if (DEBUG) {
                    Slog.d("WindowManager", "animateBounds: fullscreen animation canceled, callback on start already processed, schedule deferred update on end");
                }
                i3 = 2;
                if (z) {
                    z5 = z;
                    z6 = z2;
                    boundsAnimator.cancel();
                    z3 = z5;
                    z4 = z6;
                }
            }
        } else {
            if (DEBUG) {
                Slog.d("WindowManager", "animateBounds: same destination and moveTo/From flags as existing=" + boundsAnimator + ", ignoring...");
            }
            return boundsAnimator;
        }
        BoundsAnimator boundsAnimator2 = new BoundsAnimator(boundsAnimationTarget, rect3, rect2, i3, i4, z3, z4);
        this.mRunningAnimations.put(boundsAnimationTarget, boundsAnimator2);
        boundsAnimator2.setFloatValues(0.0f, 1.0f);
        boundsAnimator2.setDuration((i != -1 ? i : DEFAULT_TRANSITION_DURATION) * 1);
        boundsAnimator2.setInterpolator(this.mFastOutSlowInInterpolator);
        boundsAnimator2.start();
        return boundsAnimator2;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public void onAllWindowsDrawn() {
        if (DEBUG) {
            Slog.d("WindowManager", "onAllWindowsDrawn:");
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.resume();
            }
        });
    }

    private void resume() {
        for (int i = 0; i < this.mRunningAnimations.size(); i++) {
            this.mRunningAnimations.valueAt(i).resume();
        }
    }

    private void updateBooster() {
        WindowManagerService.sThreadPriorityBooster.setBoundsAnimationRunning(!this.mRunningAnimations.isEmpty());
    }
}
