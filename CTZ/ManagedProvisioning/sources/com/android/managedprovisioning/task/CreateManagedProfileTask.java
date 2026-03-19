package com.android.managedprovisioning.task;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.nonrequiredapps.NonRequiredAppsLogic;
import java.util.Set;

public class CreateManagedProfileTask extends AbstractProvisioningTask {
    private final NonRequiredAppsLogic mNonRequiredAppsLogic;
    private int mProfileUserId;
    private final UserManager mUserManager;

    public CreateManagedProfileTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        this(context, provisioningParams, callback, (UserManager) context.getSystemService(UserManager.class), new NonRequiredAppsLogic(context, true, provisioningParams));
    }

    @VisibleForTesting
    CreateManagedProfileTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback, UserManager userManager, NonRequiredAppsLogic nonRequiredAppsLogic) {
        super(context, provisioningParams, callback);
        this.mNonRequiredAppsLogic = (NonRequiredAppsLogic) Preconditions.checkNotNull(nonRequiredAppsLogic);
        this.mUserManager = (UserManager) Preconditions.checkNotNull(userManager);
    }

    @Override
    public void run(int i) {
        startTaskTimer();
        Set<String> systemAppsToRemove = this.mNonRequiredAppsLogic.getSystemAppsToRemove(i);
        UserInfo userInfoCreateProfileForUserEvenWhenDisallowed = this.mUserManager.createProfileForUserEvenWhenDisallowed(this.mContext.getString(R.string.default_managed_profile_name), 96, i, (String[]) systemAppsToRemove.toArray(new String[systemAppsToRemove.size()]));
        if (userInfoCreateProfileForUserEvenWhenDisallowed == null) {
            error(0);
            return;
        }
        this.mProfileUserId = userInfoCreateProfileForUserEvenWhenDisallowed.id;
        this.mNonRequiredAppsLogic.maybeTakeSystemAppsSnapshot(userInfoCreateProfileForUserEvenWhenDisallowed.id);
        stopTaskTimer();
        success();
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_initialize;
    }

    @Override
    protected int getMetricsCategory() {
        return 620;
    }

    public int getProfileUserId() {
        return this.mProfileUserId;
    }
}
