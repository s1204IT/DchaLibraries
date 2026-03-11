package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.util.AttributeSet;
import com.android.settings.AppListPreference;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.List;

public class DefaultHomePreference extends AppListPreference {
    private final ArrayList<ComponentName> mAllHomeComponents;
    private final IntentFilter mHomeFilter;

    public DefaultHomePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAllHomeComponents = new ArrayList<>();
        this.mHomeFilter = new IntentFilter("android.intent.action.MAIN");
        this.mHomeFilter.addCategory("android.intent.category.HOME");
        this.mHomeFilter.addCategory("android.intent.category.DEFAULT");
        refreshHomeOptions();
    }

    @Override
    public void performClick() {
        refreshHomeOptions();
        super.performClick();
    }

    @Override
    protected boolean persistString(String value) {
        if (value != null) {
            ComponentName component = ComponentName.unflattenFromString(value);
            getContext().getPackageManager().replacePreferredActivity(this.mHomeFilter, 1048576, (ComponentName[]) this.mAllHomeComponents.toArray(new ComponentName[0]), component);
            setSummary(getEntry());
        }
        return super.persistString(value);
    }

    public void refreshHomeOptions() {
        String myPkg = getContext().getPackageName();
        ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
        PackageManager pm = getContext().getPackageManager();
        ComponentName currentDefaultHome = pm.getHomeActivities(homeActivities);
        ArrayList<ComponentName> components = new ArrayList<>();
        this.mAllHomeComponents.clear();
        List<CharSequence> summaries = new ArrayList<>();
        boolean mustSupportManagedProfile = hasManagedProfile();
        for (int i = 0; i < homeActivities.size(); i++) {
            ResolveInfo candidate = homeActivities.get(i);
            ActivityInfo info = candidate.activityInfo;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            this.mAllHomeComponents.add(activityName);
            if (!info.packageName.equals(myPkg)) {
                components.add(activityName);
                if (mustSupportManagedProfile && !launcherHasManagedProfilesFeature(candidate, pm)) {
                    summaries.add(getContext().getString(R.string.home_work_profile_not_supported));
                } else {
                    summaries.add(null);
                }
            }
        }
        setComponentNames((ComponentName[]) components.toArray(new ComponentName[0]), currentDefaultHome, (CharSequence[]) summaries.toArray(new CharSequence[0]));
    }

    private boolean launcherHasManagedProfilesFeature(ResolveInfo resolveInfo, PackageManager pm) {
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(resolveInfo.activityInfo.packageName, 0);
            return versionNumberAtLeastL(appInfo.targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean versionNumberAtLeastL(int versionNumber) {
        return versionNumber >= 21;
    }

    private boolean hasManagedProfile() {
        UserManager userManager = (UserManager) getContext().getSystemService(UserManager.class);
        List<UserInfo> profiles = userManager.getProfiles(getContext().getUserId());
        for (UserInfo userInfo : profiles) {
            if (userInfo.isManagedProfile()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasHomePreference(String pkg, Context context) {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        pm.getHomeActivities(homeActivities);
        for (int i = 0; i < homeActivities.size(); i++) {
            if (homeActivities.get(i).activityInfo.packageName.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHomeDefault(String pkg, Context context) {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        ComponentName def = pm.getHomeActivities(homeActivities);
        if (def != null) {
            return def.getPackageName().equals(pkg);
        }
        return false;
    }
}
