package com.android.systemui.shared.system;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Log;
import com.android.systemui.shared.recents.model.ThumbnailData;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class TaskStackChangeListeners extends TaskStackListener {
    private static final String TAG = TaskStackChangeListeners.class.getSimpleName();
    private final Handler mHandler;
    private boolean mRegistered;
    private final List<TaskStackChangeListener> mTaskStackListeners = new ArrayList();
    private final List<TaskStackChangeListener> mTmpListeners = new ArrayList();

    public TaskStackChangeListeners(Looper looper) {
        this.mHandler = new H(looper);
    }

    public void addListener(IActivityManager iActivityManager, TaskStackChangeListener taskStackChangeListener) {
        this.mTaskStackListeners.add(taskStackChangeListener);
        if (!this.mRegistered) {
            try {
                iActivityManager.registerTaskStackListener(this);
                this.mRegistered = true;
            } catch (Exception e) {
                Log.w(TAG, "Failed to call registerTaskStackListener", e);
            }
        }
    }

    public void removeListener(TaskStackChangeListener taskStackChangeListener) {
        this.mTaskStackListeners.remove(taskStackChangeListener);
    }

    public void onTaskStackChanged() throws RemoteException {
        synchronized (this.mTaskStackListeners) {
            this.mTmpListeners.clear();
            this.mTmpListeners.addAll(this.mTaskStackListeners);
        }
        for (int size = this.mTmpListeners.size() - 1; size >= 0; size--) {
            this.mTmpListeners.get(size).onTaskStackChangedBackground();
        }
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(1);
    }

    public void onActivityPinned(String str, int i, int i2, int i3) throws RemoteException {
        this.mHandler.removeMessages(3);
        this.mHandler.obtainMessage(3, new PinnedActivityInfo(str, i, i2, i3)).sendToTarget();
    }

    public void onActivityUnpinned() throws RemoteException {
        this.mHandler.removeMessages(10);
        this.mHandler.sendEmptyMessage(10);
    }

    public void onPinnedActivityRestartAttempt(boolean z) throws RemoteException {
        this.mHandler.removeMessages(4);
        this.mHandler.obtainMessage(4, z ? 1 : 0, 0).sendToTarget();
    }

    public void onPinnedStackAnimationStarted() throws RemoteException {
        this.mHandler.removeMessages(9);
        this.mHandler.sendEmptyMessage(9);
    }

    public void onPinnedStackAnimationEnded() throws RemoteException {
        this.mHandler.removeMessages(5);
        this.mHandler.sendEmptyMessage(5);
    }

    public void onActivityForcedResizable(String str, int i, int i2) throws RemoteException {
        this.mHandler.obtainMessage(6, i, i2, str).sendToTarget();
    }

    public void onActivityDismissingDockedStack() throws RemoteException {
        this.mHandler.sendEmptyMessage(7);
    }

    public void onActivityLaunchOnSecondaryDisplayFailed() throws RemoteException {
        this.mHandler.sendEmptyMessage(11);
    }

    public void onTaskProfileLocked(int i, int i2) throws RemoteException {
        this.mHandler.obtainMessage(8, i, i2).sendToTarget();
    }

    public void onTaskSnapshotChanged(int i, ActivityManager.TaskSnapshot taskSnapshot) throws RemoteException {
        this.mHandler.obtainMessage(2, i, 0, taskSnapshot).sendToTarget();
    }

    public void onTaskCreated(int i, ComponentName componentName) throws RemoteException {
        this.mHandler.obtainMessage(12, i, 0, componentName).sendToTarget();
    }

    public void onTaskRemoved(int i) throws RemoteException {
        this.mHandler.obtainMessage(13, i, 0).sendToTarget();
    }

    public void onTaskMovedToFront(int i) throws RemoteException {
        this.mHandler.obtainMessage(14, i, 0).sendToTarget();
    }

    public void onActivityRequestedOrientationChanged(int i, int i2) throws RemoteException {
        this.mHandler.obtainMessage(15, i, i2).sendToTarget();
    }

    private final class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            synchronized (TaskStackChangeListeners.this.mTaskStackListeners) {
                switch (message.what) {
                    case 1:
                        Trace.beginSection("onTaskStackChanged");
                        for (int size = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size >= 0; size--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size)).onTaskStackChanged();
                        }
                        Trace.endSection();
                        break;
                    case 2:
                        Trace.beginSection("onTaskSnapshotChanged");
                        for (int size2 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size2 >= 0; size2--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size2)).onTaskSnapshotChanged(message.arg1, new ThumbnailData((ActivityManager.TaskSnapshot) message.obj));
                        }
                        Trace.endSection();
                        break;
                    case 3:
                        PinnedActivityInfo pinnedActivityInfo = (PinnedActivityInfo) message.obj;
                        for (int size3 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size3 >= 0; size3--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size3)).onActivityPinned(pinnedActivityInfo.mPackageName, pinnedActivityInfo.mUserId, pinnedActivityInfo.mTaskId, pinnedActivityInfo.mStackId);
                        }
                        break;
                    case 4:
                        for (int size4 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size4 >= 0; size4--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size4)).onPinnedActivityRestartAttempt(message.arg1 != 0);
                        }
                        break;
                    case 5:
                        for (int size5 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size5 >= 0; size5--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size5)).onPinnedStackAnimationEnded();
                        }
                        break;
                    case 6:
                        for (int size6 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size6 >= 0; size6--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size6)).onActivityForcedResizable((String) message.obj, message.arg1, message.arg2);
                        }
                        break;
                    case 7:
                        for (int size7 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size7 >= 0; size7--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size7)).onActivityDismissingDockedStack();
                        }
                        break;
                    case 8:
                        for (int size8 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size8 >= 0; size8--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size8)).onTaskProfileLocked(message.arg1, message.arg2);
                        }
                        break;
                    case 9:
                        for (int size9 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size9 >= 0; size9--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size9)).onPinnedStackAnimationStarted();
                        }
                        break;
                    case 10:
                        for (int size10 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size10 >= 0; size10--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size10)).onActivityUnpinned();
                        }
                        break;
                    case 11:
                        for (int size11 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size11 >= 0; size11--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size11)).onActivityLaunchOnSecondaryDisplayFailed();
                        }
                        break;
                    case 12:
                        for (int size12 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size12 >= 0; size12--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size12)).onTaskCreated(message.arg1, (ComponentName) message.obj);
                        }
                        break;
                    case 13:
                        for (int size13 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size13 >= 0; size13--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size13)).onTaskRemoved(message.arg1);
                        }
                        break;
                    case 14:
                        for (int size14 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size14 >= 0; size14--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size14)).onTaskMovedToFront(message.arg1);
                        }
                        break;
                    case 15:
                        for (int size15 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; size15 >= 0; size15--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(size15)).onActivityRequestedOrientationChanged(message.arg1, message.arg2);
                        }
                        break;
                }
            }
        }
    }

    private static class PinnedActivityInfo {
        final String mPackageName;
        final int mStackId;
        final int mTaskId;
        final int mUserId;

        PinnedActivityInfo(String str, int i, int i2, int i3) {
            this.mPackageName = str;
            this.mUserId = i;
            this.mTaskId = i2;
            this.mStackId = i3;
        }
    }
}
