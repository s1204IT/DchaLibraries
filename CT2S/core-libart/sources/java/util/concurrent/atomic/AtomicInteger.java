package java.util.concurrent.atomic;

import java.io.Serializable;
import sun.misc.Unsafe;

public class AtomicInteger extends Number implements Serializable {
    private static final long serialVersionUID = 6214790243416807050L;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;
    private volatile int value;

    static {
        try {
            valueOffset = unsafe.objectFieldOffset(AtomicInteger.class.getDeclaredField("value"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public AtomicInteger(int initialValue) {
        this.value = initialValue;
    }

    public AtomicInteger() {
    }

    public final int get() {
        return this.value;
    }

    public final void set(int newValue) {
        this.value = newValue;
    }

    public final void lazySet(int newValue) {
        unsafe.putOrderedInt(this, valueOffset, newValue);
    }

    public final int getAndSet(int newValue) {
        int current;
        do {
            current = get();
        } while (!compareAndSet(current, newValue));
        return current;
    }

    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    public final boolean weakCompareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    public final int getAndIncrement() {
        int current;
        int next;
        do {
            current = get();
            next = current + 1;
        } while (!compareAndSet(current, next));
        return current;
    }

    public final int getAndDecrement() {
        int current;
        int next;
        do {
            current = get();
            next = current - 1;
        } while (!compareAndSet(current, next));
        return current;
    }

    public final int getAndAdd(int delta) {
        int current;
        int next;
        do {
            current = get();
            next = current + delta;
        } while (!compareAndSet(current, next));
        return current;
    }

    public final int incrementAndGet() {
        int current;
        int next;
        do {
            current = get();
            next = current + 1;
        } while (!compareAndSet(current, next));
        return next;
    }

    public final int decrementAndGet() {
        int current;
        int next;
        do {
            current = get();
            next = current - 1;
        } while (!compareAndSet(current, next));
        return next;
    }

    public final int addAndGet(int delta) {
        int current;
        int next;
        do {
            current = get();
            next = current + delta;
        } while (!compareAndSet(current, next));
        return next;
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
