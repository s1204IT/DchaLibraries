package com.google.protobuf;

import com.google.protobuf.Internal;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.RandomAccess;

final class FloatArrayList extends AbstractProtobufList<Float> implements Internal.FloatList, RandomAccess {
    private static final FloatArrayList EMPTY_LIST = new FloatArrayList();
    private float[] array;
    private int size;

    static {
        EMPTY_LIST.makeImmutable();
    }

    public static FloatArrayList emptyList() {
        return EMPTY_LIST;
    }

    FloatArrayList() {
        this(new float[10], 0);
    }

    private FloatArrayList(float[] fArr, int i) {
        this.array = fArr;
        this.size = i;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FloatArrayList)) {
            return super.equals(obj);
        }
        if (this.size != obj.size) {
            return false;
        }
        float[] fArr = obj.array;
        for (int i = 0; i < this.size; i++) {
            if (this.array[i] != fArr[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int iFloatToIntBits = 1;
        for (int i = 0; i < this.size; i++) {
            iFloatToIntBits = Float.floatToIntBits(this.array[i]) + (31 * iFloatToIntBits);
        }
        return iFloatToIntBits;
    }

    @Override
    public Internal.ProtobufList<Float> mutableCopyWithCapacity2(int i) {
        if (i < this.size) {
            throw new IllegalArgumentException();
        }
        return new FloatArrayList(Arrays.copyOf(this.array, i), this.size);
    }

    @Override
    public Float get(int i) {
        return Float.valueOf(getFloat(i));
    }

    @Override
    public float getFloat(int i) {
        ensureIndexInRange(i);
        return this.array[i];
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Float set(int i, Float f) {
        return Float.valueOf(setFloat(i, f.floatValue()));
    }

    @Override
    public float setFloat(int i, float f) {
        ensureIsMutable();
        ensureIndexInRange(i);
        float f2 = this.array[i];
        this.array[i] = f;
        return f2;
    }

    @Override
    public void add(int i, Float f) {
        addFloat(i, f.floatValue());
    }

    @Override
    public void addFloat(float f) {
        addFloat(this.size, f);
    }

    private void addFloat(int i, float f) {
        ensureIsMutable();
        if (i < 0 || i > this.size) {
            throw new IndexOutOfBoundsException(makeOutOfBoundsExceptionMessage(i));
        }
        if (this.size < this.array.length) {
            System.arraycopy(this.array, i, this.array, i + 1, this.size - i);
        } else {
            float[] fArr = new float[((this.size * 3) / 2) + 1];
            System.arraycopy(this.array, 0, fArr, 0, i);
            System.arraycopy(this.array, i, fArr, i + 1, this.size - i);
            this.array = fArr;
        }
        this.array[i] = f;
        this.size++;
        ((AbstractList) this).modCount++;
    }

    @Override
    public boolean addAll(Collection<? extends Float> collection) {
        ensureIsMutable();
        if (collection == 0) {
            throw new NullPointerException();
        }
        if (!(collection instanceof FloatArrayList)) {
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
            if (obj.equals(Float.valueOf(this.array[i]))) {
                System.arraycopy(this.array, i + 1, this.array, i, this.size - i);
                this.size--;
                ((AbstractList) this).modCount++;
                return true;
            }
        }
        return false;
    }

    @Override
    public Float remove(int i) {
        ensureIsMutable();
        ensureIndexInRange(i);
        float f = this.array[i];
        System.arraycopy(this.array, i + 1, this.array, i, this.size - i);
        this.size--;
        ((AbstractList) this).modCount++;
        return Float.valueOf(f);
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
