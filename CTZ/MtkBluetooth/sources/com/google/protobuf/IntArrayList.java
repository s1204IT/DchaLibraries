package com.google.protobuf;

import com.google.protobuf.Internal;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.RandomAccess;

final class IntArrayList extends AbstractProtobufList<Integer> implements Internal.IntList, RandomAccess {
    private static final IntArrayList EMPTY_LIST = new IntArrayList();
    private int[] array;
    private int size;

    static {
        EMPTY_LIST.makeImmutable();
    }

    public static IntArrayList emptyList() {
        return EMPTY_LIST;
    }

    IntArrayList() {
        this(new int[10], 0);
    }

    private IntArrayList(int[] iArr, int i) {
        this.array = iArr;
        this.size = i;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IntArrayList)) {
            return super.equals(obj);
        }
        if (this.size != obj.size) {
            return false;
        }
        int[] iArr = obj.array;
        for (int i = 0; i < this.size; i++) {
            if (this.array[i] != iArr[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int i = 1;
        for (int i2 = 0; i2 < this.size; i2++) {
            i = this.array[i2] + (31 * i);
        }
        return i;
    }

    @Override
    public Internal.ProtobufList<Integer> mutableCopyWithCapacity2(int i) {
        if (i < this.size) {
            throw new IllegalArgumentException();
        }
        return new IntArrayList(Arrays.copyOf(this.array, i), this.size);
    }

    @Override
    public Integer get(int i) {
        return Integer.valueOf(getInt(i));
    }

    @Override
    public int getInt(int i) {
        ensureIndexInRange(i);
        return this.array[i];
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Integer set(int i, Integer num) {
        return Integer.valueOf(setInt(i, num.intValue()));
    }

    @Override
    public int setInt(int i, int i2) {
        ensureIsMutable();
        ensureIndexInRange(i);
        int i3 = this.array[i];
        this.array[i] = i2;
        return i3;
    }

    @Override
    public void add(int i, Integer num) {
        addInt(i, num.intValue());
    }

    @Override
    public void addInt(int i) {
        addInt(this.size, i);
    }

    private void addInt(int i, int i2) {
        ensureIsMutable();
        if (i < 0 || i > this.size) {
            throw new IndexOutOfBoundsException(makeOutOfBoundsExceptionMessage(i));
        }
        if (this.size < this.array.length) {
            System.arraycopy(this.array, i, this.array, i + 1, this.size - i);
        } else {
            int[] iArr = new int[((this.size * 3) / 2) + 1];
            System.arraycopy(this.array, 0, iArr, 0, i);
            System.arraycopy(this.array, i, iArr, i + 1, this.size - i);
            this.array = iArr;
        }
        this.array[i] = i2;
        this.size++;
        ((AbstractList) this).modCount++;
    }

    @Override
    public boolean addAll(Collection<? extends Integer> collection) {
        ensureIsMutable();
        if (collection == 0) {
            throw new NullPointerException();
        }
        if (!(collection instanceof IntArrayList)) {
            return super.addAll(collection);
        }
        if (collection.size == 0) {
            return false;
        }
        if (Integer.MAX_VALUE - this.size < collection.size) {
            throw new OutOfMemoryError();
        }
        int i = this.size + collection.size;
        if (i > this.array.length) {
            this.array = Arrays.copyOf(this.array, i);
        }
        System.arraycopy(collection.array, 0, this.array, this.size, collection.size);
        this.size = i;
        ((AbstractList) this).modCount++;
        return true;
    }

    @Override
    public boolean remove(Object obj) {
        ensureIsMutable();
        for (int i = 0; i < this.size; i++) {
            if (obj.equals(Integer.valueOf(this.array[i]))) {
                System.arraycopy(this.array, i + 1, this.array, i, this.size - i);
                this.size--;
                ((AbstractList) this).modCount++;
                return true;
            }
        }
        return false;
    }

    @Override
    public Integer remove(int i) {
        ensureIsMutable();
        ensureIndexInRange(i);
        int i2 = this.array[i];
        System.arraycopy(this.array, i + 1, this.array, i, this.size - i);
        this.size--;
        ((AbstractList) this).modCount++;
        return Integer.valueOf(i2);
    }

    private void ensureIndexInRange(int i) {
        if (i < 0 || i >= this.size) {
            throw new IndexOutOfBoundsException(makeOutOfBoundsExceptionMessage(i));
        }
    }

    private String makeOutOfBoundsExceptionMessage(int i) {
        return "Index:" + i + ", Size:" + this.size;
    }
}
