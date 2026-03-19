package com.android.managedprovisioning.task;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;

public class SetDevicePolicyTask extends AbstractProvisioningTask {
    private final DevicePolicyManager mDevicePolicyManager;
    private final PackageManager mPackageManager;
    private final Utils mUtils;

    public SetDevicePolicyTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        this(new Utils(), context, provisioningParams, callback);
    }

    @VisibleForTesting
    SetDevicePolicyTask(Utils utils, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
        this.mPackageManager = this.mContext.getPackageManager();
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_set_owner;
    }

    @Override
    public void run(int i) {
        boolean deviceOwner;
        try {
            ComponentName componentNameInferDeviceAdminComponentName = this.mProvisioningParams.inferDeviceAdminComponentName(this.mUtils, this.mContext, i);
            enableDevicePolicyApp(componentNameInferDeviceAdminComponentName.getPackageName());
            setActiveAdmin(componentNameInferDeviceAdminComponentName, i);
            if (this.mUtils.isProfileOwnerAction(this.mProvisioningParams.provisioningAction)) {
                deviceOwner = setProfileOwner(componentNameInferDeviceAdminComponentName, i);
            } else {
                deviceOwner = setDeviceOwner(componentNameInferDeviceAdminComponentName, this.mContext.getResources().getString(R.string.default_owned_device_username), i);
            }
            if (deviceOwner) {
                success();
            } else {
                ProvisionLogger.loge("Error when setting device or profile owner.");
                error(0);
            }
        } catch (Exception e) {
            ProvisionLogger.loge("Failure setting device or profile owner", e);
            error(0);
        }
    }

    private void enableDevicePolicyApp(String str) {
        int applicationEnabledSetting = this.mPackageManager.getApplicationEnabledSetting(str);
        if (applicationEnabledSetting != 0 && applicationEnabledSetting != 1) {
            this.mPackageManager.setApplicationEnabledSetting(str, 0, 1);
        }
    }

    private void setActiveAdmin(ComponentName componentName, int i) {
        ProvisionLogger.logd("Setting " + componentName + " as active admin.");
        this.mDevicePolicyManager.setActiveAdmin(componentName, true, i);
    }

    private boolean setDeviceOwner(ComponentName componentName, String str, int i) {
        ProvisionLogger.logd("Setting " + componentName + " as device owner of user " + i);
        if (!componentName.equals(this.mDevicePolicyManager.getDeviceOwnerComponentOnCallingUser())) {
            return this.mDevicePolicyManager.setDeviceOwner(componentName, str, i);
        }
        return true;
    }

    private boolean setProfileOwner(ComponentName componentName, int i) {
        ProvisionLogger.logd("Setting " + componentName + " as profile owner of user " + i);
        if (!componentName.equals(this.mDevicePolicyManager.getProfileOwnerAsUser(i))) {
            return this.mDevicePolicyManager.setProfileOwner(componentName, componentName.getPackageName(), i);
        }
        return true;
    }
}
