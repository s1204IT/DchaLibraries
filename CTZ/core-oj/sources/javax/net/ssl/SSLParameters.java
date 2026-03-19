package javax.net.ssl;

import java.security.AlgorithmConstraints;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SSLParameters {
    private AlgorithmConstraints algorithmConstraints;
    private String[] cipherSuites;
    private String identificationAlgorithm;
    private boolean needClientAuth;
    private boolean preferLocalCipherSuites;
    private String[] protocols;
    private boolean wantClientAuth;
    private Map<Integer, SNIServerName> sniNames = null;
    private Map<Integer, SNIMatcher> sniMatchers = null;

    public SSLParameters() {
    }

    public SSLParameters(String[] strArr) {
        setCipherSuites(strArr);
    }

    public SSLParameters(String[] strArr, String[] strArr2) {
        setCipherSuites(strArr);
        setProtocols(strArr2);
    }

    private static String[] clone(String[] strArr) {
        if (strArr == null) {
            return null;
        }
        return (String[]) strArr.clone();
    }

    public String[] getCipherSuites() {
        return clone(this.cipherSuites);
    }

    public void setCipherSuites(String[] strArr) {
        this.cipherSuites = clone(strArr);
    }

    public String[] getProtocols() {
        return clone(this.protocols);
    }

    public void setProtocols(String[] strArr) {
        this.protocols = clone(strArr);
    }

    public boolean getWantClientAuth() {
        return this.wantClientAuth;
    }

    public void setWantClientAuth(boolean z) {
        this.wantClientAuth = z;
        this.needClientAuth = false;
    }

    public boolean getNeedClientAuth() {
        return this.needClientAuth;
    }

    public void setNeedClientAuth(boolean z) {
        this.wantClientAuth = false;
        this.needClientAuth = z;
    }

    public AlgorithmConstraints getAlgorithmConstraints() {
        return this.algorithmConstraints;
    }

    public void setAlgorithmConstraints(AlgorithmConstraints algorithmConstraints) {
        this.algorithmConstraints = algorithmConstraints;
    }

    public String getEndpointIdentificationAlgorithm() {
        return this.identificationAlgorithm;
    }

    public void setEndpointIdentificationAlgorithm(String str) {
        this.identificationAlgorithm = str;
    }

    public final void setServerNames(List<SNIServerName> list) {
        if (list != null) {
            if (!list.isEmpty()) {
                this.sniNames = new LinkedHashMap(list.size());
                for (SNIServerName sNIServerName : list) {
                    if (this.sniNames.put(Integer.valueOf(sNIServerName.getType()), sNIServerName) != null) {
                        throw new IllegalArgumentException("Duplicated server name of type " + sNIServerName.getType());
                    }
                }
                return;
            }
            this.sniNames = Collections.emptyMap();
            return;
        }
        this.sniNames = null;
    }

    public final List<SNIServerName> getServerNames() {
        if (this.sniNames != null) {
            if (!this.sniNames.isEmpty()) {
                return Collections.unmodifiableList(new ArrayList(this.sniNames.values()));
            }
            return Collections.emptyList();
        }
        return null;
    }

    public final void setSNIMatchers(Collection<SNIMatcher> collection) {
        if (collection != null) {
            if (!collection.isEmpty()) {
                this.sniMatchers = new HashMap(collection.size());
                for (SNIMatcher sNIMatcher : collection) {
                    if (this.sniMatchers.put(Integer.valueOf(sNIMatcher.getType()), sNIMatcher) != null) {
                        throw new IllegalArgumentException("Duplicated server name of type " + sNIMatcher.getType());
                    }
                }
                return;
            }
            this.sniMatchers = Collections.emptyMap();
            return;
        }
        this.sniMatchers = null;
    }

    public final Collection<SNIMatcher> getSNIMatchers() {
        if (this.sniMatchers != null) {
            if (!this.sniMatchers.isEmpty()) {
                return Collections.unmodifiableList(new ArrayList(this.sniMatchers.values()));
            }
            return Collections.emptyList();
        }
        return null;
    }

    public final void setUseCipherSuitesOrder(boolean z) {
        this.preferLocalCipherSuites = z;
    }

    public final boolean getUseCipherSuitesOrder() {
        return this.preferLocalCipherSuites;
    }
}
