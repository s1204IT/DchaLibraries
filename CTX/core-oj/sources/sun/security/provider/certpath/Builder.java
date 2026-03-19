package sun.security.provider.certpath;

import java.io.IOException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import sun.security.action.GetBooleanAction;
import sun.security.provider.certpath.PKIX;
import sun.security.util.Debug;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.GeneralSubtrees;
import sun.security.x509.NameConstraintsExtension;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;

public abstract class Builder {
    final PKIX.BuilderParams buildParams;
    private Set<String> matchingPolicies;
    final X509CertSelector targetCertConstraints;
    private static final Debug debug = Debug.getInstance("certpath");
    static final boolean USE_AIA = ((Boolean) AccessController.doPrivileged(new GetBooleanAction("com.sun.security.enableAIAcaIssuers"))).booleanValue();

    abstract void addCertToPath(X509Certificate x509Certificate, LinkedList<X509Certificate> linkedList);

    abstract Collection<X509Certificate> getMatchingCerts(State state, List<CertStore> list) throws IOException, CertificateException, CertStoreException;

    abstract boolean isPathCompleted(X509Certificate x509Certificate);

    abstract void removeFinalCertFromPath(LinkedList<X509Certificate> linkedList);

    abstract void verifyCert(X509Certificate x509Certificate, State state, List<X509Certificate> list) throws GeneralSecurityException;

    Builder(PKIX.BuilderParams builderParams) {
        this.buildParams = builderParams;
        this.targetCertConstraints = (X509CertSelector) builderParams.targetCertConstraints();
    }

    static int distance(GeneralNameInterface generalNameInterface, GeneralNameInterface generalNameInterface2, int i) {
        switch (generalNameInterface.constrains(generalNameInterface2)) {
            case -1:
                if (debug != null) {
                    debug.println("Builder.distance(): Names are different types");
                }
                break;
            case 3:
                if (debug != null) {
                    debug.println("Builder.distance(): Names are same type but in different subtrees");
                }
                break;
        }
        return i;
    }

    static int hops(GeneralNameInterface generalNameInterface, GeneralNameInterface generalNameInterface2, int i) {
        switch (generalNameInterface.constrains(generalNameInterface2)) {
            case -1:
                if (debug != null) {
                    debug.println("Builder.hops(): Names are different types");
                }
                break;
            case 3:
                if (generalNameInterface.getType() != 4) {
                    if (debug != null) {
                        debug.println("Builder.hops(): hopDistance not implemented for this name type");
                    }
                } else {
                    X500Name x500Name = (X500Name) generalNameInterface;
                    X500Name x500Name2 = (X500Name) generalNameInterface2;
                    X500Name x500NameCommonAncestor = x500Name.commonAncestor(x500Name2);
                    if (x500NameCommonAncestor == null) {
                        if (debug != null) {
                            debug.println("Builder.hops(): Names are in different namespaces");
                        }
                    }
                }
                break;
        }
        return i;
    }

