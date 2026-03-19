package java.util.stream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoublePipeline;
import java.util.stream.IntPipeline;
import java.util.stream.LongPipeline;
import java.util.stream.MatchOps;
import java.util.stream.Node;
import java.util.stream.Sink;
import java.util.stream.StreamSpliterators;

public abstract class ReferencePipeline<P_IN, P_OUT> extends AbstractPipeline<P_IN, P_OUT, Stream<P_OUT>> implements Stream<P_OUT> {
    ReferencePipeline(Supplier<? extends Spliterator<?>> supplier, int i, boolean z) {
        super(supplier, i, z);
    }

    ReferencePipeline(Spliterator<?> spliterator, int i, boolean z) {
        super(spliterator, i, z);
    }

    ReferencePipeline(AbstractPipeline<?, P_IN, ?> abstractPipeline, int i) {
        super(abstractPipeline, i);
    }

    @Override
    public final StreamShape getOutputShape() {
        return StreamShape.REFERENCE;
    }

    @Override
    public final <P_IN> Node<P_OUT> evaluateToNode(PipelineHelper<P_OUT> pipelineHelper, Spliterator<P_IN> spliterator, boolean z, IntFunction<P_OUT[]> intFunction) {
        return Nodes.collect(pipelineHelper, spliterator, z, intFunction);
    }

    @Override
    public final <P_IN> Spliterator<P_OUT> wrap(PipelineHelper<P_OUT> pipelineHelper, Supplier<Spliterator<P_IN>> supplier, boolean z) {
        return new StreamSpliterators.WrappingSpliterator(pipelineHelper, supplier, z);
    }

    @Override
    public final Spliterator<P_OUT> lazySpliterator2(Supplier<? extends Spliterator<P_OUT>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator(supplier);
    }

    @Override
    public final void forEachWithCancel(Spliterator<P_OUT> spliterator, Sink<P_OUT> sink) {
        while (!sink.cancellationRequested() && spliterator.tryAdvance(sink)) {
        }
    }

    @Override
    public final Node.Builder<P_OUT> makeNodeBuilder(long j, IntFunction<P_OUT[]> intFunction) {
        return Nodes.builder(j, intFunction);
    }

    @Override
    public final Iterator<P_OUT> iterator() {
        return Spliterators.iterator(spliterator2());
    }

