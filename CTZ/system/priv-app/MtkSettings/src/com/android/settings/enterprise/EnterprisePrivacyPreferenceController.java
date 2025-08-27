package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class EnterprisePrivacyPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public EnterprisePrivacyPreferenceController(Context context) {
        super(context);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getEnterprisePrivacyFeatureProvider(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }
        String deviceOwnerOrganizationName = this.mFeatureProvider.getDeviceOwnerOrganizationName();
        if (deviceOwnerOrganizationName == null) {
            preference.setSummary(R.string.enterprise_privacy_settings_summary_generic);
        } else {
            preference.setSummary(this.mContext.getResources().getString(R.string.enterprise_privacy_settings_summary_with_name, deviceOwnerOrganizationName));
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mFeatureProvider.hasDeviceOwner();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "enterprise_privacy";
    }
}
