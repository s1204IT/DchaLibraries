package java.lang.ref;

import dalvik.annotation.optimization.FastNative;

public final class FinalizerReference<T> extends Reference<T> {
    private FinalizerReference<?> next;
    private FinalizerReference<?> prev;
    private T zombie;
    public static final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private static final Object LIST_LOCK = new Object();
    private static FinalizerReference<?> head = null;

    @FastNative
    private final native T getReferent();

    @FastNative
    private native boolean makeCircularListIfUnenqueued();

    public FinalizerReference(T t, ReferenceQueue<? super T> referenceQueue) {
        super(t, referenceQueue);
    }

    @Override
    public T get() {
        return this.zombie;
    }

    @Override
    public void clear() {
        this.zombie = null;
    }

    public static void add(Object obj) {
        FinalizerReference<?> finalizerReference = new FinalizerReference<>(obj, queue);
        synchronized (LIST_LOCK) {
            ((FinalizerReference) finalizerReference).prev = null;
            ((FinalizerReference) finalizerReference).next = head;
            if (head != null) {
                ((FinalizerReference) head).prev = finalizerReference;
            }
            head = finalizerReference;
        }
    }

    public static void remove(FinalizerReference<?> finalizerReference) {
        synchronized (LIST_LOCK) {
            FinalizerReference<?> finalizerReference2 = ((FinalizerReference) finalizerReference).next;
            FinalizerReference<?> finalizerReference3 = ((FinalizerReference) finalizerReference).prev;
            ((FinalizerReference) finalizerReference).next = null;
            ((FinalizerReference) finalizerReference).prev = null;
            if (finalizerReference3 != null) {
                ((FinalizerReference) finalizerReference3).next = finalizerReference2;
            } else {
                head = finalizerReference2;
            }
            if (finalizerReference2 != null) {
                ((FinalizerReference) finalizerReference2).prev = finalizerReference3;
            }
        }
    }

    public static void finalizeAllEnqueued(long j) throws InterruptedException {
        Sentinel sentinel;
        do {
            sentinel = new Sentinel();
        } while (!enqueueSentinelReference(sentinel));
        sentinel.awaitFinalization(j);
    }

    private static boolean enqueueSentinelReference(Sentinel sentinel) {
        synchronized (LIST_LOCK) {
            for (FinalizerReference<?> finalizerReference = head; finalizerReference != null; finalizerReference = ((FinalizerReference) finalizerReference).next) {
                if (finalizerReference.getReferent() == sentinel) {
                    finalizerReference.clearReferent();
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

        synchronized void awaitFinalization(long j) throws InterruptedException {
            long jNanoTime = System.nanoTime() + j;
            while (!this.finalized) {
                if (j != 0) {
                    long jNanoTime2 = System.nanoTime();
                    if (jNanoTime2 >= jNanoTime) {
                        break;
                    }
                    long j2 = jNanoTime - jNanoTime2;
                    wait(j2 / 1000000, (int) (j2 % 1000000));
                } else {
                    wait();
                }
            }
        }
    }
}
