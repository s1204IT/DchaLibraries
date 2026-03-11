package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppStoreUtil;
import com.android.settingslib.applications.AppUtils;

public class AppInstallerInfoPreferenceController extends AppInfoPreferenceControllerBase {
    private CharSequence mInstallerLabel;
    private String mInstallerPackage;
    private String mPackageName;

    public AppInstallerInfoPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override
    public int getAvailabilityStatus() {
        return (UserManager.get(this.mContext).isManagedProfile() || this.mInstallerLabel == null) ? 3 : 0;
    }

    @Override
    public void updateState(Preference preference) {
        int i;
        if (AppUtils.isInstant(this.mParent.getPackageInfo().applicationInfo)) {
            i = R.string.instant_app_details_summary;
        } else {
            i = R.string.app_install_details_summary;
        }
        preference.setSummary(this.mContext.getString(i, this.mInstallerLabel));
        Intent appStoreLink = AppStoreUtil.getAppStoreLink(this.mContext, this.mInstallerPackage, this.mPackageName);
        if (appStoreLink != null) {
            preference.setIntent(appStoreLink);
        } else {
            preference.setEnabled(false);
        }
    }

    public void setPackageName(String str) {
        this.mPackageName = str;
        this.mInstallerPackage = AppStoreUtil.getInstallerPackageName(this.mContext, this.mPackageName);
        this.mInstallerLabel = Utils.getApplicationLabel(this.mContext, this.mInstallerPackage);
    }
}
