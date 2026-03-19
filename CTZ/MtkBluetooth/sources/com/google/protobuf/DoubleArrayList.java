package com.google.protobuf;

import com.google.protobuf.Internal;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.RandomAccess;

final class DoubleArrayList extends AbstractProtobufList<Double> implements Internal.DoubleList, RandomAccess {
    private static final DoubleArrayList EMPTY_LIST = new DoubleArrayList();
    private double[] array;
    private int size;

    static {
        EMPTY_LIST.makeImmutable();
    }

    public static DoubleArrayList emptyList() {
        return EMPTY_LIST;
    }

    DoubleArrayList() {
        this(new double[10], 0);
    }

    private DoubleArrayList(double[] dArr, int i) {
        this.array = dArr;
        this.size = i;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DoubleArrayList)) {
            return super.equals(obj);
        }
        if (this.size != obj.size) {
            return false;
        }
        double[] dArr = obj.array;
        for (int i = 0; i < this.size; i++) {
            if (this.array[i] != dArr[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int iHashLong = 1;
        for (int i = 0; i < this.size; i++) {
            iHashLong = Internal.hashLong(Double.doubleToLongBits(this.array[i])) + (31 * iHashLong);
        }
        return iHashLong;
    }

    @Override
    public Internal.ProtobufList<Double> mutableCopyWithCapacity2(int i) {
        if (i < this.size) {
            throw new IllegalArgumentException();
        }
        return new DoubleArrayList(Arrays.copyOf(this.array, i), this.size);
    }

    @Override
    public Double get(int i) {
        return Double.valueOf(getDouble(i));
    }

    @Override
    public double getDouble(int i) {
        ensureIndexInRange(i);
        return this.array[i];
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Double set(int i, Double d) {
        return Double.valueOf(setDouble(i, d.doubleValue()));
    }

    @Override
    public double setDouble(int i, double d) {
        ensureIsMutable();
        ensureIndexInRange(i);
        double d2 = this.array[i];
        this.array[i] = d;
        return d2;
    }

    @Override
    public void add(int i, Double d) {
        addDouble(i, d.doubleValue());
    }

    @Override
    public void addDouble(double d) {
        addDouble(this.size, d);
    }

    private void addDouble(int i, double d) {
        ensureIsMutable();
        if (i < 0 || i > this.size) {
            throw new IndexOutOfBoundsException(makeOutOfBoundsExceptionMessage(i));
        }
        if (this.size < this.array.length) {
            System.arraycopy(this.array, i, this.array, i + 1, this.size - i);
        } else {
            double[] dArr = new double[((this.size * 3) / 2) + 1];
            System.arraycopy(this.array, 0, dArr, 0, i);
            System.arraycopy(this.array, i, dArr, i + 1, this.size - i);
            this.array = dArr;
        }
        this.array[i] = d;
        this.size++;
        ((AbstractList) this).modCount++;
    }

    @Override
    public boolean addAll(Collection<? extends Double> collection) {
        ensureIsMutable();
        if (collection == 0) {
            throw new NullPointerException();
        }
        if (!(collection instanceof DoubleArrayList)) {
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
            if (obj.equals(Double.valueOf(this.array[i]))) {
                System.arraycopy(this.array, i + 1, this.array, i, this.size - i);
                this.size--;
                ((AbstractList) this).modCount++;
                return true;
            }
        }
        return false;
    }

    @Override
    public Double remove(int i) {
        ensureIsMutable();
        ensureIndexInRange(i);
        double d = this.array[i];
        System.arraycopy(this.array, i + 1, this.array, i, this.size - i);
        this.size--;
        ((AbstractList) this).modCount++;
        return Double.valueOf(d);
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
