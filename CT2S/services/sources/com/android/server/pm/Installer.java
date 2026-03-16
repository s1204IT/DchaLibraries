package com.android.server.pm;

import android.content.Context;
import android.content.pm.PackageStats;
import android.os.Build;
import android.util.Slog;
import com.android.internal.os.InstallerConnection;
import com.android.server.SystemService;
import dalvik.system.VMRuntime;

public final class Installer extends SystemService {
    private static final String TAG = "Installer";
    private final InstallerConnection mInstaller;

    public Installer(Context context) {
        super(context);
        this.mInstaller = new InstallerConnection();
    }

    @Override
    public void onStart() {
        Slog.i(TAG, "Waiting for installd to be ready.");
        ping();
    }

    public int install(String name, int uid, int gid, String seinfo) {
        StringBuilder builder = new StringBuilder("install");
        builder.append(' ');
        builder.append(name);
        builder.append(' ');
        builder.append(uid);
        builder.append(' ');
        builder.append(gid);
        builder.append(' ');
        if (seinfo == null) {
            seinfo = "!";
        }
        builder.append(seinfo);
        return this.mInstaller.execute(builder.toString());
    }

    public int patchoat(String apkPath, int uid, boolean isPublic, String pkgName, String instructionSet) {
        if (isValidInstructionSet(instructionSet)) {
            return this.mInstaller.patchoat(apkPath, uid, isPublic, pkgName, instructionSet);
        }
        Slog.e(TAG, "Invalid instruction set: " + instructionSet);
        return -1;
    }

    public int patchoat(String apkPath, int uid, boolean isPublic, String instructionSet) {
        if (isValidInstructionSet(instructionSet)) {
            return this.mInstaller.patchoat(apkPath, uid, isPublic, instructionSet);
        }
        Slog.e(TAG, "Invalid instruction set: " + instructionSet);
        return -1;
    }

    public int dexopt(String apkPath, int uid, boolean isPublic, String instructionSet) {
        if (isValidInstructionSet(instructionSet)) {
            return this.mInstaller.dexopt(apkPath, uid, isPublic, instructionSet);
        }
        Slog.e(TAG, "Invalid instruction set: " + instructionSet);
        return -1;
    }

    public int dexopt(String apkPath, int uid, boolean isPublic, String pkgName, String instructionSet, boolean vmSafeMode) {
        if (isValidInstructionSet(instructionSet)) {
            return this.mInstaller.dexopt(apkPath, uid, isPublic, pkgName, instructionSet, vmSafeMode);
        }
        Slog.e(TAG, "Invalid instruction set: " + instructionSet);
        return -1;
    }

    public int idmap(String targetApkPath, String overlayApkPath, int uid) {
        return this.mInstaller.execute("idmap " + targetApkPath + ' ' + overlayApkPath + ' ' + uid);
    }

    public int movedex(String srcPath, String dstPath, String instructionSet) {
        if (!isValidInstructionSet(instructionSet)) {
            Slog.e(TAG, "Invalid instruction set: " + instructionSet);
            return -1;
        }
        return this.mInstaller.execute("movedex " + srcPath + ' ' + dstPath + ' ' + instructionSet);
    }

    public int rmdex(String codePath, String instructionSet) {
        if (!isValidInstructionSet(instructionSet)) {
            Slog.e(TAG, "Invalid instruction set: " + instructionSet);
            return -1;
        }
        return this.mInstaller.execute("rmdex " + codePath + ' ' + instructionSet);
    }

    public int remove(String name, int userId) {
        return this.mInstaller.execute("remove " + name + ' ' + userId);
    }

    public int rename(String oldname, String newname) {
        return this.mInstaller.execute("rename " + oldname + ' ' + newname);
    }

    public int fixUid(String name, int uid, int gid) {
        return this.mInstaller.execute("fixuid " + name + ' ' + uid + ' ' + gid);
    }

    public int deleteCacheFiles(String name, int userId) {
        return this.mInstaller.execute("rmcache " + name + ' ' + userId);
    }

    public int deleteCodeCacheFiles(String name, int userId) {
        return this.mInstaller.execute("rmcodecache " + name + ' ' + userId);
    }

