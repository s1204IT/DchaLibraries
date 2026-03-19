package com.android.server.wm;

import android.app.IActivityManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Slog;
import android.view.IWindow;
import com.android.internal.annotations.GuardedBy;
import com.android.server.input.InputManagerService;
import com.android.server.input.InputWindowHandle;
import com.mediatek.server.wm.WmsExt;

class TaskPositioningController {
    private final IActivityManager mActivityManager;
    private final Handler mHandler;
    private final InputManagerService mInputManager;
    private final InputMonitor mInputMonitor;
    private final WindowManagerService mService;

    @GuardedBy("WindowManagerSerivce.mWindowMap")
    private TaskPositioner mTaskPositioner;

    boolean isPositioningLocked() {
        return this.mTaskPositioner != null;
    }

    InputWindowHandle getDragWindowHandleLocked() {
        if (this.mTaskPositioner != null) {
            return this.mTaskPositioner.mDragWindowHandle;
        }
        return null;
    }

    TaskPositioningController(WindowManagerService windowManagerService, InputManagerService inputManagerService, InputMonitor inputMonitor, IActivityManager iActivityManager, Looper looper) {
        this.mService = windowManagerService;
        this.mInputMonitor = inputMonitor;
        this.mInputManager = inputManagerService;
        this.mActivityManager = iActivityManager;
        this.mHandler = new Handler(looper);
    }

    boolean startMovingTask(IWindow iWindow, float f, float f2) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowState windowStateWindowForClientLocked = this.mService.windowForClientLocked((Session) null, iWindow, false);
                if (!startPositioningLocked(windowStateWindowForClientLocked, false, false, f, f2)) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                try {
                    this.mActivityManager.setFocusedTask(windowStateWindowForClientLocked.getTask().mTaskId);
                    return true;
                } catch (RemoteException e) {
                    return true;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void handleTapOutsideTask(final DisplayContent displayContent, final int i, final int i2) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                TaskPositioningController.lambda$handleTapOutsideTask$0(this.f$0, displayContent, i, i2);
            }
        });
    }

    public static void lambda$handleTapOutsideTask$0(TaskPositioningController taskPositioningController, DisplayContent displayContent, int i, int i2) {
        int iTaskIdFromPoint;
        synchronized (taskPositioningController.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task taskFindTaskForResizePoint = displayContent.findTaskForResizePoint(i, i2);
                if (taskFindTaskForResizePoint != null) {
                    if (!taskPositioningController.startPositioningLocked(taskFindTaskForResizePoint.getTopVisibleAppMainWindow(), true, taskFindTaskForResizePoint.preserveOrientationOnResize(), i, i2)) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    iTaskIdFromPoint = taskFindTaskForResizePoint.mTaskId;
                } else {
                    iTaskIdFromPoint = displayContent.taskIdFromPoint(i, i2);
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                if (iTaskIdFromPoint >= 0) {
                    try {
                        taskPositioningController.mActivityManager.setFocusedTask(iTaskIdFromPoint);
                    } catch (RemoteException e) {
                    }
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private boolean startPositioningLocked(WindowState windowState, boolean z, boolean z2, float f, float f2) {
        WindowState windowState2;
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d(WmsExt.TAG, "startPositioningLocked: win=" + windowState + ", resize=" + z + ", preserveOrientation=" + z2 + ", {" + f + ", " + f2 + "}");
        }
        if (windowState == null || windowState.getAppToken() == null) {
            Slog.w(WmsExt.TAG, "startPositioningLocked: Bad window " + windowState);
            return false;
        }
        if (windowState.mInputChannel == null) {
            Slog.wtf(WmsExt.TAG, "startPositioningLocked: " + windowState + " has no input channel,  probably being removed");
            return false;
        }
        DisplayContent displayContent = windowState.getDisplayContent();
        if (displayContent == null) {
            Slog.w(WmsExt.TAG, "startPositioningLocked: Invalid display content " + windowState);
            return false;
        }
        displayContent.getDisplay();
        this.mTaskPositioner = TaskPositioner.create(this.mService);
        this.mTaskPositioner.register(displayContent);
        this.mInputMonitor.updateInputWindowsLw(true);
        if (this.mService.mCurrentFocus != null && this.mService.mCurrentFocus != windowState && this.mService.mCurrentFocus.mAppToken == windowState.mAppToken) {
            windowState2 = this.mService.mCurrentFocus;
        } else {
            windowState2 = windowState;
        }
        if (!this.mInputManager.transferTouchFocus(windowState2.mInputChannel, this.mTaskPositioner.mServerChannel)) {
            Slog.e(WmsExt.TAG, "startPositioningLocked: Unable to transfer touch focus");
            this.mTaskPositioner.unregister();
            this.mTaskPositioner = null;
            this.mInputMonitor.updateInputWindowsLw(true);
            return false;
        }
        this.mTaskPositioner.startDrag(windowState, z, z2, f, f2);
        return true;
    }

    void finishTaskPositioning() {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                TaskPositioningController.lambda$finishTaskPositioning$1(this.f$0);
            }
        });
    }

    public static void lambda$finishTaskPositioning$1(TaskPositioningController taskPositioningController) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d(WmsExt.TAG, "finishPositioning");
        }
        synchronized (taskPositioningController.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (taskPositioningController.mTaskPositioner != null) {
                    taskPositioningController.mTaskPositioner.unregister();
                    taskPositioningController.mTaskPositioner = null;
                    taskPositioningController.mInputMonitor.updateInputWindowsLw(true);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }
}
