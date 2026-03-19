package java.lang.ref;

public class ReferenceQueue<T> {
    private static final Reference sQueueNextUnenqueued = new PhantomReference(null, null);
    public static Reference<?> unenqueued = null;
    private Reference<? extends T> head = null;
    private Reference<? extends T> tail = null;
    private final Object lock = new Object();

    private boolean enqueueLocked(Reference<? extends T> reference) {
        if (reference.queueNext != null) {
            return false;
        }
        if (reference instanceof sun.misc.Cleaner) {
            ((sun.misc.Cleaner) reference).clean();
            reference.queueNext = sQueueNextUnenqueued;
            return true;
        }
        if (this.tail == null) {
            this.head = reference;
        } else {
            this.tail.queueNext = reference;
        }
        this.tail = reference;
        this.tail.queueNext = reference;
        return true;
    }

    boolean isEnqueued(Reference<? extends T> reference) {
        boolean z;
        synchronized (this.lock) {
            z = (reference.queueNext == null || reference.queueNext == sQueueNextUnenqueued) ? false : true;
        }
        return z;
    }

    boolean enqueue(Reference<? extends T> reference) {
        synchronized (this.lock) {
            if (enqueueLocked(reference)) {
                this.lock.notifyAll();
                return true;
            }
            return false;
        }
    }

    private Reference<? extends T> reallyPollLocked() {
        if (this.head == null) {
            return null;
        }
        Reference<? extends T> reference = this.head;
        if (this.head == this.tail) {
            this.tail = null;
            this.head = null;
        } else {
            this.head = this.head.queueNext;
        }
        reference.queueNext = sQueueNextUnenqueued;
        return reference;
    }

    public Reference<? extends T> poll() {
        synchronized (this.lock) {
            if (this.head == null) {
                return null;
            }
            return reallyPollLocked();
        }
    }

    public Reference<? extends T> remove(long j) throws InterruptedException, IllegalArgumentException {
        long jNanoTime;
        if (j < 0) {
            throw new IllegalArgumentException("Negative timeout value");
        }
        synchronized (this.lock) {
            Reference<? extends T> referenceReallyPollLocked = reallyPollLocked();
            if (referenceReallyPollLocked != null) {
                return referenceReallyPollLocked;
            }
            if (j != 0) {
                jNanoTime = System.nanoTime();
            } else {
                jNanoTime = 0;
            }
            while (true) {
                this.lock.wait(j);
                Reference<? extends T> referenceReallyPollLocked2 = reallyPollLocked();
                if (referenceReallyPollLocked2 != null) {
                    return referenceReallyPollLocked2;
                }
                if (j != 0) {
                    long jNanoTime2 = System.nanoTime();
                    j -= (jNanoTime2 - jNanoTime) / 1000000;
                    if (j <= 0) {
                        return null;
                    }
                    jNanoTime = jNanoTime2;
                }
            }
        }
    }

    public Reference<? extends T> remove() throws InterruptedException {
        return remove(0L);
    }

    public static void enqueuePending(Reference<?> reference) {
        Reference<?> reference2;
        Reference<?> reference3 = reference;
        do {
            ReferenceQueue<? super T> referenceQueue = reference3.queue;
            if (referenceQueue == 0) {
                Reference<?> reference4 = reference3.pendingNext;
                reference3.pendingNext = reference3;
                reference3 = reference4;
            } else {
                synchronized (((ReferenceQueue) referenceQueue).lock) {
                    while (true) {
                        reference2 = reference3.pendingNext;
                        reference3.pendingNext = reference3;
                        referenceQueue.enqueueLocked((Reference<? extends Object>) reference3);
                        if (reference2 == reference || reference2.queue != referenceQueue) {
                            break;
                        } else {
                            reference3 = reference2;
                        }
                    }
                    ((ReferenceQueue) referenceQueue).lock.notifyAll();
                }
                reference3 = reference2;
            }
        } while (reference3 != reference);
    }

    static void add(Reference<?> reference) {
        synchronized (ReferenceQueue.class) {
            if (unenqueued == null) {
                unenqueued = reference;
            } else {
                Reference<?> reference2 = unenqueued;
                while (reference2.pendingNext != unenqueued) {
                    reference2 = reference2.pendingNext;
                }
                reference2.pendingNext = reference;
                Reference<?> reference3 = reference;
                while (reference3.pendingNext != reference) {
                    reference3 = reference3.pendingNext;
                }
                reference3.pendingNext = unenqueued;
            }
            ReferenceQueue.class.notifyAll();
        }
    }
}
