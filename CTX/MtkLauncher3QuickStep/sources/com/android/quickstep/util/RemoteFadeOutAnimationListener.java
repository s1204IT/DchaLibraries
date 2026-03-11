package com.android.quickstep.util;

import android.animation.ValueAnimator;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.TransactionCompat;

public class RemoteFadeOutAnimationListener implements ValueAnimator.AnimatorUpdateListener {
    private boolean mFirstFrame = true;
    private final RemoteAnimationTargetSet mTarget;

    public RemoteFadeOutAnimationListener(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
        this.mTarget = new RemoteAnimationTargetSet(remoteAnimationTargetCompatArr, 1);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        TransactionCompat transactionCompat = new TransactionCompat();
        if (this.mFirstFrame) {
            RemoteAnimationProvider.prepareTargetsForFirstFrame(this.mTarget.unfilteredApps, transactionCompat, 1);
            this.mFirstFrame = false;
        }
        float animatedFraction = 1.0f - valueAnimator.getAnimatedFraction();
        for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : this.mTarget.apps) {
            transactionCompat.setAlpha(remoteAnimationTargetCompat.leash, animatedFraction);
        }
        transactionCompat.apply();
    }
}
