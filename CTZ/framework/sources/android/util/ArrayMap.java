package android.util;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;
import libcore.util.EmptyArray;

public final class ArrayMap<K, V> implements Map<K, V> {
    private static final int BASE_SIZE = 4;
    private static final int CACHE_SIZE = 10;
    private static final boolean CONCURRENT_MODIFICATION_EXCEPTIONS = true;
    private static final boolean DEBUG = false;
    private static final String TAG = "ArrayMap";
    static Object[] mBaseCache;
    static int mBaseCacheSize;
    static Object[] mTwiceBaseCache;
    static int mTwiceBaseCacheSize;
    Object[] mArray;
    MapCollections<K, V> mCollections;
    int[] mHashes;
    final boolean mIdentityHashCode;
    int mSize;
    static final int[] EMPTY_IMMUTABLE_INTS = new int[0];
    public static final ArrayMap EMPTY = new ArrayMap(-1);

    private static int binarySearchHashes(int[] iArr, int i, int i2) {
        try {
            return ContainerHelpers.binarySearch(iArr, i, i2);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
        }
    }

    int indexOf(Object obj, int i) {
        int i2 = this.mSize;
        if (i2 == 0) {
            return -1;
        }
        int iBinarySearchHashes = binarySearchHashes(this.mHashes, i2, i);
        if (iBinarySearchHashes < 0 || obj.equals(this.mArray[iBinarySearchHashes << 1])) {
            return iBinarySearchHashes;
        }
        int i3 = iBinarySearchHashes + 1;
        while (i3 < i2 && this.mHashes[i3] == i) {
            if (obj.equals(this.mArray[i3 << 1])) {
                return i3;
            }
            i3++;
        }
        for (int i4 = iBinarySearchHashes - 1; i4 >= 0 && this.mHashes[i4] == i; i4--) {
            if (obj.equals(this.mArray[i4 << 1])) {
                return i4;
            }
        }
        return ~i3;
    }

    int indexOfNull() {
        int i = this.mSize;
        if (i == 0) {
            return -1;
        }
        int iBinarySearchHashes = binarySearchHashes(this.mHashes, i, 0);
        if (iBinarySearchHashes < 0 || this.mArray[iBinarySearchHashes << 1] == null) {
            return iBinarySearchHashes;
        }
        int i2 = iBinarySearchHashes + 1;
        while (i2 < i && this.mHashes[i2] == 0) {
            if (this.mArray[i2 << 1] == null) {
                return i2;
            }
            i2++;
        }
        for (int i3 = iBinarySearchHashes - 1; i3 >= 0 && this.mHashes[i3] == 0; i3--) {
            if (this.mArray[i3 << 1] == null) {
                return i3;
            }
        }
        return ~i2;
    }

    private void allocArrays(int i) {
        if (this.mHashes == EMPTY_IMMUTABLE_INTS) {
            throw new UnsupportedOperationException("ArrayMap is immutable");
        }
        if (i == 8) {
            synchronized (ArrayMap.class) {
                if (mTwiceBaseCache != null) {
                    Object[] objArr = mTwiceBaseCache;
                    this.mArray = objArr;
                    mTwiceBaseCache = (Object[]) objArr[0];
                    this.mHashes = (int[]) objArr[1];
                    objArr[1] = null;
                    objArr[0] = null;
                    mTwiceBaseCacheSize--;
                    return;
                }
            }
        } else if (i == 4) {
            synchronized (ArrayMap.class) {
                if (mBaseCache != null) {
                    Object[] objArr2 = mBaseCache;
                    this.mArray = objArr2;
                    mBaseCache = (Object[]) objArr2[0];
                    this.mHashes = (int[]) objArr2[1];
                    objArr2[1] = null;
                    objArr2[0] = null;
                    mBaseCacheSize--;
                    return;
                }
            }
        }
        this.mHashes = new int[i];
        this.mArray = new Object[i << 1];
    }

    private static void freeArrays(int[] iArr, Object[] objArr, int i) {
        if (iArr.length == 8) {
            synchronized (ArrayMap.class) {
                if (mTwiceBaseCacheSize < 10) {
                    objArr[0] = mTwiceBaseCache;
                    objArr[1] = iArr;
                    for (int i2 = (i << 1) - 1; i2 >= 2; i2--) {
                        objArr[i2] = null;
                    }
                    mTwiceBaseCache = objArr;
                    mTwiceBaseCacheSize++;
                }
            }
            return;
        }
        if (iArr.length == 4) {
            synchronized (ArrayMap.class) {
                if (mBaseCacheSize < 10) {
                    objArr[0] = mBaseCache;
                    objArr[1] = iArr;
                    for (int i3 = (i << 1) - 1; i3 >= 2; i3--) {
                        objArr[i3] = null;
                    }
                    mBaseCache = objArr;
                    mBaseCacheSize++;
                }
            }
        }
    }

