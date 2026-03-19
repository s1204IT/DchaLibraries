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
        X500Name ca;
        PublicKey cAPublicKey;
        int i;
        HashSet hashSet;
        List list;
        ArrayList[] arrayListArr;
        PKIXCertPathValidatorSpi pKIXCertPathValidatorSpi;
        int iPrepareNextCertM;
        PublicKey nextWorkingKey;
        HashSet hashSet2;
        PKIXCertPathValidatorSpi pKIXCertPathValidatorSpi2 = this;
        if (certPathParameters instanceof PKIXParameters) {
            PKIXExtendedParameters.Builder builder = new PKIXExtendedParameters.Builder((PKIXParameters) certPathParameters);
            if (certPathParameters instanceof ExtendedPKIXParameters) {
                ExtendedPKIXParameters extendedPKIXParameters = (ExtendedPKIXParameters) certPathParameters;
                builder.setUseDeltasEnabled(extendedPKIXParameters.isUseDeltasEnabled());
                builder.setValidityModel(extendedPKIXParameters.getValidityModel());
            }
            baseParameters = builder.build();
        } else if (certPathParameters instanceof PKIXExtendedBuilderParameters) {
            baseParameters = ((PKIXExtendedBuilderParameters) certPathParameters).getBaseParameters();
        } else if (certPathParameters instanceof PKIXExtendedParameters) {
            baseParameters = (PKIXExtendedParameters) certPathParameters;
        } else {
            throw new InvalidAlgorithmParameterException("Parameters must be a " + PKIXParameters.class.getName() + " instance.");
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
            int i2 = size + 1;
            ArrayList[] arrayListArr2 = new ArrayList[i2];
            for (int i3 = 0; i3 < arrayListArr2.length; i3++) {
                arrayListArr2[i3] = new ArrayList();
            }
            HashSet hashSet3 = new HashSet();
            hashSet3.add(RFC3280CertPathUtilities.ANY_POLICY);
            PKIXPolicyNode pKIXPolicyNode = new PKIXPolicyNode(new ArrayList(), 0, hashSet3, null, new HashSet(), RFC3280CertPathUtilities.ANY_POLICY, false);
            arrayListArr2[0].add(pKIXPolicyNode);
            PKIXNameConstraintValidator pKIXNameConstraintValidator = new PKIXNameConstraintValidator();
            HashSet hashSet4 = new HashSet();
            int i4 = pKIXExtendedParametersBuild.isExplicitPolicyRequired() ? 0 : i2;
            int i5 = pKIXExtendedParametersBuild.isAnyPolicyInhibited() ? 0 : i2;
            if (pKIXExtendedParametersBuild.isPolicyMappingInhibited()) {
                i2 = 0;
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
                PublicKey publicKey = cAPublicKey;
                try {
                    AlgorithmIdentifier algorithmIdentifier = CertPathValidatorUtilities.getAlgorithmIdentifier(publicKey);
                    algorithmIdentifier.getAlgorithm();
                    algorithmIdentifier.getParameters();
                    if (pKIXExtendedParametersBuild.getTargetConstraints() != null) {
                        i = i2;
                        if (!pKIXExtendedParametersBuild.getTargetConstraints().match((Certificate) certificates.get(0))) {
                            throw new ExtCertPathValidatorException("Target certificate in certification path does not match targetConstraints.", null, certPath, 0);
                        }
                    } else {
                        i = i2;
                    }
                    List certPathCheckers = pKIXExtendedParametersBuild.getCertPathCheckers();
                    Iterator it = certPathCheckers.iterator();
                    while (it.hasNext()) {
                        ((PKIXCertPathChecker) it.next()).init(false);
                        it = it;
                        publicKey = publicKey;
                    }
                    PublicKey publicKey2 = publicKey;
                    X509Certificate x509Certificate2 = trustedCert;
                    int iPrepareNextCertJ = i5;
                    int i6 = i;
                    int size2 = certificates.size() - 1;
                    PKIXPolicyNode pKIXPolicyNode2 = pKIXPolicyNode;
                    int i7 = size;
                    PublicKey publicKey3 = publicKey2;
                    X509Certificate x509Certificate3 = null;
                    while (size2 >= 0) {
                        if (NoPreloadHolder.blacklist.isPublicKeyBlackListed(publicKey3)) {
                            String str2 = "Certificate revocation of public key " + publicKey3;
                            System.out.println(str2);
                            AnnotatedException annotatedException2 = new AnnotatedException(str2);
                            throw new CertPathValidatorException(annotatedException2.getMessage(), annotatedException2, certPath, size2);
                        }
                        int i8 = size - size2;
                        List list2 = certPathCheckers;
                        X509Certificate x509Certificate4 = (X509Certificate) certificates.get(size2);
                        boolean z = size2 == certificates.size() + (-1);
                        JcaJceHelper jcaJceHelper = pKIXCertPathValidatorSpi2.helper;
                        List<? extends Certificate> list3 = certificates;
                        int i9 = i4;
                        HashSet hashSet5 = hashSet4;
                        PublicKey publicKey4 = publicKey3;
                        int i10 = i7;
                        PKIXNameConstraintValidator pKIXNameConstraintValidator2 = pKIXNameConstraintValidator;
                        ArrayList[] arrayListArr3 = arrayListArr2;
                        PKIXExtendedParameters pKIXExtendedParameters = pKIXExtendedParametersBuild;
                        TrustAnchor trustAnchor = trustAnchorFindTrustAnchor;
                        RFC3280CertPathUtilities.processCertA(certPath, pKIXExtendedParametersBuild, size2, publicKey3, z, ca, x509Certificate2, jcaJceHelper);
                        RFC3280CertPathUtilities.processCertBC(certPath, size2, pKIXNameConstraintValidator2);
                        PKIXPolicyNode pKIXPolicyNodeProcessCertE = RFC3280CertPathUtilities.processCertE(certPath, size2, RFC3280CertPathUtilities.processCertD(certPath, size2, hashSet5, pKIXPolicyNode2, arrayListArr3, iPrepareNextCertJ));
                        RFC3280CertPathUtilities.processCertF(certPath, size2, pKIXPolicyNodeProcessCertE, i9);
                        if (i8 == size) {
                            i4 = i9;
                            list = list2;
                            arrayListArr = arrayListArr3;
                            pKIXCertPathValidatorSpi = this;
                            pKIXPolicyNode2 = pKIXPolicyNodeProcessCertE;
                            iPrepareNextCertM = i10;
                            nextWorkingKey = publicKey4;
                        } else {
                            if (x509Certificate4 != null && x509Certificate4.getVersion() == 1) {
                                throw new CertPathValidatorException("Version 1 certificates can't be used as CA ones.", null, certPath, size2);
                            }
                            RFC3280CertPathUtilities.prepareNextCertA(certPath, size2);
                            arrayListArr = arrayListArr3;
                            PKIXPolicyNode pKIXPolicyNodePrepareCertB = RFC3280CertPathUtilities.prepareCertB(certPath, size2, arrayListArr, pKIXPolicyNodeProcessCertE, i6);
                            RFC3280CertPathUtilities.prepareNextCertG(certPath, size2, pKIXNameConstraintValidator2);
                            int iPrepareNextCertH1 = RFC3280CertPathUtilities.prepareNextCertH1(certPath, size2, i9);
                            int iPrepareNextCertH2 = RFC3280CertPathUtilities.prepareNextCertH2(certPath, size2, i6);
                            int iPrepareNextCertH3 = RFC3280CertPathUtilities.prepareNextCertH3(certPath, size2, iPrepareNextCertJ);
                            int iPrepareNextCertI1 = RFC3280CertPathUtilities.prepareNextCertI1(certPath, size2, iPrepareNextCertH1);
                            int iPrepareNextCertI2 = RFC3280CertPathUtilities.prepareNextCertI2(certPath, size2, iPrepareNextCertH2);
                            iPrepareNextCertJ = RFC3280CertPathUtilities.prepareNextCertJ(certPath, size2, iPrepareNextCertH3);
                            RFC3280CertPathUtilities.prepareNextCertK(certPath, size2);
                            iPrepareNextCertM = RFC3280CertPathUtilities.prepareNextCertM(certPath, size2, RFC3280CertPathUtilities.prepareNextCertL(certPath, size2, i10));
                            RFC3280CertPathUtilities.prepareNextCertN(certPath, size2);
                            Set<String> criticalExtensionOIDs = x509Certificate4.getCriticalExtensionOIDs();
                            if (criticalExtensionOIDs != null) {
                                hashSet2 = new HashSet(criticalExtensionOIDs);
                                hashSet2.remove(RFC3280CertPathUtilities.KEY_USAGE);
                                hashSet2.remove(RFC3280CertPathUtilities.CERTIFICATE_POLICIES);
                                hashSet2.remove(RFC3280CertPathUtilities.POLICY_MAPPINGS);
                                hashSet2.remove(RFC3280CertPathUtilities.INHIBIT_ANY_POLICY);
                                hashSet2.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
                                hashSet2.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);
                                hashSet2.remove(RFC3280CertPathUtilities.POLICY_CONSTRAINTS);
                                hashSet2.remove(RFC3280CertPathUtilities.BASIC_CONSTRAINTS);
                                hashSet2.remove(RFC3280CertPathUtilities.SUBJECT_ALTERNATIVE_NAME);
                                hashSet2.remove(RFC3280CertPathUtilities.NAME_CONSTRAINTS);
                            } else {
                                hashSet2 = new HashSet();
                            }
                            list = list2;
                            RFC3280CertPathUtilities.prepareNextCertO(certPath, size2, hashSet2, list);
                            ca = PrincipalUtils.getSubjectPrincipal(x509Certificate4);
                            try {
                                pKIXCertPathValidatorSpi = this;
                                nextWorkingKey = CertPathValidatorUtilities.getNextWorkingKey(certPath.getCertificates(), size2, pKIXCertPathValidatorSpi.helper);
                                AlgorithmIdentifier algorithmIdentifier2 = CertPathValidatorUtilities.getAlgorithmIdentifier(nextWorkingKey);
                                algorithmIdentifier2.getAlgorithm();
                                algorithmIdentifier2.getParameters();
                                pKIXPolicyNode2 = pKIXPolicyNodePrepareCertB;
                                i6 = iPrepareNextCertI2;
                                x509Certificate2 = x509Certificate4;
                                i4 = iPrepareNextCertI1;
                            } catch (CertPathValidatorException e) {
                                throw new CertPathValidatorException("Next working key could not be retrieved.", e, certPath, size2);
                            }
                        }
                        size2--;
                        pKIXCertPathValidatorSpi2 = pKIXCertPathValidatorSpi;
                        x509Certificate3 = x509Certificate4;
                        certificates = list3;
                        trustAnchorFindTrustAnchor = trustAnchor;
                        arrayListArr2 = arrayListArr;
                        publicKey3 = nextWorkingKey;
                        certPathCheckers = list;
                        pKIXNameConstraintValidator = pKIXNameConstraintValidator2;
                        pKIXExtendedParametersBuild = pKIXExtendedParameters;
                        i7 = iPrepareNextCertM;
                        hashSet4 = hashSet5;
                    }
                    HashSet hashSet6 = hashSet4;
                    ArrayList[] arrayListArr4 = arrayListArr2;
                    PKIXExtendedParameters pKIXExtendedParameters2 = pKIXExtendedParametersBuild;
                    TrustAnchor trustAnchor2 = trustAnchorFindTrustAnchor;
                    List list4 = certPathCheckers;
                    int i11 = size2 + 1;
                    int iWrapupCertB = RFC3280CertPathUtilities.wrapupCertB(certPath, i11, RFC3280CertPathUtilities.wrapupCertA(i4, x509Certificate3));
                    Set<String> criticalExtensionOIDs2 = x509Certificate3.getCriticalExtensionOIDs();
                    if (criticalExtensionOIDs2 != null) {
                        hashSet = new HashSet(criticalExtensionOIDs2);
                        hashSet.remove(RFC3280CertPathUtilities.KEY_USAGE);
                        hashSet.remove(RFC3280CertPathUtilities.CERTIFICATE_POLICIES);
                        hashSet.remove(RFC3280CertPathUtilities.POLICY_MAPPINGS);
                        hashSet.remove(RFC3280CertPathUtilities.INHIBIT_ANY_POLICY);
                        hashSet.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
                        hashSet.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);
                        hashSet.remove(RFC3280CertPathUtilities.POLICY_CONSTRAINTS);
                        hashSet.remove(RFC3280CertPathUtilities.BASIC_CONSTRAINTS);
                        hashSet.remove(RFC3280CertPathUtilities.SUBJECT_ALTERNATIVE_NAME);
                        hashSet.remove(RFC3280CertPathUtilities.NAME_CONSTRAINTS);
                        hashSet.remove(RFC3280CertPathUtilities.CRL_DISTRIBUTION_POINTS);
                        hashSet.remove(Extension.extendedKeyUsage.getId());
                    } else {
                        hashSet = new HashSet();
                    }
                    RFC3280CertPathUtilities.wrapupCertF(certPath, i11, list4, hashSet);
                    X509Certificate x509Certificate5 = x509Certificate3;
                    PKIXPolicyNode pKIXPolicyNodeWrapupCertG = RFC3280CertPathUtilities.wrapupCertG(certPath, pKIXExtendedParameters2, initialPolicies, i11, arrayListArr4, pKIXPolicyNode2, hashSet6);
                    if (iWrapupCertB > 0 || pKIXPolicyNodeWrapupCertG != null) {
                        return new PKIXCertPathValidatorResult(trustAnchor2, pKIXPolicyNodeWrapupCertG, x509Certificate5.getPublicKey());
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
