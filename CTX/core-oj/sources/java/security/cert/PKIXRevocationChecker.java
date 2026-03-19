package java.security.cert;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class PKIXRevocationChecker extends PKIXCertPathChecker {
    private URI ocspResponder;
    private X509Certificate ocspResponderCert;
    private List<Extension> ocspExtensions = Collections.emptyList();
    private Map<X509Certificate, byte[]> ocspResponses = Collections.emptyMap();
    private Set<Option> options = Collections.emptySet();

    public enum Option {
        ONLY_END_ENTITY,
        PREFER_CRLS,
        NO_FALLBACK,
        SOFT_FAIL
    }

    public abstract List<CertPathValidatorException> getSoftFailExceptions();

    protected PKIXRevocationChecker() {
    }

    public void setOcspResponder(URI uri) {
        this.ocspResponder = uri;
    }

    public URI getOcspResponder() {
        return this.ocspResponder;
    }

    public void setOcspResponderCert(X509Certificate x509Certificate) {
        this.ocspResponderCert = x509Certificate;
    }

    public X509Certificate getOcspResponderCert() {
        return this.ocspResponderCert;
    }

    public void setOcspExtensions(List<Extension> list) {
        List<Extension> arrayList;
        if (list == null) {
            arrayList = Collections.emptyList();
        } else {
            arrayList = new ArrayList(list);
        }
        this.ocspExtensions = arrayList;
    }

    public List<Extension> getOcspExtensions() {
        return Collections.unmodifiableList(this.ocspExtensions);
    }

    public void setOcspResponses(Map<X509Certificate, byte[]> map) {
        if (map == null) {
            this.ocspResponses = Collections.emptyMap();
            return;
        }
        HashMap map2 = new HashMap(map.size());
        for (Map.Entry<X509Certificate, byte[]> entry : map.entrySet()) {
            map2.put(entry.getKey(), (byte[]) entry.getValue().clone());
        }
        this.ocspResponses = map2;
    }

    public Map<X509Certificate, byte[]> getOcspResponses() {
        HashMap map = new HashMap(this.ocspResponses.size());
        for (Map.Entry<X509Certificate, byte[]> entry : this.ocspResponses.entrySet()) {
            map.put(entry.getKey(), (byte[]) entry.getValue().clone());
        }
        return map;
    }

    public void setOptions(Set<Option> set) {
        Set<Option> hashSet;
        if (set == null) {
            hashSet = Collections.emptySet();
        } else {
            hashSet = new HashSet(set);
        }
        this.options = hashSet;
    }

    public Set<Option> getOptions() {
        return Collections.unmodifiableSet(this.options);
    }

    @Override
    public PKIXRevocationChecker clone() {
        PKIXRevocationChecker pKIXRevocationChecker = (PKIXRevocationChecker) super.clone();
        pKIXRevocationChecker.ocspExtensions = new ArrayList(this.ocspExtensions);
        pKIXRevocationChecker.ocspResponses = new HashMap(this.ocspResponses);
        for (Map.Entry<X509Certificate, byte[]> entry : pKIXRevocationChecker.ocspResponses.entrySet()) {
            entry.setValue((byte[]) entry.getValue().clone());
        }
        pKIXRevocationChecker.options = new HashSet(this.options);
        return pKIXRevocationChecker;
    }
}
