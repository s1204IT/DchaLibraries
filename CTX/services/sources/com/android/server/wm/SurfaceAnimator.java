package com.android.server.wm;

import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.internal.annotations.VisibleForTesting;
import java.io.PrintWriter;

class SurfaceAnimator {
    private static final String TAG = "WindowManager";
    private final Animatable mAnimatable;
    private AnimationAdapter mAnimation;

    @VisibleForTesting
    final Runnable mAnimationFinishedCallback;
    private boolean mAnimationStartDelayed;
    private final OnAnimationFinishedCallback mInnerAnimationFinishedCallback;

    @VisibleForTesting
    SurfaceControl mLeash;
    private final WindowManagerService mService;

    interface OnAnimationFinishedCallback {
        void onAnimationFinished(AnimationAdapter animationAdapter);
    }

    SurfaceAnimator(Animatable animatable, Runnable runnable, WindowManagerService windowManagerService) {
        this.mAnimatable = animatable;
        this.mService = windowManagerService;
        this.mAnimationFinishedCallback = runnable;
        this.mInnerAnimationFinishedCallback = getFinishedCallback(runnable);
    }

    private OnAnimationFinishedCallback getFinishedCallback(final Runnable runnable) {
        return new OnAnimationFinishedCallback() {
            @Override
            public final void onAnimationFinished(AnimationAdapter animationAdapter) {
                SurfaceAnimator.lambda$getFinishedCallback$1(this.f$0, runnable, animationAdapter);
            }
        };
    }

