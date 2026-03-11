package com.android.settings.network;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settingslib.RestrictedLockUtils;

public class NetworkResetRestrictionChecker {
    private final Context mContext;
    private final UserManager mUserManager;

    public NetworkResetRestrictionChecker(Context context) {
        this.mContext = context;
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    boolean hasUserBaseRestriction() {
        return RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_network_reset", UserHandle.myUserId());
    }

    boolean isRestrictionEnforcedByAdmin() {
        return RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_network_reset", UserHandle.myUserId()) != null;
    }

    boolean hasUserRestriction() {
        return !this.mUserManager.isAdminUser() || hasUserBaseRestriction();
    }
}
