package com.android.server.locksettings.recoverablekeystore;

import android.app.KeyguardManager;
import android.content.Context;
import android.security.keystore.KeyProtection;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverableKeyStoreDb;
import com.android.server.slice.SliceClientPermissions;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Locale;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class PlatformKeyManager {
    private static final String ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore";
    private static final String DECRYPT_KEY_ALIAS_SUFFIX = "decrypt";
    private static final String ENCRYPT_KEY_ALIAS_SUFFIX = "encrypt";
    private static final String KEY_ALGORITHM = "AES";
    private static final String KEY_ALIAS_PREFIX = "com.android.server.locksettings.recoverablekeystore/platform/";
    private static final int KEY_SIZE_BITS = 256;
    private static final String TAG = "PlatformKeyManager";
    private static final int USER_AUTHENTICATION_VALIDITY_DURATION_SECONDS = 15;
    private final Context mContext;
    private final RecoverableKeyStoreDb mDatabase;
    private final KeyStoreProxy mKeyStore;

    public static PlatformKeyManager getInstance(Context context, RecoverableKeyStoreDb recoverableKeyStoreDb) throws NoSuchAlgorithmException, KeyStoreException {
        return new PlatformKeyManager(context.getApplicationContext(), new KeyStoreProxyImpl(getAndLoadAndroidKeyStore()), recoverableKeyStoreDb);
    }

    @VisibleForTesting
    PlatformKeyManager(Context context, KeyStoreProxy keyStoreProxy, RecoverableKeyStoreDb recoverableKeyStoreDb) {
        this.mKeyStore = keyStoreProxy;
        this.mContext = context;
        this.mDatabase = recoverableKeyStoreDb;
    }

    public int getGenerationId(int i) {
        return this.mDatabase.getPlatformKeyGenerationId(i);
    }

    public boolean isAvailable(int i) {
        return ((KeyguardManager) this.mContext.getSystemService(KeyguardManager.class)).isDeviceSecure(i);
    }

    public void invalidatePlatformKey(int i, int i2) {
        if (i2 != -1) {
            try {
                this.mKeyStore.deleteEntry(getEncryptAlias(i, i2));
                this.mKeyStore.deleteEntry(getDecryptAlias(i, i2));
            } catch (KeyStoreException e) {
            }
        }
    }

    @VisibleForTesting
    void regenerate(int i) throws NoSuchAlgorithmException, IOException, KeyStoreException, InsecureUserException {
        int i2 = 1;
        if (!isAvailable(i)) {
            throw new InsecureUserException(String.format(Locale.US, "%d does not have a lock screen set.", Integer.valueOf(i)));
        }
        int generationId = getGenerationId(i);
        if (generationId != -1) {
            invalidatePlatformKey(i, generationId);
            i2 = 1 + generationId;
        }
        generateAndLoadKey(i, i2);
    }

    public PlatformEncryptionKey getEncryptKey(int i) throws NoSuchAlgorithmException, UnrecoverableKeyException, IOException, KeyStoreException, InsecureUserException {
        init(i);
        try {
            getDecryptKeyInternal(i);
            return getEncryptKeyInternal(i);
        } catch (UnrecoverableKeyException e) {
            Log.i(TAG, String.format(Locale.US, "Regenerating permanently invalid Platform key for user %d.", Integer.valueOf(i)));
            regenerate(i);
            return getEncryptKeyInternal(i);
        }
    }

    private PlatformEncryptionKey getEncryptKeyInternal(int i) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, InsecureUserException {
        int generationId = getGenerationId(i);
        String encryptAlias = getEncryptAlias(i, generationId);
        if (!isKeyLoaded(i, generationId)) {
            throw new UnrecoverableKeyException("KeyStore doesn't contain key " + encryptAlias);
        }
        return new PlatformEncryptionKey(generationId, this.mKeyStore.getKey(encryptAlias, null));
    }

    public PlatformDecryptionKey getDecryptKey(int i) throws NoSuchAlgorithmException, UnrecoverableKeyException, IOException, KeyStoreException, InsecureUserException {
        init(i);
        try {
            return getDecryptKeyInternal(i);
        } catch (UnrecoverableKeyException e) {
            Log.i(TAG, String.format(Locale.US, "Regenerating permanently invalid Platform key for user %d.", Integer.valueOf(i)));
            regenerate(i);
            return getDecryptKeyInternal(i);
        }
    }

    private PlatformDecryptionKey getDecryptKeyInternal(int i) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, InsecureUserException {
        int generationId = getGenerationId(i);
        String decryptAlias = getDecryptAlias(i, generationId);
        if (!isKeyLoaded(i, generationId)) {
            throw new UnrecoverableKeyException("KeyStore doesn't contain key " + decryptAlias);
        }
        return new PlatformDecryptionKey(generationId, this.mKeyStore.getKey(decryptAlias, null));
    }

    void init(int i) throws NoSuchAlgorithmException, IOException, KeyStoreException, InsecureUserException {
        int i2 = 1;
        if (!isAvailable(i)) {
            throw new InsecureUserException(String.format(Locale.US, "%d does not have a lock screen set.", Integer.valueOf(i)));
        }
        int generationId = getGenerationId(i);
        if (isKeyLoaded(i, generationId)) {
            Log.i(TAG, String.format(Locale.US, "Platform key generation %d exists already.", Integer.valueOf(generationId)));
            return;
        }
        if (generationId == -1) {
            Log.i(TAG, "Generating initial platform key generation ID.");
        } else {
            Log.w(TAG, String.format(Locale.US, "Platform generation ID was %d but no entry was present in AndroidKeyStore. Generating fresh key.", Integer.valueOf(generationId)));
            i2 = 1 + generationId;
        }
        generateAndLoadKey(i, i2);
    }

    private String getEncryptAlias(int i, int i2) {
        return KEY_ALIAS_PREFIX + i + SliceClientPermissions.SliceAuthority.DELIMITER + i2 + SliceClientPermissions.SliceAuthority.DELIMITER + ENCRYPT_KEY_ALIAS_SUFFIX;
    }

    private String getDecryptAlias(int i, int i2) {
        return KEY_ALIAS_PREFIX + i + SliceClientPermissions.SliceAuthority.DELIMITER + i2 + SliceClientPermissions.SliceAuthority.DELIMITER + DECRYPT_KEY_ALIAS_SUFFIX;
    }

    private void setGenerationId(int i, int i2) throws IOException {
        if (this.mDatabase.setPlatformKeyGenerationId(i, i2) < 0) {
            throw new IOException("Failed to set the platform key in the local DB.");
        }
    }

    private boolean isKeyLoaded(int i, int i2) throws KeyStoreException {
        return this.mKeyStore.containsAlias(getEncryptAlias(i, i2)) && this.mKeyStore.containsAlias(getDecryptAlias(i, i2));
    }

    private void generateAndLoadKey(int i, int i2) throws NoSuchAlgorithmException, IOException, KeyStoreException {
        String encryptAlias = getEncryptAlias(i, i2);
        String decryptAlias = getDecryptAlias(i, i2);
        SecretKey secretKeyGenerateAesKey = generateAesKey();
        this.mKeyStore.setEntry(decryptAlias, new KeyStore.SecretKeyEntry(secretKeyGenerateAesKey), new KeyProtection.Builder(2).setUserAuthenticationRequired(true).setUserAuthenticationValidityDurationSeconds(15).setBlockModes("GCM").setEncryptionPaddings("NoPadding").setBoundToSpecificSecureUserId(i).build());
        this.mKeyStore.setEntry(encryptAlias, new KeyStore.SecretKeyEntry(secretKeyGenerateAesKey), new KeyProtection.Builder(1).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
        setGenerationId(i, i2);
    }

    private static SecretKey generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    private static KeyStore getAndLoadAndroidKeyStore() throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER);
        try {
            keyStore.load(null);
            return keyStore;
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new KeyStoreException("Unable to load keystore.", e);
        }
    }
}
