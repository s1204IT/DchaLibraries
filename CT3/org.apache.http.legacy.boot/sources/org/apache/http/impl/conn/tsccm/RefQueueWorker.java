package org.apache.http.impl.conn.tsccm;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Deprecated
public class RefQueueWorker implements Runnable {
    protected final RefQueueHandler refHandler;
    protected final ReferenceQueue<?> refQueue;
    protected volatile Thread workerThread;
    private final Log log = LogFactory.getLog(getClass());
    private boolean mRunFlag = false;
    private Object mMutex = "RefQueueWorker";

    public RefQueueWorker(ReferenceQueue<?> queue, RefQueueHandler handler) {
        if (queue == null) {
            throw new IllegalArgumentException("Queue must not be null.");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler must not be null.");
        }
        this.refQueue = queue;
        this.refHandler = handler;
    }

    @Override
    public void run() {
        if (this.workerThread == null) {
            this.workerThread = Thread.currentThread();
        }
        synchronized (this.mMutex) {
            this.mRunFlag = true;
            this.mMutex.notify();
        }
        while (this.workerThread == Thread.currentThread()) {
            try {
                Reference<?> ref = this.refQueue.remove();
                this.refHandler.handleReference(ref);
            } catch (InterruptedException e) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug(toString() + " interrupted", e);
                }
            }
        }
    }

    public void shutdown() {
        Thread wt = this.workerThread;
        if (wt == null) {
            return;
        }
        this.workerThread = null;
        wt.interrupt();
    }

    public String toString() {
        return "RefQueueWorker::" + this.workerThread;
    }

    public void waitWorkerStart() {
        synchronized (this.mMutex) {
            while (!this.mRunFlag) {
                try {
                    this.mMutex.wait(3000L);
                } catch (InterruptedException e) {
                    System.out.println("err:" + e);
                }
            }
        }
    }
}
