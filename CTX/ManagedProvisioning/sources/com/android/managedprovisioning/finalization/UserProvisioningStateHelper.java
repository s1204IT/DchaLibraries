package com.android.managedprovisioning.finalization;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.SettingsFacade;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;

@VisibleForTesting
public class UserProvisioningStateHelper {
    private final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    private final int mMyUserId;
    private final SettingsFacade mSettingsFacade;
    private final Utils mUtils;

    UserProvisioningStateHelper(Context context) {
        this(context, new Utils(), new SettingsFacade(), UserHandle.myUserId());
    }

    @VisibleForTesting
    UserProvisioningStateHelper(Context context, Utils utils, SettingsFacade settingsFacade, int i) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
        this.mSettingsFacade = (SettingsFacade) Preconditions.checkNotNull(settingsFacade);
        this.mMyUserId = i;
    }

    @VisibleForTesting
    public void markUserProvisioningStateInitiallyDone(ProvisioningParams provisioningParams) {
        int i;
        Integer num;
        boolean zIsUserSetupCompleted = this.mSettingsFacade.isUserSetupCompleted(this.mContext);
        Integer num2 = null;
        int identifier = -10000;
        if (provisioningParams.provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
            identifier = this.mUtils.getManagedProfile(this.mContext).getIdentifier();
            if (zIsUserSetupCompleted) {
                num = 3;
            } else {
                num2 = 4;
                num = 2;
            }
        } else if (zIsUserSetupCompleted) {
            ProvisionLogger.logw("user_setup_complete set, but provisioning was started");
            num = null;
        } else {
            if (provisioningParams.skipUserSetup) {
                i = 2;
            } else {
                i = 1;
            }
            num2 = i;
            num = null;
        }
        if (num2 != null) {
            setUserProvisioningState(num2.intValue(), this.mMyUserId);
        }
        if (num != null) {
            setUserProvisioningState(num.intValue(), identifier);
        }
    }

    @VisibleForTesting
    public void markUserProvisioningStateFinalized(ProvisioningParams provisioningParams) {
        if (provisioningParams.provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
            setUserProvisioningState(3, this.mUtils.getManagedProfile(this.mContext).getIdentifier());
            setUserProvisioningState(0, this.mMyUserId);
        } else {
            setUserProvisioningState(3, this.mMyUserId);
        }
    }

    @VisibleForTesting
    public boolean isStateUnmanagedOrFinalized() {
        int userProvisioningState = this.mDevicePolicyManager.getUserProvisioningState();
        return userProvisioningState == 0 || userProvisioningState == 3;
    }

    private void setUserProvisioningState(int i, int i2) {
        ProvisionLogger.logi("Setting userProvisioningState for user " + i2 + " to: " + i);
        this.mDevicePolicyManager.setUserProvisioningState(i, i2);
    }
}
