package java.util.stream;

import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountedCompleter;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.stream.ForEachOps;
import java.util.stream.Node;
import java.util.stream.Sink;

final class ForEachOps {
    private ForEachOps() {
    }

    public static <T> TerminalOp<T, Void> makeRef(Consumer<? super T> consumer, boolean z) {
        Objects.requireNonNull(consumer);
        return new ForEachOp.OfRef(consumer, z);
    }

    public static TerminalOp<Integer, Void> makeInt(IntConsumer intConsumer, boolean z) {
        Objects.requireNonNull(intConsumer);
        return new ForEachOp.OfInt(intConsumer, z);
    }

    public static TerminalOp<Long, Void> makeLong(LongConsumer longConsumer, boolean z) {
        Objects.requireNonNull(longConsumer);
        return new ForEachOp.OfLong(longConsumer, z);
    }

    public static TerminalOp<Double, Void> makeDouble(DoubleConsumer doubleConsumer, boolean z) {
        Objects.requireNonNull(doubleConsumer);
        return new ForEachOp.OfDouble(doubleConsumer, z);
    }

    static abstract class ForEachOp<T> implements TerminalOp<T, Void>, TerminalSink<T, Void> {
        private final boolean ordered;

        protected ForEachOp(boolean z) {
            this.ordered = z;
        }

        @Override
        public int getOpFlags() {
            if (this.ordered) {
                return 0;
            }
            return StreamOpFlag.NOT_ORDERED;
        }

        @Override
        public <S> Void evaluateSequential(PipelineHelper<T> pipelineHelper, Spliterator<S> spliterator) {
            return ((ForEachOp) pipelineHelper.wrapAndCopyInto(this, spliterator)).get();
        }

        @Override
        public <S> Void evaluateParallel(PipelineHelper<T> pipelineHelper, Spliterator<S> spliterator) {
            if (this.ordered) {
                new ForEachOrderedTask(pipelineHelper, spliterator, this).invoke();
                return null;
            }
            new ForEachTask(pipelineHelper, spliterator, pipelineHelper.wrapSink(this)).invoke();
            return null;
        }

        @Override
        public Void get() {
            return null;
        }

        static final class OfRef<T> extends ForEachOp<T> {
            final Consumer<? super T> consumer;

            OfRef(Consumer<? super T> consumer, boolean z) {
                super(z);
                this.consumer = consumer;
            }

            @Override
            public void accept(T t) {
                this.consumer.accept(t);
            }
        }

        static final class OfInt extends ForEachOp<Integer> implements Sink.OfInt {
            final IntConsumer consumer;

            OfInt(IntConsumer intConsumer, boolean z) {
                super(z);
                this.consumer = intConsumer;
            }

            @Override
            public StreamShape inputShape() {
                return StreamShape.INT_VALUE;
            }

            @Override
            public void accept(int i) {
                this.consumer.accept(i);
            }
        }

        static final class OfLong extends ForEachOp<Long> implements Sink.OfLong {
            final LongConsumer consumer;

            OfLong(LongConsumer longConsumer, boolean z) {
                super(z);
                this.consumer = longConsumer;
            }

            @Override
            public StreamShape inputShape() {
                return StreamShape.LONG_VALUE;
            }

            @Override
            public void accept(long j) {
                this.consumer.accept(j);
            }
        }

        static final class OfDouble extends ForEachOp<Double> implements Sink.OfDouble {
            final DoubleConsumer consumer;

            OfDouble(DoubleConsumer doubleConsumer, boolean z) {
                super(z);
                this.consumer = doubleConsumer;
            }

            @Override
            public StreamShape inputShape() {
                return StreamShape.DOUBLE_VALUE;
            }

            @Override
            public void accept(double d) {
                this.consumer.accept(d);
            }
        }
    }

    static final class ForEachTask<S, T> extends CountedCompleter<Void> {
        private final PipelineHelper<T> helper;
        private final Sink<S> sink;
        private Spliterator<S> spliterator;
        private long targetSize;

        ForEachTask(PipelineHelper<T> pipelineHelper, Spliterator<S> spliterator, Sink<S> sink) {
            super(null);
            this.sink = sink;
            this.helper = pipelineHelper;
            this.spliterator = spliterator;
            this.targetSize = 0L;
        }

        ForEachTask(ForEachTask<S, T> forEachTask, Spliterator<S> spliterator) {
            super(forEachTask);
            this.spliterator = spliterator;
            this.sink = forEachTask.sink;
            this.targetSize = forEachTask.targetSize;
            this.helper = forEachTask.helper;
        }

        @Override
        public void compute() {
            Spliterator<S> spliteratorTrySplit;
            Spliterator<S> spliterator = this.spliterator;
            long jEstimateSize = spliterator.estimateSize();
            long jSuggestTargetSize = this.targetSize;
            if (jSuggestTargetSize == 0) {
                jSuggestTargetSize = AbstractTask.suggestTargetSize(jEstimateSize);
                this.targetSize = jSuggestTargetSize;
            }
            boolean zIsKnown = StreamOpFlag.SHORT_CIRCUIT.isKnown(this.helper.getStreamAndOpFlags());
            Sink<S> sink = this.sink;
            ForEachTask<S, T> forEachTask = this;
            boolean z = false;
            while (true) {
                if (zIsKnown && sink.cancellationRequested()) {
                    break;
                }
                if (jEstimateSize <= jSuggestTargetSize || (spliteratorTrySplit = spliterator.trySplit()) == null) {
                    break;
                }
                ForEachTask<S, T> forEachTask2 = new ForEachTask<>(forEachTask, spliteratorTrySplit);
                forEachTask.addToPendingCount(1);
                if (z) {
                    spliterator = spliteratorTrySplit;
                    z = false;
                    ForEachTask<S, T> forEachTask3 = forEachTask;
                    forEachTask = forEachTask2;
                    forEachTask2 = forEachTask3;
                } else {
                    z = true;
                }
                forEachTask2.fork();
                jEstimateSize = spliterator.estimateSize();
            }
            forEachTask.helper.copyInto(sink, spliterator);
            forEachTask.spliterator = null;
            forEachTask.propagateCompletion();
        }
    }

