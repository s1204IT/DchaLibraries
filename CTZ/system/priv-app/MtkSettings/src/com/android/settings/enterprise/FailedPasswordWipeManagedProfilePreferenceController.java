package com.android.settings.enterprise;

import android.content.Context;

/* loaded from: classes.dex */
public class FailedPasswordWipeManagedProfilePreferenceController extends FailedPasswordWipePreferenceControllerBase {
    public FailedPasswordWipeManagedProfilePreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settings.enterprise.FailedPasswordWipePreferenceControllerBase
    protected int getMaximumFailedPasswordsBeforeWipe() {
        return this.mFeatureProvider.getMaximumFailedPasswordsBeforeWipeInManagedProfile();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "failed_password_wipe_managed_profile";
    }
}
