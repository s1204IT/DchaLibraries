package com.android.org.conscrypt;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
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

    public DefaultSSLContextImpl() throws GeneralSecurityException, IOException {
        super(null);
    }

    KeyManager[] getKeyManagers() throws Throwable {
        if (KEY_MANAGERS != null) {
            return KEY_MANAGERS;
        }
        String keystore = System.getProperty("javax.net.ssl.keyStore");
        if (keystore == null) {
            return null;
        }
        String keystorepwd = System.getProperty("javax.net.ssl.keyStorePassword");
        char[] pwd = keystorepwd != null ? keystorepwd.toCharArray() : null;
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream is = null;
        try {
            InputStream is2 = new BufferedInputStream(new FileInputStream(keystore));
            try {
                ks.load(is2, pwd);
                if (is2 != null) {
                    is2.close();
                }
                String kmfAlg = KeyManagerFactory.getDefaultAlgorithm();
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmfAlg);
                kmf.init(ks, pwd);
                KEY_MANAGERS = kmf.getKeyManagers();
                return KEY_MANAGERS;
            } catch (Throwable th) {
                th = th;
                is = is2;
                if (is != null) {
                    is.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    TrustManager[] getTrustManagers() throws Throwable {
        if (TRUST_MANAGERS != null) {
            return TRUST_MANAGERS;
        }
        String keystore = System.getProperty("javax.net.ssl.trustStore");
        if (keystore == null) {
            return null;
        }
        String keystorepwd = System.getProperty("javax.net.ssl.trustStorePassword");
        char[] pwd = keystorepwd != null ? keystorepwd.toCharArray() : null;
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream is = null;
        try {
            InputStream is2 = new BufferedInputStream(new FileInputStream(keystore));
            try {
                ks.load(is2, pwd);
                if (is2 != null) {
                    is2.close();
                }
                String tmfAlg = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlg);
                tmf.init(ks);
                TRUST_MANAGERS = tmf.getTrustManagers();
                return TRUST_MANAGERS;
            } catch (Throwable th) {
                th = th;
                is = is2;
                if (is != null) {
                    is.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override
    public void engineInit(KeyManager[] kms, TrustManager[] tms, SecureRandom sr) throws KeyManagementException {
        throw new KeyManagementException("Do not init() the default SSLContext ");
    }
}