    static int targetDistance(NameConstraintsExtension nameConstraintsExtension, X509Certificate x509Certificate, GeneralNameInterface generalNameInterface) throws IOException {
        GeneralNames generalNames;
        if (nameConstraintsExtension != null && !nameConstraintsExtension.verify(x509Certificate)) {
            throw new IOException("certificate does not satisfy existing name constraints");
        }
        try {
            X509CertImpl impl = X509CertImpl.toImpl(x509Certificate);
            if (X500Name.asX500Name(impl.getSubjectX500Principal()).equals(generalNameInterface)) {
                return 0;
            }
            SubjectAlternativeNameExtension subjectAlternativeNameExtension = impl.getSubjectAlternativeNameExtension();
            if (subjectAlternativeNameExtension != null && (generalNames = subjectAlternativeNameExtension.get(SubjectAlternativeNameExtension.SUBJECT_NAME)) != null) {
                int size = generalNames.size();
                for (int i = 0; i < size; i++) {
                    if (generalNames.get(i).getName().equals(generalNameInterface)) {
                        return 0;
                    }
                }
            }
            NameConstraintsExtension nameConstraintsExtension2 = impl.getNameConstraintsExtension();
            if (nameConstraintsExtension2 == null) {
                return -1;
            }
            if (nameConstraintsExtension != null) {
                nameConstraintsExtension.merge(nameConstraintsExtension2);
            } else {
                nameConstraintsExtension = (NameConstraintsExtension) nameConstraintsExtension2.clone();
            }
            if (debug != null) {
                debug.println("Builder.targetDistance() merged constraints: " + String.valueOf(nameConstraintsExtension));
            }
            GeneralSubtrees generalSubtrees = nameConstraintsExtension.get(NameConstraintsExtension.PERMITTED_SUBTREES);
            GeneralSubtrees generalSubtrees2 = nameConstraintsExtension.get(NameConstraintsExtension.EXCLUDED_SUBTREES);
            if (generalSubtrees != null) {
                generalSubtrees.reduce(generalSubtrees2);
            }
            if (debug != null) {
                debug.println("Builder.targetDistance() reduced constraints: " + ((Object) generalSubtrees));
            }
            if (!nameConstraintsExtension.verify(generalNameInterface)) {
                throw new IOException("New certificate not allowed to sign certificate for target");
            }
            if (generalSubtrees == null) {
                return -1;
            }
            int size2 = generalSubtrees.size();
            for (int i2 = 0; i2 < size2; i2++) {
                int iDistance = distance(generalSubtrees.get(i2).getName().getName(), generalNameInterface, -1);
                if (iDistance >= 0) {
                    return iDistance + 1;
                }
            }
            return -1;
        } catch (CertificateException e) {
            throw new IOException("Invalid certificate", e);
        }
    }

    Set<String> getMatchingPolicies() {
        if (this.matchingPolicies != null) {
            Set<String> setInitialPolicies = this.buildParams.initialPolicies();
            if (!setInitialPolicies.isEmpty() && !setInitialPolicies.contains("2.5.29.32.0") && this.buildParams.policyMappingInhibited()) {
                this.matchingPolicies = new HashSet(setInitialPolicies);
                this.matchingPolicies.add("2.5.29.32.0");
            } else {
                this.matchingPolicies = Collections.emptySet();
            }
        }
        return this.matchingPolicies;
    }

    boolean addMatchingCerts(X509CertSelector x509CertSelector, Collection<CertStore> collection, Collection<X509Certificate> collection2, boolean z) {
        X509Certificate certificate = x509CertSelector.getCertificate();
        boolean z2 = false;
        if (certificate != null) {
            if (!x509CertSelector.match(certificate) || X509CertImpl.isSelfSigned(certificate, this.buildParams.sigProvider())) {
                return false;
            }
            if (debug != null) {
                debug.println("Builder.addMatchingCerts: adding target cert\n  SN: " + Debug.toHexString(certificate.getSerialNumber()) + "\n  Subject: " + ((Object) certificate.getSubjectX500Principal()) + "\n  Issuer: " + ((Object) certificate.getIssuerX500Principal()));
            }
            return collection2.add(certificate);
        }
        Iterator<CertStore> it = collection.iterator();
        while (it.hasNext()) {
            try {
                for (Certificate certificate2 : it.next().getCertificates(x509CertSelector)) {
                    if (!X509CertImpl.isSelfSigned((X509Certificate) certificate2, this.buildParams.sigProvider()) && collection2.add((X509Certificate) certificate2)) {
                        z2 = true;
                    }
                }
            } catch (CertStoreException e) {
                if (debug != null) {
                    debug.println("Builder.addMatchingCerts, non-fatal exception retrieving certs: " + ((Object) e));
                    e.printStackTrace();
                }
            }
            if (!z && z2) {
                return true;
            }
        }
        return z2;
    }
}
