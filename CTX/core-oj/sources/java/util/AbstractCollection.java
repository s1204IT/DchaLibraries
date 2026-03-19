package java.util;

import java.lang.reflect.Array;

public abstract class AbstractCollection<E> implements Collection<E> {
    private static final int MAX_ARRAY_SIZE = 2147483639;

    @Override
    public abstract Iterator<E> iterator();

    @Override
    public abstract int size();

    protected AbstractCollection() {
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object obj) {
        Iterator<E> it = iterator();
        if (obj == null) {
            while (it.hasNext()) {
                if (it.next() == null) {
                    return true;
                }
            }
            return false;
        }
        while (it.hasNext()) {
            if (obj.equals(it.next())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object[] toArray() {
        Object[] objArr = new Object[size()];
        Iterator<E> it = iterator();
        for (int i = 0; i < objArr.length; i++) {
            if (!it.hasNext()) {
                return Arrays.copyOf(objArr, i);
            }
            objArr[i] = it.next();
        }
        return it.hasNext() ? finishToArray(objArr, it) : objArr;
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        int size = size();
        Object[] objArr = tArr.length < size ? (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), size)) : tArr;
        Iterator<E> it = iterator();
        for (int i = 0; i < objArr.length; i++) {
            if (!it.hasNext()) {
                if (tArr == objArr) {
                    objArr[i] = null;
                } else {
                    if (tArr.length < i) {
                        return (T[]) Arrays.copyOf(objArr, i);
                    }
                    System.arraycopy(objArr, 0, tArr, 0, i);
                    if (tArr.length > i) {
                        tArr[i] = null;
                    }
                }
                return tArr;
            }
            objArr[i] = it.next();
        }
        return it.hasNext() ? (T[]) finishToArray(objArr, it) : (T[]) objArr;
    }

    private static <T> T[] finishToArray(T[] tArr, Iterator<?> it) {
        int length = tArr.length;
        while (it.hasNext()) {
            int length2 = tArr.length;
            if (length == length2) {
                int iHugeCapacity = (length2 >> 1) + length2 + 1;
                if (iHugeCapacity - MAX_ARRAY_SIZE > 0) {
                    iHugeCapacity = hugeCapacity(length2 + 1);
                }
                tArr = (T[]) Arrays.copyOf(tArr, iHugeCapacity);
            }
            tArr[length] = it.next();
            length++;
        }
        return length == tArr.length ? tArr : (T[]) Arrays.copyOf(tArr, length);
    }

    private static int hugeCapacity(int i) {
        if (i < 0) {
            throw new OutOfMemoryError("Required array size too large");
        }
        if (i <= MAX_ARRAY_SIZE) {
            return MAX_ARRAY_SIZE;
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object obj) {
        Iterator<E> it = iterator();
        if (obj == null) {
            while (it.hasNext()) {
                if (it.next() == null) {
                    it.remove();
                    return true;
                }
            }
            return false;
        }
        while (it.hasNext()) {
            if (obj.equals(it.next())) {
                it.remove();
                return true;
            }
        }
        return false;
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
        Iterator<? extends E> it = collection.iterator();
        boolean z = false;
        while (it.hasNext()) {
            if (add(it.next())) {
                z = true;
            }
        }
        return z;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        Objects.requireNonNull(collection);
        Iterator<E> it = iterator();
        boolean z = false;
        while (it.hasNext()) {
            if (collection.contains(it.next())) {
                it.remove();
                z = true;
            }
        }
        return z;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        Objects.requireNonNull(collection);
        Iterator<E> it = iterator();
        boolean z = false;
        while (it.hasNext()) {
            if (!collection.contains(it.next())) {
                it.remove();
                z = true;
            }
        }
        return z;
    }

    @Override
    public void clear() {
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    public String toString() {
        Iterator<E> it = iterator();
        if (!it.hasNext()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        while (true) {
            Object next = it.next();
            if (next == this) {
                next = "(this Collection)";
            }
            sb.append(next);
            if (!it.hasNext()) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(',');
            sb.append(' ');
        }
    }
}
