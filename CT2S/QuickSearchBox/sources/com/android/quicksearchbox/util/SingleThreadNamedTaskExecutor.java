package com.android.quicksearchbox.util;

import android.util.Log;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

public class SingleThreadNamedTaskExecutor implements NamedTaskExecutor {
    private volatile boolean mClosed = false;
    private final LinkedBlockingQueue<NamedTask> mQueue = new LinkedBlockingQueue<>();
    private final Thread mWorker;

    public SingleThreadNamedTaskExecutor(ThreadFactory threadFactory) {
        this.mWorker = threadFactory.newThread(new Worker());
        this.mWorker.start();
    }

    @Override
    public void execute(NamedTask task) {
        if (this.mClosed) {
            throw new IllegalStateException("execute() after close()");
        }
        this.mQueue.add(task);
    }

    private class Worker implements Runnable {
        private Worker() {
        }

        @Override
        public void run() {
            try {
                loop();
            } finally {
                if (!SingleThreadNamedTaskExecutor.this.mClosed) {
                    Log.w("QSB.SingleThreadNamedTaskExecutor", "Worker exited before close");
                }
            }
        }

        private void loop() {
            Thread currentThread = Thread.currentThread();
            String threadName = currentThread.getName();
            while (!SingleThreadNamedTaskExecutor.this.mClosed) {
                try {
                    NamedTask task = (NamedTask) SingleThreadNamedTaskExecutor.this.mQueue.take();
                    currentThread.setName(threadName + " " + task.getName());
                    try {
                        task.run();
                    } catch (RuntimeException ex) {
                        Log.e("QSB.SingleThreadNamedTaskExecutor", "Task " + task.getName() + " failed", ex);
                    }
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public static Factory<NamedTaskExecutor> factory(final ThreadFactory threadFactory) {
        return new Factory<NamedTaskExecutor>() {
            @Override
            public NamedTaskExecutor create() {
                return new SingleThreadNamedTaskExecutor(threadFactory);
            }
        };
    }
}
