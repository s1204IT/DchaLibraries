package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.jce.exception.ExtCertPathValidatorException;
import com.android.org.bouncycastle.x509.ExtendedPKIXParameters;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertPathValidatorSpi;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;

public class PKIXCertPathValidatorSpi extends CertPathValidatorSpi {

    private static class NoPreloadHolder {
        private static final CertBlacklist blacklist = new CertBlacklist();

        private NoPreloadHolder() {
        }
    }

    @Override
    public CertPathValidatorResult engineValidate(CertPath certPath, CertPathParameters params) throws CertPathValidatorException, InvalidAlgorithmParameterException {
        ExtendedPKIXParameters paramsPKIX;
        int explicitPolicy;
        int inhibitAnyPolicy;
        int policyMapping;
        X500Principal workingIssuerName;
        PublicKey workingPublicKey;
        Set criticalExtensions;
        Set criticalExtensions2;
        if (!(params instanceof PKIXParameters)) {
            throw new InvalidAlgorithmParameterException("Parameters must be a " + PKIXParameters.class.getName() + " instance.");
        }
        if (params instanceof ExtendedPKIXParameters) {
            paramsPKIX = (ExtendedPKIXParameters) params;
        } else {
            paramsPKIX = ExtendedPKIXParameters.getInstance((PKIXParameters) params);
        }
        if (paramsPKIX.getTrustAnchors() == null) {
            throw new InvalidAlgorithmParameterException("trustAnchors is null, this is not allowed for certification path validation.");
        }
        List<? extends Certificate> certificates = certPath.getCertificates();
        int n = certificates.size();
        if (certificates.isEmpty()) {
            throw new CertPathValidatorException("Certification path is empty.", null, certPath, 0);
        }
        X509Certificate cert = (X509Certificate) certificates.get(0);
        if (cert != null) {
            BigInteger serial = cert.getSerialNumber();
            if (NoPreloadHolder.blacklist.isSerialNumberBlackListed(serial)) {
                String message = "Certificate revocation of serial 0x" + serial.toString(16);
                System.out.println(message);
                AnnotatedException e = new AnnotatedException(message);
                throw new CertPathValidatorException(e.getMessage(), e, certPath, 0);
            }
        }
        Set<String> initialPolicies = paramsPKIX.getInitialPolicies();
        try {
            TrustAnchor trust = CertPathValidatorUtilities.findTrustAnchor((X509Certificate) certificates.get(certificates.size() - 1), paramsPKIX.getTrustAnchors(), paramsPKIX.getSigProvider());
            if (trust == null) {
                throw new CertPathValidatorException("Trust anchor for certification path not found.", null, certPath, -1);
            }
            List[] policyNodes = new ArrayList[n + 1];
            for (int j = 0; j < policyNodes.length; j++) {
                policyNodes[j] = new ArrayList();
            }
            Set policySet = new HashSet();
            policySet.add(RFC3280CertPathUtilities.ANY_POLICY);
            PKIXPolicyNode validPolicyTree = new PKIXPolicyNode(new ArrayList(), 0, policySet, null, new HashSet(), RFC3280CertPathUtilities.ANY_POLICY, false);
            policyNodes[0].add(validPolicyTree);
            PKIXNameConstraintValidator nameConstraintValidator = new PKIXNameConstraintValidator();
            Set acceptablePolicies = new HashSet();
            if (paramsPKIX.isExplicitPolicyRequired()) {
                explicitPolicy = 0;
            } else {
                explicitPolicy = n + 1;
            }
            if (paramsPKIX.isAnyPolicyInhibited()) {
                inhibitAnyPolicy = 0;
            } else {
                inhibitAnyPolicy = n + 1;
            }
            if (paramsPKIX.isPolicyMappingInhibited()) {
                policyMapping = 0;
            } else {
                policyMapping = n + 1;
            }
            X509Certificate sign = trust.getTrustedCert();
            try {
                if (sign != null) {
                    workingIssuerName = CertPathValidatorUtilities.getSubjectPrincipal(sign);
                    workingPublicKey = sign.getPublicKey();
                } else {
                    workingIssuerName = new X500Principal(trust.getCAName());
                    workingPublicKey = trust.getCAPublicKey();
                }
                try {
                    AlgorithmIdentifier workingAlgId = CertPathValidatorUtilities.getAlgorithmIdentifier(workingPublicKey);
                    workingAlgId.getObjectId();
                    workingAlgId.getParameters();
                    int maxPathLength = n;
                    if (paramsPKIX.getTargetConstraints() != null && !paramsPKIX.getTargetConstraints().match((X509Certificate) certificates.get(0))) {
                        throw new ExtCertPathValidatorException("Target certificate in certification path does not match targetConstraints.", null, certPath, 0);
                    }
                    List<PKIXCertPathChecker> certPathCheckers = paramsPKIX.getCertPathCheckers();
                    Iterator<PKIXCertPathChecker> it = certPathCheckers.iterator();
                    while (it.hasNext()) {
                        it.next().init(false);
                    }
                    X509Certificate cert2 = null;
                    int index = certificates.size() - 1;
                    while (index >= 0) {
                        if (NoPreloadHolder.blacklist.isPublicKeyBlackListed(workingPublicKey)) {
                            String message2 = "Certificate revocation of public key " + workingPublicKey;
                            System.out.println(message2);
                            AnnotatedException e2 = new AnnotatedException(message2);
                            throw new CertPathValidatorException(e2.getMessage(), e2, certPath, index);
                        }
                        int i = n - index;
                        cert2 = (X509Certificate) certificates.get(index);
                        boolean verificationAlreadyPerformed = index == certificates.size() + (-1);
                        RFC3280CertPathUtilities.processCertA(certPath, paramsPKIX, index, workingPublicKey, verificationAlreadyPerformed, workingIssuerName, sign);
                        RFC3280CertPathUtilities.processCertBC(certPath, index, nameConstraintValidator);
                        validPolicyTree = RFC3280CertPathUtilities.processCertE(certPath, index, RFC3280CertPathUtilities.processCertD(certPath, index, acceptablePolicies, validPolicyTree, policyNodes, inhibitAnyPolicy));
                        RFC3280CertPathUtilities.processCertF(certPath, index, validPolicyTree, explicitPolicy);
                        if (i != n) {
                            if (cert2 != null && cert2.getVersion() == 1) {
                                throw new CertPathValidatorException("Version 1 certificates can't be used as CA ones.", null, certPath, index);
                            }
                            RFC3280CertPathUtilities.prepareNextCertA(certPath, index);
                            validPolicyTree = RFC3280CertPathUtilities.prepareCertB(certPath, index, policyNodes, validPolicyTree, policyMapping);
                            RFC3280CertPathUtilities.prepareNextCertG(certPath, index, nameConstraintValidator);
                            int explicitPolicy2 = RFC3280CertPathUtilities.prepareNextCertH1(certPath, index, explicitPolicy);
                            int policyMapping2 = RFC3280CertPathUtilities.prepareNextCertH2(certPath, index, policyMapping);
                            int inhibitAnyPolicy2 = RFC3280CertPathUtilities.prepareNextCertH3(certPath, index, inhibitAnyPolicy);
                            explicitPolicy = RFC3280CertPathUtilities.prepareNextCertI1(certPath, index, explicitPolicy2);
                            policyMapping = RFC3280CertPathUtilities.prepareNextCertI2(certPath, index, policyMapping2);
                            inhibitAnyPolicy = RFC3280CertPathUtilities.prepareNextCertJ(certPath, index, inhibitAnyPolicy2);
                            RFC3280CertPathUtilities.prepareNextCertK(certPath, index);
                            maxPathLength = RFC3280CertPathUtilities.prepareNextCertM(certPath, index, RFC3280CertPathUtilities.prepareNextCertL(certPath, index, maxPathLength));
                            RFC3280CertPathUtilities.prepareNextCertN(certPath, index);
                            Set<String> criticalExtensionOIDs = cert2.getCriticalExtensionOIDs();
                            if (criticalExtensionOIDs != null) {
                                Set criticalExtensions3 = new HashSet(criticalExtensionOIDs);
                                criticalExtensions3.remove(RFC3280CertPathUtilities.KEY_USAGE);
                                criticalExtensions3.remove(RFC3280CertPathUtilities.CERTIFICATE_POLICIES);
                                criticalExtensions3.remove(RFC3280CertPathUtilities.POLICY_MAPPINGS);
                                criticalExtensions3.remove(RFC3280CertPathUtilities.INHIBIT_ANY_POLICY);
                                criticalExtensions3.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
                                criticalExtensions3.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);
                                criticalExtensions3.remove(RFC3280CertPathUtilities.POLICY_CONSTRAINTS);
                                criticalExtensions3.remove(RFC3280CertPathUtilities.BASIC_CONSTRAINTS);
                                criticalExtensions3.remove(RFC3280CertPathUtilities.SUBJECT_ALTERNATIVE_NAME);
                                criticalExtensions3.remove(RFC3280CertPathUtilities.NAME_CONSTRAINTS);
                                criticalExtensions2 = criticalExtensions3;
                            } else {
                                criticalExtensions2 = new HashSet();
                            }
                            RFC3280CertPathUtilities.prepareNextCertO(certPath, index, criticalExtensions2, certPathCheckers);
                            sign = cert2;
                            workingIssuerName = CertPathValidatorUtilities.getSubjectPrincipal(sign);
                            try {
                                workingPublicKey = CertPathValidatorUtilities.getNextWorkingKey(certPath.getCertificates(), index);
                                AlgorithmIdentifier workingAlgId2 = CertPathValidatorUtilities.getAlgorithmIdentifier(workingPublicKey);
                                workingAlgId2.getObjectId();
                                workingAlgId2.getParameters();
                            } catch (CertPathValidatorException e3) {
                                throw new CertPathValidatorException("Next working key could not be retrieved.", e3, certPath, index);
                            }
                        }
                        index--;
                    }
                    int explicitPolicy3 = RFC3280CertPathUtilities.wrapupCertB(certPath, index + 1, RFC3280CertPathUtilities.wrapupCertA(explicitPolicy, cert2));
                    Set<String> criticalExtensionOIDs2 = cert2.getCriticalExtensionOIDs();
                    if (criticalExtensionOIDs2 != null) {
                        Set criticalExtensions4 = new HashSet(criticalExtensionOIDs2);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.KEY_USAGE);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.CERTIFICATE_POLICIES);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.POLICY_MAPPINGS);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.INHIBIT_ANY_POLICY);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.POLICY_CONSTRAINTS);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.BASIC_CONSTRAINTS);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.SUBJECT_ALTERNATIVE_NAME);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.NAME_CONSTRAINTS);
                        criticalExtensions4.remove(RFC3280CertPathUtilities.CRL_DISTRIBUTION_POINTS);
                        criticalExtensions = criticalExtensions4;
                    } else {
                        criticalExtensions = new HashSet();
                    }
                    RFC3280CertPathUtilities.wrapupCertF(certPath, index + 1, certPathCheckers, criticalExtensions);
                    PKIXPolicyNode intersection = RFC3280CertPathUtilities.wrapupCertG(certPath, paramsPKIX, initialPolicies, index + 1, policyNodes, validPolicyTree, acceptablePolicies);
                    if (explicitPolicy3 > 0 || intersection != null) {
                        return new PKIXCertPathValidatorResult(trust, intersection, cert2.getPublicKey());
                    }
                    throw new CertPathValidatorException("Path processing failed on policy.", null, certPath, index);
                } catch (CertPathValidatorException e4) {
                    throw new ExtCertPathValidatorException("Algorithm identifier of public key of trust anchor could not be read.", e4, certPath, -1);
                }
            } catch (IllegalArgumentException ex) {
                throw new ExtCertPathValidatorException("Subject of trust anchor could not be (re)encoded.", ex, certPath, -1);
            }
        } catch (AnnotatedException e5) {
            throw new CertPathValidatorException(e5.getMessage(), e5, certPath, certificates.size() - 1);
        }
    }
}
