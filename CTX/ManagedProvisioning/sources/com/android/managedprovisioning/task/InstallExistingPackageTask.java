package com.android.managedprovisioning.task;

import android.content.Context;
import android.content.pm.PackageManager;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;

public class InstallExistingPackageTask extends AbstractProvisioningTask {
    private final String mPackageName;

    public InstallExistingPackageTask(String str, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mPackageName = (String) Preconditions.checkNotNull(str);
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_install;
    }

    @Override
    public void run(int i) {
        try {
            int iInstallExistingPackageAsUser = this.mContext.getPackageManager().installExistingPackageAsUser(this.mPackageName, i);
            if (iInstallExistingPackageAsUser == 1) {
                success();
            } else {
                ProvisionLogger.loge("Install failed, result code = " + iInstallExistingPackageAsUser);
                error(0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            error(0);
        }
    }
}
