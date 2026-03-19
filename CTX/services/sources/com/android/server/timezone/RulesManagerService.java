package com.android.server.timezone;

import android.app.timezone.DistroFormatVersion;
import android.app.timezone.DistroRulesVersion;
import android.app.timezone.ICallback;
import android.app.timezone.IRulesManager;
import android.app.timezone.RulesState;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.EventLogTags;
import com.android.server.NetworkManagementService;
import com.android.server.SystemService;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.timezone.distro.DistroException;
import com.android.timezone.distro.DistroVersion;
import com.android.timezone.distro.StagedDistroOperation;
import com.android.timezone.distro.TimeZoneDistro;
import com.android.timezone.distro.installer.TimeZoneDistroInstaller;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import libcore.icu.ICU;
import libcore.util.TimeZoneFinder;
import libcore.util.ZoneInfoDB;

public final class RulesManagerService extends IRulesManager.Stub {

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    static final String REQUIRED_QUERY_PERMISSION = "android.permission.QUERY_TIME_ZONE_RULES";

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    static final String REQUIRED_UPDATER_PERMISSION = "android.permission.UPDATE_TIME_ZONE_RULES";
    private static final String TAG = "timezone.RulesManagerService";
    private final Executor mExecutor;
    private final TimeZoneDistroInstaller mInstaller;
    private final RulesManagerIntentHelper mIntentHelper;
    private final AtomicBoolean mOperationInProgress = new AtomicBoolean(false);
    private final PackageTracker mPackageTracker;
    private final PermissionHelper mPermissionHelper;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    static final DistroFormatVersion DISTRO_FORMAT_VERSION_SUPPORTED = new DistroFormatVersion(2, 1);
    private static final File SYSTEM_TZ_DATA_FILE = new File("/system/usr/share/zoneinfo/tzdata");
    private static final File TZ_DATA_DIR = new File("/data/misc/zoneinfo");

