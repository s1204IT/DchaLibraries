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
    public void writeNewStateDescription(ParcelFileDescriptor parcelFileDescriptor) {
        super.writeNewStateDescription(parcelFileDescriptor);
    }

    public AbsoluteFileBackupHelper(Context context, String... strArr) {
        super(context);
        this.mContext = context;
        this.mFiles = strArr;
    }

    @Override
    public void performBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) {
        performBackup_checked(parcelFileDescriptor, backupDataOutput, parcelFileDescriptor2, this.mFiles, this.mFiles);
    }

    @Override
    public void restoreEntity(BackupDataInputStream backupDataInputStream) {
        String key = backupDataInputStream.getKey();
        if (isKeyInList(key, this.mFiles)) {
            writeFile(new File(key), backupDataInputStream);
        }
    }
}
