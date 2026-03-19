package com.android.packageinstaller.permission.utils;

import android.content.pm.PackageInfo;
import android.os.Process;
import android.util.EventLog;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.Permission;
import java.util.List;

public final class SafetyNetLogger {
    public static void logPermissionsRequested(PackageInfo packageInfo, List<AppPermissionGroup> list) {
        EventLog.writeEvent(1397638484, "individual_permissions_requested", Integer.valueOf(packageInfo.applicationInfo.uid), buildChangedPermissionForPackageMessage(packageInfo.packageName, list));
    }

    public static void logPermissionsToggled(String str, List<AppPermissionGroup> list) {
        EventLog.writeEvent(1397638484, "individual_permissions_toggled", Integer.valueOf(Process.myUid()), buildChangedPermissionForPackageMessage(str, list));
    }

    private static String buildChangedPermissionForPackageMessage(String str, List<AppPermissionGroup> list) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(':');
        int size = list.size();
        for (int i = 0; i < size; i++) {
            AppPermissionGroup appPermissionGroup = list.get(i);
            int size2 = appPermissionGroup.getPermissions().size();
            for (int i2 = 0; i2 < size2; i2++) {
                Permission permission = appPermissionGroup.getPermissions().get(i2);
                if (i > 0 || i2 > 0) {
                    sb.append(';');
                }
                sb.append(permission.getName());
                sb.append('|');
                if (appPermissionGroup.doesSupportRuntimePermissions()) {
                    sb.append(permission.isGranted());
                    sb.append('|');
                } else {
                    sb.append(permission.isGranted() && (permission.getAppOp() == null || permission.isAppOpAllowed()));
                    sb.append('|');
                }
                sb.append(permission.getFlags());
            }
        }
        return sb.toString();
    }
}
