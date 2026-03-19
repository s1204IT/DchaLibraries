package java.util.stream;

import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.SpinedBuffer;
import java.util.stream.Stream;

final class Streams {
    static final Object NONE = new Object();

    private Streams() {
        throw new Error("no instances");
    }

    static final class RangeIntSpliterator implements Spliterator.OfInt {
        private static final int BALANCED_SPLIT_THRESHOLD = 16777216;
        private static final int RIGHT_BALANCED_SPLIT_RATIO = 8;
        private int from;
        private int last;
        private final int upTo;

        RangeIntSpliterator(int i, int i2, boolean z) {
            this(i, i2, z ? 1 : 0);
        }

        private RangeIntSpliterator(int i, int i2, int i3) {
            this.from = i;
            this.upTo = i2;
            this.last = i3;
        }

        @Override
        public boolean tryAdvance(IntConsumer intConsumer) {
            Objects.requireNonNull(intConsumer);
            int i = this.from;
            if (i < this.upTo) {
                this.from++;
                intConsumer.accept(i);
                return true;
            }
            if (this.last <= 0) {
                return false;
            }
            this.last = 0;
            intConsumer.accept(i);
            return true;
        }

        @Override
        public void forEachRemaining(IntConsumer intConsumer) {
            Objects.requireNonNull(intConsumer);
            int i = this.from;
            int i2 = this.upTo;
            int i3 = this.last;
            this.from = this.upTo;
            this.last = 0;
            while (i < i2) {
                intConsumer.accept(i);
                i++;
            }
            if (i3 > 0) {
                intConsumer.accept(i);
            }
        }

        @Override
        public long estimateSize() {
            return (((long) this.upTo) - ((long) this.from)) + ((long) this.last);
        }

        @Override
        public int characteristics() {
            return 17749;
        }

        @Override
        public Comparator<? super Integer> getComparator() {
            return null;
        }

        @Override
        public Spliterator.OfInt trySplit() {
            long jEstimateSize = estimateSize();
            if (jEstimateSize <= 1) {
                return null;
            }
            int i = this.from;
            int iSplitPoint = this.from + splitPoint(jEstimateSize);
            this.from = iSplitPoint;
            return new RangeIntSpliterator(i, iSplitPoint, 0);
        }

        private int splitPoint(long j) {
            return (int) (j / ((long) (j < 16777216 ? 2 : 8)));
        }
    }

    static final class RangeLongSpliterator implements Spliterator.OfLong {
        static final boolean $assertionsDisabled = false;
        private static final long BALANCED_SPLIT_THRESHOLD = 16777216;
        private static final long RIGHT_BALANCED_SPLIT_RATIO = 8;
        private long from;
        private int last;
        private final long upTo;

        RangeLongSpliterator(long j, long j2, boolean z) {
            this(j, j2, z ? 1 : 0);
        }

        private RangeLongSpliterator(long j, long j2, int i) {
            this.from = j;
            this.upTo = j2;
            this.last = i;
        }

        @Override
        public boolean tryAdvance(LongConsumer longConsumer) {
            Objects.requireNonNull(longConsumer);
            long j = this.from;
            if (j < this.upTo) {
                this.from++;
                longConsumer.accept(j);
                return true;
            }
            if (this.last <= 0) {
                return $assertionsDisabled;
            }
            this.last = 0;
            longConsumer.accept(j);
            return true;
        }

        @Override
        public void forEachRemaining(LongConsumer longConsumer) {
            Objects.requireNonNull(longConsumer);
            long j = this.from;
            long j2 = this.upTo;
            int i = this.last;
            this.from = this.upTo;
            this.last = 0;
            while (j < j2) {
                longConsumer.accept(j);
                j = 1 + j;
            }
            if (i > 0) {
                longConsumer.accept(j);
            }
        }

        @Override
        public long estimateSize() {
            return (this.upTo - this.from) + ((long) this.last);
        }

        @Override
        public int characteristics() {
            return 17749;
        }

        @Override
        public Comparator<? super Long> getComparator() {
            return null;
        }

        @Override
        public Spliterator.OfLong trySplit() {
            long jEstimateSize = estimateSize();
            if (jEstimateSize <= 1) {
                return null;
            }
            long j = this.from;
            long jSplitPoint = this.from + splitPoint(jEstimateSize);
            this.from = jSplitPoint;
            return new RangeLongSpliterator(j, jSplitPoint, 0);
        }

        private long splitPoint(long j) {
            return j / (j < BALANCED_SPLIT_THRESHOLD ? 2L : RIGHT_BALANCED_SPLIT_RATIO);
        }
    }

