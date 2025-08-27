package com.android.quicksearchbox.util;

import android.util.Log;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

/* loaded from: classes.dex */
public class SingleThreadNamedTaskExecutor implements NamedTaskExecutor {
    private volatile boolean mClosed = false;
    private final LinkedBlockingQueue<NamedTask> mQueue = new LinkedBlockingQueue<>();
    private final Thread mWorker;

    public SingleThreadNamedTaskExecutor(ThreadFactory threadFactory) {
        this.mWorker = threadFactory.newThread(new Worker());
        this.mWorker.start();
    }

    @Override // com.android.quicksearchbox.util.NamedTaskExecutor
    public void execute(NamedTask namedTask) {
        if (this.mClosed) {
            throw new IllegalStateException("execute() after close()");
        }
        this.mQueue.add(namedTask);
    }

    private class Worker implements Runnable {
        private Worker() {
        }

        @Override // java.lang.Runnable
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
            Thread threadCurrentThread = Thread.currentThread();
            String name = threadCurrentThread.getName();
            while (!SingleThreadNamedTaskExecutor.this.mClosed) {
                try {
                    NamedTask namedTask = (NamedTask) SingleThreadNamedTaskExecutor.this.mQueue.take();
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
    }

    public static Factory<NamedTaskExecutor> factory(final ThreadFactory threadFactory) {
        return new Factory<NamedTaskExecutor>() { // from class: com.android.quicksearchbox.util.SingleThreadNamedTaskExecutor.1
            /* JADX DEBUG: Method merged with bridge method: create()Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.quicksearchbox.util.Factory
            public NamedTaskExecutor create() {
                return new SingleThreadNamedTaskExecutor(threadFactory);
            }
        };
    }
}
