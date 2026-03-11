package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import java.util.Map;
import javax.annotation.Nullable;

@GwtCompatible
abstract class AbstractMapEntry<K, V> implements Map.Entry<K, V> {
    @Override
    public abstract K getKey();

    @Override
    public abstract V getValue();

    AbstractMapEntry() {
    }

    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (!(object instanceof Map.Entry)) {
            return false;
        }
        Map.Entry<?, ?> that = (Map.Entry) object;
        if (Objects.equal(getKey(), that.getKey())) {
            return Objects.equal(getValue(), that.getValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        K k = getKey();
        V v = getValue();
        return (v != null ? v.hashCode() : 0) ^ (k == null ? 0 : k.hashCode());
    }

    public String toString() {
        return getKey() + "=" + getValue();
    }
}
