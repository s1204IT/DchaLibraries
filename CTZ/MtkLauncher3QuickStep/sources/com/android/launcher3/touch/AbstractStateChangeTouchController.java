package com.android.launcher3.touch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherStateManager;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.AnimationSuccessListener;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.touch.SwipeDetector;
import com.android.launcher3.util.FlingBlockCheck;
import com.android.launcher3.util.PendingAnimation;
import com.android.launcher3.util.TouchController;
import java.util.Iterator;

public abstract class AbstractStateChangeTouchController implements TouchController, SwipeDetector.Listener {
    protected static final long ATOMIC_DURATION = 200;
    public static final float ATOMIC_OVERVIEW_ANIM_THRESHOLD = 0.5f;
    public static final float SUCCESS_TRANSITION_PROGRESS = 0.5f;
    private static final String TAG = "ASCTouchController";
    private AnimatorSet mAtomicAnim;
    private AnimatorPlaybackController mAtomicComponentsController;
    private float mAtomicComponentsStartProgress;
    private boolean mCanBlockFling;
    protected AnimatorPlaybackController mCurrentAnimation;
    protected final SwipeDetector mDetector;
    private float mDisplacementShift;
    private FlingBlockCheck mFlingBlockCheck = new FlingBlockCheck();
    protected LauncherState mFromState;
    protected final Launcher mLauncher;
    private boolean mNoIntercept;
    private boolean mPassedOverviewAtomicThreshold;
    protected PendingAnimation mPendingAnimation;
    private float mProgressMultiplier;
    protected int mStartContainerType;
    private float mStartProgress;
    protected LauncherState mStartState;
    protected LauncherState mToState;

    protected abstract boolean canInterceptTouch(MotionEvent motionEvent);

    protected abstract int getLogContainerTypeForNormalState();

    protected abstract LauncherState getTargetState(LauncherState launcherState, boolean z);

    protected abstract float initCurrentAnimation(int i);

    public AbstractStateChangeTouchController(Launcher launcher, SwipeDetector.Direction direction) {
        this.mLauncher = launcher;
        this.mDetector = new SwipeDetector(launcher, this, direction);
    }

