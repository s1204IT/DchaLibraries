package com.android.managedprovisioning.manageduser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.managedprovisioning.common.ProvisionLogger;

public class ManagedUserCreationListener extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if ("android.app.action.MANAGED_USER_CREATED".equals(intent.getAction())) {
            final int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            final boolean booleanExtra = intent.getBooleanExtra("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", false);
            ProvisionLogger.logd("ACTION_MANAGED_USER_CREATED received for user " + intExtra);
            final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
            Thread thread = new Thread(new Runnable() {
                @Override
                public final void run() {
                    ManagedUserCreationListener.lambda$onReceive$0(intExtra, booleanExtra, context, pendingResultGoAsync);
                }
            });
            thread.setPriority(10);
            thread.start();
            return;
        }
        ProvisionLogger.logw("Unexpected intent action: " + intent.getAction());
    }

    static void lambda$onReceive$0(int i, boolean z, Context context, BroadcastReceiver.PendingResult pendingResult) {
        new ManagedUserCreationController(i, z, context).run();
        pendingResult.finish();
    }
}
