package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.util.ArraySet;
import android.util.Slog;
import android.view.DisplayListCanvas;
import android.view.RenderNode;
import android.view.SurfaceControl;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.WindowManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.TaskSnapshotPersister;
import com.android.server.wm.TaskSnapshotSurface;
import com.android.server.wm.utils.InsetUtils;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.PrintWriter;
import java.util.function.Consumer;

class TaskSnapshotController {

    @VisibleForTesting
    static final int SNAPSHOT_MODE_APP_THEME = 1;

    @VisibleForTesting
    static final int SNAPSHOT_MODE_NONE = 2;

    @VisibleForTesting
    static final int SNAPSHOT_MODE_REAL = 0;
    private static final String TAG = "WindowManager";
    private final TaskSnapshotCache mCache;
    private final boolean mIsRunningOnIoT;
    private final boolean mIsRunningOnTv;
    private final boolean mIsRunningOnWear;
    private final WindowManagerService mService;
    private final TaskSnapshotPersister mPersister = new TaskSnapshotPersister(new TaskSnapshotPersister.DirectoryResolver() {
        @Override
        public final File getSystemDirectoryForUser(int i) {
            return Environment.getDataSystemCeDirectory(i);
        }
    });
    private final TaskSnapshotLoader mLoader = new TaskSnapshotLoader(this.mPersister);
    private final ArraySet<Task> mSkipClosingAppSnapshotTasks = new ArraySet<>();
    private final ArraySet<Task> mTmpTasks = new ArraySet<>();
    private final Handler mHandler = new Handler();
    private final Rect mTmpRect = new Rect();

    TaskSnapshotController(WindowManagerService windowManagerService) {
        this.mService = windowManagerService;
        this.mCache = new TaskSnapshotCache(this.mService, this.mLoader);
        this.mIsRunningOnTv = this.mService.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        this.mIsRunningOnIoT = this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.embedded");
        this.mIsRunningOnWear = this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch");
    }

    void systemReady() {
        this.mPersister.start();
    }

    void onTransitionStarting() {
        handleClosingApps(this.mService.mClosingApps);
    }

    void notifyAppVisibilityChanged(AppWindowToken appWindowToken, boolean z) {
        if (!z) {
            handleClosingApps(Sets.newArraySet(new AppWindowToken[]{appWindowToken}));
        }
    }

    private void handleClosingApps(ArraySet<AppWindowToken> arraySet) {
        if (shouldDisableSnapshots()) {
            return;
        }
        getClosingTasks(arraySet, this.mTmpTasks);
        snapshotTasks(this.mTmpTasks);
        this.mSkipClosingAppSnapshotTasks.clear();
    }

    @VisibleForTesting
    void addSkipClosingAppSnapshotTasks(ArraySet<Task> arraySet) {
        this.mSkipClosingAppSnapshotTasks.addAll((ArraySet<? extends Task>) arraySet);
    }