    public static class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            ?? Create = RulesManagerService.create(getContext());
            Create.start();
            publishBinderService("timezone", Create);
            publishLocalService(RulesManagerService.class, Create);
        }
    }

    private static RulesManagerService create(Context context) {
        RulesManagerServiceHelperImpl rulesManagerServiceHelperImpl = new RulesManagerServiceHelperImpl(context);
        return new RulesManagerService(rulesManagerServiceHelperImpl, rulesManagerServiceHelperImpl, rulesManagerServiceHelperImpl, PackageTracker.create(context), new TimeZoneDistroInstaller(TAG, SYSTEM_TZ_DATA_FILE, TZ_DATA_DIR));
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    RulesManagerService(PermissionHelper permissionHelper, Executor executor, RulesManagerIntentHelper rulesManagerIntentHelper, PackageTracker packageTracker, TimeZoneDistroInstaller timeZoneDistroInstaller) {
        this.mPermissionHelper = permissionHelper;
        this.mExecutor = executor;
        this.mIntentHelper = rulesManagerIntentHelper;
        this.mPackageTracker = packageTracker;
        this.mInstaller = timeZoneDistroInstaller;
    }

    public void start() {
        this.mPackageTracker.start();
    }

    public RulesState getRulesState() {
        this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_QUERY_PERMISSION);
        return getRulesStateInternal();
    }

    private RulesState getRulesStateInternal() {
        int i;
        DistroRulesVersion distroRulesVersion;
        int i2;
        DistroRulesVersion distroRulesVersion2;
        int i3;
        RulesState rulesState;
        DistroRulesVersion distroRulesVersion3;
        int i4;
        synchronized (this) {
            DistroRulesVersion distroRulesVersion4 = null;
            try {
                try {
                    String systemRulesVersion = this.mInstaller.getSystemRulesVersion();
                    int i5 = 2;
                    try {
                        DistroVersion installedDistroVersion = this.mInstaller.getInstalledDistroVersion();
                        if (installedDistroVersion != null) {
                            try {
                                distroRulesVersion3 = new DistroRulesVersion(installedDistroVersion.rulesVersion, installedDistroVersion.revision);
                                i4 = 2;
                            } catch (DistroException | IOException e) {
                                e = e;
                                i = 2;
                                Slog.w(TAG, "Failed to read installed distro.", e);
                                distroRulesVersion = null;
                                i2 = i;
                            }
                        } else {
                            distroRulesVersion3 = null;
                            i4 = 1;
                        }
                        i2 = i4;
                        distroRulesVersion = distroRulesVersion3;
                    } catch (DistroException | IOException e2) {
                        e = e2;
                        i = 0;
                    }
                    boolean z = this.mOperationInProgress.get();
                    if (!z) {
                        try {
                            StagedDistroOperation stagedDistroOperation = this.mInstaller.getStagedDistroOperation();
                            if (stagedDistroOperation != null) {
                                if (!stagedDistroOperation.isUninstall) {
                                    i5 = 3;
                                    try {
                                        DistroVersion distroVersion = stagedDistroOperation.distroVersion;
                                        distroRulesVersion4 = new DistroRulesVersion(distroVersion.rulesVersion, distroVersion.revision);
                                    } catch (DistroException | IOException e3) {
                                        e = e3;
                                        Slog.w(TAG, "Failed to read staged distro.", e);
                                    }
                                }
                            } else {
                                i5 = 1;
                            }
                        } catch (DistroException | IOException e4) {
                            e = e4;
                            i5 = 0;
                        }
                        distroRulesVersion2 = distroRulesVersion4;
                        i3 = i5;
                    } else {
                        distroRulesVersion2 = null;
                        i3 = 0;
                    }
                    rulesState = new RulesState(systemRulesVersion, DISTRO_FORMAT_VERSION_SUPPORTED, z, i3, distroRulesVersion2, i2, distroRulesVersion);
                } catch (IOException e5) {
                    Slog.w(TAG, "Failed to read system rules", e5);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return rulesState;
    }

    public int requestInstall(ParcelFileDescriptor parcelFileDescriptor, byte[] bArr, ICallback iCallback) {
        boolean z = true;
        try {
            this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_UPDATER_PERMISSION);
            CheckToken checkTokenCreateCheckTokenOrThrow = null;
            if (bArr != null) {
                checkTokenCreateCheckTokenOrThrow = createCheckTokenOrThrow(bArr);
            }
            EventLogTags.writeTimezoneRequestInstall(toStringOrNull(checkTokenCreateCheckTokenOrThrow));
            synchronized (this) {
                try {
                    if (parcelFileDescriptor == null) {
                        throw new NullPointerException("distroParcelFileDescriptor == null");
                    }
                    if (iCallback == null) {
                        throw new NullPointerException("observer == null");
                    }
                    if (!this.mOperationInProgress.get()) {
                        this.mOperationInProgress.set(true);
                        this.mExecutor.execute(new InstallRunnable(parcelFileDescriptor, checkTokenCreateCheckTokenOrThrow, iCallback));
                        try {
                            if (parcelFileDescriptor != null) {
                            }
                            return 0;
                        } catch (Throwable th) {
                            z = false;
                            th = th;
                            throw th;
                        }
                    }
                    if (parcelFileDescriptor != null) {
                        try {
                            parcelFileDescriptor.close();
                        } catch (IOException e) {
                            Slog.w(TAG, "Failed to close distroParcelFileDescriptor", e);
                        }
                    }
                    return 1;
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        } catch (Throwable th3) {
            if (parcelFileDescriptor != null && z) {
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e2) {
                    Slog.w(TAG, "Failed to close distroParcelFileDescriptor", e2);
                }
            }
            throw th3;
        }
    }

    private class InstallRunnable implements Runnable {
        private final ICallback mCallback;
        private final CheckToken mCheckToken;
        private final ParcelFileDescriptor mDistroParcelFileDescriptor;

        InstallRunnable(ParcelFileDescriptor parcelFileDescriptor, CheckToken checkToken, ICallback iCallback) {
            this.mDistroParcelFileDescriptor = parcelFileDescriptor;
            this.mCheckToken = checkToken;
            this.mCallback = iCallback;
        }

        @Override
        public void run() throws Throwable {
            boolean z;
            EventLogTags.writeTimezoneInstallStarted(RulesManagerService.toStringOrNull(this.mCheckToken));
            try {
                ParcelFileDescriptor parcelFileDescriptor = this.mDistroParcelFileDescriptor;
                try {
                    int iStageInstallWithErrorCode = RulesManagerService.this.mInstaller.stageInstallWithErrorCode(new TimeZoneDistro(new FileInputStream(parcelFileDescriptor.getFileDescriptor(), false)));
                    sendInstallNotificationIntentIfRequired(iStageInstallWithErrorCode);
                    int iMapInstallerResultToApiCode = mapInstallerResultToApiCode(iStageInstallWithErrorCode);
                    EventLogTags.writeTimezoneInstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), iMapInstallerResultToApiCode);
                    RulesManagerService.this.sendFinishedStatus(this.mCallback, iMapInstallerResultToApiCode);
                    if (parcelFileDescriptor != null) {
                        try {
                            parcelFileDescriptor.close();
                        } catch (Exception e) {
                            e = e;
                            z = true;
                            try {
                                Slog.w(RulesManagerService.TAG, "Failed to install distro.", e);
                                EventLogTags.writeTimezoneInstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), 1);
                                RulesManagerService.this.sendFinishedStatus(this.mCallback, 1);
                                RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, z);
                            } catch (Throwable th) {
                                th = th;
                                RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, z);
                                RulesManagerService.this.mOperationInProgress.set(false);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            z = true;
                            RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, z);
                            RulesManagerService.this.mOperationInProgress.set(false);
                            throw th;
                        }
                    }
                    RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, true);
                } finally {
                }
            } catch (Exception e2) {
                e = e2;
                z = false;
            } catch (Throwable th3) {
                th = th3;
                z = false;
            }
            RulesManagerService.this.mOperationInProgress.set(false);
        }

        private void sendInstallNotificationIntentIfRequired(int i) {
            if (i == 0) {
                RulesManagerService.this.mIntentHelper.sendTimeZoneOperationStaged();
            }
        }

        private int mapInstallerResultToApiCode(int i) {
            switch (i) {
                case 0:
                    return 0;
                case 1:
                    return 2;
                case 2:
                    return 3;
                case 3:
                    return 4;
                case 4:
                    return 5;
                default:
                    return 1;
            }
        }
    }

    public int requestUninstall(byte[] bArr, ICallback iCallback) {
        CheckToken checkTokenCreateCheckTokenOrThrow;
        this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_UPDATER_PERMISSION);
        if (bArr != null) {
            checkTokenCreateCheckTokenOrThrow = createCheckTokenOrThrow(bArr);
        } else {
            checkTokenCreateCheckTokenOrThrow = null;
        }
        EventLogTags.writeTimezoneRequestUninstall(toStringOrNull(checkTokenCreateCheckTokenOrThrow));
        synchronized (this) {
            if (iCallback == null) {
                throw new NullPointerException("callback == null");
            }
            if (this.mOperationInProgress.get()) {
                return 1;
            }
            this.mOperationInProgress.set(true);
            this.mExecutor.execute(new UninstallRunnable(checkTokenCreateCheckTokenOrThrow, iCallback));
            return 0;
        }
    }

    private class UninstallRunnable implements Runnable {
        private final ICallback mCallback;
        private final CheckToken mCheckToken;

        UninstallRunnable(CheckToken checkToken, ICallback iCallback) {
            this.mCheckToken = checkToken;
            this.mCallback = iCallback;
        }

        @Override
        public void run() throws Throwable {
            boolean z;
            EventLogTags.writeTimezoneUninstallStarted(RulesManagerService.toStringOrNull(this.mCheckToken));
            try {
                try {
                    int iStageUninstall = RulesManagerService.this.mInstaller.stageUninstall();
                    sendUninstallNotificationIntentIfRequired(iStageUninstall);
                    z = iStageUninstall == 0 || iStageUninstall == 1;
                    int i = !z ? 1 : 0;
                    try {
                        EventLogTags.writeTimezoneUninstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), i);
                        RulesManagerService.this.sendFinishedStatus(this.mCallback, i);
                    } catch (Exception e) {
                        e = e;
                        EventLogTags.writeTimezoneUninstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), 1);
                        Slog.w(RulesManagerService.TAG, "Failed to uninstall distro.", e);
                        RulesManagerService.this.sendFinishedStatus(this.mCallback, 1);
                    }
                } catch (Throwable th) {
                    th = th;
                    RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, z);
                    RulesManagerService.this.mOperationInProgress.set(false);
                    throw th;
                }
            } catch (Exception e2) {
                e = e2;
                z = false;
            } catch (Throwable th2) {
                th = th2;
                z = false;
                RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, z);
                RulesManagerService.this.mOperationInProgress.set(false);
                throw th;
            }
            RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, z);
            RulesManagerService.this.mOperationInProgress.set(false);
        }

        private void sendUninstallNotificationIntentIfRequired(int i) {
            switch (i) {
                case 0:
                    RulesManagerService.this.mIntentHelper.sendTimeZoneOperationStaged();
                    break;
                case 1:
                    RulesManagerService.this.mIntentHelper.sendTimeZoneOperationUnstaged();
                    break;
            }
        }
    }

    private void sendFinishedStatus(ICallback iCallback, int i) {
        try {
            iCallback.onFinished(i);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to notify observer of result", e);
        }
    }

    public void requestNothing(byte[] bArr, boolean z) {
        CheckToken checkTokenCreateCheckTokenOrThrow;
        this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_UPDATER_PERMISSION);
        if (bArr != null) {
            checkTokenCreateCheckTokenOrThrow = createCheckTokenOrThrow(bArr);
        } else {
            checkTokenCreateCheckTokenOrThrow = null;
        }
        EventLogTags.writeTimezoneRequestNothing(toStringOrNull(checkTokenCreateCheckTokenOrThrow));
        this.mPackageTracker.recordCheckResult(checkTokenCreateCheckTokenOrThrow, z);
        EventLogTags.writeTimezoneNothingComplete(toStringOrNull(checkTokenCreateCheckTokenOrThrow));
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (!this.mPermissionHelper.checkDumpPermission(TAG, printWriter)) {
            return;
        }
        RulesState rulesStateInternal = getRulesStateInternal();
        if (strArr != null && strArr.length == 2) {
            if ("-format_state".equals(strArr[0]) && strArr[1] != null) {
                for (char c : strArr[1].toCharArray()) {
                    switch (c) {
                        case HdmiCecKeycode.CEC_KEYCODE_PAUSE_PLAY_FUNCTION:
                            printWriter.println("Active rules version (ICU, ZoneInfoDB, TimeZoneFinder): " + ICU.getTZDataVersion() + "," + ZoneInfoDB.getInstance().getVersion() + "," + TimeZoneFinder.getInstance().getIanaVersion());
                            break;
                        case HdmiCecKeycode.CEC_KEYCODE_PAUSE_RECORD_FUNCTION:
                            String strDistroStatusToString = "Unknown";
                            if (rulesStateInternal != null) {
                                strDistroStatusToString = distroStatusToString(rulesStateInternal.getDistroStatus());
                            }
                            printWriter.println("Current install state: " + strDistroStatusToString);
                            break;
                        case HdmiCecKeycode.CEC_KEYCODE_SELECT_AV_INPUT_FUNCTION:
                            String dumpString = "Unknown";
                            if (rulesStateInternal != null) {
                                DistroRulesVersion installedDistroRulesVersion = rulesStateInternal.getInstalledDistroRulesVersion();
                                if (installedDistroRulesVersion == null) {
                                    dumpString = "<None>";
                                } else {
                                    dumpString = installedDistroRulesVersion.toDumpString();
                                }
                            }
                            printWriter.println("Installed rules version: " + dumpString);
                            break;
                        case NetworkManagementService.NetdResponseCode.TetherInterfaceListResult:
                            String strStagedOperationToString = "Unknown";
                            if (rulesStateInternal != null) {
                                strStagedOperationToString = stagedOperationToString(rulesStateInternal.getStagedOperationType());
                            }
                            printWriter.println("Staged operation: " + strStagedOperationToString);
                            break;
                        case 'p':
                            String string = "Unknown";
                            if (rulesStateInternal != null) {
                                string = Boolean.toString(rulesStateInternal.isOperationInProgress());
                            }
                            printWriter.println("Operation in progress: " + string);
                            break;
                        case HdmiCecKeycode.CEC_KEYCODE_F3_GREEN:
                            String systemRulesVersion = "Unknown";
                            if (rulesStateInternal != null) {
                                systemRulesVersion = rulesStateInternal.getSystemRulesVersion();
                            }
                            printWriter.println("System rules version: " + systemRulesVersion);
                            break;
                        case HdmiCecKeycode.CEC_KEYCODE_F4_YELLOW:
                            String dumpString2 = "Unknown";
                            if (rulesStateInternal != null) {
                                DistroRulesVersion stagedDistroRulesVersion = rulesStateInternal.getStagedDistroRulesVersion();
                                if (stagedDistroRulesVersion == null) {
                                    dumpString2 = "<None>";
                                } else {
                                    dumpString2 = stagedDistroRulesVersion.toDumpString();
                                }
                            }
                            printWriter.println("Staged rules version: " + dumpString2);
                            break;
                        default:
                            printWriter.println("Unknown option: " + c);
                            break;
                    }
                }
                return;
            }
        }
        printWriter.println("RulesManagerService state: " + toString());
        printWriter.println("Active rules version (ICU, ZoneInfoDB, TimeZoneFinder): " + ICU.getTZDataVersion() + "," + ZoneInfoDB.getInstance().getVersion() + "," + TimeZoneFinder.getInstance().getIanaVersion());
        StringBuilder sb = new StringBuilder();
        sb.append("Distro state: ");
        sb.append(rulesStateInternal.toString());
        printWriter.println(sb.toString());
        this.mPackageTracker.dump(printWriter);
    }

    void notifyIdle() {
        this.mPackageTracker.triggerUpdateIfNeeded(false);
    }

    public String toString() {
        return "RulesManagerService{mOperationInProgress=" + this.mOperationInProgress + '}';
    }

    private static CheckToken createCheckTokenOrThrow(byte[] bArr) {
        try {
            return CheckToken.fromByteArray(bArr);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read token bytes " + Arrays.toString(bArr), e);
        }
    }

    private static String distroStatusToString(int i) {
        switch (i) {
            case 1:
                return "None";
            case 2:
                return "Installed";
            default:
                return "Unknown";
        }
    }

    private static String stagedOperationToString(int i) {
        switch (i) {
            case 1:
                return "None";
            case 2:
                return "Uninstall";
            case 3:
                return "Install";
            default:
                return "Unknown";
        }
    }

    private static String toStringOrNull(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }
}
