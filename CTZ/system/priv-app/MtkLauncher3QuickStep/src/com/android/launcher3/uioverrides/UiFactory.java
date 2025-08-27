package com.android.launcher3.uioverrides;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;
import android.util.Base64;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherAppTransitionManagerImpl;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherStateManager;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.DiscoveryBounce;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.util.TouchController;
import com.android.quickstep.OverviewInteractionState;
import com.android.quickstep.RecentsModel;
import com.android.quickstep.util.RemoteAnimationProvider;
import com.android.quickstep.util.RemoteFadeOutAnimationListener;
import com.android.quickstep.views.RecentsView;
import com.android.systemui.shared.system.ActivityCompat;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.WindowManagerWrapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.zip.Deflater;

/* loaded from: classes.dex */
public class UiFactory {
    public static TouchController[] createTouchControllers(Launcher launcher) {
        if (!OverviewInteractionState.getInstance(launcher).isSwipeUpGestureEnabled()) {
            return new TouchController[]{launcher.getDragController(), new OverviewToAllAppsTouchController(launcher), new LauncherTaskViewController(launcher)};
        }
        if (launcher.getDeviceProfile().isVerticalBarLayout()) {
            return new TouchController[]{launcher.getDragController(), new OverviewToAllAppsTouchController(launcher), new LandscapeEdgeSwipeController(launcher), new LauncherTaskViewController(launcher)};
        }
        return new TouchController[]{launcher.getDragController(), new PortraitStatesTouchController(launcher), new LauncherTaskViewController(launcher)};
    }

    public static void setOnTouchControllersChangedListener(Context context, Runnable runnable) {
        OverviewInteractionState.getInstance(context).setOnSwipeUpSettingChangedListener(runnable);
    }

    public static LauncherStateManager.StateHandler[] getStateHandler(Launcher launcher) {
        return new LauncherStateManager.StateHandler[]{launcher.getAllAppsController(), launcher.getWorkspace(), new RecentsViewStateController(launcher), new BackButtonAlphaHandler(launcher)};
    }

    public static void onLauncherStateOrFocusChanged(Launcher launcher) {
        boolean z = launcher != null && launcher.getStateManager().getState().hideBackButton && launcher.hasWindowFocus();
        if (z) {
            z = AbstractFloatingView.getTopOpenViewWithType(launcher, 415) == null;
        }
        OverviewInteractionState.getInstance(launcher).setBackButtonAlpha(z ? 0.0f : 1.0f, true);
    }

    public static void resetOverview(Launcher launcher) {
        ((RecentsView) launcher.getOverviewPanel()).reset();
    }

    public static void onCreate(final Launcher launcher) {
        if (!launcher.getSharedPrefs().getBoolean(DiscoveryBounce.HOME_BOUNCE_SEEN, false)) {
            launcher.getStateManager().addStateListener(new LauncherStateManager.StateListener() { // from class: com.android.launcher3.uioverrides.UiFactory.1
                @Override // com.android.launcher3.LauncherStateManager.StateListener
                public void onStateSetImmediately(LauncherState launcherState) {
                }

                @Override // com.android.launcher3.LauncherStateManager.StateListener
                public void onStateTransitionStart(LauncherState launcherState) {
                }

                @Override // com.android.launcher3.LauncherStateManager.StateListener
                public void onStateTransitionComplete(LauncherState launcherState) {
                    boolean zIsSwipeUpGestureEnabled = OverviewInteractionState.getInstance(launcher).isSwipeUpGestureEnabled();
                    LauncherState lastState = launcher.getStateManager().getLastState();
                    if ((zIsSwipeUpGestureEnabled && launcherState == LauncherState.OVERVIEW) || (!zIsSwipeUpGestureEnabled && launcherState == LauncherState.ALL_APPS && lastState == LauncherState.NORMAL)) {
                        launcher.getSharedPrefs().edit().putBoolean(DiscoveryBounce.HOME_BOUNCE_SEEN, true).apply();
                        launcher.getStateManager().removeStateListener(this);
                    }
                }
            });
        }
        if (!launcher.getSharedPrefs().getBoolean(DiscoveryBounce.SHELF_BOUNCE_SEEN, false)) {
            launcher.getStateManager().addStateListener(new LauncherStateManager.StateListener() { // from class: com.android.launcher3.uioverrides.UiFactory.2
                @Override // com.android.launcher3.LauncherStateManager.StateListener
                public void onStateSetImmediately(LauncherState launcherState) {
                }

                @Override // com.android.launcher3.LauncherStateManager.StateListener
                public void onStateTransitionStart(LauncherState launcherState) {
                }

                @Override // com.android.launcher3.LauncherStateManager.StateListener
                public void onStateTransitionComplete(LauncherState launcherState) {
                    LauncherState lastState = launcher.getStateManager().getLastState();
                    if (launcherState == LauncherState.ALL_APPS && lastState == LauncherState.OVERVIEW) {
                        launcher.getSharedPrefs().edit().putBoolean(DiscoveryBounce.SHELF_BOUNCE_SEEN, true).apply();
                        launcher.getStateManager().removeStateListener(this);
                    }
                }
            });
        }
    }

