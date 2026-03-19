package com.android.server.wm;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.WindowManagerInternal;
import android.view.animation.LinearInterpolator;

public class BoundsAnimationController {
    private static final int DEBUG_ANIMATION_SLOW_DOWN_FACTOR = 1;
    private static final boolean DEBUG_LOCAL = false;
    private final AppTransition mAppTransition;
    private final Handler mHandler;
    private static final boolean DEBUG = WindowManagerDebugConfig.DEBUG_ANIM;
    private static final String TAG = "WindowManager";
    private ArrayMap<AnimateBoundsUser, BoundsAnimator> mRunningAnimations = new ArrayMap<>();
    private final AppTransitionNotifier mAppTransitionNotifier = new AppTransitionNotifier(this, null);
    private boolean mFinishAnimationAfterTransition = false;

    public interface AnimateBoundsUser {
        void getFullScreenBounds(Rect rect);

        void moveToFullscreen();

        void onAnimationEnd();

        void onAnimationStart();

        boolean setPinnedStackSize(Rect rect, Rect rect2);

        boolean setSize(Rect rect);
    }

    private final class AppTransitionNotifier extends WindowManagerInternal.AppTransitionListener implements Runnable {
        AppTransitionNotifier(BoundsAnimationController this$0, AppTransitionNotifier appTransitionNotifier) {
            this();
        }

        private AppTransitionNotifier() {
        }

        public void onAppTransitionCancelledLocked() {
            animationFinished();
        }

        public void onAppTransitionFinishedLocked(IBinder token) {
            animationFinished();
        }

        private void animationFinished() {
            if (!BoundsAnimationController.this.mFinishAnimationAfterTransition) {
                return;
            }
            BoundsAnimationController.this.mHandler.removeCallbacks(this);
            BoundsAnimationController.this.mHandler.post(this);
        }

        @Override
        public void run() {
            for (int i = 0; i < BoundsAnimationController.this.mRunningAnimations.size(); i++) {
                BoundsAnimator b = (BoundsAnimator) BoundsAnimationController.this.mRunningAnimations.valueAt(i);
                b.onAnimationEnd(null);
            }
        }
    }

    BoundsAnimationController(AppTransition transition, Handler handler) {
        this.mHandler = handler;
        this.mAppTransition = transition;
        this.mAppTransition.registerListenerLocked(this.mAppTransitionNotifier);
    }

    private final class BoundsAnimator extends ValueAnimator implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        private final Rect mFrom;
        private final int mFrozenTaskHeight;
        private final int mFrozenTaskWidth;
        private final boolean mMoveToFullScreen;
        private final boolean mReplacement;
        private final AnimateBoundsUser mTarget;
        private final Rect mTmpRect = new Rect();
        private final Rect mTmpTaskBounds = new Rect();
        private final Rect mTo;
        private boolean mWillReplace;

