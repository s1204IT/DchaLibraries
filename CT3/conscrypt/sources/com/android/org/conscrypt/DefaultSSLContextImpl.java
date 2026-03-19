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
        BufferedInputStream bufferedInputStream;
        if (KEY_MANAGERS != null) {
            return KEY_MANAGERS;
        }
        String keystore = System.getProperty("javax.net.ssl.keyStore");
        if (keystore == null) {
            return null;
        }
        String keystorepwd = System.getProperty("javax.net.ssl.keyStorePassword");
        char[] charArray = keystorepwd == null ? null : keystorepwd.toCharArray();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        BufferedInputStream bufferedInputStream2 = null;
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(keystore));
        } catch (Throwable th) {
            th = th;
        }
        try {
            ks.load(bufferedInputStream, charArray);
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            String kmfAlg = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmfAlg);
            kmf.init(ks, charArray);
            KEY_MANAGERS = kmf.getKeyManagers();
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
        BufferedInputStream bufferedInputStream;
        if (TRUST_MANAGERS != null) {
            return TRUST_MANAGERS;
        }
        String keystore = System.getProperty("javax.net.ssl.trustStore");
        if (keystore == null) {
            return null;
        }
        String keystorepwd = System.getProperty("javax.net.ssl.trustStorePassword");
        char[] charArray = keystorepwd == null ? null : keystorepwd.toCharArray();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        BufferedInputStream bufferedInputStream2 = null;
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(keystore));
        } catch (Throwable th) {
            th = th;
        }
        try {
            ks.load(bufferedInputStream, charArray);
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            String tmfAlg = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlg);
            tmf.init(ks);
            TRUST_MANAGERS = tmf.getTrustManagers();
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
    public void engineInit(KeyManager[] kms, TrustManager[] tms, SecureRandom sr) throws KeyManagementException {
        throw new KeyManagementException("Do not init() the default SSLContext ");
    }
}
