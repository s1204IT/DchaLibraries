package com.android.server.pm;

import android.content.Context;
import android.content.pm.PackageStats;
import android.os.Build;
import android.os.IBinder;
import android.os.IInstalld;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.SystemService;
import dalvik.system.VMRuntime;
import java.io.FileDescriptor;

public class Installer extends SystemService {
    public static final int DEXOPT_BOOTCOMPLETE = 8;
    public static final int DEXOPT_DEBUGGABLE = 4;
    public static final int DEXOPT_ENABLE_HIDDEN_API_CHECKS = 1024;
    public static final int DEXOPT_FORCE = 64;
    public static final int DEXOPT_GENERATE_APP_IMAGE = 4096;
    public static final int DEXOPT_GENERATE_COMPACT_DEX = 2048;
    public static final int DEXOPT_IDLE_BACKGROUND_JOB = 512;
    public static final int DEXOPT_PROFILE_GUIDED = 16;
    public static final int DEXOPT_PUBLIC = 2;
    public static final int DEXOPT_SECONDARY_DEX = 32;
    public static final int DEXOPT_STORAGE_CE = 128;
    public static final int DEXOPT_STORAGE_DE = 256;
    public static final int FLAG_CLEAR_CACHE_ONLY = 256;
    public static final int FLAG_CLEAR_CODE_CACHE_ONLY = 512;
    public static final int FLAG_FORCE = 65536;
    public static final int FLAG_FREE_CACHE_NOOP = 32768;
    public static final int FLAG_FREE_CACHE_V2 = 8192;
    public static final int FLAG_FREE_CACHE_V2_DEFY_QUOTA = 16384;
    public static final int FLAG_USE_QUOTA = 4096;
    private static final String TAG = "Installer";
    private volatile IInstalld mInstalld;
    private final boolean mIsolated;
    private volatile Object mWarnIfHeld;

    public Installer(Context context) {
        this(context, false);
    }

    public Installer(Context context, boolean z) {
        super(context);
        this.mIsolated = z;
    }

    public void setWarnIfHeld(Object obj) {
        this.mWarnIfHeld = obj;
    }

    @Override
    public void onStart() {
        if (this.mIsolated) {
            this.mInstalld = null;
        } else {
            connect();
        }
    }

