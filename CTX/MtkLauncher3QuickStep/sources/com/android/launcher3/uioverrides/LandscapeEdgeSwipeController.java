package com.android.launcher3.uioverrides;

import android.view.MotionEvent;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.touch.AbstractStateChangeTouchController;
import com.android.launcher3.touch.SwipeDetector;
import com.android.quickstep.RecentsModel;

public class LandscapeEdgeSwipeController extends AbstractStateChangeTouchController {
    private static final String TAG = "LandscapeEdgeSwipeCtrl";

    public LandscapeEdgeSwipeController(Launcher launcher) {
        super(launcher, SwipeDetector.HORIZONTAL);
    }

    @Override
    protected boolean canInterceptTouch(MotionEvent motionEvent) {
        if (this.mCurrentAnimation != null) {
            return true;
        }
        if (AbstractFloatingView.getTopOpenView(this.mLauncher) != null) {
            return false;
        }
        return this.mLauncher.isInState(LauncherState.NORMAL) && (motionEvent.getEdgeFlags() & 256) != 0;
    }

    @Override
    protected LauncherState getTargetState(LauncherState launcherState, boolean z) {
        return this.mLauncher.getDeviceProfile().isSeascape() != z ? LauncherState.OVERVIEW : LauncherState.NORMAL;
    }

    @Override
    protected int getLogContainerTypeForNormalState() {
        return 11;
    }

    @Override
    protected float getShiftRange() {
        return this.mLauncher.getDragLayer().getWidth();
    }

    @Override
    protected float initCurrentAnimation(int i) {
        float shiftRange = getShiftRange();
        this.mCurrentAnimation = this.mLauncher.getStateManager().createAnimationToNewWorkspace(this.mToState, (long) (2.0f * shiftRange), i);
        return (this.mLauncher.getDeviceProfile().isSeascape() ? 2 : -2) / shiftRange;
    }

    @Override
    protected int getDirectionForLog() {
        return this.mLauncher.getDeviceProfile().isSeascape() ? 4 : 3;
    }

    @Override
    protected void onSwipeInteractionCompleted(LauncherState launcherState, int i) {
        super.onSwipeInteractionCompleted(launcherState, i);
        if (this.mStartState == LauncherState.NORMAL && launcherState == LauncherState.OVERVIEW) {
            RecentsModel.getInstance(this.mLauncher).onOverviewShown(true, TAG);
        }
    }
}
