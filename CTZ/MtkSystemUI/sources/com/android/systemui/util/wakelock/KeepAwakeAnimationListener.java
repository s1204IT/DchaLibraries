package com.android.systemui.util.wakelock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.animation.Animation;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.util.Assert;

public class KeepAwakeAnimationListener extends AnimatorListenerAdapter implements Animation.AnimationListener {

    @VisibleForTesting
    static WakeLock sWakeLock;

    public KeepAwakeAnimationListener(Context context) {
        Assert.isMainThread();
        if (sWakeLock == null) {
            sWakeLock = WakeLock.createPartial(context, "animation");
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {
        onStart();
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        onEnd();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationStart(Animator animator) {
        onStart();
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        onEnd();
    }

    private void onStart() {
        Assert.isMainThread();
        sWakeLock.acquire();
    }

    private void onEnd() {
        Assert.isMainThread();
        sWakeLock.release();
    }
}
