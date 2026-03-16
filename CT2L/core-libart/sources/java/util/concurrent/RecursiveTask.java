package java.util.concurrent;

public abstract class RecursiveTask<V> extends ForkJoinTask<V> {
    private static final long serialVersionUID = 5232453952276485270L;
    V result;

    protected abstract V compute();

    @Override
    public final V getRawResult() {
        return this.result;
    }

    @Override
    protected final void setRawResult(V value) {
        this.result = value;
    }

    @Override
    protected final boolean exec() {
        this.result = compute();
        return true;
    }
}
