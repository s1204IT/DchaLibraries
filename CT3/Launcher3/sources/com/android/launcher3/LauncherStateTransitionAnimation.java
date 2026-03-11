package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.Launcher;
import com.android.launcher3.SearchDropTargetBar;
import com.android.launcher3.Workspace;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.util.UiThreadCircularReveal;
import com.android.launcher3.widget.WidgetsContainerView;
import java.util.HashMap;

public class LauncherStateTransitionAnimation {
    AnimatorSet mCurrentAnimation;
    Launcher mLauncher;

    private static class PrivateTransitionCallbacks {
        private final float materialRevealViewFinalAlpha;

        PrivateTransitionCallbacks(float revealAlpha) {
            this.materialRevealViewFinalAlpha = revealAlpha;
        }

        float getMaterialRevealViewStartFinalRadius() {
            return 0.0f;
        }

        AnimatorListenerAdapter getMaterialRevealViewAnimatorListener(View revealView, View buttonView) {
            return null;
        }

        void onTransitionComplete() {
        }
    }

    public LauncherStateTransitionAnimation(Launcher l) {
        this.mLauncher = l;
    }

    public void startAnimationToAllApps(Workspace.State fromWorkspaceState, boolean animated, final boolean startSearchAfterTransition) {
        final AllAppsContainerView toView = this.mLauncher.getAppsView();
        View buttonView = this.mLauncher.getAllAppsButton();
        PrivateTransitionCallbacks cb = new PrivateTransitionCallbacks(1.0f) {
            @Override
            public float getMaterialRevealViewStartFinalRadius() {
                int allAppsButtonSize = LauncherStateTransitionAnimation.this.mLauncher.getDeviceProfile().allAppsButtonVisualSize;
                return allAppsButtonSize / 2;
            }

            @Override
            public AnimatorListenerAdapter getMaterialRevealViewAnimatorListener(View revealView, final View allAppsButtonView) {
                return new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        allAppsButtonView.setVisibility(4);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        allAppsButtonView.setVisibility(0);
                    }
                };
            }

