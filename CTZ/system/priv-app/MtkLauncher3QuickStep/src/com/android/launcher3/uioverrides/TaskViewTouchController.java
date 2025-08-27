package com.android.launcher3.uioverrides;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.touch.SwipeDetector;
import com.android.launcher3.util.FlingBlockCheck;
import com.android.launcher3.util.PendingAnimation;
import com.android.launcher3.util.TouchController;
import com.android.launcher3.views.BaseDragLayer;
import com.android.quickstep.OverviewInteractionState;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;

/* loaded from: classes.dex */
public abstract class TaskViewTouchController<T extends BaseDraggingActivity> extends AnimatorListenerAdapter implements TouchController, SwipeDetector.Listener {
    private static final float SUCCESS_TRANSITION_PROGRESS = 0.5f;
    private static final String TAG = "OverviewSwipeController";
    protected final T mActivity;
    private AnimatorPlaybackController mCurrentAnimation;
    private boolean mCurrentAnimationIsGoingUp;
    private final SwipeDetector mDetector;
    private float mDisplacementShift;
    private float mEndDisplacement;
    private boolean mNoIntercept;
    private PendingAnimation mPendingAnimation;
    private float mProgressMultiplier;
    private final RecentsView mRecentsView;
    private TaskView mTaskBeingDragged;
    private final int[] mTempCords = new int[2];
    private FlingBlockCheck mFlingBlockCheck = new FlingBlockCheck();

    protected abstract boolean isRecentsInteractive();

    public TaskViewTouchController(T t) {
        this.mActivity = t;
        this.mRecentsView = (RecentsView) t.getOverviewPanel();
        this.mDetector = new SwipeDetector(t, this, SwipeDetector.VERTICAL);
    }

    private boolean canInterceptTouch() {
        if (this.mCurrentAnimation != null) {
            return true;
        }
        if (AbstractFloatingView.getTopOpenView(this.mActivity) != null) {
            return false;
        }
        return isRecentsInteractive();
    }

    protected void onUserControlledAnimationCreated(AnimatorPlaybackController animatorPlaybackController) {
    }

    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
    public void onAnimationCancel(Animator animator) {
        if (this.mCurrentAnimation != null && animator == this.mCurrentAnimation.getTarget()) {
            clearState();
        }
    }

