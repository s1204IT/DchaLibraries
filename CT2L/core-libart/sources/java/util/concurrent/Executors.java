package java.util.concurrent;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Executors {
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());
    }

    public static ExecutorService newWorkStealingPool(int parallelism) {
        return new ForkJoinPool(parallelism, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    public static ExecutorService newWorkStealingPool() {
        return new ForkJoinPool(Runtime.getRuntime().availableProcessors(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), threadFactory);
    }

    public static ExecutorService newSingleThreadExecutor() {
        return new FinalizableDelegatedExecutorService(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue()));
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new FinalizableDelegatedExecutorService(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), threadFactory));
    }

    public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue());
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), threadFactory);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return new DelegatedScheduledExecutorService(new ScheduledThreadPoolExecutor(1));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return new DelegatedScheduledExecutorService(new ScheduledThreadPoolExecutor(1, threadFactory));
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return new ScheduledThreadPoolExecutor(corePoolSize);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
    }

    public static ExecutorService unconfigurableExecutorService(ExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        return new DelegatedExecutorService(executor);
    }

    public static ScheduledExecutorService unconfigurableScheduledExecutorService(ScheduledExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        return new DelegatedScheduledExecutorService(executor);
    }

    public static ThreadFactory defaultThreadFactory() {
        return new DefaultThreadFactory();
    }

    public static ThreadFactory privilegedThreadFactory() {
        return new PrivilegedThreadFactory();
    }

    public static <T> Callable<T> callable(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        return new RunnableAdapter(task, result);
    }

    public static Callable<Object> callable(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        return new RunnableAdapter(task, null);
    }

    public static Callable<Object> callable(final PrivilegedAction<?> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return new Callable<Object>() {
            @Override
            public Object call() {
                return action.run();
            }
        };
    }

    public static Callable<Object> callable(final PrivilegedExceptionAction<?> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return action.run();
            }
        };
    }

    public static <T> Callable<T> privilegedCallable(Callable<T> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        return new PrivilegedCallable(callable);
    }

    public static <T> Callable<T> privilegedCallableUsingCurrentClassLoader(Callable<T> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        return new PrivilegedCallableUsingCurrentClassLoader(callable);
    }

    static final class RunnableAdapter<T> implements Callable<T> {
        final T result;
        final Runnable task;

        RunnableAdapter(Runnable task, T result) {
            this.task = task;
            this.result = result;
        }

        @Override
        public T call() {
            this.task.run();
            return this.result;
        }
    }

    static final class PrivilegedCallable<T> implements Callable<T> {
        private final AccessControlContext acc = AccessController.getContext();
        private final Callable<T> task;

        PrivilegedCallable(Callable<T> task) {
            this.task = task;
        }

        @Override
        public T call() throws Exception {
            try {
                return (T) AccessController.doPrivileged(new PrivilegedExceptionAction<T>() {
                    @Override
                    public T run() throws Exception {
                        return (T) PrivilegedCallable.this.task.call();
                    }
                }, this.acc);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }

    static final class PrivilegedCallableUsingCurrentClassLoader<T> implements Callable<T> {
        private final AccessControlContext acc = AccessController.getContext();
        private final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        private final Callable<T> task;

        PrivilegedCallableUsingCurrentClassLoader(Callable<T> task) {
            this.task = task;
        }

        @Override
        public T call() throws Exception {
            try {
                return (T) AccessController.doPrivileged(new PrivilegedExceptionAction<T>() {
                    @Override
                    public T run() throws Exception {
                        Thread threadCurrentThread = Thread.currentThread();
                        ClassLoader contextClassLoader = threadCurrentThread.getContextClassLoader();
                        if (PrivilegedCallableUsingCurrentClassLoader.this.ccl == contextClassLoader) {
                            return (T) PrivilegedCallableUsingCurrentClassLoader.this.task.call();
                        }
                        threadCurrentThread.setContextClassLoader(PrivilegedCallableUsingCurrentClassLoader.this.ccl);
                        try {
                            return (T) PrivilegedCallableUsingCurrentClassLoader.this.task.call();
                        } finally {
                            threadCurrentThread.setContextClassLoader(contextClassLoader);
                        }
                    }
                }, this.acc);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }

    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            this.group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = "pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(this.group, r, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != 5) {
                t.setPriority(5);
            }
            return t;
        }
    }

    static class PrivilegedThreadFactory extends DefaultThreadFactory {
        private final AccessControlContext acc = AccessController.getContext();
        private final ClassLoader ccl = Thread.currentThread().getContextClassLoader();

        PrivilegedThreadFactory() {
        }

        @Override
        public Thread newThread(final Runnable r) {
            return super.newThread(new Runnable() {
                @Override
                public void run() {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            Thread.currentThread().setContextClassLoader(PrivilegedThreadFactory.this.ccl);
                            r.run();
                            return null;
                        }
                    }, PrivilegedThreadFactory.this.acc);
                }
            });
        }
    }

    static class DelegatedExecutorService extends AbstractExecutorService {
        private final ExecutorService e;

        DelegatedExecutorService(ExecutorService executor) {
            this.e = executor;
        }

        @Override
        public void execute(Runnable command) {
            this.e.execute(command);
        }

        @Override
        public void shutdown() {
            this.e.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return this.e.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return this.e.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return this.e.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return this.e.awaitTermination(timeout, unit);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return this.e.submit(task);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return this.e.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return this.e.submit(task, result);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return this.e.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return this.e.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws ExecutionException, InterruptedException {
            return (T) this.e.invokeAny(collection);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> collection, long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
            return (T) this.e.invokeAny(collection, j, timeUnit);
        }
    }

    static class FinalizableDelegatedExecutorService extends DelegatedExecutorService {
        FinalizableDelegatedExecutorService(ExecutorService executor) {
            super(executor);
        }

        protected void finalize() {
            super.shutdown();
        }
    }

    static class DelegatedScheduledExecutorService extends DelegatedExecutorService implements ScheduledExecutorService {
        private final ScheduledExecutorService e;

        DelegatedScheduledExecutorService(ScheduledExecutorService executor) {
            super(executor);
            this.e = executor;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return this.e.schedule(command, delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return this.e.schedule(callable, delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return this.e.scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return this.e.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }
    }

    private Executors() {
    }
}
