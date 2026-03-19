package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public interface Spliterator<T> {
    public static final int CONCURRENT = 4096;
    public static final int DISTINCT = 1;
    public static final int IMMUTABLE = 1024;
    public static final int NONNULL = 256;
    public static final int ORDERED = 16;
    public static final int SIZED = 64;
    public static final int SORTED = 4;
    public static final int SUBSIZED = 16384;

    int characteristics();

    long estimateSize();

    boolean tryAdvance(Consumer<? super T> consumer);

    Spliterator<T> trySplit();

    default void forEachRemaining(Consumer<? super T> consumer) {
        while (tryAdvance(consumer)) {
        }
    }

    default long getExactSizeIfKnown() {
        if ((characteristics() & 64) == 0) {
            return -1L;
        }
        return estimateSize();
    }

    default boolean hasCharacteristics(int i) {
        return (characteristics() & i) == i;
    }

    default Comparator<? super T> getComparator() {
        throw new IllegalStateException();
    }

    public interface OfPrimitive<T, T_CONS, T_SPLITR extends OfPrimitive<T, T_CONS, T_SPLITR>> extends Spliterator<T> {
        boolean tryAdvance(T_CONS t_cons);

        @Override
        T_SPLITR trySplit();

        default void forEachRemaining(T_CONS t_cons) {
            while (tryAdvance(t_cons)) {
            }
        }
    }

    public interface OfInt extends OfPrimitive<Integer, IntConsumer, OfInt> {
        @Override
        boolean tryAdvance(IntConsumer intConsumer);

        @Override
        OfInt trySplit();

        @Override
        default void forEachRemaining(IntConsumer intConsumer) {
            while (tryAdvance(intConsumer)) {
            }
        }

        @Override
        default boolean tryAdvance(Consumer<? super Integer> consumer) {
            if (consumer instanceof IntConsumer) {
                return tryAdvance((IntConsumer) consumer);
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfInt.tryAdvance((IntConsumer) action::accept)");
            }
            Objects.requireNonNull(consumer);
            return tryAdvance((IntConsumer) new $$Lambda$E08DiBhfezKzcLFK72WvmuOUJs(consumer));
        }

        @Override
        default void forEachRemaining(Consumer<? super Integer> consumer) {
            if (consumer instanceof IntConsumer) {
                forEachRemaining((IntConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfInt.forEachRemaining((IntConsumer) action::accept)");
            }
            Objects.requireNonNull(consumer);
            forEachRemaining((IntConsumer) new $$Lambda$E08DiBhfezKzcLFK72WvmuOUJs(consumer));
        }
    }

    public interface OfLong extends OfPrimitive<Long, LongConsumer, OfLong> {
        @Override
        boolean tryAdvance(LongConsumer longConsumer);

        @Override
        OfLong trySplit();

        @Override
        default void forEachRemaining(LongConsumer longConsumer) {
            while (tryAdvance(longConsumer)) {
            }
        }

        @Override
        default boolean tryAdvance(Consumer<? super Long> consumer) {
            if (consumer instanceof LongConsumer) {
                return tryAdvance((LongConsumer) consumer);
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfLong.tryAdvance((LongConsumer) action::accept)");
            }
            Objects.requireNonNull(consumer);
            return tryAdvance((LongConsumer) new $$Lambda$9llQTmDvC2fDrGds5d6BexJH00(consumer));
        }

        @Override
        default void forEachRemaining(Consumer<? super Long> consumer) {
            if (consumer instanceof LongConsumer) {
                forEachRemaining((LongConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfLong.forEachRemaining((LongConsumer) action::accept)");
            }
            Objects.requireNonNull(consumer);
            forEachRemaining((LongConsumer) new $$Lambda$9llQTmDvC2fDrGds5d6BexJH00(consumer));
        }
    }

    public interface OfDouble extends OfPrimitive<Double, DoubleConsumer, OfDouble> {
        @Override
        boolean tryAdvance(DoubleConsumer doubleConsumer);

        @Override
        OfDouble trySplit();

        @Override
        default void forEachRemaining(DoubleConsumer doubleConsumer) {
            while (tryAdvance(doubleConsumer)) {
            }
        }

        @Override
        default boolean tryAdvance(Consumer<? super Double> consumer) {
            if (consumer instanceof DoubleConsumer) {
                return tryAdvance((DoubleConsumer) consumer);
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfDouble.tryAdvance((DoubleConsumer) action::accept)");
            }
            Objects.requireNonNull(consumer);
            return tryAdvance((DoubleConsumer) new $$Lambda$2CyTD4Tuo1NS84gjxzFA3u1LWl0(consumer));
        }

        @Override
        default void forEachRemaining(Consumer<? super Double> consumer) {
            if (consumer instanceof DoubleConsumer) {
                forEachRemaining((DoubleConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfDouble.forEachRemaining((DoubleConsumer) action::accept)");
            }
            Objects.requireNonNull(consumer);
            forEachRemaining((DoubleConsumer) new $$Lambda$2CyTD4Tuo1NS84gjxzFA3u1LWl0(consumer));
        }
    }
}
