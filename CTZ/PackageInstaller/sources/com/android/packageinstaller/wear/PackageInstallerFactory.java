package com.android.packageinstaller.wear;

import android.content.Context;

public class PackageInstallerFactory {
    private static PackageInstallerImpl sPackageInstaller;

    public static synchronized PackageInstallerImpl getPackageInstaller(Context context) {
        if (sPackageInstaller == null) {
            sPackageInstaller = new PackageInstallerImpl(context);
        }
        return sPackageInstaller;
    }
}
