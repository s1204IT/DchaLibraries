package java.util;

import java.io.Serializable;
import java.util.Map;

public abstract class AbstractMap<K, V> implements Map<K, V> {
    transient Set<K> keySet;
    transient Collection<V> values;

    @Override
    public abstract Set<Map.Entry<K, V>> entrySet();

    protected AbstractMap() {
    }

    @Override
    public int size() {
        return entrySet().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsValue(Object obj) {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        if (obj == null) {
            while (it.hasNext()) {
                if (it.next().getValue() == null) {
                    return true;
                }
            }
            return false;
        }
        while (it.hasNext()) {
            if (obj.equals(it.next().getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsKey(Object obj) {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        if (obj == null) {
            while (it.hasNext()) {
                if (it.next().getKey() == null) {
                    return true;
                }
            }
            return false;
        }
        while (it.hasNext()) {
            if (obj.equals(it.next().getKey())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object obj) {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        if (obj == null) {
            while (it.hasNext()) {
                Map.Entry<K, V> next = it.next();
                if (next.getKey() == null) {
                    return next.getValue();
                }
            }
            return null;
        }
        while (it.hasNext()) {
            Map.Entry<K, V> next2 = it.next();
            if (obj.equals(next2.getKey())) {
                return next2.getValue();
            }
        }
        return null;
    }

    @Override
    public V put(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object obj) {
        Map.Entry<K, V> entry;
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        if (obj == null) {
            entry = null;
            while (entry == null && it.hasNext()) {
                Map.Entry<K, V> next = it.next();
                if (next.getKey() == null) {
                    entry = next;
                }
            }
        } else {
            Map.Entry<K, V> entry2 = null;
            while (entry2 == null && it.hasNext()) {
                Map.Entry<K, V> next2 = it.next();
                if (obj.equals(next2.getKey())) {
                    entry2 = next2;
                }
            }
            entry = entry2;
        }
        if (entry == null) {
            return null;
        }
        V value = entry.getValue();
        it.remove();
        return value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        entrySet().clear();
    }

    @Override
    public Set<K> keySet() {
        Set<K> set = this.keySet;
        if (set == null) {
            AbstractSet<K> abstractSet = new AbstractSet<K>() {
                @Override
                public Iterator<K> iterator() {
                    return new Iterator<K>() {
                        private Iterator<Map.Entry<K, V>> i;

                        {
                            this.i = AbstractMap.this.entrySet().iterator();
                        }

                        @Override
                        public boolean hasNext() {
                            return this.i.hasNext();
                        }

                        @Override
                        public K next() {
                            return this.i.next().getKey();
                        }

                        @Override
                        public void remove() {
                            this.i.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return AbstractMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return AbstractMap.this.isEmpty();
                }

                @Override
                public void clear() {
                    AbstractMap.this.clear();
                }

                @Override
                public boolean contains(Object obj) {
                    return AbstractMap.this.containsKey(obj);
                }
            };
            this.keySet = abstractSet;
            return abstractSet;
        }
        return set;
    }

    @Override
    public Collection<V> values() {
        Collection<V> collection = this.values;
        if (collection == null) {
            AbstractCollection<V> abstractCollection = new AbstractCollection<V>() {
                @Override
                public Iterator<V> iterator() {
                    return new Iterator<V>() {
                        private Iterator<Map.Entry<K, V>> i;

                        {
                            this.i = AbstractMap.this.entrySet().iterator();
                        }

                        @Override
                        public boolean hasNext() {
                            return this.i.hasNext();
                        }

                        @Override
                        public V next() {
                            return this.i.next().getValue();
                        }

                        @Override
                        public void remove() {
                            this.i.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return AbstractMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return AbstractMap.this.isEmpty();
                }

                @Override
                public void clear() {
                    AbstractMap.this.clear();
                }

                @Override
                public boolean contains(Object obj) {
                    return AbstractMap.this.containsValue(obj);
                }
            };
            this.values = abstractCollection;
            return abstractCollection;
        }
        return collection;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map)) {
            return false;
        }
        Map map = (Map) obj;
        if (map.size() != size()) {
            return false;
        }
        try {
            for (Map.Entry<K, V> entry : entrySet()) {
                K key = entry.getKey();
                V value = entry.getValue();
                if (value == null) {
                    if (map.get(key) != null || !map.containsKey(key)) {
                        return false;
                    }
                } else if (!value.equals(map.get(key))) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        int iHashCode = 0;
        while (it.hasNext()) {
            iHashCode += it.next().hashCode();
        }
        return iHashCode;
    }

    public String toString() {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        if (!it.hasNext()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        while (true) {
            Map.Entry<K, V> next = it.next();
            Object key = next.getKey();
            Object value = next.getValue();
            if (key == this) {
                key = "(this Map)";
            }
            sb.append(key);
            sb.append('=');
            if (value == this) {
                value = "(this Map)";
            }
            sb.append(value);
            if (!it.hasNext()) {
                sb.append('}');
                return sb.toString();
            }
            sb.append(',');
            sb.append(' ');
        }
    }

    protected Object clone() throws CloneNotSupportedException {
        AbstractMap abstractMap = (AbstractMap) super.clone();
        abstractMap.keySet = null;
        abstractMap.values = null;
        return abstractMap;
    }

    private static boolean eq(Object obj, Object obj2) {
        return obj == null ? obj2 == null : obj.equals(obj2);
    }

    public static class SimpleEntry<K, V> implements Map.Entry<K, V>, Serializable {
        private static final long serialVersionUID = -8499721149061103585L;
        private final K key;
        private V value;

        public SimpleEntry(K k, V v) {
            this.key = k;
            this.value = v;
        }

        public SimpleEntry(Map.Entry<? extends K, ? extends V> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V v) {
            V v2 = this.value;
            this.value = v;
            return v2;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return AbstractMap.eq(this.key, entry.getKey()) && AbstractMap.eq(this.value, entry.getValue());
        }

        @Override
        public int hashCode() {
            int iHashCode;
            if (this.key != null) {
                iHashCode = this.key.hashCode();
            } else {
                iHashCode = 0;
            }
            return iHashCode ^ (this.value != null ? this.value.hashCode() : 0);
        }

        public String toString() {
            return ((Object) this.key) + "=" + ((Object) this.value);
        }
    }

    public static class SimpleImmutableEntry<K, V> implements Map.Entry<K, V>, Serializable {
        private static final long serialVersionUID = 7138329143949025153L;
        private final K key;
        private final V value;

        public SimpleImmutableEntry(K k, V v) {
            this.key = k;
            this.value = v;
        }

        public SimpleImmutableEntry(Map.Entry<? extends K, ? extends V> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return AbstractMap.eq(this.key, entry.getKey()) && AbstractMap.eq(this.value, entry.getValue());
        }

        @Override
        public int hashCode() {
            int iHashCode;
            if (this.key != null) {
                iHashCode = this.key.hashCode();
            } else {
                iHashCode = 0;
            }
            return iHashCode ^ (this.value != null ? this.value.hashCode() : 0);
        }

        public String toString() {
            return ((Object) this.key) + "=" + ((Object) this.value);
        }
    }
}
