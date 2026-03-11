package com.android.settings.applications;

import android.app.admin.DevicePolicyManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.UserManager;
import com.android.settingslib.wrapper.PackageManagerWrapper;

public abstract class AppWithAdminGrantedPermissionsLister extends AppLister {
    private final DevicePolicyManager mDevicePolicyManager;
    private final IPackageManager mPackageManagerService;
    private final String[] mPermissions;

    public AppWithAdminGrantedPermissionsLister(String[] strArr, PackageManagerWrapper packageManagerWrapper, IPackageManager iPackageManager, DevicePolicyManager devicePolicyManager, UserManager userManager) {
        super(packageManagerWrapper, userManager);
        this.mPermissions = strArr;
        this.mPackageManagerService = iPackageManager;
        this.mDevicePolicyManager = devicePolicyManager;
    }

    @Override
    protected boolean includeInCount(ApplicationInfo applicationInfo) {
        return AppWithAdminGrantedPermissionsCounter.includeInCount(this.mPermissions, this.mDevicePolicyManager, this.mPm, this.mPackageManagerService, applicationInfo);
    }
}
