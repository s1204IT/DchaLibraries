package com.android.managedprovisioning.finalization;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import com.android.managedprovisioning.common.IllegalProvisioningArgumentException;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;

class ProvisioningIntentProvider {
    ProvisioningIntentProvider() {
    }

    void maybeLaunchDpc(ProvisioningParams provisioningParams, int i, Utils utils, Context context) {
        if (utils.canResolveIntentAsUser(context, createDpcLaunchIntent(provisioningParams), i)) {
            context.startActivityAsUser(createDpcLaunchIntent(provisioningParams), UserHandle.of(i));
            ProvisionLogger.logd("Dpc was launched for user: " + i);
        }
    }

    Intent createProvisioningCompleteIntent(ProvisioningParams provisioningParams, int i, Utils utils, Context context) {
        Intent intent = new Intent("android.app.action.PROFILE_PROVISIONING_COMPLETE");
        try {
            intent.setComponent(provisioningParams.inferDeviceAdminComponentName(utils, context, i));
            intent.addFlags(268435488);
            addExtrasToIntent(intent, provisioningParams);
            return intent;
        } catch (IllegalProvisioningArgumentException e) {
            ProvisionLogger.loge("Failed to infer the device admin component name", e);
            return null;
        }
    }

    private Intent createDpcLaunchIntent(ProvisioningParams provisioningParams) {
        Intent intent = new Intent("android.app.action.PROVISIONING_SUCCESSFUL");
        String strInferDeviceAdminPackageName = provisioningParams.inferDeviceAdminPackageName();
        if (strInferDeviceAdminPackageName == null) {
            ProvisionLogger.loge("Device admin package name is null");
            return null;
        }
        intent.setPackage(strInferDeviceAdminPackageName);
        intent.addFlags(268435456);
        addExtrasToIntent(intent, provisioningParams);
        return intent;
    }

    private void addExtrasToIntent(Intent intent, ProvisioningParams provisioningParams) {
        intent.putExtra("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE", provisioningParams.adminExtrasBundle);
    }
}
