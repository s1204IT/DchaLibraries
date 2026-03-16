package android.service.fingerprint;

import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.service.fingerprint.IFingerprintServiceReceiver;
import android.util.Log;
import android.util.Slog;

public class FingerprintManager {
    private static final boolean DEBUG = true;
    public static final int FINGERPRINT_ACQUIRED = 1;
    public static final int FINGERPRINT_ACQUIRED_GOOD = 0;
    public static final int FINGERPRINT_ACQUIRED_IMAGER_DIRTY = 4;
    public static final int FINGERPRINT_ACQUIRED_INSUFFICIENT = 2;
    public static final int FINGERPRINT_ACQUIRED_PARTIAL = 1;
    public static final int FINGERPRINT_ACQUIRED_TOO_FAST = 16;
    public static final int FINGERPRINT_ACQUIRED_TOO_SLOW = 8;
    public static final int FINGERPRINT_ERROR = -1;
    public static final int FINGERPRINT_ERROR_HW_UNAVAILABLE = 1;
    public static final int FINGERPRINT_ERROR_NO_RECEIVER = -10;
    public static final int FINGERPRINT_ERROR_NO_SPACE = 4;
    public static final int FINGERPRINT_ERROR_TIMEOUT = 3;
    public static final int FINGERPRINT_ERROR_UNABLE_TO_PROCESS = 2;
    public static final int FINGERPRINT_PROCESSED = 2;
    public static final int FINGERPRINT_TEMPLATE_ENROLLING = 3;
    public static final int FINGERPRINT_TEMPLATE_REMOVED = 4;
    private static final int MSG_ACQUIRED = 101;
    private static final int MSG_ENROLL_RESULT = 100;
    private static final int MSG_ERROR = 103;
    private static final int MSG_PROCESSED = 102;
    private static final int MSG_REMOVED = 104;
    private static final String TAG = "FingerprintManager";
    private FingerprintManagerReceiver mClientReceiver;
    private Context mContext;
    private IFingerprintService mService;
    private IBinder mToken = new Binder();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (FingerprintManager.this.mClientReceiver != null) {
                switch (msg.what) {
                    case 100:
                        FingerprintManager.this.mClientReceiver.onEnrollResult(msg.arg1, msg.arg2);
                        break;
                    case 101:
                        FingerprintManager.this.mClientReceiver.onAcquired(msg.arg1);
                        break;
                    case 102:
                        FingerprintManager.this.mClientReceiver.onProcessed(msg.arg1);
                        break;
                    case 103:
                        FingerprintManager.this.mClientReceiver.onError(msg.arg1);
                        break;
                    case 104:
                        FingerprintManager.this.mClientReceiver.onRemoved(msg.arg1);
                        break;
                }
            }
        }
    };
    private IFingerprintServiceReceiver mServiceReceiver = new IFingerprintServiceReceiver.Stub() {
        @Override
        public void onEnrollResult(int fingerprintId, int remaining) {
            FingerprintManager.this.mHandler.obtainMessage(100, fingerprintId, remaining).sendToTarget();
        }

        @Override
        public void onAcquired(int acquireInfo) {
            FingerprintManager.this.mHandler.obtainMessage(101, acquireInfo, 0).sendToTarget();
        }

        @Override
        public void onProcessed(int fingerprintId) {
            FingerprintManager.this.mHandler.obtainMessage(102, fingerprintId, 0).sendToTarget();
        }

        @Override
        public void onError(int error) {
            FingerprintManager.this.mHandler.obtainMessage(103, error, 0).sendToTarget();
        }

        @Override
        public void onRemoved(int fingerprintId) {
            FingerprintManager.this.mHandler.obtainMessage(104, fingerprintId, 0).sendToTarget();
        }
    };

    public FingerprintManager(Context context, IFingerprintService service) {
        this.mContext = context;
        this.mService = service;
        if (this.mService == null) {
            Slog.v(TAG, "FingerprintManagerService was null");
        }
    }

    public boolean enrolledAndEnabled() {
        ContentResolver res = this.mContext.getContentResolver();
        return Settings.Secure.getInt(res, "fingerprint_enabled", 0) != 0 && FingerprintUtils.getFingerprintIdsForUser(res, getCurrentUserId()).length > 0;
    }

    public void enroll(long timeout) {
        if (this.mServiceReceiver == null) {
            sendError(-10, 0, 0);
            return;
        }
        if (this.mService != null) {
            try {
                this.mService.enroll(this.mToken, timeout, getCurrentUserId());
            } catch (RemoteException e) {
                Log.v(TAG, "Remote exception while enrolling: ", e);
                sendError(1, 0, 0);
            }
        }
    }

    public void remove(int fingerprintId) {
        if (this.mServiceReceiver == null) {
            sendError(-10, 0, 0);
            return;
        }
        if (this.mService != null) {
            try {
                this.mService.remove(this.mToken, fingerprintId, getCurrentUserId());
                return;
            } catch (RemoteException e) {
                Log.v(TAG, "Remote exception during remove of fingerprintId: " + fingerprintId, e);
                return;
            }
        }
        Log.w(TAG, "remove(): Service not connected!");
        sendError(1, 0, 0);
    }

    public void startListening(FingerprintManagerReceiver receiver) {
        this.mClientReceiver = receiver;
        if (this.mService != null) {
            try {
                this.mService.startListening(this.mToken, this.mServiceReceiver, getCurrentUserId());
                return;
            } catch (RemoteException e) {
                Log.v(TAG, "Remote exception in startListening(): ", e);
                return;
            }
        }
        Log.w(TAG, "startListening(): Service not connected!");
        sendError(1, 0, 0);
    }

    private int getCurrentUserId() {
        try {
            return ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to get current user id\n");
            return -10000;
        }
    }

    public void stopListening() {
        if (this.mService != null) {
            try {
                this.mService.stopListening(this.mToken, getCurrentUserId());
                this.mClientReceiver = null;
                return;
            } catch (RemoteException e) {
                Log.v(TAG, "Remote exception in stopListening(): ", e);
                return;
            }
        }
        Log.w(TAG, "stopListening(): Service not connected!");
        sendError(1, 0, 0);
    }

    public void enrollCancel() {
        if (this.mServiceReceiver == null) {
            sendError(-10, 0, 0);
            return;
        }
        if (this.mService != null) {
            try {
                this.mService.enrollCancel(this.mToken, getCurrentUserId());
                this.mClientReceiver = null;
                return;
            } catch (RemoteException e) {
                Log.v(TAG, "Remote exception in enrollCancel(): ", e);
                sendError(1, 0, 0);
                return;
            }
        }
        Log.w(TAG, "enrollCancel(): Service not connected!");
    }

    private void sendError(int msg, int arg1, int arg2) {
        this.mHandler.obtainMessage(msg, arg1, arg2);
    }
}
