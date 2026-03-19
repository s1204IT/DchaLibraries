package com.android.server.locksettings.recoverablekeystore;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.UserHandle;
import android.security.KeyStore;
import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.RecoveryCertPath;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.HexDump;
import com.android.internal.util.Preconditions;
import com.android.server.locksettings.recoverablekeystore.certificate.CertParsingException;
import com.android.server.locksettings.recoverablekeystore.certificate.CertUtils;
import com.android.server.locksettings.recoverablekeystore.certificate.CertValidationException;
import com.android.server.locksettings.recoverablekeystore.certificate.CertXml;
import com.android.server.locksettings.recoverablekeystore.certificate.SigXml;
import com.android.server.locksettings.recoverablekeystore.storage.ApplicationKeyStorage;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverableKeyStoreDb;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySessionStorage;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySnapshotStorage;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.AEADBadTagException;

public class RecoverableKeyStoreManager {
    private static final String TAG = "RecoverableKeyStoreMgr";
    private static RecoverableKeyStoreManager mInstance;
    private final ApplicationKeyStorage mApplicationKeyStorage;
    private final Context mContext;
    private final RecoverableKeyStoreDb mDatabase;
    private final ExecutorService mExecutorService;
    private final RecoverySnapshotListenersStorage mListenersStorage;
    private final PlatformKeyManager mPlatformKeyManager;
    private final RecoverableKeyGenerator mRecoverableKeyGenerator;
    private final RecoverySessionStorage mRecoverySessionStorage;
    private final RecoverySnapshotStorage mSnapshotStorage;
    private final TestOnlyInsecureCertificateHelper mTestCertHelper;

