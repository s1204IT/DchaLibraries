package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public interface PrimitiveIterator<T, T_CONS> extends Iterator<T> {
    void forEachRemaining(T_CONS t_cons);

    public interface OfInt extends PrimitiveIterator<Integer, IntConsumer> {
        int nextInt();

        @Override
        default void forEachRemaining(IntConsumer intConsumer) {
            Objects.requireNonNull(intConsumer);
            while (hasNext()) {
                intConsumer.accept(nextInt());
            }
        }

        @Override
        default Integer next() {
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfInt.nextInt()");
            }
            return Integer.valueOf(nextInt());
        }

        @Override
        default void forEachRemaining(Consumer<? super Integer> consumer) {
            if (consumer instanceof IntConsumer) {
                forEachRemaining((IntConsumer) consumer);
                return;
            }
            Objects.requireNonNull(consumer);
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfInt.forEachRemainingInt(action::accept)");
            }
            Objects.requireNonNull(consumer);
            forEachRemaining((IntConsumer) new $$Lambda$E08DiBhfezKzcLFK72WvmuOUJs(consumer));
        }
    }

    public interface OfLong extends PrimitiveIterator<Long, LongConsumer> {
        long nextLong();

        @Override
        default void forEachRemaining(LongConsumer longConsumer) {
            Objects.requireNonNull(longConsumer);
            while (hasNext()) {
                longConsumer.accept(nextLong());
            }
        }

        @Override
        default Long next() {
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfLong.nextLong()");
            }
            return Long.valueOf(nextLong());
        }

        @Override
        default void forEachRemaining(Consumer<? super Long> consumer) {
            if (consumer instanceof LongConsumer) {
                forEachRemaining((LongConsumer) consumer);
                return;
            }
            Objects.requireNonNull(consumer);
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfLong.forEachRemainingLong(action::accept)");
            }
            Objects.requireNonNull(consumer);
            forEachRemaining((LongConsumer) new $$Lambda$9llQTmDvC2fDrGds5d6BexJH00(consumer));
        }
    }

    public interface OfDouble extends PrimitiveIterator<Double, DoubleConsumer> {
        double nextDouble();

        @Override
        default void forEachRemaining(DoubleConsumer doubleConsumer) {
            Objects.requireNonNull(doubleConsumer);
            while (hasNext()) {
                doubleConsumer.accept(nextDouble());
            }
        }

        @Override
        default Double next() {
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfDouble.nextLong()");
            }
            return Double.valueOf(nextDouble());
        }

        @Override
        default void forEachRemaining(Consumer<? super Double> consumer) {
            if (consumer instanceof DoubleConsumer) {
                forEachRemaining((DoubleConsumer) consumer);
                return;
            }
            Objects.requireNonNull(consumer);
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfDouble.forEachRemainingDouble(action::accept)");
            }
            Objects.requireNonNull(consumer);
            forEachRemaining((DoubleConsumer) new $$Lambda$2CyTD4Tuo1NS84gjxzFA3u1LWl0(consumer));
        }
    }
}
