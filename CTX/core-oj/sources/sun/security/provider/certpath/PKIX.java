package sun.security.provider.certpath;

import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathParameters;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;

class PKIX {
    private static final Debug debug = Debug.getInstance("certpath");

    private PKIX() {
    }

    static boolean isDSAPublicKeyWithoutParams(PublicKey publicKey) {
        return (publicKey instanceof DSAPublicKey) && ((DSAPublicKey) publicKey).getParams() == null;
    }

    static ValidatorParams checkParams(CertPath certPath, CertPathParameters certPathParameters) throws InvalidAlgorithmParameterException {
        if (!(certPathParameters instanceof PKIXParameters)) {
            throw new InvalidAlgorithmParameterException("inappropriate params, must be an instance of PKIXParameters");
        }
        return new ValidatorParams(certPath, (PKIXParameters) certPathParameters);
    }

    static BuilderParams checkBuilderParams(CertPathParameters certPathParameters) throws InvalidAlgorithmParameterException {
        if (!(certPathParameters instanceof PKIXBuilderParameters)) {
            throw new InvalidAlgorithmParameterException("inappropriate params, must be an instance of PKIXBuilderParameters");
        }
        return new BuilderParams((PKIXBuilderParameters) certPathParameters);
    }

    static class ValidatorParams {
        private Set<TrustAnchor> anchors;
        private CertPath certPath;
        private List<X509Certificate> certs;
        private List<PKIXCertPathChecker> checkers;
        private CertSelector constraints;
        private Date date;
        private boolean gotConstraints;
        private boolean gotDate;
        private final PKIXParameters params;
        private Set<String> policies;
        private List<CertStore> stores;

        ValidatorParams(CertPath certPath, PKIXParameters pKIXParameters) throws InvalidAlgorithmParameterException {
            this(pKIXParameters);
            if (!certPath.getType().equals("X.509") && !certPath.getType().equals("X509")) {
                throw new InvalidAlgorithmParameterException("inappropriate CertPath type specified, must be X.509 or X509");
            }
            this.certPath = certPath;
        }

        ValidatorParams(PKIXParameters pKIXParameters) throws InvalidAlgorithmParameterException {
            this.anchors = pKIXParameters.getTrustAnchors();
            Iterator<TrustAnchor> it = this.anchors.iterator();
            while (it.hasNext()) {
                if (it.next().getNameConstraints() != null) {
                    throw new InvalidAlgorithmParameterException("name constraints in trust anchor not supported");
                }
            }
            this.params = pKIXParameters;
        }

        CertPath certPath() {
            return this.certPath;
        }

        void setCertPath(CertPath certPath) {
            this.certPath = certPath;
        }

        List<X509Certificate> certificates() {
            if (this.certs == null) {
                if (this.certPath == null) {
                    this.certs = Collections.emptyList();
                } else {
                    ArrayList arrayList = new ArrayList(this.certPath.getCertificates());
                    Collections.reverse(arrayList);
                    this.certs = arrayList;
                }
            }
            return this.certs;
        }

        List<PKIXCertPathChecker> certPathCheckers() {
            if (this.checkers == null) {
                this.checkers = this.params.getCertPathCheckers();
            }
            return this.checkers;
        }

        List<CertStore> certStores() {
            if (this.stores == null) {
                this.stores = this.params.getCertStores();
            }
            return this.stores;
        }

        Date date() {
            if (!this.gotDate) {
                this.date = this.params.getDate();
                if (this.date == null) {
                    this.date = new Date();
                }
                this.gotDate = true;
            }
            return this.date;
        }

        Set<String> initialPolicies() {
            if (this.policies == null) {
                this.policies = this.params.getInitialPolicies();
            }
            return this.policies;
        }

        CertSelector targetCertConstraints() {
            if (!this.gotConstraints) {
                this.constraints = this.params.getTargetCertConstraints();
                this.gotConstraints = true;
            }
            return this.constraints;
        }

