package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.fullbackup.FullBackupObbConnection;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class FullRestoreEngine extends RestoreEngine {
    private IBackupAgent mAgent;
    private String mAgentPackage;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    final boolean mAllowApks;
    private final boolean mAllowObbs;
    private final BackupManagerService mBackupManagerService;
    final int mEphemeralOpToken;
    final IBackupManagerMonitor mMonitor;
    private final BackupRestoreTask mMonitorTask;
    private IFullBackupRestoreObserver mObserver;
    final PackageInfo mOnlyPackage;
    private ApplicationInfo mTargetApp;
    private final RestoreDeleteObserver mDeleteObserver = new RestoreDeleteObserver();
    private FullBackupObbConnection mObbConnection = null;
    private final HashMap<String, RestorePolicy> mPackagePolicies = new HashMap<>();
    private final HashMap<String, String> mPackageInstallers = new HashMap<>();
    private final HashMap<String, Signature[]> mManifestSignatures = new HashMap<>();
    private final HashSet<String> mClearedPackages = new HashSet<>();
    private ParcelFileDescriptor[] mPipes = null;
    private byte[] mWidgetData = null;
    final byte[] mBuffer = new byte[32768];
    private long mBytes = 0;

    static long access$014(FullRestoreEngine fullRestoreEngine, long j) {
        long j2 = fullRestoreEngine.mBytes + j;
        fullRestoreEngine.mBytes = j2;
        return j2;
    }

    public FullRestoreEngine(BackupManagerService backupManagerService, BackupRestoreTask backupRestoreTask, IFullBackupRestoreObserver iFullBackupRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor, PackageInfo packageInfo, boolean z, boolean z2, int i) {
        this.mBackupManagerService = backupManagerService;
        this.mEphemeralOpToken = i;
        this.mMonitorTask = backupRestoreTask;
        this.mObserver = iFullBackupRestoreObserver;
        this.mMonitor = iBackupManagerMonitor;
        this.mOnlyPackage = packageInfo;
        this.mAllowApks = z;
        this.mAllowObbs = z2;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    public IBackupAgent getAgent() {
        return this.mAgent;
    }

    public byte[] getWidgetData() {
        return this.mWidgetData;
    }

    public boolean restoreOneFile(

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$android$server$backup$restore$RestorePolicy = new int[RestorePolicy.values().length];

        static {
            try {
                $SwitchMap$com$android$server$backup$restore$RestorePolicy[RestorePolicy.IGNORE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$RestorePolicy[RestorePolicy.ACCEPT_IF_APK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$RestorePolicy[RestorePolicy.ACCEPT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private void setUpPipes() throws IOException {
        this.mPipes = ParcelFileDescriptor.createPipe();
    }

    private void tearDownPipes() {
        synchronized (this) {
            if (this.mPipes != null) {
                try {
                    this.mPipes[0].close();
                    this.mPipes[0] = null;
                    this.mPipes[1].close();
                    this.mPipes[1] = null;
                } catch (IOException e) {
                    Slog.w(BackupManagerService.TAG, "Couldn't close agent pipes", e);
                }
                this.mPipes = null;
            }
        }
    }

    private void tearDownAgent(ApplicationInfo applicationInfo) {
        if (this.mAgent != null) {
            this.mBackupManagerService.tearDownAgentAndKill(applicationInfo);
            this.mAgent = null;
        }
    }

    void handleTimeout() {
        tearDownPipes();
        setResult(-2);
        setRunning(false);
    }

    private static boolean isRestorableFile(FileMetadata fileMetadata) {
        if ("c".equals(fileMetadata.domain)) {
            return false;
        }
        return ("r".equals(fileMetadata.domain) && fileMetadata.path.startsWith("no_backup/")) ? false : true;
    }

    private static boolean isCanonicalFilePath(String str) {
        if (str.contains("..") || str.contains("//")) {
            return false;
        }
        return true;
    }

    private boolean shouldForceClearAppDataOnFullRestore(String str) {
        String string = Settings.Secure.getString(this.mBackupManagerService.getContext().getContentResolver(), "packages_to_clear_data_before_full_restore");
        if (TextUtils.isEmpty(string)) {
            return false;
        }
        return Arrays.asList(string.split(";")).contains(str);
    }

    void sendOnRestorePackage(String str) {
        if (this.mObserver != null) {
            try {
                this.mObserver.onRestorePackage(str);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: restorePackage");
                this.mObserver = null;
            }
        }
    }
}
