package com.android.commands.monkey;

import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PermissionInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MonkeyPermissionUtil {
    private static final String[] MODERN_PERMISSION_GROUPS = {"android.permission-group.CALENDAR", "android.permission-group.CAMERA", "android.permission-group.CONTACTS", "android.permission-group.LOCATION", "android.permission-group.SENSORS", "android.permission-group.SMS", "android.permission-group.PHONE", "android.permission-group.MICROPHONE", "android.permission-group.STORAGE"};
    private static final String PERMISSION_GROUP_PREFIX = "android.permission-group.";
    private static final String PERMISSION_PREFIX = "android.permission.";
    private Map<String, List<PermissionInfo>> mPermissionMap;
    private IPackageManager mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    private boolean mTargetSystemPackages;
    private List<String> mTargetedPackages;

    private static boolean isModernPermissionGroup(String str) {
        for (String str2 : MODERN_PERMISSION_GROUPS) {
            if (str2.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public void setTargetSystemPackages(boolean z) {
        this.mTargetSystemPackages = z;
    }

    private boolean shouldTargetPackage(PackageInfo packageInfo) {
        if (MonkeyUtils.getPackageFilter().checkEnteringPackage(packageInfo.packageName)) {
            return true;
        }
        return (!this.mTargetSystemPackages || MonkeyUtils.getPackageFilter().isPackageInvalid(packageInfo.packageName) || (packageInfo.applicationInfo.flags & 1) == 0) ? false : true;
    }

    private boolean shouldTargetPermission(String str, PermissionInfo permissionInfo) throws RemoteException {
        return permissionInfo.group != null && permissionInfo.protectionLevel == 1 && (this.mPm.getPermissionFlags(((PackageItemInfo) permissionInfo).name, str, UserHandle.myUserId()) & 20) == 0 && isModernPermissionGroup(permissionInfo.group);
    }

    public boolean populatePermissionsMapping() {
        this.mPermissionMap = new HashMap();
        try {
            for (PackageInfo packageInfo : this.mPm.getInstalledPackages(4096, UserHandle.myUserId()).getList()) {
                if (shouldTargetPackage(packageInfo)) {
                    ArrayList arrayList = new ArrayList();
                    if (packageInfo.applicationInfo.targetSdkVersion > 22 && packageInfo.requestedPermissions != null) {
                        for (String str : packageInfo.requestedPermissions) {
                            PermissionInfo permissionInfo = this.mPm.getPermissionInfo(str, "shell", 0);
                            if (permissionInfo != null && shouldTargetPermission(packageInfo.packageName, permissionInfo)) {
                                arrayList.add(permissionInfo);
                            }
                        }
                        if (!arrayList.isEmpty()) {
                            this.mPermissionMap.put(packageInfo.packageName, arrayList);
                        }
                    }
                }
            }
            if (!this.mPermissionMap.isEmpty()) {
                this.mTargetedPackages = new ArrayList(this.mPermissionMap.keySet());
                return true;
            }
            return true;
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with package manager!");
            return false;
        }
    }

    public void dump() {
        Logger.out.println("// Targeted packages and permissions:");
        for (Map.Entry<String, List<PermissionInfo>> entry : this.mPermissionMap.entrySet()) {
            Logger.out.println(String.format("//  + Using %s", entry.getKey()));
            for (PermissionInfo permissionInfo : entry.getValue()) {
                String strSubstring = ((PackageItemInfo) permissionInfo).name;
                if (strSubstring != null && strSubstring.startsWith(PERMISSION_PREFIX)) {
                    strSubstring = strSubstring.substring(PERMISSION_PREFIX.length());
                }
                String strSubstring2 = permissionInfo.group;
                if (strSubstring2 != null && strSubstring2.startsWith(PERMISSION_GROUP_PREFIX)) {
                    strSubstring2 = strSubstring2.substring(PERMISSION_GROUP_PREFIX.length());
                }
                Logger.out.println(String.format("//    Permission: %s [%s]", strSubstring, strSubstring2));
            }
        }
    }

    public MonkeyPermissionEvent generateRandomPermissionEvent(Random random) {
        String str = this.mTargetedPackages.get(random.nextInt(this.mTargetedPackages.size()));
        List<PermissionInfo> list = this.mPermissionMap.get(str);
        return new MonkeyPermissionEvent(str, list.get(random.nextInt(list.size())));
    }
}
