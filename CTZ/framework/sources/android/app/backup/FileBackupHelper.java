package android.app.backup;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import java.io.File;

public class FileBackupHelper extends FileBackupHelperBase implements BackupHelper {
    private static final boolean DEBUG = false;
    private static final String TAG = "FileBackupHelper";
    Context mContext;
    String[] mFiles;
    File mFilesDir;

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor parcelFileDescriptor) {
        super.writeNewStateDescription(parcelFileDescriptor);
    }

    public FileBackupHelper(Context context, String... strArr) {
        super(context);
        this.mContext = context;
        this.mFilesDir = context.getFilesDir();
        this.mFiles = strArr;
    }

    @Override
    public void performBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) {
        String[] strArr = this.mFiles;
        File filesDir = this.mContext.getFilesDir();
        int length = strArr.length;
        String[] strArr2 = new String[length];
        for (int i = 0; i < length; i++) {
            strArr2[i] = new File(filesDir, strArr[i]).getAbsolutePath();
        }
        performBackup_checked(parcelFileDescriptor, backupDataOutput, parcelFileDescriptor2, strArr2, strArr);
    }

    @Override
    public void restoreEntity(BackupDataInputStream backupDataInputStream) {
        String key = backupDataInputStream.getKey();
        if (isKeyInList(key, this.mFiles)) {
            writeFile(new File(this.mFilesDir, key), backupDataInputStream);
        }
    }
}
