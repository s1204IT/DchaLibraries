package com.android.server.wm;

import android.app.ActivityManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.EventLogTags;
import com.mediatek.server.wm.WmsExt;
import java.lang.ref.WeakReference;

public class TaskWindowContainerController extends WindowContainerController<Task, TaskWindowContainerListener> {
    private final H mHandler;
    private final int mTaskId;

    @Override
    public void onOverrideConfigurationChanged(Configuration configuration) {
        super.onOverrideConfigurationChanged(configuration);
    }

    public TaskWindowContainerController(int i, TaskWindowContainerListener taskWindowContainerListener, StackWindowController stackWindowController, int i2, Rect rect, int i3, boolean z, boolean z2, boolean z3, ActivityManager.TaskDescription taskDescription) {
        this(i, taskWindowContainerListener, stackWindowController, i2, rect, i3, z, z2, z3, taskDescription, WindowManagerService.getInstance());
    }

    public TaskWindowContainerController(int i, TaskWindowContainerListener taskWindowContainerListener, StackWindowController stackWindowController, int i2, Rect rect, int i3, boolean z, boolean z2, boolean z3, ActivityManager.TaskDescription taskDescription, WindowManagerService windowManagerService) {
        super(taskWindowContainerListener, windowManagerService);
        this.mTaskId = i;
        this.mHandler = new H(new WeakReference(this), windowManagerService.mH.getLooper());
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_STACK) {
                    Slog.i(WmsExt.TAG, "TaskWindowContainerController: taskId=" + i + " stack=" + stackWindowController + " bounds=" + rect);
                }
                TaskStack taskStack = (TaskStack) stackWindowController.mContainer;
                if (taskStack == null) {
                    throw new IllegalArgumentException("TaskWindowContainerController: invalid stack=" + stackWindowController);
                }
                EventLog.writeEvent(EventLogTags.WM_TASK_CREATED, Integer.valueOf(i), Integer.valueOf(taskStack.mStackId));
                taskStack.addTask(createTask(i, taskStack, i2, i3, z, taskDescription), z2 ? Integer.MAX_VALUE : Integer.MIN_VALUE, z3, z2);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    @VisibleForTesting
    Task createTask(int i, TaskStack taskStack, int i2, int i3, boolean z, ActivityManager.TaskDescription taskDescription) {
        return new Task(i, taskStack, i2, this.mService, i3, z, taskDescription, this);
    }

    @Override
    public void removeContainer() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    if (WindowManagerDebugConfig.DEBUG_STACK) {
                        Slog.i(WmsExt.TAG, "removeTask: could not find taskId=" + this.mTaskId);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((Task) this.mContainer).removeIfPossible();
                super.removeContainer();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void positionChildAtTop(AppWindowContainerController appWindowContainerController) {
        positionChildAt(appWindowContainerController, Integer.MAX_VALUE);
    }

    public void positionChildAt(AppWindowContainerController appWindowContainerController, int i) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                AppWindowToken appWindowToken = (AppWindowToken) appWindowContainerController.mContainer;
                if (appWindowToken == null) {
                    Slog.w(WmsExt.TAG, "Attempted to position of non-existing app : " + appWindowContainerController);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Task task = (Task) this.mContainer;
                if (task == null) {
                    throw new IllegalArgumentException("positionChildAt: invalid task=" + this);
                }
                task.positionChildAt(i, appWindowToken, false);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void reparent(StackWindowController stackWindowController, int i, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_STACK) {
                    Slog.i(WmsExt.TAG, "reparent: moving taskId=" + this.mTaskId + " to stack=" + stackWindowController + " at " + i);
                }
                if (this.mContainer == 0) {
                    if (WindowManagerDebugConfig.DEBUG_STACK) {
                        Slog.i(WmsExt.TAG, "reparent: could not find taskId=" + this.mTaskId);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                TaskStack taskStack = (TaskStack) stackWindowController.mContainer;
                if (taskStack == null) {
                    throw new IllegalArgumentException("reparent: could not find stack=" + stackWindowController);
                }
                ((Task) this.mContainer).reparent(taskStack, i, z);
                ((Task) this.mContainer).getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setResizeable(int i) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != 0) {
                    ((Task) this.mContainer).setResizeable(i);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void resize(boolean z, boolean z2) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    throw new IllegalArgumentException("resizeTask: taskId " + this.mTaskId + " not found.");
                }
                if (((Task) this.mContainer).setBounds(((Task) this.mContainer).getOverrideBounds(), z2) != 0 && z) {
                    ((Task) this.mContainer).getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void getBounds(Rect rect) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != 0) {
                    ((Task) this.mContainer).getBounds(rect);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    rect.setEmpty();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setTaskDockedResizing(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "setTaskDockedResizing: taskId " + this.mTaskId + " not found.");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((Task) this.mContainer).setDragResizing(z, 1);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void cancelWindowTransition() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "cancelWindowTransition: taskId " + this.mTaskId + " not found.");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((Task) this.mContainer).cancelTaskWindowTransition();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "setTaskDescription: taskId " + this.mTaskId + " not found.");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((Task) this.mContainer).setTaskDescription(taskDescription);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void reportSnapshotChanged(ActivityManager.TaskSnapshot taskSnapshot) {
        this.mHandler.obtainMessage(0, taskSnapshot).sendToTarget();
    }

    void requestResize(Rect rect, int i) {
        this.mHandler.obtainMessage(1, i, 0, rect).sendToTarget();
    }

    public String toString() {
        return "{TaskWindowContainerController taskId=" + this.mTaskId + "}";
    }

    private static final class H extends Handler {
        static final int REPORT_SNAPSHOT_CHANGED = 0;
        static final int REQUEST_RESIZE = 1;
        private final WeakReference<TaskWindowContainerController> mController;

        H(WeakReference<TaskWindowContainerController> weakReference, Looper looper) {
            super(looper);
            this.mController = weakReference;
        }

        @Override
        public void handleMessage(Message message) {
            TaskWindowContainerController taskWindowContainerController = this.mController.get();
            TaskWindowContainerListener taskWindowContainerListener = taskWindowContainerController != null ? (TaskWindowContainerListener) taskWindowContainerController.mListener : null;
            if (taskWindowContainerListener == null) {
            }
            switch (message.what) {
                case 0:
                    taskWindowContainerListener.onSnapshotChanged((ActivityManager.TaskSnapshot) message.obj);
                    break;
                case 1:
                    taskWindowContainerListener.requestResize((Rect) message.obj, message.arg1);
                    break;
            }
        }
    }
}
