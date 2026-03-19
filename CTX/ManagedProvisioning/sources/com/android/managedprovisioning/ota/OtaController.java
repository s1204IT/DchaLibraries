package com.android.managedprovisioning.ota;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.CrossProfileIntentFiltersSetter;
import com.android.managedprovisioning.task.DeleteNonRequiredAppsTask;
import com.android.managedprovisioning.task.DisableInstallShortcutListenersTask;
import com.android.managedprovisioning.task.DisallowAddUserTask;
import com.android.managedprovisioning.task.InstallExistingPackageTask;
import com.android.managedprovisioning.task.MigrateSystemAppsSnapshotTask;

public class OtaController {
    private final Context mContext;
    private final CrossProfileIntentFiltersSetter mCrossProfileIntentFiltersSetter;
    private final DevicePolicyManager mDevicePolicyManager;
    private final TaskExecutor mTaskExecutor;
    private final UserManager mUserManager;

    public OtaController(Context context) {
        this(context, new TaskExecutor(), new CrossProfileIntentFiltersSetter(context));
    }

    @VisibleForTesting
    OtaController(Context context, TaskExecutor taskExecutor, CrossProfileIntentFiltersSetter crossProfileIntentFiltersSetter) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mTaskExecutor = (TaskExecutor) Preconditions.checkNotNull(taskExecutor);
        this.mCrossProfileIntentFiltersSetter = (CrossProfileIntentFiltersSetter) Preconditions.checkNotNull(crossProfileIntentFiltersSetter);
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
    }

    public void run() {
        if (this.mContext.getUserId() != 0) {
            return;
        }
        this.mTaskExecutor.execute(0, new MigrateSystemAppsSnapshotTask(this.mContext, this.mTaskExecutor));
        int deviceOwnerUserId = this.mDevicePolicyManager.getDeviceOwnerUserId();
        if (deviceOwnerUserId != -10000) {
            addDeviceOwnerTasks(deviceOwnerUserId, this.mContext);
        }
        for (UserInfo userInfo : this.mUserManager.getUsers()) {
            if (userInfo.isManagedProfile()) {
                addManagedProfileTasks(userInfo.id, this.mContext);
            } else if (this.mDevicePolicyManager.getProfileOwnerAsUser(userInfo.id) != null) {
                addManagedUserTasks(userInfo.id, this.mContext);
            } else {
                this.mCrossProfileIntentFiltersSetter.resetFilters(userInfo.id);
            }
        }
    }

    void addDeviceOwnerTasks(int i, Context context) {
        ComponentName deviceOwnerComponentOnAnyUser = this.mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser();
        if (deviceOwnerComponentOnAnyUser == null) {
            ProvisionLogger.loge("No device owner found.");
            return;
        }
        ProvisioningParams provisioningParamsBuild = new ProvisioningParams.Builder().setDeviceAdminComponentName(deviceOwnerComponentOnAnyUser).setProvisioningAction("android.app.action.PROVISION_MANAGED_DEVICE").build();
        this.mTaskExecutor.execute(i, new DeleteNonRequiredAppsTask(false, context, provisioningParamsBuild, (AbstractProvisioningTask.Callback) this.mTaskExecutor));
        this.mTaskExecutor.execute(i, new DisallowAddUserTask(context, provisioningParamsBuild, this.mTaskExecutor));
    }

    void addManagedProfileTasks(int i, Context context) {
        this.mUserManager.setUserRestriction("no_wallpaper", true, UserHandle.of(i));
        this.mTaskExecutor.execute(i, new InstallExistingPackageTask("com.android.server.telecom", context, null, this.mTaskExecutor));
        ComponentName profileOwnerAsUser = this.mDevicePolicyManager.getProfileOwnerAsUser(i);
        if (profileOwnerAsUser == null) {
            ProvisionLogger.loge("No profile owner on managed profile " + i);
            return;
        }
        ProvisioningParams provisioningParamsBuild = new ProvisioningParams.Builder().setDeviceAdminComponentName(profileOwnerAsUser).setProvisioningAction("android.app.action.PROVISION_MANAGED_PROFILE").build();
        this.mTaskExecutor.execute(i, new DisableInstallShortcutListenersTask(context, provisioningParamsBuild, this.mTaskExecutor));
        this.mTaskExecutor.execute(i, new DeleteNonRequiredAppsTask(false, context, provisioningParamsBuild, (AbstractProvisioningTask.Callback) this.mTaskExecutor));
    }

    void addManagedUserTasks(int i, Context context) {
        ComponentName profileOwnerAsUser = this.mDevicePolicyManager.getProfileOwnerAsUser(i);
        if (profileOwnerAsUser == null) {
            ProvisionLogger.loge("No profile owner on managed user " + i);
            return;
        }
        this.mTaskExecutor.execute(i, new DeleteNonRequiredAppsTask(false, context, new ProvisioningParams.Builder().setDeviceAdminComponentName(profileOwnerAsUser).setProvisioningAction("android.app.action.PROVISION_MANAGED_USER").build(), (AbstractProvisioningTask.Callback) this.mTaskExecutor));
    }
}
