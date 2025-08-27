package com.android.systemui.shared.system;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import java.util.List;

/* loaded from: classes.dex */
public class PackageManagerWrapper {
    public static final String ACTION_PREFERRED_ACTIVITY_CHANGED = "android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED";
    private static final PackageManagerWrapper sInstance = new PackageManagerWrapper();
    private static final IPackageManager mIPackageManager = AppGlobals.getPackageManager();

    public static PackageManagerWrapper getInstance() {
        return sInstance;
    }

    public ActivityInfo getActivityInfo(ComponentName componentName, int userId) {
        try {
            return mIPackageManager.getActivityInfo(componentName, 128, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ComponentName getHomeActivities(List<ResolveInfo> allHomeCandidates) {
        try {
            return mIPackageManager.getHomeActivities(allHomeCandidates);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }
}