    @Override
    public final boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        int swipeDirection;
        if (motionEvent.getAction() == 0) {
            boolean z = true;
            this.mNoIntercept = !canInterceptTouch(motionEvent);
            if (this.mNoIntercept) {
                return false;
            }
            if (this.mCurrentAnimation != null) {
                swipeDirection = 3;
            } else {
                swipeDirection = getSwipeDirection();
                if (swipeDirection == 0) {
                    this.mNoIntercept = true;
                    return false;
                }
                z = false;
            }
            this.mDetector.setDetectableScrollConditions(swipeDirection, z);
        }
        if (this.mNoIntercept) {
            return false;
        }
        onControllerTouchEvent(motionEvent);
        return this.mDetector.isDraggingOrSettling();
    }

    private int getSwipeDirection() {
        LauncherState state = this.mLauncher.getStateManager().getState();
        int i = 1;
        if (getTargetState(state, true) == state) {
            i = 0;
        }
        if (getTargetState(state, false) != state) {
            return i | 2;
        }
        return i;
    }

    @Override
    public final boolean onControllerTouchEvent(MotionEvent motionEvent) {
        return this.mDetector.onTouchEvent(motionEvent);
    }

    protected float getShiftRange() {
        return this.mLauncher.getAllAppsController().getShiftRange();
    }

    private boolean reinitCurrentAnimation(boolean z, boolean z2) {
        LauncherState state;
        int i;
        if (this.mFromState == null) {
            state = this.mLauncher.getStateManager().getState();
        } else {
            state = z ? this.mToState : this.mFromState;
        }
        LauncherState targetState = getTargetState(state, z2);
        if ((state == this.mFromState && targetState == this.mToState) || state == targetState) {
            return false;
        }
        this.mFromState = state;
        this.mToState = targetState;
        this.mStartProgress = 0.0f;
        this.mPassedOverviewAtomicThreshold = false;
        if (this.mCurrentAnimation != null) {
            this.mCurrentAnimation.setOnCancelRunnable(null);
        }
        if (!goingBetweenNormalAndOverview(this.mFromState, this.mToState)) {
            i = 3;
        } else {
            i = 1;
        }
        if (this.mAtomicAnim != null) {
            this.mAtomicAnim.addListener(new AnimationSuccessListener() {
                @Override
                public void onAnimationSuccess(Animator animator) {
                    AbstractStateChangeTouchController.this.cancelAtomicComponentsController();
                    if (AbstractStateChangeTouchController.this.mCurrentAnimation != null) {
                        AbstractStateChangeTouchController.this.mAtomicComponentsStartProgress = AbstractStateChangeTouchController.this.mCurrentAnimation.getProgressFraction();
                        long shiftRange = (long) (AbstractStateChangeTouchController.this.getShiftRange() * 2.0f);
                        AbstractStateChangeTouchController.this.mAtomicComponentsController = AnimatorPlaybackController.wrap(AbstractStateChangeTouchController.this.createAtomicAnimForState(AbstractStateChangeTouchController.this.mFromState, AbstractStateChangeTouchController.this.mToState, shiftRange), shiftRange);
                        AbstractStateChangeTouchController.this.mAtomicComponentsController.dispatchOnStart();
                    }
                }
            });
            i = 1;
        }
        if (goingBetweenNormalAndOverview(this.mFromState, this.mToState)) {
            cancelAtomicComponentsController();
        }
        this.mProgressMultiplier = initCurrentAnimation(i);
        this.mCurrentAnimation.dispatchOnStart();
        return true;
    }

    private boolean goingBetweenNormalAndOverview(LauncherState launcherState, LauncherState launcherState2) {
        return (launcherState == LauncherState.NORMAL || launcherState == LauncherState.OVERVIEW) && (launcherState2 == LauncherState.NORMAL || launcherState2 == LauncherState.OVERVIEW) && this.mPendingAnimation == null;
    }

    @Override
    public void onDragStart(boolean z) {
        this.mStartState = this.mLauncher.getStateManager().getState();
        if (this.mStartState == LauncherState.ALL_APPS) {
            this.mStartContainerType = 4;
        } else if (this.mStartState == LauncherState.NORMAL) {
            this.mStartContainerType = getLogContainerTypeForNormalState();
        } else if (this.mStartState == LauncherState.OVERVIEW) {
            this.mStartContainerType = 12;
        }
        if (this.mCurrentAnimation == null) {
            this.mFromState = this.mStartState;
            this.mToState = null;
            this.mAtomicComponentsController = null;
            reinitCurrentAnimation(false, this.mDetector.wasInitialTouchPositive());
            this.mDisplacementShift = 0.0f;
        } else {
            this.mCurrentAnimation.pause();
            this.mStartProgress = this.mCurrentAnimation.getProgressFraction();
        }
        this.mCanBlockFling = this.mFromState == LauncherState.NORMAL;
        this.mFlingBlockCheck.unblockFling();
    }

    @Override
    public boolean onDrag(float f, float f2) {
        float f3 = (this.mProgressMultiplier * (f - this.mDisplacementShift)) + this.mStartProgress;
        updateProgress(f3);
        boolean z = f - this.mDisplacementShift < 0.0f;
        if (f3 <= 0.0f) {
            if (reinitCurrentAnimation(false, z)) {
                this.mDisplacementShift = f;
                if (this.mCanBlockFling) {
                    this.mFlingBlockCheck.blockFling();
                }
            }
        } else if (f3 >= 1.0f) {
            if (reinitCurrentAnimation(true, z)) {
                this.mDisplacementShift = f;
                if (this.mCanBlockFling) {
                    this.mFlingBlockCheck.blockFling();
                }
            }
        } else {
            this.mFlingBlockCheck.onEvent();
        }
        return true;
    }

    protected void updateProgress(float f) {
        this.mCurrentAnimation.setPlayFraction(f);
        if (this.mAtomicComponentsController != null) {
            this.mAtomicComponentsController.setPlayFraction(f - this.mAtomicComponentsStartProgress);
        }
        maybeUpdateAtomicAnim(this.mFromState, this.mToState, f);
    }

    private void maybeUpdateAtomicAnim(LauncherState launcherState, LauncherState launcherState2, float f) {
        if (!goingBetweenNormalAndOverview(launcherState, launcherState2)) {
            return;
        }
        if (launcherState2 == LauncherState.OVERVIEW) {
        }
        boolean z = f >= 0.5f;
        if (z != this.mPassedOverviewAtomicThreshold) {
            LauncherState launcherState3 = z ? launcherState : launcherState2;
            if (z) {
                launcherState = launcherState2;
            }
            this.mPassedOverviewAtomicThreshold = z;
            if (this.mAtomicAnim != null) {
                this.mAtomicAnim.cancel();
            }
            this.mAtomicAnim = createAtomicAnimForState(launcherState3, launcherState, ATOMIC_DURATION);
            this.mAtomicAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    AbstractStateChangeTouchController.this.mAtomicAnim = null;
                }
            });
            this.mAtomicAnim.start();
            this.mLauncher.getDragLayer().performHapticFeedback(6);
        }
    }

    public AnimatorSet createAtomicAnimForState(LauncherState launcherState, LauncherState launcherState2, long j) {
        AnimatorSetBuilder animatorSetBuilder = new AnimatorSetBuilder();
        this.mLauncher.getStateManager().prepareForAtomicAnimation(launcherState, launcherState2, animatorSetBuilder);
        LauncherStateManager.AnimationConfig animationConfig = new LauncherStateManager.AnimationConfig();
        animationConfig.animComponents = 2;
        animationConfig.duration = j;
        for (LauncherStateManager.StateHandler stateHandler : this.mLauncher.getStateManager().getStateHandlers()) {
            stateHandler.setStateWithAnimation(launcherState2, animatorSetBuilder, animationConfig);
        }
        return animatorSetBuilder.build();
    }

    @Override
    public void onDragEnd(float f, boolean z) {
        LauncherState launcherState;
        float fBoundToRange;
        long jCalculateDuration;
        final float f2;
        final int i = z ? 4 : 3;
        boolean z2 = z && this.mFlingBlockCheck.isBlocked();
        boolean z3 = z2 ? false : z;
        float progressFraction = this.mCurrentAnimation.getProgressFraction();
        if (z3) {
            launcherState = Float.compare(Math.signum(f), Math.signum(this.mProgressMultiplier)) == 0 ? this.mToState : this.mFromState;
        } else {
            LauncherState launcherState2 = this.mToState;
            LauncherState launcherState3 = LauncherState.ALL_APPS;
            launcherState = progressFraction > 0.5f ? this.mToState : this.mFromState;
        }
        final LauncherState launcherState4 = launcherState;
        int iBlockedFlingDurationFactor = (z2 && launcherState4 == this.mFromState) ? LauncherAnimUtils.blockedFlingDurationFactor(f) : 1;
        if (launcherState4 != this.mToState) {
            Runnable onCancelRunnable = this.mCurrentAnimation.getOnCancelRunnable();
            this.mCurrentAnimation.setOnCancelRunnable(null);
            this.mCurrentAnimation.dispatchOnCancel();
            this.mCurrentAnimation.setOnCancelRunnable(onCancelRunnable);
            if (progressFraction <= 0.0f) {
                fBoundToRange = 0.0f;
                f2 = fBoundToRange;
                jCalculateDuration = 0;
            } else {
                fBoundToRange = Utilities.boundToRange((f * 16.0f * this.mProgressMultiplier) + progressFraction, 0.0f, 1.0f);
                jCalculateDuration = ((long) iBlockedFlingDurationFactor) * SwipeDetector.calculateDuration(f, Math.min(progressFraction, 1.0f) - 0.0f);
                f2 = 0.0f;
            }
        } else if (progressFraction >= 1.0f) {
            fBoundToRange = 1.0f;
            f2 = fBoundToRange;
            jCalculateDuration = 0;
        } else {
            fBoundToRange = Utilities.boundToRange((f * 16.0f * this.mProgressMultiplier) + progressFraction, 0.0f, 1.0f);
            jCalculateDuration = SwipeDetector.calculateDuration(f, 1.0f - Math.max(progressFraction, 0.0f)) * ((long) iBlockedFlingDurationFactor);
            f2 = 1.0f;
        }
        this.mCurrentAnimation.setEndAction(new Runnable() {
            @Override
            public final void run() {
                this.f$0.onSwipeInteractionCompleted(launcherState4, i);
            }
        });
        final ValueAnimator animationPlayer = this.mCurrentAnimation.getAnimationPlayer();
        animationPlayer.setFloatValues(fBoundToRange, f2);
        maybeUpdateAtomicAnim(this.mFromState, launcherState4, launcherState4 == this.mToState ? 1.0f : 0.0f);
        updateSwipeCompleteAnimation(animationPlayer, Math.max(jCalculateDuration, getRemainingAtomicDuration()), launcherState4, f, z3);
        this.mCurrentAnimation.dispatchOnStart();
        if (z3 && launcherState4 == LauncherState.ALL_APPS) {
            this.mLauncher.getAppsView().addSpringFromFlingUpdateListener(animationPlayer, f);
        }
        animationPlayer.start();
        if (this.mAtomicAnim == null) {
            startAtomicComponentsAnim(f2, animationPlayer.getDuration());
        } else {
            this.mAtomicAnim.addListener(new AnimationSuccessListener() {
                @Override
                public void onAnimationSuccess(Animator animator) {
                    AbstractStateChangeTouchController.this.startAtomicComponentsAnim(f2, animationPlayer.getDuration());
                }
            });
        }
    }

    public void startAtomicComponentsAnim(float f, long j) {
        if (this.mAtomicComponentsController != null) {
            ValueAnimator animationPlayer = this.mAtomicComponentsController.getAnimationPlayer();
            animationPlayer.setFloatValues(this.mAtomicComponentsController.getProgressFraction(), f);
            animationPlayer.setDuration(j);
            animationPlayer.start();
            animationPlayer.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    AbstractStateChangeTouchController.this.mAtomicComponentsController = null;
                }
            });
        }
    }

    private long getRemainingAtomicDuration() {
        long jMax = 0;
        if (this.mAtomicAnim == null) {
            return 0L;
        }
        if (Utilities.ATLEAST_OREO) {
            return this.mAtomicAnim.getTotalDuration() - this.mAtomicAnim.getCurrentPlayTime();
        }
        Iterator<Animator> it = this.mAtomicAnim.getChildAnimations().iterator();
        while (it.hasNext()) {
            jMax = Math.max(jMax, it.next().getDuration());
        }
        return jMax;
    }

    protected void updateSwipeCompleteAnimation(ValueAnimator valueAnimator, long j, LauncherState launcherState, float f, boolean z) {
        valueAnimator.setDuration(j).setInterpolator(Interpolators.scrollInterpolatorForVelocity(f));
    }

    protected int getDirectionForLog() {
        return this.mToState.ordinal > this.mFromState.ordinal ? 1 : 2;
    }

    public void onSwipeInteractionCompleted(LauncherState launcherState, int i) {
        clearState();
        boolean z = true;
        if (this.mPendingAnimation != null) {
            boolean z2 = this.mToState == launcherState;
            this.mPendingAnimation.finish(z2, i);
            this.mPendingAnimation = null;
            z = true ^ z2;
        }
        if (z) {
            if (launcherState != this.mStartState) {
                logReachedState(i, launcherState);
            }
            this.mLauncher.getStateManager().goToState(launcherState, false);
        }
    }

    private void logReachedState(int i, LauncherState launcherState) {
        this.mLauncher.getUserEventDispatcher().logStateChangeAction(i, getDirectionForLog(), this.mStartContainerType, this.mStartState.containerType, launcherState.containerType, this.mLauncher.getWorkspace().getCurrentPage());
    }

    public void clearState() {
        this.mCurrentAnimation = null;
        cancelAtomicComponentsController();
        this.mDetector.finishedScrolling();
        this.mDetector.setDetectableScrollConditions(0, false);
    }

    public void cancelAtomicComponentsController() {
        if (this.mAtomicComponentsController != null) {
            this.mAtomicComponentsController.getAnimationPlayer().cancel();
            this.mAtomicComponentsController = null;
        }
    }
}
