package com.android.org.conscrypt;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public final class DefaultSSLContextImpl extends OpenSSLContextImpl {
    private static KeyManager[] KEY_MANAGERS;
    private static TrustManager[] TRUST_MANAGERS;

    KeyManager[] getKeyManagers() throws Throwable {
        char[] charArray;
        BufferedInputStream bufferedInputStream;
        if (KEY_MANAGERS != null) {
            return KEY_MANAGERS;
        }
        String property = System.getProperty("javax.net.ssl.keyStore");
        BufferedInputStream bufferedInputStream2 = null;
        if (property == null) {
            return null;
        }
        String property2 = System.getProperty("javax.net.ssl.keyStorePassword");
        if (property2 != null) {
            charArray = property2.toCharArray();
        } else {
            charArray = null;
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(property));
        } catch (Throwable th) {
            th = th;
        }
        try {
            keyStore.load(bufferedInputStream, charArray);
            bufferedInputStream.close();
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, charArray);
            KEY_MANAGERS = keyManagerFactory.getKeyManagers();
            return KEY_MANAGERS;
        } catch (Throwable th2) {
            th = th2;
            bufferedInputStream2 = bufferedInputStream;
            if (bufferedInputStream2 != null) {
                bufferedInputStream2.close();
            }
            throw th;
        }
    }

    TrustManager[] getTrustManagers() throws Throwable {
        char[] charArray;
        BufferedInputStream bufferedInputStream;
        if (TRUST_MANAGERS != null) {
            return TRUST_MANAGERS;
        }
        String property = System.getProperty("javax.net.ssl.trustStore");
        BufferedInputStream bufferedInputStream2 = null;
        if (property == null) {
            return null;
        }
        String property2 = System.getProperty("javax.net.ssl.trustStorePassword");
        if (property2 != null) {
            charArray = property2.toCharArray();
        } else {
            charArray = null;
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(property));
        } catch (Throwable th) {
            th = th;
        }
        try {
            keyStore.load(bufferedInputStream, charArray);
            bufferedInputStream.close();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TRUST_MANAGERS = trustManagerFactory.getTrustManagers();
            return TRUST_MANAGERS;
        } catch (Throwable th2) {
            th = th2;
            bufferedInputStream2 = bufferedInputStream;
            if (bufferedInputStream2 != null) {
                bufferedInputStream2.close();
            }
            throw th;
        }
    }

    @Override
    public void engineInit(KeyManager[] keyManagerArr, TrustManager[] trustManagerArr, SecureRandom secureRandom) throws KeyManagementException {
        throw new KeyManagementException("Do not init() the default SSLContext ");
    }
}
