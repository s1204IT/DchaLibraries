package com.android.packageinstaller.permission.service;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.permission.RuntimePermissionPresentationInfo;
import android.permissionpresenterservice.RuntimePermissionPresenterService;
import android.util.Log;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.ArrayList;
import java.util.List;

public final class RuntimePermissionPresenterServiceImpl extends RuntimePermissionPresenterService {
    public List<RuntimePermissionPresentationInfo> onGetAppPermissions(String str) {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(str, 4096);
            ArrayList arrayList = new ArrayList();
            for (AppPermissionGroup appPermissionGroup : new AppPermissions(this, packageInfo, null, false, null).getPermissionGroups()) {
                if (Utils.shouldShowPermission(appPermissionGroup, str)) {
                    arrayList.add(new RuntimePermissionPresentationInfo(appPermissionGroup.getLabel(), appPermissionGroup.areRuntimePermissionsGranted(), "android".equals(appPermissionGroup.getDeclaringPackage())));
                }
            }
            return arrayList;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("PermissionPresenter", "Error getting package:" + str, e);
            return null;
        }
    }

    public void onRevokeRuntimePermission(String str, String str2) {
        try {
            AppPermissionGroup groupForPermission = new AppPermissions(this, getPackageManager().getPackageInfo(str, 4096), null, false, null).getGroupForPermission(str2);
            if (groupForPermission != null) {
                groupForPermission.revokeRuntimePermissions(false);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("PermissionPresenter", "Error getting package:" + str, e);
        }
    }
}
