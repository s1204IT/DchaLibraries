package java.util.stream;

import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntPipeline;
import java.util.stream.LongPipeline;
import java.util.stream.MatchOps;
import java.util.stream.Node;
import java.util.stream.ReferencePipeline;
import java.util.stream.Sink;
import java.util.stream.StreamSpliterators;

public abstract class DoublePipeline<E_IN> extends AbstractPipeline<E_IN, Double, DoubleStream> implements DoubleStream {
    @Override
    public DoubleStream parallel() {
        return (DoubleStream) super.parallel();
    }

    @Override
    public DoubleStream sequential() {
        return (DoubleStream) super.sequential();
    }

    DoublePipeline(Supplier<? extends Spliterator<Double>> supplier, int i, boolean z) {
        super(supplier, i, z);
    }

    DoublePipeline(Spliterator<Double> spliterator, int i, boolean z) {
        super(spliterator, i, z);
    }

    DoublePipeline(AbstractPipeline<?, E_IN, ?> abstractPipeline, int i) {
        super(abstractPipeline, i);
    }

    private static DoubleConsumer adapt(Sink<Double> sink) {
        if (sink instanceof DoubleConsumer) {
            return (DoubleConsumer) sink;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using DoubleStream.adapt(Sink<Double> s)");
        }
        Objects.requireNonNull(sink);
        return new $$Lambda$G0LLxk8pWitjFgsOx2bYtROrGg(sink);
    }

    private static Spliterator.OfDouble adapt(Spliterator<Double> spliterator) {
        if (spliterator instanceof Spliterator.OfDouble) {
            return (Spliterator.OfDouble) spliterator;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using DoubleStream.adapt(Spliterator<Double> s)");
        }
        throw new UnsupportedOperationException("DoubleStream.adapt(Spliterator<Double> s)");
    }

    @Override
    public final StreamShape getOutputShape() {
        return StreamShape.DOUBLE_VALUE;
    }

    @Override
    public final <P_IN> Node<Double> evaluateToNode(PipelineHelper<Double> pipelineHelper, Spliterator<P_IN> spliterator, boolean z, IntFunction<Double[]> intFunction) {
        return Nodes.collectDouble(pipelineHelper, spliterator, z);
    }

    @Override
    public final <P_IN> Spliterator<Double> wrap(PipelineHelper<Double> pipelineHelper, Supplier<Spliterator<P_IN>> supplier, boolean z) {
        return new StreamSpliterators.DoubleWrappingSpliterator(pipelineHelper, supplier, z);
    }

    @Override
    public final Spliterator<Double> lazySpliterator2(Supplier<? extends Spliterator<Double>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator.OfDouble(supplier);
    }

    @Override
    public final void forEachWithCancel(Spliterator<Double> spliterator, Sink<Double> sink) {
        Spliterator.OfDouble ofDoubleAdapt = adapt(spliterator);
        DoubleConsumer doubleConsumerAdapt = adapt(sink);
        while (!sink.cancellationRequested() && ofDoubleAdapt.tryAdvance(doubleConsumerAdapt)) {
        }
    }

    @Override
    public final Node.Builder<Double> makeNodeBuilder(long j, IntFunction<Double[]> intFunction) {
        return Nodes.doubleBuilder(j);
    }

    @Override
    public final Iterator<Double> iterator() {
        return Spliterators.iterator((Spliterator.OfDouble) spliterator2());
    }

    @Override
    public final Spliterator<Double> spliterator2() {
        return adapt((Spliterator<Double>) super.spliterator2());
    }

    @Override
    public final Stream<Double> boxed() {
        return mapToObj(new DoubleFunction() {
            @Override
            public final Object apply(double d) {
                return Double.valueOf(d);
            }
        });
    }

