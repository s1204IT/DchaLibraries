package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.Enum;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.Map;

public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements Serializable, Cloneable {
    private static final Object NULL = new Object() {
        public int hashCode() {
            return 0;
        }

        public String toString() {
            return "java.util.EnumMap.NULL";
        }
    };
    private static final Enum<?>[] ZERO_LENGTH_ENUM_ARRAY = new Enum[0];
    private static final long serialVersionUID = 458661240069192865L;
    private transient Set<Map.Entry<K, V>> entrySet;
    private final Class<K> keyType;
    private transient K[] keyUniverse;
    private transient int size;
    private transient Object[] vals;

    static int access$210(EnumMap enumMap) {
        int i = enumMap.size;
        enumMap.size = i - 1;
        return i;
    }

    private Object maskNull(Object obj) {
        return obj == null ? NULL : obj;
    }

    private V unmaskNull(Object obj) {
        if (obj == NULL) {
            return null;
        }
        return obj;
    }

    public EnumMap(Class<K> cls) {
        this.size = 0;
        this.keyType = cls;
        this.keyUniverse = (K[]) getKeyUniverse(cls);
        this.vals = new Object[this.keyUniverse.length];
    }

    public EnumMap(EnumMap<K, ? extends V> enumMap) {
        this.size = 0;
        this.keyType = enumMap.keyType;
        this.keyUniverse = enumMap.keyUniverse;
        this.vals = (Object[]) enumMap.vals.clone();
        this.size = enumMap.size;
    }

    public EnumMap(Map<K, ? extends V> map) {
        this.size = 0;
        if (map instanceof EnumMap) {
            EnumMap enumMap = (EnumMap) map;
            this.keyType = enumMap.keyType;
            this.keyUniverse = enumMap.keyUniverse;
            this.vals = (Object[]) enumMap.vals.clone();
            this.size = enumMap.size;
            return;
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException("Specified map is empty");
        }
        this.keyType = ((Enum) map.keySet().iterator().next()).getDeclaringClass();
        this.keyUniverse = (K[]) getKeyUniverse(this.keyType);
        this.vals = new Object[this.keyUniverse.length];
        putAll(map);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean containsValue(Object obj) {
        Object objMaskNull = maskNull(obj);
        for (Object obj2 : this.vals) {
            if (objMaskNull.equals(obj2)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsKey(Object obj) {
        return isValidKey(obj) && this.vals[((Enum) obj).ordinal()] != null;
    }

    private boolean containsMapping(Object obj, Object obj2) {
        return isValidKey(obj) && maskNull(obj2).equals(this.vals[((Enum) obj).ordinal()]);
    }

    @Override
    public V get(Object obj) {
        if (isValidKey(obj)) {
            return unmaskNull(this.vals[((Enum) obj).ordinal()]);
        }
        return null;
    }

    @Override
    public V put(K k, V v) {
        typeCheck(k);
        int iOrdinal = k.ordinal();
        Object obj = this.vals[iOrdinal];
        this.vals[iOrdinal] = maskNull(v);
        if (obj == null) {
            this.size++;
        }
        return unmaskNull(obj);
    }

    @Override
    public V remove(Object obj) {
        if (!isValidKey(obj)) {
            return null;
        }
        int iOrdinal = ((Enum) obj).ordinal();
        Object obj2 = this.vals[iOrdinal];
        this.vals[iOrdinal] = null;
        if (obj2 != null) {
            this.size--;
        }
        return unmaskNull(obj2);
    }

    private boolean removeMapping(Object obj, Object obj2) {
        if (!isValidKey(obj)) {
            return false;
        }
        int iOrdinal = ((Enum) obj).ordinal();
        if (!maskNull(obj2).equals(this.vals[iOrdinal])) {
            return false;
        }
        this.vals[iOrdinal] = null;
        this.size--;
        return true;
    }

    private boolean isValidKey(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> cls = obj.getClass();
        return cls == this.keyType || cls.getSuperclass() == this.keyType;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map instanceof EnumMap) {
            EnumMap enumMap = (EnumMap) map;
            if (enumMap.keyType != this.keyType) {
                if (enumMap.isEmpty()) {
                    return;
                }
                throw new ClassCastException(((Object) enumMap.keyType) + " != " + ((Object) this.keyType));
            }
            for (int i = 0; i < this.keyUniverse.length; i++) {
                Object obj = enumMap.vals[i];
                if (obj != null) {
                    if (this.vals[i] == null) {
                        this.size++;
                    }
                    this.vals[i] = obj;
                }
            }
            return;
        }
        super.putAll(map);
    }

    @Override
    public void clear() {
        Arrays.fill(this.vals, (Object) null);
        this.size = 0;
    }

    @Override
    public Set<K> keySet() {
        Set<K> set = this.keySet;
        if (set == null) {
            KeySet keySet = new KeySet();
            this.keySet = keySet;
            return keySet;
        }
        return set;
    }

    private class KeySet extends AbstractSet<K> {
        private KeySet() {
        }

        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return EnumMap.this.size;
        }

        @Override
        public boolean contains(Object obj) {
            return EnumMap.this.containsKey(obj);
        }

        @Override
        public boolean remove(Object obj) {
            int i = EnumMap.this.size;
            EnumMap.this.remove(obj);
            return EnumMap.this.size != i;
        }

        @Override
        public void clear() {
            EnumMap.this.clear();
        }
    }

    @Override
    public Collection<V> values() {
        Collection<V> collection = this.values;
        if (collection == null) {
            Values values = new Values();
            this.values = values;
            return values;
        }
        return collection;
    }

    private class Values extends AbstractCollection<V> {
        private Values() {
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override
        public int size() {
            return EnumMap.this.size;
        }

        @Override
        public boolean contains(Object obj) {
            return EnumMap.this.containsValue(obj);
        }

        @Override
        public boolean remove(Object obj) {
            Object objMaskNull = EnumMap.this.maskNull(obj);
            for (int i = 0; i < EnumMap.this.vals.length; i++) {
                if (objMaskNull.equals(EnumMap.this.vals[i])) {
                    EnumMap.this.vals[i] = null;
                    EnumMap.access$210(EnumMap.this);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            EnumMap.this.clear();
        }
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

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        private EntrySet() {
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return EnumMap.this.containsMapping(entry.getKey(), entry.getValue());
        }

        @Override
        public boolean remove(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return EnumMap.this.removeMapping(entry.getKey(), entry.getValue());
        }

        @Override
        public int size() {
            return EnumMap.this.size;
        }

        @Override
        public void clear() {
            EnumMap.this.clear();
        }

        @Override
        public Object[] toArray() {
            return fillEntryArray(new Object[EnumMap.this.size]);
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            int size = size();
            if (tArr.length < size) {
                tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), size));
            }
            if (tArr.length > size) {
                tArr[size] = null;
            }
            return (T[]) fillEntryArray(tArr);
        }

        private Object[] fillEntryArray(Object[] objArr) {
            int i = 0;
            for (int i2 = 0; i2 < EnumMap.this.vals.length; i2++) {
                if (EnumMap.this.vals[i2] != null) {
                    objArr[i] = new AbstractMap.SimpleEntry(EnumMap.this.keyUniverse[i2], EnumMap.this.unmaskNull(EnumMap.this.vals[i2]));
                    i++;
                }
            }
            return objArr;
        }
    }

    private abstract class EnumMapIterator<T> implements Iterator<T> {
        int index;
        int lastReturnedIndex;

        private EnumMapIterator() {
            this.index = 0;
            this.lastReturnedIndex = -1;
        }

        @Override
        public boolean hasNext() {
            while (this.index < EnumMap.this.vals.length && EnumMap.this.vals[this.index] == null) {
                this.index++;
            }
            return this.index != EnumMap.this.vals.length;
        }

        @Override
        public void remove() {
            checkLastReturnedIndex();
            if (EnumMap.this.vals[this.lastReturnedIndex] != null) {
                EnumMap.this.vals[this.lastReturnedIndex] = null;
                EnumMap.access$210(EnumMap.this);
            }
            this.lastReturnedIndex = -1;
        }

        private void checkLastReturnedIndex() {
            if (this.lastReturnedIndex < 0) {
                throw new IllegalStateException();
            }
        }
    }

    private class KeyIterator extends EnumMap<K, V>.EnumMapIterator<K> {
        private KeyIterator() {
            super();
        }

        @Override
        public K next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int i = this.index;
            this.index = i + 1;
            this.lastReturnedIndex = i;
            return (K) EnumMap.this.keyUniverse[this.lastReturnedIndex];
        }
    }

    private class ValueIterator extends EnumMap<K, V>.EnumMapIterator<V> {
        private ValueIterator() {
            super();
        }

        @Override
        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int i = this.index;
            this.index = i + 1;
            this.lastReturnedIndex = i;
            return (V) EnumMap.this.unmaskNull(EnumMap.this.vals[this.lastReturnedIndex]);
        }
    }

    private class EntryIterator extends EnumMap<K, V>.EnumMapIterator<Map.Entry<K, V>> {
        private EnumMap<K, V>.EntryIterator.Entry lastReturnedEntry;

        private EntryIterator() {
            super();
        }

        @Override
        public Map.Entry<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int i = this.index;
            this.index = i + 1;
            this.lastReturnedEntry = new Entry(i);
            return this.lastReturnedEntry;
        }

        @Override
        public void remove() {
            this.lastReturnedIndex = this.lastReturnedEntry == null ? -1 : ((Entry) this.lastReturnedEntry).index;
            super.remove();
            ((Entry) this.lastReturnedEntry).index = this.lastReturnedIndex;
            this.lastReturnedEntry = null;
        }

        private class Entry implements Map.Entry<K, V> {
            private int index;

            private Entry(int i) {
                this.index = i;
            }

            @Override
            public K getKey() {
                checkIndexForEntryUse();
                return (K) EnumMap.this.keyUniverse[this.index];
            }

            @Override
            public V getValue() {
                checkIndexForEntryUse();
                return (V) EnumMap.this.unmaskNull(EnumMap.this.vals[this.index]);
            }

            @Override
            public V setValue(V v) {
                checkIndexForEntryUse();
                V v2 = (V) EnumMap.this.unmaskNull(EnumMap.this.vals[this.index]);
                EnumMap.this.vals[this.index] = EnumMap.this.maskNull(v);
                return v2;
            }

            @Override
            public boolean equals(Object obj) {
                if (this.index < 0) {
                    return obj == this;
                }
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry entry = (Map.Entry) obj;
                Object objUnmaskNull = EnumMap.this.unmaskNull(EnumMap.this.vals[this.index]);
                Object value = entry.getValue();
                if (entry.getKey() == EnumMap.this.keyUniverse[this.index]) {
                    if (objUnmaskNull == value) {
                        return true;
                    }
                    if (objUnmaskNull != null && objUnmaskNull.equals(value)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public int hashCode() {
                if (this.index >= 0) {
                    return EnumMap.this.entryHashCode(this.index);
                }
                return super.hashCode();
            }

            public String toString() {
                if (this.index < 0) {
                    return super.toString();
                }
                return ((Object) EnumMap.this.keyUniverse[this.index]) + "=" + EnumMap.this.unmaskNull(EnumMap.this.vals[this.index]);
            }

            private void checkIndexForEntryUse() {
                if (this.index < 0) {
                    throw new IllegalStateException("Entry was removed");
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EnumMap) {
            return equals((EnumMap<?, ?>) obj);
        }
        if (!(obj instanceof Map)) {
            return false;
        }
        Map map = (Map) obj;
        if (this.size != map.size()) {
            return false;
        }
        for (int i = 0; i < this.keyUniverse.length; i++) {
            if (this.vals[i] != null) {
                K k = this.keyUniverse[i];
                V vUnmaskNull = unmaskNull(this.vals[i]);
                if (vUnmaskNull == null) {
                    if (map.get(k) != null || !map.containsKey(k)) {
                        return false;
                    }
                } else if (!vUnmaskNull.equals(map.get(k))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean equals(EnumMap<?, ?> enumMap) {
        if (enumMap.keyType != this.keyType) {
            return this.size == 0 && enumMap.size == 0;
        }
        for (int i = 0; i < this.keyUniverse.length; i++) {
            Object obj = this.vals[i];
            Object obj2 = enumMap.vals[i];
            if (obj2 != obj && (obj2 == null || !obj2.equals(obj))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int iEntryHashCode = 0;
        for (int i = 0; i < this.keyUniverse.length; i++) {
            if (this.vals[i] != null) {
                iEntryHashCode += entryHashCode(i);
            }
        }
        return iEntryHashCode;
    }

    private int entryHashCode(int i) {
        return this.vals[i].hashCode() ^ this.keyUniverse[i].hashCode();
    }

    @Override
    public EnumMap<K, V> clone() {
        try {
            EnumMap<K, V> enumMap = (EnumMap) super.clone();
            enumMap.vals = (Object[]) enumMap.vals.clone();
            enumMap.entrySet = null;
            return enumMap;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    private void typeCheck(K k) {
        Class<?> cls = k.getClass();
        if (cls != this.keyType && cls.getSuperclass() != this.keyType) {
            throw new ClassCastException(((Object) cls) + " != " + ((Object) this.keyType));
        }
    }

    private static <K extends Enum<K>> K[] getKeyUniverse(Class<K> cls) {
        return cls.getEnumConstantsShared();
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(this.size);
        int i = this.size;
        int i2 = 0;
        while (i > 0) {
            if (this.vals[i2] != null) {
                objectOutputStream.writeObject(this.keyUniverse[i2]);
                objectOutputStream.writeObject(unmaskNull(this.vals[i2]));
                i--;
            }
            i2++;
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        this.keyUniverse = (K[]) getKeyUniverse(this.keyType);
        this.vals = new Object[this.keyUniverse.length];
        int i = objectInputStream.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            put((Enum) objectInputStream.readObject(), objectInputStream.readObject());
        }
    }
}
