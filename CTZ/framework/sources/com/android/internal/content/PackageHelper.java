package com.android.internal.content;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.dex.DexMetadataHelper;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.NativeLibraryHelper;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import libcore.io.IoUtils;

public class PackageHelper {
    public static final int APP_INSTALL_AUTO = 0;
    public static final int APP_INSTALL_EXTERNAL = 2;
    public static final int APP_INSTALL_INTERNAL = 1;
    public static final int RECOMMEND_FAILED_ALREADY_EXISTS = -4;
    public static final int RECOMMEND_FAILED_INSUFFICIENT_STORAGE = -1;
    public static final int RECOMMEND_FAILED_INVALID_APK = -2;
    public static final int RECOMMEND_FAILED_INVALID_LOCATION = -3;
    public static final int RECOMMEND_FAILED_INVALID_URI = -6;
    public static final int RECOMMEND_FAILED_VERSION_DOWNGRADE = -7;
    public static final int RECOMMEND_INSTALL_EPHEMERAL = 3;
    public static final int RECOMMEND_INSTALL_EXTERNAL = 2;
    public static final int RECOMMEND_INSTALL_INTERNAL = 1;
    public static final int RECOMMEND_MEDIA_UNAVAILABLE = -5;
    private static final String TAG = "PackageHelper";
    private static TestableInterface sDefaultTestableInterface = null;

    public static abstract class TestableInterface {
        public abstract boolean getAllow3rdPartyOnInternalConfig(Context context);

        public abstract File getDataDirectory();

        public abstract ApplicationInfo getExistingAppInfo(Context context, String str);

        public abstract boolean getForceAllowOnExternalSetting(Context context);

        public abstract StorageManager getStorageManager(Context context);
    }

