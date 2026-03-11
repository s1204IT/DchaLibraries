package com.android.launcher3.uioverrides;

import android.view.View;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.Workspace;
import com.android.launcher3.allapps.DiscoveryBounce;
import com.android.launcher3.anim.Interpolators;
import com.android.quickstep.views.RecentsView;

public class OverviewState extends LauncherState {
    private static final int STATE_FLAGS = 142;

    public OverviewState(int i) {
        this(i, 250, 142);
    }

    protected OverviewState(int i, int i2, int i3) {
        super(i, 12, i2, i3);
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(Launcher launcher) {
        RecentsView recentsView = (RecentsView) launcher.getOverviewPanel();
        Workspace workspace = launcher.getWorkspace();
        View pageAt = workspace.getPageAt(workspace.getCurrentPage());
        int width = (pageAt == null || pageAt.getWidth() == 0) ? launcher.getDeviceProfile().availableWidthPx : pageAt.getWidth();
        recentsView.getTaskSize(sTempRect);
        return new float[]{sTempRect.width() / width, 0.0f, (-getDefaultSwipeHeight(launcher)) * 0.5f};
    }

    @Override
    public float[] getOverviewScaleAndTranslationYFactor(Launcher launcher) {
        return new float[]{1.0f, 0.0f};
    }

    @Override
    public void onStateEnabled(Launcher launcher) {
        ((RecentsView) launcher.getOverviewPanel()).setOverviewStateEnabled(true);
        AbstractFloatingView.closeAllOpenViews(launcher);
    }

    @Override
    public void onStateDisabled(Launcher launcher) {
        ((RecentsView) launcher.getOverviewPanel()).setOverviewStateEnabled(false);
    }

    @Override
    public void onStateTransitionEnd(Launcher launcher) {
        launcher.getRotationHelper().setCurrentStateRequest(1);
        DiscoveryBounce.showForOverviewIfNeeded(launcher);
    }

    @Override
    public LauncherState.PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        return new LauncherState.PageAlphaProvider(Interpolators.DEACCEL_2) {
            @Override
            public float getPageAlpha(int i) {
                return 0.0f;
            }
        };
    }

    @Override
    public int getVisibleElements(Launcher launcher) {
        if (launcher.getDeviceProfile().isVerticalBarLayout()) {
            return 32;
        }
        return (launcher.getAppsView().getFloatingHeaderView().hasVisibleContent() ? 8 : 1) | 34;
    }

    @Override
    public float getWorkspaceScrimAlpha(Launcher launcher) {
        return 0.5f;
    }

    @Override
    public float getVerticalProgress(Launcher launcher) {
        if ((getVisibleElements(launcher) & 8) == 0) {
            return super.getVerticalProgress(launcher);
        }
        return 1.0f - (getDefaultSwipeHeight(launcher) / launcher.getAllAppsController().getShiftRange());
    }

    public static float getDefaultSwipeHeight(Launcher launcher) {
        return r1.allAppsCellHeightPx - launcher.getDeviceProfile().allAppsIconTextSizePx;
    }
}
