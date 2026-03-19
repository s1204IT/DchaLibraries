package com.android.server.pm.dex;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser;
import android.database.ContentObserver;
import android.os.Build;
import android.os.FileUtils;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.util.jar.StrictJarFile;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.Installer;
import com.android.server.pm.InstructionSets;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.PackageManagerServiceUtils;
import com.android.server.pm.dex.PackageDexUsage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

public class DexManager {
    private static final boolean DEBUG = false;
    private static final String PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB = "pm.dexopt.priv-apps-oob";
    private static final String PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB_LIST = "pm.dexopt.priv-apps-oob-list";
    private static final String TAG = "DexManager";
    private final Context mContext;
    private final Object mInstallLock;

    @GuardedBy("mInstallLock")
    private final Installer mInstaller;
    private final Listener mListener;
    private final PackageDexOptimizer mPackageDexOptimizer;
    private final IPackageManager mPackageManager;
    private static int DEX_SEARCH_NOT_FOUND = 0;
    private static int DEX_SEARCH_FOUND_PRIMARY = 1;
    private static int DEX_SEARCH_FOUND_SPLIT = 2;
    private static int DEX_SEARCH_FOUND_SECONDARY = 3;
    private static final PackageDexUsage.PackageUseInfo DEFAULT_USE_INFO = new PackageDexUsage.PackageUseInfo();

    @GuardedBy("mPackageCodeLocationsCache")
    private final Map<String, PackageCodeLocations> mPackageCodeLocationsCache = new HashMap();
    private final PackageDexUsage mPackageDexUsage = new PackageDexUsage();

    public interface Listener {
        void onReconcileSecondaryDexFile(ApplicationInfo applicationInfo, PackageDexUsage.DexUseInfo dexUseInfo, String str, int i);
    }

    public DexManager(Context context, IPackageManager iPackageManager, PackageDexOptimizer packageDexOptimizer, Installer installer, Object obj, Listener listener) {
        this.mContext = context;
        this.mPackageManager = iPackageManager;
        this.mPackageDexOptimizer = packageDexOptimizer;
        this.mInstaller = installer;
        this.mInstallLock = obj;
        this.mListener = listener;
    }

    public void systemReady() {
        registerSettingObserver();
    }

    public void notifyDexLoad(ApplicationInfo applicationInfo, List<String> list, List<String> list2, String str, int i) {
        try {
            notifyDexLoadInternal(applicationInfo, list, list2, str, i);
        } catch (Exception e) {
            Slog.w(TAG, "Exception while notifying dex load for package " + applicationInfo.packageName, e);
        }
    }

    private void notifyDexLoadInternal(ApplicationInfo applicationInfo, List<String> list, List<String> list2, String str, int i) {
        int i2;
        String str2;
        if (list.size() != list2.size()) {
            Slog.wtf(TAG, "Bad call to noitfyDexLoad: args have different size");
            return;
        }
        if (list.isEmpty()) {
            Slog.wtf(TAG, "Bad call to notifyDexLoad: class loaders list is empty");
            return;
        }
        if (!PackageManagerServiceUtils.checkISA(str)) {
            Slog.w(TAG, "Loading dex files " + list2 + " in unsupported ISA: " + str + "?");
            return;
        }
        String[] strArrSplit = list2.get(0).split(File.pathSeparator);
        String[] strArrProcessContextForDexLoad = DexoptUtils.processContextForDexLoad(list, list2);
        int length = strArrSplit.length;
        int i3 = 0;
        int i4 = 0;
        while (i3 < length) {
            String str3 = strArrSplit[i3];
            DexSearchResult dexPackage = getDexPackage(applicationInfo, str3, i);
            if (dexPackage.mOutcome != DEX_SEARCH_NOT_FOUND) {
                boolean z = !applicationInfo.packageName.equals(dexPackage.mOwningPackageName);
                boolean z2 = dexPackage.mOutcome == DEX_SEARCH_FOUND_PRIMARY || dexPackage.mOutcome == DEX_SEARCH_FOUND_SPLIT;
                if (z2 && !z) {
                    i2 = i3;
                    i3 = i2 + 1;
                } else {
                    if (strArrProcessContextForDexLoad == null) {
                        str2 = "=UnsupportedClassLoaderContext=";
                    } else {
                        str2 = strArrProcessContextForDexLoad[i4];
                    }
                    i2 = i3;
                    if (this.mPackageDexUsage.record(dexPackage.mOwningPackageName, str3, i, str, z, z2, applicationInfo.packageName, str2)) {
                        this.mPackageDexUsage.maybeWriteAsync();
                    }
                }
            } else {
                i2 = i3;
            }
            i4++;
            i3 = i2 + 1;
        }
    }

