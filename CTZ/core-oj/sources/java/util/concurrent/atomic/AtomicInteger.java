package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import sun.misc.Unsafe;

public class AtomicInteger extends Number implements Serializable {
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long VALUE;
    private static final long serialVersionUID = 6214790243416807050L;
    private volatile int value;

    static {
        try {
            VALUE = U.objectFieldOffset(AtomicInteger.class.getDeclaredField("value"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    public AtomicInteger(int i) {
        this.value = i;
    }

    public AtomicInteger() {
    }

    public final int get() {
        return this.value;
    }

    public final void set(int i) {
        this.value = i;
    }

    public final void lazySet(int i) {
        U.putOrderedInt(this, VALUE, i);
    }

    public final int getAndSet(int i) {
        return U.getAndSetInt(this, VALUE, i);
    }

    public final boolean compareAndSet(int i, int i2) {
        return U.compareAndSwapInt(this, VALUE, i, i2);
    }

    public final boolean weakCompareAndSet(int i, int i2) {
        return U.compareAndSwapInt(this, VALUE, i, i2);
    }

    public final int getAndIncrement() {
        return U.getAndAddInt(this, VALUE, 1);
    }

    public final int getAndDecrement() {
        return U.getAndAddInt(this, VALUE, -1);
    }

    public final int getAndAdd(int i) {
        return U.getAndAddInt(this, VALUE, i);
    }

    public final int incrementAndGet() {
        return U.getAndAddInt(this, VALUE, 1) + 1;
    }

    public final int decrementAndGet() {
        return U.getAndAddInt(this, VALUE, -1) - 1;
    }

    public final int addAndGet(int i) {
        return U.getAndAddInt(this, VALUE, i) + i;
    }

    public final int getAndUpdate(IntUnaryOperator intUnaryOperator) {
        int i;
        do {
            i = get();
        } while (!compareAndSet(i, intUnaryOperator.applyAsInt(i)));
        return i;
    }

    public final int updateAndGet(IntUnaryOperator intUnaryOperator) {
        int i;
        int iApplyAsInt;
        do {
            i = get();
            iApplyAsInt = intUnaryOperator.applyAsInt(i);
        } while (!compareAndSet(i, iApplyAsInt));
        return iApplyAsInt;
    }

    public final int getAndAccumulate(int i, IntBinaryOperator intBinaryOperator) {
        int i2;
        do {
            i2 = get();
        } while (!compareAndSet(i2, intBinaryOperator.applyAsInt(i2, i)));
        return i2;
    }

    public final int accumulateAndGet(int i, IntBinaryOperator intBinaryOperator) {
        int i2;
        int iApplyAsInt;
        do {
            i2 = get();
            iApplyAsInt = intBinaryOperator.applyAsInt(i2, i);
        } while (!compareAndSet(i2, iApplyAsInt));
        return iApplyAsInt;
    }

    public String toString() {
        return Integer.toString(get());
    }

    @Override
    public int intValue() {
        return get();
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
