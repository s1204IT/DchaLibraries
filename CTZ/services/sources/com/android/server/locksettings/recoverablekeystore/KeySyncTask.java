package com.android.server.locksettings.recoverablekeystore;

import android.content.Context;
import android.security.Scrypt;
import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.KeyDerivationParams;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverableKeyStoreDb;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySnapshotStorage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class KeySyncTask implements Runnable {
    private static final int LENGTH_PREFIX_BYTES = 4;
    private static final String LOCK_SCREEN_HASH_ALGORITHM = "SHA-256";
    private static final String RECOVERY_KEY_ALGORITHM = "AES";
    private static final int RECOVERY_KEY_SIZE_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    @VisibleForTesting
    static final int SCRYPT_PARAM_N = 4096;

    @VisibleForTesting
    static final int SCRYPT_PARAM_OUTLEN_BYTES = 32;

    @VisibleForTesting
    static final int SCRYPT_PARAM_P = 1;

    @VisibleForTesting
    static final int SCRYPT_PARAM_R = 8;
    private static final String TAG = "KeySyncTask";
    private static final int TRUSTED_HARDWARE_MAX_ATTEMPTS = 10;
    private final String mCredential;
    private final int mCredentialType;
    private final boolean mCredentialUpdated;
    private final PlatformKeyManager mPlatformKeyManager;
    private final RecoverableKeyStoreDb mRecoverableKeyStoreDb;
    private final RecoverySnapshotStorage mRecoverySnapshotStorage;
    private final Scrypt mScrypt;
    private final RecoverySnapshotListenersStorage mSnapshotListenersStorage;
    private final TestOnlyInsecureCertificateHelper mTestOnlyInsecureCertificateHelper;
    private final int mUserId;

    public static KeySyncTask newInstance(Context context, RecoverableKeyStoreDb recoverableKeyStoreDb, RecoverySnapshotStorage recoverySnapshotStorage, RecoverySnapshotListenersStorage recoverySnapshotListenersStorage, int i, int i2, String str, boolean z) throws NoSuchAlgorithmException, KeyStoreException, InsecureUserException {
        return new KeySyncTask(recoverableKeyStoreDb, recoverySnapshotStorage, recoverySnapshotListenersStorage, i, i2, str, z, PlatformKeyManager.getInstance(context, recoverableKeyStoreDb), new TestOnlyInsecureCertificateHelper(), new Scrypt());
    }

    @VisibleForTesting
    KeySyncTask(RecoverableKeyStoreDb recoverableKeyStoreDb, RecoverySnapshotStorage recoverySnapshotStorage, RecoverySnapshotListenersStorage recoverySnapshotListenersStorage, int i, int i2, String str, boolean z, PlatformKeyManager platformKeyManager, TestOnlyInsecureCertificateHelper testOnlyInsecureCertificateHelper, Scrypt scrypt) {
        this.mSnapshotListenersStorage = recoverySnapshotListenersStorage;
        this.mRecoverableKeyStoreDb = recoverableKeyStoreDb;
        this.mUserId = i;
        this.mCredentialType = i2;
        this.mCredential = str;
        this.mCredentialUpdated = z;
        this.mPlatformKeyManager = platformKeyManager;
        this.mRecoverySnapshotStorage = recoverySnapshotStorage;
        this.mTestOnlyInsecureCertificateHelper = testOnlyInsecureCertificateHelper;
        this.mScrypt = scrypt;
    }

    @Override
    public void run() {
        try {
            synchronized (KeySyncTask.class) {
                syncKeys();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception thrown during KeySyncTask", e);
        }
    }

    private void syncKeys() throws Exception {
        if (this.mCredentialType == -1) {
            Log.w(TAG, "Credentials are not set for user " + this.mUserId);
            this.mPlatformKeyManager.invalidatePlatformKey(this.mUserId, this.mPlatformKeyManager.getGenerationId(this.mUserId));
            return;
        }
        if (isCustomLockScreen()) {
            Log.w(TAG, "Unsupported credential type " + this.mCredentialType + "for user " + this.mUserId);
            this.mRecoverableKeyStoreDb.invalidateKeysForUserIdOnCustomScreenLock(this.mUserId);
            return;
        }
        List<Integer> recoveryAgents = this.mRecoverableKeyStoreDb.getRecoveryAgents(this.mUserId);
        Iterator<Integer> it = recoveryAgents.iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            try {
                syncKeysForAgent(iIntValue);
            } catch (IOException e) {
                Log.e(TAG, "IOException during sync for agent " + iIntValue, e);
            }
        }
        if (recoveryAgents.isEmpty()) {
            Log.w(TAG, "No recovery agent initialized for user " + this.mUserId);
        }
    }

    private boolean isCustomLockScreen() {
        return (this.mCredentialType == -1 || this.mCredentialType == 1 || this.mCredentialType == 2) ? false : true;
    }

    private void syncKeysForAgent(int i) throws Exception {
        boolean z;
        PublicKey recoveryServicePublicKey;
        byte[] bArrHashCredentialsBySaltedSha256;
        Long counterId;
        KeyDerivationParams keyDerivationParamsCreateSha256Params;
        if (!shouldCreateSnapshot(i)) {
            z = this.mRecoverableKeyStoreDb.getSnapshotVersion(this.mUserId, i) != null && this.mRecoverySnapshotStorage.get(i) == null;
            if (z) {
                Log.d(TAG, "Recreating most recent snapshot");
            } else {
                Log.d(TAG, "Key sync not needed.");
                return;
            }
        } else {
            z = false;
        }
        String defaultCertificateAliasIfEmpty = this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(this.mRecoverableKeyStoreDb.getActiveRootOfTrust(this.mUserId, i));
        CertPath recoveryServiceCertPath = this.mRecoverableKeyStoreDb.getRecoveryServiceCertPath(this.mUserId, i, defaultCertificateAliasIfEmpty);
        if (recoveryServiceCertPath != null) {
            Log.d(TAG, "Using the public key in stored CertPath for syncing");
            recoveryServicePublicKey = recoveryServiceCertPath.getCertificates().get(0).getPublicKey();
        } else {
            Log.d(TAG, "Using the stored raw public key for syncing");
            recoveryServicePublicKey = this.mRecoverableKeyStoreDb.getRecoveryServicePublicKey(this.mUserId, i);
        }
        if (recoveryServicePublicKey == null) {
            Log.w(TAG, "Not initialized for KeySync: no public key set. Cancelling task.");
            return;
        }
        byte[] serverParams = this.mRecoverableKeyStoreDb.getServerParams(this.mUserId, i);
        if (serverParams == null) {
            Log.w(TAG, "No device ID set for user " + this.mUserId);
            return;
        }
        if (this.mTestOnlyInsecureCertificateHelper.isTestOnlyCertificateAlias(defaultCertificateAliasIfEmpty)) {
            Log.w(TAG, "Insecure root certificate is used by recovery agent " + i);
            if (this.mTestOnlyInsecureCertificateHelper.doesCredentialSupportInsecureMode(this.mCredentialType, this.mCredential)) {
                Log.w(TAG, "Whitelisted credential is used to generate snapshot by recovery agent " + i);
            } else {
                Log.w(TAG, "Non whitelisted credential is used to generate recovery snapshot by " + i + " - ignore attempt.");
                return;
            }
        }
        boolean zShouldUseScryptToHashCredential = shouldUseScryptToHashCredential();
        byte[] bArrGenerateSalt = generateSalt();
        if (zShouldUseScryptToHashCredential) {
            bArrHashCredentialsBySaltedSha256 = hashCredentialsByScrypt(bArrGenerateSalt, this.mCredential);
        } else {
            bArrHashCredentialsBySaltedSha256 = hashCredentialsBySaltedSha256(bArrGenerateSalt, this.mCredential);
        }
        try {
            Map<String, SecretKey> keysToSync = getKeysToSync(i);
            if (this.mTestOnlyInsecureCertificateHelper.isTestOnlyCertificateAlias(defaultCertificateAliasIfEmpty)) {
                keysToSync = this.mTestOnlyInsecureCertificateHelper.keepOnlyWhitelistedInsecureKeys(keysToSync);
            }
            try {
                SecretKey secretKeyGenerateRecoveryKey = generateRecoveryKey();
                try {
                    Map<String, byte[]> mapEncryptKeysWithRecoveryKey = KeySyncUtils.encryptKeysWithRecoveryKey(secretKeyGenerateRecoveryKey, keysToSync);
                    if (this.mCredentialUpdated || (counterId = this.mRecoverableKeyStoreDb.getCounterId(this.mUserId, i)) == null) {
                        counterId = Long.valueOf(generateAndStoreCounterId(i));
                    }
                    try {
                        byte[] bArrThmEncryptRecoveryKey = KeySyncUtils.thmEncryptRecoveryKey(recoveryServicePublicKey, bArrHashCredentialsBySaltedSha256, KeySyncUtils.packVaultParams(recoveryServicePublicKey, counterId.longValue(), 10, serverParams), secretKeyGenerateRecoveryKey);
                        if (zShouldUseScryptToHashCredential) {
                            keyDerivationParamsCreateSha256Params = KeyDerivationParams.createScryptParams(bArrGenerateSalt, 4096);
                        } else {
                            keyDerivationParamsCreateSha256Params = KeyDerivationParams.createSha256Params(bArrGenerateSalt);
                        }
                        KeyChainProtectionParams keyChainProtectionParamsBuild = new KeyChainProtectionParams.Builder().setUserSecretType(100).setLockScreenUiFormat(getUiFormat(this.mCredentialType, this.mCredential)).setKeyDerivationParams(keyDerivationParamsCreateSha256Params).setSecret(new byte[0]).build();
                        ArrayList arrayList = new ArrayList();
                        arrayList.add(keyChainProtectionParamsBuild);
                        KeyChainSnapshot.Builder encryptedRecoveryKeyBlob = new KeyChainSnapshot.Builder().setSnapshotVersion(getSnapshotVersion(i, z)).setMaxAttempts(10).setCounterId(counterId.longValue()).setServerParams(serverParams).setKeyChainProtectionParams(arrayList).setWrappedApplicationKeys(createApplicationKeyEntries(mapEncryptKeysWithRecoveryKey)).setEncryptedRecoveryKeyBlob(bArrThmEncryptRecoveryKey);
                        try {
                            encryptedRecoveryKeyBlob.setTrustedHardwareCertPath(recoveryServiceCertPath);
                            this.mRecoverySnapshotStorage.put(i, encryptedRecoveryKeyBlob.build());
                            this.mSnapshotListenersStorage.recoverySnapshotAvailable(i);
                            this.mRecoverableKeyStoreDb.setShouldCreateSnapshot(this.mUserId, i, false);
                        } catch (CertificateException e) {
                            Log.wtf(TAG, "Cannot serialize CertPath when calling setTrustedHardwareCertPath", e);
                        }
                    } catch (InvalidKeyException e2) {
                        Log.e(TAG, "Could not encrypt with recovery key", e2);
                    } catch (NoSuchAlgorithmException e3) {
                        Log.wtf(TAG, "SecureBox encrypt algorithms unavailable", e3);
                    }
                } catch (InvalidKeyException | NoSuchAlgorithmException e4) {
                    Log.wtf(TAG, "Should be impossible: could not encrypt application keys with random key", e4);
                }
            } catch (NoSuchAlgorithmException e5) {
                Log.wtf("AES should never be unavailable", e5);
            }
        } catch (BadPlatformKeyException e6) {
            Log.e(TAG, "Loaded keys for same generation ID as platform key, so BadPlatformKeyException should be impossible.", e6);
        } catch (InsecureUserException e7) {
            Log.e(TAG, "A screen unlock triggered the key sync flow, so user must have lock screen. This should be impossible.", e7);
        } catch (IOException e8) {
            Log.e(TAG, "Local database error.", e8);
        } catch (GeneralSecurityException e9) {
            Log.e(TAG, "Failed to load recoverable keys for sync", e9);
        }
    }

    @VisibleForTesting
    int getSnapshotVersion(int i, boolean z) throws IOException {
        Long lValueOf;
        Long snapshotVersion = this.mRecoverableKeyStoreDb.getSnapshotVersion(this.mUserId, i);
        if (!z) {
            lValueOf = Long.valueOf(snapshotVersion != null ? 1 + snapshotVersion.longValue() : 1L);
        } else {
            lValueOf = Long.valueOf(snapshotVersion != null ? snapshotVersion.longValue() : 1L);
        }
        if (this.mRecoverableKeyStoreDb.setSnapshotVersion(this.mUserId, i, lValueOf.longValue()) < 0) {
            Log.e(TAG, "Failed to set the snapshot version in the local DB.");
            throw new IOException("Failed to set the snapshot version in the local DB.");
        }
        return lValueOf.intValue();
    }

    private long generateAndStoreCounterId(int i) throws IOException {
        long jNextLong = new SecureRandom().nextLong();
        if (this.mRecoverableKeyStoreDb.setCounterId(this.mUserId, i, jNextLong) < 0) {
            Log.e(TAG, "Failed to set the snapshot version in the local DB.");
            throw new IOException("Failed to set counterId in the local DB.");
        }
        return jNextLong;
    }

    private Map<String, SecretKey> getKeysToSync(int i) throws NoSuchPaddingException, NoSuchAlgorithmException, UnrecoverableKeyException, BadPlatformKeyException, IOException, InvalidKeyException, KeyStoreException, InsecureUserException, InvalidAlgorithmParameterException {
        PlatformDecryptionKey decryptKey = this.mPlatformKeyManager.getDecryptKey(this.mUserId);
        return WrappedKey.unwrapKeys(decryptKey, this.mRecoverableKeyStoreDb.getAllKeys(this.mUserId, i, decryptKey.getGenerationId()));
    }

    private boolean shouldCreateSnapshot(int i) {
        if (!ArrayUtils.contains(this.mRecoverableKeyStoreDb.getRecoverySecretTypes(this.mUserId, i), 100)) {
            return false;
        }
        if (this.mCredentialUpdated && this.mRecoverableKeyStoreDb.getSnapshotVersion(this.mUserId, i) != null) {
            this.mRecoverableKeyStoreDb.setShouldCreateSnapshot(this.mUserId, i, true);
            return true;
        }
        return this.mRecoverableKeyStoreDb.getShouldCreateSnapshot(this.mUserId, i);
    }

    @VisibleForTesting
    static int getUiFormat(int i, String str) {
        if (i == 1) {
            return 3;
        }
        if (isPin(str)) {
            return 1;
        }
        return 2;
    }

    private static byte[] generateSalt() {
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        return bArr;
    }

    @VisibleForTesting
    static boolean isPin(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @VisibleForTesting
    static byte[] hashCredentialsBySaltedSha256(byte[] bArr, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArr.length + bytes.length + 8);
        byteBufferAllocate.order(ByteOrder.LITTLE_ENDIAN);
        byteBufferAllocate.putInt(bArr.length);
        byteBufferAllocate.put(bArr);
        byteBufferAllocate.putInt(bytes.length);
        byteBufferAllocate.put(bytes);
        try {
            return MessageDigest.getInstance(LOCK_SCREEN_HASH_ALGORITHM).digest(byteBufferAllocate.array());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] hashCredentialsByScrypt(byte[] bArr, String str) {
        return this.mScrypt.scrypt(str.getBytes(StandardCharsets.UTF_8), bArr, 4096, 8, 1, 32);
    }

    private static SecretKey generateRecoveryKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(RECOVERY_KEY_ALGORITHM);
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    private static List<WrappedApplicationKey> createApplicationKeyEntries(Map<String, byte[]> map) {
        ArrayList arrayList = new ArrayList();
        for (String str : map.keySet()) {
            arrayList.add(new WrappedApplicationKey.Builder().setAlias(str).setEncryptedKeyMaterial(map.get(str)).build());
        }
        return arrayList;
    }

    private boolean shouldUseScryptToHashCredential() {
        return this.mCredentialType == 2;
    }
}