    public static void lambda$getFinishedCallback$1(final SurfaceAnimator surfaceAnimator, final Runnable runnable, AnimationAdapter animationAdapter) {
        synchronized (surfaceAnimator.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                SurfaceAnimator surfaceAnimatorRemove = surfaceAnimator.mService.mAnimationTransferMap.remove(animationAdapter);
                if (surfaceAnimatorRemove != null) {
                    surfaceAnimatorRemove.mInnerAnimationFinishedCallback.onAnimationFinished(animationAdapter);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    if (animationAdapter != surfaceAnimator.mAnimation) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    Runnable runnable2 = new Runnable() {
                        @Override
                        public final void run() {
                            SurfaceAnimator.lambda$getFinishedCallback$0(this.f$0, runnable);
                        }
                    };
                    if (!surfaceAnimator.mAnimatable.shouldDeferAnimationFinish(runnable2)) {
                        runnable2.run();
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public static void lambda$getFinishedCallback$0(SurfaceAnimator surfaceAnimator, Runnable runnable) {
        surfaceAnimator.reset(surfaceAnimator.mAnimatable.getPendingTransaction(), true);
        if (runnable != null) {
            runnable.run();
        }
    }

    void startAnimation(SurfaceControl.Transaction transaction, AnimationAdapter animationAdapter, boolean z) {
        cancelAnimation(transaction, true, true);
        this.mAnimation = animationAdapter;
        SurfaceControl surfaceControl = this.mAnimatable.getSurfaceControl();
        if (surfaceControl == null) {
            Slog.w("WindowManager", "Unable to start animation, surface is null or no children.");
            cancelAnimation();
            return;
        }
        this.mLeash = createAnimationLeash(surfaceControl, transaction, this.mAnimatable.getSurfaceWidth(), this.mAnimatable.getSurfaceHeight(), z);
        this.mAnimatable.onAnimationLeashCreated(transaction, this.mLeash);
        if (this.mAnimationStartDelayed) {
            if (WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.i("WindowManager", "Animation start delayed");
                return;
            }
            return;
        }
        this.mAnimation.startAnimation(this.mLeash, transaction, this.mInnerAnimationFinishedCallback);
    }

    void startDelayingAnimationStart() {
        if (!isAnimating()) {
            this.mAnimationStartDelayed = true;
        }
    }

    void endDelayingAnimationStart() {
        boolean z = this.mAnimationStartDelayed;
        this.mAnimationStartDelayed = false;
        if (z && this.mAnimation != null) {
            this.mAnimation.startAnimation(this.mLeash, this.mAnimatable.getPendingTransaction(), this.mInnerAnimationFinishedCallback);
            this.mAnimatable.commitPendingTransaction();
        }
    }

    boolean isAnimating() {
        return this.mAnimation != null;
    }

    AnimationAdapter getAnimation() {
        return this.mAnimation;
    }

    void cancelAnimation() {
        cancelAnimation(this.mAnimatable.getPendingTransaction(), false, true);
        this.mAnimatable.commitPendingTransaction();
    }

    void setLayer(SurfaceControl.Transaction transaction, int i) {
        transaction.setLayer(this.mLeash != null ? this.mLeash : this.mAnimatable.getSurfaceControl(), i);
    }

    void setRelativeLayer(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl, int i) {
        transaction.setRelativeLayer(this.mLeash != null ? this.mLeash : this.mAnimatable.getSurfaceControl(), surfaceControl, i);
    }

    void reparent(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        transaction.reparent(this.mLeash != null ? this.mLeash : this.mAnimatable.getSurfaceControl(), surfaceControl.getHandle());
    }

    boolean hasLeash() {
        return this.mLeash != null;
    }

    void transferAnimation(SurfaceAnimator surfaceAnimator) {
        if (surfaceAnimator.mLeash == null) {
            return;
        }
        SurfaceControl surfaceControl = this.mAnimatable.getSurfaceControl();
        SurfaceControl animationLeashParent = this.mAnimatable.getAnimationLeashParent();
        if (surfaceControl == null || animationLeashParent == null) {
            Slog.w("WindowManager", "Unable to transfer animation, surface or parent is null");
            cancelAnimation();
            return;
        }
        endDelayingAnimationStart();
        SurfaceControl.Transaction pendingTransaction = this.mAnimatable.getPendingTransaction();
        cancelAnimation(pendingTransaction, true, true);
        this.mLeash = surfaceAnimator.mLeash;
        this.mAnimation = surfaceAnimator.mAnimation;
        surfaceAnimator.cancelAnimation(pendingTransaction, false, false);
        pendingTransaction.reparent(surfaceControl, this.mLeash.getHandle());
        pendingTransaction.reparent(this.mLeash, animationLeashParent.getHandle());
        this.mAnimatable.onAnimationLeashCreated(pendingTransaction, this.mLeash);
        this.mService.mAnimationTransferMap.put(this.mAnimation, this);
    }

    boolean isAnimationStartDelayed() {
        return this.mAnimationStartDelayed;
    }

    private void cancelAnimation(SurfaceControl.Transaction transaction, boolean z, boolean z2) {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.i("WindowManager", "Cancelling animation restarting=" + z);
        }
        SurfaceControl surfaceControl = this.mLeash;
        AnimationAdapter animationAdapter = this.mAnimation;
        reset(transaction, z2);
        if (animationAdapter != null) {
            if (!this.mAnimationStartDelayed && z2) {
                animationAdapter.onAnimationCancelled(surfaceControl);
            }
            if (!z) {
                this.mAnimationFinishedCallback.run();
            }
        }
        if (!z) {
            this.mAnimationStartDelayed = false;
        }
    }

    private void reset(SurfaceControl.Transaction transaction, boolean z) {
        SurfaceControl surfaceControl = this.mAnimatable.getSurfaceControl();
        SurfaceControl parentSurfaceControl = this.mAnimatable.getParentSurfaceControl();
        boolean z2 = false;
        boolean z3 = (this.mLeash == null || surfaceControl == null || parentSurfaceControl == null) ? false : true;
        if (z3) {
            if (WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.i("WindowManager", "Reparenting to original parent");
            }
            transaction.reparent(surfaceControl, parentSurfaceControl.getHandle());
            z2 = true;
        }
        this.mService.mAnimationTransferMap.remove(this.mAnimation);
        if (this.mLeash != null && z) {
            transaction.destroy(this.mLeash);
            z2 = true;
        }
        this.mLeash = null;
        this.mAnimation = null;
        if (z3) {
            this.mAnimatable.onAnimationLeashDestroyed(transaction);
            z2 = true;
        }
        if (z2) {
            this.mService.scheduleAnimationLocked();
        }
    }

    private SurfaceControl createAnimationLeash(SurfaceControl surfaceControl, SurfaceControl.Transaction transaction, int i, int i2, boolean z) {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.i("WindowManager", "Reparenting to leash");
        }
        SurfaceControl surfaceControlBuild = this.mAnimatable.makeAnimationLeash().setParent(this.mAnimatable.getAnimationLeashParent()).setName(surfaceControl + " - animation-leash").setSize(i, i2).build();
        if (!z) {
            transaction.show(surfaceControlBuild);
        }
        transaction.reparent(surfaceControl, surfaceControlBuild.getHandle());
        return surfaceControlBuild;
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        if (this.mAnimation != null) {
            this.mAnimation.writeToProto(protoOutputStream, 1146756268035L);
        }
        if (this.mLeash != null) {
            this.mLeash.writeToProto(protoOutputStream, 1146756268033L);
        }
        protoOutputStream.write(1133871366146L, this.mAnimationStartDelayed);
        protoOutputStream.end(jStart);
    }

    void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("mLeash=");
        printWriter.print(this.mLeash);
        if (this.mAnimationStartDelayed) {
            printWriter.print(" mAnimationStartDelayed=");
            printWriter.println(this.mAnimationStartDelayed);
        } else {
            printWriter.println();
        }
        printWriter.print(str);
        printWriter.println("Animation:");
        if (this.mAnimation != null) {
            this.mAnimation.dump(printWriter, str + "  ");
            return;
        }
        printWriter.print(str);
        printWriter.println("null");
    }

    interface Animatable {
        void commitPendingTransaction();

        SurfaceControl getAnimationLeashParent();

        SurfaceControl getParentSurfaceControl();

        SurfaceControl.Transaction getPendingTransaction();

        SurfaceControl getSurfaceControl();

        int getSurfaceHeight();

        int getSurfaceWidth();

        SurfaceControl.Builder makeAnimationLeash();

        void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl);

        void onAnimationLeashDestroyed(SurfaceControl.Transaction transaction);

        default boolean shouldDeferAnimationFinish(Runnable runnable) {
            return false;
        }
    }
}