    public static void onStart(Context context) {
        RecentsModel recentsModel = RecentsModel.getInstance(context);
        if (recentsModel != null) {
            recentsModel.onStart();
        }
    }

    public static void onLauncherStateOrResumeChanged(Launcher launcher) {
        LauncherState state = launcher.getStateManager().getState();
        DeviceProfile deviceProfile = launcher.getDeviceProfile();
        WindowManagerWrapper.getInstance().setShelfHeight((state == LauncherState.ALL_APPS || !launcher.isUserActive() || deviceProfile.isVerticalBarLayout()) ? false : true, deviceProfile.hotseatBarSizePx);
        if (state == LauncherState.NORMAL) {
            ((RecentsView) launcher.getOverviewPanel()).setSwipeDownShouldLaunchApp(false);
        }
    }

    public static void onTrimMemory(Context context, int i) {
        RecentsModel recentsModel = RecentsModel.getInstance(context);
        if (recentsModel != null) {
            recentsModel.onTrimMemory(i);
        }
    }

    public static void useFadeOutAnimationForLauncherStart(Launcher launcher, final CancellationSignal cancellationSignal) {
        ((LauncherAppTransitionManagerImpl) launcher.getAppTransitionManager()).setRemoteAnimationProvider(new RemoteAnimationProvider() { // from class: com.android.launcher3.uioverrides.-$$Lambda$UiFactory$Og3-Yu69fZtMHNRi5ShuaZAcOjI
            @Override // com.android.quickstep.util.RemoteAnimationProvider
            public final AnimatorSet createWindowAnimation(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
                return UiFactory.lambda$useFadeOutAnimationForLauncherStart$0(cancellationSignal, remoteAnimationTargetCompatArr);
            }
        }, cancellationSignal);
    }

    static /* synthetic */ AnimatorSet lambda$useFadeOutAnimationForLauncherStart$0(CancellationSignal cancellationSignal, RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
        cancellationSignal.cancel();
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(1.0f, 0.0f);
        valueAnimatorOfFloat.addUpdateListener(new RemoteFadeOutAnimationListener(remoteAnimationTargetCompatArr));
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(valueAnimatorOfFloat);
        return animatorSet;
    }

    public static boolean dumpActivity(Activity activity, PrintWriter printWriter) {
        if (!Utilities.IS_DEBUG_DEVICE) {
            return false;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (!new ActivityCompat(activity).encodeViewHierarchy(byteArrayOutputStream)) {
            return false;
        }
        Deflater deflater = new Deflater();
        deflater.setInput(byteArrayOutputStream.toByteArray());
        deflater.finish();
        byteArrayOutputStream.reset();
        byte[] bArr = new byte[1024];
        while (!deflater.finished()) {
            byteArrayOutputStream.write(bArr, 0, deflater.deflate(bArr));
        }
        printWriter.println("--encoded-view-dump-v0--");
        printWriter.println(Base64.encodeToString(byteArrayOutputStream.toByteArray(), 3));
        return true;
    }

    public static void prepareToShowOverview(Launcher launcher) {
        RecentsView recentsView = (RecentsView) launcher.getOverviewPanel();
        if (recentsView.getVisibility() != 0 || recentsView.getContentAlpha() == 0.0f) {
            LauncherAnimUtils.SCALE_PROPERTY.set(recentsView, Float.valueOf(1.33f));
        }
    }

    private static class LauncherTaskViewController extends TaskViewTouchController<Launcher> {
        public LauncherTaskViewController(Launcher launcher) {
            super(launcher);
        }

        @Override // com.android.launcher3.uioverrides.TaskViewTouchController
        protected boolean isRecentsInteractive() {
            return ((Launcher) this.mActivity).isInState(LauncherState.OVERVIEW);
        }

        @Override // com.android.launcher3.uioverrides.TaskViewTouchController
        protected void onUserControlledAnimationCreated(AnimatorPlaybackController animatorPlaybackController) {
            ((Launcher) this.mActivity).getStateManager().setCurrentUserControlledAnimation(animatorPlaybackController);
        }
    }
}