    public void load(Map<Integer, List<PackageInfo>> map) {
        try {
            loadInternal(map);
        } catch (Exception e) {
            this.mPackageDexUsage.clear();
            Slog.w(TAG, "Exception while loading package dex usage. Starting with a fresh state.", e);
        }
    }

    public void notifyPackageInstalled(PackageInfo packageInfo, int i) {
        if (i == -1) {
            throw new IllegalArgumentException("notifyPackageInstalled called with USER_ALL");
        }
        cachePackageInfo(packageInfo, i);
    }

    public void notifyPackageUpdated(String str, String str2, String[] strArr) {
        cachePackageCodeLocation(str, str2, strArr, null, -1);
        if (this.mPackageDexUsage.clearUsedByOtherApps(str)) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
    }

    public void notifyPackageDataDestroyed(String str, int i) {
        boolean zRemoveUserPackage;
        if (i == -1) {
            zRemoveUserPackage = this.mPackageDexUsage.removePackage(str);
        } else {
            zRemoveUserPackage = this.mPackageDexUsage.removeUserPackage(str, i);
        }
        if (zRemoveUserPackage) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
    }

    private void cachePackageInfo(PackageInfo packageInfo, int i) {
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        cachePackageCodeLocation(packageInfo.packageName, applicationInfo.sourceDir, applicationInfo.splitSourceDirs, new String[]{applicationInfo.dataDir, applicationInfo.deviceProtectedDataDir, applicationInfo.credentialProtectedDataDir}, i);
    }

    private void cachePackageCodeLocation(String str, String str2, String[] strArr, String[] strArr2, int i) {
        synchronized (this.mPackageCodeLocationsCache) {
            PackageCodeLocations packageCodeLocations = (PackageCodeLocations) putIfAbsent(this.mPackageCodeLocationsCache, str, new PackageCodeLocations(str, str2, strArr));
            packageCodeLocations.updateCodeLocation(str2, strArr);
            if (strArr2 != null) {
                for (String str3 : strArr2) {
                    if (str3 != null) {
                        packageCodeLocations.mergeAppDataDirs(str3, i);
                    }
                }
            }
        }
    }

    private void loadInternal(Map<Integer, List<PackageInfo>> map) {
        HashMap map2 = new HashMap();
        HashMap map3 = new HashMap();
        for (Map.Entry<Integer, List<PackageInfo>> entry : map.entrySet()) {
            List<PackageInfo> value = entry.getValue();
            int iIntValue = entry.getKey().intValue();
            for (PackageInfo packageInfo : value) {
                cachePackageInfo(packageInfo, iIntValue);
                ((Set) putIfAbsent(map2, packageInfo.packageName, new HashSet())).add(Integer.valueOf(iIntValue));
                Set set = (Set) putIfAbsent(map3, packageInfo.packageName, new HashSet());
                set.add(packageInfo.applicationInfo.sourceDir);
                if (packageInfo.applicationInfo.splitSourceDirs != null) {
                    Collections.addAll(set, packageInfo.applicationInfo.splitSourceDirs);
                }
            }
        }
        this.mPackageDexUsage.read();
        this.mPackageDexUsage.syncData(map2, map3);
    }

    public PackageDexUsage.PackageUseInfo getPackageUseInfoOrDefault(String str) {
        PackageDexUsage.PackageUseInfo packageUseInfo = this.mPackageDexUsage.getPackageUseInfo(str);
        return packageUseInfo == null ? DEFAULT_USE_INFO : packageUseInfo;
    }

    boolean hasInfoOnPackage(String str) {
        return this.mPackageDexUsage.getPackageUseInfo(str) != null;
    }