    public static synchronized RecoverableKeyStoreManager getInstance(Context context, KeyStore keyStore) {
        if (mInstance == null) {
            RecoverableKeyStoreDb recoverableKeyStoreDbNewInstance = RecoverableKeyStoreDb.newInstance(context);
            try {
                try {
                    mInstance = new RecoverableKeyStoreManager(context.getApplicationContext(), recoverableKeyStoreDbNewInstance, new RecoverySessionStorage(), Executors.newSingleThreadExecutor(), RecoverySnapshotStorage.newInstance(), new RecoverySnapshotListenersStorage(), PlatformKeyManager.getInstance(context, recoverableKeyStoreDbNewInstance), ApplicationKeyStorage.getInstance(keyStore), new TestOnlyInsecureCertificateHelper());
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            } catch (KeyStoreException e2) {
                throw new ServiceSpecificException(22, e2.getMessage());
            }
        }
        return mInstance;
    }

    @VisibleForTesting
    RecoverableKeyStoreManager(Context context, RecoverableKeyStoreDb recoverableKeyStoreDb, RecoverySessionStorage recoverySessionStorage, ExecutorService executorService, RecoverySnapshotStorage recoverySnapshotStorage, RecoverySnapshotListenersStorage recoverySnapshotListenersStorage, PlatformKeyManager platformKeyManager, ApplicationKeyStorage applicationKeyStorage, TestOnlyInsecureCertificateHelper testOnlyInsecureCertificateHelper) throws ServiceSpecificException {
        this.mContext = context;
        this.mDatabase = recoverableKeyStoreDb;
        this.mRecoverySessionStorage = recoverySessionStorage;
        this.mExecutorService = executorService;
        this.mListenersStorage = recoverySnapshotListenersStorage;
        this.mSnapshotStorage = recoverySnapshotStorage;
        this.mPlatformKeyManager = platformKeyManager;
        this.mApplicationKeyStorage = applicationKeyStorage;
        this.mTestCertHelper = testOnlyInsecureCertificateHelper;
        try {
            this.mRecoverableKeyGenerator = RecoverableKeyGenerator.newInstance(this.mDatabase);
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(TAG, "AES keygen algorithm not available. AOSP must support this.", e);
            throw new ServiceSpecificException(22, e.getMessage());
        }
    }

    @VisibleForTesting
    void initRecoveryService(String str, byte[] bArr) throws Exception {
        checkRecoverKeyStorePermission();
        int callingUserId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        String defaultCertificateAliasIfEmpty = this.mTestCertHelper.getDefaultCertificateAliasIfEmpty(str);
        if (!this.mTestCertHelper.isValidRootCertificateAlias(defaultCertificateAliasIfEmpty)) {
            throw new ServiceSpecificException(28, "Invalid root certificate alias");
        }
        String activeRootOfTrust = this.mDatabase.getActiveRootOfTrust(callingUserId, callingUid);
        if (activeRootOfTrust == null) {
            Log.d(TAG, "Root of trust for recovery agent + " + callingUid + " is assigned for the first time to " + defaultCertificateAliasIfEmpty);
        } else if (!activeRootOfTrust.equals(defaultCertificateAliasIfEmpty)) {
            Log.i(TAG, "Root of trust for recovery agent " + callingUid + " is changed to " + defaultCertificateAliasIfEmpty + " from  " + activeRootOfTrust);
        }
        if (this.mDatabase.setActiveRootOfTrust(callingUserId, callingUid, defaultCertificateAliasIfEmpty) < 0) {
            throw new ServiceSpecificException(22, "Failed to set the root of trust in the local DB.");
        }
        try {
            CertXml certXml = CertXml.parse(bArr);
            long serial = certXml.getSerial();
            Long recoveryServiceCertSerial = this.mDatabase.getRecoveryServiceCertSerial(callingUserId, callingUid, defaultCertificateAliasIfEmpty);
            if (recoveryServiceCertSerial != null && recoveryServiceCertSerial.longValue() >= serial && !this.mTestCertHelper.isTestOnlyCertificateAlias(defaultCertificateAliasIfEmpty)) {
                if (recoveryServiceCertSerial.longValue() == serial) {
                    Log.i(TAG, "The cert file serial number is the same, so skip updating.");
                    return;
                } else {
                    Log.e(TAG, "The cert file serial number is older than the one in database.");
                    throw new ServiceSpecificException(29, "The cert file serial number is older than the one in database.");
                }
            }
            Log.i(TAG, "Updating the certificate with the new serial number " + serial);
            X509Certificate rootCertificate = this.mTestCertHelper.getRootCertificate(defaultCertificateAliasIfEmpty);
            try {
                Log.d(TAG, "Getting and validating a random endpoint certificate");
                CertPath randomEndpointCert = certXml.getRandomEndpointCert(rootCertificate);
                try {
                    Log.d(TAG, "Saving the randomly chosen endpoint certificate to database");
                    long recoveryServiceCertPath = this.mDatabase.setRecoveryServiceCertPath(callingUserId, callingUid, defaultCertificateAliasIfEmpty, randomEndpointCert);
                    if (recoveryServiceCertPath <= 0) {
                        if (recoveryServiceCertPath < 0) {
                            throw new ServiceSpecificException(22, "Failed to set the certificate path in the local DB.");
                        }
                    } else {
                        if (this.mDatabase.setRecoveryServiceCertSerial(callingUserId, callingUid, defaultCertificateAliasIfEmpty, serial) < 0) {
                            throw new ServiceSpecificException(22, "Failed to set the certificate serial number in the local DB.");
                        }
                        if (this.mDatabase.getSnapshotVersion(callingUserId, callingUid) != null) {
                            this.mDatabase.setShouldCreateSnapshot(callingUserId, callingUid, true);
                            Log.i(TAG, "This is a certificate change. Snapshot must be updated");
                        } else {
                            Log.i(TAG, "This is a certificate change. Snapshot didn't exist");
                        }
                        if (this.mDatabase.setCounterId(callingUserId, callingUid, new SecureRandom().nextLong()) < 0) {
                            Log.e(TAG, "Failed to set the counter id in the local DB.");
                        }
                    }
                } catch (CertificateEncodingException e) {
                    Log.e(TAG, "Failed to encode CertPath", e);
                    throw new ServiceSpecificException(25, e.getMessage());
                }
            } catch (CertValidationException e2) {
                Log.e(TAG, "Invalid endpoint cert", e2);
                throw new ServiceSpecificException(28, e2.getMessage());
            }
        } catch (CertParsingException e3) {
            Log.d(TAG, "Failed to parse the input as a cert file: " + HexDump.toHexString(bArr));
            throw new ServiceSpecificException(25, e3.getMessage());
        }
    }

    public void initRecoveryServiceWithSigFile(String str, byte[] bArr, byte[] bArr2) throws Exception {
        checkRecoverKeyStorePermission();
        String defaultCertificateAliasIfEmpty = this.mTestCertHelper.getDefaultCertificateAliasIfEmpty(str);
        Preconditions.checkNotNull(bArr, "recoveryServiceCertFile is null");
        Preconditions.checkNotNull(bArr2, "recoveryServiceSigFile is null");
        try {
            try {
                SigXml.parse(bArr2).verifyFileSignature(this.mTestCertHelper.getRootCertificate(defaultCertificateAliasIfEmpty), bArr);
                initRecoveryService(defaultCertificateAliasIfEmpty, bArr);
            } catch (CertValidationException e) {
                Log.d(TAG, "The signature over the cert file is invalid. Cert: " + HexDump.toHexString(bArr) + " Sig: " + HexDump.toHexString(bArr2));
                throw new ServiceSpecificException(28, e.getMessage());
            }
        } catch (CertParsingException e2) {
            Log.d(TAG, "Failed to parse the sig file: " + HexDump.toHexString(bArr2));
            throw new ServiceSpecificException(25, e2.getMessage());
        }
    }

    public KeyChainSnapshot getKeyChainSnapshot() throws RemoteException, ServiceSpecificException {
        checkRecoverKeyStorePermission();
        KeyChainSnapshot keyChainSnapshot = this.mSnapshotStorage.get(Binder.getCallingUid());
        if (keyChainSnapshot == null) {
            throw new ServiceSpecificException(21);
        }
        return keyChainSnapshot;
    }

    public void setSnapshotCreatedPendingIntent(PendingIntent pendingIntent) throws RemoteException {
        checkRecoverKeyStorePermission();
        this.mListenersStorage.setSnapshotListener(Binder.getCallingUid(), pendingIntent);
    }

    public void setServerParams(byte[] bArr) throws RemoteException, ServiceSpecificException {
        checkRecoverKeyStorePermission();
        int callingUserId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        byte[] serverParams = this.mDatabase.getServerParams(callingUserId, callingUid);
        if (Arrays.equals(bArr, serverParams)) {
            Log.v(TAG, "Not updating server params - same as old value.");
            return;
        }
        if (this.mDatabase.setServerParams(callingUserId, callingUid, bArr) < 0) {
            throw new ServiceSpecificException(22, "Database failure trying to set server params.");
        }
        if (serverParams == null) {
            Log.i(TAG, "Initialized server params.");
        } else if (this.mDatabase.getSnapshotVersion(callingUserId, callingUid) != null) {
            this.mDatabase.setShouldCreateSnapshot(callingUserId, callingUid, true);
            Log.i(TAG, "Updated server params. Snapshot must be updated");
        } else {
            Log.i(TAG, "Updated server params. Snapshot didn't exist");
        }
    }

    public void setRecoveryStatus(String str, int i) throws RemoteException, ServiceSpecificException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(str, "alias is null");
        if (this.mDatabase.setRecoveryStatus(Binder.getCallingUid(), str, i) < 0) {
            throw new ServiceSpecificException(22, "Failed to set the key recovery status in the local DB.");
        }
    }

