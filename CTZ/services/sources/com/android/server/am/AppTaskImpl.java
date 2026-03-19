package com.android.server.am;

import android.app.ActivityManager;
import android.app.IAppTask;
import android.app.IApplicationThread;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import com.android.server.pm.DumpState;

class AppTaskImpl extends IAppTask.Stub {
    private int mCallingUid;
    private ActivityManagerService mService;
    private int mTaskId;

    public AppTaskImpl(ActivityManagerService activityManagerService, int i, int i2) {
        this.mService = activityManagerService;
        this.mTaskId = i;
        this.mCallingUid = i2;
    }

    private void checkCaller() {
        if (this.mCallingUid != Binder.getCallingUid()) {
            throw new SecurityException("Caller " + this.mCallingUid + " does not match caller of getAppTasks(): " + Binder.getCallingUid());
        }
    }

    public void finishAndRemoveTask() {
        checkCaller();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (!this.mService.mStackSupervisor.removeTaskByIdLocked(this.mTaskId, false, true, "finish-and-remove-task")) {
                        throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    public ActivityManager.RecentTaskInfo getTaskInfo() {
        ActivityManager.RecentTaskInfo recentTaskInfoCreateRecentTaskInfo;
        checkCaller();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    TaskRecord taskRecordAnyTaskForIdLocked = this.mService.mStackSupervisor.anyTaskForIdLocked(this.mTaskId, 1);
                    if (taskRecordAnyTaskForIdLocked == null) {
                        throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                    }
                    recentTaskInfoCreateRecentTaskInfo = this.mService.getRecentTasks().createRecentTaskInfo(taskRecordAnyTaskForIdLocked);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return recentTaskInfoCreateRecentTaskInfo;
    }

    public void moveToFront() {
        checkCaller();
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.mStackSupervisor.startActivityFromRecents(callingPid, callingUid, this.mTaskId, null);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int startActivity(IBinder iBinder, String str, Intent intent, String str2, Bundle bundle) {
        TaskRecord taskRecordAnyTaskForIdLocked;
        IApplicationThread iApplicationThreadAsInterface;
        checkCaller();
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                taskRecordAnyTaskForIdLocked = this.mService.mStackSupervisor.anyTaskForIdLocked(this.mTaskId, 1);
                if (taskRecordAnyTaskForIdLocked == null) {
                    throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                }
                iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(iBinder);
                if (iApplicationThreadAsInterface == null) {
                    throw new IllegalArgumentException("Bad app thread " + iApplicationThreadAsInterface);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return this.mService.getActivityStartController().obtainStarter(intent, "AppTaskImpl").setCaller(iApplicationThreadAsInterface).setCallingPackage(str).setResolvedType(str2).setActivityOptions(bundle).setMayWait(callingUserId).setInTask(taskRecordAnyTaskForIdLocked).execute();
    }

    public void setExcludeFromRecents(boolean z) {
        checkCaller();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    TaskRecord taskRecordAnyTaskForIdLocked = this.mService.mStackSupervisor.anyTaskForIdLocked(this.mTaskId, 1);
                    if (taskRecordAnyTaskForIdLocked == null) {
                        throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                    }
                    Intent baseIntent = taskRecordAnyTaskForIdLocked.getBaseIntent();
                    if (z) {
                        baseIntent.addFlags(DumpState.DUMP_VOLUMES);
                    } else {
                        baseIntent.setFlags(baseIntent.getFlags() & (-8388609));
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }
}
