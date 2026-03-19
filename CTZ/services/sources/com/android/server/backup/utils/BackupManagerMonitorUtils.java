package com.android.server.backup.utils;

import android.app.backup.IBackupManagerMonitor;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;

public class BackupManagerMonitorUtils {
    public static IBackupManagerMonitor monitorEvent(IBackupManagerMonitor iBackupManagerMonitor, int i, PackageInfo packageInfo, int i2, Bundle bundle) {
        if (iBackupManagerMonitor != null) {
            try {
                Bundle bundle2 = new Bundle();
                bundle2.putInt("android.app.backup.extra.LOG_EVENT_ID", i);
                bundle2.putInt("android.app.backup.extra.LOG_EVENT_CATEGORY", i2);
                if (packageInfo != null) {
                    bundle2.putString("android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", packageInfo.packageName);
                    bundle2.putInt("android.app.backup.extra.LOG_EVENT_PACKAGE_VERSION", packageInfo.versionCode);
                    bundle2.putLong("android.app.backup.extra.LOG_EVENT_PACKAGE_FULL_VERSION", packageInfo.getLongVersionCode());
                }
                if (bundle != null) {
                    bundle2.putAll(bundle);
                }
                iBackupManagerMonitor.onEvent(bundle2);
                return iBackupManagerMonitor;
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "backup manager monitor went away");
                return null;
            }
        }
        return null;
    }

    public static Bundle putMonitoringExtra(Bundle bundle, String str, String str2) {
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putString(str, str2);
        return bundle;
    }

    public static Bundle putMonitoringExtra(Bundle bundle, String str, long j) {
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putLong(str, j);
        return bundle;
    }

    public static Bundle putMonitoringExtra(Bundle bundle, String str, boolean z) {
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putBoolean(str, z);
        return bundle;
    }
}
