package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.asn1.x509.Extension;
import com.android.org.bouncycastle.jcajce.PKIXCertStore;
import com.android.org.bouncycastle.jcajce.PKIXCertStoreSelector;
import com.android.org.bouncycastle.jcajce.PKIXExtendedBuilderParameters;
import com.android.org.bouncycastle.jcajce.PKIXExtendedParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import com.android.org.bouncycastle.jce.exception.ExtCertPathBuilderException;
import com.android.org.bouncycastle.x509.ExtendedPKIXBuilderParameters;
import com.android.org.bouncycastle.x509.ExtendedPKIXParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathBuilderSpi;
import java.security.cert.CertPathParameters;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class PKIXCertPathBuilderSpi extends CertPathBuilderSpi {
    private Exception certPathException;

    @Override
    public CertPathBuilderResult engineBuild(CertPathParameters certPathParameters) throws CertPathBuilderException, InvalidAlgorithmParameterException {
        PKIXExtendedBuilderParameters pKIXExtendedBuilderParametersBuild;
        PKIXExtendedBuilderParameters.Builder builder;
        if (certPathParameters instanceof PKIXBuilderParameters) {
            PKIXExtendedParameters.Builder builder2 = new PKIXExtendedParameters.Builder((PKIXParameters) certPathParameters);
            if (certPathParameters instanceof ExtendedPKIXParameters) {
                ExtendedPKIXBuilderParameters extendedPKIXBuilderParameters = (ExtendedPKIXBuilderParameters) certPathParameters;
                Iterator it = extendedPKIXBuilderParameters.getAdditionalStores().iterator();
                while (it.hasNext()) {
                    builder2.addCertificateStore((PKIXCertStore) it.next());
                }
                builder = new PKIXExtendedBuilderParameters.Builder(builder2.build());
                builder.addExcludedCerts(extendedPKIXBuilderParameters.getExcludedCerts());
                builder.setMaxPathLength(extendedPKIXBuilderParameters.getMaxPathLength());
            } else {
                builder = new PKIXExtendedBuilderParameters.Builder((PKIXBuilderParameters) certPathParameters);
            }
            pKIXExtendedBuilderParametersBuild = builder.build();
        } else {
            if (!(certPathParameters instanceof PKIXExtendedBuilderParameters)) {
                throw new InvalidAlgorithmParameterException("Parameters must be an instance of " + PKIXBuilderParameters.class.getName() + " or " + PKIXExtendedBuilderParameters.class.getName() + ".");
            }
            pKIXExtendedBuilderParametersBuild = certPathParameters;
        }
        List arrayList = new ArrayList();
        PKIXCertStoreSelector targetConstraints = pKIXExtendedBuilderParametersBuild.getBaseParameters().getTargetConstraints();
        try {
            Collection collectionFindCertificates = CertPathValidatorUtilities.findCertificates(targetConstraints, pKIXExtendedBuilderParametersBuild.getBaseParameters().getCertificateStores());
            collectionFindCertificates.addAll(CertPathValidatorUtilities.findCertificates(targetConstraints, pKIXExtendedBuilderParametersBuild.getBaseParameters().getCertStores()));
            if (collectionFindCertificates.isEmpty()) {
                throw new CertPathBuilderException("No certificate found matching targetContraints.");
            }
            CertPathBuilderResult certPathBuilderResultBuild = null;
            Iterator it2 = collectionFindCertificates.iterator();
            while (it2.hasNext() && certPathBuilderResultBuild == null) {
                certPathBuilderResultBuild = build((X509Certificate) it2.next(), pKIXExtendedBuilderParametersBuild, arrayList);
            }
            if (certPathBuilderResultBuild == null && this.certPathException != null) {
                if (this.certPathException instanceof AnnotatedException) {
                    throw new CertPathBuilderException(this.certPathException.getMessage(), this.certPathException.getCause());
                }
                throw new CertPathBuilderException("Possible certificate chain could not be validated.", this.certPathException);
            }
            if (certPathBuilderResultBuild == null && this.certPathException == null) {
                throw new CertPathBuilderException("Unable to find certificate chain.");
            }
            return certPathBuilderResultBuild;
        } catch (AnnotatedException e) {
            throw new ExtCertPathBuilderException("Error finding target certificate.", e);
        }
    }

    protected CertPathBuilderResult build(X509Certificate tbvCert, PKIXExtendedBuilderParameters pkixParams, List tbvPath) {
        if (tbvPath.contains(tbvCert) || pkixParams.getExcludedCerts().contains(tbvCert)) {
            return null;
        }
        if (pkixParams.getMaxPathLength() != -1 && tbvPath.size() - 1 > pkixParams.getMaxPathLength()) {
            return null;
        }
        tbvPath.add(tbvCert);
        CertPathBuilderResult builderResult = null;
        try {
            CertificateFactory cFact = new CertificateFactory();
            PKIXCertPathValidatorSpi validator = new PKIXCertPathValidatorSpi();
            try {
            } catch (AnnotatedException e) {
                this.certPathException = e;
            }
            if (CertPathValidatorUtilities.findTrustAnchor(tbvCert, pkixParams.getBaseParameters().getTrustAnchors(), pkixParams.getBaseParameters().getSigProvider()) != null) {
                try {
                    CertPath certPath = cFact.engineGenerateCertPath(tbvPath);
                    try {
                        PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator.engineValidate(certPath, pkixParams);
                        return new PKIXCertPathBuilderResult(certPath, result.getTrustAnchor(), result.getPolicyTree(), result.getPublicKey());
                    } catch (Exception e2) {
                        throw new AnnotatedException("Certification path could not be validated.", e2);
                    }
                } catch (Exception e3) {
                    throw new AnnotatedException("Certification path could not be constructed from certificate list.", e3);
                }
            }
            List stores = new ArrayList();
            stores.addAll(pkixParams.getBaseParameters().getCertificateStores());
            try {
                stores.addAll(CertPathValidatorUtilities.getAdditionalStoresFromAltNames(tbvCert.getExtensionValue(Extension.issuerAlternativeName.getId()), pkixParams.getBaseParameters().getNamedCertificateStoreMap()));
                Collection issuers = new HashSet();
                try {
                    issuers.addAll(CertPathValidatorUtilities.findIssuerCerts(tbvCert, pkixParams.getBaseParameters().getCertStores(), stores));
                    if (issuers.isEmpty()) {
                        throw new AnnotatedException("No issuer certificate for certificate in certification path found.");
                    }
                    Iterator it = issuers.iterator();
                    while (it.hasNext() && builderResult == null) {
                        X509Certificate issuer = (X509Certificate) it.next();
                        builderResult = build(issuer, pkixParams, tbvPath);
                    }
                    if (builderResult == null) {
                        tbvPath.remove(tbvCert);
                    }
                    return builderResult;
                } catch (AnnotatedException e4) {
                    throw new AnnotatedException("Cannot find issuer certificate for certificate in certification path.", e4);
                }
            } catch (CertificateParsingException e5) {
                throw new AnnotatedException("No additional X.509 stores can be added from certificate locations.", e5);
            }
            this.certPathException = e;
            if (builderResult == null) {
            }
            return builderResult;
        } catch (Exception e6) {
            throw new RuntimeException("Exception creating support classes.");
        }
    }
}
