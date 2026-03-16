package com.android.gallery3d.util;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
    public static final JobContext JOB_CONTEXT_STUB = new JobContextStub();
    ResourceCounter mCpuCounter;
    private final Executor mExecutor;
    ResourceCounter mNetworkCounter;

    public interface CancelListener {
        void onCancel();
    }

    public interface Job<T> {
        T run(JobContext jobContext);
    }

    public interface JobContext {
        boolean isCancelled();

        void setCancelListener(CancelListener cancelListener);

        boolean setMode(int i);
    }

    private static class JobContextStub implements JobContext {
        private JobContextStub() {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void setCancelListener(CancelListener listener) {
        }

        @Override
        public boolean setMode(int mode) {
            return true;
        }
    }

    private static class ResourceCounter {
        public int value;

        public ResourceCounter(int v) {
            this.value = v;
        }
    }

    public ThreadPool() {
        this(4, 8);
    }

    public ThreadPool(int initPoolSize, int maxPoolSize) {
        this.mCpuCounter = new ResourceCounter(2);
        this.mNetworkCounter = new ResourceCounter(2);
        this.mExecutor = new ThreadPoolExecutor(initPoolSize, maxPoolSize, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue(), new PriorityThreadFactory("thread-pool", 10));
    }

    public <T> Future<T> submit(Job<T> job, FutureListener<T> listener) {
        Worker<T> w = new Worker<>(job, listener);
        this.mExecutor.execute(w);
        return w;
    }

    public <T> Future<T> submit(Job<T> job) {
        return submit(job, null);
    }

    private class Worker<T> implements Future<T>, JobContext, Runnable {
        private CancelListener mCancelListener;
        private volatile boolean mIsCancelled;
        private boolean mIsDone;
        private Job<T> mJob;
        private FutureListener<T> mListener;
        private int mMode;
        private T mResult;
        private ResourceCounter mWaitOnResource;

        public Worker(Job<T> job, FutureListener<T> listener) {
            this.mJob = job;
            this.mListener = listener;
        }

        @Override
        public void run() {
            T result = null;
            if (setMode(1)) {
                try {
                    result = this.mJob.run(this);
                } catch (Throwable ex) {
                    android.util.Log.w("Worker", "Exception in running a job", ex);
                }
            }
            synchronized (this) {
                setMode(0);
                this.mResult = result;
                this.mIsDone = true;
                notifyAll();
            }
            if (this.mListener != null) {
                this.mListener.onFutureDone(this);
            }
        }

        @Override
        public synchronized void cancel() {
            if (!this.mIsCancelled) {
                this.mIsCancelled = true;
                if (this.mWaitOnResource != null) {
                    synchronized (this.mWaitOnResource) {
                        this.mWaitOnResource.notifyAll();
                    }
                }
                if (this.mCancelListener != null) {
                    this.mCancelListener.onCancel();
                }
            }
        }

        @Override
        public boolean isCancelled() {
            return this.mIsCancelled;
        }

        @Override
        public synchronized T get() {
            while (!this.mIsDone) {
                try {
                    wait();
                } catch (Exception ex) {
                    android.util.Log.w("Worker", "ingore exception", ex);
                }
            }
            return this.mResult;
        }

        @Override
        public void waitDone() {
            get();
        }

        @Override
        public synchronized void setCancelListener(CancelListener listener) {
            this.mCancelListener = listener;
            if (this.mIsCancelled && this.mCancelListener != null) {
                this.mCancelListener.onCancel();
            }
        }

        @Override
        public boolean setMode(int mode) {
            ResourceCounter rc = modeToCounter(this.mMode);
            if (rc != null) {
                releaseResource(rc);
            }
            this.mMode = 0;
            ResourceCounter rc2 = modeToCounter(mode);
            if (rc2 != null) {
                if (!acquireResource(rc2)) {
                    return false;
                }
                this.mMode = mode;
            }
            return true;
        }

        private ResourceCounter modeToCounter(int mode) {
            if (mode == 1) {
                return ThreadPool.this.mCpuCounter;
            }
            if (mode == 2) {
                return ThreadPool.this.mNetworkCounter;
            }
            return null;
        }

        private boolean acquireResource(ResourceCounter counter) {
            while (true) {
                synchronized (this) {
                    if (this.mIsCancelled) {
                        this.mWaitOnResource = null;
                        return false;
                    }
                    this.mWaitOnResource = counter;
                    synchronized (counter) {
                        if (counter.value > 0) {
                            counter.value--;
                            synchronized (this) {
                                this.mWaitOnResource = null;
                            }
                            return true;
                        }
                        try {
                            counter.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }

        private void releaseResource(ResourceCounter counter) {
            synchronized (counter) {
                counter.value++;
                counter.notifyAll();
            }
        }
    }
}
