package java.util.concurrent.atomic;

import sun.misc.Unsafe;

public class AtomicMarkableReference<V> {
    private static final long PAIR;
    private static final Unsafe U = Unsafe.getUnsafe();
    private volatile Pair<V> pair;

    private static class Pair<T> {
        final boolean mark;
        final T reference;

        private Pair(T t, boolean z) {
            this.reference = t;
            this.mark = z;
        }

        static <T> Pair<T> of(T t, boolean z) {
            return new Pair<>(t, z);
        }
    }

    public AtomicMarkableReference(V v, boolean z) {
        this.pair = Pair.of(v, z);
    }

    public V getReference() {
        return this.pair.reference;
    }

    public boolean isMarked() {
        return this.pair.mark;
    }

    public V get(boolean[] zArr) {
        Pair<V> pair = this.pair;
        zArr[0] = pair.mark;
        return pair.reference;
    }

    public boolean weakCompareAndSet(V v, V v2, boolean z, boolean z2) {
        return compareAndSet(v, v2, z, z2);
    }

    public boolean compareAndSet(V v, V v2, boolean z, boolean z2) {
        Pair<V> pair = this.pair;
        return v == pair.reference && z == pair.mark && ((v2 == pair.reference && z2 == pair.mark) || casPair(pair, Pair.of(v2, z2)));
    }

    public void set(V v, boolean z) {
        Pair<V> pair = this.pair;
        if (v != pair.reference || z != pair.mark) {
            this.pair = Pair.of(v, z);
        }
    }

    public boolean attemptMark(V v, boolean z) {
        Pair<V> pair = this.pair;
        return v == pair.reference && (z == pair.mark || casPair(pair, Pair.of(v, z)));
    }

    static {
        try {
            PAIR = U.objectFieldOffset(AtomicMarkableReference.class.getDeclaredField("pair"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private boolean casPair(Pair<V> pair, Pair<V> pair2) {
        return U.compareAndSwapObject(this, PAIR, pair, pair2);
    }
}
