package java.lang.ref;

public class ReferenceQueue<T> {
    private static final int NANOS_PER_MILLI = 1000000;
    public static Reference<?> unenqueued = null;
    private Reference<? extends T> head;
    private Reference<? extends T> tail;

    public synchronized Reference<? extends T> poll() {
        Reference<? extends T> ret = null;
        synchronized (this) {
            if (this.head != null) {
                ret = this.head;
                if (this.head == this.tail) {
                    this.tail = null;
                    this.head = null;
                } else {
                    this.head = this.head.queueNext;
                }
                ret.queueNext = null;
            }
        }
        return ret;
    }

    public Reference<? extends T> remove() throws InterruptedException {
        return remove(0L);
    }

    public synchronized Reference<? extends T> remove(long timeoutMillis) throws InterruptedException {
        Reference<? extends T> referencePoll;
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeout < 0: " + timeoutMillis);
        }
        if (this.head != null) {
            referencePoll = poll();
        } else if (timeoutMillis == 0 || timeoutMillis > 9223372036854L) {
            do {
                wait(0L);
            } while (this.head == null);
            referencePoll = poll();
        } else {
            long nanosToWait = timeoutMillis * 1000000;
            int timeoutNanos = 0;
            long startTime = System.nanoTime();
            while (true) {
                wait(timeoutMillis, timeoutNanos);
                if (this.head == null) {
                    long nanosElapsed = System.nanoTime() - startTime;
                    long nanosRemaining = nanosToWait - nanosElapsed;
                    if (nanosRemaining <= 0) {
                        break;
                    }
                    timeoutMillis = nanosRemaining / 1000000;
                    timeoutNanos = (int) (nanosRemaining - (1000000 * timeoutMillis));
                } else {
                    break;
                }
            }
            referencePoll = poll();
        }
        return referencePoll;
    }

    synchronized void enqueue(Reference<? extends T> reference) {
        if (this.tail == null) {
            this.head = reference;
        } else {
            this.tail.queueNext = reference;
        }
        this.tail = reference;
        this.tail.queueNext = reference;
        notify();
    }

    static void add(Reference<?> list) {
        synchronized (ReferenceQueue.class) {
            if (unenqueued == null) {
                unenqueued = list;
            } else {
                Reference<?> last = unenqueued;
                while (last.pendingNext != unenqueued) {
                    last = last.pendingNext;
                }
                last.pendingNext = list;
                Reference<?> last2 = list;
                while (last2.pendingNext != list) {
                    last2 = last2.pendingNext;
                }
                last2.pendingNext = unenqueued;
            }
            ReferenceQueue.class.notifyAll();
        }
    }
}
