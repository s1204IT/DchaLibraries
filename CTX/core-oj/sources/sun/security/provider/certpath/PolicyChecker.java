package sun.security.provider.certpath;

import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.PolicyNode;
import java.security.cert.PolicyQualifierInfo;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import sun.security.util.Debug;
import sun.security.x509.CertificatePoliciesExtension;
import sun.security.x509.CertificatePolicyMap;
import sun.security.x509.InhibitAnyPolicyExtension;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.PolicyConstraintsExtension;
import sun.security.x509.PolicyInformation;
import sun.security.x509.PolicyMappingsExtension;
import sun.security.x509.X509CertImpl;

class PolicyChecker extends PKIXCertPathChecker {
    static final String ANY_POLICY = "2.5.29.32.0";
    private static final Debug debug = Debug.getInstance("certpath");
    private final boolean anyPolicyInhibited;
    private int certIndex;
    private final int certPathLen;
    private final boolean expPolicyRequired;
    private int explicitPolicy;
    private int inhibitAnyPolicy;
    private final Set<String> initPolicies;
    private final boolean polMappingInhibited;
    private int policyMapping;
    private final boolean rejectPolicyQualifiers;
    private PolicyNodeImpl rootNode;
    private Set<String> supportedExts;

    PolicyChecker(Set<String> set, int i, boolean z, boolean z2, boolean z3, boolean z4, PolicyNodeImpl policyNodeImpl) {
        if (set.isEmpty()) {
            this.initPolicies = new HashSet(1);
            this.initPolicies.add(ANY_POLICY);
        } else {
            this.initPolicies = new HashSet(set);
        }
        this.certPathLen = i;
        this.expPolicyRequired = z;
        this.polMappingInhibited = z2;
        this.anyPolicyInhibited = z3;
        this.rejectPolicyQualifiers = z4;
        this.rootNode = policyNodeImpl;
    }

    @Override
    public void init(boolean z) throws CertPathValidatorException {
        if (z) {
            throw new CertPathValidatorException("forward checking not supported");
        }
        this.certIndex = 1;
        this.explicitPolicy = this.expPolicyRequired ? 0 : this.certPathLen + 1;
        this.policyMapping = this.polMappingInhibited ? 0 : this.certPathLen + 1;
        this.inhibitAnyPolicy = this.anyPolicyInhibited ? 0 : this.certPathLen + 1;
    }

    @Override
    public boolean isForwardCheckingSupported() {
        return false;
    }

    @Override
    public Set<String> getSupportedExtensions() {
        if (this.supportedExts == null) {
            this.supportedExts = new HashSet(4);
            this.supportedExts.add(PKIXExtensions.CertificatePolicies_Id.toString());
            this.supportedExts.add(PKIXExtensions.PolicyMappings_Id.toString());
            this.supportedExts.add(PKIXExtensions.PolicyConstraints_Id.toString());
            this.supportedExts.add(PKIXExtensions.InhibitAnyPolicy_Id.toString());
            this.supportedExts = Collections.unmodifiableSet(this.supportedExts);
        }
        return this.supportedExts;
    }

    @Override
    public void check(Certificate certificate, Collection<String> collection) throws CertPathValidatorException {
        checkPolicy((X509Certificate) certificate);
        if (collection != null && !collection.isEmpty()) {
            collection.remove(PKIXExtensions.CertificatePolicies_Id.toString());
            collection.remove(PKIXExtensions.PolicyMappings_Id.toString());
            collection.remove(PKIXExtensions.PolicyConstraints_Id.toString());
            collection.remove(PKIXExtensions.InhibitAnyPolicy_Id.toString());
        }
    }

