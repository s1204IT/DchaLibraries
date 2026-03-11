package com.android.settings.enterprise;

import android.content.Context;

public class FailedPasswordWipeManagedProfilePreferenceController extends FailedPasswordWipePreferenceControllerBase {
    public FailedPasswordWipeManagedProfilePreferenceController(Context context) {
        super(context);
    }

    @Override
    protected int getMaximumFailedPasswordsBeforeWipe() {
        return this.mFeatureProvider.getMaximumFailedPasswordsBeforeWipeInManagedProfile();
    }

    @Override
    public String getPreferenceKey() {
        return "failed_password_wipe_managed_profile";
    }
}
