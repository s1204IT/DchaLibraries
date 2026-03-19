package com.android.server.am;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.util.Slog;
import android.view.RemoteAnimationAdapter;
import com.android.internal.annotations.VisibleForTesting;

class SafeActivityOptions {
    private static final String TAG = "ActivityManager";
    private ActivityOptions mCallerOptions;
    private final int mOriginalCallingPid = Binder.getCallingPid();
    private final int mOriginalCallingUid = Binder.getCallingUid();
    private final ActivityOptions mOriginalOptions;
    private int mRealCallingPid;
    private int mRealCallingUid;

    static SafeActivityOptions fromBundle(Bundle bundle) {
        if (bundle != null) {
            return new SafeActivityOptions(ActivityOptions.fromBundle(bundle));
        }
        return null;
    }

    SafeActivityOptions(ActivityOptions activityOptions) {
        this.mOriginalOptions = activityOptions;
    }

    void setCallerOptions(ActivityOptions activityOptions) {
        this.mRealCallingPid = Binder.getCallingPid();
        this.mRealCallingUid = Binder.getCallingUid();
        this.mCallerOptions = activityOptions;
    }

    ActivityOptions getOptions(ActivityRecord activityRecord) throws SecurityException {
        return getOptions(activityRecord.intent, activityRecord.info, activityRecord.app, activityRecord.mStackSupervisor);
    }

    ActivityOptions getOptions(ActivityStackSupervisor activityStackSupervisor) throws SecurityException {
        return getOptions(null, null, null, activityStackSupervisor);
    }

    ActivityOptions getOptions(Intent intent, ActivityInfo activityInfo, ProcessRecord processRecord, ActivityStackSupervisor activityStackSupervisor) throws SecurityException {
        if (this.mOriginalOptions != null) {
            checkPermissions(intent, activityInfo, processRecord, activityStackSupervisor, this.mOriginalOptions, this.mOriginalCallingPid, this.mOriginalCallingUid);
            setCallingPidForRemoteAnimationAdapter(this.mOriginalOptions, this.mOriginalCallingPid);
        }
        if (this.mCallerOptions != null) {
            checkPermissions(intent, activityInfo, processRecord, activityStackSupervisor, this.mCallerOptions, this.mRealCallingPid, this.mRealCallingUid);
            setCallingPidForRemoteAnimationAdapter(this.mCallerOptions, this.mRealCallingPid);
        }
        return mergeActivityOptions(this.mOriginalOptions, this.mCallerOptions);
    }

    private void setCallingPidForRemoteAnimationAdapter(ActivityOptions activityOptions, int i) {
        RemoteAnimationAdapter remoteAnimationAdapter = activityOptions.getRemoteAnimationAdapter();
        if (remoteAnimationAdapter == null) {
            return;
        }
        if (i == Process.myPid()) {
            Slog.wtf(TAG, "Safe activity options constructed after clearing calling id");
        } else {
            remoteAnimationAdapter.setCallingPid(i);
        }
    }

    Bundle popAppVerificationBundle() {
        if (this.mOriginalOptions != null) {
            return this.mOriginalOptions.popAppVerificationBundle();
        }
        return null;
    }

    private void abort() {
        if (this.mOriginalOptions != null) {
            ActivityOptions.abort(this.mOriginalOptions);
        }
        if (this.mCallerOptions != null) {
            ActivityOptions.abort(this.mCallerOptions);
        }
    }

    static void abort(SafeActivityOptions safeActivityOptions) {
        if (safeActivityOptions != null) {
            safeActivityOptions.abort();
        }
    }

    @VisibleForTesting
    ActivityOptions mergeActivityOptions(ActivityOptions activityOptions, ActivityOptions activityOptions2) {
        if (activityOptions == null) {
            return activityOptions2;
        }
        if (activityOptions2 == null) {
            return activityOptions;
        }
        Bundle bundle = activityOptions.toBundle();
        bundle.putAll(activityOptions2.toBundle());
        return ActivityOptions.fromBundle(bundle);
    }

    private void checkPermissions(Intent intent, ActivityInfo activityInfo, ProcessRecord processRecord, ActivityStackSupervisor activityStackSupervisor, ActivityOptions activityOptions, int i, int i2) {
        if (activityOptions.getLaunchTaskId() != -1 && !activityStackSupervisor.mRecentTasks.isCallerRecents(i2) && activityStackSupervisor.mService.checkPermission("android.permission.START_TASKS_FROM_RECENTS", i, i2) == -1) {
            String str = "Permission Denial: starting " + getIntentString(intent) + " from " + processRecord + " (pid=" + i + ", uid=" + i2 + ") with launchTaskId=" + activityOptions.getLaunchTaskId();
            Slog.w(TAG, str);
            throw new SecurityException(str);
        }
        int launchDisplayId = activityOptions.getLaunchDisplayId();
        if (activityInfo != null && launchDisplayId != -1 && !activityStackSupervisor.isCallerAllowedToLaunchOnDisplay(i, i2, launchDisplayId, activityInfo)) {
            String str2 = "Permission Denial: starting " + getIntentString(intent) + " from " + processRecord + " (pid=" + i + ", uid=" + i2 + ") with launchDisplayId=" + launchDisplayId;
            Slog.w(TAG, str2);
            throw new SecurityException(str2);
        }
        boolean lockTaskMode = activityOptions.getLockTaskMode();
        if (activityInfo != null && lockTaskMode && !activityStackSupervisor.mService.getLockTaskController().isPackageWhitelisted(UserHandle.getUserId(i2), activityInfo.packageName)) {
            String str3 = "Permission Denial: starting " + getIntentString(intent) + " from " + processRecord + " (pid=" + i + ", uid=" + i2 + ") with lockTaskMode=true";
            Slog.w(TAG, str3);
            throw new SecurityException(str3);
        }
        if (activityOptions.getRemoteAnimationAdapter() != null && activityStackSupervisor.mService.checkPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", i, i2) != 0) {
            String str4 = "Permission Denial: starting " + getIntentString(intent) + " from " + processRecord + " (pid=" + i + ", uid=" + i2 + ") with remoteAnimationAdapter";
            Slog.w(TAG, str4);
            throw new SecurityException(str4);
        }
    }

    private String getIntentString(Intent intent) {
        return intent != null ? intent.toString() : "(no intent)";
    }
}
