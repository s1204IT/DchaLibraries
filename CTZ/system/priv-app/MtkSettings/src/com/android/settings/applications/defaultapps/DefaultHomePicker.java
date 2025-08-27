package com.android.settings.applications.defaultapps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settingslib.applications.DefaultAppInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class DefaultHomePicker extends DefaultAppPickerFragment {
    private String mPackageName;

    @Override // com.android.settings.applications.defaultapps.DefaultAppPickerFragment, com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mPackageName = context.getPackageName();
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.default_home_settings;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 787;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected List<DefaultAppInfo> getCandidates() {
        String string;
        boolean zHasManagedProfile = hasManagedProfile();
        ArrayList arrayList = new ArrayList();
        ArrayList<ResolveInfo> arrayList2 = new ArrayList();
        Context context = getContext();
        this.mPm.getHomeActivities(arrayList2);
        for (ResolveInfo resolveInfo : arrayList2) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            ComponentName componentName = new ComponentName(activityInfo.packageName, activityInfo.name);
            if (!activityInfo.packageName.equals(this.mPackageName)) {
                boolean z = true;
                if (zHasManagedProfile && !launcherHasManagedProfilesFeature(resolveInfo)) {
                    string = getContext().getString(R.string.home_work_profile_not_supported);
                    z = false;
                } else {
                    string = null;
                }
                arrayList.add(new DefaultAppInfo(context, this.mPm, this.mUserId, componentName, string, z));
            }
        }
        return arrayList;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected String getDefaultKey() {
        ComponentName homeActivities = this.mPm.getHomeActivities(new ArrayList());
        if (homeActivities != null) {
            return homeActivities.flattenToString();
        }
        return null;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected boolean setDefaultKey(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(str);
        ArrayList arrayList = new ArrayList();
        this.mPm.getHomeActivities(arrayList);
        ArrayList arrayList2 = new ArrayList();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ActivityInfo activityInfo = ((ResolveInfo) it.next()).activityInfo;
            arrayList2.add(new ComponentName(activityInfo.packageName, activityInfo.name));
        }
        this.mPm.replacePreferredActivity(DefaultHomePreferenceController.HOME_FILTER, 1048576, (ComponentName[]) arrayList2.toArray(new ComponentName[0]), componentNameUnflattenFromString);
        Context context = getContext();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.setFlags(268435456);
        context.startActivity(intent);
        return true;
    }

    private boolean hasManagedProfile() {
        Iterator it = this.mUserManager.getProfiles(getContext().getUserId()).iterator();
        while (it.hasNext()) {
            if (((UserInfo) it.next()).isManagedProfile()) {
                return true;
            }
        }
        return false;
    }

    private boolean launcherHasManagedProfilesFeature(ResolveInfo resolveInfo) {
        try {
            return versionNumberAtLeastL(this.mPm.getPackageManager().getApplicationInfo(resolveInfo.activityInfo.packageName, 0).targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean versionNumberAtLeastL(int i) {
        return i >= 21;
    }
}
