package com.android.server.pm;

import android.content.Context;
import android.content.pm.PackageParser;
import android.os.Environment;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.InstallerConnection;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.pm.PackageManagerService;
import dalvik.system.DexFile;
import java.io.File;
import java.io.IOException;
import java.util.List;

class PackageDexOptimizer {
    static final int DEX_OPT_FAILED = -1;
    static final int DEX_OPT_PERFORMED = 1;
    static final int DEX_OPT_SKIPPED = 0;
    static final String OAT_DIR_NAME = "oat";
    private static final String TAG = "PackageManager.DexOptimizer";
    private final PowerManager.WakeLock mDexoptWakeLock;
    private final Object mInstallLock;
    private final Installer mInstaller;
    private volatile boolean mSystemReady;

    PackageDexOptimizer(Installer installer, Object installLock, Context context, String wakeLockTag) {
        this.mInstaller = installer;
        this.mInstallLock = installLock;
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mDexoptWakeLock = powerManager.newWakeLock(1, wakeLockTag);
    }

    protected PackageDexOptimizer(PackageDexOptimizer from) {
        this.mInstaller = from.mInstaller;
        this.mInstallLock = from.mInstallLock;
        this.mDexoptWakeLock = from.mDexoptWakeLock;
        this.mSystemReady = from.mSystemReady;
    }

    static boolean canOptimizePackage(PackageParser.Package pkg) {
        return (pkg.applicationInfo.flags & 4) != 0;
    }

    int performDexOpt(PackageParser.Package pkg, String[] sharedLibraries, String[] instructionSets, boolean checkProfiles, String targetCompilationFilter) {
        int iPerformDexOptLI;
        synchronized (this.mInstallLock) {
            boolean useLock = this.mSystemReady;
            if (useLock) {
                this.mDexoptWakeLock.setWorkSource(new WorkSource(pkg.applicationInfo.uid));
                this.mDexoptWakeLock.acquire();
            }
            try {
                iPerformDexOptLI = performDexOptLI(pkg, sharedLibraries, instructionSets, checkProfiles, targetCompilationFilter);
            } finally {
                if (useLock) {
                    this.mDexoptWakeLock.release();
                }
            }
        }
        return iPerformDexOptLI;
    }

    protected int adjustDexoptNeeded(int dexoptNeeded) {
        return dexoptNeeded;
    }

    protected int adjustDexoptFlags(int dexoptFlags) {
        return dexoptFlags;
    }

    void dumpDexoptState(IndentingPrintWriter pw, PackageParser.Package pkg) {
        String status;
        String[] instructionSets = InstructionSets.getAppDexInstructionSets(pkg.applicationInfo);
        String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(instructionSets);
        List<String> paths = pkg.getAllCodePathsExcludingResourceOnly();
        for (String instructionSet : dexCodeInstructionSets) {
            pw.println("Instruction Set: " + instructionSet);
            pw.increaseIndent();
            for (String path : paths) {
                try {
                    status = DexFile.getDexFileStatus(path, instructionSet);
                } catch (IOException ioe) {
                    status = "[Exception]: " + ioe.getMessage();
                }
                pw.println("path: " + path);
                pw.println("status: " + status);
            }
            pw.decreaseIndent();
        }
    }

