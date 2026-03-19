package com.android.managedprovisioning.finalization;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.UserHandle;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.finalization.DpcReceivedSuccessReceiver;
import com.android.managedprovisioning.model.ProvisioningParams;

public class SendDpcBroadcastService extends Service implements DpcReceivedSuccessReceiver.Callback {
    static String EXTRA_PROVISIONING_PARAMS = "com.android.managedprovisioning.PROVISIONING_PARAMS";

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        ProvisioningParams provisioningParams = (ProvisioningParams) intent.getParcelableExtra(EXTRA_PROVISIONING_PARAMS);
        Utils utils = new Utils();
        ProvisioningIntentProvider provisioningIntentProvider = new ProvisioningIntentProvider();
        UserHandle managedProfile = utils.getManagedProfile(getApplicationContext());
        int identifier = managedProfile.getIdentifier();
        sendOrderedBroadcastAsUser(provisioningIntentProvider.createProvisioningCompleteIntent(provisioningParams, identifier, utils, getApplicationContext()), managedProfile, null, new DpcReceivedSuccessReceiver(provisioningParams.accountToMigrate, provisioningParams.keepAccountMigrated, managedProfile, provisioningParams.inferDeviceAdminPackageName(), this), null, -1, null, null);
        ProvisionLogger.logd("Provisioning complete broadcast has been sent to user " + identifier);
        provisioningIntentProvider.maybeLaunchDpc(provisioningParams, identifier, utils, getApplicationContext());
        return 1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void cleanup() {
        stopSelf();
    }
}
