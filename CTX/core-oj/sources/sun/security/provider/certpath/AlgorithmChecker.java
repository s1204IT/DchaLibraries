package sun.security.provider.certpath;

import java.security.AlgorithmConstraints;
import java.security.AlgorithmParameters;
import java.security.CryptoPrimitive;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CRLException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import sun.security.util.AnchorCertificates;
import sun.security.util.CertConstraintParameters;
import sun.security.util.Debug;
import sun.security.util.DisabledAlgorithmConstraints;
import sun.security.util.KeyUtil;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X509CRLImpl;
import sun.security.x509.X509CertImpl;

public final class AlgorithmChecker extends PKIXCertPathChecker {
    private final AlgorithmConstraints constraints;
    private PublicKey prevPubKey;
    private boolean trustedMatch;
    private final PublicKey trustedPubKey;
    private static final Debug debug = Debug.getInstance("certpath");
    private static final Set<CryptoPrimitive> SIGNATURE_PRIMITIVE_SET = Collections.unmodifiableSet(EnumSet.of(CryptoPrimitive.SIGNATURE));
    private static final Set<CryptoPrimitive> KU_PRIMITIVE_SET = Collections.unmodifiableSet(EnumSet.of(CryptoPrimitive.SIGNATURE, CryptoPrimitive.KEY_ENCAPSULATION, CryptoPrimitive.PUBLIC_KEY_ENCRYPTION, CryptoPrimitive.KEY_AGREEMENT));
    private static final DisabledAlgorithmConstraints certPathDefaultConstraints = new DisabledAlgorithmConstraints(DisabledAlgorithmConstraints.PROPERTY_CERTPATH_DISABLED_ALGS);
    private static final boolean publicCALimits = certPathDefaultConstraints.checkProperty("jdkCA");

    public AlgorithmChecker(TrustAnchor trustAnchor) {
        this(trustAnchor, certPathDefaultConstraints);
    }

    public AlgorithmChecker(AlgorithmConstraints algorithmConstraints) {
        this.trustedMatch = false;
        this.prevPubKey = null;
        this.trustedPubKey = null;
        this.constraints = algorithmConstraints;
    }

    public AlgorithmChecker(TrustAnchor trustAnchor, AlgorithmConstraints algorithmConstraints) {
        this.trustedMatch = false;
        if (trustAnchor == null) {
            throw new IllegalArgumentException("The trust anchor cannot be null");
        }
        if (trustAnchor.getTrustedCert() != null) {
            this.trustedPubKey = trustAnchor.getTrustedCert().getPublicKey();
            this.trustedMatch = checkFingerprint(trustAnchor.getTrustedCert());
            if (this.trustedMatch && debug != null) {
                debug.println("trustedMatch = true");
            }
        } else {
            this.trustedPubKey = trustAnchor.getCAPublicKey();
        }
        this.prevPubKey = this.trustedPubKey;
        this.constraints = algorithmConstraints;
    }

    private static boolean checkFingerprint(X509Certificate x509Certificate) {
        if (!publicCALimits) {
            return false;
        }
        if (debug != null) {
            debug.println("AlgorithmChecker.contains: " + x509Certificate.getSigAlgName());
        }
        return AnchorCertificates.contains(x509Certificate);
    }

