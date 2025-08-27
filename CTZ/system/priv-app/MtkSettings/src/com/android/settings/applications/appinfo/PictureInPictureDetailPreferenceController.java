package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.SettingsPreferenceFragment;

/* loaded from: classes.dex */
public class PictureInPictureDetailPreferenceController extends AppInfoPreferenceControllerBase {
    private static final String TAG = "PicInPicDetailControl";
    private final PackageManager mPackageManager;
    private String mPackageName;

    public PictureInPictureDetailPreferenceController(Context context, String str) {
        super(context, str);
        this.mPackageManager = context.getPackageManager();
    }

    @Override // com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase, com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return hasPictureInPictureActivites() ? 0 : 3;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        preference.setSummary(getPreferenceSummary());
    }

    @Override // com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return PictureInPictureDetails.class;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [69=4] */
    boolean hasPictureInPictureActivites() {
        if (!this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
            return false;
        }
        PackageInfo packageInfoAsUser = null;
        try {
            packageInfoAsUser = this.mPackageManager.getPackageInfoAsUser(this.mPackageName, 1, UserHandle.myUserId());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Exception while retrieving the package info of " + this.mPackageName, e);
        }
        return packageInfoAsUser != null && PictureInPictureSettings.checkPackageHasPictureInPictureActivities(packageInfoAsUser.packageName, packageInfoAsUser.activities);
    }

    int getPreferenceSummary() {
        return PictureInPictureDetails.getPreferenceSummary(this.mContext, this.mParent.getPackageInfo().applicationInfo.uid, this.mPackageName);
    }

    public void setPackageName(String str) {
        this.mPackageName = str;
    }
}
