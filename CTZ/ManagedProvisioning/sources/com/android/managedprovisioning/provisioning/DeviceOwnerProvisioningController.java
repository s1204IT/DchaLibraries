package com.android.managedprovisioning.provisioning;

import android.content.Context;
import com.android.internal.annotations.VisibleForTesting;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.finalization.FinalizationController;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.preprovisioning.EncryptionController;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.AddWifiNetworkTask;
import com.android.managedprovisioning.task.ConnectMobileNetworkTask;
import com.android.managedprovisioning.task.CopyAccountToUserTask;
import com.android.managedprovisioning.task.DeleteNonRequiredAppsTask;
import com.android.managedprovisioning.task.DeviceOwnerInitializeProvisioningTask;
import com.android.managedprovisioning.task.DisallowAddUserTask;
import com.android.managedprovisioning.task.DownloadPackageTask;
import com.android.managedprovisioning.task.InstallPackageTask;
import com.android.managedprovisioning.task.SetDevicePolicyTask;
import com.android.managedprovisioning.task.VerifyPackageTask;

public class DeviceOwnerProvisioningController extends AbstractProvisioningController {
    public DeviceOwnerProvisioningController(Context context, ProvisioningParams provisioningParams, int i, ProvisioningControllerCallback provisioningControllerCallback) {
        this(context, provisioningParams, i, provisioningControllerCallback, new FinalizationController(context));
    }

    @VisibleForTesting
    DeviceOwnerProvisioningController(Context context, ProvisioningParams provisioningParams, int i, ProvisioningControllerCallback provisioningControllerCallback, FinalizationController finalizationController) {
        super(context, provisioningParams, i, provisioningControllerCallback, finalizationController);
    }

    @Override
    protected void setUpTasks() {
        addTasks(new DeviceOwnerInitializeProvisioningTask(this.mContext, this.mParams, this));
        if (this.mParams.wifiInfo != null) {
            addTasks(new AddWifiNetworkTask(this.mContext, this.mParams, this));
        } else if (this.mParams.useMobileData) {
            addTasks(new ConnectMobileNetworkTask(this.mContext, this.mParams, this));
        }
        if (this.mParams.deviceAdminDownloadInfo != null) {
            DownloadPackageTask downloadPackageTask = new DownloadPackageTask(this.mContext, this.mParams, this);
            addTasks(downloadPackageTask, new VerifyPackageTask(downloadPackageTask, this.mContext, this.mParams, this), new InstallPackageTask(downloadPackageTask, this.mContext, this.mParams, this));
        }
        addTasks(new DeleteNonRequiredAppsTask(true, this.mContext, this.mParams, (AbstractProvisioningTask.Callback) this), new SetDevicePolicyTask(this.mContext, this.mParams, this), new DisallowAddUserTask(this.mContext, this.mParams, this));
        if (this.mParams.accountToMigrate != null) {
            addTasks(new CopyAccountToUserTask(0, this.mContext, this.mParams, this));
        }
    }

    @Override
    protected int getErrorTitle() {
        return R.string.cant_set_up_device;
    }

    @Override
    protected int getErrorMsgId(AbstractProvisioningTask abstractProvisioningTask, int i) {
        if (abstractProvisioningTask instanceof AddWifiNetworkTask) {
            return R.string.device_owner_error_wifi;
        }
        if (abstractProvisioningTask instanceof DownloadPackageTask) {
            switch (i) {
                case 0:
                    return R.string.device_owner_error_download_failed;
                case EncryptionController.NOTIFICATION_ID:
                    return R.string.cant_set_up_device;
            }
        }
        if (abstractProvisioningTask instanceof VerifyPackageTask) {
            switch (i) {
                case 0:
                    return R.string.device_owner_error_hash_mismatch;
                case EncryptionController.NOTIFICATION_ID:
                    return R.string.device_owner_error_package_invalid;
            }
        }
        if (abstractProvisioningTask instanceof InstallPackageTask) {
            switch (i) {
                case 0:
                    return R.string.device_owner_error_package_invalid;
                case EncryptionController.NOTIFICATION_ID:
                    return R.string.device_owner_error_installation_failed;
            }
        }
        return R.string.cant_set_up_device;
    }

    @Override
    protected boolean getRequireFactoryReset(AbstractProvisioningTask abstractProvisioningTask, int i) {
        return ((abstractProvisioningTask instanceof AddWifiNetworkTask) || (abstractProvisioningTask instanceof DeviceOwnerInitializeProvisioningTask)) ? false : true;
    }

    @Override
    protected void performCleanup() {
    }
}
