package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.asn1.x500.X500Name;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x509.Extension;
import com.android.org.bouncycastle.jcajce.PKIXExtendedBuilderParameters;
import com.android.org.bouncycastle.jcajce.PKIXExtendedParameters;
import com.android.org.bouncycastle.jcajce.util.BCJcaJceHelper;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
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

public class PKIXCertPathValidatorSpi extends CertPathValidatorSpi {
    private final JcaJceHelper helper = new BCJcaJceHelper();

    private static class NoPreloadHolder {
        private static final CertBlacklist blacklist = new CertBlacklist();

        private NoPreloadHolder() {
        }
    }

    @Override
    public CertPathValidatorResult engineValidate(CertPath certPath, CertPathParameters certPathParameters) throws CertPathValidatorException, InvalidAlgorithmParameterException {
        PKIXExtendedParameters baseParameters;
        int iPrepareNextCertI1;
        int iPrepareNextCertJ;
        int iPrepareNextCertI2;
        X500Name ca;
        PublicKey cAPublicKey;
        HashSet hashSet;
        HashSet hashSet2;
        if (certPathParameters instanceof PKIXParameters) {
            PKIXExtendedParameters.Builder builder = new PKIXExtendedParameters.Builder((PKIXParameters) certPathParameters);
            if (certPathParameters instanceof ExtendedPKIXParameters) {
                builder.setUseDeltasEnabled(certPathParameters.isUseDeltasEnabled());
                builder.setValidityModel(certPathParameters.getValidityModel());
            }
            baseParameters = builder.build();
        } else if (certPathParameters instanceof PKIXExtendedBuilderParameters) {
            baseParameters = certPathParameters.getBaseParameters();
        } else {
            if (!(certPathParameters instanceof PKIXExtendedParameters)) {
                throw new InvalidAlgorithmParameterException("Parameters must be a " + PKIXParameters.class.getName() + " instance.");
            }
            baseParameters = certPathParameters;
        }
        if (baseParameters.getTrustAnchors() == null) {
            throw new InvalidAlgorithmParameterException("trustAnchors is null, this is not allowed for certification path validation.");
        }
        List<? extends Certificate> certificates = certPath.getCertificates();
        int size = certificates.size();
        if (certificates.isEmpty()) {
            throw new CertPathValidatorException("Certification path is empty.", null, certPath, -1);
        }
        X509Certificate x509Certificate = (X509Certificate) certificates.get(0);
        if (x509Certificate != null) {
            BigInteger serialNumber = x509Certificate.getSerialNumber();
            if (NoPreloadHolder.blacklist.isSerialNumberBlackListed(serialNumber)) {
                String str = "Certificate revocation of serial 0x" + serialNumber.toString(16);
                System.out.println(str);
                AnnotatedException annotatedException = new AnnotatedException(str);
                throw new CertPathValidatorException(annotatedException.getMessage(), annotatedException, certPath, 0);
            }
        }
        Set initialPolicies = baseParameters.getInitialPolicies();
        try {
            TrustAnchor trustAnchorFindTrustAnchor = CertPathValidatorUtilities.findTrustAnchor((X509Certificate) certificates.get(certificates.size() - 1), baseParameters.getTrustAnchors(), baseParameters.getSigProvider());
            if (trustAnchorFindTrustAnchor == null) {
                throw new CertPathValidatorException("Trust anchor for certification path not found.", null, certPath, -1);
            }
            PKIXExtendedParameters pKIXExtendedParametersBuild = new PKIXExtendedParameters.Builder(baseParameters).setTrustAnchor(trustAnchorFindTrustAnchor).build();
            ArrayList[] arrayListArr = new ArrayList[size + 1];
            for (int i = 0; i < arrayListArr.length; i++) {
                arrayListArr[i] = new ArrayList();
            }
            HashSet hashSet3 = new HashSet();
            hashSet3.add(RFC3280CertPathUtilities.ANY_POLICY);
            PKIXPolicyNode pKIXPolicyNode = new PKIXPolicyNode(new ArrayList(), 0, hashSet3, null, new HashSet(), RFC3280CertPathUtilities.ANY_POLICY, false);
            arrayListArr[0].add(pKIXPolicyNode);
            PKIXNameConstraintValidator pKIXNameConstraintValidator = new PKIXNameConstraintValidator();
            HashSet hashSet4 = new HashSet();
            if (pKIXExtendedParametersBuild.isExplicitPolicyRequired()) {
                iPrepareNextCertI1 = 0;
            } else {
                iPrepareNextCertI1 = size + 1;
            }
            if (pKIXExtendedParametersBuild.isAnyPolicyInhibited()) {
                iPrepareNextCertJ = 0;
            } else {
                iPrepareNextCertJ = size + 1;
            }
            if (pKIXExtendedParametersBuild.isPolicyMappingInhibited()) {
                iPrepareNextCertI2 = 0;
            } else {
                iPrepareNextCertI2 = size + 1;
            }
            X509Certificate trustedCert = trustAnchorFindTrustAnchor.getTrustedCert();
            try {
                if (trustedCert != null) {
                    ca = PrincipalUtils.getSubjectPrincipal(trustedCert);
                    cAPublicKey = trustedCert.getPublicKey();
                } else {
                    ca = PrincipalUtils.getCA(trustAnchorFindTrustAnchor);
                    cAPublicKey = trustAnchorFindTrustAnchor.getCAPublicKey();
                }
                try {
                    AlgorithmIdentifier algorithmIdentifier = CertPathValidatorUtilities.getAlgorithmIdentifier(cAPublicKey);
                    algorithmIdentifier.getAlgorithm();
                    algorithmIdentifier.getParameters();
                    int iPrepareNextCertM = size;
                    if (pKIXExtendedParametersBuild.getTargetConstraints() != null && !pKIXExtendedParametersBuild.getTargetConstraints().match((Certificate) certificates.get(0))) {
                        throw new ExtCertPathValidatorException("Target certificate in certification path does not match targetConstraints.", null, certPath, 0);
                    }
                    List certPathCheckers = pKIXExtendedParametersBuild.getCertPathCheckers();
                    Iterator it = certPathCheckers.iterator();
                    while (it.hasNext()) {
                        ((PKIXCertPathChecker) it.next()).init(false);
                    }
                    X509Certificate x509Certificate2 = null;
                    int size2 = certificates.size() - 1;
                    while (size2 >= 0) {
                        if (NoPreloadHolder.blacklist.isPublicKeyBlackListed(cAPublicKey)) {
                            String str2 = "Certificate revocation of public key " + cAPublicKey;
                            System.out.println(str2);
                            AnnotatedException annotatedException2 = new AnnotatedException(str2);
                            throw new CertPathValidatorException(annotatedException2.getMessage(), annotatedException2, certPath, size2);
                        }
                        int i2 = size - size2;
                        x509Certificate2 = (X509Certificate) certificates.get(size2);
                        RFC3280CertPathUtilities.processCertA(certPath, pKIXExtendedParametersBuild, size2, cAPublicKey, size2 == certificates.size() + (-1), ca, trustedCert, this.helper);
                        RFC3280CertPathUtilities.processCertBC(certPath, size2, pKIXNameConstraintValidator);
                        pKIXPolicyNode = RFC3280CertPathUtilities.processCertE(certPath, size2, RFC3280CertPathUtilities.processCertD(certPath, size2, hashSet4, pKIXPolicyNode, arrayListArr, iPrepareNextCertJ));
                        RFC3280CertPathUtilities.processCertF(certPath, size2, pKIXPolicyNode, iPrepareNextCertI1);
                        if (i2 != size) {
                            if (x509Certificate2 != null && x509Certificate2.getVersion() == 1) {
                                throw new CertPathValidatorException("Version 1 certificates can't be used as CA ones.", null, certPath, size2);
                            }
                            RFC3280CertPathUtilities.prepareNextCertA(certPath, size2);
                            pKIXPolicyNode = RFC3280CertPathUtilities.prepareCertB(certPath, size2, arrayListArr, pKIXPolicyNode, iPrepareNextCertI2);
                            RFC3280CertPathUtilities.prepareNextCertG(certPath, size2, pKIXNameConstraintValidator);
                            int iPrepareNextCertH1 = RFC3280CertPathUtilities.prepareNextCertH1(certPath, size2, iPrepareNextCertI1);
                            int iPrepareNextCertH2 = RFC3280CertPathUtilities.prepareNextCertH2(certPath, size2, iPrepareNextCertI2);
                            int iPrepareNextCertH3 = RFC3280CertPathUtilities.prepareNextCertH3(certPath, size2, iPrepareNextCertJ);
                            iPrepareNextCertI1 = RFC3280CertPathUtilities.prepareNextCertI1(certPath, size2, iPrepareNextCertH1);
                            iPrepareNextCertI2 = RFC3280CertPathUtilities.prepareNextCertI2(certPath, size2, iPrepareNextCertH2);
                            iPrepareNextCertJ = RFC3280CertPathUtilities.prepareNextCertJ(certPath, size2, iPrepareNextCertH3);
                            RFC3280CertPathUtilities.prepareNextCertK(certPath, size2);
                            iPrepareNextCertM = RFC3280CertPathUtilities.prepareNextCertM(certPath, size2, RFC3280CertPathUtilities.prepareNextCertL(certPath, size2, iPrepareNextCertM));
                            RFC3280CertPathUtilities.prepareNextCertN(certPath, size2);
                            Set<String> criticalExtensionOIDs = x509Certificate2.getCriticalExtensionOIDs();
                            if (criticalExtensionOIDs != null) {
                                HashSet hashSet5 = new HashSet(criticalExtensionOIDs);
                                hashSet5.remove(RFC3280CertPathUtilities.KEY_USAGE);
                                hashSet5.remove(RFC3280CertPathUtilities.CERTIFICATE_POLICIES);
                                hashSet5.remove(RFC3280CertPathUtilities.POLICY_MAPPINGS);
                                hashSet5.remove(RFC3280CertPathUtilities.INHIBIT_ANY_POLICY);
                                hashSet5.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
                                hashSet5.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);
                                hashSet5.remove(RFC3280CertPathUtilities.POLICY_CONSTRAINTS);
                                hashSet5.remove(RFC3280CertPathUtilities.BASIC_CONSTRAINTS);
                                hashSet5.remove(RFC3280CertPathUtilities.SUBJECT_ALTERNATIVE_NAME);
                                hashSet5.remove(RFC3280CertPathUtilities.NAME_CONSTRAINTS);
                                hashSet2 = hashSet5;
                            } else {
                                hashSet2 = new HashSet();
                            }
                            RFC3280CertPathUtilities.prepareNextCertO(certPath, size2, hashSet2, certPathCheckers);
                            trustedCert = x509Certificate2;
                            ca = PrincipalUtils.getSubjectPrincipal(x509Certificate2);
                            try {
                                cAPublicKey = CertPathValidatorUtilities.getNextWorkingKey(certPath.getCertificates(), size2, this.helper);
                                AlgorithmIdentifier algorithmIdentifier2 = CertPathValidatorUtilities.getAlgorithmIdentifier(cAPublicKey);
                                algorithmIdentifier2.getAlgorithm();
                                algorithmIdentifier2.getParameters();
                            } catch (CertPathValidatorException e) {
                                throw new CertPathValidatorException("Next working key could not be retrieved.", e, certPath, size2);
                            }
                        }
                        size2--;
                    }
                    int iWrapupCertB = RFC3280CertPathUtilities.wrapupCertB(certPath, size2 + 1, RFC3280CertPathUtilities.wrapupCertA(iPrepareNextCertI1, x509Certificate2));
                    Set<String> criticalExtensionOIDs2 = x509Certificate2.getCriticalExtensionOIDs();
                    if (criticalExtensionOIDs2 != null) {
                        HashSet hashSet6 = new HashSet(criticalExtensionOIDs2);
                        hashSet6.remove(RFC3280CertPathUtilities.KEY_USAGE);
                        hashSet6.remove(RFC3280CertPathUtilities.CERTIFICATE_POLICIES);
                        hashSet6.remove(RFC3280CertPathUtilities.POLICY_MAPPINGS);
                        hashSet6.remove(RFC3280CertPathUtilities.INHIBIT_ANY_POLICY);
                        hashSet6.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
                        hashSet6.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);
                        hashSet6.remove(RFC3280CertPathUtilities.POLICY_CONSTRAINTS);
                        hashSet6.remove(RFC3280CertPathUtilities.BASIC_CONSTRAINTS);
                        hashSet6.remove(RFC3280CertPathUtilities.SUBJECT_ALTERNATIVE_NAME);
                        hashSet6.remove(RFC3280CertPathUtilities.NAME_CONSTRAINTS);
                        hashSet6.remove(RFC3280CertPathUtilities.CRL_DISTRIBUTION_POINTS);
                        hashSet6.remove(Extension.extendedKeyUsage.getId());
                        hashSet = hashSet6;
                    } else {
                        hashSet = new HashSet();
                    }
                    RFC3280CertPathUtilities.wrapupCertF(certPath, size2 + 1, certPathCheckers, hashSet);
                    PKIXPolicyNode pKIXPolicyNodeWrapupCertG = RFC3280CertPathUtilities.wrapupCertG(certPath, pKIXExtendedParametersBuild, initialPolicies, size2 + 1, arrayListArr, pKIXPolicyNode, hashSet4);
                    if (iWrapupCertB > 0 || pKIXPolicyNodeWrapupCertG != null) {
                        return new PKIXCertPathValidatorResult(trustAnchorFindTrustAnchor, pKIXPolicyNodeWrapupCertG, x509Certificate2.getPublicKey());
                    }
                    throw new CertPathValidatorException("Path processing failed on policy.", null, certPath, size2);
                } catch (CertPathValidatorException e2) {
                    throw new ExtCertPathValidatorException("Algorithm identifier of public key of trust anchor could not be read.", e2, certPath, -1);
                }
            } catch (IllegalArgumentException e3) {
                throw new ExtCertPathValidatorException("Subject of trust anchor could not be (re)encoded.", e3, certPath, -1);
            }
        } catch (AnnotatedException e4) {
            throw new CertPathValidatorException(e4.getMessage(), e4, certPath, certificates.size() - 1);
        }
    }
}
