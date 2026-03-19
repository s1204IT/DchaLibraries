package com.google.protobuf;

import com.google.protobuf.Internal;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.RandomAccess;

final class BooleanArrayList extends AbstractProtobufList<Boolean> implements Internal.BooleanList, RandomAccess {
    private static final BooleanArrayList EMPTY_LIST = new BooleanArrayList();
    private boolean[] array;
    private int size;

    static {
        EMPTY_LIST.makeImmutable();
    }

    public static BooleanArrayList emptyList() {
        return EMPTY_LIST;
    }

    BooleanArrayList() {
        this(new boolean[10], 0);
    }

    private BooleanArrayList(boolean[] zArr, int i) {
        this.array = zArr;
        this.size = i;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BooleanArrayList)) {
            return super.equals(obj);
        }
        if (this.size != obj.size) {
            return false;
        }
        boolean[] zArr = obj.array;
        for (int i = 0; i < this.size; i++) {
            if (this.array[i] != zArr[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int iHashBoolean = 1;
        for (int i = 0; i < this.size; i++) {
            iHashBoolean = Internal.hashBoolean(this.array[i]) + (31 * iHashBoolean);
        }
        return iHashBoolean;
    }

    @Override
    public Internal.ProtobufList<Boolean> mutableCopyWithCapacity2(int i) {
        if (i < this.size) {
            throw new IllegalArgumentException();
        }
        return new BooleanArrayList(Arrays.copyOf(this.array, i), this.size);
    }

    @Override
    public Boolean get(int i) {
        return Boolean.valueOf(getBoolean(i));
    }

    @Override
    public boolean getBoolean(int i) {
        ensureIndexInRange(i);
        return this.array[i];
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Boolean set(int i, Boolean bool) {
        return Boolean.valueOf(setBoolean(i, bool.booleanValue()));
    }

    @Override
    public boolean setBoolean(int i, boolean z) {
        ensureIsMutable();
        ensureIndexInRange(i);
        boolean z2 = this.array[i];
        this.array[i] = z;
        return z2;
    }

    @Override
    public void add(int i, Boolean bool) {
        addBoolean(i, bool.booleanValue());
    }

    @Override
    public void addBoolean(boolean z) {
        addBoolean(this.size, z);
    }

    private void addBoolean(int i, boolean z) {
        ensureIsMutable();
        if (i < 0 || i > this.size) {
            throw new IndexOutOfBoundsException(makeOutOfBoundsExceptionMessage(i));
        }
        if (this.size < this.array.length) {
            System.arraycopy(this.array, i, this.array, i + 1, this.size - i);
        } else {
            boolean[] zArr = new boolean[((this.size * 3) / 2) + 1];
            System.arraycopy(this.array, 0, zArr, 0, i);
            System.arraycopy(this.array, i, zArr, i + 1, this.size - i);
            this.array = zArr;
        }
        this.array[i] = z;
        this.size++;
        ((AbstractList) this).modCount++;
    }

    @Override
    public boolean addAll(Collection<? extends Boolean> collection) {
        ensureIsMutable();
        if (collection == 0) {
            throw new NullPointerException();
        }
        if (!(collection instanceof BooleanArrayList)) {
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
            if (obj.equals(Boolean.valueOf(this.array[i]))) {
                System.arraycopy(this.array, i + 1, this.array, i, this.size - i);
                this.size--;
                ((AbstractList) this).modCount++;
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean remove(int i) {
        ensureIsMutable();
        ensureIndexInRange(i);
        boolean z = this.array[i];
        System.arraycopy(this.array, i + 1, this.array, i, this.size - i);
        this.size--;
        ((AbstractList) this).modCount++;
        return Boolean.valueOf(z);
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
