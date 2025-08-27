package com.android.launcher3.uioverrides;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.touch.AbstractStateChangeTouchController;
import com.android.launcher3.touch.SwipeDetector;
import com.android.quickstep.RecentsModel;
import com.android.quickstep.TouchInteractionService;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;

/* loaded from: classes.dex */
public class PortraitStatesTouchController extends AbstractStateChangeTouchController {
    private static final String TAG = "PortraitStatesTouchCtrl";
    private InterpolatorWrapper mAllAppsInterpolatorWrapper;
    private boolean mFinishFastOnSecondTouch;

    public PortraitStatesTouchController(Launcher launcher) {
        super(launcher, SwipeDetector.VERTICAL);
        this.mAllAppsInterpolatorWrapper = new InterpolatorWrapper();
    }

    @Override // com.android.launcher3.touch.AbstractStateChangeTouchController
    protected boolean canInterceptTouch(MotionEvent motionEvent) {
        if (this.mCurrentAnimation != null) {
            if (this.mFinishFastOnSecondTouch) {
                this.mCurrentAnimation.getAnimationPlayer().end();
            }
            return true;
        }
        if (this.mLauncher.isInState(LauncherState.ALL_APPS)) {
            if (!this.mLauncher.getAppsView().shouldContainerScroll(motionEvent)) {
                return false;
            }
        } else {
            DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
            if (motionEvent.getY() < this.mLauncher.getDragLayer().getHeight() - (deviceProfile.hotseatBarSizePx + deviceProfile.getInsets().bottom)) {
                return false;
            }
        }
        return AbstractFloatingView.getTopOpenView(this.mLauncher) == null;
    }

    @Override // com.android.launcher3.touch.AbstractStateChangeTouchController
    protected LauncherState getTargetState(LauncherState launcherState, boolean z) {
        if (launcherState == LauncherState.ALL_APPS && !z) {
            return TouchInteractionService.isConnected() ? this.mLauncher.getStateManager().getLastState() : LauncherState.NORMAL;
        }
        if (launcherState == LauncherState.OVERVIEW) {
            return z ? LauncherState.ALL_APPS : LauncherState.NORMAL;
        }
        if (launcherState == LauncherState.NORMAL && z) {
            return TouchInteractionService.isConnected() ? LauncherState.OVERVIEW : LauncherState.ALL_APPS;
        }
        return launcherState;
    }

    @Override // com.android.launcher3.touch.AbstractStateChangeTouchController
    protected int getLogContainerTypeForNormalState() {
        return 2;
    }

    private AnimatorSetBuilder getNormalToOverviewAnimation() {
        this.mAllAppsInterpolatorWrapper.baseInterpolator = Interpolators.LINEAR;
        AnimatorSetBuilder animatorSetBuilder = new AnimatorSetBuilder();
        animatorSetBuilder.setInterpolator(0, this.mAllAppsInterpolatorWrapper);
        return animatorSetBuilder;
    }

