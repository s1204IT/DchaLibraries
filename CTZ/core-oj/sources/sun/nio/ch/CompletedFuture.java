package sun.nio.ch;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

final class CompletedFuture<V> implements Future<V> {
    private final Throwable exc;
    private final V result;

    private CompletedFuture(V v, Throwable th) {
        this.result = v;
        this.exc = th;
    }

    static <V> CompletedFuture<V> withResult(V v) {
        return new CompletedFuture<>(v, null);
    }

    static <V> CompletedFuture<V> withFailure(Throwable th) {
        if (!(th instanceof IOException) && !(th instanceof SecurityException)) {
            th = new IOException(th);
        }
        return new CompletedFuture<>(null, th);
    }

    static <V> CompletedFuture<V> withResult(V v, Throwable th) {
        if (th == null) {
            return withResult(v);
        }
        return withFailure(th);
    }

    @Override
    public V get() throws ExecutionException {
        if (this.exc != null) {
            throw new ExecutionException(this.exc);
        }
        return this.result;
    }

    @Override
    public V get(long j, TimeUnit timeUnit) throws ExecutionException {
        if (timeUnit == null) {
            throw new NullPointerException();
        }
        if (this.exc != null) {
            throw new ExecutionException(this.exc);
        }
        return this.result;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean cancel(boolean z) {
        return false;
    }
}
