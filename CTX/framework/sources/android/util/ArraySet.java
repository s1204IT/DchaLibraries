package android.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import libcore.util.EmptyArray;

public final class ArraySet<E> implements Collection<E>, Set<E> {
    private static final int BASE_SIZE = 4;
    private static final int CACHE_SIZE = 10;
    private static final boolean DEBUG = false;
    private static final String TAG = "ArraySet";
    static Object[] sBaseCache;
    static int sBaseCacheSize;
    static Object[] sTwiceBaseCache;
    static int sTwiceBaseCacheSize;
    Object[] mArray;
    MapCollections<E, E> mCollections;
    int[] mHashes;
    final boolean mIdentityHashCode;
    int mSize;

    private int indexOf(Object obj, int i) {
        int i2 = this.mSize;
        if (i2 == 0) {
            return -1;
        }
        int iBinarySearch = ContainerHelpers.binarySearch(this.mHashes, i2, i);
        if (iBinarySearch < 0 || obj.equals(this.mArray[iBinarySearch])) {
            return iBinarySearch;
        }
        int i3 = iBinarySearch + 1;
        while (i3 < i2 && this.mHashes[i3] == i) {
            if (obj.equals(this.mArray[i3])) {
                return i3;
            }
            i3++;
        }
        for (int i4 = iBinarySearch - 1; i4 >= 0 && this.mHashes[i4] == i; i4--) {
            if (obj.equals(this.mArray[i4])) {
                return i4;
            }
        }
        return ~i3;
    }

    private int indexOfNull() {
        int i = this.mSize;
        if (i == 0) {
            return -1;
        }
        int iBinarySearch = ContainerHelpers.binarySearch(this.mHashes, i, 0);
        if (iBinarySearch < 0 || this.mArray[iBinarySearch] == null) {
            return iBinarySearch;
        }
        int i2 = iBinarySearch + 1;
        while (i2 < i && this.mHashes[i2] == 0) {
            if (this.mArray[i2] == null) {
                return i2;
            }
            i2++;
        }
        for (int i3 = iBinarySearch - 1; i3 >= 0 && this.mHashes[i3] == 0; i3--) {
            if (this.mArray[i3] == null) {
                return i3;
            }
        }
        return ~i2;
    }

    private void allocArrays(int i) {
        if (i == 8) {
            synchronized (ArraySet.class) {
                if (sTwiceBaseCache != null) {
                    Object[] objArr = sTwiceBaseCache;
                    try {
                        this.mArray = objArr;
                        sTwiceBaseCache = (Object[]) objArr[0];
                        this.mHashes = (int[]) objArr[1];
                        objArr[1] = null;
                        objArr[0] = null;
                        sTwiceBaseCacheSize--;
                        return;
                    } catch (ClassCastException e) {
                        Slog.wtf(TAG, "Found corrupt ArraySet cache: [0]=" + objArr[0] + " [1]=" + objArr[1]);
                        sTwiceBaseCache = null;
                        sTwiceBaseCacheSize = 0;
                    }
                }
            }
        } else if (i == 4) {
            synchronized (ArraySet.class) {
                if (sBaseCache != null) {
                    Object[] objArr2 = sBaseCache;
                    try {
                        this.mArray = objArr2;
                        sBaseCache = (Object[]) objArr2[0];
                        this.mHashes = (int[]) objArr2[1];
                        objArr2[1] = null;
                        objArr2[0] = null;
                        sBaseCacheSize--;
                        return;
                    } catch (ClassCastException e2) {
                        Slog.wtf(TAG, "Found corrupt ArraySet cache: [0]=" + objArr2[0] + " [1]=" + objArr2[1]);
                        sBaseCache = null;
                        sBaseCacheSize = 0;
                    }
                }
            }
        }
        this.mHashes = new int[i];
        this.mArray = new Object[i];
    }

    private static void freeArrays(int[] iArr, Object[] objArr, int i) {
        if (iArr.length == 8) {
            synchronized (ArraySet.class) {
                if (sTwiceBaseCacheSize < 10) {
                    objArr[0] = sTwiceBaseCache;
                    objArr[1] = iArr;
                    for (int i2 = i - 1; i2 >= 2; i2--) {
                        objArr[i2] = null;
                    }
                    sTwiceBaseCache = objArr;
                    sTwiceBaseCacheSize++;
                }
            }
            return;
        }
        if (iArr.length == 4) {
            synchronized (ArraySet.class) {
                if (sBaseCacheSize < 10) {
                    objArr[0] = sBaseCache;
                    objArr[1] = iArr;
                    for (int i3 = i - 1; i3 >= 2; i3--) {
                        objArr[i3] = null;
                    }
                    sBaseCache = objArr;
                    sBaseCacheSize++;
                }
            }
        }
    }

