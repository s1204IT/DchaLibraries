package android.app.backup;

import android.os.ParcelFileDescriptor;
import java.io.IOException;

public class BackupAgentHelper extends BackupAgent {
    static final String TAG = "BackupAgentHelper";
    BackupHelperDispatcher mDispatcher = new BackupHelperDispatcher();

    @Override
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException {
        this.mDispatcher.performBackup(parcelFileDescriptor, backupDataOutput, parcelFileDescriptor2);
    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        this.mDispatcher.performRestore(backupDataInput, i, parcelFileDescriptor);
    }

    public BackupHelperDispatcher getDispatcher() {
        return this.mDispatcher;
    }

    public void addHelper(String str, BackupHelper backupHelper) {
        this.mDispatcher.addHelper(str, backupHelper);
    }
}
