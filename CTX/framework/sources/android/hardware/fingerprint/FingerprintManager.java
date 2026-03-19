package android.hardware.fingerprint;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricFingerprintConstants;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.IFingerprintServiceLockoutResetCallback;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.R;
import java.security.Signature;
import java.util.List;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.Mac;

@Deprecated
public class FingerprintManager implements BiometricFingerprintConstants {
    private static final boolean DEBUG = true;
    private static final int MSG_ACQUIRED = 101;
    private static final int MSG_AUTHENTICATION_FAILED = 103;
    private static final int MSG_AUTHENTICATION_SUCCEEDED = 102;
    private static final int MSG_ENROLL_RESULT = 100;
    private static final int MSG_ENUMERATED = 106;
    private static final int MSG_ERROR = 104;
    private static final int MSG_REMOVED = 105;
    private static final String TAG = "FingerprintManager";
    private BiometricAuthenticator.AuthenticationCallback mAuthenticationCallback;
    private Context mContext;
    private android.hardware.biometrics.CryptoObject mCryptoObject;
    private EnrollmentCallback mEnrollmentCallback;
    private EnumerateCallback mEnumerateCallback;
    private Executor mExecutor;
    private Handler mHandler;
    private RemovalCallback mRemovalCallback;
    private Fingerprint mRemovalFingerprint;
    private IFingerprintService mService;
    private IBinder mToken = new Binder();
    private IFingerprintServiceReceiver mServiceReceiver = new AnonymousClass2();

    private class OnEnrollCancelListener implements CancellationSignal.OnCancelListener {
        private OnEnrollCancelListener() {
        }

        OnEnrollCancelListener(FingerprintManager fingerprintManager, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        public void onCancel() {
            FingerprintManager.this.cancelEnrollment();
        }
    }

    private class OnAuthenticationCancelListener implements CancellationSignal.OnCancelListener {
        private android.hardware.biometrics.CryptoObject mCrypto;

        public OnAuthenticationCancelListener(android.hardware.biometrics.CryptoObject cryptoObject) {
            this.mCrypto = cryptoObject;
        }

        @Override
        public void onCancel() {
            FingerprintManager.this.cancelAuthentication(this.mCrypto);
        }
    }

    @Deprecated
    public static final class CryptoObject extends android.hardware.biometrics.CryptoObject {
        public CryptoObject(Signature signature) {
            super(signature);
        }

        public CryptoObject(Cipher cipher) {
            super(cipher);
        }

        public CryptoObject(Mac mac) {
            super(mac);
        }

        @Override
        public Signature getSignature() {
            return super.getSignature();
        }

        @Override
        public Cipher getCipher() {
            return super.getCipher();
        }

        @Override
        public Mac getMac() {
            return super.getMac();
        }
    }

    @Deprecated
    public static class AuthenticationResult {
        private CryptoObject mCryptoObject;
        private Fingerprint mFingerprint;
        private int mUserId;