        BoundsAnimator(AnimateBoundsUser target, Rect from, Rect to, boolean moveToFullScreen, boolean replacement) {
            this.mTarget = target;
            this.mFrom = from;
            this.mTo = to;
            this.mMoveToFullScreen = moveToFullScreen;
            this.mReplacement = replacement;
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

        boolean animatingToLargerSize() {
            if (this.mFrom.width() * this.mFrom.height() > this.mTo.width() * this.mTo.height()) {
                return false;
            }
            return true;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float value = ((Float) animation.getAnimatedValue()).floatValue();
            float remains = 1.0f - value;
            this.mTmpRect.left = (int) ((this.mFrom.left * remains) + (this.mTo.left * value) + 0.5f);
            this.mTmpRect.top = (int) ((this.mFrom.top * remains) + (this.mTo.top * value) + 0.5f);
            this.mTmpRect.right = (int) ((this.mFrom.right * remains) + (this.mTo.right * value) + 0.5f);
            this.mTmpRect.bottom = (int) ((this.mFrom.bottom * remains) + (this.mTo.bottom * value) + 0.5f);
            if (BoundsAnimationController.DEBUG) {
                Slog.d(BoundsAnimationController.TAG, "animateUpdate: mTarget=" + this.mTarget + " mBounds=" + this.mTmpRect + " from=" + this.mFrom + " mTo=" + this.mTo + " value=" + value + " remains=" + remains);
            }
            this.mTmpTaskBounds.set(this.mTmpRect.left, this.mTmpRect.top, this.mTmpRect.left + this.mFrozenTaskWidth, this.mTmpRect.top + this.mFrozenTaskHeight);
            if (this.mTarget.setPinnedStackSize(this.mTmpRect, this.mTmpTaskBounds)) {
                return;
            }
            animation.cancel();
        }

        @Override
        public void onAnimationStart(Animator animation) {
            if (BoundsAnimationController.DEBUG) {
                Slog.d(BoundsAnimationController.TAG, "onAnimationStart: mTarget=" + this.mTarget + " mReplacement=" + this.mReplacement);
            }
            BoundsAnimationController.this.mFinishAnimationAfterTransition = false;
            if (!this.mReplacement) {
                this.mTarget.onAnimationStart();
            }
            if (!animatingToLargerSize()) {
                return;
            }
            this.mTmpRect.set(this.mFrom.left, this.mFrom.top, this.mFrom.left + this.mFrozenTaskWidth, this.mFrom.top + this.mFrozenTaskHeight);
            this.mTarget.setPinnedStackSize(this.mFrom, this.mTmpRect);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (BoundsAnimationController.DEBUG) {
                Slog.d(BoundsAnimationController.TAG, "onAnimationEnd: mTarget=" + this.mTarget + " mMoveToFullScreen=" + this.mMoveToFullScreen + " mWillReplace=" + this.mWillReplace);
            }
            if (BoundsAnimationController.this.mAppTransition.isRunning() && !BoundsAnimationController.this.mFinishAnimationAfterTransition) {
                BoundsAnimationController.this.mFinishAnimationAfterTransition = true;
                return;
            }
            finishAnimation();
            this.mTarget.setPinnedStackSize(this.mTo, null);
            if (!this.mMoveToFullScreen || this.mWillReplace) {
                return;
            }
            this.mTarget.moveToFullscreen();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            finishAnimation();
        }

        @Override
        public void cancel() {
            this.mWillReplace = true;
            if (BoundsAnimationController.DEBUG) {
                Slog.d(BoundsAnimationController.TAG, "cancel: willReplace mTarget=" + this.mTarget);
            }
            super.cancel();
        }

        public boolean isAnimatingTo(Rect bounds) {
            return this.mTo.equals(bounds);
        }

        private void finishAnimation() {
            if (BoundsAnimationController.DEBUG) {
                Slog.d(BoundsAnimationController.TAG, "finishAnimation: mTarget=" + this.mTarget + " callers" + Debug.getCallers(2));
            }
            if (!this.mWillReplace) {
                this.mTarget.onAnimationEnd();
            }
            removeListener(this);
            removeUpdateListener(this);
            BoundsAnimationController.this.mRunningAnimations.remove(this.mTarget);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }

    void animateBounds(AnimateBoundsUser target, Rect from, Rect to, int animationDuration) {
        boolean moveToFullscreen = false;
        if (to == null) {
            to = new Rect();
            target.getFullScreenBounds(to);
            moveToFullscreen = true;
        }
        BoundsAnimator existing = this.mRunningAnimations.get(target);
        boolean replacing = existing != null;
        if (DEBUG) {
            Slog.d(TAG, "animateBounds: target=" + target + " from=" + from + " to=" + to + " moveToFullscreen=" + moveToFullscreen + " replacing=" + replacing);
        }
        if (replacing) {
            if (existing.isAnimatingTo(to)) {
                if (DEBUG) {
                    Slog.d(TAG, "animateBounds: same destination as existing=" + existing + " ignoring...");
                    return;
                }
                return;
            }
            existing.cancel();
        }
        BoundsAnimator animator = new BoundsAnimator(target, from, to, moveToFullscreen, replacing);
        this.mRunningAnimations.put(target, animator);
        animator.setFloatValues(0.0f, 1.0f);
        if (animationDuration == -1) {
            animationDuration = 336;
        }
        animator.setDuration(animationDuration * 1);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }
}