            @Override
            void onTransitionComplete() {
                if (!startSearchAfterTransition) {
                    return;
                }
                toView.startAppsSearch();
            }
        };
        this.mCurrentAnimation = startAnimationToOverlay(fromWorkspaceState, Workspace.State.NORMAL_HIDDEN, buttonView, toView, animated, cb);
    }

    public void startAnimationToWidgets(Workspace.State fromWorkspaceState, boolean animated) {
        WidgetsContainerView toView = this.mLauncher.getWidgetsView();
        View buttonView = this.mLauncher.getWidgetsButton();
        this.mCurrentAnimation = startAnimationToOverlay(fromWorkspaceState, Workspace.State.OVERVIEW_HIDDEN, buttonView, toView, animated, new PrivateTransitionCallbacks(0.3f));
    }

    public void startAnimationToWorkspace(Launcher.State fromState, Workspace.State fromWorkspaceState, Workspace.State toWorkspaceState, int toWorkspacePage, boolean animated, Runnable onCompleteRunnable) {
        if (toWorkspaceState != Workspace.State.NORMAL && toWorkspaceState != Workspace.State.SPRING_LOADED && toWorkspaceState != Workspace.State.OVERVIEW) {
            Log.e("LSTAnimation", "Unexpected call to startAnimationToWorkspace");
        }
        if (fromState == Launcher.State.APPS || fromState == Launcher.State.APPS_SPRING_LOADED) {
            startAnimationToWorkspaceFromAllApps(fromWorkspaceState, toWorkspaceState, toWorkspacePage, animated, onCompleteRunnable);
        } else {
            startAnimationToWorkspaceFromWidgets(fromWorkspaceState, toWorkspaceState, toWorkspacePage, animated, onCompleteRunnable);
        }
    }

    @SuppressLint({"NewApi"})
    private AnimatorSet startAnimationToOverlay(Workspace.State fromWorkspaceState, Workspace.State toWorkspaceState, View buttonView, final BaseContainerView toView, final boolean animated, final PrivateTransitionCallbacks pCb) {
        float revealViewToAlpha;
        float revealViewToYDrift;
        float revealViewToXDrift;
        final AnimatorSet animation = LauncherAnimUtils.createAnimatorSet();
        Resources res = this.mLauncher.getResources();
        boolean material = Utilities.ATLEAST_LOLLIPOP;
        int revealDuration = res.getInteger(R.integer.config_overlayRevealTime);
        int itemsAlphaStagger = res.getInteger(R.integer.config_overlayItemsAlphaStagger);
        final View fromView = this.mLauncher.getWorkspace();
        final HashMap<View, Integer> layerViews = new HashMap<>();
        boolean initialized = buttonView != null;
        cancelAnimation();
        Animator workspaceAnim = this.mLauncher.startWorkspaceStateChangeAnimation(toWorkspaceState, -1, animated, layerViews);
        startWorkspaceSearchBarAnimation(toWorkspaceState, animated ? revealDuration : 0, animation);
        Animator updateTransitionStepAnim = dispatchOnLauncherTransitionStepAnim(fromView, toView);
        View contentView = toView.getContentView();
        if (animated && initialized) {
            final View revealView = toView.getRevealView();
            int width = revealView.getMeasuredWidth();
            int height = revealView.getMeasuredHeight();
            float revealRadius = (float) Math.hypot(width / 2, height / 2);
            revealView.setVisibility(0);
            revealView.setAlpha(0.0f);
            revealView.setTranslationY(0.0f);
            revealView.setTranslationX(0.0f);
            if (material) {
                int[] buttonViewToPanelDelta = Utilities.getCenterDeltaInScreenSpace(revealView, buttonView, null);
                revealViewToAlpha = pCb.materialRevealViewFinalAlpha;
                revealViewToYDrift = buttonViewToPanelDelta[1];
                revealViewToXDrift = buttonViewToPanelDelta[0];
            } else {
                revealViewToAlpha = 0.0f;
                revealViewToYDrift = (height * 2) / 3;
                revealViewToXDrift = 0.0f;
            }
            PropertyValuesHolder panelAlpha = PropertyValuesHolder.ofFloat("alpha", revealViewToAlpha, 1.0f);
            PropertyValuesHolder panelDriftY = PropertyValuesHolder.ofFloat("translationY", revealViewToYDrift, 0.0f);
            PropertyValuesHolder panelDriftX = PropertyValuesHolder.ofFloat("translationX", revealViewToXDrift, 0.0f);
            ObjectAnimator panelAlphaAndDrift = ObjectAnimator.ofPropertyValuesHolder(revealView, panelAlpha, panelDriftY, panelDriftX);
            panelAlphaAndDrift.setDuration(revealDuration);
            panelAlphaAndDrift.setInterpolator(new LogDecelerateInterpolator(100, 0));
            layerViews.put(revealView, 1);
            animation.play(panelAlphaAndDrift);
            contentView.setVisibility(0);
            contentView.setAlpha(0.0f);
            contentView.setTranslationY(revealViewToYDrift);
            layerViews.put(contentView, 1);
            ObjectAnimator pageDrift = ObjectAnimator.ofFloat(contentView, "translationY", revealViewToYDrift, 0.0f);
            pageDrift.setDuration(revealDuration);
            pageDrift.setInterpolator(new LogDecelerateInterpolator(100, 0));
            pageDrift.setStartDelay(itemsAlphaStagger);
            animation.play(pageDrift);
            ObjectAnimator itemsAlpha = ObjectAnimator.ofFloat(contentView, "alpha", 0.0f, 1.0f);
            itemsAlpha.setDuration(revealDuration);
            itemsAlpha.setInterpolator(new AccelerateInterpolator(1.5f));
            itemsAlpha.setStartDelay(itemsAlphaStagger);
            animation.play(itemsAlpha);
            if (material) {
                float startRadius = pCb.getMaterialRevealViewStartFinalRadius();
                Animator.AnimatorListener listener = pCb.getMaterialRevealViewAnimatorListener(revealView, buttonView);
                Animator reveal = UiThreadCircularReveal.createCircularReveal(revealView, width / 2, height / 2, startRadius, revealRadius);
                reveal.setDuration(revealDuration);
                reveal.setInterpolator(new LogDecelerateInterpolator(100, 0));
                if (listener != null) {
                    reveal.addListener(listener);
                }
                animation.play(reveal);
            }
            animation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation2) {
                    LauncherStateTransitionAnimation.this.dispatchOnLauncherTransitionEnd(fromView, animated, false);
                    LauncherStateTransitionAnimation.this.dispatchOnLauncherTransitionEnd(toView, animated, false);
                    revealView.setVisibility(4);
                    for (View v : layerViews.keySet()) {
                        if (((Integer) layerViews.get(v)).intValue() == 1) {
                            v.setLayerType(0, null);
                        }
                    }
                    LauncherStateTransitionAnimation.this.cleanupAnimation();
                    pCb.onTransitionComplete();
                }
            });
            if (workspaceAnim != null) {
                animation.play(workspaceAnim);
            }
            animation.play(updateTransitionStepAnim);
            dispatchOnLauncherTransitionPrepare(fromView, animated, false);
            dispatchOnLauncherTransitionPrepare(toView, animated, false);
            Runnable startAnimRunnable = new Runnable() {
                @Override
                public void run() {
                    if (LauncherStateTransitionAnimation.this.mCurrentAnimation != animation) {
                        return;
                    }
                    LauncherStateTransitionAnimation.this.dispatchOnLauncherTransitionStart(fromView, animated, false);
                    LauncherStateTransitionAnimation.this.dispatchOnLauncherTransitionStart(toView, animated, false);
                    for (View v : layerViews.keySet()) {
                        if (((Integer) layerViews.get(v)).intValue() == 1) {
                            v.setLayerType(2, null);
                        }
                        if (Utilities.ATLEAST_LOLLIPOP && Utilities.isViewAttachedToWindow(v)) {
                            v.buildLayer();
                        }
                    }
                    toView.requestFocus();
                    animation.start();
                }
            };
            toView.bringToFront();
            toView.setVisibility(0);
            toView.post(startAnimRunnable);
            return animation;
        }
        toView.setTranslationX(0.0f);
        toView.setTranslationY(0.0f);
        toView.setScaleX(1.0f);
        toView.setScaleY(1.0f);
        toView.setVisibility(0);
        toView.bringToFront();
        contentView.setVisibility(0);
        dispatchOnLauncherTransitionPrepare(fromView, animated, false);
        dispatchOnLauncherTransitionStart(fromView, animated, false);
        dispatchOnLauncherTransitionEnd(fromView, animated, false);
        dispatchOnLauncherTransitionPrepare(toView, animated, false);
        dispatchOnLauncherTransitionStart(toView, animated, false);
        dispatchOnLauncherTransitionEnd(toView, animated, false);
        pCb.onTransitionComplete();
        return null;
    }

    private Animator dispatchOnLauncherTransitionStepAnim(final View fromView, final View toView) {
        ValueAnimator updateAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        updateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                LauncherStateTransitionAnimation.this.dispatchOnLauncherTransitionStep(fromView, animation.getAnimatedFraction());
                LauncherStateTransitionAnimation.this.dispatchOnLauncherTransitionStep(toView, animation.getAnimatedFraction());
            }
        });
        return updateAnimator;
    }

    private void startAnimationToWorkspaceFromAllApps(Workspace.State fromWorkspaceState, Workspace.State toWorkspaceState, int toWorkspacePage, boolean animated, Runnable onCompleteRunnable) {
        AllAppsContainerView appsView = this.mLauncher.getAppsView();
        PrivateTransitionCallbacks cb = new PrivateTransitionCallbacks(1.0f) {
            @Override
            float getMaterialRevealViewStartFinalRadius() {
                int allAppsButtonSize = LauncherStateTransitionAnimation.this.mLauncher.getDeviceProfile().allAppsButtonVisualSize;
                return allAppsButtonSize / 2;
            }

            @Override
            public AnimatorListenerAdapter getMaterialRevealViewAnimatorListener(final View revealView, final View allAppsButtonView) {
                return new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        allAppsButtonView.setVisibility(0);
                        allAppsButtonView.setAlpha(0.0f);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        revealView.setVisibility(4);
                        allAppsButtonView.setAlpha(1.0f);
                    }
                };
            }
        };
        this.mCurrentAnimation = startAnimationToWorkspaceFromOverlay(fromWorkspaceState, toWorkspaceState, toWorkspacePage, this.mLauncher.getAllAppsButton(), appsView, animated, onCompleteRunnable, cb);
    }

    private void startAnimationToWorkspaceFromWidgets(Workspace.State fromWorkspaceState, Workspace.State toWorkspaceState, int toWorkspacePage, boolean animated, Runnable onCompleteRunnable) {
        WidgetsContainerView widgetsView = this.mLauncher.getWidgetsView();
        PrivateTransitionCallbacks cb = new PrivateTransitionCallbacks(0.3f) {
            @Override
            public AnimatorListenerAdapter getMaterialRevealViewAnimatorListener(final View revealView, View widgetsButtonView) {
                return new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        revealView.setVisibility(4);
                    }
                };
            }
        };
        this.mCurrentAnimation = startAnimationToWorkspaceFromOverlay(fromWorkspaceState, toWorkspaceState, toWorkspacePage, this.mLauncher.getWidgetsButton(), widgetsView, animated, onCompleteRunnable, cb);
    }

    private AnimatorSet startAnimationToWorkspaceFromOverlay(Workspace.State fromWorkspaceState, Workspace.State toWorkspaceState, int toWorkspacePage, View buttonView, final BaseContainerView fromView, final boolean animated, final Runnable onCompleteRunnable, final PrivateTransitionCallbacks pCb) {
        float revealViewToYDrift;
        float revealViewToXDrift;
        TimeInterpolator decelerateInterpolator;
        final AnimatorSet animation = LauncherAnimUtils.createAnimatorSet();
        Resources res = this.mLauncher.getResources();
        boolean material = Utilities.ATLEAST_LOLLIPOP;
        int revealDuration = res.getInteger(R.integer.config_overlayRevealTime);
        int itemsAlphaStagger = res.getInteger(R.integer.config_overlayItemsAlphaStagger);
        final View toView = this.mLauncher.getWorkspace();
        final HashMap<View, Integer> layerViews = new HashMap<>();
        boolean initialized = buttonView != null;
        cancelAnimation();
        Animator workspaceAnim = this.mLauncher.startWorkspaceStateChangeAnimation(toWorkspaceState, toWorkspacePage, animated, layerViews);
        startWorkspaceSearchBarAnimation(toWorkspaceState, animated ? revealDuration : 0, animation);
        Animator updateTransitionStepAnim = dispatchOnLauncherTransitionStepAnim(fromView, toView);
        if (animated && initialized) {
            if (workspaceAnim != null) {
                animation.play(workspaceAnim);
            }
            animation.play(updateTransitionStepAnim);
            View revealView = fromView.getRevealView();
            final View contentView = fromView.getContentView();
            if (fromView.getVisibility() == 0) {
                int width = revealView.getMeasuredWidth();
                int height = revealView.getMeasuredHeight();
                float revealRadius = (float) Math.hypot(width / 2, height / 2);
                revealView.setVisibility(0);
                revealView.setAlpha(1.0f);
                revealView.setTranslationY(0.0f);
                layerViews.put(revealView, 1);
                if (material) {
                    int[] buttonViewToPanelDelta = Utilities.getCenterDeltaInScreenSpace(revealView, buttonView, null);
                    revealViewToYDrift = buttonViewToPanelDelta[1];
                    revealViewToXDrift = buttonViewToPanelDelta[0];
                } else {
                    revealViewToYDrift = (height * 2) / 3;
                    revealViewToXDrift = 0.0f;
                }
                if (material) {
                    decelerateInterpolator = new LogDecelerateInterpolator(100, 0);
                } else {
                    decelerateInterpolator = new DecelerateInterpolator(1.0f);
                }
                ObjectAnimator panelDriftY = ObjectAnimator.ofFloat(revealView, "translationY", 0.0f, revealViewToYDrift);
                panelDriftY.setDuration(revealDuration - 16);
                panelDriftY.setStartDelay(itemsAlphaStagger + 16);
                panelDriftY.setInterpolator(decelerateInterpolator);
                animation.play(panelDriftY);
                ObjectAnimator panelDriftX = ObjectAnimator.ofFloat(revealView, "translationX", 0.0f, revealViewToXDrift);
                panelDriftX.setDuration(revealDuration - 16);
                panelDriftX.setStartDelay(itemsAlphaStagger + 16);
                panelDriftX.setInterpolator(decelerateInterpolator);
                animation.play(panelDriftX);
                float revealViewToAlpha = !material ? 0.0f : pCb.materialRevealViewFinalAlpha;
                if (revealViewToAlpha != 1.0f) {
                    ObjectAnimator panelAlpha = ObjectAnimator.ofFloat(revealView, "alpha", 1.0f, revealViewToAlpha);
                    panelAlpha.setDuration(material ? revealDuration : 150);
                    panelAlpha.setStartDelay(material ? 0 : itemsAlphaStagger + 16);
                    panelAlpha.setInterpolator(decelerateInterpolator);
                    animation.play(panelAlpha);
                }
                layerViews.put(contentView, 1);
                ObjectAnimator pageDrift = ObjectAnimator.ofFloat(contentView, "translationY", 0.0f, revealViewToYDrift);
                contentView.setTranslationY(0.0f);
                pageDrift.setDuration(revealDuration - 16);
                pageDrift.setInterpolator(decelerateInterpolator);
                pageDrift.setStartDelay(itemsAlphaStagger + 16);
                animation.play(pageDrift);
                contentView.setAlpha(1.0f);
                ObjectAnimator itemsAlpha = ObjectAnimator.ofFloat(contentView, "alpha", 1.0f, 0.0f);
                itemsAlpha.setDuration(100L);
                itemsAlpha.setInterpolator(decelerateInterpolator);
                animation.play(itemsAlpha);
                if (material) {
                    float finalRadius = pCb.getMaterialRevealViewStartFinalRadius();
                    Animator.AnimatorListener listener = pCb.getMaterialRevealViewAnimatorListener(revealView, buttonView);
                    Animator reveal = UiThreadCircularReveal.createCircularReveal(revealView, width / 2, height / 2, revealRadius, finalRadius);
                    reveal.setInterpolator(new LogDecelerateInterpolator(100, 0));
                    reveal.setDuration(revealDuration);
                    reveal.setStartDelay(itemsAlphaStagger);
                    if (listener != null) {
                        reveal.addListener(listener);
                    }
                    animation.play(reveal);
                }
            }
            dispatchOnLauncherTransitionPrepare(fromView, animated, true);
            dispatchOnLauncherTransitionPrepare(toView, animated, true);
            animation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation2) {
                    fromView.setVisibility(8);
                    LauncherStateTransitionAnimation.this.dispatchOnLauncherTransitionEnd(fromView, animated, true);
                    LauncherStateTransitionAnimation.this.dispatchOnLauncherTransitionEnd(toView, animated, true);
                    if (onCompleteRunnable != null) {
                        onCompleteRunnable.run();
                    }
                    for (View v : layerViews.keySet()) {
                        if (((Integer) layerViews.get(v)).intValue() == 1) {
                            v.setLayerType(0, null);
                        }
                    }
                    if (contentView != null) {
                        contentView.setTranslationX(0.0f);
                        contentView.setTranslationY(0.0f);
                        contentView.setAlpha(1.0f);
                    }
                    LauncherStateTransitionAnimation.this.cleanupAnimation();
                    pCb.onTransitionComplete();
                }
            });
            Runnable startAnimRunnable = new Runnable() {
                @Override
                public void run() {
                    if (LauncherStateTransitionAnimation.this.mCurrentAnimation != animation) {
                        return;
                    }
                    LauncherStateTransitionAnimation.this.dispatchOnLauncherTransitionStart(fromView, animated, false);
                    LauncherStateTransitionAnimation.this.dispatchOnLauncherTransitionStart(toView, animated, false);
                    for (View v : layerViews.keySet()) {
                        if (((Integer) layerViews.get(v)).intValue() == 1) {
                            v.setLayerType(2, null);
                        }
                        if (Utilities.ATLEAST_LOLLIPOP && Utilities.isViewAttachedToWindow(v)) {
                            v.buildLayer();
                        }
                    }
                    animation.start();
                }
            };
            fromView.post(startAnimRunnable);
            return animation;
        }
        fromView.setVisibility(8);
        dispatchOnLauncherTransitionPrepare(fromView, animated, true);
        dispatchOnLauncherTransitionStart(fromView, animated, true);
        dispatchOnLauncherTransitionEnd(fromView, animated, true);
        dispatchOnLauncherTransitionPrepare(toView, animated, true);
        dispatchOnLauncherTransitionStart(toView, animated, true);
        dispatchOnLauncherTransitionEnd(toView, animated, true);
        pCb.onTransitionComplete();
        if (onCompleteRunnable != null) {
            onCompleteRunnable.run();
            return null;
        }
        return null;
    }

    private void startWorkspaceSearchBarAnimation(Workspace.State toWorkspaceState, int duration, AnimatorSet animation) {
        SearchDropTargetBar.State toSearchBarState = toWorkspaceState.searchDropTargetBarState;
        this.mLauncher.getSearchDropTargetBar().animateToState(toSearchBarState, duration, animation);
    }

    void dispatchOnLauncherTransitionPrepare(View view, boolean animated, boolean toWorkspace) {
        if (!(view instanceof LauncherTransitionable)) {
            return;
        }
        ((LauncherTransitionable) view).onLauncherTransitionPrepare(this.mLauncher, animated, toWorkspace);
    }

    void dispatchOnLauncherTransitionStart(View view, boolean animated, boolean toWorkspace) {
        if (view instanceof LauncherTransitionable) {
            ((LauncherTransitionable) view).onLauncherTransitionStart(this.mLauncher, animated, toWorkspace);
        }
        dispatchOnLauncherTransitionStep(view, 0.0f);
    }

    void dispatchOnLauncherTransitionStep(View view, float t) {
        if (!(view instanceof LauncherTransitionable)) {
            return;
        }
        ((LauncherTransitionable) view).onLauncherTransitionStep(this.mLauncher, t);
    }

    void dispatchOnLauncherTransitionEnd(View view, boolean animated, boolean toWorkspace) {
        if (view instanceof LauncherTransitionable) {
            ((LauncherTransitionable) view).onLauncherTransitionEnd(this.mLauncher, animated, toWorkspace);
        }
        dispatchOnLauncherTransitionStep(view, 1.0f);
    }

    private void cancelAnimation() {
        if (this.mCurrentAnimation == null) {
            return;
        }
        this.mCurrentAnimation.setDuration(0L);
        this.mCurrentAnimation.cancel();
        this.mCurrentAnimation = null;
    }

    void cleanupAnimation() {
        this.mCurrentAnimation = null;
    }
}
