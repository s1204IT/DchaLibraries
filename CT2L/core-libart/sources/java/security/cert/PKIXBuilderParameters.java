package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Set;

public class PKIXBuilderParameters extends PKIXParameters {
    private int maxPathLength;

    public PKIXBuilderParameters(Set<TrustAnchor> trustAnchors, CertSelector targetConstraints) throws InvalidAlgorithmParameterException {
        super(trustAnchors);
        this.maxPathLength = 5;
        super.setTargetCertConstraints(targetConstraints);
    }

    public PKIXBuilderParameters(KeyStore keyStore, CertSelector targetConstraints) throws KeyStoreException, InvalidAlgorithmParameterException {
        super(keyStore);
        this.maxPathLength = 5;
        super.setTargetCertConstraints(targetConstraints);
    }

    public int getMaxPathLength() {
        return this.maxPathLength;
    }

    public void setMaxPathLength(int maxPathLength) {
        if (maxPathLength < -1) {
            throw new InvalidParameterException("maxPathLength < -1");
        }
        this.maxPathLength = maxPathLength;
    }

    @Override
    public String toString() {
        return "[\n" + super.toString() + " Max Path Length: " + this.maxPathLength + "\n]";
    }
}
