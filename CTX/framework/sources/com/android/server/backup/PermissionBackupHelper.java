package com.android.server.backup;

import android.app.AppGlobals;
import android.app.backup.BlobBackupHelper;
import android.content.pm.IPackageManager;
import android.util.Slog;

public class PermissionBackupHelper extends BlobBackupHelper {
    private static final boolean DEBUG = false;
    private static final String KEY_PERMISSIONS = "permissions";
    private static final int STATE_VERSION = 1;
    private static final String TAG = "PermissionBackup";

    public PermissionBackupHelper() {
        super(1, "permissions");
    }

    @Override
    protected byte[] getBackupPayload(String str) {
        IPackageManager packageManager = AppGlobals.getPackageManager();
        byte b = -1;
        try {
            if (str.hashCode() == 1133704324 && str.equals("permissions")) {
                b = 0;
            }
            if (b == 0) {
                return packageManager.getPermissionGrantBackup(0);
            }
            Slog.w(TAG, "Unexpected backup key " + str);
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "Unable to store payload " + str);
            return null;
        }
    }

    @Override
    protected void applyRestoredPayload(String str, byte[] bArr) {
        IPackageManager packageManager = AppGlobals.getPackageManager();
        byte b = -1;
        try {
            if (str.hashCode() == 1133704324 && str.equals("permissions")) {
                b = 0;
            }
            if (b == 0) {
                packageManager.restorePermissionGrants(bArr, 0);
                return;
            }
            Slog.w(TAG, "Unexpected restore key " + str);
        } catch (Exception e) {
            Slog.w(TAG, "Unable to restore key " + str);
        }
    }
}
