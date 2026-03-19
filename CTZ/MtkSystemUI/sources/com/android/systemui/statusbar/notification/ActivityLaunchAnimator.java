package com.android.systemui.statusbar.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.MathUtils;
import android.view.IRemoteAnimationFinishedCallback;
import android.view.IRemoteAnimationRunner;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationTarget;
import com.android.systemui.Interpolators;
import com.android.systemui.shared.system.SurfaceControlCompat;
import com.android.systemui.shared.system.SyncRtSurfaceTransactionApplier;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationListContainer;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;

public class ActivityLaunchAnimator {
    private boolean mAnimationPending;
    private final NotificationListContainer mNotificationContainer;
    private final NotificationPanelView mNotificationPanel;
    private StatusBar mStatusBar;
    private final StatusBarWindowView mStatusBarWindow;
    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public final void run() {
            ActivityLaunchAnimator.lambda$new$0(this.f$0);
        }
    };

    public static void lambda$new$0(ActivityLaunchAnimator activityLaunchAnimator) {
        activityLaunchAnimator.setAnimationPending(false);
        activityLaunchAnimator.mStatusBar.collapsePanel(true);
    }

    public ActivityLaunchAnimator(StatusBarWindowView statusBarWindowView, StatusBar statusBar, NotificationPanelView notificationPanelView, NotificationListContainer notificationListContainer) {
        this.mNotificationPanel = notificationPanelView;
        this.mNotificationContainer = notificationListContainer;
        this.mStatusBarWindow = statusBarWindowView;
        this.mStatusBar = statusBar;
    }

    public RemoteAnimationAdapter getLaunchAnimation(ExpandableNotificationRow expandableNotificationRow, boolean z) {
        if (this.mStatusBar.getBarState() != 0 || z) {
            return null;
        }
        return new RemoteAnimationAdapter(new AnimationRunner(expandableNotificationRow), 400L, 250L);
    }

    public boolean isAnimationPending() {
        return this.mAnimationPending;
    }

    public void setLaunchResult(int i) {
        setAnimationPending((i == 2 || i == 0) && this.mStatusBar.getBarState() == 0);
    }

    private void setAnimationPending(boolean z) {
        this.mAnimationPending = z;
        this.mStatusBarWindow.setExpandAnimationPending(z);
        if (z) {
            this.mStatusBarWindow.postDelayed(this.mTimeoutRunnable, 500L);
        } else {
            this.mStatusBarWindow.removeCallbacks(this.mTimeoutRunnable);
        }
    }

    class AnimationRunner extends IRemoteAnimationRunner.Stub {
        private final ExpandableNotificationRow mSourceNotification;
        private final SyncRtSurfaceTransactionApplier mSyncRtTransactionApplier;
        private final Rect mWindowCrop = new Rect();
        private boolean mInstantCollapsePanel = true;
        private final ExpandAnimationParameters mParams = new ExpandAnimationParameters();

        public AnimationRunner(ExpandableNotificationRow expandableNotificationRow) {
            this.mSourceNotification = expandableNotificationRow;
            this.mSyncRtTransactionApplier = new SyncRtSurfaceTransactionApplier(this.mSourceNotification);
        }

        public void onAnimationStart(final RemoteAnimationTarget[] remoteAnimationTargetArr, final IRemoteAnimationFinishedCallback iRemoteAnimationFinishedCallback) throws RemoteException {
            this.mSourceNotification.post(new Runnable() {
                @Override
                public final void run() {
                    ActivityLaunchAnimator.AnimationRunner.lambda$onAnimationStart$0(this.f$0, remoteAnimationTargetArr, iRemoteAnimationFinishedCallback);
                }
            });
        }

        public static void lambda$onAnimationStart$0(AnimationRunner animationRunner, RemoteAnimationTarget[] remoteAnimationTargetArr, final IRemoteAnimationFinishedCallback iRemoteAnimationFinishedCallback) {
            final RemoteAnimationTarget primaryRemoteAnimationTarget = animationRunner.getPrimaryRemoteAnimationTarget(remoteAnimationTargetArr);
            if (primaryRemoteAnimationTarget == null) {
                ActivityLaunchAnimator.this.setAnimationPending(false);
                animationRunner.invokeCallback(iRemoteAnimationFinishedCallback);
                return;
            }
            boolean z = true;
            animationRunner.setExpandAnimationRunning(true);
            if (primaryRemoteAnimationTarget.position.y != 0 || primaryRemoteAnimationTarget.sourceContainerBounds.height() < ActivityLaunchAnimator.this.mNotificationPanel.getHeight()) {
                z = false;
            }
            animationRunner.mInstantCollapsePanel = z;
            if (!animationRunner.mInstantCollapsePanel) {
                ActivityLaunchAnimator.this.mNotificationPanel.collapseWithDuration(400);
            }
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            animationRunner.mParams.startPosition = animationRunner.mSourceNotification.getLocationOnScreen();
            animationRunner.mParams.startTranslationZ = animationRunner.mSourceNotification.getTranslationZ();
            animationRunner.mParams.startClipTopAmount = animationRunner.mSourceNotification.getClipTopAmount();
            if (animationRunner.mSourceNotification.isChildInGroup()) {
                int clipTopAmount = animationRunner.mSourceNotification.getNotificationParent().getClipTopAmount();
                animationRunner.mParams.parentStartClipTopAmount = clipTopAmount;
                if (clipTopAmount != 0) {
                    float translationY = clipTopAmount - animationRunner.mSourceNotification.getTranslationY();
                    if (translationY > 0.0f) {
                        animationRunner.mParams.startClipTopAmount = (int) Math.ceil(translationY);
                    }
                }
            }
            final int iWidth = primaryRemoteAnimationTarget.sourceContainerBounds.width();
            final int actualHeight = animationRunner.mSourceNotification.getActualHeight() - animationRunner.mSourceNotification.getClipBottomAmount();
            final int width = animationRunner.mSourceNotification.getWidth();
            valueAnimatorOfFloat.setDuration(400L);
            valueAnimatorOfFloat.setInterpolator(Interpolators.LINEAR);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    AnimationRunner.this.mParams.linearProgress = valueAnimator.getAnimatedFraction();
                    float interpolation = Interpolators.FAST_OUT_SLOW_IN.getInterpolation(AnimationRunner.this.mParams.linearProgress);
                    int iLerp = (int) MathUtils.lerp(width, iWidth, interpolation);
                    AnimationRunner.this.mParams.left = (int) ((iWidth - iLerp) / 2.0f);
                    AnimationRunner.this.mParams.right = AnimationRunner.this.mParams.left + iLerp;
                    AnimationRunner.this.mParams.top = (int) MathUtils.lerp(AnimationRunner.this.mParams.startPosition[1], primaryRemoteAnimationTarget.position.y, interpolation);
                    AnimationRunner.this.mParams.bottom = (int) MathUtils.lerp(AnimationRunner.this.mParams.startPosition[1] + actualHeight, primaryRemoteAnimationTarget.position.y + primaryRemoteAnimationTarget.sourceContainerBounds.bottom, interpolation);
                    AnimationRunner.this.applyParamsToWindow(primaryRemoteAnimationTarget);
                    AnimationRunner.this.applyParamsToNotification(AnimationRunner.this.mParams);
                    AnimationRunner.this.applyParamsToNotificationList(AnimationRunner.this.mParams);
                }
            });
            valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    AnimationRunner.this.setExpandAnimationRunning(false);
                    if (AnimationRunner.this.mInstantCollapsePanel) {
                        ActivityLaunchAnimator.this.mStatusBar.collapsePanel(false);
                    }
                    AnimationRunner.this.invokeCallback(iRemoteAnimationFinishedCallback);
                }
            });
            valueAnimatorOfFloat.start();
            ActivityLaunchAnimator.this.setAnimationPending(false);
        }

        private void invokeCallback(IRemoteAnimationFinishedCallback iRemoteAnimationFinishedCallback) {
            try {
                iRemoteAnimationFinishedCallback.onAnimationFinished();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        private RemoteAnimationTarget getPrimaryRemoteAnimationTarget(RemoteAnimationTarget[] remoteAnimationTargetArr) {
            for (RemoteAnimationTarget remoteAnimationTarget : remoteAnimationTargetArr) {
                if (remoteAnimationTarget.mode == 0) {
                    return remoteAnimationTarget;
                }
            }
            return null;
        }

        private void setExpandAnimationRunning(boolean z) {
            ActivityLaunchAnimator.this.mNotificationPanel.setLaunchingNotification(z);
            this.mSourceNotification.setExpandAnimationRunning(z);
            ActivityLaunchAnimator.this.mStatusBarWindow.setExpandAnimationRunning(z);
            ActivityLaunchAnimator.this.mNotificationContainer.setExpandingNotification(z ? this.mSourceNotification : null);
            if (!z) {
                applyParamsToNotification(null);
                applyParamsToNotificationList(null);
            }
        }

        private void applyParamsToNotificationList(ExpandAnimationParameters expandAnimationParameters) {
            ActivityLaunchAnimator.this.mNotificationContainer.applyExpandAnimationParams(expandAnimationParameters);
            ActivityLaunchAnimator.this.mNotificationPanel.applyExpandAnimationParams(expandAnimationParameters);
        }

        private void applyParamsToNotification(ExpandAnimationParameters expandAnimationParameters) {
            this.mSourceNotification.applyExpandAnimationParams(expandAnimationParameters);
        }

        private void applyParamsToWindow(RemoteAnimationTarget remoteAnimationTarget) {
            Matrix matrix = new Matrix();
            matrix.postTranslate(0.0f, this.mParams.top - remoteAnimationTarget.position.y);
            this.mWindowCrop.set(this.mParams.left, 0, this.mParams.right, this.mParams.getHeight());
            this.mSyncRtTransactionApplier.scheduleApply(new SyncRtSurfaceTransactionApplier.SurfaceParams(new SurfaceControlCompat(remoteAnimationTarget.leash), 1.0f, matrix, this.mWindowCrop, remoteAnimationTarget.prefixOrderIndex));
        }

        public void onAnimationCancelled() throws RemoteException {
            this.mSourceNotification.post(new Runnable() {
                @Override
                public final void run() {
                    ActivityLaunchAnimator.AnimationRunner.lambda$onAnimationCancelled$1(this.f$0);
                }
            });
        }

        public static void lambda$onAnimationCancelled$1(AnimationRunner animationRunner) {
            ActivityLaunchAnimator.this.setAnimationPending(false);
            ActivityLaunchAnimator.this.mStatusBar.onLaunchAnimationCancelled();
        }
    }

    public static class ExpandAnimationParameters {
        int bottom;
        int left;
        float linearProgress;
        int parentStartClipTopAmount;
        int right;
        int startClipTopAmount;
        int[] startPosition;
        float startTranslationZ;
        int top;

        public int getTop() {
            return this.top;
        }

        public int getWidth() {
            return this.right - this.left;
        }

        public int getHeight() {
            return this.bottom - this.top;
        }

        public int getTopChange() {
            int iLerp;
            if (this.startClipTopAmount != 0.0f) {
                iLerp = (int) MathUtils.lerp(0.0f, this.startClipTopAmount, Interpolators.FAST_OUT_SLOW_IN.getInterpolation(this.linearProgress));
            } else {
                iLerp = 0;
            }
            return Math.min((this.top - this.startPosition[1]) - iLerp, 0);
        }

        public float getProgress() {
            return this.linearProgress;
        }

        public float getProgress(long j, long j2) {
            return MathUtils.constrain(((this.linearProgress * 400.0f) - j) / j2, 0.0f, 1.0f);
        }

        public int getStartClipTopAmount() {
            return this.startClipTopAmount;
        }

        public int getParentStartClipTopAmount() {
            return this.parentStartClipTopAmount;
        }

        public float getStartTranslationZ() {
            return this.startTranslationZ;
        }
    }
}
