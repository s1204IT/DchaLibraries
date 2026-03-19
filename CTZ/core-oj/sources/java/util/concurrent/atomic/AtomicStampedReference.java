package java.util.concurrent.atomic;

import sun.misc.Unsafe;

public class AtomicStampedReference<V> {
    private static final long PAIR;
    private static final Unsafe U = Unsafe.getUnsafe();
    private volatile Pair<V> pair;

    private static class Pair<T> {
        final T reference;
        final int stamp;

        private Pair(T t, int i) {
            this.reference = t;
            this.stamp = i;
        }

        static <T> Pair<T> of(T t, int i) {
            return new Pair<>(t, i);
        }
    }

    public AtomicStampedReference(V v, int i) {
        this.pair = Pair.of(v, i);
    }

    public V getReference() {
        return this.pair.reference;
    }

    public int getStamp() {
        return this.pair.stamp;
    }

    public V get(int[] iArr) {
        Pair<V> pair = this.pair;
        iArr[0] = pair.stamp;
        return pair.reference;
    }

    public boolean weakCompareAndSet(V v, V v2, int i, int i2) {
        return compareAndSet(v, v2, i, i2);
    }

    public boolean compareAndSet(V v, V v2, int i, int i2) {
        Pair<V> pair = this.pair;
        return v == pair.reference && i == pair.stamp && ((v2 == pair.reference && i2 == pair.stamp) || casPair(pair, Pair.of(v2, i2)));
    }

    public void set(V v, int i) {
        Pair<V> pair = this.pair;
        if (v != pair.reference || i != pair.stamp) {
            this.pair = Pair.of(v, i);
        }
    }

    public boolean attemptStamp(V v, int i) {
        Pair<V> pair = this.pair;
        return v == pair.reference && (i == pair.stamp || casPair(pair, Pair.of(v, i)));
    }

    static {
        try {
            PAIR = U.objectFieldOffset(AtomicStampedReference.class.getDeclaredField("pair"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private boolean casPair(Pair<V> pair, Pair<V> pair2) {
        return U.compareAndSwapObject(this, PAIR, pair, pair2);
    }
}
