package com.android.settings.security;

import android.content.Context;

public class InstallCredentialsPreferenceController extends RestrictedEncryptionPreferenceController {
    public InstallCredentialsPreferenceController(Context context) {
        super(context, "no_config_credentials");
    }

    @Override
    public String getPreferenceKey() {
        return "credentials_install";
    }
}
