package com.android.settings.enterprise;

import android.content.Context;

/* loaded from: classes.dex */
public class FailedPasswordWipeCurrentUserPreferenceController extends FailedPasswordWipePreferenceControllerBase {
    public FailedPasswordWipeCurrentUserPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settings.enterprise.FailedPasswordWipePreferenceControllerBase
    protected int getMaximumFailedPasswordsBeforeWipe() {
        return this.mFeatureProvider.getMaximumFailedPasswordsBeforeWipeInCurrentUser();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "failed_password_wipe_current_user";
    }
}