    @Override // com.android.launcher3.util.TouchController
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            boolean z = true;
            this.mNoIntercept = !canInterceptTouch();
            if (this.mNoIntercept) {
                return false;
            }
            int i = 3;
            if (this.mCurrentAnimation == null) {
                this.mTaskBeingDragged = null;
                int i2 = 0;
                while (true) {
                    if (i2 < this.mRecentsView.getChildCount()) {
                        TaskView pageAt = this.mRecentsView.getPageAt(i2);
                        if (!this.mRecentsView.isTaskViewVisible(pageAt) || !this.mActivity.getDragLayer().isEventOverView(pageAt, motionEvent)) {
                            i2++;
                        } else {
                            this.mTaskBeingDragged = pageAt;
                            if (!OverviewInteractionState.getInstance(this.mActivity).isSwipeUpGestureEnabled() || i2 != this.mRecentsView.getCurrentPage()) {
                                i = 1;
                            }
                        }
                    } else {
                        i = 0;
                        break;
                    }
                }
                if (this.mTaskBeingDragged == null) {
                    this.mNoIntercept = true;
                    return false;
                }
                z = false;
            }
            this.mDetector.setDetectableScrollConditions(i, z);
        }
        if (this.mNoIntercept) {
            return false;
        }
        onControllerTouchEvent(motionEvent);
        return this.mDetector.isDraggingOrSettling();
    }

    @Override // com.android.launcher3.util.TouchController
    public boolean onControllerTouchEvent(MotionEvent motionEvent) {
        return this.mDetector.onTouchEvent(motionEvent);
    }

    private void reInitAnimationController(boolean z) {
        if (this.mCurrentAnimation != null && this.mCurrentAnimationIsGoingUp == z) {
            return;
        }
        int scrollDirections = this.mDetector.getScrollDirections();
        if (z && (scrollDirections & 1) == 0) {
            return;
        }
        if (!z && (scrollDirections & 2) == 0) {
            return;
        }
        if (this.mCurrentAnimation != null) {
            this.mCurrentAnimation.setPlayFraction(0.0f);
        }
        if (this.mPendingAnimation != null) {
            this.mPendingAnimation.finish(false, 3);
            this.mPendingAnimation = null;
        }
        this.mCurrentAnimationIsGoingUp = z;
        BaseDragLayer dragLayer = this.mActivity.getDragLayer();
        long height = 2 * dragLayer.getHeight();
        if (z) {
            this.mPendingAnimation = this.mRecentsView.createTaskDismissAnimation(this.mTaskBeingDragged, true, true, height);
            this.mEndDisplacement = -this.mTaskBeingDragged.getHeight();
        } else {
            this.mPendingAnimation = this.mRecentsView.createTaskLauncherAnimation(this.mTaskBeingDragged, height);
            this.mPendingAnimation.anim.setInterpolator(Interpolators.ZOOM_IN);
            this.mTempCords[1] = this.mTaskBeingDragged.getHeight();
            dragLayer.getDescendantCoordRelativeToSelf(this.mTaskBeingDragged, this.mTempCords);
            this.mEndDisplacement = dragLayer.getHeight() - this.mTempCords[1];
        }
        if (this.mCurrentAnimation != null) {
            this.mCurrentAnimation.setOnCancelRunnable(null);
        }
        this.mCurrentAnimation = AnimatorPlaybackController.wrap(this.mPendingAnimation.anim, height, new Runnable() { // from class: com.android.launcher3.uioverrides.-$$Lambda$TaskViewTouchController$4GTuxdpgbIVgblH80yOIPyBpedc
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.clearState();
            }
        });
        onUserControlledAnimationCreated(this.mCurrentAnimation);
        this.mCurrentAnimation.getTarget().addListener(this);
        this.mCurrentAnimation.dispatchOnStart();
        this.mProgressMultiplier = 1.0f / this.mEndDisplacement;
    }

    @Override // com.android.launcher3.touch.SwipeDetector.Listener
    public void onDragStart(boolean z) {
        if (this.mCurrentAnimation == null) {
            reInitAnimationController(this.mDetector.wasInitialTouchPositive());
            this.mDisplacementShift = 0.0f;
        } else {
            this.mDisplacementShift = this.mCurrentAnimation.getProgressFraction() / this.mProgressMultiplier;
            this.mCurrentAnimation.pause();
        }
        this.mFlingBlockCheck.unblockFling();
    }

    @Override // com.android.launcher3.touch.SwipeDetector.Listener
    public boolean onDrag(float f, float f2) {
        boolean z;
        float f3 = f + this.mDisplacementShift;
        if (f3 == 0.0f) {
            z = this.mCurrentAnimationIsGoingUp;
        } else if (f3 >= 0.0f) {
            z = false;
        } else {
            z = true;
        }
        if (z != this.mCurrentAnimationIsGoingUp) {
            reInitAnimationController(z);
            this.mFlingBlockCheck.blockFling();
        } else {
            this.mFlingBlockCheck.onEvent();
        }
        this.mCurrentAnimation.setPlayFraction(f3 * this.mProgressMultiplier);
        return true;
    }

    @Override // com.android.launcher3.touch.SwipeDetector.Listener
    public void onDragEnd(float f, boolean z) {
        final int i;
        final boolean z2;
        float f2;
        boolean z3 = z && this.mFlingBlockCheck.isBlocked();
        if (z3) {
            z = false;
        }
        if (z) {
            i = 4;
            z2 = ((f > 0.0f ? 1 : (f == 0.0f ? 0 : -1)) < 0) == this.mCurrentAnimationIsGoingUp;
        } else {
            i = 3;
            z2 = this.mCurrentAnimation.getProgressFraction() > 0.5f;
        }
        float progressFraction = this.mCurrentAnimation.getProgressFraction();
        if (!z2) {
            f2 = progressFraction;
        } else {
            f2 = 1.0f - progressFraction;
        }
        long jCalculateDuration = SwipeDetector.calculateDuration(f, f2);
        if (z3 && !z2) {
            jCalculateDuration *= LauncherAnimUtils.blockedFlingDurationFactor(f);
        }
        float fBoundToRange = Utilities.boundToRange(progressFraction + ((16.0f * f) / Math.abs(this.mEndDisplacement)), 0.0f, 1.0f);
        this.mCurrentAnimation.setEndAction(new Runnable() { // from class: com.android.launcher3.uioverrides.-$$Lambda$TaskViewTouchController$lRDfqKw2NqVZoHwkdXukBoQMYTs
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.onCurrentAnimationEnd(z2, i);
            }
        });
        ValueAnimator animationPlayer = this.mCurrentAnimation.getAnimationPlayer();
        float[] fArr = new float[2];
        fArr[0] = fBoundToRange;
        fArr[1] = z2 ? 1.0f : 0.0f;
        animationPlayer.setFloatValues(fArr);
        animationPlayer.setDuration(jCalculateDuration);
        animationPlayer.setInterpolator(Interpolators.scrollInterpolatorForVelocity(f));
        animationPlayer.start();
    }

    private void onCurrentAnimationEnd(boolean z, int i) {
        if (this.mPendingAnimation != null) {
            this.mPendingAnimation.finish(z, i);
            this.mPendingAnimation = null;
        }
        clearState();
    }

    private void clearState() {
        this.mDetector.finishedScrolling();
        this.mDetector.setDetectableScrollConditions(0, false);
        this.mTaskBeingDragged = null;
        this.mCurrentAnimation = null;
        if (this.mPendingAnimation != null) {
            this.mPendingAnimation.finish(false, 3);
            this.mPendingAnimation = null;
        }
    }
}
