package com.android.launcher3;

import android.view.View;
import android.view.animation.Interpolator;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherStateManager;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.anim.PropertySetter;
import com.android.launcher3.graphics.WorkspaceAndHotseatScrim;

/* loaded from: classes.dex */
public class WorkspaceStateTransitionAnimation {
    private final Launcher mLauncher;
    private float mNewScale;
    private final Workspace mWorkspace;

    public WorkspaceStateTransitionAnimation(Launcher launcher, Workspace workspace) {
        this.mLauncher = launcher;
        this.mWorkspace = workspace;
    }

    public void setState(LauncherState launcherState) {
        setWorkspaceProperty(launcherState, PropertySetter.NO_ANIM_PROPERTY_SETTER, new AnimatorSetBuilder(), new LauncherStateManager.AnimationConfig());
    }

    public void setStateWithAnimation(LauncherState launcherState, AnimatorSetBuilder animatorSetBuilder, LauncherStateManager.AnimationConfig animationConfig) {
        setWorkspaceProperty(launcherState, animationConfig.getPropertySetter(animatorSetBuilder), animatorSetBuilder, animationConfig);
    }

    public float getFinalScale() {
        return this.mNewScale;
    }

    private void setWorkspaceProperty(LauncherState launcherState, PropertySetter propertySetter, AnimatorSetBuilder animatorSetBuilder, LauncherStateManager.AnimationConfig animationConfig) {
        float[] workspaceScaleAndTranslation = launcherState.getWorkspaceScaleAndTranslation(this.mLauncher);
        this.mNewScale = workspaceScaleAndTranslation[0];
        LauncherState.PageAlphaProvider workspacePageAlphaProvider = launcherState.getWorkspacePageAlphaProvider(this.mLauncher);
        int childCount = this.mWorkspace.getChildCount();
        for (int i = 0; i < childCount; i++) {
            applyChildState(launcherState, (CellLayout) this.mWorkspace.getChildAt(i), i, workspacePageAlphaProvider, propertySetter, animatorSetBuilder, animationConfig);
        }
        int visibleElements = launcherState.getVisibleElements(this.mLauncher);
        Interpolator interpolator = animatorSetBuilder.getInterpolator(2, workspacePageAlphaProvider.interpolator);
        boolean zPlayAtomicComponent = animationConfig.playAtomicComponent();
        if (zPlayAtomicComponent) {
            propertySetter.setFloat(this.mWorkspace, LauncherAnimUtils.SCALE_PROPERTY, this.mNewScale, animatorSetBuilder.getInterpolator(1, Interpolators.ZOOM_OUT));
            float f = (visibleElements & 1) != 0 ? 1.0f : 0.0f;
            propertySetter.setViewAlpha(this.mLauncher.getHotseat().getLayout(), f, interpolator);
            propertySetter.setViewAlpha(this.mLauncher.getWorkspace().getPageIndicator(), f, interpolator);
        }
        if (!animationConfig.playNonAtomicComponent()) {
            return;
        }
        Interpolator interpolator2 = !zPlayAtomicComponent ? Interpolators.LINEAR : Interpolators.ZOOM_OUT;
        propertySetter.setFloat(this.mWorkspace, View.TRANSLATION_X, workspaceScaleAndTranslation[1], interpolator2);
        propertySetter.setFloat(this.mWorkspace, View.TRANSLATION_Y, workspaceScaleAndTranslation[2], interpolator2);
        propertySetter.setViewAlpha(this.mLauncher.getHotseatSearchBox(), (visibleElements & 2) != 0 ? 1.0f : 0.0f, interpolator);
        WorkspaceAndHotseatScrim scrim = this.mLauncher.getDragLayer().getScrim();
        propertySetter.setFloat(scrim, WorkspaceAndHotseatScrim.SCRIM_PROGRESS, launcherState.getWorkspaceScrimAlpha(this.mLauncher), Interpolators.LINEAR);
        propertySetter.setFloat(scrim, WorkspaceAndHotseatScrim.SYSUI_PROGRESS, launcherState.hasSysUiScrim ? 1.0f : 0.0f, Interpolators.LINEAR);
    }

    public void applyChildState(LauncherState launcherState, CellLayout cellLayout, int i) {
        applyChildState(launcherState, cellLayout, i, launcherState.getWorkspacePageAlphaProvider(this.mLauncher), PropertySetter.NO_ANIM_PROPERTY_SETTER, new AnimatorSetBuilder(), new LauncherStateManager.AnimationConfig());
    }

    private void applyChildState(LauncherState launcherState, CellLayout cellLayout, int i, LauncherState.PageAlphaProvider pageAlphaProvider, PropertySetter propertySetter, AnimatorSetBuilder animatorSetBuilder, LauncherStateManager.AnimationConfig animationConfig) {
        float pageAlpha = pageAlphaProvider.getPageAlpha(i);
        int iRound = Math.round((launcherState.hasWorkspacePageBackground ? 255 : 0) * pageAlpha);
        if (animationConfig.playNonAtomicComponent()) {
            propertySetter.setInt(cellLayout.getScrimBackground(), LauncherAnimUtils.DRAWABLE_ALPHA, iRound, Interpolators.ZOOM_OUT);
        }
        if (animationConfig.playAtomicComponent()) {
            propertySetter.setFloat(cellLayout.getShortcutsAndWidgets(), View.ALPHA, pageAlpha, animatorSetBuilder.getInterpolator(2, pageAlphaProvider.interpolator));
        }
    }
}