        public AuthenticationResult(CryptoObject cryptoObject, Fingerprint fingerprint, int i) {
            this.mCryptoObject = cryptoObject;
            this.mFingerprint = fingerprint;
            this.mUserId = i;
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

    @Deprecated
    public static abstract class AuthenticationCallback extends BiometricAuthenticator.AuthenticationCallback {
        @Override
        public void onAuthenticationError(int i, CharSequence charSequence) {
        }

        @Override
        public void onAuthenticationHelp(int i, CharSequence charSequence) {
        }

        public void onAuthenticationSucceeded(AuthenticationResult authenticationResult) {
        }

        @Override
        public void onAuthenticationFailed() {
        }

        @Override
        public void onAuthenticationAcquired(int i) {
        }

        @Override
        public void onAuthenticationSucceeded(BiometricAuthenticator.AuthenticationResult authenticationResult) {
            onAuthenticationSucceeded(new AuthenticationResult((CryptoObject) authenticationResult.getCryptoObject(), (Fingerprint) authenticationResult.getId(), authenticationResult.getUserId()));
        }
    }

    public static abstract class EnrollmentCallback {
        public void onEnrollmentError(int i, CharSequence charSequence) {
        }

        public void onEnrollmentHelp(int i, CharSequence charSequence) {
        }

        public void onEnrollmentProgress(int i) {
        }
    }

    public static abstract class RemovalCallback {
        public void onRemovalError(Fingerprint fingerprint, int i, CharSequence charSequence) {
        }

        public void onRemovalSucceeded(Fingerprint fingerprint, int i) {
        }
    }

    public static abstract class EnumerateCallback {
        public void onEnumerateError(int i, CharSequence charSequence) {
        }

        public void onEnumerate(Fingerprint fingerprint) {
        }
    }

    public static abstract class LockoutResetCallback {
        public void onLockoutReset() {
        }
    }

    @Deprecated
    public void authenticate(CryptoObject cryptoObject, CancellationSignal cancellationSignal, int i, AuthenticationCallback authenticationCallback, Handler handler) {
        authenticate(cryptoObject, cancellationSignal, i, authenticationCallback, handler, this.mContext.getUserId());
    }

    private void useHandler(Handler handler) {
        AnonymousClass1 anonymousClass1 = null;
        if (handler != null) {
            this.mHandler = new MyHandler(this, handler.getLooper(), anonymousClass1);
        } else if (this.mHandler.getLooper() != this.mContext.getMainLooper()) {
            this.mHandler = new MyHandler(this, this.mContext.getMainLooper(), anonymousClass1);
        }
    }

    public void authenticate(CryptoObject cryptoObject, CancellationSignal cancellationSignal, int i, AuthenticationCallback authenticationCallback, Handler handler, int i2) {
        if (authenticationCallback == null) {
            throw new IllegalArgumentException("Must supply an authentication callback");
        }
        if (cancellationSignal != null) {
            if (cancellationSignal.isCanceled()) {
                Slog.w(TAG, "authentication already canceled");
                return;
            }
            cancellationSignal.setOnCancelListener(new OnAuthenticationCancelListener(cryptoObject));
        }
        if (this.mService != null) {
            try {
                useHandler(handler);
                this.mAuthenticationCallback = authenticationCallback;
                this.mCryptoObject = cryptoObject;
                this.mService.authenticate(this.mToken, cryptoObject != null ? cryptoObject.getOpId() : 0L, i2, this.mServiceReceiver, i, this.mContext.getOpPackageName(), null, null);
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception while authenticating: ", e);
                if (authenticationCallback != null) {
                    authenticationCallback.onAuthenticationError(1, getErrorString(1, 0));
                }
            }
        }
    }

    private void authenticate(int i, android.hardware.biometrics.CryptoObject cryptoObject, CancellationSignal cancellationSignal, Bundle bundle, Executor executor, IBiometricPromptReceiver iBiometricPromptReceiver, final BiometricAuthenticator.AuthenticationCallback authenticationCallback) {
        this.mCryptoObject = cryptoObject;
        if (cancellationSignal.isCanceled()) {
            Slog.w(TAG, "authentication already canceled");
            return;
        }
        cancellationSignal.setOnCancelListener(new OnAuthenticationCancelListener(cryptoObject));
        if (this.mService != null) {
            try {
                this.mExecutor = executor;
                this.mAuthenticationCallback = authenticationCallback;
                this.mService.authenticate(this.mToken, cryptoObject != null ? cryptoObject.getOpId() : 0L, i, this.mServiceReceiver, 0, this.mContext.getOpPackageName(), bundle, iBiometricPromptReceiver);
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception while authenticating", e);
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        authenticationCallback.onAuthenticationError(1, this.f$0.getErrorString(1, 0));
                    }
                });
            }
        }
    }

    public void authenticate(CancellationSignal cancellationSignal, Bundle bundle, Executor executor, IBiometricPromptReceiver iBiometricPromptReceiver, BiometricAuthenticator.AuthenticationCallback authenticationCallback) {
        if (cancellationSignal == null) {
            throw new IllegalArgumentException("Must supply a cancellation signal");
        }
        if (bundle == null) {
            throw new IllegalArgumentException("Must supply a bundle");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must supply an executor");
        }
        if (iBiometricPromptReceiver == null) {
            throw new IllegalArgumentException("Must supply a receiver");
        }
        if (authenticationCallback == null) {
            throw new IllegalArgumentException("Must supply a calback");
        }
        authenticate(this.mContext.getUserId(), null, cancellationSignal, bundle, executor, iBiometricPromptReceiver, authenticationCallback);
    }

