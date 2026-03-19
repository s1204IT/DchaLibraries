package android.app;

import android.app.ActivityManager;
import android.app.ITaskStackListener;
import android.content.ComponentName;
import android.os.RemoteException;

public abstract class TaskStackListener extends ITaskStackListener.Stub {
    @Override
    public void onTaskStackChanged() throws RemoteException {
    }

    @Override
    public void onActivityPinned(String str, int i, int i2, int i3) throws RemoteException {
    }

    @Override
    public void onActivityUnpinned() throws RemoteException {
    }

    @Override
    public void onPinnedActivityRestartAttempt(boolean z) throws RemoteException {
    }

    @Override
    public void onPinnedStackAnimationStarted() throws RemoteException {
    }

    @Override
    public void onPinnedStackAnimationEnded() throws RemoteException {
    }

    @Override
    public void onActivityForcedResizable(String str, int i, int i2) throws RemoteException {
    }

    @Override
    public void onActivityDismissingDockedStack() throws RemoteException {
    }

    @Override
    public void onActivityLaunchOnSecondaryDisplayFailed() throws RemoteException {
    }

    @Override
    public void onTaskCreated(int i, ComponentName componentName) throws RemoteException {
    }

    @Override
    public void onTaskRemoved(int i) throws RemoteException {
    }

    public void onTaskMovedToFront(int i) throws RemoteException {
    }

    @Override
    public void onTaskRemovalStarted(int i) throws RemoteException {
    }

    public void onTaskDescriptionChanged(int i, ActivityManager.TaskDescription taskDescription) throws RemoteException {
    }

    @Override
    public void onActivityRequestedOrientationChanged(int i, int i2) throws RemoteException {
    }

    @Override
    public void onTaskProfileLocked(int i, int i2) throws RemoteException {
    }

    @Override
    public void onTaskSnapshotChanged(int i, ActivityManager.TaskSnapshot taskSnapshot) throws RemoteException {
    }
}