    public ArraySet() {
        this(0, false);
    }

    public ArraySet(int i) {
        this(i, false);
    }

    public ArraySet(int i, boolean z) {
        this.mIdentityHashCode = z;
        if (i == 0) {
            this.mHashes = EmptyArray.INT;
            this.mArray = EmptyArray.OBJECT;
        } else {
            allocArrays(i);
        }
        this.mSize = 0;
    }

    public ArraySet(ArraySet<E> arraySet) {
        this();
        if (arraySet != 0) {
            addAll((ArraySet) arraySet);
        }
    }

    public ArraySet(Collection<E> collection) {
        this();
        if (collection != 0) {
            addAll(collection);
        }
    }

    @Override
    public void clear() {
        if (this.mSize != 0) {
            freeArrays(this.mHashes, this.mArray, this.mSize);
            this.mHashes = EmptyArray.INT;
            this.mArray = EmptyArray.OBJECT;
            this.mSize = 0;
        }
    }

    public void ensureCapacity(int i) {
        if (this.mHashes.length < i) {
            int[] iArr = this.mHashes;
            Object[] objArr = this.mArray;
            allocArrays(i);
            if (this.mSize > 0) {
                System.arraycopy(iArr, 0, this.mHashes, 0, this.mSize);
                System.arraycopy(objArr, 0, this.mArray, 0, this.mSize);
            }
            freeArrays(iArr, objArr, this.mSize);
        }
    }

    @Override
    public boolean contains(Object obj) {
        return indexOf(obj) >= 0;
    }

    public int indexOf(Object obj) {
        if (obj == null) {
            return indexOfNull();
        }
        return indexOf(obj, this.mIdentityHashCode ? System.identityHashCode(obj) : obj.hashCode());
    }

    public E valueAt(int i) {
        return (E) this.mArray[i];
    }

    @Override
    public boolean isEmpty() {
        return this.mSize <= 0;
    }

    @Override
    public boolean add(E e) {
        int i;
        int iIndexOf;
        if (e == null) {
            iIndexOf = indexOfNull();
            i = 0;
        } else {
            int iIdentityHashCode = this.mIdentityHashCode ? System.identityHashCode(e) : e.hashCode();
            i = iIdentityHashCode;
            iIndexOf = indexOf(e, iIdentityHashCode);
        }
        if (iIndexOf >= 0) {
            return false;
        }
        int i2 = ~iIndexOf;
        if (this.mSize >= this.mHashes.length) {
            int i3 = 4;
            if (this.mSize >= 8) {
                i3 = (this.mSize >> 1) + this.mSize;
            } else if (this.mSize >= 4) {
                i3 = 8;
            }
            int[] iArr = this.mHashes;
            Object[] objArr = this.mArray;
            allocArrays(i3);
            if (this.mHashes.length > 0) {
                System.arraycopy(iArr, 0, this.mHashes, 0, iArr.length);
                System.arraycopy(objArr, 0, this.mArray, 0, objArr.length);
            }
            freeArrays(iArr, objArr, this.mSize);
        }
        if (i2 < this.mSize) {
            int i4 = i2 + 1;
            System.arraycopy(this.mHashes, i2, this.mHashes, i4, this.mSize - i2);
            System.arraycopy(this.mArray, i2, this.mArray, i4, this.mSize - i2);
        }
        this.mHashes[i2] = i;
        this.mArray[i2] = e;
        this.mSize++;
        return true;
    }

    public void append(E e) {
        int iIdentityHashCode;
        int i = this.mSize;
        if (e == null) {
            iIdentityHashCode = 0;
        } else {
            iIdentityHashCode = this.mIdentityHashCode ? System.identityHashCode(e) : e.hashCode();
        }
        if (i >= this.mHashes.length) {
            throw new IllegalStateException("Array is full");
        }
        if (i > 0 && this.mHashes[i - 1] > iIdentityHashCode) {
            add(e);
            return;
        }
        this.mSize = i + 1;
        this.mHashes[i] = iIdentityHashCode;
        this.mArray[i] = e;
    }

    public void addAll(ArraySet<? extends E> arraySet) {
        int i = arraySet.mSize;
        ensureCapacity(this.mSize + i);
        if (this.mSize == 0) {
            if (i > 0) {
                System.arraycopy(arraySet.mHashes, 0, this.mHashes, 0, i);
                System.arraycopy(arraySet.mArray, 0, this.mArray, 0, i);
                this.mSize = i;
                return;
            }
            return;
        }
        for (int i2 = 0; i2 < i; i2++) {
            add(arraySet.valueAt(i2));
        }
    }

    @Override
    public boolean remove(Object obj) {
        int iIndexOf = indexOf(obj);
        if (iIndexOf >= 0) {
            removeAt(iIndexOf);
            return true;
        }
        return false;
    }

