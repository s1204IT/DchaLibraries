package java.util.concurrent.atomic;

import java.io.Serializable;
import sun.misc.Unsafe;

public class AtomicIntegerArray implements Serializable {
    private static final long serialVersionUID = 2862133569453604235L;
    private static final int shift;
    private final int[] array;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final int base = unsafe.arrayBaseOffset(int[].class);

    static {
        int scale = unsafe.arrayIndexScale(int[].class);
        if (((scale - 1) & scale) != 0) {
            throw new Error("data type scale not a power of two");
        }
        shift = 31 - Integer.numberOfLeadingZeros(scale);
    }

    private long checkedByteOffset(int i) {
        if (i < 0 || i >= this.array.length) {
            throw new IndexOutOfBoundsException("index " + i);
        }
        return byteOffset(i);
    }

    private static long byteOffset(int i) {
        return (((long) i) << shift) + ((long) base);
    }

    public AtomicIntegerArray(int length) {
        this.array = new int[length];
    }

    public AtomicIntegerArray(int[] array) {
        this.array = (int[]) array.clone();
    }

    public final int length() {
        return this.array.length;
    }

    public final int get(int i) {
        return getRaw(checkedByteOffset(i));
    }

    private int getRaw(long offset) {
        return unsafe.getIntVolatile(this.array, offset);
    }

    public final void set(int i, int newValue) {
        unsafe.putIntVolatile(this.array, checkedByteOffset(i), newValue);
    }

    public final void lazySet(int i, int newValue) {
        unsafe.putOrderedInt(this.array, checkedByteOffset(i), newValue);
    }

    public final int getAndSet(int i, int newValue) {
        int current;
        long offset = checkedByteOffset(i);
        do {
            current = getRaw(offset);
        } while (!compareAndSetRaw(offset, current, newValue));
        return current;
    }

    public final boolean compareAndSet(int i, int expect, int update) {
        return compareAndSetRaw(checkedByteOffset(i), expect, update);
    }

    private boolean compareAndSetRaw(long offset, int expect, int update) {
        return unsafe.compareAndSwapInt(this.array, offset, expect, update);
    }

    public final boolean weakCompareAndSet(int i, int expect, int update) {
        return compareAndSet(i, expect, update);
    }

    public final int getAndIncrement(int i) {
        return getAndAdd(i, 1);
    }

    public final int getAndDecrement(int i) {
        return getAndAdd(i, -1);
    }

    public final int getAndAdd(int i, int delta) {
        int current;
        long offset = checkedByteOffset(i);
        do {
            current = getRaw(offset);
        } while (!compareAndSetRaw(offset, current, current + delta));
        return current;
    }

    public final int incrementAndGet(int i) {
        return addAndGet(i, 1);
    }

    public final int decrementAndGet(int i) {
        return addAndGet(i, -1);
    }

    public final int addAndGet(int i, int delta) {
        int current;
        int next;
        long offset = checkedByteOffset(i);
        do {
            current = getRaw(offset);
            next = current + delta;
        } while (!compareAndSetRaw(offset, current, next));
        return next;
    }

    public String toString() {
        int iMax = this.array.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(getRaw(byteOffset(i)));
            if (i == iMax) {
                return b.append(']').toString();
            }
            b.append(',').append(' ');
            i++;
        }
    }
}
