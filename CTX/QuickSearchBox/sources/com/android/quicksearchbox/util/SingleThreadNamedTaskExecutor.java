package com.android.quicksearchbox.util;

import android.util.Log;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

public class SingleThreadNamedTaskExecutor implements NamedTaskExecutor {
    private volatile boolean mClosed = false;
    private final LinkedBlockingQueue<NamedTask> mQueue = new LinkedBlockingQueue<>();
    private final Thread mWorker;

    private class Worker implements Runnable {
        final SingleThreadNamedTaskExecutor this$0;

        private Worker(SingleThreadNamedTaskExecutor singleThreadNamedTaskExecutor) {
            this.this$0 = singleThreadNamedTaskExecutor;
        }

        private void loop() {
            Thread threadCurrentThread = Thread.currentThread();
            String name = threadCurrentThread.getName();
            while (!this.this$0.mClosed) {
                try {
                    NamedTask namedTask = (NamedTask) this.this$0.mQueue.take();
                    threadCurrentThread.setName(name + " " + namedTask.getName());
                    try {
                        namedTask.run();
                    } catch (RuntimeException e) {
                        Log.e("QSB.SingleThreadNamedTaskExecutor", "Task " + namedTask.getName() + " failed", e);
                    }
                } catch (InterruptedException e2) {
                }
            }
        }

        @Override
        public void run() {
            try {
                loop();
            } finally {
                if (!this.this$0.mClosed) {
                    Log.w("QSB.SingleThreadNamedTaskExecutor", "Worker exited before close");
                }
            }
        }
    }

    public SingleThreadNamedTaskExecutor(ThreadFactory threadFactory) {
        this.mWorker = threadFactory.newThread(new Worker());
        this.mWorker.start();
    }

    public static Factory<NamedTaskExecutor> factory(ThreadFactory threadFactory) {
        return new Factory<NamedTaskExecutor>(threadFactory) {
            final ThreadFactory val$threadFactory;

            {
                this.val$threadFactory = threadFactory;
            }

            @Override
            public NamedTaskExecutor create() {
                return new SingleThreadNamedTaskExecutor(this.val$threadFactory);
            }
        };
    }

    @Override
    public void execute(NamedTask namedTask) {
        if (this.mClosed) {
            throw new IllegalStateException("execute() after close()");
        }
        this.mQueue.add(namedTask);
    }
}
