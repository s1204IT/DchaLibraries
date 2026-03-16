package java.security.cert;

import java.util.Collection;
import java.util.Collections;

public class CollectionCertStoreParameters implements CertStoreParameters {
    private static final Collection<?> defaultCollection = Collections.EMPTY_SET;
    private final Collection<?> collection;

    public CollectionCertStoreParameters() {
        this.collection = defaultCollection;
    }

    public CollectionCertStoreParameters(Collection<?> collection) {
        if (collection == null) {
            throw new NullPointerException("collection == null");
        }
        this.collection = collection;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public Collection<?> getCollection() {
        return this.collection;
    }

    public String toString() {
        return "CollectionCertStoreParameters: [\ncollection: " + getCollection().toString() + "\n]";
    }
}
