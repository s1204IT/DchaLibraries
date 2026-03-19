package android.app.backup;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import java.io.File;

public class AbsoluteFileBackupHelper extends FileBackupHelperBase implements BackupHelper {
    private static final boolean DEBUG = false;
    private static final String TAG = "AbsoluteFileBackupHelper";
    Context mContext;
    String[] mFiles;

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor fd) {
        super.writeNewStateDescription(fd);
    }

    public AbsoluteFileBackupHelper(Context context, String... files) {
        super(context);
        this.mContext = context;
        this.mFiles = files;
    }

    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
        performBackup_checked(oldState, data, newState, this.mFiles, this.mFiles);
    }

    @Override
    public void restoreEntity(BackupDataInputStream data) {
        String key = data.getKey();
        if (!isKeyInList(key, this.mFiles)) {
            return;
        }
        File f = new File(key);
        writeFile(f, data);
    }
}
