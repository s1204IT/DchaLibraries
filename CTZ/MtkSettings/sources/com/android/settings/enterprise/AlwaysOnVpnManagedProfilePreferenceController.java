package com.android.settings.enterprise;

import android.content.Context;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

public class AlwaysOnVpnManagedProfilePreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public AlwaysOnVpnManagedProfilePreferenceController(Context context) {
        super(context);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getEnterprisePrivacyFeatureProvider(context);
    }

    @Override
    public boolean isAvailable() {
        return this.mFeatureProvider.isAlwaysOnVpnSetInManagedProfile();
    }

    @Override
    public String getPreferenceKey() {
        return "always_on_vpn_managed_profile";
    }
}