    private static abstract class AbstractStreamBuilderImpl<T, S extends Spliterator<T>> implements Spliterator<T> {
        int count;

        private AbstractStreamBuilderImpl() {
        }

        @Override
        public S trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return (-this.count) - 1;
        }

        @Override
        public int characteristics() {
            return 17488;
        }
    }

    static final class StreamBuilderImpl<T> extends AbstractStreamBuilderImpl<T, Spliterator<T>> implements Stream.Builder<T> {
        SpinedBuffer<T> buffer;
        T first;

        StreamBuilderImpl() {
            super();
        }

        StreamBuilderImpl(T t) {
            super();
            this.first = t;
            this.count = -2;
        }

        @Override
        public void accept(T t) {
            if (this.count == 0) {
                this.first = t;
                this.count++;
            } else {
                if (this.count > 0) {
                    if (this.buffer == null) {
                        this.buffer = new SpinedBuffer<>();
                        this.buffer.accept(this.first);
                        this.count++;
                    }
                    this.buffer.accept(t);
                    return;
                }
                throw new IllegalStateException();
            }
        }

        @Override
        public Stream.Builder<T> add(T t) {
            accept(t);
            return this;
        }

        @Override
        public Stream<T> build() {
            int i = this.count;
            if (i >= 0) {
                this.count = (-this.count) - 1;
                return i < 2 ? StreamSupport.stream(this, false) : StreamSupport.stream(this.buffer.spliterator(), false);
            }
            throw new IllegalStateException();
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            Objects.requireNonNull(consumer);
            if (this.count == -2) {
                consumer.accept(this.first);
                this.count = -1;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> consumer) {
            Objects.requireNonNull(consumer);
            if (this.count == -2) {
                consumer.accept(this.first);
                this.count = -1;
            }
        }
    }

    static final class IntStreamBuilderImpl extends AbstractStreamBuilderImpl<Integer, Spliterator.OfInt> implements IntStream.Builder, Spliterator.OfInt {
        SpinedBuffer.OfInt buffer;
        int first;

        @Override
        public Spliterator.OfInt trySplit() {
            return (Spliterator.OfInt) super.trySplit();
        }

        @Override
        public Spliterator.OfPrimitive trySplit() {
            return (Spliterator.OfPrimitive) super.trySplit();
        }

        IntStreamBuilderImpl() {
            super();
        }

        IntStreamBuilderImpl(int i) {
            super();
            this.first = i;
            this.count = -2;
        }

        @Override
        public void accept(int i) {
            if (this.count == 0) {
                this.first = i;
                this.count++;
            } else {
                if (this.count > 0) {
                    if (this.buffer == null) {
                        this.buffer = new SpinedBuffer.OfInt();
                        this.buffer.accept(this.first);
                        this.count++;
                    }
                    this.buffer.accept(i);
                    return;
                }
                throw new IllegalStateException();
            }
        }

        @Override
        public IntStream build() {
            int i = this.count;
            if (i >= 0) {
                this.count = (-this.count) - 1;
                return i < 2 ? StreamSupport.intStream(this, false) : StreamSupport.intStream(this.buffer.spliterator(), false);
            }
            throw new IllegalStateException();
        }

        @Override
        public boolean tryAdvance(IntConsumer intConsumer) {
            Objects.requireNonNull(intConsumer);
            if (this.count == -2) {
                intConsumer.accept(this.first);
                this.count = -1;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(IntConsumer intConsumer) {
            Objects.requireNonNull(intConsumer);
            if (this.count == -2) {
                intConsumer.accept(this.first);
                this.count = -1;
            }
        }
    }

    static final class LongStreamBuilderImpl extends AbstractStreamBuilderImpl<Long, Spliterator.OfLong> implements LongStream.Builder, Spliterator.OfLong {
        SpinedBuffer.OfLong buffer;
        long first;

        @Override
        public Spliterator.OfLong trySplit() {
            return (Spliterator.OfLong) super.trySplit();
        }

        @Override
        public Spliterator.OfPrimitive trySplit() {
            return (Spliterator.OfPrimitive) super.trySplit();
        }

        LongStreamBuilderImpl() {
            super();
        }

        LongStreamBuilderImpl(long j) {
            super();
            this.first = j;
            this.count = -2;
        }

        @Override
        public void accept(long j) {
            if (this.count == 0) {
                this.first = j;
                this.count++;
            } else {
                if (this.count > 0) {
                    if (this.buffer == null) {
                        this.buffer = new SpinedBuffer.OfLong();
                        this.buffer.accept(this.first);
                        this.count++;
                    }
                    this.buffer.accept(j);
                    return;
                }
                throw new IllegalStateException();
            }
        }

        @Override
        public LongStream build() {
            int i = this.count;
            if (i >= 0) {
                this.count = (-this.count) - 1;
                return i < 2 ? StreamSupport.longStream(this, false) : StreamSupport.longStream(this.buffer.spliterator(), false);
            }
            throw new IllegalStateException();
        }

        @Override
        public boolean tryAdvance(LongConsumer longConsumer) {
            Objects.requireNonNull(longConsumer);
            if (this.count == -2) {
                longConsumer.accept(this.first);
                this.count = -1;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(LongConsumer longConsumer) {
            Objects.requireNonNull(longConsumer);
            if (this.count == -2) {
                longConsumer.accept(this.first);
                this.count = -1;
            }
        }
    }

    static final class DoubleStreamBuilderImpl extends AbstractStreamBuilderImpl<Double, Spliterator.OfDouble> implements DoubleStream.Builder, Spliterator.OfDouble {
        SpinedBuffer.OfDouble buffer;
        double first;

        @Override
        public Spliterator.OfDouble trySplit() {
            return (Spliterator.OfDouble) super.trySplit();
        }

        @Override
        public Spliterator.OfPrimitive trySplit() {
            return (Spliterator.OfPrimitive) super.trySplit();
        }

        DoubleStreamBuilderImpl() {
            super();
        }

        DoubleStreamBuilderImpl(double d) {
            super();
            this.first = d;
            this.count = -2;
        }

        @Override
        public void accept(double d) {
            if (this.count == 0) {
                this.first = d;
                this.count++;
            } else {
                if (this.count > 0) {
                    if (this.buffer == null) {
                        this.buffer = new SpinedBuffer.OfDouble();
                        this.buffer.accept(this.first);
                        this.count++;
                    }
                    this.buffer.accept(d);
                    return;
                }
                throw new IllegalStateException();
            }
        }

        @Override
        public DoubleStream build() {
            int i = this.count;
            if (i >= 0) {
                this.count = (-this.count) - 1;
                return i < 2 ? StreamSupport.doubleStream(this, false) : StreamSupport.doubleStream(this.buffer.spliterator(), false);
            }
            throw new IllegalStateException();
        }

        @Override
        public boolean tryAdvance(DoubleConsumer doubleConsumer) {
            Objects.requireNonNull(doubleConsumer);
            if (this.count == -2) {
                doubleConsumer.accept(this.first);
                this.count = -1;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(DoubleConsumer doubleConsumer) {
            Objects.requireNonNull(doubleConsumer);
            if (this.count == -2) {
                doubleConsumer.accept(this.first);
                this.count = -1;
            }
        }
    }

    static abstract class ConcatSpliterator<T, T_SPLITR extends Spliterator<T>> implements Spliterator<T> {
        protected final T_SPLITR aSpliterator;
        protected final T_SPLITR bSpliterator;
        boolean beforeSplit = true;
        final boolean unsized;

        public ConcatSpliterator(T_SPLITR t_splitr, T_SPLITR t_splitr2) {
            this.aSpliterator = t_splitr;
            this.bSpliterator = t_splitr2;
            this.unsized = t_splitr.estimateSize() + t_splitr2.estimateSize() < 0;
        }

        @Override
        public T_SPLITR trySplit() {
            T_SPLITR t_splitr = this.beforeSplit ? this.aSpliterator : (T_SPLITR) this.bSpliterator.trySplit();
            this.beforeSplit = false;
            return t_splitr;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            if (this.beforeSplit) {
                boolean zTryAdvance = this.aSpliterator.tryAdvance(consumer);
                if (!zTryAdvance) {
                    this.beforeSplit = false;
                    return this.bSpliterator.tryAdvance(consumer);
                }
                return zTryAdvance;
            }
            return this.bSpliterator.tryAdvance(consumer);
        }

        @Override
        public void forEachRemaining(Consumer<? super T> consumer) {
            if (this.beforeSplit) {
                this.aSpliterator.forEachRemaining(consumer);
            }
            this.bSpliterator.forEachRemaining(consumer);
        }

        @Override
        public long estimateSize() {
            if (this.beforeSplit) {
                long jEstimateSize = this.aSpliterator.estimateSize() + this.bSpliterator.estimateSize();
                return jEstimateSize >= 0 ? jEstimateSize : Long.MAX_VALUE;
            }
            return this.bSpliterator.estimateSize();
        }

        @Override
        public int characteristics() {
            if (this.beforeSplit) {
                return this.aSpliterator.characteristics() & this.bSpliterator.characteristics() & (~(5 | (this.unsized ? 16448 : 0)));
            }
            return this.bSpliterator.characteristics();
        }

        @Override
        public Comparator<? super T> getComparator() {
            if (this.beforeSplit) {
                throw new IllegalStateException();
            }
            return this.bSpliterator.getComparator();
        }

        static class OfRef<T> extends ConcatSpliterator<T, Spliterator<T>> {
            OfRef(Spliterator<T> spliterator, Spliterator<T> spliterator2) {
                super(spliterator, spliterator2);
            }
        }

        private static abstract class OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends ConcatSpliterator<T, T_SPLITR> implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            @Override
            public Spliterator.OfPrimitive trySplit() {
                return (Spliterator.OfPrimitive) super.trySplit();
            }

            private OfPrimitive(T_SPLITR t_splitr, T_SPLITR t_splitr2) {
                super(t_splitr, t_splitr2);
            }

            @Override
            public boolean tryAdvance(T_CONS t_cons) {
                if (this.beforeSplit) {
                    boolean zTryAdvance = ((Spliterator.OfPrimitive) this.aSpliterator).tryAdvance(t_cons);
                    if (!zTryAdvance) {
                        this.beforeSplit = false;
                        return ((Spliterator.OfPrimitive) this.bSpliterator).tryAdvance(t_cons);
                    }
                    return zTryAdvance;
                }
                return ((Spliterator.OfPrimitive) this.bSpliterator).tryAdvance(t_cons);
            }

            @Override
            public void forEachRemaining(T_CONS t_cons) {
                if (this.beforeSplit) {
                    ((Spliterator.OfPrimitive) this.aSpliterator).forEachRemaining(t_cons);
                }
                ((Spliterator.OfPrimitive) this.bSpliterator).forEachRemaining(t_cons);
            }
        }

        static class OfInt extends OfPrimitive<Integer, IntConsumer, Spliterator.OfInt> implements Spliterator.OfInt {
            @Override
            public void forEachRemaining(IntConsumer intConsumer) {
                super.forEachRemaining(intConsumer);
            }

            @Override
            public boolean tryAdvance(IntConsumer intConsumer) {
                return super.tryAdvance(intConsumer);
            }

            @Override
            public Spliterator.OfInt trySplit() {
                return (Spliterator.OfInt) super.trySplit();
            }

            OfInt(Spliterator.OfInt ofInt, Spliterator.OfInt ofInt2) {
                super(ofInt, ofInt2);
            }
        }

        static class OfLong extends OfPrimitive<Long, LongConsumer, Spliterator.OfLong> implements Spliterator.OfLong {
            @Override
            public void forEachRemaining(LongConsumer longConsumer) {
                super.forEachRemaining(longConsumer);
            }

            @Override
            public boolean tryAdvance(LongConsumer longConsumer) {
                return super.tryAdvance(longConsumer);
            }

            @Override
            public Spliterator.OfLong trySplit() {
                return (Spliterator.OfLong) super.trySplit();
            }

            OfLong(Spliterator.OfLong ofLong, Spliterator.OfLong ofLong2) {
                super(ofLong, ofLong2);
            }
        }

        static class OfDouble extends OfPrimitive<Double, DoubleConsumer, Spliterator.OfDouble> implements Spliterator.OfDouble {
            @Override
            public void forEachRemaining(DoubleConsumer doubleConsumer) {
                super.forEachRemaining(doubleConsumer);
            }

            @Override
            public boolean tryAdvance(DoubleConsumer doubleConsumer) {
                return super.tryAdvance(doubleConsumer);
            }

            @Override
            public Spliterator.OfDouble trySplit() {
                return (Spliterator.OfDouble) super.trySplit();
            }

            OfDouble(Spliterator.OfDouble ofDouble, Spliterator.OfDouble ofDouble2) {
                super(ofDouble, ofDouble2);
            }
        }
    }

    static Runnable composeWithExceptions(final Runnable runnable, final Runnable runnable2) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                    runnable2.run();
                } catch (Throwable th) {
                    try {
                        runnable2.run();
                    } catch (Throwable th2) {
                        try {
                            th.addSuppressed(th2);
                        } catch (Throwable th3) {
                        }
                    }
                    throw th;
                }
            }
        };
    }

    static Runnable composedClose(final BaseStream<?, ?> baseStream, final BaseStream<?, ?> baseStream2) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    baseStream.close();
                    baseStream2.close();
                } catch (Throwable th) {
                    try {
                        baseStream2.close();
                    } catch (Throwable th2) {
                        try {
                            th.addSuppressed(th2);
                        } catch (Throwable th3) {
                        }
                    }
                    throw th;
                }
            }
        };
    }
}
