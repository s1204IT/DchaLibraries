package sun.nio.ch;

import java.security.AccessController;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import sun.security.action.GetPropertyAction;

public class ThreadPool {
    private static final String DEFAULT_THREAD_POOL_INITIAL_SIZE = "java.nio.channels.DefaultThreadPool.initialSize";
    private static final String DEFAULT_THREAD_POOL_THREAD_FACTORY = "java.nio.channels.DefaultThreadPool.threadFactory";
    private final ExecutorService executor;
    private final boolean isFixed;
    private final int poolSize;

    private ThreadPool(ExecutorService executorService, boolean z, int i) {
        this.executor = executorService;
        this.isFixed = z;
        this.poolSize = i;
    }

    ExecutorService executor() {
        return this.executor;
    }

    boolean isFixedThreadPool() {
        return this.isFixed;
    }

    int poolSize() {
        return this.poolSize;
    }

    static ThreadFactory defaultThreadFactory() {
        return new ThreadFactory() {
            @Override
            public final Thread newThread(Runnable runnable) {
                return ThreadPool.lambda$defaultThreadFactory$0(runnable);
            }
        };
    }

    static Thread lambda$defaultThreadFactory$0(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    }

    private static class DefaultThreadPoolHolder {
        static final ThreadPool defaultThreadPool = ThreadPool.createDefault();

        private DefaultThreadPoolHolder() {
        }
    }

    static ThreadPool getDefault() {
        return DefaultThreadPoolHolder.defaultThreadPool;
    }

    static ThreadPool createDefault() {
        int defaultThreadPoolInitialSize = getDefaultThreadPoolInitialSize();
        if (defaultThreadPoolInitialSize < 0) {
            defaultThreadPoolInitialSize = Runtime.getRuntime().availableProcessors();
        }
        ThreadFactory defaultThreadPoolThreadFactory = getDefaultThreadPoolThreadFactory();
        if (defaultThreadPoolThreadFactory == null) {
            defaultThreadPoolThreadFactory = defaultThreadFactory();
        }
        return new ThreadPool(Executors.newCachedThreadPool(defaultThreadPoolThreadFactory), false, defaultThreadPoolInitialSize);
    }

    static ThreadPool create(int i, ThreadFactory threadFactory) {
        if (i <= 0) {
            throw new IllegalArgumentException("'nThreads' must be > 0");
        }
        return new ThreadPool(Executors.newFixedThreadPool(i, threadFactory), true, i);
    }

    public static ThreadPool wrap(ExecutorService executorService, int i) {
        if (executorService == null) {
            throw new NullPointerException("'executor' is null");
        }
        if (executorService instanceof ThreadPoolExecutor) {
            if (((ThreadPoolExecutor) executorService).getMaximumPoolSize() == Integer.MAX_VALUE) {
                if (i < 0) {
                    i = Runtime.getRuntime().availableProcessors();
                } else {
                    i = 0;
                }
            }
        } else if (i < 0) {
            i = 0;
        }
        return new ThreadPool(executorService, false, i);
    }

    private static int getDefaultThreadPoolInitialSize() {
        String str = (String) AccessController.doPrivileged(new GetPropertyAction(DEFAULT_THREAD_POOL_INITIAL_SIZE));
        if (str != null) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                throw new Error("Value of property 'java.nio.channels.DefaultThreadPool.initialSize' is invalid: " + ((Object) e));
            }
        }
        return -1;
    }

    private static ThreadFactory getDefaultThreadPoolThreadFactory() {
        String str = (String) AccessController.doPrivileged(new GetPropertyAction(DEFAULT_THREAD_POOL_THREAD_FACTORY));
        if (str != null) {
            try {
                return (ThreadFactory) Class.forName(str, true, ClassLoader.getSystemClassLoader()).newInstance();
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            } catch (IllegalAccessException e2) {
                throw new Error(e2);
            } catch (InstantiationException e3) {
                throw new Error(e3);
            }
        }
        return null;
    }
}
