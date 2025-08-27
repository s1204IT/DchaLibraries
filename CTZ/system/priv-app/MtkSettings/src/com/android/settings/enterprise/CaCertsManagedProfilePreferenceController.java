package com.android.settings.enterprise;

import android.content.Context;

/* loaded from: classes.dex */
public class CaCertsManagedProfilePreferenceController extends CaCertsPreferenceControllerBase {
    static final String CA_CERTS_MANAGED_PROFILE = "ca_certs_managed_profile";

    public CaCertsManagedProfilePreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return CA_CERTS_MANAGED_PROFILE;
    }

    @Override // com.android.settings.enterprise.CaCertsPreferenceControllerBase
    protected int getNumberOfCaCerts() {
        return this.mFeatureProvider.getNumberOfOwnerInstalledCaCertsForManagedProfile();
    }
}
