package android.security.keystore.recovery;

import android.annotation.SystemApi;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.ArrayMap;
import android.util.Log;
import java.security.Key;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SystemApi
public class RecoverySession implements AutoCloseable {
    private static final int SESSION_ID_LENGTH_BYTES = 16;
    private static final String TAG = "RecoverySession";
    private final RecoveryController mRecoveryController;
    private final String mSessionId;

    private RecoverySession(RecoveryController recoveryController, String str) {
        this.mRecoveryController = recoveryController;
        this.mSessionId = str;
    }

    static RecoverySession newInstance(RecoveryController recoveryController) {
        return new RecoverySession(recoveryController, newSessionId());
    }

    private static String newSessionId() {
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        StringBuilder sb = new StringBuilder();
        for (byte b : bArr) {
            sb.append(Byte.toHexString(b, false));
        }
        return sb.toString();
    }

    @Deprecated
    public byte[] start(byte[] bArr, byte[] bArr2, byte[] bArr3, List<KeyChainProtectionParams> list) throws InternalRecoveryServiceException, CertificateException {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public byte[] start(CertPath certPath, byte[] bArr, byte[] bArr2, List<KeyChainProtectionParams> list) throws InternalRecoveryServiceException, CertificateException {
        throw new UnsupportedOperationException();
    }

    public byte[] start(String str, CertPath certPath, byte[] bArr, byte[] bArr2, List<KeyChainProtectionParams> list) throws InternalRecoveryServiceException, CertificateException {
        try {
            return this.mRecoveryController.getBinder().startRecoverySessionWithCertPath(this.mSessionId, str, RecoveryCertPath.createRecoveryCertPath(certPath), bArr, bArr2, list);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode == 25 || e2.errorCode == 28) {
                throw new CertificateException("Invalid certificate for recovery session", e2);
            }
            throw this.mRecoveryController.wrapUnexpectedServiceSpecificException(e2);
        }
    }

    @Deprecated
    public Map<String, byte[]> recoverKeys(byte[] bArr, List<WrappedApplicationKey> list) throws SessionExpiredException, InternalRecoveryServiceException, DecryptionFailedException {
        throw new UnsupportedOperationException();
    }

    public Map<String, Key> recoverKeyChainSnapshot(byte[] bArr, List<WrappedApplicationKey> list) throws SessionExpiredException, InternalRecoveryServiceException, DecryptionFailedException {
        try {
            return getKeysFromGrants(this.mRecoveryController.getBinder().recoverKeyChainSnapshot(this.mSessionId, bArr, list));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode == 26) {
                throw new DecryptionFailedException(e2.getMessage());
            }
            if (e2.errorCode == 24) {
                throw new SessionExpiredException(e2.getMessage());
            }
            throw this.mRecoveryController.wrapUnexpectedServiceSpecificException(e2);
        }
    }

    private Map<String, Key> getKeysFromGrants(Map<String, String> map) throws InternalRecoveryServiceException {
        ArrayMap arrayMap = new ArrayMap(map.size());
        for (String str : map.keySet()) {
            String str2 = map.get(str);
            try {
                arrayMap.put(str, this.mRecoveryController.getKeyFromGrant(str2));
            } catch (UnrecoverableKeyException e) {
                throw new InternalRecoveryServiceException(String.format(Locale.US, "Failed to get key '%s' from grant '%s'", str, str2), e);
            }
        }
        return arrayMap;
    }

    String getSessionId() {
        return this.mSessionId;
    }

    @Override
    public void close() {
        try {
            this.mRecoveryController.getBinder().closeSession(this.mSessionId);
        } catch (RemoteException | ServiceSpecificException e) {
            Log.e(TAG, "Unexpected error trying to close session", e);
        }
    }
}
