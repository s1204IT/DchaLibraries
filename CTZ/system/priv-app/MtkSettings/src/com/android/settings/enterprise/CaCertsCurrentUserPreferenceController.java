package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;

/* loaded from: classes.dex */
public class CaCertsCurrentUserPreferenceController extends CaCertsPreferenceControllerBase {
    static final String CA_CERTS_CURRENT_USER = "ca_certs_current_user";

    public CaCertsCurrentUserPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return CA_CERTS_CURRENT_USER;
    }

    @Override // com.android.settings.enterprise.CaCertsPreferenceControllerBase, com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        int i;
        super.updateState(preference);
        if (this.mFeatureProvider.isInCompMode()) {
            i = R.string.enterprise_privacy_ca_certs_personal;
        } else {
            i = R.string.enterprise_privacy_ca_certs_device;
        }
        preference.setTitle(i);
    }

    @Override // com.android.settings.enterprise.CaCertsPreferenceControllerBase
    protected int getNumberOfCaCerts() {
        return this.mFeatureProvider.getNumberOfOwnerInstalledCaCertsForCurrentUser();
    }
}
