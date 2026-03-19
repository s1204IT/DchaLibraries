package com.android.server.pm.dex;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser;
import android.content.pm.dex.ArtManager;
import android.content.pm.dex.ArtManagerInternal;
import android.content.pm.dex.DexMetadataHelper;
import android.content.pm.dex.IArtManager;
import android.content.pm.dex.ISnapshotRuntimeProfileCallback;
import android.content.pm.dex.PackageOptimizationInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.system.Os;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.RoSystemProperties;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerServiceCompilerMapping;
import dalvik.system.DexFile;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileNotFoundException;
import libcore.io.IoUtils;

public class ArtManagerService extends IArtManager.Stub {
    private static final String BOOT_IMAGE_ANDROID_PACKAGE = "android";
    private static final String BOOT_IMAGE_PROFILE_NAME = "android.prof";
    private static final int TRON_COMPILATION_FILTER_ASSUMED_VERIFIED = 2;
    private static final int TRON_COMPILATION_FILTER_ERROR = 0;
    private static final int TRON_COMPILATION_FILTER_EVERYTHING = 11;
    private static final int TRON_COMPILATION_FILTER_EVERYTHING_PROFILE = 10;
    private static final int TRON_COMPILATION_FILTER_EXTRACT = 3;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_APK = 12;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_APK_FALLBACK = 13;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_VDEX_FALLBACK = 14;
    private static final int TRON_COMPILATION_FILTER_QUICKEN = 5;
    private static final int TRON_COMPILATION_FILTER_SPACE = 7;
    private static final int TRON_COMPILATION_FILTER_SPACE_PROFILE = 6;
    private static final int TRON_COMPILATION_FILTER_SPEED = 9;
    private static final int TRON_COMPILATION_FILTER_SPEED_PROFILE = 8;
    private static final int TRON_COMPILATION_FILTER_UNKNOWN = 1;
    private static final int TRON_COMPILATION_FILTER_VERIFY = 4;
    private static final int TRON_COMPILATION_REASON_AB_OTA = 6;
    private static final int TRON_COMPILATION_REASON_BG_DEXOPT = 5;
    private static final int TRON_COMPILATION_REASON_BOOT = 3;
    private static final int TRON_COMPILATION_REASON_ERROR = 0;
    private static final int TRON_COMPILATION_REASON_FIRST_BOOT = 2;
    private static final int TRON_COMPILATION_REASON_INACTIVE = 7;
    private static final int TRON_COMPILATION_REASON_INSTALL = 4;
    private static final int TRON_COMPILATION_REASON_SHARED = 8;
    private static final int TRON_COMPILATION_REASON_UNKNOWN = 1;
    private final Context mContext;
    private final Handler mHandler = new Handler(BackgroundThread.getHandler().getLooper());
    private final Object mInstallLock;

    @GuardedBy("mInstallLock")
    private final Installer mInstaller;
    private final IPackageManager mPackageManager;
    private static final String TAG = "ArtManagerService";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    static {
        verifyTronLoggingConstants();
    }

    public ArtManagerService(Context context, IPackageManager iPackageManager, Installer installer, Object obj) {
        this.mContext = context;
        this.mPackageManager = iPackageManager;
        this.mInstaller = installer;
        this.mInstallLock = obj;
        LocalServices.addService(ArtManagerInternal.class, new ArtManagerInternalImpl());
    }

