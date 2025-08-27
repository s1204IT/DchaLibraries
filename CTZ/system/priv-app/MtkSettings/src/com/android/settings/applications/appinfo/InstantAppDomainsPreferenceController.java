package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.preference.Preference;
import android.util.ArraySet;
import com.android.settings.Utils;
import com.android.settings.applications.AppDomainsPreference;
import com.android.settingslib.applications.AppUtils;

/* loaded from: classes.dex */
public class InstantAppDomainsPreferenceController extends AppInfoPreferenceControllerBase {
    private PackageManager mPackageManager;

    public InstantAppDomainsPreferenceController(Context context, String str) {
        super(context, str);
        this.mPackageManager = this.mContext.getPackageManager();
    }

    @Override // com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase, com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return AppUtils.isInstant(this.mParent.getPackageInfo().applicationInfo) ? 0 : 3;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        AppDomainsPreference appDomainsPreference = (AppDomainsPreference) preference;
        ArraySet<String> handledDomains = Utils.getHandledDomains(this.mPackageManager, this.mParent.getPackageInfo().packageName);
        String[] strArr = (String[]) handledDomains.toArray(new String[handledDomains.size()]);
        appDomainsPreference.setTitles(strArr);
        appDomainsPreference.setValues(new int[strArr.length]);
    }
}
