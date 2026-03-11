package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.ArraySet;
import com.android.settings.R;
import com.android.settingslib.applications.DefaultAppInfo;
import java.util.ArrayList;
import java.util.List;

public class DefaultBrowserPicker extends DefaultAppPickerFragment {
    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.default_browser_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 785;
    }

    @Override
    protected String getDefaultKey() {
        return this.mPm.getDefaultBrowserPackageNameAsUser(this.mUserId);
    }

    @Override
    protected boolean setDefaultKey(String str) {
        return this.mPm.setDefaultBrowserPackageNameAsUser(str, this.mUserId);
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        ArrayList arrayList = new ArrayList();
        Context context = getContext();
        List<ResolveInfo> listQueryIntentActivitiesAsUser = this.mPm.queryIntentActivitiesAsUser(DefaultBrowserPreferenceController.BROWSE_PROBE, 131072, this.mUserId);
        int size = listQueryIntentActivitiesAsUser.size();
        ArraySet arraySet = new ArraySet();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = listQueryIntentActivitiesAsUser.get(i);
            if (resolveInfo.activityInfo != null && resolveInfo.handleAllWebDataURI) {
                String str = resolveInfo.activityInfo.packageName;
                if (!arraySet.contains(str)) {
                    try {
                        arrayList.add(new DefaultAppInfo(context, this.mPm, this.mPm.getApplicationInfoAsUser(str, 0, this.mUserId)));
                        arraySet.add(str);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
        }
        return arrayList;
    }
}
