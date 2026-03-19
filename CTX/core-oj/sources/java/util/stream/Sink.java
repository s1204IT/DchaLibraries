package java.util.stream;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public interface Sink<T> extends Consumer<T> {
    default void begin(long j) {
    }

    default void end() {
    }

    default boolean cancellationRequested() {
        return false;
    }

    default void accept(int i) {
        throw new IllegalStateException("called wrong accept method");
    }

    default void accept(long j) {
        throw new IllegalStateException("called wrong accept method");
    }

    default void accept(double d) {
        throw new IllegalStateException("called wrong accept method");
    }

    public interface OfInt extends Sink<Integer>, IntConsumer {
        @Override
        void accept(int i);

        @Override
        default void accept(Integer num) {
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Sink.OfInt.accept(Integer)");
            }
            accept(num.intValue());
        }
    }

    public interface OfLong extends Sink<Long>, LongConsumer {
        @Override
        void accept(long j);

        @Override
        default void accept(Long l) {
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Sink.OfLong.accept(Long)");
            }
            accept(l.longValue());
        }
    }

    public interface OfDouble extends Sink<Double>, DoubleConsumer {
        @Override
        void accept(double d);

        @Override
        default void accept(Double d) {
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Sink.OfDouble.accept(Double)");
            }
            accept(d.doubleValue());
        }
    }

    public static abstract class ChainedReference<T, E_OUT> implements Sink<T> {
        protected final Sink<? super E_OUT> downstream;

        public ChainedReference(Sink<? super E_OUT> sink) {
            this.downstream = (Sink) Objects.requireNonNull(sink);
        }

        @Override
        public void begin(long j) {
            this.downstream.begin(j);
        }

        @Override
        public void end() {
            this.downstream.end();
        }

        @Override
        public boolean cancellationRequested() {
            return this.downstream.cancellationRequested();
        }
    }

    public static abstract class ChainedInt<E_OUT> implements OfInt {
        protected final Sink<? super E_OUT> downstream;

        public ChainedInt(Sink<? super E_OUT> sink) {
            this.downstream = (Sink) Objects.requireNonNull(sink);
        }

        @Override
        public void begin(long j) {
            this.downstream.begin(j);
        }

        @Override
        public void end() {
            this.downstream.end();
        }

        @Override
        public boolean cancellationRequested() {
            return this.downstream.cancellationRequested();
        }
    }

    public static abstract class ChainedLong<E_OUT> implements OfLong {
        protected final Sink<? super E_OUT> downstream;

        public ChainedLong(Sink<? super E_OUT> sink) {
            this.downstream = (Sink) Objects.requireNonNull(sink);
        }

        @Override
        public void begin(long j) {
            this.downstream.begin(j);
        }

        @Override
        public void end() {
            this.downstream.end();
        }

        @Override
        public boolean cancellationRequested() {
            return this.downstream.cancellationRequested();
        }
    }

    public static abstract class ChainedDouble<E_OUT> implements OfDouble {
        protected final Sink<? super E_OUT> downstream;

        public ChainedDouble(Sink<? super E_OUT> sink) {
            this.downstream = (Sink) Objects.requireNonNull(sink);
        }

        @Override
        public void begin(long j) {
            this.downstream.begin(j);
        }

        @Override
        public void end() {
            this.downstream.end();
        }

        @Override
        public boolean cancellationRequested() {
            return this.downstream.cancellationRequested();
        }
    }
}
