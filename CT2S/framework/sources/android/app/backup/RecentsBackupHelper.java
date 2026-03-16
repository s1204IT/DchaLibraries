package android.app.backup;

import android.content.Context;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import java.io.File;

public class RecentsBackupHelper implements BackupHelper {
    private static final boolean DEBUG = false;
    private static final String RECENTS_IMAGE_DIR = "recent_images";
    private static final String RECENTS_IMAGE_KEY = "image:";
    private static final String RECENTS_IMAGE_RESTORE_DIR = "restored_recent_images";
    private static final String RECENTS_TASK_DIR = "recent_tasks";
    private static final String RECENTS_TASK_KEY = "task:";
    private static final String RECENTS_TASK_RESTORE_DIR = "restored_recent_tasks";
    private static final String TAG = "RecentsBackup";
    final String[] mRecentFiles;
    final String[] mRecentKeys;
    FileBackupHelperBase mTaskFileHelper;
    final File mSystemDir = new File(Environment.getDataDirectory(), "system");
    final File mTasksDir = new File(this.mSystemDir, RECENTS_TASK_DIR);
    final File mRestoredTasksDir = new File(this.mSystemDir, RECENTS_TASK_RESTORE_DIR);
    final File mRestoredImagesDir = new File(this.mSystemDir, RECENTS_IMAGE_RESTORE_DIR);

    public RecentsBackupHelper(Context context) {
        this.mTaskFileHelper = new FileBackupHelperBase(context);
        File[] recentFiles = this.mTasksDir.listFiles();
        if (recentFiles != null) {
            int N = recentFiles.length;
            this.mRecentKeys = new String[N];
            this.mRecentFiles = new String[N];
            for (int i = 0; i < N; i++) {
                this.mRecentKeys[i] = new String(RECENTS_TASK_KEY + recentFiles[i].getName());
                this.mRecentFiles[i] = recentFiles[i].getAbsolutePath();
            }
            return;
        }
        String[] strArr = new String[0];
        this.mRecentKeys = strArr;
        this.mRecentFiles = strArr;
    }

    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
        FileBackupHelperBase.performBackup_checked(oldState, data, newState, this.mRecentFiles, this.mRecentKeys);
    }

    @Override
    public void restoreEntity(BackupDataInputStream data) {
        String key = data.getKey();
        File output = null;
        if (key.startsWith(RECENTS_TASK_KEY)) {
            String name = key.substring(RECENTS_TASK_KEY.length());
            output = new File(this.mRestoredTasksDir, name);
            this.mRestoredTasksDir.mkdirs();
        } else if (key.startsWith(RECENTS_IMAGE_KEY)) {
            String name2 = key.substring(RECENTS_IMAGE_KEY.length());
            output = new File(this.mRestoredImagesDir, name2);
            this.mRestoredImagesDir.mkdirs();
        }
        if (output != null) {
            this.mTaskFileHelper.writeFile(output, data);
        }
    }

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState) {
        this.mTaskFileHelper.writeNewStateDescription(newState);
    }
}
