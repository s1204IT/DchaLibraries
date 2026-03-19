package java.security.cert;

import java.security.PublicKey;

public class PKIXCertPathValidatorResult implements CertPathValidatorResult {
    private PolicyNode policyTree;
    private PublicKey subjectPublicKey;
    private TrustAnchor trustAnchor;

    public PKIXCertPathValidatorResult(TrustAnchor trustAnchor, PolicyNode policyNode, PublicKey publicKey) {
        if (publicKey == null) {
            throw new NullPointerException("subjectPublicKey must be non-null");
        }
        if (trustAnchor == null) {
            throw new NullPointerException("trustAnchor must be non-null");
        }
        this.trustAnchor = trustAnchor;
        this.policyTree = policyNode;
        this.subjectPublicKey = publicKey;
    }

    public TrustAnchor getTrustAnchor() {
        return this.trustAnchor;
    }

    public PolicyNode getPolicyTree() {
        return this.policyTree;
    }

    public PublicKey getPublicKey() {
        return this.subjectPublicKey;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("PKIXCertPathValidatorResult: [\n");
        stringBuffer.append("  Trust Anchor: " + this.trustAnchor.toString() + "\n");
        stringBuffer.append("  Policy Tree: " + String.valueOf(this.policyTree) + "\n");
        stringBuffer.append("  Subject Public Key: " + ((Object) this.subjectPublicKey) + "\n");
        stringBuffer.append("]");
        return stringBuffer.toString();
    }
}
