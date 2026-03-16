package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.jce.exception.ExtCertPathBuilderException;
import com.android.org.bouncycastle.util.Selector;
import com.android.org.bouncycastle.x509.ExtendedPKIXBuilderParameters;
import com.android.org.bouncycastle.x509.X509CertStoreSelector;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathBuilderSpi;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class PKIXCertPathBuilderSpi extends CertPathBuilderSpi {
    private Exception certPathException;

    @Override
    public CertPathBuilderResult engineBuild(CertPathParameters params) throws CertPathBuilderException, InvalidAlgorithmParameterException {
        ExtendedPKIXBuilderParameters pkixParams;
        if (!(params instanceof PKIXBuilderParameters) && !(params instanceof ExtendedPKIXBuilderParameters)) {
            throw new InvalidAlgorithmParameterException("Parameters must be an instance of " + PKIXBuilderParameters.class.getName() + " or " + ExtendedPKIXBuilderParameters.class.getName() + ".");
        }
        if (params instanceof ExtendedPKIXBuilderParameters) {
            pkixParams = (ExtendedPKIXBuilderParameters) params;
        } else {
            pkixParams = (ExtendedPKIXBuilderParameters) ExtendedPKIXBuilderParameters.getInstance((PKIXBuilderParameters) params);
        }
        List certPathList = new ArrayList();
        Selector certSelect = pkixParams.getTargetConstraints();
        if (!(certSelect instanceof X509CertStoreSelector)) {
            throw new CertPathBuilderException("TargetConstraints must be an instance of " + X509CertStoreSelector.class.getName() + " for " + getClass().getName() + " class.");
        }
        try {
            Collection targets = CertPathValidatorUtilities.findCertificates((X509CertStoreSelector) certSelect, pkixParams.getStores());
            targets.addAll(CertPathValidatorUtilities.findCertificates((X509CertStoreSelector) certSelect, pkixParams.getCertStores()));
            if (targets.isEmpty()) {
                throw new CertPathBuilderException("No certificate found matching targetContraints.");
            }
            CertPathBuilderResult result = null;
            Iterator targetIter = targets.iterator();
            while (targetIter.hasNext() && result == null) {
                X509Certificate cert = (X509Certificate) targetIter.next();
                result = build(cert, pkixParams, certPathList);
            }
            if (result == null && this.certPathException != null) {
                if (this.certPathException instanceof AnnotatedException) {
                    throw new CertPathBuilderException(this.certPathException.getMessage(), this.certPathException.getCause());
                }
                throw new CertPathBuilderException("Possible certificate chain could not be validated.", this.certPathException);
            }
            if (result == null && this.certPathException == null) {
                throw new CertPathBuilderException("Unable to find certificate chain.");
            }
            return result;
        } catch (AnnotatedException e) {
            throw new ExtCertPathBuilderException("Error finding target certificate.", e);
        }
    }

    protected CertPathBuilderResult build(X509Certificate tbvCert, ExtendedPKIXBuilderParameters pkixParams, List tbvPath) {
        if (tbvPath.contains(tbvCert) || pkixParams.getExcludedCerts().contains(tbvCert)) {
            return null;
        }
        if (pkixParams.getMaxPathLength() != -1 && tbvPath.size() - 1 > pkixParams.getMaxPathLength()) {
            return null;
        }
        tbvPath.add(tbvCert);
        CertPathBuilderResult builderResult = null;
        try {
            CertificateFactory cFact = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
            CertPathValidator validator = CertPathValidator.getInstance("PKIX", BouncyCastleProvider.PROVIDER_NAME);
            try {
            } catch (AnnotatedException e) {
                this.certPathException = e;
            }
            if (CertPathValidatorUtilities.findTrustAnchor(tbvCert, pkixParams.getTrustAnchors(), pkixParams.getSigProvider()) != null) {
                try {
                    CertPath certPath = cFact.generateCertPath((List<? extends Certificate>) tbvPath);
                    try {
                        PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator.validate(certPath, pkixParams);
                        return new PKIXCertPathBuilderResult(certPath, result.getTrustAnchor(), result.getPolicyTree(), result.getPublicKey());
                    } catch (Exception e2) {
                        throw new AnnotatedException("Certification path could not be validated.", e2);
                    }
                } catch (Exception e3) {
                    throw new AnnotatedException("Certification path could not be constructed from certificate list.", e3);
                }
            }
            try {
                CertPathValidatorUtilities.addAdditionalStoresFromAltNames(tbvCert, pkixParams);
                Collection issuers = new HashSet();
                try {
                    issuers.addAll(CertPathValidatorUtilities.findIssuerCerts(tbvCert, pkixParams));
                    if (issuers.isEmpty()) {
                        throw new AnnotatedException("No issuer certificate for certificate in certification path found.");
                    }
                    Iterator it = issuers.iterator();
                    while (it.hasNext() && builderResult == null) {
                        X509Certificate issuer = (X509Certificate) it.next();
                        builderResult = build(issuer, pkixParams, tbvPath);
                    }
                    if (builderResult != null) {
                        tbvPath.remove(tbvCert);
                        return builderResult;
                    }
                    return builderResult;
                } catch (AnnotatedException e4) {
                    throw new AnnotatedException("Cannot find issuer certificate for certificate in certification path.", e4);
                }
            } catch (CertificateParsingException e5) {
                throw new AnnotatedException("No additiontal X.509 stores can be added from certificate locations.", e5);
            }
            this.certPathException = e;
            if (builderResult != null) {
            }
        } catch (Exception e6) {
            throw new RuntimeException("Exception creating support classes.");
        }
    }
}