    public E removeAt(int i) {
        E e = (E) this.mArray[i];
        if (this.mSize <= 1) {
            freeArrays(this.mHashes, this.mArray, this.mSize);
            this.mHashes = EmptyArray.INT;
            this.mArray = EmptyArray.OBJECT;
            this.mSize = 0;
        } else {
            int i2 = 8;
            if (this.mHashes.length > 8 && this.mSize < this.mHashes.length / 3) {
                if (this.mSize > 8) {
                    i2 = (this.mSize >> 1) + this.mSize;
                }
                int[] iArr = this.mHashes;
                Object[] objArr = this.mArray;
                allocArrays(i2);
                this.mSize--;
                if (i > 0) {
                    System.arraycopy(iArr, 0, this.mHashes, 0, i);
                    System.arraycopy(objArr, 0, this.mArray, 0, i);
                }
                if (i < this.mSize) {
                    int i3 = i + 1;
                    System.arraycopy(iArr, i3, this.mHashes, i, this.mSize - i);
                    System.arraycopy(objArr, i3, this.mArray, i, this.mSize - i);
                }
            } else {
                this.mSize--;
                if (i < this.mSize) {
                    int i4 = i + 1;
                    System.arraycopy(this.mHashes, i4, this.mHashes, i, this.mSize - i);
                    System.arraycopy(this.mArray, i4, this.mArray, i, this.mSize - i);
                }
                this.mArray[this.mSize] = null;
            }
        }
        return e;
    }

    public boolean removeAll(ArraySet<? extends E> arraySet) {
        int i = arraySet.mSize;
        int i2 = this.mSize;
        for (int i3 = 0; i3 < i; i3++) {
            remove(arraySet.valueAt(i3));
        }
        return i2 != this.mSize;
    }

    @Override
    public int size() {
        return this.mSize;
    }

    @Override
    public Object[] toArray() {
        Object[] objArr = new Object[this.mSize];
        System.arraycopy(this.mArray, 0, objArr, 0, this.mSize);
        return objArr;
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        if (tArr.length < this.mSize) {
            tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), this.mSize));
        }
        System.arraycopy(this.mArray, 0, tArr, 0, this.mSize);
        if (tArr.length > this.mSize) {
            tArr[this.mSize] = null;
        }
        return tArr;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Set)) {
            return false;
        }
        Set set = (Set) obj;
        if (size() != set.size()) {
            return false;
        }
        for (int i = 0; i < this.mSize; i++) {
            try {
                if (!set.contains(valueAt(i))) {
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
        int i = this.mSize;
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            i2 += iArr[i3];
        }
        return i2;
    }

    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder(this.mSize * 14);
        sb.append('{');
        for (int i = 0; i < this.mSize; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            E eValueAt = valueAt(i);
            if (eValueAt != this) {
                sb.append(eValueAt);
            } else {
                sb.append("(this Set)");
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private MapCollections<E, E> getCollection() {
        if (this.mCollections == null) {
            this.mCollections = new MapCollections<E, E>() {
                @Override
                protected int colGetSize() {
                    return ArraySet.this.mSize;
                }

                @Override
                protected Object colGetEntry(int i, int i2) {
                    return ArraySet.this.mArray[i];
                }

                @Override
                protected int colIndexOfKey(Object obj) {
                    return ArraySet.this.indexOf(obj);
                }

                @Override
                protected int colIndexOfValue(Object obj) {
                    return ArraySet.this.indexOf(obj);
                }

                @Override
                protected Map<E, E> colGetMap() {
                    throw new UnsupportedOperationException("not a map");
                }

                @Override
                protected void colPut(E e, E e2) {
                    ArraySet.this.add(e);
                }

                @Override
                protected E colSetValue(int i, E e) {
                    throw new UnsupportedOperationException("not a map");
                }

                @Override
                protected void colRemoveAt(int i) {
                    ArraySet.this.removeAt(i);
                }

                @Override
                protected void colClear() {
                    ArraySet.this.clear();
                }
            };
        }
        return this.mCollections;
    }

    @Override
    public Iterator<E> iterator() {
        return getCollection().getKeySet().iterator();
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            if (!contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        ensureCapacity(this.mSize + collection.size());
        Iterator<? extends E> it = collection.iterator();
        boolean zAdd = false;
        while (it.hasNext()) {
            zAdd |= add(it.next());
        }
        return zAdd;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        Iterator<?> it = collection.iterator();
        boolean zRemove = false;
        while (it.hasNext()) {
            zRemove |= remove(it.next());
        }
        return zRemove;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean z = false;
        for (int i = this.mSize - 1; i >= 0; i--) {
            if (!collection.contains(this.mArray[i])) {
                removeAt(i);
                z = true;
            }
        }
        return z;
    }
}
