package sun.security.provider.certpath;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathBuilderSpi;
import java.security.cert.CertPathChecker;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertSelector;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.PolicyNode;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import sun.security.provider.certpath.PKIX;
import sun.security.util.Debug;
import sun.security.x509.PKIXExtensions;

public final class SunCertPathBuilder extends CertPathBuilderSpi {
    private static final Debug debug = Debug.getInstance("certpath");
    private PKIX.BuilderParams buildParams;
    private CertificateFactory cf;
    private PublicKey finalPublicKey;
    private boolean pathCompleted = false;
    private PolicyNode policyTreeResult;
    private TrustAnchor trustAnchor;

    public SunCertPathBuilder() throws CertPathBuilderException {
        try {
            this.cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new CertPathBuilderException(e);
        }
    }

    @Override
    public CertPathChecker engineGetRevocationChecker() {
        return new RevocationChecker();
    }

    @Override
    public CertPathBuilderResult engineBuild(CertPathParameters certPathParameters) throws CertPathBuilderException, InvalidAlgorithmParameterException {
        if (debug != null) {
            debug.println("SunCertPathBuilder.engineBuild(" + ((Object) certPathParameters) + ")");
        }
        this.buildParams = PKIX.checkBuilderParams(certPathParameters);
        return build();
    }

    private PKIXCertPathBuilderResult build() throws CertPathBuilderException {
        ArrayList arrayList = new ArrayList();
        PKIXCertPathBuilderResult pKIXCertPathBuilderResultBuildCertPath = buildCertPath(false, arrayList);
        if (pKIXCertPathBuilderResultBuildCertPath == null) {
            if (debug != null) {
                debug.println("SunCertPathBuilder.engineBuild: 2nd pass; try building again searching all certstores");
            }
            arrayList.clear();
            pKIXCertPathBuilderResultBuildCertPath = buildCertPath(true, arrayList);
            if (pKIXCertPathBuilderResultBuildCertPath == null) {
                throw new SunCertPathBuilderException("unable to find valid certification path to requested target", new AdjacencyList(arrayList));
            }
        }
        return pKIXCertPathBuilderResultBuildCertPath;
    }

    private PKIXCertPathBuilderResult buildCertPath(boolean z, List<List<Vertex>> list) throws CertPathBuilderException {
        this.pathCompleted = false;
        this.trustAnchor = null;
        this.finalPublicKey = null;
        this.policyTreeResult = null;
        LinkedList<X509Certificate> linkedList = new LinkedList<>();
        try {
            buildForward(list, linkedList, z);
            try {
                if (!this.pathCompleted) {
                    return null;
                }
                if (debug != null) {
                    debug.println("SunCertPathBuilder.engineBuild() pathCompleted");
                }
                Collections.reverse(linkedList);
                return new SunCertPathBuilderResult(this.cf.generateCertPath(linkedList), this.trustAnchor, this.policyTreeResult, this.finalPublicKey, new AdjacencyList(list));
            } catch (CertificateException e) {
                if (debug != null) {
                    debug.println("SunCertPathBuilder.engineBuild() exception in wrap-up");
                    e.printStackTrace();
                }
                throw new SunCertPathBuilderException("unable to find valid certification path to requested target", e, new AdjacencyList(list));
            }
        } catch (IOException | GeneralSecurityException e2) {
            if (debug != null) {
                debug.println("SunCertPathBuilder.engineBuild() exception in build");
                e2.printStackTrace();
            }
            throw new SunCertPathBuilderException("unable to find valid certification path to requested target", e2, new AdjacencyList(list));
        }
    }

    private void buildForward(List<List<Vertex>> list, LinkedList<X509Certificate> linkedList, boolean z) throws GeneralSecurityException, IOException {
        if (debug != null) {
            debug.println("SunCertPathBuilder.buildForward()...");
        }
        ForwardState forwardState = new ForwardState();
        forwardState.initState(this.buildParams.certPathCheckers());
        list.clear();
        list.add(new LinkedList());
        depthFirstSearchForward(this.buildParams.targetSubject(), forwardState, new ForwardBuilder(this.buildParams, z), list, linkedList);
    }

