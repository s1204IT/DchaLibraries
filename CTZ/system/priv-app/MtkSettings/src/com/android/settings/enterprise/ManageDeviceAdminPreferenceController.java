package com.android.settings.enterprise;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class ManageDeviceAdminPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public ManageDeviceAdminPreferenceController(Context context) {
        super(context);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getEnterprisePrivacyFeatureProvider(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) throws Resources.NotFoundException {
        String quantityString;
        int numberOfActiveDeviceAdminsForCurrentUserAndManagedProfile = this.mFeatureProvider.getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile();
        if (numberOfActiveDeviceAdminsForCurrentUserAndManagedProfile == 0) {
            quantityString = this.mContext.getResources().getString(R.string.number_of_device_admins_none);
        } else {
            quantityString = this.mContext.getResources().getQuantityString(R.plurals.number_of_device_admins, numberOfActiveDeviceAdminsForCurrentUserAndManagedProfile, Integer.valueOf(numberOfActiveDeviceAdminsForCurrentUserAndManagedProfile));
        }
        preference.setSummary(quantityString);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_manage_device_admin);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "manage_device_admin";
    }
}
