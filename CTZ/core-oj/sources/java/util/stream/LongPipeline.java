package java.util.stream;

import java.util.Iterator;
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.DoublePipeline;
import java.util.stream.IntPipeline;
import java.util.stream.MatchOps;
import java.util.stream.Node;
import java.util.stream.ReferencePipeline;
import java.util.stream.Sink;
import java.util.stream.StreamSpliterators;

public abstract class LongPipeline<E_IN> extends AbstractPipeline<E_IN, Long, LongStream> implements LongStream {
    @Override
    public LongStream parallel() {
        return (LongStream) super.parallel();
    }

    @Override
    public LongStream sequential() {
        return (LongStream) super.sequential();
    }

    LongPipeline(Supplier<? extends Spliterator<Long>> supplier, int i, boolean z) {
        super(supplier, i, z);
    }

    LongPipeline(Spliterator<Long> spliterator, int i, boolean z) {
        super(spliterator, i, z);
    }

    LongPipeline(AbstractPipeline<?, E_IN, ?> abstractPipeline, int i) {
        super(abstractPipeline, i);
    }

    private static LongConsumer adapt(Sink<Long> sink) {
        if (sink instanceof LongConsumer) {
            return (LongConsumer) sink;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using LongStream.adapt(Sink<Long> s)");
        }
        Objects.requireNonNull(sink);
        return new $$Lambda$zQ9PoGPFOA3MjNNbaERnRB6ik(sink);
    }

    private static Spliterator.OfLong adapt(Spliterator<Long> spliterator) {
        if (spliterator instanceof Spliterator.OfLong) {
            return (Spliterator.OfLong) spliterator;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using LongStream.adapt(Spliterator<Long> s)");
        }
        throw new UnsupportedOperationException("LongStream.adapt(Spliterator<Long> s)");
    }

    @Override
    public final StreamShape getOutputShape() {
        return StreamShape.LONG_VALUE;
    }

    @Override
    public final <P_IN> Node<Long> evaluateToNode(PipelineHelper<Long> pipelineHelper, Spliterator<P_IN> spliterator, boolean z, IntFunction<Long[]> intFunction) {
        return Nodes.collectLong(pipelineHelper, spliterator, z);
    }

    @Override
    public final <P_IN> Spliterator<Long> wrap(PipelineHelper<Long> pipelineHelper, Supplier<Spliterator<P_IN>> supplier, boolean z) {
        return new StreamSpliterators.LongWrappingSpliterator(pipelineHelper, supplier, z);
    }

    @Override
    public final Spliterator<Long> lazySpliterator2(Supplier<? extends Spliterator<Long>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator.OfLong(supplier);
    }

    @Override
    public final void forEachWithCancel(Spliterator<Long> spliterator, Sink<Long> sink) {
        Spliterator.OfLong ofLongAdapt = adapt(spliterator);
        LongConsumer longConsumerAdapt = adapt(sink);
        while (!sink.cancellationRequested() && ofLongAdapt.tryAdvance(longConsumerAdapt)) {
        }
    }

    @Override
    public final Node.Builder<Long> makeNodeBuilder(long j, IntFunction<Long[]> intFunction) {
        return Nodes.longBuilder(j);
    }

    @Override
    public final Iterator<Long> iterator() {
        return Spliterators.iterator((Spliterator.OfLong) spliterator2());
    }

    @Override
    public final Spliterator<Long> spliterator2() {
        return adapt((Spliterator<Long>) super.spliterator2());
    }

