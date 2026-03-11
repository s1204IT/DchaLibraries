package com.android.quickstep.util;

import android.animation.AnimatorSet;
import android.app.ActivityOptions;
import android.os.Handler;
import com.android.launcher3.LauncherAnimationRunner;
import com.android.systemui.shared.system.ActivityOptionsCompat;
import com.android.systemui.shared.system.RemoteAnimationAdapterCompat;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.TransactionCompat;

@FunctionalInterface
public interface RemoteAnimationProvider {
    AnimatorSet createWindowAnimation(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr);

    default ActivityOptions toActivityOptions(Handler handler, long j) {
        return ActivityOptionsCompat.makeRemoteAnimation(new RemoteAnimationAdapterCompat(new LauncherAnimationRunner(handler, false) {
            @Override
            public void onCreateAnimation(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, LauncherAnimationRunner.AnimationResult animationResult) {
                animationResult.setAnimation(RemoteAnimationProvider.this.createWindowAnimation(remoteAnimationTargetCompatArr));
            }
        }, j, 0L));
    }

    static void prepareTargetsForFirstFrame(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, TransactionCompat transactionCompat, int i) {
        int i2;
        for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : remoteAnimationTargetCompatArr) {
            if (remoteAnimationTargetCompat.mode == i) {
                i2 = Integer.MAX_VALUE;
            } else {
                i2 = remoteAnimationTargetCompat.prefixOrderIndex;
            }
            transactionCompat.setLayer(remoteAnimationTargetCompat.leash, i2);
            transactionCompat.show(remoteAnimationTargetCompat.leash);
        }
    }
}
