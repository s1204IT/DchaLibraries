package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.Enum;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.MapEntry;

public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements Serializable, Cloneable, Map<K, V> {
    private static final long serialVersionUID = 458661240069192865L;
    private transient EnumMapEntrySet<K, V> entrySet = null;
    transient int enumSize;
    transient boolean[] hasMapping;
    private Class<K> keyType;
    transient Enum[] keys;
    private transient int mappingsCount;
    transient Object[] values;

    private static class Entry<KT extends Enum<KT>, VT> extends MapEntry<KT, VT> {
        private final EnumMap<KT, VT> enumMap;
        private final int ordinal;

        Entry(KT theKey, VT theValue, EnumMap<KT, VT> em) {
            super(theKey, theValue);
            this.enumMap = em;
            this.ordinal = theKey.ordinal();
        }

        @Override
        public boolean equals(Object object) {
            if (!this.enumMap.hasMapping[this.ordinal]) {
                return false;
            }
            boolean isEqual = false;
            if (object instanceof Map.Entry) {
                Map.Entry<KT, VT> entry = (Map.Entry) object;
                Object enumKey = entry.getKey();
                if (((Enum) this.key).equals(enumKey)) {
                    Object theValue = entry.getValue();
                    isEqual = this.enumMap.values[this.ordinal] == null ? theValue == null : this.enumMap.values[this.ordinal].equals(theValue);
                }
            }
            return isEqual;
        }

        @Override
        public int hashCode() {
            return (this.enumMap.keys[this.ordinal] == null ? 0 : this.enumMap.keys[this.ordinal].hashCode()) ^ (this.enumMap.values[this.ordinal] != null ? this.enumMap.values[this.ordinal].hashCode() : 0);
        }

        @Override
        public KT getKey() {
            checkEntryStatus();
            return (KT) this.enumMap.keys[this.ordinal];
        }

        @Override
        public VT getValue() {
            checkEntryStatus();
            return (VT) this.enumMap.values[this.ordinal];
        }

        @Override
        public VT setValue(VT value) {
            checkEntryStatus();
            return this.enumMap.put(this.enumMap.keys[this.ordinal], value);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(this.enumMap.keys[this.ordinal].toString());
            result.append("=");
            result.append(this.enumMap.values[this.ordinal] == null ? "null" : this.enumMap.values[this.ordinal].toString());
            return result.toString();
        }

        private void checkEntryStatus() {
            if (!this.enumMap.hasMapping[this.ordinal]) {
                throw new IllegalStateException();
            }
        }
    }

    private static class EnumMapIterator<E, KT extends Enum<KT>, VT> implements Iterator<E> {
        final EnumMap<KT, VT> enumMap;
        int position = 0;
        int prePosition = -1;
        final MapEntry.Type<E, KT, VT> type;

        EnumMapIterator(MapEntry.Type<E, KT, VT> value, EnumMap<KT, VT> em) {
            this.enumMap = em;
            this.type = value;
        }

        @Override
        public boolean hasNext() {
            int length = this.enumMap.enumSize;
            while (this.position < length && !this.enumMap.hasMapping[this.position]) {
                this.position++;
            }
            return this.position != length;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int i = this.position;
            this.position = i + 1;
            this.prePosition = i;
            return this.type.get(new MapEntry<>(this.enumMap.keys[this.prePosition], this.enumMap.values[this.prePosition]));
        }

        @Override
        public void remove() {
            checkStatus();
            if (this.enumMap.hasMapping[this.prePosition]) {
                this.enumMap.remove(this.enumMap.keys[this.prePosition]);
            }
            this.prePosition = -1;
        }

        public String toString() {
            return this.prePosition == -1 ? super.toString() : this.type.get(new MapEntry<>(this.enumMap.keys[this.prePosition], this.enumMap.values[this.prePosition])).toString();
        }

        private void checkStatus() {
            if (this.prePosition == -1) {
                throw new IllegalStateException();
            }
        }
    }

    private static class EnumMapKeySet<KT extends Enum<KT>, VT> extends AbstractSet<KT> {
        private final EnumMap<KT, VT> enumMap;

        EnumMapKeySet(EnumMap<KT, VT> em) {
            this.enumMap = em;
        }

        @Override
        public void clear() {
            this.enumMap.clear();
        }

        @Override
        public boolean contains(Object object) {
            return this.enumMap.containsKey(object);
        }

        @Override
        public Iterator iterator() {
            return new EnumMapIterator(new MapEntry.Type<KT, KT, VT>() {
                @Override
                public KT get(MapEntry<KT, VT> entry) {
                    return entry.key;
                }
            }, this.enumMap);
        }

        @Override
        public boolean remove(Object object) {
            if (!contains(object)) {
                return false;
            }
            this.enumMap.remove(object);
            return true;
        }

        @Override
        public int size() {
            return this.enumMap.size();
        }
    }

    private static class EnumMapValueCollection<KT extends Enum<KT>, VT> extends AbstractCollection<VT> {
        private final EnumMap<KT, VT> enumMap;

        EnumMapValueCollection(EnumMap<KT, VT> em) {
            this.enumMap = em;
        }

        @Override
        public void clear() {
            this.enumMap.clear();
        }

        @Override
        public boolean contains(Object object) {
            return this.enumMap.containsValue(object);
        }

        @Override
        public Iterator iterator() {
            return new EnumMapIterator(new MapEntry.Type<VT, KT, VT>() {
                @Override
                public VT get(MapEntry<KT, VT> entry) {
                    return entry.value;
                }
            }, this.enumMap);
        }

        @Override
        public boolean remove(Object object) {
            if (object == null) {
                for (int i = 0; i < this.enumMap.enumSize; i++) {
                    if (this.enumMap.hasMapping[i] && this.enumMap.values[i] == null) {
                        this.enumMap.remove(this.enumMap.keys[i]);
                        return true;
                    }
                }
            } else {
                for (int i2 = 0; i2 < this.enumMap.enumSize; i2++) {
                    if (this.enumMap.hasMapping[i2] && object.equals(this.enumMap.values[i2])) {
                        this.enumMap.remove(this.enumMap.keys[i2]);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public int size() {
            return this.enumMap.size();
        }
    }

    private static class EnumMapEntryIterator<E, KT extends Enum<KT>, VT> extends EnumMapIterator<E, KT, VT> {
        EnumMapEntryIterator(MapEntry.Type<E, KT, VT> value, EnumMap<KT, VT> em) {
            super(value, em);
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int i = this.position;
            this.position = i + 1;
            this.prePosition = i;
            return this.type.get(new Entry(this.enumMap.keys[this.prePosition], this.enumMap.values[this.prePosition], this.enumMap));
        }
    }

    private static class EnumMapEntrySet<KT extends Enum<KT>, VT> extends AbstractSet<Map.Entry<KT, VT>> {
        private final EnumMap<KT, VT> enumMap;

        EnumMapEntrySet(EnumMap<KT, VT> em) {
            this.enumMap = em;
        }

        @Override
        public void clear() {
            this.enumMap.clear();
        }

        @Override
        public boolean contains(Object object) {
            if (!(object instanceof Map.Entry)) {
                return false;
            }
            Object enumKey = ((Map.Entry) object).getKey();
            Object enumValue = ((Map.Entry) object).getValue();
            if (!this.enumMap.containsKey(enumKey)) {
                return false;
            }
            VT value = this.enumMap.get(enumKey);
            if (value == null) {
                return enumValue == null;
            }
            boolean isEqual = value.equals(enumValue);
            return isEqual;
        }

        @Override
        public Iterator<Map.Entry<KT, VT>> iterator() {
            return new EnumMapEntryIterator(new MapEntry.Type<Map.Entry<KT, VT>, KT, VT>() {
                @Override
                public Map.Entry<KT, VT> get(MapEntry<KT, VT> entry) {
                    return entry;
                }
            }, this.enumMap);
        }

        @Override
        public boolean remove(Object object) {
            if (!contains(object)) {
                return false;
            }
            this.enumMap.remove(((Map.Entry) object).getKey());
            return true;
        }

        @Override
        public int size() {
            return this.enumMap.size();
        }

        @Override
        public Object[] toArray() {
            Object[] entryArray = new Object[this.enumMap.size()];
            return toArray(entryArray);
        }

        @Override
        public Object[] toArray(Object[] array) {
            int size = this.enumMap.size();
            int index = 0;
            Object[] entryArray = array;
            if (size > array.length) {
                Class<?> clazz = array.getClass().getComponentType();
                Object[] entryArray2 = (Object[]) Array.newInstance(clazz, size);
                entryArray = entryArray2;
            }
            Iterator<Map.Entry<KT, VT>> iter = iterator();
            while (index < size) {
                Map.Entry<KT, VT> entry = iter.next();
                entryArray[index] = new MapEntry(entry.getKey(), entry.getValue());
                index++;
            }
            if (index < array.length) {
                entryArray[index] = null;
            }
            return entryArray;
        }
    }

    public EnumMap(Class<K> keyType) {
        initialization(keyType);
    }

    public EnumMap(EnumMap<K, ? extends V> map) {
        initialization(map);
    }

    public EnumMap(Map<K, ? extends V> map) {
        if (map instanceof EnumMap) {
            initialization((EnumMap) map);
            return;
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException("map is empty");
        }
        Iterator<K> iter = map.keySet().iterator();
        K enumKey = iter.next();
        Class<?> cls = enumKey.getClass();
        if (cls.isEnum()) {
            initialization(cls);
        } else {
            initialization(cls.getSuperclass());
        }
        putAllImpl(map);
    }

    @Override
    public void clear() {
        Arrays.fill(this.values, (Object) null);
        Arrays.fill(this.hasMapping, false);
        this.mappingsCount = 0;
    }

    @Override
    public EnumMap<K, V> clone() {
        try {
            EnumMap<K, V> enumMap = (EnumMap) super.clone();
            enumMap.initialization(this);
            return enumMap;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        if (!isValidKeyType(key)) {
            return false;
        }
        int keyOrdinal = ((Enum) key).ordinal();
        return this.hasMapping[keyOrdinal];
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            for (int i = 0; i < this.enumSize; i++) {
                if (this.hasMapping[i] && this.values[i] == null) {
                    return true;
                }
            }
        } else {
            for (int i2 = 0; i2 < this.enumSize; i2++) {
                if (this.hasMapping[i2] && value.equals(this.values[i2])) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (this.entrySet == null) {
            this.entrySet = new EnumMapEntrySet<>(this);
        }
        return this.entrySet;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof EnumMap)) {
            return super.equals(object);
        }
        EnumMap<K, V> enumMap = (EnumMap) object;
        if (this.keyType == enumMap.keyType && size() == enumMap.size()) {
            return Arrays.equals(this.hasMapping, enumMap.hasMapping) && Arrays.equals(this.values, enumMap.values);
        }
        return false;
    }

    @Override
    public V get(Object obj) {
        if (!isValidKeyType(obj)) {
            return null;
        }
        return (V) this.values[((Enum) obj).ordinal()];
    }

    @Override
    public Set<K> keySet() {
        if (this.keySet == null) {
            this.keySet = new EnumMapKeySet(this);
        }
        return this.keySet;
    }

    @Override
    public V put(K key, V value) {
        return putImpl(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        putAllImpl(map);
    }

    @Override
    public V remove(Object obj) {
        if (!isValidKeyType(obj)) {
            return null;
        }
        int iOrdinal = ((Enum) obj).ordinal();
        if (this.hasMapping[iOrdinal]) {
            this.hasMapping[iOrdinal] = false;
            this.mappingsCount--;
        }
        V v = (V) this.values[iOrdinal];
        this.values[iOrdinal] = null;
        return v;
    }

    @Override
    public int size() {
        return this.mappingsCount;
    }

    @Override
    public Collection<V> values() {
        if (this.valuesCollection == null) {
            this.valuesCollection = new EnumMapValueCollection(this);
        }
        return this.valuesCollection;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        initialization(this.keyType);
        int elementCount = stream.readInt();
        for (int i = elementCount; i > 0; i--) {
            Enum<K> enumKey = (Enum) stream.readObject();
            Object value = stream.readObject();
            putImpl(enumKey, value);
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(this.mappingsCount);
        for (Map.Entry<K, V> entry : entrySet()) {
            stream.writeObject(entry.getKey());
            stream.writeObject(entry.getValue());
        }
    }

    private boolean isValidKeyType(Object key) {
        return key != null && this.keyType.isInstance(key);
    }

    private void initialization(EnumMap enumMap) {
        this.keyType = enumMap.keyType;
        this.keys = enumMap.keys;
        this.enumSize = enumMap.enumSize;
        this.values = (Object[]) enumMap.values.clone();
        this.hasMapping = (boolean[]) enumMap.hasMapping.clone();
        this.mappingsCount = enumMap.mappingsCount;
    }

    private void initialization(Class<K> type) {
        this.keyType = type;
        this.keys = Enum.getSharedConstants(this.keyType);
        this.enumSize = this.keys.length;
        this.values = new Object[this.enumSize];
        this.hasMapping = new boolean[this.enumSize];
    }

    private void putAllImpl(Map map) {
        for (Map.Entry entry : map.entrySet()) {
            putImpl((Enum) entry.getKey(), entry.getValue());
        }
    }

    private V putImpl(K k, V v) {
        if (k == null) {
            throw new NullPointerException("key == null");
        }
        this.keyType.cast(k);
        int iOrdinal = k.ordinal();
        if (!this.hasMapping[iOrdinal]) {
            this.hasMapping[iOrdinal] = true;
            this.mappingsCount++;
        }
        V v2 = (V) this.values[iOrdinal];
        this.values[iOrdinal] = v;
        return v2;
    }
}
