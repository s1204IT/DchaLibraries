package com.android.settings.users;

import android.app.Fragment;
import android.content.Context;
import android.os.UserHandle;
import com.android.settings.Utils;

public class AutoSyncWorkDataPreferenceController extends AutoSyncPersonalDataPreferenceController {
    public AutoSyncWorkDataPreferenceController(Context context, Fragment fragment) {
        super(context, fragment);
        this.mUserHandle = Utils.getManagedProfileWithDisabled(this.mUserManager);
    }

    @Override
    public String getPreferenceKey() {
        return "auto_sync_work_account_data";
    }

    @Override
    public boolean isAvailable() {
        return (this.mUserHandle == null || this.mUserManager.isManagedProfile() || this.mUserManager.isLinkedUser() || this.mUserManager.getProfiles(UserHandle.myUserId()).size() <= 1) ? false : true;
    }
}