    public Map<String, Integer> getRecoveryStatus() throws RemoteException {
        checkRecoverKeyStorePermission();
        return this.mDatabase.getStatusForAllKeys(Binder.getCallingUid());
    }

    public void setRecoverySecretTypes(int[] iArr) throws Exception {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(iArr, "secretTypes is null");
        int callingUserId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        int[] recoverySecretTypes = this.mDatabase.getRecoverySecretTypes(callingUserId, callingUid);
        if (Arrays.equals(iArr, recoverySecretTypes)) {
            Log.v(TAG, "Not updating secret types - same as old value.");
            return;
        }
        if (this.mDatabase.setRecoverySecretTypes(callingUserId, callingUid, iArr) < 0) {
            throw new ServiceSpecificException(22, "Database error trying to set secret types.");
        }
        if (recoverySecretTypes.length == 0) {
            Log.i(TAG, "Initialized secret types.");
            return;
        }
        Log.i(TAG, "Updated secret types. Snapshot pending.");
        if (this.mDatabase.getSnapshotVersion(callingUserId, callingUid) != null) {
            this.mDatabase.setShouldCreateSnapshot(callingUserId, callingUid, true);
            Log.i(TAG, "Updated secret types. Snapshot must be updated");
        } else {
            Log.i(TAG, "Updated secret types. Snapshot didn't exist");
        }
    }

