package sun.security.provider.certpath;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.security.auth.x500.X500Principal;
import sun.security.provider.certpath.PKIX;
import sun.security.util.Debug;
import sun.security.x509.AccessDescription;
import sun.security.x509.AuthorityInfoAccessExtension;
import sun.security.x509.AuthorityKeyIdentifierExtension;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;

class ForwardBuilder extends Builder {
    private static final Debug debug = Debug.getInstance("certpath");
    private AdaptableX509CertSelector caSelector;
    private X509CertSelector caTargetSelector;
    private X509CertSelector eeSelector;
    private boolean searchAllCertStores;
    TrustAnchor trustAnchor;
    private final Set<TrustAnchor> trustAnchors;
    private final Set<X509Certificate> trustedCerts;
    private final Set<X500Principal> trustedSubjectDNs;

    ForwardBuilder(PKIX.BuilderParams builderParams, boolean z) {
        super(builderParams);
        this.searchAllCertStores = true;
        this.trustAnchors = builderParams.trustAnchors();
        this.trustedCerts = new HashSet(this.trustAnchors.size());
        this.trustedSubjectDNs = new HashSet(this.trustAnchors.size());
        for (TrustAnchor trustAnchor : this.trustAnchors) {
            X509Certificate trustedCert = trustAnchor.getTrustedCert();
            if (trustedCert != null) {
                this.trustedCerts.add(trustedCert);
                this.trustedSubjectDNs.add(trustedCert.getSubjectX500Principal());
            } else {
                this.trustedSubjectDNs.add(trustAnchor.getCA());
            }
        }
        this.searchAllCertStores = z;
    }

    @Override
    Collection<X509Certificate> getMatchingCerts(State state, List<CertStore> list) throws IOException, CertificateException, CertStoreException {
        if (debug != null) {
            debug.println("ForwardBuilder.getMatchingCerts()...");
        }
        ForwardState forwardState = (ForwardState) state;
        TreeSet treeSet = new TreeSet(new PKIXCertComparator(this.trustedSubjectDNs, forwardState.cert));
        if (forwardState.isInitial()) {
            getMatchingEECerts(forwardState, list, treeSet);
        }
        getMatchingCACerts(forwardState, list, treeSet);
        return treeSet;
    }

    private void getMatchingEECerts(ForwardState forwardState, List<CertStore> list, Collection<X509Certificate> collection) throws IOException {
        if (debug != null) {
            debug.println("ForwardBuilder.getMatchingEECerts()...");
        }
        if (this.eeSelector == null) {
            this.eeSelector = (X509CertSelector) this.targetCertConstraints.clone();
            this.eeSelector.setCertificateValid(this.buildParams.date());
            if (this.buildParams.explicitPolicyRequired()) {
                this.eeSelector.setPolicy(getMatchingPolicies());
            }
            this.eeSelector.setBasicConstraints(-2);
        }
        addMatchingCerts(this.eeSelector, list, collection, this.searchAllCertStores);
    }

