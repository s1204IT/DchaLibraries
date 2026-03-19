package com.android.server.backup.restore;

import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AdbRestoreFinishedLatch implements BackupRestoreTask {
    private static final String TAG = "AdbRestoreFinishedLatch";
    private BackupManagerService backupManagerService;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private final int mCurrentOpToken;
    final CountDownLatch mLatch = new CountDownLatch(1);

    public AdbRestoreFinishedLatch(BackupManagerService backupManagerService, int i) {
        this.backupManagerService = backupManagerService;
        this.mCurrentOpToken = i;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    void await() {
        try {
            this.mLatch.await(this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Slog.w(TAG, "Interrupted!");
        }
    }

    @Override
    public void execute() {
    }

    @Override
    public void operationComplete(long j) {
        this.mLatch.countDown();
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }

    @Override
    public void handleCancel(boolean z) {
        Slog.w(TAG, "adb onRestoreFinished() timed out");
        this.mLatch.countDown();
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }
}