    @Override
    public Stream<P_OUT> unordered() {
        if (!isOrdered()) {
            return this;
        }
        return new StatelessOp<P_OUT, P_OUT>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_ORDERED) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<P_OUT> sink) {
                return sink;
            }
        };
    }

    @Override
    public final Stream<P_OUT> filter(final Predicate<? super P_OUT> predicate) {
        Objects.requireNonNull(predicate);
        return new StatelessOp<P_OUT, P_OUT>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SIZED) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<P_OUT> sink) {
                return new Sink.ChainedReference<P_OUT, P_OUT>(sink) {
                    @Override
                    public void begin(long j) {
                        this.downstream.begin(-1L);
                    }

                    @Override
                    public void accept(P_OUT p_out) {
                        if (predicate.test(p_out)) {
                            this.downstream.accept((Object) p_out);
                        }
                    }
                };
            }
        };
    }

    @Override
    public final <R> Stream<R> map(final Function<? super P_OUT, ? extends R> function) {
        Objects.requireNonNull(function);
        return new StatelessOp<P_OUT, R>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<R> sink) {
                return new Sink.ChainedReference<P_OUT, R>(sink) {
                    @Override
                    public void accept(P_OUT p_out) {
                        this.downstream.accept((Object) function.apply(p_out));
                    }
                };
            }
        };
    }

    @Override
    public final IntStream mapToInt(final ToIntFunction<? super P_OUT> toIntFunction) {
        Objects.requireNonNull(toIntFunction);
        return new IntPipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<Integer> sink) {
                return new Sink.ChainedReference<P_OUT, Integer>(sink) {
                    @Override
                    public void accept(P_OUT p_out) {
                        this.downstream.accept(toIntFunction.applyAsInt(p_out));
                    }
                };
            }
        };
    }

    @Override
    public final LongStream mapToLong(final ToLongFunction<? super P_OUT> toLongFunction) {
        Objects.requireNonNull(toLongFunction);
        return new LongPipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<Long> sink) {
                return new Sink.ChainedReference<P_OUT, Long>(sink) {
                    @Override
                    public void accept(P_OUT p_out) {
                        this.downstream.accept(toLongFunction.applyAsLong(p_out));
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream mapToDouble(final ToDoubleFunction<? super P_OUT> toDoubleFunction) {
        Objects.requireNonNull(toDoubleFunction);
        return new DoublePipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<Double> sink) {
                return new Sink.ChainedReference<P_OUT, Double>(sink) {
                    @Override
                    public void accept(P_OUT p_out) {
                        this.downstream.accept(toDoubleFunction.applyAsDouble(p_out));
                    }
                };
            }
        };
    }

    @Override
    public final <R> Stream<R> flatMap(final Function<? super P_OUT, ? extends Stream<? extends R>> function) {
        Objects.requireNonNull(function);
        return new StatelessOp<P_OUT, R>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<R> sink) {
                return new Sink.ChainedReference<P_OUT, R>(sink) {
                    @Override
                    public void begin(long j) {
                        this.downstream.begin(-1L);
                    }

                    @Override
                    public void accept(P_OUT p_out) {
                        Stream stream = (Stream) function.apply(p_out);
                        if (stream != null) {
                            Throwable th = null;
                            try {
                                stream.sequential().forEach(this.downstream);
                            } catch (Throwable th2) {
                                if (stream != null) {
                                    if (th != null) {
                                        try {
                                            stream.close();
                                        } catch (Throwable th3) {
                                            th.addSuppressed(th3);
                                        }
                                    } else {
                                        stream.close();
                                    }
                                }
                                throw th2;
                            }
                        }
                        if (stream != null) {
                            stream.close();
                        }
                    }
                };
            }
        };
    }

    @Override
    public final IntStream flatMapToInt(final Function<? super P_OUT, ? extends IntStream> function) {
        Objects.requireNonNull(function);
        return new IntPipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<Integer> sink) {
                return new Sink.ChainedReference<P_OUT, Integer>(sink) {
                    IntConsumer downstreamAsInt;

                    {
                        Sink<? super E_OUT> sink2 = this.downstream;
                        Objects.requireNonNull(sink2);
                        this.downstreamAsInt = new $$Lambda$wDsxx48ovPSGeNEb3P6H9u7YX0k(sink2);
                    }

                    @Override
                    public void begin(long j) {
                        this.downstream.begin(-1L);
                    }

                    @Override
                    public void accept(P_OUT p_out) {
                        IntStream intStream = (IntStream) function.apply(p_out);
                        if (intStream != null) {
                            Throwable th = null;
                            try {
                                intStream.sequential().forEach(this.downstreamAsInt);
                            } catch (Throwable th2) {
                                if (intStream != null) {
                                    if (0 != 0) {
                                        try {
                                            intStream.close();
                                        } catch (Throwable th3) {
                                            th.addSuppressed(th3);
                                        }
                                    } else {
                                        intStream.close();
                                    }
                                }
                                throw th2;
                            }
                        }
                        if (intStream != null) {
                            intStream.close();
                        }
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream flatMapToDouble(final Function<? super P_OUT, ? extends DoubleStream> function) {
        Objects.requireNonNull(function);
        return new DoublePipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<Double> sink) {
                return new Sink.ChainedReference<P_OUT, Double>(sink) {
                    DoubleConsumer downstreamAsDouble;

                    {
                        Sink<? super E_OUT> sink2 = this.downstream;
                        Objects.requireNonNull(sink2);
                        this.downstreamAsDouble = new $$Lambda$G0LLxk8pWitjFgsOx2bYtROrGg(sink2);
                    }

                    @Override
                    public void begin(long j) {
                        this.downstream.begin(-1L);
                    }

                    @Override
                    public void accept(P_OUT p_out) {
                        DoubleStream doubleStream = (DoubleStream) function.apply(p_out);
                        if (doubleStream != null) {
                            Throwable th = null;
                            try {
                                doubleStream.sequential().forEach(this.downstreamAsDouble);
                            } catch (Throwable th2) {
                                if (doubleStream != null) {
                                    if (0 != 0) {
                                        try {
                                            doubleStream.close();
                                        } catch (Throwable th3) {
                                            th.addSuppressed(th3);
                                        }
                                    } else {
                                        doubleStream.close();
                                    }
                                }
                                throw th2;
                            }
                        }
                        if (doubleStream != null) {
                            doubleStream.close();
                        }
                    }
                };
            }
        };
    }

    @Override
    public final LongStream flatMapToLong(final Function<? super P_OUT, ? extends LongStream> function) {
        Objects.requireNonNull(function);
        return new LongPipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<Long> sink) {
                return new Sink.ChainedReference<P_OUT, Long>(sink) {
                    LongConsumer downstreamAsLong;

                    {
                        Sink<? super E_OUT> sink2 = this.downstream;
                        Objects.requireNonNull(sink2);
                        this.downstreamAsLong = new $$Lambda$zQ9PoGPFOA3MjNNbaERnRB6ik(sink2);
                    }

                    @Override
                    public void begin(long j) {
                        this.downstream.begin(-1L);
                    }

                    @Override
                    public void accept(P_OUT p_out) {
                        LongStream longStream = (LongStream) function.apply(p_out);
                        if (longStream != null) {
                            Throwable th = null;
                            try {
                                longStream.sequential().forEach(this.downstreamAsLong);
                            } catch (Throwable th2) {
                                if (longStream != null) {
                                    if (0 != 0) {
                                        try {
                                            longStream.close();
                                        } catch (Throwable th3) {
                                            th.addSuppressed(th3);
                                        }
                                    } else {
                                        longStream.close();
                                    }
                                }
                                throw th2;
                            }
                        }
                        if (longStream != null) {
                            longStream.close();
                        }
                    }
                };
            }
        };
    }

    @Override
    public final Stream<P_OUT> peek(final Consumer<? super P_OUT> consumer) {
        Objects.requireNonNull(consumer);
        return new StatelessOp<P_OUT, P_OUT>(this, StreamShape.REFERENCE, 0) {
            @Override
            public Sink<P_OUT> opWrapSink(int i, Sink<P_OUT> sink) {
                return new Sink.ChainedReference<P_OUT, P_OUT>(sink) {
                    @Override
                    public void accept(P_OUT p_out) {
                        consumer.accept(p_out);
                        this.downstream.accept((Object) p_out);
                    }
                };
            }
        };
    }

    @Override
    public final Stream<P_OUT> distinct() {
        return DistinctOps.makeRef(this);
    }

    @Override
    public final Stream<P_OUT> sorted() {
        return SortedOps.makeRef(this);
    }

    @Override
    public final Stream<P_OUT> sorted(Comparator<? super P_OUT> comparator) {
        return SortedOps.makeRef(this, comparator);
    }

    @Override
    public final Stream<P_OUT> limit(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(Long.toString(j));
        }
        return SliceOps.makeRef(this, 0L, j);
    }

    @Override
    public final Stream<P_OUT> skip(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(Long.toString(j));
        }
        if (j == 0) {
            return this;
        }
        return SliceOps.makeRef(this, j, -1L);
    }

    @Override
    public void forEach(Consumer<? super P_OUT> consumer) {
        evaluate(ForEachOps.makeRef(consumer, false));
    }

    @Override
    public void forEachOrdered(Consumer<? super P_OUT> consumer) {
        evaluate(ForEachOps.makeRef(consumer, true));
    }

    @Override
    public final <A> A[] toArray(IntFunction<A[]> intFunction) {
        return (A[]) Nodes.flatten(evaluateToArrayNode(intFunction), intFunction).asArray(intFunction);
    }

    static Object[] lambda$toArray$0(int i) {
        return new Object[i];
    }

    @Override
    public final Object[] toArray() {
        return toArray(new IntFunction() {
            @Override
            public final Object apply(int i) {
                return ReferencePipeline.lambda$toArray$0(i);
            }
        });
    }

    @Override
    public final boolean anyMatch(Predicate<? super P_OUT> predicate) {
        return ((Boolean) evaluate(MatchOps.makeRef(predicate, MatchOps.MatchKind.ANY))).booleanValue();
    }

    @Override
    public final boolean allMatch(Predicate<? super P_OUT> predicate) {
        return ((Boolean) evaluate(MatchOps.makeRef(predicate, MatchOps.MatchKind.ALL))).booleanValue();
    }

    @Override
    public final boolean noneMatch(Predicate<? super P_OUT> predicate) {
        return ((Boolean) evaluate(MatchOps.makeRef(predicate, MatchOps.MatchKind.NONE))).booleanValue();
    }

    @Override
    public final Optional<P_OUT> findFirst() {
        return (Optional) evaluate(FindOps.makeRef(true));
    }

    @Override
    public final Optional<P_OUT> findAny() {
        return (Optional) evaluate(FindOps.makeRef(false));
    }

    @Override
    public final P_OUT reduce(P_OUT p_out, BinaryOperator<P_OUT> binaryOperator) {
        return (P_OUT) evaluate(ReduceOps.makeRef(p_out, binaryOperator, binaryOperator));
    }

    @Override
    public final Optional<P_OUT> reduce(BinaryOperator<P_OUT> binaryOperator) {
        return (Optional) evaluate(ReduceOps.makeRef(binaryOperator));
    }

    @Override
    public final <R> R reduce(R r, BiFunction<R, ? super P_OUT, R> biFunction, BinaryOperator<R> binaryOperator) {
        return (R) evaluate(ReduceOps.makeRef(r, (BiFunction<R, ? super T, R>) biFunction, binaryOperator));
    }

    @Override
    public final <R, A> R collect(Collector<? super P_OUT, A, R> collector) {
        final A a;
        if (isParallel() && collector.characteristics().contains(Collector.Characteristics.CONCURRENT) && (!isOrdered() || collector.characteristics().contains(Collector.Characteristics.UNORDERED))) {
            a = collector.supplier().get();
            final BiConsumer<A, ? super P_OUT> biConsumerAccumulator = collector.accumulator();
            forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    biConsumerAccumulator.accept(a, obj);
                }
            });
        } else {
            a = (R) evaluate(ReduceOps.makeRef(collector));
        }
        return collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH) ? a : (R) collector.finisher().apply(a);
    }

    @Override
    public final <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super P_OUT> biConsumer, BiConsumer<R, R> biConsumer2) {
        return (R) evaluate(ReduceOps.makeRef(supplier, biConsumer, biConsumer2));
    }

    @Override
    public final Optional<P_OUT> max(Comparator<? super P_OUT> comparator) {
        return reduce(BinaryOperator.maxBy(comparator));
    }

    @Override
    public final Optional<P_OUT> min(Comparator<? super P_OUT> comparator) {
        return reduce(BinaryOperator.minBy(comparator));
    }

    static long lambda$count$2(Object obj) {
        return 1L;
    }

    @Override
    public final long count() {
        return mapToLong(new ToLongFunction() {
            @Override
            public final long applyAsLong(Object obj) {
                return ReferencePipeline.lambda$count$2(obj);
            }
        }).sum();
    }

    public static class Head<E_IN, E_OUT> extends ReferencePipeline<E_IN, E_OUT> {
        public Head(Supplier<? extends Spliterator<?>> supplier, int i, boolean z) {
            super(supplier, i, z);
        }

        public Head(Spliterator<?> spliterator, int i, boolean z) {
            super(spliterator, i, z);
        }

        @Override
        public final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final Sink<E_IN> opWrapSink(int i, Sink<E_OUT> sink) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEach(Consumer<? super E_OUT> consumer) {
            if (!isParallel()) {
                sourceStageSpliterator().forEachRemaining(consumer);
            } else {
                super.forEach(consumer);
            }
        }

        @Override
        public void forEachOrdered(Consumer<? super E_OUT> consumer) {
            if (!isParallel()) {
                sourceStageSpliterator().forEachRemaining(consumer);
            } else {
                super.forEachOrdered(consumer);
            }
        }
    }

    public static abstract class StatelessOp<E_IN, E_OUT> extends ReferencePipeline<E_IN, E_OUT> {
        static final boolean $assertionsDisabled = false;

        public StatelessOp(AbstractPipeline<?, E_IN, ?> abstractPipeline, StreamShape streamShape, int i) {
            super(abstractPipeline, i);
        }

        @Override
        public final boolean opIsStateful() {
            return false;
        }
    }

    public static abstract class StatefulOp<E_IN, E_OUT> extends ReferencePipeline<E_IN, E_OUT> {
        static final boolean $assertionsDisabled = false;

        @Override
        public abstract <P_IN> Node<E_OUT> opEvaluateParallel(PipelineHelper<E_OUT> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<E_OUT[]> intFunction);

        public StatefulOp(AbstractPipeline<?, E_IN, ?> abstractPipeline, StreamShape streamShape, int i) {
            super(abstractPipeline, i);
        }

        @Override
        public final boolean opIsStateful() {
            return true;
        }
    }
}
