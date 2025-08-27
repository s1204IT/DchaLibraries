package com.android.settings.security;

import android.content.Context;

/* loaded from: classes.dex */
public class InstallCredentialsPreferenceController extends RestrictedEncryptionPreferenceController {
    public InstallCredentialsPreferenceController(Context context) {
        super(context, "no_config_credentials");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "credentials_install";
    }
}
