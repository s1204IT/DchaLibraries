package java.util.stream;

import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.function.IntFunction;
import java.util.stream.DoublePipeline;
import java.util.stream.IntPipeline;
import java.util.stream.LongPipeline;
import java.util.stream.Node;
import java.util.stream.ReferencePipeline;
import java.util.stream.Sink;
import java.util.stream.SliceOps;
import java.util.stream.StreamSpliterators;

final class SliceOps {
    static final boolean $assertionsDisabled = false;

    private SliceOps() {
    }

    private static long calcSize(long j, long j2, long j3) {
        if (j >= 0) {
            return Math.max(-1L, Math.min(j - j2, j3));
        }
        return -1L;
    }

    private static long calcSliceFence(long j, long j2) {
        long j3 = j2 >= 0 ? j + j2 : Long.MAX_VALUE;
        return j3 >= 0 ? j3 : Long.MAX_VALUE;
    }

    private static <P_IN> Spliterator<P_IN> sliceSpliterator(StreamShape streamShape, Spliterator<P_IN> spliterator, long j, long j2) {
        long jCalcSliceFence = calcSliceFence(j, j2);
        switch (streamShape) {
            case REFERENCE:
                return new StreamSpliterators.SliceSpliterator.OfRef(spliterator, j, jCalcSliceFence);
            case INT_VALUE:
                return new StreamSpliterators.SliceSpliterator.OfInt((Spliterator.OfInt) spliterator, j, jCalcSliceFence);
            case LONG_VALUE:
                return new StreamSpliterators.SliceSpliterator.OfLong((Spliterator.OfLong) spliterator, j, jCalcSliceFence);
            case DOUBLE_VALUE:
                return new StreamSpliterators.SliceSpliterator.OfDouble((Spliterator.OfDouble) spliterator, j, jCalcSliceFence);
            default:
                throw new IllegalStateException("Unknown shape " + ((Object) streamShape));
        }
    }

    private static <T> IntFunction<T[]> castingArray() {
        return new IntFunction() {
            @Override
            public final Object apply(int i) {
                return SliceOps.lambda$castingArray$0(i);
            }
        };
    }

    static Object[] lambda$castingArray$0(int i) {
        return new Object[i];
    }