    @Override
    public final DoubleStream map(final DoubleUnaryOperator doubleUnaryOperator) {
        Objects.requireNonNull(doubleUnaryOperator);
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Double> opWrapSink(int i, Sink<Double> sink) {
                return new Sink.ChainedDouble<Double>(sink) {
                    @Override
                    public void accept(double d) {
                        this.downstream.accept(doubleUnaryOperator.applyAsDouble(d));
                    }
                };
            }
        };
    }

    @Override
    public final <U> Stream<U> mapToObj(final DoubleFunction<? extends U> doubleFunction) {
        Objects.requireNonNull(doubleFunction);
        return new ReferencePipeline.StatelessOp<Double, U>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Double> opWrapSink(int i, Sink<U> sink) {
                return new Sink.ChainedDouble<U>(sink) {
                    @Override
                    public void accept(double d) {
                        this.downstream.accept((Object) doubleFunction.apply(d));
                    }
                };
            }
        };
    }

    @Override
    public final IntStream mapToInt(final DoubleToIntFunction doubleToIntFunction) {
        Objects.requireNonNull(doubleToIntFunction);
        return new IntPipeline.StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Double> opWrapSink(int i, Sink<Integer> sink) {
                return new Sink.ChainedDouble<Integer>(sink) {
                    @Override
                    public void accept(double d) {
                        this.downstream.accept(doubleToIntFunction.applyAsInt(d));
                    }
                };
            }
        };
    }

    @Override
    public final LongStream mapToLong(final DoubleToLongFunction doubleToLongFunction) {
        Objects.requireNonNull(doubleToLongFunction);
        return new LongPipeline.StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            public Sink<Double> opWrapSink(int i, Sink<Long> sink) {
                return new Sink.ChainedDouble<Long>(sink) {
                    @Override
                    public void accept(double d) {
                        this.downstream.accept(doubleToLongFunction.applyAsLong(d));
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream flatMap(final DoubleFunction<? extends DoubleStream> doubleFunction) {
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {

            class AnonymousClass1 extends Sink.ChainedDouble<Double> {
                AnonymousClass1(Sink sink) {
                    super(sink);
                }

                @Override
                public void begin(long j) {
                    this.downstream.begin(-1L);
                }

                @Override
                public void accept(double d) {
                    DoubleStream doubleStream = (DoubleStream) doubleFunction.apply(d);
                    if (doubleStream != null) {
                        Throwable th = null;
                        try {
                            doubleStream.sequential().forEach(new DoubleConsumer() {
                                @Override
                                public final void accept(double d2) {
                                    this.f$0.downstream.accept(d2);
                                }
                            });
                        } catch (Throwable th2) {
                            if (doubleStream != null) {
                                if (th != null) {
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
            }

            @Override
            public Sink<Double> opWrapSink(int i, Sink<Double> sink) {
                return new AnonymousClass1(sink);
            }
        };
    }

    @Override
    public DoubleStream unordered() {
        if (!isOrdered()) {
            return this;
        }
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_ORDERED) {
            @Override
            public Sink<Double> opWrapSink(int i, Sink<Double> sink) {
                return sink;
            }
        };
    }

    @Override
    public final DoubleStream filter(final DoublePredicate doublePredicate) {
        Objects.requireNonNull(doublePredicate);
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SIZED) {
            @Override
            public Sink<Double> opWrapSink(int i, Sink<Double> sink) {
                return new Sink.ChainedDouble<Double>(sink) {
                    @Override
                    public void begin(long j) {
                        this.downstream.begin(-1L);
                    }

                    @Override
                    public void accept(double d) {
                        if (doublePredicate.test(d)) {
                            this.downstream.accept(d);
                        }
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream peek(final DoubleConsumer doubleConsumer) {
        Objects.requireNonNull(doubleConsumer);
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, 0) {
            @Override
            public Sink<Double> opWrapSink(int i, Sink<Double> sink) {
                return new Sink.ChainedDouble<Double>(sink) {
                    @Override
                    public void accept(double d) {
                        doubleConsumer.accept(d);
                        this.downstream.accept(d);
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream limit(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(Long.toString(j));
        }
        return SliceOps.makeDouble(this, 0L, j);
    }

    @Override
    public final DoubleStream skip(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(Long.toString(j));
        }
        if (j == 0) {
            return this;
        }
        return SliceOps.makeDouble(this, j, -1L);
    }

    @Override
    public final DoubleStream sorted() {
        return SortedOps.makeDouble(this);
    }

    @Override
    public final DoubleStream distinct() {
        return boxed().distinct().mapToDouble(new ToDoubleFunction() {
            @Override
            public final double applyAsDouble(Object obj) {
                return ((Double) obj).doubleValue();
            }
        });
    }

    @Override
    public void forEach(DoubleConsumer doubleConsumer) {
        evaluate(ForEachOps.makeDouble(doubleConsumer, false));
    }

    @Override
    public void forEachOrdered(DoubleConsumer doubleConsumer) {
        evaluate(ForEachOps.makeDouble(doubleConsumer, true));
    }

    static double[] lambda$sum$1() {
        return new double[3];
    }

    @Override
    public final double sum() {
        return Collectors.computeFinalSum((double[]) collect(new Supplier() {
            @Override
            public final Object get() {
                return DoublePipeline.lambda$sum$1();
            }
        }, new ObjDoubleConsumer() {
            @Override
            public final void accept(Object obj, double d) {
                DoublePipeline.lambda$sum$2((double[]) obj, d);
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                DoublePipeline.lambda$sum$3((double[]) obj, (double[]) obj2);
            }
        }));
    }

    static void lambda$sum$2(double[] dArr, double d) {
        Collectors.sumWithCompensation(dArr, d);
        dArr[2] = dArr[2] + d;
    }

    static void lambda$sum$3(double[] dArr, double[] dArr2) {
        Collectors.sumWithCompensation(dArr, dArr2[0]);
        Collectors.sumWithCompensation(dArr, dArr2[1]);
        dArr[2] = dArr[2] + dArr2[2];
    }

    @Override
    public final OptionalDouble min() {
        return reduce(new DoubleBinaryOperator() {
            @Override
            public final double applyAsDouble(double d, double d2) {
                return Math.min(d, d2);
            }
        });
    }

    @Override
    public final OptionalDouble max() {
        return reduce(new DoubleBinaryOperator() {
            @Override
            public final double applyAsDouble(double d, double d2) {
                return Math.max(d, d2);
            }
        });
    }

    static double[] lambda$average$4() {
        return new double[4];
    }

    @Override
    public final OptionalDouble average() {
        double[] dArr = (double[]) collect(new Supplier() {
            @Override
            public final Object get() {
                return DoublePipeline.lambda$average$4();
            }
        }, new ObjDoubleConsumer() {
            @Override
            public final void accept(Object obj, double d) {
                DoublePipeline.lambda$average$5((double[]) obj, d);
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                DoublePipeline.lambda$average$6((double[]) obj, (double[]) obj2);
            }
        });
        if (dArr[2] > 0.0d) {
            return OptionalDouble.of(Collectors.computeFinalSum(dArr) / dArr[2]);
        }
        return OptionalDouble.empty();
    }

    static void lambda$average$5(double[] dArr, double d) {
        dArr[2] = dArr[2] + 1.0d;
        Collectors.sumWithCompensation(dArr, d);
        dArr[3] = dArr[3] + d;
    }

    static void lambda$average$6(double[] dArr, double[] dArr2) {
        Collectors.sumWithCompensation(dArr, dArr2[0]);
        Collectors.sumWithCompensation(dArr, dArr2[1]);
        dArr[2] = dArr[2] + dArr2[2];
        dArr[3] = dArr[3] + dArr2[3];
    }

    static long lambda$count$7(double d) {
        return 1L;
    }

    @Override
    public final long count() {
        return mapToLong(new DoubleToLongFunction() {
            @Override
            public final long applyAsLong(double d) {
                return DoublePipeline.lambda$count$7(d);
            }
        }).sum();
    }

    @Override
    public final DoubleSummaryStatistics summaryStatistics() {
        return (DoubleSummaryStatistics) collect($$Lambda$745FUy7cYwYu7KrMQTYh2DNqh1I.INSTANCE, new ObjDoubleConsumer() {
            @Override
            public final void accept(Object obj, double d) {
                ((DoubleSummaryStatistics) obj).accept(d);
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((DoubleSummaryStatistics) obj).combine((DoubleSummaryStatistics) obj2);
            }
        });
    }

    @Override
    public final double reduce(double d, DoubleBinaryOperator doubleBinaryOperator) {
        return ((Double) evaluate(ReduceOps.makeDouble(d, doubleBinaryOperator))).doubleValue();
    }

    @Override
    public final OptionalDouble reduce(DoubleBinaryOperator doubleBinaryOperator) {
        return (OptionalDouble) evaluate(ReduceOps.makeDouble(doubleBinaryOperator));
    }

    @Override
    public final <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> objDoubleConsumer, final BiConsumer<R, R> biConsumer) {
        return (R) evaluate(ReduceOps.makeDouble(supplier, objDoubleConsumer, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return DoublePipeline.lambda$collect$8(biConsumer, obj, obj2);
            }
        }));
    }

    static Object lambda$collect$8(BiConsumer biConsumer, Object obj, Object obj2) {
        biConsumer.accept(obj, obj2);
        return obj;
    }

    @Override
    public final boolean anyMatch(DoublePredicate doublePredicate) {
        return ((Boolean) evaluate(MatchOps.makeDouble(doublePredicate, MatchOps.MatchKind.ANY))).booleanValue();
    }

    @Override
    public final boolean allMatch(DoublePredicate doublePredicate) {
        return ((Boolean) evaluate(MatchOps.makeDouble(doublePredicate, MatchOps.MatchKind.ALL))).booleanValue();
    }

    @Override
    public final boolean noneMatch(DoublePredicate doublePredicate) {
        return ((Boolean) evaluate(MatchOps.makeDouble(doublePredicate, MatchOps.MatchKind.NONE))).booleanValue();
    }

    @Override
    public final OptionalDouble findFirst() {
        return (OptionalDouble) evaluate(FindOps.makeDouble(true));
    }

    @Override
    public final OptionalDouble findAny() {
        return (OptionalDouble) evaluate(FindOps.makeDouble(false));
    }

    static Double[] lambda$toArray$9(int i) {
        return new Double[i];
    }

    @Override
    public final double[] toArray() {
        return Nodes.flattenDouble((Node.OfDouble) evaluateToArrayNode(new IntFunction() {
            @Override
            public final Object apply(int i) {
                return DoublePipeline.lambda$toArray$9(i);
            }
        })).asPrimitiveArray();
    }

    public static class Head<E_IN> extends DoublePipeline<E_IN> {
        @Override
        public DoubleStream parallel() {
            return (DoubleStream) super.parallel();
        }

        @Override
        public DoubleStream sequential() {
            return (DoubleStream) super.sequential();
        }

        public Head(Supplier<? extends Spliterator<Double>> supplier, int i, boolean z) {
            super(supplier, i, z);
        }

        public Head(Spliterator<Double> spliterator, int i, boolean z) {
            super(spliterator, i, z);
        }

        @Override
        public final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final Sink<E_IN> opWrapSink(int i, Sink<Double> sink) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEach(DoubleConsumer doubleConsumer) {
            if (!isParallel()) {
                DoublePipeline.adapt(sourceStageSpliterator()).forEachRemaining(doubleConsumer);
            } else {
                super.forEach(doubleConsumer);
            }
        }

        @Override
        public void forEachOrdered(DoubleConsumer doubleConsumer) {
            if (!isParallel()) {
                DoublePipeline.adapt(sourceStageSpliterator()).forEachRemaining(doubleConsumer);
            } else {
                super.forEachOrdered(doubleConsumer);
            }
        }
    }

    public static abstract class StatelessOp<E_IN> extends DoublePipeline<E_IN> {
        static final boolean $assertionsDisabled = false;

        @Override
        public DoubleStream parallel() {
            return (DoubleStream) super.parallel();
        }

        @Override
        public DoubleStream sequential() {
            return (DoubleStream) super.sequential();
        }

        public StatelessOp(AbstractPipeline<?, E_IN, ?> abstractPipeline, StreamShape streamShape, int i) {
            super(abstractPipeline, i);
        }

        @Override
        public final boolean opIsStateful() {
            return false;
        }
    }

    public static abstract class StatefulOp<E_IN> extends DoublePipeline<E_IN> {
        static final boolean $assertionsDisabled = false;

        @Override
        public abstract <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Double[]> intFunction);

        @Override
        public DoubleStream parallel() {
            return (DoubleStream) super.parallel();
        }

        @Override
        public DoubleStream sequential() {
            return (DoubleStream) super.sequential();
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
