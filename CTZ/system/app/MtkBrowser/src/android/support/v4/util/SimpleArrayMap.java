package android.support.v4.util;

import java.util.ConcurrentModificationException;
import java.util.Map;
/* loaded from: classes.dex */
public class SimpleArrayMap<K, V> {
    static Object[] mBaseCache;
    static int mBaseCacheSize;
    static Object[] mTwiceBaseCache;
    static int mTwiceBaseCacheSize;
    int[] mHashes = ContainerHelpers.EMPTY_INTS;
    Object[] mArray = ContainerHelpers.EMPTY_OBJECTS;
    int mSize = 0;

    private static int binarySearchHashes(int[] hashes, int N, int hash) {
        try {
            return ContainerHelpers.binarySearch(hashes, N, hash);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
        }
    }

    int indexOf(Object key, int hash) {
        int N = this.mSize;
        if (N == 0) {
            return -1;
        }
        int index = binarySearchHashes(this.mHashes, N, hash);
        if (index < 0) {
            return index;
        }
        if (key.equals(this.mArray[index << 1])) {
            return index;
        }
        int end = index + 1;
        while (end < N && this.mHashes[end] == hash) {
            if (key.equals(this.mArray[end << 1])) {
                return end;
            }
            end++;
        }
        for (int i = index - 1; i >= 0 && this.mHashes[i] == hash; i--) {
            if (key.equals(this.mArray[i << 1])) {
                return i;
            }
        }
        int i2 = ~end;
        return i2;
    }

    int indexOfNull() {
        int N = this.mSize;
        if (N == 0) {
            return -1;
        }
        int index = binarySearchHashes(this.mHashes, N, 0);
        if (index < 0) {
            return index;
        }
        if (this.mArray[index << 1] == null) {
            return index;
        }
        int end = index + 1;
        while (end < N && this.mHashes[end] == 0) {
            if (this.mArray[end << 1] == null) {
                return end;
            }
            end++;
        }
        for (int i = index - 1; i >= 0 && this.mHashes[i] == 0; i--) {
            if (this.mArray[i << 1] == null) {
                return i;
            }
        }
        int i2 = ~end;
        return i2;
    }

    private void allocArrays(int size) {
        if (size == 8) {
            synchronized (ArrayMap.class) {
                if (mTwiceBaseCache != null) {
                    Object[] array = mTwiceBaseCache;
                    this.mArray = array;
                    mTwiceBaseCache = (Object[]) array[0];
                    this.mHashes = (int[]) array[1];
                    array[1] = null;
                    array[0] = null;
                    mTwiceBaseCacheSize--;
                    return;
                }
            }
        } else if (size == 4) {
            synchronized (ArrayMap.class) {
                if (mBaseCache != null) {
                    Object[] array2 = mBaseCache;
                    this.mArray = array2;
                    mBaseCache = (Object[]) array2[0];
                    this.mHashes = (int[]) array2[1];
                    array2[1] = null;
                    array2[0] = null;
                    mBaseCacheSize--;
                    return;
                }
            }
        }
        this.mHashes = new int[size];
        this.mArray = new Object[size << 1];
    }

    private static void freeArrays(int[] hashes, Object[] array, int size) {
        if (hashes.length == 8) {
            synchronized (ArrayMap.class) {
                if (mTwiceBaseCacheSize < 10) {
                    array[0] = mTwiceBaseCache;
                    array[1] = hashes;
                    for (int i = (size << 1) - 1; i >= 2; i--) {
                        array[i] = null;
                    }
                    mTwiceBaseCache = array;
                    mTwiceBaseCacheSize++;
                }
            }
        } else if (hashes.length == 4) {
            synchronized (ArrayMap.class) {
                if (mBaseCacheSize < 10) {
                    array[0] = mBaseCache;
                    array[1] = hashes;
                    for (int i2 = (size << 1) - 1; i2 >= 2; i2--) {
                        array[i2] = null;
                    }
                    mBaseCache = array;
                    mBaseCacheSize++;
                }
            }
        }
    }

