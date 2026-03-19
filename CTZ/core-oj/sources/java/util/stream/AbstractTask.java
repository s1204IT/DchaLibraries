package java.util.stream;

import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.AbstractTask;

abstract class AbstractTask<P_IN, P_OUT, R, K extends AbstractTask<P_IN, P_OUT, R, K>> extends CountedCompleter<R> {
    static final int LEAF_TARGET = ForkJoinPool.getCommonPoolParallelism() << 2;
    protected final PipelineHelper<P_OUT> helper;
    protected K leftChild;
    private R localResult;
    protected K rightChild;
    protected Spliterator<P_IN> spliterator;
    protected long targetSize;

    protected abstract R doLeaf();

    protected abstract K makeChild(Spliterator<P_IN> spliterator);

    protected AbstractTask(PipelineHelper<P_OUT> pipelineHelper, Spliterator<P_IN> spliterator) {
        super(null);
        this.helper = pipelineHelper;
        this.spliterator = spliterator;
        this.targetSize = 0L;
    }

    protected AbstractTask(K k, Spliterator<P_IN> spliterator) {
        super(k);
        this.spliterator = spliterator;
        this.helper = k.helper;
        this.targetSize = k.targetSize;
    }

    public static long suggestTargetSize(long j) {
        long j2 = j / ((long) LEAF_TARGET);
        if (j2 > 0) {
            return j2;
        }
        return 1L;
    }

    protected final long getTargetSize(long j) {
        long j2 = this.targetSize;
        if (j2 != 0) {
            return j2;
        }
        long jSuggestTargetSize = suggestTargetSize(j);
        this.targetSize = jSuggestTargetSize;
        return jSuggestTargetSize;
    }

    @Override
    public R getRawResult() {
        return this.localResult;
    }

    @Override
    protected void setRawResult(R r) {
        if (r != null) {
            throw new IllegalStateException();
        }
    }

    protected R getLocalResult() {
        return this.localResult;
    }

    protected void setLocalResult(R r) {
        this.localResult = r;
    }

    protected boolean isLeaf() {
        return this.leftChild == null;
    }

    protected boolean isRoot() {
        return getParent() == null;
    }

    protected K getParent() {
        return (K) getCompleter();
    }

    @Override
    public void compute() {
        Spliterator<P_IN> spliteratorTrySplit;
        Spliterator<P_IN> spliterator = this.spliterator;
        long jEstimateSize = spliterator.estimateSize();
        long targetSize = getTargetSize(jEstimateSize);
        AbstractTask<P_IN, P_OUT, R, K> abstractTask = this;
        boolean z = false;
        while (jEstimateSize > targetSize && (spliteratorTrySplit = spliterator.trySplit()) != null) {
            AbstractTask<P_IN, P_OUT, R, K> abstractTaskMakeChild = abstractTask.makeChild(spliteratorTrySplit);
            abstractTask.leftChild = abstractTaskMakeChild;
            AbstractTask<P_IN, P_OUT, R, K> abstractTaskMakeChild2 = abstractTask.makeChild(spliterator);
            abstractTask.rightChild = abstractTaskMakeChild2;
            abstractTask.setPendingCount(1);
            if (z) {
                spliterator = spliteratorTrySplit;
                abstractTask = abstractTaskMakeChild;
                z = false;
                abstractTaskMakeChild = abstractTaskMakeChild2;
            } else {
                abstractTask = abstractTaskMakeChild2;
                z = true;
            }
            abstractTaskMakeChild.fork();
            jEstimateSize = spliterator.estimateSize();
        }
        abstractTask.setLocalResult(abstractTask.doLeaf());
        abstractTask.tryComplete();
    }

    @Override
    public void onCompletion(CountedCompleter<?> countedCompleter) {
        this.spliterator = null;
        this.rightChild = null;
        this.leftChild = null;
    }

    protected boolean isLeftmostNode() {
        AbstractTask<P_IN, P_OUT, R, K> abstractTask = this;
        while (abstractTask != null) {
            AbstractTask<P_IN, P_OUT, R, K> parent = abstractTask.getParent();
            if (parent == null || parent.leftChild == abstractTask) {
                abstractTask = parent;
            } else {
                return false;
            }
        }
        return true;
    }
}
