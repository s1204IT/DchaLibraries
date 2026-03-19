package com.android.gallery3d.util;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
    public static final int PARALLEL_THREAD_NUM;
    ResourceCounter mCpuCounter;
    private final Executor mExecutor;
    ResourceCounter mNetworkCounter;
    public static final JobContext JOB_CONTEXT_STUB = new JobContextStub();
    public static final int CPU_CORES_NUM = Runtime.getRuntime().availableProcessors();

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

    static {
        PARALLEL_THREAD_NUM = 2 <= CPU_CORES_NUM / 2 ? CPU_CORES_NUM / 2 : 2;
    }

    private static class JobContextStub implements JobContext {
        private JobContextStub() {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void setCancelListener(CancelListener cancelListener) {
        }

        @Override
        public boolean setMode(int i) {
            return true;
        }
    }

    private static class ResourceCounter {
        public int value;

        public ResourceCounter(int i) {
            Log.d("Gallery2/ThreadPool", "<ResourceCounter.ResourceCounter> ResourceCounter max thread num " + i + " CPU Core " + ThreadPool.CPU_CORES_NUM);
            this.value = i;
        }
    }

    public ThreadPool() {
        this(4, 8);
    }

    public ThreadPool(int i, int i2) {
        this.mCpuCounter = new ResourceCounter(PARALLEL_THREAD_NUM);
        this.mNetworkCounter = new ResourceCounter(PARALLEL_THREAD_NUM);
        this.mExecutor = new ThreadPoolExecutor(i, i2, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue(), new PriorityThreadFactory("thread-pool", 0));
    }

    public <T> Future<T> submit(Job<T> job, FutureListener<T> futureListener) {
        Worker worker = new Worker(job, futureListener);
        this.mExecutor.execute(worker);
        return worker;
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

        public Worker(Job<T> job, FutureListener<T> futureListener) {
            this.mJob = job;
            this.mListener = futureListener;
        }

        @Override
        public void run() {
            T tRun;
            if (setMode(1)) {
                try {
                    tRun = this.mJob.run(this);
                } catch (Throwable th) {
                    Log.w("Gallery2/Worker", "Exception in running a job", th);
                    tRun = null;
                }
            } else {
                tRun = null;
            }
            synchronized (this) {
                setMode(0);
                this.mResult = tRun;
                this.mIsDone = true;
                notifyAll();
            }
            if (this.mListener != null) {
                this.mListener.onFutureDone(this);
            }
        }

        @Override
        public synchronized void cancel() {
            if (this.mIsCancelled) {
                return;
            }
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

        @Override
        public boolean isCancelled() {
            return this.mIsCancelled;
        }

        @Override
        public synchronized boolean isDone() {
            return this.mIsDone;
        }

        @Override
        public synchronized T get() {
            while (!this.mIsDone) {
                try {
                    wait();
                } catch (Exception e) {
                    Log.w("Gallery2/Worker", "ingore exception", e);
                }
            }
            return this.mResult;
        }

        @Override
        public void waitDone() {
            get();
        }

        @Override
        public synchronized void setCancelListener(CancelListener cancelListener) {
            this.mCancelListener = cancelListener;
            if (this.mIsCancelled && this.mCancelListener != null) {
                this.mCancelListener.onCancel();
            }
        }

        @Override
        public boolean setMode(int i) {
            ResourceCounter resourceCounterModeToCounter = modeToCounter(this.mMode);
            if (resourceCounterModeToCounter != null) {
                releaseResource(resourceCounterModeToCounter);
            }
            this.mMode = 0;
            ResourceCounter resourceCounterModeToCounter2 = modeToCounter(i);
            if (resourceCounterModeToCounter2 != null) {
                if (!acquireResource(resourceCounterModeToCounter2)) {
                    return false;
                }
                this.mMode = i;
                return true;
            }
            return true;
        }

        private ResourceCounter modeToCounter(int i) {
            if (i == 1) {
                return ThreadPool.this.mCpuCounter;
            }
            if (i == 2) {
                return ThreadPool.this.mNetworkCounter;
            }
            return null;
        }

        private boolean acquireResource(ResourceCounter resourceCounter) {
            while (true) {
                synchronized (this) {
                    if (this.mIsCancelled) {
                        this.mWaitOnResource = null;
                        return false;
                    }
                    this.mWaitOnResource = resourceCounter;
                    synchronized (resourceCounter) {
                        if (resourceCounter.value > 0) {
                            resourceCounter.value--;
                            synchronized (this) {
                                this.mWaitOnResource = null;
                            }
                            return true;
                        }
                        try {
                            resourceCounter.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }

        private void releaseResource(ResourceCounter resourceCounter) {
            synchronized (resourceCounter) {
                resourceCounter.value++;
                resourceCounter.notifyAll();
            }
        }
    }
}
