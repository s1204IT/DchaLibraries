package com.android.launcher3.allapps;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.os.Handler;
import android.view.MotionEvent;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.states.InternalStateHandler;

/* loaded from: classes.dex */
public class DiscoveryBounce extends AbstractFloatingView {
    private static final long DELAY_MS = 450;
    public static final String HOME_BOUNCE_SEEN = "launcher.apps_view_shown";
    public static final String SHELF_BOUNCE_SEEN = "launcher.shelf_bounce_seen";
    private final Animator mDiscoBounceAnimation;
    private final Launcher mLauncher;

    public DiscoveryBounce(Launcher launcher, float f) {
        super(launcher, null);
        this.mLauncher = launcher;
        AllAppsTransitionController allAppsController = this.mLauncher.getAllAppsController();
        this.mDiscoBounceAnimation = AnimatorInflater.loadAnimator(launcher, R.animator.discovery_bounce);
        this.mDiscoBounceAnimation.setTarget(new VerticalProgressWrapper(allAppsController, f));
        this.mDiscoBounceAnimation.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.allapps.DiscoveryBounce.1
            AnonymousClass1() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                DiscoveryBounce.this.handleClose(false);
            }
        });
        this.mDiscoBounceAnimation.addListener(allAppsController.getProgressAnimatorListener());
    }

    /* renamed from: com.android.launcher3.allapps.DiscoveryBounce$1 */
    class AnonymousClass1 extends AnimatorListenerAdapter {
        AnonymousClass1() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            DiscoveryBounce.this.handleClose(false);
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mDiscoBounceAnimation.start();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mDiscoBounceAnimation.isRunning()) {
            this.mDiscoBounceAnimation.end();
        }
    }

    @Override // com.android.launcher3.AbstractFloatingView
    public boolean onBackPressed() {
        super.onBackPressed();
        return false;
    }

    @Override // com.android.launcher3.util.TouchController
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        handleClose(false);
        return false;
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected void handleClose(boolean z) {
        if (this.mIsOpen) {
            this.mIsOpen = false;
            this.mLauncher.getDragLayer().removeView(this);
            this.mLauncher.getAllAppsController().setProgress(this.mLauncher.getStateManager().getState().getVerticalProgress(this.mLauncher));
        }
    }

    @Override // com.android.launcher3.AbstractFloatingView
    public void logActionCommand(int i) {
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected boolean isOfType(int i) {
        return (i & 64) != 0;
    }

    private void show(int i) {
        this.mIsOpen = true;
        this.mLauncher.getDragLayer().addView(this);
        this.mLauncher.getUserEventDispatcher().logActionBounceTip(i);
    }

    public static void showForHomeIfNeeded(Launcher launcher) {
        showForHomeIfNeeded(launcher, true);
    }

    private static void showForHomeIfNeeded(final Launcher launcher, boolean z) {
        if (!launcher.isInState(LauncherState.NORMAL) || launcher.getSharedPrefs().getBoolean(HOME_BOUNCE_SEEN, false) || AbstractFloatingView.getTopOpenView(launcher) != null || UserManagerCompat.getInstance(launcher).isDemoUser() || ActivityManager.isRunningInTestHarness()) {
            return;
        }
        if (z) {
            new Handler().postDelayed(new Runnable() { // from class: com.android.launcher3.allapps.-$$Lambda$DiscoveryBounce$UWP5iGtDzA1k_-2kVLNOLEwtkow
                @Override // java.lang.Runnable
                public final void run() {
                    DiscoveryBounce.showForHomeIfNeeded(launcher, false);
                }
            }, DELAY_MS);
        } else {
            new DiscoveryBounce(launcher, 0.0f).show(2);
        }
    }

    public static void showForOverviewIfNeeded(Launcher launcher) {
        showForOverviewIfNeeded(launcher, true);
    }

    private static void showForOverviewIfNeeded(final Launcher launcher, boolean z) {
        if (!launcher.isInState(LauncherState.OVERVIEW) || !launcher.hasBeenResumed() || launcher.isForceInvisible() || launcher.getDeviceProfile().isVerticalBarLayout() || launcher.getSharedPrefs().getBoolean(SHELF_BOUNCE_SEEN, false) || UserManagerCompat.getInstance(launcher).isDemoUser() || ActivityManager.isRunningInTestHarness()) {
            return;
        }
        if (z) {
            new Handler().postDelayed(new Runnable() { // from class: com.android.launcher3.allapps.-$$Lambda$DiscoveryBounce$nH_kGUpN9-eIXSNWizqgojM7CJQ
                @Override // java.lang.Runnable
                public final void run() {
                    DiscoveryBounce.showForOverviewIfNeeded(launcher, false);
                }
            }, DELAY_MS);
        } else {
            if (InternalStateHandler.hasPending() || AbstractFloatingView.getTopOpenView(launcher) != null) {
                return;
            }
            new DiscoveryBounce(launcher, 1.0f - LauncherState.OVERVIEW.getVerticalProgress(launcher)).show(7);
        }
    }

    public static class VerticalProgressWrapper {
        private final AllAppsTransitionController mController;
        private final float mDelta;

        /* synthetic */ VerticalProgressWrapper(AllAppsTransitionController allAppsTransitionController, float f, AnonymousClass1 anonymousClass1) {
            this(allAppsTransitionController, f);
        }

        private VerticalProgressWrapper(AllAppsTransitionController allAppsTransitionController, float f) {
            this.mController = allAppsTransitionController;
            this.mDelta = f;
        }

        public float getProgress() {
            return this.mController.getProgress() + this.mDelta;
        }

        public void setProgress(float f) {
            this.mController.setProgress(f - this.mDelta);
        }
    }
}
