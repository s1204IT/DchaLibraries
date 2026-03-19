package java.util.stream;

import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.DoublePipeline;
import java.util.stream.LongPipeline;
import java.util.stream.MatchOps;
import java.util.stream.Node;
import java.util.stream.ReferencePipeline;
import java.util.stream.Sink;
import java.util.stream.StreamSpliterators;

public abstract class IntPipeline<E_IN> extends AbstractPipeline<E_IN, Integer, IntStream> implements IntStream {
    @Override
    public IntStream parallel() {
        return (IntStream) super.parallel();
    }

    @Override
    public IntStream sequential() {
        return (IntStream) super.sequential();
    }

    IntPipeline(Supplier<? extends Spliterator<Integer>> supplier, int i, boolean z) {
        super(supplier, i, z);
    }

    IntPipeline(Spliterator<Integer> spliterator, int i, boolean z) {
        super(spliterator, i, z);
    }

    IntPipeline(AbstractPipeline<?, E_IN, ?> abstractPipeline, int i) {
        super(abstractPipeline, i);
    }

    private static IntConsumer adapt(Sink<Integer> sink) {
        if (sink instanceof IntConsumer) {
            return (IntConsumer) sink;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using IntStream.adapt(Sink<Integer> s)");
        }
        Objects.requireNonNull(sink);
        return new $$Lambda$wDsxx48ovPSGeNEb3P6H9u7YX0k(sink);
    }

    private static Spliterator.OfInt adapt(Spliterator<Integer> spliterator) {
        if (spliterator instanceof Spliterator.OfInt) {
            return (Spliterator.OfInt) spliterator;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using IntStream.adapt(Spliterator<Integer> s)");
        }
        throw new UnsupportedOperationException("IntStream.adapt(Spliterator<Integer> s)");
    }

    @Override
    public final StreamShape getOutputShape() {
        return StreamShape.INT_VALUE;
    }

    @Override
    public final <P_IN> Node<Integer> evaluateToNode(PipelineHelper<Integer> pipelineHelper, Spliterator<P_IN> spliterator, boolean z, IntFunction<Integer[]> intFunction) {
        return Nodes.collectInt(pipelineHelper, spliterator, z);
    }

    @Override
    public final <P_IN> Spliterator<Integer> wrap(PipelineHelper<Integer> pipelineHelper, Supplier<Spliterator<P_IN>> supplier, boolean z) {
        return new StreamSpliterators.IntWrappingSpliterator(pipelineHelper, supplier, z);
    }

    @Override
    public final Spliterator<Integer> lazySpliterator2(Supplier<? extends Spliterator<Integer>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator.OfInt(supplier);
    }

    @Override
    public final void forEachWithCancel(Spliterator<Integer> spliterator, Sink<Integer> sink) {
        Spliterator.OfInt ofIntAdapt = adapt(spliterator);
        IntConsumer intConsumerAdapt = adapt(sink);
        while (!sink.cancellationRequested() && ofIntAdapt.tryAdvance(intConsumerAdapt)) {
        }
    }

    @Override
    public final Node.Builder<Integer> makeNodeBuilder(long j, IntFunction<Integer[]> intFunction) {
        return Nodes.intBuilder(j);
    }

    @Override
    public final Iterator<Integer> iterator() {
        return Spliterators.iterator((Spliterator.OfInt) spliterator2());
    }

    @Override
    public final Spliterator<Integer> spliterator2() {
        return adapt((Spliterator<Integer>) super.spliterator2());
    }

