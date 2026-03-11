package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.Workspace;
import java.util.HashMap;

public class WorkspaceStateTransitionAnimation {
    int mAllAppsTransitionTime;
    final Launcher mLauncher;
    float[] mNewAlphas;
    float[] mNewBackgroundAlphas;
    float mNewScale;
    float[] mOldAlphas;
    float[] mOldBackgroundAlphas;
    int mOverlayTransitionTime;
    float mOverviewModeShrinkFactor;
    int mOverviewTransitionTime;
    float mSpringLoadedShrinkFactor;
    AnimatorSet mStateAnimator;
    final Workspace mWorkspace;
    boolean mWorkspaceFadeInAdjacentScreens;
    float mWorkspaceScrimAlpha;
    int mLastChildCount = -1;
    final ZoomInInterpolator mZoomInInterpolator = new ZoomInInterpolator();

    public WorkspaceStateTransitionAnimation(Launcher launcher, Workspace workspace) {
        this.mLauncher = launcher;
        this.mWorkspace = workspace;
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        Resources res = launcher.getResources();
        this.mAllAppsTransitionTime = res.getInteger(R.integer.config_allAppsTransitionTime);
        this.mOverviewTransitionTime = res.getInteger(R.integer.config_overviewTransitionTime);
        this.mOverlayTransitionTime = res.getInteger(R.integer.config_overlayTransitionTime);
        this.mSpringLoadedShrinkFactor = res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f;
        this.mOverviewModeShrinkFactor = res.getInteger(R.integer.config_workspaceOverviewShrinkPercentage) / 100.0f;
        this.mWorkspaceScrimAlpha = res.getInteger(R.integer.config_workspaceScrimAlpha) / 100.0f;
        this.mWorkspaceFadeInAdjacentScreens = grid.shouldFadeAdjacentWorkspaceScreens();
    }

    public AnimatorSet getAnimationToState(Workspace.State fromState, Workspace.State toState, int toPage, boolean animated, HashMap<View, Integer> layerViews) {
        AccessibilityManager am = (AccessibilityManager) this.mLauncher.getSystemService("accessibility");
        boolean accessibilityEnabled = am.isEnabled();
        TransitionStates states = new TransitionStates(fromState, toState);
        int workspaceDuration = getAnimationDuration(states);
        animateWorkspace(states, toPage, animated, workspaceDuration, layerViews, accessibilityEnabled);
        animateBackgroundGradient(states, animated, 350);
        return this.mStateAnimator;
    }

    public float getFinalScale() {
        return this.mNewScale;
    }

    private void reinitializeAnimationArrays() {
        int childCount = this.mWorkspace.getChildCount();
        if (this.mLastChildCount == childCount) {
            return;
        }
        this.mOldBackgroundAlphas = new float[childCount];
        this.mOldAlphas = new float[childCount];
        this.mNewBackgroundAlphas = new float[childCount];
        this.mNewAlphas = new float[childCount];
    }

    private int getAnimationDuration(TransitionStates states) {
        if (states.workspaceToAllApps || states.overviewToAllApps) {
            return this.mAllAppsTransitionTime;
        }
        if (states.workspaceToOverview || states.overviewToWorkspace) {
            return this.mOverviewTransitionTime;
        }
        return this.mOverlayTransitionTime;
    }

