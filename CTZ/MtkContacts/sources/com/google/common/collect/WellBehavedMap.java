package com.google.common.collect;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class WellBehavedMap<K, V> extends ForwardingMap<K, V> {
    private final Map<K, V> delegate;
    private Set<Map.Entry<K, V>> entrySet;

    private WellBehavedMap(Map<K, V> map) {
        this.delegate = map;
    }

    static <K, V> WellBehavedMap<K, V> wrap(Map<K, V> map) {
        return new WellBehavedMap<>(map);
    }

    @Override
    protected Map<K, V> delegate() {
        return this.delegate;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> set = this.entrySet;
        if (set != null) {
            return set;
        }
        EntrySet entrySet = new EntrySet();
        this.entrySet = entrySet;
        return entrySet;
    }

    private final class EntrySet extends Maps.EntrySet<K, V> {
        private EntrySet() {
        }

        @Override
        Map<K, V> map() {
            return WellBehavedMap.this;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new TransformedIterator<K, Map.Entry<K, V>>(WellBehavedMap.this.keySet().iterator()) {
                @Override
                Map.Entry<K, V> transform(final K k) {
                    return new AbstractMapEntry<K, V>() {
                        @Override
                        public K getKey() {
                            return (K) k;
                        }

                        @Override
                        public V getValue() {
                            return WellBehavedMap.this.get(k);
                        }

                        @Override
                        public V setValue(V v) {
                            return (V) WellBehavedMap.this.put(k, v);
                        }
                    };
                }
            };
        }
    }
}
