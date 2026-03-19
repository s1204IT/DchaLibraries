package android.security.net.config;

import android.util.ArraySet;
import com.android.org.conscrypt.Hex;
import com.android.org.conscrypt.NativeCrypto;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import libcore.io.IoUtils;

abstract class DirectoryCertificateSource implements CertificateSource {
    private final CertificateFactory mCertFactory;
    private Set<X509Certificate> mCertificates;
    private final File mDir;
    private final Object mLock = new Object();

    private interface CertSelector {
        boolean match(X509Certificate x509Certificate);
    }

    protected abstract boolean isCertMarkedAsRemoved(String str);

    protected DirectoryCertificateSource(File caDir) {
        this.mDir = caDir;
        try {
            this.mCertFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException("Failed to obtain X.509 CertificateFactory", e);
        }
    }

    @Override
    public Set<X509Certificate> getCertificates() {
        X509Certificate cert;
        synchronized (this.mLock) {
            if (this.mCertificates != null) {
                return this.mCertificates;
            }
            Set<X509Certificate> certs = new ArraySet<>();
            if (this.mDir.isDirectory()) {
                for (String caFile : this.mDir.list()) {
                    if (!isCertMarkedAsRemoved(caFile) && (cert = readCertificate(caFile)) != null) {
                        certs.add(cert);
                    }
                }
            }
            this.mCertificates = certs;
            return this.mCertificates;
        }
    }

    @Override
    public X509Certificate findBySubjectAndPublicKey(final X509Certificate cert) {
        return findCert(cert.getSubjectX500Principal(), new CertSelector() {
            @Override
            public boolean match(X509Certificate ca) {
                return ca.getPublicKey().equals(cert.getPublicKey());
            }
        });
    }

    @Override
    public X509Certificate findByIssuerAndSignature(final X509Certificate cert) {
        return findCert(cert.getIssuerX500Principal(), new CertSelector() {
            @Override
            public boolean match(X509Certificate ca) {
                try {
                    cert.verify(ca.getPublicKey());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    @Override
    public Set<X509Certificate> findAllByIssuerAndSignature(final X509Certificate cert) {
        return findCerts(cert.getIssuerX500Principal(), new CertSelector() {
            @Override
            public boolean match(X509Certificate ca) {
                try {
                    cert.verify(ca.getPublicKey());
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

    private Set<X509Certificate> findCerts(X500Principal subj, CertSelector selector) throws Throwable {
        String hash = getHash(subj);
        Set<X509Certificate> certs = null;
        for (int index = 0; index >= 0; index++) {
            String fileName = hash + "." + index;
            if (!new File(this.mDir, fileName).exists()) {
                break;
            }
            if (!isCertMarkedAsRemoved(fileName)) {
                X509Certificate cert = readCertificate(fileName);
                if (subj.equals(cert.getSubjectX500Principal()) && selector.match(cert)) {
                    if (certs == null) {
                        certs = new ArraySet<>();
                    }
                    certs.add(cert);
                }
            }
        }
        return certs != null ? certs : Collections.emptySet();
    }

    private X509Certificate findCert(X500Principal subj, CertSelector selector) throws Throwable {
        String hash = getHash(subj);
        for (int index = 0; index >= 0; index++) {
            String fileName = hash + "." + index;
            if (new File(this.mDir, fileName).exists()) {
                if (!isCertMarkedAsRemoved(fileName)) {
                    X509Certificate cert = readCertificate(fileName);
                    if (subj.equals(cert.getSubjectX500Principal()) && selector.match(cert)) {
                        return cert;
                    }
                }
            } else {
                return null;
            }
        }
        return null;
    }

    private String getHash(X500Principal name) {
        int hash = NativeCrypto.X509_NAME_hash_old(name);
        return Hex.intToHexString(hash, 8);
    }

    private X509Certificate readCertificate(String file) throws Throwable {
        InputStream is;
        InputStream is2 = null;
        try {
            is = new BufferedInputStream(new FileInputStream(new File(this.mDir, file)));
        } catch (IOException | CertificateException e) {
        } catch (Throwable th) {
            th = th;
        }
        try {
            X509Certificate x509Certificate = (X509Certificate) this.mCertFactory.generateCertificate(is);
            IoUtils.closeQuietly(is);
            return x509Certificate;
        } catch (IOException | CertificateException e2) {
            is2 = is;
            IoUtils.closeQuietly(is2);
            return null;
        } catch (Throwable th2) {
            th = th2;
            is2 = is;
            IoUtils.closeQuietly(is2);
            throw th;
        }
    }
}