    private void getMatchingCACerts(ForwardState forwardState, List<CertStore> list, Collection<X509Certificate> collection) throws IOException {
        X509CertSelector x509CertSelector;
        AuthorityInfoAccessExtension authorityInfoAccessExtension;
        if (debug != null) {
            debug.println("ForwardBuilder.getMatchingCACerts()...");
        }
        int size = collection.size();
        if (forwardState.isInitial()) {
            if (this.targetCertConstraints.getBasicConstraints() == -2) {
                return;
            }
            if (debug != null) {
                debug.println("ForwardBuilder.getMatchingCACerts(): the target is a CA");
            }
            if (this.caTargetSelector == null) {
                this.caTargetSelector = (X509CertSelector) this.targetCertConstraints.clone();
                if (this.buildParams.explicitPolicyRequired()) {
                    this.caTargetSelector.setPolicy(getMatchingPolicies());
                }
            }
            x509CertSelector = this.caTargetSelector;
        } else {
            if (this.caSelector == null) {
                this.caSelector = new AdaptableX509CertSelector();
                if (this.buildParams.explicitPolicyRequired()) {
                    this.caSelector.setPolicy(getMatchingPolicies());
                }
            }
            this.caSelector.setSubject(forwardState.issuerDN);
            CertPathHelper.setPathToNames(this.caSelector, forwardState.subjectNamesTraversed);
            this.caSelector.setValidityPeriod(forwardState.cert.getNotBefore(), forwardState.cert.getNotAfter());
            x509CertSelector = this.caSelector;
        }
        x509CertSelector.setBasicConstraints(-1);
        for (X509Certificate x509Certificate : this.trustedCerts) {
            if (x509CertSelector.match(x509Certificate)) {
                if (debug != null) {
                    debug.println("ForwardBuilder.getMatchingCACerts: found matching trust anchor.\n  SN: " + Debug.toHexString(x509Certificate.getSerialNumber()) + "\n  Subject: " + ((Object) x509Certificate.getSubjectX500Principal()) + "\n  Issuer: " + ((Object) x509Certificate.getIssuerX500Principal()));
                }
                if (collection.add(x509Certificate) && !this.searchAllCertStores) {
                    return;
                }
            }
        }
        x509CertSelector.setCertificateValid(this.buildParams.date());
        x509CertSelector.setBasicConstraints(forwardState.traversedCACerts);
        if ((forwardState.isInitial() || this.buildParams.maxPathLength() == -1 || this.buildParams.maxPathLength() > forwardState.traversedCACerts) && addMatchingCerts(x509CertSelector, list, collection, this.searchAllCertStores) && !this.searchAllCertStores) {
            return;
        }
        if (!forwardState.isInitial() && Builder.USE_AIA && (authorityInfoAccessExtension = forwardState.cert.getAuthorityInfoAccessExtension()) != null) {
            getCerts(authorityInfoAccessExtension, collection);
        }
        if (debug != null) {
            int size2 = collection.size() - size;
            debug.println("ForwardBuilder.getMatchingCACerts: found " + size2 + " CA certs");
        }
    }

    private boolean getCerts(AuthorityInfoAccessExtension authorityInfoAccessExtension, Collection<X509Certificate> collection) {
        List<AccessDescription> accessDescriptions;
        boolean z = false;
        if (!Builder.USE_AIA || (accessDescriptions = authorityInfoAccessExtension.getAccessDescriptions()) == null || accessDescriptions.isEmpty()) {
            return false;
        }
        Iterator<AccessDescription> it = accessDescriptions.iterator();
        while (it.hasNext()) {
            CertStore uRICertStore = URICertStore.getInstance(it.next());
            if (uRICertStore != null) {
                try {
                    if (collection.addAll(uRICertStore.getCertificates(this.caSelector))) {
                        try {
                            if (!this.searchAllCertStores) {
                                return true;
                            }
                            z = true;
                        } catch (CertStoreException e) {
                            e = e;
                            z = true;
                            if (debug != null) {
                                debug.println("exception getting certs from CertStore:");
                                e.printStackTrace();
                            }
                        }
                    } else {
                        continue;
                    }
                } catch (CertStoreException e2) {
                    e = e2;
                }
            }
        }
        return z;
    }

    static class PKIXCertComparator implements Comparator<X509Certificate> {
        static final String METHOD_NME = "PKIXCertComparator.compare()";
        private final X509CertSelector certSkidSelector;
        private final Set<X500Principal> trustedSubjectDNs;

        PKIXCertComparator(Set<X500Principal> set, X509CertImpl x509CertImpl) throws IOException {
            this.trustedSubjectDNs = set;
            this.certSkidSelector = getSelector(x509CertImpl);
        }

        private X509CertSelector getSelector(X509CertImpl x509CertImpl) throws IOException {
            AuthorityKeyIdentifierExtension authorityKeyIdentifierExtension;
            byte[] encodedKeyIdentifier;
            if (x509CertImpl != null && (authorityKeyIdentifierExtension = x509CertImpl.getAuthorityKeyIdentifierExtension()) != null && (encodedKeyIdentifier = authorityKeyIdentifierExtension.getEncodedKeyIdentifier()) != null) {
                X509CertSelector x509CertSelector = new X509CertSelector();
                x509CertSelector.setSubjectKeyIdentifier(encodedKeyIdentifier);
                return x509CertSelector;
            }
            return null;
        }

