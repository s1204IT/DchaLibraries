package javax.net.ssl;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class KeyStoreBuilderParameters implements ManagerFactoryParameters {
    private final List<KeyStore.Builder> parameters;

    public KeyStoreBuilderParameters(KeyStore.Builder builder) {
        this.parameters = Collections.singletonList((KeyStore.Builder) Objects.requireNonNull(builder));
    }

    public KeyStoreBuilderParameters(List<KeyStore.Builder> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.parameters = Collections.unmodifiableList(new ArrayList(list));
    }

    public List<KeyStore.Builder> getParameters() {
        return this.parameters;
    }
}