    public int[] getRecoverySecretTypes() throws RemoteException {
        checkRecoverKeyStorePermission();
        return this.mDatabase.getRecoverySecretTypes(UserHandle.getCallingUserId(), Binder.getCallingUid());
    }

    @VisibleForTesting
    byte[] startRecoverySession(String str, byte[] bArr, byte[] bArr2, byte[] bArr3, List<KeyChainProtectionParams> list) throws RemoteException, ServiceSpecificException {
        checkRecoverKeyStorePermission();
        int callingUid = Binder.getCallingUid();
        if (list.size() != 1) {
            throw new UnsupportedOperationException("Only a single KeyChainProtectionParams is supported");
        }
        try {
            PublicKey publicKeyDeserializePublicKey = KeySyncUtils.deserializePublicKey(bArr);
            if (!publicKeysMatch(publicKeyDeserializePublicKey, bArr2)) {
                throw new ServiceSpecificException(28, "The public keys given in verifierPublicKey and vaultParams do not match.");
            }
            byte[] bArrGenerateKeyClaimant = KeySyncUtils.generateKeyClaimant();
            byte[] secret = list.get(0).getSecret();
            this.mRecoverySessionStorage.add(callingUid, new RecoverySessionStorage.Entry(str, secret, bArrGenerateKeyClaimant, bArr2));
            Log.i(TAG, "Received VaultParams for recovery: " + HexDump.toHexString(bArr2));
            try {
                return KeySyncUtils.encryptRecoveryClaim(publicKeyDeserializePublicKey, bArr2, bArr3, KeySyncUtils.calculateThmKfHash(secret), bArrGenerateKeyClaimant);
            } catch (InvalidKeyException e) {
                throw new ServiceSpecificException(25, e.getMessage());
            } catch (NoSuchAlgorithmException e2) {
                Log.wtf(TAG, "SecureBox algorithm missing. AOSP must support this.", e2);
                throw new ServiceSpecificException(22, e2.getMessage());
            }
        } catch (InvalidKeySpecException e3) {
            throw new ServiceSpecificException(25, e3.getMessage());
        }
    }

    public byte[] startRecoverySessionWithCertPath(String str, String str2, RecoveryCertPath recoveryCertPath, byte[] bArr, byte[] bArr2, List<KeyChainProtectionParams> list) throws RemoteException, ServiceSpecificException {
        checkRecoverKeyStorePermission();
        String defaultCertificateAliasIfEmpty = this.mTestCertHelper.getDefaultCertificateAliasIfEmpty(str2);
        Preconditions.checkNotNull(str, "invalid session");
        Preconditions.checkNotNull(recoveryCertPath, "verifierCertPath is null");
        Preconditions.checkNotNull(bArr, "vaultParams is null");
        Preconditions.checkNotNull(bArr2, "vaultChallenge is null");
        Preconditions.checkNotNull(list, "secrets is null");
        try {
            CertPath certPath = recoveryCertPath.getCertPath();
            try {
                CertUtils.validateCertPath(this.mTestCertHelper.getRootCertificate(defaultCertificateAliasIfEmpty), certPath);
                byte[] encoded = certPath.getCertificates().get(0).getPublicKey().getEncoded();
                if (encoded == null) {
                    Log.e(TAG, "Failed to encode verifierPublicKey");
                    throw new ServiceSpecificException(25, "Failed to encode verifierPublicKey");
                }
                return startRecoverySession(str, encoded, bArr, bArr2, list);
            } catch (CertValidationException e) {
                Log.e(TAG, "Failed to validate the given cert path", e);
                throw new ServiceSpecificException(28, e.getMessage());
            }
        } catch (CertificateException e2) {
            throw new ServiceSpecificException(25, e2.getMessage());
        }
    }

    public Map<String, String> recoverKeyChainSnapshot(String str, byte[] bArr, List<WrappedApplicationKey> list) throws RemoteException, ServiceSpecificException {
        checkRecoverKeyStorePermission();
        int callingUserId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        RecoverySessionStorage.Entry entry = this.mRecoverySessionStorage.get(callingUid, str);
        try {
            if (entry == null) {
                throw new ServiceSpecificException(24, String.format(Locale.US, "Application uid=%d does not have pending session '%s'", Integer.valueOf(callingUid), str));
            }
            try {
                return importKeyMaterials(callingUserId, callingUid, recoverApplicationKeys(decryptRecoveryKey(entry, bArr), list));
            } catch (KeyStoreException e) {
                throw new ServiceSpecificException(22, e.getMessage());
            }
        } finally {
            entry.destroy();
            this.mRecoverySessionStorage.remove(callingUid);
        }
    }