    public void clear() {
        if (this.mSize > 0) {
            int[] ohashes = this.mHashes;
            Object[] oarray = this.mArray;
            int osize = this.mSize;
            this.mHashes = ContainerHelpers.EMPTY_INTS;
            this.mArray = ContainerHelpers.EMPTY_OBJECTS;
            this.mSize = 0;
            freeArrays(ohashes, oarray, osize);
        }
        if (this.mSize > 0) {
            throw new ConcurrentModificationException();
        }
    }

    public void ensureCapacity(int minimumCapacity) {
        int osize = this.mSize;
        if (this.mHashes.length < minimumCapacity) {
            int[] ohashes = this.mHashes;
            Object[] oarray = this.mArray;
            allocArrays(minimumCapacity);
            if (this.mSize > 0) {
                System.arraycopy(ohashes, 0, this.mHashes, 0, osize);
                System.arraycopy(oarray, 0, this.mArray, 0, osize << 1);
            }
            freeArrays(ohashes, oarray, osize);
        }
        if (this.mSize != osize) {
            throw new ConcurrentModificationException();
        }
    }

    public boolean containsKey(Object key) {
        return indexOfKey(key) >= 0;
    }

    public int indexOfKey(Object key) {
        return key == null ? indexOfNull() : indexOf(key, key.hashCode());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int indexOfValue(Object value) {
        int N = this.mSize * 2;
        Object[] array = this.mArray;
        int i = 1;
        if (value == null) {
            while (i < N) {
                if (array[i] != null) {
                    i += 2;
                } else {
                    return i >> 1;
                }
            }
            return -1;
        }
        while (i < N) {
            if (!value.equals(array[i])) {
                i += 2;
            } else {
                return i >> 1;
            }
        }
        return -1;
    }

    public boolean containsValue(Object value) {
        return indexOfValue(value) >= 0;
    }

    public V get(Object key) {
        int index = indexOfKey(key);
        if (index >= 0) {
            return (V) this.mArray[(index << 1) + 1];
        }
        return null;
    }

    public K keyAt(int index) {
        return (K) this.mArray[index << 1];
    }

    public V valueAt(int index) {
        return (V) this.mArray[(index << 1) + 1];
    }

    public V setValueAt(int index, V value) {
        int index2 = (index << 1) + 1;
        V old = (V) this.mArray[index2];
        this.mArray[index2] = value;
        return old;
    }

    public boolean isEmpty() {
        return this.mSize <= 0;
    }

    public V put(K key, V value) {
        int hash;
        int index;
        int osize = this.mSize;
        if (key == null) {
            hash = 0;
            index = indexOfNull();
        } else {
            hash = key.hashCode();
            index = indexOf(key, hash);
        }
        if (index >= 0) {
            int index2 = (index << 1) + 1;
            V old = (V) this.mArray[index2];
            this.mArray[index2] = value;
            return old;
        }
        int index3 = ~index;
        if (osize >= this.mHashes.length) {
            int n = 4;
            if (osize >= 8) {
                n = (osize >> 1) + osize;
            } else if (osize >= 4) {
                n = 8;
            }
            int[] ohashes = this.mHashes;
            Object[] oarray = this.mArray;
            allocArrays(n);
            if (osize != this.mSize) {
                throw new ConcurrentModificationException();
            }
            if (this.mHashes.length > 0) {
                System.arraycopy(ohashes, 0, this.mHashes, 0, ohashes.length);
                System.arraycopy(oarray, 0, this.mArray, 0, oarray.length);
            }
            freeArrays(ohashes, oarray, osize);
        }
        if (index3 < osize) {
            System.arraycopy(this.mHashes, index3, this.mHashes, index3 + 1, osize - index3);
            System.arraycopy(this.mArray, index3 << 1, this.mArray, (index3 + 1) << 1, (this.mSize - index3) << 1);
        }
        if (osize != this.mSize || index3 >= this.mHashes.length) {
            throw new ConcurrentModificationException();
        }
        this.mHashes[index3] = hash;
        this.mArray[index3 << 1] = key;
        this.mArray[(index3 << 1) + 1] = value;
        this.mSize++;
        return null;
    }

    public V remove(Object key) {
        int index = indexOfKey(key);
        if (index >= 0) {
            return removeAt(index);
        }
        return null;
    }

    public V removeAt(int index) {
        int nsize;
        V v = (V) this.mArray[(index << 1) + 1];
        int osize = this.mSize;
        if (osize <= 1) {
            freeArrays(this.mHashes, this.mArray, osize);
            this.mHashes = ContainerHelpers.EMPTY_INTS;
            this.mArray = ContainerHelpers.EMPTY_OBJECTS;
            nsize = 0;
        } else {
            int nsize2 = osize - 1;
            if (this.mHashes.length > 8 && this.mSize < this.mHashes.length / 3) {
                int n = osize > 8 ? osize + (osize >> 1) : 8;
                int[] ohashes = this.mHashes;
                Object[] oarray = this.mArray;
                allocArrays(n);
                if (osize != this.mSize) {
                    throw new ConcurrentModificationException();
                }
                if (index > 0) {
                    System.arraycopy(ohashes, 0, this.mHashes, 0, index);
                    System.arraycopy(oarray, 0, this.mArray, 0, index << 1);
                }
                if (index < nsize2) {
                    System.arraycopy(ohashes, index + 1, this.mHashes, index, nsize2 - index);
                    System.arraycopy(oarray, (index + 1) << 1, this.mArray, index << 1, (nsize2 - index) << 1);
                }
            } else {
                if (index < nsize2) {
                    System.arraycopy(this.mHashes, index + 1, this.mHashes, index, nsize2 - index);
                    System.arraycopy(this.mArray, (index + 1) << 1, this.mArray, index << 1, (nsize2 - index) << 1);
                }
                this.mArray[nsize2 << 1] = null;
                this.mArray[(nsize2 << 1) + 1] = null;
            }
            nsize = nsize2;
        }
        if (osize != this.mSize) {
            throw new ConcurrentModificationException();
        }
        this.mSize = nsize;
        return v;
    }

    public int size() {
        return this.mSize;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof SimpleArrayMap) {
            SimpleArrayMap<?, ?> map = (SimpleArrayMap) object;
            if (size() != map.size()) {
                return false;
            }
            for (int i = 0; i < this.mSize; i++) {
                try {
                    K key = keyAt(i);
                    V mine = valueAt(i);
                    Object theirs = map.get(key);
                    if (mine == null) {
                        if (theirs != null || !map.containsKey(key)) {
                            return false;
                        }
                    } else if (!mine.equals(theirs)) {
                        return false;
                    }
                } catch (ClassCastException e) {
                    return false;
                } catch (NullPointerException e2) {
                    return false;
                }
            }
            return true;
        } else if (object instanceof Map) {
            Map<?, ?> map2 = (Map) object;
            if (size() != map2.size()) {
                return false;
            }
            for (int i2 = 0; i2 < this.mSize; i2++) {
                try {
                    K key2 = keyAt(i2);
                    V mine2 = valueAt(i2);
                    Object theirs2 = map2.get(key2);
                    if (mine2 == null) {
                        if (theirs2 != null || !map2.containsKey(key2)) {
                            return false;
                        }
                    } else if (!mine2.equals(theirs2)) {
                        return false;
                    }
                } catch (ClassCastException e3) {
                    return false;
                } catch (NullPointerException e4) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        int[] hashes = this.mHashes;
        Object[] array = this.mArray;
        int result = 0;
        int i = 0;
        int v = 1;
        int s = this.mSize;
        while (i < s) {
            Object value = array[v];
            result += hashes[i] ^ (value == null ? 0 : value.hashCode());
            i++;
            v += 2;
        }
        return result;
    }

    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder(this.mSize * 28);
        buffer.append('{');
        for (int i = 0; i < this.mSize; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            Object key = keyAt(i);
            if (key != this) {
                buffer.append(key);
            } else {
                buffer.append("(this Map)");
            }
            buffer.append('=');
            Object value = valueAt(i);
            if (value != this) {
                buffer.append(value);
            } else {
                buffer.append("(this Map)");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }
}