    @Override
    public final LongStream asLongStream() {
        return new LongPipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Integer> opWrapSink(int i, Sink<Long> sink) {
                return new Sink.ChainedInt<Long>(sink) {
                    @Override
                    public void accept(int i2) {
                        this.downstream.accept(i2);
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream asDoubleStream() {
        return new DoublePipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Integer> opWrapSink(int i, Sink<Double> sink) {
                return new Sink.ChainedInt<Double>(sink) {
                    @Override
                    public void accept(int i2) {
                        this.downstream.accept(i2);
                    }
                };
            }
        };
    }

    @Override
    public final Stream<Integer> boxed() {
        return mapToObj(new IntFunction() {
            @Override
            public final Object apply(int i) {
                return Integer.valueOf(i);
            }
        });
    }

    @Override
    public final IntStream map(final IntUnaryOperator intUnaryOperator) {
        Objects.requireNonNull(intUnaryOperator);
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Integer> opWrapSink(int i, Sink<Integer> sink) {
                return new Sink.ChainedInt<Integer>(sink) {
                    @Override
                    public void accept(int i2) {
                        this.downstream.accept(intUnaryOperator.applyAsInt(i2));
                    }
                };
            }
        };
    }

    @Override
    public final <U> Stream<U> mapToObj(final IntFunction<? extends U> intFunction) {
        Objects.requireNonNull(intFunction);
        return new ReferencePipeline.StatelessOp<Integer, U>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Integer> opWrapSink(int i, Sink<U> sink) {
                return new Sink.ChainedInt<U>(sink) {
                    @Override
                    public void accept(int i2) {
                        this.downstream.accept((Object) intFunction.apply(i2));
                    }
                };
            }
        };
    }

    @Override
    public final LongStream mapToLong(final IntToLongFunction intToLongFunction) {
        Objects.requireNonNull(intToLongFunction);
        return new LongPipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Integer> opWrapSink(int i, Sink<Long> sink) {
                return new Sink.ChainedInt<Long>(sink) {
                    @Override
                    public void accept(int i2) {
                        this.downstream.accept(intToLongFunction.applyAsLong(i2));
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream mapToDouble(final IntToDoubleFunction intToDoubleFunction) {
        Objects.requireNonNull(intToDoubleFunction);
        return new DoublePipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Integer> opWrapSink(int i, Sink<Double> sink) {
                return new Sink.ChainedInt<Double>(sink) {
                    @Override
                    public void accept(int i2) {
                        this.downstream.accept(intToDoubleFunction.applyAsDouble(i2));
                    }
                };
            }
        };
    }

    @Override
    public final IntStream flatMap(final IntFunction<? extends IntStream> intFunction) {
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {

            class AnonymousClass1 extends Sink.ChainedInt<Integer> {
                AnonymousClass1(Sink sink) {
                    super(sink);
                }

                @Override
                public void begin(long j) {
                    this.downstream.begin(-1L);
                }

                @Override
                public void accept(int i) {
                    IntStream intStream = (IntStream) intFunction.apply(i);
                    if (intStream != null) {
                        Throwable th = null;
                        try {
                            intStream.sequential().forEach(new IntConsumer() {
                                @Override
                                public final void accept(int i2) {
                                    this.f$0.downstream.accept(i2);
                                }
                            });
                        } catch (Throwable th2) {
                            if (intStream != null) {
                                if (th != null) {
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
            }

            @Override
            public Sink<Integer> opWrapSink(int i, Sink<Integer> sink) {
                return new AnonymousClass1(sink);
            }
        };
    }

    @Override
    public IntStream unordered() {
        if (!isOrdered()) {
            return this;
        }
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_ORDERED) {
            @Override
            public Sink<Integer> opWrapSink(int i, Sink<Integer> sink) {
                return sink;
            }
        };
    }

    @Override
    public final IntStream filter(final IntPredicate intPredicate) {
        Objects.requireNonNull(intPredicate);
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SIZED) {
            @Override
            public Sink<Integer> opWrapSink(int i, Sink<Integer> sink) {
                return new Sink.ChainedInt<Integer>(sink) {
                    @Override
                    public void begin(long j) {
                        this.downstream.begin(-1L);
                    }

                    @Override
                    public void accept(int i2) {
                        if (intPredicate.test(i2)) {
                            this.downstream.accept(i2);
                        }
                    }
                };
            }
        };
    }

    @Override
    public final IntStream peek(final IntConsumer intConsumer) {
        Objects.requireNonNull(intConsumer);
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, 0) {
            @Override
            public Sink<Integer> opWrapSink(int i, Sink<Integer> sink) {
                return new Sink.ChainedInt<Integer>(sink) {
                    @Override
                    public void accept(int i2) {
                        intConsumer.accept(i2);
                        this.downstream.accept(i2);
                    }
                };
            }
        };
    }

    @Override
    public final IntStream limit(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(Long.toString(j));
        }
        return SliceOps.makeInt(this, 0L, j);
    }

    @Override
    public final IntStream skip(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(Long.toString(j));
        }
        if (j == 0) {
            return this;
        }
        return SliceOps.makeInt(this, j, -1L);
    }

    @Override
    public final IntStream sorted() {
        return SortedOps.makeInt(this);
    }

    @Override
    public final IntStream distinct() {
        return boxed().distinct().mapToInt(new ToIntFunction() {
            @Override
            public final int applyAsInt(Object obj) {
                return ((Integer) obj).intValue();
            }
        });
    }

    @Override
    public void forEach(IntConsumer intConsumer) {
        evaluate(ForEachOps.makeInt(intConsumer, false));
    }

    @Override
    public void forEachOrdered(IntConsumer intConsumer) {
        evaluate(ForEachOps.makeInt(intConsumer, true));
    }

    @Override
    public final int sum() {
        return reduce(0, new IntBinaryOperator() {
            @Override
            public final int applyAsInt(int i, int i2) {
                return Integer.sum(i, i2);
            }
        });
    }

    @Override
    public final OptionalInt min() {
        return reduce(new IntBinaryOperator() {
            @Override
            public final int applyAsInt(int i, int i2) {
                return Math.min(i, i2);
            }
        });
    }

    @Override
    public final OptionalInt max() {
        return reduce(new IntBinaryOperator() {
            @Override
            public final int applyAsInt(int i, int i2) {
                return Math.max(i, i2);
            }
        });
    }

    static long lambda$count$1(int i) {
        return 1L;
    }

    @Override
    public final long count() {
        return mapToLong(new IntToLongFunction() {
            @Override
            public final long applyAsLong(int i) {
                return IntPipeline.lambda$count$1(i);
            }
        }).sum();
    }

    static long[] lambda$average$2() {
        return new long[2];
    }

    @Override
    public final OptionalDouble average() {
        if (((long[]) collect(new Supplier() {
            @Override
            public final Object get() {
                return IntPipeline.lambda$average$2();
            }
        }, new ObjIntConsumer() {
            @Override
            public final void accept(Object obj, int i) {
                IntPipeline.lambda$average$3((long[]) obj, i);
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                IntPipeline.lambda$average$4((long[]) obj, (long[]) obj2);
            }
        }))[0] > 0) {
            return OptionalDouble.of(r0[1] / r0[0]);
        }
        return OptionalDouble.empty();
    }

    static void lambda$average$3(long[] jArr, int i) {
        jArr[0] = jArr[0] + 1;
        jArr[1] = jArr[1] + ((long) i);
    }

    static void lambda$average$4(long[] jArr, long[] jArr2) {
        jArr[0] = jArr[0] + jArr2[0];
        jArr[1] = jArr[1] + jArr2[1];
    }

    @Override
    public final IntSummaryStatistics summaryStatistics() {
        return (IntSummaryStatistics) collect($$Lambda$_Ea_sNpqZAwihIOCRBaP7hHgWWI.INSTANCE, new ObjIntConsumer() {
            @Override
            public final void accept(Object obj, int i) {
                ((IntSummaryStatistics) obj).accept(i);
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((IntSummaryStatistics) obj).combine((IntSummaryStatistics) obj2);
            }
        });
    }

    @Override
    public final int reduce(int i, IntBinaryOperator intBinaryOperator) {
        return ((Integer) evaluate(ReduceOps.makeInt(i, intBinaryOperator))).intValue();
    }

    @Override
    public final OptionalInt reduce(IntBinaryOperator intBinaryOperator) {
        return (OptionalInt) evaluate(ReduceOps.makeInt(intBinaryOperator));
    }

    @Override
    public final <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> objIntConsumer, final BiConsumer<R, R> biConsumer) {
        return (R) evaluate(ReduceOps.makeInt(supplier, objIntConsumer, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return IntPipeline.lambda$collect$5(biConsumer, obj, obj2);
            }
        }));
    }

    static Object lambda$collect$5(BiConsumer biConsumer, Object obj, Object obj2) {
        biConsumer.accept(obj, obj2);
        return obj;
    }

    @Override
    public final boolean anyMatch(IntPredicate intPredicate) {
        return ((Boolean) evaluate(MatchOps.makeInt(intPredicate, MatchOps.MatchKind.ANY))).booleanValue();
    }

    @Override
    public final boolean allMatch(IntPredicate intPredicate) {
        return ((Boolean) evaluate(MatchOps.makeInt(intPredicate, MatchOps.MatchKind.ALL))).booleanValue();
    }

    @Override
    public final boolean noneMatch(IntPredicate intPredicate) {
        return ((Boolean) evaluate(MatchOps.makeInt(intPredicate, MatchOps.MatchKind.NONE))).booleanValue();
    }

    @Override
    public final OptionalInt findFirst() {
        return (OptionalInt) evaluate(FindOps.makeInt(true));
    }

    @Override
    public final OptionalInt findAny() {
        return (OptionalInt) evaluate(FindOps.makeInt(false));
    }

    static Integer[] lambda$toArray$6(int i) {
        return new Integer[i];
    }

    @Override
    public final int[] toArray() {
        return Nodes.flattenInt((Node.OfInt) evaluateToArrayNode(new IntFunction() {
            @Override
            public final Object apply(int i) {
                return IntPipeline.lambda$toArray$6(i);
            }
        })).asPrimitiveArray();
    }

    public static class Head<E_IN> extends IntPipeline<E_IN> {
        @Override
        public IntStream parallel() {
            return (IntStream) super.parallel();
        }

        @Override
        public IntStream sequential() {
            return (IntStream) super.sequential();
        }

        public Head(Supplier<? extends Spliterator<Integer>> supplier, int i, boolean z) {
            super(supplier, i, z);
        }

        public Head(Spliterator<Integer> spliterator, int i, boolean z) {
            super(spliterator, i, z);
        }

        @Override
        public final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final Sink<E_IN> opWrapSink(int i, Sink<Integer> sink) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEach(IntConsumer intConsumer) {
            if (!isParallel()) {
                IntPipeline.adapt(sourceStageSpliterator()).forEachRemaining(intConsumer);
            } else {
                super.forEach(intConsumer);
            }
        }

        @Override
        public void forEachOrdered(IntConsumer intConsumer) {
            if (!isParallel()) {
                IntPipeline.adapt(sourceStageSpliterator()).forEachRemaining(intConsumer);
            } else {
                super.forEachOrdered(intConsumer);
            }
        }
    }

    public static abstract class StatelessOp<E_IN> extends IntPipeline<E_IN> {
        static final boolean $assertionsDisabled = false;

        @Override
        public IntStream parallel() {
            return (IntStream) super.parallel();
        }

        @Override
        public IntStream sequential() {
            return (IntStream) super.sequential();
        }

        public StatelessOp(AbstractPipeline<?, E_IN, ?> abstractPipeline, StreamShape streamShape, int i) {
            super(abstractPipeline, i);
        }

        @Override
        public final boolean opIsStateful() {
            return false;
        }
    }

    public static abstract class StatefulOp<E_IN> extends IntPipeline<E_IN> {
        static final boolean $assertionsDisabled = false;

        @Override
        public abstract <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Integer[]> intFunction);

        @Override
        public IntStream parallel() {
            return (IntStream) super.parallel();
        }

        @Override
        public IntStream sequential() {
            return (IntStream) super.sequential();
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
