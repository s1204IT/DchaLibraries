package com.android.server.backup;

import android.app.INotificationManager;
import android.app.backup.BlobBackupHelper;
import android.content.Context;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;

public class NotificationBackupHelper extends BlobBackupHelper {
    static final int BLOB_VERSION = 1;
    static final String KEY_NOTIFICATIONS = "notifications";
    static final String TAG = "NotifBackupHelper";
    static final boolean DEBUG = Log.isLoggable(TAG, 3);

    public NotificationBackupHelper(Context context) {
        super(1, KEY_NOTIFICATIONS);
    }

    @Override
    protected byte[] getBackupPayload(String str) {
        if (!KEY_NOTIFICATIONS.equals(str)) {
            return null;
        }
        try {
            return INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE)).getBackupPayload(0);
        } catch (Exception e) {
            Slog.e(TAG, "Couldn't communicate with notification manager");
            return null;
        }
    }

    @Override
    protected void applyRestoredPayload(String str, byte[] bArr) {
        if (DEBUG) {
            Slog.v(TAG, "Got restore of " + str);
        }
        if (KEY_NOTIFICATIONS.equals(str)) {
            try {
                INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE)).applyRestore(bArr, 0);
            } catch (Exception e) {
                Slog.e(TAG, "Couldn't communicate with notification manager");
            }
        }
    }
}
