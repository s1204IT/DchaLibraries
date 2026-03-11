package com.android.launcher3;

import android.graphics.Rect;
import android.view.animation.Interpolator;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.states.SpringLoadedState;
import com.android.launcher3.uioverrides.AllAppsState;
import com.android.launcher3.uioverrides.FastOverviewState;
import com.android.launcher3.uioverrides.OverviewState;
import com.android.launcher3.uioverrides.UiFactory;
import java.util.Arrays;

public class LauncherState {
    public static final int ALL_APPS_CONTENT = 16;
    public static final int ALL_APPS_HEADER = 4;
    public static final int ALL_APPS_HEADER_EXTRA = 8;
    protected static final int FLAG_DISABLE_ACCESSIBILITY = 2;
    protected static final int FLAG_DISABLE_INTERACTION = 64;
    protected static final int FLAG_DISABLE_PAGE_CLIPPING = 16;
    protected static final int FLAG_DISABLE_RESTORE = 4;
    protected static final int FLAG_HAS_SYS_UI_SCRIM = 512;
    protected static final int FLAG_HIDE_BACK_BUTTON = 256;
    protected static final int FLAG_MULTI_PAGE = 1;
    protected static final int FLAG_OVERVIEW_UI = 128;
    protected static final int FLAG_PAGE_BACKGROUNDS = 32;
    protected static final int FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED = 8;
    public static final int HOTSEAT_ICONS = 1;
    public static final int HOTSEAT_SEARCH_BOX = 2;
    public static final int NONE = 0;
    public static final int VERTICAL_SWIPE_INDICATOR = 32;
    public final int containerType;
    public final boolean disableInteraction;
    public final boolean disablePageClipping;
    public final boolean disableRestore;
    public final boolean hasMultipleVisiblePages;
    public final boolean hasSysUiScrim;
    public final boolean hasWorkspacePageBackground;
    public final boolean hideBackButton;
    public final int ordinal;
    public final boolean overviewUi;
    public final int transitionDuration;
    public final int workspaceAccessibilityFlag;
    public final boolean workspaceIconsCanBeDragged;
    protected static final PageAlphaProvider DEFAULT_ALPHA_PROVIDER = new PageAlphaProvider(Interpolators.ACCEL_2) {
        @Override
        public float getPageAlpha(int i) {
            return 1.0f;
        }
    };
    private static final LauncherState[] sAllStates = new LauncherState[5];
    public static final LauncherState NORMAL = new LauncherState(0, 1, 0, 780);
    public static final LauncherState SPRING_LOADED = new SpringLoadedState(1);
    public static final LauncherState OVERVIEW = new OverviewState(2);
    public static final LauncherState FAST_OVERVIEW = new FastOverviewState(3);
    public static final LauncherState ALL_APPS = new AllAppsState(4);
    protected static final Rect sTempRect = new Rect();

    public LauncherState(int i, int i2, int i3, int i4) {
        this.containerType = i2;
        this.transitionDuration = i3;
        this.hasWorkspacePageBackground = (i4 & 32) != 0;
        this.hasMultipleVisiblePages = (i4 & 1) != 0;
        this.workspaceAccessibilityFlag = (i4 & 2) != 0 ? 4 : 0;
        this.disableRestore = (i4 & 4) != 0;
        this.workspaceIconsCanBeDragged = (i4 & 8) != 0;
        this.disablePageClipping = (i4 & 16) != 0;
        this.disableInteraction = (i4 & 64) != 0;
        this.overviewUi = (i4 & 128) != 0;
        this.hideBackButton = (i4 & 256) != 0;
        this.hasSysUiScrim = (i4 & 512) != 0;
        this.ordinal = i;
        sAllStates[i] = this;
    }

    public static LauncherState[] values() {
        return (LauncherState[]) Arrays.copyOf(sAllStates, sAllStates.length);
    }

    public float[] getWorkspaceScaleAndTranslation(Launcher launcher) {
        return new float[]{1.0f, 0.0f, 0.0f};
    }

    public float[] getOverviewScaleAndTranslationYFactor(Launcher launcher) {
        return new float[]{1.1f, 0.0f};
    }

    public void onStateEnabled(Launcher launcher) {
        dispatchWindowStateChanged(launcher);
    }

    public void onStateDisabled(Launcher launcher) {
    }

    public int getVisibleElements(Launcher launcher) {
        if (launcher.getDeviceProfile().isVerticalBarLayout()) {
            return 33;
        }
        return 35;
    }

    public float getVerticalProgress(Launcher launcher) {
        return 1.0f;
    }

    public float getWorkspaceScrimAlpha(Launcher launcher) {
        return 0.0f;
    }

    public String getDescription(Launcher launcher) {
        return launcher.getWorkspace().getCurrentPageDescription();
    }

    public PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        if (this != NORMAL || !launcher.getDeviceProfile().shouldFadeAdjacentWorkspaceScreens()) {
            return DEFAULT_ALPHA_PROVIDER;
        }
        final int nextPage = launcher.getWorkspace().getNextPage();
        return new PageAlphaProvider(Interpolators.ACCEL_2) {
            @Override
            public float getPageAlpha(int i) {
                return i != nextPage ? 0.0f : 1.0f;
            }
        };
    }

    public LauncherState getHistoryForState(LauncherState launcherState) {
        return NORMAL;
    }

    public void onStateTransitionEnd(Launcher launcher) {
        if (this == NORMAL) {
            UiFactory.resetOverview(launcher);
            launcher.getRotationHelper().setCurrentStateRequest(0);
        }
    }

    protected static void dispatchWindowStateChanged(Launcher launcher) {
        launcher.getWindow().getDecorView().sendAccessibilityEvent(32);
    }

    public static abstract class PageAlphaProvider {
        public final Interpolator interpolator;

        public abstract float getPageAlpha(int i);

        public PageAlphaProvider(Interpolator interpolator) {
            this.interpolator = interpolator;
        }
    }
}
