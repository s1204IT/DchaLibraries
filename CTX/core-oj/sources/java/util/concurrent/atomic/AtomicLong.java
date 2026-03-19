package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;
import sun.misc.Unsafe;

public class AtomicLong extends Number implements Serializable {
    private static final long VALUE;
    private static final long serialVersionUID = 1927816293512124184L;
    private volatile long value;
    private static final Unsafe U = Unsafe.getUnsafe();
    static final boolean VM_SUPPORTS_LONG_CAS = VMSupportsCS8();

    private static native boolean VMSupportsCS8();

    static {
        try {
            VALUE = U.objectFieldOffset(AtomicLong.class.getDeclaredField("value"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    public AtomicLong(long j) {
        this.value = j;
    }

    public AtomicLong() {
    }

    public final long get() {
        return this.value;
    }

    public final void set(long j) {
        U.putLongVolatile(this, VALUE, j);
    }

    public final void lazySet(long j) {
        U.putOrderedLong(this, VALUE, j);
    }

    public final long getAndSet(long j) {
        return U.getAndSetLong(this, VALUE, j);
    }

    public final boolean compareAndSet(long j, long j2) {
        return U.compareAndSwapLong(this, VALUE, j, j2);
    }

    public final boolean weakCompareAndSet(long j, long j2) {
        return U.compareAndSwapLong(this, VALUE, j, j2);
    }

    public final long getAndIncrement() {
        return U.getAndAddLong(this, VALUE, 1L);
    }

    public final long getAndDecrement() {
        return U.getAndAddLong(this, VALUE, -1L);
    }

    public final long getAndAdd(long j) {
        return U.getAndAddLong(this, VALUE, j);
    }

    public final long incrementAndGet() {
        return U.getAndAddLong(this, VALUE, 1L) + 1;
    }

    public final long decrementAndGet() {
        return U.getAndAddLong(this, VALUE, -1L) - 1;
    }

    public final long addAndGet(long j) {
        return U.getAndAddLong(this, VALUE, j) + j;
    }

    public final long getAndUpdate(LongUnaryOperator longUnaryOperator) {
        long j;
        do {
            j = get();
        } while (!compareAndSet(j, longUnaryOperator.applyAsLong(j)));
        return j;
    }

    public final long updateAndGet(LongUnaryOperator longUnaryOperator) {
        long j;
        long jApplyAsLong;
        do {
            j = get();
            jApplyAsLong = longUnaryOperator.applyAsLong(j);
        } while (!compareAndSet(j, jApplyAsLong));
        return jApplyAsLong;
    }

    public final long getAndAccumulate(long j, LongBinaryOperator longBinaryOperator) {
        long j2;
        do {
            j2 = get();
        } while (!compareAndSet(j2, longBinaryOperator.applyAsLong(j2, j)));
        return j2;
    }

    public final long accumulateAndGet(long j, LongBinaryOperator longBinaryOperator) {
        long j2;
        long jApplyAsLong;
        do {
            j2 = get();
            jApplyAsLong = longBinaryOperator.applyAsLong(j2, j);
        } while (!compareAndSet(j2, jApplyAsLong));
        return jApplyAsLong;
    }

    public String toString() {
        return Long.toString(get());
    }

    @Override
    public int intValue() {
        return (int) get();
    }

    @Override
    public long longValue() {
        return get();
    }

    @Override
    public float floatValue() {
        return get();
    }

    @Override
    public double doubleValue() {
        return get();
    }
}
