package com.android.managedprovisioning.task;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;

public class DisallowAddUserTask extends AbstractProvisioningTask {
    private final boolean mIsSplitSystemUser;
    private final UserManager mUserManager;

    public DisallowAddUserTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        this(UserManager.isSplitSystemUser(), context, provisioningParams, callback);
    }

    @VisibleForTesting
    DisallowAddUserTask(boolean z, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mIsSplitSystemUser = z;
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    @Override
    public void run(int i) {
        if (this.mIsSplitSystemUser && i == 0) {
            ProvisionLogger.logi("Not setting DISALLOW_ADD_USER as system device-owner detected.");
            success();
            return;
        }
        for (UserInfo userInfo : this.mUserManager.getUsers()) {
            UserHandle userHandle = userInfo.getUserHandle();
            if (!this.mUserManager.hasUserRestriction("no_add_user", userHandle)) {
                this.mUserManager.setUserRestriction("no_add_user", true, userHandle);
                ProvisionLogger.logi("DISALLOW_ADD_USER restriction set on user: " + userInfo.id);
            }
        }
        success();
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_finishing_touches;
    }
}
