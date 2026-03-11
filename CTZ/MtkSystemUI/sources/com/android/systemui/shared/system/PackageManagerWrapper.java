package com.android.systemui.shared.system;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.os.RemoteException;

public class PackageManagerWrapper {
    private static final PackageManagerWrapper sInstance = new PackageManagerWrapper();
    private static final IPackageManager mIPackageManager = AppGlobals.getPackageManager();

    public static PackageManagerWrapper getInstance() {
        return sInstance;
    }

    public ActivityInfo getActivityInfo(ComponentName componentName, int i) {
        try {
            return mIPackageManager.getActivityInfo(componentName, 128, i);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }
}
