package java.util.stream;

import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Sink;
import java.util.stream.SpinedBuffer;
import java.util.stream.StreamSpliterators;

class StreamSpliterators {
    StreamSpliterators() {
    }

    private static abstract class AbstractWrappingSpliterator<P_IN, P_OUT, T_BUFFER extends AbstractSpinedBuffer> implements Spliterator<P_OUT> {
        T_BUFFER buffer;
        Sink<P_IN> bufferSink;
        boolean finished;
        final boolean isParallel;
        long nextToConsume;
        final PipelineHelper<P_OUT> ph;
        BooleanSupplier pusher;
        Spliterator<P_IN> spliterator;
        private Supplier<Spliterator<P_IN>> spliteratorSupplier;

        abstract void initPartialTraversalState();

        abstract AbstractWrappingSpliterator<P_IN, P_OUT, ?> wrap(Spliterator<P_IN> spliterator);

        AbstractWrappingSpliterator(PipelineHelper<P_OUT> pipelineHelper, Supplier<Spliterator<P_IN>> supplier, boolean z) {
            this.ph = pipelineHelper;
            this.spliteratorSupplier = supplier;
            this.spliterator = null;
            this.isParallel = z;
        }

        AbstractWrappingSpliterator(PipelineHelper<P_OUT> pipelineHelper, Spliterator<P_IN> spliterator, boolean z) {
            this.ph = pipelineHelper;
            this.spliteratorSupplier = null;
            this.spliterator = spliterator;
            this.isParallel = z;
        }

        final void init() {
            if (this.spliterator == null) {
                this.spliterator = this.spliteratorSupplier.get();
                this.spliteratorSupplier = null;
            }
        }

        final boolean doAdvance() {
            if (this.buffer == null) {
                if (this.finished) {
                    return false;
                }
                init();
                initPartialTraversalState();
                this.nextToConsume = 0L;
                this.bufferSink.begin(this.spliterator.getExactSizeIfKnown());
                return fillBuffer();
            }
            this.nextToConsume++;
            boolean z = this.nextToConsume < this.buffer.count();
            if (!z) {
                this.nextToConsume = 0L;
                this.buffer.clear();
                return fillBuffer();
            }
            return z;
        }

        @Override
        public Spliterator<P_OUT> trySplit() {
            if (!this.isParallel || this.finished) {
                return null;
            }
            init();
            Spliterator<P_IN> spliteratorTrySplit = this.spliterator.trySplit();
            if (spliteratorTrySplit == null) {
                return null;
            }
            return wrap(spliteratorTrySplit);
        }

        private boolean fillBuffer() {
            while (this.buffer.count() == 0) {
                if (this.bufferSink.cancellationRequested() || !this.pusher.getAsBoolean()) {
                    if (this.finished) {
                        return false;
                    }
                    this.bufferSink.end();
                    this.finished = true;
                }
            }
            return true;
        }

        @Override
        public final long estimateSize() {
            init();
            return this.spliterator.estimateSize();
        }

        @Override
        public final long getExactSizeIfKnown() {
            init();
            if (StreamOpFlag.SIZED.isKnown(this.ph.getStreamAndOpFlags())) {
                return this.spliterator.getExactSizeIfKnown();
            }
            return -1L;
        }

        @Override
        public final int characteristics() {
            init();
            int characteristics = StreamOpFlag.toCharacteristics(StreamOpFlag.toStreamFlags(this.ph.getStreamAndOpFlags()));
            if ((characteristics & 64) != 0) {
                return (characteristics & (-16449)) | (this.spliterator.characteristics() & 16448);
            }
            return characteristics;
        }

        @Override
        public Comparator<? super P_OUT> getComparator() {
            if (!hasCharacteristics(4)) {
                throw new IllegalStateException();
            }
            return null;
        }

        public final String toString() {
            return String.format("%s[%s]", getClass().getName(), this.spliterator);
        }
    }

    static final class WrappingSpliterator<P_IN, P_OUT> extends AbstractWrappingSpliterator<P_IN, P_OUT, SpinedBuffer<P_OUT>> {
        WrappingSpliterator(PipelineHelper<P_OUT> pipelineHelper, Supplier<Spliterator<P_IN>> supplier, boolean z) {
            super(pipelineHelper, supplier, z);
        }

        WrappingSpliterator(PipelineHelper<P_OUT> pipelineHelper, Spliterator<P_IN> spliterator, boolean z) {
            super(pipelineHelper, spliterator, z);
        }

        @Override
        WrappingSpliterator<P_IN, P_OUT> wrap(Spliterator<P_IN> spliterator) {
            return new WrappingSpliterator<>(this.ph, spliterator, this.isParallel);
        }

        @Override
        void initPartialTraversalState() {
            final SpinedBuffer spinedBuffer = new SpinedBuffer();
            this.buffer = spinedBuffer;
            PipelineHelper<P_OUT> pipelineHelper = this.ph;
            Objects.requireNonNull(spinedBuffer);
            this.bufferSink = pipelineHelper.wrapSink(new Sink() {
                @Override
                public final void accept(Object obj) {
                    spinedBuffer.accept(obj);
                }
            });
            this.pusher = new BooleanSupplier() {
                @Override
                public final boolean getAsBoolean() {
                    StreamSpliterators.WrappingSpliterator wrappingSpliterator = this.f$0;
                    return wrappingSpliterator.spliterator.tryAdvance(wrappingSpliterator.bufferSink);
                }
            };
        }

