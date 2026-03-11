package com.android.settings.users;

import android.app.Fragment;
import android.content.Context;
import android.os.UserHandle;

public class AutoSyncPersonalDataPreferenceController extends AutoSyncDataPreferenceController {
    public AutoSyncPersonalDataPreferenceController(Context context, Fragment fragment) {
        super(context, fragment);
    }

    @Override
    public boolean isAvailable() {
        return (this.mUserManager.isManagedProfile() || this.mUserManager.isLinkedUser() || this.mUserManager.getProfiles(UserHandle.myUserId()).size() <= 1) ? false : true;
    }

    @Override
    public String getPreferenceKey() {
        return "auto_sync_personal_account_data";
    }
}
