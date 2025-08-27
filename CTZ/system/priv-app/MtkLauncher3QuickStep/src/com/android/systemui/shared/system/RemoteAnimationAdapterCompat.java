package com.android.systemui.shared.system;

import android.os.RemoteException;
import android.util.Log;
import android.view.IRemoteAnimationFinishedCallback;
import android.view.IRemoteAnimationRunner;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationTarget;

/* loaded from: classes.dex */
public class RemoteAnimationAdapterCompat {
    private final RemoteAnimationAdapter mWrapped;

    public RemoteAnimationAdapterCompat(RemoteAnimationRunnerCompat runner, long duration, long statusBarTransitionDelay) {
        this.mWrapped = new RemoteAnimationAdapter(wrapRemoteAnimationRunner(runner), duration, statusBarTransitionDelay);
    }

    RemoteAnimationAdapter getWrapped() {
        return this.mWrapped;
    }

    /* renamed from: com.android.systemui.shared.system.RemoteAnimationAdapterCompat$1 */
    class AnonymousClass1 extends IRemoteAnimationRunner.Stub {
        AnonymousClass1() {
        }

        public void onAnimationStart(RemoteAnimationTarget[] apps, IRemoteAnimationFinishedCallback finishedCallback) throws RemoteException {
            RemoteAnimationTargetCompat[] appsCompat = RemoteAnimationTargetCompat.wrap(apps);
            Runnable animationFinishedCallback = new Runnable() { // from class: com.android.systemui.shared.system.RemoteAnimationAdapterCompat.1.1
                final /* synthetic */ IRemoteAnimationFinishedCallback val$finishedCallback;

                RunnableC00081(IRemoteAnimationFinishedCallback finishedCallback2) {
                    iRemoteAnimationFinishedCallback = finishedCallback2;
                }

                @Override // java.lang.Runnable
                public void run() {
                    try {
                        iRemoteAnimationFinishedCallback.onAnimationFinished();
                    } catch (RemoteException e) {
                        Log.e("ActivityOptionsCompat", "Failed to call app controlled animation finished callback", e);
                    }
                }
            };
            remoteAnimationRunnerCompat.onAnimationStart(appsCompat, animationFinishedCallback);
        }

        /* renamed from: com.android.systemui.shared.system.RemoteAnimationAdapterCompat$1$1 */
        class RunnableC00081 implements Runnable {
            final /* synthetic */ IRemoteAnimationFinishedCallback val$finishedCallback;

            RunnableC00081(IRemoteAnimationFinishedCallback finishedCallback2) {
                iRemoteAnimationFinishedCallback = finishedCallback2;
            }

            @Override // java.lang.Runnable
            public void run() {
                try {
                    iRemoteAnimationFinishedCallback.onAnimationFinished();
                } catch (RemoteException e) {
                    Log.e("ActivityOptionsCompat", "Failed to call app controlled animation finished callback", e);
                }
            }
        }

        public void onAnimationCancelled() throws RemoteException {
            remoteAnimationRunnerCompat.onAnimationCancelled();
        }
    }

    private static IRemoteAnimationRunner.Stub wrapRemoteAnimationRunner(RemoteAnimationRunnerCompat remoteAnimationAdapter) {
        return new IRemoteAnimationRunner.Stub() { // from class: com.android.systemui.shared.system.RemoteAnimationAdapterCompat.1
            AnonymousClass1() {
            }

            public void onAnimationStart(RemoteAnimationTarget[] apps, IRemoteAnimationFinishedCallback finishedCallback2) throws RemoteException {
                RemoteAnimationTargetCompat[] appsCompat = RemoteAnimationTargetCompat.wrap(apps);
                Runnable animationFinishedCallback = new Runnable() { // from class: com.android.systemui.shared.system.RemoteAnimationAdapterCompat.1.1
                    final /* synthetic */ IRemoteAnimationFinishedCallback val$finishedCallback;

                    RunnableC00081(IRemoteAnimationFinishedCallback finishedCallback22) {
                        iRemoteAnimationFinishedCallback = finishedCallback22;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            iRemoteAnimationFinishedCallback.onAnimationFinished();
                        } catch (RemoteException e) {
                            Log.e("ActivityOptionsCompat", "Failed to call app controlled animation finished callback", e);
                        }
                    }
                };
                remoteAnimationRunnerCompat.onAnimationStart(appsCompat, animationFinishedCallback);
            }

            /* renamed from: com.android.systemui.shared.system.RemoteAnimationAdapterCompat$1$1 */
            class RunnableC00081 implements Runnable {
                final /* synthetic */ IRemoteAnimationFinishedCallback val$finishedCallback;

                RunnableC00081(IRemoteAnimationFinishedCallback finishedCallback22) {
                    iRemoteAnimationFinishedCallback = finishedCallback22;
                }

                @Override // java.lang.Runnable
                public void run() {
                    try {
                        iRemoteAnimationFinishedCallback.onAnimationFinished();
                    } catch (RemoteException e) {
                        Log.e("ActivityOptionsCompat", "Failed to call app controlled animation finished callback", e);
                    }
                }
            }

            public void onAnimationCancelled() throws RemoteException {
                remoteAnimationRunnerCompat.onAnimationCancelled();
            }
        };
    }
}
