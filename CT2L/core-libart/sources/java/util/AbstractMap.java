package java.util;

import java.io.Serializable;
import java.util.Map;

public abstract class AbstractMap<K, V> implements Map<K, V> {
    Set<K> keySet;
    Collection<V> valuesCollection;

    @Override
    public abstract Set<Map.Entry<K, V>> entrySet();

    public static class SimpleImmutableEntry<K, V> implements Map.Entry<K, V>, Serializable {
        private static final long serialVersionUID = 7138329143949025153L;
        private final K key;
        private final V value;

        public SimpleImmutableEntry(K theKey, V theValue) {
            this.key = theKey;
            this.value = theValue;
        }

        public SimpleImmutableEntry(Map.Entry<? extends K, ? extends V> copyFrom) {
            this.key = copyFrom.getKey();
            this.value = copyFrom.getValue();
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
        public V setValue(V object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry) object;
            if (this.key != null ? this.key.equals(entry.getKey()) : entry.getKey() == null) {
                if (this.value == null) {
                    if (entry.getValue() == null) {
                        return true;
                    }
                } else if (this.value.equals(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (this.key == null ? 0 : this.key.hashCode()) ^ (this.value != null ? this.value.hashCode() : 0);
        }

        public String toString() {
            return this.key + "=" + this.value;
        }
    }

    public static class SimpleEntry<K, V> implements Map.Entry<K, V>, Serializable {
        private static final long serialVersionUID = -8499721149061103585L;
        private final K key;
        private V value;

        public SimpleEntry(K theKey, V theValue) {
            this.key = theKey;
            this.value = theValue;
        }

        public SimpleEntry(Map.Entry<? extends K, ? extends V> copyFrom) {
            this.key = copyFrom.getKey();
            this.value = copyFrom.getValue();
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
        public V setValue(V object) {
            V result = this.value;
            this.value = object;
            return result;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry) object;
            if (this.key != null ? this.key.equals(entry.getKey()) : entry.getKey() == null) {
                if (this.value == null) {
                    if (entry.getValue() == null) {
                        return true;
                    }
                } else if (this.value.equals(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (this.key == null ? 0 : this.key.hashCode()) ^ (this.value != null ? this.value.hashCode() : 0);
        }

        public String toString() {
            return this.key + "=" + this.value;
        }
    }

    protected AbstractMap() {
    }

    @Override
    public void clear() {
        entrySet().clear();
    }

    @Override
    public boolean containsKey(Object key) {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        if (key != null) {
            while (it.hasNext()) {
                if (key.equals(it.next().getKey())) {
                    return true;
                }
            }
        } else {
            while (it.hasNext()) {
                if (it.next().getKey() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        if (value != null) {
            while (it.hasNext()) {
                if (value.equals(it.next().getValue())) {
                    return true;
                }
            }
        } else {
            while (it.hasNext()) {
                if (it.next().getValue() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Map)) {
            return false;
        }
        Map<?, ?> map = (Map) object;
        if (size() != map.size()) {
            return false;
        }
        try {
            for (Map.Entry<K, V> entry : entrySet()) {
                K key = entry.getKey();
                V mine = entry.getValue();
                Object theirs = map.get(key);
                if (mine == null) {
                    if (theirs != null || !map.containsKey(key)) {
                        return false;
                    }
                } else if (!mine.equals(theirs)) {
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
    public V get(Object key) {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        if (key != null) {
            while (it.hasNext()) {
                Map.Entry<K, V> entry = it.next();
                if (key.equals(entry.getKey())) {
                    return entry.getValue();
                }
            }
        } else {
            while (it.hasNext()) {
                Map.Entry<K, V> entry2 = it.next();
                if (entry2.getKey() == null) {
                    return entry2.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        int result = 0;
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext()) {
            result += it.next().hashCode();
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Set<K> keySet() {
        if (this.keySet == null) {
            this.keySet = new AbstractSet<K>() {
                @Override
                public boolean contains(Object object) {
                    return AbstractMap.this.containsKey(object);
                }

                @Override
                public int size() {
                    return AbstractMap.this.size();
                }

                @Override
                public Iterator<K> iterator() {
                    return new Iterator<K>() {
                        Iterator<Map.Entry<K, V>> setIterator;

                        {
                            this.setIterator = AbstractMap.this.entrySet().iterator();
                        }

                        @Override
                        public boolean hasNext() {
                            return this.setIterator.hasNext();
                        }

                        @Override
                        public K next() {
                            return this.setIterator.next().getKey();
                        }

                        @Override
                        public void remove() {
                            this.setIterator.remove();
                        }
                    };
                }
            };
        }
        return this.keySet;
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        if (key != null) {
            while (it.hasNext()) {
                Map.Entry<K, V> entry = it.next();
                if (key.equals(entry.getKey())) {
                    it.remove();
                    return entry.getValue();
                }
            }
        } else {
            while (it.hasNext()) {
                Map.Entry<K, V> entry2 = it.next();
                if (entry2.getKey() == null) {
                    it.remove();
                    return entry2.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public int size() {
        return entrySet().size();
    }

    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder(size() * 28);
        buffer.append('{');
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            Object key = entry.getKey();
            if (key != this) {
                buffer.append(key);
            } else {
                buffer.append("(this Map)");
            }
            buffer.append('=');
            Object value = entry.getValue();
            if (value != this) {
                buffer.append(value);
            } else {
                buffer.append("(this Map)");
            }
            if (it.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

    @Override
    public Collection<V> values() {
        if (this.valuesCollection == null) {
            this.valuesCollection = new AbstractCollection<V>() {
                @Override
                public int size() {
                    return AbstractMap.this.size();
                }

                @Override
                public boolean contains(Object object) {
                    return AbstractMap.this.containsValue(object);
                }

                @Override
                public Iterator<V> iterator() {
                    return new Iterator<V>() {
                        Iterator<Map.Entry<K, V>> setIterator;

                        {
                            this.setIterator = AbstractMap.this.entrySet().iterator();
                        }

                        @Override
                        public boolean hasNext() {
                            return this.setIterator.hasNext();
                        }

                        @Override
                        public V next() {
                            return this.setIterator.next().getValue();
                        }

                        @Override
                        public void remove() {
                            this.setIterator.remove();
                        }
                    };
                }
            };
        }
        return this.valuesCollection;
    }

    protected Object clone() throws CloneNotSupportedException {
        AbstractMap<K, V> result = (AbstractMap) super.clone();
        result.keySet = null;
        result.valuesCollection = null;
        return result;
    }
}
