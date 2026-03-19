package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;
import sun.misc.Unsafe;

public class AtomicLongArray implements Serializable {
    private static final int ASHIFT;
    private static final long serialVersionUID = -2308431214976778248L;
    private final long[] array;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final int ABASE = U.arrayBaseOffset(long[].class);

    static {
        int iArrayIndexScale = U.arrayIndexScale(long[].class);
        if (((iArrayIndexScale - 1) & iArrayIndexScale) != 0) {
            throw new Error("array index scale not a power of two");
        }
        ASHIFT = 31 - Integer.numberOfLeadingZeros(iArrayIndexScale);
    }

    private long checkedByteOffset(int i) {
        if (i < 0 || i >= this.array.length) {
            throw new IndexOutOfBoundsException("index " + i);
        }
        return byteOffset(i);
    }

    private static long byteOffset(int i) {
        return (((long) i) << ASHIFT) + ((long) ABASE);
    }

    public AtomicLongArray(int i) {
        this.array = new long[i];
    }

    public AtomicLongArray(long[] jArr) {
        this.array = (long[]) jArr.clone();
    }

    public final int length() {
        return this.array.length;
    }

    public final long get(int i) {
        return getRaw(checkedByteOffset(i));
    }

    private long getRaw(long j) {
        return U.getLongVolatile(this.array, j);
    }

    public final void set(int i, long j) {
        U.putLongVolatile(this.array, checkedByteOffset(i), j);
    }

    public final void lazySet(int i, long j) {
        U.putOrderedLong(this.array, checkedByteOffset(i), j);
    }

    public final long getAndSet(int i, long j) {
        return U.getAndSetLong(this.array, checkedByteOffset(i), j);
    }

    public final boolean compareAndSet(int i, long j, long j2) {
        return compareAndSetRaw(checkedByteOffset(i), j, j2);
    }

    private boolean compareAndSetRaw(long j, long j2, long j3) {
        return U.compareAndSwapLong(this.array, j, j2, j3);
    }

    public final boolean weakCompareAndSet(int i, long j, long j2) {
        return compareAndSet(i, j, j2);
    }

    public final long getAndIncrement(int i) {
        return getAndAdd(i, 1L);
    }

    public final long getAndDecrement(int i) {
        return getAndAdd(i, -1L);
    }

    public final long getAndAdd(int i, long j) {
        return U.getAndAddLong(this.array, checkedByteOffset(i), j);
    }

    public final long incrementAndGet(int i) {
        return getAndAdd(i, 1L) + 1;
    }

    public final long decrementAndGet(int i) {
        return getAndAdd(i, -1L) - 1;
    }

    public long addAndGet(int i, long j) {
        return getAndAdd(i, j) + j;
    }

    public final long getAndUpdate(int i, LongUnaryOperator longUnaryOperator) {
        long raw;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, longUnaryOperator.applyAsLong(raw)));
        return raw;
    }

    public final long updateAndGet(int i, LongUnaryOperator longUnaryOperator) {
        long raw;
        long jApplyAsLong;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
            jApplyAsLong = longUnaryOperator.applyAsLong(raw);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, jApplyAsLong));
        return jApplyAsLong;
    }

    public final long getAndAccumulate(int i, long j, LongBinaryOperator longBinaryOperator) {
        long raw;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, longBinaryOperator.applyAsLong(raw, j)));
        return raw;
    }

    public final long accumulateAndGet(int i, long j, LongBinaryOperator longBinaryOperator) {
        long raw;
        long jApplyAsLong;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
            jApplyAsLong = longBinaryOperator.applyAsLong(raw, j);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, jApplyAsLong));
        return jApplyAsLong;
    }

    public String toString() {
        int length = this.array.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append(getRaw(byteOffset(i)));
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(',');
            sb.append(' ');
            i++;
        }
    }
}
