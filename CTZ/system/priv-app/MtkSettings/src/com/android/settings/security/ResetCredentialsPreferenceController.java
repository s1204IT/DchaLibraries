package com.android.settings.security;

import android.content.Context;
import android.security.KeyStore;
import android.support.v7.preference.PreferenceScreen;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public class ResetCredentialsPreferenceController extends RestrictedEncryptionPreferenceController implements LifecycleObserver, OnResume {
    private final KeyStore mKeyStore;
    private RestrictedPreference mPreference;

    public ResetCredentialsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, "no_config_credentials");
        this.mKeyStore = KeyStore.getInstance();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "credentials_reset";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (RestrictedPreference) preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mPreference != null && !this.mPreference.isDisabledByAdmin()) {
            this.mPreference.setEnabled(!this.mKeyStore.isEmpty());
        }
    }
}
