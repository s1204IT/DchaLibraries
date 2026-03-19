package java.util.concurrent;

public class ExecutorCompletionService<V> implements CompletionService<V> {
    private final AbstractExecutorService aes;
    private final BlockingQueue<Future<V>> completionQueue;
    private final Executor executor;

    private static class QueueingFuture<V> extends FutureTask<Void> {
        private final BlockingQueue<Future<V>> completionQueue;
        private final Future<V> task;

        QueueingFuture(RunnableFuture<V> runnableFuture, BlockingQueue<Future<V>> blockingQueue) {
            super(runnableFuture, null);
            this.task = runnableFuture;
            this.completionQueue = blockingQueue;
        }

        @Override
        protected void done() {
            this.completionQueue.add(this.task);
        }
    }

    private RunnableFuture<V> newTaskFor(Callable<V> callable) {
        if (this.aes == null) {
            return new FutureTask(callable);
        }
        return this.aes.newTaskFor(callable);
    }

    private RunnableFuture<V> newTaskFor(Runnable runnable, V v) {
        if (this.aes == null) {
            return new FutureTask(runnable, v);
        }
        return this.aes.newTaskFor(runnable, v);
    }

    public ExecutorCompletionService(Executor executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.aes = executor instanceof AbstractExecutorService ? (AbstractExecutorService) executor : null;
        this.completionQueue = new LinkedBlockingQueue();
    }

    public ExecutorCompletionService(Executor executor, BlockingQueue<Future<V>> blockingQueue) {
        if (executor == null || blockingQueue == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.aes = executor instanceof AbstractExecutorService ? (AbstractExecutorService) executor : null;
        this.completionQueue = blockingQueue;
    }

    @Override
    public Future<V> submit(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        RunnableFuture<V> runnableFutureNewTaskFor = newTaskFor(callable);
        this.executor.execute(new QueueingFuture(runnableFutureNewTaskFor, this.completionQueue));
        return runnableFutureNewTaskFor;
    }

    @Override
    public Future<V> submit(Runnable runnable, V v) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        RunnableFuture<V> runnableFutureNewTaskFor = newTaskFor(runnable, v);
        this.executor.execute(new QueueingFuture(runnableFutureNewTaskFor, this.completionQueue));
        return runnableFutureNewTaskFor;
    }

    @Override
    public Future<V> take() throws InterruptedException {
        return this.completionQueue.take();
    }

    @Override
    public Future<V> poll() {
        return this.completionQueue.poll();
    }

    @Override
    public Future<V> poll(long j, TimeUnit timeUnit) throws InterruptedException {
        return this.completionQueue.poll(j, timeUnit);
    }
}
