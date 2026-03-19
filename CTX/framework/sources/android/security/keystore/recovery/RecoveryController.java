package android.security.keystore.recovery;

import android.annotation.SystemApi;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.security.KeyStore;
import android.security.keystore.AndroidKeyStoreProvider;
import com.android.internal.widget.ILockSettings;
import java.security.Key;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SystemApi
public class RecoveryController {
    public static final int ERROR_BAD_CERTIFICATE_FORMAT = 25;
    public static final int ERROR_DECRYPTION_FAILED = 26;
    public static final int ERROR_DOWNGRADE_CERTIFICATE = 29;
    public static final int ERROR_INSECURE_USER = 23;
    public static final int ERROR_INVALID_CERTIFICATE = 28;
    public static final int ERROR_INVALID_KEY_FORMAT = 27;
    public static final int ERROR_NO_SNAPSHOT_PENDING = 21;
    public static final int ERROR_SERVICE_INTERNAL_ERROR = 22;
    public static final int ERROR_SESSION_EXPIRED = 24;
    public static final int RECOVERY_STATUS_PERMANENT_FAILURE = 3;
    public static final int RECOVERY_STATUS_SYNCED = 0;
    public static final int RECOVERY_STATUS_SYNC_IN_PROGRESS = 1;
    private static final String TAG = "RecoveryController";
    private final ILockSettings mBinder;
    private final KeyStore mKeyStore;

    private RecoveryController(ILockSettings iLockSettings, KeyStore keyStore) {
        this.mBinder = iLockSettings;
        this.mKeyStore = keyStore;
    }

    ILockSettings getBinder() {
        return this.mBinder;
    }

    public static RecoveryController getInstance(Context context) {
        return new RecoveryController(ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings")), KeyStore.getInstance());
    }

    public static boolean isRecoverableKeyStoreEnabled(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(KeyguardManager.class);
        return keyguardManager != null && keyguardManager.isDeviceSecure();
    }

    @Deprecated
    public void initRecoveryService(String str, byte[] bArr) throws InternalRecoveryServiceException, CertificateException {
        throw new UnsupportedOperationException();
    }

    public void initRecoveryService(String str, byte[] bArr, byte[] bArr2) throws InternalRecoveryServiceException, CertificateException {
        try {
            this.mBinder.initRecoveryServiceWithSigFile(str, bArr, bArr2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode == 25 || e2.errorCode == 28) {
                throw new CertificateException("Invalid certificate for recovery service", e2);
            }
            if (e2.errorCode == 29) {
                throw new CertificateException("Downgrading certificate serial version isn't supported.", e2);
            }
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    @Deprecated
    public KeyChainSnapshot getRecoveryData() throws InternalRecoveryServiceException {
        throw new UnsupportedOperationException();
    }

    public KeyChainSnapshot getKeyChainSnapshot() throws InternalRecoveryServiceException {
        try {
            return this.mBinder.getKeyChainSnapshot();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode == 21) {
                return null;
            }
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    public void setSnapshotCreatedPendingIntent(PendingIntent pendingIntent) throws InternalRecoveryServiceException {
        try {
            this.mBinder.setSnapshotCreatedPendingIntent(pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    public void setServerParams(byte[] bArr) throws InternalRecoveryServiceException {
        try {
            this.mBinder.setServerParams(bArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    @Deprecated
    public List<String> getAliases(String str) throws InternalRecoveryServiceException {
        throw new UnsupportedOperationException();
    }

    public List<String> getAliases() throws InternalRecoveryServiceException {
        try {
            return new ArrayList(this.mBinder.getRecoveryStatus().keySet());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    @Deprecated
    public void setRecoveryStatus(String str, String str2, int i) throws PackageManager.NameNotFoundException, InternalRecoveryServiceException {
        throw new UnsupportedOperationException();
    }

    public void setRecoveryStatus(String str, int i) throws InternalRecoveryServiceException {
        try {
            this.mBinder.setRecoveryStatus(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    @Deprecated
    public int getRecoveryStatus(String str, String str2) throws InternalRecoveryServiceException {
        throw new UnsupportedOperationException();
    }

    public int getRecoveryStatus(String str) throws InternalRecoveryServiceException {
        try {
            Integer num = (Integer) this.mBinder.getRecoveryStatus().get(str);
            if (num == null) {
                return 3;
            }
            return num.intValue();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    public void setRecoverySecretTypes(int[] iArr) throws InternalRecoveryServiceException {
        try {
            this.mBinder.setRecoverySecretTypes(iArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    public int[] getRecoverySecretTypes() throws InternalRecoveryServiceException {
        try {
            return this.mBinder.getRecoverySecretTypes();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    @Deprecated
    public byte[] generateAndStoreKey(String str, byte[] bArr) throws InternalRecoveryServiceException, LockScreenRequiredException {
        throw new UnsupportedOperationException("Operation is not supported, use generateKey");
    }

    @Deprecated
    public Key generateKey(String str, byte[] bArr) throws InternalRecoveryServiceException, LockScreenRequiredException {
        throw new UnsupportedOperationException();
    }

    public Key generateKey(String str) throws InternalRecoveryServiceException, LockScreenRequiredException {
        try {
            String strGenerateKey = this.mBinder.generateKey(str);
            if (strGenerateKey == null) {
                throw new InternalRecoveryServiceException("null grant alias");
            }
            return getKeyFromGrant(strGenerateKey);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode == 23) {
                throw new LockScreenRequiredException(e2.getMessage());
            }
            throw wrapUnexpectedServiceSpecificException(e2);
        } catch (UnrecoverableKeyException e3) {
            throw new InternalRecoveryServiceException("Failed to get key from keystore", e3);
        }
    }

    public Key importKey(String str, byte[] bArr) throws InternalRecoveryServiceException, LockScreenRequiredException {
        try {
            String strImportKey = this.mBinder.importKey(str, bArr);
            if (strImportKey == null) {
                throw new InternalRecoveryServiceException("Null grant alias");
            }
            return getKeyFromGrant(strImportKey);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode == 23) {
                throw new LockScreenRequiredException(e2.getMessage());
            }
            throw wrapUnexpectedServiceSpecificException(e2);
        } catch (UnrecoverableKeyException e3) {
            throw new InternalRecoveryServiceException("Failed to get key from keystore", e3);
        }
    }

    public Key getKey(String str) throws InternalRecoveryServiceException, UnrecoverableKeyException {
        try {
            String key = this.mBinder.getKey(str);
            if (key != null && !"".equals(key)) {
                return getKeyFromGrant(key);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    Key getKeyFromGrant(String str) throws UnrecoverableKeyException {
        return AndroidKeyStoreProvider.loadAndroidKeyStoreKeyFromKeystore(this.mKeyStore, str, -1);
    }

    public void removeKey(String str) throws InternalRecoveryServiceException {
        try {
            this.mBinder.removeKey(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw wrapUnexpectedServiceSpecificException(e2);
        }
    }

    public RecoverySession createRecoverySession() {
        return RecoverySession.newInstance(this);
    }

    public Map<String, X509Certificate> getRootCertificates() {
        return TrustedRootCertificates.getRootCertificates();
    }

    InternalRecoveryServiceException wrapUnexpectedServiceSpecificException(ServiceSpecificException serviceSpecificException) {
        if (serviceSpecificException.errorCode == 22) {
            return new InternalRecoveryServiceException(serviceSpecificException.getMessage());
        }
        return new InternalRecoveryServiceException("Unexpected error code for method: " + serviceSpecificException.errorCode, serviceSpecificException);
    }
}
