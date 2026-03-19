package com.android.internal.content;

import android.content.pm.PackageParser;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.SELinux;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Slog;
import dalvik.system.CloseGuard;
import dalvik.system.VMRuntime;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

public class NativeLibraryHelper {
    private static final int BITCODE_PRESENT = 1;
    public static final String CLEAR_ABI_OVERRIDE = "-";
    private static final boolean DEBUG_NATIVE = false;
    private static final boolean HAS_NATIVE_BRIDGE = !WifiEnterpriseConfig.ENGINE_DISABLE.equals(SystemProperties.get("ro.dalvik.vm.native.bridge", WifiEnterpriseConfig.ENGINE_DISABLE));
    public static final String LIB64_DIR_NAME = "lib64";
    public static final String LIB_DIR_NAME = "lib";
    private static final String TAG = "NativeHelper";

    private static native int hasRenderscriptBitcode(long j);

    private static native void nativeClose(long j);

    private static native int nativeCopyNativeBinaries(long j, String str, String str2, boolean z, boolean z2, boolean z3);

    private static native int nativeFindSupportedAbi(long j, String[] strArr, boolean z);

    private static native long nativeOpenApk(String str);

    private static native long nativeOpenApkFd(FileDescriptor fileDescriptor, String str);

    private static native long nativeSumNativeBinaries(long j, String str, boolean z);

    public static class Handle implements Closeable {
        final long[] apkHandles;
        final boolean debuggable;
        final boolean extractNativeLibs;
        private volatile boolean mClosed;
        private final CloseGuard mGuard = CloseGuard.get();
        final boolean multiArch;

        public static Handle create(File file) throws IOException {
            try {
                return create(PackageParser.parsePackageLite(file, 0));
            } catch (PackageParser.PackageParserException e) {
                throw new IOException("Failed to parse package: " + file, e);
            }
        }

        public static Handle create(PackageParser.Package r6) throws IOException {
            return create(r6.getAllCodePaths(), (r6.applicationInfo.flags & Integer.MIN_VALUE) != 0, (r6.applicationInfo.flags & 268435456) != 0, (r6.applicationInfo.flags & 2) != 0);
        }

        public static Handle create(PackageParser.PackageLite packageLite) throws IOException {
            return create(packageLite.getAllCodePaths(), packageLite.multiArch, packageLite.extractNativeLibs, packageLite.debuggable);
        }

        private static Handle create(List<String> list, boolean z, boolean z2, boolean z3) throws IOException {
            int size = list.size();
            long[] jArr = new long[size];
            for (int i = 0; i < size; i++) {
                String str = list.get(i);
                jArr[i] = NativeLibraryHelper.nativeOpenApk(str);
                if (jArr[i] == 0) {
                    for (int i2 = 0; i2 < i; i2++) {
                        NativeLibraryHelper.nativeClose(jArr[i2]);
                    }
                    throw new IOException("Unable to open APK: " + str);
                }
            }
            return new Handle(jArr, z, z2, z3);
        }

        public static Handle createFd(PackageParser.PackageLite packageLite, FileDescriptor fileDescriptor) throws IOException {
            String str = packageLite.baseCodePath;
            long[] jArr = {NativeLibraryHelper.nativeOpenApkFd(fileDescriptor, str)};
            if (jArr[0] == 0) {
                throw new IOException("Unable to open APK " + str + " from fd " + fileDescriptor);
            }
            return new Handle(jArr, packageLite.multiArch, packageLite.extractNativeLibs, packageLite.debuggable);
        }

        Handle(long[] jArr, boolean z, boolean z2, boolean z3) {
            this.apkHandles = jArr;
            this.multiArch = z;
            this.extractNativeLibs = z2;
            this.debuggable = z3;
            this.mGuard.open("close");
        }

        @Override
        public void close() {
            for (long j : this.apkHandles) {
                NativeLibraryHelper.nativeClose(j);
            }
            this.mGuard.close();
            this.mClosed = true;
        }

        protected void finalize() throws Throwable {
            if (this.mGuard != null) {
                this.mGuard.warnIfOpen();
            }
            try {
                if (!this.mClosed) {
                    close();
                }
            } finally {
                super.finalize();
            }
        }
    }

