package android.security.net.config;

import android.util.ArraySet;
import android.util.Log;
import com.android.org.conscrypt.Hex;
import com.android.org.conscrypt.NativeCrypto;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import libcore.io.IoUtils;

abstract class DirectoryCertificateSource implements CertificateSource {
    private static final String LOG_TAG = "DirectoryCertificateSrc";
    private final CertificateFactory mCertFactory;
    private Set<X509Certificate> mCertificates;
    private final File mDir;
    private final Object mLock = new Object();

    private interface CertSelector {
        boolean match(X509Certificate x509Certificate);
    }

    protected abstract boolean isCertMarkedAsRemoved(String str);

    protected DirectoryCertificateSource(File file) {
        this.mDir = file;
        try {
            this.mCertFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException("Failed to obtain X.509 CertificateFactory", e);
        }
    }

    @Override
    public Set<X509Certificate> getCertificates() {
        X509Certificate certificate;
        synchronized (this.mLock) {
            if (this.mCertificates != null) {
                return this.mCertificates;
            }
            ArraySet arraySet = new ArraySet();
            if (this.mDir.isDirectory()) {
                for (String str : this.mDir.list()) {
                    if (!isCertMarkedAsRemoved(str) && (certificate = readCertificate(str)) != null) {
                        arraySet.add(certificate);
                    }
                }
            }
            this.mCertificates = arraySet;
            return this.mCertificates;
        }
    }

    @Override
    public X509Certificate findBySubjectAndPublicKey(final X509Certificate x509Certificate) {
        return findCert(x509Certificate.getSubjectX500Principal(), new CertSelector() {
            @Override
            public boolean match(X509Certificate x509Certificate2) {
                return x509Certificate2.getPublicKey().equals(x509Certificate.getPublicKey());
            }
        });
    }

    @Override
    public X509Certificate findByIssuerAndSignature(final X509Certificate x509Certificate) {
        return findCert(x509Certificate.getIssuerX500Principal(), new CertSelector() {
            @Override
            public boolean match(X509Certificate x509Certificate2) {
                try {
                    x509Certificate.verify(x509Certificate2.getPublicKey());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    @Override
    public Set<X509Certificate> findAllByIssuerAndSignature(final X509Certificate x509Certificate) {
        return findCerts(x509Certificate.getIssuerX500Principal(), new CertSelector() {
            @Override
            public boolean match(X509Certificate x509Certificate2) {
                try {
                    x509Certificate.verify(x509Certificate2.getPublicKey());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    @Override
    public void handleTrustStorageUpdate() {
        synchronized (this.mLock) {
            this.mCertificates = null;
        }
    }

    private Set<X509Certificate> findCerts(X500Principal x500Principal, CertSelector certSelector) {
        X509Certificate certificate;
        String hash = getHash(x500Principal);
        ArraySet arraySet = null;
        for (int i = 0; i >= 0; i++) {
            String str = hash + "." + i;
            if (!new File(this.mDir, str).exists()) {
                break;
            }
            if (!isCertMarkedAsRemoved(str) && (certificate = readCertificate(str)) != null && x500Principal.equals(certificate.getSubjectX500Principal()) && certSelector.match(certificate)) {
                if (arraySet == null) {
                    arraySet = new ArraySet();
                }
                arraySet.add(certificate);
            }
        }
        return arraySet != null ? arraySet : Collections.emptySet();
    }

    private X509Certificate findCert(X500Principal x500Principal, CertSelector certSelector) {
        X509Certificate certificate;
        String hash = getHash(x500Principal);
        for (int i = 0; i >= 0; i++) {
            String str = hash + "." + i;
            if (new File(this.mDir, str).exists()) {
                if (!isCertMarkedAsRemoved(str) && (certificate = readCertificate(str)) != null && x500Principal.equals(certificate.getSubjectX500Principal()) && certSelector.match(certificate)) {
                    return certificate;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    private String getHash(X500Principal x500Principal) {
        return Hex.intToHexString(NativeCrypto.X509_NAME_hash_old(x500Principal), 8);
    }

    private X509Certificate readCertificate(String str) throws Throwable {
        BufferedInputStream bufferedInputStream;
        try {
            try {
                bufferedInputStream = new BufferedInputStream(new FileInputStream(new File(this.mDir, str)));
                try {
                    X509Certificate x509Certificate = (X509Certificate) this.mCertFactory.generateCertificate(bufferedInputStream);
                    IoUtils.closeQuietly(bufferedInputStream);
                    return x509Certificate;
                } catch (IOException | CertificateException e) {
                    e = e;
                    Log.e(LOG_TAG, "Failed to read certificate from " + str, e);
                    IoUtils.closeQuietly(bufferedInputStream);
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
        } catch (IOException | CertificateException e2) {
            e = e2;
            bufferedInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }
}
