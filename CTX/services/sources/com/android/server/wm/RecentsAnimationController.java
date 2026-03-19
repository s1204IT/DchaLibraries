package com.android.server.wm;

import android.app.ActivityManager;
import android.app.WindowConfiguration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import android.view.IRecentsAnimationController;
import android.view.IRecentsAnimationRunner;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.LocalServices;
import com.android.server.wm.SurfaceAnimator;
import com.android.server.wm.utils.InsetUtils;
import com.google.android.collect.Sets;
import java.io.PrintWriter;
import java.util.ArrayList;

public class RecentsAnimationController implements IBinder.DeathRecipient {
    private static final long FAILSAFE_DELAY = 1000;
    public static final int REORDER_KEEP_IN_PLACE = 0;
    public static final int REORDER_MOVE_TO_ORIGINAL_POSITION = 2;
    public static final int REORDER_MOVE_TO_TOP = 1;
    private static final String TAG = RecentsAnimationController.class.getSimpleName();
    private final RecentsAnimationCallbacks mCallbacks;
    private boolean mCanceled;
    private final int mDisplayId;
    private boolean mInputConsumerEnabled;
    private boolean mLinkedToDeathOfRunner;
    private IRecentsAnimationRunner mRunner;
    private final WindowManagerService mService;
    private boolean mSplitScreenMinimized;
    private AppWindowToken mTargetAppToken;
    private final ArrayList<TaskAnimationAdapter> mPendingAnimations = new ArrayList<>();
    private final Runnable mFailsafeRunnable = new Runnable() {
        @Override
        public final void run() {
            this.f$0.cancelAnimation(2, "failSafeRunnable");
        }
    };
    private Rect mMinimizedHomeBounds = new Rect();
    private boolean mPendingStart = true;
    private final Rect mTmpRect = new Rect();
    private final IRecentsAnimationController mController = new IRecentsAnimationController.Stub() {
        public ActivityManager.TaskSnapshot screenshotTask(int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    if (RecentsAnimationController.this.mCanceled) {
                        return null;
                    }
                    for (int size = RecentsAnimationController.this.mPendingAnimations.size() - 1; size >= 0; size--) {
                        Task task = ((TaskAnimationAdapter) RecentsAnimationController.this.mPendingAnimations.get(size)).mTask;
                        if (task.mTaskId == i) {
                            TaskSnapshotController taskSnapshotController = RecentsAnimationController.this.mService.mTaskSnapshotController;
                            ArraySet<Task> arraySetNewArraySet = Sets.newArraySet(new Task[]{task});
                            taskSnapshotController.snapshotTasks(arraySetNewArraySet);
                            taskSnapshotController.addSkipClosingAppSnapshotTasks(arraySetNewArraySet);
                            return taskSnapshotController.getSnapshot(i, 0, false, false);
                        }
                    }
                    return null;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void finish(boolean z) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    if (RecentsAnimationController.this.mCanceled) {
                        return;
                    }
                    RecentsAnimationController.this.mCallbacks.onAnimationFinished(z ? 1 : 2, true);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setAnimationTargetsBehindSystemBars(boolean z) throws RemoteException {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    for (int size = RecentsAnimationController.this.mPendingAnimations.size() - 1; size >= 0; size--) {
                        ((TaskAnimationAdapter) RecentsAnimationController.this.mPendingAnimations.get(size)).mTask.setCanAffectSystemUiFlags(z);
                    }
                    RecentsAnimationController.this.mService.mWindowPlacerLocked.requestTraversal();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setInputConsumerEnabled(boolean z) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    if (!RecentsAnimationController.this.mCanceled) {
                        RecentsAnimationController.this.mInputConsumerEnabled = z;
                        RecentsAnimationController.this.mService.mInputMonitor.updateInputWindowsLw(true);
                        RecentsAnimationController.this.mService.scheduleAnimationLocked();
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setSplitScreenMinimized(boolean z) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    if (!RecentsAnimationController.this.mCanceled) {
                        RecentsAnimationController.this.mSplitScreenMinimized = z;
                        RecentsAnimationController.this.mService.checkSplitScreenMinimizedChanged(true);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void hideCurrentInputMethod() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
                if (inputMethodManagerInternal != null) {
                    inputMethodManagerInternal.hideCurrentInputMethod();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    };

    public interface RecentsAnimationCallbacks {
        void onAnimationFinished(@ReorderMode int i, boolean z);
    }

    public @interface ReorderMode {
    }

    RecentsAnimationController(WindowManagerService windowManagerService, IRecentsAnimationRunner iRecentsAnimationRunner, RecentsAnimationCallbacks recentsAnimationCallbacks, int i) {
        this.mService = windowManagerService;
        this.mRunner = iRecentsAnimationRunner;
        this.mCallbacks = recentsAnimationCallbacks;
        this.mDisplayId = i;
    }

    public void initialize(int i, SparseBooleanArray sparseBooleanArray) {
        DisplayContent displayContent = this.mService.mRoot.getDisplayContent(this.mDisplayId);
        ArrayList<Task> visibleTasks = displayContent.getVisibleTasks();
        int size = visibleTasks.size();
        for (int i2 = 0; i2 < size; i2++) {
            Task task = visibleTasks.get(i2);
            WindowConfiguration windowConfiguration = task.getWindowConfiguration();
            if (!windowConfiguration.tasksAreFloating() && windowConfiguration.getWindowingMode() != 3 && windowConfiguration.getActivityType() != i) {
                addAnimation(task, !sparseBooleanArray.get(task.mTaskId));
            }
        }
        if (this.mPendingAnimations.isEmpty()) {
            cancelAnimation(2, "initialize-noVisibleTasks");
            return;
        }
        try {
            linkToDeathOfRunner();
            AppWindowToken topFullscreenAppToken = displayContent.getStack(0, i).getTopChild().getTopFullscreenAppToken();
            if (topFullscreenAppToken != null) {
                this.mTargetAppToken = topFullscreenAppToken;
                if (topFullscreenAppToken.windowsCanBeWallpaperTarget()) {
                    displayContent.pendingLayoutChanges |= 4;
                    displayContent.setLayoutNeeded();
                }
            }
            displayContent.getDockedDividerController().getHomeStackBoundsInDockedMode(this.mMinimizedHomeBounds);
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        } catch (RemoteException e) {
            cancelAnimation(2, "initialize-failedToLinkToDeath");
        }
    }

    @VisibleForTesting
    AnimationAdapter addAnimation(Task task, boolean z) {
        SurfaceAnimator surfaceAnimator = new SurfaceAnimator(task, null, this.mService);
        TaskAnimationAdapter taskAnimationAdapter = new TaskAnimationAdapter(task, z);
        surfaceAnimator.startAnimation(task.getPendingTransaction(), taskAnimationAdapter, false);
        task.commitPendingTransaction();
        this.mPendingAnimations.add(taskAnimationAdapter);
        return taskAnimationAdapter;
    }

    @VisibleForTesting
    void removeAnimation(TaskAnimationAdapter taskAnimationAdapter) {
        taskAnimationAdapter.mTask.setCanAffectSystemUiFlags(true);
        taskAnimationAdapter.mCapturedFinishCallback.onAnimationFinished(taskAnimationAdapter);
        this.mPendingAnimations.remove(taskAnimationAdapter);
    }

    void startAnimation() {
        ArrayList arrayList;
        Rect rect;
        if (!this.mPendingStart || this.mCanceled) {
            return;
        }
        try {
            arrayList = new ArrayList();
            for (int size = this.mPendingAnimations.size() - 1; size >= 0; size--) {
                TaskAnimationAdapter taskAnimationAdapter = this.mPendingAnimations.get(size);
                RemoteAnimationTarget remoteAnimationTargetCreateRemoteAnimationApp = taskAnimationAdapter.createRemoteAnimationApp();
                if (remoteAnimationTargetCreateRemoteAnimationApp != null) {
                    arrayList.add(remoteAnimationTargetCreateRemoteAnimationApp);
                } else {
                    removeAnimation(taskAnimationAdapter);
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to start recents animation", e);
        }
        if (arrayList.isEmpty()) {
            cancelAnimation(2, "startAnimation-noAppWindows");
            return;
        }
        RemoteAnimationTarget[] remoteAnimationTargetArr = (RemoteAnimationTarget[]) arrayList.toArray(new RemoteAnimationTarget[arrayList.size()]);
        this.mPendingStart = false;
        Rect rect2 = null;
        if (this.mTargetAppToken != null && this.mTargetAppToken.inSplitScreenSecondaryWindowingMode()) {
            rect = this.mMinimizedHomeBounds;
        } else {
            rect = null;
        }
        if (this.mTargetAppToken != null && this.mTargetAppToken.findMainWindow() != null) {
            rect2 = this.mTargetAppToken.findMainWindow().mContentInsets;
        }
        this.mRunner.onAnimationStart(this.mController, remoteAnimationTargetArr, rect2, rect);
        SparseIntArray sparseIntArray = new SparseIntArray();
        sparseIntArray.put(1, 5);
        this.mService.mH.obtainMessage(47, sparseIntArray).sendToTarget();
    }

    void cancelAnimation(@ReorderMode int i, String str) {
        cancelAnimation(i, false, str);
    }

    void cancelAnimationSynchronously(@ReorderMode int i, String str) {
        cancelAnimation(i, true, str);
    }

    private void cancelAnimation(@ReorderMode int i, boolean z, String str) {
        synchronized (this.mService.getWindowManagerLock()) {
            if (this.mCanceled) {
                return;
            }
            this.mService.mH.removeCallbacks(this.mFailsafeRunnable);
            this.mCanceled = true;
            try {
                this.mRunner.onAnimationCanceled();
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to cancel recents animation", e);
            }
            this.mCallbacks.onAnimationFinished(i, z);
        }
    }

    void cleanupAnimation(@ReorderMode int i) {
        for (int size = this.mPendingAnimations.size() - 1; size >= 0; size--) {
            TaskAnimationAdapter taskAnimationAdapter = this.mPendingAnimations.get(size);
            if (i == 1 || i == 0) {
                taskAnimationAdapter.mTask.dontAnimateDimExit();
            }
            removeAnimation(taskAnimationAdapter);
        }
        this.mService.mH.removeCallbacks(this.mFailsafeRunnable);
        unlinkToDeathOfRunner();
        this.mRunner = null;
        this.mCanceled = true;
        this.mService.mInputMonitor.updateInputWindowsLw(true);
        this.mService.destroyInputConsumer("recents_animation_input_consumer");
        if (this.mTargetAppToken != null) {
            if (i == 1 || i == 0) {
                this.mService.mAppTransition.notifyAppTransitionFinishedLocked(this.mTargetAppToken.token);
            }
        }
    }

    void scheduleFailsafe() {
        this.mService.mH.postDelayed(this.mFailsafeRunnable, 1000L);
    }

    private void linkToDeathOfRunner() throws RemoteException {
        if (!this.mLinkedToDeathOfRunner) {
            this.mRunner.asBinder().linkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = true;
        }
    }

    private void unlinkToDeathOfRunner() {
        if (this.mLinkedToDeathOfRunner) {
            this.mRunner.asBinder().unlinkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = false;
        }
    }

    @Override
    public void binderDied() {
        cancelAnimation(2, "binderDied");
    }

    void checkAnimationReady(WallpaperController wallpaperController) {
        if (this.mPendingStart) {
            if (!isTargetOverWallpaper() || (wallpaperController.getWallpaperTarget() != null && wallpaperController.wallpaperTransitionReady())) {
                this.mService.getRecentsAnimationController().startAnimation();
            }
        }
    }

    boolean isSplitScreenMinimized() {
        return this.mSplitScreenMinimized;
    }

    boolean isWallpaperVisible(WindowState windowState) {
        return windowState != null && windowState.mAppToken != null && this.mTargetAppToken == windowState.mAppToken && isTargetOverWallpaper();
    }

    boolean hasInputConsumerForApp(AppWindowToken appWindowToken) {
        return this.mInputConsumerEnabled && isAnimatingApp(appWindowToken);
    }

    boolean updateInputConsumerForApp(InputConsumerImpl inputConsumerImpl, boolean z) {
        WindowState windowStateFindMainWindow;
        if (this.mTargetAppToken != null) {
            windowStateFindMainWindow = this.mTargetAppToken.findMainWindow();
        } else {
            windowStateFindMainWindow = null;
        }
        if (windowStateFindMainWindow != null) {
            windowStateFindMainWindow.getBounds(this.mTmpRect);
            inputConsumerImpl.mWindowHandle.hasFocus = z;
            inputConsumerImpl.mWindowHandle.touchableRegion.set(this.mTmpRect);
            return true;
        }
        return false;
    }

    boolean isTargetApp(AppWindowToken appWindowToken) {
        return this.mTargetAppToken != null && appWindowToken == this.mTargetAppToken;
    }

    private boolean isTargetOverWallpaper() {
        if (this.mTargetAppToken == null) {
            return false;
        }
        return this.mTargetAppToken.windowsCanBeWallpaperTarget();
    }

    boolean isAnimatingTask(Task task) {
        for (int size = this.mPendingAnimations.size() - 1; size >= 0; size--) {
            if (task == this.mPendingAnimations.get(size).mTask) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnimatingApp(AppWindowToken appWindowToken) {
        for (int size = this.mPendingAnimations.size() - 1; size >= 0; size--) {
            Task task = this.mPendingAnimations.get(size).mTask;
            for (int childCount = task.getChildCount() - 1; childCount >= 0; childCount--) {
                if (((AppWindowToken) task.getChildAt(childCount)) == appWindowToken) {
                    return true;
                }
            }
        }
        return false;
    }

    @VisibleForTesting
    class TaskAnimationAdapter implements AnimationAdapter {
        private SurfaceAnimator.OnAnimationFinishedCallback mCapturedFinishCallback;
        private SurfaceControl mCapturedLeash;
        private final boolean mIsRecentTaskInvisible;
        private RemoteAnimationTarget mTarget;
        private final Task mTask;
        private final Point mPosition = new Point();
        private final Rect mBounds = new Rect();

        TaskAnimationAdapter(Task task, boolean z) {
            this.mTask = task;
            this.mIsRecentTaskInvisible = z;
            WindowContainer parent = this.mTask.getParent();
            parent.getRelativePosition(this.mPosition);
            parent.getBounds(this.mBounds);
        }

        RemoteAnimationTarget createRemoteAnimationApp() {
            WindowState windowStateFindMainWindow;
            AppWindowToken topVisibleAppToken = this.mTask.getTopVisibleAppToken();
            if (topVisibleAppToken != null) {
                windowStateFindMainWindow = topVisibleAppToken.findMainWindow();
            } else {
                windowStateFindMainWindow = null;
            }
            if (windowStateFindMainWindow == null) {
                return null;
            }
            Rect rect = new Rect(windowStateFindMainWindow.mContentInsets);
            InsetUtils.addInsets(rect, windowStateFindMainWindow.mAppToken.getLetterboxInsets());
            this.mTarget = new RemoteAnimationTarget(this.mTask.mTaskId, 1, this.mCapturedLeash, !topVisibleAppToken.fillsParent(), windowStateFindMainWindow.mWinAnimator.mLastClipRect, rect, this.mTask.getPrefixOrderIndex(), this.mPosition, this.mBounds, this.mTask.getWindowConfiguration(), this.mIsRecentTaskInvisible);
            return this.mTarget;
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
            transaction.setLayer(surfaceControl, this.mTask.getPrefixOrderIndex());
            transaction.setPosition(surfaceControl, this.mPosition.x, this.mPosition.y);
            RecentsAnimationController.this.mTmpRect.set(this.mBounds);
            RecentsAnimationController.this.mTmpRect.offsetTo(0, 0);
            transaction.setWindowCrop(surfaceControl, RecentsAnimationController.this.mTmpRect);
            this.mCapturedLeash = surfaceControl;
            this.mCapturedFinishCallback = onAnimationFinishedCallback;
        }

        @Override
        public void onAnimationCancelled(SurfaceControl surfaceControl) {
            RecentsAnimationController.this.cancelAnimation(2, "taskAnimationAdapterCanceled");
        }

        @Override
        public long getDurationHint() {
            return 0L;
        }

        @Override
        public long getStatusBarTransitionsStartTime() {
            return SystemClock.uptimeMillis();
        }

        @Override
        public void dump(PrintWriter printWriter, String str) {
            printWriter.print(str);
            printWriter.println("task=" + this.mTask);
            if (this.mTarget != null) {
                printWriter.print(str);
                printWriter.println("Target:");
                this.mTarget.dump(printWriter, str + "  ");
            } else {
                printWriter.print(str);
                printWriter.println("Target: null");
            }
            printWriter.println("mIsRecentTaskInvisible=" + this.mIsRecentTaskInvisible);
            printWriter.println("mPosition=" + this.mPosition);
            printWriter.println("mBounds=" + this.mBounds);
            printWriter.println("mIsRecentTaskInvisible=" + this.mIsRecentTaskInvisible);
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

    public void dump(PrintWriter printWriter, String str) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.println(RecentsAnimationController.class.getSimpleName() + ":");
        printWriter.print(str2);
        printWriter.println("mPendingStart=" + this.mPendingStart);
        printWriter.print(str2);
        printWriter.println("mCanceled=" + this.mCanceled);
        printWriter.print(str2);
        printWriter.println("mInputConsumerEnabled=" + this.mInputConsumerEnabled);
        printWriter.print(str2);
        printWriter.println("mSplitScreenMinimized=" + this.mSplitScreenMinimized);
        printWriter.print(str2);
        printWriter.println("mTargetAppToken=" + this.mTargetAppToken);
        printWriter.print(str2);
        printWriter.println("isTargetOverWallpaper=" + isTargetOverWallpaper());
    }
}
