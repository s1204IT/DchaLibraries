package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import sun.misc.Unsafe;

public class AtomicIntegerArray implements Serializable {
    private static final int ASHIFT;
    private static final long serialVersionUID = 2862133569453604235L;
    private final int[] array;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final int ABASE = U.arrayBaseOffset(int[].class);

    static {
        int iArrayIndexScale = U.arrayIndexScale(int[].class);
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

    public AtomicIntegerArray(int i) {
        this.array = new int[i];
    }

    public AtomicIntegerArray(int[] iArr) {
        this.array = (int[]) iArr.clone();
    }

    public final int length() {
        return this.array.length;
    }

    public final int get(int i) {
        return getRaw(checkedByteOffset(i));
    }

    private int getRaw(long j) {
        return U.getIntVolatile(this.array, j);
    }

    public final void set(int i, int i2) {
        U.putIntVolatile(this.array, checkedByteOffset(i), i2);
    }

    public final void lazySet(int i, int i2) {
        U.putOrderedInt(this.array, checkedByteOffset(i), i2);
    }

    public final int getAndSet(int i, int i2) {
        return U.getAndSetInt(this.array, checkedByteOffset(i), i2);
    }

    public final boolean compareAndSet(int i, int i2, int i3) {
        return compareAndSetRaw(checkedByteOffset(i), i2, i3);
    }

    private boolean compareAndSetRaw(long j, int i, int i2) {
        return U.compareAndSwapInt(this.array, j, i, i2);
    }

    public final boolean weakCompareAndSet(int i, int i2, int i3) {
        return compareAndSet(i, i2, i3);
    }

    public final int getAndIncrement(int i) {
        return getAndAdd(i, 1);
    }

    public final int getAndDecrement(int i) {
        return getAndAdd(i, -1);
    }

    public final int getAndAdd(int i, int i2) {
        return U.getAndAddInt(this.array, checkedByteOffset(i), i2);
    }

    public final int incrementAndGet(int i) {
        return getAndAdd(i, 1) + 1;
    }

    public final int decrementAndGet(int i) {
        return getAndAdd(i, -1) - 1;
    }

    public final int addAndGet(int i, int i2) {
        return getAndAdd(i, i2) + i2;
    }

    public final int getAndUpdate(int i, IntUnaryOperator intUnaryOperator) {
        int raw;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, intUnaryOperator.applyAsInt(raw)));
        return raw;
    }

    public final int updateAndGet(int i, IntUnaryOperator intUnaryOperator) {
        int raw;
        int iApplyAsInt;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
            iApplyAsInt = intUnaryOperator.applyAsInt(raw);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, iApplyAsInt));
        return iApplyAsInt;
    }

    public final int getAndAccumulate(int i, int i2, IntBinaryOperator intBinaryOperator) {
        int raw;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, intBinaryOperator.applyAsInt(raw, i2)));
        return raw;
    }

    public final int accumulateAndGet(int i, int i2, IntBinaryOperator intBinaryOperator) {
        int raw;
        int iApplyAsInt;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
            iApplyAsInt = intBinaryOperator.applyAsInt(raw, i2);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, iApplyAsInt));
        return iApplyAsInt;
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
