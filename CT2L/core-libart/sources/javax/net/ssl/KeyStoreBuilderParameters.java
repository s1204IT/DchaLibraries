package javax.net.ssl;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KeyStoreBuilderParameters implements ManagerFactoryParameters {
    private final List<KeyStore.Builder> ksbuilders;

    public KeyStoreBuilderParameters(KeyStore.Builder builder) {
        if (builder == null) {
            throw new NullPointerException("builder == null");
        }
        this.ksbuilders = Collections.singletonList(builder);
    }

    public KeyStoreBuilderParameters(List<KeyStore.Builder> parameters) {
        if (parameters == null) {
            throw new NullPointerException("parameters == null");
        }
        if (parameters.isEmpty()) {
            throw new IllegalArgumentException("parameters.isEmpty()");
        }
        this.ksbuilders = Collections.unmodifiableList(new ArrayList(parameters));
    }

    public List<KeyStore.Builder> getParameters() {
        return this.ksbuilders;
    }
}
