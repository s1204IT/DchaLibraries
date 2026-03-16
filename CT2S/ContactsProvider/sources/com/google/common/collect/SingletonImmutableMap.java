package com.google.common.collect;

import java.util.Map;

final class SingletonImmutableMap<K, V> extends ImmutableMap<K, V> {
    private transient Map.Entry<K, V> entry;
    private transient ImmutableSet<Map.Entry<K, V>> entrySet;
    private transient ImmutableSet<K> keySet;
    final transient K singleKey;
    final transient V singleValue;
    private transient ImmutableCollection<V> values;

    SingletonImmutableMap(Map.Entry<K, V> entry) {
        this.entry = entry;
        this.singleKey = entry.getKey();
        this.singleValue = entry.getValue();
    }

    private Map.Entry<K, V> entry() {
        Map.Entry<K, V> e = this.entry;
        if (e != null) {
            return e;
        }
        Map.Entry<K, V> e2 = Maps.immutableEntry(this.singleKey, this.singleValue);
        this.entry = e2;
        return e2;
    }

    @Override
    public V get(Object key) {
        if (this.singleKey.equals(key)) {
            return this.singleValue;
        }
        return null;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return this.singleKey.equals(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.singleValue.equals(value);
    }

    @Override
    public ImmutableSet<Map.Entry<K, V>> entrySet() {
        ImmutableSet<Map.Entry<K, V>> es = this.entrySet;
        if (es != null) {
            return es;
        }
        ImmutableSet<Map.Entry<K, V>> es2 = ImmutableSet.of(entry());
        this.entrySet = es2;
        return es2;
    }

    @Override
    public ImmutableSet<K> keySet() {
        ImmutableSet<K> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        ImmutableSet<K> ks2 = ImmutableSet.of(this.singleKey);
        this.keySet = ks2;
        return ks2;
    }

    @Override
    public ImmutableCollection<V> values() {
        ImmutableCollection<V> v = this.values;
        if (v != null) {
            return v;
        }
        Values values = new Values(this.singleValue);
        this.values = values;
        return values;
    }

    private static class Values<V> extends ImmutableCollection<V> {
        final V singleValue;

        Values(V singleValue) {
            this.singleValue = singleValue;
        }

        @Override
        public boolean contains(Object object) {
            return this.singleValue.equals(object);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public UnmodifiableIterator<V> iterator() {
            return Iterators.singletonIterator(this.singleValue);
        }

        @Override
        boolean isPartialView() {
            return true;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Map)) {
            return false;
        }
        Map<?, ?> that = (Map) object;
        if (that.size() != 1) {
            return false;
        }
        Map.Entry<K, V> next = that.entrySet().iterator().next();
        return this.singleKey.equals(next.getKey()) && this.singleValue.equals(next.getValue());
    }

    @Override
    public int hashCode() {
        return this.singleKey.hashCode() ^ this.singleValue.hashCode();
    }

    @Override
    public String toString() {
        return '{' + this.singleKey.toString() + '=' + this.singleValue.toString() + '}';
    }
}
