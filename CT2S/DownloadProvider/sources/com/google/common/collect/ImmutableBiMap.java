package com.google.common.collect;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class ImmutableBiMap<K, V> extends ImmutableMap<K, V> implements BiMap<K, V> {
    private static final ImmutableBiMap<Object, Object> EMPTY_IMMUTABLE_BIMAP = new EmptyBiMap();

    abstract ImmutableMap<K, V> delegate();

    public abstract ImmutableBiMap<V, K> inverse();

    public static <K, V> ImmutableBiMap<K, V> of() {
        return (ImmutableBiMap<K, V>) EMPTY_IMMUTABLE_BIMAP;
    }

    public static final class Builder<K, V> extends ImmutableMap.Builder<K, V> {
        @Override
        public Builder<K, V> put(K key, V value) {
            super.put((Object) key, (Object) value);
            return this;
        }

        @Override
        public ImmutableBiMap<K, V> build() {
            ImmutableMap<K, V> map = super.build();
            return map.isEmpty() ? ImmutableBiMap.of() : new RegularImmutableBiMap(map);
        }
    }

    ImmutableBiMap() {
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return inverse().containsKey(value);
    }

    @Override
    public ImmutableSet<Map.Entry<K, V>> entrySet() {
        return delegate().entrySet();
    }

    @Override
    public V get(Object key) {
        return delegate().get(key);
    }

    @Override
    public ImmutableSet<K> keySet() {
        return delegate().keySet();
    }

    @Override
    public ImmutableSet<V> values() {
        return inverse().keySet();
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public int size() {
        return delegate().size();
    }

    @Override
    public boolean equals(Object object) {
        return object == this || delegate().equals(object);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

    static class EmptyBiMap extends ImmutableBiMap<Object, Object> {
        @Override
        public Set entrySet() {
            return super.entrySet();
        }

        @Override
        public Set keySet() {
            return super.keySet();
        }

        @Override
        public ImmutableCollection values() {
            return super.values();
        }

        @Override
        public Collection values() {
            return super.values();
        }

        @Override
        public Set values() {
            return super.values();
        }

        EmptyBiMap() {
        }

        @Override
        ImmutableMap<Object, Object> delegate() {
            return ImmutableMap.of();
        }

        @Override
        public ImmutableBiMap<Object, Object> inverse() {
            return this;
        }

        Object readResolve() {
            return ImmutableBiMap.EMPTY_IMMUTABLE_BIMAP;
        }
    }

    private static class SerializedForm extends ImmutableMap.SerializedForm {
        private static final long serialVersionUID = 0;

        SerializedForm(ImmutableBiMap<?, ?> bimap) {
            super(bimap);
        }

        @Override
        Object readResolve() {
            Builder<Object, Object> builder = new Builder<>();
            return createMap(builder);
        }
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(this);
    }
}