    public ArrayMap() {
        this(0, false);
    }

    public ArrayMap(int i) {
        this(i, false);
    }

    public ArrayMap(int i, boolean z) {
        this.mIdentityHashCode = z;
        if (i < 0) {
            this.mHashes = EMPTY_IMMUTABLE_INTS;
            this.mArray = EmptyArray.OBJECT;
        } else if (i == 0) {
            this.mHashes = EmptyArray.INT;
            this.mArray = EmptyArray.OBJECT;
        } else {
            allocArrays(i);
        }
        this.mSize = 0;
    }

    public ArrayMap(ArrayMap<K, V> arrayMap) {
        this();
        if (arrayMap != 0) {
            putAll((ArrayMap) arrayMap);
        }
    }

    @Override
    public void clear() {
        if (this.mSize > 0) {
            int[] iArr = this.mHashes;
            Object[] objArr = this.mArray;
            int i = this.mSize;
            this.mHashes = EmptyArray.INT;
            this.mArray = EmptyArray.OBJECT;
            this.mSize = 0;
            freeArrays(iArr, objArr, i);
        }
        if (this.mSize > 0) {
            throw new ConcurrentModificationException();
        }
    }

    public void erase() {
        if (this.mSize > 0) {
            int i = this.mSize << 1;
            Object[] objArr = this.mArray;
            for (int i2 = 0; i2 < i; i2++) {
                objArr[i2] = null;
            }
            this.mSize = 0;
        }
    }

