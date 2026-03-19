package sun.security.provider.certpath;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;

class BasicChecker extends PKIXCertPathChecker {
    private static final Debug debug = Debug.getInstance("certpath");
    private final X500Principal caName;
    private final Date date;
    private PublicKey prevPubKey;
    private X500Principal prevSubject;
    private final boolean sigOnly;
    private final String sigProvider;
    private final PublicKey trustedPubKey;

    BasicChecker(TrustAnchor trustAnchor, Date date, String str, boolean z) {
        if (trustAnchor.getTrustedCert() != null) {
            this.trustedPubKey = trustAnchor.getTrustedCert().getPublicKey();
            this.caName = trustAnchor.getTrustedCert().getSubjectX500Principal();
        } else {
            this.trustedPubKey = trustAnchor.getCAPublicKey();
            this.caName = trustAnchor.getCA();
        }
        this.date = date;
        this.sigProvider = str;
        this.sigOnly = z;
        this.prevPubKey = this.trustedPubKey;
    }

    @Override
    public void init(boolean z) throws CertPathValidatorException {
        if (!z) {
            this.prevPubKey = this.trustedPubKey;
            if (PKIX.isDSAPublicKeyWithoutParams(this.prevPubKey)) {
                throw new CertPathValidatorException("Key parameters missing");
            }
            this.prevSubject = this.caName;
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
        return null;
    }

    @Override
    public void check(Certificate certificate, Collection<String> collection) throws CertPathValidatorException {
        X509Certificate x509Certificate = (X509Certificate) certificate;
        if (!this.sigOnly) {
            verifyTimestamp(x509Certificate);
            verifyNameChaining(x509Certificate);
        }
        verifySignature(x509Certificate);
        updateState(x509Certificate);
    }

    private void verifySignature(X509Certificate x509Certificate) throws CertPathValidatorException {
        if (debug != null) {
            debug.println("---checking " + X509CertImpl.SIGNATURE + "...");
        }
        try {
            if (this.sigProvider != null) {
                x509Certificate.verify(this.prevPubKey, this.sigProvider);
            } else {
                x509Certificate.verify(this.prevPubKey);
            }
            if (debug != null) {
                debug.println(X509CertImpl.SIGNATURE + " verified.");
            }
        } catch (SignatureException e) {
            throw new CertPathValidatorException(X509CertImpl.SIGNATURE + " check failed", e, null, -1, CertPathValidatorException.BasicReason.INVALID_SIGNATURE);
        } catch (GeneralSecurityException e2) {
            throw new CertPathValidatorException(X509CertImpl.SIGNATURE + " check failed", e2);
        }
    }

    private void verifyTimestamp(X509Certificate x509Certificate) throws CertPathValidatorException {
        if (debug != null) {
            debug.println("---checking timestamp:" + this.date.toString() + "...");
        }
        try {
            x509Certificate.checkValidity(this.date);
            if (debug != null) {
                debug.println("timestamp verified.");
            }
        } catch (CertificateExpiredException e) {
            throw new CertPathValidatorException("timestamp check failed", e, null, -1, CertPathValidatorException.BasicReason.EXPIRED);
        } catch (CertificateNotYetValidException e2) {
            throw new CertPathValidatorException("timestamp check failed", e2, null, -1, CertPathValidatorException.BasicReason.NOT_YET_VALID);
        }
    }

    private void verifyNameChaining(X509Certificate x509Certificate) throws CertPathValidatorException {
        if (this.prevSubject != null) {
            if (debug != null) {
                debug.println("---checking subject/issuer name chaining...");
            }
            X500Principal issuerX500Principal = x509Certificate.getIssuerX500Principal();
            if (X500Name.asX500Name(issuerX500Principal).isEmpty()) {
                throw new CertPathValidatorException("subject/issuer name chaining check failed: empty/null issuer DN in certificate is invalid", null, null, -1, PKIXReason.NAME_CHAINING);
            }
            if (!issuerX500Principal.equals(this.prevSubject)) {
                throw new CertPathValidatorException("subject/issuer name chaining check failed", null, null, -1, PKIXReason.NAME_CHAINING);
            }
            if (debug != null) {
                debug.println("subject/issuer name chaining verified.");
            }
        }
    }

    private void updateState(X509Certificate x509Certificate) throws CertPathValidatorException {
        PublicKey publicKey = x509Certificate.getPublicKey();
        if (debug != null) {
            debug.println("BasicChecker.updateState issuer: " + x509Certificate.getIssuerX500Principal().toString() + "; subject: " + ((Object) x509Certificate.getSubjectX500Principal()) + "; serial#: " + x509Certificate.getSerialNumber().toString());
        }
        if (PKIX.isDSAPublicKeyWithoutParams(publicKey)) {
            publicKey = makeInheritedParamsKey(publicKey, this.prevPubKey);
            if (debug != null) {
                debug.println("BasicChecker.updateState Made key with inherited params");
            }
        }
        this.prevPubKey = publicKey;
        this.prevSubject = x509Certificate.getSubjectX500Principal();
    }

    static PublicKey makeInheritedParamsKey(PublicKey publicKey, PublicKey publicKey2) throws CertPathValidatorException {
        if (!(publicKey instanceof DSAPublicKey) || !(publicKey2 instanceof DSAPublicKey)) {
            throw new CertPathValidatorException("Input key is not appropriate type for inheriting parameters");
        }
        DSAParams params = ((DSAPublicKey) publicKey2).getParams();
        if (params == null) {
            throw new CertPathValidatorException("Key parameters missing");
        }
        try {
            return KeyFactory.getInstance("DSA").generatePublic(new DSAPublicKeySpec(((DSAPublicKey) publicKey).getY(), params.getP(), params.getQ(), params.getG()));
        } catch (GeneralSecurityException e) {
            throw new CertPathValidatorException("Unable to generate key with inherited parameters: " + e.getMessage(), e);
        }
    }

    PublicKey getPublicKey() {
        return this.prevPubKey;
    }
}
