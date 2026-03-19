package com.android.server.locksettings.recoverablekeystore.storage;

import android.os.ServiceSpecificException;
import android.security.KeyStore;
import android.security.keystore.KeyProtection;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.locksettings.recoverablekeystore.KeyStoreProxy;
import com.android.server.locksettings.recoverablekeystore.KeyStoreProxyImpl;
import com.android.server.slice.SliceClientPermissions;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Locale;
import javax.crypto.spec.SecretKeySpec;

public class ApplicationKeyStorage {
    private static final String APPLICATION_KEY_ALIAS_PREFIX = "com.android.server.locksettings.recoverablekeystore/application/";
    private static final String TAG = "RecoverableAppKeyStore";
    private final KeyStoreProxy mKeyStore;
    private final KeyStore mKeystoreService;

    public static ApplicationKeyStorage getInstance(KeyStore keyStore) throws KeyStoreException {
        return new ApplicationKeyStorage(new KeyStoreProxyImpl(KeyStoreProxyImpl.getAndLoadAndroidKeyStore()), keyStore);
    }

    @VisibleForTesting
    ApplicationKeyStorage(KeyStoreProxy keyStoreProxy, KeyStore keyStore) {
        this.mKeyStore = keyStoreProxy;
        this.mKeystoreService = keyStore;
    }

    public String getGrantAlias(int i, int i2, String str) {
        Log.i(TAG, String.format(Locale.US, "Get %d/%d/%s", Integer.valueOf(i), Integer.valueOf(i2), str));
        return this.mKeystoreService.grant("USRPKEY_" + getInternalAlias(i, i2, str), i2);
    }

    public void setSymmetricKeyEntry(int i, int i2, String str, byte[] bArr) throws KeyStoreException, ServiceSpecificException {
        Log.i(TAG, String.format(Locale.US, "Set %d/%d/%s: %d bytes of key material", Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(bArr.length)));
        try {
            this.mKeyStore.setEntry(getInternalAlias(i, i2, str), new KeyStore.SecretKeyEntry(new SecretKeySpec(bArr, "AES")), new KeyProtection.Builder(3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
        } catch (KeyStoreException e) {
            throw new ServiceSpecificException(22, e.getMessage());
        }
    }

    public void deleteEntry(int i, int i2, String str) throws ServiceSpecificException {
        Log.i(TAG, String.format(Locale.US, "Del %d/%d/%s", Integer.valueOf(i), Integer.valueOf(i2), str));
        try {
            this.mKeyStore.deleteEntry(getInternalAlias(i, i2, str));
        } catch (KeyStoreException e) {
            throw new ServiceSpecificException(22, e.getMessage());
        }
    }

    private String getInternalAlias(int i, int i2, String str) {
        return APPLICATION_KEY_ALIAS_PREFIX + i + SliceClientPermissions.SliceAuthority.DELIMITER + i2 + SliceClientPermissions.SliceAuthority.DELIMITER + str;
    }
}
