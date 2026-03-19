package java.util.concurrent;

public interface Future<V> {
    boolean cancel(boolean z);

    V get() throws ExecutionException, InterruptedException;

    V get(long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException;

    boolean isCancelled();

    boolean isDone();
}