        @Override
        public boolean tryAdvance(Consumer<? super P_OUT> consumer) {
            Objects.requireNonNull(consumer);
            boolean zDoAdvance = doAdvance();
            if (zDoAdvance) {
                consumer.accept((Object) ((SpinedBuffer) this.buffer).get(this.nextToConsume));
            }
            return zDoAdvance;
        }

        @Override
        public void forEachRemaining(final Consumer<? super P_OUT> consumer) {
            if (this.buffer == 0 && !this.finished) {
                Objects.requireNonNull(consumer);
                init();
                PipelineHelper<P_OUT> pipelineHelper = this.ph;
                Objects.requireNonNull(consumer);
                pipelineHelper.wrapAndCopyInto(new Sink() {
                    @Override
                    public final void accept(Object obj) {
                        consumer.accept(obj);
                    }
                }, this.spliterator);
                this.finished = true;
                return;
            }
            while (tryAdvance(consumer)) {
            }
        }
    }

    static final class IntWrappingSpliterator<P_IN> extends AbstractWrappingSpliterator<P_IN, Integer, SpinedBuffer.OfInt> implements Spliterator.OfInt {
        IntWrappingSpliterator(PipelineHelper<Integer> pipelineHelper, Supplier<Spliterator<P_IN>> supplier, boolean z) {
            super(pipelineHelper, supplier, z);
        }

        IntWrappingSpliterator(PipelineHelper<Integer> pipelineHelper, Spliterator<P_IN> spliterator, boolean z) {
            super(pipelineHelper, spliterator, z);
        }

        @Override
        AbstractWrappingSpliterator<P_IN, Integer, ?> wrap(Spliterator<P_IN> spliterator) {
            return new IntWrappingSpliterator((PipelineHelper<Integer>) this.ph, (Spliterator) spliterator, this.isParallel);
        }

        @Override
        void initPartialTraversalState() {
            final SpinedBuffer.OfInt ofInt = new SpinedBuffer.OfInt();
            this.buffer = ofInt;
            PipelineHelper<P_OUT> pipelineHelper = this.ph;
            Objects.requireNonNull(ofInt);
            this.bufferSink = pipelineHelper.wrapSink(new Sink.OfInt() {
                @Override
                public final void accept(int i) {
                    ofInt.accept(i);
                }
            });
            this.pusher = new BooleanSupplier() {
                @Override
                public final boolean getAsBoolean() {
                    StreamSpliterators.IntWrappingSpliterator intWrappingSpliterator = this.f$0;
                    return intWrappingSpliterator.spliterator.tryAdvance(intWrappingSpliterator.bufferSink);
                }
            };
        }

        @Override
        public Spliterator.OfInt trySplit() {
            return (Spliterator.OfInt) super.trySplit();
        }

        @Override
        public boolean tryAdvance(IntConsumer intConsumer) {
            Objects.requireNonNull(intConsumer);
            boolean zDoAdvance = doAdvance();
            if (zDoAdvance) {
                intConsumer.accept(((SpinedBuffer.OfInt) this.buffer).get(this.nextToConsume));
            }
            return zDoAdvance;
        }

        @Override
        public void forEachRemaining(final IntConsumer intConsumer) {
            if (this.buffer == 0 && !this.finished) {
                Objects.requireNonNull(intConsumer);
                init();
                PipelineHelper<P_OUT> pipelineHelper = this.ph;
                Objects.requireNonNull(intConsumer);
                pipelineHelper.wrapAndCopyInto(new Sink.OfInt() {
                    @Override
                    public final void accept(int i) {
                        intConsumer.accept(i);
                    }
                }, this.spliterator);
                this.finished = true;
                return;
            }
            while (tryAdvance(intConsumer)) {
            }
        }
    }

    static final class LongWrappingSpliterator<P_IN> extends AbstractWrappingSpliterator<P_IN, Long, SpinedBuffer.OfLong> implements Spliterator.OfLong {
        LongWrappingSpliterator(PipelineHelper<Long> pipelineHelper, Supplier<Spliterator<P_IN>> supplier, boolean z) {
            super(pipelineHelper, supplier, z);
        }

        LongWrappingSpliterator(PipelineHelper<Long> pipelineHelper, Spliterator<P_IN> spliterator, boolean z) {
            super(pipelineHelper, spliterator, z);
        }

        @Override
        AbstractWrappingSpliterator<P_IN, Long, ?> wrap(Spliterator<P_IN> spliterator) {
            return new LongWrappingSpliterator((PipelineHelper<Long>) this.ph, (Spliterator) spliterator, this.isParallel);
        }

        @Override
        void initPartialTraversalState() {
            final SpinedBuffer.OfLong ofLong = new SpinedBuffer.OfLong();
            this.buffer = ofLong;
            PipelineHelper<P_OUT> pipelineHelper = this.ph;
            Objects.requireNonNull(ofLong);
            this.bufferSink = pipelineHelper.wrapSink(new Sink.OfLong() {
                @Override
                public final void accept(long j) {
                    ofLong.accept(j);
                }
            });
            this.pusher = new BooleanSupplier() {
                @Override
                public final boolean getAsBoolean() {
                    StreamSpliterators.LongWrappingSpliterator longWrappingSpliterator = this.f$0;
                    return longWrappingSpliterator.spliterator.tryAdvance(longWrappingSpliterator.bufferSink);
                }
            };
        }

