package com.android.launcher3.uioverrides;

import android.view.MotionEvent;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.quickstep.TouchInteractionService;
import com.android.quickstep.views.RecentsView;

/* loaded from: classes.dex */
public class OverviewToAllAppsTouchController extends PortraitStatesTouchController {
    public OverviewToAllAppsTouchController(Launcher launcher) {
        super(launcher);
    }

    @Override // com.android.launcher3.uioverrides.PortraitStatesTouchController, com.android.launcher3.touch.AbstractStateChangeTouchController
    protected boolean canInterceptTouch(MotionEvent motionEvent) {
        if (this.mCurrentAnimation != null) {
            return true;
        }
        if (AbstractFloatingView.getTopOpenView(this.mLauncher) != null) {
            return false;
        }
        if (this.mLauncher.isInState(LauncherState.ALL_APPS)) {
            return this.mLauncher.getAppsView().shouldContainerScroll(motionEvent);
        }
        if (this.mLauncher.isInState(LauncherState.NORMAL)) {
            return true;
        }
        if (!this.mLauncher.isInState(LauncherState.OVERVIEW)) {
            return false;
        }
        RecentsView recentsView = (RecentsView) this.mLauncher.getOverviewPanel();
        return motionEvent.getY() > ((float) (recentsView.getBottom() - recentsView.getPaddingBottom()));
    }

    @Override // com.android.launcher3.uioverrides.PortraitStatesTouchController, com.android.launcher3.touch.AbstractStateChangeTouchController
    protected LauncherState getTargetState(LauncherState launcherState, boolean z) {
        if (launcherState == LauncherState.ALL_APPS && !z) {
            return TouchInteractionService.isConnected() ? this.mLauncher.getStateManager().getLastState() : LauncherState.NORMAL;
        }
        if (z) {
            return LauncherState.ALL_APPS;
        }
        return launcherState;
    }

    @Override // com.android.launcher3.uioverrides.PortraitStatesTouchController, com.android.launcher3.touch.AbstractStateChangeTouchController
    protected int getLogContainerTypeForNormalState() {
        return 1;
    }
}
