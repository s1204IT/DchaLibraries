package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateInstallAppsBridge;

/* loaded from: classes.dex */
public class ExternalSourceDetailPreferenceController extends AppInfoPreferenceControllerBase {
    private String mPackageName;

    public ExternalSourceDetailPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override // com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase, com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return (!UserManager.get(this.mContext).isManagedProfile() && isPotentialAppSource()) ? 0 : 3;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        preference.setSummary(getPreferenceSummary());
    }

    @Override // com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return ExternalSourcesDetails.class;
    }

    CharSequence getPreferenceSummary() {
        return ExternalSourcesDetails.getPreferenceSummary(this.mContext, this.mParent.getAppEntry());
    }

    boolean isPotentialAppSource() {
        PackageInfo packageInfo = this.mParent.getPackageInfo();
        if (packageInfo == null) {
            return false;
        }
        return new AppStateInstallAppsBridge(this.mContext, null, null).createInstallAppsStateFor(this.mPackageName, packageInfo.applicationInfo.uid).isPotentialAppSource();
    }

    public void setPackageName(String str) {
        this.mPackageName = str;
    }
}
