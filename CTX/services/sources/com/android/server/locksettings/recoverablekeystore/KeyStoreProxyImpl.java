package com.android.server.locksettings.recoverablekeystore;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class KeyStoreProxyImpl implements KeyStoreProxy {
    private static final String ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore";
    private final KeyStore mKeyStore;

    public KeyStoreProxyImpl(KeyStore keyStore) {
        this.mKeyStore = keyStore;
    }

    @Override
    public boolean containsAlias(String str) throws KeyStoreException {
        return this.mKeyStore.containsAlias(str);
    }

    @Override
    public Key getKey(String str, char[] cArr) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        return this.mKeyStore.getKey(str, cArr);
    }

    @Override
    public void setEntry(String str, KeyStore.Entry entry, KeyStore.ProtectionParameter protectionParameter) throws KeyStoreException {
        this.mKeyStore.setEntry(str, entry, protectionParameter);
    }

    @Override
    public void deleteEntry(String str) throws KeyStoreException {
        this.mKeyStore.deleteEntry(str);
    }

    public static KeyStore getAndLoadAndroidKeyStore() throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER);
        try {
            keyStore.load(null);
            return keyStore;
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new KeyStoreException("Unable to load keystore.", e);
        }
    }
}
