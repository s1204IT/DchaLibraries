package com.google.protobuf;

import com.google.protobuf.Internal;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.RandomAccess;

final class LongArrayList extends AbstractProtobufList<Long> implements Internal.LongList, RandomAccess {
    private static final LongArrayList EMPTY_LIST = new LongArrayList();
    private long[] array;
    private int size;

    static {
        EMPTY_LIST.makeImmutable();
    }

    public static LongArrayList emptyList() {
        return EMPTY_LIST;
    }

    LongArrayList() {
        this(new long[10], 0);
    }

    private LongArrayList(long[] jArr, int i) {
        this.array = jArr;
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
        LongArrayList longArrayList = (LongArrayList) obj;
        if (this.size != longArrayList.size) {
            return false;
        }
        long[] jArr = longArrayList.array;
        for (int i = 0; i < this.size; i++) {
            if (this.array[i] != jArr[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int iHashLong = 1;
        for (int i = 0; i < this.size; i++) {
            iHashLong = Internal.hashLong(this.array[i]) + (31 * iHashLong);
        }
        return iHashLong;
    }

    @Override
    public Internal.ProtobufList<Long> mutableCopyWithCapacity2(int i) {
        if (i < this.size) {
            throw new IllegalArgumentException();
        }
        return new LongArrayList(Arrays.copyOf(this.array, i), this.size);
    }

    @Override
    public Long get(int i) {
        return Long.valueOf(getLong(i));
    }

    @Override
    public long getLong(int i) {
        ensureIndexInRange(i);
        return this.array[i];
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Long set(int i, Long l) {
        return Long.valueOf(setLong(i, l.longValue()));
    }

    @Override
    public long setLong(int i, long j) {
        ensureIsMutable();
        ensureIndexInRange(i);
        long j2 = this.array[i];
        this.array[i] = j;
        return j2;
    }

    @Override
    public void add(int i, Long l) {
        addLong(i, l.longValue());
    }

    @Override
    public void addLong(long j) {
        addLong(this.size, j);
    }

    private void addLong(int i, long j) {
        ensureIsMutable();
        if (i < 0 || i > this.size) {
            throw new IndexOutOfBoundsException(makeOutOfBoundsExceptionMessage(i));
        }
        if (this.size < this.array.length) {
            System.arraycopy(this.array, i, this.array, i + 1, this.size - i);
        } else {
            long[] jArr = new long[((this.size * 3) / 2) + 1];
            System.arraycopy(this.array, 0, jArr, 0, i);
            System.arraycopy(this.array, i, jArr, i + 1, this.size - i);
            this.array = jArr;
        }
        this.array[i] = j;
        this.size++;
        ((AbstractList) this).modCount++;
    }

    @Override
    public boolean addAll(Collection<? extends Long> collection) {
        ensureIsMutable();
        if (collection == 0) {
            throw new NullPointerException();
        }
        if (!(collection instanceof LongArrayList)) {
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
            if (obj.equals(Long.valueOf(this.array[i]))) {
                System.arraycopy(this.array, i + 1, this.array, i, this.size - i);
                this.size--;
                ((AbstractList) this).modCount++;
                return true;
            }
        }
        return false;
    }

    @Override
    public Long remove(int i) {
        ensureIsMutable();
        ensureIndexInRange(i);
        long j = this.array[i];
        System.arraycopy(this.array, i + 1, this.array, i, this.size - i);
        this.size--;
        ((AbstractList) this).modCount++;
        return Long.valueOf(j);
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
