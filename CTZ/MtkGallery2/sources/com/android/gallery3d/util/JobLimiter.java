package com.android.gallery3d.util;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.ThreadPool;
import java.util.LinkedList;

public class JobLimiter implements FutureListener {
    private final LinkedList<JobWrapper<?>> mJobs = new LinkedList<>();
    private int mLimit;
    private final ThreadPool mPool;

    private static class JobWrapper<T> implements Future<T>, ThreadPool.Job<T> {
        private Future<T> mDelegate;
        private ThreadPool.Job<T> mJob;
        private FutureListener<T> mListener;
        private T mResult;
        private int mState = 0;

        public JobWrapper(ThreadPool.Job<T> job, FutureListener<T> futureListener) {
            this.mJob = job;
            this.mListener = futureListener;
        }

        public synchronized void setFuture(Future<T> future) {
            if (this.mState != 0) {
                return;
            }
            this.mDelegate = future;
        }

        @Override
        public void cancel() {
            FutureListener<T> futureListener;
            synchronized (this) {
                if (this.mState != 1) {
                    futureListener = this.mListener;
                    this.mJob = null;
                    this.mListener = null;
                    if (this.mDelegate != null) {
                        this.mDelegate.cancel();
                        this.mDelegate = null;
                    }
                } else {
                    futureListener = null;
                }
                this.mState = 2;
                this.mResult = null;
                notifyAll();
            }
            if (futureListener != null) {
                futureListener.onFutureDone(this);
            }
        }

        @Override
        public synchronized boolean isCancelled() {
            return this.mState == 2;
        }

        @Override
        public boolean isDone() {
            return this.mState != 0;
        }

        @Override
        public synchronized T get() {
            while (this.mState == 0) {
                Utils.waitWithoutInterrupt(this);
            }
            return this.mResult;
        }

        @Override
        public void waitDone() {
            get();
        }

        @Override
        public T run(ThreadPool.JobContext jobContext) {
            T tRun;
            synchronized (this) {
                if (this.mState == 2) {
                    return null;
                }
                ThreadPool.Job<T> job = this.mJob;
                try {
                    tRun = job.run(jobContext);
                } catch (Throwable th) {
                    Log.w("Gallery2/JobLimiter", "error executing job: " + job, th);
                    tRun = null;
                }
                synchronized (this) {
                    if (this.mState == 2) {
                        return null;
                    }
                    this.mState = 1;
                    FutureListener<T> futureListener = this.mListener;
                    this.mListener = null;
                    this.mJob = null;
                    this.mResult = tRun;
                    notifyAll();
                    if (futureListener != null) {
                        futureListener.onFutureDone(this);
                    }
                    return tRun;
                }
            }
        }
    }

    public JobLimiter(ThreadPool threadPool, int i) {
        this.mPool = (ThreadPool) Utils.checkNotNull(threadPool);
        this.mLimit = i;
    }

    public synchronized <T> Future<T> submit(ThreadPool.Job<T> job, FutureListener<T> futureListener) {
        JobWrapper<?> jobWrapper;
        jobWrapper = new JobWrapper<>((ThreadPool.Job) Utils.checkNotNull(job), futureListener);
        this.mJobs.addLast(jobWrapper);
        submitTasksIfAllowed();
        return jobWrapper;
    }

    private void submitTasksIfAllowed() {
        while (this.mLimit > 0 && !this.mJobs.isEmpty()) {
            JobWrapper<?> jobWrapperRemoveFirst = this.mJobs.removeFirst();
            if (!jobWrapperRemoveFirst.isCancelled()) {
                this.mLimit--;
                jobWrapperRemoveFirst.setFuture(this.mPool.submit(jobWrapperRemoveFirst, this));
            }
        }
    }

    @Override
    public synchronized void onFutureDone(Future future) {
        this.mLimit++;
        submitTasksIfAllowed();
    }
}
