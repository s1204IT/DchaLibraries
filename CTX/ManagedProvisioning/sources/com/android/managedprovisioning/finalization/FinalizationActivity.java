package com.android.managedprovisioning.finalization;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;

public class FinalizationActivity extends Activity {
    private FinalizationController mFinalizationController;
    private boolean mIsReceiverRegistered;
    private final BroadcastReceiver mUserUnlockedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                return;
            }
            if (((UserManager) FinalizationActivity.this.getSystemService(UserManager.class)).isManagedProfile(intent.getIntExtra("android.intent.extra.user_handle", -1))) {
                FinalizationActivity.this.unregisterUserUnlockedReceiver();
                FinalizationActivity.this.tryFinalizeProvisioning();
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFinalizationController = new FinalizationController(this);
        ProvisioningParams provisioningParams = this.mFinalizationController.getProvisioningParams();
        registerUserUnlockedReceiver();
        UserManager userManager = (UserManager) getSystemService(UserManager.class);
        Utils utils = new Utils();
        if (!provisioningParams.provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE") || userManager.isUserUnlocked(utils.getManagedProfile(this))) {
            unregisterUserUnlockedReceiver();
            tryFinalizeProvisioning();
        }
    }

    private void tryFinalizeProvisioning() {
        this.mFinalizationController.provisioningFinalized();
        finish();
    }

    private void registerUserUnlockedReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        registerReceiverAsUser(this.mUserUnlockedReceiver, UserHandle.ALL, intentFilter, null, null);
        this.mIsReceiverRegistered = true;
    }

    private void unregisterUserUnlockedReceiver() {
        if (!this.mIsReceiverRegistered) {
            return;
        }
        unregisterReceiver(this.mUserUnlockedReceiver);
        this.mIsReceiverRegistered = false;
    }

    @Override
    public final void onDestroy() {
        unregisterUserUnlockedReceiver();
        super.onDestroy();
    }
}
