package java.util.concurrent.atomic;

import java.io.Serializable;
import sun.misc.Unsafe;

public class AtomicLongArray implements Serializable {
    private static final long serialVersionUID = -2308431214976778248L;
    private static final int shift;
    private final long[] array;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final int base = unsafe.arrayBaseOffset(long[].class);

    static {
        int scale = unsafe.arrayIndexScale(long[].class);
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

    public AtomicLongArray(int length) {
        this.array = new long[length];
    }

    public AtomicLongArray(long[] array) {
        this.array = (long[]) array.clone();
    }

    public final int length() {
        return this.array.length;
    }

    public final long get(int i) {
        return getRaw(checkedByteOffset(i));
    }

    private long getRaw(long offset) {
        return unsafe.getLongVolatile(this.array, offset);
    }

    public final void set(int i, long newValue) {
        unsafe.putLongVolatile(this.array, checkedByteOffset(i), newValue);
    }

    public final void lazySet(int i, long newValue) {
        unsafe.putOrderedLong(this.array, checkedByteOffset(i), newValue);
    }

    public final long getAndSet(int i, long newValue) {
        long current;
        long offset = checkedByteOffset(i);
        do {
            current = getRaw(offset);
        } while (!compareAndSetRaw(offset, current, newValue));
        return current;
    }

    public final boolean compareAndSet(int i, long expect, long update) {
        return compareAndSetRaw(checkedByteOffset(i), expect, update);
    }

    private boolean compareAndSetRaw(long offset, long expect, long update) {
        return unsafe.compareAndSwapLong(this.array, offset, expect, update);
    }

    public final boolean weakCompareAndSet(int i, long expect, long update) {
        return compareAndSet(i, expect, update);
    }

    public final long getAndIncrement(int i) {
        return getAndAdd(i, 1L);
    }

    public final long getAndDecrement(int i) {
        return getAndAdd(i, -1L);
    }

    public final long getAndAdd(int i, long delta) {
        long current;
        long offset = checkedByteOffset(i);
        do {
            current = getRaw(offset);
        } while (!compareAndSetRaw(offset, current, current + delta));
        return current;
    }

    public final long incrementAndGet(int i) {
        return addAndGet(i, 1L);
    }

    public final long decrementAndGet(int i) {
        return addAndGet(i, -1L);
    }

    public long addAndGet(int i, long delta) {
        long current;
        long next;
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
