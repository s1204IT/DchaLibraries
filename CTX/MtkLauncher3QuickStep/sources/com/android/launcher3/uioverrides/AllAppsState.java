package com.android.launcher3.uioverrides;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherState;
import com.android.launcher3.anim.Interpolators;

public class AllAppsState extends LauncherState {
    private static final LauncherState.PageAlphaProvider PAGE_ALPHA_PROVIDER = new LauncherState.PageAlphaProvider(Interpolators.DEACCEL_2) {
        @Override
        public float getPageAlpha(int i) {
            return 0.0f;
        }
    };
    private static final int STATE_FLAGS = 2;

    public AllAppsState(int i) {
        super(i, 4, LauncherAnimUtils.ALL_APPS_TRANSITION_MS, 2);
    }

    @Override
    public void onStateEnabled(Launcher launcher) {
        AbstractFloatingView.closeAllOpenViews(launcher);
        dispatchWindowStateChanged(launcher);
    }

    @Override
    public String getDescription(Launcher launcher) {
        return launcher.getAppsView().getDescription();
    }

    @Override
    public float getVerticalProgress(Launcher launcher) {
        return 0.0f;
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(Launcher launcher) {
        float[] workspaceScaleAndTranslation = LauncherState.OVERVIEW.getWorkspaceScaleAndTranslation(launcher);
        workspaceScaleAndTranslation[0] = 1.0f;
        return workspaceScaleAndTranslation;
    }

    @Override
    public LauncherState.PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        return PAGE_ALPHA_PROVIDER;
    }

    @Override
    public int getVisibleElements(Launcher launcher) {
        return 28;
    }

    @Override
    public float[] getOverviewScaleAndTranslationYFactor(Launcher launcher) {
        return new float[]{0.9f, -0.2f};
    }

    @Override
    public LauncherState getHistoryForState(LauncherState launcherState) {
        return launcherState == OVERVIEW ? OVERVIEW : NORMAL;
    }
}
