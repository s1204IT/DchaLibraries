package com.android.server.am;

import android.app.ActivityOptions;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.HarmfulAppWarningActivity;
import com.android.internal.app.SuspendedAppActivity;
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.server.LocalServices;
import com.android.server.pm.PackageManagerService;

class ActivityStartInterceptor {
    ActivityInfo mAInfo;
    ActivityOptions mActivityOptions;
    private String mCallingPackage;
    int mCallingPid;
    int mCallingUid;
    TaskRecord mInTask;
    Intent mIntent;
    ResolveInfo mRInfo;
    private int mRealCallingPid;
    private int mRealCallingUid;
    String mResolvedType;
    private final ActivityManagerService mService;
    private final Context mServiceContext;
    private int mStartFlags;
    private final ActivityStackSupervisor mSupervisor;
    private final UserController mUserController;
    private int mUserId;
    private UserManager mUserManager;

    ActivityStartInterceptor(ActivityManagerService activityManagerService, ActivityStackSupervisor activityStackSupervisor) {
        this(activityManagerService, activityStackSupervisor, activityManagerService.mContext, activityManagerService.mUserController);
    }

    @VisibleForTesting
    ActivityStartInterceptor(ActivityManagerService activityManagerService, ActivityStackSupervisor activityStackSupervisor, Context context, UserController userController) {
        this.mService = activityManagerService;
        this.mSupervisor = activityStackSupervisor;
        this.mServiceContext = context;
        this.mUserController = userController;
    }

    void setStates(int i, int i2, int i3, int i4, String str) {
        this.mRealCallingPid = i2;
        this.mRealCallingUid = i3;
        this.mUserId = i;
        this.mStartFlags = i4;
        this.mCallingPackage = str;
    }

    private IntentSender createIntentSenderForOriginalIntent(int i, int i2) {
        return new IntentSender(this.mService.getIntentSenderLocked(2, this.mCallingPackage, i, this.mUserId, null, null, 0, new Intent[]{this.mIntent}, new String[]{this.mResolvedType}, i2, deferCrossProfileAppsAnimationIfNecessary()));
    }

    boolean intercept(Intent intent, ResolveInfo resolveInfo, ActivityInfo activityInfo, String str, TaskRecord taskRecord, int i, int i2, ActivityOptions activityOptions) {
        this.mUserManager = UserManager.get(this.mServiceContext);
        this.mIntent = intent;
        this.mCallingPid = i;
        this.mCallingUid = i2;
        this.mRInfo = resolveInfo;
        this.mAInfo = activityInfo;
        this.mResolvedType = str;
        this.mInTask = taskRecord;
        this.mActivityOptions = activityOptions;
        if (interceptSuspendedPackageIfNeeded() || interceptQuietProfileIfNeeded() || interceptHarmfulAppIfNeeded()) {
            return true;
        }
        return interceptWorkProfileChallengeIfNeeded();
    }

    private Bundle deferCrossProfileAppsAnimationIfNecessary() {
        if (this.mActivityOptions == null || this.mActivityOptions.getAnimationType() != 12) {
            return null;
        }
        this.mActivityOptions = null;
        return ActivityOptions.makeOpenCrossProfileAppsAnimation().toBundle();
    }