    private static long sumNativeBinaries(Handle handle, String str) {
        long jNativeSumNativeBinaries = 0;
        for (long j : handle.apkHandles) {
            jNativeSumNativeBinaries += nativeSumNativeBinaries(j, str, handle.debuggable);
        }
        return jNativeSumNativeBinaries;
    }

    public static int copyNativeBinaries(Handle handle, File file, String str) {
        for (long j : handle.apkHandles) {
            int iNativeCopyNativeBinaries = nativeCopyNativeBinaries(j, file.getPath(), str, handle.extractNativeLibs, HAS_NATIVE_BRIDGE, handle.debuggable);
            if (iNativeCopyNativeBinaries != 1) {
                return iNativeCopyNativeBinaries;
            }
        }
        return 1;
    }

    public static int findSupportedAbi(Handle handle, String[] strArr) {
        int i = -114;
        for (long j : handle.apkHandles) {
            int iNativeFindSupportedAbi = nativeFindSupportedAbi(j, strArr, handle.debuggable);
            if (iNativeFindSupportedAbi != -114) {
                if (iNativeFindSupportedAbi == -113) {
                    if (i < 0) {
                        i = -113;
                    }
                } else if (iNativeFindSupportedAbi >= 0) {
                    if (i < 0 || iNativeFindSupportedAbi < i) {
                        i = iNativeFindSupportedAbi;
                    }
                } else {
                    return iNativeFindSupportedAbi;
                }
            }
        }
        return i;
    }

    public static void removeNativeBinariesLI(String str) {
        if (str == null) {
            return;
        }
        removeNativeBinariesFromDirLI(new File(str), false);
    }

    public static void removeNativeBinariesFromDirLI(File file, boolean z) {
        if (file.exists()) {
            File[] fileArrListFiles = file.listFiles();
            if (fileArrListFiles != null) {
                for (int i = 0; i < fileArrListFiles.length; i++) {
                    if (fileArrListFiles[i].isDirectory()) {
                        removeNativeBinariesFromDirLI(fileArrListFiles[i], true);
                    } else if (!fileArrListFiles[i].delete()) {
                        Slog.w(TAG, "Could not delete native binary: " + fileArrListFiles[i].getPath());
                    }
                }
            }
            if (z && !file.delete()) {
                Slog.w(TAG, "Could not delete native binary directory: " + file.getPath());
            }
        }
    }

    public static void createNativeLibrarySubdir(File file) throws IOException {
        if (!file.isDirectory()) {
            file.delete();
            if (!file.mkdir()) {
                throw new IOException("Cannot create " + file.getPath());
            }
            try {
                Os.chmod(file.getPath(), OsConstants.S_IRWXU | OsConstants.S_IRGRP | OsConstants.S_IXGRP | OsConstants.S_IROTH | OsConstants.S_IXOTH);
                return;
            } catch (ErrnoException e) {
                throw new IOException("Cannot chmod native library directory " + file.getPath(), e);
            }
        }
        if (!SELinux.restorecon(file)) {
            throw new IOException("Cannot set SELinux context for " + file.getPath());
        }
    }

    private static long sumNativeBinariesForSupportedAbi(Handle handle, String[] strArr) {
        int iFindSupportedAbi = findSupportedAbi(handle, strArr);
        if (iFindSupportedAbi >= 0) {
            return sumNativeBinaries(handle, strArr[iFindSupportedAbi]);
        }
        return 0L;
    }

    public static int copyNativeBinariesForSupportedAbi(Handle handle, File file, String[] strArr, boolean z) throws IOException {
        createNativeLibrarySubdir(file);
        int iFindSupportedAbi = findSupportedAbi(handle, strArr);
        if (iFindSupportedAbi >= 0) {
            String instructionSet = VMRuntime.getInstructionSet(strArr[iFindSupportedAbi]);
            if (z) {
                File file2 = new File(file, instructionSet);
                createNativeLibrarySubdir(file2);
                file = file2;
            }
            int iCopyNativeBinaries = copyNativeBinaries(handle, file, strArr[iFindSupportedAbi]);
            if (iCopyNativeBinaries != 1) {
                return iCopyNativeBinaries;
            }
        }
        return iFindSupportedAbi;
    }

