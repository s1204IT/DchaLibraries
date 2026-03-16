package java.security.cert;

import java.security.PublicKey;

public class PKIXCertPathBuilderResult extends PKIXCertPathValidatorResult implements CertPathBuilderResult {
    private final CertPath certPath;

    public PKIXCertPathBuilderResult(CertPath certPath, TrustAnchor trustAnchor, PolicyNode policyTree, PublicKey subjectPublicKey) {
        super(trustAnchor, policyTree, subjectPublicKey);
        if (certPath == null) {
            throw new NullPointerException("certPath == null");
        }
        this.certPath = certPath;
    }

    @Override
    public CertPath getCertPath() {
        return this.certPath;
    }

    @Override
    public String toString() {
        return super.toString() + "\n Certification Path: " + this.certPath.toString() + "\n]";
    }
}
