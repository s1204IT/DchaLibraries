package com.android.launcher3.uioverrides;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherState;
import com.android.launcher3.anim.Interpolators;

/* loaded from: classes.dex */
public class AllAppsState extends LauncherState {
    private static final LauncherState.PageAlphaProvider PAGE_ALPHA_PROVIDER = new LauncherState.PageAlphaProvider(Interpolators.DEACCEL_2) { // from class: com.android.launcher3.uioverrides.AllAppsState.1
        @Override // com.android.launcher3.LauncherState.PageAlphaProvider
        public float getPageAlpha(int i) {
            return 0.0f;
        }
    };
    private static final int STATE_FLAGS = 2;

    public AllAppsState(int i) {
        super(i, 4, LauncherAnimUtils.ALL_APPS_TRANSITION_MS, 2);
    }

    @Override // com.android.launcher3.LauncherState
    public void onStateEnabled(Launcher launcher) {
        AbstractFloatingView.closeAllOpenViews(launcher);
        dispatchWindowStateChanged(launcher);
    }

    @Override // com.android.launcher3.LauncherState
    public String getDescription(Launcher launcher) {
        return launcher.getAppsView().getDescription();
    }

    @Override // com.android.launcher3.LauncherState
    public float getVerticalProgress(Launcher launcher) {
        return 0.0f;
    }

    @Override // com.android.launcher3.LauncherState
    public float[] getWorkspaceScaleAndTranslation(Launcher launcher) {
        float[] workspaceScaleAndTranslation = LauncherState.OVERVIEW.getWorkspaceScaleAndTranslation(launcher);
        workspaceScaleAndTranslation[0] = 1.0f;
        return workspaceScaleAndTranslation;
    }

    @Override // com.android.launcher3.LauncherState
    public LauncherState.PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        return PAGE_ALPHA_PROVIDER;
    }

    @Override // com.android.launcher3.LauncherState
    public int getVisibleElements(Launcher launcher) {
        return 28;
    }

    @Override // com.android.launcher3.LauncherState
    public float[] getOverviewScaleAndTranslationYFactor(Launcher launcher) {
        return new float[]{0.9f, -0.2f};
    }

    @Override // com.android.launcher3.LauncherState
    public LauncherState getHistoryForState(LauncherState launcherState) {
        return launcherState == OVERVIEW ? OVERVIEW : NORMAL;
    }
}
