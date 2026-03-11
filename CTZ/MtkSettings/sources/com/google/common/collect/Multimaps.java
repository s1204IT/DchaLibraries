package com.google.common.collect;

import java.util.AbstractCollection;
import java.util.Map;

public final class Multimaps {

    static abstract class Entries<K, V> extends AbstractCollection<Map.Entry<K, V>> {
        abstract Multimap<K, V> multimap();

        Entries() {
        }

        @Override
        public int size() {
            return multimap().size();
        }

        @Override
        public boolean contains(Object obj) {
            if (obj instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) obj;
                return multimap().containsEntry(entry.getKey(), entry.getValue());
            }
            return false;
        }

        @Override
        public boolean remove(Object obj) {
            if (obj instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) obj;
                return multimap().remove(entry.getKey(), entry.getValue());
            }
            return false;
        }

        @Override
        public void clear() {
            multimap().clear();
        }
    }

    static boolean equalsImpl(Multimap<?, ?> multimap, Object obj) {
        if (obj == multimap) {
            return true;
        }
        if (obj instanceof Multimap) {
            return multimap.asMap().equals(((Multimap) obj).asMap());
        }
        return false;
    }
}
