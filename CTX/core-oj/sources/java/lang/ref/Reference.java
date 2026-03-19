package java.lang.ref;

import dalvik.annotation.optimization.FastNative;

public abstract class Reference<T> {
    private static boolean disableIntrinsic = false;
    private static boolean slowPathEnabled = false;
    Reference<?> pendingNext;
    final ReferenceQueue<? super T> queue;
    Reference queueNext;
    volatile T referent;

    @FastNative
    private final native T getReferent();

    @FastNative
    native void clearReferent();

    public T get() {
        return getReferent();
    }

    public void clear() {
        clearReferent();
    }

    public boolean isEnqueued() {
        return this.queue != null && this.queue.isEnqueued(this);
    }

    public boolean enqueue() {
        return this.queue != null && this.queue.enqueue(this);
    }

    Reference(T t) {
        this(t, null);
    }

    Reference(T t, ReferenceQueue<? super T> referenceQueue) {
        this.referent = t;
        this.queue = referenceQueue;
    }

    public static void reachabilityFence(Object obj) {
        SinkHolder.sink = obj;
        if (SinkHolder.finalize_count == 0) {
            SinkHolder.sink = null;
        }
    }

    private static class SinkHolder {
        static volatile Object sink;
        private static volatile int finalize_count = 0;
        private static Object sinkUser = new Object() {
            protected void finalize() {
                if (SinkHolder.sink == null && SinkHolder.finalize_count > 0) {
                    throw new AssertionError((Object) "Can't get here");
                }
                SinkHolder.access$008();
            }
        };

        private SinkHolder() {
        }

        static int access$008() {
            int i = finalize_count;
            finalize_count = i + 1;
            return i;
        }
    }
}
