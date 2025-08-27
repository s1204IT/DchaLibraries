package com.android.launcher3.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.PackageManagerHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/* loaded from: classes.dex */
public class SdCardAvailableReceiver extends BroadcastReceiver {
    private final Context mContext;
    private final LauncherModel mModel;
    private final MultiHashMap<UserHandle, String> mPackages;

    public SdCardAvailableReceiver(LauncherAppState launcherAppState, MultiHashMap<UserHandle, String> multiHashMap) {
        this.mModel = launcherAppState.getModel();
        this.mContext = launcherAppState.getContext();
        this.mPackages = multiHashMap;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        LauncherAppsCompat launcherAppsCompat = LauncherAppsCompat.getInstance(context);
        PackageManagerHelper packageManagerHelper = new PackageManagerHelper(context);
        for (Map.Entry<UserHandle, String> entry : this.mPackages.entrySet()) {
            UserHandle key = entry.getKey();
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            Iterator it = new HashSet((Collection) entry.getValue()).iterator();
            while (it.hasNext()) {
                String str = (String) it.next();
                if (!launcherAppsCompat.isPackageEnabledForProfile(str, key)) {
                    if (packageManagerHelper.isAppOnSdcard(str, key)) {
                        arrayList2.add(str);
                    } else {
                        arrayList.add(str);
                    }
                }
            }
            if (!arrayList.isEmpty()) {
                this.mModel.onPackagesRemoved(key, (String[]) arrayList.toArray(new String[arrayList.size()]));
            }
            if (!arrayList2.isEmpty()) {
                this.mModel.onPackagesUnavailable((String[]) arrayList2.toArray(new String[arrayList2.size()]), key, false);
            }
        }
        this.mContext.unregisterReceiver(this);
    }
}
