package com.mediatek.server.pm;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageParser;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import com.android.server.pm.PackageManagerException;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageSetting;
import com.android.server.pm.UserManagerService;
import java.io.PrintWriter;
import java.util.List;

public class PmsExt {
    public static final int INDEX_CIP_FW = 1;
    public static final int INDEX_CUSTOM_APP = 7;
    public static final int INDEX_CUSTOM_PLUGIN = 8;
    public static final int INDEX_OP_APP = 4;
    public static final int INDEX_ROOT_PLUGIN = 5;
    public static final int INDEX_RSC_APP = 12;
    public static final int INDEX_RSC_FW = 10;
    public static final int INDEX_RSC_OVERLAY = 9;
    public static final int INDEX_RSC_PLUGIN = 13;
    public static final int INDEX_RSC_PRIV = 11;
    public static final int INDEX_VENDOR_FW = 2;
    public static final int INDEX_VENDOR_PLUGIN = 6;
    public static final int INDEX_VENDOR_PRIV = 3;

    public void init(PackageManagerService packageManagerService, UserManagerService userManagerService) {
    }

    public void scanDirLI(int i, int i2, int i3, long j) {
    }

    public void scanMoreDirLi(int i, int i2) {
    }

    public void checkMtkResPkg(PackageParser.Package r1) throws PackageManagerException {
    }

    public boolean needSkipScanning(PackageParser.Package r1, PackageSetting packageSetting, PackageSetting packageSetting2) {
        return false;
    }

    public boolean needSkipAppInfo(ApplicationInfo applicationInfo) {
        return false;
    }

    public void onPackageAdded(String str, int i) {
    }

    public void initBeforeScan() {
    }

    public void initAfterScan(ArrayMap<String, PackageSetting> arrayMap) {
    }

    public int customizeInstallPkgFlags(int i, PackageInfoLite packageInfoLite, ArrayMap<String, PackageSetting> arrayMap, UserHandle userHandle) {
        return i;
    }

    public void updatePackageSettings(int i, String str, PackageParser.Package r3, PackageSetting packageSetting, int[] iArr, String str2) {
    }

    public int customizeDeletePkgFlags(int i, String str) {
        return i;
    }

    public int customizeDeletePkg(int[] iArr, String str, int i, int i2) {
        return 1;
    }

    public boolean dumpCmdHandle(String str, PrintWriter printWriter, String[] strArr, int i) {
        return false;
    }

    public ApplicationInfo updateApplicationInfoForRemovable(ApplicationInfo applicationInfo) {
        return applicationInfo;
    }

    public ApplicationInfo updateApplicationInfoForRemovable(String str, ApplicationInfo applicationInfo) {
        return applicationInfo;
    }

    public ActivityInfo updateActivityInfoForRemovable(ActivityInfo activityInfo) throws RemoteException {
        return activityInfo;
    }

    public List<ResolveInfo> updateResolveInfoListForRemovable(List<ResolveInfo> list) throws RemoteException {
        return list;
    }

    public PackageInfo updatePackageInfoForRemovable(PackageInfo packageInfo) {
        return packageInfo;
    }

    public boolean isRemovableSysApp(String str) {
        return false;
    }

    public boolean updateNativeLibDir(ApplicationInfo applicationInfo, String str) {
        return false;
    }
}