    private boolean checkAndroidPermissions(int i, String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_RUNTIME_PROFILES", TAG);
        int iNoteOp = ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).noteOp(43, i, str);
        if (iNoteOp == 0) {
            return true;
        }
        if (iNoteOp == 3) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", TAG);
            return true;
        }
        return false;
    }

    private boolean checkShellPermissions(int i, String str, int i2) {
        if (i2 != 2000) {
            return false;
        }
        if (RoSystemProperties.DEBUGGABLE) {
            return true;
        }
        if (i == 1) {
            return false;
        }
        PackageInfo packageInfo = null;
        try {
            packageInfo = this.mPackageManager.getPackageInfo(str, 0, 0);
        } catch (RemoteException e) {
        }
        return packageInfo != null && (packageInfo.applicationInfo.flags & 2) == 2;
    }

    public void snapshotRuntimeProfile(int i, String str, String str2, ISnapshotRuntimeProfileCallback iSnapshotRuntimeProfileCallback, String str3) {
        int callingUid = Binder.getCallingUid();
        if (!checkShellPermissions(i, str, callingUid) && !checkAndroidPermissions(callingUid, str3)) {
            try {
                iSnapshotRuntimeProfileCallback.onError(2);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        Preconditions.checkNotNull(iSnapshotRuntimeProfileCallback);
        boolean z = i == 1;
        if (!z) {
            Preconditions.checkStringNotEmpty(str2);
            Preconditions.checkStringNotEmpty(str);
        }
        if (!isRuntimeProfilingEnabled(i, str3)) {
            throw new IllegalStateException("Runtime profiling is not enabled for " + i);
        }
        if (DEBUG) {
            Slog.d(TAG, "Requested snapshot for " + str + ":" + str2);
        }
        if (z) {
            snapshotBootImageProfile(iSnapshotRuntimeProfileCallback);
        } else {
            snapshotAppProfile(str, str2, iSnapshotRuntimeProfileCallback);
        }
    }

    private void snapshotAppProfile(String str, String str2, ISnapshotRuntimeProfileCallback iSnapshotRuntimeProfileCallback) {
        PackageInfo packageInfo;
        String str3 = null;
        try {
            packageInfo = this.mPackageManager.getPackageInfo(str, 0, 0);
        } catch (RemoteException e) {
            packageInfo = null;
        }
        if (packageInfo == null) {
            postError(iSnapshotRuntimeProfileCallback, str, 0);
            return;
        }
        boolean zEquals = packageInfo.applicationInfo.getBaseCodePath().equals(str2);
        String[] splitCodePaths = packageInfo.applicationInfo.getSplitCodePaths();
        if (!zEquals && splitCodePaths != null) {
            int length = splitCodePaths.length - 1;
            while (true) {
                if (length < 0) {
                    break;
                }
                if (!splitCodePaths[length].equals(str2)) {
                    length--;
                } else {
                    str3 = packageInfo.applicationInfo.splitNames[length];
                    zEquals = true;
                    break;
                }
            }
        }
        if (!zEquals) {
            postError(iSnapshotRuntimeProfileCallback, str, 1);
            return;
        }
        int appId = UserHandle.getAppId(packageInfo.applicationInfo.uid);
        if (appId < 0) {
            postError(iSnapshotRuntimeProfileCallback, str, 2);
            Slog.wtf(TAG, "AppId is -1 for package: " + str);
            return;
        }
        createProfileSnapshot(str, ArtManager.getProfileName(str3), str2, appId, iSnapshotRuntimeProfileCallback);
        destroyProfileSnapshot(str, ArtManager.getProfileName(str3));
    }

    private void createProfileSnapshot(String str, String str2, String str3, int i, ISnapshotRuntimeProfileCallback iSnapshotRuntimeProfileCallback) {
        synchronized (this.mInstallLock) {
            try {
                if (!this.mInstaller.createProfileSnapshot(i, str, str2, str3)) {
                    postError(iSnapshotRuntimeProfileCallback, str, 2);
                    return;
                }
                File profileSnapshotFileForName = ArtManager.getProfileSnapshotFileForName(str, str2);
                try {
                    ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(profileSnapshotFileForName, 268435456);
                    if (parcelFileDescriptorOpen == null || !parcelFileDescriptorOpen.getFileDescriptor().valid()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("ParcelFileDescriptor.open returned an invalid descriptor for ");
                        sb.append(str);
                        sb.append(":");
                        sb.append(profileSnapshotFileForName);
                        sb.append(". isNull=");
                        sb.append(parcelFileDescriptorOpen == null);
                        Slog.wtf(TAG, sb.toString());
                        postError(iSnapshotRuntimeProfileCallback, str, 2);
                    } else {
                        postSuccess(str, parcelFileDescriptorOpen, iSnapshotRuntimeProfileCallback);
                    }
                } catch (FileNotFoundException e) {
                    Slog.w(TAG, "Could not open snapshot profile for " + str + ":" + profileSnapshotFileForName, e);
                    postError(iSnapshotRuntimeProfileCallback, str, 2);
                }
            } catch (Installer.InstallerException e2) {
                postError(iSnapshotRuntimeProfileCallback, str, 2);
            }
        }
    }

    private void destroyProfileSnapshot(String str, String str2) {
        if (DEBUG) {
            Slog.d(TAG, "Destroying profile snapshot for" + str + ":" + str2);
        }
        synchronized (this.mInstallLock) {
            try {
                this.mInstaller.destroyProfileSnapshot(str, str2);
            } catch (Installer.InstallerException e) {
                Slog.e(TAG, "Failed to destroy profile snapshot for " + str + ":" + str2, e);
            }
        }
    }

    public boolean isRuntimeProfilingEnabled(int i, String str) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 2000 || checkAndroidPermissions(callingUid, str)) {
            switch (i) {
                case 0:
                    return SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false);
                case 1:
                    return (Build.IS_USERDEBUG || Build.IS_ENG) && SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false) && SystemProperties.getBoolean("dalvik.vm.profilebootimage", false);
                default:
                    throw new IllegalArgumentException("Invalid profile type:" + i);
            }
        }
        return false;
    }

    private void snapshotBootImageProfile(ISnapshotRuntimeProfileCallback iSnapshotRuntimeProfileCallback) {
        createProfileSnapshot("android", BOOT_IMAGE_PROFILE_NAME, String.join(":", Os.getenv("BOOTCLASSPATH"), Os.getenv("SYSTEMSERVERCLASSPATH")), -1, iSnapshotRuntimeProfileCallback);
        destroyProfileSnapshot("android", BOOT_IMAGE_PROFILE_NAME);
    }

    private void postError(final ISnapshotRuntimeProfileCallback iSnapshotRuntimeProfileCallback, final String str, final int i) {
        if (DEBUG) {
            Slog.d(TAG, "Failed to snapshot profile for " + str + " with error: " + i);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                ArtManagerService.lambda$postError$0(iSnapshotRuntimeProfileCallback, i, str);
            }
        });
    }

    static void lambda$postError$0(ISnapshotRuntimeProfileCallback iSnapshotRuntimeProfileCallback, int i, String str) {
        try {
            iSnapshotRuntimeProfileCallback.onError(i);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to callback after profile snapshot for " + str, e);
        }
    }

    private void postSuccess(final String str, final ParcelFileDescriptor parcelFileDescriptor, final ISnapshotRuntimeProfileCallback iSnapshotRuntimeProfileCallback) {
        if (DEBUG) {
            Slog.d(TAG, "Successfully snapshot profile for " + str);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                ArtManagerService.lambda$postSuccess$1(parcelFileDescriptor, iSnapshotRuntimeProfileCallback, str);
            }
        });
    }

    static void lambda$postSuccess$1(ParcelFileDescriptor parcelFileDescriptor, ISnapshotRuntimeProfileCallback iSnapshotRuntimeProfileCallback, String str) {
        try {
            try {
                if (parcelFileDescriptor.getFileDescriptor().valid()) {
                    iSnapshotRuntimeProfileCallback.onSuccess(parcelFileDescriptor);
                } else {
                    Slog.wtf(TAG, "The snapshot FD became invalid before posting the result for " + str);
                    iSnapshotRuntimeProfileCallback.onError(2);
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to call onSuccess after profile snapshot for " + str, e);
            }
        } finally {
            IoUtils.closeQuietly(parcelFileDescriptor);
        }
    }

    public void prepareAppProfiles(PackageParser.Package r13, int i) {
        int appId = UserHandle.getAppId(r13.applicationInfo.uid);
        if (i < 0) {
            Slog.wtf(TAG, "Invalid user id: " + i);
            return;
        }
        if (appId < 0) {
            Slog.wtf(TAG, "Invalid app id: " + appId);
            return;
        }
        try {
            ArrayMap<String, String> packageProfileNames = getPackageProfileNames(r13);
            for (int size = packageProfileNames.size() - 1; size >= 0; size--) {
                String strKeyAt = packageProfileNames.keyAt(size);
                String strValueAt = packageProfileNames.valueAt(size);
                File fileFindDexMetadataForFile = DexMetadataHelper.findDexMetadataForFile(new File(strKeyAt));
                String absolutePath = fileFindDexMetadataForFile == null ? null : fileFindDexMetadataForFile.getAbsolutePath();
                synchronized (this.mInstaller) {
                    if (!this.mInstaller.prepareAppProfile(r13.packageName, i, appId, strValueAt, strKeyAt, absolutePath)) {
                        Slog.e(TAG, "Failed to prepare profile for " + r13.packageName + ":" + strKeyAt);
                    }
                }
            }
        } catch (Installer.InstallerException e) {
            Slog.e(TAG, "Failed to prepare profile for " + r13.packageName, e);
        }
    }

    public void prepareAppProfiles(PackageParser.Package r3, int[] iArr) {
        for (int i : iArr) {
            prepareAppProfiles(r3, i);
        }
    }

    public void clearAppProfiles(PackageParser.Package r6) {
        try {
            ArrayMap<String, String> packageProfileNames = getPackageProfileNames(r6);
            for (int size = packageProfileNames.size() - 1; size >= 0; size--) {
                this.mInstaller.clearAppProfiles(r6.packageName, packageProfileNames.valueAt(size));
            }
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, String.valueOf(e));
        }
    }

    public void dumpProfiles(PackageParser.Package r9) {
        int sharedAppGid = UserHandle.getSharedAppGid(r9.applicationInfo.uid);
        try {
            ArrayMap<String, String> packageProfileNames = getPackageProfileNames(r9);
            for (int size = packageProfileNames.size() - 1; size >= 0; size--) {
                String strKeyAt = packageProfileNames.keyAt(size);
                String strValueAt = packageProfileNames.valueAt(size);
                synchronized (this.mInstallLock) {
                    this.mInstaller.dumpProfiles(sharedAppGid, r9.packageName, strValueAt, strKeyAt);
                }
            }
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, "Failed to dump profiles", e);
        }
    }

    private ArrayMap<String, String> getPackageProfileNames(PackageParser.Package r5) {
        ArrayMap<String, String> arrayMap = new ArrayMap<>();
        if ((r5.applicationInfo.flags & 4) != 0) {
            arrayMap.put(r5.baseCodePath, ArtManager.getProfileName((String) null));
        }
        if (!ArrayUtils.isEmpty(r5.splitCodePaths)) {
            for (int i = 0; i < r5.splitCodePaths.length; i++) {
                if ((r5.splitFlags[i] & 4) != 0) {
                    arrayMap.put(r5.splitCodePaths[i], ArtManager.getProfileName(r5.splitNames[i]));
                }
            }
        }
        return arrayMap;
    }

    private static int getCompilationReasonTronValue(String str) {
        switch (str) {
            case "unknown":
                return 1;
            case "error":
                return 0;
            case "first-boot":
                return 2;
            case "boot":
                return 3;
            case "install":
                return 4;
            case "bg-dexopt":
                return 5;
            case "ab-ota":
                return 6;
            case "inactive":
                return 7;
            case "shared":
                return 8;
            default:
                return 1;
        }
    }

    private static int getCompilationFilterTronValue(String str) {
        switch (str) {
            case "error":
                return 0;
            case "unknown":
                return 1;
            case "assume-verified":
                return 2;
            case "extract":
                return 3;
            case "verify":
                return 4;
            case "quicken":
                return 5;
            case "space-profile":
                return 6;
            case "space":
                return 7;
            case "speed-profile":
                return 8;
            case "speed":
                return 9;
            case "everything-profile":
                return 10;
            case "everything":
                return 11;
            case "run-from-apk":
                return 12;
            case "run-from-apk-fallback":
                return 13;
            case "run-from-vdex-fallback":
                return 14;
            default:
                return 1;
        }
    }

    private static void verifyTronLoggingConstants() {
        for (int i = 0; i < PackageManagerServiceCompilerMapping.REASON_STRINGS.length; i++) {
            String str = PackageManagerServiceCompilerMapping.REASON_STRINGS[i];
            int compilationReasonTronValue = getCompilationReasonTronValue(str);
            if (compilationReasonTronValue == 0 || compilationReasonTronValue == 1) {
                throw new IllegalArgumentException("Compilation reason not configured for TRON logging: " + str);
            }
        }
    }

    private class ArtManagerInternalImpl extends ArtManagerInternal {
        private ArtManagerInternalImpl() {
        }

        public PackageOptimizationInfo getPackageOptimizationInfo(ApplicationInfo applicationInfo, String str) {
            String status;
            String reason;
            try {
                DexFile.OptimizationInfo dexFileOptimizationInfo = DexFile.getDexFileOptimizationInfo(applicationInfo.getBaseCodePath(), VMRuntime.getInstructionSet(str));
                status = dexFileOptimizationInfo.getStatus();
                reason = dexFileOptimizationInfo.getReason();
            } catch (FileNotFoundException e) {
                Slog.e(ArtManagerService.TAG, "Could not get optimizations status for " + applicationInfo.getBaseCodePath(), e);
                status = "error";
                reason = "error";
            } catch (IllegalArgumentException e2) {
                Slog.wtf(ArtManagerService.TAG, "Requested optimization status for " + applicationInfo.getBaseCodePath() + " due to an invalid abi " + str, e2);
                status = "error";
                reason = "error";
            }
            return new PackageOptimizationInfo(ArtManagerService.getCompilationFilterTronValue(status), ArtManagerService.getCompilationReasonTronValue(reason));
        }
    }
}
