package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class EnterpriseInstalledPackagesPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final boolean mAsync;
    private final ApplicationFeatureProvider mFeatureProvider;

    public EnterpriseInstalledPackagesPreferenceController(Context context, boolean z) {
        super(context);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getApplicationFeatureProvider(context);
        this.mAsync = z;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(final Preference preference) {
        this.mFeatureProvider.calculateNumberOfPolicyInstalledApps(true, new ApplicationFeatureProvider.NumberOfAppsCallback() { // from class: com.android.settings.enterprise.-$$Lambda$EnterpriseInstalledPackagesPreferenceController$ywnQ5T98AEytxQMBHl3WTR7fuAo
            @Override // com.android.settings.applications.ApplicationFeatureProvider.NumberOfAppsCallback
            public final void onNumberOfAppsResult(int i) {
                EnterpriseInstalledPackagesPreferenceController.lambda$updateState$0(this.f$0, preference, i);
            }
        });
    }

    public static /* synthetic */ void lambda$updateState$0(EnterpriseInstalledPackagesPreferenceController enterpriseInstalledPackagesPreferenceController, Preference preference, int i) {
        boolean z = true;
        if (i != 0) {
            preference.setSummary(enterpriseInstalledPackagesPreferenceController.mContext.getResources().getQuantityString(R.plurals.enterprise_privacy_number_packages_lower_bound, i, Integer.valueOf(i)));
        } else {
            z = false;
        }
        preference.setVisible(z);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        if (this.mAsync) {
            return true;
        }
        final Boolean[] boolArr = {null};
        this.mFeatureProvider.calculateNumberOfPolicyInstalledApps(false, new ApplicationFeatureProvider.NumberOfAppsCallback() { // from class: com.android.settings.enterprise.-$$Lambda$EnterpriseInstalledPackagesPreferenceController$cz4T-BR7YJ9IEY1tdj7V5o_-Yuo
            @Override // com.android.settings.applications.ApplicationFeatureProvider.NumberOfAppsCallback
            public final void onNumberOfAppsResult(int i) {
                EnterpriseInstalledPackagesPreferenceController.lambda$isAvailable$1(boolArr, i);
            }
        });
        return boolArr[0].booleanValue();
    }

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: 0x000a: APUT 
  (r1v0 java.lang.Boolean[])
  (0 ??[int, short, byte, char])
  (wrap:java.lang.Boolean:0x0006: INVOKE (wrap:boolean:?: TERNARY null = ((r2v0 int) > (0 int)) ? true : false) STATIC call: java.lang.Boolean.valueOf(boolean):java.lang.Boolean A[MD:(boolean):java.lang.Boolean (c), WRAPPED] (LINE:72))
 (LINE:72) */
    static /* synthetic */ void lambda$isAvailable$1(Boolean[] boolArr, int i) {
        boolArr[0] = Boolean.valueOf(i > 0);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "number_enterprise_installed_packages";
    }
}
