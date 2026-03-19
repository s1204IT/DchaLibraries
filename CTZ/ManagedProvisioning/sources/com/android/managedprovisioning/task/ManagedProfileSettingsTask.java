package com.android.managedprovisioning.task;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.SettingsFacade;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;

public class ManagedProfileSettingsTask extends AbstractProvisioningTask {

    @VisibleForTesting
    static final boolean DEFAULT_CONTACT_REMOTE_SEARCH = true;
    private final CrossProfileIntentFiltersSetter mCrossProfileIntentFiltersSetter;
    private final SettingsFacade mSettingsFacade;

    public ManagedProfileSettingsTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        this(new SettingsFacade(), new CrossProfileIntentFiltersSetter(context), context, provisioningParams, callback);
    }

    @VisibleForTesting
    ManagedProfileSettingsTask(SettingsFacade settingsFacade, CrossProfileIntentFiltersSetter crossProfileIntentFiltersSetter, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mSettingsFacade = (SettingsFacade) Preconditions.checkNotNull(settingsFacade);
        this.mCrossProfileIntentFiltersSetter = (CrossProfileIntentFiltersSetter) Preconditions.checkNotNull(crossProfileIntentFiltersSetter);
    }

    @Override
    public void run(int i) {
        this.mSettingsFacade.setProfileContactRemoteSearch(this.mContext, DEFAULT_CONTACT_REMOTE_SEARCH, i);
        ((UserManager) this.mContext.getSystemService("user")).setUserRestriction("no_wallpaper", DEFAULT_CONTACT_REMOTE_SEARCH, UserHandle.of(i));
        if (this.mProvisioningParams.mainColor != null) {
            ((DevicePolicyManager) this.mContext.getSystemService("device_policy")).setOrganizationColorForUser(this.mProvisioningParams.mainColor.intValue(), i);
        }
        this.mCrossProfileIntentFiltersSetter.setFilters(UserHandle.myUserId(), i);
        this.mSettingsFacade.setUserSetupCompleted(this.mContext, i);
        success();
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_finishing_touches;
    }
}
