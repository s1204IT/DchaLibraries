package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.IRemoteAnimationFinishedCallback;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import com.android.internal.util.FastPrintWriter;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.wm.SurfaceAnimator;
import com.android.server.wm.utils.InsetUtils;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

class RemoteAnimationController implements IBinder.DeathRecipient {
    private static final String TAG;
    private static final long TIMEOUT_MS = 2000;
    private boolean mCanceled;
    private FinishedCallback mFinishedCallback;
    private final Handler mHandler;
    private boolean mLinkedToDeathOfRunner;
    private final RemoteAnimationAdapter mRemoteAnimationAdapter;
    private final WindowManagerService mService;
    private final ArrayList<RemoteAnimationAdapterWrapper> mPendingAnimations = new ArrayList<>();
    private final Rect mTmpRect = new Rect();
    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public final void run() {
            this.f$0.cancelAnimation("timeoutRunnable");
        }
    };

    static {
        TAG = (!WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS || WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) ? WmsExt.TAG : "RemoteAnimationController";
    }

    RemoteAnimationController(WindowManagerService windowManagerService, RemoteAnimationAdapter remoteAnimationAdapter, Handler handler) {
        this.mService = windowManagerService;
        this.mRemoteAnimationAdapter = remoteAnimationAdapter;
        this.mHandler = handler;
    }

    AnimationAdapter createAnimationAdapter(AppWindowToken appWindowToken, Point point, Rect rect) {
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.d(TAG, "createAnimationAdapter(): token=" + appWindowToken);
        }
        RemoteAnimationAdapterWrapper remoteAnimationAdapterWrapper = new RemoteAnimationAdapterWrapper(appWindowToken, point, rect);
        this.mPendingAnimations.add(remoteAnimationAdapterWrapper);
        return remoteAnimationAdapterWrapper;
    }

    void goodToGo() {
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.d(TAG, "goodToGo()");
        }
        if (this.mPendingAnimations.isEmpty() || this.mCanceled) {
            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                Slog.d(TAG, "goodToGo(): Animation finished already, canceled=" + this.mCanceled + " mPendingAnimations=" + this.mPendingAnimations.size());
            }
            onAnimationFinished();
            return;
        }
        this.mHandler.postDelayed(this.mTimeoutRunnable, (long) (2000.0f * this.mService.getCurrentAnimatorScale()));
        this.mFinishedCallback = new FinishedCallback(this);
        final RemoteAnimationTarget[] remoteAnimationTargetArrCreateAnimations = createAnimations();
        if (remoteAnimationTargetArrCreateAnimations.length == 0) {
            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                Slog.d(TAG, "goodToGo(): No apps to animate");
            }
            onAnimationFinished();
        } else {
            this.mService.mAnimator.addAfterPrepareSurfacesRunnable(new Runnable() {
                @Override
                public final void run() {
                    RemoteAnimationController.lambda$goodToGo$1(this.f$0, remoteAnimationTargetArrCreateAnimations);
                }
            });
            sendRunningRemoteAnimation(true);
        }
    }

    public static void lambda$goodToGo$1(RemoteAnimationController remoteAnimationController, RemoteAnimationTarget[] remoteAnimationTargetArr) {
        try {
            remoteAnimationController.linkToDeathOfRunner();
            remoteAnimationController.mRemoteAnimationAdapter.getRunner().onAnimationStart(remoteAnimationTargetArr, remoteAnimationController.mFinishedCallback);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to start remote animation", e);
            remoteAnimationController.onAnimationFinished();
        }
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.d(TAG, "startAnimation(): Notify animation start:");
            remoteAnimationController.writeStartDebugStatement();
        }
    }

    private void cancelAnimation(String str) {
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.d(TAG, "cancelAnimation(): reason=" + str);
        }
        synchronized (this.mService.getWindowManagerLock()) {
            if (this.mCanceled) {
                return;
            }
            this.mCanceled = true;
            onAnimationFinished();
            invokeAnimationCancelled();
        }
    }

    private void writeStartDebugStatement() {
        Slog.i(TAG, "Starting remote animation");
        StringWriter stringWriter = new StringWriter();
        PrintWriter fastPrintWriter = new FastPrintWriter(stringWriter);
        for (int size = this.mPendingAnimations.size() - 1; size >= 0; size--) {
            this.mPendingAnimations.get(size).dump(fastPrintWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        fastPrintWriter.close();
        Slog.i(TAG, stringWriter.toString());
    }

    private RemoteAnimationTarget[] createAnimations() {
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.d(TAG, "createAnimations()");
        }
        ArrayList arrayList = new ArrayList();
        for (int size = this.mPendingAnimations.size() - 1; size >= 0; size--) {
            RemoteAnimationAdapterWrapper remoteAnimationAdapterWrapper = this.mPendingAnimations.get(size);
            RemoteAnimationTarget remoteAnimationTargetCreateRemoteAppAnimation = remoteAnimationAdapterWrapper.createRemoteAppAnimation();
            if (remoteAnimationTargetCreateRemoteAppAnimation != null) {
                if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                    Slog.d(TAG, "\tAdd token=" + remoteAnimationAdapterWrapper.mAppWindowToken);
                }
                arrayList.add(remoteAnimationTargetCreateRemoteAppAnimation);
            } else {
                if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                    Slog.d(TAG, "\tRemove token=" + remoteAnimationAdapterWrapper.mAppWindowToken);
                }
                if (remoteAnimationAdapterWrapper.mCapturedFinishCallback != null) {
                    remoteAnimationAdapterWrapper.mCapturedFinishCallback.onAnimationFinished(remoteAnimationAdapterWrapper);
                }
                this.mPendingAnimations.remove(size);
            }
        }
        return (RemoteAnimationTarget[]) arrayList.toArray(new RemoteAnimationTarget[arrayList.size()]);
    }

    private void onAnimationFinished() {
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.d(TAG, "onAnimationFinished(): mPendingAnimations=" + this.mPendingAnimations.size());
        }
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                unlinkToDeathOfRunner();
                releaseFinishedCallback();
                this.mService.openSurfaceTransaction();
                try {
                    try {
                        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                            Slog.d(TAG, "onAnimationFinished(): Notify animation finished:");
                        }
                        for (int size = this.mPendingAnimations.size() - 1; size >= 0; size--) {
                            RemoteAnimationAdapterWrapper remoteAnimationAdapterWrapper = this.mPendingAnimations.get(size);
                            remoteAnimationAdapterWrapper.mCapturedFinishCallback.onAnimationFinished(remoteAnimationAdapterWrapper);
                            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                                Slog.d(TAG, "\t" + remoteAnimationAdapterWrapper.mAppWindowToken);
                            }
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "Failed to finish remote animation", e);
                        throw e;
                    }
                } finally {
                    this.mService.closeSurfaceTransaction("RemoteAnimationController#finished");
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        sendRunningRemoteAnimation(false);
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.i(TAG, "Finishing remote animation");
        }
    }

    private void invokeAnimationCancelled() {
        try {
            this.mRemoteAnimationAdapter.getRunner().onAnimationCancelled();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to notify cancel", e);
        }
    }

    private void releaseFinishedCallback() {
        if (this.mFinishedCallback != null) {
            this.mFinishedCallback.release();
            this.mFinishedCallback = null;
        }
    }

    private void sendRunningRemoteAnimation(boolean z) {
        int callingPid = this.mRemoteAnimationAdapter.getCallingPid();
        if (callingPid == 0) {
            throw new RuntimeException("Calling pid of remote animation was null");
        }
        this.mService.sendSetRunningRemoteAnimation(callingPid, z);
    }

    private void linkToDeathOfRunner() throws RemoteException {
        if (!this.mLinkedToDeathOfRunner) {
            this.mRemoteAnimationAdapter.getRunner().asBinder().linkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = true;
        }
    }

    private void unlinkToDeathOfRunner() {
        if (this.mLinkedToDeathOfRunner) {
            this.mRemoteAnimationAdapter.getRunner().asBinder().unlinkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = false;
        }
    }

    @Override
    public void binderDied() {
        cancelAnimation("binderDied");
    }

    private static final class FinishedCallback extends IRemoteAnimationFinishedCallback.Stub {
        RemoteAnimationController mOuter;

        FinishedCallback(RemoteAnimationController remoteAnimationController) {
            this.mOuter = remoteAnimationController;
        }

        public void onAnimationFinished() throws RemoteException {
            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                Slog.d(RemoteAnimationController.TAG, "app-onAnimationFinished(): mOuter=" + this.mOuter);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (this.mOuter != null) {
                    this.mOuter.onAnimationFinished();
                    this.mOuter = null;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        void release() {
            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                Slog.d(RemoteAnimationController.TAG, "app-release(): mOuter=" + this.mOuter);
            }
            this.mOuter = null;
        }
    }

    private class RemoteAnimationAdapterWrapper implements AnimationAdapter {
        private final AppWindowToken mAppWindowToken;
        private SurfaceAnimator.OnAnimationFinishedCallback mCapturedFinishCallback;
        private SurfaceControl mCapturedLeash;
        private final Point mPosition = new Point();
        private final Rect mStackBounds = new Rect();
        private RemoteAnimationTarget mTarget;

        RemoteAnimationAdapterWrapper(AppWindowToken appWindowToken, Point point, Rect rect) {
            this.mAppWindowToken = appWindowToken;
            this.mPosition.set(point.x, point.y);
            this.mStackBounds.set(rect);
        }

        RemoteAnimationTarget createRemoteAppAnimation() {
            Task task = this.mAppWindowToken.getTask();
            WindowState windowStateFindMainWindow = this.mAppWindowToken.findMainWindow();
            if (task == null || windowStateFindMainWindow == null || this.mCapturedFinishCallback == null || this.mCapturedLeash == null) {
                return null;
            }
            Rect rect = new Rect(windowStateFindMainWindow.mContentInsets);
            InsetUtils.addInsets(rect, this.mAppWindowToken.getLetterboxInsets());
            this.mTarget = new RemoteAnimationTarget(task.mTaskId, getMode(), this.mCapturedLeash, !this.mAppWindowToken.fillsParent(), windowStateFindMainWindow.mWinAnimator.mLastClipRect, rect, this.mAppWindowToken.getPrefixOrderIndex(), this.mPosition, this.mStackBounds, task.getWindowConfiguration(), false);
            return this.mTarget;
        }

        private int getMode() {
            if (RemoteAnimationController.this.mService.mOpeningApps.contains(this.mAppWindowToken)) {
                return 0;
            }
            return 1;
        }

        @Override
        public boolean getDetachWallpaper() {
            return false;
        }

        @Override
        public boolean getShowWallpaper() {
            return false;
        }

        @Override
        public int getBackgroundColor() {
            return 0;
        }

        @Override
        public void startAnimation(SurfaceControl surfaceControl, SurfaceControl.Transaction transaction, SurfaceAnimator.OnAnimationFinishedCallback onAnimationFinishedCallback) {
            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                Slog.d(RemoteAnimationController.TAG, "startAnimation");
            }
            transaction.setLayer(surfaceControl, this.mAppWindowToken.getPrefixOrderIndex());
            transaction.setPosition(surfaceControl, this.mPosition.x, this.mPosition.y);
            RemoteAnimationController.this.mTmpRect.set(this.mStackBounds);
            RemoteAnimationController.this.mTmpRect.offsetTo(0, 0);
            transaction.setWindowCrop(surfaceControl, RemoteAnimationController.this.mTmpRect);
            this.mCapturedLeash = surfaceControl;
            this.mCapturedFinishCallback = onAnimationFinishedCallback;
        }

        @Override
        public void onAnimationCancelled(SurfaceControl surfaceControl) {
            RemoteAnimationController.this.mPendingAnimations.remove(this);
            if (RemoteAnimationController.this.mPendingAnimations.isEmpty()) {
                RemoteAnimationController.this.mHandler.removeCallbacks(RemoteAnimationController.this.mTimeoutRunnable);
                RemoteAnimationController.this.releaseFinishedCallback();
                RemoteAnimationController.this.invokeAnimationCancelled();
                RemoteAnimationController.this.sendRunningRemoteAnimation(false);
            }
        }

        @Override
        public long getDurationHint() {
            return RemoteAnimationController.this.mRemoteAnimationAdapter.getDuration();
        }

        @Override
        public long getStatusBarTransitionsStartTime() {
            return SystemClock.uptimeMillis() + RemoteAnimationController.this.mRemoteAnimationAdapter.getStatusBarTransitionDelay();
        }

        @Override
        public void dump(PrintWriter printWriter, String str) {
            printWriter.print(str);
            printWriter.print("token=");
            printWriter.println(this.mAppWindowToken);
            if (this.mTarget != null) {
                printWriter.print(str);
                printWriter.println("Target:");
                this.mTarget.dump(printWriter, str + "  ");
                return;
            }
            printWriter.print(str);
            printWriter.println("Target: null");
        }

        @Override
        public void writeToProto(ProtoOutputStream protoOutputStream) {
            long jStart = protoOutputStream.start(1146756268034L);
            if (this.mTarget != null) {
                this.mTarget.writeToProto(protoOutputStream, 1146756268033L);
            }
            protoOutputStream.end(jStart);
        }
    }
}
