package com.android.settings.security;

import android.content.Context;
import android.security.KeyStore;
import android.support.v7.preference.Preference;
import com.android.settings.R;

public class CredentialStoragePreferenceController extends RestrictedEncryptionPreferenceController {
    private final KeyStore mKeyStore;

    public CredentialStoragePreferenceController(Context context) {
        super(context, "no_config_credentials");
        this.mKeyStore = KeyStore.getInstance();
    }

    @Override
    public String getPreferenceKey() {
        return "credential_storage_type";
    }

    @Override
    public void updateState(Preference preference) {
        int i;
        if (this.mKeyStore.isHardwareBacked()) {
            i = R.string.credential_storage_type_hardware;
        } else {
            i = R.string.credential_storage_type_software;
        }
        preference.setSummary(i);
    }
}
