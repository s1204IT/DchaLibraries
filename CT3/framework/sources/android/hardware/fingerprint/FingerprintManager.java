package android.hardware.fingerprint;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.hardware.fingerprint.IFingerprintServiceLockoutResetCallback;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.keystore.AndroidKeyStoreProvider;
import android.util.Log;
import android.util.Slog;
import java.security.Signature;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.Mac;

public class FingerprintManager {
    private static final boolean DEBUG = true;
    public static final int FINGERPRINT_ACQUIRED_GOOD = 0;
    public static final int FINGERPRINT_ACQUIRED_IMAGER_DIRTY = 3;
    public static final int FINGERPRINT_ACQUIRED_INSUFFICIENT = 2;
    public static final int FINGERPRINT_ACQUIRED_PARTIAL = 1;
    public static final int FINGERPRINT_ACQUIRED_TOO_FAST = 5;
    public static final int FINGERPRINT_ACQUIRED_TOO_SLOW = 4;
    public static final int FINGERPRINT_ACQUIRED_VENDOR_BASE = 1000;
    public static final int FINGERPRINT_ERROR_CANCELED = 5;
    public static final int FINGERPRINT_ERROR_HW_UNAVAILABLE = 1;
    public static final int FINGERPRINT_ERROR_LOCKOUT = 7;
    public static final int FINGERPRINT_ERROR_NO_SPACE = 4;
    public static final int FINGERPRINT_ERROR_TIMEOUT = 3;
    public static final int FINGERPRINT_ERROR_UNABLE_TO_PROCESS = 2;
    public static final int FINGERPRINT_ERROR_UNABLE_TO_REMOVE = 6;
    public static final int FINGERPRINT_ERROR_VENDOR_BASE = 1000;
    private static final int MSG_ACQUIRED = 101;
    private static final int MSG_AUTHENTICATION_FAILED = 103;
    private static final int MSG_AUTHENTICATION_SUCCEEDED = 102;
    private static final int MSG_ENROLL_RESULT = 100;
    private static final int MSG_ERROR = 104;
    private static final int MSG_REMOVED = 105;
    private static final String TAG = "FingerprintManager";
    private AuthenticationCallback mAuthenticationCallback;
    private Context mContext;
    private CryptoObject mCryptoObject;
    private EnrollmentCallback mEnrollmentCallback;
    private Handler mHandler;
    private RemovalCallback mRemovalCallback;
    private Fingerprint mRemovalFingerprint;
    private IFingerprintService mService;
    private IBinder mToken = new Binder();
    private IFingerprintServiceReceiver mServiceReceiver = new IFingerprintServiceReceiver.Stub() {
        @Override
        public void onEnrollResult(long deviceId, int fingerId, int groupId, int remaining) {
            FingerprintManager.this.mHandler.obtainMessage(100, remaining, 0, new Fingerprint(null, groupId, fingerId, deviceId)).sendToTarget();
        }

        @Override
        public void onAcquired(long deviceId, int acquireInfo) {
            FingerprintManager.this.mHandler.obtainMessage(101, acquireInfo, 0, Long.valueOf(deviceId)).sendToTarget();
        }

        @Override
        public void onAuthenticationSucceeded(long deviceId, Fingerprint fp, int userId) {
            FingerprintManager.this.mHandler.obtainMessage(102, userId, 0, fp).sendToTarget();
        }

        @Override
        public void onAuthenticationFailed(long deviceId) {
            FingerprintManager.this.mHandler.obtainMessage(103).sendToTarget();
        }

        @Override
        public void onError(long deviceId, int error) {
            FingerprintManager.this.mHandler.obtainMessage(104, error, 0, Long.valueOf(deviceId)).sendToTarget();
        }

        @Override
        public void onRemoved(long deviceId, int fingerId, int groupId) {
            FingerprintManager.this.mHandler.obtainMessage(105, fingerId, groupId, Long.valueOf(deviceId)).sendToTarget();
        }
    };

    private class OnEnrollCancelListener implements CancellationSignal.OnCancelListener {
        OnEnrollCancelListener(FingerprintManager this$0, OnEnrollCancelListener onEnrollCancelListener) {
            this();
        }

        private OnEnrollCancelListener() {
        }

        @Override
        public void onCancel() {
            FingerprintManager.this.cancelEnrollment();
        }
    }

    private class OnAuthenticationCancelListener implements CancellationSignal.OnCancelListener {
        private CryptoObject mCrypto;

        public OnAuthenticationCancelListener(CryptoObject crypto) {
            this.mCrypto = crypto;
        }

        @Override
        public void onCancel() {
            FingerprintManager.this.cancelAuthentication(this.mCrypto);
        }
    }

    public static final class CryptoObject {
        private final Object mCrypto;

