package java.util.stream;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.Node;

public abstract class AbstractPipeline<E_IN, E_OUT, S extends BaseStream<E_OUT, S>> extends PipelineHelper<E_OUT> implements BaseStream<E_OUT, S> {
    static final boolean $assertionsDisabled = false;
    private static final String MSG_CONSUMED = "source already consumed or closed";
    private static final String MSG_STREAM_LINKED = "stream has already been operated upon or closed";
    private int combinedFlags;
    private int depth;
    private boolean linkedOrConsumed;
    private AbstractPipeline nextStage;
    private boolean parallel;
    private final AbstractPipeline previousStage;
    private boolean sourceAnyStateful;
    private Runnable sourceCloseAction;
    protected final int sourceOrOpFlags;
    private Spliterator<?> sourceSpliterator;
    private final AbstractPipeline sourceStage;
    private Supplier<? extends Spliterator<?>> sourceSupplier;

    public abstract <P_IN> Node<E_OUT> evaluateToNode(PipelineHelper<E_OUT> pipelineHelper, Spliterator<P_IN> spliterator, boolean z, IntFunction<E_OUT[]> intFunction);

    public abstract void forEachWithCancel(Spliterator<E_OUT> spliterator, Sink<E_OUT> sink);

    public abstract StreamShape getOutputShape();

    public abstract Spliterator<E_OUT> lazySpliterator(Supplier<? extends Spliterator<E_OUT>> supplier);

    @Override
    public abstract Node.Builder<E_OUT> makeNodeBuilder(long j, IntFunction<E_OUT[]> intFunction);

    public abstract boolean opIsStateful();

    public abstract Sink<E_IN> opWrapSink(int i, Sink<E_OUT> sink);

    public abstract <P_IN> Spliterator<E_OUT> wrap(PipelineHelper<E_OUT> pipelineHelper, Supplier<Spliterator<P_IN>> supplier, boolean z);

    AbstractPipeline(Supplier<? extends Spliterator<?>> supplier, int i, boolean z) {
        this.previousStage = null;
        this.sourceSupplier = supplier;
        this.sourceStage = this;
        this.sourceOrOpFlags = StreamOpFlag.STREAM_MASK & i;
        this.combinedFlags = (~(this.sourceOrOpFlags << 1)) & StreamOpFlag.INITIAL_OPS_VALUE;
        this.depth = 0;
        this.parallel = z;
    }

    AbstractPipeline(Spliterator<?> spliterator, int i, boolean z) {
        this.previousStage = null;
        this.sourceSpliterator = spliterator;
        this.sourceStage = this;
        this.sourceOrOpFlags = StreamOpFlag.STREAM_MASK & i;
        this.combinedFlags = (~(this.sourceOrOpFlags << 1)) & StreamOpFlag.INITIAL_OPS_VALUE;
        this.depth = 0;
        this.parallel = z;
    }

