package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PKIXParameters implements CertPathParameters {
    private List<PKIXCertPathChecker> certPathCheckers;
    private List<CertStore> certStores;
    private Date date;
    private Set<String> initialPolicies;
    private String sigProvider;
    private CertSelector targetCertConstraints;
    private Set<TrustAnchor> trustAnchors;
    private boolean revocationEnabled = true;
    private boolean explicitPolicyRequired = false;
    private boolean policyMappingInhibited = false;
    private boolean anyPolicyInhibited = false;
    private boolean policyQualifiersRejected = true;

    public PKIXParameters(Set<TrustAnchor> trustAnchors) throws InvalidAlgorithmParameterException {
        if (trustAnchors == null) {
            throw new NullPointerException("trustAnchors == null");
        }
        checkTrustAnchors(trustAnchors);
        this.trustAnchors = new HashSet(trustAnchors);
    }

    public PKIXParameters(KeyStore keyStore) throws KeyStoreException, InvalidAlgorithmParameterException {
        if (keyStore == null) {
            throw new NullPointerException("keyStore == null");
        }
        if (keyStore.size() == 0) {
            throw new InvalidAlgorithmParameterException("keyStore.size() == 0");
        }
        this.trustAnchors = new HashSet();
        Enumeration<String> enumerationAliases = keyStore.aliases();
        while (enumerationAliases.hasMoreElements()) {
            String alias = enumerationAliases.nextElement();
            if (keyStore.isCertificateEntry(alias)) {
                Certificate c = keyStore.getCertificate(alias);
                if (c instanceof X509Certificate) {
                    this.trustAnchors.add(new TrustAnchor((X509Certificate) c, null));
                }
            }
        }
        checkTrustAnchors(this.trustAnchors);
    }

    public Set<TrustAnchor> getTrustAnchors() {
        return Collections.unmodifiableSet(this.trustAnchors);
    }

    public void setTrustAnchors(Set<TrustAnchor> trustAnchors) throws InvalidAlgorithmParameterException {
        if (trustAnchors == null) {
            throw new NullPointerException("trustAnchors == null");
        }
        checkTrustAnchors(trustAnchors);
        this.trustAnchors = new HashSet(trustAnchors);
    }

    public boolean isAnyPolicyInhibited() {
        return this.anyPolicyInhibited;
    }

    public void setAnyPolicyInhibited(boolean anyPolicyInhibited) {
        this.anyPolicyInhibited = anyPolicyInhibited;
    }

    public List<PKIXCertPathChecker> getCertPathCheckers() {
        if (this.certPathCheckers == null) {
            this.certPathCheckers = new ArrayList();
        }
        if (this.certPathCheckers.isEmpty()) {
            return Collections.unmodifiableList(this.certPathCheckers);
        }
        ArrayList<PKIXCertPathChecker> modifiableList = new ArrayList<>();
        for (PKIXCertPathChecker certPathChecker : this.certPathCheckers) {
            modifiableList.add((PKIXCertPathChecker) certPathChecker.clone());
        }
        return Collections.unmodifiableList(modifiableList);
    }

    public void setCertPathCheckers(List<PKIXCertPathChecker> certPathCheckers) {
        if (certPathCheckers == null || certPathCheckers.isEmpty()) {
            if (this.certPathCheckers != null && !this.certPathCheckers.isEmpty()) {
                this.certPathCheckers = null;
                return;
            }
            return;
        }
        this.certPathCheckers = new ArrayList();
        for (PKIXCertPathChecker certPathChecker : certPathCheckers) {
            this.certPathCheckers.add((PKIXCertPathChecker) certPathChecker.clone());
        }
    }

    public void addCertPathChecker(PKIXCertPathChecker checker) {
        if (checker != null) {
            if (this.certPathCheckers == null) {
                this.certPathCheckers = new ArrayList();
            }
            this.certPathCheckers.add((PKIXCertPathChecker) checker.clone());
        }
    }

    public List<CertStore> getCertStores() {
        if (this.certStores == null) {
            this.certStores = new ArrayList();
        }
        if (this.certStores.isEmpty()) {
            return Collections.unmodifiableList(this.certStores);
        }
        ArrayList<CertStore> modifiableList = new ArrayList<>(this.certStores);
        return Collections.unmodifiableList(modifiableList);
    }

    public void setCertStores(List<CertStore> certStores) {
        if (certStores == null || certStores.isEmpty()) {
            if (this.certStores != null && !this.certStores.isEmpty()) {
                this.certStores = null;
                return;
            }
            return;
        }
        this.certStores = new ArrayList(certStores);
    }

    public void addCertStore(CertStore store) {
        if (store != null) {
            if (this.certStores == null) {
                this.certStores = new ArrayList();
            }
            this.certStores.add(store);
        }
    }

    public Date getDate() {
        if (this.date == null) {
            return null;
        }
        return (Date) this.date.clone();
    }

    public void setDate(Date date) {
        this.date = date == null ? null : new Date(date.getTime());
    }

    public boolean isExplicitPolicyRequired() {
        return this.explicitPolicyRequired;
    }

    public void setExplicitPolicyRequired(boolean explicitPolicyRequired) {
        this.explicitPolicyRequired = explicitPolicyRequired;
    }

    public Set<String> getInitialPolicies() {
        if (this.initialPolicies == null) {
            this.initialPolicies = new HashSet();
        }
        if (this.initialPolicies.isEmpty()) {
            return Collections.unmodifiableSet(this.initialPolicies);
        }
        HashSet<String> modifiableSet = new HashSet<>(this.initialPolicies);
        return Collections.unmodifiableSet(modifiableSet);
    }

    public void setInitialPolicies(Set<String> initialPolicies) {
        if (initialPolicies == null || initialPolicies.isEmpty()) {
            if (this.initialPolicies != null && !this.initialPolicies.isEmpty()) {
                this.initialPolicies = null;
                return;
            }
            return;
        }
        this.initialPolicies = new HashSet(initialPolicies);
    }

    public boolean isPolicyMappingInhibited() {
        return this.policyMappingInhibited;
    }

    public void setPolicyMappingInhibited(boolean policyMappingInhibited) {
        this.policyMappingInhibited = policyMappingInhibited;
    }

    public boolean getPolicyQualifiersRejected() {
        return this.policyQualifiersRejected;
    }

    public void setPolicyQualifiersRejected(boolean policyQualifiersRejected) {
        this.policyQualifiersRejected = policyQualifiersRejected;
    }

    public boolean isRevocationEnabled() {
        return this.revocationEnabled;
    }

    public void setRevocationEnabled(boolean revocationEnabled) {
        this.revocationEnabled = revocationEnabled;
    }

    public String getSigProvider() {
        return this.sigProvider;
    }

    public void setSigProvider(String sigProvider) {
        this.sigProvider = sigProvider;
    }

    public CertSelector getTargetCertConstraints() {
        if (this.targetCertConstraints == null) {
            return null;
        }
        return (CertSelector) this.targetCertConstraints.clone();
    }

    public void setTargetCertConstraints(CertSelector targetCertConstraints) {
        this.targetCertConstraints = targetCertConstraints == null ? null : (CertSelector) targetCertConstraints.clone();
    }

    @Override
    public Object clone() {
        try {
            PKIXParameters ret = (PKIXParameters) super.clone();
            if (this.certStores != null) {
                ret.certStores = new ArrayList(this.certStores);
            }
            if (this.certPathCheckers != null) {
                ret.certPathCheckers = new ArrayList(this.certPathCheckers);
            }
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[\n Trust Anchors: ");
        sb.append(this.trustAnchors);
        sb.append("\n Revocation Enabled: ");
        sb.append(this.revocationEnabled);
        sb.append("\n Explicit Policy Required: ");
        sb.append(this.explicitPolicyRequired);
        sb.append("\n Policy Mapping Inhibited: ");
        sb.append(this.policyMappingInhibited);
        sb.append("\n Any Policy Inhibited: ");
        sb.append(this.anyPolicyInhibited);
        sb.append("\n Policy Qualifiers Rejected: ");
        sb.append(this.policyQualifiersRejected);
        sb.append("\n Initial Policy OIDs: ");
        sb.append((this.initialPolicies == null || this.initialPolicies.isEmpty()) ? "any" : this.initialPolicies.toString());
        sb.append("\n Cert Stores: ");
        sb.append((this.certStores == null || this.certStores.isEmpty()) ? "no" : this.certStores.toString());
        sb.append("\n Validity Date: ");
        sb.append(this.date);
        sb.append("\n Cert Path Checkers: ");
        sb.append((this.certPathCheckers == null || this.certPathCheckers.isEmpty()) ? "no" : this.certPathCheckers.toString());
        sb.append("\n Signature Provider: ");
        sb.append(this.sigProvider);
        sb.append("\n Target Certificate Constraints: ");
        sb.append(this.targetCertConstraints);
        sb.append("\n]");
        return sb.toString();
    }

    private void checkTrustAnchors(Set<TrustAnchor> trustAnchors) throws InvalidAlgorithmParameterException {
        if (trustAnchors.isEmpty()) {
            throw new InvalidAlgorithmParameterException("trustAnchors.isEmpty()");
        }
    }
}
