package android.app.backup;

import android.os.ParcelFileDescriptor;

public class FullBackupDataOutput {
    private BackupDataOutput mData;

    public FullBackupDataOutput(ParcelFileDescriptor fd) {
        this.mData = new BackupDataOutput(fd.getFileDescriptor());
    }

    public BackupDataOutput getData() {
        return this.mData;
    }
}
