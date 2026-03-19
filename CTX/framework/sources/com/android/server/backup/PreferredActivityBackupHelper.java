package com.android.server.backup;

import android.app.AppGlobals;
import android.app.backup.BlobBackupHelper;
import android.content.pm.IPackageManager;
import android.util.Slog;

public class PreferredActivityBackupHelper extends BlobBackupHelper {
    private static final boolean DEBUG = false;
    private static final String KEY_DEFAULT_APPS = "default-apps";
    private static final String KEY_INTENT_VERIFICATION = "intent-verification";
    private static final String KEY_PREFERRED = "preferred-activity";
    private static final int STATE_VERSION = 3;
    private static final String TAG = "PreferredBackup";

    public PreferredActivityBackupHelper() {
        super(3, KEY_PREFERRED, KEY_DEFAULT_APPS, KEY_INTENT_VERIFICATION);
    }

    @Override
    protected byte[] getBackupPayload(String str) {
        IPackageManager packageManager = AppGlobals.getPackageManager();
        byte b = -1;
        try {
            int iHashCode = str.hashCode();
            if (iHashCode != -696985986) {
                if (iHashCode != -429170260) {
                    if (iHashCode == 1336142555 && str.equals(KEY_PREFERRED)) {
                        b = 0;
                    }
                } else if (str.equals(KEY_INTENT_VERIFICATION)) {
                    b = 2;
                }
            } else if (str.equals(KEY_DEFAULT_APPS)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    return packageManager.getPreferredActivityBackup(0);
                case 1:
                    return packageManager.getDefaultAppsBackup(0);
                case 2:
                    return packageManager.getIntentFilterVerificationBackup(0);
                default:
                    Slog.w(TAG, "Unexpected backup key " + str);
                    return null;
            }
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
            int iHashCode = str.hashCode();
            if (iHashCode != -696985986) {
                if (iHashCode != -429170260) {
                    if (iHashCode == 1336142555 && str.equals(KEY_PREFERRED)) {
                        b = 0;
                    }
                } else if (str.equals(KEY_INTENT_VERIFICATION)) {
                    b = 2;
                }
            } else if (str.equals(KEY_DEFAULT_APPS)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    packageManager.restorePreferredActivities(bArr, 0);
                    break;
                case 1:
                    packageManager.restoreDefaultApps(bArr, 0);
                    break;
                case 2:
                    packageManager.restoreIntentFilterVerification(bArr, 0);
                    break;
                default:
                    Slog.w(TAG, "Unexpected restore key " + str);
                    break;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Unable to restore key " + str);
        }
    }
}
