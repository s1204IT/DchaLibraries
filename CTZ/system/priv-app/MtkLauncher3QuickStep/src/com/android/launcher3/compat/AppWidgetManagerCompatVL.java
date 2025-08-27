package com.android.launcher3.compat;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.Nullable;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.PackageUserKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
class AppWidgetManagerCompatVL extends AppWidgetManagerCompat {
    private final UserManager mUserManager;

    AppWidgetManagerCompatVL(Context context) {
        super(context);
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    @Override // com.android.launcher3.compat.AppWidgetManagerCompat
    public List<AppWidgetProviderInfo> getAllProviders(@Nullable PackageUserKey packageUserKey) {
        if (packageUserKey == null) {
            ArrayList arrayList = new ArrayList();
            Iterator<UserHandle> it = this.mUserManager.getUserProfiles().iterator();
            while (it.hasNext()) {
                arrayList.addAll(this.mAppWidgetManager.getInstalledProvidersForProfile(it.next()));
            }
            return arrayList;
        }
        ArrayList arrayList2 = new ArrayList(this.mAppWidgetManager.getInstalledProvidersForProfile(packageUserKey.mUser));
        Iterator it2 = arrayList2.iterator();
        while (it2.hasNext()) {
            if (!((AppWidgetProviderInfo) it2.next()).provider.getPackageName().equals(packageUserKey.mPackageName)) {
                it2.remove();
            }
        }
        return arrayList2;
    }

    @Override // com.android.launcher3.compat.AppWidgetManagerCompat
    public boolean bindAppWidgetIdIfAllowed(int i, AppWidgetProviderInfo appWidgetProviderInfo, Bundle bundle) {
        return this.mAppWidgetManager.bindAppWidgetIdIfAllowed(i, appWidgetProviderInfo.getProfile(), appWidgetProviderInfo.provider, bundle);
    }

    @Override // com.android.launcher3.compat.AppWidgetManagerCompat
    public LauncherAppWidgetProviderInfo findProvider(ComponentName componentName, UserHandle userHandle) {
        for (AppWidgetProviderInfo appWidgetProviderInfo : getAllProviders(new PackageUserKey(componentName.getPackageName(), userHandle))) {
            if (appWidgetProviderInfo.provider.equals(componentName)) {
                return LauncherAppWidgetProviderInfo.fromProviderInfo(this.mContext, appWidgetProviderInfo);
            }
        }
        return null;
    }

    @Override // com.android.launcher3.compat.AppWidgetManagerCompat
    public HashMap<ComponentKey, AppWidgetProviderInfo> getAllProvidersMap() {
        HashMap<ComponentKey, AppWidgetProviderInfo> map = new HashMap<>();
        for (UserHandle userHandle : this.mUserManager.getUserProfiles()) {
            for (AppWidgetProviderInfo appWidgetProviderInfo : this.mAppWidgetManager.getInstalledProvidersForProfile(userHandle)) {
                map.put(new ComponentKey(appWidgetProviderInfo.provider, userHandle), appWidgetProviderInfo);
            }
        }
        return map;
    }
}
