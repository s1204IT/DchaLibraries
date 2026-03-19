package com.android.packageinstaller;

import android.app.Application;
import android.content.pm.PackageItemInfo;

public class PackageInstallerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PackageItemInfo.setForceSafeLabels(true);
    }
}