        @Override
        public Spliterator.OfLong trySplit() {
            return (Spliterator.OfLong) super.trySplit();
        }

        @Override
        public boolean tryAdvance(LongConsumer longConsumer) {
            Objects.requireNonNull(longConsumer);
            boolean zDoAdvance = doAdvance();
            if (zDoAdvance) {
                longConsumer.accept(((SpinedBuffer.OfLong) this.buffer).get(this.nextToConsume));
            }
            return zDoAdvance;
        }

        @Override
        public void forEachRemaining(final LongConsumer longConsumer) {
            if (this.buffer == 0 && !this.finished) {
                Objects.requireNonNull(longConsumer);
                init();
                PipelineHelper<P_OUT> pipelineHelper = this.ph;
                Objects.requireNonNull(longConsumer);
                pipelineHelper.wrapAndCopyInto(new Sink.OfLong() {
                    @Override
                    public final void accept(long j) {
                        longConsumer.accept(j);
                    }
                }, this.spliterator);
                this.finished = true;
                return;
            }
            while (tryAdvance(longConsumer)) {
            }
        }
    }

    static final class DoubleWrappingSpliterator<P_IN> extends AbstractWrappingSpliterator<P_IN, Double, SpinedBuffer.OfDouble> implements Spliterator.OfDouble {
        DoubleWrappingSpliterator(PipelineHelper<Double> pipelineHelper, Supplier<Spliterator<P_IN>> supplier, boolean z) {
            super(pipelineHelper, supplier, z);
        }

        DoubleWrappingSpliterator(PipelineHelper<Double> pipelineHelper, Spliterator<P_IN> spliterator, boolean z) {
            super(pipelineHelper, spliterator, z);
        }

        @Override
        AbstractWrappingSpliterator<P_IN, Double, ?> wrap(Spliterator<P_IN> spliterator) {
            return new DoubleWrappingSpliterator((PipelineHelper<Double>) this.ph, (Spliterator) spliterator, this.isParallel);
        }

        @Override
        void initPartialTraversalState() {
            final SpinedBuffer.OfDouble ofDouble = new SpinedBuffer.OfDouble();
            this.buffer = ofDouble;
            PipelineHelper<P_OUT> pipelineHelper = this.ph;
            Objects.requireNonNull(ofDouble);
            this.bufferSink = pipelineHelper.wrapSink(new Sink.OfDouble() {
                @Override
                public final void accept(double d) {
                    ofDouble.accept(d);
                }
            });
            this.pusher = new BooleanSupplier() {
                @Override
                public final boolean getAsBoolean() {
                    StreamSpliterators.DoubleWrappingSpliterator doubleWrappingSpliterator = this.f$0;
                    return doubleWrappingSpliterator.spliterator.tryAdvance(doubleWrappingSpliterator.bufferSink);
                }
            };
        }

        @Override
        public Spliterator.OfDouble trySplit() {
            return (Spliterator.OfDouble) super.trySplit();
        }

        @Override
        public boolean tryAdvance(DoubleConsumer doubleConsumer) {
            Objects.requireNonNull(doubleConsumer);
            boolean zDoAdvance = doAdvance();
            if (zDoAdvance) {
                doubleConsumer.accept(((SpinedBuffer.OfDouble) this.buffer).get(this.nextToConsume));
            }
            return zDoAdvance;
        }

        @Override
        public void forEachRemaining(final DoubleConsumer doubleConsumer) {
            if (this.buffer == 0 && !this.finished) {
                Objects.requireNonNull(doubleConsumer);
                init();
                PipelineHelper<P_OUT> pipelineHelper = this.ph;
                Objects.requireNonNull(doubleConsumer);
                pipelineHelper.wrapAndCopyInto(new Sink.OfDouble() {
                    @Override
                    public final void accept(double d) {
                        doubleConsumer.accept(d);
                    }
                }, this.spliterator);
                this.finished = true;
                return;
            }
            while (tryAdvance(doubleConsumer)) {
            }
        }
    }

    static class DelegatingSpliterator<T, T_SPLITR extends Spliterator<T>> implements Spliterator<T> {
        private T_SPLITR s;
        private final Supplier<? extends T_SPLITR> supplier;

        DelegatingSpliterator(Supplier<? extends T_SPLITR> supplier) {
            this.supplier = supplier;
        }

        T_SPLITR get() {
            if (this.s == null) {
                this.s = this.supplier.get();
            }
            return this.s;
        }

        @Override
        public T_SPLITR trySplit() {
            return (T_SPLITR) get().trySplit();
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            return get().tryAdvance(consumer);
        }

        @Override
        public void forEachRemaining(Consumer<? super T> consumer) {
            get().forEachRemaining(consumer);
        }

        @Override
        public long estimateSize() {
            return get().estimateSize();
        }

        @Override
        public int characteristics() {
            return get().characteristics();
        }

        @Override
        public Comparator<? super T> getComparator() {
            return get().getComparator();
        }

        @Override
        public long getExactSizeIfKnown() {
            return get().getExactSizeIfKnown();
        }

        public String toString() {
            return getClass().getName() + "[" + ((Object) get()) + "]";
        }

        static class OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends DelegatingSpliterator<T, T_SPLITR> implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            @Override
            public Spliterator.OfPrimitive trySplit() {
                return (Spliterator.OfPrimitive) super.trySplit();
            }

            OfPrimitive(Supplier<? extends T_SPLITR> supplier) {
                super(supplier);
            }