    private void depthFirstSearchForward(X500Principal x500Principal, ForwardState forwardState, ForwardBuilder forwardBuilder, List<List<Vertex>> list, LinkedList<X509Certificate> linkedList) throws GeneralSecurityException, IOException {
        GeneralSecurityException generalSecurityException;
        X509Certificate last;
        ArrayList arrayList;
        Set<String> supportedExtensions;
        ArrayList arrayList2;
        Iterator it;
        CertPathValidatorException certPathValidatorException;
        if (debug != null) {
            debug.println("SunCertPathBuilder.depthFirstSearchForward(" + ((Object) x500Principal) + ", " + forwardState.toString() + ")");
        }
        List<Vertex> listAddVertices = addVertices(forwardBuilder.getMatchingCerts(forwardState, this.buildParams.certStores()), list);
        if (debug != null) {
            debug.println("SunCertPathBuilder.depthFirstSearchForward(): certs.size=" + listAddVertices.size());
        }
        for (Vertex vertex : listAddVertices) {
            ForwardState forwardState2 = (ForwardState) forwardState.clone();
            X509Certificate certificate = vertex.getCertificate();
            try {
                forwardBuilder.verifyCert(certificate, forwardState2, linkedList);
                if (forwardBuilder.isPathCompleted(certificate)) {
                    if (debug != null) {
                        debug.println("SunCertPathBuilder.depthFirstSearchForward(): commencing final verification");
                    }
                    ArrayList arrayList3 = new ArrayList(linkedList);
                    if (forwardBuilder.trustAnchor.getTrustedCert() == null) {
                        arrayList3.add(0, certificate);
                    }
                    PolicyNodeImpl policyNodeImpl = new PolicyNodeImpl(null, "2.5.29.32.0", null, false, Collections.singleton("2.5.29.32.0"), false);
                    ArrayList arrayList4 = new ArrayList();
                    PolicyChecker policyChecker = new PolicyChecker(this.buildParams.initialPolicies(), arrayList3.size(), this.buildParams.explicitPolicyRequired(), this.buildParams.policyMappingInhibited(), this.buildParams.anyPolicyInhibited(), this.buildParams.policyQualifiersRejected(), policyNodeImpl);
                    arrayList4.add(policyChecker);
                    arrayList4.add(new AlgorithmChecker(forwardBuilder.trustAnchor));
                    BasicChecker basicChecker = null;
                    if (forwardState2.keyParamsNeeded()) {
                        PublicKey publicKey = certificate.getPublicKey();
                        if (forwardBuilder.trustAnchor.getTrustedCert() == null) {
                            publicKey = forwardBuilder.trustAnchor.getCAPublicKey();
                            if (debug != null) {
                                debug.println("SunCertPathBuilder.depthFirstSearchForward using buildParams public key: " + publicKey.toString());
                            }
                        }
                        basicChecker = new BasicChecker(new TrustAnchor(certificate.getSubjectX500Principal(), publicKey, (byte[]) null), this.buildParams.date(), this.buildParams.sigProvider(), true);
                        arrayList4.add(basicChecker);
                    }
                    this.buildParams.setCertPath(this.cf.generateCertPath(arrayList3));
                    List<PKIXCertPathChecker> listCertPathCheckers = this.buildParams.certPathCheckers();
                    Iterator<PKIXCertPathChecker> it2 = listCertPathCheckers.iterator();
                    boolean z = false;
                    while (it2.hasNext()) {
                        PKIXCertPathChecker next = it2.next();
                        Iterator<PKIXCertPathChecker> it3 = it2;
                        if (next instanceof PKIXRevocationChecker) {
                            if (z) {
                                throw new CertPathValidatorException("Only one PKIXRevocationChecker can be specified");
                            }
                            if (next instanceof RevocationChecker) {
                                ((RevocationChecker) next).init(forwardBuilder.trustAnchor, this.buildParams);
                            }
                            z = true;
                        }
                        it2 = it3;
                    }
                    if (this.buildParams.revocationEnabled() && !z) {
                        arrayList4.add(new RevocationChecker(forwardBuilder.trustAnchor, this.buildParams));
                    }
                    arrayList4.addAll(listCertPathCheckers);
                    int i = 0;
                    while (i < arrayList3.size()) {
                        X509Certificate x509Certificate = (X509Certificate) arrayList3.get(i);
                        if (debug != null) {
                            Debug debug2 = debug;
                            StringBuilder sb = new StringBuilder();
                            arrayList = arrayList3;
                            sb.append("current subject = ");
                            sb.append((Object) x509Certificate.getSubjectX500Principal());
                            debug2.println(sb.toString());
                        } else {
                            arrayList = arrayList3;
                        }
                        Set<String> criticalExtensionOIDs = x509Certificate.getCriticalExtensionOIDs();
                        if (criticalExtensionOIDs == null) {
                            criticalExtensionOIDs = Collections.emptySet();
                        }
                        for (Iterator it4 = arrayList4.iterator(); it4.hasNext(); it4 = it) {
                            PKIXCertPathChecker pKIXCertPathChecker = (PKIXCertPathChecker) it4.next();
                            if (!pKIXCertPathChecker.isForwardCheckingSupported()) {
                                try {
                                    if (i == 0) {
                                        arrayList2 = arrayList4;
                                        pKIXCertPathChecker.init(false);
                                        if (pKIXCertPathChecker instanceof AlgorithmChecker) {
                                            it = it4;
                                            ((AlgorithmChecker) pKIXCertPathChecker).trySetTrustAnchor(forwardBuilder.trustAnchor);
                                        }
                                        pKIXCertPathChecker.check(x509Certificate, criticalExtensionOIDs);
                                    } else {
                                        arrayList2 = arrayList4;
                                    }
                                    pKIXCertPathChecker.check(x509Certificate, criticalExtensionOIDs);
                                } catch (CertPathValidatorException e) {
                                    if (debug != null) {
                                        Debug debug3 = debug;
                                        StringBuilder sb2 = new StringBuilder();
                                        sb2.append("SunCertPathBuilder.depthFirstSearchForward(): final verification failed: ");
                                        certPathValidatorException = e;
                                        sb2.append((Object) certPathValidatorException);
                                        debug3.println(sb2.toString());
                                    } else {
                                        certPathValidatorException = e;
                                    }
                                    if (this.buildParams.targetCertConstraints().match(x509Certificate) && certPathValidatorException.getReason() == CertPathValidatorException.BasicReason.REVOKED) {
                                        throw certPathValidatorException;
                                    }
                                    vertex.setThrowable(certPathValidatorException);
                                }
                                it = it4;
                            } else {
                                arrayList2 = arrayList4;
                                it = it4;
                            }
                            arrayList4 = arrayList2;
                        }
                        ArrayList arrayList5 = arrayList4;
                        for (PKIXCertPathChecker pKIXCertPathChecker2 : this.buildParams.certPathCheckers()) {
                            if (pKIXCertPathChecker2.isForwardCheckingSupported() && (supportedExtensions = pKIXCertPathChecker2.getSupportedExtensions()) != null) {
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
                                throw new CertPathValidatorException("unrecognized critical extension(s)", null, null, -1, PKIXReason.UNRECOGNIZED_CRIT_EXT);
                            }
                        }
                        i++;
                        arrayList3 = arrayList;
                        arrayList4 = arrayList5;
                    }
                    if (debug != null) {
                        debug.println("SunCertPathBuilder.depthFirstSearchForward(): final verification succeeded - path completed!");
                    }
                    this.pathCompleted = true;
                    if (forwardBuilder.trustAnchor.getTrustedCert() == null) {
                        forwardBuilder.addCertToPath(certificate, linkedList);
                    }
                    this.trustAnchor = forwardBuilder.trustAnchor;
                    if (basicChecker != null) {
                        this.finalPublicKey = basicChecker.getPublicKey();
                    } else {
                        if (linkedList.isEmpty()) {
                            last = forwardBuilder.trustAnchor.getTrustedCert();
                        } else {
                            last = linkedList.getLast();
                        }
                        this.finalPublicKey = last.getPublicKey();
                    }
                    this.policyTreeResult = policyChecker.getPolicyTree();
                    return;
                }
                forwardBuilder.addCertToPath(certificate, linkedList);
                forwardState2.updateState(certificate);
                list.add(new LinkedList());
                vertex.setIndex(list.size() - 1);
                depthFirstSearchForward(certificate.getIssuerX500Principal(), forwardState2, forwardBuilder, list, linkedList);
                if (this.pathCompleted) {
                    return;
                }
                if (debug != null) {
                    debug.println("SunCertPathBuilder.depthFirstSearchForward(): backtracking");
                }
                forwardBuilder.removeFinalCertFromPath(linkedList);
            } catch (GeneralSecurityException e2) {
                if (debug != null) {
                    Debug debug4 = debug;
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("SunCertPathBuilder.depthFirstSearchForward(): validation failed: ");
                    generalSecurityException = e2;
                    sb3.append((Object) generalSecurityException);
                    debug4.println(sb3.toString());
                    generalSecurityException.printStackTrace();
                } else {
                    generalSecurityException = e2;
                }
                vertex.setThrowable(generalSecurityException);
            }
        }
    }

    private static List<Vertex> addVertices(Collection<X509Certificate> collection, List<List<Vertex>> list) {
        List<Vertex> list2 = list.get(list.size() - 1);
        Iterator<X509Certificate> it = collection.iterator();
        while (it.hasNext()) {
            list2.add(new Vertex(it.next()));
        }
        return list2;
    }

    private static boolean anchorIsTarget(TrustAnchor trustAnchor, CertSelector certSelector) {
        X509Certificate trustedCert = trustAnchor.getTrustedCert();
        if (trustedCert != null) {
            return certSelector.match(trustedCert);
        }
        return false;
    }
}