    public static <T> Stream<T> makeRef(AbstractPipeline<?, T, ?> abstractPipeline, final long j, final long j2) {
        if (j < 0) {
            throw new IllegalArgumentException("Skip must be non-negative: " + j);
        }
        return new ReferencePipeline.StatefulOp<T, T>(abstractPipeline, StreamShape.REFERENCE, flags(j2)) {
            Spliterator<T> unorderedSkipLimitSpliterator(Spliterator<T> spliterator, long j3, long j4, long j5) {
                long j6;
                long jMin;
                if (j3 <= j5) {
                    jMin = j4 >= 0 ? Math.min(j4, j5 - j3) : j5 - j3;
                    j6 = 0;
                } else {
                    j6 = j3;
                    jMin = j4;
                }
                return new StreamSpliterators.UnorderedSliceSpliterator.OfRef(spliterator, j6, jMin);
            }

            @Override
            public <P_IN> Spliterator<T> opEvaluateParallelLazy(PipelineHelper<T> pipelineHelper, Spliterator<P_IN> spliterator) {
                Spliterator<P_IN> spliterator2;
                long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
                if (jExactOutputSizeIfKnown > 0) {
                    spliterator2 = spliterator;
                    if (spliterator2.hasCharacteristics(16384)) {
                        return new StreamSpliterators.SliceSpliterator.OfRef(pipelineHelper.wrapSpliterator(spliterator), j, SliceOps.calcSliceFence(j, j2));
                    }
                } else {
                    spliterator2 = spliterator;
                }
                if (!StreamOpFlag.ORDERED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                    return unorderedSkipLimitSpliterator(pipelineHelper.wrapSpliterator(spliterator), j, j2, jExactOutputSizeIfKnown);
                }
                return new SliceTask(this, pipelineHelper, spliterator2, SliceOps.castingArray(), j, j2).invoke().spliterator();
            }

            @Override
            public <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<T[]> intFunction) {
                PipelineHelper<T> pipelineHelper2;
                Spliterator<P_IN> spliterator2;
                long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
                if (jExactOutputSizeIfKnown > 0) {
                    spliterator2 = spliterator;
                    if (spliterator2.hasCharacteristics(16384)) {
                        return Nodes.collect(pipelineHelper, SliceOps.sliceSpliterator(pipelineHelper.getSourceShape(), spliterator2, j, j2), true, intFunction);
                    }
                    pipelineHelper2 = pipelineHelper;
                } else {
                    pipelineHelper2 = pipelineHelper;
                    spliterator2 = spliterator;
                }
                if (!StreamOpFlag.ORDERED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                    return Nodes.collect(this, unorderedSkipLimitSpliterator(pipelineHelper.wrapSpliterator(spliterator), j, j2, jExactOutputSizeIfKnown), true, intFunction);
                }
                return (Node) new SliceTask(this, pipelineHelper2, spliterator2, intFunction, j, j2).invoke();
            }

            @Override
            public Sink<T> opWrapSink(int i, Sink<T> sink) {
                return new Sink.ChainedReference<T, T>(sink) {
                    long m;
                    long n;

                    {
                        this.n = j;
                        this.m = j2 >= 0 ? j2 : Long.MAX_VALUE;
                    }

                    @Override
                    public void begin(long j3) {
                        this.downstream.begin(SliceOps.calcSize(j3, j, this.m));
                    }

                    @Override
                    public void accept(T t) {
                        if (this.n != 0) {
                            this.n--;
                        } else if (this.m > 0) {
                            this.m--;
                            this.downstream.accept((Object) t);
                        }
                    }

                    @Override
                    public boolean cancellationRequested() {
                        return this.m == 0 || this.downstream.cancellationRequested();
                    }
                };
            }
        };
    }

    public static IntStream makeInt(AbstractPipeline<?, Integer, ?> abstractPipeline, long j, long j2) {
        if (j < 0) {
            throw new IllegalArgumentException("Skip must be non-negative: " + j);
        }
        return new AnonymousClass2(abstractPipeline, StreamShape.INT_VALUE, flags(j2), j, j2);
    }

    class AnonymousClass2 extends IntPipeline.StatefulOp<Integer> {
        final long val$limit;
        final long val$skip;

        AnonymousClass2(AbstractPipeline abstractPipeline, StreamShape streamShape, int i, long j, long j2) {
            super(abstractPipeline, streamShape, i);
            this.val$skip = j;
            this.val$limit = j2;
        }

        Spliterator.OfInt unorderedSkipLimitSpliterator(Spliterator.OfInt ofInt, long j, long j2, long j3) {
            long j4;
            long jMin;
            if (j <= j3) {
                jMin = j2 >= 0 ? Math.min(j2, j3 - j) : j3 - j;
                j4 = 0;
            } else {
                j4 = j;
                jMin = j2;
            }
            return new StreamSpliterators.UnorderedSliceSpliterator.OfInt(ofInt, j4, jMin);
        }

        @Override
        public <P_IN> Spliterator<Integer> opEvaluateParallelLazy(PipelineHelper<Integer> pipelineHelper, Spliterator<P_IN> spliterator) {
            Spliterator<P_IN> spliterator2;
            long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
            if (jExactOutputSizeIfKnown > 0) {
                spliterator2 = spliterator;
                if (spliterator2.hasCharacteristics(16384)) {
                    return new StreamSpliterators.SliceSpliterator.OfInt((Spliterator.OfInt) pipelineHelper.wrapSpliterator(spliterator), this.val$skip, SliceOps.calcSliceFence(this.val$skip, this.val$limit));
                }
            } else {
                spliterator2 = spliterator;
            }
            if (!StreamOpFlag.ORDERED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return unorderedSkipLimitSpliterator((Spliterator.OfInt) pipelineHelper.wrapSpliterator(spliterator), this.val$skip, this.val$limit, jExactOutputSizeIfKnown);
            }
            return new SliceTask(this, pipelineHelper, spliterator2, new IntFunction() {
                @Override
                public final Object apply(int i) {
                    return SliceOps.AnonymousClass2.lambda$opEvaluateParallelLazy$0(i);
                }
            }, this.val$skip, this.val$limit).invoke().spliterator();
        }

        static Integer[] lambda$opEvaluateParallelLazy$0(int i) {
            return new Integer[i];
        }

        @Override
        public <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Integer[]> intFunction) {
            PipelineHelper<Integer> pipelineHelper2;
            Spliterator<P_IN> spliterator2;
            long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
            if (jExactOutputSizeIfKnown > 0) {
                spliterator2 = spliterator;
                if (spliterator2.hasCharacteristics(16384)) {
                    return Nodes.collectInt(pipelineHelper, SliceOps.sliceSpliterator(pipelineHelper.getSourceShape(), spliterator2, this.val$skip, this.val$limit), true);
                }
                pipelineHelper2 = pipelineHelper;
            } else {
                pipelineHelper2 = pipelineHelper;
                spliterator2 = spliterator;
            }
            if (!StreamOpFlag.ORDERED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return Nodes.collectInt(this, unorderedSkipLimitSpliterator((Spliterator.OfInt) pipelineHelper.wrapSpliterator(spliterator), this.val$skip, this.val$limit, jExactOutputSizeIfKnown), true);
            }
            return (Node) new SliceTask(this, pipelineHelper2, spliterator2, intFunction, this.val$skip, this.val$limit).invoke();
        }

        @Override
        public Sink<Integer> opWrapSink(int i, Sink<Integer> sink) {
            return new Sink.ChainedInt<Integer>(sink) {
                long m;
                long n;

                {
                    this.n = AnonymousClass2.this.val$skip;
                    this.m = AnonymousClass2.this.val$limit >= 0 ? AnonymousClass2.this.val$limit : Long.MAX_VALUE;
                }

                @Override
                public void begin(long j) {
                    this.downstream.begin(SliceOps.calcSize(j, AnonymousClass2.this.val$skip, this.m));
                }

                @Override
                public void accept(int i2) {
                    if (this.n != 0) {
                        this.n--;
                    } else if (this.m > 0) {
                        this.m--;
                        this.downstream.accept(i2);
                    }
                }

                @Override
                public boolean cancellationRequested() {
                    return this.m == 0 || this.downstream.cancellationRequested();
                }
            };
        }
    }

    public static LongStream makeLong(AbstractPipeline<?, Long, ?> abstractPipeline, long j, long j2) {
        if (j < 0) {
            throw new IllegalArgumentException("Skip must be non-negative: " + j);
        }
        return new AnonymousClass3(abstractPipeline, StreamShape.LONG_VALUE, flags(j2), j, j2);
    }

    class AnonymousClass3 extends LongPipeline.StatefulOp<Long> {
        final long val$limit;
        final long val$skip;

        AnonymousClass3(AbstractPipeline abstractPipeline, StreamShape streamShape, int i, long j, long j2) {
            super(abstractPipeline, streamShape, i);
            this.val$skip = j;
            this.val$limit = j2;
        }

        Spliterator.OfLong unorderedSkipLimitSpliterator(Spliterator.OfLong ofLong, long j, long j2, long j3) {
            long j4;
            long jMin;
            if (j <= j3) {
                jMin = j2 >= 0 ? Math.min(j2, j3 - j) : j3 - j;
                j4 = 0;
            } else {
                j4 = j;
                jMin = j2;
            }
            return new StreamSpliterators.UnorderedSliceSpliterator.OfLong(ofLong, j4, jMin);
        }

        @Override
        public <P_IN> Spliterator<Long> opEvaluateParallelLazy(PipelineHelper<Long> pipelineHelper, Spliterator<P_IN> spliterator) {
            Spliterator<P_IN> spliterator2;
            long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
            if (jExactOutputSizeIfKnown > 0) {
                spliterator2 = spliterator;
                if (spliterator2.hasCharacteristics(16384)) {
                    return new StreamSpliterators.SliceSpliterator.OfLong((Spliterator.OfLong) pipelineHelper.wrapSpliterator(spliterator), this.val$skip, SliceOps.calcSliceFence(this.val$skip, this.val$limit));
                }
            } else {
                spliterator2 = spliterator;
            }
            if (!StreamOpFlag.ORDERED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return unorderedSkipLimitSpliterator((Spliterator.OfLong) pipelineHelper.wrapSpliterator(spliterator), this.val$skip, this.val$limit, jExactOutputSizeIfKnown);
            }
            return new SliceTask(this, pipelineHelper, spliterator2, new IntFunction() {
                @Override
                public final Object apply(int i) {
                    return SliceOps.AnonymousClass3.lambda$opEvaluateParallelLazy$0(i);
                }
            }, this.val$skip, this.val$limit).invoke().spliterator();
        }

        static Long[] lambda$opEvaluateParallelLazy$0(int i) {
            return new Long[i];
        }

        @Override
        public <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Long[]> intFunction) {
            PipelineHelper<Long> pipelineHelper2;
            Spliterator<P_IN> spliterator2;
            long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
            if (jExactOutputSizeIfKnown > 0) {
                spliterator2 = spliterator;
                if (spliterator2.hasCharacteristics(16384)) {
                    return Nodes.collectLong(pipelineHelper, SliceOps.sliceSpliterator(pipelineHelper.getSourceShape(), spliterator2, this.val$skip, this.val$limit), true);
                }
                pipelineHelper2 = pipelineHelper;
            } else {
                pipelineHelper2 = pipelineHelper;
                spliterator2 = spliterator;
            }
            if (!StreamOpFlag.ORDERED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return Nodes.collectLong(this, unorderedSkipLimitSpliterator((Spliterator.OfLong) pipelineHelper.wrapSpliterator(spliterator), this.val$skip, this.val$limit, jExactOutputSizeIfKnown), true);
            }
            return (Node) new SliceTask(this, pipelineHelper2, spliterator2, intFunction, this.val$skip, this.val$limit).invoke();
        }

        @Override
        public Sink<Long> opWrapSink(int i, Sink<Long> sink) {
            return new Sink.ChainedLong<Long>(sink) {
                long m;
                long n;

                {
                    this.n = AnonymousClass3.this.val$skip;
                    this.m = AnonymousClass3.this.val$limit >= 0 ? AnonymousClass3.this.val$limit : Long.MAX_VALUE;
                }

                @Override
                public void begin(long j) {
                    this.downstream.begin(SliceOps.calcSize(j, AnonymousClass3.this.val$skip, this.m));
                }

                @Override
                public void accept(long j) {
                    if (this.n != 0) {
                        this.n--;
                    } else if (this.m > 0) {
                        this.m--;
                        this.downstream.accept(j);
                    }
                }

                @Override
                public boolean cancellationRequested() {
                    return this.m == 0 || this.downstream.cancellationRequested();
                }
            };
        }
    }

    public static DoubleStream makeDouble(AbstractPipeline<?, Double, ?> abstractPipeline, long j, long j2) {
        if (j < 0) {
            throw new IllegalArgumentException("Skip must be non-negative: " + j);
        }
        return new AnonymousClass4(abstractPipeline, StreamShape.DOUBLE_VALUE, flags(j2), j, j2);
    }

    class AnonymousClass4 extends DoublePipeline.StatefulOp<Double> {
        final long val$limit;
        final long val$skip;

        AnonymousClass4(AbstractPipeline abstractPipeline, StreamShape streamShape, int i, long j, long j2) {
            super(abstractPipeline, streamShape, i);
            this.val$skip = j;
            this.val$limit = j2;
        }

        Spliterator.OfDouble unorderedSkipLimitSpliterator(Spliterator.OfDouble ofDouble, long j, long j2, long j3) {
            long j4;
            long jMin;
            if (j <= j3) {
                jMin = j2 >= 0 ? Math.min(j2, j3 - j) : j3 - j;
                j4 = 0;
            } else {
                j4 = j;
                jMin = j2;
            }
            return new StreamSpliterators.UnorderedSliceSpliterator.OfDouble(ofDouble, j4, jMin);
        }

        @Override
        public <P_IN> Spliterator<Double> opEvaluateParallelLazy(PipelineHelper<Double> pipelineHelper, Spliterator<P_IN> spliterator) {
            Spliterator<P_IN> spliterator2;
            long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
            if (jExactOutputSizeIfKnown > 0) {
                spliterator2 = spliterator;
                if (spliterator2.hasCharacteristics(16384)) {
                    return new StreamSpliterators.SliceSpliterator.OfDouble((Spliterator.OfDouble) pipelineHelper.wrapSpliterator(spliterator), this.val$skip, SliceOps.calcSliceFence(this.val$skip, this.val$limit));
                }
            } else {
                spliterator2 = spliterator;
            }
            if (!StreamOpFlag.ORDERED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return unorderedSkipLimitSpliterator((Spliterator.OfDouble) pipelineHelper.wrapSpliterator(spliterator), this.val$skip, this.val$limit, jExactOutputSizeIfKnown);
            }
            return new SliceTask(this, pipelineHelper, spliterator2, new IntFunction() {
                @Override
                public final Object apply(int i) {
                    return SliceOps.AnonymousClass4.lambda$opEvaluateParallelLazy$0(i);
                }
            }, this.val$skip, this.val$limit).invoke().spliterator();
        }

        static Double[] lambda$opEvaluateParallelLazy$0(int i) {
            return new Double[i];
        }

        @Override
        public <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Double[]> intFunction) {
            PipelineHelper<Double> pipelineHelper2;
            Spliterator<P_IN> spliterator2;
            long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
            if (jExactOutputSizeIfKnown > 0) {
                spliterator2 = spliterator;
                if (spliterator2.hasCharacteristics(16384)) {
                    return Nodes.collectDouble(pipelineHelper, SliceOps.sliceSpliterator(pipelineHelper.getSourceShape(), spliterator2, this.val$skip, this.val$limit), true);
                }
                pipelineHelper2 = pipelineHelper;
            } else {
                pipelineHelper2 = pipelineHelper;
                spliterator2 = spliterator;
            }
            if (!StreamOpFlag.ORDERED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return Nodes.collectDouble(this, unorderedSkipLimitSpliterator((Spliterator.OfDouble) pipelineHelper.wrapSpliterator(spliterator), this.val$skip, this.val$limit, jExactOutputSizeIfKnown), true);
            }
            return (Node) new SliceTask(this, pipelineHelper2, spliterator2, intFunction, this.val$skip, this.val$limit).invoke();
        }

        @Override
        public Sink<Double> opWrapSink(int i, Sink<Double> sink) {
            return new Sink.ChainedDouble<Double>(sink) {
                long m;
                long n;

                {
                    this.n = AnonymousClass4.this.val$skip;
                    this.m = AnonymousClass4.this.val$limit >= 0 ? AnonymousClass4.this.val$limit : Long.MAX_VALUE;
                }

                @Override
                public void begin(long j) {
                    this.downstream.begin(SliceOps.calcSize(j, AnonymousClass4.this.val$skip, this.m));
                }

                @Override
                public void accept(double d) {
                    if (this.n != 0) {
                        this.n--;
                    } else if (this.m > 0) {
                        this.m--;
                        this.downstream.accept(d);
                    }
                }

                @Override
                public boolean cancellationRequested() {
                    return this.m == 0 || this.downstream.cancellationRequested();
                }
            };
        }
    }

    private static int flags(long j) {
        return (j != -1 ? StreamOpFlag.IS_SHORT_CIRCUIT : 0) | StreamOpFlag.NOT_SIZED;
    }

    private static final class SliceTask<P_IN, P_OUT> extends AbstractShortCircuitTask<P_IN, P_OUT, Node<P_OUT>, SliceTask<P_IN, P_OUT>> {
        private volatile boolean completed;
        private final IntFunction<P_OUT[]> generator;
        private final AbstractPipeline<P_OUT, P_OUT, ?> op;
        private final long targetOffset;
        private final long targetSize;
        private long thisNodeSize;

        SliceTask(AbstractPipeline<P_OUT, P_OUT, ?> abstractPipeline, PipelineHelper<P_OUT> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<P_OUT[]> intFunction, long j, long j2) {
            super(pipelineHelper, spliterator);
            this.op = abstractPipeline;
            this.generator = intFunction;
            this.targetOffset = j;
            this.targetSize = j2;
        }

        SliceTask(SliceTask<P_IN, P_OUT> sliceTask, Spliterator<P_IN> spliterator) {
            super(sliceTask, spliterator);
            this.op = sliceTask.op;
            this.generator = sliceTask.generator;
            this.targetOffset = sliceTask.targetOffset;
            this.targetSize = sliceTask.targetSize;
        }

        @Override
        protected SliceTask<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator) {
            return new SliceTask<>(this, spliterator);
        }

        @Override
        protected final Node<P_OUT> getEmptyResult() {
            return Nodes.emptyNode(this.op.getOutputShape());
        }

        @Override
        protected final Node<P_OUT> doLeaf() {
            if (isRoot()) {
                Node.Builder<P_OUT> builderMakeNodeBuilder = this.op.makeNodeBuilder(StreamOpFlag.SIZED.isPreserved(this.op.sourceOrOpFlags) ? this.op.exactOutputSizeIfKnown(this.spliterator) : -1L, this.generator);
                this.helper.copyIntoWithCancel(this.helper.wrapSink(this.op.opWrapSink(this.helper.getStreamAndOpFlags(), builderMakeNodeBuilder)), this.spliterator);
                return builderMakeNodeBuilder.build2();
            }
            Node<P_OUT> nodeBuild2 = ((Node.Builder) this.helper.wrapAndCopyInto(this.helper.makeNodeBuilder(-1L, this.generator), this.spliterator)).build2();
            this.thisNodeSize = nodeBuild2.count();
            this.completed = true;
            this.spliterator = null;
            return nodeBuild2;
        }

        @Override
        public final void onCompletion(CountedCompleter<?> countedCompleter) {
            Node<P_OUT> nodeConc;
            if (!isLeaf()) {
                this.thisNodeSize = ((SliceTask) this.leftChild).thisNodeSize + ((SliceTask) this.rightChild).thisNodeSize;
                if (this.canceled) {
                    this.thisNodeSize = 0L;
                    nodeConc = getEmptyResult();
                } else if (this.thisNodeSize == 0) {
                    nodeConc = getEmptyResult();
                } else if (((SliceTask) this.leftChild).thisNodeSize == 0) {
                    nodeConc = ((SliceTask) this.rightChild).getLocalResult();
                } else {
                    nodeConc = Nodes.conc(this.op.getOutputShape(), ((SliceTask) this.leftChild).getLocalResult(), ((SliceTask) this.rightChild).getLocalResult());
                }
                if (isRoot()) {
                    nodeConc = doTruncate(nodeConc);
                }
                setLocalResult(nodeConc);
                this.completed = true;
            }
            if (this.targetSize >= 0 && !isRoot() && isLeftCompleted(this.targetOffset + this.targetSize)) {
                cancelLaterNodes();
            }
            super.onCompletion(countedCompleter);
        }

        @Override
        protected void cancel() {
            super.cancel();
            if (this.completed) {
                setLocalResult(getEmptyResult());
            }
        }

        private Node<P_OUT> doTruncate(Node<P_OUT> node) {
            return node.truncate(this.targetOffset, this.targetSize >= 0 ? Math.min(node.count(), this.targetOffset + this.targetSize) : this.thisNodeSize, this.generator);
        }

        private boolean isLeftCompleted(long j) {
            SliceTask sliceTask;
            long jCompletedSize = this.completed ? this.thisNodeSize : completedSize(j);
            if (jCompletedSize >= j) {
                return true;
            }
            SliceTask<P_IN, P_OUT> sliceTask2 = (SliceTask) getParent();
            long jCompletedSize2 = jCompletedSize;
            SliceTask<P_IN, P_OUT> sliceTask3 = this;
            while (sliceTask2 != null) {
                if (sliceTask3 == sliceTask2.rightChild && (sliceTask = (SliceTask) sliceTask2.leftChild) != null) {
                    jCompletedSize2 += sliceTask.completedSize(j);
                    if (jCompletedSize2 >= j) {
                        return true;
                    }
                }
                SliceTask<P_IN, P_OUT> sliceTask4 = sliceTask2;
                sliceTask2 = (SliceTask) sliceTask2.getParent();
                sliceTask3 = sliceTask4;
            }
            return jCompletedSize2 >= j;
        }

        private long completedSize(long j) {
            if (this.completed) {
                return this.thisNodeSize;
            }
            SliceTask sliceTask = (SliceTask) this.leftChild;
            SliceTask sliceTask2 = (SliceTask) this.rightChild;
            if (sliceTask == null || sliceTask2 == null) {
                return this.thisNodeSize;
            }
            long jCompletedSize = sliceTask.completedSize(j);
            return jCompletedSize >= j ? jCompletedSize : jCompletedSize + sliceTask2.completedSize(j);
        }
    }
}