    private void checkPolicy(X509Certificate x509Certificate) throws CertPathValidatorException {
        boolean z;
        if (debug != null) {
            debug.println("PolicyChecker.checkPolicy() ---checking certificate policies...");
            debug.println("PolicyChecker.checkPolicy() certIndex = " + this.certIndex);
            debug.println("PolicyChecker.checkPolicy() BEFORE PROCESSING: explicitPolicy = " + this.explicitPolicy);
            debug.println("PolicyChecker.checkPolicy() BEFORE PROCESSING: policyMapping = " + this.policyMapping);
            debug.println("PolicyChecker.checkPolicy() BEFORE PROCESSING: inhibitAnyPolicy = " + this.inhibitAnyPolicy);
            debug.println("PolicyChecker.checkPolicy() BEFORE PROCESSING: policyTree = " + ((Object) this.rootNode));
        }
        try {
            X509CertImpl impl = X509CertImpl.toImpl(x509Certificate);
            if (this.certIndex != this.certPathLen) {
                z = false;
            } else {
                z = true;
            }
            this.rootNode = processPolicies(this.certIndex, this.initPolicies, this.explicitPolicy, this.policyMapping, this.inhibitAnyPolicy, this.rejectPolicyQualifiers, this.rootNode, impl, z);
            if (!z) {
                this.explicitPolicy = mergeExplicitPolicy(this.explicitPolicy, impl, z);
                this.policyMapping = mergePolicyMapping(this.policyMapping, impl);
                this.inhibitAnyPolicy = mergeInhibitAnyPolicy(this.inhibitAnyPolicy, impl);
            }
            this.certIndex++;
            if (debug != null) {
                debug.println("PolicyChecker.checkPolicy() AFTER PROCESSING: explicitPolicy = " + this.explicitPolicy);
                debug.println("PolicyChecker.checkPolicy() AFTER PROCESSING: policyMapping = " + this.policyMapping);
                debug.println("PolicyChecker.checkPolicy() AFTER PROCESSING: inhibitAnyPolicy = " + this.inhibitAnyPolicy);
                debug.println("PolicyChecker.checkPolicy() AFTER PROCESSING: policyTree = " + ((Object) this.rootNode));
                debug.println("PolicyChecker.checkPolicy() certificate policies verified");
            }
        } catch (CertificateException e) {
            throw new CertPathValidatorException(e);
        }
    }