    public static int copyNativeBinariesWithOverride(Handle handle, File file, String str) {
        int iCopyNativeBinariesForSupportedAbi;
        int iCopyNativeBinariesForSupportedAbi2;
        try {
            if (handle.multiArch) {
                if (str != null && !CLEAR_ABI_OVERRIDE.equals(str)) {
                    Slog.w(TAG, "Ignoring abiOverride for multi arch application.");
                }
                if (Build.SUPPORTED_32_BIT_ABIS.length > 0 && (iCopyNativeBinariesForSupportedAbi2 = copyNativeBinariesForSupportedAbi(handle, file, Build.SUPPORTED_32_BIT_ABIS, true)) < 0 && iCopyNativeBinariesForSupportedAbi2 != -114 && iCopyNativeBinariesForSupportedAbi2 != -113) {
                    Slog.w(TAG, "Failure copying 32 bit native libraries; copyRet=" + iCopyNativeBinariesForSupportedAbi2);
                    return iCopyNativeBinariesForSupportedAbi2;
                }
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && (iCopyNativeBinariesForSupportedAbi = copyNativeBinariesForSupportedAbi(handle, file, Build.SUPPORTED_64_BIT_ABIS, true)) < 0 && iCopyNativeBinariesForSupportedAbi != -114 && iCopyNativeBinariesForSupportedAbi != -113) {
                    Slog.w(TAG, "Failure copying 64 bit native libraries; copyRet=" + iCopyNativeBinariesForSupportedAbi);
                    return iCopyNativeBinariesForSupportedAbi;
                }
            } else {
                String str2 = null;
                if (!CLEAR_ABI_OVERRIDE.equals(str) && str != null) {
                    str2 = str;
                }
                String[] strArr = str2 != null ? new String[]{str2} : Build.SUPPORTED_ABIS;
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && str2 == null && hasRenderscriptBitcode(handle)) {
                    strArr = Build.SUPPORTED_32_BIT_ABIS;
                }
                int iCopyNativeBinariesForSupportedAbi3 = copyNativeBinariesForSupportedAbi(handle, file, strArr, true);
                if (iCopyNativeBinariesForSupportedAbi3 < 0 && iCopyNativeBinariesForSupportedAbi3 != -114) {
                    Slog.w(TAG, "Failure copying native libraries [errorCode=" + iCopyNativeBinariesForSupportedAbi3 + "]");
                    return iCopyNativeBinariesForSupportedAbi3;
                }
            }
            return 1;
        } catch (IOException e) {
            Slog.e(TAG, "Copying native libraries failed", e);
            return -110;
        }
    }

    public static long sumNativeBinariesWithOverride(Handle handle, String str) throws IOException {
        if (handle.multiArch) {
            if (str != null && !CLEAR_ABI_OVERRIDE.equals(str)) {
                Slog.w(TAG, "Ignoring abiOverride for multi arch application.");
            }
            long jSumNativeBinariesForSupportedAbi = Build.SUPPORTED_32_BIT_ABIS.length > 0 ? 0 + sumNativeBinariesForSupportedAbi(handle, Build.SUPPORTED_32_BIT_ABIS) : 0L;
            if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                return jSumNativeBinariesForSupportedAbi + sumNativeBinariesForSupportedAbi(handle, Build.SUPPORTED_64_BIT_ABIS);
            }
            return jSumNativeBinariesForSupportedAbi;
        }
        String str2 = null;
        if (!CLEAR_ABI_OVERRIDE.equals(str) && str != null) {
            str2 = str;
        }
        String[] strArr = str2 != null ? new String[]{str2} : Build.SUPPORTED_ABIS;
        if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && str2 == null && hasRenderscriptBitcode(handle)) {
            strArr = Build.SUPPORTED_32_BIT_ABIS;
        }
        return 0 + sumNativeBinariesForSupportedAbi(handle, strArr);
    }

    public static boolean hasRenderscriptBitcode(Handle handle) throws IOException {
        for (long j : handle.apkHandles) {
            int iHasRenderscriptBitcode = hasRenderscriptBitcode(j);
            if (iHasRenderscriptBitcode < 0) {
                throw new IOException("Error scanning APK, code: " + iHasRenderscriptBitcode);
            }
            if (iHasRenderscriptBitcode == 1) {
                return true;
            }
        }
        return false;
    }
}
