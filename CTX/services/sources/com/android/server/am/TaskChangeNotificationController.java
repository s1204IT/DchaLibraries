package com.android.server.am;

import android.app.ActivityManager;
import android.app.ITaskStackListener;
import android.content.ComponentName;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import java.util.ArrayList;

class TaskChangeNotificationController {
    private static final int LOG_STACK_STATE_MSG = 1;
    private static final int NOTIFY_ACTIVITY_DISMISSING_DOCKED_STACK_MSG = 7;
    private static final int NOTIFY_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_FAILED_MSG = 18;
    private static final int NOTIFY_ACTIVITY_PINNED_LISTENERS_MSG = 3;
    private static final int NOTIFY_ACTIVITY_REQUESTED_ORIENTATION_CHANGED_LISTENERS = 12;
    private static final int NOTIFY_ACTIVITY_UNPINNED_LISTENERS_MSG = 17;
    private static final int NOTIFY_FORCED_RESIZABLE_MSG = 6;
    private static final int NOTIFY_PINNED_ACTIVITY_RESTART_ATTEMPT_LISTENERS_MSG = 4;
    private static final int NOTIFY_PINNED_STACK_ANIMATION_ENDED_LISTENERS_MSG = 5;
    private static final int NOTIFY_PINNED_STACK_ANIMATION_STARTED_LISTENERS_MSG = 16;
    private static final int NOTIFY_TASK_ADDED_LISTENERS_MSG = 8;
    private static final int NOTIFY_TASK_DESCRIPTION_CHANGED_LISTENERS_MSG = 11;
    private static final int NOTIFY_TASK_MOVED_TO_FRONT_LISTENERS_MSG = 10;
    private static final int NOTIFY_TASK_PROFILE_LOCKED_LISTENERS_MSG = 14;
    private static final int NOTIFY_TASK_REMOVAL_STARTED_LISTENERS = 13;
    private static final int NOTIFY_TASK_REMOVED_LISTENERS_MSG = 9;
    private static final int NOTIFY_TASK_SNAPSHOT_CHANGED_LISTENERS_MSG = 15;
    private static final int NOTIFY_TASK_STACK_CHANGE_LISTENERS_DELAY = 100;
    private static final int NOTIFY_TASK_STACK_CHANGE_LISTENERS_MSG = 2;
    private final Handler mHandler;
    private final ActivityManagerService mService;
    private final ActivityStackSupervisor mStackSupervisor;
    private final RemoteCallbackList<ITaskStackListener> mRemoteTaskStackListeners = new RemoteCallbackList<>();
    private final ArrayList<ITaskStackListener> mLocalTaskStackListeners = new ArrayList<>();
    private final TaskStackConsumer mNotifyTaskStackChanged = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskStackChanged();
        }
    };
    private final TaskStackConsumer mNotifyTaskCreated = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskCreated(message.arg1, (ComponentName) message.obj);
        }
    };
    private final TaskStackConsumer mNotifyTaskRemoved = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskRemoved(message.arg1);
        }
    };
    private final TaskStackConsumer mNotifyTaskMovedToFront = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskMovedToFront(message.arg1);
        }
    };
    private final TaskStackConsumer mNotifyTaskDescriptionChanged = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskDescriptionChanged(message.arg1, (ActivityManager.TaskDescription) message.obj);
        }
    };
    private final TaskStackConsumer mNotifyActivityRequestedOrientationChanged = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityRequestedOrientationChanged(message.arg1, message.arg2);
        }
    };
    private final TaskStackConsumer mNotifyTaskRemovalStarted = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskRemovalStarted(message.arg1);
        }
    };
    private final TaskStackConsumer mNotifyActivityPinned = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityPinned((String) message.obj, message.sendingUid, message.arg1, message.arg2);
        }
    };
    private final TaskStackConsumer mNotifyActivityUnpinned = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityUnpinned();
        }
    };
    private final TaskStackConsumer mNotifyPinnedActivityRestartAttempt = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onPinnedActivityRestartAttempt(message.arg1 != 0);
        }
    };
    private final TaskStackConsumer mNotifyPinnedStackAnimationStarted = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onPinnedStackAnimationStarted();
        }
    };
    private final TaskStackConsumer mNotifyPinnedStackAnimationEnded = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onPinnedStackAnimationEnded();
        }
    };
    private final TaskStackConsumer mNotifyActivityForcedResizable = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityForcedResizable((String) message.obj, message.arg1, message.arg2);
        }
    };
    private final TaskStackConsumer mNotifyActivityDismissingDockedStack = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityDismissingDockedStack();
        }
    };
    private final TaskStackConsumer mNotifyActivityLaunchOnSecondaryDisplayFailed = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityLaunchOnSecondaryDisplayFailed();
        }
    };
    private final TaskStackConsumer mNotifyTaskProfileLocked = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskProfileLocked(message.arg1, message.arg2);
        }
    };
    private final TaskStackConsumer mNotifyTaskSnapshotChanged = new TaskStackConsumer() {
        @Override
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskSnapshotChanged(message.arg1, (ActivityManager.TaskSnapshot) message.obj);
        }
    };

    @FunctionalInterface
    public interface TaskStackConsumer {
        void accept(ITaskStackListener iTaskStackListener, Message message) throws RemoteException;
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    synchronized (TaskChangeNotificationController.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            TaskChangeNotificationController.this.mStackSupervisor.logStackState();
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                        break;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                case 2:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskStackChanged, message);
                    return;
                case 3:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityPinned, message);
                    return;
                case 4:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyPinnedActivityRestartAttempt, message);
                    return;
                case 5:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyPinnedStackAnimationEnded, message);
                    return;
                case 6:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityForcedResizable, message);
                    return;
                case 7:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityDismissingDockedStack, message);
                    return;
                case 8:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskCreated, message);
                    return;
                case 9:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskRemoved, message);
                    return;
                case 10:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskMovedToFront, message);
                    return;
                case 11:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskDescriptionChanged, message);
                    return;
                case 12:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityRequestedOrientationChanged, message);
                    return;
                case 13:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskRemovalStarted, message);
                    return;
                case 14:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskProfileLocked, message);
                    return;
                case 15:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskSnapshotChanged, message);
                    return;
                case 16:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyPinnedStackAnimationStarted, message);
                    return;
                case 17:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityUnpinned, message);
                    return;
                case 18:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityLaunchOnSecondaryDisplayFailed, message);
                    return;
                default:
                    return;
            }
        }
    }

    public TaskChangeNotificationController(ActivityManagerService activityManagerService, ActivityStackSupervisor activityStackSupervisor, Handler handler) {
        this.mService = activityManagerService;
        this.mStackSupervisor = activityStackSupervisor;
        this.mHandler = new MainHandler(handler.getLooper());
    }

    public void registerTaskStackListener(ITaskStackListener iTaskStackListener) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (iTaskStackListener != null) {
                    if (Binder.getCallingPid() == Process.myPid()) {
                        if (!this.mLocalTaskStackListeners.contains(iTaskStackListener)) {
                            this.mLocalTaskStackListeners.add(iTaskStackListener);
                        }
                    } else {
                        this.mRemoteTaskStackListeners.register(iTaskStackListener);
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    public void unregisterTaskStackListener(ITaskStackListener iTaskStackListener) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (iTaskStackListener != null) {
                    if (Binder.getCallingPid() == Process.myPid()) {
                        this.mLocalTaskStackListeners.remove(iTaskStackListener);
                    } else {
                        this.mRemoteTaskStackListeners.unregister(iTaskStackListener);
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    private void forAllRemoteListeners(TaskStackConsumer taskStackConsumer, Message message) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                for (int iBeginBroadcast = this.mRemoteTaskStackListeners.beginBroadcast() - 1; iBeginBroadcast >= 0; iBeginBroadcast--) {
                    try {
                        taskStackConsumer.accept((ITaskStackListener) this.mRemoteTaskStackListeners.getBroadcastItem(iBeginBroadcast), message);
                    } catch (RemoteException e) {
                    }
                }
                this.mRemoteTaskStackListeners.finishBroadcast();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    private void forAllLocalListeners(TaskStackConsumer taskStackConsumer, Message message) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                for (int size = this.mLocalTaskStackListeners.size() - 1; size >= 0; size--) {
                    try {
                        taskStackConsumer.accept(this.mLocalTaskStackListeners.get(size), message);
                    } catch (RemoteException e) {
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    void notifyTaskStackChanged() {
        this.mHandler.sendEmptyMessage(1);
        this.mHandler.removeMessages(2);
        Message messageObtainMessage = this.mHandler.obtainMessage(2);
        forAllLocalListeners(this.mNotifyTaskStackChanged, messageObtainMessage);
        this.mHandler.sendMessageDelayed(messageObtainMessage, 100L);
    }

    void notifyActivityPinned(ActivityRecord activityRecord) {
        this.mHandler.removeMessages(3);
        Message messageObtainMessage = this.mHandler.obtainMessage(3, activityRecord.getTask().taskId, activityRecord.getStackId(), activityRecord.packageName);
        messageObtainMessage.sendingUid = activityRecord.userId;
        forAllLocalListeners(this.mNotifyActivityPinned, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyActivityUnpinned() {
        this.mHandler.removeMessages(17);
        Message messageObtainMessage = this.mHandler.obtainMessage(17);
        forAllLocalListeners(this.mNotifyActivityUnpinned, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyPinnedActivityRestartAttempt(boolean z) {
        this.mHandler.removeMessages(4);
        Message messageObtainMessage = this.mHandler.obtainMessage(4, z ? 1 : 0, 0);
        forAllLocalListeners(this.mNotifyPinnedActivityRestartAttempt, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyPinnedStackAnimationStarted() {
        this.mHandler.removeMessages(16);
        Message messageObtainMessage = this.mHandler.obtainMessage(16);
        forAllLocalListeners(this.mNotifyPinnedStackAnimationStarted, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyPinnedStackAnimationEnded() {
        this.mHandler.removeMessages(5);
        Message messageObtainMessage = this.mHandler.obtainMessage(5);
        forAllLocalListeners(this.mNotifyPinnedStackAnimationEnded, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyActivityDismissingDockedStack() {
        this.mHandler.removeMessages(7);
        Message messageObtainMessage = this.mHandler.obtainMessage(7);
        forAllLocalListeners(this.mNotifyActivityDismissingDockedStack, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyActivityForcedResizable(int i, int i2, String str) {
        this.mHandler.removeMessages(6);
        Message messageObtainMessage = this.mHandler.obtainMessage(6, i, i2, str);
        forAllLocalListeners(this.mNotifyActivityForcedResizable, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyActivityLaunchOnSecondaryDisplayFailed() {
        this.mHandler.removeMessages(18);
        Message messageObtainMessage = this.mHandler.obtainMessage(18);
        forAllLocalListeners(this.mNotifyActivityLaunchOnSecondaryDisplayFailed, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyTaskCreated(int i, ComponentName componentName) {
        Message messageObtainMessage = this.mHandler.obtainMessage(8, i, 0, componentName);
        forAllLocalListeners(this.mNotifyTaskCreated, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyTaskRemoved(int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(9, i, 0);
        forAllLocalListeners(this.mNotifyTaskRemoved, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyTaskMovedToFront(int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(10, i, 0);
        forAllLocalListeners(this.mNotifyTaskMovedToFront, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyTaskDescriptionChanged(int i, ActivityManager.TaskDescription taskDescription) {
        Message messageObtainMessage = this.mHandler.obtainMessage(11, i, 0, taskDescription);
        forAllLocalListeners(this.mNotifyTaskDescriptionChanged, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyActivityRequestedOrientationChanged(int i, int i2) {
        Message messageObtainMessage = this.mHandler.obtainMessage(12, i, i2);
        forAllLocalListeners(this.mNotifyActivityRequestedOrientationChanged, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyTaskRemovalStarted(int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(13, i, 0);
        forAllLocalListeners(this.mNotifyTaskRemovalStarted, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyTaskProfileLocked(int i, int i2) {
        Message messageObtainMessage = this.mHandler.obtainMessage(14, i, i2);
        forAllLocalListeners(this.mNotifyTaskProfileLocked, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    void notifyTaskSnapshotChanged(int i, ActivityManager.TaskSnapshot taskSnapshot) {
        Message messageObtainMessage = this.mHandler.obtainMessage(15, i, 0, taskSnapshot);
        forAllLocalListeners(this.mNotifyTaskSnapshotChanged, messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }
}
