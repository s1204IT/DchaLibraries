package com.google.common.collect;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class Multimaps {

    static abstract class Values<K, V> extends AbstractCollection<V> {
        abstract Multimap<K, V> multimap();

        Values() {
        }

        @Override
        public Iterator<V> iterator() {
            final Iterator<Map.Entry<K, V>> backingIterator = multimap().entries().iterator();
            return new Iterator<V>() {
                @Override
                public boolean hasNext() {
                    return backingIterator.hasNext();
                }

                @Override
                public V next() {
                    return (V) ((Map.Entry) backingIterator.next()).getValue();
                }

                @Override
                public void remove() {
                    backingIterator.remove();
                }
            };
        }

        @Override
        public int size() {
            return multimap().size();
        }

        @Override
        public boolean contains(Object o) {
            return multimap().containsValue(o);
        }

        @Override
        public void clear() {
            multimap().clear();
        }
    }

    static abstract class Entries<K, V> extends AbstractCollection<Map.Entry<K, V>> {
        abstract Multimap<K, V> multimap();

        Entries() {
        }

        @Override
        public int size() {
            return multimap().size();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry) o;
            return multimap().containsEntry(entry.getKey(), entry.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry) o;
            return multimap().remove(entry.getKey(), entry.getValue());
        }

        @Override
        public void clear() {
            multimap().clear();
        }
    }

    static abstract class EntrySet<K, V> extends Entries<K, V> implements Set<Map.Entry<K, V>> {
        EntrySet() {
        }

        @Override
        public int hashCode() {
            return Sets.hashCodeImpl(this);
        }

        @Override
        public boolean equals(Object obj) {
            return Sets.equalsImpl(this, obj);
        }
    }
}
