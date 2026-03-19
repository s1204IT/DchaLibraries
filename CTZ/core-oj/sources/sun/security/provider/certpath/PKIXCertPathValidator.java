package sun.security.provider.certpath;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPath;
import java.security.cert.CertPathChecker;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertPathValidatorSpi;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXReason;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sun.security.provider.certpath.PKIX;
import sun.security.util.Debug;
import sun.security.x509.X509CertImpl;

public final class PKIXCertPathValidator extends CertPathValidatorSpi {
    private static final Debug debug = Debug.getInstance("certpath");

    @Override
    public CertPathChecker engineGetRevocationChecker() {
        return new RevocationChecker();
    }

    @Override
    public CertPathValidatorResult engineValidate(CertPath certPath, CertPathParameters certPathParameters) throws CertPathValidatorException, InvalidAlgorithmParameterException {
        return validate(PKIX.checkParams(certPath, certPathParameters));
    }

    private static PKIXCertPathValidatorResult validate(PKIX.ValidatorParams validatorParams) throws CertPathValidatorException {
        AdaptableX509CertSelector adaptableX509CertSelector;
        if (debug != null) {
            debug.println("PKIXCertPathValidator.engineValidate()...");
        }
        List<X509Certificate> listCertificates = validatorParams.certificates();
        CertPathValidatorException e = null;
        if (!listCertificates.isEmpty()) {
            adaptableX509CertSelector = new AdaptableX509CertSelector();
            X509Certificate x509Certificate = listCertificates.get(0);
            adaptableX509CertSelector.setSubject(x509Certificate.getIssuerX500Principal());
            try {
                adaptableX509CertSelector.setSkiAndSerialNumber(X509CertImpl.toImpl(x509Certificate).getAuthorityKeyIdentifierExtension());
            } catch (IOException | CertificateException e2) {
            }
        } else {
            adaptableX509CertSelector = null;
        }
        for (TrustAnchor trustAnchor : validatorParams.trustAnchors()) {
            X509Certificate trustedCert = trustAnchor.getTrustedCert();
            if (trustedCert != null) {
                if (adaptableX509CertSelector != null && !adaptableX509CertSelector.match(trustedCert)) {
                    if (debug != null) {
                        debug.println("NO - don't try this trustedCert");
                    }
                } else if (debug != null) {
                    debug.println("YES - try this trustedCert");
                    debug.println("anchor.getTrustedCert().getSubjectX500Principal() = " + ((Object) trustedCert.getSubjectX500Principal()));
                }
            } else if (debug != null) {
                debug.println("PKIXCertPathValidator.engineValidate(): anchor.getTrustedCert() == null");
            }
            try {
                return validate(trustAnchor, validatorParams);
            } catch (CertPathValidatorException e3) {
                e = e3;
            }
        }
        if (e != null) {
            throw e;
        }
        throw new CertPathValidatorException("Path does not chain with any of the trust anchors", null, null, -1, PKIXReason.NO_TRUST_ANCHOR);
    }

    private static PKIXCertPathValidatorResult validate(TrustAnchor trustAnchor, PKIX.ValidatorParams validatorParams) throws CertPathValidatorException {
        int size = validatorParams.certificates().size();
        ArrayList arrayList = new ArrayList();
        arrayList.add(new AlgorithmChecker(trustAnchor));
        arrayList.add(new KeyChecker(size, validatorParams.targetCertConstraints()));
        arrayList.add(new ConstraintsChecker(size));
        PolicyChecker policyChecker = new PolicyChecker(validatorParams.initialPolicies(), size, validatorParams.explicitPolicyRequired(), validatorParams.policyMappingInhibited(), validatorParams.anyPolicyInhibited(), validatorParams.policyQualifiersRejected(), new PolicyNodeImpl(null, "2.5.29.32.0", null, false, Collections.singleton("2.5.29.32.0"), false));
        arrayList.add(policyChecker);
        boolean z = false;
        BasicChecker basicChecker = new BasicChecker(trustAnchor, validatorParams.date(), validatorParams.sigProvider(), false);
        arrayList.add(basicChecker);
        List<PKIXCertPathChecker> listCertPathCheckers = validatorParams.certPathCheckers();
        for (PKIXCertPathChecker pKIXCertPathChecker : listCertPathCheckers) {
            if (pKIXCertPathChecker instanceof PKIXRevocationChecker) {
                if (z) {
                    throw new CertPathValidatorException("Only one PKIXRevocationChecker can be specified");
                }
                z = true;
                if (pKIXCertPathChecker instanceof RevocationChecker) {
                    ((RevocationChecker) pKIXCertPathChecker).init(trustAnchor, validatorParams);
                }
            }
        }
        if (validatorParams.revocationEnabled() && !z) {
            arrayList.add(new RevocationChecker(trustAnchor, validatorParams));
        }
        arrayList.addAll(listCertPathCheckers);
        PKIXMasterCertPathValidator.validate(validatorParams.certPath(), validatorParams.certificates(), arrayList);
        return new PKIXCertPathValidatorResult(trustAnchor, policyChecker.getPolicyTree(), basicChecker.getPublicKey());
    }
}