    private Map<String, String> importKeyMaterials(int i, int i2, Map<String, byte[]> map) throws KeyStoreException, ServiceSpecificException {
        ArrayMap arrayMap = new ArrayMap(map.size());
        for (String str : map.keySet()) {
            this.mApplicationKeyStorage.setSymmetricKeyEntry(i, i2, str, map.get(str));
            String alias = getAlias(i, i2, str);
            Log.i(TAG, String.format(Locale.US, "Import %s -> %s", str, alias));
            arrayMap.put(str, alias);
        }
        return arrayMap;
    }

    private String getAlias(int i, int i2, String str) {
        return this.mApplicationKeyStorage.getGrantAlias(i, i2, str);
    }

    public void closeSession(String str) throws RemoteException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(str, "invalid session");
        this.mRecoverySessionStorage.remove(Binder.getCallingUid(), str);
    }

    public void removeKey(String str) throws RemoteException, ServiceSpecificException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(str, "alias is null");
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        if (this.mDatabase.removeKey(callingUid, str)) {
            this.mDatabase.setShouldCreateSnapshot(callingUserId, callingUid, true);
            this.mApplicationKeyStorage.deleteEntry(callingUserId, callingUid, str);
        }
    }

    public String generateKey(String str) throws RemoteException, ServiceSpecificException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(str, "alias is null");
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        try {
            try {
                this.mApplicationKeyStorage.setSymmetricKeyEntry(callingUserId, callingUid, str, this.mRecoverableKeyGenerator.generateAndStoreKey(this.mPlatformKeyManager.getEncryptKey(callingUserId), callingUserId, callingUid, str));
                return getAlias(callingUserId, callingUid, str);
            } catch (RecoverableKeyStorageException | InvalidKeyException | KeyStoreException e) {
                throw new ServiceSpecificException(22, e.getMessage());
            }
        } catch (InsecureUserException e2) {
            throw new ServiceSpecificException(23, e2.getMessage());
        } catch (IOException | KeyStoreException | UnrecoverableKeyException e3) {
            throw new ServiceSpecificException(22, e3.getMessage());
        } catch (NoSuchAlgorithmException e4) {
            throw new RuntimeException(e4);
        }
    }

    public String importKey(String str, byte[] bArr) throws RemoteException, ServiceSpecificException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(str, "alias is null");
        Preconditions.checkNotNull(bArr, "keyBytes is null");
        if (bArr.length != 32) {
            Log.e(TAG, "The given key for import doesn't have the required length 256");
            throw new ServiceSpecificException(27, "The given key does not contain 256 bits.");
        }
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        try {
            try {
                this.mRecoverableKeyGenerator.importKey(this.mPlatformKeyManager.getEncryptKey(callingUserId), callingUserId, callingUid, str, bArr);
                this.mApplicationKeyStorage.setSymmetricKeyEntry(callingUserId, callingUid, str, bArr);
                return getAlias(callingUserId, callingUid, str);
            } catch (RecoverableKeyStorageException | InvalidKeyException | KeyStoreException e) {
                throw new ServiceSpecificException(22, e.getMessage());
            }
        } catch (InsecureUserException e2) {
            throw new ServiceSpecificException(23, e2.getMessage());
        } catch (IOException | KeyStoreException | UnrecoverableKeyException e3) {
            throw new ServiceSpecificException(22, e3.getMessage());
        } catch (NoSuchAlgorithmException e4) {
            throw new RuntimeException(e4);
        }
    }

    public String getKey(String str) throws RemoteException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(str, "alias is null");
        return getAlias(UserHandle.getCallingUserId(), Binder.getCallingUid(), str);
    }

    private byte[] decryptRecoveryKey(RecoverySessionStorage.Entry entry, byte[] bArr) throws RemoteException, ServiceSpecificException {
        try {
            try {
                return KeySyncUtils.decryptRecoveryKey(entry.getLskfHash(), KeySyncUtils.decryptRecoveryClaimResponse(entry.getKeyClaimant(), entry.getVaultParams(), bArr));
            } catch (InvalidKeyException e) {
                Log.e(TAG, "Got InvalidKeyException during decrypting recovery key", e);
                throw new ServiceSpecificException(26, "Failed to decrypt recovery key " + e.getMessage());
            } catch (NoSuchAlgorithmException e2) {
                throw new ServiceSpecificException(22, e2.getMessage());
            } catch (AEADBadTagException e3) {
                Log.e(TAG, "Got AEADBadTagException during decrypting recovery key", e3);
                throw new ServiceSpecificException(26, "Failed to decrypt recovery key " + e3.getMessage());
            }
        } catch (InvalidKeyException e4) {
            Log.e(TAG, "Got InvalidKeyException during decrypting recovery claim response", e4);
            throw new ServiceSpecificException(26, "Failed to decrypt recovery key " + e4.getMessage());
        } catch (NoSuchAlgorithmException e5) {
            throw new ServiceSpecificException(22, e5.getMessage());
        } catch (AEADBadTagException e6) {
            Log.e(TAG, "Got AEADBadTagException during decrypting recovery claim response", e6);
            throw new ServiceSpecificException(26, "Failed to decrypt recovery key " + e6.getMessage());
        }
    }

    private Map<String, byte[]> recoverApplicationKeys(byte[] bArr, List<WrappedApplicationKey> list) throws RemoteException, ServiceSpecificException {
        HashMap map = new HashMap();
        for (WrappedApplicationKey wrappedApplicationKey : list) {
            String alias = wrappedApplicationKey.getAlias();
            try {
                map.put(alias, KeySyncUtils.decryptApplicationKey(bArr, wrappedApplicationKey.getEncryptedKeyMaterial()));
            } catch (InvalidKeyException e) {
                Log.e(TAG, "Got InvalidKeyException during decrypting application key with alias: " + alias, e);
                throw new ServiceSpecificException(26, "Failed to recover key with alias '" + alias + "': " + e.getMessage());
            } catch (NoSuchAlgorithmException e2) {
                Log.wtf(TAG, "Missing SecureBox algorithm. AOSP required to support this.", e2);
                throw new ServiceSpecificException(22, e2.getMessage());
            } catch (AEADBadTagException e3) {
                Log.e(TAG, "Got AEADBadTagException during decrypting application key with alias: " + alias, e3);
            }
        }
        if (!list.isEmpty() && map.isEmpty()) {
            Log.e(TAG, "Failed to recover any of the application keys.");
            throw new ServiceSpecificException(26, "Failed to recover any of the application keys.");
        }
        return map;
    }

    public void lockScreenSecretAvailable(int i, String str, int i2) {
        try {
            this.mExecutorService.execute(KeySyncTask.newInstance(this.mContext, this.mDatabase, this.mSnapshotStorage, this.mListenersStorage, i2, i, str, false));
        } catch (InsecureUserException e) {
            Log.wtf(TAG, "Impossible - insecure user, but user just entered lock screen", e);
        } catch (KeyStoreException e2) {
            Log.e(TAG, "Key store error encountered during recoverable key sync", e2);
        } catch (NoSuchAlgorithmException e3) {
            Log.wtf(TAG, "Should never happen - algorithm unavailable for KeySync", e3);
        }
    }

    public void lockScreenSecretChanged(int i, String str, int i2) {
        try {
            this.mExecutorService.execute(KeySyncTask.newInstance(this.mContext, this.mDatabase, this.mSnapshotStorage, this.mListenersStorage, i2, i, str, true));
        } catch (InsecureUserException e) {
            Log.e(TAG, "InsecureUserException during lock screen secret update", e);
        } catch (KeyStoreException e2) {
            Log.e(TAG, "Key store error encountered during recoverable key sync", e2);
        } catch (NoSuchAlgorithmException e3) {
            Log.wtf(TAG, "Should never happen - algorithm unavailable for KeySync", e3);
        }
    }

    private void checkRecoverKeyStorePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVER_KEYSTORE", "Caller " + Binder.getCallingUid() + " doesn't have RecoverKeyStore permission.");
    }

    private boolean publicKeysMatch(PublicKey publicKey, byte[] bArr) {
        byte[] bArrEncodePublicKey = SecureBox.encodePublicKey(publicKey);
        return Arrays.equals(bArrEncodePublicKey, Arrays.copyOf(bArr, bArrEncodePublicKey.length));
    }
}
