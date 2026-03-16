package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class Maps {
    static final Joiner.MapJoiner STANDARD_JOINER = Collections2.STANDARD_JOINER.withKeyValueSeparator("=");

    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(int expectedSize) {
        return new HashMap<>(capacity(expectedSize));
    }

    static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            Preconditions.checkArgument(expectedSize >= 0);
            return expectedSize + 1;
        }
        if (expectedSize < 1073741824) {
            return (expectedSize / 3) + expectedSize;
        }
        return Integer.MAX_VALUE;
    }

    public static <K, V> Map.Entry<K, V> immutableEntry(K key, V value) {
        return new ImmutableEntry(key, value);
    }

    static <V> V safeGet(Map<?, V> map, Object key) {
        try {
            return map.get(key);
        } catch (ClassCastException e) {
            return null;
        }
    }

    static boolean safeContainsKey(Map<?, ?> map, Object key) {
        try {
            return map.containsKey(key);
        } catch (ClassCastException e) {
            return false;
        }
    }

    static String toStringImpl(Map<?, ?> map) {
        StringBuilder sb = Collections2.newStringBuilderForCollection(map.size()).append('{');
        STANDARD_JOINER.appendTo(sb, map);
        return sb.append('}').toString();
    }

    static abstract class KeySet<K, V> extends AbstractSet<K> {
        abstract Map<K, V> map();

        KeySet() {
        }

        @Override
        public Iterator<K> iterator() {
            return Iterators.transform(map().entrySet().iterator(), new Function<Map.Entry<K, V>, K>() {
                @Override
                public K apply(Map.Entry<K, V> entry) {
                    return entry.getKey();
                }
            });
        }

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return map().containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            if (!contains(o)) {
                return false;
            }
            map().remove(o);
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return super.removeAll((Collection) Preconditions.checkNotNull(c));
        }

        @Override
        public void clear() {
            map().clear();
        }
    }

    static abstract class EntrySet<K, V> extends AbstractSet<Map.Entry<K, V>> {
        abstract Map<K, V> map();

        EntrySet() {
        }

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public void clear() {
            map().clear();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry) o;
            Object key = entry.getKey();
            V value = map().get(key);
            if (Objects.equal(value, entry.getValue())) {
                return value != null || map().containsKey(key);
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean remove(Object o) {
            if (!contains(o)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry) o;
            return map().keySet().remove(entry.getKey());
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            try {
                return super.removeAll((Collection) Preconditions.checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                boolean changed = true;
                for (Object o : c) {
                    changed |= remove(o);
                }
                return changed;
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            try {
                return super.retainAll((Collection) Preconditions.checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                Set<Object> keys = Sets.newHashSetWithExpectedSize(c.size());
                for (Object o : c) {
                    if (contains(o)) {
                        Map.Entry<?, ?> entry = (Map.Entry) o;
                        keys.add(entry.getKey());
                    }
                }
                return map().keySet().retainAll(keys);
            }
        }
    }
}