    @Override
    public final DoubleStream asDoubleStream() {
        return new DoublePipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Long> opWrapSink(int i, Sink<Double> sink) {
                return new Sink.ChainedLong<Double>(sink) {
                    @Override
                    public void accept(long j) {
                        this.downstream.accept(j);
                    }
                };
            }
        };
    }

    @Override
    public final Stream<Long> boxed() {
        return mapToObj(new LongFunction() {
            @Override
            public final Object apply(long j) {
                return Long.valueOf(j);
            }
        });
    }

    @Override
    public final LongStream map(final LongUnaryOperator longUnaryOperator) {
        Objects.requireNonNull(longUnaryOperator);
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Long> opWrapSink(int i, Sink<Long> sink) {
                return new Sink.ChainedLong<Long>(sink) {
                    @Override
                    public void accept(long j) {
                        this.downstream.accept(longUnaryOperator.applyAsLong(j));
                    }
                };
            }
        };
    }

    @Override
    public final <U> Stream<U> mapToObj(final LongFunction<? extends U> longFunction) {
        Objects.requireNonNull(longFunction);
        return new ReferencePipeline.StatelessOp<Long, U>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Long> opWrapSink(int i, Sink<U> sink) {
                return new Sink.ChainedLong<U>(sink) {
                    @Override
                    public void accept(long j) {
                        this.downstream.accept((Object) longFunction.apply(j));
                    }
                };
            }
        };
    }

    @Override
    public final IntStream mapToInt(final LongToIntFunction longToIntFunction) {
        Objects.requireNonNull(longToIntFunction);
        return new IntPipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Long> opWrapSink(int i, Sink<Integer> sink) {
                return new Sink.ChainedLong<Integer>(sink) {
                    @Override
                    public void accept(long j) {
                        this.downstream.accept(longToIntFunction.applyAsInt(j));
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream mapToDouble(final LongToDoubleFunction longToDoubleFunction) {
        Objects.requireNonNull(longToDoubleFunction);
        return new DoublePipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Long> opWrapSink(int i, Sink<Double> sink) {
                return new Sink.ChainedLong<Double>(sink) {
                    @Override
                    public void accept(long j) {
                        this.downstream.accept(longToDoubleFunction.applyAsDouble(j));
                    }
                };
            }
        };
    }

    @Override
    public final LongStream flatMap(final LongFunction<? extends LongStream> longFunction) {
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {

            class AnonymousClass1 extends Sink.ChainedLong<Long> {
                AnonymousClass1(Sink sink) {
                    super(sink);
                }

                @Override
                public void begin(long j) {
                    this.downstream.begin(-1L);
                }

                @Override
                public void accept(long j) {
                    LongStream longStream = (LongStream) longFunction.apply(j);
                    if (longStream != null) {
                        Throwable th = null;
                        try {
                            longStream.sequential().forEach(new LongConsumer() {
                                @Override
                                public final void accept(long j2) {
                                    this.f$0.downstream.accept(j2);
                                }
                            });
                        } catch (Throwable th2) {
                            if (longStream != null) {
                                if (th != null) {
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
            }

            @Override
            public Sink<Long> opWrapSink(int i, Sink<Long> sink) {
                return new AnonymousClass1(sink);
            }
        };
    }

    @Override
    public LongStream unordered() {
        if (!isOrdered()) {
            return this;
        }
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_ORDERED) {
            @Override
            public Sink<Long> opWrapSink(int i, Sink<Long> sink) {
                return sink;
            }
        };
    }

    @Override
    public final LongStream filter(final LongPredicate longPredicate) {
        Objects.requireNonNull(longPredicate);
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SIZED) {
            @Override
            public Sink<Long> opWrapSink(int i, Sink<Long> sink) {
                return new Sink.ChainedLong<Long>(sink) {
                    @Override
                    public void begin(long j) {
                        this.downstream.begin(-1L);
                    }

                    @Override
                    public void accept(long j) {
                        if (longPredicate.test(j)) {
                            this.downstream.accept(j);
                        }
                    }
                };
            }
        };
    }

    @Override
    public final LongStream peek(final LongConsumer longConsumer) {
        Objects.requireNonNull(longConsumer);
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, 0) {
            @Override
            public Sink<Long> opWrapSink(int i, Sink<Long> sink) {
                return new Sink.ChainedLong<Long>(sink) {
                    @Override
                    public void accept(long j) {
                        longConsumer.accept(j);
                        this.downstream.accept(j);
                    }
                };
            }
        };
    }

    @Override
    public final LongStream limit(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(Long.toString(j));
        }
        return SliceOps.makeLong(this, 0L, j);
    }

    @Override
    public final LongStream skip(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(Long.toString(j));
        }
        if (j == 0) {
            return this;
        }
        return SliceOps.makeLong(this, j, -1L);
    }

    @Override
    public final LongStream sorted() {
        return SortedOps.makeLong(this);
    }

    @Override
    public final LongStream distinct() {
        return boxed().distinct().mapToLong(new ToLongFunction() {
            @Override
            public final long applyAsLong(Object obj) {
                return ((Long) obj).longValue();
            }
        });
    }

    @Override
    public void forEach(LongConsumer longConsumer) {
        evaluate(ForEachOps.makeLong(longConsumer, false));
    }

    @Override
    public void forEachOrdered(LongConsumer longConsumer) {
        evaluate(ForEachOps.makeLong(longConsumer, true));
    }

    @Override
    public final long sum() {
        return reduce(0L, new LongBinaryOperator() {
            @Override
            public final long applyAsLong(long j, long j2) {
                return Long.sum(j, j2);
            }
        });
    }

    @Override
    public final OptionalLong min() {
        return reduce(new LongBinaryOperator() {
            @Override
            public final long applyAsLong(long j, long j2) {
                return Math.min(j, j2);
            }
        });
    }

    @Override
    public final OptionalLong max() {
        return reduce(new LongBinaryOperator() {
            @Override
            public final long applyAsLong(long j, long j2) {
                return Math.max(j, j2);
            }
        });
    }

    static long[] lambda$average$1() {
        return new long[2];
    }

    @Override
    public final OptionalDouble average() {
        if (((long[]) collect(new Supplier() {
            @Override
            public final Object get() {
                return LongPipeline.lambda$average$1();
            }
        }, new ObjLongConsumer() {
            @Override
            public final void accept(Object obj, long j) {
                LongPipeline.lambda$average$2((long[]) obj, j);
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                LongPipeline.lambda$average$3((long[]) obj, (long[]) obj2);
            }
        }))[0] > 0) {
            return OptionalDouble.of(r0[1] / r0[0]);
        }
        return OptionalDouble.empty();
    }

    static void lambda$average$2(long[] jArr, long j) {
        jArr[0] = jArr[0] + 1;
        jArr[1] = jArr[1] + j;
    }

    static void lambda$average$3(long[] jArr, long[] jArr2) {
        jArr[0] = jArr[0] + jArr2[0];
        jArr[1] = jArr[1] + jArr2[1];
    }

    static long lambda$count$4(long j) {
        return 1L;
    }

    @Override
    public final long count() {
        return map(new LongUnaryOperator() {
            @Override
            public final long applyAsLong(long j) {
                return LongPipeline.lambda$count$4(j);
            }
        }).sum();
    }

    @Override
    public final LongSummaryStatistics summaryStatistics() {
        return (LongSummaryStatistics) collect($$Lambda$kZuTETptiPwvB1J27Na7j760aLU.INSTANCE, new ObjLongConsumer() {
            @Override
            public final void accept(Object obj, long j) {
                ((LongSummaryStatistics) obj).accept(j);
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((LongSummaryStatistics) obj).combine((LongSummaryStatistics) obj2);
            }
        });
    }

    @Override
    public final long reduce(long j, LongBinaryOperator longBinaryOperator) {
        return ((Long) evaluate(ReduceOps.makeLong(j, longBinaryOperator))).longValue();
    }

    @Override
    public final OptionalLong reduce(LongBinaryOperator longBinaryOperator) {
        return (OptionalLong) evaluate(ReduceOps.makeLong(longBinaryOperator));
    }

    @Override
    public final <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> objLongConsumer, final BiConsumer<R, R> biConsumer) {
        return (R) evaluate(ReduceOps.makeLong(supplier, objLongConsumer, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return LongPipeline.lambda$collect$5(biConsumer, obj, obj2);
            }
        }));
    }

    static Object lambda$collect$5(BiConsumer biConsumer, Object obj, Object obj2) {
        biConsumer.accept(obj, obj2);
        return obj;
    }

    @Override
    public final boolean anyMatch(LongPredicate longPredicate) {
        return ((Boolean) evaluate(MatchOps.makeLong(longPredicate, MatchOps.MatchKind.ANY))).booleanValue();
    }

    @Override
    public final boolean allMatch(LongPredicate longPredicate) {
        return ((Boolean) evaluate(MatchOps.makeLong(longPredicate, MatchOps.MatchKind.ALL))).booleanValue();
    }

    @Override
    public final boolean noneMatch(LongPredicate longPredicate) {
        return ((Boolean) evaluate(MatchOps.makeLong(longPredicate, MatchOps.MatchKind.NONE))).booleanValue();
    }

    @Override
    public final OptionalLong findFirst() {
        return (OptionalLong) evaluate(FindOps.makeLong(true));
    }

    @Override
    public final OptionalLong findAny() {
        return (OptionalLong) evaluate(FindOps.makeLong(false));
    }

    static Long[] lambda$toArray$6(int i) {
        return new Long[i];
    }

    @Override
    public final long[] toArray() {
        return Nodes.flattenLong((Node.OfLong) evaluateToArrayNode(new IntFunction() {
            @Override
            public final Object apply(int i) {
                return LongPipeline.lambda$toArray$6(i);
            }
        })).asPrimitiveArray();
    }

    public static class Head<E_IN> extends LongPipeline<E_IN> {
        @Override
        public LongStream parallel() {
            return (LongStream) super.parallel();
        }

        @Override
        public LongStream sequential() {
            return (LongStream) super.sequential();
        }

        public Head(Supplier<? extends Spliterator<Long>> supplier, int i, boolean z) {
            super(supplier, i, z);
        }

        public Head(Spliterator<Long> spliterator, int i, boolean z) {
            super(spliterator, i, z);
        }

        @Override
        public final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final Sink<E_IN> opWrapSink(int i, Sink<Long> sink) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEach(LongConsumer longConsumer) {
            if (!isParallel()) {
                LongPipeline.adapt(sourceStageSpliterator()).forEachRemaining(longConsumer);
            } else {
                super.forEach(longConsumer);
            }
        }

        @Override
        public void forEachOrdered(LongConsumer longConsumer) {
            if (!isParallel()) {
                LongPipeline.adapt(sourceStageSpliterator()).forEachRemaining(longConsumer);
            } else {
                super.forEachOrdered(longConsumer);
            }
        }
    }

    public static abstract class StatelessOp<E_IN> extends LongPipeline<E_IN> {
        static final boolean $assertionsDisabled = false;

        @Override
        public LongStream parallel() {
            return (LongStream) super.parallel();
        }

        @Override
        public LongStream sequential() {
            return (LongStream) super.sequential();
        }

        public StatelessOp(AbstractPipeline<?, E_IN, ?> abstractPipeline, StreamShape streamShape, int i) {
            super(abstractPipeline, i);
        }

        @Override
        public final boolean opIsStateful() {
            return false;
        }
    }

    public static abstract class StatefulOp<E_IN> extends LongPipeline<E_IN> {
        static final boolean $assertionsDisabled = false;

        @Override
        public abstract <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Long[]> intFunction);

        @Override
        public LongStream parallel() {
            return (LongStream) super.parallel();
        }

        @Override
        public LongStream sequential() {
            return (LongStream) super.sequential();
        }

        public StatefulOp(AbstractPipeline<?, E_IN, ?> abstractPipeline, StreamShape streamShape, int i) {
            super(abstractPipeline, i);
        }

        @Override
        public final boolean opIsStateful() {
            return true;
        }
    }
}