    AbstractPipeline(AbstractPipeline<?, E_IN, ?> abstractPipeline, int i) {
        if (abstractPipeline.linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        abstractPipeline.linkedOrConsumed = true;
        abstractPipeline.nextStage = this;
        this.previousStage = abstractPipeline;
        this.sourceOrOpFlags = StreamOpFlag.OP_MASK & i;
        this.combinedFlags = StreamOpFlag.combineOpFlags(i, abstractPipeline.combinedFlags);
        this.sourceStage = abstractPipeline.sourceStage;
        if (opIsStateful()) {
            this.sourceStage.sourceAnyStateful = true;
        }
        this.depth = abstractPipeline.depth + 1;
    }

    final <R> R evaluate(TerminalOp<E_OUT, R> terminalOp) {
        if (this.linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        this.linkedOrConsumed = true;
        if (isParallel()) {
            return terminalOp.evaluateParallel(this, sourceSpliterator(terminalOp.getOpFlags()));
        }
        return terminalOp.evaluateSequential(this, sourceSpliterator(terminalOp.getOpFlags()));
    }

    public final Node<E_OUT> evaluateToArrayNode(IntFunction<E_OUT[]> intFunction) {
        if (this.linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        this.linkedOrConsumed = true;
        if (!isParallel() || this.previousStage == null || !opIsStateful()) {
            return evaluate(sourceSpliterator(0), true, intFunction);
        }
        this.depth = 0;
        return opEvaluateParallel(this.previousStage, this.previousStage.sourceSpliterator(0), intFunction);
    }

    final Spliterator<E_OUT> sourceStageSpliterator() {
        if (this != this.sourceStage) {
            throw new IllegalStateException();
        }
        if (this.linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        this.linkedOrConsumed = true;
        if (this.sourceStage.sourceSpliterator != null) {
            Spliterator<E_OUT> spliterator = (Spliterator<E_OUT>) this.sourceStage.sourceSpliterator;
            this.sourceStage.sourceSpliterator = null;
            return spliterator;
        }
        if (this.sourceStage.sourceSupplier != null) {
            Spliterator<E_OUT> spliterator2 = (Spliterator) this.sourceStage.sourceSupplier.get();
            this.sourceStage.sourceSupplier = null;
            return spliterator2;
        }
        throw new IllegalStateException(MSG_CONSUMED);
    }

    @Override
    public final S sequential() {
        this.sourceStage.parallel = $assertionsDisabled;
        return this;
    }

    @Override
    public final S parallel() {
        this.sourceStage.parallel = true;
        return this;
    }

    @Override
    public void close() {
        this.linkedOrConsumed = true;
        this.sourceSupplier = null;
        this.sourceSpliterator = null;
        if (this.sourceStage.sourceCloseAction != null) {
            Runnable runnable = this.sourceStage.sourceCloseAction;
            this.sourceStage.sourceCloseAction = null;
            runnable.run();
        }
    }

    @Override
    public S onClose(Runnable runnable) {
        Runnable runnable2 = this.sourceStage.sourceCloseAction;
        AbstractPipeline abstractPipeline = this.sourceStage;
        if (runnable2 != null) {
            runnable = Streams.composeWithExceptions(runnable2, runnable);
        }
        abstractPipeline.sourceCloseAction = runnable;
        return this;
    }

    @Override
    public Spliterator<E_OUT> spliterator2() {
        if (this.linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        this.linkedOrConsumed = true;
        if (this == this.sourceStage) {
            if (this.sourceStage.sourceSpliterator != null) {
                Spliterator<E_OUT> spliterator = (Spliterator<E_OUT>) this.sourceStage.sourceSpliterator;
                this.sourceStage.sourceSpliterator = null;
                return spliterator;
            }
            if (this.sourceStage.sourceSupplier != null) {
                Supplier<? extends Spliterator<?>> supplier = this.sourceStage.sourceSupplier;
                this.sourceStage.sourceSupplier = null;
                return lazySpliterator(supplier);
            }
            throw new IllegalStateException(MSG_CONSUMED);
        }
        return wrap(this, new Supplier() {
            @Override
            public final Object get() {
                return this.f$0.sourceSpliterator(0);
            }
        }, isParallel());
    }

    @Override
    public final boolean isParallel() {
        return this.sourceStage.parallel;
    }

    public final int getStreamFlags() {
        return StreamOpFlag.toStreamFlags(this.combinedFlags);
    }

    private Spliterator<?> sourceSpliterator(int i) {
        Spliterator<?> spliteratorOpEvaluateParallelLazy;
        if (this.sourceStage.sourceSpliterator != null) {
            spliteratorOpEvaluateParallelLazy = this.sourceStage.sourceSpliterator;
            this.sourceStage.sourceSpliterator = null;
        } else if (this.sourceStage.sourceSupplier != null) {
            spliteratorOpEvaluateParallelLazy = this.sourceStage.sourceSupplier.get();
            this.sourceStage.sourceSupplier = null;
        } else {
            throw new IllegalStateException(MSG_CONSUMED);
        }
        if (isParallel() && this.sourceStage.sourceAnyStateful) {
            AbstractPipeline abstractPipeline = this.sourceStage;
            AbstractPipeline abstractPipeline2 = this.sourceStage.nextStage;
            int i2 = 1;
            while (abstractPipeline != this) {
                int i3 = abstractPipeline2.sourceOrOpFlags;
                if (abstractPipeline2.opIsStateful()) {
                    i2 = 0;
                    if (StreamOpFlag.SHORT_CIRCUIT.isKnown(i3)) {
                        i3 &= ~StreamOpFlag.IS_SHORT_CIRCUIT;
                    }
                    spliteratorOpEvaluateParallelLazy = abstractPipeline2.opEvaluateParallelLazy(abstractPipeline, spliteratorOpEvaluateParallelLazy);
                    if (spliteratorOpEvaluateParallelLazy.hasCharacteristics(64)) {
                        i3 = (i3 & (~StreamOpFlag.NOT_SIZED)) | StreamOpFlag.IS_SIZED;
                    } else {
                        i3 = (i3 & (~StreamOpFlag.IS_SIZED)) | StreamOpFlag.NOT_SIZED;
                    }
                }
                abstractPipeline2.depth = i2;
                abstractPipeline2.combinedFlags = StreamOpFlag.combineOpFlags(i3, abstractPipeline.combinedFlags);
                i2++;
                AbstractPipeline abstractPipeline3 = abstractPipeline2;
                abstractPipeline2 = abstractPipeline2.nextStage;
                abstractPipeline = abstractPipeline3;
            }
        }
        if (i != 0) {
            this.combinedFlags = StreamOpFlag.combineOpFlags(i, this.combinedFlags);
        }
        return spliteratorOpEvaluateParallelLazy;
    }

    @Override
    final StreamShape getSourceShape() {
        AbstractPipeline<E_IN, E_OUT, S> abstractPipeline = this;
        while (abstractPipeline.depth > 0) {
            abstractPipeline = abstractPipeline.previousStage;
        }
        return abstractPipeline.getOutputShape();
    }

    @Override
    final <P_IN> long exactOutputSizeIfKnown(Spliterator<P_IN> spliterator) {
        if (StreamOpFlag.SIZED.isKnown(getStreamAndOpFlags())) {
            return spliterator.getExactSizeIfKnown();
        }
        return -1L;
    }

    @Override
    final Sink wrapAndCopyInto(Sink sink, Spliterator spliterator) {
        copyInto(wrapSink((Sink) Objects.requireNonNull(sink)), spliterator);
        return sink;
    }

    @Override
    final <P_IN> void copyInto(Sink<P_IN> sink, Spliterator<P_IN> spliterator) {
        Objects.requireNonNull(sink);
        if (!StreamOpFlag.SHORT_CIRCUIT.isKnown(getStreamAndOpFlags())) {
            sink.begin(spliterator.getExactSizeIfKnown());
            spliterator.forEachRemaining(sink);
            sink.end();
            return;
        }
        copyIntoWithCancel(sink, spliterator);
    }

    @Override
    final <P_IN> void copyIntoWithCancel(Sink<P_IN> sink, Spliterator<P_IN> spliterator) {
        AbstractPipeline<E_IN, E_OUT, S> abstractPipeline = this;
        while (abstractPipeline.depth > 0) {
            abstractPipeline = abstractPipeline.previousStage;
        }
        sink.begin(spliterator.getExactSizeIfKnown());
        abstractPipeline.forEachWithCancel(spliterator, sink);
        sink.end();
    }

    @Override
    public final int getStreamAndOpFlags() {
        return this.combinedFlags;
    }

    final boolean isOrdered() {
        return StreamOpFlag.ORDERED.isKnown(this.combinedFlags);
    }

    @Override
    public final <P_IN> Sink<P_IN> wrapSink(Sink<E_OUT> sink) {
        Objects.requireNonNull(sink);
        Sink<E_OUT> sink2 = (Sink<P_IN>) sink;
        for (AbstractPipeline<E_IN, E_OUT, S> abstractPipeline = this; abstractPipeline.depth > 0; abstractPipeline = abstractPipeline.previousStage) {
            sink2 = (Sink<P_IN>) abstractPipeline.opWrapSink(abstractPipeline.previousStage.combinedFlags, sink2);
        }
        return (Sink<P_IN>) sink2;
    }

    @Override
    final <P_IN> Spliterator<E_OUT> wrapSpliterator(final Spliterator<P_IN> spliterator) {
        if (this.depth == 0) {
            return spliterator;
        }
        return wrap(this, new Supplier() {
            @Override
            public final Object get() {
                return AbstractPipeline.lambda$wrapSpliterator$1(spliterator);
            }
        }, isParallel());
    }

    static Spliterator lambda$wrapSpliterator$1(Spliterator spliterator) {
        return spliterator;
    }

    @Override
    public final <P_IN> Node<E_OUT> evaluate(Spliterator<P_IN> spliterator, boolean z, IntFunction<E_OUT[]> intFunction) {
        if (isParallel()) {
            return evaluateToNode(this, spliterator, z, intFunction);
        }
        return ((Node.Builder) wrapAndCopyInto(makeNodeBuilder(exactOutputSizeIfKnown(spliterator), intFunction), spliterator)).build2();
    }

    public <P_IN> Node<E_OUT> opEvaluateParallel(PipelineHelper<E_OUT> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<E_OUT[]> intFunction) {
        throw new UnsupportedOperationException("Parallel evaluation is not supported");
    }

    static Object[] lambda$opEvaluateParallelLazy$2(int i) {
        return new Object[i];
    }

    public <P_IN> Spliterator<E_OUT> opEvaluateParallelLazy(PipelineHelper<E_OUT> pipelineHelper, Spliterator<P_IN> spliterator) {
        return opEvaluateParallel(pipelineHelper, spliterator, new IntFunction() {
            @Override
            public final Object apply(int i) {
                return AbstractPipeline.lambda$opEvaluateParallelLazy$2(i);
            }
        }).spliterator();
    }
}
