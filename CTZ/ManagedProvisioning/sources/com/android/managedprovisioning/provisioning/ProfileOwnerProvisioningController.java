package com.android.managedprovisioning.provisioning;

import android.content.Context;
import android.os.UserManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.finalization.FinalizationController;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.CopyAccountToUserTask;
import com.android.managedprovisioning.task.CreateManagedProfileTask;
import com.android.managedprovisioning.task.DeleteNonRequiredAppsTask;
import com.android.managedprovisioning.task.DisableInstallShortcutListenersTask;
import com.android.managedprovisioning.task.InstallExistingPackageTask;
import com.android.managedprovisioning.task.ManagedProfileSettingsTask;
import com.android.managedprovisioning.task.SetDevicePolicyTask;
import com.android.managedprovisioning.task.StartManagedProfileTask;

public class ProfileOwnerProvisioningController extends AbstractProvisioningController {
    private final int mParentUserId;

    public ProfileOwnerProvisioningController(Context context, ProvisioningParams provisioningParams, int i, ProvisioningControllerCallback provisioningControllerCallback) {
        this(context, provisioningParams, i, provisioningControllerCallback, new FinalizationController(context));
    }

    @VisibleForTesting
    ProfileOwnerProvisioningController(Context context, ProvisioningParams provisioningParams, int i, ProvisioningControllerCallback provisioningControllerCallback, FinalizationController finalizationController) {
        super(context, provisioningParams, i, provisioningControllerCallback, finalizationController);
        this.mParentUserId = i;
    }

    @Override
    protected void setUpTasks() {
        if ("android.app.action.PROVISION_MANAGED_PROFILE".equals(this.mParams.provisioningAction)) {
            setUpTasksManagedProfile();
        } else {
            setUpTasksManagedUser();
        }
    }

    private void setUpTasksManagedProfile() {
        addTasks(new CreateManagedProfileTask(this.mContext, this.mParams, this), new InstallExistingPackageTask(this.mParams.inferDeviceAdminPackageName(), this.mContext, this.mParams, this), new SetDevicePolicyTask(this.mContext, this.mParams, this), new ManagedProfileSettingsTask(this.mContext, this.mParams, this), new DisableInstallShortcutListenersTask(this.mContext, this.mParams, this), new StartManagedProfileTask(this.mContext, this.mParams, this), new CopyAccountToUserTask(this.mParentUserId, this.mContext, this.mParams, this));
    }

    private void setUpTasksManagedUser() {
        addTasks(new DeleteNonRequiredAppsTask(true, this.mContext, this.mParams, (AbstractProvisioningTask.Callback) this), new InstallExistingPackageTask(this.mParams.inferDeviceAdminPackageName(), this.mContext, this.mParams, this), new SetDevicePolicyTask(this.mContext, this.mParams, this));
    }

    @Override
    public synchronized void onSuccess(AbstractProvisioningTask abstractProvisioningTask) {
        if (abstractProvisioningTask instanceof CreateManagedProfileTask) {
            this.mUserId = ((CreateManagedProfileTask) abstractProvisioningTask).getProfileUserId();
        }
        super.onSuccess(abstractProvisioningTask);
    }

    @Override
    protected void performCleanup() {
        if ("android.app.action.PROVISION_MANAGED_PROFILE".equals(this.mParams.provisioningAction) && this.mCurrentTaskIndex != 0) {
            ProvisionLogger.logd("Removing managed profile");
            ((UserManager) this.mContext.getSystemService(UserManager.class)).removeUserEvenWhenDisallowed(this.mUserId);
        }
    }

    @Override
    protected int getErrorTitle() {
        return R.string.cant_set_up_profile;
    }

    @Override
    protected int getErrorMsgId(AbstractProvisioningTask abstractProvisioningTask, int i) {
        return R.string.managed_provisioning_error_text;
    }

    @Override
    protected boolean getRequireFactoryReset(AbstractProvisioningTask abstractProvisioningTask, int i) {
        return false;
    }
}
