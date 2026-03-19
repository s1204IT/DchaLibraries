package java.util.concurrent;

import dalvik.annotation.optimization.ReachabilitySensitive;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Executors {
    public static ExecutorService newFixedThreadPool(int i) {
        return new ThreadPoolExecutor(i, i, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());
    }

    public static ExecutorService newWorkStealingPool(int i) {
        return new ForkJoinPool(i, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    public static ExecutorService newWorkStealingPool() {
        return new ForkJoinPool(Runtime.getRuntime().availableProcessors(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    public static ExecutorService newFixedThreadPool(int i, ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(i, i, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), threadFactory);
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

    public static ScheduledExecutorService newScheduledThreadPool(int i) {
        return new ScheduledThreadPoolExecutor(i);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int i, ThreadFactory threadFactory) {
        return new ScheduledThreadPoolExecutor(i, threadFactory);
    }

    public static ExecutorService unconfigurableExecutorService(ExecutorService executorService) {
        if (executorService == null) {
            throw new NullPointerException();
        }
        return new DelegatedExecutorService(executorService);
    }

    public static ScheduledExecutorService unconfigurableScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        if (scheduledExecutorService == null) {
            throw new NullPointerException();
        }
        return new DelegatedScheduledExecutorService(scheduledExecutorService);
    }

    public static ThreadFactory defaultThreadFactory() {
        return new DefaultThreadFactory();
    }

    public static ThreadFactory privilegedThreadFactory() {
        return new PrivilegedThreadFactory();
    }

    public static <T> Callable<T> callable(Runnable runnable, T t) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        return new RunnableAdapter(runnable, t);
    }

    public static Callable<Object> callable(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        return new RunnableAdapter(runnable, null);
    }

    public static Callable<Object> callable(final PrivilegedAction<?> privilegedAction) {
        if (privilegedAction == null) {
            throw new NullPointerException();
        }
        return new Callable<Object>() {
            @Override
            public Object call() {
                return privilegedAction.run();
            }
        };
    }

    public static Callable<Object> callable(final PrivilegedExceptionAction<?> privilegedExceptionAction) {
        if (privilegedExceptionAction == null) {
            throw new NullPointerException();
        }
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return privilegedExceptionAction.run();
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

    private static final class RunnableAdapter<T> implements Callable<T> {
        private final T result;
        private final Runnable task;

        RunnableAdapter(Runnable runnable, T t) {
            this.task = runnable;
            this.result = t;
        }

        @Override
        public T call() {
            this.task.run();
            return this.result;
        }
    }

    private static final class PrivilegedCallable<T> implements Callable<T> {
        final AccessControlContext acc = AccessController.getContext();
        final Callable<T> task;

        PrivilegedCallable(Callable<T> callable) {
            this.task = callable;
        }

        @Override
        public T call() throws Exception {
            try {
                return (T) AccessController.doPrivileged(new PrivilegedExceptionAction<T>() {
                    @Override
                    public T run() throws Exception {
                        return PrivilegedCallable.this.task.call();
                    }
                }, this.acc);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }

    private static final class PrivilegedCallableUsingCurrentClassLoader<T> implements Callable<T> {
        final AccessControlContext acc = AccessController.getContext();
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        final Callable<T> task;

        PrivilegedCallableUsingCurrentClassLoader(Callable<T> callable) {
            this.task = callable;
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
                            return PrivilegedCallableUsingCurrentClassLoader.this.task.call();
                        }
                        threadCurrentThread.setContextClassLoader(PrivilegedCallableUsingCurrentClassLoader.this.ccl);
                        try {
                            return PrivilegedCallableUsingCurrentClassLoader.this.task.call();
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

    private static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        DefaultThreadFactory() {
            SecurityManager securityManager = System.getSecurityManager();
            this.group = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = "pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(this.group, runnable, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != 5) {
                thread.setPriority(5);
            }
            return thread;
        }
    }

    private static class PrivilegedThreadFactory extends DefaultThreadFactory {
        final AccessControlContext acc = AccessController.getContext();
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();

        PrivilegedThreadFactory() {
        }

        @Override
        public Thread newThread(final Runnable runnable) {
            return super.newThread(new Runnable() {
                @Override
                public void run() {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            Thread.currentThread().setContextClassLoader(PrivilegedThreadFactory.this.ccl);
                            runnable.run();
                            return null;
                        }
                    }, PrivilegedThreadFactory.this.acc);
                }
            });
        }
    }

    private static class DelegatedExecutorService extends AbstractExecutorService {

        @ReachabilitySensitive
        private final ExecutorService e;

        DelegatedExecutorService(ExecutorService executorService) {
            this.e = executorService;
        }

        @Override
        public void execute(Runnable runnable) {
            this.e.execute(runnable);
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
        public boolean awaitTermination(long j, TimeUnit timeUnit) throws InterruptedException {
            return this.e.awaitTermination(j, timeUnit);
        }

        @Override
        public Future<?> submit(Runnable runnable) {
            return this.e.submit(runnable);
        }

        @Override
        public <T> Future<T> submit(Callable<T> callable) {
            return this.e.submit(callable);
        }

        @Override
        public <T> Future<T> submit(Runnable runnable, T t) {
            return this.e.submit(runnable, t);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) throws InterruptedException {
            return this.e.invokeAll(collection);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long j, TimeUnit timeUnit) throws InterruptedException {
            return this.e.invokeAll(collection, j, timeUnit);
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

    private static class FinalizableDelegatedExecutorService extends DelegatedExecutorService {
        FinalizableDelegatedExecutorService(ExecutorService executorService) {
            super(executorService);
        }

        protected void finalize() {
            super.shutdown();
        }
    }

    private static class DelegatedScheduledExecutorService extends DelegatedExecutorService implements ScheduledExecutorService {
        private final ScheduledExecutorService e;

        DelegatedScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
            super(scheduledExecutorService);
            this.e = scheduledExecutorService;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable runnable, long j, TimeUnit timeUnit) {
            return this.e.schedule(runnable, j, timeUnit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long j, TimeUnit timeUnit) {
            return this.e.schedule(callable, j, timeUnit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long j, long j2, TimeUnit timeUnit) {
            return this.e.scheduleAtFixedRate(runnable, j, j2, timeUnit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long j, long j2, TimeUnit timeUnit) {
            return this.e.scheduleWithFixedDelay(runnable, j, j2, timeUnit);
        }
    }

    private Executors() {
    }
}
