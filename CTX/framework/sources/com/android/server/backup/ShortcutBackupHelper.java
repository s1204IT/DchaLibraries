package com.android.server.backup;

import android.app.backup.BlobBackupHelper;
import android.content.pm.IShortcutService;
import android.os.ServiceManager;
import android.util.Slog;

public class ShortcutBackupHelper extends BlobBackupHelper {
    private static final int BLOB_VERSION = 1;
    private static final String KEY_USER_FILE = "shortcutuser.xml";
    private static final String TAG = "ShortcutBackupAgent";

    public ShortcutBackupHelper() {
        super(1, KEY_USER_FILE);
    }

    private IShortcutService getShortcutService() {
        return IShortcutService.Stub.asInterface(ServiceManager.getService("shortcut"));
    }

    @Override
    protected byte[] getBackupPayload(String str) {
        if (((str.hashCode() == -792920646 && str.equals(KEY_USER_FILE)) ? (byte) 0 : (byte) -1) == 0) {
            try {
                return getShortcutService().getBackupPayload(0);
            } catch (Exception e) {
                Slog.wtf(TAG, "Backup failed", e);
                return null;
            }
        }
        Slog.w(TAG, "Unknown key: " + str);
        return null;
    }

    @Override
    protected void applyRestoredPayload(String str, byte[] bArr) {
        if (((str.hashCode() == -792920646 && str.equals(KEY_USER_FILE)) ? (byte) 0 : (byte) -1) == 0) {
            try {
                getShortcutService().applyRestore(bArr, 0);
                return;
            } catch (Exception e) {
                Slog.wtf(TAG, "Restore failed", e);
                return;
            }
        }
        Slog.w(TAG, "Unknown key: " + str);
    }
}