    void snapshotTasks(ArraySet<Task> arraySet) {
        ActivityManager.TaskSnapshot taskSnapshotSnapshotTask;
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            Task taskValueAt = arraySet.valueAt(size);
            switch (getSnapshotMode(taskValueAt)) {
                case 0:
                    taskSnapshotSnapshotTask = snapshotTask(taskValueAt);
                    if (taskSnapshotSnapshotTask != null) {
                        GraphicBuffer snapshot = taskSnapshotSnapshotTask.getSnapshot();
                        if (snapshot.getWidth() == 0 || snapshot.getHeight() == 0) {
                            snapshot.destroy();
                            Slog.e("WindowManager", "Invalid task snapshot dimensions " + snapshot.getWidth() + "x" + snapshot.getHeight());
                        } else {
                            this.mCache.putSnapshot(taskValueAt, taskSnapshotSnapshotTask);
                            this.mPersister.persistSnapshot(taskValueAt.mTaskId, taskValueAt.mUserId, taskSnapshotSnapshotTask);
                            if (taskValueAt.getController() != null) {
                                taskValueAt.getController().reportSnapshotChanged(taskSnapshotSnapshotTask);
                            }
                        }
                    }
                    break;
                case 1:
                    taskSnapshotSnapshotTask = drawAppThemeSnapshot(taskValueAt);
                    if (taskSnapshotSnapshotTask != null) {
                    }
                    break;
                case 2:
                    break;
                default:
                    taskSnapshotSnapshotTask = null;
                    if (taskSnapshotSnapshotTask != null) {
                    }
                    break;
            }
        }
    }

    ActivityManager.TaskSnapshot getSnapshot(int i, int i2, boolean z, boolean z2) {
        return this.mCache.getSnapshot(i, i2, z, z2 || TaskSnapshotPersister.DISABLE_FULL_SIZED_BITMAPS);
    }

    WindowManagerPolicy.StartingSurface createStartingSurface(AppWindowToken appWindowToken, ActivityManager.TaskSnapshot taskSnapshot) {
        return TaskSnapshotSurface.create(this.mService, appWindowToken, taskSnapshot);
    }

    private ActivityManager.TaskSnapshot snapshotTask(Task task) {
        WindowState windowStateFindMainWindow;
        AppWindowToken topChild = task.getTopChild();
        if (topChild == null || (windowStateFindMainWindow = topChild.findMainWindow()) == null) {
            return null;
        }
        if (!this.mService.mPolicy.isScreenOn()) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.i("WindowManager", "Attempted to take screenshot while display was off.");
            }
            return null;
        }
        if (task.getSurfaceControl() == null) {
            return null;
        }
        if (topChild.hasCommittedReparentToAnimationLeash()) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.w("WindowManager", "Failed to take screenshot. App is animating " + topChild);
            }
            return null;
        }
        if (!topChild.forAllWindows((ToBooleanFunction<WindowState>) new ToBooleanFunction() {
            public final boolean apply(Object obj) {
                return TaskSnapshotController.lambda$snapshotTask$0((WindowState) obj);
            }
        }, true)) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.w("WindowManager", "Failed to take screenshot. No visible windows for " + task);
            }
            return null;
        }
        boolean zIsLowRamDeviceStatic = ActivityManager.isLowRamDeviceStatic();
        float f = zIsLowRamDeviceStatic ? TaskSnapshotPersister.REDUCED_SCALE : 1.0f;
        task.getBounds(this.mTmpRect);
        this.mTmpRect.offsetTo(0, 0);
        GraphicBuffer graphicBufferCaptureLayers = SurfaceControl.captureLayers(task.getSurfaceControl().getHandle(), this.mTmpRect, f);
        boolean z = windowStateFindMainWindow.getAttrs().format != -1;
        if (graphicBufferCaptureLayers == null || graphicBufferCaptureLayers.getWidth() <= 1 || graphicBufferCaptureLayers.getHeight() <= 1) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.w("WindowManager", "Failed to take screenshot for " + task);
            }
            return null;
        }
        return new ActivityManager.TaskSnapshot(graphicBufferCaptureLayers, topChild.getConfiguration().orientation, getInsets(windowStateFindMainWindow), zIsLowRamDeviceStatic, f, true, task.getWindowingMode(), getSystemUiVisibility(task), !topChild.fillsParent() || z);
    }

    static boolean lambda$snapshotTask$0(WindowState windowState) {
        return (windowState.mAppToken == null || windowState.mAppToken.isSurfaceShowing()) && windowState.mWinAnimator != null && windowState.mWinAnimator.getShown() && windowState.mWinAnimator.mLastAlpha > 0.0f;
    }

    private boolean shouldDisableSnapshots() {
        return this.mIsRunningOnWear || this.mIsRunningOnTv || this.mIsRunningOnIoT;
    }

    private Rect getInsets(WindowState windowState) {
        Rect rectMinRect = minRect(windowState.mContentInsets, windowState.mStableInsets);
        InsetUtils.addInsets(rectMinRect, windowState.mAppToken.getLetterboxInsets());
        return rectMinRect;
    }

    private Rect minRect(Rect rect, Rect rect2) {
        return new Rect(Math.min(rect.left, rect2.left), Math.min(rect.top, rect2.top), Math.min(rect.right, rect2.right), Math.min(rect.bottom, rect2.bottom));
    }

    @VisibleForTesting
    void getClosingTasks(ArraySet<AppWindowToken> arraySet, ArraySet<Task> arraySet2) {
        arraySet2.clear();
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            Task task = arraySet.valueAt(size).getTask();
            if (task != null && !task.isVisible() && !this.mSkipClosingAppSnapshotTasks.contains(task)) {
                arraySet2.add(task);
            }
        }
    }

    @VisibleForTesting
    int getSnapshotMode(Task task) {
        AppWindowToken topChild = task.getTopChild();
        if (!task.isActivityTypeStandardOrUndefined() && !task.isActivityTypeAssistant()) {
            return 2;
        }
        if (topChild != null && topChild.shouldUseAppThemeSnapshot()) {
            return 1;
        }
        return 0;
    }

    private ActivityManager.TaskSnapshot drawAppThemeSnapshot(Task task) {
        WindowState windowStateFindMainWindow;
        AppWindowToken topChild = task.getTopChild();
        if (topChild == null || (windowStateFindMainWindow = topChild.findMainWindow()) == null) {
            return null;
        }
        int alphaComponent = ColorUtils.setAlphaComponent(task.getTaskDescription().getBackgroundColor(), 255);
        int statusBarColor = task.getTaskDescription().getStatusBarColor();
        int navigationBarColor = task.getTaskDescription().getNavigationBarColor();
        WindowManager.LayoutParams attrs = windowStateFindMainWindow.getAttrs();
        TaskSnapshotSurface.SystemBarBackgroundPainter systemBarBackgroundPainter = new TaskSnapshotSurface.SystemBarBackgroundPainter(attrs.flags, attrs.privateFlags, attrs.systemUiVisibility, statusBarColor, navigationBarColor);
        int iWidth = windowStateFindMainWindow.getFrameLw().width();
        int iHeight = windowStateFindMainWindow.getFrameLw().height();
        RenderNode renderNodeCreate = RenderNode.create("TaskSnapshotController", (View) null);
        renderNodeCreate.setLeftTopRightBottom(0, 0, iWidth, iHeight);
        renderNodeCreate.setClipToBounds(false);
        DisplayListCanvas displayListCanvasStart = renderNodeCreate.start(iWidth, iHeight);
        displayListCanvasStart.drawColor(alphaComponent);
        systemBarBackgroundPainter.setInsets(windowStateFindMainWindow.mContentInsets, windowStateFindMainWindow.mStableInsets);
        systemBarBackgroundPainter.drawDecors(displayListCanvasStart, null);
        renderNodeCreate.end(displayListCanvasStart);
        Bitmap bitmapCreateHardwareBitmap = ThreadedRenderer.createHardwareBitmap(renderNodeCreate, iWidth, iHeight);
        if (bitmapCreateHardwareBitmap == null) {
            return null;
        }
        return new ActivityManager.TaskSnapshot(bitmapCreateHardwareBitmap.createGraphicBufferHandle(), topChild.getConfiguration().orientation, windowStateFindMainWindow.mStableInsets, ActivityManager.isLowRamDeviceStatic(), 1.0f, false, task.getWindowingMode(), getSystemUiVisibility(task), false);
    }

    void onAppRemoved(AppWindowToken appWindowToken) {
        this.mCache.onAppRemoved(appWindowToken);
    }

    void onAppDied(AppWindowToken appWindowToken) {
        this.mCache.onAppDied(appWindowToken);
    }

    void notifyTaskRemovedFromRecents(int i, int i2) {
        this.mCache.onTaskRemoved(i);
        this.mPersister.onTaskRemovedFromRecents(i, i2);
    }

    void removeObsoleteTaskFiles(ArraySet<Integer> arraySet, int[] iArr) {
        this.mPersister.removeObsoleteFiles(arraySet, iArr);
    }

    void setPersisterPaused(boolean z) {
        this.mPersister.setPaused(z);
    }

    void screenTurningOff(final WindowManagerPolicy.ScreenOffListener screenOffListener) {
        if (shouldDisableSnapshots()) {
            screenOffListener.onScreenOff();
        } else {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    TaskSnapshotController.lambda$screenTurningOff$2(this.f$0, screenOffListener);
                }
            });
        }
    }

    public static void lambda$screenTurningOff$2(final TaskSnapshotController taskSnapshotController, WindowManagerPolicy.ScreenOffListener screenOffListener) {
        try {
            synchronized (taskSnapshotController.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    taskSnapshotController.mTmpTasks.clear();
                    taskSnapshotController.mService.mRoot.forAllTasks(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            TaskSnapshotController.lambda$screenTurningOff$1(this.f$0, (Task) obj);
                        }
                    });
                    taskSnapshotController.snapshotTasks(taskSnapshotController.mTmpTasks);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            screenOffListener.onScreenOff();
        }
    }

    public static void lambda$screenTurningOff$1(TaskSnapshotController taskSnapshotController, Task task) {
        if (task.isVisible()) {
            taskSnapshotController.mTmpTasks.add(task);
        }
    }

    private int getSystemUiVisibility(Task task) {
        WindowState topFullscreenWindow;
        AppWindowToken topFullscreenAppToken = task.getTopFullscreenAppToken();
        if (topFullscreenAppToken != null) {
            topFullscreenWindow = topFullscreenAppToken.getTopFullscreenWindow();
        } else {
            topFullscreenWindow = null;
        }
        if (topFullscreenWindow != null) {
            return topFullscreenWindow.getSystemUiVisibility();
        }
        return 0;
    }

    void dump(PrintWriter printWriter, String str) {
        this.mCache.dump(printWriter, str);
    }
}
