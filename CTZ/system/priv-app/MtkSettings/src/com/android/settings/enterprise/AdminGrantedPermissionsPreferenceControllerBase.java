package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
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

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(final Preference preference) {
        this.mFeatureProvider.calculateNumberOfAppsWithAdminGrantedPermissions(this.mPermissions, true, new ApplicationFeatureProvider.NumberOfAppsCallback() { // from class: com.android.settings.enterprise.-$$Lambda$AdminGrantedPermissionsPreferenceControllerBase$8oa0oEjJK2SZdXqZGB2HrMlBk_0
            @Override // com.android.settings.applications.ApplicationFeatureProvider.NumberOfAppsCallback
            public final void onNumberOfAppsResult(int i) {
                AdminGrantedPermissionsPreferenceControllerBase.lambda$updateState$0(this.f$0, preference, i);
            }
        });
    }

    public static /* synthetic */ void lambda$updateState$0(AdminGrantedPermissionsPreferenceControllerBase adminGrantedPermissionsPreferenceControllerBase, Preference preference, int i) {
        if (i == 0) {
            adminGrantedPermissionsPreferenceControllerBase.mHasApps = false;
        } else {
            preference.setSummary(adminGrantedPermissionsPreferenceControllerBase.mContext.getResources().getQuantityString(R.plurals.enterprise_privacy_number_packages_lower_bound, i, Integer.valueOf(i)));
            adminGrantedPermissionsPreferenceControllerBase.mHasApps = true;
        }
        preference.setVisible(adminGrantedPermissionsPreferenceControllerBase.mHasApps);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        if (this.mAsync) {
            return true;
        }
        final Boolean[] boolArr = {null};
        this.mFeatureProvider.calculateNumberOfAppsWithAdminGrantedPermissions(this.mPermissions, false, new ApplicationFeatureProvider.NumberOfAppsCallback() { // from class: com.android.settings.enterprise.-$$Lambda$AdminGrantedPermissionsPreferenceControllerBase$4ZAcP8cSJJvD_RXkeJP9Rdjuu0k
            @Override // com.android.settings.applications.ApplicationFeatureProvider.NumberOfAppsCallback
            public final void onNumberOfAppsResult(int i) {
                AdminGrantedPermissionsPreferenceControllerBase.lambda$isAvailable$1(boolArr, i);
            }
        });
        this.mHasApps = boolArr[0].booleanValue();
        return this.mHasApps;
    }

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: 0x000a: APUT 
  (r1v0 java.lang.Boolean[])
  (0 ??[int, short, byte, char])
  (wrap:java.lang.Boolean:0x0006: INVOKE (wrap:boolean:?: TERNARY null = ((r2v0 int) > (0 int)) ? true : false) STATIC call: java.lang.Boolean.valueOf(boolean):java.lang.Boolean A[MD:(boolean):java.lang.Boolean (c), WRAPPED] (LINE:78))
 (LINE:78) */
    static /* synthetic */ void lambda$isAvailable$1(Boolean[] boolArr, int i) {
        boolArr[0] = Boolean.valueOf(i > 0);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey()) && this.mHasApps) {
            return super.handlePreferenceTreeClick(preference);
        }
        return false;
    }
}