    static int mergeExplicitPolicy(int i, X509CertImpl x509CertImpl, boolean z) throws CertPathValidatorException {
        if (i > 0 && !X509CertImpl.isSelfIssued(x509CertImpl)) {
            i--;
        }
        try {
            PolicyConstraintsExtension policyConstraintsExtension = x509CertImpl.getPolicyConstraintsExtension();
            if (policyConstraintsExtension == null) {
                return i;
            }
            int iIntValue = policyConstraintsExtension.get(PolicyConstraintsExtension.REQUIRE).intValue();
            if (debug != null) {
                debug.println("PolicyChecker.mergeExplicitPolicy() require Index from cert = " + iIntValue);
            }
            if (!z) {
                if (iIntValue != -1) {
                    if (i != -1 && iIntValue >= i) {
                        return i;
                    }
                } else {
                    return i;
                }
            } else if (iIntValue != 0) {
                return i;
            }
            return iIntValue;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("PolicyChecker.mergeExplicitPolicy unexpected exception");
                e.printStackTrace();
            }
            throw new CertPathValidatorException(e);
        }
    }

    static int mergePolicyMapping(int i, X509CertImpl x509CertImpl) throws CertPathValidatorException {
        if (i > 0 && !X509CertImpl.isSelfIssued(x509CertImpl)) {
            i--;
        }
        try {
            PolicyConstraintsExtension policyConstraintsExtension = x509CertImpl.getPolicyConstraintsExtension();
            if (policyConstraintsExtension == null) {
                return i;
            }
            int iIntValue = policyConstraintsExtension.get(PolicyConstraintsExtension.INHIBIT).intValue();
            if (debug != null) {
                debug.println("PolicyChecker.mergePolicyMapping() inhibit Index from cert = " + iIntValue);
            }
            if (iIntValue != -1) {
                if (i == -1 || iIntValue < i) {
                    return iIntValue;
                }
                return i;
            }
            return i;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("PolicyChecker.mergePolicyMapping unexpected exception");
                e.printStackTrace();
            }
            throw new CertPathValidatorException(e);
        }
    }

    static int mergeInhibitAnyPolicy(int i, X509CertImpl x509CertImpl) throws CertPathValidatorException {
        if (i > 0 && !X509CertImpl.isSelfIssued(x509CertImpl)) {
            i--;
        }
        try {
            InhibitAnyPolicyExtension inhibitAnyPolicyExtension = (InhibitAnyPolicyExtension) x509CertImpl.getExtension(PKIXExtensions.InhibitAnyPolicy_Id);
            if (inhibitAnyPolicyExtension == null) {
                return i;
            }
            int iIntValue = inhibitAnyPolicyExtension.get(InhibitAnyPolicyExtension.SKIP_CERTS).intValue();
            if (debug != null) {
                debug.println("PolicyChecker.mergeInhibitAnyPolicy() skipCerts Index from cert = " + iIntValue);
            }
            if (iIntValue != -1 && iIntValue < i) {
                return iIntValue;
            }
            return i;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("PolicyChecker.mergeInhibitAnyPolicy unexpected exception");
                e.printStackTrace();
            }
            throw new CertPathValidatorException(e);
        }
    }

    static PolicyNodeImpl processPolicies(int i, Set<String> set, int i2, int i3, int i4, boolean z, PolicyNodeImpl policyNodeImpl, X509CertImpl x509CertImpl, boolean z2) throws CertPathValidatorException {
        PolicyNodeImpl policyNodeImplCopyTree;
        Set<PolicyQualifierInfo> set2;
        boolean z3;
        PolicyNodeImpl policyNodeImplRemoveInvalidNodes;
        int iMergeExplicitPolicy;
        HashSet hashSet = new HashSet();
        PolicyNodeImpl policyNodeImpl2 = null;
        if (policyNodeImpl != null) {
            policyNodeImplCopyTree = policyNodeImpl.copyTree();
        } else {
            policyNodeImplCopyTree = null;
        }
        CertificatePoliciesExtension certificatePoliciesExtension = x509CertImpl.getCertificatePoliciesExtension();
        if (certificatePoliciesExtension != null && policyNodeImplCopyTree != null) {
            boolean zIsCritical = certificatePoliciesExtension.isCritical();
            if (debug != null) {
                debug.println("PolicyChecker.processPolicies() policiesCritical = " + zIsCritical);
            }
            try {
                List<PolicyInformation> list = certificatePoliciesExtension.get(CertificatePoliciesExtension.POLICIES);
                if (debug != null) {
                    debug.println("PolicyChecker.processPolicies() rejectPolicyQualifiers = " + z);
                }
                Set<PolicyQualifierInfo> policyQualifiers = hashSet;
                boolean z4 = false;
                for (PolicyInformation policyInformation : list) {
                    String string = policyInformation.getPolicyIdentifier().getIdentifier().toString();
                    if (string.equals(ANY_POLICY)) {
                        policyQualifiers = policyInformation.getPolicyQualifiers();
                        z4 = true;
                    } else {
                        if (debug != null) {
                            debug.println("PolicyChecker.processPolicies() processing policy: " + string);
                        }
                        Set<PolicyQualifierInfo> policyQualifiers2 = policyInformation.getPolicyQualifiers();
                        if (!policyQualifiers2.isEmpty() && z && zIsCritical) {
                            throw new CertPathValidatorException("critical policy qualifiers present in certificate", null, null, -1, PKIXReason.INVALID_POLICY);
                        }
                        if (!processParents(i, zIsCritical, z, policyNodeImplCopyTree, string, policyQualifiers2, false)) {
                            processParents(i, zIsCritical, z, policyNodeImplCopyTree, string, policyQualifiers2, true);
                        }
                    }
                }
                if (z4 && (i4 > 0 || (!z2 && X509CertImpl.isSelfIssued(x509CertImpl)))) {
                    if (debug != null) {
                        debug.println("PolicyChecker.processPolicies() processing policy: 2.5.29.32.0");
                    }
                    processParents(i, zIsCritical, z, policyNodeImplCopyTree, ANY_POLICY, policyQualifiers, true);
                }
                policyNodeImplCopyTree.prune(i);
                if (policyNodeImplCopyTree.getChildren().hasNext()) {
                    policyNodeImpl2 = policyNodeImplCopyTree;
                }
                policyNodeImplRemoveInvalidNodes = policyNodeImpl2;
                z3 = zIsCritical;
                set2 = policyQualifiers;
            } catch (IOException e) {
                throw new CertPathValidatorException("Exception while retrieving policyOIDs", e);
            }
        } else if (certificatePoliciesExtension != null) {
            set2 = hashSet;
            z3 = false;
            policyNodeImplRemoveInvalidNodes = policyNodeImplCopyTree;
        } else {
            if (debug != null) {
                debug.println("PolicyChecker.processPolicies() no policies present in cert");
            }
            set2 = hashSet;
            z3 = false;
            policyNodeImplRemoveInvalidNodes = null;
        }
        if (policyNodeImplRemoveInvalidNodes != null && !z2) {
            policyNodeImplRemoveInvalidNodes = processPolicyMappings(x509CertImpl, i, i3, policyNodeImplRemoveInvalidNodes, z3, set2);
        }
        if (policyNodeImplRemoveInvalidNodes != null && !set.contains(ANY_POLICY) && certificatePoliciesExtension != null && (policyNodeImplRemoveInvalidNodes = removeInvalidNodes(policyNodeImplRemoveInvalidNodes, i, set, certificatePoliciesExtension)) != null && z2) {
            policyNodeImplRemoveInvalidNodes = rewriteLeafNodes(i, set, policyNodeImplRemoveInvalidNodes);
        }
        if (z2) {
            iMergeExplicitPolicy = mergeExplicitPolicy(i2, x509CertImpl, z2);
        } else {
            iMergeExplicitPolicy = i2;
        }
        if (iMergeExplicitPolicy == 0 && policyNodeImplRemoveInvalidNodes == null) {
            throw new CertPathValidatorException("non-null policy tree required and policy tree is null", null, null, -1, PKIXReason.INVALID_POLICY);
        }
        return policyNodeImplRemoveInvalidNodes;
    }

    private static PolicyNodeImpl rewriteLeafNodes(int i, Set<String> set, PolicyNodeImpl policyNodeImpl) {
        Set<PolicyNodeImpl> policyNodesValid = policyNodeImpl.getPolicyNodesValid(i, ANY_POLICY);
        if (policyNodesValid.isEmpty()) {
            return policyNodeImpl;
        }
        PolicyNodeImpl next = policyNodesValid.iterator().next();
        PolicyNodeImpl policyNodeImpl2 = (PolicyNodeImpl) next.getParent();
        policyNodeImpl2.deleteChild(next);
        HashSet<String> hashSet = new HashSet(set);
        Iterator<PolicyNodeImpl> it = policyNodeImpl.getPolicyNodes(i).iterator();
        while (it.hasNext()) {
            hashSet.remove(it.next().getValidPolicy());
        }
        if (hashSet.isEmpty()) {
            policyNodeImpl.prune(i);
            if (!policyNodeImpl.getChildren().hasNext()) {
                return null;
            }
            return policyNodeImpl;
        }
        boolean zIsCritical = next.isCritical();
        Set<PolicyQualifierInfo> policyQualifiers = next.getPolicyQualifiers();
        for (String str : hashSet) {
            new PolicyNodeImpl(policyNodeImpl2, str, policyQualifiers, zIsCritical, Collections.singleton(str), false);
        }
        return policyNodeImpl;
    }

    private static boolean processParents(int i, boolean z, boolean z2, PolicyNodeImpl policyNodeImpl, String str, Set<PolicyQualifierInfo> set, boolean z3) throws CertPathValidatorException {
        if (debug != null) {
            debug.println("PolicyChecker.processParents(): matchAny = " + z3);
        }
        boolean z4 = false;
        for (PolicyNodeImpl policyNodeImpl2 : policyNodeImpl.getPolicyNodesExpected(i - 1, str, z3)) {
            if (debug != null) {
                debug.println("PolicyChecker.processParents() found parent:\n" + policyNodeImpl2.asString());
            }
            policyNodeImpl2.getValidPolicy();
            if (str.equals(ANY_POLICY)) {
                for (String str2 : policyNodeImpl2.getExpectedPolicies()) {
                    Iterator<PolicyNodeImpl> children = policyNodeImpl2.getChildren();
                    while (true) {
                        if (children.hasNext()) {
                            String validPolicy = children.next().getValidPolicy();
                            if (str2.equals(validPolicy)) {
                                if (debug != null) {
                                    debug.println(validPolicy + " in parent's expected policy set already appears in child node");
                                }
                            }
                        } else {
                            HashSet hashSet = new HashSet();
                            hashSet.add(str2);
                            new PolicyNodeImpl(policyNodeImpl2, str2, set, z, hashSet, false);
                            break;
                        }
                    }
                }
            } else {
                HashSet hashSet2 = new HashSet();
                hashSet2.add(str);
                new PolicyNodeImpl(policyNodeImpl2, str, set, z, hashSet2, false);
            }
            z4 = true;
        }
        return z4;
    }

    private static PolicyNodeImpl processPolicyMappings(X509CertImpl x509CertImpl, int i, int i2, PolicyNodeImpl policyNodeImpl, boolean z, Set<PolicyQualifierInfo> set) throws CertPathValidatorException {
        PolicyMappingsExtension policyMappingsExtension = x509CertImpl.getPolicyMappingsExtension();
        if (policyMappingsExtension == null) {
            return policyNodeImpl;
        }
        if (debug != null) {
            debug.println("PolicyChecker.processPolicyMappings() inside policyMapping check");
        }
        try {
            boolean z2 = false;
            for (CertificatePolicyMap certificatePolicyMap : policyMappingsExtension.get(PolicyMappingsExtension.MAP)) {
                String string = certificatePolicyMap.getIssuerIdentifier().getIdentifier().toString();
                String string2 = certificatePolicyMap.getSubjectIdentifier().getIdentifier().toString();
                if (debug != null) {
                    debug.println("PolicyChecker.processPolicyMappings() issuerDomain = " + string);
                    debug.println("PolicyChecker.processPolicyMappings() subjectDomain = " + string2);
                }
                if (string.equals(ANY_POLICY)) {
                    throw new CertPathValidatorException("encountered an issuerDomainPolicy of ANY_POLICY", null, null, -1, PKIXReason.INVALID_POLICY);
                }
                if (string2.equals(ANY_POLICY)) {
                    throw new CertPathValidatorException("encountered a subjectDomainPolicy of ANY_POLICY", null, null, -1, PKIXReason.INVALID_POLICY);
                }
                Set<PolicyNodeImpl> policyNodesValid = policyNodeImpl.getPolicyNodesValid(i, string);
                if (!policyNodesValid.isEmpty()) {
                    for (PolicyNodeImpl policyNodeImpl2 : policyNodesValid) {
                        if (i2 > 0 || i2 == -1) {
                            policyNodeImpl2.addExpectedPolicy(string2);
                        } else if (i2 == 0) {
                            PolicyNodeImpl policyNodeImpl3 = (PolicyNodeImpl) policyNodeImpl2.getParent();
                            if (debug != null) {
                                debug.println("PolicyChecker.processPolicyMappings() before deleting: policy tree = " + ((Object) policyNodeImpl));
                            }
                            policyNodeImpl3.deleteChild(policyNodeImpl2);
                            z2 = true;
                            if (debug != null) {
                                debug.println("PolicyChecker.processPolicyMappings() after deleting: policy tree = " + ((Object) policyNodeImpl));
                            }
                        }
                    }
                } else if (i2 > 0 || i2 == -1) {
                    Iterator<PolicyNodeImpl> it = policyNodeImpl.getPolicyNodesValid(i, ANY_POLICY).iterator();
                    while (it.hasNext()) {
                        PolicyNodeImpl policyNodeImpl4 = (PolicyNodeImpl) it.next().getParent();
                        HashSet hashSet = new HashSet();
                        hashSet.add(string2);
                        new PolicyNodeImpl(policyNodeImpl4, string, set, z, hashSet, true);
                    }
                }
            }
            if (z2) {
                policyNodeImpl.prune(i);
                if (!policyNodeImpl.getChildren().hasNext()) {
                    if (debug != null) {
                        debug.println("setting rootNode to null");
                    }
                    return null;
                }
            }
            return policyNodeImpl;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("PolicyChecker.processPolicyMappings() mapping exception");
                e.printStackTrace();
            }
            throw new CertPathValidatorException("Exception while checking mapping", e);
        }
    }

    private static PolicyNodeImpl removeInvalidNodes(PolicyNodeImpl policyNodeImpl, int i, Set<String> set, CertificatePoliciesExtension certificatePoliciesExtension) throws CertPathValidatorException {
        try {
            boolean z = false;
            Iterator<PolicyInformation> it = certificatePoliciesExtension.get(CertificatePoliciesExtension.POLICIES).iterator();
            while (it.hasNext()) {
                String string = it.next().getPolicyIdentifier().getIdentifier().toString();
                if (debug != null) {
                    debug.println("PolicyChecker.processPolicies() processing policy second time: " + string);
                }
                for (PolicyNodeImpl policyNodeImpl2 : policyNodeImpl.getPolicyNodesValid(i, string)) {
                    PolicyNodeImpl policyNodeImpl3 = (PolicyNodeImpl) policyNodeImpl2.getParent();
                    if (policyNodeImpl3.getValidPolicy().equals(ANY_POLICY) && !set.contains(string) && !string.equals(ANY_POLICY)) {
                        if (debug != null) {
                            debug.println("PolicyChecker.processPolicies() before deleting: policy tree = " + ((Object) policyNodeImpl));
                        }
                        policyNodeImpl3.deleteChild(policyNodeImpl2);
                        z = true;
                        if (debug != null) {
                            debug.println("PolicyChecker.processPolicies() after deleting: policy tree = " + ((Object) policyNodeImpl));
                        }
                    }
                }
            }
            if (z) {
                policyNodeImpl.prune(i);
                if (!policyNodeImpl.getChildren().hasNext()) {
                    return null;
                }
                return policyNodeImpl;
            }
            return policyNodeImpl;
        } catch (IOException e) {
            throw new CertPathValidatorException("Exception while retrieving policyOIDs", e);
        }
    }

    PolicyNode getPolicyTree() {
        if (this.rootNode == null) {
            return null;
        }
        PolicyNodeImpl policyNodeImplCopyTree = this.rootNode.copyTree();
        policyNodeImplCopyTree.setImmutable();
        return policyNodeImplCopyTree;
    }
}
