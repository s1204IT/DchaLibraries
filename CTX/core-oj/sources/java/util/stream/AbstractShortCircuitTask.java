package java.util.stream;

import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.AbstractShortCircuitTask;

abstract class AbstractShortCircuitTask<P_IN, P_OUT, R, K extends AbstractShortCircuitTask<P_IN, P_OUT, R, K>> extends AbstractTask<P_IN, P_OUT, R, K> {
    protected volatile boolean canceled;
    protected final AtomicReference<R> sharedResult;

    protected abstract R getEmptyResult();

    protected AbstractShortCircuitTask(PipelineHelper<P_OUT> pipelineHelper, Spliterator<P_IN> spliterator) {
        super(pipelineHelper, spliterator);
        this.sharedResult = new AtomicReference<>(null);
    }

    protected AbstractShortCircuitTask(K k, Spliterator<P_IN> spliterator) {
        super(k, spliterator);
        this.sharedResult = k.sharedResult;
    }

    @Override
    public void compute() {
        R emptyResult;
        Spliterator<P_IN> spliteratorTrySplit;
        Spliterator<P_IN> spliterator = this.spliterator;
        long jEstimateSize = spliterator.estimateSize();
        long targetSize = getTargetSize(jEstimateSize);
        AtomicReference<R> atomicReference = this.sharedResult;
        Spliterator<P_IN> spliterator2 = spliterator;
        boolean z = false;
        AbstractShortCircuitTask<P_IN, P_OUT, R, K> abstractShortCircuitTask = this;
        while (true) {
            emptyResult = atomicReference.get();
            if (emptyResult != null) {
                break;
            }
            if (abstractShortCircuitTask.taskCanceled()) {
                emptyResult = abstractShortCircuitTask.getEmptyResult();
                break;
            }
            if (jEstimateSize <= targetSize || (spliteratorTrySplit = spliterator2.trySplit()) == null) {
                break;
            }
            K kMakeChild = abstractShortCircuitTask.makeChild(spliteratorTrySplit);
            abstractShortCircuitTask.leftChild = kMakeChild;
            K kMakeChild2 = abstractShortCircuitTask.makeChild(spliterator2);
            abstractShortCircuitTask.rightChild = kMakeChild2;
            abstractShortCircuitTask.setPendingCount(1);
            if (z) {
                spliterator2 = spliteratorTrySplit;
                abstractShortCircuitTask = kMakeChild;
                z = false;
                kMakeChild = kMakeChild2;
            } else {
                abstractShortCircuitTask = kMakeChild2;
                z = true;
            }
            kMakeChild.fork();
            jEstimateSize = spliterator2.estimateSize();
        }
        emptyResult = abstractShortCircuitTask.doLeaf();
        abstractShortCircuitTask.setLocalResult(emptyResult);
        abstractShortCircuitTask.tryComplete();
    }

    protected void shortCircuit(R r) {
        if (r != null) {
            this.sharedResult.compareAndSet(null, r);
        }
    }

    @Override
    protected void setLocalResult(R r) {
        if (isRoot()) {
            if (r != null) {
                this.sharedResult.compareAndSet(null, r);
                return;
            }
            return;
        }
        super.setLocalResult(r);
    }

    @Override
    public R getRawResult() {
        return getLocalResult();
    }

    @Override
    public R getLocalResult() {
        if (isRoot()) {
            R r = this.sharedResult.get();
            return r == null ? getEmptyResult() : r;
        }
        return (R) super.getLocalResult();
    }

    protected void cancel() {
        this.canceled = true;
    }

    protected boolean taskCanceled() {
        boolean z = this.canceled;
        if (!z) {
            K parent = getParent();
            while (true) {
                AbstractShortCircuitTask abstractShortCircuitTask = parent;
                if (z || abstractShortCircuitTask == null) {
                    break;
                }
                z = abstractShortCircuitTask.canceled;
                parent = (K) abstractShortCircuitTask.getParent();
            }
        }
        return z;
    }

    protected void cancelLaterNodes() {
        AbstractShortCircuitTask<P_IN, P_OUT, R, K> abstractShortCircuitTask = this;
        for (K parent = getParent(); parent != null; parent = parent.getParent()) {
            if (parent.leftChild == abstractShortCircuitTask) {
                AbstractShortCircuitTask abstractShortCircuitTask2 = (AbstractShortCircuitTask) parent.rightChild;
                if (!abstractShortCircuitTask2.canceled) {
                    abstractShortCircuitTask2.cancel();
                }
            }
            abstractShortCircuitTask = parent;
        }
    }
}