        @Override
        public int compare(X509Certificate x509Certificate, X509Certificate x509Certificate2) {
            if (x509Certificate.equals(x509Certificate2)) {
                return 0;
            }
            if (this.certSkidSelector != null) {
                if (this.certSkidSelector.match(x509Certificate)) {
                    return -1;
                }
                if (this.certSkidSelector.match(x509Certificate2)) {
                    return 1;
                }
            }
            X500Principal issuerX500Principal = x509Certificate.getIssuerX500Principal();
            X500Principal issuerX500Principal2 = x509Certificate2.getIssuerX500Principal();
            X500Name x500NameAsX500Name = X500Name.asX500Name(issuerX500Principal);
            X500Name x500NameAsX500Name2 = X500Name.asX500Name(issuerX500Principal2);
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() o1 Issuer:  " + ((Object) issuerX500Principal));
                ForwardBuilder.debug.println("PKIXCertComparator.compare() o2 Issuer:  " + ((Object) issuerX500Principal2));
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() MATCH TRUSTED SUBJECT TEST...");
            }
            boolean zContains = this.trustedSubjectDNs.contains(issuerX500Principal);
            boolean zContains2 = this.trustedSubjectDNs.contains(issuerX500Principal2);
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() m1: " + zContains);
                ForwardBuilder.debug.println("PKIXCertComparator.compare() m2: " + zContains2);
            }
            if ((zContains && zContains2) || zContains) {
                return -1;
            }
            if (zContains2) {
                return 1;
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() NAMING DESCENDANT TEST...");
            }
            Iterator<X500Principal> it = this.trustedSubjectDNs.iterator();
            while (it.hasNext()) {
                X500Name x500NameAsX500Name3 = X500Name.asX500Name(it.next());
                int iDistance = Builder.distance(x500NameAsX500Name3, x500NameAsX500Name, -1);
                int iDistance2 = Builder.distance(x500NameAsX500Name3, x500NameAsX500Name2, -1);
                if (ForwardBuilder.debug != null) {
                    ForwardBuilder.debug.println("PKIXCertComparator.compare() distanceTto1: " + iDistance);
                    ForwardBuilder.debug.println("PKIXCertComparator.compare() distanceTto2: " + iDistance2);
                }
                if (iDistance > 0 || iDistance2 > 0) {
                    if (iDistance == iDistance2) {
                        return -1;
                    }
                    if (iDistance <= 0 || iDistance2 > 0) {
                        return ((iDistance > 0 || iDistance2 <= 0) && iDistance < iDistance2) ? -1 : 1;
                    }
                    return -1;
                }
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() NAMING ANCESTOR TEST...");
            }
            Iterator<X500Principal> it2 = this.trustedSubjectDNs.iterator();
            while (it2.hasNext()) {
                X500Name x500NameAsX500Name4 = X500Name.asX500Name(it2.next());
                int iDistance3 = Builder.distance(x500NameAsX500Name4, x500NameAsX500Name, Integer.MAX_VALUE);
                int iDistance4 = Builder.distance(x500NameAsX500Name4, x500NameAsX500Name2, Integer.MAX_VALUE);
                if (ForwardBuilder.debug != null) {
                    ForwardBuilder.debug.println("PKIXCertComparator.compare() distanceTto1: " + iDistance3);
                    ForwardBuilder.debug.println("PKIXCertComparator.compare() distanceTto2: " + iDistance4);
                }
                if (iDistance3 < 0 || iDistance4 < 0) {
                    if (iDistance3 == iDistance4) {
                        return -1;
                    }
                    if (iDistance3 >= 0 || iDistance4 < 0) {
                        return ((iDistance3 < 0 || iDistance4 >= 0) && iDistance3 > iDistance4) ? -1 : 1;
                    }
                    return -1;
                }
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() SAME NAMESPACE AS TRUSTED TEST...");
            }
            Iterator<X500Principal> it3 = this.trustedSubjectDNs.iterator();
            while (it3.hasNext()) {
                X500Name x500NameAsX500Name5 = X500Name.asX500Name(it3.next());
                X500Name x500NameCommonAncestor = x500NameAsX500Name5.commonAncestor(x500NameAsX500Name);
                X500Name x500NameCommonAncestor2 = x500NameAsX500Name5.commonAncestor(x500NameAsX500Name2);
                if (ForwardBuilder.debug != null) {
                    ForwardBuilder.debug.println("PKIXCertComparator.compare() tAo1: " + String.valueOf(x500NameCommonAncestor));
                    ForwardBuilder.debug.println("PKIXCertComparator.compare() tAo2: " + String.valueOf(x500NameCommonAncestor2));
                }
                if (x500NameCommonAncestor != null || x500NameCommonAncestor2 != null) {
                    if (x500NameCommonAncestor == null || x500NameCommonAncestor2 == null) {
                        return x500NameCommonAncestor == null ? 1 : -1;
                    }
                    int iHops = Builder.hops(x500NameAsX500Name5, x500NameAsX500Name, Integer.MAX_VALUE);
                    int iHops2 = Builder.hops(x500NameAsX500Name5, x500NameAsX500Name2, Integer.MAX_VALUE);
                    if (ForwardBuilder.debug != null) {
                        ForwardBuilder.debug.println("PKIXCertComparator.compare() hopsTto1: " + iHops);
                        ForwardBuilder.debug.println("PKIXCertComparator.compare() hopsTto2: " + iHops2);
                    }
                    if (iHops != iHops2) {
                        return iHops > iHops2 ? 1 : -1;
                    }
                }
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() CERT ISSUER/SUBJECT COMPARISON TEST...");
            }
            X500Principal subjectX500Principal = x509Certificate.getSubjectX500Principal();
            X500Principal subjectX500Principal2 = x509Certificate2.getSubjectX500Principal();
            X500Name x500NameAsX500Name6 = X500Name.asX500Name(subjectX500Principal);
            X500Name x500NameAsX500Name7 = X500Name.asX500Name(subjectX500Principal2);
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() o1 Subject: " + ((Object) subjectX500Principal));
                ForwardBuilder.debug.println("PKIXCertComparator.compare() o2 Subject: " + ((Object) subjectX500Principal2));
            }
            int iDistance5 = Builder.distance(x500NameAsX500Name6, x500NameAsX500Name, Integer.MAX_VALUE);
            int iDistance6 = Builder.distance(x500NameAsX500Name7, x500NameAsX500Name2, Integer.MAX_VALUE);
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() distanceStoI1: " + iDistance5);
                ForwardBuilder.debug.println("PKIXCertComparator.compare() distanceStoI2: " + iDistance6);
            }
            if (iDistance6 > iDistance5) {
                return -1;
            }
            if (iDistance6 < iDistance5) {
                return 1;
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() no tests matched; RETURN 0");
            }
            return -1;
        }
    }

    @Override
    void verifyCert(X509Certificate x509Certificate, State state, List<X509Certificate> list) throws GeneralSecurityException {
        Set<String> supportedExtensions;
        if (debug != null) {
            debug.println("ForwardBuilder.verifyCert(SN: " + Debug.toHexString(x509Certificate.getSerialNumber()) + "\n  Issuer: " + ((Object) x509Certificate.getIssuerX500Principal()) + ")\n  Subject: " + ((Object) x509Certificate.getSubjectX500Principal()) + ")");
        }
        ForwardState forwardState = (ForwardState) state;
        if (list != null) {
            Iterator<X509Certificate> it = list.iterator();
            while (it.hasNext()) {
                if (x509Certificate.equals(it.next())) {
                    if (debug != null) {
                        debug.println("loop detected!!");
                    }
                    throw new CertPathValidatorException("loop detected");
                }
            }
        }
        boolean zContains = this.trustedCerts.contains(x509Certificate);
        if (!zContains) {
            Set<String> criticalExtensionOIDs = x509Certificate.getCriticalExtensionOIDs();
            if (criticalExtensionOIDs == null) {
                criticalExtensionOIDs = Collections.emptySet();
            }
            Iterator<PKIXCertPathChecker> it2 = forwardState.forwardCheckers.iterator();
            while (it2.hasNext()) {
                it2.next().check(x509Certificate, criticalExtensionOIDs);
            }
            for (PKIXCertPathChecker pKIXCertPathChecker : this.buildParams.certPathCheckers()) {
                if (!pKIXCertPathChecker.isForwardCheckingSupported() && (supportedExtensions = pKIXCertPathChecker.getSupportedExtensions()) != null) {
                    criticalExtensionOIDs.removeAll(supportedExtensions);
                }
            }
            if (!criticalExtensionOIDs.isEmpty()) {
                criticalExtensionOIDs.remove(PKIXExtensions.BasicConstraints_Id.toString());
                criticalExtensionOIDs.remove(PKIXExtensions.NameConstraints_Id.toString());
                criticalExtensionOIDs.remove(PKIXExtensions.CertificatePolicies_Id.toString());
                criticalExtensionOIDs.remove(PKIXExtensions.PolicyMappings_Id.toString());
                criticalExtensionOIDs.remove(PKIXExtensions.PolicyConstraints_Id.toString());
                criticalExtensionOIDs.remove(PKIXExtensions.InhibitAnyPolicy_Id.toString());
                criticalExtensionOIDs.remove(PKIXExtensions.SubjectAlternativeName_Id.toString());
                criticalExtensionOIDs.remove(PKIXExtensions.KeyUsage_Id.toString());
                criticalExtensionOIDs.remove(PKIXExtensions.ExtendedKeyUsage_Id.toString());
                if (!criticalExtensionOIDs.isEmpty()) {
                    throw new CertPathValidatorException("Unrecognized critical extension(s)", null, null, -1, PKIXReason.UNRECOGNIZED_CRIT_EXT);
                }
            }
        }
        if (forwardState.isInitial()) {
            return;
        }
        if (!zContains) {
            if (x509Certificate.getBasicConstraints() == -1) {
                throw new CertificateException("cert is NOT a CA cert");
            }
            KeyChecker.verifyCAKeyUsage(x509Certificate);
        }
        if (!forwardState.keyParamsNeeded()) {
            if (this.buildParams.sigProvider() != null) {
                forwardState.cert.verify(x509Certificate.getPublicKey(), this.buildParams.sigProvider());
            } else {
                forwardState.cert.verify(x509Certificate.getPublicKey());
            }
        }
    }

    @Override
    boolean isPathCompleted(X509Certificate x509Certificate) {
        ArrayList<TrustAnchor> arrayList = new ArrayList();
        for (TrustAnchor trustAnchor : this.trustAnchors) {
            if (trustAnchor.getTrustedCert() != null) {
                if (x509Certificate.equals(trustAnchor.getTrustedCert())) {
                    this.trustAnchor = trustAnchor;
                    return true;
                }
            } else {
                X500Principal ca = trustAnchor.getCA();
                PublicKey cAPublicKey = trustAnchor.getCAPublicKey();
                if (ca != null && cAPublicKey != null && ca.equals(x509Certificate.getSubjectX500Principal()) && cAPublicKey.equals(x509Certificate.getPublicKey())) {
                    this.trustAnchor = trustAnchor;
                    return true;
                }
                arrayList.add(trustAnchor);
            }
        }
        for (TrustAnchor trustAnchor2 : arrayList) {
            X500Principal ca2 = trustAnchor2.getCA();
            PublicKey cAPublicKey2 = trustAnchor2.getCAPublicKey();
            if (ca2 != null && ca2.equals(x509Certificate.getIssuerX500Principal()) && !PKIX.isDSAPublicKeyWithoutParams(cAPublicKey2)) {
                try {
                    if (this.buildParams.sigProvider() != null) {
                        x509Certificate.verify(cAPublicKey2, this.buildParams.sigProvider());
                    } else {
                        x509Certificate.verify(cAPublicKey2);
                    }
                    this.trustAnchor = trustAnchor2;
                    return true;
                } catch (InvalidKeyException e) {
                    if (debug != null) {
                        debug.println("ForwardBuilder.isPathCompleted() invalid DSA key found");
                    }
                } catch (GeneralSecurityException e2) {
                    if (debug != null) {
                        debug.println("ForwardBuilder.isPathCompleted() unexpected exception");
                        e2.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    @Override
    void addCertToPath(X509Certificate x509Certificate, LinkedList<X509Certificate> linkedList) {
        linkedList.addFirst(x509Certificate);
    }

    @Override
    void removeFinalCertFromPath(LinkedList<X509Certificate> linkedList) {
        linkedList.removeFirst();
    }
}
