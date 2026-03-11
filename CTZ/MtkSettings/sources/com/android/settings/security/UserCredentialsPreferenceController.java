package com.android.settings.security;

import android.content.Context;

public class UserCredentialsPreferenceController extends RestrictedEncryptionPreferenceController {
    public UserCredentialsPreferenceController(Context context) {
        super(context, "no_config_credentials");
    }

    @Override
    public String getPreferenceKey() {
        return "user_credentials";
    }
}
