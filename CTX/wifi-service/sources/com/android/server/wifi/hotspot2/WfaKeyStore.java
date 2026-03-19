package com.android.server.wifi.hotspot2;

import android.os.Environment;
import android.util.Log;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;

public class WfaKeyStore {
    private static final String DEFAULT_WFA_CERT_DIR = Environment.getRootDirectory() + "/etc/security/cacerts_wfa";
    private static final String TAG = "WfaKeyStore";
    private boolean mVerboseLoggingEnabled = false;
    private KeyStore mKeyStore = null;

    public void load() {
        if (this.mKeyStore != null) {
            return;
        }
        try {
            this.mKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            this.mKeyStore.load(null, null);
            Iterator<X509Certificate> it = WfaCertBuilder.loadCertsFromDisk(DEFAULT_WFA_CERT_DIR).iterator();
            int i = 0;
            while (it.hasNext()) {
                this.mKeyStore.setCertificateEntry(String.format("%d", Integer.valueOf(i)), it.next());
                i++;
            }
            if (i <= 0) {
                Log.wtf(TAG, "No certs loaded");
            }
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }
    }

    public KeyStore get() {
        return this.mKeyStore;
    }
}
