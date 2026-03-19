package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ListenerCallQueue<L> implements Runnable {
    private static final Logger logger = Logger.getLogger(ListenerCallQueue.class.getName());
    private final Executor executor;
    private boolean isThreadScheduled;
    private final L listener;
    private final Queue<Callback<L>> waitQueue = Queues.newArrayDeque();

    static abstract class Callback<L> {
        private final String methodCall;

        abstract void call(L l);

        Callback(String str) {
            this.methodCall = str;
        }

        void enqueueOn(Iterable<ListenerCallQueue<L>> iterable) {
            Iterator<ListenerCallQueue<L>> it = iterable.iterator();
            while (it.hasNext()) {
                it.next().add(this);
            }
        }
    }

    ListenerCallQueue(L l, Executor executor) {
        this.listener = (L) Preconditions.checkNotNull(l);
        this.executor = (Executor) Preconditions.checkNotNull(executor);
    }

    synchronized void add(Callback<L> callback) {
        this.waitQueue.add(callback);
    }

    void execute() {
        boolean z;
        synchronized (this) {
            z = true;
            if (!this.isThreadScheduled) {
                this.isThreadScheduled = true;
            } else {
                z = false;
            }
        }
        if (z) {
            try {
                this.executor.execute(this);
            } catch (RuntimeException e) {
                synchronized (this) {
                    this.isThreadScheduled = false;
                    logger.log(Level.SEVERE, "Exception while running callbacks for " + this.listener + " on " + this.executor, (Throwable) e);
                    throw e;
                }
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            r1 = true;
            synchronized (r8) {
                ;
                com.google.common.base.Preconditions.checkState(r8.isThreadScheduled);
                r2 = r8.waitQueue.poll();
                if (r2 == null) {
                    r8.isThreadScheduled = false;
                    return;
                }
            }
            while (true) {
                throw r2;
            }
        }
    }
}
