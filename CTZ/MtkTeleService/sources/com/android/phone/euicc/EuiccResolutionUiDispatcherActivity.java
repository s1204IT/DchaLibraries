package com.android.phone.euicc;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.euicc.EuiccService;
import android.util.Log;

public class EuiccResolutionUiDispatcherActivity extends EuiccUiDispatcherActivity {
    @Override
    protected Intent getEuiccUiIntent() {
        String action = getIntent().getAction();
        if (!"android.telephony.euicc.action.RESOLVE_ERROR".equals(action)) {
            Log.w("EuiccResUiDispatcher", "Unsupported action: " + action);
            return null;
        }
        String stringExtra = getIntent().getStringExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_ACTION");
        if (!EuiccService.RESOLUTION_ACTIONS.contains(stringExtra)) {
            Log.w("EuiccResUiDispatcher", "Unknown resolution action: " + stringExtra);
            return null;
        }
        Intent intent = new Intent(stringExtra);
        intent.putExtras(getIntent());
        return intent;
    }

    @Override
    protected void onDispatchFailure() {
        PendingIntent pendingIntent = (PendingIntent) getIntent().getParcelableExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT");
        if (pendingIntent != null) {
            try {
                pendingIntent.send(2);
            } catch (PendingIntent.CanceledException e) {
            }
        }
    }
}
