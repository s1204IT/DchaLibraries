package java.util.concurrent;

public class ExecutorCompletionService<V> implements CompletionService<V> {
    private final AbstractExecutorService aes;
    private final BlockingQueue<Future<V>> completionQueue;
    private final Executor executor;

    private class QueueingFuture extends FutureTask<Void> {
        private final Future<V> task;

        QueueingFuture(RunnableFuture<V> task) {
            super(task, null);
            this.task = task;
        }

        @Override
        protected void done() {
            ExecutorCompletionService.this.completionQueue.add(this.task);
        }
    }

    private RunnableFuture<V> newTaskFor(Callable<V> task) {
        return this.aes == null ? new FutureTask(task) : this.aes.newTaskFor(task);
    }

    private RunnableFuture<V> newTaskFor(Runnable task, V result) {
        return this.aes == null ? new FutureTask(task, result) : this.aes.newTaskFor(task, result);
    }

    public ExecutorCompletionService(Executor executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.aes = executor instanceof AbstractExecutorService ? (AbstractExecutorService) executor : null;
        this.completionQueue = new LinkedBlockingQueue();
    }

    public ExecutorCompletionService(Executor executor, BlockingQueue<Future<V>> completionQueue) {
        if (executor == null || completionQueue == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.aes = executor instanceof AbstractExecutorService ? (AbstractExecutorService) executor : null;
        this.completionQueue = completionQueue;
    }

    @Override
    public Future<V> submit(Callable<V> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<V> f = newTaskFor(task);
        this.executor.execute(new QueueingFuture(f));
        return f;
    }

    @Override
    public Future<V> submit(Runnable task, V result) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<V> f = newTaskFor(task, result);
        this.executor.execute(new QueueingFuture(f));
        return f;
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
    public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return this.completionQueue.poll(timeout, unit);
    }
}
