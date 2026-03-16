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

    public static void finalizeAllEnqueued() throws InterruptedException {
        Sentinel sentinel;
        do {
            sentinel = new Sentinel();
        } while (!enqueueSentinelReference(sentinel));
        sentinel.awaitFinalization();
    }

    private static boolean enqueueSentinelReference(Sentinel sentinel) {
        boolean z;
        synchronized (LIST_LOCK) {
            for (FinalizerReference<?> r = head; r != null; r = ((FinalizerReference) r).next) {
                if (r.referent == sentinel) {
                    FinalizerReference<?> finalizerReference = r;
                    finalizerReference.referent = null;
                    ((FinalizerReference) finalizerReference).zombie = sentinel;
                    if (!finalizerReference.makeCircularListIfUnenqueued()) {
                        z = false;
                    } else {
                        ReferenceQueue.add(finalizerReference);
                        z = true;
                    }
                    return z;
                }
            }
            throw new AssertionError("newly-created live Sentinel not on list!");
        }
    }

    private static class Sentinel {
        boolean finalized;

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

        synchronized void awaitFinalization() throws InterruptedException {
            while (!this.finalized) {
                wait();
            }
        }
    }
}