    private void connect() {
        IBinder service = ServiceManager.getService("installd");
        if (service != null) {
            try {
                service.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Slog.w(Installer.TAG, "installd died; reconnecting");
                        Installer.this.connect();
                    }
                }, 0);
            } catch (RemoteException e) {
                service = null;
            }
        }
        if (service != null) {
            this.mInstalld = IInstalld.Stub.asInterface(service);
            try {
                invalidateMounts();
            } catch (InstallerException e2) {
            }
        } else {
            Slog.w(TAG, "installd not found; trying again");
            BackgroundThread.getHandler().postDelayed(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.connect();
                }
            }, 1000L);
        }
    }

    private boolean checkBeforeRemote() {
        if (this.mWarnIfHeld != null && Thread.holdsLock(this.mWarnIfHeld)) {
            Slog.wtf(TAG, "Calling thread " + Thread.currentThread().getName() + " is holding 0x" + Integer.toHexString(System.identityHashCode(this.mWarnIfHeld)), new Throwable());
        }
        if (this.mIsolated) {
            Slog.i(TAG, "Ignoring request because this installer is isolated");
            return false;
        }
        return true;
    }

    public long createAppData(String str, String str2, int i, int i2, int i3, String str3, int i4) throws InstallerException {
        if (!checkBeforeRemote()) {
            return -1L;
        }
        try {
            return this.mInstalld.createAppData(str, str2, i, i2, i3, str3, i4);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public void restoreconAppData(String str, String str2, int i, int i2, int i3, String str3) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.restoreconAppData(str, str2, i, i2, i3, str3);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void migrateAppData(String str, String str2, int i, int i2) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.migrateAppData(str, str2, i, i2);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void clearAppData(String str, String str2, int i, int i2, long j) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.clearAppData(str, str2, i, i2, j);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void destroyAppData(String str, String str2, int i, int i2, long j) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.destroyAppData(str, str2, i, i2, j);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void fixupAppData(String str, int i) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.fixupAppData(str, i);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void moveCompleteApp(String str, String str2, String str3, String str4, int i, String str5, int i2) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.moveCompleteApp(str, str2, str3, str4, i, str5, i2);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void getAppSize(String str, String[] strArr, int i, int i2, int i3, long[] jArr, String[] strArr2, PackageStats packageStats) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                long[] appSize = this.mInstalld.getAppSize(str, strArr, i, i2, i3, jArr, strArr2);
                packageStats.codeSize += appSize[0];
                packageStats.dataSize += appSize[1];
                packageStats.cacheSize += appSize[2];
                packageStats.externalCodeSize += appSize[3];
                packageStats.externalDataSize += appSize[4];
                packageStats.externalCacheSize += appSize[5];
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void getUserSize(String str, int i, int i2, int[] iArr, PackageStats packageStats) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                long[] userSize = this.mInstalld.getUserSize(str, i, i2, iArr);
                packageStats.codeSize += userSize[0];
                packageStats.dataSize += userSize[1];
                packageStats.cacheSize += userSize[2];
                packageStats.externalCodeSize += userSize[3];
                packageStats.externalDataSize += userSize[4];
                packageStats.externalCacheSize += userSize[5];
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public long[] getExternalSize(String str, int i, int i2, int[] iArr) throws InstallerException {
        if (!checkBeforeRemote()) {
            return new long[6];
        }
        try {
            return this.mInstalld.getExternalSize(str, i, i2, iArr);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public void setAppQuota(String str, int i, int i2, long j) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.setAppQuota(str, i, i2, j);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void dexopt(String str, int i, String str2, String str3, int i2, String str4, int i3, String str5, String str6, String str7, String str8, boolean z, int i4, String str9, String str10, String str11) throws InstallerException {
        assertValidInstructionSet(str3);
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.dexopt(str, i, str2, str3, i2, str4, i3, str5, str6, str7, str8, z, i4, str9, str10, str11);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public boolean mergeProfiles(int i, String str, String str2) throws InstallerException {
        if (!checkBeforeRemote()) {
            return false;
        }
        try {
            return this.mInstalld.mergeProfiles(i, str, str2);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public boolean dumpProfiles(int i, String str, String str2, String str3) throws InstallerException {
        if (!checkBeforeRemote()) {
            return false;
        }
        try {
            return this.mInstalld.dumpProfiles(i, str, str2, str3);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public boolean copySystemProfile(String str, int i, String str2, String str3) throws InstallerException {
        if (!checkBeforeRemote()) {
            return false;
        }
        try {
            return this.mInstalld.copySystemProfile(str, i, str2, str3);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public void idmap(String str, String str2, int i) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.idmap(str, str2, i);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void removeIdmap(String str) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.removeIdmap(str);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void rmdex(String str, String str2) throws InstallerException {
        assertValidInstructionSet(str2);
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.rmdex(str, str2);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void rmPackageDir(String str) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.rmPackageDir(str);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void clearAppProfiles(String str, String str2) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.clearAppProfiles(str, str2);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void destroyAppProfiles(String str) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.destroyAppProfiles(str);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void createUserData(String str, int i, int i2, int i3) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.createUserData(str, i, i2, i3);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void destroyUserData(String str, int i, int i2) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.destroyUserData(str, i, i2);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void markBootComplete(String str) throws InstallerException {
        assertValidInstructionSet(str);
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.markBootComplete(str);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void freeCache(String str, long j, long j2, int i) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.freeCache(str, j, j2, i);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void linkNativeLibraryDirectory(String str, String str2, String str3, int i) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.linkNativeLibraryDirectory(str, str2, str3, i);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void createOatDir(String str, String str2) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.createOatDir(str, str2);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void linkFile(String str, String str2, String str3) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.linkFile(str, str2, str3);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void moveAb(String str, String str2, String str3) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.moveAb(str, str2, str3);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void deleteOdex(String str, String str2, String str3) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.deleteOdex(str, str2, str3);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void installApkVerity(String str, FileDescriptor fileDescriptor, int i) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.installApkVerity(str, fileDescriptor, i);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void assertFsverityRootHashMatches(String str, byte[] bArr) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.assertFsverityRootHashMatches(str, bArr);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public boolean reconcileSecondaryDexFile(String str, String str2, int i, String[] strArr, String str3, int i2) throws InstallerException {
        for (String str4 : strArr) {
            assertValidInstructionSet(str4);
        }
        if (!checkBeforeRemote()) {
            return false;
        }
        try {
            return this.mInstalld.reconcileSecondaryDexFile(str, str2, i, strArr, str3, i2);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public byte[] hashSecondaryDexFile(String str, String str2, int i, String str3, int i2) throws InstallerException {
        if (!checkBeforeRemote()) {
            return new byte[0];
        }
        try {
            return this.mInstalld.hashSecondaryDexFile(str, str2, i, str3, i2);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public boolean createProfileSnapshot(int i, String str, String str2, String str3) throws InstallerException {
        if (!checkBeforeRemote()) {
            return false;
        }
        try {
            return this.mInstalld.createProfileSnapshot(i, str, str2, str3);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public void destroyProfileSnapshot(String str, String str2) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.destroyProfileSnapshot(str, str2);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void invalidateMounts() throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.invalidateMounts();
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public boolean isQuotaSupported(String str) throws InstallerException {
        if (!checkBeforeRemote()) {
            return false;
        }
        try {
            return this.mInstalld.isQuotaSupported(str);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public boolean prepareAppProfile(String str, int i, int i2, String str2, String str3, String str4) throws InstallerException {
        if (!checkBeforeRemote()) {
            return false;
        }
        try {
            return this.mInstalld.prepareAppProfile(str, i, i2, str2, str3, str4);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    private static void assertValidInstructionSet(String str) throws InstallerException {
        for (String str2 : Build.SUPPORTED_ABIS) {
            if (VMRuntime.getInstructionSet(str2).equals(str)) {
                return;
            }
        }
        throw new InstallerException("Invalid instruction set: " + str);
    }

    public static class InstallerException extends Exception {
        public InstallerException(String str) {
            super(str);
        }

        public static InstallerException from(Exception exc) throws InstallerException {
            throw new InstallerException(exc.toString());
        }
    }
}