    public boolean dexoptSecondaryDex(DexoptOptions dexoptOptions) {
        PackageDexOptimizer forcedUpdatePackageDexOptimizer;
        if (dexoptOptions.isForce()) {
            forcedUpdatePackageDexOptimizer = new PackageDexOptimizer.ForcedUpdatePackageDexOptimizer(this.mPackageDexOptimizer);
        } else {
            forcedUpdatePackageDexOptimizer = this.mPackageDexOptimizer;
        }
        String packageName = dexoptOptions.getPackageName();
        PackageDexUsage.PackageUseInfo packageUseInfoOrDefault = getPackageUseInfoOrDefault(packageName);
        if (packageUseInfoOrDefault.getDexUseInfoMap().isEmpty()) {
            return true;
        }
        boolean z = true;
        for (Map.Entry<String, PackageDexUsage.DexUseInfo> entry : packageUseInfoOrDefault.getDexUseInfoMap().entrySet()) {
            String key = entry.getKey();
            PackageDexUsage.DexUseInfo value = entry.getValue();
            try {
                PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 0, value.getOwnerUserId());
                if (packageInfo == null) {
                    Slog.d(TAG, "Could not find package when compiling secondary dex " + packageName + " for user " + value.getOwnerUserId());
                    this.mPackageDexUsage.removeUserPackage(packageName, value.getOwnerUserId());
                } else {
                    z = z && forcedUpdatePackageDexOptimizer.dexOptSecondaryDexPath(packageInfo.applicationInfo, key, value, dexoptOptions) != -1;
                }
            } catch (RemoteException e) {
                throw new AssertionError(e);
            }
        }
        return z;
    }

    public void reconcileSecondaryDexFiles(String str) {
        int i;
        boolean zReconcileSecondaryDexFile;
        PackageDexUsage.PackageUseInfo packageUseInfoOrDefault = getPackageUseInfoOrDefault(str);
        if (packageUseInfoOrDefault.getDexUseInfoMap().isEmpty()) {
            return;
        }
        boolean z = false;
        for (Map.Entry<String, PackageDexUsage.DexUseInfo> entry : packageUseInfoOrDefault.getDexUseInfoMap().entrySet()) {
            String key = entry.getKey();
            PackageDexUsage.DexUseInfo value = entry.getValue();
            PackageInfo packageInfo = null;
            try {
                packageInfo = this.mPackageManager.getPackageInfo(str, 0, value.getOwnerUserId());
            } catch (RemoteException e) {
            }
            boolean z2 = true;
            if (packageInfo == null) {
                Slog.d(TAG, "Could not find package when compiling secondary dex " + str + " for user " + value.getOwnerUserId());
                z = this.mPackageDexUsage.removeUserPackage(str, value.getOwnerUserId()) || z;
            } else {
                ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                if (applicationInfo.deviceProtectedDataDir == null || !FileUtils.contains(applicationInfo.deviceProtectedDataDir, key)) {
                    if (applicationInfo.credentialProtectedDataDir == null || !FileUtils.contains(applicationInfo.credentialProtectedDataDir, key)) {
                        Slog.e(TAG, "Could not infer CE/DE storage for path " + key);
                        z = this.mPackageDexUsage.removeDexFile(str, key, value.getOwnerUserId()) || z;
                    } else {
                        i = 2;
                    }
                } else {
                    i = 1;
                }
                if (this.mListener != null) {
                    this.mListener.onReconcileSecondaryDexFile(applicationInfo, value, key, i);
                }
                synchronized (this.mInstallLock) {
                    try {
                        zReconcileSecondaryDexFile = this.mInstaller.reconcileSecondaryDexFile(key, str, applicationInfo.uid, (String[]) value.getLoaderIsas().toArray(new String[0]), applicationInfo.volumeUuid, i);
                    } catch (Installer.InstallerException e2) {
                        Slog.e(TAG, "Got InstallerException when reconciling dex " + key + " : " + e2.getMessage());
                        zReconcileSecondaryDexFile = true;
                    }
                }
                if (!zReconcileSecondaryDexFile) {
                    if (!this.mPackageDexUsage.removeDexFile(str, key, value.getOwnerUserId()) && !z) {
                        z2 = false;
                    }
                    z = z2;
                }
            }
        }
        if (z) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
    }

    public RegisterDexModuleResult registerDexModule(ApplicationInfo applicationInfo, String str, boolean z, int i) {
        DexSearchResult dexPackage = getDexPackage(applicationInfo, str, i);
        if (dexPackage.mOutcome == DEX_SEARCH_NOT_FOUND) {
            return new RegisterDexModuleResult(false, "Package not found");
        }
        if (!applicationInfo.packageName.equals(dexPackage.mOwningPackageName)) {
            return new RegisterDexModuleResult(false, "Dex path does not belong to package");
        }
        if (dexPackage.mOutcome == DEX_SEARCH_FOUND_PRIMARY || dexPackage.mOutcome == DEX_SEARCH_FOUND_SPLIT) {
            return new RegisterDexModuleResult(false, "Main apks cannot be registered");
        }
        String[] appDexInstructionSets = InstructionSets.getAppDexInstructionSets(applicationInfo);
        int i2 = 0;
        boolean zRecord = false;
        for (int length = appDexInstructionSets.length; i2 < length; length = length) {
            zRecord |= this.mPackageDexUsage.record(dexPackage.mOwningPackageName, str, i, appDexInstructionSets[i2], z, false, dexPackage.mOwningPackageName, "=UnknownClassLoaderContext=");
            i2++;
        }
        if (zRecord) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
        if (this.mPackageDexOptimizer.dexOptSecondaryDexPath(applicationInfo, str, this.mPackageDexUsage.getPackageUseInfo(dexPackage.mOwningPackageName).getDexUseInfoMap().get(str), new DexoptOptions(applicationInfo.packageName, 2, 0)) != -1) {
            Slog.e(TAG, "Failed to optimize dex module " + str);
        }
        return new RegisterDexModuleResult(true, "Dex module registered successfully");
    }

    public Set<String> getAllPackagesWithSecondaryDexFiles() {
        return this.mPackageDexUsage.getAllPackagesWithSecondaryDexFiles();
    }

    private DexSearchResult getDexPackage(ApplicationInfo applicationInfo, String str, int i) {
        if (str.startsWith("/system/framework/")) {
            return new DexSearchResult("framework", DEX_SEARCH_NOT_FOUND);
        }
        PackageCodeLocations packageCodeLocations = new PackageCodeLocations(applicationInfo, i);
        int iSearchDex = packageCodeLocations.searchDex(str, i);
        if (iSearchDex != DEX_SEARCH_NOT_FOUND) {
            return new DexSearchResult(packageCodeLocations.mPackageName, iSearchDex);
        }
        synchronized (this.mPackageCodeLocationsCache) {
            for (PackageCodeLocations packageCodeLocations2 : this.mPackageCodeLocationsCache.values()) {
                int iSearchDex2 = packageCodeLocations2.searchDex(str, i);
                if (iSearchDex2 != DEX_SEARCH_NOT_FOUND) {
                    return new DexSearchResult(packageCodeLocations2.mPackageName, iSearchDex2);
                }
            }
            return new DexSearchResult(null, DEX_SEARCH_NOT_FOUND);
        }
    }

    private static <K, V> V putIfAbsent(Map<K, V> map, K k, V v) {
        V vPutIfAbsent = map.putIfAbsent(k, v);
        return vPutIfAbsent == null ? v : vPutIfAbsent;
    }

    public void writePackageDexUsageNow() {
        this.mPackageDexUsage.writeNow();
    }

    private void registerSettingObserver() {
        final ContentResolver contentResolver = this.mContext.getContentResolver();
        Handler handler = null;
        ContentObserver contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean z) {
                SystemProperties.set(DexManager.PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB, Settings.Global.getInt(contentResolver, "priv_app_oob_enabled", 0) == 1 ? "true" : "false");
            }
        };
        contentResolver.registerContentObserver(Settings.Global.getUriFor("priv_app_oob_enabled"), false, contentObserver, 0);
        contentObserver.onChange(true);
        ContentObserver contentObserver2 = new ContentObserver(handler) {
            @Override
            public void onChange(boolean z) {
                String string = Settings.Global.getString(contentResolver, "priv_app_oob_list");
                if (string == null) {
                    string = "ALL";
                }
                SystemProperties.set(DexManager.PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB_LIST, string);
            }
        };
        contentResolver.registerContentObserver(Settings.Global.getUriFor("priv_app_oob_list"), false, contentObserver2, 0);
        contentObserver2.onChange(true);
    }

    public static boolean isPackageSelectedToRunOob(String str) {
        return isPackageSelectedToRunOob(Arrays.asList(str));
    }

    public static boolean isPackageSelectedToRunOob(Collection<String> collection) {
        if (!SystemProperties.getBoolean(PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB, false)) {
            return false;
        }
        String str = SystemProperties.get(PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB_LIST, "ALL");
        if ("ALL".equals(str)) {
            return true;
        }
        for (String str2 : str.split(",")) {
            if (collection.contains(str2)) {
                return true;
            }
        }
        return false;
    }

    public static void maybeLogUnexpectedPackageDetails(PackageParser.Package r1) {
        if (Build.IS_DEBUGGABLE && r1.isPrivileged() && isPackageSelectedToRunOob(r1.packageName)) {
            logIfPackageHasUncompressedCode(r1);
        }
    }

    private static void logIfPackageHasUncompressedCode(PackageParser.Package r2) throws Throwable {
        logIfApkHasUncompressedCode(r2.baseCodePath);
        if (!ArrayUtils.isEmpty(r2.splitCodePaths)) {
            for (int i = 0; i < r2.splitCodePaths.length; i++) {
                logIfApkHasUncompressedCode(r2.splitCodePaths[i]);
            }
        }
    }

    private static void logIfApkHasUncompressedCode(String str) throws Throwable {
        StrictJarFile<ZipEntry> strictJarFile;
        StrictJarFile strictJarFile2 = null;
        try {
            try {
                try {
                    strictJarFile = new StrictJarFile(str, false, false);
                } catch (IOException e) {
                    return;
                }
            } catch (IOException e2) {
            }
        } catch (Throwable th) {
            th = th;
            strictJarFile = strictJarFile2;
        }
        try {
            for (ZipEntry zipEntry : strictJarFile) {
                if (zipEntry.getName().endsWith(".dex")) {
                    if (zipEntry.getMethod() != 0) {
                        Slog.w(TAG, "APK " + str + " has compressed dex code " + zipEntry.getName());
                    } else if ((zipEntry.getDataOffset() & 3) != 0) {
                        Slog.w(TAG, "APK " + str + " has unaligned dex code " + zipEntry.getName());
                    }
                } else if (zipEntry.getName().endsWith(".so")) {
                    if (zipEntry.getMethod() != 0) {
                        Slog.w(TAG, "APK " + str + " has compressed native code " + zipEntry.getName());
                    } else if ((zipEntry.getDataOffset() & 4095) != 0) {
                        Slog.w(TAG, "APK " + str + " has unaligned native code " + zipEntry.getName());
                    }
                }
            }
            strictJarFile.close();
        } catch (IOException e3) {
            strictJarFile2 = strictJarFile;
            Slog.wtf(TAG, "Error when parsing APK " + str);
            if (strictJarFile2 != null) {
                strictJarFile2.close();
            }
        } catch (Throwable th2) {
            th = th2;
            if (strictJarFile != null) {
                try {
                    strictJarFile.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
    }

    public static class RegisterDexModuleResult {
        public final String message;
        public final boolean success;

        public RegisterDexModuleResult() {
            this(false, null);
        }

        public RegisterDexModuleResult(boolean z, String str) {
            this.success = z;
            this.message = str;
        }
    }

    private static class PackageCodeLocations {
        private final Map<Integer, Set<String>> mAppDataDirs;
        private String mBaseCodePath;
        private final String mPackageName;
        private final Set<String> mSplitCodePaths;

        public PackageCodeLocations(ApplicationInfo applicationInfo, int i) {
            this(applicationInfo.packageName, applicationInfo.sourceDir, applicationInfo.splitSourceDirs);
            mergeAppDataDirs(applicationInfo.dataDir, i);
        }

        public PackageCodeLocations(String str, String str2, String[] strArr) {
            this.mPackageName = str;
            this.mSplitCodePaths = new HashSet();
            this.mAppDataDirs = new HashMap();
            updateCodeLocation(str2, strArr);
        }

        public void updateCodeLocation(String str, String[] strArr) {
            this.mBaseCodePath = str;
            this.mSplitCodePaths.clear();
            if (strArr != null) {
                for (String str2 : strArr) {
                    this.mSplitCodePaths.add(str2);
                }
            }
        }

        public void mergeAppDataDirs(String str, int i) {
            ((Set) DexManager.putIfAbsent(this.mAppDataDirs, Integer.valueOf(i), new HashSet())).add(str);
        }

        public int searchDex(String str, int i) {
            Set<String> set = this.mAppDataDirs.get(Integer.valueOf(i));
            if (set == null) {
                return DexManager.DEX_SEARCH_NOT_FOUND;
            }
            if (this.mBaseCodePath.equals(str)) {
                return DexManager.DEX_SEARCH_FOUND_PRIMARY;
            }
            if (this.mSplitCodePaths.contains(str)) {
                return DexManager.DEX_SEARCH_FOUND_SPLIT;
            }
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                if (str.startsWith(it.next())) {
                    return DexManager.DEX_SEARCH_FOUND_SECONDARY;
                }
            }
            return DexManager.DEX_SEARCH_NOT_FOUND;
        }
    }

    private class DexSearchResult {
        private int mOutcome;
        private String mOwningPackageName;

        public DexSearchResult(String str, int i) {
            this.mOwningPackageName = str;
            this.mOutcome = i;
        }

        public String toString() {
            return this.mOwningPackageName + "-" + this.mOutcome;
        }
    }
}
