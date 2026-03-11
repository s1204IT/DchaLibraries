package com.android.launcher3;

import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.CancellationSignal;
import android.os.Handler;
import com.android.launcher3.states.InternalStateHandler;
import com.android.quickstep.ActivityControlHelper;
import com.android.quickstep.OverviewCallbacks;
import com.android.quickstep.util.RemoteAnimationProvider;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import java.util.function.BiPredicate;

@TargetApi(28)
public class LauncherInitListener extends InternalStateHandler implements ActivityControlHelper.ActivityInitListener {
    private final BiPredicate<Launcher, Boolean> mOnInitListener;
    private RemoteAnimationProvider mRemoteAnimationProvider;

    public LauncherInitListener(BiPredicate<Launcher, Boolean> biPredicate) {
        this.mOnInitListener = biPredicate;
    }

    @Override
    protected boolean init(final Launcher launcher, boolean z) {
        if (this.mRemoteAnimationProvider != null) {
            LauncherAppTransitionManagerImpl launcherAppTransitionManagerImpl = (LauncherAppTransitionManagerImpl) launcher.getAppTransitionManager();
            final CancellationSignal cancellationSignal = new CancellationSignal();
            launcherAppTransitionManagerImpl.setRemoteAnimationProvider(new RemoteAnimationProvider() {
                @Override
                public final AnimatorSet createWindowAnimation(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
                    return LauncherInitListener.lambda$init$0(this.f$0, cancellationSignal, launcher, remoteAnimationTargetCompatArr);
                }
            }, cancellationSignal);
        }
        OverviewCallbacks.get(launcher).onInitOverviewTransition();
        return this.mOnInitListener.test(launcher, Boolean.valueOf(z));
    }

    public static AnimatorSet lambda$init$0(LauncherInitListener launcherInitListener, CancellationSignal cancellationSignal, Launcher launcher, RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
        cancellationSignal.cancel();
        RemoteAnimationProvider remoteAnimationProvider = launcherInitListener.mRemoteAnimationProvider;
        launcherInitListener.mRemoteAnimationProvider = null;
        if (remoteAnimationProvider == null || !launcher.getStateManager().getState().overviewUi) {
            return null;
        }
        return remoteAnimationProvider.createWindowAnimation(remoteAnimationTargetCompatArr);
    }

    @Override
    public void register() {
        initWhenReady();
    }

    @Override
    public void unregister() {
        this.mRemoteAnimationProvider = null;
        clearReference();
    }

    @Override
    public void registerAndStartActivity(Intent intent, RemoteAnimationProvider remoteAnimationProvider, Context context, Handler handler, long j) {
        this.mRemoteAnimationProvider = remoteAnimationProvider;
        register();
        context.startActivity(addToIntent(new Intent(intent)), remoteAnimationProvider.toActivityOptions(handler, j).toBundle());
    }
}
