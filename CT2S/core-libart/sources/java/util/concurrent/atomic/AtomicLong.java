package java.util.concurrent.atomic;

import java.io.Serializable;
import sun.misc.Unsafe;

public class AtomicLong extends Number implements Serializable {
    private static final long serialVersionUID = 1927816293512124184L;
    private static final long valueOffset;
    private volatile long value;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    static final boolean VM_SUPPORTS_LONG_CAS = VMSupportsCS8();

    private static native boolean VMSupportsCS8();

    static {
        try {
            valueOffset = unsafe.objectFieldOffset(AtomicLong.class.getDeclaredField("value"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public AtomicLong(long initialValue) {
        this.value = initialValue;
    }

    public AtomicLong() {
    }

    public final long get() {
        return this.value;
    }

    public final void set(long newValue) {
        this.value = newValue;
    }

    public final void lazySet(long newValue) {
        unsafe.putOrderedLong(this, valueOffset, newValue);
    }

    public final long getAndSet(long newValue) {
        long current;
        do {
            current = get();
        } while (!compareAndSet(current, newValue));
        return current;
    }

    public final boolean compareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
    }

    public final boolean weakCompareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
    }

    public final long getAndIncrement() {
        long current;
        long next;
        do {
            current = get();
            next = current + 1;
        } while (!compareAndSet(current, next));
        return current;
    }

    public final long getAndDecrement() {
        long current;
        long next;
        do {
            current = get();
            next = current - 1;
        } while (!compareAndSet(current, next));
        return current;
    }

    public final long getAndAdd(long delta) {
        long current;
        long next;
        do {
            current = get();
            next = current + delta;
        } while (!compareAndSet(current, next));
        return current;
    }

    public final long incrementAndGet() {
        long current;
        long next;
        do {
            current = get();
            next = current + 1;
        } while (!compareAndSet(current, next));
        return next;
    }

    public final long decrementAndGet() {
        long current;
        long next;
        do {
            current = get();
            next = current - 1;
        } while (!compareAndSet(current, next));
        return next;
    }

    public final long addAndGet(long delta) {
        long current;
        long next;
        do {
            current = get();
            next = current + delta;
        } while (!compareAndSet(current, next));
        return next;
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