        public CryptoObject(Signature signature) {
            this.mCrypto = signature;
        }

        public CryptoObject(Cipher cipher) {
            this.mCrypto = cipher;
        }

        public CryptoObject(Mac mac) {
            this.mCrypto = mac;
        }

        public Signature getSignature() {
            if (this.mCrypto instanceof Signature) {
                return (Signature) this.mCrypto;
            }
            return null;
        }

        public Cipher getCipher() {
            if (this.mCrypto instanceof Cipher) {
                return (Cipher) this.mCrypto;
            }
            return null;
        }

        public Mac getMac() {
            if (this.mCrypto instanceof Mac) {
                return (Mac) this.mCrypto;
            }
            return null;
        }

        public long getOpId() {
            if (this.mCrypto != null) {
                return AndroidKeyStoreProvider.getKeyStoreOperationHandle(this.mCrypto);
            }
            return 0L;
        }
    }

    public static class AuthenticationResult {
        private CryptoObject mCryptoObject;
        private Fingerprint mFingerprint;
        private int mUserId;

        public AuthenticationResult(CryptoObject crypto, Fingerprint fingerprint, int userId) {
            this.mCryptoObject = crypto;
            this.mFingerprint = fingerprint;
            this.mUserId = userId;
        }

        public CryptoObject getCryptoObject() {
            return this.mCryptoObject;
        }

        public Fingerprint getFingerprint() {
            return this.mFingerprint;
        }

        public int getUserId() {
            return this.mUserId;
        }
    }

    public static abstract class AuthenticationCallback {
        public void onAuthenticationError(int errorCode, CharSequence errString) {
        }

        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        }

        public void onAuthenticationSucceeded(AuthenticationResult result) {
        }

        public void onAuthenticationFailed() {
        }

