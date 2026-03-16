package java.util;

import java.util.Map;

class MapEntry<K, V> implements Map.Entry<K, V>, Cloneable {
    K key;
    V value;

    interface Type<RT, KT, VT> {
        RT get(MapEntry<KT, VT> mapEntry);
    }

    MapEntry(K theKey) {
        this.key = theKey;
    }

    MapEntry(K theKey, V theValue) {
        this.key = theKey;
        this.value = theValue;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
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
    public K getKey() {
        return this.key;
    }

    @Override
    public V getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return (this.key == null ? 0 : this.key.hashCode()) ^ (this.value != null ? this.value.hashCode() : 0);
    }

    @Override
    public V setValue(V object) {
        V result = this.value;
        this.value = object;
        return result;
    }

    public String toString() {
        return this.key + "=" + this.value;
    }
}
