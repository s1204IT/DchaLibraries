package com.android.quickstep;

import android.animation.ValueAnimator;
import android.view.Surface;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AllAppsTransitionController;
import com.android.launcher3.allapps.DiscoveryBounce;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.util.FlingBlockCheck;
import com.android.quickstep.util.RemoteAnimationTargetSet;
import com.android.quickstep.views.RecentsView;
import com.android.systemui.shared.recents.utilities.Utilities;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.TransactionCompat;

public class LongSwipeHelper {
    private static final float SWIPE_DURATION_MULTIPLIER = Math.min(2.0f, 2.0f);
    private AnimatorPlaybackController mAnimator;
    private final Launcher mLauncher;
    private final RemoteAnimationTargetSet mTargetSet;
    private float mMaxSwipeDistance = 1.0f;
    private FlingBlockCheck mFlingBlockCheck = new FlingBlockCheck();

    LongSwipeHelper(Launcher launcher, RemoteAnimationTargetSet remoteAnimationTargetSet) {
        this.mLauncher = launcher;
        this.mTargetSet = remoteAnimationTargetSet;
        init();
    }

    private void init() {
        setTargetAlpha(0.0f, true);
        this.mFlingBlockCheck.blockFling();
        AllAppsTransitionController allAppsController = this.mLauncher.getAllAppsController();
        this.mMaxSwipeDistance = Math.max(1.0f, allAppsController.getProgress() * allAppsController.getShiftRange());
        this.mAnimator = this.mLauncher.getStateManager().createAnimationToNewWorkspace(LauncherState.ALL_APPS, Math.round(2.0f * this.mMaxSwipeDistance));
        this.mAnimator.dispatchOnStart();
    }

    public void onMove(float f) {
        this.mAnimator.setPlayFraction(f / this.mMaxSwipeDistance);
        this.mFlingBlockCheck.onEvent();
    }

    public void destroy() {
        setTargetAlpha(1.0f, false);
        this.mLauncher.getStateManager().goToState(LauncherState.OVERVIEW, false);
    }

    public void end(float f, boolean z, final Runnable runnable) {
        final boolean z2;
        float f2;
        float progressFraction = this.mAnimator.getProgressFraction();
        boolean z3 = z && this.mFlingBlockCheck.isBlocked();
        final boolean z4 = z3 ? false : z;
        long jMin = 350;
        if (!z4) {
            boolean z5 = progressFraction > 0.5f;
            f2 = z5 ? 1.0f : 0.0f;
            jMin = Math.min(350L, Math.abs(Math.round((f2 - progressFraction) * 350.0f * SWIPE_DURATION_MULTIPLIER)));
            z2 = z5;
        } else {
            z2 = f < 0.0f;
            f2 = z2 ? 1.0f : 0.0f;
            if (Math.abs(f) > this.mLauncher.getResources().getDimension(R.dimen.quickstep_fling_min_velocity) && this.mMaxSwipeDistance > 0.0f) {
                jMin = Math.min(350L, 2 * ((long) Math.round(1000.0f * Math.abs(((f2 - progressFraction) * this.mMaxSwipeDistance) / f))));
            }
        }
        if (z3 && !z2) {
            jMin *= (long) LauncherAnimUtils.blockedFlingDurationFactor(0.0f);
        }
        this.mAnimator.setEndAction(new Runnable() {
            @Override
            public final void run() {
                this.f$0.onSwipeAnimationComplete(z2, z4, runnable);
            }
        });
        ValueAnimator animationPlayer = this.mAnimator.getAnimationPlayer();
        animationPlayer.setDuration(jMin).setInterpolator(Interpolators.DEACCEL);
        animationPlayer.setFloatValues(progressFraction, f2);
        animationPlayer.start();
    }

    private void setTargetAlpha(float f, boolean z) {
        Surface surface = Utilities.getSurface(this.mLauncher.getDragLayer());
        long nextFrameNumber = (!z || surface == null) ? -1L : Utilities.getNextFrameNumber(surface);
        if (z) {
            if (nextFrameNumber != -1) {
                this.mLauncher.getDragLayer().invalidate();
            } else {
                z = false;
            }
        }
        TransactionCompat transactionCompat = new TransactionCompat();
        for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : this.mTargetSet.apps) {
            if (!remoteAnimationTargetCompat.isNotInRecents && remoteAnimationTargetCompat.activityType != 2) {
                transactionCompat.setAlpha(remoteAnimationTargetCompat.leash, f);
                if (z) {
                    transactionCompat.deferTransactionUntil(remoteAnimationTargetCompat.leash, surface, nextFrameNumber);
                }
            }
        }
        transactionCompat.setEarlyWakeup();
        transactionCompat.apply();
    }

    private void onSwipeAnimationComplete(boolean z, boolean z2, Runnable runnable) {
        this.mLauncher.getStateManager().goToState(z ? LauncherState.ALL_APPS : LauncherState.OVERVIEW, false);
        if (!z) {
            DiscoveryBounce.showForOverviewIfNeeded(this.mLauncher);
            ((RecentsView) this.mLauncher.getOverviewPanel()).setSwipeDownShouldLaunchApp(true);
        }
        this.mLauncher.getUserEventDispatcher().logStateChangeAction(z2 ? 4 : 3, 1, 11, 13, z ? 4 : 12, 0);
        runnable.run();
    }
}