    public static IStorageManager getStorageManager() throws RemoteException {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IStorageManager.Stub.asInterface(service);
        }
        Log.e(TAG, "Can't get storagemanager service");
        throw new RemoteException("Could not contact storagemanager service");
    }

    private static synchronized TestableInterface getDefaultTestableInterface() {
        if (sDefaultTestableInterface == null) {
            sDefaultTestableInterface = new TestableInterface() {
                @Override
                public StorageManager getStorageManager(Context context) {
                    return (StorageManager) context.getSystemService(StorageManager.class);
                }

                @Override
                public boolean getForceAllowOnExternalSetting(Context context) {
                    return Settings.Global.getInt(context.getContentResolver(), Settings.Global.FORCE_ALLOW_ON_EXTERNAL, 0) != 0;
                }

                @Override
                public boolean getAllow3rdPartyOnInternalConfig(Context context) {
                    return context.getResources().getBoolean(R.bool.config_allow3rdPartyAppOnInternal);
                }

                @Override
                public ApplicationInfo getExistingAppInfo(Context context, String str) {
                    try {
                        return context.getPackageManager().getApplicationInfo(str, 4194304);
                    } catch (PackageManager.NameNotFoundException e) {
                        return null;
                    }
                }

                @Override
                public File getDataDirectory() {
                    return Environment.getDataDirectory();
                }
            };
        }
        return sDefaultTestableInterface;
    }

    @VisibleForTesting
    @Deprecated
    public static String resolveInstallVolume(Context context, String str, int i, long j, TestableInterface testableInterface) throws IOException {
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(-1);
        sessionParams.appPackageName = str;
        sessionParams.installLocation = i;
        sessionParams.sizeBytes = j;
        return resolveInstallVolume(context, sessionParams, testableInterface);
    }

    public static String resolveInstallVolume(Context context, PackageInstaller.SessionParams sessionParams) throws IOException {
        return resolveInstallVolume(context, sessionParams.appPackageName, sessionParams.installLocation, sessionParams.sizeBytes, getDefaultTestableInterface());
    }

    @VisibleForTesting
    public static String resolveInstallVolume(Context context, PackageInstaller.SessionParams sessionParams, TestableInterface testableInterface) throws Throwable {
        StorageManager storageManager = testableInterface.getStorageManager(context);
        boolean forceAllowOnExternalSetting = testableInterface.getForceAllowOnExternalSetting(context);
        boolean allow3rdPartyOnInternalConfig = testableInterface.getAllow3rdPartyOnInternalConfig(context);
        ApplicationInfo existingAppInfo = testableInterface.getExistingAppInfo(context, sessionParams.appPackageName);
        ArraySet arraySet = new ArraySet();
        long j = Long.MIN_VALUE;
        VolumeInfo volumeInfo = null;
        boolean z = false;
        for (VolumeInfo volumeInfo2 : storageManager.getVolumes()) {
            if (volumeInfo2.type == 1 && volumeInfo2.isMountedWritable()) {
                boolean zEquals = VolumeInfo.ID_PRIVATE_INTERNAL.equals(volumeInfo2.id);
                long allocatableBytes = storageManager.getAllocatableBytes(storageManager.getUuidForPath(new File(volumeInfo2.path)), translateAllocateFlags(sessionParams.installFlags));
                if (zEquals) {
                    z = sessionParams.sizeBytes <= allocatableBytes;
                }
                if (!zEquals || allow3rdPartyOnInternalConfig) {
                    if (allocatableBytes >= sessionParams.sizeBytes) {
                        arraySet.add(volumeInfo2.fsUuid);
                    }
                    if (allocatableBytes >= j) {
                        volumeInfo = volumeInfo2;
                        j = allocatableBytes;
                    }
                }
            }
        }
        if (existingAppInfo != null && existingAppInfo.isSystemApp()) {
            if (z) {
                return StorageManager.UUID_PRIVATE_INTERNAL;
            }
            throw new IOException("Not enough space on existing volume " + existingAppInfo.volumeUuid + " for system app " + sessionParams.appPackageName + " upgrade");
        }
        if (!forceAllowOnExternalSetting && sessionParams.installLocation == 1) {
            if (existingAppInfo != null && !Objects.equals(existingAppInfo.volumeUuid, StorageManager.UUID_PRIVATE_INTERNAL)) {
                throw new IOException("Cannot automatically move " + sessionParams.appPackageName + " from " + existingAppInfo.volumeUuid + " to internal storage");
            }
            if (!allow3rdPartyOnInternalConfig) {
                throw new IOException("Not allowed to install non-system apps on internal storage");
            }
            if (z) {
                return StorageManager.UUID_PRIVATE_INTERNAL;
            }
            throw new IOException("Requested internal only, but not enough space");
        }
        if (existingAppInfo != null) {
            if (Objects.equals(existingAppInfo.volumeUuid, StorageManager.UUID_PRIVATE_INTERNAL) && z) {
                return StorageManager.UUID_PRIVATE_INTERNAL;
            }
            if (arraySet.contains(existingAppInfo.volumeUuid)) {
                return existingAppInfo.volumeUuid;
            }
            throw new IOException("Not enough space on existing volume " + existingAppInfo.volumeUuid + " for " + sessionParams.appPackageName + " upgrade");
        }
        if (volumeInfo != null) {
            return volumeInfo.fsUuid;
        }
        throw new IOException("No special requests, but no room on allowed volumes.  allow3rdPartyOnInternal? " + allow3rdPartyOnInternalConfig);
    }

    public static boolean fitsOnInternal(Context context, PackageInstaller.SessionParams sessionParams) throws IOException {
        StorageManager storageManager = (StorageManager) context.getSystemService(StorageManager.class);
        return sessionParams.sizeBytes <= storageManager.getAllocatableBytes(storageManager.getUuidForPath(Environment.getDataDirectory()), translateAllocateFlags(sessionParams.installFlags));
    }

    public static boolean fitsOnExternal(Context context, PackageInstaller.SessionParams sessionParams) {
        StorageManager storageManager = (StorageManager) context.getSystemService(StorageManager.class);
        StorageVolume primaryVolume = storageManager.getPrimaryVolume();
        return sessionParams.sizeBytes > 0 && !primaryVolume.isEmulated() && Environment.MEDIA_MOUNTED.equals(primaryVolume.getState()) && sessionParams.sizeBytes <= storageManager.getStorageBytesUntilLow(primaryVolume.getPathFile());
    }

    @Deprecated
    public static int resolveInstallLocation(Context context, String str, int i, long j, int i2) {
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(-1);
        sessionParams.appPackageName = str;
        sessionParams.installLocation = i;
        sessionParams.sizeBytes = j;
        sessionParams.installFlags = i2;
        try {
            return resolveInstallLocation(context, sessionParams);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int resolveInstallLocation(Context context, PackageInstaller.SessionParams sessionParams) throws IOException {
        ApplicationInfo applicationInfo;
        char c;
        boolean z;
        boolean z2;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(sessionParams.appPackageName, 4194304);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        if ((sessionParams.installFlags & 2048) != 0) {
            z2 = false;
            c = 1;
            z = true;
        } else if ((sessionParams.installFlags & 16) == 0) {
            if ((sessionParams.installFlags & 8) != 0) {
                c = 2;
                z2 = false;
                z = false;
            } else if (sessionParams.installLocation != 1) {
                if (sessionParams.installLocation == 2) {
                    c = 2;
                } else {
                    if (sessionParams.installLocation == 0) {
                        c = (applicationInfo == null || (applicationInfo.flags & 262144) == 0) ? (char) 1 : (char) 2;
                    }
                    z2 = false;
                    z = false;
                    c = 1;
                }
                z = false;
                z2 = true;
            } else {
                z2 = false;
                z = false;
                c = 1;
            }
        }
        boolean zFitsOnInternal = (z2 || c == 1) ? fitsOnInternal(context, sessionParams) : false;
        boolean zFitsOnExternal = (z2 || c == 2) ? fitsOnExternal(context, sessionParams) : false;
        if (c == 1) {
            if (zFitsOnInternal) {
                return z ? 3 : 1;
            }
        } else if (c == 2 && zFitsOnExternal) {
            return 2;
        }
        if (!z2) {
            return -1;
        }
        if (zFitsOnInternal) {
            return 1;
        }
        return zFitsOnExternal ? 2 : -1;
    }

    @Deprecated
    public static long calculateInstalledSize(PackageParser.PackageLite packageLite, boolean z, String str) throws IOException {
        return calculateInstalledSize(packageLite, str);
    }

    public static long calculateInstalledSize(PackageParser.PackageLite packageLite, String str) throws IOException {
        return calculateInstalledSize(packageLite, str, (FileDescriptor) null);
    }

    public static long calculateInstalledSize(PackageParser.PackageLite packageLite, String str, FileDescriptor fileDescriptor) throws IOException {
        NativeLibraryHelper.Handle handleCreateFd = null;
        try {
            handleCreateFd = fileDescriptor != null ? NativeLibraryHelper.Handle.createFd(packageLite, fileDescriptor) : NativeLibraryHelper.Handle.create(packageLite);
            long jCalculateInstalledSize = calculateInstalledSize(packageLite, handleCreateFd, str);
            IoUtils.closeQuietly(handleCreateFd);
            return jCalculateInstalledSize;
        } catch (Throwable th) {
            IoUtils.closeQuietly(handleCreateFd);
            throw th;
        }
    }

    @Deprecated
    public static long calculateInstalledSize(PackageParser.PackageLite packageLite, boolean z, NativeLibraryHelper.Handle handle, String str) throws IOException {
        return calculateInstalledSize(packageLite, handle, str);
    }

    public static long calculateInstalledSize(PackageParser.PackageLite packageLite, NativeLibraryHelper.Handle handle, String str) throws IOException {
        Iterator<String> it = packageLite.getAllCodePaths().iterator();
        long length = 0;
        while (it.hasNext()) {
            length += new File(it.next()).length();
        }
        return length + DexMetadataHelper.getPackageDexMetadataSize(packageLite) + NativeLibraryHelper.sumNativeBinariesWithOverride(handle, str);
    }

    public static String replaceEnd(String str, String str2, String str3) {
        if (!str.endsWith(str2)) {
            throw new IllegalArgumentException("Expected " + str + " to end with " + str2);
        }
        return str.substring(0, str.length() - str2.length()) + str3;
    }

    public static int translateAllocateFlags(int i) {
        if ((i & 32768) != 0) {
            return 1;
        }
        return 0;
    }
}
