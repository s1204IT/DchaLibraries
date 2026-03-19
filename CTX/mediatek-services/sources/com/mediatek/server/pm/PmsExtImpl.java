package com.mediatek.server.pm;

import android.app.AppGlobals;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageParser;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.PackageManagerException;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageSetting;
import com.android.server.pm.UserManagerService;
import com.mediatek.datashaping.DataShapingServiceImpl;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class PmsExtImpl extends PmsExt {
    private static final int PARSE_IS_OPERATOR = 256;
    private static final File REMOVABLE_SYS_APP_LIST_BAK;
    private static final File REMOVABLE_SYS_APP_LIST_SYSTEM;
    private static final File REMOVABLE_SYS_APP_LIST_VENDOR;
    static final int SCAN_AS_OEM = 524288;
    static final int SCAN_AS_PRIVILEGED = 262144;
    static final int SCAN_AS_PRODUCT = 2097152;
    static final int SCAN_AS_SYSTEM = 131072;
    static final int SCAN_AS_VENDOR = 1048576;
    private static final String SCAN_NAME_NO_DEX = "SCAN_NO_DEX";
    static final int SCAN_NO_DEX = 1;
    private static final String SYS_RSC_PATH_CAP = "/system";
    static final String TAG = "PmsExtImpl";
    private static final String VND_RSC_PATH_CAP = "/vendor";
    private static File mAppLib32InstallDir;
    private static boolean sRemovableSysAppEnabled;
    private static int sScanNoDex;
    private static HashSet<String> sUninstallerAppSet;
    private ApplicationInfo mMediatekApplication = null;
    private PackageManagerService mPms;
    private UserManagerService mUms;
    private static boolean sLogEnabled = false;
    private static String sSysRscPath = SystemProperties.get("ro.vendor.sys.current_rsc_path", "");
    private static String sVndRscPath = SystemProperties.get("ro.vendor.vnd.current_rsc_path", "");
    private static HashSet<String> sRemovableSystemAppSet = new HashSet<>();
    private static HashSet<String> sRemovableSystemAppSetBak = new HashSet<>();

    static {
        sRemovableSysAppEnabled = SystemProperties.getInt("persist.vendor.pms_removable", 0) == 1;
        REMOVABLE_SYS_APP_LIST_SYSTEM = Environment.buildPath(Environment.getRootDirectory(), new String[]{"etc", "permissions", "pms_sysapp_removable_system_list.txt"});
        REMOVABLE_SYS_APP_LIST_VENDOR = Environment.buildPath(Environment.getVendorDirectory(), new String[]{"etc", "permissions", "pms_sysapp_removable_vendor_list.txt"});
        REMOVABLE_SYS_APP_LIST_BAK = Environment.buildPath(Environment.getDataDirectory(), new String[]{"system", "pms_sysapp_removable_list_bak.txt"});
        sUninstallerAppSet = new HashSet<>();
    }

    public PmsExtImpl() {
        sScanNoDex = ReflectionHelper.getIntValue(PackageManagerService.class, SCAN_NAME_NO_DEX);
        if (SYS_RSC_PATH_CAP.equals(sSysRscPath)) {
            sSysRscPath = "";
        }
        if (VND_RSC_PATH_CAP.equals(sVndRscPath)) {
            sVndRscPath = "";
        }
        mAppLib32InstallDir = new File(Environment.getDataDirectory(), "app-lib");
    }

    public void init(PackageManagerService packageManagerService, UserManagerService userManagerService) {
        this.mPms = packageManagerService;
        this.mUms = userManagerService;
    }

    public void scanDirLI(int i, int i2, int i3, long j) throws IOException {
        File canonicalFile;
        File canonicalFile2;
        File canonicalFile3;
        File canonicalFile4;
        File canonicalFile5;
        File canonicalFile6;
        File canonicalFile7;
        File canonicalFile8;
        switch (i) {
            case 1:
                this.mPms.scanDirTracedLI(new File("/custom/framework"), i2 | 16, i3 | sScanNoDex | SCAN_AS_SYSTEM, j);
                break;
            case DataShapingServiceImpl.DATA_SHAPING_STATE_OPEN:
                File file = new File(Environment.getVendorDirectory(), "framework");
                try {
                    canonicalFile = file.getCanonicalFile();
                } catch (IOException e) {
                    canonicalFile = file;
                }
                this.mPms.scanDirTracedLI(canonicalFile, i2 | 16, i3 | sScanNoDex | SCAN_AS_SYSTEM, j);
                break;
            case DataShapingServiceImpl.DATA_SHAPING_STATE_CLOSE:
                File file2 = new File(Environment.getVendorDirectory(), "priv-app");
                try {
                    canonicalFile2 = file2.getCanonicalFile();
                } catch (IOException e2) {
                    canonicalFile2 = file2;
                }
                this.mPms.scanDirTracedLI(canonicalFile2, i2 | 16, i3 | SCAN_AS_SYSTEM | SCAN_AS_PRIVILEGED, j);
                break;
            case 4:
                File file3 = new File(Environment.getVendorDirectory(), "/operator/app");
                try {
                    file3 = file3.getCanonicalFile();
                } catch (IOException e3) {
                }
                this.mPms.scanDirTracedLI(file3, i2 | PARSE_IS_OPERATOR, i3, j);
                break;
            case 5:
                this.mPms.scanDirTracedLI(new File(Environment.getRootDirectory(), "plugin"), i2 | 16, i3 | SCAN_AS_SYSTEM, j);
                break;
            case 6:
                File file4 = new File(Environment.getVendorDirectory(), "plugin");
                try {
                    canonicalFile3 = file4.getCanonicalFile();
                } catch (IOException e4) {
                    canonicalFile3 = file4;
                }
                this.mPms.scanDirTracedLI(canonicalFile3, i2 | 16, i3 | SCAN_AS_SYSTEM, j);
                break;
            case 7:
                this.mPms.scanDirTracedLI(new File("/custom/app"), i2 | 16, i3 | SCAN_AS_SYSTEM, j);
                break;
            case 8:
                this.mPms.scanDirTracedLI(new File("/custom/plugin"), i2 | 16, i3 | SCAN_AS_SYSTEM, j);
                break;
            case 9:
                if (!sSysRscPath.isEmpty()) {
                    this.mPms.scanDirTracedLI(new File(sSysRscPath, "overlay"), i2 | 16, i3 | SCAN_AS_SYSTEM, j);
                }
                if (!sVndRscPath.isEmpty()) {
                    File file5 = new File(sVndRscPath, "overlay");
                    try {
                        canonicalFile4 = file5.getCanonicalFile();
                    } catch (IOException e5) {
                        canonicalFile4 = file5;
                    }
                    this.mPms.scanDirTracedLI(canonicalFile4, i2 | 16, i3 | SCAN_AS_SYSTEM | SCAN_AS_VENDOR, j);
                }
                break;
            case 10:
                if (!sSysRscPath.isEmpty()) {
                    this.mPms.scanDirTracedLI(new File(sSysRscPath, "framework"), i2 | 16, i3 | 1 | SCAN_AS_SYSTEM | SCAN_AS_PRIVILEGED, j);
                }
                if (!sVndRscPath.isEmpty()) {
                    File file6 = new File(sVndRscPath, "framework");
                    try {
                        canonicalFile5 = file6.getCanonicalFile();
                    } catch (IOException e6) {
                        canonicalFile5 = file6;
                    }
                    this.mPms.scanDirTracedLI(canonicalFile5, i2 | 16, i3 | 1 | SCAN_AS_SYSTEM | SCAN_AS_VENDOR | SCAN_AS_PRIVILEGED, j);
                }
                break;
            case 11:
                if (!sSysRscPath.isEmpty()) {
                    this.mPms.scanDirTracedLI(new File(sSysRscPath, "priv-app"), i2 | 16, i3 | SCAN_AS_SYSTEM | SCAN_AS_PRIVILEGED, j);
                }
                if (!sVndRscPath.isEmpty()) {
                    File file7 = new File(sVndRscPath, "priv-app");
                    try {
                        canonicalFile6 = file7.getCanonicalFile();
                    } catch (IOException e7) {
                        canonicalFile6 = file7;
                    }
                    this.mPms.scanDirTracedLI(canonicalFile6, i2 | 16, i3 | SCAN_AS_SYSTEM | SCAN_AS_VENDOR | SCAN_AS_PRIVILEGED, j);
                }
                break;
            case 12:
                if (!sSysRscPath.isEmpty()) {
                    this.mPms.scanDirTracedLI(new File(sSysRscPath, "app"), i2 | 16, i3 | SCAN_AS_SYSTEM, j);
                }
                if (!sVndRscPath.isEmpty()) {
                    File file8 = new File(sVndRscPath, "app");
                    try {
                        canonicalFile7 = file8.getCanonicalFile();
                    } catch (IOException e8) {
                        canonicalFile7 = file8;
                    }
                    this.mPms.scanDirTracedLI(canonicalFile7, i2 | 16, i3 | SCAN_AS_SYSTEM | SCAN_AS_VENDOR, j);
                }
                break;
            case 13:
                if (!sSysRscPath.isEmpty()) {
                    this.mPms.scanDirTracedLI(new File(sSysRscPath, "plugin"), i2 | 16, i3 | SCAN_AS_SYSTEM, j);
                }
                if (!sVndRscPath.isEmpty()) {
                    File file9 = new File(sVndRscPath, "plugin");
                    try {
                        canonicalFile8 = file9.getCanonicalFile();
                    } catch (IOException e9) {
                        canonicalFile8 = file9;
                    }
                    this.mPms.scanDirTracedLI(canonicalFile8, i2 | 16, i3 | SCAN_AS_SYSTEM | SCAN_AS_VENDOR, j);
                }
                break;
            default:
                Slog.d(TAG, "Unknown index for ext:" + i);
                break;
        }
    }

    public void scanMoreDirLi(int i, int i2) throws IOException {
        scanDirLI(2, i, i2, 0L);
        scanDirLI(6, i, i2, 0L);
        scanDirLI(4, i, i2, 0L);
        scanDirLI(5, i, i2, 0L);
        scanDirLI(1, i, i2, 0L);
        scanDirLI(11, i, i2, 0L);
        scanDirLI(12, i, i2, 0L);
        scanDirLI(13, i, i2, 0L);
        carrierExpressInstall(i, i2, 0L);
    }

    public void checkMtkResPkg(PackageParser.Package r3) throws PackageManagerException {
        if (r3.packageName.equals("com.mediatek")) {
            if (this.mMediatekApplication != null) {
                Slog.w(TAG, "Core mediatek package being redefined. Skipping.");
                throw new PackageManagerException(-5, "Core android package being redefined. Skipping.");
            }
            this.mMediatekApplication = r3.applicationInfo;
        }
    }

    public boolean needSkipScanning(PackageParser.Package r4, PackageSetting packageSetting, PackageSetting packageSetting2) {
        if (!this.mPms.isFirstBoot() && isRemovableSysApp(r4.packageName) && packageSetting2 == null && packageSetting == null) {
            if (this.mPms.isUpgrade() && !sRemovableSystemAppSetBak.contains(r4.packageName)) {
                Slog.d(TAG, "New added removable sys app by OTA:" + r4.packageName);
                return false;
            }
            Slog.d(TAG, "Skip scanning uninstalled sys package " + r4.packageName);
            return true;
        }
        if (packageSetting2 != null || packageSetting == null) {
            return false;
        }
        Slog.d(TAG, "Skip scanning uninstalled package: " + r4.packageName);
        return true;
    }

    public boolean needSkipAppInfo(ApplicationInfo applicationInfo) {
        if (sRemovableSysAppEnabled && applicationInfo != null && (applicationInfo.flags & 8388608) == 0) {
            return isRemovableSysApp(applicationInfo.packageName);
        }
        return false;
    }

    public void onPackageAdded(String str, int i) {
        updateUninstallerAppSetWithPkg(str, i);
    }

    public void initBeforeScan() throws Throwable {
        if (sRemovableSysAppEnabled) {
            if (sLogEnabled) {
                Slog.d(TAG, "initBeforeScan start");
            }
            buildRemovableSystemAppSet();
            if (sLogEnabled) {
                Slog.d(TAG, "initBeforeScan end");
            }
        }
    }

    public void initAfterScan(ArrayMap<String, PackageSetting> arrayMap) throws Throwable {
        if (sRemovableSysAppEnabled) {
            if (sLogEnabled) {
                Slog.d(TAG, "initAfterScan start");
            }
            buildUninstallerAppSet();
            if ((this.mPms.isFirstBoot() || this.mPms.isUpgrade()) && (sRemovableSystemAppSetBak.isEmpty() || onUpgradeRemovableSystemAppList(sRemovableSystemAppSetBak, sRemovableSystemAppSet, arrayMap))) {
                sWriteRemovableSystemAppToFile(sRemovableSystemAppSet, REMOVABLE_SYS_APP_LIST_BAK);
            }
            if (sLogEnabled) {
                Slog.d(TAG, "initAfterScan end");
            }
        }
    }

    public int customizeInstallPkgFlags(int i, PackageInfoLite packageInfoLite, ArrayMap<String, PackageSetting> arrayMap, UserHandle userHandle) {
        PackageSetting packageSetting = arrayMap.get(packageInfoLite.packageName);
        if (packageSetting != null && isRemovableSysApp(packageInfoLite.packageName)) {
            int[] iArrQueryInstalledUsers = packageSetting.queryInstalledUsers(this.mUms.getUserIds(), true);
            if (sLogEnabled) {
                Slog.d(TAG, "getUser()=" + userHandle + " installedUsers=" + Arrays.toString(iArrQueryInstalledUsers));
            }
            if ((userHandle == UserHandle.ALL || !ArrayUtils.contains(iArrQueryInstalledUsers, userHandle.getIdentifier())) && iArrQueryInstalledUsers != null && iArrQueryInstalledUsers.length != this.mUms.getUserIds().length) {
                Slog.d(TAG, "built in app, set replace and allow downgrade");
                return i | 128 | 2;
            }
            return i;
        }
        return i;
    }

    public void updatePackageSettings(int i, String str, PackageParser.Package r5, PackageSetting packageSetting, int[] iArr, String str2) {
        if (i == -1 && isRemovableSysApp(str) && (r5.applicationInfo.flags & 1) != 0) {
            for (int i2 : iArr) {
                packageSetting.setInstalled(true, i2);
                packageSetting.setEnabled(0, i2, str2);
            }
        }
    }

    public int customizeDeletePkgFlags(int i, String str) {
        if (isRemovableSysApp(str)) {
            return i | 4;
        }
        return i;
    }

    public int customizeDeletePkg(int[] iArr, String str, int i, int i2) {
        int i3 = i2 & (-3);
        int iDeletePackageX = 1;
        for (int i4 : iArr) {
            iDeletePackageX = this.mPms.deletePackageX(str, i, i4, i3);
            if (iDeletePackageX != 1) {
                Slog.w(TAG, "Package delete failed for user " + i4 + ", returnCode " + iDeletePackageX);
            }
        }
        return iDeletePackageX;
    }

    public boolean dumpCmdHandle(String str, PrintWriter printWriter, String[] strArr, int i) {
        if ("log".equals(str)) {
            configLogTag(printWriter, strArr, i);
        } else if ("removable".equals(str)) {
            dumpRemovableSysApps(printWriter, strArr, i);
        } else {
            return super.dumpCmdHandle(str, printWriter, strArr, i);
        }
        return true;
    }

    public ApplicationInfo updateApplicationInfoForRemovable(ApplicationInfo applicationInfo) {
        if (!sRemovableSysAppEnabled || applicationInfo == null) {
            return applicationInfo;
        }
        return updateApplicationInfoForRemovable(this.mPms.getNameForUid(Binder.getCallingUid()), applicationInfo);
    }

    public ApplicationInfo updateApplicationInfoForRemovable(String str, ApplicationInfo applicationInfo) {
        boolean zIsUninstallerApp;
        if (!sRemovableSysAppEnabled || applicationInfo == null) {
            return applicationInfo;
        }
        String str2 = applicationInfo.packageName;
        boolean zIsUninstallerApp2 = false;
        int i = 0;
        zIsUninstallerApp2 = false;
        zIsUninstallerApp2 = false;
        zIsUninstallerApp2 = false;
        if (Binder.getCallingPid() != Process.myPid() && isRemovableSysApp(str2) && str != null) {
            String[] strArrSplit = str.split(":");
            if (strArrSplit.length == 1) {
                zIsUninstallerApp2 = isUninstallerApp(strArrSplit[0]);
            } else if (strArrSplit.length > 1) {
                boolean zEquals = strArrSplit[1].equals("1000");
                if (zEquals) {
                    zIsUninstallerApp2 = zEquals;
                } else {
                    try {
                        String[] packagesForUid = AppGlobals.getPackageManager().getPackagesForUid(Integer.valueOf(strArrSplit[1]).intValue());
                        int length = packagesForUid.length;
                        while (true) {
                            if (i >= length) {
                                break;
                            }
                            String str3 = packagesForUid[i];
                            zIsUninstallerApp = isUninstallerApp(str3);
                            if (!zIsUninstallerApp) {
                                i++;
                                zEquals = zIsUninstallerApp;
                            } else {
                                try {
                                    break;
                                } catch (RemoteException e) {
                                    zIsUninstallerApp2 = zIsUninstallerApp;
                                }
                            }
                        }
                        zIsUninstallerApp2 = zEquals;
                    } catch (RemoteException e2) {
                        zIsUninstallerApp = zEquals;
                    }
                }
            }
            if (sLogEnabled) {
                Slog.d(TAG, "judge for " + str2 + " name=" + str + " clear ? " + zIsUninstallerApp2);
            }
        }
        if (zIsUninstallerApp2 && applicationInfo != null) {
            ApplicationInfo applicationInfo2 = new ApplicationInfo(applicationInfo);
            applicationInfo2.flags &= -130;
            return applicationInfo2;
        }
        return applicationInfo;
    }

    public ActivityInfo updateActivityInfoForRemovable(ActivityInfo activityInfo) throws RemoteException {
        if (activityInfo != null) {
            activityInfo.applicationInfo = updateApplicationInfoForRemovable(AppGlobals.getPackageManager().getNameForUid(Binder.getCallingUid()), activityInfo.applicationInfo);
        }
        return activityInfo;
    }

    public List<ResolveInfo> updateResolveInfoListForRemovable(List<ResolveInfo> list) throws RemoteException {
        if (list != null) {
            for (ResolveInfo resolveInfo : list) {
                resolveInfo.activityInfo.applicationInfo = updateApplicationInfoForRemovable(AppGlobals.getPackageManager().getNameForUid(Binder.getCallingUid()), resolveInfo.activityInfo.applicationInfo);
            }
        }
        return list;
    }

    public PackageInfo updatePackageInfoForRemovable(PackageInfo packageInfo) {
        if (!sRemovableSysAppEnabled || packageInfo == null) {
            return packageInfo;
        }
        packageInfo.applicationInfo = updateApplicationInfoForRemovable(packageInfo.applicationInfo);
        return packageInfo;
    }

    public boolean isRemovableSysApp(String str) {
        if (sRemovableSysAppEnabled) {
            return sRemovableSystemAppSet.contains(str);
        }
        return false;
    }

    public boolean updateNativeLibDir(ApplicationInfo applicationInfo, String str) {
        if (str == null || !str.contains("vendor/operator/app")) {
            return false;
        }
        applicationInfo.nativeLibraryRootDir = new File(mAppLib32InstallDir, PackageManagerService.deriveCodePathName(str)).getAbsolutePath();
        applicationInfo.nativeLibraryRootRequiresIsa = false;
        applicationInfo.nativeLibraryDir = applicationInfo.nativeLibraryRootDir;
        return true;
    }

    private void configLogTag(PrintWriter printWriter, String[] strArr, int i) {
        int i2 = i + 1;
        if (i2 >= strArr.length) {
            printWriter.println("  Invalid argument!");
            return;
        }
        String str = strArr[i];
        boolean zEquals = "on".equals(strArr[i2]);
        if ("a".equals(str)) {
            PackageManagerService.DEBUG_SETTINGS = zEquals;
            PackageManagerService.DEBUG_PREFERRED = zEquals;
            PackageManagerService.DEBUG_UPGRADE = zEquals;
            PackageManagerService.DEBUG_DOMAIN_VERIFICATION = zEquals;
            PackageManagerService.DEBUG_BACKUP = zEquals;
            PackageManagerService.DEBUG_INSTALL = zEquals;
            PackageManagerService.DEBUG_REMOVE = zEquals;
            PackageManagerService.DEBUG_BROADCASTS = zEquals;
            PackageManagerService.DEBUG_SHOW_INFO = zEquals;
            PackageManagerService.DEBUG_PACKAGE_INFO = zEquals;
            PackageManagerService.DEBUG_INTENT_MATCHING = zEquals;
            PackageManagerService.DEBUG_PACKAGE_SCANNING = zEquals;
            PackageManagerService.DEBUG_VERIFY = zEquals;
            PackageManagerService.DEBUG_FILTERS = zEquals;
            PackageManagerService.DEBUG_PERMISSIONS = zEquals;
            PackageManagerService.DEBUG_SHARED_LIBRARIES = zEquals;
            PackageManagerService.DEBUG_DEXOPT = zEquals;
            PackageManagerService.DEBUG_ABI_SELECTION = zEquals;
            PackageManagerService.DEBUG_TRIAGED_MISSING = zEquals;
            PackageManagerService.DEBUG_APP_DATA = zEquals;
        }
    }

    private void carrierExpressInstall(int i, int i2, long j) throws IOException {
        if (!"1".equals(SystemProperties.get("ro.vendor.mtk_carrierexpress_inst_sup"))) {
            scanDirLI(7, i, i2, j);
            scanDirLI(8, i, i2, j);
        } else {
            scanOperatorDirLI(i2);
        }
    }

    private void scanOperatorDirLI(int i) {
        String str = SystemProperties.get("persist.vendor.operator.optr");
        if (str == null || str.length() <= 0) {
            Slog.d(TAG, "No operater defined.");
            return;
        }
        String str2 = "usp-apks-path-" + str + ".txt";
        File file = new File("/custom/usp");
        if (file.exists()) {
            scanCxpApp(file, str2, i);
            return;
        }
        File file2 = new File("/system/usp");
        if (file2.exists()) {
            scanCxpApp(file2, str2, i);
        } else {
            Slog.d(TAG, "No Carrier Express Pack directory.");
        }
    }

    private void scanCxpApp(File file, String str, int i) {
        int i2;
        List<String> pathsFromFile = readPathsFromFile(new File(file, str));
        for (int i3 = 0; i3 < pathsFromFile.size(); i3++) {
            String str2 = pathsFromFile.get(i3);
            File file2 = new File(str2);
            if (str2.contains("removable")) {
                i2 = PARSE_IS_OPERATOR;
            } else {
                i2 = 131088;
            }
            long jUptimeMillis = SystemClock.uptimeMillis();
            Slog.d(TAG, "scan package: " + file2.toString() + " , start at: " + jUptimeMillis + "ms.");
            try {
                this.mPms.scanPackageTracedLI(file2, i2 | 1, i, 0L, (UserHandle) null);
            } catch (PackageManagerException e) {
                Slog.w(TAG, "Failed to parse " + file2 + ": " + e.getMessage());
            }
            long jUptimeMillis2 = SystemClock.uptimeMillis();
            Slog.d(TAG, "scan package: " + file2.toString() + " , end at: " + jUptimeMillis2 + "ms. elapsed time = " + (jUptimeMillis2 - jUptimeMillis) + "ms.");
        }
    }

    private List<String> readPathsFromFile(File file) {
        FileInputStream fileInputStream;
        BufferedReader bufferedReader;
        byte[] bArr = new byte[(int) file.length()];
        ArrayList arrayList = new ArrayList();
        try {
            fileInputStream = new FileInputStream(file);
            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
        } catch (FileNotFoundException e) {
            Slog.d(TAG, "File not found: " + e.toString());
        } catch (IOException e2) {
            Slog.d(TAG, "Can not read file: " + e2.toString());
        }
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null) {
                break;
            }
            arrayList.add(line);
            return arrayList;
        }
        fileInputStream.close();
        return arrayList;
    }

    private void dumpRemovableSysApps(PrintWriter printWriter, String[] strArr, int i) {
        printWriter.println(" sRemovableSysAppEnabled: " + sRemovableSysAppEnabled);
        Iterator<String> it = sRemovableSystemAppSet.iterator();
        printWriter.println(" sRemovableSystemAppSet:");
        while (it.hasNext()) {
            printWriter.println("  " + it.next());
        }
        Iterator<String> it2 = sUninstallerAppSet.iterator();
        printWriter.println(" sUninstallerAppSet:");
        while (it2.hasNext()) {
            printWriter.println("  " + it2.next());
        }
    }

    private void buildRemovableSystemAppSet() throws Throwable {
        if (sRemovableSysAppEnabled) {
            if (sLogEnabled) {
                Slog.d(TAG, "BuildRemovableSystemAppSet start");
            }
            sGetRemovableSystemAppFromFile(sRemovableSystemAppSet, REMOVABLE_SYS_APP_LIST_SYSTEM);
            sGetRemovableSystemAppFromFile(sRemovableSystemAppSet, REMOVABLE_SYS_APP_LIST_VENDOR);
            sGetRemovableSystemAppFromFile(sRemovableSystemAppSetBak, REMOVABLE_SYS_APP_LIST_BAK);
            if (sLogEnabled) {
                Slog.d(TAG, "BuildRemovableSystemAppSet end");
            }
        }
    }

    private void buildUninstallerAppSet() {
        if (sRemovableSysAppEnabled) {
            if (sLogEnabled) {
                Slog.d(TAG, "buildUninstallerAppSet start");
            }
            int[] userIds = this.mUms.getUserIds();
            for (int i = 0; i < userIds.length; i++) {
                Intent intent = new Intent("android.settings.SETTINGS");
                intent.addCategory("android.intent.category.DEFAULT");
                getAppSetByIntent(sUninstallerAppSet, intent, userIds[i]);
                Intent intent2 = new Intent("android.intent.action.MAIN");
                intent2.addCategory("android.intent.category.HOME");
                intent2.addCategory("android.intent.category.DEFAULT");
                getAppSetByIntent(sUninstallerAppSet, intent2, userIds[i]);
                Intent intent3 = new Intent("android.intent.action.MAIN");
                intent3.addCategory("android.intent.category.APP_MARKET");
                intent3.addCategory("android.intent.category.DEFAULT");
                getAppSetByIntent(sUninstallerAppSet, intent3, userIds[i]);
                Intent intent4 = new Intent("android.intent.action.INSTALL_PACKAGE");
                intent4.addCategory("android.intent.category.DEFAULT");
                intent4.setData(Uri.fromParts("package", "foo.bar", null));
                Intent intent5 = new Intent("android.intent.action.UNINSTALL_PACKAGE");
                intent5.addCategory("android.intent.category.DEFAULT");
                intent5.setData(Uri.fromParts("package", "foo.bar", null));
                getAppSetByIntent(sUninstallerAppSet, intent4, userIds[i]);
                getAppSetByIntent(sUninstallerAppSet, intent5, userIds[i]);
                if (sLogEnabled) {
                    Slog.d(TAG, "buildUninstallerAppSet end");
                }
            }
        }
    }

    private void updateUninstallerAppSetWithPkg(String str, int i) {
        if (sRemovableSysAppEnabled && str != null) {
            if (sUninstallerAppSet.contains(str)) {
                Slog.d(TAG, "already in set:" + str);
                return;
            }
            if (sLogEnabled) {
                Slog.d(TAG, "updateUninstallerAppSetWithPkg for:" + str + " with:" + i);
            }
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.HOME");
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setPackage(str);
            getAppSetByIntent(sUninstallerAppSet, intent, i);
            Intent intent2 = new Intent("android.intent.action.MAIN");
            intent2.addCategory("android.intent.category.APP_MARKET");
            intent2.setPackage(str);
            getAppSetByIntent(sUninstallerAppSet, intent2, i);
            if (sLogEnabled) {
                Slog.d(TAG, "updateUninstallerAppSetWithPkg end");
            }
        }
    }

    private static void sGetRemovableSystemAppFromFile(HashSet<String> hashSet, File file) throws Throwable {
        FileReader fileReader;
        ?? line = 0;
        line = 0;
        line = 0;
        line = 0;
        line = 0;
        try {
            try {
                try {
                } catch (IOException e) {
                    Slog.d(TAG, e.getMessage());
                }
            } catch (IOException e2) {
                e = e2;
                fileReader = null;
            } catch (Throwable th) {
                th = th;
                fileReader = null;
            }
            if (!file.exists()) {
                Slog.d(TAG, "file in " + file + " does not exist!");
                return;
            }
            fileReader = new FileReader(file);
            try {
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                while (true) {
                    try {
                        line = bufferedReader.readLine();
                        if (line == 0) {
                            break;
                        }
                        String strTrim = line.trim();
                        if (!TextUtils.isEmpty(strTrim)) {
                            if (sLogEnabled) {
                                Slog.d(TAG, "read line " + strTrim);
                            }
                            hashSet.add(strTrim);
                        }
                    } catch (IOException e3) {
                        e = e3;
                        line = bufferedReader;
                        Slog.d(TAG, e.getMessage());
                        if (line != 0) {
                            line.close();
                        }
                        if (fileReader != null) {
                            fileReader.close();
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        line = bufferedReader;
                        if (line != 0) {
                            try {
                                line.close();
                            } catch (IOException e4) {
                                Slog.d(TAG, e4.getMessage());
                                throw th;
                            }
                        }
                        if (fileReader != null) {
                            fileReader.close();
                        }
                        throw th;
                    }
                }
                bufferedReader.close();
                fileReader.close();
            } catch (IOException e5) {
                e = e5;
            }
        } catch (Throwable th3) {
            th = th3;
        }
    }

    private static void sWriteRemovableSystemAppToFile(HashSet<String> hashSet, File file) throws Throwable {
        BufferedWriter bufferedWriter;
        FileWriter fileWriter;
        BufferedWriter bufferedWriter2 = null;
        try {
            try {
                if (file.exists()) {
                    file.delete();
                }
                fileWriter = new FileWriter(file, false);
                try {
                    try {
                        bufferedWriter = new BufferedWriter(fileWriter);
                        if (hashSet != null) {
                            try {
                                if (!hashSet.isEmpty()) {
                                    Iterator<String> it = hashSet.iterator();
                                    while (it.hasNext()) {
                                        bufferedWriter.write(it.next());
                                        bufferedWriter.newLine();
                                    }
                                    bufferedWriter.flush();
                                    bufferedWriter.close();
                                    fileWriter.close();
                                    return;
                                }
                            } catch (IOException e) {
                                e = e;
                                bufferedWriter2 = bufferedWriter;
                                Slog.d(TAG, e.getMessage());
                                if (bufferedWriter2 != null) {
                                    bufferedWriter2.close();
                                }
                                if (fileWriter != null) {
                                    fileWriter.close();
                                    return;
                                }
                                return;
                            } catch (Throwable th) {
                                th = th;
                                if (bufferedWriter != null) {
                                    try {
                                        bufferedWriter.close();
                                    } catch (IOException e2) {
                                        Slog.d(TAG, e2.getMessage());
                                        throw th;
                                    }
                                }
                                if (fileWriter != null) {
                                    fileWriter.close();
                                }
                                throw th;
                            }
                        }
                        bufferedWriter.write("");
                        bufferedWriter.flush();
                        try {
                            bufferedWriter.close();
                            fileWriter.close();
                        } catch (IOException e3) {
                            Slog.d(TAG, e3.getMessage());
                        }
                    } catch (IOException e4) {
                        e = e4;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    bufferedWriter = bufferedWriter2;
                }
            } catch (IOException e5) {
                Slog.d(TAG, e5.getMessage());
            }
        } catch (IOException e6) {
            e = e6;
            fileWriter = null;
        } catch (Throwable th3) {
            th = th3;
            bufferedWriter = null;
            fileWriter = null;
        }
    }

    private static boolean isUninstallerApp(String str) {
        if (sRemovableSysAppEnabled) {
            return sUninstallerAppSet.contains(str);
        }
        return false;
    }

    private void getAppSetByIntent(HashSet<String> hashSet, Intent intent, int i) {
        List listQueryIntentActivitiesInternal = this.mPms.queryIntentActivitiesInternal(intent, (String) null, 786944, i);
        int size = listQueryIntentActivitiesInternal.size();
        if (sLogEnabled) {
            Slog.d(TAG, "getAppSetByIntent:" + intent + " size=" + size);
        }
        if (size >= 1) {
            for (int i2 = 0; i2 < size; i2++) {
                hashSet.add(((ResolveInfo) listQueryIntentActivitiesInternal.get(i2)).getComponentInfo().packageName);
            }
        }
    }

    private boolean onUpgradeRemovableSystemAppList(HashSet<String> hashSet, HashSet<String> hashSet2, ArrayMap<String, PackageSetting> arrayMap) {
        HashSet hashSet3 = new HashSet();
        HashSet hashSet4 = new HashSet();
        hashSet3.addAll(hashSet2);
        hashSet3.removeAll(hashSet);
        hashSet4.addAll(hashSet);
        hashSet4.removeAll(hashSet2);
        if (sLogEnabled) {
            Slog.d(TAG, "onUpgradeRemovableSystemAppList: add=" + hashSet3.size() + " removed=" + hashSet4.size());
        }
        int[] userIds = this.mUms.getUserIds();
        Iterator it = hashSet4.iterator();
        boolean z = false;
        while (it.hasNext()) {
            PackageSetting packageSetting = arrayMap.get((String) it.next());
            if (packageSetting != null) {
                int[] iArrQueryInstalledUsers = packageSetting.queryInstalledUsers(userIds, false);
                if (iArrQueryInstalledUsers.length > 0) {
                    boolean z2 = z;
                    int i = 0;
                    while (i < iArrQueryInstalledUsers.length) {
                        packageSetting.setInstalled(true, iArrQueryInstalledUsers[i]);
                        packageSetting.setEnabled(0, iArrQueryInstalledUsers[i], "android");
                        i++;
                        z2 = true;
                    }
                    z = z2;
                }
            }
        }
        if (z) {
            this.mPms.scheduleWriteSettingsLocked();
        }
        return hashSet4.size() > 0 || hashSet3.size() > 0;
    }
}