        public void onAuthenticationAcquired(int acquireInfo) {
        }
    }

    public static abstract class EnrollmentCallback {
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
        }

        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        }

        public void onEnrollmentProgress(int remaining) {
        }
    }

    public static abstract class RemovalCallback {
        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
        }

        public void onRemovalSucceeded(Fingerprint fingerprint) {
        }
    }

    public static abstract class LockoutResetCallback {
        public void onLockoutReset() {
        }
    }

    public void authenticate(CryptoObject crypto, CancellationSignal cancel, int flags, AuthenticationCallback callback, Handler handler) {
        authenticate(crypto, cancel, flags, callback, handler, UserHandle.myUserId());
    }

    private void useHandler(Handler handler) {
        MyHandler myHandler = null;
        if (handler != null) {
            this.mHandler = new MyHandler(this, handler.getLooper(), myHandler);
        } else {
            if (this.mHandler.getLooper() == this.mContext.getMainLooper()) {
                return;
            }
            this.mHandler = new MyHandler(this, this.mContext.getMainLooper(), myHandler);
        }
    }

    public void authenticate(CryptoObject crypto, CancellationSignal cancel, int flags, AuthenticationCallback callback, Handler handler, int userId) {
        if (callback == null) {
            throw new IllegalArgumentException("Must supply an authentication callback");
        }
        if (cancel != null) {
            if (cancel.isCanceled()) {
                Log.w(TAG, "authentication already canceled");
                return;
            }
            cancel.setOnCancelListener(new OnAuthenticationCancelListener(crypto));
        }
        if (this.mService == null) {
            return;
        }
        try {
            useHandler(handler);
            this.mAuthenticationCallback = callback;
            this.mCryptoObject = crypto;
            long sessionId = crypto != null ? crypto.getOpId() : 0L;
            this.mService.authenticate(this.mToken, sessionId, userId, this.mServiceReceiver, flags, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception while authenticating: ", e);
            if (callback == null) {
                return;
            }
            callback.onAuthenticationError(1, getErrorString(1));
        }
    }

    public void enroll(byte[] token, CancellationSignal cancel, int flags, int userId, EnrollmentCallback callback) {
        OnEnrollCancelListener onEnrollCancelListener = null;
        if (userId == -2) {
            userId = getCurrentUserId();
        }
        if (callback == null) {
            throw new IllegalArgumentException("Must supply an enrollment callback");
        }
        if (cancel != null) {
            if (cancel.isCanceled()) {
                Log.w(TAG, "enrollment already canceled");
                return;
            }
            cancel.setOnCancelListener(new OnEnrollCancelListener(this, onEnrollCancelListener));
        }
        if (this.mService == null) {
            return;
        }
        try {
            this.mEnrollmentCallback = callback;
            this.mService.enroll(this.mToken, token, userId, this.mServiceReceiver, flags, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception in enroll: ", e);
            if (callback == null) {
                return;
            }
            callback.onEnrollmentError(1, getErrorString(1));
        }
    }

    public long preEnroll() {
        if (this.mService == null) {
            return 0L;
        }
        try {
            long result = this.mService.preEnroll(this.mToken);
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int postEnroll() {
        if (this.mService == null) {
            return 0;
        }
        try {
            int result = this.mService.postEnroll(this.mToken);
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setActiveUser(int userId) {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.setActiveUser(userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void remove(Fingerprint fp, int userId, RemovalCallback callback) {
        if (this.mService == null) {
            return;
        }
        try {
            this.mRemovalCallback = callback;
            this.mRemovalFingerprint = fp;
            this.mService.remove(this.mToken, fp.getFingerId(), fp.getGroupId(), userId, this.mServiceReceiver);
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception in remove: ", e);
            if (callback == null) {
                return;
            }
            callback.onRemovalError(fp, 1, getErrorString(1));
        }
    }

    public void rename(int fpId, int userId, String newName) {
        if (this.mService != null) {
            try {
                this.mService.rename(fpId, userId, newName);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "rename(): Service not connected!");
    }

    public List<Fingerprint> getEnrolledFingerprints(int userId) {
        if (this.mService == null) {
            return null;
        }
        try {
            return this.mService.getEnrolledFingerprints(userId, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<Fingerprint> getEnrolledFingerprints() {
        return getEnrolledFingerprints(UserHandle.myUserId());
    }

    public boolean hasEnrolledFingerprints() {
        if (this.mService != null) {
            try {
                return this.mService.hasEnrolledFingerprints(UserHandle.myUserId(), this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean hasEnrolledFingerprints(int userId) {
        if (this.mService != null) {
            try {
                return this.mService.hasEnrolledFingerprints(userId, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean isHardwareDetected() {
        if (this.mService != null) {
            try {
                return this.mService.isHardwareDetected(0L, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "isFingerprintHardwareDetected(): Service not connected!");
        return false;
    }

    public long getAuthenticatorId() {
        if (this.mService != null) {
            try {
                return this.mService.getAuthenticatorId(this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "getAuthenticatorId(): Service not connected!");
        return 0L;
    }

    public void resetTimeout(byte[] token) {
        if (this.mService != null) {
            try {
                this.mService.resetTimeout(token);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "resetTimeout(): Service not connected!");
    }

    public void addLockoutResetCallback(final LockoutResetCallback callback) {
        if (this.mService != null) {
            try {
                final PowerManager powerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
                this.mService.addLockoutResetCallback(new IFingerprintServiceLockoutResetCallback.Stub() {
                    @Override
                    public void onLockoutReset(long deviceId) throws RemoteException {
                        final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(1, "lockoutResetCallback");
                        wakeLock.acquire();
                        Handler handler = FingerprintManager.this.mHandler;
                        final LockoutResetCallback lockoutResetCallback = callback;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    lockoutResetCallback.onLockoutReset();
                                } finally {
                                    wakeLock.release();
                                }
                            }
                        });
                    }
                });
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "addLockoutResetCallback(): Service not connected!");
    }

    private class MyHandler extends Handler {
        MyHandler(FingerprintManager this$0, Context context, MyHandler myHandler) {
            this(context);
        }

        MyHandler(FingerprintManager this$0, Looper looper, MyHandler myHandler) {
            this(looper);
        }

        private MyHandler(Context context) {
            super(context.getMainLooper());
        }

        private MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    sendEnrollResult((Fingerprint) msg.obj, msg.arg1);
                    break;
                case 101:
                    sendAcquiredResult(((Long) msg.obj).longValue(), msg.arg1);
                    break;
                case 102:
                    sendAuthenticatedSucceeded((Fingerprint) msg.obj, msg.arg1);
                    break;
                case 103:
                    sendAuthenticatedFailed();
                    break;
                case 104:
                    sendErrorResult(((Long) msg.obj).longValue(), msg.arg1);
                    break;
                case 105:
                    sendRemovedResult(((Long) msg.obj).longValue(), msg.arg1, msg.arg2);
                    break;
            }
        }

        private void sendRemovedResult(long deviceId, int fingerId, int groupId) {
            if (FingerprintManager.this.mRemovalCallback == null) {
                return;
            }
            int reqFingerId = FingerprintManager.this.mRemovalFingerprint.getFingerId();
            int reqGroupId = FingerprintManager.this.mRemovalFingerprint.getGroupId();
            if (reqFingerId != 0 && fingerId != 0 && fingerId != reqFingerId) {
                Log.w(FingerprintManager.TAG, "Finger id didn't match: " + fingerId + " != " + reqFingerId);
            } else if (groupId != reqGroupId) {
                Log.w(FingerprintManager.TAG, "Group id didn't match: " + groupId + " != " + reqGroupId);
            } else {
                FingerprintManager.this.mRemovalCallback.onRemovalSucceeded(new Fingerprint(null, groupId, fingerId, deviceId));
            }
        }

        private void sendErrorResult(long deviceId, int errMsgId) {
            if (FingerprintManager.this.mEnrollmentCallback != null) {
                FingerprintManager.this.mEnrollmentCallback.onEnrollmentError(errMsgId, FingerprintManager.this.getErrorString(errMsgId));
            } else if (FingerprintManager.this.mAuthenticationCallback != null) {
                FingerprintManager.this.mAuthenticationCallback.onAuthenticationError(errMsgId, FingerprintManager.this.getErrorString(errMsgId));
            } else {
                if (FingerprintManager.this.mRemovalCallback == null) {
                    return;
                }
                FingerprintManager.this.mRemovalCallback.onRemovalError(FingerprintManager.this.mRemovalFingerprint, errMsgId, FingerprintManager.this.getErrorString(errMsgId));
            }
        }

        private void sendEnrollResult(Fingerprint fp, int remaining) {
            if (FingerprintManager.this.mEnrollmentCallback == null) {
                return;
            }
            FingerprintManager.this.mEnrollmentCallback.onEnrollmentProgress(remaining);
        }

        private void sendAuthenticatedSucceeded(Fingerprint fp, int userId) {
            if (FingerprintManager.this.mAuthenticationCallback == null) {
                return;
            }
            AuthenticationResult result = new AuthenticationResult(FingerprintManager.this.mCryptoObject, fp, userId);
            FingerprintManager.this.mAuthenticationCallback.onAuthenticationSucceeded(result);
        }

        private void sendAuthenticatedFailed() {
            if (FingerprintManager.this.mAuthenticationCallback == null) {
                return;
            }
            FingerprintManager.this.mAuthenticationCallback.onAuthenticationFailed();
        }

        private void sendAcquiredResult(long deviceId, int acquireInfo) {
            if (FingerprintManager.this.mAuthenticationCallback != null) {
                FingerprintManager.this.mAuthenticationCallback.onAuthenticationAcquired(acquireInfo);
            }
            String msg = FingerprintManager.this.getAcquiredString(acquireInfo);
            if (msg == null) {
                return;
            }
            if (FingerprintManager.this.mEnrollmentCallback != null) {
                FingerprintManager.this.mEnrollmentCallback.onEnrollmentHelp(acquireInfo, msg);
            } else {
                if (FingerprintManager.this.mAuthenticationCallback == null) {
                    return;
                }
                FingerprintManager.this.mAuthenticationCallback.onAuthenticationHelp(acquireInfo, msg);
            }
        }
    }

    public FingerprintManager(Context context, IFingerprintService service) {
        MyHandler myHandler = null;
        this.mContext = context;
        this.mService = service;
        if (this.mService == null) {
            Slog.v(TAG, "FingerprintManagerService was null");
        }
        this.mHandler = new MyHandler(this, context, myHandler);
    }

    private int getCurrentUserId() {
        try {
            return ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void cancelEnrollment() {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.cancelEnrollment(this.mToken);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void cancelAuthentication(CryptoObject cryptoObject) {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.cancelAuthentication(this.mToken, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private String getErrorString(int errMsg) {
        switch (errMsg) {
            case 1:
                return this.mContext.getString(17039842);
            case 2:
                return this.mContext.getString(17039847);
            case 3:
                return this.mContext.getString(17039844);
            case 4:
                return this.mContext.getString(17039843);
            case 5:
                return this.mContext.getString(17039845);
            case 6:
            default:
                if (errMsg >= 1000) {
                    int msgNumber = errMsg - 1000;
                    String[] msgArray = this.mContext.getResources().getStringArray(17236052);
                    if (msgNumber < msgArray.length) {
                        return msgArray[msgNumber];
                    }
                    return null;
                }
                return null;
            case 7:
                return this.mContext.getString(17039846);
        }
    }

    private String getAcquiredString(int acquireInfo) {
        switch (acquireInfo) {
            case 0:
                return null;
            case 1:
                return this.mContext.getString(17039837);
            case 2:
                return this.mContext.getString(17039838);
            case 3:
                return this.mContext.getString(17039839);
            case 4:
                return this.mContext.getString(17039841);
            case 5:
                return this.mContext.getString(17039840);
            default:
                if (acquireInfo >= 1000) {
                    int msgNumber = acquireInfo - 1000;
                    String[] msgArray = this.mContext.getResources().getStringArray(17236051);
                    if (msgNumber < msgArray.length) {
                        return msgArray[msgNumber];
                    }
                }
                return null;
        }
    }
}
