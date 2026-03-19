package com.android.server.backup.internal;

import android.database.ContentObserver;
import android.os.Handler;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.KeyValueBackupJob;

public class ProvisionedObserver extends ContentObserver {
    private BackupManagerService backupManagerService;

    public ProvisionedObserver(BackupManagerService backupManagerService, Handler handler) {
        super(handler);
        this.backupManagerService = backupManagerService;
    }

    @Override
    public void onChange(boolean z) {
        boolean zIsProvisioned = this.backupManagerService.isProvisioned();
        this.backupManagerService.setProvisioned(zIsProvisioned || this.backupManagerService.deviceIsProvisioned());
        synchronized (this.backupManagerService.getQueueLock()) {
            if (this.backupManagerService.isProvisioned() && !zIsProvisioned && this.backupManagerService.isEnabled()) {
                KeyValueBackupJob.schedule(this.backupManagerService.getContext(), this.backupManagerService.getConstants());
                this.backupManagerService.scheduleNextFullBackupJob(0L);
            }
        }
    }
}
