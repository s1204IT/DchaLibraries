package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.UserManager;
import com.android.settings.SettingsPreferenceFragment;

/* loaded from: classes.dex */
public class WriteSystemSettingsPreferenceController extends AppInfoPreferenceControllerBase {
    public WriteSystemSettingsPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override // com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase, com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        PackageInfo packageInfo;
        if (UserManager.get(this.mContext).isManagedProfile() || (packageInfo = this.mParent.getPackageInfo()) == null || packageInfo.requestedPermissions == null) {
            return 3;
        }
        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
            if (packageInfo.requestedPermissions[i].equals("android.permission.WRITE_SETTINGS")) {
                return 0;
            }
        }
        return 3;
    }

    @Override // com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return WriteSettingsDetails.class;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        return WriteSettingsDetails.getSummary(this.mContext, this.mParent.getAppEntry());
    }
}
