package com.android.systemui.shared.system;

/* loaded from: classes.dex */
public interface RemoteAnimationRunnerCompat {
    void onAnimationCancelled();

    void onAnimationStart(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, Runnable runnable);
}