    @Override // com.android.launcher3.touch.AbstractStateChangeTouchController
    protected float initCurrentAnimation(int i) {
        AnimatorSetBuilder animatorSetBuilder;
        float shiftRange = getShiftRange();
        long j = (long) (2.0f * shiftRange);
        float verticalProgress = (this.mToState.getVerticalProgress(this.mLauncher) * shiftRange) - (this.mFromState.getVerticalProgress(this.mLauncher) * shiftRange);
        if (this.mFromState == LauncherState.NORMAL && this.mToState == LauncherState.OVERVIEW && verticalProgress != 0.0f) {
            animatorSetBuilder = getNormalToOverviewAnimation();
        } else {
            animatorSetBuilder = new AnimatorSetBuilder();
        }
        AnimatorSetBuilder animatorSetBuilder2 = animatorSetBuilder;
        cancelPendingAnim();
        RecentsView recentsView = (RecentsView) this.mLauncher.getOverviewPanel();
        TaskView taskView = (TaskView) recentsView.getChildAt(recentsView.getNextPage());
        if (recentsView.shouldSwipeDownLaunchApp() && this.mFromState == LauncherState.OVERVIEW && this.mToState == LauncherState.NORMAL && taskView != null) {
            this.mPendingAnimation = recentsView.createTaskLauncherAnimation(taskView, j);
            this.mPendingAnimation.anim.setInterpolator(Interpolators.ZOOM_IN);
            this.mCurrentAnimation = AnimatorPlaybackController.wrap(this.mPendingAnimation.anim, j, new Runnable() { // from class: com.android.launcher3.uioverrides.-$$Lambda$PortraitStatesTouchController$Lr8WCJ6fPg-JUNf5e-gNGi0C5VA
                @Override // java.lang.Runnable
                public final void run() {
                    PortraitStatesTouchController.lambda$initCurrentAnimation$0(this.f$0);
                }
            });
            this.mLauncher.getStateManager().setCurrentUserControlledAnimation(this.mCurrentAnimation);
        } else {
            this.mCurrentAnimation = this.mLauncher.getStateManager().createAnimationToNewWorkspace(this.mToState, animatorSetBuilder2, j, new Runnable() { // from class: com.android.launcher3.uioverrides.-$$Lambda$PortraitStatesTouchController$giQThULY0XWptg0AjAH1tO0p39A
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.clearState();
                }
            }, i);
        }
        if (verticalProgress == 0.0f) {
            verticalProgress = OverviewState.getDefaultSwipeHeight(this.mLauncher) * Math.signum(this.mFromState.ordinal - this.mToState.ordinal);
        }
        return 1.0f / verticalProgress;
    }

    public static /* synthetic */ void lambda$initCurrentAnimation$0(PortraitStatesTouchController portraitStatesTouchController) {
        portraitStatesTouchController.cancelPendingAnim();
        portraitStatesTouchController.clearState();
    }

    private void cancelPendingAnim() {
        if (this.mPendingAnimation != null) {
            this.mPendingAnimation.finish(false, 3);
            this.mPendingAnimation = null;
        }
    }

    @Override // com.android.launcher3.touch.AbstractStateChangeTouchController
    protected void updateSwipeCompleteAnimation(ValueAnimator valueAnimator, long j, LauncherState launcherState, float f, boolean z) {
        super.updateSwipeCompleteAnimation(valueAnimator, j, launcherState, f, z);
        handleFirstSwipeToOverview(valueAnimator, j, launcherState, f, z);
    }

    private void handleFirstSwipeToOverview(ValueAnimator valueAnimator, long j, LauncherState launcherState, float f, boolean z) {
        if (this.mFromState == LauncherState.NORMAL && this.mToState == LauncherState.OVERVIEW && launcherState == LauncherState.OVERVIEW) {
            this.mFinishFastOnSecondTouch = true;
            if (z && j != 0) {
                float progressFraction = this.mCurrentAnimation.getProgressFraction();
                this.mAllAppsInterpolatorWrapper.baseInterpolator = Interpolators.clampToProgress(new OvershootInterpolator(Math.min(Math.abs(f), 3.0f)), progressFraction, 1.0f);
                valueAnimator.setDuration(Math.min(j, 200L)).setInterpolator(Interpolators.LINEAR);
                return;
            }
            return;
        }
        this.mFinishFastOnSecondTouch = false;
    }

    @Override // com.android.launcher3.touch.AbstractStateChangeTouchController
    protected void onSwipeInteractionCompleted(LauncherState launcherState, int i) {
        super.onSwipeInteractionCompleted(launcherState, i);
        if (this.mStartState == LauncherState.NORMAL && launcherState == LauncherState.OVERVIEW) {
            RecentsModel.getInstance(this.mLauncher).onOverviewShown(true, TAG);
        }
    }

    private static class InterpolatorWrapper implements Interpolator {
        public TimeInterpolator baseInterpolator;

        private InterpolatorWrapper() {
            this.baseInterpolator = Interpolators.LINEAR;
        }

        /* synthetic */ InterpolatorWrapper(AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float f) {
            return this.baseInterpolator.getInterpolation(f);
        }
    }
}