    private int performDexOptLI(PackageParser.Package pkg, String[] sharedLibraries, String[] targetInstructionSets, boolean checkProfiles, String targetCompilerFilter) {
        String dexoptType;
        String sharedLibrariesPath;
        String[] instructionSets = targetInstructionSets != null ? targetInstructionSets : InstructionSets.getAppDexInstructionSets(pkg.applicationInfo);
        if (!canOptimizePackage(pkg)) {
            return 0;
        }
        List<String> paths = pkg.getAllCodePathsExcludingResourceOnly();
        int sharedGid = UserHandle.getSharedAppGid(pkg.applicationInfo.uid);
        boolean isProfileGuidedFilter = DexFile.isProfileGuidedCompilerFilter(targetCompilerFilter);
        if (isProfileGuidedFilter && isUsedByOtherApps(pkg)) {
            checkProfiles = false;
            targetCompilerFilter = PackageManagerServiceCompilerMapping.getNonProfileGuidedCompilerFilter(targetCompilerFilter);
            if (DexFile.isProfileGuidedCompilerFilter(targetCompilerFilter)) {
                throw new IllegalStateException(targetCompilerFilter);
            }
            isProfileGuidedFilter = false;
        }
        boolean newProfile = false;
        if (checkProfiles && isProfileGuidedFilter) {
            try {
                newProfile = this.mInstaller.mergeProfiles(sharedGid, pkg.packageName);
            } catch (InstallerConnection.InstallerException e) {
                Slog.w(TAG, "Failed to merge profiles", e);
            }
        }
        boolean vmSafeMode = (pkg.applicationInfo.flags & PackageManagerService.DumpState.DUMP_KEYSETS) != 0;
        boolean debuggable = (pkg.applicationInfo.flags & 2) != 0;
        boolean performedDexOpt = false;
        boolean successfulDexOpt = true;
        String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(instructionSets);
        int i = 0;
        int length = dexCodeInstructionSets.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                if (successfulDexOpt) {
                    return performedDexOpt ? 1 : 0;
                }
                return -1;
            }
            String dexCodeInstructionSet = dexCodeInstructionSets[i2];
            for (String path : paths) {
                try {
                    int dexoptNeeded = adjustDexoptNeeded(DexFile.getDexOptNeeded(path, dexCodeInstructionSet, targetCompilerFilter, newProfile));
                    if (PackageManagerService.DEBUG_DEXOPT) {
                        Log.i(TAG, "DexoptNeeded for " + path + "@" + targetCompilerFilter + " is " + dexoptNeeded);
                    }
                    String oatDir = null;
                    switch (dexoptNeeded) {
                        case 0:
                            break;
                        case 1:
                            dexoptType = "dex2oat";
                            oatDir = createOatDirIfSupported(pkg, dexCodeInstructionSet);
                            sharedLibrariesPath = null;
                            if (sharedLibraries != null && sharedLibraries.length != 0) {
                                StringBuilder sb = new StringBuilder();
                                for (String lib : sharedLibraries) {
                                    if (sb.length() != 0) {
                                        sb.append(":");
                                    }
                                    sb.append(lib);
                                }
                                sharedLibrariesPath = sb.toString();
                            }
                            Log.i(TAG, "Running dexopt (" + dexoptType + ") on: " + path + " pkg=" + pkg.applicationInfo.packageName + " isa=" + dexCodeInstructionSet + " vmSafeMode=" + vmSafeMode + " debuggable=" + debuggable + " target-filter=" + targetCompilerFilter + " oatDir = " + oatDir + " sharedLibraries=" + sharedLibrariesPath);
                            boolean isPublic = (pkg.isForwardLocked() || isProfileGuidedFilter) ? false : true;
                            int profileFlag = !isProfileGuidedFilter ? 32 : 0;
                            int dexFlags = adjustDexoptFlags((!debuggable ? 8 : 0) | (!isPublic ? 2 : 0) | (!vmSafeMode ? 4 : 0) | profileFlag | 16);
                            try {
                                this.mInstaller.dexopt(path, sharedGid, pkg.packageName, dexCodeInstructionSet, dexoptNeeded, oatDir, dexFlags, targetCompilerFilter, pkg.volumeUuid, sharedLibrariesPath);
                                performedDexOpt = true;
                            } catch (InstallerConnection.InstallerException e2) {
                                Slog.w(TAG, "Failed to dexopt", e2);
                                successfulDexOpt = false;
                            }
                            break;
                        case 2:
                            dexoptType = "patchoat";
                            sharedLibrariesPath = null;
                            if (sharedLibraries != null) {
                                StringBuilder sb2 = new StringBuilder();
                                while (i < r5) {
                                }
                                sharedLibrariesPath = sb2.toString();
                            }
                            Log.i(TAG, "Running dexopt (" + dexoptType + ") on: " + path + " pkg=" + pkg.applicationInfo.packageName + " isa=" + dexCodeInstructionSet + " vmSafeMode=" + vmSafeMode + " debuggable=" + debuggable + " target-filter=" + targetCompilerFilter + " oatDir = " + oatDir + " sharedLibraries=" + sharedLibrariesPath);
                            if (pkg.isForwardLocked()) {
                                if (!isProfileGuidedFilter) {
                                }
                                int dexFlags2 = adjustDexoptFlags((!debuggable ? 8 : 0) | (!isPublic ? 2 : 0) | (!vmSafeMode ? 4 : 0) | profileFlag | 16);
                                this.mInstaller.dexopt(path, sharedGid, pkg.packageName, dexCodeInstructionSet, dexoptNeeded, oatDir, dexFlags2, targetCompilerFilter, pkg.volumeUuid, sharedLibrariesPath);
                                performedDexOpt = true;
                            }
                            break;
                        case 3:
                            dexoptType = "self patchoat";
                            sharedLibrariesPath = null;
                            if (sharedLibraries != null) {
                            }
                            Log.i(TAG, "Running dexopt (" + dexoptType + ") on: " + path + " pkg=" + pkg.applicationInfo.packageName + " isa=" + dexCodeInstructionSet + " vmSafeMode=" + vmSafeMode + " debuggable=" + debuggable + " target-filter=" + targetCompilerFilter + " oatDir = " + oatDir + " sharedLibraries=" + sharedLibrariesPath);
                            if (pkg.isForwardLocked()) {
                            }
                            break;
                        default:
                            throw new IllegalStateException("Invalid dexopt:" + dexoptNeeded);
                    }
                } catch (IOException ioe) {
                    Slog.w(TAG, "IOException reading apk: " + path, ioe);
                    return -1;
                }
            }
            i = i2 + 1;
        }
    }

    private String createOatDirIfSupported(PackageParser.Package pkg, String dexInstructionSet) {
        if (!pkg.canHaveOatDir()) {
            return null;
        }
        File codePath = new File(pkg.codePath);
        if (!codePath.isDirectory()) {
            return null;
        }
        File oatDir = getOatDir(codePath);
        try {
            this.mInstaller.createOatDir(oatDir.getAbsolutePath(), dexInstructionSet);
            return oatDir.getAbsolutePath();
        } catch (InstallerConnection.InstallerException e) {
            Slog.w(TAG, "Failed to create oat dir", e);
            return null;
        }
    }

    static File getOatDir(File codePath) {
        return new File(codePath, OAT_DIR_NAME);
    }

    void systemReady() {
        this.mSystemReady = true;
    }

    public static boolean isUsedByOtherApps(PackageParser.Package pkg) {
        if (pkg.isForwardLocked()) {
            return false;
        }
        for (String apkPath : pkg.getAllCodePathsExcludingResourceOnly()) {
            try {
                String useMarker = PackageManagerServiceUtils.realpath(new File(apkPath)).replace('/', '@');
                int[] currentUserIds = UserManagerService.getInstance().getUserIds();
                for (int i : currentUserIds) {
                    File profileDir = Environment.getDataProfilesDeForeignDexDirectory(i);
                    File foreignUseMark = new File(profileDir, useMarker);
                    if (foreignUseMark.exists()) {
                        return true;
                    }
                }
            } catch (IOException e) {
                Slog.w(TAG, "Failed to get canonical path", e);
            }
        }
        return false;
    }

    public static class ForcedUpdatePackageDexOptimizer extends PackageDexOptimizer {
        public ForcedUpdatePackageDexOptimizer(Installer installer, Object installLock, Context context, String wakeLockTag) {
            super(installer, installLock, context, wakeLockTag);
        }

        public ForcedUpdatePackageDexOptimizer(PackageDexOptimizer from) {
            super(from);
        }

        @Override
        protected int adjustDexoptNeeded(int dexoptNeeded) {
            return 1;
        }
    }
}
