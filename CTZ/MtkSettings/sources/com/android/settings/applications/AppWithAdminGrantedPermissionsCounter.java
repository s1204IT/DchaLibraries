package com.android.settings.applications;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import com.android.settingslib.wrapper.PackageManagerWrapper;

public abstract class AppWithAdminGrantedPermissionsCounter extends AppCounter {
    private final DevicePolicyManager mDevicePolicyManager;
    private final IPackageManager mPackageManagerService;
    private final String[] mPermissions;

    public AppWithAdminGrantedPermissionsCounter(Context context, String[] strArr, PackageManagerWrapper packageManagerWrapper, IPackageManager iPackageManager, DevicePolicyManager devicePolicyManager) {
        super(context, packageManagerWrapper);
        this.mPermissions = strArr;
        this.mPackageManagerService = iPackageManager;
        this.mDevicePolicyManager = devicePolicyManager;
    }

    @Override
    protected boolean includeInCount(ApplicationInfo applicationInfo) {
        return includeInCount(this.mPermissions, this.mDevicePolicyManager, this.mPm, this.mPackageManagerService, applicationInfo);
    }

    public static boolean includeInCount(String[] strArr, DevicePolicyManager devicePolicyManager, PackageManagerWrapper packageManagerWrapper, IPackageManager iPackageManager, ApplicationInfo applicationInfo) {
        if (applicationInfo.targetSdkVersion >= 23) {
            for (String str : strArr) {
                if (devicePolicyManager.getPermissionGrantState(null, applicationInfo.packageName, str) == 1) {
                    return true;
                }
            }
            return false;
        }
        if (packageManagerWrapper.getInstallReason(applicationInfo.packageName, new UserHandle(UserHandle.getUserId(applicationInfo.uid))) != 1) {
            return false;
        }
        try {
            for (String str2 : strArr) {
                if (iPackageManager.checkUidPermission(str2, applicationInfo.uid) == 0) {
                    return true;
                }
            }
        } catch (RemoteException e) {
        }
        return false;
    }
}
