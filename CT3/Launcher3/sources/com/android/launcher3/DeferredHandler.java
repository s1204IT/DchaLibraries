package com.android.launcher3;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import java.util.LinkedList;

public class DeferredHandler {
    LinkedList<Runnable> mQueue = new LinkedList<>();
    private MessageQueue mMessageQueue = Looper.myQueue();
    private Impl mHandler = new Impl();

    class Impl extends Handler implements MessageQueue.IdleHandler {
        Impl() {
        }

        @Override
        public void handleMessage(Message msg) {
            synchronized (DeferredHandler.this.mQueue) {
                if (DeferredHandler.this.mQueue.size() == 0) {
                    return;
                }
                Runnable r = DeferredHandler.this.mQueue.removeFirst();
                r.run();
                synchronized (DeferredHandler.this.mQueue) {
                    DeferredHandler.this.scheduleNextLocked();
                }
            }
        }

        @Override
        public boolean queueIdle() {
            handleMessage(null);
            return false;
        }
    }

    private class IdleRunnable implements Runnable {
        Runnable mRunnable;

        IdleRunnable(Runnable r) {
            this.mRunnable = r;
        }

        @Override
        public void run() {
            this.mRunnable.run();
        }
    }

    public void post(Runnable runnable) {
        synchronized (this.mQueue) {
            this.mQueue.add(runnable);
            if (this.mQueue.size() == 1) {
                scheduleNextLocked();
            }
        }
    }

    public void postIdle(Runnable runnable) {
        post(new IdleRunnable(runnable));
    }

    public void cancelAll() {
        synchronized (this.mQueue) {
            this.mQueue.clear();
        }
    }

    public void flush() {
        LinkedList<Runnable> queue = new LinkedList<>();
        synchronized (this.mQueue) {
            queue.addAll(this.mQueue);
            this.mQueue.clear();
        }
        for (Runnable r : queue) {
            r.run();
        }
    }

    void scheduleNextLocked() {
        if (this.mQueue.size() <= 0) {
            return;
        }
        Runnable peek = this.mQueue.getFirst();
        if (peek instanceof IdleRunnable) {
            this.mMessageQueue.addIdleHandler(this.mHandler);
        } else {
            this.mHandler.sendEmptyMessage(1);
        }
    }
}
