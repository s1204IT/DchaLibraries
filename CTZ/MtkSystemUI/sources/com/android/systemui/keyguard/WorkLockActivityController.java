package com.android.systemui.keyguard;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.ProfilerInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.shared.system.ActivityManagerWrapper;

public class WorkLockActivityController {
    private static final String TAG = WorkLockActivityController.class.getSimpleName();
    private final Context mContext;
    private final IActivityManager mIam;
    private final SysUiTaskStackChangeListener mLockListener;

    public WorkLockActivityController(Context context) {
        this(context, ActivityManagerWrapper.getInstance(), ActivityManager.getService());
    }

    @VisibleForTesting
    WorkLockActivityController(Context context, ActivityManagerWrapper activityManagerWrapper, IActivityManager iActivityManager) {
        this.mLockListener = new SysUiTaskStackChangeListener() {
            @Override
            public void onTaskProfileLocked(int i, int i2) {
                WorkLockActivityController.this.startWorkChallengeInTask(i, i2);
            }
        };
        this.mContext = context;
        this.mIam = iActivityManager;
        activityManagerWrapper.registerTaskStackListener(this.mLockListener);
    }

    private void startWorkChallengeInTask(int i, int i2) {
        ActivityManager.TaskDescription taskDescription;
        try {
            taskDescription = this.mIam.getTaskDescription(i);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to get description for task=" + i);
            taskDescription = null;
        }
        Intent intentAddFlags = new Intent("android.app.action.CONFIRM_DEVICE_CREDENTIAL_WITH_USER").setComponent(new ComponentName(this.mContext, (Class<?>) WorkLockActivity.class)).putExtra("android.intent.extra.USER_ID", i2).putExtra("com.android.systemui.keyguard.extra.TASK_DESCRIPTION", taskDescription).addFlags(67239936);
        ActivityOptions activityOptionsMakeBasic = ActivityOptions.makeBasic();
        activityOptionsMakeBasic.setLaunchTaskId(i);
        activityOptionsMakeBasic.setTaskOverlay(true, false);
        if (!ActivityManager.isStartResultSuccessful(startActivityAsUser(intentAddFlags, activityOptionsMakeBasic.toBundle(), -2))) {
            try {
                this.mIam.removeTask(i);
            } catch (RemoteException e2) {
                Log.w(TAG, "Failed to get description for task=" + i);
            }
        }
    }

    private int startActivityAsUser(Intent intent, Bundle bundle, int i) {
        try {
            return this.mIam.startActivityAsUser(this.mContext.getIApplicationThread(), this.mContext.getBasePackageName(), intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 268435456, (ProfilerInfo) null, bundle, i);
        } catch (RemoteException e) {
            return -96;
        } catch (Exception e2) {
            return -96;
        }
    }
}
