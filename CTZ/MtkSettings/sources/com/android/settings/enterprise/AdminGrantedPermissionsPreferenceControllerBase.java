package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

public abstract class AdminGrantedPermissionsPreferenceControllerBase extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final boolean mAsync;
    private final ApplicationFeatureProvider mFeatureProvider;
    private boolean mHasApps;
    private final String[] mPermissions;

    public AdminGrantedPermissionsPreferenceControllerBase(Context context, boolean z, String[] strArr) {
        super(context);
        this.mPermissions = strArr;
        this.mFeatureProvider = FeatureFactory.getFactory(context).getApplicationFeatureProvider(context);
        this.mAsync = z;
        this.mHasApps = false;
    }

    @Override
    public void updateState(final Preference preference) {
        this.mFeatureProvider.calculateNumberOfAppsWithAdminGrantedPermissions(this.mPermissions, true, new ApplicationFeatureProvider.NumberOfAppsCallback() {
            @Override
            public final void onNumberOfAppsResult(int i) {
                AdminGrantedPermissionsPreferenceControllerBase.lambda$updateState$0(this.f$0, preference, i);
            }
        });
    }

    public static void lambda$updateState$0(AdminGrantedPermissionsPreferenceControllerBase adminGrantedPermissionsPreferenceControllerBase, Preference preference, int i) {
        if (i == 0) {
            adminGrantedPermissionsPreferenceControllerBase.mHasApps = false;
        } else {
            preference.setSummary(adminGrantedPermissionsPreferenceControllerBase.mContext.getResources().getQuantityString(R.plurals.enterprise_privacy_number_packages_lower_bound, i, Integer.valueOf(i)));
            adminGrantedPermissionsPreferenceControllerBase.mHasApps = true;
        }
        preference.setVisible(adminGrantedPermissionsPreferenceControllerBase.mHasApps);
    }

    @Override
    public boolean isAvailable() {
        if (this.mAsync) {
            return true;
        }
        final Boolean[] boolArr = {null};
        this.mFeatureProvider.calculateNumberOfAppsWithAdminGrantedPermissions(this.mPermissions, false, new ApplicationFeatureProvider.NumberOfAppsCallback() {
            @Override
            public final void onNumberOfAppsResult(int i) {
                AdminGrantedPermissionsPreferenceControllerBase.lambda$isAvailable$1(boolArr, i);
            }
        });
        this.mHasApps = boolArr[0].booleanValue();
        return this.mHasApps;
    }

    static void lambda$isAvailable$1(Boolean[] boolArr, int i) {
        boolArr[0] = Boolean.valueOf(i > 0);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey()) && this.mHasApps) {
            return super.handlePreferenceTreeClick(preference);
        }
        return false;
    }
}