    public void authenticate(android.hardware.biometrics.CryptoObject cryptoObject, CancellationSignal cancellationSignal, Bundle bundle, Executor executor, IBiometricPromptReceiver iBiometricPromptReceiver, BiometricAuthenticator.AuthenticationCallback authenticationCallback) {
        if (cryptoObject == null) {
            throw new IllegalArgumentException("Must supply a crypto object");
        }
        if (cancellationSignal == null) {
            throw new IllegalArgumentException("Must supply a cancellation signal");
        }
        if (bundle == null) {
            throw new IllegalArgumentException("Must supply a bundle");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must supply an executor");
        }
        if (iBiometricPromptReceiver == null) {
            throw new IllegalArgumentException("Must supply a receiver");
        }
        if (authenticationCallback == null) {
            throw new IllegalArgumentException("Must supply a callback");
        }
        authenticate(this.mContext.getUserId(), cryptoObject, cancellationSignal, bundle, executor, iBiometricPromptReceiver, authenticationCallback);
    }

    public void enroll(byte[] bArr, CancellationSignal cancellationSignal, int i, int i2, EnrollmentCallback enrollmentCallback) {
        if (i2 == -2) {
            i2 = getCurrentUserId();
        }
        int i3 = i2;
        if (enrollmentCallback == null) {
            throw new IllegalArgumentException("Must supply an enrollment callback");
        }
        if (cancellationSignal != null) {
            if (cancellationSignal.isCanceled()) {
                Slog.w(TAG, "enrollment already canceled");
                return;
            }
            cancellationSignal.setOnCancelListener(new OnEnrollCancelListener(this, null));
        }
        if (this.mService != null) {
            try {
                this.mEnrollmentCallback = enrollmentCallback;
                this.mService.enroll(this.mToken, bArr, i3, this.mServiceReceiver, i, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception in enroll: ", e);
                if (enrollmentCallback != null) {
                    enrollmentCallback.onEnrollmentError(1, getErrorString(1, 0));
                }
            }
        }
    }

    public long preEnroll() {
        if (this.mService != null) {
            try {
                return this.mService.preEnroll(this.mToken);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0L;
    }

    public int postEnroll() {
        if (this.mService != null) {
            try {
                return this.mService.postEnroll(this.mToken);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setActiveUser(int i) {
        if (this.mService != null) {
            try {
                this.mService.setActiveUser(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void remove(Fingerprint fingerprint, int i, RemovalCallback removalCallback) {
        if (this.mService != null) {
            try {
                this.mRemovalCallback = removalCallback;
                this.mRemovalFingerprint = fingerprint;
                this.mService.remove(this.mToken, fingerprint.getFingerId(), fingerprint.getGroupId(), i, this.mServiceReceiver);
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception in remove: ", e);
                if (removalCallback != null) {
                    removalCallback.onRemovalError(fingerprint, 1, getErrorString(1, 0));
                }
            }
        }
    }

    public void enumerate(int i, EnumerateCallback enumerateCallback) {
        if (this.mService != null) {
            try {
                this.mEnumerateCallback = enumerateCallback;
                this.mService.enumerate(this.mToken, i, this.mServiceReceiver);
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception in enumerate: ", e);
                if (enumerateCallback != null) {
                    enumerateCallback.onEnumerateError(1, getErrorString(1, 0));
                }
            }
        }
    }

    public void rename(int i, int i2, String str) {
        if (this.mService != null) {
            try {
                this.mService.rename(i, i2, str);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "rename(): Service not connected!");
    }

    public List<Fingerprint> getEnrolledFingerprints(int i) {
        if (this.mService != null) {
            try {
                return this.mService.getEnrolledFingerprints(i, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public List<Fingerprint> getEnrolledFingerprints() {
        return getEnrolledFingerprints(this.mContext.getUserId());
    }

    @Deprecated
    public boolean hasEnrolledFingerprints() {
        if (this.mService != null) {
            try {
                return this.mService.hasEnrolledFingerprints(this.mContext.getUserId(), this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean hasEnrolledFingerprints(int i) {
        if (this.mService != null) {
            try {
                return this.mService.hasEnrolledFingerprints(i, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    @Deprecated
    public boolean isHardwareDetected() {
        if (this.mService != null) {
            try {
                return this.mService.isHardwareDetected(0L, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "isFingerprintHardwareDetected(): Service not connected!");
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
        Slog.w(TAG, "getAuthenticatorId(): Service not connected!");
        return 0L;
    }

    public void resetTimeout(byte[] bArr) {
        if (this.mService != null) {
            try {
                this.mService.resetTimeout(bArr);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "resetTimeout(): Service not connected!");
    }

    public void addLockoutResetCallback(LockoutResetCallback lockoutResetCallback) {
        if (this.mService != null) {
            try {
                this.mService.addLockoutResetCallback(new AnonymousClass1((PowerManager) this.mContext.getSystemService(PowerManager.class), lockoutResetCallback));
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "addLockoutResetCallback(): Service not connected!");
    }

    class AnonymousClass1 extends IFingerprintServiceLockoutResetCallback.Stub {
        final LockoutResetCallback val$callback;
        final PowerManager val$powerManager;

        AnonymousClass1(PowerManager powerManager, LockoutResetCallback lockoutResetCallback) {
            this.val$powerManager = powerManager;
            this.val$callback = lockoutResetCallback;
        }

        @Override
        public void onLockoutReset(long j, IRemoteCallback iRemoteCallback) throws RemoteException {
            try {
                final PowerManager.WakeLock wakeLockNewWakeLock = this.val$powerManager.newWakeLock(1, "lockoutResetCallback");
                wakeLockNewWakeLock.acquire();
                Handler handler = FingerprintManager.this.mHandler;
                final LockoutResetCallback lockoutResetCallback = this.val$callback;
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        FingerprintManager.AnonymousClass1.lambda$onLockoutReset$0(lockoutResetCallback, wakeLockNewWakeLock);
                    }
                });
            } finally {
                iRemoteCallback.sendResult(null);
            }
        }

        static void lambda$onLockoutReset$0(LockoutResetCallback lockoutResetCallback, PowerManager.WakeLock wakeLock) {
            try {
                lockoutResetCallback.onLockoutReset();
            } finally {
                wakeLock.release();
            }
        }
    }

    private class MyHandler extends Handler {
        MyHandler(FingerprintManager fingerprintManager, Context context, AnonymousClass1 anonymousClass1) {
            this(context);
        }

        MyHandler(FingerprintManager fingerprintManager, Looper looper, AnonymousClass1 anonymousClass1) {
            this(looper);
        }

        private MyHandler(Context context) {
            super(context.getMainLooper());
        }

        private MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 100:
                    sendEnrollResult((Fingerprint) message.obj, message.arg1);
                    break;
                case 101:
                    FingerprintManager.this.sendAcquiredResult(((Long) message.obj).longValue(), message.arg1, message.arg2);
                    break;
                case 102:
                    FingerprintManager.this.sendAuthenticatedSucceeded((Fingerprint) message.obj, message.arg1);
                    break;
                case 103:
                    FingerprintManager.this.sendAuthenticatedFailed();
                    break;
                case 104:
                    FingerprintManager.this.sendErrorResult(((Long) message.obj).longValue(), message.arg1, message.arg2);
                    break;
                case 105:
                    sendRemovedResult((Fingerprint) message.obj, message.arg1);
                    break;
                case 106:
                    sendEnumeratedResult(((Long) message.obj).longValue(), message.arg1, message.arg2);
                    break;
            }
        }

        private void sendRemovedResult(Fingerprint fingerprint, int i) {
            if (FingerprintManager.this.mRemovalCallback == null) {
                return;
            }
            if (fingerprint == null) {
                Slog.e(FingerprintManager.TAG, "Received MSG_REMOVED, but fingerprint is null");
                return;
            }
            int fingerId = fingerprint.getFingerId();
            int fingerId2 = FingerprintManager.this.mRemovalFingerprint.getFingerId();
            if (fingerId2 != 0 && fingerId != 0 && fingerId != fingerId2) {
                Slog.w(FingerprintManager.TAG, "Finger id didn't match: " + fingerId + " != " + fingerId2);
                return;
            }
            int groupId = fingerprint.getGroupId();
            int groupId2 = FingerprintManager.this.mRemovalFingerprint.getGroupId();
            if (groupId == groupId2) {
                FingerprintManager.this.mRemovalCallback.onRemovalSucceeded(fingerprint, i);
                return;
            }
            Slog.w(FingerprintManager.TAG, "Group id didn't match: " + groupId + " != " + groupId2);
        }

        private void sendEnumeratedResult(long j, int i, int i2) {
            if (FingerprintManager.this.mEnumerateCallback != null) {
                FingerprintManager.this.mEnumerateCallback.onEnumerate(new Fingerprint(null, i2, i, j));
            }
        }

        private void sendEnrollResult(Fingerprint fingerprint, int i) {
            if (FingerprintManager.this.mEnrollmentCallback != null) {
                FingerprintManager.this.mEnrollmentCallback.onEnrollmentProgress(i);
            }
        }
    }

    private void sendAuthenticatedSucceeded(Fingerprint fingerprint, int i) {
        if (this.mAuthenticationCallback != null) {
            this.mAuthenticationCallback.onAuthenticationSucceeded(new BiometricAuthenticator.AuthenticationResult(this.mCryptoObject, fingerprint, i));
        }
    }

    private void sendAuthenticatedFailed() {
        if (this.mAuthenticationCallback != null) {
            this.mAuthenticationCallback.onAuthenticationFailed();
        }
    }

    private void sendAcquiredResult(long j, int i, int i2) {
        if (this.mAuthenticationCallback != null) {
            this.mAuthenticationCallback.onAuthenticationAcquired(i);
        }
        String acquiredString = getAcquiredString(i, i2);
        if (acquiredString == null) {
            return;
        }
        if (i == 6) {
            i = i2 + 1000;
        }
        if (this.mEnrollmentCallback != null) {
            this.mEnrollmentCallback.onEnrollmentHelp(i, acquiredString);
        } else if (this.mAuthenticationCallback != null) {
            this.mAuthenticationCallback.onAuthenticationHelp(i, acquiredString);
        }
    }

    private void sendErrorResult(long j, int i, int i2) {
        int i3;
        if (i == 8) {
            i3 = i2 + 1000;
        } else {
            i3 = i;
        }
        if (this.mEnrollmentCallback != null) {
            this.mEnrollmentCallback.onEnrollmentError(i3, getErrorString(i, i2));
            return;
        }
        if (this.mAuthenticationCallback != null) {
            this.mAuthenticationCallback.onAuthenticationError(i3, getErrorString(i, i2));
        } else if (this.mRemovalCallback != null) {
            this.mRemovalCallback.onRemovalError(this.mRemovalFingerprint, i3, getErrorString(i, i2));
        } else if (this.mEnumerateCallback != null) {
            this.mEnumerateCallback.onEnumerateError(i3, getErrorString(i, i2));
        }
    }

    public FingerprintManager(Context context, IFingerprintService iFingerprintService) {
        this.mContext = context;
        this.mService = iFingerprintService;
        if (this.mService == null) {
            Slog.v(TAG, "FingerprintManagerService was null");
        }
        this.mHandler = new MyHandler(this, context, (AnonymousClass1) null);
    }

    private int getCurrentUserId() {
        try {
            return ActivityManager.getService().getCurrentUser().id;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void cancelEnrollment() {
        if (this.mService != null) {
            try {
                this.mService.cancelEnrollment(this.mToken);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    private void cancelAuthentication(android.hardware.biometrics.CryptoObject cryptoObject) {
        if (this.mService != null) {
            try {
                this.mService.cancelAuthentication(this.mToken, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public String getErrorString(int i, int i2) {
        switch (i) {
            case 1:
                return this.mContext.getString(R.string.fingerprint_error_hw_not_available);
            case 2:
                return this.mContext.getString(R.string.fingerprint_error_unable_to_process);
            case 3:
                return this.mContext.getString(R.string.fingerprint_error_timeout);
            case 4:
                return this.mContext.getString(R.string.fingerprint_error_no_space);
            case 5:
                return this.mContext.getString(R.string.fingerprint_error_canceled);
            case 7:
                return this.mContext.getString(R.string.fingerprint_error_lockout);
            case 8:
                String[] stringArray = this.mContext.getResources().getStringArray(R.array.fingerprint_error_vendor);
                if (i2 < stringArray.length) {
                    return stringArray[i2];
                }
                break;
            case 9:
                return this.mContext.getString(R.string.fingerprint_error_lockout_permanent);
            case 10:
                return this.mContext.getString(R.string.fingerprint_error_user_canceled);
            case 11:
                return this.mContext.getString(R.string.fingerprint_error_no_fingerprints);
            case 12:
                return this.mContext.getString(R.string.fingerprint_error_hw_not_present);
        }
        Slog.w(TAG, "Invalid error message: " + i + ", " + i2);
        return null;
    }

    public String getAcquiredString(int i, int i2) {
        switch (i) {
            case 0:
                return null;
            case 1:
                return this.mContext.getString(R.string.fingerprint_acquired_partial);
            case 2:
                return this.mContext.getString(R.string.fingerprint_acquired_insufficient);
            case 3:
                return this.mContext.getString(R.string.fingerprint_acquired_imager_dirty);
            case 4:
                return this.mContext.getString(R.string.fingerprint_acquired_too_slow);
            case 5:
                return this.mContext.getString(R.string.fingerprint_acquired_too_fast);
            case 6:
                String[] stringArray = this.mContext.getResources().getStringArray(R.array.fingerprint_acquired_vendor);
                if (i2 < stringArray.length) {
                    return stringArray[i2];
                }
                break;
        }
        Slog.w(TAG, "Invalid acquired message: " + i + ", " + i2);
        return null;
    }

    class AnonymousClass2 extends IFingerprintServiceReceiver.Stub {
        AnonymousClass2() {
        }

        @Override
        public void onEnrollResult(long j, int i, int i2, int i3) {
            FingerprintManager.this.mHandler.obtainMessage(100, i3, 0, new Fingerprint(null, i2, i, j)).sendToTarget();
        }

        @Override
        public void onAcquired(final long j, final int i, final int i2) {
            if (FingerprintManager.this.mExecutor != null) {
                FingerprintManager.this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        FingerprintManager.this.sendAcquiredResult(j, i, i2);
                    }
                });
            } else {
                FingerprintManager.this.mHandler.obtainMessage(101, i, i2, Long.valueOf(j)).sendToTarget();
            }
        }

        @Override
        public void onAuthenticationSucceeded(long j, final Fingerprint fingerprint, final int i) {
            if (FingerprintManager.this.mExecutor != null) {
                FingerprintManager.this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        FingerprintManager.this.sendAuthenticatedSucceeded(fingerprint, i);
                    }
                });
            } else {
                FingerprintManager.this.mHandler.obtainMessage(102, i, 0, fingerprint).sendToTarget();
            }
        }

        @Override
        public void onAuthenticationFailed(long j) {
            if (FingerprintManager.this.mExecutor != null) {
                FingerprintManager.this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        FingerprintManager.this.sendAuthenticatedFailed();
                    }
                });
            } else {
                FingerprintManager.this.mHandler.obtainMessage(103).sendToTarget();
            }
        }

        @Override
        public void onError(final long j, final int i, final int i2) {
            if (FingerprintManager.this.mExecutor == null) {
                FingerprintManager.this.mHandler.obtainMessage(104, i, i2, Long.valueOf(j)).sendToTarget();
            } else if (i == 10 || i == 5) {
                FingerprintManager.this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        FingerprintManager.this.sendErrorResult(j, i, i2);
                    }
                });
            } else {
                FingerprintManager.this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public final void run() {
                        FingerprintManager.AnonymousClass2 anonymousClass2 = this.f$0;
                        FingerprintManager.this.mExecutor.execute(new Runnable() {
                            @Override
                            public final void run() {
                                FingerprintManager.this.sendErrorResult(j, i, i);
                            }
                        });
                    }
                }, 2000L);
            }
        }

        @Override
        public void onRemoved(long j, int i, int i2, int i3) {
            FingerprintManager.this.mHandler.obtainMessage(105, i3, 0, new Fingerprint(null, i2, i, j)).sendToTarget();
        }

        @Override
        public void onEnumerated(long j, int i, int i2, int i3) {
            FingerprintManager.this.mHandler.obtainMessage(106, i, i2, Long.valueOf(j)).sendToTarget();
        }
    }
}
