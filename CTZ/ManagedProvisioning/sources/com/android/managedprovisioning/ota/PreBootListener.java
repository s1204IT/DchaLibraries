package com.android.managedprovisioning.ota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.managedprovisioning.common.ProvisionLogger;

public class PreBootListener extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if ("android.intent.action.PRE_BOOT_COMPLETED".equals(intent.getAction())) {
            final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
            Thread thread = new Thread(new Runnable() {
                @Override
                public final void run() {
                    PreBootListener.lambda$onReceive$0(context, pendingResultGoAsync);
                }
            });
            thread.setPriority(10);
            thread.start();
            return;
        }
        ProvisionLogger.logw("Unexpected intent action: " + intent.getAction());
    }

    static void lambda$onReceive$0(Context context, BroadcastReceiver.PendingResult pendingResult) {
        new OtaController(context).run();
        pendingResult.finish();
    }
}