            @Override
            public boolean tryAdvance(T_CONS t_cons) {
                return get().tryAdvance(t_cons);
            }

            @Override
            public void forEachRemaining(T_CONS t_cons) {
                get().forEachRemaining(t_cons);
            }
        }

        static final class OfInt extends OfPrimitive<Integer, IntConsumer, Spliterator.OfInt> implements Spliterator.OfInt {
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

            OfInt(Supplier<Spliterator.OfInt> supplier) {
                super(supplier);
            }
        }

        static final class OfLong extends OfPrimitive<Long, LongConsumer, Spliterator.OfLong> implements Spliterator.OfLong {
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

            OfLong(Supplier<Spliterator.OfLong> supplier) {
                super(supplier);
            }
        }

        static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, Spliterator.OfDouble> implements Spliterator.OfDouble {
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

            OfDouble(Supplier<Spliterator.OfDouble> supplier) {
                super(supplier);
            }
        }
    }

    static abstract class SliceSpliterator<T, T_SPLITR extends Spliterator<T>> {
        static final boolean $assertionsDisabled = false;
        long fence;
        long index;
        T_SPLITR s;
        final long sliceFence;
        final long sliceOrigin;

        protected abstract T_SPLITR makeSpliterator(T_SPLITR t_splitr, long j, long j2, long j3, long j4);

        SliceSpliterator(T_SPLITR t_splitr, long j, long j2, long j3, long j4) {
            this.s = t_splitr;
            this.sliceOrigin = j;
            this.sliceFence = j2;
            this.index = j3;
            this.fence = j4;
        }

        public T_SPLITR trySplit() {
            if (this.sliceOrigin >= this.fence || this.index >= this.fence) {
                return null;
            }
            while (true) {
                T_SPLITR t_splitr = (T_SPLITR) this.s.trySplit();
                if (t_splitr == null) {
                    return null;
                }
                long jEstimateSize = this.index + t_splitr.estimateSize();
                long jMin = Math.min(jEstimateSize, this.sliceFence);
                if (this.sliceOrigin >= jMin) {
                    this.index = jMin;
                } else {
                    if (jMin < this.sliceFence) {
                        if (this.index >= this.sliceOrigin && jEstimateSize <= this.sliceFence) {
                            this.index = jMin;
                            return t_splitr;
                        }
                        long j = this.sliceOrigin;
                        long j2 = this.sliceFence;
                        long j3 = this.index;
                        this.index = jMin;
                        return (T_SPLITR) makeSpliterator(t_splitr, j, j2, j3, jMin);
                    }
                    this.s = t_splitr;
                    this.fence = jMin;
                }
            }
        }

        public long estimateSize() {
            if (this.sliceOrigin < this.fence) {
                return this.fence - Math.max(this.sliceOrigin, this.index);
            }
            return 0L;
        }

        public int characteristics() {
            return this.s.characteristics();
        }

        static final class OfRef<T> extends SliceSpliterator<T, Spliterator<T>> implements Spliterator<T> {
            OfRef(Spliterator<T> spliterator, long j, long j2) {
                this(spliterator, j, j2, 0L, Math.min(spliterator.estimateSize(), j2));
            }

            private OfRef(Spliterator<T> spliterator, long j, long j2, long j3, long j4) {
                super(spliterator, j, j2, j3, j4);
            }

            @Override
            protected Spliterator<T> makeSpliterator(Spliterator<T> spliterator, long j, long j2, long j3, long j4) {
                return new OfRef(spliterator, j, j2, j3, j4);
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                Objects.requireNonNull(consumer);
                if (this.sliceOrigin >= this.fence) {
                    return false;
                }
                while (this.sliceOrigin > this.index) {
                    this.s.tryAdvance(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            StreamSpliterators.SliceSpliterator.OfRef.lambda$tryAdvance$0(obj);
                        }
                    });
                    this.index++;
                }
                if (this.index >= this.fence) {
                    return false;
                }
                this.index++;
                return this.s.tryAdvance(consumer);
            }

            static void lambda$tryAdvance$0(Object obj) {
            }

            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                Objects.requireNonNull(consumer);
                if (this.sliceOrigin < this.fence && this.index < this.fence) {
                    if (this.index >= this.sliceOrigin && this.index + this.s.estimateSize() <= this.sliceFence) {
                        this.s.forEachRemaining(consumer);
                        this.index = this.fence;
                        return;
                    }
                    while (this.sliceOrigin > this.index) {
                        this.s.tryAdvance(new Consumer() {
                            @Override
                            public final void accept(Object obj) {
                                StreamSpliterators.SliceSpliterator.OfRef.lambda$forEachRemaining$1(obj);
                            }
                        });
                        this.index++;
                    }
                    while (this.index < this.fence) {
                        this.s.tryAdvance(consumer);
                        this.index++;
                    }
                }
            }

            static void lambda$forEachRemaining$1(Object obj) {
            }
        }

        static abstract class OfPrimitive<T, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_CONS> extends SliceSpliterator<T, T_SPLITR> implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            protected abstract T_CONS emptyConsumer();

            @Override
            public Spliterator.OfPrimitive trySplit() {
                return (Spliterator.OfPrimitive) super.trySplit();
            }

            OfPrimitive(T_SPLITR t_splitr, long j, long j2) {
                this(t_splitr, j, j2, 0L, Math.min(t_splitr.estimateSize(), j2));
            }

            private OfPrimitive(T_SPLITR t_splitr, long j, long j2, long j3, long j4) {
                super(t_splitr, j, j2, j3, j4);
            }

            @Override
            public boolean tryAdvance(T_CONS t_cons) {
                Objects.requireNonNull(t_cons);
                if (this.sliceOrigin >= this.fence) {
                    return false;
                }
                while (this.sliceOrigin > this.index) {
                    ((Spliterator.OfPrimitive) this.s).tryAdvance(emptyConsumer());
                    this.index++;
                }
                if (this.index >= this.fence) {
                    return false;
                }
                this.index++;
                return ((Spliterator.OfPrimitive) this.s).tryAdvance(t_cons);
            }

            @Override
            public void forEachRemaining(T_CONS t_cons) {
                Objects.requireNonNull(t_cons);
                if (this.sliceOrigin < this.fence && this.index < this.fence) {
                    if (this.index >= this.sliceOrigin && this.index + ((Spliterator.OfPrimitive) this.s).estimateSize() <= this.sliceFence) {
                        ((Spliterator.OfPrimitive) this.s).forEachRemaining(t_cons);
                        this.index = this.fence;
                        return;
                    }
                    while (this.sliceOrigin > this.index) {
                        ((Spliterator.OfPrimitive) this.s).tryAdvance(emptyConsumer());
                        this.index++;
                    }
                    while (this.index < this.fence) {
                        ((Spliterator.OfPrimitive) this.s).tryAdvance(t_cons);
                        this.index++;
                    }
                }
            }
        }

        static final class OfInt extends OfPrimitive<Integer, Spliterator.OfInt, IntConsumer> implements Spliterator.OfInt {
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

            OfInt(Spliterator.OfInt ofInt, long j, long j2) {
                super(ofInt, j, j2);
            }

            OfInt(Spliterator.OfInt ofInt, long j, long j2, long j3, long j4) {
                super(ofInt, j, j2, j3, j4);
            }

            @Override
            protected Spliterator.OfInt makeSpliterator(Spliterator.OfInt ofInt, long j, long j2, long j3, long j4) {
                return new OfInt(ofInt, j, j2, j3, j4);
            }

            static void lambda$emptyConsumer$0(int i) {
            }

            @Override
            protected IntConsumer emptyConsumer() {
                return new IntConsumer() {
                    @Override
                    public final void accept(int i) {
                        StreamSpliterators.SliceSpliterator.OfInt.lambda$emptyConsumer$0(i);
                    }
                };
            }
        }

        static final class OfLong extends OfPrimitive<Long, Spliterator.OfLong, LongConsumer> implements Spliterator.OfLong {
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

            OfLong(Spliterator.OfLong ofLong, long j, long j2) {
                super(ofLong, j, j2);
            }

            OfLong(Spliterator.OfLong ofLong, long j, long j2, long j3, long j4) {
                super(ofLong, j, j2, j3, j4);
            }

            @Override
            protected Spliterator.OfLong makeSpliterator(Spliterator.OfLong ofLong, long j, long j2, long j3, long j4) {
                return new OfLong(ofLong, j, j2, j3, j4);
            }

            static void lambda$emptyConsumer$0(long j) {
            }

            @Override
            protected LongConsumer emptyConsumer() {
                return new LongConsumer() {
                    @Override
                    public final void accept(long j) {
                        StreamSpliterators.SliceSpliterator.OfLong.lambda$emptyConsumer$0(j);
                    }
                };
            }
        }

        static final class OfDouble extends OfPrimitive<Double, Spliterator.OfDouble, DoubleConsumer> implements Spliterator.OfDouble {
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

            OfDouble(Spliterator.OfDouble ofDouble, long j, long j2) {
                super(ofDouble, j, j2);
            }

            OfDouble(Spliterator.OfDouble ofDouble, long j, long j2, long j3, long j4) {
                super(ofDouble, j, j2, j3, j4);
            }

            @Override
            protected Spliterator.OfDouble makeSpliterator(Spliterator.OfDouble ofDouble, long j, long j2, long j3, long j4) {
                return new OfDouble(ofDouble, j, j2, j3, j4);
            }

            static void lambda$emptyConsumer$0(double d) {
            }

            @Override
            protected DoubleConsumer emptyConsumer() {
                return new DoubleConsumer() {
                    @Override
                    public final void accept(double d) {
                        StreamSpliterators.SliceSpliterator.OfDouble.lambda$emptyConsumer$0(d);
                    }
                };
            }
        }
    }

    static abstract class UnorderedSliceSpliterator<T, T_SPLITR extends Spliterator<T>> {
        static final boolean $assertionsDisabled = false;
        static final int CHUNK_SIZE = 128;
        private final AtomicLong permits;
        protected final T_SPLITR s;
        private final long skipThreshold;
        protected final boolean unlimited;

        enum PermitStatus {
            NO_MORE,
            MAYBE_MORE,
            UNLIMITED
        }

        protected abstract T_SPLITR makeSpliterator(T_SPLITR t_splitr);

        UnorderedSliceSpliterator(T_SPLITR t_splitr, long j, long j2) {
            this.s = t_splitr;
            this.unlimited = j2 < 0 ? true : $assertionsDisabled;
            this.skipThreshold = j2 >= 0 ? j2 : 0L;
            this.permits = new AtomicLong(j2 >= 0 ? j + j2 : j);
        }

        UnorderedSliceSpliterator(T_SPLITR t_splitr, UnorderedSliceSpliterator<T, T_SPLITR> unorderedSliceSpliterator) {
            this.s = t_splitr;
            this.unlimited = unorderedSliceSpliterator.unlimited;
            this.permits = unorderedSliceSpliterator.permits;
            this.skipThreshold = unorderedSliceSpliterator.skipThreshold;
        }

        protected final long acquirePermits(long j) {
            long j2;
            long jMin;
            do {
                j2 = this.permits.get();
                if (j2 == 0) {
                    if (this.unlimited) {
                        return j;
                    }
                    return 0L;
                }
                jMin = Math.min(j2, j);
                if (jMin <= 0) {
                    break;
                }
            } while (!this.permits.compareAndSet(j2, j2 - jMin));
            if (this.unlimited) {
                return Math.max(j - jMin, 0L);
            }
            if (j2 > this.skipThreshold) {
                return Math.max(jMin - (j2 - this.skipThreshold), 0L);
            }
            return jMin;
        }

        protected final PermitStatus permitStatus() {
            if (this.permits.get() > 0) {
                return PermitStatus.MAYBE_MORE;
            }
            return this.unlimited ? PermitStatus.UNLIMITED : PermitStatus.NO_MORE;
        }

        public final T_SPLITR trySplit() {
            Spliterator<T> spliteratorTrySplit;
            if (this.permits.get() == 0 || (spliteratorTrySplit = this.s.trySplit()) == null) {
                return null;
            }
            return (T_SPLITR) makeSpliterator(spliteratorTrySplit);
        }

        public final long estimateSize() {
            return this.s.estimateSize();
        }

        public final int characteristics() {
            return this.s.characteristics() & (-16465);
        }

        static final class OfRef<T> extends UnorderedSliceSpliterator<T, Spliterator<T>> implements Spliterator<T>, Consumer<T> {
            T tmpSlot;

            OfRef(Spliterator<T> spliterator, long j, long j2) {
                super(spliterator, j, j2);
            }

            OfRef(Spliterator<T> spliterator, OfRef<T> ofRef) {
                super(spliterator, ofRef);
            }

            @Override
            public final void accept(T t) {
                this.tmpSlot = t;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                Objects.requireNonNull(consumer);
                while (permitStatus() != PermitStatus.NO_MORE && this.s.tryAdvance(this)) {
                    if (acquirePermits(1L) == 1) {
                        consumer.accept(this.tmpSlot);
                        this.tmpSlot = null;
                        return true;
                    }
                }
                return UnorderedSliceSpliterator.$assertionsDisabled;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                Objects.requireNonNull(consumer);
                ArrayBuffer.OfRef ofRef = null;
                while (true) {
                    PermitStatus permitStatus = permitStatus();
                    if (permitStatus != PermitStatus.NO_MORE) {
                        if (permitStatus == PermitStatus.MAYBE_MORE) {
                            if (ofRef == null) {
                                ofRef = new ArrayBuffer.OfRef(128);
                            } else {
                                ofRef.reset();
                            }
                            long j = 0;
                            while (this.s.tryAdvance(ofRef)) {
                                j++;
                                if (j >= 128) {
                                    break;
                                }
                            }
                            if (j == 0) {
                                return;
                            } else {
                                ofRef.forEach(consumer, acquirePermits(j));
                            }
                        } else {
                            this.s.forEachRemaining(consumer);
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }

            @Override
            protected Spliterator<T> makeSpliterator(Spliterator<T> spliterator) {
                return new OfRef(spliterator, this);
            }
        }

        static abstract class OfPrimitive<T, T_CONS, T_BUFF extends ArrayBuffer.OfPrimitive<T_CONS>, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends UnorderedSliceSpliterator<T, T_SPLITR> implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            protected abstract void acceptConsumed(T_CONS t_cons);

            protected abstract T_BUFF bufferCreate(int i);

            @Override
            public Spliterator.OfPrimitive trySplit() {
                return (Spliterator.OfPrimitive) super.trySplit();
            }

            OfPrimitive(T_SPLITR t_splitr, long j, long j2) {
                super(t_splitr, j, j2);
            }

            OfPrimitive(T_SPLITR t_splitr, OfPrimitive<T, T_CONS, T_BUFF, T_SPLITR> ofPrimitive) {
                super(t_splitr, ofPrimitive);
            }

            @Override
            public boolean tryAdvance(T_CONS t_cons) {
                Objects.requireNonNull(t_cons);
                while (permitStatus() != PermitStatus.NO_MORE && ((Spliterator.OfPrimitive) this.s).tryAdvance(this)) {
                    if (acquirePermits(1L) == 1) {
                        acceptConsumed(t_cons);
                        return true;
                    }
                }
                return UnorderedSliceSpliterator.$assertionsDisabled;
            }

            @Override
            public void forEachRemaining(T_CONS t_cons) {
                Objects.requireNonNull(t_cons);
                ArrayBuffer.OfPrimitive ofPrimitiveBufferCreate = null;
                while (true) {
                    PermitStatus permitStatus = permitStatus();
                    if (permitStatus != PermitStatus.NO_MORE) {
                        if (permitStatus == PermitStatus.MAYBE_MORE) {
                            if (ofPrimitiveBufferCreate == null) {
                                ofPrimitiveBufferCreate = bufferCreate(128);
                            } else {
                                ofPrimitiveBufferCreate.reset();
                            }
                            long j = 0;
                            while (((Spliterator.OfPrimitive) this.s).tryAdvance(ofPrimitiveBufferCreate)) {
                                j++;
                                if (j >= 128) {
                                    break;
                                }
                            }
                            if (j == 0) {
                                return;
                            } else {
                                ofPrimitiveBufferCreate.forEach(t_cons, acquirePermits(j));
                            }
                        } else {
                            ((Spliterator.OfPrimitive) this.s).forEachRemaining(t_cons);
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }
        }

        static final class OfInt extends OfPrimitive<Integer, IntConsumer, ArrayBuffer.OfInt, Spliterator.OfInt> implements Spliterator.OfInt, IntConsumer {
            int tmpValue;

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

            OfInt(Spliterator.OfInt ofInt, long j, long j2) {
                super(ofInt, j, j2);
            }

            OfInt(Spliterator.OfInt ofInt, OfInt ofInt2) {
                super(ofInt, ofInt2);
            }

            @Override
            public void accept(int i) {
                this.tmpValue = i;
            }

            @Override
            protected void acceptConsumed(IntConsumer intConsumer) {
                intConsumer.accept(this.tmpValue);
            }

            @Override
            protected ArrayBuffer.OfInt bufferCreate(int i) {
                return new ArrayBuffer.OfInt(i);
            }

            @Override
            protected Spliterator.OfInt makeSpliterator(Spliterator.OfInt ofInt) {
                return new OfInt(ofInt, this);
            }
        }

        static final class OfLong extends OfPrimitive<Long, LongConsumer, ArrayBuffer.OfLong, Spliterator.OfLong> implements Spliterator.OfLong, LongConsumer {
            long tmpValue;

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

            OfLong(Spliterator.OfLong ofLong, long j, long j2) {
                super(ofLong, j, j2);
            }

            OfLong(Spliterator.OfLong ofLong, OfLong ofLong2) {
                super(ofLong, ofLong2);
            }

            @Override
            public void accept(long j) {
                this.tmpValue = j;
            }

            @Override
            protected void acceptConsumed(LongConsumer longConsumer) {
                longConsumer.accept(this.tmpValue);
            }

            @Override
            protected ArrayBuffer.OfLong bufferCreate(int i) {
                return new ArrayBuffer.OfLong(i);
            }

            @Override
            protected Spliterator.OfLong makeSpliterator(Spliterator.OfLong ofLong) {
                return new OfLong(ofLong, this);
            }
        }

        static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, ArrayBuffer.OfDouble, Spliterator.OfDouble> implements Spliterator.OfDouble, DoubleConsumer {
            double tmpValue;

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

            OfDouble(Spliterator.OfDouble ofDouble, long j, long j2) {
                super(ofDouble, j, j2);
            }

            OfDouble(Spliterator.OfDouble ofDouble, OfDouble ofDouble2) {
                super(ofDouble, ofDouble2);
            }

            @Override
            public void accept(double d) {
                this.tmpValue = d;
            }

            @Override
            protected void acceptConsumed(DoubleConsumer doubleConsumer) {
                doubleConsumer.accept(this.tmpValue);
            }

            @Override
            protected ArrayBuffer.OfDouble bufferCreate(int i) {
                return new ArrayBuffer.OfDouble(i);
            }

            @Override
            protected Spliterator.OfDouble makeSpliterator(Spliterator.OfDouble ofDouble) {
                return new OfDouble(ofDouble, this);
            }
        }
    }

    static final class DistinctSpliterator<T> implements Spliterator<T>, Consumer<T> {
        private static final Object NULL_VALUE = new Object();
        private final Spliterator<T> s;
        private final ConcurrentHashMap<T, Boolean> seen;
        private T tmpSlot;

        DistinctSpliterator(Spliterator<T> spliterator) {
            this(spliterator, new ConcurrentHashMap());
        }

        private DistinctSpliterator(Spliterator<T> spliterator, ConcurrentHashMap<T, Boolean> concurrentHashMap) {
            this.s = spliterator;
            this.seen = concurrentHashMap;
        }

        @Override
        public void accept(T t) {
            this.tmpSlot = t;
        }

        private T mapNull(T t) {
            return t != null ? t : (T) NULL_VALUE;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            while (this.s.tryAdvance(this)) {
                if (this.seen.putIfAbsent(mapNull(this.tmpSlot), Boolean.TRUE) == null) {
                    consumer.accept(this.tmpSlot);
                    this.tmpSlot = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void forEachRemaining(final Consumer<? super T> consumer) {
            this.s.forEachRemaining(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    StreamSpliterators.DistinctSpliterator.lambda$forEachRemaining$0(this.f$0, consumer, obj);
                }
            });
        }

        public static void lambda$forEachRemaining$0(DistinctSpliterator distinctSpliterator, Consumer consumer, Object obj) {
            if (distinctSpliterator.seen.putIfAbsent((T) distinctSpliterator.mapNull(obj), Boolean.TRUE) == null) {
                consumer.accept(obj);
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            Spliterator<T> spliteratorTrySplit = this.s.trySplit();
            if (spliteratorTrySplit != null) {
                return new DistinctSpliterator(spliteratorTrySplit, this.seen);
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return this.s.estimateSize();
        }

        @Override
        public int characteristics() {
            return (this.s.characteristics() & (-16469)) | 1;
        }

        @Override
        public Comparator<? super T> getComparator() {
            return this.s.getComparator();
        }
    }

    static abstract class InfiniteSupplyingSpliterator<T> implements Spliterator<T> {
        long estimate;

        protected InfiniteSupplyingSpliterator(long j) {
            this.estimate = j;
        }

        @Override
        public long estimateSize() {
            return this.estimate;
        }

        @Override
        public int characteristics() {
            return 1024;
        }

        static final class OfRef<T> extends InfiniteSupplyingSpliterator<T> {
            final Supplier<T> s;

            OfRef(long j, Supplier<T> supplier) {
                super(j);
                this.s = supplier;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                Objects.requireNonNull(consumer);
                consumer.accept(this.s.get());
                return true;
            }

            @Override
            public Spliterator<T> trySplit() {
                if (this.estimate == 0) {
                    return null;
                }
                long j = this.estimate >>> 1;
                this.estimate = j;
                return new OfRef(j, this.s);
            }
        }

        static final class OfInt extends InfiniteSupplyingSpliterator<Integer> implements Spliterator.OfInt {
            final IntSupplier s;

            OfInt(long j, IntSupplier intSupplier) {
                super(j);
                this.s = intSupplier;
            }

            @Override
            public boolean tryAdvance(IntConsumer intConsumer) {
                Objects.requireNonNull(intConsumer);
                intConsumer.accept(this.s.getAsInt());
                return true;
            }

            @Override
            public Spliterator.OfInt trySplit() {
                if (this.estimate == 0) {
                    return null;
                }
                long j = this.estimate >>> 1;
                this.estimate = j;
                return new OfInt(j, this.s);
            }
        }

        static final class OfLong extends InfiniteSupplyingSpliterator<Long> implements Spliterator.OfLong {
            final LongSupplier s;

            OfLong(long j, LongSupplier longSupplier) {
                super(j);
                this.s = longSupplier;
            }

            @Override
            public boolean tryAdvance(LongConsumer longConsumer) {
                Objects.requireNonNull(longConsumer);
                longConsumer.accept(this.s.getAsLong());
                return true;
            }

            @Override
            public Spliterator.OfLong trySplit() {
                if (this.estimate == 0) {
                    return null;
                }
                long j = this.estimate >>> 1;
                this.estimate = j;
                return new OfLong(j, this.s);
            }
        }

        static final class OfDouble extends InfiniteSupplyingSpliterator<Double> implements Spliterator.OfDouble {
            final DoubleSupplier s;

            OfDouble(long j, DoubleSupplier doubleSupplier) {
                super(j);
                this.s = doubleSupplier;
            }

            @Override
            public boolean tryAdvance(DoubleConsumer doubleConsumer) {
                Objects.requireNonNull(doubleConsumer);
                doubleConsumer.accept(this.s.getAsDouble());
                return true;
            }

            @Override
            public Spliterator.OfDouble trySplit() {
                if (this.estimate == 0) {
                    return null;
                }
                long j = this.estimate >>> 1;
                this.estimate = j;
                return new OfDouble(j, this.s);
            }
        }
    }

    static abstract class ArrayBuffer {
        int index;

        ArrayBuffer() {
        }

        void reset() {
            this.index = 0;
        }

        static final class OfRef<T> extends ArrayBuffer implements Consumer<T> {
            final Object[] array;

            OfRef(int i) {
                this.array = new Object[i];
            }

            @Override
            public void accept(T t) {
                Object[] objArr = this.array;
                int i = this.index;
                this.index = i + 1;
                objArr[i] = t;
            }

            public void forEach(Consumer<? super T> consumer, long j) {
                for (int i = 0; i < j; i++) {
                    consumer.accept(this.array[i]);
                }
            }
        }

        static abstract class OfPrimitive<T_CONS> extends ArrayBuffer {
            int index;

            abstract void forEach(T_CONS t_cons, long j);

            OfPrimitive() {
            }

            @Override
            void reset() {
                this.index = 0;
            }
        }

        static final class OfInt extends OfPrimitive<IntConsumer> implements IntConsumer {
            final int[] array;

            OfInt(int i) {
                this.array = new int[i];
            }

            @Override
            public void accept(int i) {
                int[] iArr = this.array;
                int i2 = this.index;
                this.index = i2 + 1;
                iArr[i2] = i;
            }

            @Override
            public void forEach(IntConsumer intConsumer, long j) {
                for (int i = 0; i < j; i++) {
                    intConsumer.accept(this.array[i]);
                }
            }
        }

        static final class OfLong extends OfPrimitive<LongConsumer> implements LongConsumer {
            final long[] array;

            OfLong(int i) {
                this.array = new long[i];
            }

            @Override
            public void accept(long j) {
                long[] jArr = this.array;
                int i = this.index;
                this.index = i + 1;
                jArr[i] = j;
            }

            @Override
            public void forEach(LongConsumer longConsumer, long j) {
                for (int i = 0; i < j; i++) {
                    longConsumer.accept(this.array[i]);
                }
            }
        }

        static final class OfDouble extends OfPrimitive<DoubleConsumer> implements DoubleConsumer {
            final double[] array;

            OfDouble(int i) {
                this.array = new double[i];
            }

            @Override
            public void accept(double d) {
                double[] dArr = this.array;
                int i = this.index;
                this.index = i + 1;
                dArr[i] = d;
            }

            @Override
            void forEach(DoubleConsumer doubleConsumer, long j) {
                for (int i = 0; i < j; i++) {
                    doubleConsumer.accept(this.array[i]);
                }
            }
        }
    }
}
