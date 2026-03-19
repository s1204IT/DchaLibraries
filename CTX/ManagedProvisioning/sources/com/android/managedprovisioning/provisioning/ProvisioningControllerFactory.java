package com.android.managedprovisioning.provisioning;

import android.content.Context;
import android.os.UserHandle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;

@VisibleForTesting
public class ProvisioningControllerFactory {
    private final Utils mUtils = new Utils();

    @VisibleForTesting
    public AbstractProvisioningController createProvisioningController(Context context, ProvisioningParams provisioningParams, ProvisioningControllerCallback provisioningControllerCallback) {
        if (this.mUtils.isDeviceOwnerAction(provisioningParams.provisioningAction)) {
            return new DeviceOwnerProvisioningController(context, provisioningParams, UserHandle.myUserId(), provisioningControllerCallback);
        }
        return new ProfileOwnerProvisioningController(context, provisioningParams, UserHandle.myUserId(), provisioningControllerCallback);
    }
}
