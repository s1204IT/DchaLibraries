package com.android.contacts.util.concurrent;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import com.google.common.util.concurrent.ForwardingFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ContactsExecutors {
    private static final ListeningExecutorService DEFAULT_THREAD_POOL_EXECUTOR;
    private static ListeningExecutorService sSimExecutor;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;

    static {
        ListeningExecutorService listeningExecutorServiceListeningDecorator;
        if (AsyncTask.THREAD_POOL_EXECUTOR instanceof ExecutorService) {
            listeningExecutorServiceListeningDecorator = MoreExecutors.listeningDecorator((ExecutorService) AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            listeningExecutorServiceListeningDecorator = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(CORE_POOL_SIZE));
        }
        DEFAULT_THREAD_POOL_EXECUTOR = listeningExecutorServiceListeningDecorator;
    }

    public static ListeningExecutorService getDefaultThreadPoolExecutor() {
        return DEFAULT_THREAD_POOL_EXECUTOR;
    }

    public static ScheduledExecutorService newUiThreadExecutor() {
        return newHandlerExecutor(new Handler(Looper.getMainLooper()));
    }

    public static ScheduledExecutorService newHandlerExecutor(Handler handler) {
        return new HandlerExecutorService(handler);
    }

    public static synchronized ListeningExecutorService getSimReadExecutor() {
        if (sSimExecutor == null) {
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue());
            threadPoolExecutor.allowCoreThreadTimeOut(true);
            sSimExecutor = MoreExecutors.listeningDecorator(threadPoolExecutor);
        }
        return sSimExecutor;
    }

    private static class HandlerExecutorService extends AbstractExecutorService implements ScheduledExecutorService {
        private final Handler mHandler;

        private HandlerExecutorService(Handler handler) {
            this.mHandler = handler;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable runnable, long j, TimeUnit timeUnit) {
            HandlerFuture<Void> handlerFutureFromRunnable = HandlerFuture.fromRunnable(this.mHandler, j, timeUnit, runnable);
            this.mHandler.postDelayed(handlerFutureFromRunnable, timeUnit.toMillis(j));
            return handlerFutureFromRunnable;
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long j, TimeUnit timeUnit) {
            HandlerFuture handlerFuture = new HandlerFuture(this.mHandler, j, timeUnit, callable);
            this.mHandler.postDelayed(handlerFuture, timeUnit.toMillis(j));
            return handlerFuture;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long j, long j2, TimeUnit timeUnit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long j, long j2, TimeUnit timeUnit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long j, TimeUnit timeUnit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void execute(Runnable runnable) {
            this.mHandler.post(runnable);
        }
    }

    private static class HandlerFuture<T> extends ForwardingFuture<T> implements RunnableScheduledFuture<T> {
        private final long mDelayMillis;
        private final SettableFuture<T> mDelegate;
        private final Handler mHandler;
        private final AtomicLong mStart;
        private final Callable<T> mTask;

        private HandlerFuture(Handler handler, long j, TimeUnit timeUnit, Callable<T> callable) {
            this.mDelegate = SettableFuture.create();
            this.mStart = new AtomicLong(-1L);
            this.mHandler = handler;
            this.mDelayMillis = timeUnit.toMillis(j);
            this.mTask = callable;
        }

        @Override
        public boolean isPeriodic() {
            return false;
        }

        @Override
        public long getDelay(TimeUnit timeUnit) {
            long j = this.mStart.get();
            if (j < 0) {
                return this.mDelayMillis;
            }
            return TimeUnit.MILLISECONDS.convert(this.mDelayMillis - (System.currentTimeMillis() - j), timeUnit);
        }

        @Override
        public int compareTo(Delayed delayed) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), delayed.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        protected Future<T> delegate() {
            return this.mDelegate;
        }

        @Override
        public boolean cancel(boolean z) {
            this.mHandler.removeCallbacks(this);
            return super.cancel(z);
        }

        @Override
        public void run() {
            if (!this.mStart.compareAndSet(-1L, System.currentTimeMillis())) {
                return;
            }
            try {
                this.mDelegate.set(this.mTask.call());
            } catch (Exception e) {
                this.mDelegate.setException(e);
            }
        }

        public static HandlerFuture<Void> fromRunnable(Handler handler, long j, TimeUnit timeUnit, final Runnable runnable) {
            return new HandlerFuture<>(handler, j, timeUnit, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    runnable.run();
                    return null;
                }
            });
        }
    }
}
