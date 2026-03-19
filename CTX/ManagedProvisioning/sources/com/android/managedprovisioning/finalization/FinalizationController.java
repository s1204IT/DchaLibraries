package com.android.managedprovisioning.finalization;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.SettingsFacade;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;
import java.io.File;

public class FinalizationController {
    private final Context mContext;
    private final UserProvisioningStateHelper mHelper;
    private ProvisioningParams mParams;
    private final ProvisioningIntentProvider mProvisioningIntentProvider;
    private final SettingsFacade mSettingsFacade;
    private final Utils mUtils;

    public FinalizationController(Context context) {
        this(context, new Utils(), new SettingsFacade(), new UserProvisioningStateHelper(context));
    }

    @VisibleForTesting
    FinalizationController(Context context, Utils utils, SettingsFacade settingsFacade, UserProvisioningStateHelper userProvisioningStateHelper) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
        this.mSettingsFacade = (SettingsFacade) Preconditions.checkNotNull(settingsFacade);
        this.mHelper = (UserProvisioningStateHelper) Preconditions.checkNotNull(userProvisioningStateHelper);
        this.mProvisioningIntentProvider = new ProvisioningIntentProvider();
    }

    public void provisioningInitiallyDone(ProvisioningParams provisioningParams) {
        if (!this.mHelper.isStateUnmanagedOrFinalized()) {
            ProvisionLogger.logw("provisioningInitiallyDone called, but state is not finalized or unmanaged");
            return;
        }
        this.mHelper.markUserProvisioningStateInitiallyDone(provisioningParams);
        if ("android.app.action.PROVISION_MANAGED_PROFILE".equals(provisioningParams.provisioningAction) && this.mSettingsFacade.isUserSetupCompleted(this.mContext)) {
            notifyDpcManagedProfile(provisioningParams);
        } else {
            storeProvisioningParams(provisioningParams);
        }
    }

    void provisioningFinalized() {
        if (this.mHelper.isStateUnmanagedOrFinalized()) {
            ProvisionLogger.logw("provisioningInitiallyDone called, but state is finalized or unmanaged");
            return;
        }
        ProvisioningParams provisioningParams = getProvisioningParams();
        if (provisioningParams == null) {
            ProvisionLogger.logw("FinalizationController invoked, but no stored params");
            return;
        }
        if (provisioningParams.provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
            notifyDpcManagedProfile(provisioningParams);
        } else {
            int iMyUserId = UserHandle.myUserId();
            Intent intentCreateProvisioningCompleteIntent = this.mProvisioningIntentProvider.createProvisioningCompleteIntent(provisioningParams, iMyUserId, this.mUtils, this.mContext);
            if (intentCreateProvisioningCompleteIntent == null) {
                return;
            }
            this.mContext.sendBroadcast(intentCreateProvisioningCompleteIntent);
            this.mProvisioningIntentProvider.maybeLaunchDpc(provisioningParams, iMyUserId, this.mUtils, this.mContext);
        }
        this.mHelper.markUserProvisioningStateFinalized(provisioningParams);
    }

    ProvisioningParams getProvisioningParams() {
        if (this.mParams == null) {
            this.mParams = loadProvisioningParamsAndClearFile();
        }
        return this.mParams;
    }

    private void notifyDpcManagedProfile(ProvisioningParams provisioningParams) {
        this.mContext.startService(new Intent(this.mContext, (Class<?>) SendDpcBroadcastService.class).putExtra(SendDpcBroadcastService.EXTRA_PROVISIONING_PARAMS, provisioningParams));
    }

    private void storeProvisioningParams(ProvisioningParams provisioningParams) {
        provisioningParams.save(getProvisioningParamsFile());
    }

    private File getProvisioningParamsFile() {
        return new File(this.mContext.getFilesDir(), "finalization_activity_provisioning_params.xml");
    }

    @VisibleForTesting
    ProvisioningParams loadProvisioningParamsAndClearFile() {
        File provisioningParamsFile = getProvisioningParamsFile();
        ProvisioningParams provisioningParamsLoad = ProvisioningParams.load(provisioningParamsFile);
        provisioningParamsFile.delete();
        return provisioningParamsLoad;
    }
}
