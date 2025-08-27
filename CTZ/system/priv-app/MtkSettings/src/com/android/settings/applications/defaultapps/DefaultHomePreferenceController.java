package com.android.settings.applications.defaultapps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import com.android.settings.R;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class DefaultHomePreferenceController extends DefaultAppPreferenceController {
    static final IntentFilter HOME_FILTER = new IntentFilter("android.intent.action.MAIN");
    private final String mPackageName;

    static {
        HOME_FILTER.addCategory("android.intent.category.HOME");
        HOME_FILTER.addCategory("android.intent.category.DEFAULT");
    }

    public DefaultHomePreferenceController(Context context) {
        super(context);
        this.mPackageName = this.mContext.getPackageName();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "default_home";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_default_home);
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPreferenceController
    protected DefaultAppInfo getDefaultAppInfo() {
        ArrayList arrayList = new ArrayList();
        ComponentName homeActivities = this.mPackageManager.getHomeActivities(arrayList);
        if (homeActivities != null) {
            return new DefaultAppInfo(this.mContext, this.mPackageManager, this.mUserId, homeActivities);
        }
        ActivityInfo onlyAppInfo = getOnlyAppInfo(arrayList);
        if (onlyAppInfo != null) {
            return new DefaultAppInfo(this.mContext, this.mPackageManager, this.mUserId, onlyAppInfo.getComponentName());
        }
        return null;
    }

    private ActivityInfo getOnlyAppInfo(List<ResolveInfo> list) {
        ArrayList arrayList = new ArrayList();
        this.mPackageManager.getHomeActivities(list);
        Iterator<ResolveInfo> it = list.iterator();
        while (it.hasNext()) {
            ActivityInfo activityInfo = it.next().activityInfo;
            if (!activityInfo.packageName.equals(this.mPackageName)) {
                arrayList.add(activityInfo);
            }
        }
        if (arrayList.size() == 1) {
            return (ActivityInfo) arrayList.get(0);
        }
        return null;
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPreferenceController
    protected Intent getSettingIntent(DefaultAppInfo defaultAppInfo) {
        String packageName;
        if (defaultAppInfo == null) {
            return null;
        }
        if (defaultAppInfo.componentName != null) {
            packageName = defaultAppInfo.componentName.getPackageName();
        } else {
            if (defaultAppInfo.packageItemInfo == null) {
                return null;
            }
            packageName = defaultAppInfo.packageItemInfo.packageName;
        }
        Intent intentAddFlags = new Intent("android.intent.action.APPLICATION_PREFERENCES").setPackage(packageName).addFlags(268468224);
        if (this.mPackageManager.queryIntentActivities(intentAddFlags, 0).size() == 1) {
            return intentAddFlags;
        }
        return null;
    }

    public static boolean hasHomePreference(String str, Context context) {
        ArrayList arrayList = new ArrayList();
        context.getPackageManager().getHomeActivities(arrayList);
        for (int i = 0; i < arrayList.size(); i++) {
            if (((ResolveInfo) arrayList.get(i)).activityInfo.packageName.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHomeDefault(String str, PackageManagerWrapper packageManagerWrapper) {
        ComponentName homeActivities = packageManagerWrapper.getHomeActivities(new ArrayList());
        return homeActivities == null || homeActivities.getPackageName().equals(str);
    }
}
