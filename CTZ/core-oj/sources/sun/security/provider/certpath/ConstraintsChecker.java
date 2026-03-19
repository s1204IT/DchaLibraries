package sun.security.provider.certpath;

import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import sun.security.util.Debug;
import sun.security.x509.NameConstraintsExtension;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.X509CertImpl;

class ConstraintsChecker extends PKIXCertPathChecker {
    private static final Debug debug = Debug.getInstance("certpath");
    private final int certPathLength;
    private int i;
    private int maxPathLength;
    private NameConstraintsExtension prevNC;
    private Set<String> supportedExts;

    ConstraintsChecker(int i) {
        this.certPathLength = i;
    }

    @Override
    public void init(boolean z) throws CertPathValidatorException {
        if (!z) {
            this.i = 0;
            this.maxPathLength = this.certPathLength;
            this.prevNC = null;
            return;
        }
        throw new CertPathValidatorException("forward checking not supported");
    }

    @Override
    public boolean isForwardCheckingSupported() {
        return false;
    }

    @Override
    public Set<String> getSupportedExtensions() {
        if (this.supportedExts == null) {
            this.supportedExts = new HashSet(2);
            this.supportedExts.add(PKIXExtensions.BasicConstraints_Id.toString());
            this.supportedExts.add(PKIXExtensions.NameConstraints_Id.toString());
            this.supportedExts = Collections.unmodifiableSet(this.supportedExts);
        }
        return this.supportedExts;
    }

    @Override
    public void check(Certificate certificate, Collection<String> collection) throws CertPathValidatorException {
        X509Certificate x509Certificate = (X509Certificate) certificate;
        this.i++;
        checkBasicConstraints(x509Certificate);
        verifyNameConstraints(x509Certificate);
        if (collection != null && !collection.isEmpty()) {
            collection.remove(PKIXExtensions.BasicConstraints_Id.toString());
            collection.remove(PKIXExtensions.NameConstraints_Id.toString());
        }
    }

    private void verifyNameConstraints(X509Certificate x509Certificate) throws CertPathValidatorException {
        if (debug != null) {
            debug.println("---checking name constraints...");
        }
        if (this.prevNC != null && (this.i == this.certPathLength || !X509CertImpl.isSelfIssued(x509Certificate))) {
            if (debug != null) {
                debug.println("prevNC = " + ((Object) this.prevNC) + ", currDN = " + ((Object) x509Certificate.getSubjectX500Principal()));
            }
            try {
                if (!this.prevNC.verify(x509Certificate)) {
                    throw new CertPathValidatorException("name constraints check failed", null, null, -1, PKIXReason.INVALID_NAME);
                }
            } catch (IOException e) {
                throw new CertPathValidatorException(e);
            }
        }
        this.prevNC = mergeNameConstraints(x509Certificate, this.prevNC);
        if (debug != null) {
            debug.println("name constraints verified.");
        }
    }

    static NameConstraintsExtension mergeNameConstraints(X509Certificate x509Certificate, NameConstraintsExtension nameConstraintsExtension) throws CertPathValidatorException {
        try {
            NameConstraintsExtension nameConstraintsExtension2 = X509CertImpl.toImpl(x509Certificate).getNameConstraintsExtension();
            if (debug != null) {
                debug.println("prevNC = " + ((Object) nameConstraintsExtension) + ", newNC = " + String.valueOf(nameConstraintsExtension2));
            }
            if (nameConstraintsExtension == null) {
                if (debug != null) {
                    debug.println("mergedNC = " + String.valueOf(nameConstraintsExtension2));
                }
                if (nameConstraintsExtension2 == null) {
                    return nameConstraintsExtension2;
                }
                return (NameConstraintsExtension) nameConstraintsExtension2.clone();
            }
            try {
                nameConstraintsExtension.merge(nameConstraintsExtension2);
                if (debug != null) {
                    debug.println("mergedNC = " + ((Object) nameConstraintsExtension));
                }
                return nameConstraintsExtension;
            } catch (IOException e) {
                throw new CertPathValidatorException(e);
            }
        } catch (CertificateException e2) {
            throw new CertPathValidatorException(e2);
        }
    }

    private void checkBasicConstraints(X509Certificate x509Certificate) throws CertPathValidatorException {
        int basicConstraints;
        if (debug != null) {
            debug.println("---checking basic constraints...");
            debug.println("i = " + this.i + ", maxPathLength = " + this.maxPathLength);
        }
        if (this.i < this.certPathLength) {
            if (x509Certificate.getVersion() < 3) {
                if (this.i == 1 && X509CertImpl.isSelfIssued(x509Certificate)) {
                    basicConstraints = Integer.MAX_VALUE;
                } else {
                    basicConstraints = -1;
                }
            } else {
                basicConstraints = x509Certificate.getBasicConstraints();
            }
            if (basicConstraints == -1) {
                throw new CertPathValidatorException("basic constraints check failed: this is not a CA certificate", null, null, -1, PKIXReason.NOT_CA_CERT);
            }
            if (!X509CertImpl.isSelfIssued(x509Certificate)) {
                if (this.maxPathLength <= 0) {
                    throw new CertPathValidatorException("basic constraints check failed: pathLenConstraint violated - this cert must be the last cert in the certification path", null, null, -1, PKIXReason.PATH_TOO_LONG);
                }
                this.maxPathLength--;
            }
            if (basicConstraints < this.maxPathLength) {
                this.maxPathLength = basicConstraints;
            }
        }
        if (debug != null) {
            debug.println("after processing, maxPathLength = " + this.maxPathLength);
            debug.println("basic constraints verified.");
        }
    }

    static int mergeBasicConstraints(X509Certificate x509Certificate, int i) {
        int basicConstraints = x509Certificate.getBasicConstraints();
        if (!X509CertImpl.isSelfIssued(x509Certificate)) {
            i--;
        }
        return basicConstraints < i ? basicConstraints : i;
    }
}