    public int createUserData(String name, int uid, int userId, String seinfo) {
        StringBuilder builder = new StringBuilder("mkuserdata");
        builder.append(' ');
        builder.append(name);
        builder.append(' ');
        builder.append(uid);
        builder.append(' ');
        builder.append(userId);
        builder.append(' ');
        if (seinfo == null) {
            seinfo = "!";
        }
        builder.append(seinfo);
        return this.mInstaller.execute(builder.toString());
    }

    public int createUserConfig(int userId) {
        return this.mInstaller.execute("mkuserconfig " + userId);
    }

    public int removeUserDataDirs(int userId) {
        return this.mInstaller.execute("rmuser " + userId);
    }

    public int clearUserData(String name, int userId) {
        return this.mInstaller.execute("rmuserdata " + name + ' ' + userId);
    }

    public int markBootComplete(String instructionSet) {
        if (!isValidInstructionSet(instructionSet)) {
            Slog.e(TAG, "Invalid instruction set: " + instructionSet);
            return -1;
        }
        return this.mInstaller.execute("markbootcomplete " + instructionSet);
    }

    public boolean ping() {
        return this.mInstaller.execute("ping") >= 0;
    }

    public int freeCache(long freeStorageSize) {
        return this.mInstaller.execute("freecache " + String.valueOf(freeStorageSize));
    }

    public int getSizeInfo(String pkgName, int persona, String apkPath, String libDirPath, String fwdLockApkPath, String asecPath, String[] instructionSets, PackageStats pStats) {
        for (String instructionSet : instructionSets) {
            if (!isValidInstructionSet(instructionSet)) {
                Slog.e(TAG, "Invalid instruction set: " + instructionSet);
                return -1;
            }
        }
        StringBuilder builder = new StringBuilder("getsize");
        builder.append(' ');
        builder.append(pkgName);
        builder.append(' ');
        builder.append(persona);
        builder.append(' ');
        builder.append(apkPath);
        builder.append(' ');
        if (libDirPath == null) {
            libDirPath = "!";
        }
        builder.append(libDirPath);
        builder.append(' ');
        if (fwdLockApkPath == null) {
            fwdLockApkPath = "!";
        }
        builder.append(fwdLockApkPath);
        builder.append(' ');
        if (asecPath == null) {
            asecPath = "!";
        }
        builder.append(asecPath);
        builder.append(' ');
        builder.append(instructionSets[0]);
        String s = this.mInstaller.transact(builder.toString());
        String[] res = s.split(" ");
        if (res == null || res.length != 5) {
            return -1;
        }
        try {
            pStats.codeSize = Long.parseLong(res[1]);
            pStats.dataSize = Long.parseLong(res[2]);
            pStats.cacheSize = Long.parseLong(res[3]);
            pStats.externalCodeSize = Long.parseLong(res[4]);
            return Integer.parseInt(res[0]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public int moveFiles() {
        return this.mInstaller.execute("movefiles");
    }

    public int linkNativeLibraryDirectory(String dataPath, String nativeLibPath32, int userId) {
        if (dataPath == null) {
            Slog.e(TAG, "linkNativeLibraryDirectory dataPath is null");
            return -1;
        }
        if (nativeLibPath32 == null) {
            Slog.e(TAG, "linkNativeLibraryDirectory nativeLibPath is null");
            return -1;
        }
        return this.mInstaller.execute("linklib " + dataPath + ' ' + nativeLibPath32 + ' ' + userId);
    }

    public boolean restoreconData(String pkgName, String seinfo, int uid) {
        StringBuilder builder = new StringBuilder("restorecondata");
        builder.append(' ');
        builder.append(pkgName);
        builder.append(' ');
        if (seinfo == null) {
            seinfo = "!";
        }
        builder.append(seinfo);
        builder.append(' ');
        builder.append(uid);
        return this.mInstaller.execute(builder.toString()) == 0;
    }

    private static boolean isValidInstructionSet(String instructionSet) {
        if (instructionSet == null) {
            return false;
        }
        String[] arr$ = Build.SUPPORTED_ABIS;
        for (String abi : arr$) {
            if (instructionSet.equals(VMRuntime.getInstructionSet(abi))) {
                return true;
            }
        }
        return false;
    }
}