    static final class ForEachOrderedTask<S, T> extends CountedCompleter<Void> {
        private final Sink<T> action;
        private final ConcurrentHashMap<ForEachOrderedTask<S, T>, ForEachOrderedTask<S, T>> completionMap;
        private final PipelineHelper<T> helper;
        private final ForEachOrderedTask<S, T> leftPredecessor;
        private Node<T> node;
        private Spliterator<S> spliterator;
        private final long targetSize;

        protected ForEachOrderedTask(PipelineHelper<T> pipelineHelper, Spliterator<S> spliterator, Sink<T> sink) {
            super(null);
            this.helper = pipelineHelper;
            this.spliterator = spliterator;
            this.targetSize = AbstractTask.suggestTargetSize(spliterator.estimateSize());
            this.completionMap = new ConcurrentHashMap<>(Math.max(16, AbstractTask.LEAF_TARGET << 1));
            this.action = sink;
            this.leftPredecessor = null;
        }

        ForEachOrderedTask(ForEachOrderedTask<S, T> forEachOrderedTask, Spliterator<S> spliterator, ForEachOrderedTask<S, T> forEachOrderedTask2) {
            super(forEachOrderedTask);
            this.helper = forEachOrderedTask.helper;
            this.spliterator = spliterator;
            this.targetSize = forEachOrderedTask.targetSize;
            this.completionMap = forEachOrderedTask.completionMap;
            this.action = forEachOrderedTask.action;
            this.leftPredecessor = forEachOrderedTask2;
        }

        @Override
        public final void compute() {
            doCompute(this);
        }

        private static <S, T> void doCompute(ForEachOrderedTask<S, T> forEachOrderedTask) {
            Spliterator<S> spliteratorTrySplit;
            Spliterator<S> spliterator = ((ForEachOrderedTask) forEachOrderedTask).spliterator;
            long j = ((ForEachOrderedTask) forEachOrderedTask).targetSize;
            boolean z = false;
            while (spliterator.estimateSize() > j && (spliteratorTrySplit = spliterator.trySplit()) != null) {
                ForEachOrderedTask<S, T> forEachOrderedTask2 = new ForEachOrderedTask<>(forEachOrderedTask, spliteratorTrySplit, ((ForEachOrderedTask) forEachOrderedTask).leftPredecessor);
                ForEachOrderedTask<S, T> forEachOrderedTask3 = new ForEachOrderedTask<>(forEachOrderedTask, spliterator, forEachOrderedTask2);
                forEachOrderedTask.addToPendingCount(1);
                forEachOrderedTask3.addToPendingCount(1);
                ((ForEachOrderedTask) forEachOrderedTask).completionMap.put(forEachOrderedTask2, forEachOrderedTask3);
                if (((ForEachOrderedTask) forEachOrderedTask).leftPredecessor != null) {
                    forEachOrderedTask2.addToPendingCount(1);
                    if (((ForEachOrderedTask) forEachOrderedTask).completionMap.replace(((ForEachOrderedTask) forEachOrderedTask).leftPredecessor, forEachOrderedTask, forEachOrderedTask2)) {
                        forEachOrderedTask.addToPendingCount(-1);
                    } else {
                        forEachOrderedTask2.addToPendingCount(-1);
                    }
                }
                if (z) {
                    z = false;
                    spliterator = spliteratorTrySplit;
                    forEachOrderedTask = forEachOrderedTask2;
                    forEachOrderedTask2 = forEachOrderedTask3;
                } else {
                    forEachOrderedTask = forEachOrderedTask3;
                    z = true;
                }
                forEachOrderedTask2.fork();
            }
            if (forEachOrderedTask.getPendingCount() > 0) {
                ((ForEachOrderedTask) forEachOrderedTask).node = ((Node.Builder) ((ForEachOrderedTask) forEachOrderedTask).helper.wrapAndCopyInto(((ForEachOrderedTask) forEachOrderedTask).helper.makeNodeBuilder(((ForEachOrderedTask) forEachOrderedTask).helper.exactOutputSizeIfKnown(spliterator), new IntFunction() {
                    @Override
                    public final Object apply(int i) {
                        return ForEachOps.ForEachOrderedTask.lambda$doCompute$0(i);
                    }
                }), spliterator)).build2();
                ((ForEachOrderedTask) forEachOrderedTask).spliterator = null;
            }
            forEachOrderedTask.tryComplete();
        }

        static Object[] lambda$doCompute$0(int i) {
            return new Object[i];
        }

        @Override
        public void onCompletion(CountedCompleter<?> countedCompleter) {
            if (this.node != null) {
                this.node.forEach(this.action);
                this.node = null;
            } else if (this.spliterator != null) {
                this.helper.wrapAndCopyInto(this.action, this.spliterator);
                this.spliterator = null;
            }
            ForEachOrderedTask<S, T> forEachOrderedTaskRemove = this.completionMap.remove(this);
            if (forEachOrderedTaskRemove != null) {
                forEachOrderedTaskRemove.tryComplete();
            }
        }
    }
}
