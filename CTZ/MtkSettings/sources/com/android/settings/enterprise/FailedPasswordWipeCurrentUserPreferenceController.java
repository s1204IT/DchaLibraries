package com.android.settings.enterprise;

import android.content.Context;

public class FailedPasswordWipeCurrentUserPreferenceController extends FailedPasswordWipePreferenceControllerBase {
    public FailedPasswordWipeCurrentUserPreferenceController(Context context) {
        super(context);
    }

    @Override
    protected int getMaximumFailedPasswordsBeforeWipe() {
        return this.mFeatureProvider.getMaximumFailedPasswordsBeforeWipeInCurrentUser();
    }

    @Override
    public String getPreferenceKey() {
        return "failed_password_wipe_current_user";
    }
}
