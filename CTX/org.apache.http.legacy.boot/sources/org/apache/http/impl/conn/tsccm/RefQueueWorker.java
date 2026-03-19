package org.apache.http.impl.conn.tsccm;

import java.lang.ref.ReferenceQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Deprecated
public class RefQueueWorker implements Runnable {
    private final Log log = LogFactory.getLog(getClass());
    protected final RefQueueHandler refHandler;
    protected final ReferenceQueue<?> refQueue;
    protected volatile Thread workerThread;

    public RefQueueWorker(ReferenceQueue<?> referenceQueue, RefQueueHandler refQueueHandler) {
        if (referenceQueue == null) {
            throw new IllegalArgumentException("Queue must not be null.");
        }
        if (refQueueHandler == null) {
            throw new IllegalArgumentException("Handler must not be null.");
        }
        this.refQueue = referenceQueue;
        this.refHandler = refQueueHandler;
    }

    @Override
    public void run() {
        if (this.workerThread == null) {
            this.workerThread = Thread.currentThread();
        }
        while (this.workerThread == Thread.currentThread()) {
            try {
                this.refHandler.handleReference(this.refQueue.remove());
            } catch (InterruptedException e) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug(toString() + " interrupted", e);
                }
            }
        }
    }

    public void shutdown() {
        Thread thread = this.workerThread;
        if (thread != null) {
            this.workerThread = null;
            thread.interrupt();
        }
    }

    public String toString() {
        return "RefQueueWorker::" + this.workerThread;
    }
}
