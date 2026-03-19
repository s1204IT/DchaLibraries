package java.lang.ref;

public final class FinalizerReference<T> extends Reference<T> {
    private FinalizerReference<?> next;
    private FinalizerReference<?> prev;
    private T zombie;
    public static final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private static final Object LIST_LOCK = new Object();
    private static FinalizerReference<?> head = null;

    private native boolean makeCircularListIfUnenqueued();

    public FinalizerReference(T r, ReferenceQueue<? super T> q) {
        super(r, q);
    }

    @Override
    public T get() {
        return this.zombie;
    }

    @Override
    public void clear() {
        this.zombie = null;
    }

    public static void add(Object referent) {
        FinalizerReference<?> reference = new FinalizerReference<>(referent, queue);
        synchronized (LIST_LOCK) {
            ((FinalizerReference) reference).prev = null;
            ((FinalizerReference) reference).next = head;
            if (head != null) {
                ((FinalizerReference) head).prev = reference;
            }
            head = reference;
        }
    }

    public static void remove(FinalizerReference<?> reference) {
        synchronized (LIST_LOCK) {
            FinalizerReference<?> next = ((FinalizerReference) reference).next;
            FinalizerReference<?> prev = ((FinalizerReference) reference).prev;
            ((FinalizerReference) reference).next = null;
            ((FinalizerReference) reference).prev = null;
            if (prev != null) {
                ((FinalizerReference) prev).next = next;
            } else {
                head = next;
            }
            if (next != null) {
                ((FinalizerReference) next).prev = prev;
            }
        }
    }

    public static void finalizeAllEnqueued(long timeout) throws InterruptedException {
        Sentinel sentinel;
        do {
            sentinel = new Sentinel(null);
        } while (!enqueueSentinelReference(sentinel));
        sentinel.awaitFinalization(timeout);
    }

    private static boolean enqueueSentinelReference(Sentinel sentinel) {
        synchronized (LIST_LOCK) {
            for (FinalizerReference<?> r = head; r != null; r = ((FinalizerReference) r).next) {
                if (r.referent == sentinel) {
                    FinalizerReference<?> finalizerReference = r;
                    finalizerReference.referent = null;
                    ((FinalizerReference) finalizerReference).zombie = sentinel;
                    if (!finalizerReference.makeCircularListIfUnenqueued()) {
                        return false;
                    }
                    ReferenceQueue.add(finalizerReference);
                    return true;
                }
            }
            throw new AssertionError("newly-created live Sentinel not on list!");
        }
    }

    private static class Sentinel {
        boolean finalized;

        Sentinel(Sentinel sentinel) {
            this();
        }

        private Sentinel() {
            this.finalized = false;
        }

        protected synchronized void finalize() throws Throwable {
            if (this.finalized) {
                throw new AssertionError();
            }
            this.finalized = true;
            notifyAll();
        }

        synchronized void awaitFinalization(long timeout) throws InterruptedException {
            long startTime = System.nanoTime();
            long endTime = startTime + timeout;
            while (!this.finalized) {
                if (timeout != 0) {
                    long currentTime = System.nanoTime();
                    if (currentTime >= endTime) {
                        break;
                    }
                    long deltaTime = endTime - currentTime;
                    wait(deltaTime / 1000000, (int) (deltaTime % 1000000));
                } else {
                    wait();
                }
            }
        }
    }
}
