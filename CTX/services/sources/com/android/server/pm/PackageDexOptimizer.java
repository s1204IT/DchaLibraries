package com.android.server.pm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser;
import android.content.pm.dex.ArtManager;
import android.content.pm.dex.DexMetadataHelper;
import android.os.FileUtils;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.CompilerStats;
import com.android.server.pm.Installer;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.dex.DexoptUtils;
import com.android.server.pm.dex.PackageDexUsage;
import dalvik.system.DexFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PackageDexOptimizer {
    public static final int DEX_OPT_FAILED = -1;
    public static final int DEX_OPT_PERFORMED = 1;
    public static final int DEX_OPT_SKIPPED = 0;
    static final String OAT_DIR_NAME = "oat";
    public static final String SKIP_SHARED_LIBRARY_CHECK = "&";
    private static final String TAG = "PackageManager.DexOptimizer";
    private static final long WAKELOCK_TIMEOUT_MS = 660000;

    @GuardedBy("mInstallLock")
    private final PowerManager.WakeLock mDexoptWakeLock;
    private final Object mInstallLock;

    @GuardedBy("mInstallLock")
    private final Installer mInstaller;
    private volatile boolean mSystemReady;

    PackageDexOptimizer(Installer installer, Object obj, Context context, String str) {
        this.mInstaller = installer;
        this.mInstallLock = obj;
        this.mDexoptWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, str);
    }

    protected PackageDexOptimizer(PackageDexOptimizer packageDexOptimizer) {
        this.mInstaller = packageDexOptimizer.mInstaller;
        this.mInstallLock = packageDexOptimizer.mInstallLock;
        this.mDexoptWakeLock = packageDexOptimizer.mDexoptWakeLock;
        this.mSystemReady = packageDexOptimizer.mSystemReady;
    }

    static boolean canOptimizePackage(PackageParser.Package r0) {
        if ((r0.applicationInfo.flags & 4) == 0) {
            return false;
        }
        return true;
    }

    int performDexOpt(PackageParser.Package r4, String[] strArr, String[] strArr2, CompilerStats.PackageStats packageStats, PackageDexUsage.PackageUseInfo packageUseInfo, DexoptOptions dexoptOptions) {
        int iPerformDexOptLI;
        if (r4.applicationInfo.uid == -1) {
            throw new IllegalArgumentException("Dexopt for " + r4.packageName + " has invalid uid.");
        }
        if (!canOptimizePackage(r4)) {
            return 0;
        }
        synchronized (this.mInstallLock) {
            long jAcquireWakeLockLI = acquireWakeLockLI(r4.applicationInfo.uid);
            try {
                iPerformDexOptLI = performDexOptLI(r4, strArr, strArr2, packageStats, packageUseInfo, dexoptOptions);
            } finally {
                releaseWakeLockLI(jAcquireWakeLockLI);
            }
        }
        return iPerformDexOptLI;
    }

    @GuardedBy("mInstallLock")
    private int performDexOptLI(PackageParser.Package r34, String[] strArr, String[] strArr2, CompilerStats.PackageStats packageStats, PackageDexUsage.PackageUseInfo packageUseInfo, DexoptOptions dexoptOptions) {
        String[] appDexInstructionSets;
        int i;
        String[] strArr3;
        boolean[] zArr;
        int i2;
        int i3;
        List list;
        String[] strArr4;
        File fileFindDexMetadataForFile;
        PackageDexOptimizer packageDexOptimizer = this;
        PackageParser.Package r15 = r34;
        if (strArr2 == null) {
            appDexInstructionSets = InstructionSets.getAppDexInstructionSets(r15.applicationInfo);
        } else {
            appDexInstructionSets = strArr2;
        }
        String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(appDexInstructionSets);
        List allCodePaths = r34.getAllCodePaths();
        int sharedAppGid = UserHandle.getSharedAppGid(r15.applicationInfo.uid);
        int i4 = -1;
        if (sharedAppGid == -1) {
            Slog.wtf(TAG, "Well this is awkward; package " + r15.applicationInfo.name + " had UID " + r15.applicationInfo.uid, new Throwable());
            sharedAppGid = 9999;
        }
        int i5 = sharedAppGid;
        boolean[] zArr2 = new boolean[allCodePaths.size()];
        zArr2[0] = (r15.applicationInfo.flags & 4) != 0;
        for (int i6 = 1; i6 < allCodePaths.size(); i6++) {
            zArr2[i6] = (r15.splitFlags[i6 + (-1)] & 4) != 0;
        }
        String[] classLoaderContexts = DexoptUtils.getClassLoaderContexts(r15.applicationInfo, strArr, zArr2);
        if (allCodePaths.size() != classLoaderContexts.length) {
            String[] splitCodePaths = r15.applicationInfo.getSplitCodePaths();
            StringBuilder sb = new StringBuilder();
            sb.append("Inconsistent information between PackageParser.Package and its ApplicationInfo. pkg.getAllCodePaths=");
            sb.append(allCodePaths);
            sb.append(" pkg.applicationInfo.getBaseCodePath=");
            sb.append(r15.applicationInfo.getBaseCodePath());
            sb.append(" pkg.applicationInfo.getSplitCodePaths=");
            sb.append(splitCodePaths == null ? "null" : Arrays.toString(splitCodePaths));
            throw new IllegalStateException(sb.toString());
        }
        int i7 = 0;
        int i8 = 0;
        while (i8 < allCodePaths.size()) {
            if (zArr2[i8]) {
                if (classLoaderContexts[i8] == null) {
                    int i9 = i8;
                    throw new IllegalStateException("Inconsistent information in the package structure. A split is marked to contain code but has no dependency listed. Index=" + i9 + " path=" + ((String) allCodePaths.get(i9)));
                }
                String str = (String) allCodePaths.get(i8);
                if (dexoptOptions.getSplitName() != null && !dexoptOptions.getSplitName().equals(new File(str).getName())) {
                    i = i8;
                    strArr3 = classLoaderContexts;
                    zArr = zArr2;
                    i2 = i5;
                    i3 = i4;
                    list = allCodePaths;
                    strArr4 = dexCodeInstructionSets;
                } else {
                    String absolutePath = null;
                    String profileName = ArtManager.getProfileName(i8 == 0 ? null : r15.splitNames[i8 - 1]);
                    if (dexoptOptions.isDexoptInstallWithDexMetadata() && (fileFindDexMetadataForFile = DexMetadataHelper.findDexMetadataForFile(new File(str))) != null) {
                        absolutePath = fileFindDexMetadataForFile.getAbsolutePath();
                    }
                    String str2 = absolutePath;
                    String realCompilerFilter = packageDexOptimizer.getRealCompilerFilter(r15.applicationInfo, dexoptOptions.getCompilerFilter(), dexoptOptions.isDexoptAsSharedLibrary() || packageUseInfo.isUsedByOtherApps(str));
                    boolean z = dexoptOptions.isCheckForProfileUpdates() && packageDexOptimizer.isProfileUpdated(r15, i5, profileName, realCompilerFilter);
                    int dexFlags = packageDexOptimizer.getDexFlags(r15, realCompilerFilter, dexoptOptions);
                    int length = dexCodeInstructionSets.length;
                    int i10 = i7;
                    int i11 = 0;
                    while (i11 < length) {
                        int i12 = i11;
                        int i13 = length;
                        String str3 = realCompilerFilter;
                        String str4 = profileName;
                        String str5 = str;
                        int i14 = i8;
                        String[] strArr5 = classLoaderContexts;
                        boolean[] zArr3 = zArr2;
                        int i15 = i5;
                        int i16 = i4;
                        List list2 = allCodePaths;
                        String[] strArr6 = dexCodeInstructionSets;
                        int iDexOptPath = packageDexOptimizer.dexOptPath(r15, str, dexCodeInstructionSets[i11], str3, z, classLoaderContexts[i8], dexFlags, i5, packageStats, dexoptOptions.isDowngrade(), str4, str2, dexoptOptions.getCompilationReason());
                        int i17 = i10;
                        i10 = (i17 == i16 || iDexOptPath == 0) ? i17 : iDexOptPath;
                        i11 = i12 + 1;
                        i8 = i14;
                        i4 = i16;
                        profileName = str4;
                        classLoaderContexts = strArr5;
                        dexCodeInstructionSets = strArr6;
                        length = i13;
                        realCompilerFilter = str3;
                        str = str5;
                        zArr2 = zArr3;
                        i5 = i15;
                        allCodePaths = list2;
                        packageDexOptimizer = this;
                        r15 = r34;
                    }
                    i = i8;
                    strArr3 = classLoaderContexts;
                    zArr = zArr2;
                    i2 = i5;
                    i3 = i4;
                    list = allCodePaths;
                    strArr4 = dexCodeInstructionSets;
                    i7 = i10;
                }
            }
            i8 = i + 1;
            i4 = i3;
            classLoaderContexts = strArr3;
            dexCodeInstructionSets = strArr4;
            zArr2 = zArr;
            i5 = i2;
            allCodePaths = list;
            packageDexOptimizer = this;
            r15 = r34;
        }
        return i7;
    }

    @GuardedBy("mInstallLock")
    private int dexOptPath(PackageParser.Package r23, String str, String str2, String str3, boolean z, String str4, int i, int i2, CompilerStats.PackageStats packageStats, boolean z2, String str5, String str6, int i3) {
        int dexoptNeeded = getDexoptNeeded(str, str2, str3, str4, z, z2);
        if (Math.abs(dexoptNeeded) == 0) {
            return 0;
        }
        String strCreateOatDirIfSupported = createOatDirIfSupported(r23, str2);
        Log.i(TAG, "Running dexopt (dexoptNeeded=" + dexoptNeeded + ") on: " + str + " pkg=" + r23.applicationInfo.packageName + " isa=" + str2 + " dexoptFlags=" + printDexoptFlags(i) + " targetFilter=" + str3 + " oatDir=" + strCreateOatDirIfSupported + " classLoaderContext=" + str4);
        try {
            long jCurrentTimeMillis = System.currentTimeMillis();
            this.mInstaller.dexopt(str, i2, r23.packageName, str2, dexoptNeeded, strCreateOatDirIfSupported, i, str3, r23.volumeUuid, str4, r23.applicationInfo.seInfo, false, r23.applicationInfo.targetSdkVersion, str5, str6, PackageManagerServiceCompilerMapping.getReasonName(i3));
            if (packageStats != null) {
                packageStats.setCompileTime(str, (int) (System.currentTimeMillis() - jCurrentTimeMillis));
                return 1;
            }
            return 1;
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, "Failed to dexopt", e);
            return -1;
        }
    }

    public int dexOptSecondaryDexPath(ApplicationInfo applicationInfo, String str, PackageDexUsage.DexUseInfo dexUseInfo, DexoptOptions dexoptOptions) {
        int iDexOptSecondaryDexPathLI;
        if (applicationInfo.uid == -1) {
            throw new IllegalArgumentException("Dexopt for path " + str + " has invalid uid.");
        }
        synchronized (this.mInstallLock) {
            long jAcquireWakeLockLI = acquireWakeLockLI(applicationInfo.uid);
            try {
                iDexOptSecondaryDexPathLI = dexOptSecondaryDexPathLI(applicationInfo, str, dexUseInfo, dexoptOptions);
            } finally {
                releaseWakeLockLI(jAcquireWakeLockLI);
            }
        }
        return iDexOptSecondaryDexPathLI;
    }

    @GuardedBy("mInstallLock")
    private long acquireWakeLockLI(int i) {
        if (!this.mSystemReady) {
            return -1L;
        }
        this.mDexoptWakeLock.setWorkSource(new WorkSource(i));
        this.mDexoptWakeLock.acquire(WAKELOCK_TIMEOUT_MS);
        return SystemClock.elapsedRealtime();
    }

    @GuardedBy("mInstallLock")
    private void releaseWakeLockLI(long j) {
        if (j < 0) {
            return;
        }
        try {
            if (this.mDexoptWakeLock.isHeld()) {
                this.mDexoptWakeLock.release();
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime() - j;
            if (jElapsedRealtime >= WAKELOCK_TIMEOUT_MS) {
                Slog.wtf(TAG, "WakeLock " + this.mDexoptWakeLock.getTag() + " time out. Operation took " + jElapsedRealtime + " ms. Thread: " + Thread.currentThread().getName());
            }
        } catch (Exception e) {
            Slog.wtf(TAG, "Error while releasing " + this.mDexoptWakeLock.getTag() + " lock", e);
        }
    }

    @GuardedBy("mInstallLock")
    private int dexOptSecondaryDexPathLI(ApplicationInfo applicationInfo, String str, PackageDexUsage.DexUseInfo dexUseInfo, DexoptOptions dexoptOptions) {
        int i;
        String str2 = str;
        if (dexoptOptions.isDexoptOnlySharedDex() && !dexUseInfo.isUsedByOtherApps()) {
            return 0;
        }
        String realCompilerFilter = getRealCompilerFilter(applicationInfo, dexoptOptions.getCompilerFilter(), dexUseInfo.isUsedByOtherApps());
        int dexFlags = getDexFlags(applicationInfo, realCompilerFilter, dexoptOptions) | 32;
        if (applicationInfo.deviceProtectedDataDir != null && FileUtils.contains(applicationInfo.deviceProtectedDataDir, str2)) {
            i = dexFlags | 256;
        } else if (applicationInfo.credentialProtectedDataDir != null && FileUtils.contains(applicationInfo.credentialProtectedDataDir, str2)) {
            i = dexFlags | 128;
        } else {
            Slog.e(TAG, "Could not infer CE/DE storage for package " + applicationInfo.packageName);
            return -1;
        }
        int i2 = i;
        Log.d(TAG, "Running dexopt on: " + str2 + " pkg=" + applicationInfo.packageName + " isa=" + dexUseInfo.getLoaderIsas() + " dexoptFlags=" + printDexoptFlags(i2) + " target-filter=" + realCompilerFilter);
        int compilationReason = dexoptOptions.getCompilationReason();
        try {
            Iterator<String> it = dexUseInfo.getLoaderIsas().iterator();
            while (it.hasNext()) {
                Iterator<String> it2 = it;
                int i3 = compilationReason;
                int i4 = i2;
                String str3 = realCompilerFilter;
                this.mInstaller.dexopt(str2, applicationInfo.uid, applicationInfo.packageName, it.next(), 0, null, i2, realCompilerFilter, applicationInfo.volumeUuid, SKIP_SHARED_LIBRARY_CHECK, applicationInfo.seInfoUser, dexoptOptions.isDowngrade(), applicationInfo.targetSdkVersion, null, null, PackageManagerServiceCompilerMapping.getReasonName(compilationReason));
                str2 = str;
                realCompilerFilter = str3;
                i2 = i4;
                it = it2;
                compilationReason = i3;
            }
            return 1;
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, "Failed to dexopt", e);
            return -1;
        }
    }

    protected int adjustDexoptNeeded(int i) {
        return i;
    }

    protected int adjustDexoptFlags(int i) {
        return i;
    }

    void dumpDexoptState(IndentingPrintWriter indentingPrintWriter, PackageParser.Package r10, PackageDexUsage.PackageUseInfo packageUseInfo) {
        String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(InstructionSets.getAppDexInstructionSets(r10.applicationInfo));
        for (String str : r10.getAllCodePathsExcludingResourceOnly()) {
            indentingPrintWriter.println("path: " + str);
            indentingPrintWriter.increaseIndent();
            for (String str2 : dexCodeInstructionSets) {
                try {
                    DexFile.OptimizationInfo dexFileOptimizationInfo = DexFile.getDexFileOptimizationInfo(str, str2);
                    indentingPrintWriter.println(str2 + ": [status=" + dexFileOptimizationInfo.getStatus() + "] [reason=" + dexFileOptimizationInfo.getReason() + "]");
                } catch (IOException e) {
                    indentingPrintWriter.println(str2 + ": [Exception]: " + e.getMessage());
                }
            }
            if (packageUseInfo.isUsedByOtherApps(str)) {
                indentingPrintWriter.println("used by other apps: " + packageUseInfo.getLoadingPackages(str));
            }
            Map<String, PackageDexUsage.DexUseInfo> dexUseInfoMap = packageUseInfo.getDexUseInfoMap();
            if (!dexUseInfoMap.isEmpty()) {
                indentingPrintWriter.println("known secondary dex files:");
                indentingPrintWriter.increaseIndent();
                for (Map.Entry<String, PackageDexUsage.DexUseInfo> entry : dexUseInfoMap.entrySet()) {
                    String key = entry.getKey();
                    PackageDexUsage.DexUseInfo value = entry.getValue();
                    indentingPrintWriter.println(key);
                    indentingPrintWriter.increaseIndent();
                    indentingPrintWriter.println("class loader context: " + value.getClassLoaderContext());
                    if (value.isUsedByOtherApps()) {
                        indentingPrintWriter.println("used by other apps: " + value.getLoadingPackages());
                    }
                    indentingPrintWriter.decreaseIndent();
                }
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.decreaseIndent();
        }
    }

    private String getRealCompilerFilter(ApplicationInfo applicationInfo, String str, boolean z) {
        boolean z2 = (applicationInfo.flags & 16384) != 0;
        if (applicationInfo.isPrivilegedApp() && DexManager.isPackageSelectedToRunOob(applicationInfo.packageName)) {
            return "verify";
        }
        if (z2) {
            return DexFile.getSafeModeCompilerFilter(str);
        }
        if (DexFile.isProfileGuidedCompilerFilter(str) && z) {
            return PackageManagerServiceCompilerMapping.getCompilerFilterForReason(6);
        }
        return str;
    }

    private int getDexFlags(PackageParser.Package r1, String str, DexoptOptions dexoptOptions) {
        return getDexFlags(r1.applicationInfo, str, dexoptOptions);
    }

    private boolean isAppImageEnabled() {
        return SystemProperties.get("dalvik.vm.appimageformat", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).length() > 0;
    }

    private int getDexFlags(ApplicationInfo applicationInfo, String str, DexoptOptions dexoptOptions) {
        int i;
        boolean z;
        int i2 = 2;
        boolean z2 = (applicationInfo.flags & 2) != 0;
        boolean zIsProfileGuidedCompilerFilter = DexFile.isProfileGuidedCompilerFilter(str);
        boolean z3 = !applicationInfo.isForwardLocked() && (!zIsProfileGuidedCompilerFilter || dexoptOptions.isDexoptInstallWithDexMetadata());
        int i3 = zIsProfileGuidedCompilerFilter ? 16 : 0;
        if (applicationInfo.getHiddenApiEnforcementPolicy() != 0) {
            i = 1024;
        } else {
            i = 0;
        }
        switch (dexoptOptions.getCompilationReason()) {
            case 0:
            case 1:
            case 2:
                z = false;
                break;
            default:
                z = true;
                break;
        }
        boolean z4 = zIsProfileGuidedCompilerFilter && (applicationInfo.splitDependencies == null || !applicationInfo.requestsIsolatedSplitLoading()) && isAppImageEnabled();
        if (!z3) {
            i2 = 0;
        }
        return adjustDexoptFlags((z2 ? 4 : 0) | i2 | i3 | (dexoptOptions.isBootComplete() ? 8 : 0) | (dexoptOptions.isDexoptIdleBackgroundJob() ? 512 : 0) | (z ? 2048 : 0) | (z4 ? 4096 : 0) | i);
    }

    private int getDexoptNeeded(String str, String str2, String str3, String str4, boolean z, boolean z2) {
        try {
            return adjustDexoptNeeded(DexFile.getDexOptNeeded(str, str2, str3, str4, z, z2));
        } catch (IOException e) {
            Slog.w(TAG, "IOException reading apk: " + str, e);
            return -1;
        }
    }

    private boolean isProfileUpdated(PackageParser.Package r2, int i, String str, String str2) {
        if (!DexFile.isProfileGuidedCompilerFilter(str2)) {
            return false;
        }
        try {
            return this.mInstaller.mergeProfiles(i, r2.packageName, str);
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, "Failed to merge profiles", e);
            return false;
        }
    }

    private String createOatDirIfSupported(PackageParser.Package r4, String str) {
        if (!r4.canHaveOatDir()) {
            return null;
        }
        File file = new File(r4.codePath);
        if (!file.isDirectory()) {
            return null;
        }
        File oatDir = getOatDir(file);
        try {
            this.mInstaller.createOatDir(oatDir.getAbsolutePath(), str);
            return oatDir.getAbsolutePath();
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, "Failed to create oat dir", e);
            return null;
        }
    }

    static File getOatDir(File file) {
        return new File(file, OAT_DIR_NAME);
    }

    void systemReady() {
        this.mSystemReady = true;
    }

    private String printDexoptFlags(int i) {
        ArrayList arrayList = new ArrayList();
        if ((i & 8) == 8) {
            arrayList.add("boot_complete");
        }
        if ((i & 4) == 4) {
            arrayList.add("debuggable");
        }
        if ((i & 16) == 16) {
            arrayList.add("profile_guided");
        }
        if ((i & 2) == 2) {
            arrayList.add("public");
        }
        if ((i & 32) == 32) {
            arrayList.add("secondary");
        }
        if ((i & 64) == 64) {
            arrayList.add("force");
        }
        if ((i & 128) == 128) {
            arrayList.add("storage_ce");
        }
        if ((i & 256) == 256) {
            arrayList.add("storage_de");
        }
        if ((i & 512) == 512) {
            arrayList.add("idle_background_job");
        }
        if ((i & 1024) == 1024) {
            arrayList.add("enable_hidden_api_checks");
        }
        return String.join(",", arrayList);
    }

    public static class ForcedUpdatePackageDexOptimizer extends PackageDexOptimizer {
        public ForcedUpdatePackageDexOptimizer(Installer installer, Object obj, Context context, String str) {
            super(installer, obj, context, str);
        }

        public ForcedUpdatePackageDexOptimizer(PackageDexOptimizer packageDexOptimizer) {
            super(packageDexOptimizer);
        }

        @Override
        protected int adjustDexoptNeeded(int i) {
            if (i == 0) {
                return -3;
            }
            return i;
        }

        @Override
        protected int adjustDexoptFlags(int i) {
            return i | 64;
        }
    }
}
