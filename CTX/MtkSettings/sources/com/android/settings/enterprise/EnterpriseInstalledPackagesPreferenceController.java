package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

public class EnterpriseInstalledPackagesPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final boolean mAsync;
    private final ApplicationFeatureProvider mFeatureProvider;

    public EnterpriseInstalledPackagesPreferenceController(Context context, boolean z) {
        super(context);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getApplicationFeatureProvider(context);
        this.mAsync = z;
    }

    @Override
    public void updateState(final Preference preference) {
        this.mFeatureProvider.calculateNumberOfPolicyInstalledApps(true, new ApplicationFeatureProvider.NumberOfAppsCallback() {
            @Override
            public final void onNumberOfAppsResult(int i) {
                EnterpriseInstalledPackagesPreferenceController.lambda$updateState$0(this.f$0, preference, i);
            }
        });
    }

    public static void lambda$updateState$0(EnterpriseInstalledPackagesPreferenceController enterpriseInstalledPackagesPreferenceController, Preference preference, int i) {
        boolean z = true;
        if (i != 0) {
            preference.setSummary(enterpriseInstalledPackagesPreferenceController.mContext.getResources().getQuantityString(R.plurals.enterprise_privacy_number_packages_lower_bound, i, Integer.valueOf(i)));
        } else {
            z = false;
        }
        preference.setVisible(z);
    }

    @Override
    public boolean isAvailable() {
        if (this.mAsync) {
            return true;
        }
        final Boolean[] boolArr = {null};
        this.mFeatureProvider.calculateNumberOfPolicyInstalledApps(false, new ApplicationFeatureProvider.NumberOfAppsCallback() {
            @Override
            public final void onNumberOfAppsResult(int i) {
                EnterpriseInstalledPackagesPreferenceController.lambda$isAvailable$1(boolArr, i);
            }
        });
        return boolArr[0].booleanValue();
    }

    static void lambda$isAvailable$1(Boolean[] boolArr, int i) {
        boolArr[0] = Boolean.valueOf(i > 0);
    }

    @Override
    public String getPreferenceKey() {
        return "number_enterprise_installed_packages";
    }
}
