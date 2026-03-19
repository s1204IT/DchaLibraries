package android.app.backup;

import android.annotation.SystemApi;

@SystemApi
public abstract class BackupObserver {
    public void onUpdate(String str, BackupProgress backupProgress) {
    }

    public void onResult(String str, int i) {
    }

    public void backupFinished(int i) {
    }
}
