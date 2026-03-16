package java.util;

import java.lang.reflect.Array;

public class UnsafeArrayList<T> extends AbstractList<T> {
    private T[] array;
    private final Class<T> elementType;
    private int size;

    public UnsafeArrayList(Class<T> cls, int i) {
        this.array = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, i));
        this.elementType = cls;
    }

    @Override
    public boolean add(T t) {
        if (this.size == this.array.length) {
            T[] tArr = (T[]) ((Object[]) Array.newInstance((Class<?>) this.elementType, this.size * 2));
            System.arraycopy(this.array, 0, tArr, 0, this.size);
            this.array = tArr;
        }
        T[] tArr2 = this.array;
        int i = this.size;
        this.size = i + 1;
        tArr2[i] = t;
        this.modCount++;
        return true;
    }

    public T[] array() {
        return this.array;
    }

    @Override
    public T get(int i) {
        return this.array[i];
    }

    @Override
    public int size() {
        return this.size;
    }
}
