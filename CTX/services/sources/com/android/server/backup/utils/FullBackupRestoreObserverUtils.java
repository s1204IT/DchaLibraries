package com.android.server.backup.utils;

import android.app.backup.IFullBackupRestoreObserver;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;

public class FullBackupRestoreObserverUtils {
    public static IFullBackupRestoreObserver sendStartRestore(IFullBackupRestoreObserver iFullBackupRestoreObserver) {
        if (iFullBackupRestoreObserver != null) {
            try {
                iFullBackupRestoreObserver.onStartRestore();
                return iFullBackupRestoreObserver;
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: startRestore");
                return null;
            }
        }
        return iFullBackupRestoreObserver;
    }

    public static IFullBackupRestoreObserver sendOnRestorePackage(IFullBackupRestoreObserver iFullBackupRestoreObserver, String str) {
        if (iFullBackupRestoreObserver != null) {
            try {
                iFullBackupRestoreObserver.onRestorePackage(str);
                return iFullBackupRestoreObserver;
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: restorePackage");
                return null;
            }
        }
        return iFullBackupRestoreObserver;
    }

    public static IFullBackupRestoreObserver sendEndRestore(IFullBackupRestoreObserver iFullBackupRestoreObserver) {
        if (iFullBackupRestoreObserver != null) {
            try {
                iFullBackupRestoreObserver.onEndRestore();
                return iFullBackupRestoreObserver;
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: endRestore");
                return null;
            }
        }
        return iFullBackupRestoreObserver;
    }
}