    private boolean interceptQuietProfileIfNeeded() {
        if (!this.mUserManager.isQuietModeEnabled(UserHandle.of(this.mUserId))) {
            return false;
        }
        this.mIntent = UnlaunchableAppActivity.createInQuietModeDialogIntent(this.mUserId, createIntentSenderForOriginalIntent(this.mCallingUid, 1342177280));
        this.mCallingPid = this.mRealCallingPid;
        this.mCallingUid = this.mRealCallingUid;
        this.mResolvedType = null;
        this.mRInfo = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, this.mUserManager.getProfileParent(this.mUserId).id, 0, this.mRealCallingUid);
        this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, this.mRInfo, this.mStartFlags, null);
        return true;
    }

    private boolean interceptSuspendedByAdminPackage() {
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (devicePolicyManagerInternal == null) {
            return false;
        }
        this.mIntent = devicePolicyManagerInternal.createShowAdminSupportIntent(this.mUserId, true);
        this.mIntent.putExtra("android.app.extra.RESTRICTION", "policy_suspend_packages");
        this.mCallingPid = this.mRealCallingPid;
        this.mCallingUid = this.mRealCallingUid;
        this.mResolvedType = null;
        UserInfo profileParent = this.mUserManager.getProfileParent(this.mUserId);
        if (profileParent != null) {
            this.mRInfo = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, profileParent.id, 0, this.mRealCallingUid);
        } else {
            this.mRInfo = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, this.mUserId, 0, this.mRealCallingUid);
        }
        this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, this.mRInfo, this.mStartFlags, null);
        return true;
    }

    private boolean interceptSuspendedPackageIfNeeded() {
        PackageManagerInternal packageManagerInternalLocked;
        if (this.mAInfo == null || this.mAInfo.applicationInfo == null || (this.mAInfo.applicationInfo.flags & 1073741824) == 0 || (packageManagerInternalLocked = this.mService.getPackageManagerInternalLocked()) == null) {
            return false;
        }
        String str = this.mAInfo.applicationInfo.packageName;
        String suspendingPackage = packageManagerInternalLocked.getSuspendingPackage(str, this.mUserId);
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(suspendingPackage)) {
            return interceptSuspendedByAdminPackage();
        }
        this.mIntent = SuspendedAppActivity.createSuspendedAppInterceptIntent(str, suspendingPackage, packageManagerInternalLocked.getSuspendedDialogMessage(str, this.mUserId), this.mUserId);
        this.mCallingPid = this.mRealCallingPid;
        this.mCallingUid = this.mRealCallingUid;
        this.mResolvedType = null;
        this.mRInfo = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, this.mUserId, 0, this.mRealCallingUid);
        this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, this.mRInfo, this.mStartFlags, null);
        return true;
    }

    private boolean interceptWorkProfileChallengeIfNeeded() {
        Intent intentInterceptWithConfirmCredentialsIfNeeded = interceptWithConfirmCredentialsIfNeeded(this.mAInfo, this.mUserId);
        if (intentInterceptWithConfirmCredentialsIfNeeded == null) {
            return false;
        }
        this.mIntent = intentInterceptWithConfirmCredentialsIfNeeded;
        this.mCallingPid = this.mRealCallingPid;
        this.mCallingUid = this.mRealCallingUid;
        this.mResolvedType = null;
        if (this.mInTask != null) {
            this.mIntent.putExtra("android.intent.extra.TASK_ID", this.mInTask.taskId);
            this.mInTask = null;
        }
        if (this.mActivityOptions == null) {
            this.mActivityOptions = ActivityOptions.makeBasic();
        }
        ActivityRecord homeActivity = this.mSupervisor.getHomeActivity();
        if (homeActivity != null && homeActivity.getTask() != null) {
            this.mActivityOptions.setLaunchTaskId(homeActivity.getTask().taskId);
        }
        this.mRInfo = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, this.mUserManager.getProfileParent(this.mUserId).id, 0, this.mRealCallingUid);
        this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, this.mRInfo, this.mStartFlags, null);
        return true;
    }

    private Intent interceptWithConfirmCredentialsIfNeeded(ActivityInfo activityInfo, int i) {
        if (!this.mUserController.shouldConfirmCredentials(i)) {
            return null;
        }
        IntentSender intentSenderCreateIntentSenderForOriginalIntent = createIntentSenderForOriginalIntent(Binder.getCallingUid(), 1409286144);
        Intent intentCreateConfirmDeviceCredentialIntent = ((KeyguardManager) this.mServiceContext.getSystemService("keyguard")).createConfirmDeviceCredentialIntent(null, null, i);
        if (intentCreateConfirmDeviceCredentialIntent == null) {
            return null;
        }
        intentCreateConfirmDeviceCredentialIntent.setFlags(276840448);
        intentCreateConfirmDeviceCredentialIntent.putExtra("android.intent.extra.PACKAGE_NAME", activityInfo.packageName);
        intentCreateConfirmDeviceCredentialIntent.putExtra("android.intent.extra.INTENT", intentSenderCreateIntentSenderForOriginalIntent);
        return intentCreateConfirmDeviceCredentialIntent;
    }

    private boolean interceptHarmfulAppIfNeeded() {
        try {
            CharSequence harmfulAppWarning = this.mService.getPackageManager().getHarmfulAppWarning(this.mAInfo.packageName, this.mUserId);
            if (harmfulAppWarning == null) {
                return false;
            }
            this.mIntent = HarmfulAppWarningActivity.createHarmfulAppWarningIntent(this.mServiceContext, this.mAInfo.packageName, createIntentSenderForOriginalIntent(this.mCallingUid, 1409286144), harmfulAppWarning);
            this.mCallingPid = this.mRealCallingPid;
            this.mCallingUid = this.mRealCallingUid;
            this.mResolvedType = null;
            this.mRInfo = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, this.mUserId, 0, this.mRealCallingUid);
            this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, this.mRInfo, this.mStartFlags, null);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }
}