        Set<TrustAnchor> trustAnchors() {
            return this.anchors;
        }

        boolean revocationEnabled() {
            return this.params.isRevocationEnabled();
        }

        boolean policyMappingInhibited() {
            return this.params.isPolicyMappingInhibited();
        }

        boolean explicitPolicyRequired() {
            return this.params.isExplicitPolicyRequired();
        }

        boolean policyQualifiersRejected() {
            return this.params.getPolicyQualifiersRejected();
        }

        String sigProvider() {
            return this.params.getSigProvider();
        }

        boolean anyPolicyInhibited() {
            return this.params.isAnyPolicyInhibited();
        }

        PKIXParameters getPKIXParameters() {
            return this.params;
        }
    }

    static class BuilderParams extends ValidatorParams {
        private PKIXBuilderParameters params;
        private List<CertStore> stores;
        private X500Principal targetSubject;

        BuilderParams(PKIXBuilderParameters pKIXBuilderParameters) throws InvalidAlgorithmParameterException {
            super(pKIXBuilderParameters);
            checkParams(pKIXBuilderParameters);
        }

        private void checkParams(PKIXBuilderParameters pKIXBuilderParameters) throws InvalidAlgorithmParameterException {
            if (!(targetCertConstraints() instanceof X509CertSelector)) {
                throw new InvalidAlgorithmParameterException("the targetCertConstraints parameter must be an X509CertSelector");
            }
            this.params = pKIXBuilderParameters;
            this.targetSubject = getTargetSubject(certStores(), (X509CertSelector) targetCertConstraints());
        }

        @Override
        List<CertStore> certStores() {
            if (this.stores == null) {
                this.stores = new ArrayList(this.params.getCertStores());
                Collections.sort(this.stores, new CertStoreComparator());
            }
            return this.stores;
        }

        int maxPathLength() {
            return this.params.getMaxPathLength();
        }

        PKIXBuilderParameters params() {
            return this.params;
        }

        X500Principal targetSubject() {
            return this.targetSubject;
        }

        private static X500Principal getTargetSubject(List<CertStore> list, X509CertSelector x509CertSelector) throws InvalidAlgorithmParameterException {
            X500Principal subject = x509CertSelector.getSubject();
            if (subject != null) {
                return subject;
            }
            X509Certificate certificate = x509CertSelector.getCertificate();
            if (certificate != null) {
                subject = certificate.getSubjectX500Principal();
            }
            if (subject != null) {
                return subject;
            }
            Iterator<CertStore> it = list.iterator();
            while (it.hasNext()) {
                try {
                    Collection<? extends Certificate> certificates = it.next().getCertificates(x509CertSelector);
                    if (!certificates.isEmpty()) {
                        return ((X509Certificate) certificates.iterator().next()).getSubjectX500Principal();
                    }
                    continue;
                } catch (CertStoreException e) {
                    if (PKIX.debug != null) {
                        PKIX.debug.println("BuilderParams.getTargetSubjectDN: non-fatal exception retrieving certs: " + ((Object) e));
                        e.printStackTrace();
                    }
                }
            }
            throw new InvalidAlgorithmParameterException("Could not determine unique target subject");
        }
    }

    static class CertStoreTypeException extends CertStoreException {
        private static final long serialVersionUID = 7463352639238322556L;
        private final String type;

        CertStoreTypeException(String str, CertStoreException certStoreException) {
            super(certStoreException.getMessage(), certStoreException.getCause());
            this.type = str;
        }

        String getType() {
            return this.type;
        }
    }

    private static class CertStoreComparator implements Comparator<CertStore> {
        private CertStoreComparator() {
        }

        @Override
        public int compare(CertStore certStore, CertStore certStore2) {
            if (certStore.getType().equals("Collection") || (certStore.getCertStoreParameters() instanceof CollectionCertStoreParameters)) {
                return -1;
            }
            return 1;
        }
    }
}