    @Override
    public void init(boolean z) throws CertPathValidatorException {
        if (!z) {
            if (this.trustedPubKey != null) {
                this.prevPubKey = this.trustedPubKey;
                return;
            } else {
                this.prevPubKey = null;
                return;
            }
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
        if (!(certificate instanceof X509Certificate) || this.constraints == null) {
            return;
        }
        X509Certificate x509Certificate = (X509Certificate) certificate;
        boolean[] keyUsage = x509Certificate.getKeyUsage();
        if (keyUsage != null && keyUsage.length < 9) {
            throw new CertPathValidatorException("incorrect KeyUsage extension", null, null, -1, PKIXReason.INVALID_KEY_USAGE);
        }
        Set<CryptoPrimitive> setNoneOf = KU_PRIMITIVE_SET;
        if (keyUsage != null) {
            setNoneOf = EnumSet.noneOf(CryptoPrimitive.class);
            if (keyUsage[0] || keyUsage[1] || keyUsage[5] || keyUsage[6]) {
                setNoneOf.add(CryptoPrimitive.SIGNATURE);
            }
            if (keyUsage[2]) {
                setNoneOf.add(CryptoPrimitive.KEY_ENCAPSULATION);
            }
            if (keyUsage[3]) {
                setNoneOf.add(CryptoPrimitive.PUBLIC_KEY_ENCRYPTION);
            }
            if (keyUsage[4]) {
                setNoneOf.add(CryptoPrimitive.KEY_AGREEMENT);
            }
            if (setNoneOf.isEmpty()) {
                throw new CertPathValidatorException("incorrect KeyUsage extension bits", null, null, -1, PKIXReason.INVALID_KEY_USAGE);
            }
        }
        PublicKey publicKey = certificate.getPublicKey();
        if (this.constraints instanceof DisabledAlgorithmConstraints) {
            ((DisabledAlgorithmConstraints) this.constraints).permits(setNoneOf, new CertConstraintParameters(x509Certificate, this.trustedMatch));
            if (this.prevPubKey == null) {
                this.prevPubKey = publicKey;
                return;
            }
        }
        try {
            X509CertImpl impl = X509CertImpl.toImpl((X509Certificate) certificate);
            AlgorithmParameters parameters = ((AlgorithmId) impl.get(X509CertImpl.SIG_ALG)).getParameters();
            String sigAlgName = impl.getSigAlgName();
            if (!(this.constraints instanceof DisabledAlgorithmConstraints)) {
                if (!this.constraints.permits(SIGNATURE_PRIMITIVE_SET, sigAlgName, parameters)) {
                    throw new CertPathValidatorException("Algorithm constraints check failed on signature algorithm: " + sigAlgName, null, null, -1, CertPathValidatorException.BasicReason.ALGORITHM_CONSTRAINED);
                }
                if (!this.constraints.permits(setNoneOf, publicKey)) {
                    throw new CertPathValidatorException("Algorithm constraints check failed on keysize: " + KeyUtil.getKeySize(publicKey), null, null, -1, CertPathValidatorException.BasicReason.ALGORITHM_CONSTRAINED);
                }
            }
            if (this.prevPubKey != null) {
                if (!this.constraints.permits(SIGNATURE_PRIMITIVE_SET, sigAlgName, this.prevPubKey, parameters)) {
                    throw new CertPathValidatorException("Algorithm constraints check failed on signature algorithm: " + sigAlgName, null, null, -1, CertPathValidatorException.BasicReason.ALGORITHM_CONSTRAINED);
                }
                if (PKIX.isDSAPublicKeyWithoutParams(publicKey)) {
                    if (!(this.prevPubKey instanceof DSAPublicKey)) {
                        throw new CertPathValidatorException("Input key is not of a appropriate type for inheriting parameters");
                    }
                    DSAParams params = ((DSAPublicKey) this.prevPubKey).getParams();
                    if (params == null) {
                        throw new CertPathValidatorException("Key parameters missing from public key.");
                    }
                    try {
                        publicKey = KeyFactory.getInstance("DSA").generatePublic(new DSAPublicKeySpec(((DSAPublicKey) publicKey).getY(), params.getP(), params.getQ(), params.getG()));
                    } catch (GeneralSecurityException e) {
                        throw new CertPathValidatorException("Unable to generate key with inherited parameters: " + e.getMessage(), e);
                    }
                }
            }
            this.prevPubKey = publicKey;
        } catch (CertificateException e2) {
            throw new CertPathValidatorException(e2);
        }
    }

    void trySetTrustAnchor(TrustAnchor trustAnchor) {
        if (this.prevPubKey == null) {
            if (trustAnchor == null) {
                throw new IllegalArgumentException("The trust anchor cannot be null");
            }
            if (trustAnchor.getTrustedCert() != null) {
                this.prevPubKey = trustAnchor.getTrustedCert().getPublicKey();
                this.trustedMatch = checkFingerprint(trustAnchor.getTrustedCert());
                if (this.trustedMatch && debug != null) {
                    debug.println("trustedMatch = true");
                    return;
                }
                return;
            }
            this.prevPubKey = trustAnchor.getCAPublicKey();
        }
    }

    static void check(PublicKey publicKey, X509CRL x509crl) throws CertPathValidatorException {
        try {
            check(publicKey, X509CRLImpl.toImpl(x509crl).getSigAlgId());
        } catch (CRLException e) {
            throw new CertPathValidatorException(e);
        }
    }

    static void check(PublicKey publicKey, AlgorithmId algorithmId) throws CertPathValidatorException {
        String name = algorithmId.getName();
        if (!certPathDefaultConstraints.permits(SIGNATURE_PRIMITIVE_SET, name, publicKey, algorithmId.getParameters())) {
            throw new CertPathValidatorException("Algorithm constraints check failed on signature algorithm: " + name + " is disabled", null, null, -1, CertPathValidatorException.BasicReason.ALGORITHM_CONSTRAINED);
        }
    }
}
