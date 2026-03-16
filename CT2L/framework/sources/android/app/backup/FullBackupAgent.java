package android.app.backup;

import android.os.ParcelFileDescriptor;
import java.io.IOException;

public class FullBackupAgent extends BackupAgent {
    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
    }
}
