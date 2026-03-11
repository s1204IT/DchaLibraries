package com.android.settings.applications;

import android.content.pm.ApplicationInfo;
import android.os.UserManager;
import com.android.settingslib.wrapper.PackageManagerWrapper;

public abstract class InstalledAppLister extends AppLister {
    public InstalledAppLister(PackageManagerWrapper packageManagerWrapper, UserManager userManager) {
        super(packageManagerWrapper, userManager);
    }

    @Override
    protected boolean includeInCount(ApplicationInfo applicationInfo) {
        return InstalledAppCounter.includeInCount(1, this.mPm, applicationInfo);
    }
}
