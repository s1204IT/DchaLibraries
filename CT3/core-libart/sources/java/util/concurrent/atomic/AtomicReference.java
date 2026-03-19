package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import sun.misc.Unsafe;

public class AtomicReference<V> implements Serializable {
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long VALUE;
    private static final long serialVersionUID = -1848883965231344442L;
    private volatile V value;

    static {
        try {
            VALUE = U.objectFieldOffset(AtomicReference.class.getDeclaredField("value"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    public AtomicReference(V initialValue) {
        this.value = initialValue;
    }

    public AtomicReference() {
    }

    public final V get() {
        return this.value;
    }

    public final void set(V newValue) {
        this.value = newValue;
    }

    public final void lazySet(V newValue) {
        U.putOrderedObject(this, VALUE, newValue);
    }

    public final boolean compareAndSet(V expect, V update) {
        return U.compareAndSwapObject(this, VALUE, expect, update);
    }

    public final boolean weakCompareAndSet(V expect, V update) {
        return U.compareAndSwapObject(this, VALUE, expect, update);
    }

    public final V getAndSet(V v) {
        return (V) U.getAndSetObject(this, VALUE, v);
    }

    public final V getAndUpdate(UnaryOperator<V> unaryOperator) {
        V v;
        do {
            v = (V) get();
        } while (!compareAndSet(v, unaryOperator.apply(v)));
        return v;
    }

    public final V updateAndGet(UnaryOperator<V> unaryOperator) {
        V v;
        V v2;
        do {
            v = get();
            v2 = (V) unaryOperator.apply(v);
        } while (!compareAndSet(v, v2));
        return v2;
    }

    public final V getAndAccumulate(V v, BinaryOperator<V> binaryOperator) {
        V v2;
        do {
            v2 = (V) get();
        } while (!compareAndSet(v2, binaryOperator.apply(v2, v)));
        return v2;
    }

    public final V accumulateAndGet(V v, BinaryOperator<V> binaryOperator) {
        V v2;
        V v3;
        do {
            v2 = get();
            v3 = (V) binaryOperator.apply(v2, v);
        } while (!compareAndSet(v2, v3));
        return v3;
    }

    public String toString() {
        return String.valueOf(get());
    }
}