    public void ensureCapacity(int i) {
        int i2 = this.mSize;
        if (this.mHashes.length < i) {
            int[] iArr = this.mHashes;
            Object[] objArr = this.mArray;
            allocArrays(i);
            if (this.mSize > 0) {
                System.arraycopy(iArr, 0, this.mHashes, 0, i2);
                System.arraycopy(objArr, 0, this.mArray, 0, i2 << 1);
            }
            freeArrays(iArr, objArr, i2);
        }
        if (this.mSize != i2) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public boolean containsKey(Object obj) {
        return indexOfKey(obj) >= 0;
    }

    public int indexOfKey(Object obj) {
        if (obj == null) {
            return indexOfNull();
        }
        return indexOf(obj, this.mIdentityHashCode ? System.identityHashCode(obj) : obj.hashCode());
    }

    int indexOfValue(Object obj) {
        int i = this.mSize * 2;
        Object[] objArr = this.mArray;
        if (obj == null) {
            for (int i2 = 1; i2 < i; i2 += 2) {
                if (objArr[i2] == null) {
                    return i2 >> 1;
                }
            }
            return -1;
        }
        for (int i3 = 1; i3 < i; i3 += 2) {
            if (obj.equals(objArr[i3])) {
                return i3 >> 1;
            }
        }
        return -1;
    }

    @Override
    public boolean containsValue(Object obj) {
        return indexOfValue(obj) >= 0;
    }

    @Override
    public V get(Object obj) {
        int iIndexOfKey = indexOfKey(obj);
        if (iIndexOfKey >= 0) {
            return (V) this.mArray[(iIndexOfKey << 1) + 1];
        }
        return null;
    }

    public K keyAt(int i) {
        return (K) this.mArray[i << 1];
    }

    public V valueAt(int i) {
        return (V) this.mArray[(i << 1) + 1];
    }

    public V setValueAt(int i, V v) {
        int i2 = (i << 1) + 1;
        V v2 = (V) this.mArray[i2];
        this.mArray[i2] = v;
        return v2;
    }

    @Override
    public boolean isEmpty() {
        return this.mSize <= 0;
    }

    @Override
    public V put(K k, V v) {
        int i;
        int iIndexOf;
        int i2 = this.mSize;
        if (k == null) {
            iIndexOf = indexOfNull();
            i = 0;
        } else {
            int iIdentityHashCode = this.mIdentityHashCode ? System.identityHashCode(k) : k.hashCode();
            i = iIdentityHashCode;
            iIndexOf = indexOf(k, iIdentityHashCode);
        }
        if (iIndexOf >= 0) {
            int i3 = (iIndexOf << 1) + 1;
            V v2 = (V) this.mArray[i3];
            this.mArray[i3] = v;
            return v2;
        }
        int i4 = ~iIndexOf;
        if (i2 >= this.mHashes.length) {
            int i5 = 4;
            if (i2 >= 8) {
                i5 = (i2 >> 1) + i2;
            } else if (i2 >= 4) {
                i5 = 8;
            }
            int[] iArr = this.mHashes;
            Object[] objArr = this.mArray;
            allocArrays(i5);
            if (i2 != this.mSize) {
                throw new ConcurrentModificationException();
            }
            if (this.mHashes.length > 0) {
                System.arraycopy(iArr, 0, this.mHashes, 0, iArr.length);
                System.arraycopy(objArr, 0, this.mArray, 0, objArr.length);
            }
            freeArrays(iArr, objArr, i2);
        }
        if (i4 < i2) {
            int i6 = i4 + 1;
            System.arraycopy(this.mHashes, i4, this.mHashes, i6, i2 - i4);
            System.arraycopy(this.mArray, i4 << 1, this.mArray, i6 << 1, (this.mSize - i4) << 1);
        }
        if (i2 != this.mSize || i4 >= this.mHashes.length) {
            throw new ConcurrentModificationException();
        }
        this.mHashes[i4] = i;
        int i7 = i4 << 1;
        this.mArray[i7] = k;
        this.mArray[i7 + 1] = v;
        this.mSize++;
        return null;
    }

    public void append(K k, V v) {
        int iIdentityHashCode;
        int i = this.mSize;
        if (k == null) {
            iIdentityHashCode = 0;
        } else {
            iIdentityHashCode = this.mIdentityHashCode ? System.identityHashCode(k) : k.hashCode();
        }
        if (i >= this.mHashes.length) {
            throw new IllegalStateException("Array is full");
        }
        if (i > 0) {
            int i2 = i - 1;
            if (this.mHashes[i2] > iIdentityHashCode) {
                RuntimeException runtimeException = new RuntimeException("here");
                runtimeException.fillInStackTrace();
                Log.w(TAG, "New hash " + iIdentityHashCode + " is before end of array hash " + this.mHashes[i2] + " at index " + i + " key " + k, runtimeException);
                put(k, v);
                return;
            }
        }
        this.mSize = i + 1;
        this.mHashes[i] = iIdentityHashCode;
        int i3 = i << 1;
        this.mArray[i3] = k;
        this.mArray[i3 + 1] = v;
    }

    public void validate() {
        int i = this.mSize;
        if (i <= 1) {
            return;
        }
        int i2 = 0;
        int i3 = this.mHashes[0];
        for (int i4 = 1; i4 < i; i4++) {
            int i5 = this.mHashes[i4];
            if (i5 != i3) {
                i2 = i4;
                i3 = i5;
            } else {
                Object obj = this.mArray[i4 << 1];
                for (int i6 = i4 - 1; i6 >= i2; i6--) {
                    Object obj2 = this.mArray[i6 << 1];
                    if (obj == obj2) {
                        throw new IllegalArgumentException("Duplicate key in ArrayMap: " + obj);
                    }
                    if (obj != null && obj2 != null && obj.equals(obj2)) {
                        throw new IllegalArgumentException("Duplicate key in ArrayMap: " + obj);
                    }
                }
            }
        }
    }

    public void putAll(ArrayMap<? extends K, ? extends V> arrayMap) {
        int i = arrayMap.mSize;
        ensureCapacity(this.mSize + i);
        if (this.mSize == 0) {
            if (i > 0) {
                System.arraycopy(arrayMap.mHashes, 0, this.mHashes, 0, i);
                System.arraycopy(arrayMap.mArray, 0, this.mArray, 0, i << 1);
                this.mSize = i;
                return;
            }
            return;
        }
        for (int i2 = 0; i2 < i; i2++) {
            put(arrayMap.keyAt(i2), arrayMap.valueAt(i2));
        }
    }

    @Override
    public V remove(Object obj) {
        int iIndexOfKey = indexOfKey(obj);
        if (iIndexOfKey >= 0) {
            return removeAt(iIndexOfKey);
        }
        return null;
    }

    public V removeAt(int i) {
        int i2 = i << 1;
        V v = (V) this.mArray[i2 + 1];
        int i3 = this.mSize;
        int i4 = 0;
        if (i3 <= 1) {
            int[] iArr = this.mHashes;
            Object[] objArr = this.mArray;
            this.mHashes = EmptyArray.INT;
            this.mArray = EmptyArray.OBJECT;
            freeArrays(iArr, objArr, i3);
        } else {
            int i5 = i3 - 1;
            if (this.mHashes.length > 8 && this.mSize < this.mHashes.length / 3) {
                int i6 = i3 > 8 ? i3 + (i3 >> 1) : 8;
                int[] iArr2 = this.mHashes;
                Object[] objArr2 = this.mArray;
                allocArrays(i6);
                if (i3 != this.mSize) {
                    throw new ConcurrentModificationException();
                }
                if (i > 0) {
                    System.arraycopy(iArr2, 0, this.mHashes, 0, i);
                    System.arraycopy(objArr2, 0, this.mArray, 0, i2);
                }
                if (i < i5) {
                    int i7 = i + 1;
                    int i8 = i5 - i;
                    System.arraycopy(iArr2, i7, this.mHashes, i, i8);
                    System.arraycopy(objArr2, i7 << 1, this.mArray, i2, i8 << 1);
                }
            } else {
                if (i < i5) {
                    int i9 = i + 1;
                    int i10 = i5 - i;
                    System.arraycopy(this.mHashes, i9, this.mHashes, i, i10);
                    System.arraycopy(this.mArray, i9 << 1, this.mArray, i2, i10 << 1);
                }
                int i11 = i5 << 1;
                this.mArray[i11] = null;
                this.mArray[i11 + 1] = null;
            }
            i4 = i5;
        }
        if (i3 != this.mSize) {
            throw new ConcurrentModificationException();
        }
        this.mSize = i4;
        return v;
    }

    @Override
    public int size() {
        return this.mSize;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Map)) {
            return false;
        }
        Map map = (Map) obj;
        if (size() != map.size()) {
            return false;
        }
        for (int i = 0; i < this.mSize; i++) {
            try {
                K kKeyAt = keyAt(i);
                V vValueAt = valueAt(i);
                Object obj2 = map.get(kKeyAt);
                if (vValueAt == null) {
                    if (obj2 != null || !map.containsKey(kKeyAt)) {
                        return false;
                    }
                } else if (!vValueAt.equals(obj2)) {
                    return false;
                }
            } catch (ClassCastException e) {
                return false;
            } catch (NullPointerException e2) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int[] iArr = this.mHashes;
        Object[] objArr = this.mArray;
        int i = this.mSize;
        int i2 = 1;
        int i3 = 0;
        int iHashCode = 0;
        while (i3 < i) {
            Object obj = objArr[i2];
            iHashCode += (obj == null ? 0 : obj.hashCode()) ^ iArr[i3];
            i3++;
            i2 += 2;
        }
        return iHashCode;
    }

    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder(this.mSize * 28);
        sb.append('{');
        for (int i = 0; i < this.mSize; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            K kKeyAt = keyAt(i);
            if (kKeyAt != this) {
                sb.append(kKeyAt);
            } else {
                sb.append("(this Map)");
            }
            sb.append('=');
            V vValueAt = valueAt(i);
            if (vValueAt != this) {
                sb.append(vValueAt);
            } else {
                sb.append("(this Map)");
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private MapCollections<K, V> getCollection() {
        if (this.mCollections == null) {
            this.mCollections = new MapCollections<K, V>() {
                @Override
                protected int colGetSize() {
                    return ArrayMap.this.mSize;
                }

                @Override
                protected Object colGetEntry(int i, int i2) {
                    return ArrayMap.this.mArray[(i << 1) + i2];
                }

                @Override
                protected int colIndexOfKey(Object obj) {
                    return ArrayMap.this.indexOfKey(obj);
                }

                @Override
                protected int colIndexOfValue(Object obj) {
                    return ArrayMap.this.indexOfValue(obj);
                }

                @Override
                protected Map<K, V> colGetMap() {
                    return ArrayMap.this;
                }

                @Override
                protected void colPut(K k, V v) {
                    ArrayMap.this.put(k, v);
                }

                @Override
                protected V colSetValue(int i, V v) {
                    return (V) ArrayMap.this.setValueAt(i, v);
                }

                @Override
                protected void colRemoveAt(int i) {
                    ArrayMap.this.removeAt(i);
                }

                @Override
                protected void colClear() {
                    ArrayMap.this.clear();
                }
            };
        }
        return this.mCollections;
    }

    public boolean containsAll(Collection<?> collection) {
        return MapCollections.containsAllHelper(this, collection);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        ensureCapacity(this.mSize + map.size());
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public boolean removeAll(Collection<?> collection) {
        return MapCollections.removeAllHelper(this, collection);
    }

    public boolean retainAll(Collection<?> collection) {
        return MapCollections.retainAllHelper(this, collection);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return getCollection().getEntrySet();
    }

    @Override
    public Set<K> keySet() {
        return getCollection().getKeySet();
    }

    @Override
    public Collection<V> values() {
        return getCollection().getValues();
    }
}