    private void animateWorkspace(TransitionStates states, int toPage, boolean animated, int duration, HashMap<View, Integer> layerViews, final boolean accessibilityEnabled) {
        Animator pageIndicatorAlpha;
        reinitializeAnimationArrays();
        cancelAnimation();
        if (animated) {
            this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
        }
        float finalBackgroundAlpha = (states.stateIsSpringLoaded || states.stateIsOverview) ? 1.0f : 0.0f;
        float finalHotseatAndPageIndicatorAlpha = (states.stateIsNormal || states.stateIsSpringLoaded) ? 1.0f : 0.0f;
        float finalOverviewPanelAlpha = states.stateIsOverview ? 1.0f : 0.0f;
        float finalWorkspaceTranslationY = (states.stateIsOverview || states.stateIsOverviewHidden) ? this.mWorkspace.getOverviewModeTranslationY() : 0;
        int childCount = this.mWorkspace.getChildCount();
        int customPageCount = this.mWorkspace.numCustomPages();
        this.mNewScale = 1.0f;
        if (states.oldStateIsOverview) {
            this.mWorkspace.disableFreeScroll();
        } else if (states.stateIsOverview) {
            this.mWorkspace.enableFreeScroll();
        }
        if (!states.stateIsNormal) {
            if (states.stateIsSpringLoaded) {
                this.mNewScale = this.mSpringLoadedShrinkFactor;
            } else if (states.stateIsOverview || states.stateIsOverviewHidden) {
                this.mNewScale = this.mOverviewModeShrinkFactor;
            }
        }
        if (toPage == -1) {
            toPage = this.mWorkspace.getPageNearestToCenterOfScreen();
        }
        this.mWorkspace.snapToPage(toPage, duration, this.mZoomInInterpolator);
        int i = 0;
        while (i < childCount) {
            CellLayout cl = (CellLayout) this.mWorkspace.getChildAt(i);
            boolean isCurrentPage = i == toPage;
            float initialAlpha = cl.getShortcutsAndWidgets().getAlpha();
            float finalAlpha = (states.stateIsNormalHidden || states.stateIsOverviewHidden) ? 0.0f : (!states.stateIsNormal || !this.mWorkspaceFadeInAdjacentScreens || i == toPage || i < customPageCount) ? 1.0f : 0.0f;
            if (!this.mWorkspace.isSwitchingState() && (states.workspaceToAllApps || states.allAppsToWorkspace)) {
                if (states.allAppsToWorkspace && isCurrentPage) {
                    initialAlpha = 0.0f;
                } else if (!isCurrentPage) {
                    finalAlpha = 0.0f;
                    initialAlpha = 0.0f;
                }
                cl.setShortcutAndWidgetAlpha(initialAlpha);
            }
            this.mOldAlphas[i] = initialAlpha;
            this.mNewAlphas[i] = finalAlpha;
            if (animated) {
                this.mOldBackgroundAlphas[i] = cl.getBackgroundAlpha();
                this.mNewBackgroundAlphas[i] = finalBackgroundAlpha;
            } else {
                cl.setBackgroundAlpha(finalBackgroundAlpha);
                cl.setShortcutAndWidgetAlpha(finalAlpha);
                cl.invalidate();
            }
            i++;
        }
        final ViewGroup overviewPanel = this.mLauncher.getOverviewPanel();
        View hotseat = this.mLauncher.getHotseat();
        View pageIndicator = this.mWorkspace.getPageIndicator();
        if (!animated) {
            overviewPanel.setAlpha(finalOverviewPanelAlpha);
            AlphaUpdateListener.updateVisibility(overviewPanel, accessibilityEnabled);
            hotseat.setAlpha(finalHotseatAndPageIndicatorAlpha);
            AlphaUpdateListener.updateVisibility(hotseat, accessibilityEnabled);
            if (pageIndicator != null) {
                pageIndicator.setAlpha(finalHotseatAndPageIndicatorAlpha);
                AlphaUpdateListener.updateVisibility(pageIndicator, accessibilityEnabled);
            }
            this.mWorkspace.updateCustomContentVisibility();
            this.mWorkspace.setScaleX(this.mNewScale);
            this.mWorkspace.setScaleY(this.mNewScale);
            this.mWorkspace.setTranslationY(finalWorkspaceTranslationY);
            if (accessibilityEnabled && overviewPanel.getVisibility() == 0) {
                overviewPanel.getChildAt(0).performAccessibilityAction(64, null);
                return;
            }
            return;
        }
        LauncherViewPropertyAnimator scale = new LauncherViewPropertyAnimator(this.mWorkspace);
        scale.scaleX(this.mNewScale).scaleY(this.mNewScale).translationY(finalWorkspaceTranslationY).setDuration(duration).setInterpolator(this.mZoomInInterpolator);
        this.mStateAnimator.play(scale);
        for (int index = 0; index < childCount; index++) {
            int i2 = index;
            CellLayout cl2 = (CellLayout) this.mWorkspace.getChildAt(i2);
            float currentAlpha = cl2.getShortcutsAndWidgets().getAlpha();
            if (this.mOldAlphas[i2] == 0.0f && this.mNewAlphas[i2] == 0.0f) {
                cl2.setBackgroundAlpha(this.mNewBackgroundAlphas[i2]);
                cl2.setShortcutAndWidgetAlpha(this.mNewAlphas[i2]);
            } else {
                if (layerViews != null) {
                    layerViews.put(cl2, 0);
                }
                if (this.mOldAlphas[i2] != this.mNewAlphas[i2] || currentAlpha != this.mNewAlphas[i2]) {
                    LauncherViewPropertyAnimator alphaAnim = new LauncherViewPropertyAnimator(cl2.getShortcutsAndWidgets());
                    alphaAnim.alpha(this.mNewAlphas[i2]).setDuration(duration).setInterpolator(this.mZoomInInterpolator);
                    this.mStateAnimator.play(alphaAnim);
                }
                if (this.mOldBackgroundAlphas[i2] != 0.0f || this.mNewBackgroundAlphas[i2] != 0.0f) {
                    ValueAnimator bgAnim = ObjectAnimator.ofFloat(cl2, "backgroundAlpha", this.mOldBackgroundAlphas[i2], this.mNewBackgroundAlphas[i2]);
                    LauncherAnimUtils.ofFloat(cl2, 0.0f, 1.0f);
                    bgAnim.setInterpolator(this.mZoomInInterpolator);
                    bgAnim.setDuration(duration);
                    this.mStateAnimator.play(bgAnim);
                }
            }
        }
        if (pageIndicator != null) {
            pageIndicatorAlpha = new LauncherViewPropertyAnimator(pageIndicator).alpha(finalHotseatAndPageIndicatorAlpha).withLayer();
            pageIndicatorAlpha.addListener(new AlphaUpdateListener(pageIndicator, accessibilityEnabled));
        } else {
            pageIndicatorAlpha = ValueAnimator.ofFloat(0.0f, 0.0f);
        }
        LauncherViewPropertyAnimator hotseatAlpha = new LauncherViewPropertyAnimator(hotseat).alpha(finalHotseatAndPageIndicatorAlpha);
        hotseatAlpha.addListener(new AlphaUpdateListener(hotseat, accessibilityEnabled));
        LauncherViewPropertyAnimator overviewPanelAlpha = new LauncherViewPropertyAnimator(overviewPanel).alpha(finalOverviewPanelAlpha);
        overviewPanelAlpha.addListener(new AlphaUpdateListener(overviewPanel, accessibilityEnabled));
        hotseat.setLayerType(2, null);
        overviewPanel.setLayerType(2, null);
        if (layerViews != null) {
            layerViews.put(hotseat, 1);
            layerViews.put(overviewPanel, 1);
        } else {
            hotseatAlpha.withLayer();
            overviewPanelAlpha.withLayer();
        }
        if (states.workspaceToOverview) {
            pageIndicatorAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
            hotseatAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
            overviewPanelAlpha.setInterpolator(null);
        } else if (states.overviewToWorkspace) {
            pageIndicatorAlpha.setInterpolator(null);
            hotseatAlpha.setInterpolator(null);
            overviewPanelAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
        }
        overviewPanelAlpha.setDuration(duration);
        pageIndicatorAlpha.setDuration(duration);
        hotseatAlpha.setDuration(duration);
        this.mStateAnimator.play(overviewPanelAlpha);
        this.mStateAnimator.play(hotseatAlpha);
        this.mStateAnimator.play(pageIndicatorAlpha);
        this.mStateAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                WorkspaceStateTransitionAnimation.this.mStateAnimator = null;
                if (!accessibilityEnabled || overviewPanel.getVisibility() != 0) {
                    return;
                }
                overviewPanel.getChildAt(0).performAccessibilityAction(64, null);
            }
        });
    }

    private void animateBackgroundGradient(TransitionStates states, boolean animated, int duration) {
        final DragLayer dragLayer = this.mLauncher.getDragLayer();
        float startAlpha = dragLayer.getBackgroundAlpha();
        float finalAlpha = states.stateIsNormal ? 0.0f : this.mWorkspaceScrimAlpha;
        if (finalAlpha == startAlpha) {
            return;
        }
        if (animated) {
            ValueAnimator bgFadeOutAnimation = LauncherAnimUtils.ofFloat(this.mWorkspace, startAlpha, finalAlpha);
            bgFadeOutAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    dragLayer.setBackgroundAlpha(((Float) animation.getAnimatedValue()).floatValue());
                }
            });
            bgFadeOutAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
            bgFadeOutAnimation.setDuration(duration);
            this.mStateAnimator.play(bgFadeOutAnimation);
            return;
        }
        dragLayer.setBackgroundAlpha(finalAlpha);
    }

    private void cancelAnimation() {
        if (this.mStateAnimator != null) {
            this.mStateAnimator.setDuration(0L);
            this.mStateAnimator.cancel();
        }
        this.mStateAnimator = null;
    }
}
