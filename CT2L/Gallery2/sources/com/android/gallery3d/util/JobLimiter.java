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

        public JobWrapper(ThreadPool.Job<T> job, FutureListener<T> listener) {
            this.mJob = job;
            this.mListener = listener;
        }

        public synchronized void setFuture(Future<T> future) {
            if (this.mState == 0) {
                this.mDelegate = future;
            }
        }

        @Override
        public void cancel() {
            FutureListener<T> listener = null;
            synchronized (this) {
                if (this.mState != 1) {
                    listener = this.mListener;
                    this.mJob = null;
                    this.mListener = null;
                    if (this.mDelegate != null) {
                        this.mDelegate.cancel();
                        this.mDelegate = null;
                    }
                }
                this.mState = 2;
                this.mResult = null;
                notifyAll();
            }
            if (listener != null) {
                listener.onFutureDone(this);
            }
        }

        @Override
        public synchronized boolean isCancelled() {
            return this.mState == 2;
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
        public T run(ThreadPool.JobContext jc) {
            synchronized (this) {
                if (this.mState == 2) {
                    return null;
                }
                ThreadPool.Job<T> job = this.mJob;
                T result = null;
                try {
                    result = job.run(jc);
                } catch (Throwable t) {
                    Log.w("JobLimiter", "error executing job: " + job, t);
                }
                synchronized (this) {
                    if (this.mState == 2) {
                        result = null;
                    } else {
                        this.mState = 1;
                        FutureListener<T> listener = this.mListener;
                        this.mListener = null;
                        this.mJob = null;
                        this.mResult = result;
                        notifyAll();
                        if (listener != null) {
                            listener.onFutureDone(this);
                        }
                    }
                }
                return result;
            }
        }
    }

    public JobLimiter(ThreadPool pool, int limit) {
        this.mPool = (ThreadPool) Utils.checkNotNull(pool);
        this.mLimit = limit;
    }

    public synchronized <T> Future<T> submit(ThreadPool.Job<T> job, FutureListener<T> listener) {
        JobWrapper<?> jobWrapper;
        jobWrapper = new JobWrapper<>((ThreadPool.Job) Utils.checkNotNull(job), listener);
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
