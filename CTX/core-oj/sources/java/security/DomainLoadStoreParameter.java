package java.security;

import java.net.URI;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DomainLoadStoreParameter implements KeyStore.LoadStoreParameter {
    private final URI configuration;
    private final Map<String, KeyStore.ProtectionParameter> protectionParams;

    public DomainLoadStoreParameter(URI uri, Map<String, KeyStore.ProtectionParameter> map) {
        if (uri == null || map == null) {
            throw new NullPointerException("invalid null input");
        }
        this.configuration = uri;
        this.protectionParams = Collections.unmodifiableMap(new HashMap(map));
    }

    public URI getConfiguration() {
        return this.configuration;
    }

    public Map<String, KeyStore.ProtectionParameter> getProtectionParams() {
        return this.protectionParams;
    }

    @Override
    public KeyStore.ProtectionParameter getProtectionParameter() {
        return null;
    }
}
